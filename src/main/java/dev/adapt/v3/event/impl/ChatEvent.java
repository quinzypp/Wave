package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;
import net.minecraft.network.chat.Component;

public final class ChatEvent extends CancellableEvent {
    public final Component message;
    public boolean overlay;

    public ChatEvent(Component component, boolean flag) {
        this.message = component;
        this.overlay = flag;
    }

    public Component getMessage() {
        return this.message;
    }

    public boolean isOverlay() {
        return this.overlay;
    }

    public void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }
}
