package net.zhaiji.ae2feedingcard.item;

import appeng.items.materials.UpgradeCardItem;

public class FeedingCardItem extends UpgradeCardItem {
    public static final int UPGRADE_SLOT_COUNT = 8;

    /**
     * 无需容量卡即可使用的白名单位数。
     */
    public static final int BASE_FILTER_SLOT_COUNT = 27;

    /**
     * 每张容量卡额外解锁的位数。
     */
    public static final int FILTER_SLOTS_PER_CAPACITY_CARD = 9;

    /**
     * 白名单总位数，27 个基础位加 3 张容量卡各 9 位。
     *
     * <p>这个 3 是喂食卡自身能装的容量卡数，见 {@link net.zhaiji.ae2feedingcard.init.AE2FeedingCardUpgrades}。
     * 不要和「喂食卡能装进几台无线终端」的上限混为一谈，那是另一回事，且是 1。</p>
     */
    public static final int FILTER_SLOT_COUNT = 54;

    /**
     * 装了若干张容量卡后实际可用的白名单位数。
     *
     * <p>装卡上限由 {@code Upgrades.add} 保证，所以这里不再截断；
     * 调用方仍会用白名单实际长度取小，组件被改坏时也不会越界。</p>
     */
    public static int availableFilterSlots(int capacityCardCount) {
        return BASE_FILTER_SLOT_COUNT + FILTER_SLOTS_PER_CAPACITY_CARD * capacityCardCount;
    }

    public FeedingCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}
