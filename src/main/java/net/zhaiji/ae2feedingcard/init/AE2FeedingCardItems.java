package net.zhaiji.ae2feedingcard.init;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;
import net.zhaiji.ae2feedingcard.item.FeedingCardItem;

public class AE2FeedingCardItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2FeedingCard.MOD_ID);

    public static final DeferredItem<Item> FEEDING_CARD = ITEMS.registerItem(
            "feeding_card",
            FeedingCardItem::new
    );
}
