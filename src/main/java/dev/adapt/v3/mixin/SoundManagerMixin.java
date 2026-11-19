package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.module.impl.visual.NoSwingSoundModule;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void adaptv3$noSwingSound(SoundInstance instance, CallbackInfoReturnable<PlayResult> cir) {
        if (NoSwingSoundModule.shouldCancelSound(instance)) {
            cir.setReturnValue(PlayResult.NOT_STARTED);
        }
    }
}
