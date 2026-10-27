package net.zhaiji.ae2feedingcard.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardCreativeModeTabs;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardItems;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy;
import org.jetbrains.annotations.Nullable;

public class AE2FeedingCardLanguageProvider extends LanguageProvider {
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";

    public final String locale;

    public AE2FeedingCardLanguageProvider(PackOutput output, String locale) {
        super(output, AE2FeedingCard.MOD_ID, locale);
        this.locale = locale;
    }

    /**
     * 添加 ConfigurationScreen 配置项或分组的名称与可选说明，键前缀由模组ID拼出。
     */
    private void addConfig(String key, String name, @Nullable String tooltip) {
        add(AE2FeedingCard.MOD_ID + ".configuration." + key, name);
        if (tooltip != null) add(AE2FeedingCard.MOD_ID + ".configuration." + key + ".tooltip", tooltip);
    }

    private void addConfig(String key, String name) {
        addConfig(key, name, null);
    }

    /**
     * 添加界面文案，自动补上gui与模组ID前缀。
     */
    private void addGui(String key, String value) {
        add("gui." + AE2FeedingCard.MOD_ID + "." + key, value);
    }

    /**
     * 八个进食策略枚举各有一个方法，内部用穷尽switch遍历常量：新增枚举常量却不补译名时编译失败。
     */
    private void addPrimary(String avoidWaste, String highSaturation) {
        for (FeedingStrategy.Primary primary : FeedingStrategy.Primary.values()) {
            addGui("primary." + primary.getSerializedName(), switch (primary) {
                case AVOID_WASTE -> avoidWaste;
                case HIGH_SATURATION -> highSaturation;
            });
        }
    }

    private void addSecondary(String ignore, String preferNoEffect, String preferBeneficial) {
        for (FeedingStrategy.Secondary secondary : FeedingStrategy.Secondary.values()) {
            addGui("secondary." + secondary.getSerializedName(), switch (secondary) {
                case IGNORE -> ignore;
                case PREFER_NO_EFFECT -> preferNoEffect;
                case PREFER_BENEFICIAL -> preferBeneficial;
            });
        }
    }

    private void addCompoundEffect(String strict, String offset) {
        for (FeedingStrategy.CompoundEffectHandling compoundEffect : FeedingStrategy.CompoundEffectHandling.values()) {
            addGui("compound_effect." + compoundEffect.getSerializedName(), switch (compoundEffect) {
                case STRICT -> strict;
                case OFFSET -> offset;
            });
        }
    }

    private void addFullHunger(String stop, String eatBelowFifteen, String eatBelowTen, String eatBelowFive) {
        for (FeedingStrategy.FullHungerBehavior fullHunger : FeedingStrategy.FullHungerBehavior.values()) {
            addGui("full_hunger." + fullHunger.getSerializedName(), switch (fullHunger) {
                case STOP -> stop;
                case EAT_BELOW_FIFTEEN -> eatBelowFifteen;
                case EAT_BELOW_TEN -> eatBelowTen;
                case EAT_BELOW_FIVE -> eatBelowFive;
            });
        }
    }

    private void addCraftTiming(String onlyWhenEmpty, String preferBetter) {
        for (FeedingStrategy.CraftTiming craftTiming : FeedingStrategy.CraftTiming.values()) {
            addGui("craft_timing." + craftTiming.getSerializedName(), switch (craftTiming) {
                case ONLY_WHEN_EMPTY -> onlyWhenEmpty;
                case PREFER_BETTER -> preferBetter;
            });
        }
    }

    private void addCraftWaiting(String keepEating, String waitForCompletion) {
        for (FeedingStrategy.CraftWaiting craftWaiting : FeedingStrategy.CraftWaiting.values()) {
            addGui("craft_waiting." + craftWaiting.getSerializedName(), switch (craftWaiting) {
                case KEEP_EATING -> keepEating;
                case WAIT_FOR_COMPLETION -> waitForCompletion;
            });
        }
    }

    private void addHarmfulEffect(String keep, String excludeDefinite, String excludeAll) {
        for (FeedingStrategy.HarmfulEffectHandling harmfulEffect : FeedingStrategy.HarmfulEffectHandling.values()) {
            addGui("harmful_effect." + harmfulEffect.getSerializedName(), switch (harmfulEffect) {
                case KEEP -> keep;
                case EXCLUDE_DEFINITE -> excludeDefinite;
                case EXCLUDE_ALL -> excludeAll;
            });
        }
    }

    private void addUneaten(String ignore, String preferUneaten, String preferUneatenIncludingHarmful) {
        for (FeedingStrategy.UneatenFoodHandling uneaten : FeedingStrategy.UneatenFoodHandling.values()) {
            addGui("uneaten." + uneaten.getSerializedName(), switch (uneaten) {
                case IGNORE -> ignore;
                case PREFER_UNEATEN -> preferUneaten;
                case PREFER_UNEATEN_INCLUDING_HARMFUL -> preferUneatenIncludingHarmful;
            });
        }
    }

    public void English() {
        addItem(AE2FeedingCardItems.FEEDING_CARD, "Feeding Card");
        add(AE2FeedingCardCreativeModeTabs.AE2FEEDINGCARD_TAB_TRANSLATABLE, "AE2 Feeding Card");

        addGui("feeding_card", "Feeding Card");
        addGui("feeding_card_settings", "Feeding Card Settings");
        addGui("feeding_card_toggle", "Feeding Card Toggle");
        addGui("feeding_card_toggle.state_off", "Off");
        addGui("feeding_card_toggle.state_on", "On");
        addGui("back", "Back");
        addGui("remainder_to_network", "Store Containers in ME");
        addGui("remainder_to_inventory", "Containers to Inventory");
        addGui("void_overflow", "Void Containers if Full");
        addGui("play_eat_sound", "Play Eat Sound");

        addGui("group.feeding", "Basic Settings");
        addGui("group.void_overflow_card", "Void Overflow Card");
        addGui("group.crafting_card", "Crafting Card");
        addGui("group.inverter_card", "Inverter Card");
        addGui("group.mod_compat", "Mod Compatibility");

        addGui("subtitle.primary_strategy", "Primary Feeding Strategy");
        addGui("subtitle.secondary_strategy", "Secondary Feeding Strategy");
        addGui("subtitle.compound_effect", "Compound Effect Handling");
        addGui("subtitle.full_hunger_behavior", "Behavior at Full Hunger");
        addGui("subtitle.craft_timing", "Crafting Timing");
        addGui("subtitle.craft_waiting", "While Crafting");
        addGui("subtitle.harmful_effect", "Harmful Effect Handling");
        addGui("subtitle.spice_of_life", "Spice of Life: Carrot Edition");
        addGui("subtitle.uneaten_food", "Uneaten Food Handling");

        addPrimary("Avoid Waste First", "Prefer High Saturation");

        addSecondary("Ignore Food Effects", "Prefer Food Without Effects", "Prefer Food with Beneficial Effects");

        addCompoundEffect("Any Harmful Effect Counts as Harmful", "Offset Beneficial Against Harmful");

        addFullHunger("Stop Eating", "Eat Below 15 Saturation", "Eat Below 10 Saturation", "Eat Below 5 Saturation");

        addCraftTiming("Craft Only Without Stock", "Craftables Join Comparison (Costly)");

        addCraftWaiting("Keep Eating Other Food", "Wait for Crafting");

        addHarmfulEffect("Keep Harmful Effects", "Exclude Certainly Harmful", "Exclude All Harmful");

        addUneaten("Leave Alone", "Prefer Untried Food", "Prefer Untried Food, Even if Harmful");

        addConfig("title", "Feeding Card");
        addConfig("section.ae2feedingcard.server.toml", "Server Settings");
        addConfig("section.ae2feedingcard.server.toml.title", "AE2 Feeding Card Server Configuration");
        addConfig("baseCooldownTicks", "Base Cooldown", "Ticks between two feeding attempts without any speed card");
        addConfig("cooldownFactorPerSpeedCard", "Cooldown Factor per Speed Card", "Cooldown multiplier applied by each speed card installed in the feeding card, the result is rounded down");
        addConfig("minCooldownTicks", "Minimum Cooldown", "Lower bound of the cooldown, speed cards cannot go below it");
        addConfig("ignoreItemCooldowns", "Ignore Item Cooldowns", "Allow feeding to ignore item cooldowns, so foods that set one are eaten again before it expires");
        addConfig("saturationGapRatio", "Saturation Gap Ratio", "Waste tolerance while topping up saturation, a food is only eaten when the saturation gap is at least its own saturation multiplied by this ratio");
        addConfig("dangerousFoodEffects", "Certainly Harmful Effects", "Effects treated as certainly harmful when the harmful effect handling of an inverter card is set to exclude certain harmful effects, one entry per list item");
        addConfig("craftScanPeriodTicks", "Craft Scan Period", "Ticks between two scans of the craftable items, only used while a crafting card is installed; independent of the feeding cooldown");
        addConfig("craftSettleCooldownTicks", "Craft Settle Cooldown", "Ticks to wait after a crafting job settles before another one may be submitted, only used while a crafting card is installed; it is only set during a feeding check, so the effective wait is at least the feeding cooldown");
        addConfig("craftRetryCooldownTicks", "Craft Retry Cooldown", "Ticks before another crafting plan is attempted after a plan failed or could not be submitted");
    }

    public void Chinese() {
        addItem(AE2FeedingCardItems.FEEDING_CARD, "喂食卡");
        add(AE2FeedingCardCreativeModeTabs.AE2FEEDINGCARD_TAB_TRANSLATABLE, "AE2喂食卡");

        addGui("feeding_card", "喂食卡");
        addGui("feeding_card_settings", "喂食卡设置");
        addGui("feeding_card_toggle", "喂食卡开关");
        addGui("feeding_card_toggle.state_off", "关闭");
        addGui("feeding_card_toggle.state_on", "开启");
        addGui("back", "返回");
        addGui("remainder_to_network", "食物容器存入ME网络");
        addGui("remainder_to_inventory", "食物容器放入物品栏");
        addGui("void_overflow", "无法存储时销毁食物容器");
        addGui("play_eat_sound", "进食时播放音效");

        addGui("group.feeding", "基础设置");
        addGui("group.void_overflow_card", "溢出销毁卡设置");
        addGui("group.crafting_card", "合成卡设置");
        addGui("group.inverter_card", "反向卡设置");
        addGui("group.mod_compat", "模组兼容");

        addGui("subtitle.primary_strategy", "主进食策略");
        addGui("subtitle.secondary_strategy", "副进食策略");
        addGui("subtitle.compound_effect", "复合效果处理");
        addGui("subtitle.full_hunger_behavior", "饥饿值满时的行为");
        addGui("subtitle.craft_timing", "合成时机");
        addGui("subtitle.craft_waiting", "合成期间");
        addGui("subtitle.harmful_effect", "有害效果处理");
        addGui("subtitle.spice_of_life", "生活调味料：胡萝卜版");
        addGui("subtitle.uneaten_food", "未吃过食物的处理");

        addPrimary("优先不浪费", "优先选高饱和度的");

        addSecondary("不管食物效果", "优先无效果食物", "优先有益效果食物");

        addCompoundEffect("只要包含有害效果就算有害", "有益与有害按数量抵消");

        addFullHunger("停止进食", "饱和度低于15时进食", "饱和度低于10时进食", "饱和度低于5时进食");

        addCraftTiming("库存没有食物才合成", "可合成项参与比较（较耗性能）");

        addCraftWaiting("照常吃其他食物", "等待合成完成");

        addHarmfulEffect("不排除有害效果", "排除确定性有害效果", "排除所有有害效果");

        addUneaten("不干预", "优先吃没吃过的", "优先吃没吃过的，哪怕有害");

        addConfig("title", "喂食卡");
        addConfig("section.ae2feedingcard.server.toml", "服务端设置");
        addConfig("section.ae2feedingcard.server.toml.title", "AE2喂食卡服务端配置");
        addConfig("baseCooldownTicks", "基础冷却", "未装加速卡时，两次进食尝试之间的间隔tick");
        addConfig("cooldownFactorPerSpeedCard", "每张加速卡的冷却系数", "喂食卡每装一张加速卡，冷却乘以该系数，结果向下取整");
        addConfig("minCooldownTicks", "冷却下限", "冷却的下限，加速卡再多也不会低于它");
        addConfig("ignoreItemCooldowns", "无视物品冷却", "允许喂食无视物品冷却，使设置了冷却的食物在冷却结束前就能再次被食用");
        addConfig("saturationGapRatio", "饱和度容差比例", "补饱和度时允许的浪费比例，只有饱和度缺口达到食物自身饱和度乘以此比例时才会食用");
        addConfig("dangerousFoodEffects", "确定性有害效果", "反向卡的有害效果处理设为排除确定性有害时，被视为确定有害的效果，每个列表项一个效果id");
        addConfig("craftScanPeriodTicks", "合成扫描周期", "两次扫描可合成物品之间的间隔tick，仅在装了合成卡时使用，与进食冷却无关");
        addConfig("craftSettleCooldownTicks", "合成结算冷却", "合成任务完成结算后，再次提交前的间隔tick，仅在装了合成卡时使用；该冷却只在进食检查时设置，所以实际等待至少是一个进食冷却");
        addConfig("craftRetryCooldownTicks", "合成重试冷却", "规划失败或提交失败后，再次尝试规划前的冷却tick");
    }

    @Override
    protected void addTranslations() {
        switch (locale) {
            case EN_US -> English();
            case ZH_CN -> Chinese();
        }
    }
}
