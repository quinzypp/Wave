package dev.adapt.v3.feature.module.impl.combat.killaura.target;

import dev.adapt.v3.util.player.RaytracedRotation;
import net.minecraft.world.entity.LivingEntity;

public final class AimedTarget {
    public final CombatTarget target;
    public final RaytracedRotation raytracedRotation;

    public AimedTarget(CombatTarget target, RaytracedRotation raytracedRotation) {
        this.target = target;
        this.raytracedRotation = raytracedRotation;
    }

    public CombatTarget getTarget() {
        return this.target;
    }

    public LivingEntity getEntity() {
        return this.getTarget().getLivingTarget().getEntity();
    }

    public RaytracedRotation getRaytracedRotation() {
        return this.raytracedRotation;
    }
}
