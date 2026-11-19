package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;
import dev.adapt.v3.feature.module.Module;

public final class ModuleToggleEvent extends CancellableEvent {
    public final Module module;
    public final boolean enabled;

    public ModuleToggleEvent(Module module, boolean flag) {
        this.module = module;
        this.enabled = flag;
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}
