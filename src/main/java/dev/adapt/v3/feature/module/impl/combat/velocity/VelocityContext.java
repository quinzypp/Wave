package dev.adapt.v3.feature.module.impl.combat.velocity;

import net.minecraft.world.entity.LivingEntity;

public interface VelocityContext {
    boolean isKillAuraEnabled();

    LivingEntity getKillAuraTarget();

    float getAttackRange();

    boolean isCurrentTarget(LivingEntity living);

    boolean shouldSkipVelocity();

    boolean isScaffoldEnabled();

    float getYaw();

    float getPitch();

    void setRotation(float value, float value2, float value3, float value4, VelocityContext.RotationMode rotationMode);

    enum RotationMode {
        SILENT,
        STRICT;
    }
}
