package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;

public final class VelocityUpdateEvent extends CancellableEvent {
    public double x;
    public double y;
    public double z;
    public final boolean explosion;

    public VelocityUpdateEvent(double value, double value2, double value3, boolean flag) {
        this.x = value;
        this.y = value2;
        this.z = value3;
        this.explosion = flag;
    }

    public boolean isExplosion() {
        return this.explosion;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
