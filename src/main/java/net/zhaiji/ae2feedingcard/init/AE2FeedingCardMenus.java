package net.zhaiji.ae2feedingcard.init;

import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;
import net.zhaiji.ae2feedingcard.menu.FeedingCardMenu;
import net.zhaiji.ae2feedingcard.menu.FeedingCardSettingsMenu;

public class AE2FeedingCardMenus {
    public static final String TOGGLE_ENABLED_ACTION = "toggleFeedingCardEnabled";

    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(Registries.MENU, AE2FeedingCard.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FeedingCardMenu>> FEEDING_CARD = MENU_TYPE.register(
            "feeding_card",
            () -> MenuTypeBuilder.create(FeedingCardMenu::new, IPortableTerminal.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2FeedingCard.MOD_ID, "feeding_card"))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<FeedingCardSettingsMenu>> FEEDING_CARD_SETTINGS = MENU_TYPE.register(
            "feeding_card_settings",
            () -> MenuTypeBuilder.create(FeedingCardSettingsMenu::new, IPortableTerminal.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2FeedingCard.MOD_ID, "feeding_card_settings"))
    );
}
