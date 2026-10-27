package net.zhaiji.ae2feedingcard.client.event;

import net.neoforged.bus.api.IEventBus;

public class ClientEventManager {
    public static void init(IEventBus modBus, IEventBus gameBus) {
        modBusListener(modBus);
        gameBusListener(gameBus);
    }

    public static void modBusListener(IEventBus modBus) {
        modBus.addListener(ClientEventHandler::handlerRegisterMenuScreensEvent);
    }

    public static void gameBusListener(IEventBus gameBus) {
    }
}
