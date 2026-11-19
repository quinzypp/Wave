package dev.adapt.v3.event.system;

public interface Listener {
    default boolean isListening() {
        return true;
    }
}
