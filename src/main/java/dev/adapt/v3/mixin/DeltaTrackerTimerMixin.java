package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.helper.timer.TimerHelper;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Timer.class)
public abstract class DeltaTrackerTimerMixin {
    @Redirect(method = "advanceGameTime", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float adapt$timer(FloatUnaryOperator targetMsptProvider, float msPerTick) {
        return targetMsptProvider.apply(msPerTick) / TimerHelper.getInstance().getTimerSpeed();
    }
}
