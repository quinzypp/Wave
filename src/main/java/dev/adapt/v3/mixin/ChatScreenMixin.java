package dev.adapt.v3.mixin;

import dev.adapt.v3.Adapt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void adapt$handleChatInput(String message, boolean addToRecentChat, CallbackInfo ci) {
        Adapt adapt = Adapt.getInstance();
        if (adapt != null) {
            String s = message.trim();
            if (adapt.getCommandManager().handleChatInput(s)) {
                if (addToRecentChat) {
                    Minecraft.getInstance().gui.hud.getChat().addRecentChat(s);
                }

                ci.cancel();
            }
        }
    }
}
