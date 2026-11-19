package dev.adapt.v3.binding;

import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.adapt.v3.binding.type.InputType;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.KeyEvent;
import dev.adapt.v3.event.impl.MousePressEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;

public final class BindingManager implements Listener {
    public final BindRegistry registry = new BindRegistry();

    public BindingManager() {
        EventManager.register(this);
    }

    public BindRegistry getRegistry() {
        return this.registry;
    }

    public String getBindName(Bindable bindable) {
        return this.registry.getBind(bindable).map(BindingManager::formatBind).orElse(null);
    }

    public static String formatBind(Bind bind) {
        try {
            Type type = bind.type() == InputType.MOUSE ? Type.MOUSE : Type.KEYBOARD;
            return type.getOrCreate(bind.code()).getDisplayName().getString();
        } catch (Throwable throwable) {
            return (bind.type() == InputType.MOUSE ? "Mouse " : "Key ") + bind.code();
        }
    }

    @Subscribe
    public void onKey(KeyEvent event) {
        this.registry.trigger(event.getCode(), InputType.KEYBOARD);
    }

    @Subscribe
    public void onMousePress(MousePressEvent event) {
        this.registry.trigger(event.getCode(), InputType.MOUSE);
    }
}
