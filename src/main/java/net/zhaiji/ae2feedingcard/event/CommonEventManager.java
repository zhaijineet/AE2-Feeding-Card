package net.zhaiji.ae2feedingcard.event;

import net.neoforged.bus.api.IEventBus;
import net.zhaiji.ae2feedingcard.datagen.DataGenHandler;

public class CommonEventManager {
    public static void init(IEventBus modBus, IEventBus gameBus) {
        modBusListener(modBus);
        gameBusListener(gameBus);
    }

    public static void modBusListener(IEventBus modBus) {
        modBus.addListener(DataGenHandler::handlerGatherDataEvent);
        modBus.addListener(CommonEventHandler::handlerFMLCommonSetupEvent);
    }

    public static void gameBusListener(IEventBus gameBus) {
        gameBus.addListener(CommonEventHandler::handlerPlayerTickEvent$Post);
    }
}
