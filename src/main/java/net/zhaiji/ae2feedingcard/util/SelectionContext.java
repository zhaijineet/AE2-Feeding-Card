package net.zhaiji.ae2feedingcard.util;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.zhaiji.ae2feedingcard.attachment.FeedingCardRuntimeData;
import net.zhaiji.ae2feedingcard.attachment.TerminalState;
import net.zhaiji.ae2feedingcard.item.FeedingCardItem;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.UneatenFoodHandling;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

record SelectionContext(
        ServerPlayer player,
        IGrid grid,
        ItemStack carrierStack,
        IUpgradeInventory terminalUpgrades,
        int feedingCardSlot,
        FeedingCardAccess access,
        FeedingCardRuntimeData runtimeData,
        TerminalState terminalState,
        /**
         * 本轮决策里已确认没吃过的键，收集候选时顺带写入，优先未尝试分组只查它，不再回读物品。
         */
        Set<AEItemKey> uneatenKeys,
        Set<ResourceLocation> dangerousEffectIds,
        UneatenFoodHandling uneatenFoodHandling
) {
    /**
     * 组件写入必须基于终端里的当前副本，否则后一次写入会盖掉前一次刚写的组件。
     *
     * <p>槽位里已经不是喂食卡时返回{@code null}，调用方按无值处理。改动前这里会退回tick开始时的旧副本，
     * 而槽位此时可能已经装了别的东西，{@code setItemDirect}会把旧副本写回去、覆盖玩家的物品。</p>
     */
    @Nullable
    ItemStack cardStack() {
        ItemStack currentStack = this.terminalUpgrades.getStackInSlot(this.feedingCardSlot);
        return currentStack.getItem() instanceof FeedingCardItem ? currentStack : null;
    }
}
