package dev.adapt.v3.feature.helper.movement;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.StrafeEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class MovementHelper implements Listener {
    public static final MovementHelper INSTANCE = new MovementHelper();
    public float airSpeed = Float.NaN;

    public static void init() {
        EventManager.register(INSTANCE);
    }

    public static void setAirSpeed(float airSpeed) {
        INSTANCE.airSpeed = airSpeed;
    }

    @Subscribe(priority = 1000)
    public void onStrafe(StrafeEvent event) {
        if (!Float.isNaN(this.airSpeed)) {
            float f = this.airSpeed;
            this.airSpeed = Float.NaN;
            LocalPlayer localplayer = Minecraft.getInstance().player;
            if (localplayer != null && !localplayer.onGround()) {
                event.setSpeed(f);
            }
        }
    }
}
