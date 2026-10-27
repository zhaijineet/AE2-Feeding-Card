package net.zhaiji.ae2feedingcard.attachment;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEItemKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Future;

/**
 * 单台终端的瞬时状态：可合成集合的缓存、扫描与冷却的剩余tick数、在途规划。
 */
public class TerminalState {
    private List<AEItemKey> craftableKeys = List.of();
    /**
     * 距下次可扫描的剩余tick数，0表示可以立刻执行；按tick推进，不看世界时刻。
     */
    private int ticksUntilScan;
    /**
     * 距下次可提交合成的剩余tick数，0表示没有冷却；按tick推进，不看世界时刻。
     */
    private int ticksUntilCraft;

    @Nullable
    private Future<ICraftingPlan> planningFuture;

    /**
     * 每tick推进一次：两个计时器各自递减到0为止，0表示可以立刻执行。
     */
    public void advance() {
        if (this.ticksUntilScan > 0) this.ticksUntilScan--;
        if (this.ticksUntilCraft > 0) this.ticksUntilCraft--;
    }

    /**
     * 距上次扫描满一个周期才刷新，进度按tick推进，不受进食冷却的周期影响；首次取用时立即放行。
     */
    public boolean isCraftScanDue(int period) {
        if (this.ticksUntilScan > 0) return false;

        this.ticksUntilScan = period;
        return true;
    }

    public List<AEItemKey> getCraftableKeys() {
        return this.craftableKeys;
    }

    public void setCraftableKeys(List<AEItemKey> craftableKeys) {
        this.craftableKeys = craftableKeys;
    }

    /**
     * 冷却由「结算成功」压短、由失败路径压长，两者互斥写入。
     */
    public boolean isCraftCooldownActive() {
        return this.ticksUntilCraft > 0;
    }

    public void setCraftCooldown(int ticks) {
        this.ticksUntilCraft = ticks;
    }

    /**
     * 当前在途的规划，没有正在计算的规划时为null。
     */
    @Nullable
    public Future<ICraftingPlan> getPlanningFuture() {
        return this.planningFuture;
    }

    /**
     * 开启一单规划，与{@link #clearPlanning()}成对，不收回会让后续请求一直被这条Future挡住。
     */
    public void startPlanning(Future<ICraftingPlan> planningFuture) {
        this.planningFuture = planningFuture;
    }

    public void clearPlanning() {
        this.planningFuture = null;
    }
}
