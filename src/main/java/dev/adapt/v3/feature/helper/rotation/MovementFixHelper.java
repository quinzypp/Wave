package dev.adapt.v3.feature.helper.rotation;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;

public final class MovementFixHelper implements Listener {
    public static final MovementFixHelper INSTANCE = new MovementFixHelper();
    public static int tickCount;
    public static int lastForceFixTick = -2147483647;
    public static int lastYawOverrideTick = -2147483647;
    public static float overrideYaw;

    public MovementFixHelper() {
        EventManager.register(this);
    }

    public static void init() {
    }

    @Subscribe(priority = 1000)
    public void onPreTick(PreTickEvent event) {
        tickCount++;
    }

    public static void markForceFix() {
        lastForceFixTick = tickCount;
    }

    public static boolean isForceFixActive() {
        return lastForceFixTick == tickCount || lastForceFixTick == tickCount - 1;
    }

    public static void setMovementYaw(float movementYaw) {
        overrideYaw = movementYaw;
        lastYawOverrideTick = tickCount;
    }

    public static boolean hasYawOverride() {
        return lastYawOverrideTick == tickCount;
    }

    public static float resolveYaw(float yaw) {
        return hasYawOverride() ? overrideYaw : yaw;
    }
}
