package net.zhaiji.ae2feedingcard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.zhaiji.ae2feedingcard.init.AE2FeedingCardAttachmentTypes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 拦下玩家进食后的打嗝音，喂食卡关掉音效开关时那一次调用不发声。
 *
 * <p>用WrapOperation而不是Redirect：别的模组也包同一个playSound调用时，Redirect会互斥导致注入失败，
 * 而本模组是required:true，注入失败就是启动崩溃。</p>
 */
@Mixin(Player.class)
public abstract class PlayerEatSoundMixin {
    @WrapOperation(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
            )
    )
    public void ae2feedingcard$eat(Level level, @Nullable Player player, double x, double y, double z, SoundEvent sound,
            SoundSource source, float volume, float pitch, Operation<Void> original) {
        if (ae2feedingcard$self().getData(AE2FeedingCardAttachmentTypes.FEEDING_CARD_RUNTIME_STATE).isSuppressingEatSound()) {
            return;
        }

        original.call(level, player, x, y, z, sound, source, volume, pitch);
    }

    @Unique
    private Player ae2feedingcard$self() {
        return (Player) (Object) this;
    }
}
