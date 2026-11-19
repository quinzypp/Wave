package dev.adapt.v3.mixin;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.module.impl.visual.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void adapt$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (Adapt.getInstance() != null) {
            ZoomModule zoommodule = Adapt.getInstance().getModuleManager().getByClass(ZoomModule.class);
            cir.setReturnValue(zoommodule.modifyFov(cir.getReturnValue()));
        }
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float adapt$cameraYaw(Entity instance, float partialTicks) {
        return instance == Minecraft.getInstance().player
            ? RotationHelper.getState().getCameraYaw(instance.getViewYRot(partialTicks))
            : instance.getViewYRot(partialTicks);
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float adapt$cameraPitch(Entity instance, float partialTicks) {
        return instance == Minecraft.getInstance().player
            ? RotationHelper.getState().getCameraPitch(instance.getViewXRot(partialTicks))
            : instance.getViewXRot(partialTicks);
    }
}
