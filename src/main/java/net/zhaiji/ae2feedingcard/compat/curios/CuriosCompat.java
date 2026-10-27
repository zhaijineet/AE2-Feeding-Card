package net.zhaiji.ae2feedingcard.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompat {
    /**
     * 收集玩家所有 Curios 槽位里的非空物品，供喂食卡在自己的背包扫描之外再找一遍。
     */
    public static List<ItemStack> getEquippedStacks(Player player) {
        ICuriosItemHandler curiosItemHandler = CuriosApi.getCuriosInventory(player).orElse(null);
        if (curiosItemHandler == null) return List.of();

        IItemHandlerModifiable equippedCurios = curiosItemHandler.getEquippedCurios();
        List<ItemStack> equippedStacks = new ArrayList<>(equippedCurios.getSlots());
        for (int slot = 0; slot < equippedCurios.getSlots(); slot++) {
            ItemStack equippedStack = equippedCurios.getStackInSlot(slot);
            if (!equippedStack.isEmpty()) equippedStacks.add(equippedStack);
        }

        return equippedStacks;
    }
}
