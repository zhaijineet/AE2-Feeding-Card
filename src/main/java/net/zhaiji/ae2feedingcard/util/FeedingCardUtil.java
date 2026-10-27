package net.zhaiji.ae2feedingcard.util;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.AEKeyFilter;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;
import net.zhaiji.ae2feedingcard.attachment.FeedingCardRuntimeData;
import net.zhaiji.ae2feedingcard.attachment.TerminalState;
import net.zhaiji.ae2feedingcard.compat.CompatManager;
import net.zhaiji.ae2feedingcard.config.AE2FeedingCardServerConfig;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardAttachmentTypes;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardDataComponents;
import net.zhaiji.ae2feedingcard.item.FeedingCardItem;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CompoundEffectHandling;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CraftTiming;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.CraftWaiting;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.HarmfulEffectHandling;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.Secondary;
import net.zhaiji.ae2feedingcard.util.FeedingStrategy.UneatenFoodHandling;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 喂食卡的核心逻辑：从装在无线终端里的喂食卡读取设置与白名单，按主副策略挑一份食物喂给玩家，
 * 缺货且装了合成卡时自己发起一份的合成请求。
 */
public class FeedingCardUtil {
    private static final double FILL_RATIO_HIGH_TIER = 0.9;
    private static final double FILL_RATIO_MIDDLE_TIER = 0.5;

    public static void tick(ServerPlayer player) {
        long gameTime = player.level().getGameTime();

        FeedingCardRuntimeData runtimeData = player.getData(AE2FeedingCardAttachmentTypes.FEEDING_CARD_RUNTIME_STATE);

        List<ItemStack> carrierStacks = collectCarrierStacks(player);
        runtimeData.retainTerminals(presentTerminalStacks(carrierStacks));

        for (ItemStack carrierStack : carrierStacks) {
            if (!(carrierStack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) continue;

            IUpgradeInventory terminalUpgrades = wirelessTerminalItem.getUpgrades(carrierStack);
            int feedingCardSlot = findFeedingCardSlot(terminalUpgrades);
            if (feedingCardSlot < 0) continue;

            // 槽位刚由 findFeedingCardSlot 定位，这里不再重复遍历
            FeedingCardAccess access = FeedingCardAccess.fromTerminalStack(() -> carrierStack, feedingCardSlot);

            // 开关关闭时整张卡跳过；没装红石卡时这把锁不存在，开关被忽略、组件里的值保留
            boolean enabled = !access.isRedstoneCardInstalled() || access.readEnabled();
            if (!enabled) continue;

            TerminalState terminalState = runtimeData.stateOf(carrierStack);
            // 必须在isFeedingDue之前调用：两个计时器随后被扫描与进食检查读取，靠每tick推进才能按墙钟tick计时
            terminalState.advance();
            refreshCraftableKeysIfDue(player, wirelessTerminalItem, carrierStack, terminalUpgrades,
                    feedingCardSlot, access, runtimeData, terminalState);
            if (!isFeedingDue(gameTime, cooldownTicks(access.getCardUpgrades()), carrierStack)) continue;

            // 电量、未链接、超出接入点范围都由终端自己的链路状态判定，喂食卡是终端的升级卡，不另立判据
            WirelessTerminalMenuHost<?> terminalHost = wirelessTerminalItem.getMenuHost(player,
                    new FeedingCardTerminalLocator(carrierStack), null);
            if (!terminalHost.getLinkStatus().connected()) continue;

            IGrid grid = wirelessTerminalItem.getLinkedGrid(carrierStack, player.level(), null);

            SelectionContext context = selectionContextOf(player, grid, carrierStack, terminalUpgrades,
                    feedingCardSlot, access, runtimeData, terminalState);
            if (tryFeed(context)) return;
        }
    }

    private static Set<ItemStack> presentTerminalStacks(List<ItemStack> carrierStacks) {
        // 身份集合：ItemStack 没有覆写 equals/hashCode，IdentityHashMap 把「同一实例」这条语义写在类型上
        Set<ItemStack> presentTerminalStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack carrierStack : carrierStacks) {
            if (carrierStack.getItem() instanceof WirelessTerminalItem) presentTerminalStacks.add(carrierStack);
        }
        return presentTerminalStacks;
    }

    /**
     * 依次收集背包物品栏、盔甲、副手三块槽位，Curios 在场时再追加已佩戴饰品。
     */
    private static List<ItemStack> collectCarrierStacks(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> carrierStacks = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            carrierStacks.add(inventory.getItem(slot));
        }
        carrierStacks.addAll(CompatManager.getEquippedStacks(player));
        return carrierStacks;
    }

    /**
     * 返回终端升级槽里第一个喂食卡的槽位，没有时返回 -1。
     */
    private static int findFeedingCardSlot(IUpgradeInventory terminalUpgrades) {
        for (int slot = 0; slot < terminalUpgrades.size(); slot++) {
            if (terminalUpgrades.getStackInSlot(slot).getItem() instanceof FeedingCardItem) return slot;
        }
        return -1;
    }

    /**
     * 进食检查按周期加相位错峰。相位直接对周期取模，任意周期都均匀分布，不需要额外常量；
     * 相位取自终端栈的身份哈希，同一实例跨tick恒定，实例被替换时自然重派生。
     *
     * <p>{@code ItemStack} 没有覆写{@code hashCode}，所以这里拿到的是稳定的身份哈希。</p>
     */
    private static boolean isFeedingDue(long gameTime, int period, ItemStack carrierStack) {
        return gameTime % period == Math.floorMod(carrierStack.hashCode(), period);
    }

    private static int cooldownTicks(IUpgradeInventory cardUpgrades) {
        int speedCardCount = cardUpgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        int cooldownTicks = (int) (AE2FeedingCardServerConfig.BASE_COOLDOWN_TICKS.get()
                * Math.pow(AE2FeedingCardServerConfig.COOLDOWN_FACTOR_PER_SPEED_CARD.get(), speedCardCount));
        return Math.max(AE2FeedingCardServerConfig.MIN_COOLDOWN_TICKS.get(), cooldownTicks);
    }

    private static SelectionContext selectionContextOf(ServerPlayer player, IGrid grid, ItemStack carrierStack,
            IUpgradeInventory terminalUpgrades, int feedingCardSlot, FeedingCardAccess access,
            FeedingCardRuntimeData runtimeData, TerminalState terminalState) {
        UneatenFoodHandling uneatenFoodHandling = CompatManager.SOL_CARROT_LOADED
                ? access.readSettings().uneatenFoodHandling()
                : UneatenFoodHandling.IGNORE;

        return new SelectionContext(player, grid, carrierStack, terminalUpgrades, feedingCardSlot, access,
                runtimeData, terminalState, new HashSet<>(), dangerousEffectIds(), uneatenFoodHandling);
    }

    /**
     * 能量从终端自己的电池扣，而不是从网格扣，与AE2的无线终端菜单一致。
     *
     * <p>不这么做的话，一块电量归零的终端插上喂食卡仍会免费进食，而用同一块终端手动抽取却抽不动。
     * 调用方是tick里已经筛过载体类型的路径，所以这里直接强转。</p>
     */
    private static IEnergySource terminalEnergySource(SelectionContext context) {
        WirelessTerminalItem wirelessTerminalItem = (WirelessTerminalItem) context.carrierStack().getItem();
        return (amount, mode, usePowerMultiplier) -> {
            ItemStack terminalStack = context.carrierStack();
            double available = Math.min(amount, wirelessTerminalItem.getAECurrentPower(terminalStack));
            if (mode == Actionable.SIMULATE) return available;
            return wirelessTerminalItem.usePower(context.player(), available, terminalStack) ? available : 0;
        };
    }

    private static Set<ResourceLocation> dangerousEffectIds() {
        Set<ResourceLocation> effectIds = new HashSet<>();
        // 配置加载期correct()已按元素验证器剔除非法项并回写，此处不需要容错
        for (String effectId : AE2FeedingCardServerConfig.DANGEROUS_FOOD_EFFECTS.get()) {
            effectIds.add(ResourceLocation.parse(effectId));
        }
        return effectIds;
    }

    private static boolean tryFeed(SelectionContext context) {
        ServerPlayer player = context.player();
        // 与ItemStack.consume用同一个判据：consume短路看的是hasInfiniteMaterials而不是isCreative，
        // 模组把instabuild置真却不改游戏模式时，用isCreative拦不住、食物会被取出后原样丢失。
        // isSpectator不能合并进来：旁观者的instabuild是false，只靠hasInfiniteMaterials拦不住
        if (player.isSpectator() || player.hasInfiniteMaterials()) return false;

        FeedingCardSettings settings = context.access().readSettings();
        FoodData foodData = player.getFoodData();
        boolean toppingUpSaturation = isToppingUpSaturation(foodData);
        if (toppingUpSaturation) {
            // 饱了还能继续吃意味着多出来的部分被销毁，这一点由溢出销毁卡提供，没装卡就一律停
            if (!context.access().getCardUpgrades().isInstalled(AEItems.VOID_CARD)) return false;
            if (settings.fullHungerBehavior().stopsFeeding()) return false;
            if (foodData.getSaturationLevel() >= settings.fullHungerBehavior().getSaturationThreshold()) return false;
        }

        boolean pendingCraftInFlight = settlePendingCraft(context);
        if (pendingCraftInFlight && settings.craftWaiting() == CraftWaiting.WAIT_FOR_COMPLETION) return false;

        List<AEItemKey> stockCandidates = filterCandidates(context, collectStockCandidates(context),
                toppingUpSaturation);

        boolean craftablesNeeded = craftablesNeeded(context, stockCandidates);
        List<AEItemKey> craftableCandidates = craftablesNeeded
                ? filterCandidates(context, measureCraftableCandidates(context), toppingUpSaturation)
                : List.of();

        List<AEItemKey> decisionPool = buildDecisionPool(stockCandidates, craftableCandidates,
                settings.craftTiming());
        discardStalePlanning(context, stockCandidates, decisionPool);

        AEItemKey chosenKey = choose(context, decisionPool, toppingUpSaturation);
        if (chosenKey == null) return false;

        if (!stockCandidates.contains(chosenKey)) {
            boolean craftingInFlight = requestCraft(context, chosenKey, pendingCraftInFlight);
            if (craftingInFlight && settings.craftWaiting() == CraftWaiting.WAIT_FOR_COMPLETION) return false;

            chosenKey = choose(context, stockCandidates, toppingUpSaturation);
            if (chosenKey == null) return false;
        }

        return eat(context, chosenKey);
    }

    /**
     * 只有库存候选为空、或合成时机为「可优先合成更好的」时才碰可合成集合，避免每 tick 全量测量；
     * 扫描已与进食检查解耦、独立按 {@code craftScanPeriodTicks} 推进，与本判定无关。
     */
    private static boolean craftablesNeeded(SelectionContext context, List<AEItemKey> stockCandidates) {
        if (!context.access().getCardUpgrades().isInstalled(AEItems.CRAFTING_CARD)) return false;
        if (context.access().readSettings().craftTiming() == CraftTiming.PREFER_BETTER) return true;
        return stockCandidates.isEmpty();
    }

    /**
     * 规划已完成但目标已经不可用时丢弃：目标已到货，或已经不在本次决策池里。
     *
     * <p>请求键不必单独存：{@code finalOutput().what()}恒等于请求键，依据是{@code CraftingCalculation}
     * 建树时树根就是它。规划失败时拿不到结果，直接按无目标收回并发起重试冷却，
     * 否则库存里还有食物时{@code requestCraft}根本不会被调用，规划会一直卡在这条已完成的Future上。</p>
     */
    private static void discardStalePlanning(SelectionContext context, List<AEItemKey> stockCandidates,
            List<AEItemKey> decisionPool) {
        TerminalState terminalState = context.terminalState();
        Future<ICraftingPlan> planningFuture = terminalState.getPlanningFuture();
        if (planningFuture == null || !planningFuture.isDone()) return;

        ICraftingPlan craftingPlan = awaitPlanningResult(planningFuture);
        if (craftingPlan == null) {
            terminalState.clearPlanning();
            terminalState.setCraftCooldown(AE2FeedingCardServerConfig.CRAFT_RETRY_COOLDOWN_TICKS.get());
            return;
        }

        AEItemKey planningKey = craftingPlan.finalOutput().what() instanceof AEItemKey itemKey ? itemKey : null;
        if (!stockCandidates.contains(planningKey) && decisionPool.contains(planningKey)) return;

        terminalState.clearPlanning();
    }

    private static boolean isToppingUpSaturation(FoodData foodData) {
        return foodData.getFoodLevel() >= FoodConstants.MAX_FOOD;
    }

    /**
     * 补饱和度只在饱和度缺口撑得住这份食物的浪费时进行，比例由服务器配置决定。
     */
    private static boolean passesSaturationGate(AEItemKey candidateKey, ServerPlayer player) {
        double saturationGap = FoodConstants.MAX_SATURATION - player.getFoodData().getSaturationLevel();
        double saturation = candidateKey.getReadOnlyStack().getFoodProperties(player).saturation();
        // 零饱和度让下面的不等式恒真，负饱和度吃下去反而降低饱和度，两者都补不了缺口
        if (saturation <= 0) return false;
        return saturationGap >= saturation * AE2FeedingCardServerConfig.SATURATION_GAP_RATIO.get();
    }

    private static List<AEItemKey> filterCandidates(SelectionContext context,
            List<AEItemKey> candidates, boolean toppingUpSaturation) {
        ServerPlayer player = context.player();
        if (!AE2FeedingCardServerConfig.IGNORE_ITEM_COOLDOWNS.get()) {
            candidates.removeIf(candidateKey -> player.getCooldowns().isOnCooldown(candidateKey.getItem()));
        }

        if (toppingUpSaturation) {
            candidates.removeIf(candidateKey -> !passesSaturationGate(candidateKey, player));
        }
        return preferUneaten(candidates, context.uneatenKeys(), context.uneatenFoodHandling());
    }

    /**
     * 两档「优先吃没吃过的」都只在未尝试组非空时用它，为空就回落到全部候选，所以永远不会因为挑不到而不吃。
     * 含毒那一档与普通档的差别不在分组，而在 {@link #passesCardFilters} 里给未尝试候开放宽有害排除。
     */
    private static List<AEItemKey> preferUneaten(List<AEItemKey> candidates, Set<AEItemKey> uneatenKeys,
            UneatenFoodHandling uneatenFoodHandling) {
        if (uneatenFoodHandling == UneatenFoodHandling.IGNORE) return candidates;

        List<AEItemKey> uneatenCandidates = new ArrayList<>();
        for (AEItemKey candidateKey : candidates) {
            if (uneatenKeys.contains(candidateKey)) uneatenCandidates.add(candidateKey);
        }

        return uneatenCandidates.isEmpty() ? candidates : uneatenCandidates;
    }

    /**
     * 从缓存库存里筛出可喂的候选键，只读遍历，不改动网络内容。
     */
    private static List<AEItemKey> collectStockCandidates(SelectionContext context) {
        KeyCounter cachedInventory = context.grid().getStorageService().getCachedInventory();
        List<AEItemKey> candidates = new ArrayList<>();

        for (Object2LongMap.Entry<AEKey> counterEntry : cachedInventory) {
            if (!(counterEntry.getKey() instanceof AEItemKey itemKey)) continue;

            FoodProperties foodProperties = itemKey.getReadOnlyStack().getFoodProperties(context.player());
            if (foodProperties == null) continue;

            boolean uneaten = isUneaten(context, itemKey);
            if (!passesCardFilters(itemKey, foodProperties, uneaten, context)) continue;
            if (uneaten) context.uneatenKeys().add(itemKey);

            candidates.add(itemKey);
        }
        return candidates;
    }

    /**
     * 可合成集合同步按扫描周期刷新，不再受进食冷却门控；链路不通时本周期作废，换取不必每tick构造终端宿主。
     */
    private static void refreshCraftableKeysIfDue(ServerPlayer player, WirelessTerminalItem wirelessTerminalItem,
            ItemStack carrierStack, IUpgradeInventory terminalUpgrades, int feedingCardSlot, FeedingCardAccess access,
            FeedingCardRuntimeData runtimeData, TerminalState terminalState) {
        if (!access.getCardUpgrades().isInstalled(AEItems.CRAFTING_CARD)) return;
        if (!terminalState.isCraftScanDue(AE2FeedingCardServerConfig.CRAFT_SCAN_PERIOD_TICKS.get())) return;

        WirelessTerminalMenuHost<?> terminalHost = wirelessTerminalItem.getMenuHost(player,
                new FeedingCardTerminalLocator(carrierStack), null);
        if (!terminalHost.getLinkStatus().connected()) return;

        IGrid grid = wirelessTerminalItem.getLinkedGrid(carrierStack, player.level(), null);
        refreshCraftableKeys(selectionContextOf(player, grid, carrierStack, terminalUpgrades, feedingCardSlot, access,
                runtimeData, terminalState));
    }

    /**
     * 可合成集合按独立周期刷新，扫描时先按当前卡片状态过滤一遍，收窄缓存规模。
     */
    private static void refreshCraftableKeys(SelectionContext context) {
        AEKeyFilter craftableFilter = key -> key instanceof AEItemKey itemKey
                && isCraftableFood(itemKey, itemKey.getReadOnlyStack().getFoodProperties(context.player()), context);
        Set<AEKey> craftableKeys = context.grid().getCraftingService().getCraftables(craftableFilter);

        List<AEItemKey> cachedKeys = new ArrayList<>(craftableKeys.size());
        for (AEKey craftableKey : craftableKeys) {
            if (craftableKey instanceof AEItemKey itemKey) cachedKeys.add(itemKey);
        }
        context.terminalState().setCraftableKeys(cachedKeys);
    }

    /**
     * 通过卡片过滤的才算候选，判定通过且没吃过时顺带记进本轮未尝试集合。
     */
    private static boolean isCraftableFood(AEItemKey itemKey, @Nullable FoodProperties foodProperties,
            SelectionContext context) {
        if (foodProperties == null) return false;

        boolean uneaten = isUneaten(context, itemKey);
        if (!passesCardFilters(itemKey, foodProperties, uneaten, context)) return false;

        if (uneaten) context.uneatenKeys().add(itemKey);
        return true;
    }

    /**
     * 缓存挂在终端上而白名单挂在卡上，取用时必须用当前卡片状态复核一遍，否则移出白名单的食物还会被发合成。
     * 调用方已保证装了合成卡，这里不重复判定。
     */
    private static List<AEItemKey> measureCraftableCandidates(SelectionContext context) {
        List<AEItemKey> candidates = new ArrayList<>();

        for (AEItemKey itemKey : context.terminalState().getCraftableKeys()) {
            FoodProperties foodProperties = itemKey.getReadOnlyStack().getFoodProperties(context.player());
            if (!isCraftableFood(itemKey, foodProperties, context)) continue;

            candidates.add(itemKey);
        }
        return candidates;
    }

    /**
     * 不干预时这个标记不会被读，直接短路掉跨模组的能力查询；纯判定，不写本轮未尝试集合。
     */
    private static boolean isUneaten(SelectionContext context, AEItemKey itemKey) {
        if (context.uneatenFoodHandling() == UneatenFoodHandling.IGNORE) return false;
        return !CompatManager.hasEaten(context.player(), itemKey.getItem());
    }

    /**
     * 有害排除只在反向卡下生效；「优先吃没吃过的，哪怕有害」只给没吃过的候选放宽，
     * 已经吃过的仍然照常排除，所以全部吃过后回落时行为自动恢复正常。
     */
    private static boolean passesCardFilters(AEItemKey itemKey, FoodProperties foodProperties, boolean uneaten,
            SelectionContext context) {
        IUpgradeInventory cardUpgrades = context.access().getCardUpgrades();
        boolean fuzzyCard = cardUpgrades.isInstalled(AEItems.FUZZY_CARD);
        boolean inWhitelist = matchesWhitelist(itemKey, context.access().readWhitelist(), fuzzyCard);
        boolean inverterCard = cardUpgrades.isInstalled(AEItems.INVERTER_CARD);
        if (inverterCard ? inWhitelist : !inWhitelist) return false;

        if (!inverterCard) return true;
        if (uneaten && context.uneatenFoodHandling() == UneatenFoodHandling.PREFER_UNEATEN_INCLUDING_HARMFUL) {
            return true;
        }
        return !isExcludedByEffectHandling(foodProperties, context.access().readSettings().harmfulEffectHandling(),
                context.dangerousEffectIds());
    }

    private static boolean matchesWhitelist(AEItemKey candidateKey, List<AEItemKey> whitelist, boolean fuzzyCard) {
        for (AEItemKey whitelistKey : whitelist) {
            boolean matched = fuzzyCard
                    ? whitelistKey.fuzzyEquals(candidateKey, FuzzyMode.IGNORE_ALL)
                    : whitelistKey.equals(candidateKey);
            if (matched) return true;
        }
        return false;
    }

    /**
     * 有害效果排除只挂在反向卡上，正向模式不改玩家意愿。
     */
    private static boolean isExcludedByEffectHandling(FoodProperties foodProperties,
            HarmfulEffectHandling harmfulEffectHandling, Set<ResourceLocation> dangerousEffectIds) {
        if (harmfulEffectHandling == HarmfulEffectHandling.KEEP) return false;

        for (FoodProperties.PossibleEffect possibleEffect : foodProperties.effects()) {
            if (possibleEffect.probability() <= 0) continue;

            MobEffect mobEffect = possibleEffect.effect().getEffect().value();
            if (mobEffect.getCategory() != MobEffectCategory.HARMFUL) continue;
            if (harmfulEffectHandling == HarmfulEffectHandling.EXCLUDE_ALL) return true;
            if (dangerousEffectIds.contains(BuiltInRegistries.MOB_EFFECT.getKey(mobEffect))) return true;
        }
        return false;
    }

    /**
     * 「没食物时才合成」下库存优先，「可优先合成更好的」下库存与可合成同池按质量排。
     */
    private static List<AEItemKey> buildDecisionPool(List<AEItemKey> stockCandidates,
            List<AEItemKey> craftableCandidates, CraftTiming craftTiming) {
        if (craftableCandidates.isEmpty()) return stockCandidates;
        if (stockCandidates.isEmpty()) return craftableCandidates;
        if (craftTiming == CraftTiming.ONLY_WHEN_EMPTY) return stockCandidates;

        List<AEItemKey> decisionPool = new ArrayList<>(stockCandidates);
        for (AEItemKey craftableKey : craftableCandidates) {
            if (stockCandidates.contains(craftableKey)) continue;
            decisionPool.add(craftableKey);
        }
        return decisionPool;
    }

    @Nullable
    private static AEItemKey choose(SelectionContext context, List<AEItemKey> candidates,
            boolean toppingUpSaturation) {
        if (candidates.isEmpty()) return null;
        if (context.access().getCardUpgrades().isInstalled(AEItems.EQUAL_DISTRIBUTION_CARD)) {
            return pickByRotation(context, candidates);
        }
        return chooseByStrategy(context, candidates, toppingUpSaturation);
    }

    /**
     * 一遍求最小值，不为每个候选构造中间对象，代价是每次比较都现算派生值；候选规模是网络里的食物种类数。
     */
    private static AEItemKey chooseByStrategy(SelectionContext context, List<AEItemKey> candidates,
            boolean toppingUpSaturation) {
        Comparator<AEItemKey> comparator = comparatorFor(context, toppingUpSaturation);
        AEItemKey best = null;
        for (AEItemKey candidate : candidates) {
            if (best == null || comparator.compare(candidate, best) < 0) best = candidate;
        }
        return best;
    }

    private static Comparator<AEItemKey> comparatorFor(SelectionContext context, boolean toppingUpSaturation) {
        ServerPlayer player = context.player();
        FeedingCardSettings settings = context.access().readSettings();
        Comparator<AEItemKey> primaryComparator = switch (settings.primaryStrategy()) {
            case AVOID_WASTE -> {
                Comparator<AEItemKey> fillRatioComparator = Comparator
                        .comparingInt(candidateKey -> fillRatioTier(fillRatioOf(candidateKey, player)));
                // 饥饿值已满时，食物提供的饥饿值全部作废，只剩饱和度有收益。单次浪费排会挑中补得慢的，
                // 轮数优先又会为了少跑一趟而选一份大食物、多作废几倍饥饿值，
                // 所以按「单次浪费 × 还需几轮」估算总代价，偏向少跑几趟；轮数相同时再看单次浪费。
                // 这是估算值不是真实总浪费，最后一份会被缺口截断，那部分本来就是要买的饱和度。
                // 第三键是升序不是漏了reversed：前两键并列说明轮数与单次浪费都相同，
                // 此时增益小的那个最后一次被缺口截得少、浪费更小，所以增益越小越优
                yield toppingUpSaturation
                        ? Comparator.<AEItemKey>comparingDouble(candidateKey -> {
                            int feedsLeft = feedsLeftOf(candidateKey, player);
                            return feedsLeft == Integer.MAX_VALUE
                                    ? Double.MAX_VALUE
                                    : overflowOf(candidateKey, player) * feedsLeft;
                        })
                                .thenComparingInt(candidateKey -> feedsLeftOf(candidateKey, player))
                                .thenComparingDouble(candidateKey -> saturationGainOf(candidateKey, player))
                        : fillRatioComparator.thenComparingDouble(candidateKey -> overflowOf(candidateKey, player));
            }
            // 饱和度增益会被缺口截断而大量并列，并列时比单次浪费：
            // 比原始饱和度只会选到一个同样补满却多作废饥饿值的食物。
            // 「优先」高饱和不等于「保证最高」——缺口小于两者饱和度时它们补得一样多，
            // 此时按浪费选更合理，名义饱和度更高的那个只是多出来的部分被丢掉
            case HIGH_SATURATION -> Comparator
                    .comparingDouble((AEItemKey candidateKey) -> saturationGainOf(candidateKey, player)).reversed()
                    .thenComparingDouble(candidateKey -> overflowOf(candidateKey, player));
        };

        return primaryComparator
                .thenComparingInt(candidateKey -> effectRankOf(candidateKey, settings.compoundEffectHandling(),
                        settings.secondaryStrategy(), player))
                .thenComparing(FeedingCardUtil::sortKeyOf);
    }

    private static int fillRatioTier(double fillRatio) {
        if (fillRatio >= FILL_RATIO_HIGH_TIER) return 1;
        if (fillRatio >= FILL_RATIO_MIDDLE_TIER) return 2;
        if (fillRatio > 0) return 3;
        return 4;
    }

    /**
     * 这份食物按当前饥饿与饱和度实际能吃进去的饱和度，收益以原版饥饿上限20计算，模组抬高上限时缺口会被低估。
     */
    private static double saturationGainOf(AEItemKey candidateKey, ServerPlayer player) {
        FoodProperties foodProperties = candidateKey.getReadOnlyStack().getFoodProperties(player);
        FoodData foodData = player.getFoodData();
        double foodLevelGap = FoodConstants.MAX_FOOD - foodData.getFoodLevel();
        double foodLevelGain = Math.min(foodProperties.nutrition(), foodLevelGap);
        double saturationGap = foodData.getFoodLevel() + foodLevelGain - foodData.getSaturationLevel();
        return Math.min(foodProperties.saturation(), saturationGap);
    }

    /**
     * 名义增益与实际增益之差，即这一份会被销毁掉的部分。
     */
    private static double overflowOf(AEItemKey candidateKey, ServerPlayer player) {
        FoodProperties foodProperties = candidateKey.getReadOnlyStack().getFoodProperties(player);
        FoodData foodData = player.getFoodData();
        double foodLevelGap = FoodConstants.MAX_FOOD - foodData.getFoodLevel();
        double foodLevelGain = Math.min(foodProperties.nutrition(), foodLevelGap);
        double saturationGap = foodData.getFoodLevel() + foodLevelGain - foodData.getSaturationLevel();
        double saturationGain = Math.min(foodProperties.saturation(), saturationGap);
        double nominalGain = foodProperties.nutrition() + foodProperties.saturation();
        return nominalGain - (foodLevelGain + saturationGain);
    }

    /**
     * 这一份能填上多少饥饿缺口，缺口为0时按1处理。
     */
    private static double fillRatioOf(AEItemKey candidateKey, ServerPlayer player) {
        FoodProperties foodProperties = candidateKey.getReadOnlyStack().getFoodProperties(player);
        double foodLevelGap = FoodConstants.MAX_FOOD - player.getFoodData().getFoodLevel();
        return foodLevelGap > 0 ? Math.min(foodProperties.nutrition(), foodLevelGap) / foodLevelGap : 1.0;
    }

    /**
     * 按当前饱和度缺口还需要进食几次才补满，只在补饱和度（{@code foodLevel == MAX_FOOD}）时有意义。
     *
     * <p>其他饥饿状态下它仍会被算出来，但那时每吃一次饥饿值上升、上限跟着涨，缺口重算，
     * 「还要几次」不成立，所以不要拿它做别的排序。</p>
     */
    private static int feedsLeftOf(AEItemKey candidateKey, ServerPlayer player) {
        FoodProperties foodProperties = candidateKey.getReadOnlyStack().getFoodProperties(player);
        FoodData foodData = player.getFoodData();
        double foodLevelGap = FoodConstants.MAX_FOOD - foodData.getFoodLevel();
        double foodLevelGain = Math.min(foodProperties.nutrition(), foodLevelGap);
        double saturationGap = foodData.getFoodLevel() + foodLevelGain - foodData.getSaturationLevel();
        double saturationGain = Math.min(foodProperties.saturation(), saturationGap);
        if (saturationGain <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(saturationGap / saturationGain);
    }

    /**
     * 副策略只在主策略并列时生效，有害在两档里都被压到末位；这里直接返回排序权重，省掉中间的档位枚举。
     *
     * <p>{@code STRICT}要求任意一条有害即归有害，{@code OFFSET}按条数比大小，中性条目只在前两者都为零时决定归类。</p>
     */
    private static int effectRankOf(AEItemKey candidateKey, CompoundEffectHandling compoundEffectHandling,
            Secondary secondaryStrategy, ServerPlayer player) {
        if (secondaryStrategy == Secondary.IGNORE) return 0;

        FoodProperties foodProperties = candidateKey.getReadOnlyStack().getFoodProperties(player);
        int beneficialCount = 0;
        int harmfulCount = 0;
        boolean hasNeutralEffect = false;
        for (FoodProperties.PossibleEffect possibleEffect : foodProperties.effects()) {
            if (possibleEffect.probability() <= 0) continue;

            MobEffectCategory effectCategory = possibleEffect.effect().getEffect().value().getCategory();
            if (effectCategory == MobEffectCategory.HARMFUL) {
                harmfulCount++;
            } else if (effectCategory == MobEffectCategory.BENEFICIAL) {
                beneficialCount++;
            } else {
                hasNeutralEffect = true;
            }
        }

        boolean strict = compoundEffectHandling == CompoundEffectHandling.STRICT;
        boolean harmful = strict ? harmfulCount > 0 : harmfulCount > beneficialCount;
        boolean beneficial = !harmful && (strict ? beneficialCount > 0 : beneficialCount > harmfulCount);
        boolean neutral = !harmful && !beneficial
                && (strict ? hasNeutralEffect : harmfulCount > 0 || hasNeutralEffect);

        if (secondaryStrategy == Secondary.PREFER_BENEFICIAL) {
            if (harmful) return 3;
            if (beneficial) return 0;
            return neutral ? 1 : 2;
        }

        if (harmful) return 3;
        if (beneficial) return 1;
        return neutral ? 2 : 0;
    }

    /**
     * 均分卡模式：按物品注册 ID 加组件内容的两级排序键轮流取，找到第一个键大于游标的候选，找不到回到第一个。
     */
    private static AEItemKey pickByRotation(SelectionContext context, List<AEItemKey> candidates) {
        List<AEItemKey> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(Comparator.comparing(FeedingCardUtil::sortKeyOf));

        AEItemKey cursorKey = readSingleKey(context.cardStack(),
                AE2FeedingCardDataComponents.FEEDING_CURSOR.get());
        if (cursorKey != null) {
            String cursorSortKey = sortKeyOf(cursorKey);
            for (AEItemKey candidateKey : sortedCandidates) {
                if (sortKeyOf(candidateKey).compareTo(cursorSortKey) > 0) return candidateKey;
            }
        }

        return sortedCandidates.getFirst();
    }

    private static String sortKeyOf(AEItemKey key) {
        return BuiltInRegistries.ITEM.getKey(key.getItem()) + "|" + key.getReadOnlyStack().getComponents().hashCode();
    }

    /**
     * 还在途的合成请求返回 true；读到有效目标且确认不在途时才清除并压一次结算冷却，不区分成功失败。
     *
     * <p>先查库存：CPU 上的任务状态只说明「有人在合成这种物品」，不区分是谁下的单，
     * 玩家手动合成同一种食物时会被误判成自己那一单还没到货。库存里已经有就直接清标记，不压冷却。
     * 压冷却是因为到货瞬间库存缓存可能还没刷新，不压会马上又下一单。这里用的是短冷却，
     * 重试冷却只给失败路径。两种冷却都只在进食检查里读取，短于进食冷却时实际间隔取进食冷却。
     * 目标损坏时按无在途处理，既不清除也不压冷却，占位物留着可在外部模组装回来后恢复。</p>
     */
    private static boolean settlePendingCraft(SelectionContext context) {
        AEItemKey pendingKey = readSingleKey(context.cardStack(),
                AE2FeedingCardDataComponents.FEEDING_PENDING_CRAFT.get());
        if (pendingKey == null) return false;
        if (context.grid().getStorageService().getCachedInventory().get(pendingKey) > 0) {
            writePendingCraft(context, null);
            return false;
        }
        if (isCraftInFlight(context.grid().getCraftingService(), pendingKey)) return true;

        writePendingCraft(context, null);
        context.terminalState().setCraftCooldown(AE2FeedingCardServerConfig.CRAFT_SETTLE_COOLDOWN_TICKS.get());
        return false;
    }

    /**
     * 用 CPU 上的任务状态判断该产物是否还在合成。
     *
     * <p>不能用 AE2 的 {@code isRequesting}：它只看等待补货的原料，最终产物在多步合成的前期一直不在其中。</p>
     */
    private static boolean isCraftInFlight(ICraftingService craftingService, AEItemKey itemKey) {
        for (ICraftingCPU craftingCPU : craftingService.getCpus()) {
            CraftingJobStatus jobStatus = craftingCPU.getJobStatus();
            if (jobStatus != null && itemKey.equals(jobStatus.crafting().what())) return true;
        }
        return false;
    }

    /**
     * 产物已经合成完、却因为网络存不下而卡在某台CPU的内部容器里。
     *
     * <p>这种CPU的{@code isBusy}仍是false，会被当成空闲CPU再次选中，
     * 于是同一单被反复提交、每单都完整消耗原料。只判自己的目标键，避免被别人的满网络连累。
     * 网络满时每种新物品仍会多提交一单并同样卡住，这里只是止住对同一物品的重复提交。</p>
     *
     * <p>判键依赖「{@code isCantStoreItems}为真时{@code job}必为null」这条AE2的实现细节：
     * getAllItems还会塞入等待补货的原料与预期产物，只有job为null时才只剩出口容器。
     * AE2若放宽该判断，这里会把正常在跑的合成误判成卡货。</p>
     */
    private static boolean isProductStuckInCpu(ICraftingService craftingService, AEItemKey itemKey) {
        for (ICraftingCPU craftingCPU : craftingService.getCpus()) {
            if (!(craftingCPU instanceof CraftingCPUCluster cluster)) continue;
            if (!cluster.craftingLogic.isCantStoreItems()) continue;

            KeyCounter stuckItems = new KeyCounter();
            cluster.craftingLogic.getAllItems(stuckItems);
            if (stuckItems.get(itemKey) > 0) return true;
        }
        return false;
    }

    /**
     * 从最多一个元素的容错列表组件里读出一个键，卡槽已不是喂食卡、空列表、元素为null、非物品键与
     * missing-content占位物都按无值处理。
     *
     * <p>元素为null只在伪造包或第三方模组直接写组件时出现：写入侧只会写List.of(new GenericStack(key, 1))，
     * 持久化侧的容错codec把解析失败的元素降级成missing-content占位物而不是null，NBT本身也表达不了null。
     * 这里判空是零成本的防御，不是常规路径。</p>
     */
    @Nullable
    private static AEItemKey readSingleKey(@Nullable ItemStack feedingCardStack,
            DataComponentType<List<GenericStack>> componentType) {
        if (feedingCardStack == null) return null;
        List<GenericStack> entries = feedingCardStack.getOrDefault(componentType, List.of());
        if (entries.isEmpty()) return null;
        GenericStack firstEntry = entries.getFirst();
        if (firstEntry == null) return null;
        if (!(firstEntry.what() instanceof AEItemKey itemKey)) return null;
        return itemKey.is(AEItems.MISSING_CONTENT) ? null : itemKey;
    }

    private static void writePendingCraft(SelectionContext context, @Nullable AEItemKey pendingKey) {
        ItemStack cardStack = context.cardStack();
        if (cardStack == null) return;
        ItemStack updatedCardStack = cardStack.copy();
        if (pendingKey == null) {
            updatedCardStack.remove(AE2FeedingCardDataComponents.FEEDING_PENDING_CRAFT.get());
        } else {
            updatedCardStack.set(AE2FeedingCardDataComponents.FEEDING_PENDING_CRAFT.get(),
                    List.of(new GenericStack(pendingKey, 1)));
        }
        context.terminalUpgrades().setItemDirect(context.feedingCardSlot(), updatedCardStack);
    }

    /**
     * 请求量固定一份，规划交给 AE2 自己的线程池，这里只存 Future 防重入。
     *
     * <p>返回值表示「是否已有一单在途」，它可能是别的目标：卡上已有在途标记时直接返回 {@code true}，
     * 不区分当前目标是否就是那一单。冷却拦截、规划失败、提交失败返回 {@code false}，
     * 调用方据此回落到库存进食，避免合成不成功就一直挨饿。</p>
     */
    private static boolean requestCraft(SelectionContext context, AEItemKey chosenKey, boolean pendingCraftInFlight) {
        if (!context.access().getCardUpgrades().isInstalled(AEItems.CRAFTING_CARD)) return false;
        if (pendingCraftInFlight) return true;

        TerminalState terminalState = context.terminalState();

        ICraftingService craftingService = context.grid().getCraftingService();
        Future<ICraftingPlan> planningFuture = terminalState.getPlanningFuture();
        if (planningFuture == null) {
            if (terminalState.isCraftCooldownActive()) return false;
            if (isCraftInFlight(craftingService, chosenKey)) {
                writePendingCraft(context, chosenKey);
                return true;
            }
            // 产物卡在CPU内部容器时该CPU的isBusy仍是false，会让同一单反复被提交
            if (isProductStuckInCpu(craftingService, chosenKey)) {
                terminalState.setCraftCooldown(AE2FeedingCardServerConfig.CRAFT_RETRY_COOLDOWN_TICKS.get());
                return false;
            }
            startPlanning(context, chosenKey, craftingService);
            return true;
        }
        if (!planningFuture.isDone()) return true;

        ICraftingPlan craftingPlan = awaitPlanningResult(planningFuture);
        if (craftingPlan == null) {
            terminalState.clearPlanning();
            terminalState.setCraftCooldown(AE2FeedingCardServerConfig.CRAFT_RETRY_COOLDOWN_TICKS.get());
            return false;
        }

        return submitPlannedCraft(context, craftingService, chosenKey, craftingPlan);
    }

    private static void startPlanning(SelectionContext context, AEItemKey chosenKey, ICraftingService craftingService) {
        ServerPlayer player = context.player();
        IGrid grid = context.grid();
        // 只带玩家的source没有machine，getGridNode的默认实现会拿到null、样板全部查不到；
        // 把grid的枢轴节点作为machine挂上，默认实现才能取到节点
        IActionSource actionSource = IActionSource.ofPlayer(player, () -> grid.getPivot());
        Future<ICraftingPlan> planningFuture = craftingService.beginCraftingCalculation(
                player.level(),
                () -> actionSource,
                chosenKey,
                1,
                CalculationStrategy.CRAFT_LESS
        );

        context.terminalState().startPlanning(planningFuture);
    }

    /**
     * 只在 {@code planningFuture.isDone()} 之后调用：未完成时 {@code Future.get()} 会阻塞服务端主线程。
     */
    @Nullable
    private static ICraftingPlan awaitPlanningResult(Future<ICraftingPlan> planningFuture) {
        // 与 AE2 MultiCraftingTracker 对齐的既有写法，是本项目唯一允许出现 try/catch 的地方
        try {
            return planningFuture.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException ignored) {
            return null;
        }
    }

    private static boolean submitPlannedCraft(SelectionContext context, ICraftingService craftingService,
            AEItemKey chosenKey, ICraftingPlan craftingPlan) {
        TerminalState terminalState = context.terminalState();
        terminalState.clearPlanning();

        // 不带玩家，任务不记录 playerId，完成时不会给玩家发 CraftingJobStatusPacket
        ICraftingSubmitResult submitResult = craftingService.submitJob(
                craftingPlan,
                null,
                null,
                false,
                IActionSource.empty()
        );
        if (!submitResult.successful()) {
            terminalState.setCraftCooldown(AE2FeedingCardServerConfig.CRAFT_RETRY_COOLDOWN_TICKS.get());
            return false;
        }

        writePendingCraft(context, chosenKey);
        return true;
    }

    private static boolean eat(SelectionContext context, AEItemKey chosenKey) {
        ServerPlayer player = context.player();
        IGrid grid = context.grid();
        IEnergySource energySource = terminalEnergySource(context);

        long extracted = StorageHelper.poweredExtraction(
                energySource,
                grid.getStorageService().getInventory(),
                chosenKey,
                1,
                IActionSource.ofPlayer(player)
        );
        if (extracted != 1) return false;

        if (context.access().getCardUpgrades().isInstalled(AEItems.EQUAL_DISTRIBUTION_CARD)) {
            writeCursor(context, chosenKey);
        }

        // 音效开关只影响喂食卡自己这一次调用，置位—进食—清除顺序执行
        FeedingCardRuntimeData runtimeData = context.runtimeData();
        runtimeData.setSuppressingEatSound(!context.access().readSettings().playEatSound());

        // 与原版 completeUsingItem 同序：先 finishUsingItem，再用使用前的副本跑 onItemUseFinish 并取事件返回值
        ItemStack usedStack = chosenKey.toStack(1);
        int usedCount = usedStack.getCount();
        ItemStack eventCopy = usedStack.copy();
        ItemStack finishedStack = usedStack.finishUsingItem(player.level(), player);
        // 普通食物进食返回同一实例、只是数量被consume扣掉，带容器的返回新的碗，
        // 只有getFoodProperties为null时实例与数量都不变，那才是真的什么都没吃到
        boolean consumed = finishedStack != usedStack || finishedStack.getCount() < usedCount;
        ItemStack remainder = EventHooks.onItemUseFinish(
                player,
                eventCopy,
                0,
                finishedStack
        );

        runtimeData.setSuppressingEatSound(false);

        // 没吃到说明食物原样没被消耗，按余物规则处理而不是让它凭空消失。
        // 装虚空卡且网络与背包都放不进时仍会按余物策略销毁，这条路径现在几乎不可达，
        // 兜底的意义是「至少不会在玩家不知情时丢物品」而不是绝对不丢
        if (consumed) {
            if (!remainder.isEmpty()) {
                handleRemainder(player, grid, energySource, context.access().getCardUpgrades(),
                        context.access().readSettings(), remainder);
            }
        } else {
            handleRemainder(player, grid, energySource, context.access().getCardUpgrades(),
                    context.access().readSettings(), finishedStack);
        }

        return consumed;
    }

    private static void writeCursor(SelectionContext context, AEItemKey chosenKey) {
        ItemStack cardStack = context.cardStack();
        if (cardStack == null) return;
        ItemStack updatedCardStack = cardStack.copy();
        updatedCardStack.set(AE2FeedingCardDataComponents.FEEDING_CURSOR.get(),
                List.of(new GenericStack(chosenKey, 1)));
        context.terminalUpgrades().setItemDirect(context.feedingCardSlot(), updatedCardStack);
    }

    /**
     * 余物按设置顺序处理：先进网络、再进背包、有虚空卡且开了开关则丢弃，仍未处理掉的直接掉在脚下。
     */
    private static void handleRemainder(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IUpgradeInventory cardUpgrades, FeedingCardSettings settings, ItemStack remainder) {
        int remaining = remainder.getCount();

        if (settings.remainderToNetwork() && remaining > 0) {
            long inserted = StorageHelper.poweredInsert(
                    energySource,
                    grid.getStorageService().getInventory(),
                    AEItemKey.of(remainder),
                    remaining,
                    IActionSource.ofPlayer(player)
            );
            remaining -= (int) inserted;
        }

        if (settings.remainderToInventory() && remaining > 0) {
            ItemStack inventoryStack = remainder.copyWithCount(remaining);
            player.getInventory().add(inventoryStack);
            remaining = inventoryStack.getCount();
        }

        if (remaining <= 0) return;
        if (settings.voidOverflow() && cardUpgrades.isInstalled(AEItems.VOID_CARD)) return;

        player.spawnAtLocation(remainder.copyWithCount(remaining));
    }
}
