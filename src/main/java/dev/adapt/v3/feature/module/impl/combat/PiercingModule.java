package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;

public final class PiercingModule extends Module {
    public PiercingModule() {
        super("Piercing", "Allows you to take players through blocks.", ModuleCategory.COMBAT);
    }

    public static PiercingModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(PiercingModule.class);
    }
}
