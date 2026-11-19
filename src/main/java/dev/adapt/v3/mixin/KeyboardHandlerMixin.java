package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void adapt$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1 && handle == Minecraft.getInstance().getWindow().handle()) {
            EventManager.post(new dev.adapt.v3.event.impl.KeyEvent(event.key()));
        }
    }
}
