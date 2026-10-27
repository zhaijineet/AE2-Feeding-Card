package net.zhaiji.ae2feedingcard.util;

import appeng.menu.guisync.PacketWritable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CompoundEffectHandling;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CraftTiming;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CraftWaiting;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.FullHungerBehavior;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.HarmfulEffectHandling;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.Primary;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.Secondary;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.UneatenFoodHandling;

/**
 * 每张喂食卡独立的全部设置，字段顺序与设置界面的分组顺序一致。
 */
public record FeedingCardSettings(
        boolean remainderToNetwork,
        boolean remainderToInventory,
        boolean playEatSound,
        Primary primaryStrategy,
        Secondary secondaryStrategy,
        CompoundEffectHandling compoundEffectHandling,
        boolean voidOverflow,
        FullHungerBehavior fullHungerBehavior,
        CraftTiming craftTiming,
        CraftWaiting craftWaiting,
        HarmfulEffectHandling harmfulEffectHandling,
        UneatenFoodHandling uneatenFoodHandling
) implements PacketWritable {
    public static final FeedingCardSettings DEFAULT = new FeedingCardSettings(
            true,
            true,
            true,
            Primary.AVOID_WASTE,
            Secondary.PREFER_NO_EFFECT,
            CompoundEffectHandling.STRICT,
            true,
            FullHungerBehavior.EAT_BELOW_FIVE,
            CraftTiming.ONLY_WHEN_EMPTY,
            CraftWaiting.KEEP_EATING,
            HarmfulEffectHandling.EXCLUDE_DEFINITE,
            UneatenFoodHandling.PREFER_UNEATEN
    );

    private static final Codec<Primary> PRIMARY_CODEC = StringRepresentable.fromEnum(Primary::values);
    private static final Codec<Secondary> SECONDARY_CODEC = StringRepresentable.fromEnum(Secondary::values);
    private static final Codec<CompoundEffectHandling> COMPOUND_EFFECT_CODEC = StringRepresentable.fromEnum(CompoundEffectHandling::values);
    private static final Codec<FullHungerBehavior> FULL_HUNGER_CODEC = StringRepresentable.fromEnum(FullHungerBehavior::values);
    private static final Codec<CraftTiming> CRAFT_TIMING_CODEC = StringRepresentable.fromEnum(CraftTiming::values);
    private static final Codec<CraftWaiting> CRAFT_WAITING_CODEC = StringRepresentable.fromEnum(CraftWaiting::values);
    private static final Codec<HarmfulEffectHandling> HARMFUL_EFFECT_CODEC = StringRepresentable.fromEnum(HarmfulEffectHandling::values);
    private static final Codec<UneatenFoodHandling> UNEATEN_FOOD_CODEC = StringRepresentable.fromEnum(UneatenFoodHandling::values);

    /**
     * 12个字段全部必填：发版后要加字段必须先提供旧档迁移，否则旧卡缺字段就解码失败，整张卡的组件跟着失败，
     * 再经ItemContainerContents与UPGRADES逐层放大，最终整台无线终端退化成ItemStack.EMPTY而不是重置设置。
     *
     * <p>不要图省事换成optionalFieldOf：它的编码侧在值等于默认时会省略该字段，DEFAULT因此成了存档格式的一部分，
     * 以后改默认值会静默改变旧档的读法。</p>
     */
    public static final Codec<FeedingCardSettings> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.BOOL.fieldOf("remainder_to_network").forGetter(FeedingCardSettings::remainderToNetwork),
            Codec.BOOL.fieldOf("remainder_to_inventory").forGetter(FeedingCardSettings::remainderToInventory),
            Codec.BOOL.fieldOf("play_eat_sound").forGetter(FeedingCardSettings::playEatSound),
            PRIMARY_CODEC.fieldOf("primary_strategy").forGetter(FeedingCardSettings::primaryStrategy),
            SECONDARY_CODEC.fieldOf("secondary_strategy").forGetter(FeedingCardSettings::secondaryStrategy),
            COMPOUND_EFFECT_CODEC.fieldOf("compound_effect_handling").forGetter(FeedingCardSettings::compoundEffectHandling),
            Codec.BOOL.fieldOf("void_overflow").forGetter(FeedingCardSettings::voidOverflow),
            FULL_HUNGER_CODEC.fieldOf("full_hunger_behavior").forGetter(FeedingCardSettings::fullHungerBehavior),
            CRAFT_TIMING_CODEC.fieldOf("craft_timing").forGetter(FeedingCardSettings::craftTiming),
            CRAFT_WAITING_CODEC.fieldOf("craft_waiting").forGetter(FeedingCardSettings::craftWaiting),
            HARMFUL_EFFECT_CODEC.fieldOf("harmful_effect_handling").forGetter(FeedingCardSettings::harmfulEffectHandling),
            UNEATEN_FOOD_CODEC.fieldOf("uneaten_food_handling").forGetter(FeedingCardSettings::uneatenFoodHandling)
    ).apply(builder, FeedingCardSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FeedingCardSettings> STREAM_CODEC = StreamCodec.of(
            FeedingCardSettings::encode,
            FeedingCardSettings::new
    );

    /**
     * 读取顺序必须与 {@link #encode} 的写出顺序一致，AE2 的菜单同步也靠这个构造器还原字段。
     */
    public FeedingCardSettings(RegistryFriendlyByteBuf data) {
        this(
                data.readBoolean(),
                data.readBoolean(),
                data.readBoolean(),
                data.readEnum(Primary.class),
                data.readEnum(Secondary.class),
                data.readEnum(CompoundEffectHandling.class),
                data.readBoolean(),
                data.readEnum(FullHungerBehavior.class),
                data.readEnum(CraftTiming.class),
                data.readEnum(CraftWaiting.class),
                data.readEnum(HarmfulEffectHandling.class),
                data.readEnum(UneatenFoodHandling.class)
        );
    }

    private static void encode(RegistryFriendlyByteBuf data, FeedingCardSettings settings) {
        data.writeBoolean(settings.remainderToNetwork());
        data.writeBoolean(settings.remainderToInventory());
        data.writeBoolean(settings.playEatSound());
        data.writeEnum(settings.primaryStrategy());
        data.writeEnum(settings.secondaryStrategy());
        data.writeEnum(settings.compoundEffectHandling());
        data.writeBoolean(settings.voidOverflow());
        data.writeEnum(settings.fullHungerBehavior());
        data.writeEnum(settings.craftTiming());
        data.writeEnum(settings.craftWaiting());
        data.writeEnum(settings.harmfulEffectHandling());
        data.writeEnum(settings.uneatenFoodHandling());
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        encode(data, this);
    }
}
