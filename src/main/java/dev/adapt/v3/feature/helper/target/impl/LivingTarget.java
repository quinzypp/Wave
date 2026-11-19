package dev.adapt.v3.feature.helper.target.impl;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

public class LivingTarget extends EntityTarget {
    public LivingTarget(LivingEntity living) {
        super(living);
    }

    @Override
    public boolean matches(int value) {
        return this.entity instanceof Enemy ? (value & 2) != 0 : (value & 4) != 0;
    }

    public float getEffectiveHealth() {
        return this.entity.getHealth() + this.entity.getAbsorptionAmount();
    }
}
