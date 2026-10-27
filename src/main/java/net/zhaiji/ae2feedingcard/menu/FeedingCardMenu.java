package net.zhaiji.ae2feedingcard.menu;

import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.IOptionalSlotHost;
import appeng.menu.slot.OptionalFakeSlot;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardMenus;
import net.zhaiji.ae2feedingcard.item.FeedingCardItem;
import net.zhaiji.ae2feedingcard.util.FeedingCardAccess;

/**
 * 喂食卡界面：白名单槽位与卡片自身的升级槽，进食策略等设置都在喂食卡设置界面里。
 */
public class FeedingCardMenu extends AEBaseMenu implements ISubMenu, IOptionalSlotHost {
    private final IPortableTerminal host;
    private final FeedingCardAccess access;
    private final ConfigInventory filterConfig;

    @GuiSync(0)
    public boolean enabled = true;

    public FeedingCardMenu(MenuType<?> menuType, int id, Inventory playerInventory, IPortableTerminal host) {
        super(menuType, id, playerInventory, host);
        this.host = host;
        this.access = FeedingCardAccess.fromTerminalHost(host);

        this.filterConfig = ConfigInventory.configTypes(FeedingCardItem.FILTER_SLOT_COUNT)
                .slotFilter(FeedingCardMenu::isFoodKey)
                .changeListener(this::updateFilterComponent)
                .build();

        if (this.access != null) {
            this.filterConfig.beginBatch();
            this.access.readFilter(this.filterConfig);
            this.filterConfig.endBatchSuppressed();
            setupUpgrades(this.access.getCardUpgrades());
            this.enabled = this.access.readEnabled();
        }

        addFilterSlots();
        createPlayerInventorySlots(playerInventory);
        registerClientAction(AE2FeedingCardMenus.TOGGLE_ENABLED_ACTION, this::toggleEnabled);
    }

    /**
     * 白名单槽只判断「允许吃」，用组件的原始值即可，这里也没有玩家；
     * 卡片侧的收益测量另按玩家实际会吃到的值算，两侧口径不同是有意的。
     */
    private static boolean isFoodKey(AEKey key) {
        return key instanceof AEItemKey itemKey && itemKey.getReadOnlyStack().has(DataComponents.FOOD);
    }

    private void addFilterSlots() {
        ConfigMenuInventory filterMenuInventory = this.filterConfig.createMenuWrapper();

        // 基础位数之外的槽位按容量卡分组，组号从1开始，与isSlotEnabled的判据一致
        for (int index = 0; index < FeedingCardItem.FILTER_SLOT_COUNT; index++) {
            if (index < FeedingCardItem.BASE_FILTER_SLOT_COUNT) {
                this.addSlot(new FakeSlot(filterMenuInventory, index), SlotSemantics.CONFIG);
            } else {
                int group = (index - FeedingCardItem.BASE_FILTER_SLOT_COUNT) / FeedingCardItem.FILTER_SLOTS_PER_CAPACITY_CARD + 1;
                this.addSlot(new OptionalFakeSlot(filterMenuInventory, this, index, group), SlotSemantics.CONFIG);
            }
        }
    }

    /**
     * 组件只由服务端写，客户端这一侧只镜像。
     */
    private void updateFilterComponent() {
        if (isClientSide()) return;
        if (this.access == null) return;
        this.access.writeFilter(this.filterConfig);
    }

    public int getInstalledCapacityCards() {
        return this.access == null ? 0 : this.access.getInstalledCapacityCards();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isRedstoneCardInstalled() {
        return this.access != null && this.access.isRedstoneCardInstalled();
    }

    /**
     * 服务端以卡片组件为准；客户端只发动作，按钮图标要等一个tick的回包才更新。
     */
    public void toggleEnabled() {
        if (!isRedstoneCardInstalled()) return;

        if (isClientSide()) {
            sendClientAction(AE2FeedingCardMenus.TOGGLE_ENABLED_ACTION);
            return;
        }

        if (this.access == null) return;

        this.access.writeEnabled(!this.access.readEnabled());
    }

    /**
     * 服务端每tick从卡片重读同步字段；卡片不在时保持旧值。
     *
     * <p>必须在{@code super.broadcastChanges()}之前刷新：GuiSync的脏检查在父类里做，
     * 顺序反了会让新值永远晚一tick才发出去。</p>
     */
    @Override
    public void broadcastChanges() {
        if (isServerSide() && this.access != null) {
            ItemStack feedingCardStack = this.access.getFeedingCardStack();
            if (feedingCardStack != null) {
                this.enabled = FeedingCardAccess.readEnabled(feedingCardStack);
            }
        }

        super.broadcastChanges();
    }

    @Override
    public boolean isSlotEnabled(int group) {
        return getInstalledCapacityCards() >= group;
    }

    @Override
    public ISubMenuHost getHost() {
        return this.host;
    }
}
