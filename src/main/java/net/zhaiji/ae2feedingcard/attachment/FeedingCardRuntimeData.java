package net.zhaiji.ae2feedingcard.attachment;

import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 挂在玩家身上的运行状态：按终端栈实例隔离的各终端状态，外加一个瞬时的音效标记，不参与存档与同步。
 *
 * <p>用引用身份而不是 equals 或哈希：{@code ItemStack} 没有覆写 {@code equals}/{@code hashCode}，本身就等价于身份比较；
 * 这里显式用{@code IdentityHashMap}只是把这条语义写在类型上，避免以后有人误以为它是值语义。</p>
 */
public class FeedingCardRuntimeData {
    private final Map<ItemStack, TerminalState> terminalStates = new IdentityHashMap<>();
    private boolean suppressEatSound;

    public TerminalState stateOf(ItemStack terminalStack) {
        return this.terminalStates.computeIfAbsent(terminalStack, ignored -> new TerminalState());
    }

    /**
     * 丢弃已经不在玩家身上（或已不是同一实例）的终端状态，缓存可重建，未完成的规划作废。
     */
    public void retainTerminals(Set<ItemStack> presentTerminalStacks) {
        this.terminalStates.keySet().retainAll(presentTerminalStacks);
    }

    public boolean isSuppressingEatSound() {
        return this.suppressEatSound;
    }

    public void setSuppressingEatSound(boolean suppressEatSound) {
        this.suppressEatSound = suppressEatSound;
    }
}
