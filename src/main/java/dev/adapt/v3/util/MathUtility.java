package dev.adapt.v3.util;

import net.minecraft.util.Mth;

public final class MathUtility {
    public static double roundToStepAndClamp(double value, double value2, double value3, double value4) {
        double d0 = Math.round(value / value4) * value4;
        return Mth.clamp(d0, value2, value3);
    }

    public static float lerp(float value, float value2, float value3) {
        return value + (value2 - value) * value3;
    }

    public static double lerp(double value, double value2, double value3) {
        return value + (value2 - value) * value3;
    }

    public static float clamp01(float value) {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }
}
