package dev.adapt.v3.feature.module.impl.movement.flight.mode;

import dev.adapt.v3.event.impl.StrafeEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.movement.TargetStrafeModule;
import dev.adapt.v3.feature.module.impl.movement.flight.FlightModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class MotionFlight extends ModuleMode<FlightModule> {
    public static final Minecraft mc = Minecraft.getInstance();
    public final NumberProperty horizontalSpeed = (NumberProperty)new NumberProperty("Horizontal Speed", 1.0, 0.1, 5.0, 0.05).hideIf(this::isNotSelected);
    public final NumberProperty verticalSpeed = (NumberProperty)new NumberProperty("Vertical Speed", 0.5, 0.05, 3.0, 0.05).hideIf(this::isNotSelected);

    public MotionFlight(FlightModule flightModule) {
        super(flightModule);
        flightModule.addProperties(this.horizontalSpeed, this.verticalSpeed);
    }

    public boolean isNotSelected() {
        return this.module.getCurrentMode() != this;
    }

    @Subscribe
    public void onStrafe(StrafeEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null) {
            event.cancel();
            MoveUtility.strafeOrStop(this.horizontalSpeed.getValue());
            boolean flag = localplayer.input.keyPresses.jump() && !TargetStrafeModule.isStrafing();
            boolean flag1 = localplayer.input.keyPresses.shift();
            double d0 = flag == flag1 ? 0.0 : (flag ? 1 : -1) * this.verticalSpeed.getValue();
            localplayer.setDeltaMovement(localplayer.getDeltaMovement().x, d0, localplayer.getDeltaMovement().z);
        }
    }

    @Override
    public void onDisable() {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null) {
            localplayer.setDeltaMovement(0.0, 0.0, 0.0);
        }

        super.onDisable();
    }

    @Override
    public Enum getMode() {
        return FlightModule.Mode.MOTION;
    }
}
