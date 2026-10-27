package net.zhaiji.ae2feedingcard.init;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class AE2FeedingCardUpgrades {
    public static void registerAll() {
        registerFeedingCardUpgrades();
        registerToWirelessTerminals();
    }

    /**
     * 声明喂食卡自身能装入的升级卡与各卡上限。
     */
    private static void registerFeedingCardUpgrades() {
        Item feedingCard = AE2FeedingCardItems.FEEDING_CARD.get();
        Upgrades.add(AEItems.CAPACITY_CARD, feedingCard, 3);
        Upgrades.add(AEItems.VOID_CARD, feedingCard, 1);
        Upgrades.add(AEItems.INVERTER_CARD, feedingCard, 1);
        Upgrades.add(AEItems.FUZZY_CARD, feedingCard, 1);
        Upgrades.add(AEItems.SPEED_CARD, feedingCard, 4);
        Upgrades.add(AEItems.EQUAL_DISTRIBUTION_CARD, feedingCard, 1);
        Upgrades.add(AEItems.CRAFTING_CARD, feedingCard, 1);
        Upgrades.add(AEItems.REDSTONE_CARD, feedingCard, 1);
    }

    /**
     * 把喂食卡挂到每个无线终端上，供其在终端内安装。
     */
    private static void registerToWirelessTerminals() {
        Item feedingCard = AE2FeedingCardItems.FEEDING_CARD.get();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof WirelessTerminalItem) {
                Upgrades.add(feedingCard, item, 1, GuiText.WirelessTerminals.getTranslationKey());
            }
        }
    }
}
