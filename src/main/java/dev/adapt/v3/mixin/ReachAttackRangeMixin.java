package dev.adapt.v3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.adapt.v3.feature.module.impl.combat.ReachModule;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class ReachAttackRangeMixin {
    @ModifyExpressionValue(
        method = "raycastHitResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
    )
    private Object adapt$widenRaycastRange(Object component) {
        return ReachModule.widenAttackRange(component);
    }
}
