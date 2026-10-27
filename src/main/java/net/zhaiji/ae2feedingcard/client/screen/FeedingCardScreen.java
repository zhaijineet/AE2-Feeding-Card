package net.zhaiji.ae2feedingcard.client.screen;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.menu.SlotSemantics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardItems;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardMenus;
import net.zhaiji.ae2feedingcard.menu.FeedingCardMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * 喂食卡界面：白名单槽位、卡片升级槽，以及左栏的设置按钮与红石卡开关按钮。
 */
public class FeedingCardScreen extends AEBaseScreen<FeedingCardMenu> {
    private final ToggleButton toggleButton;

    public FeedingCardScreen(FeedingCardMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));
        AESubScreen.addBackButton(menu, "back", this.widgets, Component.translatable("gui.ae2feedingcard.back"));

        this.addToLeftToolbar(new FeedingCardSettingsButton(button -> PacketDistributor.sendToServer(
                SwitchGuisPacket.openSubMenu(AE2FeedingCardMenus.FEEDING_CARD_SETTINGS.get()))));

        this.toggleButton = this.addToLeftToolbar(new ToggleButton(
                Icon.REDSTONE_ON, Icon.INVALID, act -> menu.toggleEnabled()));
        this.toggleButton.setMessage(Component.translatable("gui.ae2feedingcard.feeding_card_toggle"));
        this.toggleButton.setTooltipOn(List.of(
                Component.translatable("gui.ae2feedingcard.feeding_card_toggle"),
                Component.translatable("gui.ae2feedingcard.feeding_card_toggle.state_on")));
        this.toggleButton.setTooltipOff(List.of(
                Component.translatable("gui.ae2feedingcard.feeding_card_toggle"),
                Component.translatable("gui.ae2feedingcard.feeding_card_toggle.state_off")));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.toggleButton.setState(this.getMenu().isEnabled());
        this.toggleButton.setVisibility(this.getMenu().isRedstoneCardInstalled());
    }

    private List<Component> getCompatibleUpgrades() {
        List<Component> lines = new ArrayList<>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.addAll(Upgrades.getTooltipLinesForMachine(AE2FeedingCardItems.FEEDING_CARD.get()));
        return lines;
    }
}
