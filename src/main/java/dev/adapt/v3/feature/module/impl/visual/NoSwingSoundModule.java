package dev.adapt.v3.feature.module.impl.visual;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class NoSwingSoundModule extends Module {
    public NoSwingSoundModule() {
        super("No Swing Sound", "Removes the sword sweep attack sound.", ModuleCategory.UTILITY);
    }

    public static boolean shouldCancelSound(SoundInstance soundInstance) {
        Adapt adapt = Adapt.getInstance();
        if (adapt != null && soundInstance != null) {
            NoSwingSoundModule noswingsoundmodule = adapt.getModuleManager().getByClass(NoSwingSoundModule.class);
            return noswingsoundmodule != null && noswingsoundmodule.isEnabled() && SoundEvents.PLAYER_ATTACK_SWEEP.location().equals(soundInstance.getIdentifier());
        } else {
            return false;
        }
    }
}
