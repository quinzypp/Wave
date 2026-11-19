package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;

public final class SlowdownEvent extends CancellableEvent {
    public float multiplier;

    public SlowdownEvent(float value) {
        this.multiplier = value;
    }

    public float getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }
}
