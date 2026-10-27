package net.zhaiji.ae2feedingcard.util;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 喂食卡各档策略的枚举集合，序列化名一律取枚举名的小写形式，界面翻译键也复用它。
 *
 * <p>序列化名同时是存档格式：重命名任何枚举常量都会让旧卡反序列化失败，
 * {@link FeedingCardSettings} 的 12 个字段同理——失败的表现不是读出 null，而是整张喂食卡加载失败。</p>
 */
public class FeedingStrategy {
    private interface LowercaseName extends StringRepresentable {
        @Override
        default String getSerializedName() {
            return ((Enum<?>) this).name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Primary implements LowercaseName {
        AVOID_WASTE,
        HIGH_SATURATION
    }

    public enum Secondary implements LowercaseName {
        IGNORE,
        PREFER_NO_EFFECT,
        PREFER_BENEFICIAL
    }

    public enum CompoundEffectHandling implements LowercaseName {
        STRICT,
        OFFSET
    }

    public enum FullHungerBehavior implements LowercaseName {
        STOP(0),
        EAT_BELOW_FIFTEEN(15),
        EAT_BELOW_TEN(10),
        EAT_BELOW_FIVE(5);

        private final float saturationThreshold;

        FullHungerBehavior(float saturationThreshold) {
            this.saturationThreshold = saturationThreshold;
        }

        public boolean stopsFeeding() {
            return this == STOP;
        }

        public float getSaturationThreshold() {
            return this.saturationThreshold;
        }
    }

    public enum CraftTiming implements LowercaseName {
        ONLY_WHEN_EMPTY,
        PREFER_BETTER
    }

    public enum CraftWaiting implements LowercaseName {
        KEEP_EATING,
        WAIT_FOR_COMPLETION
    }

    public enum HarmfulEffectHandling implements LowercaseName {
        KEEP,
        EXCLUDE_DEFINITE,
        EXCLUDE_ALL
    }

    public enum UneatenFoodHandling implements LowercaseName {
        IGNORE,
        PREFER_UNEATEN,
        PREFER_UNEATEN_INCLUDING_HARMFUL
    }
}
