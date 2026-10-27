package net.zhaiji.ae2feedingcard.util;

import appeng.menu.locator.ItemMenuHostLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 按物品栈直接定位终端宿主，用于不打开菜单就读终端的链路状态；AE2自带的同类定位器是包私有record，外部无法构造。
 */
public record FeedingCardTerminalLocator(ItemStack stack) implements ItemMenuHostLocator {
    @Override
    public ItemStack locateItem(Player player) {
        return this.stack;
    }

    @Override
    @Nullable
    public BlockHitResult hitResult() {
        return null;
    }
}
