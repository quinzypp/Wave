package dev.adapt.v3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.adapt.v3.feature.module.impl.combat.ReachModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class ReachAttackMixin {
    @ModifyExpressionValue(
        method = "startAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
    )
    private Object adapt$widenAttackRange(Object component) {
        return ReachModule.widenAttackRange(component);
    }
}
