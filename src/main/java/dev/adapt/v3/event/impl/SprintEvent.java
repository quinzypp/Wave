package dev.adapt.v3.event.impl;

public final class SprintEvent {
    public boolean sprinting;

    public SprintEvent(boolean flag) {
        this.sprinting = flag;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }
}
