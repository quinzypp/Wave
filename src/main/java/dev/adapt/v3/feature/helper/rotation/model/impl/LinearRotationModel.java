package dev.adapt.v3.feature.helper.rotation.model.impl;

import dev.adapt.v3.feature.helper.rotation.model.ModelType;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public final class LinearRotationModel implements RotationModel {
    public final double maxAngle;

    public LinearRotationModel(double value) {
        this.maxAngle = value;
    }

    @Override
    public Vec2 interpolate(Vec2 rotation, Vec2 rotation2, float value) {
        float f = Mth.wrapDegrees(rotation2.x - rotation.x) * value;
        float f1 = (rotation2.y - rotation.y) * value;
        double d0 = Math.sqrt(f * f + f1 * f1);
        if (d0 == 0.0) {
            return new Vec2(rotation.x + f, rotation.y + f1);
        }

        double d1 = Math.abs(f / d0);
        double d2 = Math.abs(f1 / d0);
        double d3 = this.maxAngle * d1;
        double d4 = 360.0 * d2;
        float f2 = (float)Math.max(Math.min(f, d3), -d3);
        float f3 = (float)Math.max(Math.min(f1, d4), -d4);
        return new Vec2(rotation.x + f2, rotation.y + f3);
    }

    @Override
    public ModelType getType() {
        return ModelType.LINEAR;
    }
}
