package dev.adapt.v3.feature.module.impl.combat.velocity;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.combat.velocity.mode.HypixelVelocity;
import dev.adapt.v3.feature.module.impl.combat.velocity.mode.VanillaVelocity;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VelocityModule extends Module {
    public final List<ModuleMode<?>> modes = new ArrayList<>();
    public final EnumProperty<VelocityModule.Mode> mode = new EnumProperty<>("Mode", VelocityModule.Mode.HYPIXEL);
    public final VelocityContext context = new AdaptVelocityContext();

    public VelocityModule() {
        super("Velocity", "Controls knockback and sprint-hit reduction.", ModuleCategory.COMBAT);
        this.addProperties(this.mode);
        this.registerModes(this.mode, new HypixelVelocity(this), new VanillaVelocity(this));
        PlayerRaycastHelper.init();
    }

    @SafeVarargs
    public final void registerModes(EnumProperty property, ModuleMode<?>... modes) {
        this.modes.addAll(Arrays.asList(modes));
        property.addListener((oldValue, newValue) -> {
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

    public VelocityContext getContext() {
        return this.context;
    }

    public EnumProperty<VelocityModule.Mode> getModeProperty() {
        return this.mode;
    }

    @Override
    public String getSuffix() {
        return this.getCurrentMode() instanceof VelocityMode velocitymode ? velocitymode.getSuffix() : this.mode.getValue().toString();
    }

    public enum Mode implements Displayable {
        HYPIXEL("Hypixel"),
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
