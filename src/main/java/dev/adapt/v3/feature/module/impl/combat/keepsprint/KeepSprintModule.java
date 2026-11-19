package dev.adapt.v3.feature.module.impl.combat.keepsprint;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.combat.keepsprint.mode.VanillaKeepSprint;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class KeepSprintModule extends Module {
    public final EnumProperty<KeepSprintModule.Mode> mode = new EnumProperty<>("Mode", KeepSprintModule.Mode.VANILLA);
    public final NumberProperty motion = (NumberProperty)new NumberProperty("Motion", 100.0, 0.0, 100.0, 1.0)
        .hideIf(() -> this.mode.getValue() != KeepSprintModule.Mode.VANILLA);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public KeepSprintModule() {
        super("Keep Sprint", "Keeps your sprint and speed when you hit something.", ModuleCategory.COMBAT);
        this.addProperties(this.mode, this.motion);
        this.registerModes(new VanillaKeepSprint(this));
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

    public NumberProperty getMotion() {
        return this.motion;
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

    public static VanillaKeepSprint getActiveVanillaMode() {
        Adapt adapt = Adapt.getInstance();
        if (adapt == null) {
            return null;
        } else {
            KeepSprintModule keepsprintmodule = adapt.getModuleManager().getByClass(KeepSprintModule.class);
            if (keepsprintmodule != null && keepsprintmodule.isEnabled()) {
                return keepsprintmodule.getCurrentMode() instanceof VanillaKeepSprint vanillakeepsprint && vanillakeepsprint.isListening()
                    ? vanillakeepsprint
                    : null;
            } else {
                return null;
            }
        }
    }

    public enum Mode implements Displayable {
        VANILLA("Vanilla");

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
