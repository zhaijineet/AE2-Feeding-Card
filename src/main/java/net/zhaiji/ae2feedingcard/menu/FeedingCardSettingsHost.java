package net.zhaiji.ae2feedingcard.menu;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * 喂食卡设置界面的上级宿主，返回时按构造时给定的父菜单类型与子菜单的 locator 重开父界面。
 */
public class FeedingCardSettingsHost implements ISubMenuHost {
    private final ItemStack mainMenuIcon;
    private final MenuType<?> parentMenuType;

    public FeedingCardSettingsHost(ItemStack mainMenuIcon, MenuType<?> parentMenuType) {
        this.mainMenuIcon = mainMenuIcon;
        this.parentMenuType = parentMenuType;
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(this.parentMenuType, player, subMenu.getLocator());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return this.mainMenuIcon;
    }
}
