package dev.adapt.v3.feature.module.impl.movement.noslow;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.movement.noslow.mode.NcpNoSlow;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class NoSlowModule extends Module {
    public final EnumProperty<NoSlowModule.Mode> mode = new EnumProperty<>("Mode", NoSlowModule.Mode.NCP);
    public final BooleanProperty allowSprinting = new BooleanProperty("Allow sprinting", true);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public NoSlowModule() {
        super("No Slow", "Removes the slowdown from using items.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode, this.allowSprinting);
        this.registerModes(new NcpNoSlow(this));
    }

    public static boolean canSprintWhileUsing() {
        Adapt adapt = Adapt.getInstance();
        NoSlowModule noslowmodule = adapt == null ? null : adapt.getModuleManager().getByClass(NoSlowModule.class);
        return noslowmodule != null && noslowmodule.isEnabled() && noslowmodule.allowSprinting.getValue();
    }

    @SafeVarargs
    public final void registerModes(ModuleMode<?>... modes) {
        this.modes.addAll(Arrays.asList(modes));
        this.mode.addListener((mode, mode2) -> {
            if (this.isEnabled()) {
                this.modes.forEach(ModuleMode::onDisable);
                this.enableCurrentMode();
            }
        });
    }

    public ModuleMode getCurrentMode() {
        for (ModuleMode modulemode : this.modes) {
            if (modulemode.getMode() == this.mode.getValue()) {
                return modulemode;
            }
        }

        return null;
    }

    public void enableCurrentMode() {
        ModuleMode modulemode = this.getCurrentMode();
        if (modulemode != null) {
            modulemode.onEnable();
        }
    }

    @Override
    public void onEnable() {
        this.enableCurrentMode();
    }

    @Override
    public void onDisable() {
        this.modes.forEach(ModuleMode::onDisable);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    public enum Mode implements Displayable {
        NCP("NCP");

        public final String displayName;

        Mode(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
