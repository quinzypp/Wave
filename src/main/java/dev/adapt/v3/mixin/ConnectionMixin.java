package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.impl.SendPacketEvent;
import dev.adapt.v3.util.PacketUtility;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    private static void genericsFtw(Packet packet, PacketListener listener) {
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void adapt$onSend(Packet packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (!PacketUtility.isSendingSilent()) {
            SendPacketEvent sendpacketevent = new SendPacketEvent(packet);
            EventManager.post(sendpacketevent);
            if (sendpacketevent.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static void adapt$onReceive(Packet packet, PacketListener listener, CallbackInfo ci) {
        if (listener instanceof ClientPacketListener) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.isSameThread()) {
                minecraft.execute(() -> genericsFtw(packet, listener));
                ci.cancel();
            } else if (!(packet instanceof ClientboundBundlePacket clientboundbundlepacket)) {
                ReceivePacketEvent receivepacketevent = new ReceivePacketEvent(packet);
                EventManager.post(receivepacketevent);
                if (receivepacketevent.isCancelled()) {
                    ci.cancel();
                }
            } else {
                ci.cancel();

                for (Packet<?> packetx : clientboundbundlepacket.subPackets()) {
                    try {
                        genericsFtw(packetx, listener);
                    } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
                    }
                }
            }
        }
    }
}
