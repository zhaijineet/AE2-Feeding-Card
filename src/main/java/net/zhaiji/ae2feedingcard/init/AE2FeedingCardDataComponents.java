package net.zhaiji.ae2feedingcard.init;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;
import net.zhaiji.ae2feedingcard.util.FeedingCardSettings;

import java.util.List;

public class AE2FeedingCardDataComponents {
    public static final DeferredRegister.DataComponents COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AE2FeedingCard.MOD_ID);

    /**
     * 食物白名单，空槽以null占位。
     *
     * <p>用{@link GenericStack#FAULT_TOLERANT_NULLABLE_LIST_CODEC}，它与游标、在途目标所用的
     * {@link GenericStack#FAULT_TOLERANT_LIST_CODEC}的差别是允许null元素：外部模组的物品被卸载后，
     * 该元素降级成missing-content占位物。空槽与占位物在tick侧都按无值处理。</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GenericStack>>> FEEDING_FILTER = COMPONENT_TYPES.registerComponentType(
            "feeding_filter",
            builder -> builder
                    .persistent(GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC)
                    .networkSynchronized(GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
    );

    /**
     * 喂食卡的全套设置，缺省时按 {@link FeedingCardSettings#DEFAULT} 处理。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FeedingCardSettings>> FEEDING_SETTINGS = COMPONENT_TYPES.registerComponentType(
            "feeding_settings",
            builder -> builder
                    .persistent(FeedingCardSettings.CODEC)
                    .networkSynchronized(FeedingCardSettings.STREAM_CODEC)
    );

    /**
     * 均分卡的轮转游标，写入侧只放一个元素、读取侧取第一个，空列表表示无值。
     *
     * <p>用{@link GenericStack#FAULT_TOLERANT_LIST_CODEC}，持久化侧不会出现null元素（NBT表达不了），
     * 外部模组的物品被卸载后该元素降级成missing-content占位物，按无游标处理；
     * 网络侧走原始stream codec，解出的列表可含null，由读取方自守。</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GenericStack>>> FEEDING_CURSOR = COMPONENT_TYPES.registerComponentType(
            "feeding_cursor",
            builder -> builder
                    .persistent(GenericStack.FAULT_TOLERANT_LIST_CODEC)
                    .networkSynchronized(GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
    );

    /**
     * 喂食卡总开关，缺省视为开启，只在装了红石卡时才真正起作用。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FEEDING_CARD_ENABLED = COMPONENT_TYPES.registerComponentType(
            "feeding_card_enabled",
            builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
    );

    /**
     * 已提交但还没到货的合成请求目标，写入侧只放一个元素、读取侧取第一个，空列表表示无值，
     * 靠CPU上的任务状态判断是否仍在途。
     *
     * <p>与游标同样用{@link GenericStack#FAULT_TOLERANT_LIST_CODEC}：持久化侧不会出现null元素（NBT表达不了），
     * 单个元素解析失败只降级成missing-content占位物，按无在途合成处理；
     * 网络侧走原始stream codec，解出的列表可含null，由读取方自守。</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GenericStack>>> FEEDING_PENDING_CRAFT = COMPONENT_TYPES.registerComponentType(
            "feeding_pending_craft",
            builder -> builder
                    .persistent(GenericStack.FAULT_TOLERANT_LIST_CODEC)
                    .networkSynchronized(GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
    );
}
