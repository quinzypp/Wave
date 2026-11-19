package dev.adapt.v3.feature.module.impl.world.scaffold;

import dev.adapt.v3.feature.helper.rotation.RotationProperties;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import dev.adapt.v3.feature.helper.rotation.model.impl.InstantRotationModel;
import dev.adapt.v3.feature.helper.swing.CpsProperties;
import dev.adapt.v3.feature.module.impl.world.scaffold.mode.VanillaScaffold;
import dev.adapt.v3.feature.module.impl.world.scaffold.mode.WatchdogScaffold;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;

public final class ScaffoldSettings {
    public final RotationProperties rotationConfig;
    public final CpsProperties interactCps;
    public final BooleanProperty tower;
    public final EnumProperty<ScaffoldSettings.Mode> mode;
    public final EnumProperty<ScaffoldSettings.SwitchMode> switchMode;
    public final EnumProperty<ScaffoldSettings.SwingMode> swingMode;
    public final BooleanProperty snapRotations;
    public final BooleanProperty sameY;
    public final BooleanProperty autoJump;
    public final NumberProperty cancelRotationAirTicks;
    public final NumberProperty placeAirTick;
    public final BooleanProperty blockOverlay;
    public final BooleanProperty overrideRaycast;

    public ScaffoldSettings(ScaffoldModule scaffoldModule) {
        this.rotationConfig = new RotationProperties(scaffoldModule, InstantRotationModel.INSTANCE).hideIf(this::isWatchdog);
        this.interactCps = new CpsProperties(scaffoldModule, "Interact CPS", false);
        this.mode = new EnumProperty<>("Mode", ScaffoldSettings.Mode.VANILLA);
        this.tower = new BooleanProperty("Tower", false);
        this.switchMode = new EnumProperty<>("Switch mode", ScaffoldSettings.SwitchMode.HOTBAR);
        this.swingMode = new EnumProperty<>("Swing mode", ScaffoldSettings.SwingMode.CLIENT);
        this.snapRotations = new BooleanProperty("Snap rotations", false);
        this.sameY = (BooleanProperty)new BooleanProperty("Same Y", true).hideIf(this::isWatchdog);
        this.autoJump = (BooleanProperty)new BooleanProperty("Auto jump", true).hideIf(() -> this.isWatchdog() || !this.isSameY());
        this.cancelRotationAirTicks = (NumberProperty)new NumberProperty("Cancel rotation air ticks", 0.0, 0.0, 11.0, 1.0).hideIf(() -> !this.isWatchdog());
        this.placeAirTick = (NumberProperty)new NumberProperty("Place air tick", 1.0, 1.0, 11.0, 1.0).hideIf(() -> !this.isWatchdog());
        this.blockOverlay = new BooleanProperty("Block overlay", true);
        this.overrideRaycast = new BooleanProperty("Override raycast", false);
        scaffoldModule.registerModes(this.mode, new VanillaScaffold(scaffoldModule), new WatchdogScaffold(scaffoldModule));
        scaffoldModule.addProperties(
            this.mode,
            this.cancelRotationAirTicks,
            this.placeAirTick,
            this.switchMode,
            this.swingMode,
            this.tower,
            this.snapRotations,
            this.overrideRaycast,
            this.sameY,
            this.autoJump,
            this.blockOverlay
        );
    }

    public boolean isWatchdog() {
        return this.mode.getValue() == ScaffoldSettings.Mode.WATCHDOG;
    }

    public int getCancelRotationAirTicks() {
        return this.cancelRotationAirTicks.getValue().intValue();
    }

    public int getPlaceAirTick() {
        return this.placeAirTick.getValue().intValue();
    }

    public RotationProperties getRotationConfig() {
        return this.rotationConfig;
    }

    public CpsProperties getInteractCps() {
        return this.interactCps;
    }

    public boolean isAutoJump() {
        return this.isWatchdog() || this.autoJump.getValue();
    }

    public EnumProperty<ScaffoldSettings.Mode> getModeProperty() {
        return this.mode;
    }

    public boolean isTower() {
        return this.tower.getValue();
    }

    public boolean isBlockOverlay() {
        return this.blockOverlay.getValue();
    }

    public EnumProperty<ScaffoldSettings.SwitchMode> getSwitchModeProperty() {
        return this.switchMode;
    }

    public EnumProperty<ScaffoldSettings.SwingMode> getSwingModeProperty() {
        return this.swingMode;
    }

    public boolean isSnapRotations() {
        return this.snapRotations.getValue();
    }

    public boolean isSameY() {
        return this.isWatchdog() || this.sameY.getValue();
    }

    public boolean isOverrideRaycast() {
        return this.overrideRaycast.getValue();
    }

    public RotationModel getRotationModel() {
        return this.isWatchdog() ? InstantRotationModel.INSTANCE : this.rotationConfig.createModel();
    }

    public enum Mode implements Displayable {
        VANILLA("Vanilla"),
        WATCHDOG("Watchdog");

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

    public enum SwingMode implements Displayable {
        CLIENT("Client"),
        SERVER("Server");

        public final String displayName;

        SwingMode(String text) {
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

    public enum SwitchMode implements Displayable {
        NORMAL("Normal"),
        HOTBAR("Hotbar"),
        FULL("Full");

        public final String displayName;

        SwitchMode(String text) {
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
