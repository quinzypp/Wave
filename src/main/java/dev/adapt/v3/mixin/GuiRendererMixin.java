package dev.adapt.v3.mixin;

import dev.adapt.v3.ui.render.BackdropBlur;
import dev.adapt.v3.ui.render.LogoReveal;
import dev.adapt.v3.ui.render.MaskOutline;
import dev.adapt.v3.ui.render.MenuNebula;
import dev.adapt.v3.util.Profiler;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Unique
    private long adapt$drawStart;

    @Inject(method = "draw", at = @At("HEAD"))
    private void adapt$backdrop(CallbackInfo ci) {
        Profiler.onFrame();
        long i = Profiler.beginSection();
        MaskOutline.flush();
        Profiler.endSection("esp", i);
        MenuNebula.flush();
        i = Profiler.beginSection();
        BackdropBlur.process();
        Profiler.endSection("blur", i);
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void adapt$loadingScreen(CallbackInfo ci) {
        LogoReveal.flush();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void adapt$renderStart(CallbackInfo ci) {
        this.adapt$drawStart = Profiler.beginSection();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void adapt$renderEnd(CallbackInfo ci) {
        Profiler.endSection("gui", this.adapt$drawStart);
    }
}
