package dev.adapt.v3.feature.module.impl.combat.velocity;

import dev.adapt.v3.feature.module.mode.ModuleMode;

public abstract class VelocityMode extends ModuleMode<VelocityModule> {
    public VelocityMode(VelocityModule velocityModule) {
        super(velocityModule);
    }

    public final VelocityContext getContext() {
        return this.module.getContext();
    }

    public String getSuffix() {
        return this.getMode().toString();
    }
}
