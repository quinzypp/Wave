package dev.adapt.v3.feature.module.impl.combat.keepsprint.mode;

import dev.adapt.v3.feature.module.impl.combat.keepsprint.KeepSprintModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;

public final class VanillaKeepSprint extends ModuleMode<KeepSprintModule> {
    public VanillaKeepSprint(KeepSprintModule keepSprintModule) {
        super(keepSprintModule);
    }

    public double getMultiplier() {
        return this.module.getMotion().getValue() / 100.0;
    }

    @Override
    public Enum getMode() {
        return KeepSprintModule.Mode.VANILLA;
    }
}
