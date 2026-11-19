package dev.adapt.v3.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.adapt.v3.feature.module.impl.visual.PerspectiveModule;
import dev.adapt.v3.util.render.ProjectionUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    private void bobHurt(CameraRenderState cameraState, PoseStack poseStack) {
    }

    @Shadow
    private void bobView(CameraRenderState cameraState, PoseStack poseStack) {
    }

    @Inject(method = "extractOptions", at = @At("TAIL"))
    private void adapt$perspective(CallbackInfo ci) {
        PerspectiveModule perspectivemodule = PerspectiveModule.getInstance();
        if (perspectivemodule != null) {
            OptionsRenderState optionsrenderstate = this.gameRenderState.optionsRenderState;
            optionsrenderstate.bobView = perspectivemodule.modifyBobView(optionsrenderstate.bobView);
            optionsrenderstate.damageTiltStrength = (float)perspectivemodule.modifyDamageTilt(optionsrenderstate.damageTiltStrength);
        }
    }

    @Inject(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V")
    )
    private void adapt$captureCamera(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (!this.gameRenderState.shouldRenderLevel) {
            ProjectionUtility.invalidate();
        } else {
            LevelRenderState levelrenderstate = this.gameRenderState.levelRenderState;
            CameraRenderState camerarenderstate = levelrenderstate.cameraRenderState;
            PlayerRenderState playerrenderstate = levelrenderstate.playerRenderState;
            Matrix4f matrix4f = new Matrix4f(camerarenderstate.projectionMatrix);
            PoseStack posestack = new PoseStack();
            this.bobHurt(camerarenderstate, posestack);
            if (this.gameRenderState.optionsRenderState.bobView) {
                this.bobView(camerarenderstate, posestack);
            }

            matrix4f.mul(posestack.last().pose());
            float f = this.gameRenderState.optionsRenderState.screenEffectScale;
            float f1 = Math.max(playerrenderstate.portalEffectIntensity, playerrenderstate.nauseaEffectIntensity) * (f * f);
            if (f1 > 0.0F) {
                float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
                f2 *= f2;
                Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
                float f3 = playerrenderstate.spinningEffectAngle * (float) (Math.PI / 180.0);
                matrix4f.rotate(f3, vector3f);
                matrix4f.scale(1.0F / f2, 1.0F, 1.0F);
                matrix4f.rotate(-f3, vector3f);
            }

            ProjectionUtility.capture(matrix4f, camerarenderstate.viewRotationMatrix, camerarenderstate.pos, deltaTracker.getGameTimeDeltaPartialTick(false));
        }
    }
}
