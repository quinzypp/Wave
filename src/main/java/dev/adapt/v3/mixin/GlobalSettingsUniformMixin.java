package dev.adapt.v3.mixin;

import dev.adapt.v3.ui.render.BackdropBlur;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlobalSettingsUniform.class)
public abstract class GlobalSettingsUniformMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void adaptv3$capture(
        int width, int height, double glintAlpha, long gameTime, float worldPartialTicks, int menuBlurRadius, Vec3 cameraPos, boolean useRgss, CallbackInfo ci
    ) {
        BackdropBlur.captureGlobalSettings(
            (GlobalSettingsUniform)(Object)this, width, height, glintAlpha, gameTime, worldPartialTicks, menuBlurRadius, cameraPos, useRgss
        );
    }
}
