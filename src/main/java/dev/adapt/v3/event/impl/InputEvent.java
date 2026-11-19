package dev.adapt.v3.event.impl;

public class InputEvent {
    public final int code;

    public InputEvent(int value) {
        this.code = value;
    }

    public int getCode() {
        return this.code;
    }
}
