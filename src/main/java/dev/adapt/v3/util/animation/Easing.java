package dev.adapt.v3.util.animation;

import java.util.function.Function;

public enum Easing {
    LINEAR(value -> value),
    DECELERATE(value -> 1.0F - (value - 1.0F) * (value - 1.0F)),
    SMOOTH_STEP(value -> (float)(-2.0 * Math.pow(value.floatValue(), 3.0) + 3.0 * Math.pow(value.floatValue(), 2.0))),
    EASE_OUT_QUAD(value -> value * (2.0F - value)),
    EASE_IN_OUT_QUAD(value -> value.floatValue() < 0.5 ? 2.0F * value * value : -1.0F + (4.0F - 2.0F * value) * value),
    EASE_OUT_CUBIC(value -> {
        float f = value - 1.0F;
        return f * f * f + 1.0F;
    }),
    EASE_IN_OUT_CUBIC(value -> value.floatValue() < 0.5 ? 4.0F * value * value * value : (value - 1.0F) * (2.0F * value - 2.0F) * (2.0F * value - 2.0F) + 1.0F),
    EASE_OUT_QUINT(value -> {
        float f = value - 1.0F;
        return 1.0F + f * f * f * f * f;
    }),
    EASE_OUT_EXPO(value -> value == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0F * value)),
    EASE_OUT_BACK(value -> 1.0F + 2.70158F * (float)Math.pow(value - 1.0F, 3.0) + 1.70158F * (float)Math.pow(value - 1.0F, 2.0)),
    EASE_OUT_ELASTIC(
        value -> value == 0.0F
            ? 0.0F
            : (value == 1.0F ? 1.0F : (float)(Math.pow(2.0, -10.0F * value) * Math.sin((value * 10.0F - 0.75) * (Math.PI * 2.0 / 3.0)) * 0.5 + 1.0))
    ),
    DYNAMIC_ISLAND(value -> (float)(1.0 - Math.cos(value.floatValue() * Math.PI * (0.2 + 2.5 * Math.pow(value.floatValue(), 3.0))) * Math.exp(-value * 5.0F)));

    public final Function<Float, Float> function;

    Easing(Function<Float, Float> mapper) {
        this.function = mapper;
    }

    public Function<Float, Float> getFunction() {
        return this.function;
    }
}
