package dev.adapt.v3.feature.module.impl.client;

import dev.adapt.v3.feature.module.HiddenModule;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;

public final class NotificationsModule extends Module implements HiddenModule {
    public final BooleanProperty moduleToggles = new BooleanProperty("Module Toggles", true);

    public NotificationsModule() {
        super("Notifications", "Spring-animated toasts with live progress bars.", ModuleCategory.UTILITY);
        this.addProperties(this.moduleToggles);
    }
}
