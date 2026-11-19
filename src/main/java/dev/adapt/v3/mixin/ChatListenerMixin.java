package dev.adapt.v3.mixin;

import com.mojang.authlib.GameProfile;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.ChatEvent;
import java.time.Instant;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.ChatType.Bound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @Unique
    private static boolean adapt$postChatEvent(Component message, boolean overlay) {
        return EventManager.post(new ChatEvent(message, overlay)).isCancelled();
    }

    @Inject(
        method = "showMessageToPlayer",
        cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;getChat()Lnet/minecraft/client/gui/components/ChatComponent;", ordinal = 0)
    )
    private void adapt$onSignedChat(
        Bound params,
        PlayerChatMessage message,
        Component decorated,
        GameProfile sender,
        boolean onlyShowSecureChat,
        Instant received,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (adapt$postChatEvent(decorated, false)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "showMessageToPlayer",
        cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;getChat()Lnet/minecraft/client/gui/components/ChatComponent;", ordinal = 1)
    )
    private void adapt$onFilteredSignedChat(
        Bound params,
        PlayerChatMessage message,
        Component decorated,
        GameProfile sender,
        boolean onlyShowSecureChat,
        Instant received,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Component component = message.filterMask().applyWithFormatting(message.signedContent());
        if (component != null && adapt$postChatEvent(params.decorate(component), false)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "lambda$handleDisguisedChatMessage$0",
        cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;getChat()Lnet/minecraft/client/gui/components/ChatComponent;")
    )
    private void adapt$onDisguisedChat(Bound params, Component content, Instant received, CallbackInfoReturnable<Boolean> cir) {
        if (adapt$postChatEvent(params.decorate(content), false)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void adapt$onSystemMessage(Component message, boolean remote, CallbackInfo ci) {
        if (adapt$postChatEvent(message, false)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleOverlay", at = @At("HEAD"), cancellable = true)
    private void adapt$onOverlayMessage(Component message, CallbackInfo ci) {
        if (adapt$postChatEvent(message, true)) {
            ci.cancel();
        }
    }
}
