package dev.adapt.v3.feature.helper.rotation.model.impl;

import dev.adapt.v3.feature.helper.rotation.model.ModelType;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public final class InstantRotationModel implements RotationModel {
    public static final InstantRotationModel INSTANCE = new InstantRotationModel();

    @Override
    public Vec2 interpolate(Vec2 rotation, Vec2 rotation2, float value) {
        float f = Mth.wrapDegrees(rotation2.x - rotation.x) * value;
        float f1 = (rotation2.y - rotation.y) * value;
        return new Vec2(rotation.x + f, rotation.y + f1);
    }

    @Override
    public ModelType getType() {
        return ModelType.INSTANT;
    }
}
