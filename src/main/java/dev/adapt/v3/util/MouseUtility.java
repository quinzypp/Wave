package dev.adapt.v3.util;

public final class MouseUtility {
    public static final boolean[] BUTTON_STATES = new boolean[16];

    public static void setButtonDown(int value, boolean flag) {
        if (value >= 0 && value < BUTTON_STATES.length) {
            BUTTON_STATES[value] = flag;
        }
    }

    public static boolean isButtonDown(int value) {
        return value >= 0 && value < BUTTON_STATES.length && BUTTON_STATES[value];
    }
}
