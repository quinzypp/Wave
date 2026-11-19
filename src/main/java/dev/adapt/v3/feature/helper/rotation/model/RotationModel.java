package dev.adapt.v3.feature.helper.rotation.model;

import net.minecraft.world.phys.Vec2;

public interface RotationModel {
    Vec2 interpolate(Vec2 rotation, Vec2 rotation2, float value);

    ModelType getType();
}
