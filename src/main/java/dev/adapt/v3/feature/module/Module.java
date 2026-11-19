package dev.adapt.v3.feature.module;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.binding.Bindable;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.ModuleToggleEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.feature.property.Configurable;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.notification.NotificationType;
// VENDOR-DISABLED: import dev.adapt.v3.security.LaunchGuard;
import dev.adapt.v3.util.animation.Spring;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class Module implements Bindable, Configurable, Listener {
    protected static final Minecraft mc = Minecraft.getInstance();
    public final String name;
    public final String description;
    public final String id;
    public final ModuleCategory category;
    public boolean enabled;
    public final List<Property<?>> properties = new ArrayList<>();
    public final Spring toggleSpring = Spring.smooth(0.0F);
    public String propertyGroup;

    public Module(String text, String text2, ModuleCategory moduleCategory) {
        this.name = text;
        this.id = text.toLowerCase().replace(' ', '_');
        this.description = text2;
        this.category = moduleCategory;
        EventManager.register(this);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            if (enabled || this.canToggle()) {
                if (true) { // VENDOR-DISABLED: if (!enabled || !this.canToggle() || LaunchGuard.isAuthorised())
                    ModuleToggleEvent moduletoggleevent = new ModuleToggleEvent(this, enabled);
                    EventManager.post(moduletoggleevent);
                    if (!moduletoggleevent.isCancelled()) {
                        this.enabled = enabled;
                        if (enabled) {
                            this.onEnable();
                        } else {
                            this.onDisable();
                        }
                    }
                }
            }
        }
    }

    public boolean canToggle() {
        return true;
    }

    public final void toggle() {
        if (this.canToggle()) {
            this.setEnabled(!this.isEnabled());
            if (Adapt.getInstance() != null) {
                Adapt.getInstance().notifyModuleToggle(this);
            }
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public final String getName() {
        return this.name;
    }

    public final String getId() {
        return this.id;
    }

    public final String getDescription() {
        return this.description;
    }

    public final ModuleCategory getCategory() {
        return this.category;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    public final void addProperties(Property<?>... properties) {
        for (Property property : properties) {
            if (property != null) {
                if (this.propertyGroup != null && property.getDescription() == null) {
                    property.describe(this.propertyGroup);
                }

                this.properties.add(property);
            }
        }
    }

    public final void setPropertyGroup(String propertyGroup) {
        this.propertyGroup = propertyGroup;
    }

    public String getSuffix() {
        return null;
    }

    @Override
    public final List<Property<?>> getProperties() {
        return this.properties;
    }

    @Override
    public void onBind() {
        this.toggle();
    }

    @Override
    public final boolean isListening() {
        return this.enabled;
    }

    public NotificationType getNotificationType() {
        return this.enabled ? NotificationType.SUCCESS : NotificationType.DISABLED;
    }
}
