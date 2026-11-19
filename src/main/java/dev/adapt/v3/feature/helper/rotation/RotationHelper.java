package dev.adapt.v3.feature.helper.rotation;

public final class RotationHelper {
    public static final RotationState STATE = new RotationState();
    public static final RotationUpdater UPDATER = new RotationUpdater();

    public static void init() {
    }

    public static RotationUpdater getUpdater() {
        return UPDATER;
    }

    public static RotationState getState() {
        return STATE;
    }
}
