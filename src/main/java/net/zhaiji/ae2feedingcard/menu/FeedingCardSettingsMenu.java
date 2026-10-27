package net.zhaiji.ae2feedingcard.menu;

import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardItems;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardMenus;
import net.zhaiji.ae2feedingcard.util.FeedingCardAccess;
import net.zhaiji.ae2feedingcard.util.FeedingCardSettings;

/**
 * 喂食卡设置界面：余物开关、主副进食策略、饥饿值满时的行为、合成与反向卡设置、模组兼容，宿主是上层的喂食卡界面。
 */
public class FeedingCardSettingsMenu extends AEBaseMenu implements ISubMenu {
    private static final String SETTINGS_ACTION = "feedingCardSettings";

    private final FeedingCardAccess access;
    private final ISubMenuHost settingsHost;

    @GuiSync(0)
    public FeedingCardSettings settings = FeedingCardSettings.DEFAULT;

    public FeedingCardSettingsMenu(MenuType<?> menuType, int id, Inventory playerInventory, IPortableTerminal host) {
        super(menuType, id, playerInventory, host);
        this.access = FeedingCardAccess.fromTerminalHost(host);

        this.settings = this.access == null ? FeedingCardSettings.DEFAULT : this.access.readSettings();

        this.settingsHost = new FeedingCardSettingsHost(
                getMainMenuIcon(),
                AE2FeedingCardMenus.FEEDING_CARD.get()
        );

        registerClientAction(SETTINGS_ACTION, FeedingCardSettings.class, this::applySettings);
    }

    private ItemStack getMainMenuIcon() {
        if (this.access == null) return new ItemStack(AE2FeedingCardItems.FEEDING_CARD.get());

        ItemStack feedingCardStack = this.access.getFeedingCardStack();
        if (feedingCardStack == null) return new ItemStack(AE2FeedingCardItems.FEEDING_CARD.get());

        return feedingCardStack.copyWithCount(1);
    }

    public FeedingCardSettings getSettings() {
        return this.settings;
    }

    /**
     * 把整套设置发给服务端，走 AE2 的菜单客户端动作通道。
     *
     * <p>客户端先改本地副本：{@code AECheckbox} 的 {@code onPress} 会自己翻转选中状态，
     * 不提前同步字段的话下一帧 {@code updateBeforeRender} 会用旧值把它刷回去，表现为点了又弹回。</p>
     */
    public void sendSettings(FeedingCardSettings settings) {
        if (isClientSide()) {
            this.settings = settings;
        }
        sendClientAction(SETTINGS_ACTION, settings);
    }

    /**
     * 写入整套设置，只在服务端执行；与组件现值的比较由 {@link net.zhaiji.ae2feedingcard.util.FeedingCardAccess} 负责。
     *
     * <p>这里不校验载荷，与AE2的做法一致：{@code registerClientAction}传的是Gson JSON，
     * {@code EnumTypeAdapter}遇到未知枚举名只回填null、不抛异常（{@code TypeAdapters:905-913}）；
     * AE2自己的{@code CellWorkbenchMenu:79}用{@code FuzzyMode.class}、{@code PatternEncodingTermMenu:191}
     * 用{@code EncodingMode.class}做载荷时同样不校验，而{@code GuiActionPacket}只作用于发起者自己的
     * {@code containerMenu}，坏值先坏在他自己那儿。</p>
     *
     * <p>已知代价：新旧版本混跑时枚举名对不上会产出null字段，后果分三层递进。组件网络编码时
     * {@code writeEnum(null)}取{@code value.ordinal()}，在服务端发包线程抛NPE；存档编码时
     * {@code stringResolver}对null调{@code getSerializedName()}，同样抛NPE；组件解码时枚举名解不出来，
     * 整个{@code FEEDING_SETTINGS}解码失败，再经{@code ItemContainerContents}与{@code UPGRADES}逐层放大，
     * 整台无线终端退化成{@code ItemStack.EMPTY}。没写进组件时，{@code switch (settings.primaryStrategy())}
     * 也会在下一次进食检查时抛NPE。</p>
     */
    public void applySettings(FeedingCardSettings settings) {
        if (isClientSide()) return;
        if (this.access == null) return;

        this.access.writeSettings(settings);
    }

    /**
     * 服务端每tick从卡片重读同步字段；卡片不在时保持旧值，避免界面跳到默认设置。
     *
     * <p>必须在{@code super.broadcastChanges()}之前刷新：GuiSync的脏检查在父类里做，
     * 顺序反了会让新值永远晚一tick才发出去。</p>
     */
    @Override
    public void broadcastChanges() {
        if (isServerSide() && this.access != null) {
            ItemStack feedingCardStack = this.access.getFeedingCardStack();
            if (feedingCardStack != null) {
                this.settings = FeedingCardAccess.readSettings(feedingCardStack);
            }
        }

        super.broadcastChanges();
    }

    public boolean isVoidCardInstalled() {
        return this.access != null && this.access.isVoidCardInstalled();
    }

    public boolean isEqualDistributionCardInstalled() {
        return this.access != null && this.access.isEqualDistributionCardInstalled();
    }

    public boolean isInverterCardInstalled() {
        return this.access != null && this.access.isInverterCardInstalled();
    }

    public boolean isCraftingCardInstalled() {
        return this.access != null && this.access.isCraftingCardInstalled();
    }

    @Override
    public ISubMenuHost getHost() {
        return this.settingsHost;
    }
}
