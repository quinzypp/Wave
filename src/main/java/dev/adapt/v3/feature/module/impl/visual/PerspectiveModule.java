package dev.adapt.v3.feature.module.impl.visual;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;

public final class PerspectiveModule extends Module {
    public final BooleanProperty smoothThirdPerson = new BooleanProperty("Smooth Third Person", true);
    public final BooleanProperty hurtCam = new BooleanProperty("Hurt Cam", false);
    public final NumberProperty hurtCamStrength = (NumberProperty)new NumberProperty("Hurt Cam Strength", "%", 50.0, 0.0, 100.0, 5.0)
        .hideIf(() -> !this.hurtCam.getValue());

    public PerspectiveModule() {
        super("Perspective", "Camera tweaks: smooth third person and hurt cam.", ModuleCategory.VISUAL);
        this.addProperties(this.smoothThirdPerson, this.hurtCam, this.hurtCamStrength);
    }

    public static PerspectiveModule getInstance() {
        return Adapt.getInstance() == null ? null : Adapt.getInstance().getModuleManager().getByClass(PerspectiveModule.class);
    }

    public boolean modifyBobView(boolean flag) {
        return this.isEnabled() && this.smoothThirdPerson.getValue() && !mc.options.getCameraType().isFirstPerson() ? false : flag;
    }

    public double modifyDamageTilt(double value) {
        if (!this.isEnabled()) {
            return value;
        } else {
            return this.hurtCam.getValue() ? value * this.hurtCamStrength.getValue() / 100.0 : 0.0;
        }
    }
}
