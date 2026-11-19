package dev.adapt.v3.feature.module.impl.utility.nofall;

import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.utility.nofall.mode.CubeCraftNoFall;
import dev.adapt.v3.feature.module.impl.utility.nofall.mode.VanillaNoFall;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class NoFallModule extends Module {
    public final EnumProperty<NoFallModule.Mode> mode = new EnumProperty<>("Mode", NoFallModule.Mode.CUBECRAFT);
    public final List<ModuleMode<?>> modes = new ArrayList<>();
    public double handledFallDistance;

    public NoFallModule() {
        super("No Fall", "Removes fall damage.", ModuleCategory.UTILITY);
        this.addProperties(this.mode);
        this.registerModes(new VanillaNoFall(this), new CubeCraftNoFall(this));
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

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        if (mc.player != null && mc.player.fallDistance == 0.0) {
            this.markFallDistanceHandled();
        }
    }

    public void markFallDistanceHandled() {
        this.handledFallDistance = mc.player.fallDistance;
    }

    public double getRemainingFallDistance() {
        return mc.player.getAbilities().mayfly ? 0.0 : mc.player.fallDistance - this.handledFallDistance;
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
        this.handledFallDistance = 0.0;
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
        VANILLA("Vanilla"),
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
