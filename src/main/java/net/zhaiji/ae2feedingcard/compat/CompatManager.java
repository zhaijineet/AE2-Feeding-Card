package net.zhaiji.ae2feedingcard.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.zhaiji.ae2feedingcard.compat.curios.CuriosCompat;
import net.zhaiji.ae2feedingcard.compat.solcarrot.SpiceOfLifeCompat;

import java.util.List;

public class CompatManager {
    public static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");
    public static final boolean SOL_CARROT_LOADED = ModList.get().isLoaded("solcarrot");

    /**
     * 收集玩家饰品槽里的非空物品，未装 Curios 时返回空表。
     */
    public static List<ItemStack> getEquippedStacks(Player player) {
        if (!CURIOS_LOADED) return List.of();
        return CuriosCompat.getEquippedStacks(player);
    }

    /**
     * 玩家是否吃过该食物；未装 Spice of Life 时调用方已经把它降级成不干预，不会走到这里。
     */
    public static boolean hasEaten(Player player, Item item) {
        return SpiceOfLifeCompat.hasEaten(player, item);
    }
}
