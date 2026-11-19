package dev.adapt.v3.feature.module.impl.combat.killaura;

import dev.adapt.v3.feature.helper.rotation.RotationProperties;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import dev.adapt.v3.feature.helper.rotation.model.impl.InstantRotationModel;
import dev.adapt.v3.feature.helper.swing.CpsProperties;
import dev.adapt.v3.feature.helper.target.TargetProperties;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;

public final class KillAuraSettings {
    public final RotationProperties rotationProperties;
    public final EnumProperty<KillAuraSettings.Mode> mode;
    public final TargetProperties targetProperties;
    public final CpsProperties attackCps;
    public final NumberProperty rotationRange;
    public final BooleanProperty requireAttackKey;
    public final BooleanProperty requireWeapon;
    public final BooleanProperty overrideRaycast;
    public final NumberProperty fov;

    public KillAuraSettings(KillAuraModule killAuraModule) {
        killAuraModule.setPropertyGroup("Rotation");
        this.rotationProperties = new RotationProperties(killAuraModule, InstantRotationModel.INSTANCE);
        killAuraModule.setPropertyGroup(null);
        this.requireWeapon = new BooleanProperty("Require weapon", false);
        this.requireAttackKey = new BooleanProperty("Require attack key", false);
        this.mode = new EnumProperty<>("Mode", KillAuraSettings.Mode.SWITCH);
        killAuraModule.addProperties(this.mode, this.requireWeapon, this.requireAttackKey);
        killAuraModule.setPropertyGroup("Attack");
        this.attackCps = new CpsProperties(killAuraModule, "Attack CPS", true);
        this.rotationRange = (NumberProperty)new NumberProperty("Rotation range", 5.0, 3.0, 8.0, 0.1).describe("Rotation");
        killAuraModule.addProperties(this.rotationRange);
        killAuraModule.setPropertyGroup("Targets");
        this.targetProperties = new TargetProperties(killAuraModule, true, false, false, false, false, true);
        this.fov = (NumberProperty)new NumberProperty("FOV", 180.0, 1.0, 180.0, 1.0).describe("Rotation");
        killAuraModule.setPropertyGroup("Raycast");
        this.overrideRaycast = new BooleanProperty("Override raycast", false);
        killAuraModule.addProperties(this.fov, this.overrideRaycast);
        killAuraModule.setPropertyGroup(null);
    }

    public boolean isOverrideRaycast() {
        return this.overrideRaycast.getValue();
    }

    public double getRotationRange() {
        return this.rotationRange.getValue();
    }

    public TargetProperties getTargetProperties() {
        return this.targetProperties;
    }

    public CpsProperties getAttackCps() {
        return this.attackCps;
    }

    public boolean isRequireAttackKey() {
        return this.requireAttackKey.getValue();
    }

    public boolean isRequireWeapon() {
        return this.requireWeapon.getValue();
    }

    public RotationModel getRotationModel() {
        return this.rotationProperties.createModel();
    }

    public KillAuraSettings.Mode getMode() {
        return this.mode.getValue();
    }

    public float getFov() {
        return this.fov.getValue().floatValue();
    }

    public enum Mode implements Displayable {
        SINGLE("Single"),
        SWITCH("Switch");

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
