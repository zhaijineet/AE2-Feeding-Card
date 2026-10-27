package net.zhaiji.ae2feedingcard.client.event;

import appeng.client.gui.style.StyleManager;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.zhaiji.ae2feedingcard.client.screen.FeedingCardScreen;
import net.zhaiji.ae2feedingcard.client.screen.FeedingCardSettingsScreen;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardMenus;
import net.zhaiji.ae2feedingcard.menu.FeedingCardMenu;
import net.zhaiji.ae2feedingcard.menu.FeedingCardSettingsMenu;

public class ClientEventHandler {
    public static void handlerRegisterMenuScreensEvent(RegisterMenuScreensEvent event) {
        event.<FeedingCardMenu, FeedingCardScreen>register(
                AE2FeedingCardMenus.FEEDING_CARD.get(),
                (menu, playerInventory, title) -> new FeedingCardScreen(
                        menu,
                        playerInventory,
                        title,
                        StyleManager.loadStyleDoc("/screens/ae2feedingcard/feeding_card.json")
                )
        );

        event.<FeedingCardSettingsMenu, FeedingCardSettingsScreen>register(
                AE2FeedingCardMenus.FEEDING_CARD_SETTINGS.get(),
                (menu, playerInventory, title) -> new FeedingCardSettingsScreen(
                        menu,
                        playerInventory,
                        title,
                        StyleManager.loadStyleDoc("/screens/ae2feedingcard/feeding_card_settings.json")
                )
        );
    }
}
