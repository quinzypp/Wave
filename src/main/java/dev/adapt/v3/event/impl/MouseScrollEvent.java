package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;

public final class MouseScrollEvent extends CancellableEvent {
    public final double verticalAmount;

    public MouseScrollEvent(double value) {
        this.verticalAmount = value;
    }

    public double getVerticalAmount() {
        return this.verticalAmount;
    }
}
