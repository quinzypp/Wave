package dev.adapt.v3.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.adapt.v3.feature.module.impl.combat.PiercingModule;
import dev.adapt.v3.util.player.RaytraceUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPiercingMixin {
    @Unique
    private static boolean adapt$passThroughBlocks;

    @Redirect(
        method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;")
    )
    private static Type adapt$redirectBlockHitResultType(HitResult instance) {
        if (adapt$passThroughBlocks) {
            adapt$passThroughBlocks = false;
            return Type.MISS;
        } else {
            return instance.getType();
        }
    }

    @Redirect(
        method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", ordinal = 0)
    )
    private static double adapt$redirectPassedThroughBlockDistance(
        Vec3 instance, Vec3 vec, @Local(ordinal = 1, argsOnly = true) double entityInteractionRange, @Local(argsOnly = true) float tickDelta
    ) {
        PiercingModule piercingmodule = PiercingModule.getInstance();
        Minecraft minecraft = Minecraft.getInstance();
        if (piercingmodule != null && piercingmodule.isEnabled() && minecraft.player != null) {
            HitResult hitresult = RaytraceUtility.raytraceEntity(
                entityInteractionRange, tickDelta, minecraft.player.getYRot(), minecraft.player.getXRot(), entity -> true
            );
            if (hitresult != null && hitresult.getType() == Type.ENTITY) {
                adapt$passThroughBlocks = true;
                return Double.MAX_VALUE;
            }
        }

        return instance.distanceToSqr(vec);
    }
}
