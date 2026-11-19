package dev.adapt.v3.util;

import dev.adapt.v3.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;

public final class PacketUtility {
    public static final Minecraft mc = Minecraft.getInstance();
    public static boolean sendingSilent;

    public static void sendNoEvent(Packet packet) {
        if (mc.getConnection() != null) {
            sendingSilent = true;

            try {
                mc.getConnection().getConnection().send(packet);
            } finally {
                sendingSilent = false;
            }
        }
    }

    public static void send(Packet packet) {
        if (mc.getConnection() != null) {
            mc.getConnection().send(packet);
        }
    }

    public static void sendPredicted(PredictiveAction predictiveAction) {
        if (mc.gameMode != null && mc.level != null) {
            ((MultiPlayerGameModeAccessor)mc.gameMode).adapt$startPrediction(mc.level, predictiveAction);
        }
    }

    public static boolean isSendingSilent() {
        return sendingSilent;
    }

    public record TimedPacket(Packet<?> packet, long timestamp) {
    }
}
