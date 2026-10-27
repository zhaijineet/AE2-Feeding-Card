package net.zhaiji.ae2feedingcard;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.zhaiji.ae2feedingcard.client.event.ClientEventManager;

@Mod(value = AE2FeedingCard.MOD_ID, dist = Dist.CLIENT)
public class AE2FeedingCardClient {
    public AE2FeedingCardClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        ClientEventManager.init(modEventBus, NeoForge.EVENT_BUS);
    }
}
