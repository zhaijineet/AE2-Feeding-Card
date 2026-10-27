package net.zhaiji.ae2feedingcard.client.screen;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Inventory;
import net.zhaiji.ae2feedingcard.compat.CompatManager;
import net.zhaiji.ae2feedingcard.menu.FeedingCardSettingsMenu;
import net.zhaiji.ae2feedingcard.util.FeedingCardSettings;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CompoundEffectHandling;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CraftTiming;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CraftWaiting;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.FullHungerBehavior;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.HarmfulEffectHandling;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.Primary;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.Secondary;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.UneatenFoodHandling;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 喂食卡设置界面：内容按行建模，滚动条以行为单位滚动，可见行之外的控件直接隐藏，不做裁剪。
 */
public class FeedingCardSettingsScreen extends AEBaseScreen<FeedingCardSettingsMenu> {
    sealed interface Row {
    }

    /**
     * 标题行：主标题顶格、副标题缩进到选项文字，层级只靠缩进不靠颜色或分隔线；依赖不可用时整条变灰。
     */
    record TitleRow(Component text, boolean group, BooleanSupplier available) implements Row {
    }

    /**
     * 控件行：复选框或单选按钮。
     */
    record ControlRow(AECheckbox control) implements Row {
    }

    private static final String PRIMARY_STRATEGY = "primary";
    private static final String SECONDARY_STRATEGY = "secondary";
    private static final String COMPOUND_EFFECT = "compound_effect";
    private static final String FULL_HUNGER_BEHAVIOR = "full_hunger";
    private static final String CRAFT_TIMING = "craft_timing";
    private static final String CRAFT_WAITING = "craft_waiting";
    private static final String HARMFUL_EFFECT = "harmful_effect";
    private static final String UNEATEN_FOOD = "uneaten";

    /**
     * 视口几何与底图里的轨道、以及 JSON 的 scrollbar 条目必须三方一致：
     * 底图 {@code ae2feedingcard:textures/guis/feeding_card_settings.png} 的轨道是 x 183~188、y 20~205，
     * JSON 的 scrollbar 用 {@code left 180 / top 21 / height 184} 把 12×15 的滑块摆在轨道中心（左右各凸出 3px）。
     * 视口左边界 8 与 JSON 里 dialog_title 的 {@code left 8} 对齐，右边界 178 与轨道间隔 5px。
     * JSON 里 scrollbar 的 {@code width} 对 Scrollbar 无效，宽度取 {@code big_scroller} 精灵的真实像素宽。
     *
     * <p>视口高 186 与 JSON 的 {@code scrollbar.height} 184 差 2px，量的不是同一件事：186 是轨道含上下各一条
     * 1px 边线的总高（底图 y 20~205），184 是去掉边线后的内腔（y 21~204），也就是滑块的实际行程空间。
     * 依据是 {@code Scrollbar} 的 {@code availableHeight = this.height - style.handleHeight()}：
     * JSON 的 {@code height} 只当行程用内腔，不是控件外框高。</p>
     */
    private static final int VIEWPORT_LEFT = 8;
    private static final int VIEWPORT_TOP = 20;
    private static final int VIEWPORT_WIDTH = 170;
    private static final int VIEWPORT_HEIGHT = 186;
    /**
     * 自定的最小行距，AE2 没有对应常量，取 20 对齐 AE2 标准控件高度。
     */
    private static final int MIN_ROW_HEIGHT = 20;
    /**
     * {@code AECheckbox} 单行文字相对控件 y 的偏移，取自 AE2
     * {@code appeng/client/gui/widgets/AECheckbox.java:111} 的 {@code getY() + 4}，不是文字高度。
     */
    private static final int SINGLE_LINE_TEXT_OFFSET = 4;
    /**
     * 行高按最长文字块加上下各这么多像素得出，不再按 14px 控件本身推导：多行文字会画出控件高度之外，
     * 让控件居中反而会顶穿行边界。
     */
    private static final int ROW_PADDING = 3;
    /**
     * {@code AECheckbox} 折行时预留的宽度，取自 AE2 {@code appeng/client/gui/widgets/AECheckbox.java:109}
     * 的 {@code font.split(getMessage(), width - 22)}，用于估算折行行数。
     */
    private static final int TEXT_WRAP_RESERVE = 22;
    /**
     * 层级按 AE2 的数值规律：标题在 8，选项比标题右 2px 在 10；副标题在 26，与缩进单选的方框左边缘对齐。
     *
     * <p>控件内文字的内缩由 {@code AECheckbox} 自己决定（{@code appeng/client/gui/widgets/AECheckbox.java:113}；
     * {@code getX() + (isRadio() ? 16 : 26)}）：顶格复选框方框 10 → 文字 36，缩进单选方框 26 → 文字 42。</p>
     */
    private static final int OPTION_LEFT = VIEWPORT_LEFT + 2;
    private static final int OPTION_INDENT = 16;
    private static final int INDENTED_LEFT = OPTION_LEFT + OPTION_INDENT;
    /**
     * 末尾减 4 是主动补偿文字右端的 4px 外溢：{@code AECheckbox} 折行宽度取 {@code width - 22}
     * 而复选框的文字起点在 {@code getX() + 26}，这一支的文字右端上限为 {@code X + W + 4}，比命中区右边界多 4px。
     * 减掉后顶格选项的文字右端正好落在视口右边界 178，代价是命中区止于 174；
     * 不减则文字右端到 182，离轨道左沿 183 只剩 1px，会压线。
     *
     * <p>AE2 自身不做这个补偿：{@code terminal_settings.json} 的 {@code pinAutoCraftedItemsCheckbox}
     * 同样外溢 4px，只是被面板内边线遮住。</p>
     */
    private static final int FLUSH_OPTION_WIDTH = VIEWPORT_WIDTH - (OPTION_LEFT - VIEWPORT_LEFT) - 4;
    /**
     * 单选分支的文字起点是 {@code getX() + 16}、折行宽度同为 {@code width - 22}，文字右端为 {@code X + W - 6}。
     * 与复选框一样用 {@code -4}，两者的宽度算法保持同形。命中区右端 174 与顶格选项对齐，
     * 宽度到 155 才会覆盖滚动条命中区的第一列 180（两侧判据都是左闭右开，154 时右端正好停在 179）。
     * 另外「可合成项参与比较（较耗性能）」正好14个全角字符×9px=126等于折行宽度，再多一个字就会折成两行；
     * 这个 9px 来自原版字体资源包的 CJK 区间设置，玩家换了改字宽的字体包时该边界会跟着移动。
     */
    private static final int INDENTED_OPTION_WIDTH = VIEWPORT_WIDTH - (INDENTED_LEFT - VIEWPORT_LEFT) - 4;

    private static final Component REMAINDER_TO_NETWORK_LABEL = Component.translatable("gui.ae2feedingcard.remainder_to_network");
    private static final Component REMAINDER_TO_INVENTORY_LABEL = Component.translatable("gui.ae2feedingcard.remainder_to_inventory");
    private static final Component PLAY_EAT_SOUND_LABEL = Component.translatable("gui.ae2feedingcard.play_eat_sound");
    private static final Component VOID_OVERFLOW_LABEL = Component.translatable("gui.ae2feedingcard.void_overflow");

    private final Scrollbar scrollbar;
    /**
     * 界面内容按从上到下的顺序追加，下标即行号：滚动偏移直接当行下标用，标题与控件不再各自记账。
     */
    private final List<Row> rows = new ArrayList<>();
    private final Map<AECheckbox, BooleanSupplier> dependencyEnabled = new IdentityHashMap<>();
    /**
     * 每个控件折行后的行数，创建时量一次供摆放与居中取用，不在每帧的排布里重算。
     */
    private final Map<AECheckbox, Integer> textLineCounts = new IdentityHashMap<>();

    private AECheckbox remainderToNetworkCheckbox;
    private AECheckbox remainderToInventoryCheckbox;
    private AECheckbox playEatSoundCheckbox;
    private AECheckbox voidOverflowCheckbox;

    private Map<Primary, AECheckbox> primaryStrategyRadios = Map.of();
    private Map<Secondary, AECheckbox> secondaryStrategyRadios = Map.of();
    private Map<CompoundEffectHandling, AECheckbox> compoundEffectRadios = Map.of();
    private Map<FullHungerBehavior, AECheckbox> fullHungerBehaviorRadios = Map.of();
    private Map<CraftTiming, AECheckbox> craftTimingRadios = Map.of();
    private Map<CraftWaiting, AECheckbox> craftWaitingRadios = Map.of();
    private Map<HarmfulEffectHandling, AECheckbox> harmfulEffectRadios = Map.of();
    private Map<UneatenFoodHandling, AECheckbox> uneatenFoodRadios = Map.of();

    private int maxTextLines = 1;
    private int rowHeight = MIN_ROW_HEIGHT;
    private int visibleRows;

    private boolean voidCardAvailable;
    private boolean craftingCardAvailable;
    private boolean inverterCardAvailable;
    private boolean equalDistributionCardAvailable;
    private boolean secondaryIgnoresEffect;

    /**
     * 标题与选项共用同一份依赖状态，避免同一组里标题亮着而选项全灰；用供应商延迟求值，
     * 因为建内容发生在 {@link #init()} 里、早于 {@link #updateBeforeRender()}，那时卡片安装状态还是初始值。
     */
    private final BooleanSupplier alwaysAvailable = () -> true;
    private final BooleanSupplier voidCardGate = () -> this.voidCardAvailable;
    private final BooleanSupplier craftingCardGate = () -> this.craftingCardAvailable;
    private final BooleanSupplier inverterCardGate = () -> this.inverterCardAvailable;
    private final BooleanSupplier spiceOfLifeGate = () -> CompatManager.SOL_CARROT_LOADED;

    /**
     * 均分卡按轮转挑选，主副策略与复合效果处理都不再参与，三组一起变灰；副策略选「不管效果」时，
     * 复合效果处理的分档结果无人读取，这一组再单独变灰。
     */
    private final BooleanSupplier strategyGate = () -> !this.equalDistributionCardAvailable;
    private final BooleanSupplier compoundEffectGate = () -> !this.equalDistributionCardAvailable
            && !this.secondaryIgnoresEffect;

    public FeedingCardSettingsScreen(FeedingCardSettingsMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        AESubScreen.addBackButton(menu, "back", this.widgets, Component.translatable("gui.ae2feedingcard.back"));

        this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.BIG);
    }

    @Override
    protected void init() {
        super.init();

        this.rows.clear();
        this.dependencyEnabled.clear();
        this.textLineCounts.clear();
        this.maxTextLines = 1;

        this.remainderToNetworkCheckbox = createCheckbox(REMAINDER_TO_NETWORK_LABEL, this.alwaysAvailable);
        this.remainderToInventoryCheckbox = createCheckbox(REMAINDER_TO_INVENTORY_LABEL, this.alwaysAvailable);
        this.playEatSoundCheckbox = createCheckbox(PLAY_EAT_SOUND_LABEL, this.alwaysAvailable);
        this.voidOverflowCheckbox = createCheckbox(VOID_OVERFLOW_LABEL, this.voidCardGate);

        this.primaryStrategyRadios = createRadioGroup(PRIMARY_STRATEGY, Primary.values(), this.strategyGate);
        this.secondaryStrategyRadios = createRadioGroup(SECONDARY_STRATEGY, Secondary.values(), this.strategyGate);
        this.compoundEffectRadios = createRadioGroup(COMPOUND_EFFECT, CompoundEffectHandling.values(),
                this.compoundEffectGate);
        this.fullHungerBehaviorRadios = createRadioGroup(FULL_HUNGER_BEHAVIOR, FullHungerBehavior.values(),
                this.voidCardGate);
        this.craftTimingRadios = createRadioGroup(CRAFT_TIMING, CraftTiming.values(), this.craftingCardGate);
        this.craftWaitingRadios = createRadioGroup(CRAFT_WAITING, CraftWaiting.values(), this.craftingCardGate);
        this.harmfulEffectRadios = createRadioGroup(HARMFUL_EFFECT, HarmfulEffectHandling.values(),
                this.inverterCardGate);
        this.uneatenFoodRadios = createRadioGroup(UNEATEN_FOOD, UneatenFoodHandling.values(), this.spiceOfLifeGate);

        this.rowHeight = Math.max(MIN_ROW_HEIGHT, this.maxTextLines * this.font.lineHeight + 2 * ROW_PADDING);
        this.visibleRows = VIEWPORT_HEIGHT / this.rowHeight;

        buildContent();

        this.scrollbar.setRange(0, Math.max(0, this.rows.size() - this.visibleRows), 1);
        applyScroll();
    }

    /**
     * 按界面从上到下把标题与控件追加进 {@link #rows}，下标即行号；每个复选框占一行，每个单选组按单列竖排占对应行数。
     *
     * <p>标题的依赖按「它下面所有内容是否都不可用」给：无条件内容一律用 {@link #alwaysAvailable}。</p>
     */
    private void buildContent() {
        addTitle("gui.ae2feedingcard.group.feeding", true, this.alwaysAvailable);
        placeCheckbox(this.remainderToNetworkCheckbox);
        placeCheckbox(this.remainderToInventoryCheckbox);
        placeCheckbox(this.playEatSoundCheckbox);

        addTitle("gui.ae2feedingcard.subtitle.primary_strategy", false, this.strategyGate);
        placeRadioGroup(this.primaryStrategyRadios);

        addTitle("gui.ae2feedingcard.subtitle.secondary_strategy", false, this.strategyGate);
        placeRadioGroup(this.secondaryStrategyRadios);

        addTitle("gui.ae2feedingcard.subtitle.compound_effect", false, this.compoundEffectGate);
        placeRadioGroup(this.compoundEffectRadios);

        addTitle("gui.ae2feedingcard.group.void_overflow_card", true, this.voidCardGate);
        placeCheckbox(this.voidOverflowCheckbox);

        addTitle("gui.ae2feedingcard.subtitle.full_hunger_behavior", false, this.voidCardGate);
        placeRadioGroup(this.fullHungerBehaviorRadios);

        addTitle("gui.ae2feedingcard.group.crafting_card", true, this.craftingCardGate);

        addTitle("gui.ae2feedingcard.subtitle.craft_timing", false, this.craftingCardGate);
        placeRadioGroup(this.craftTimingRadios);

        addTitle("gui.ae2feedingcard.subtitle.craft_waiting", false, this.craftingCardGate);
        placeRadioGroup(this.craftWaitingRadios);

        addTitle("gui.ae2feedingcard.group.inverter_card", true, this.inverterCardGate);

        addTitle("gui.ae2feedingcard.subtitle.harmful_effect", false, this.inverterCardGate);
        placeRadioGroup(this.harmfulEffectRadios);

        addTitle("gui.ae2feedingcard.group.mod_compat", true, this.spiceOfLifeGate);

        addTitle("gui.ae2feedingcard.subtitle.spice_of_life", false, this.spiceOfLifeGate);
        addTitle("gui.ae2feedingcard.subtitle.uneaten_food", false, this.spiceOfLifeGate);
        placeRadioGroup(this.uneatenFoodRadios);
    }

    /**
     * 主标题顶格、副标题缩进到选项文字，层级只靠缩进不靠颜色或分隔线；依赖不可用时整条变灰。
     */
    private void addTitle(String translationKey, boolean group, BooleanSupplier available) {
        // 标题假定单行，不参与行高测量；drawString 既不折行也不裁剪，翻译变长会直接画出对话框外
        this.rows.add(new TitleRow(Component.translatable(translationKey), group, available));
    }

    /**
     * 复选框都没有副标题，顶格摆放。
     */
    private AECheckbox createCheckbox(Component label, BooleanSupplier gate) {
        AECheckbox checkbox = new AECheckbox(0, 0, FLUSH_OPTION_WIDTH, AECheckbox.SIZE, this.style, label);
        checkbox.setChangeListener(this::sendSettingsIfChanged);
        this.dependencyEnabled.put(checkbox, gate);
        int lineCount = this.font.split(label, FLUSH_OPTION_WIDTH - TEXT_WRAP_RESERVE).size();
        this.textLineCounts.put(checkbox, lineCount);
        this.maxTextLines = Math.max(this.maxTextLines, lineCount);
        addRenderableWidget(checkbox);
        return checkbox;
    }

    private void placeCheckbox(AECheckbox checkbox) {
        checkbox.setX(this.leftPos + OPTION_LEFT);
        this.rows.add(new ControlRow(checkbox));
    }

    /**
     * 一组单列竖排的单选按钮，都属于紧邻其上的副标题，所以整体缩进；组内始终只有一个被选中。
     */
    private <E extends Enum<E> & StringRepresentable> Map<E, AECheckbox> createRadioGroup(
            String widgetIdPrefix, E[] options, BooleanSupplier gate) {
        Map<E, AECheckbox> radios = new LinkedHashMap<>();

        for (E radioValue : options) {
            Component label = Component.translatable(
                    "gui.ae2feedingcard." + widgetIdPrefix + "." + radioValue.getSerializedName());
            AECheckbox radioButton = new AECheckbox(0, 0, INDENTED_OPTION_WIDTH, AECheckbox.SIZE, this.style, label);
            radioButton.setRadio(true);
            radioButton.setChangeListener(() -> selectAndSend(radios, radioValue));
            this.dependencyEnabled.put(radioButton, gate);
            radios.put(radioValue, radioButton);

            int lineCount = this.font.split(label, INDENTED_OPTION_WIDTH - TEXT_WRAP_RESERVE).size();
            this.textLineCounts.put(radioButton, lineCount);
            this.maxTextLines = Math.max(this.maxTextLines, lineCount);
            addRenderableWidget(radioButton);
        }

        return radios;
    }

    private void placeRadioGroup(Map<?, AECheckbox> radios) {
        for (AECheckbox radioButton : radios.values()) {
            radioButton.setX(this.leftPos + INDENTED_LEFT);
            this.rows.add(new ControlRow(radioButton));
        }
    }

    private static void syncRadioSelection(Map<?, AECheckbox> radios, Object selected) {
        radios.forEach((radioValue, radioButton) -> {
            boolean shouldSelect = radioValue == selected;
            if (radioButton.isSelected() != shouldSelect) radioButton.setSelected(shouldSelect);
        });
    }

    private static <E> E selectedRadioValue(Map<E, AECheckbox> radios, E fallback) {
        for (Map.Entry<E, AECheckbox> entry : radios.entrySet()) {
            if (entry.getValue().isSelected()) return entry.getKey();
        }

        return fallback;
    }

    private <E> void selectAndSend(Map<E, AECheckbox> radios, E radioValue) {
        syncRadioSelection(radios, radioValue);
        sendSettingsIfChanged();
    }

    /**
     * 把可见行排到固定位置，不可见行直接隐藏；越界的控件不参与绘制也不参与点击。
     */
    private void applyScroll() {
        int firstVisibleRow = this.scrollbar.getCurrentScroll();

        for (int rowIndex = 0; rowIndex < this.rows.size(); rowIndex++) {
            if (!(this.rows.get(rowIndex) instanceof ControlRow controlRow)) continue;

            AECheckbox control = controlRow.control();
            int relativeRow = rowIndex - firstVisibleRow;
            boolean rowVisible = relativeRow >= 0 && relativeRow < this.visibleRows;

            control.visible = rowVisible;
            control.active = rowVisible && isDependencyEnabled(control);
            if (!rowVisible) continue;

            // 文字顶相对控件 y 是 4（单行）或 1（多行），要让文字块在行内居中就得反推控件 y
            int lineCount = this.textLineCounts.getOrDefault(control, 1);
            control.setY(this.topPos + VIEWPORT_TOP + relativeRow * this.rowHeight
                    + (this.rowHeight - lineCount * this.font.lineHeight) / 2
                    - (lineCount <= 1 ? SINGLE_LINE_TEXT_OFFSET : 1));
        }
    }

    /**
     * 标题在renderLabels阶段绘制，此时pose已平移到leftPos/topPos，所以坐标是相对值。
     */
    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        int firstVisibleRow = this.scrollbar.getCurrentScroll();

        for (int rowIndex = 0; rowIndex < this.rows.size(); rowIndex++) {
            if (!(this.rows.get(rowIndex) instanceof TitleRow titleRow)) continue;

            int relativeRow = rowIndex - firstVisibleRow;
            if (relativeRow < 0 || relativeRow >= this.visibleRows) continue;

            int textX = titleRow.group() ? VIEWPORT_LEFT : INDENTED_LEFT;
            int textY = VIEWPORT_TOP + relativeRow * this.rowHeight
                    + (this.rowHeight - this.font.lineHeight) / 2;
            int textColor = titleRow.available().getAsBoolean()
                    ? this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB()
                    : this.style.getColor(PaletteColor.MUTED_TEXT_COLOR).toARGB();
            guiGraphics.drawString(this.font, titleRow.text(), textX, textY, textColor, false);
        }
    }

    private boolean isDependencyEnabled(AECheckbox checkbox) {
        return this.dependencyEnabled.getOrDefault(checkbox, this.alwaysAvailable).getAsBoolean();
    }

    private void sendSettingsIfChanged() {
        FeedingCardSettings currentSettings = this.getMenu().getSettings();
        FeedingCardSettings widgetSettings = new FeedingCardSettings(
                this.remainderToNetworkCheckbox.isSelected(),
                this.remainderToInventoryCheckbox.isSelected(),
                this.playEatSoundCheckbox.isSelected(),
                selectedRadioValue(this.primaryStrategyRadios, currentSettings.primaryStrategy()),
                selectedRadioValue(this.secondaryStrategyRadios, currentSettings.secondaryStrategy()),
                selectedRadioValue(this.compoundEffectRadios, currentSettings.compoundEffectHandling()),
                this.voidOverflowCheckbox.isSelected(),
                selectedRadioValue(this.fullHungerBehaviorRadios, currentSettings.fullHungerBehavior()),
                selectedRadioValue(this.craftTimingRadios, currentSettings.craftTiming()),
                selectedRadioValue(this.craftWaitingRadios, currentSettings.craftWaiting()),
                selectedRadioValue(this.harmfulEffectRadios, currentSettings.harmfulEffectHandling()),
                selectedRadioValue(this.uneatenFoodRadios, currentSettings.uneatenFoodHandling())
        );
        if (widgetSettings.equals(currentSettings)) return;

        this.getMenu().sendSettings(widgetSettings);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        FeedingCardSettings settings = this.getMenu().getSettings();
        syncSelected(this.remainderToNetworkCheckbox, settings.remainderToNetwork());
        syncSelected(this.remainderToInventoryCheckbox, settings.remainderToInventory());
        syncSelected(this.playEatSoundCheckbox, settings.playEatSound());
        syncSelected(this.voidOverflowCheckbox, settings.voidOverflow());

        syncRadioSelection(this.primaryStrategyRadios, settings.primaryStrategy());
        syncRadioSelection(this.secondaryStrategyRadios, settings.secondaryStrategy());
        syncRadioSelection(this.compoundEffectRadios, settings.compoundEffectHandling());
        syncRadioSelection(this.fullHungerBehaviorRadios, settings.fullHungerBehavior());
        syncRadioSelection(this.craftTimingRadios, settings.craftTiming());
        syncRadioSelection(this.craftWaitingRadios, settings.craftWaiting());
        syncRadioSelection(this.harmfulEffectRadios, settings.harmfulEffectHandling());
        syncRadioSelection(this.uneatenFoodRadios, settings.uneatenFoodHandling());

        this.voidCardAvailable = this.getMenu().isVoidCardInstalled();
        this.craftingCardAvailable = this.getMenu().isCraftingCardInstalled();
        this.inverterCardAvailable = this.getMenu().isInverterCardInstalled();
        this.equalDistributionCardAvailable = this.getMenu().isEqualDistributionCardInstalled();
        this.secondaryIgnoresEffect = settings.secondaryStrategy() == Secondary.IGNORE;

        applyScroll();
    }

    private static void syncSelected(AECheckbox checkbox, boolean selected) {
        if (checkbox.isSelected() != selected) checkbox.setSelected(selected);
    }
}
