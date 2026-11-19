package dev.adapt.v3.event.impl;

public final class PreMovementPacketEvent {
    public float yaw;
    public float pitch;
    public boolean sprinting;
    public boolean onGround;

    public PreMovementPacketEvent(float first, float second, boolean flag, boolean flag2) {
        this.yaw = first;
        this.pitch = second;
        this.sprinting = flag;
        this.onGround = flag2;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
}
