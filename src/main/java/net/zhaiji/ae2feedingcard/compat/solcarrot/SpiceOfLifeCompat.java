package net.zhaiji.ae2feedingcard.compat.solcarrot;

import com.cazsius.solcarrot.api.SOLCarrotAPI;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Spice of Life: Carrot Edition 的食用记录查询，只在模组存在时被加载。
 */
public class SpiceOfLifeCompat {
    public static boolean hasEaten(Player player, Item item) {
        return SOLCarrotAPI.getFoodCapability(player).hasEaten(item);
    }
}
