package dev.adapt.v3.util;

import java.util.Random;

public final class RandomUtility {
    public static final Random RANDOM = new Random();
    public static double tickRandomFactor = RANDOM.nextDouble();

    public static int nextInt(int first, int second) {
        return first == second ? first : first + RANDOM.nextInt(second - first);
    }

    public static boolean chance(int value) {
        return RANDOM.nextInt(100) < value;
    }

    public static int nextIntBounded(int value) {
        return RANDOM.nextInt(value);
    }

    public static double nextDouble(double first, double second) {
        return lerpDouble(first, second, RANDOM.nextDouble());
    }

    public static float nextFloat(float first, float second) {
        return lerpFloat(first, second, RANDOM.nextFloat());
    }

    public static float lerpFloat(float value, float value2, float value3) {
        return value == value2 ? value : value + (value2 - value) * value3;
    }

    public static double lerpDouble(double value, double value2, double value3) {
        return value == value2 ? value : value + (value2 - value) * value3;
    }

    public static double sampleTickDouble(double first, double second) {
        return lerpDouble(first, second, tickRandomFactor);
    }

    public static void updateTickFactor() {
        tickRandomFactor = RANDOM.nextDouble();
    }
}
