package dev.adapt.v3.mixin;

import dev.adapt.v3.ui.render.LogoReveal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private ReloadInstance reload;
    @Shadow
    @Final
    private boolean fadeIn;
    @Shadow
    private float currentProgress;
    @Shadow
    private long fadeOutStart;
    @Shadow
    private long fadeInStart;
    private static final float BAR_OUT = 0.4F;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void adapt$reveal(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (LogoReveal.isAvailable()) {
            ci.cancel();
            long i = Util.getMillis();
            if (this.fadeIn && this.fadeInStart == -1L) {
                this.fadeInStart = i;
            }

            float f = this.fadeOutStart > -1L ? (float)(i - this.fadeOutStart) / 1000.0F : -1.0F;
            float f1 = this.fadeInStart > -1L ? (float)(i - this.fadeInStart) / 500.0F : -1.0F;
            float f2 = this.reload.getActualProgress();
            this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + f2 * 0.050000012F, 0.0F, 1.0F);
            if (f >= 0.4F) {
                if (this.minecraft.gui.screen() != null) {
                    this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, 0, 0, a);
                } else {
                    this.minecraft.gui.hud.extractDeferredSubtitles();
                }

                float f5 = f - 0.4F;
                if (LogoReveal.hasTargetLogo()) {
                    float f6 = smooth(0.62F, 0.8F, f5);
                    LogoReveal.submitWithHandoff(
                        this, 1.0F - smooth(0.0F, 0.55F, f5), this.currentProgress, 0.0F, LogoReveal.criticallyDampedSpring(f5, 0.65F), 1.0F - f6, f6
                    );
                    if (f5 >= 0.85F) {
                        this.minecraft.gui.setOverlay(null);
                    }
                } else {
                    LogoReveal.submit(this, 1.0F - Mth.clamp(f5, 0.0F, 1.0F), this.currentProgress, 0.0F);
                    if (f5 >= 1.0F) {
                        this.minecraft.gui.setOverlay(null);
                    }
                }
            } else {
                float f3;
                if (this.fadeIn && f1 < 1.0F) {
                    if (this.minecraft.gui.screen() != null) {
                        this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a);
                    } else {
                        this.minecraft.gui.hud.extractDeferredSubtitles();
                    }

                    f3 = Mth.clamp(f1, 0.0F, 1.0F);
                } else {
                    this.minecraft.gui.hud.extractDeferredSubtitles();
                    f3 = 1.0F;
                }

                float f4 = f < 0.0F ? 1.0F : 1.0F - Mth.clamp(f / 0.4F, 0.0F, 1.0F);
                LogoReveal.submit(this, f3, this.currentProgress, f4);
            }
        }
    }

    private static float smooth(float lo, float hi, float x) {
        float f = Mth.clamp((x - lo) / (hi - lo), 0.0F, 1.0F);
        return f * f * (3.0F - 2.0F * f);
    }

    @Inject(method = "isReadyToFadeOut", at = @At("RETURN"), cancellable = true)
    private void adapt$waitForReveal(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !LogoReveal.isRevealComplete(this)) {
            cir.setReturnValue(false);
        }
    }
}
