package dev.adapt.v3.event.impl;

public final class JumpEvent {
    public boolean sprinting;

    public JumpEvent(boolean flag) {
        this.sprinting = flag;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }
}
