package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

public final class SendPacketEvent extends CancellableEvent {
    public final Packet packet;

    public SendPacketEvent(Packet packet) {
        this.packet = packet;
    }

    public Packet getPacket() {
        return this.packet;
    }

    @Override
    public boolean shouldStopPropagation() {
        return false;
    }
}
