package dev.adapt.v3.feature.module.impl.movement.flight;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.movement.flight.mode.DamageFlight;
import dev.adapt.v3.feature.module.impl.movement.flight.mode.MotionFlight;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FlightModule extends Module {
    public final EnumProperty<FlightModule.Mode> mode = new EnumProperty<>("Mode", FlightModule.Mode.DAMAGE);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public FlightModule() {
        super("Flight", "Lets you fly.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode);
        this.registerModes(new DamageFlight(this), new MotionFlight(this));
    }

    @SafeVarargs
    public final void registerModes(ModuleMode<?>... modes) {
        this.modes.addAll(Arrays.asList(modes));
        this.mode.addListener((mode, mode2) -> {
            if (this.isEnabled()) {
                this.modes.forEach(ModuleMode::onDisable);
                TimerHelper.getInstance().reset();
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
        TimerHelper.getInstance().reset();
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    public static boolean isFlightEnabled() {
        Adapt adapt = Adapt.getInstance();
        if (adapt == null) {
            return false;
        }

        FlightModule flightmodule = adapt.getModuleManager().getByClass(FlightModule.class);
        return flightmodule != null && flightmodule.isEnabled();
    }

    public enum Mode implements Displayable {
        DAMAGE("Damage"),
        MOTION("Motion");

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
