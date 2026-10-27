package net.zhaiji.ae2feedingcard.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class AE2FeedingCardServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BASE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between two feeding attempts without any speed card")
            .defineInRange(
                    "baseCooldownTicks",
                    200,
                    16,
                    Integer.MAX_VALUE
            );

    /**
     * 每张加速卡把当前冷却乘以该系数，结果向下取整，所以系数越小越快；1.0表示加速卡不改变冷却。
     */
    public static final ModConfigSpec.DoubleValue COOLDOWN_FACTOR_PER_SPEED_CARD = BUILDER
            .comment("Cooldown multiplier applied by each speed card installed in the feeding card, the result is rounded down")
            .defineInRange(
                    "cooldownFactorPerSpeedCard",
                    0.5,
                    0.1,
                    1.0
            );

    /**
     * 下界1是除零的唯一防线：冷却参与取模，改成0会抛ArithmeticException。
     */
    public static final ModConfigSpec.IntValue MIN_COOLDOWN_TICKS = BUILDER
            .comment("Lower bound of the cooldown, speed cards cannot go below it")
            .defineInRange(
                    "minCooldownTicks",
                    10,
                    1,
                    40
            );

    public static final ModConfigSpec.BooleanValue IGNORE_ITEM_COOLDOWNS = BUILDER
            .comment("Allow feeding to ignore item cooldowns, so foods that set one are eaten again before it expires")
            .define(
                    "ignoreItemCooldowns",
                    false
            );

    public static final ModConfigSpec.DoubleValue SATURATION_GAP_RATIO = BUILDER
            .comment("Waste tolerance while topping up saturation, a food is only eaten when the saturation gap is at least its own saturation multiplied by this ratio")
            .defineInRange(
                    "saturationGapRatio",
                    0.5,
                    0.1,
                    1.0
            );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DANGEROUS_FOOD_EFFECTS = BUILDER
            .comment("Effects treated as certainly harmful when the harmful effect handling of an inverter card is set to exclude certain harmful effects, one entry per list item")
            .defineListAllowEmpty(
                    "dangerousFoodEffects",
                    List.of("minecraft:wither", "minecraft:poison", "minecraft:instant_damage"),
                    () -> "minecraft:wither",
                    element -> element instanceof String effectId && ResourceLocation.tryParse(effectId) != null
            );

    public static final ModConfigSpec.IntValue CRAFT_SCAN_PERIOD_TICKS = BUILDER
            .comment("Ticks between two scans of the craftable items, only used while a crafting card is installed; independent of the feeding cooldown")
            .defineInRange(
                    "craftScanPeriodTicks",
                    20,
                    20,
                    Integer.MAX_VALUE
            );

    public static final ModConfigSpec.IntValue CRAFT_SETTLE_COOLDOWN_TICKS = BUILDER
            .comment("Ticks to wait after a crafting job settles before another one may be submitted, only used while a crafting card is installed; it is only set during a feeding check, so the effective wait is at least the feeding cooldown")
            .defineInRange(
                    "craftSettleCooldownTicks",
                    1,
                    1,
                    20
            );

    public static final ModConfigSpec.IntValue CRAFT_RETRY_COOLDOWN_TICKS = BUILDER
            .comment("Ticks before another crafting plan is attempted after a plan failed or could not be submitted")
            .defineInRange(
                    "craftRetryCooldownTicks",
                    100,
                    20,
                    Integer.MAX_VALUE
            );

    public static final ModConfigSpec SPEC = BUILDER.build();
}
