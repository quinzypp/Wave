package dev.adapt.v3.event.impl;

public final class MouseUpdateEvent {
    public double deltaX;
    public double deltaY;
    public final double sensitivityMultiplier;
    public final boolean unlockedCursor;
    public boolean modified;

    public MouseUpdateEvent(double value, double value2, double value3, boolean flag) {
        this.sensitivityMultiplier = value3;
        this.deltaY = value2;
        this.deltaX = value;
        this.unlockedCursor = flag;
    }

    public double getDeltaX() {
        return this.deltaX;
    }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }

    public double getDeltaY() {
        return this.deltaY;
    }

    public void setDeltaY(double deltaY) {
        this.deltaY = deltaY;
    }

    public double getSensitivityMultiplier() {
        return this.sensitivityMultiplier;
    }

    public boolean isModified() {
        return this.modified;
    }

    public void markModified() {
        this.modified = true;
    }

    public boolean isUnlockedCursor() {
        return this.unlockedCursor;
    }
}
