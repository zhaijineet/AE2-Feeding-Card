package net.zhaiji.ae2feedingcard.util;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardDataComponents;
import net.zhaiji.ae2feedingcard.item.FeedingCardItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 从终端栈解析出喂食卡，并统一读写它自己的升级库存、白名单与整套设置。
 *
 * <p>终端升级、卡片升级、设置与白名单四份缓存，键都是「栈引用 + 该栈的UPGRADES组件实例」。
 * 第二维不是多余的：tick侧与这里各持一份独立的升级库存对象（tick的只写，这里的只读），
 * 而AE2的{@code ItemUpgradeInventory.saveChangedInventory}是{@code stack.set(...)}，原地改被捕获的
 * 那份栈，所以别处写过之后终端栈的引用可以不变而组件已经换了实例，这里就得靠第二维才知道重读。</p>
 *
 * <p>四份里只有终端升级直接依赖上面那条；卡片升级、设置与白名单的第二维是连带失效的——
 * 终端升级重建时{@code ItemContainerContents.copyInto}用{@code itemstack.copy()}造出新的卡片栈实例，
 * 它们才跟着失效。</p>
 */
public class FeedingCardAccess {
    private final Supplier<ItemStack> terminalStackSupplier;
    private final int feedingCardSlot;

    private ItemStack cachedTerminalStack;
    private ItemContainerContents cachedUpgradesContents;
    private IUpgradeInventory cachedTerminalUpgrades;

    private ItemStack cachedFeedingCardStack;
    private ItemContainerContents cachedCardUpgradesContents;
    private IUpgradeInventory cachedCardUpgrades;

    private ItemStack cachedSettingsCardStack;
    private ItemContainerContents cachedSettingsUpgradesContents;
    private FeedingCardSettings cachedSettings;

    private ItemStack cachedWhitelistCardStack;
    private ItemContainerContents cachedWhitelistUpgradesContents;
    private List<AEItemKey> cachedWhitelist;

    private FeedingCardAccess(Supplier<ItemStack> terminalStackSupplier, int feedingCardSlot) {
        this.terminalStackSupplier = terminalStackSupplier;
        this.feedingCardSlot = feedingCardSlot;
    }

    @Nullable
    public static FeedingCardAccess fromTerminalHost(IPortableTerminal host) {
        if (!(host instanceof ItemMenuHost<?> itemMenuHost)) return null;
        return fromTerminalStack(itemMenuHost::getItemStack);
    }

    @Nullable
    public static FeedingCardAccess fromTerminalStack(Supplier<ItemStack> terminalStackSupplier) {
        IUpgradeInventory terminalUpgrades = terminalUpgradesOf(terminalStackSupplier.get());
        for (int slot = 0; slot < terminalUpgrades.size(); slot++) {
            if (terminalUpgrades.getStackInSlot(slot).getItem() instanceof FeedingCardItem) {
                return new FeedingCardAccess(terminalStackSupplier, slot);
            }
        }

        return null;
    }

    /**
     * 调用方已经定位过槽位时用它，省掉一次重复遍历。
     */
    public static FeedingCardAccess fromTerminalStack(Supplier<ItemStack> terminalStackSupplier, int feedingCardSlot) {
        return new FeedingCardAccess(terminalStackSupplier, feedingCardSlot);
    }

    /**
     * 终端升级库存，按终端栈与它的UPGRADES组件实例缓存。
     *
     * <p>只看栈引用会漏掉「引用不变、组件换新实例」这一种：{@code saveChangedInventory}是原地
     * {@code stack.set(...)}，不换栈实例。</p>
     */
    private IUpgradeInventory terminalUpgrades() {
        ItemStack terminalStack = this.terminalStackSupplier.get();
        if (!(terminalStack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            this.cachedTerminalStack = null;
            this.cachedUpgradesContents = null;
            this.cachedTerminalUpgrades = UpgradeInventories.empty();
            this.cachedFeedingCardStack = null;
            this.cachedCardUpgradesContents = null;
            this.cachedCardUpgrades = null;
            return this.cachedTerminalUpgrades;
        }

        ItemContainerContents upgradesContents = terminalStack.getOrDefault(AEComponents.UPGRADES,
                ItemContainerContents.EMPTY);
        if (terminalStack != this.cachedTerminalStack || upgradesContents != this.cachedUpgradesContents) {
            this.cachedTerminalStack = terminalStack;
            this.cachedUpgradesContents = upgradesContents;
            this.cachedTerminalUpgrades = wirelessTerminalItem.getUpgrades(terminalStack);
        }

        return this.cachedTerminalUpgrades;
    }

    private static IUpgradeInventory terminalUpgradesOf(ItemStack terminalStack) {
        if (!(terminalStack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            return UpgradeInventories.empty();
        }

        return wirelessTerminalItem.getUpgrades(terminalStack);
    }

    @Nullable
    private ItemStack feedingCardStackOf(IUpgradeInventory terminalUpgrades) {
        if (this.feedingCardSlot >= terminalUpgrades.size()) return null;

        ItemStack feedingCardStack = terminalUpgrades.getStackInSlot(this.feedingCardSlot);
        return feedingCardStack.getItem() instanceof FeedingCardItem ? feedingCardStack : null;
    }

    /**
     * 每次可能返回不同实例，调用方只能读，不能依赖引用身份。
     */
    @Nullable
    public ItemStack getFeedingCardStack() {
        return feedingCardStackOf(terminalUpgrades());
    }

    /**
     * 每份卡片数据缓存的第二维都取自它，三处判据因此不可能不一致。
     */
    private static ItemContainerContents cardUpgradesContentsOf(ItemStack feedingCardStack) {
        return feedingCardStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
    }

    /**
     * 卡片自身的升级库存，按卡片栈与它的UPGRADES组件实例缓存。
     *
     * <p>两条重建来源：卡片被替换或玩家改插拔升级卡；以及终端UPGRADES被写入后（库存内部是副本，
     * 卡片栈随之变成新实例）。菜单的升级槽绑定的始终是首次建出的实例，写回靠{@code onCardUpgradesChanged}重读。</p>
     */
    public IUpgradeInventory getCardUpgrades() {
        ItemStack feedingCardStack = getFeedingCardStack();
        if (feedingCardStack == null) {
            this.cachedFeedingCardStack = null;
            this.cachedCardUpgradesContents = null;
            this.cachedCardUpgrades = null;
            return UpgradeInventories.empty();
        }

        ItemContainerContents cardUpgradesContents = cardUpgradesContentsOf(feedingCardStack);
        if (feedingCardStack != this.cachedFeedingCardStack
                || cardUpgradesContents != this.cachedCardUpgradesContents) {
            this.cachedFeedingCardStack = feedingCardStack;
            this.cachedCardUpgradesContents = cardUpgradesContents;
            this.cachedCardUpgrades = UpgradeInventories.forItem(feedingCardStack,
                    FeedingCardItem.UPGRADE_SLOT_COUNT, this::onCardUpgradesChanged);
        }

        return this.cachedCardUpgrades;
    }

    /**
     * 升级库存的回调发生在它自己把内容写回卡片组件之前，所以不能拿回调传入的实例当基准，要从槽位重新读。
     *
     * <p>{@code AppEngInternalInventory} 先调回调（{@code onChangeInventory}），之后才由
     * {@code saveChangedInventory} 把升级内容写进卡片栈；回调传入的快照缺此后写进去的开关、设置与白名单组件。</p>
     */
    private void onCardUpgradesChanged(ItemStack changedCardStack, IUpgradeInventory upgrades) {
        if (!(upgrades instanceof AppEngInternalInventory internalUpgrades)) return;

        IUpgradeInventory terminalUpgrades = terminalUpgrades();
        ItemStack currentCardStack = feedingCardStackOf(terminalUpgrades);
        if (currentCardStack == null) return;

        ItemStack updatedCardStack = currentCardStack.copy();
        updatedCardStack.set(AEComponents.UPGRADES, internalUpgrades.toItemContainerContents());
        terminalUpgrades.setItemDirect(this.feedingCardSlot, updatedCardStack);
    }

    public void readFilter(ConfigInventory filterConfig) {
        ItemStack feedingCardStack = getFeedingCardStack();
        List<GenericStack> filter = feedingCardStack == null
                ? List.of()
                : feedingCardStack.getOrDefault(AE2FeedingCardDataComponents.FEEDING_FILTER.get(), List.of());

        filterConfig.readFromList(filter);
    }

    public void writeFilter(ConfigInventory filterConfig) {
        IUpgradeInventory terminalUpgrades = terminalUpgrades();
        ItemStack feedingCardStack = feedingCardStackOf(terminalUpgrades);
        if (feedingCardStack == null) return;

        ItemStack updatedCardStack = feedingCardStack.copy();
        updatedCardStack.set(AE2FeedingCardDataComponents.FEEDING_FILTER.get(), filterConfig.toList());
        terminalUpgrades.setItemDirect(this.feedingCardSlot, updatedCardStack);
    }

    /**
     * 整套设置，按卡片栈与它的UPGRADES组件实例缓存；卡片缺失时返回默认值。
     */
    public FeedingCardSettings readSettings() {
        ItemStack feedingCardStack = getFeedingCardStack();
        if (feedingCardStack == null) {
            this.cachedSettingsCardStack = null;
            this.cachedSettingsUpgradesContents = null;
            this.cachedSettings = null;
            return FeedingCardSettings.DEFAULT;
        }

        ItemContainerContents cardUpgradesContents = cardUpgradesContentsOf(feedingCardStack);
        if (feedingCardStack != this.cachedSettingsCardStack
                || cardUpgradesContents != this.cachedSettingsUpgradesContents) {
            this.cachedSettingsCardStack = feedingCardStack;
            this.cachedSettingsUpgradesContents = cardUpgradesContents;
            this.cachedSettings = readSettings(feedingCardStack);
        }

        return this.cachedSettings;
    }

    /**
     * 白名单条目，按卡片栈与它的UPGRADES组件实例缓存，条目数按容量卡数截断。
     */
    public List<AEItemKey> readWhitelist() {
        ItemStack feedingCardStack = getFeedingCardStack();
        if (feedingCardStack == null) {
            this.cachedWhitelistCardStack = null;
            this.cachedWhitelistUpgradesContents = null;
            this.cachedWhitelist = null;
            return List.of();
        }

        ItemContainerContents cardUpgradesContents = cardUpgradesContentsOf(feedingCardStack);
        if (feedingCardStack != this.cachedWhitelistCardStack
                || cardUpgradesContents != this.cachedWhitelistUpgradesContents) {
            this.cachedWhitelistCardStack = feedingCardStack;
            this.cachedWhitelistUpgradesContents = cardUpgradesContents;
            this.cachedWhitelist = usableWhitelistOf(feedingCardStack);
        }

        return this.cachedWhitelist;
    }

    /**
     * 按容量卡数决定白名单可用条目数，超出的条目忽略。
     */
    private List<AEItemKey> usableWhitelistOf(ItemStack feedingCardStack) {
        List<GenericStack> filter = feedingCardStack.getOrDefault(
                AE2FeedingCardDataComponents.FEEDING_FILTER.get(),
                List.of()
        );

        int filterLimit = Math.min(FeedingCardItem.availableFilterSlots(getInstalledCapacityCards()), filter.size());

        List<AEItemKey> whitelist = new ArrayList<>(filterLimit);
        for (int index = 0; index < filterLimit; index++) {
            GenericStack entry = filter.get(index);
            // 空槽是null；解析失败的条目被容错codec换成missing-content占位物。两者都不是真实条目
            if (entry == null) continue;
            if (!(entry.what() instanceof AEItemKey itemKey)) continue;
            if (itemKey.is(AEItems.MISSING_CONTENT)) continue;
            whitelist.add(itemKey);
        }
        return whitelist;
    }

    public static FeedingCardSettings readSettings(ItemStack feedingCardStack) {
        return feedingCardStack.getOrDefault(AE2FeedingCardDataComponents.FEEDING_SETTINGS.get(),
                FeedingCardSettings.DEFAULT);
    }

    /**
     * 写整套设置，调用方负责只在服务端执行；与组件现值相同则不改动。
     */
    public void writeSettings(FeedingCardSettings settings) {
        IUpgradeInventory terminalUpgrades = terminalUpgrades();
        ItemStack feedingCardStack = feedingCardStackOf(terminalUpgrades);
        if (feedingCardStack == null) return;
        if (readSettings(feedingCardStack).equals(settings)) return;

        ItemStack updatedCardStack = feedingCardStack.copy();
        updatedCardStack.set(AE2FeedingCardDataComponents.FEEDING_SETTINGS.get(), settings);
        terminalUpgrades.setItemDirect(this.feedingCardSlot, updatedCardStack);
    }

    public int getInstalledCapacityCards() {
        return getCardUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD);
    }

    /**
     * 卡片缺失时视为开启；菜单的同步字段不走这里，见各自的{@code broadcastChanges}。
     */
    public boolean readEnabled() {
        ItemStack feedingCardStack = getFeedingCardStack();
        return feedingCardStack == null || readEnabled(feedingCardStack);
    }

    /**
     * 没有这个组件时视为开启，保证从没碰过开关的旧卡行为不变。
     */
    public static boolean readEnabled(ItemStack feedingCardStack) {
        return feedingCardStack.getOrDefault(AE2FeedingCardDataComponents.FEEDING_CARD_ENABLED.get(), true);
    }

    public void writeEnabled(boolean enabled) {
        IUpgradeInventory terminalUpgrades = terminalUpgrades();
        ItemStack feedingCardStack = feedingCardStackOf(terminalUpgrades);
        if (feedingCardStack == null) return;
        if (readEnabled(feedingCardStack) == enabled) return;

        ItemStack updatedCardStack = feedingCardStack.copy();
        updatedCardStack.set(AE2FeedingCardDataComponents.FEEDING_CARD_ENABLED.get(), enabled);
        terminalUpgrades.setItemDirect(this.feedingCardSlot, updatedCardStack);
    }

    public boolean isRedstoneCardInstalled() {
        return isRedstoneCardInstalled(getCardUpgrades());
    }

    public static boolean isRedstoneCardInstalled(IUpgradeInventory cardUpgrades) {
        return cardUpgrades.getInstalledUpgrades(AEItems.REDSTONE_CARD) > 0;
    }

    public boolean isVoidCardInstalled() {
        return getCardUpgrades().getInstalledUpgrades(AEItems.VOID_CARD) > 0;
    }

    public boolean isEqualDistributionCardInstalled() {
        return getCardUpgrades().getInstalledUpgrades(AEItems.EQUAL_DISTRIBUTION_CARD) > 0;
    }

    public boolean isInverterCardInstalled() {
        return getCardUpgrades().getInstalledUpgrades(AEItems.INVERTER_CARD) > 0;
    }

    public boolean isCraftingCardInstalled() {
        return getCardUpgrades().getInstalledUpgrades(AEItems.CRAFTING_CARD) > 0;
    }
}
