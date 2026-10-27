package net.zhaiji.ae2feedingcard.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.zhaiji.ae2feedingcard.client.screen.FeedingCardButton;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardItems;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardMenus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageScreen.class)
public abstract class MEStorageScreenMixin extends AEBaseScreen<MEStorageMenu> {
    @Unique
    private FeedingCardButton ae2feedingcard$settingsButton;

    public MEStorageScreenMixin(MEStorageMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(
            method = "<init>(Lappeng/menu/me/common/MEStorageMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;Lappeng/client/gui/style/ScreenStyle;)V",
            at = @At(
                    value = "RETURN"
            )
    )
    public void ae2feedingcard$init(MEStorageMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style, CallbackInfo ci) {
        this.ae2feedingcard$settingsButton = this.addToLeftToolbar(new FeedingCardButton(button ->
                PacketDistributor.sendToServer(SwitchGuisPacket.openSubMenu(AE2FeedingCardMenus.FEEDING_CARD.get()))));
    }

    @Inject(
            method = "updateBeforeRender",
            at = @At(
                    value = "RETURN"
            )
    )
    public void ae2feedingcard$updateBeforeRender(CallbackInfo ci) {
        ITerminalHost host = this.getMenu().getHost();
        this.ae2feedingcard$settingsButton.setVisibility(
                host.getInstalledUpgrades(AE2FeedingCardItems.FEEDING_CARD.get()) > 0);
    }
}
