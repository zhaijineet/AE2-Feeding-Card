package net.zhaiji.ae2feedingcard.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;

import java.util.function.Supplier;

public class AE2FeedingCardCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AE2FeedingCard.MOD_ID);

    public static final String AE2FEEDINGCARD_TAB_TRANSLATABLE = "creativetab.ae2feedingcard.ae2feedingcard_tab";

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AE2FEEDINGCARD_TAB = CREATIVE_MODE_TAB.register(
            "ae2feedingcard_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(AE2FeedingCardItems.FEEDING_CARD.get()))
                    .title(Component.translatable(AE2FEEDINGCARD_TAB_TRANSLATABLE))
                    .displayItems((itemDisplayParameters, output) -> {
                        AE2FeedingCardItems.ITEMS.getEntries()
                                                .stream()
                                                .map(Supplier::get)
                                                .forEach(output::accept);
                    })
                    .build()
    );
}
