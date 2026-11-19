package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import dev.adapt.v3.feature.module.impl.visual.animations.Animations17;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow
    @Final
    public ModelPart rightArm;
    @Shadow
    @Final
    public ModelPart head;

    @Inject(method = "poseRightArm(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void adapt$blockingArm17(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState
            && state.rightArmPose == ArmPose.BLOCK
            && !(state.rightHandItemStack.getItem() instanceof ShieldItem)
            && AnimationsModule.isActive()) {
            this.rightArm.xRot = Animations17.computeBlockingArmPitch(this.rightArm.xRot);
            this.rightArm.yRot = 0.0F;
            ci.cancel();
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void adapt$blockingArm17Final(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState
            && state.rightArmPose == ArmPose.BLOCK
            && !(state.rightHandItemStack.getItem() instanceof ShieldItem)
            && !(state.swimAmount > 0.0F)
            && AnimationsModule.isActive()) {
            float[] afloat = Animations17.computeThirdPersonBlockingArmAngles(
                state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.swingAnimation, this.head.xRot, state.isPassenger, state.isCrouching
            );
            this.rightArm.xRot = afloat[0];
            this.rightArm.yRot = afloat[1];
            this.rightArm.zRot = afloat[2];
        }
    }
}
