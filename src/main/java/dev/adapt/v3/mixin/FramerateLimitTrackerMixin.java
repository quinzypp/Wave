package dev.adapt.v3.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.FramerateLimitTracker.FramerateThrottleReason;
import dev.adapt.v3.ui.screen.BaseScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
    @Shadow
    private int framerateLimit;

    @Shadow
    public abstract FramerateThrottleReason getThrottleReason();

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void adapt$uncapMenus(CallbackInfoReturnable<Integer> cir) {
        if (this.getThrottleReason() == FramerateThrottleReason.OUT_OF_LEVEL_MENU && Minecraft.getInstance().gui.screen() instanceof BaseScreen) {
            cir.setReturnValue(this.framerateLimit);
        }
    }
}
