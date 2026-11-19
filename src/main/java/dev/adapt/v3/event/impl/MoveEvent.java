package dev.adapt.v3.event.impl;

public final class MoveEvent {
    public double x;
    public double y;
    public double z;

    public MoveEvent(double value, double value2, double value3) {
        this.x = value;
        this.y = value2;
        this.z = value3;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
