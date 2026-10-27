package net.zhaiji.ae2feedingcard.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardUpgrades;
import net.zhaiji.ae2feedingcard.util.FeedingCardUtil;

public class CommonEventHandler {
    public static void handlerFMLCommonSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(AE2FeedingCardUpgrades::registerAll);
    }

    public static void handlerPlayerTickEvent$Post(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        FeedingCardUtil.tick(serverPlayer);
    }
}
