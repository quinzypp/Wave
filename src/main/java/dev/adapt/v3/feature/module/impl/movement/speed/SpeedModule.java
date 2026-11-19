package dev.adapt.v3.feature.module.impl.movement.speed;

import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.movement.speed.mode.LegitSpeed;
import dev.adapt.v3.feature.module.impl.movement.speed.mode.NcpSpeed;
import dev.adapt.v3.feature.module.impl.movement.speed.mode.VanillaSpeed;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SpeedModule extends Module {
    public final EnumProperty<SpeedModule.Mode> mode = new EnumProperty<>("Mode", SpeedModule.Mode.LEGIT);
    public final BooleanProperty lagBack = (BooleanProperty)new BooleanProperty("Lag back", false).hideIf(() -> this.mode.getValue() != SpeedModule.Mode.NCP);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public SpeedModule() {
        super("Speed", "Makes you move faster.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode, this.lagBack);
        this.registerModes(new LegitSpeed(this), new NcpSpeed(this), new VanillaSpeed(this));
    }

    public boolean isLagBackDisable() {
        return this.lagBack.getValue();
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

    public EnumProperty<SpeedModule.Mode> getModeProperty() {
        return this.mode;
    }

    public enum Mode implements Displayable {
        LEGIT("Legit"),
        NCP("NCP"),
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
