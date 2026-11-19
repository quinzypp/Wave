package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import dev.adapt.v3.feature.module.impl.visual.NametagsModule;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void adapt$fakeSwordBlockPose(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (NametagsModule.shouldSuppressVanillaNametag(entity)) {
            state.nameTag = null;
            state.scoreText = null;
        }

        AnimationsModule animationsmodule = AnimationsModule.getInstance();
        if (animationsmodule != null
            && animationsmodule.isEnabled()
            && state.mainArm == HumanoidArm.RIGHT
            && AnimationsModule.shouldHideOffhandShield(entity.getOffhandItem())) {
            state.leftArmPose = ArmPose.EMPTY;
            if (animationsmodule.isFakeSwordBlocking(entity)) {
                state.rightArmPose = ArmPose.BLOCK;
            }
        }
    }
}
