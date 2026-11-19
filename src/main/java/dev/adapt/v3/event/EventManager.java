package dev.adapt.v3.event;

import dev.adapt.v3.event.system.EventBus;

public final class EventManager {
    public static final EventBus BUS = new EventBus();

    public static void register(Object key) {
        BUS.subscribe(key);
    }

    public static <T> T post(T value) {
        BUS.post(value);
        return (T)value;
    }
}
