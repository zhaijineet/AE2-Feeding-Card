package net.zhaiji.ae2feedingcard.init;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;
import net.zhaiji.ae2feedingcard.attachment.FeedingCardRuntimeData;

public class AE2FeedingCardAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AE2FeedingCard.MOD_ID);

    /**
     * 玩家的每 tick 运行状态，只在内存里存在，不序列化也不同步。
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FeedingCardRuntimeData>> FEEDING_CARD_RUNTIME_STATE = ATTACHMENT_TYPES.register(
            "feeding_card_runtime_state",
            () -> AttachmentType.builder(FeedingCardRuntimeData::new).build()
    );
}
