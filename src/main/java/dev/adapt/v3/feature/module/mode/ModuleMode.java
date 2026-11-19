package dev.adapt.v3.feature.module.mode;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.feature.module.Module;

public abstract class ModuleMode<T extends Module> implements Listener {
    public final T module;
    public boolean active;

    public ModuleMode(T value) {
        this.module = (T)value;
        EventManager.register(this);
    }

    public T getModule() {
        return this.module;
    }

    public void onEnable() {
        this.active = true;
    }

    public void onDisable() {
        this.active = false;
    }

    @Override
    public boolean isListening() {
        return this.active;
    }

    public abstract Enum<?> getMode();
}
