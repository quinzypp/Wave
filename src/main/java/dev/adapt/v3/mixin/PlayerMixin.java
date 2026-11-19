package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.module.impl.combat.keepsprint.KeepSprintModule;
import dev.adapt.v3.feature.module.impl.combat.keepsprint.mode.VanillaKeepSprint;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getMaxHeadRotationRelativeToBody", at = @At("HEAD"), cancellable = true)
    private void adapt$blockHeadTurn(CallbackInfoReturnable<Float> cir) {
        AnimationsModule animationsmodule = AnimationsModule.getInstance();
        Player player = (Player)(Object)this;
        if (animationsmodule != null && animationsmodule.isEnabled() && player.isBlocking()) {
            cir.setReturnValue(50.0F);
        }
    }

    @Redirect(
        method = "causeExtraKnockback",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 adapt$keepSprintMotion(Vec3 motion, double x, double y, double z) {
        VanillaKeepSprint vanillakeepsprint = this.adapt$keepSprint();
        if (vanillakeepsprint == null) {
            return motion.multiply(x, y, z);
        }

        double d0 = vanillakeepsprint.getMultiplier();
        return motion.multiply(d0, y, d0);
    }

    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void adapt$keepSprintState(Player instance, boolean sprinting) {
        if (this.adapt$keepSprint() == null) {
            instance.setSprinting(sprinting);
        }
    }

    @Unique
    private VanillaKeepSprint adapt$keepSprint() {
        return (Object)this == Minecraft.getInstance().player ? KeepSprintModule.getActiveVanillaMode() : null;
    }
}
