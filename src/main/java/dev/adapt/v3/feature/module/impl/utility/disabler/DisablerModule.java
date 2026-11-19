package dev.adapt.v3.feature.module.impl.utility.disabler;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.utility.disabler.mode.CubeCraftDisabler;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DisablerModule extends Module {
    public final EnumProperty<DisablerModule.Mode> mode = new EnumProperty<>("Mode", DisablerModule.Mode.CUBECRAFT);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public DisablerModule() {
        super("Disabler", "Disables parts of server anticheats.", ModuleCategory.WORLD);
        this.addProperties(this.mode);
        this.registerModes(new CubeCraftDisabler(this));
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
        CUBECRAFT("CubeCraft");

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
