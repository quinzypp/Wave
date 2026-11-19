package dev.adapt.v3.mixin;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.impl.world.WorldEditorModule;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void adapt$fullbright(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        if (Adapt.getInstance() != null) {
            float f = Adapt.getInstance().getModuleManager().getByClass(WorldEditorModule.class).getBrightnessFactor();
            if (f > 0.001F) {
                state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, f);
                state.brightness = Math.max(state.brightness, f);
                state.needsUpdate = true;
            }
        }
    }
}
