package dev.adapt.v3.feature.module.impl.movement.speed.mode;

import dev.adapt.v3.event.impl.MoveInputEvent;
import dev.adapt.v3.event.impl.StrafeEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.movement.speed.SpeedModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.Minecraft;

public final class VanillaSpeed extends ModuleMode<SpeedModule> {
    public static final Minecraft mc = Minecraft.getInstance();
    public final NumberProperty vanillaSpeed = (NumberProperty)new NumberProperty("Vanilla Speed", 0.5, 0.1, 3.0, 0.05).hideIf(this::isNotSelected);
    public final BooleanProperty autoJump = (BooleanProperty)new BooleanProperty("Auto Jump", false).hideIf(this::isNotSelected);

    public VanillaSpeed(SpeedModule speedModule) {
        super(speedModule);
        speedModule.addProperties(this.vanillaSpeed, this.autoJump);
    }

    public boolean isNotSelected() {
        return this.module.getCurrentMode() != this;
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (this.autoJump.getValue() && mc.player != null && mc.player.onGround() && MoveUtility.isMoving()) {
            event.setJump(true);
        }
    }

    @Subscribe
    public void onStrafe(StrafeEvent event) {
        if (mc.player != null && MoveUtility.isMoving()) {
            event.cancel();
            MoveUtility.strafe(this.vanillaSpeed.getValue());
        }
    }

    @Override
    public Enum getMode() {
        return SpeedModule.Mode.VANILLA;
    }
}
