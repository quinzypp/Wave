package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.module.impl.combat.ReachModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerReachMixin {
    @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void adapt$hookEntityReach(CallbackInfoReturnable<Double> cir) {
        if ((Object)this instanceof LocalPlayer) {
            ReachModule reachmodule = ReachModule.getInstance();
            if (reachmodule != null && reachmodule.isEnabled()) {
                cir.setReturnValue(reachmodule.getEntityReach());
            }
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void adapt$hookBlockReach(CallbackInfoReturnable<Double> cir) {
        if ((Object)this instanceof LocalPlayer) {
            ReachModule reachmodule = ReachModule.getInstance();
            if (reachmodule != null && reachmodule.isEnabled()) {
                cir.setReturnValue(reachmodule.getBlockReach());
            }
        }
    }
}
