package net.zhaiji.ae2feedingcard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.zhaiji.ae2feedingcard.config.AE2FeedingCardServerConfig;
import net.zhaiji.ae2feedingcard.event.CommonEventManager;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardAttachmentTypes;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardCreativeModeTabs;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardDataComponents;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardItems;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardMenus;

@Mod(AE2FeedingCard.MOD_ID)
public class AE2FeedingCard {
    public static final String MOD_ID = "ae2feedingcard";

    public AE2FeedingCard(IEventBus modEventBus, ModContainer modContainer) {
        AE2FeedingCardItems.ITEMS.register(modEventBus);
        AE2FeedingCardCreativeModeTabs.CREATIVE_MODE_TAB.register(modEventBus);
        AE2FeedingCardDataComponents.COMPONENT_TYPES.register(modEventBus);
        AE2FeedingCardMenus.MENU_TYPE.register(modEventBus);
        AE2FeedingCardAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, AE2FeedingCardServerConfig.SPEC);
        CommonEventManager.init(modEventBus, NeoForge.EVENT_BUS);
    }
}
