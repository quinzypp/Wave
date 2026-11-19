package dev.adapt.v3.mixin;

import dev.adapt.v3.util.player.ChatUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void adapt$runnableGameClick(ClickEvent event, Minecraft mc, Screen screen, CallbackInfo ci) {
        if (event instanceof ChatUtility.RunnableClick chatutility$runnableclick) {
            chatutility$runnableclick.runnable().run();
            ci.cancel();
        }
    }

    @Inject(method = "defaultHandleClickEvent", at = @At("HEAD"), cancellable = true)
    private static void adapt$runnableClick(ClickEvent event, Minecraft mc, Screen screen, CallbackInfo ci) {
        if (event instanceof ChatUtility.RunnableClick chatutility$runnableclick) {
            chatutility$runnableclick.runnable().run();
            ci.cancel();
        }
    }
}
