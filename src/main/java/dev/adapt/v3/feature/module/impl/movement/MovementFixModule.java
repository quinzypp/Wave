package dev.adapt.v3.feature.module.impl.movement;

import dev.adapt.v3.event.impl.JumpEvent;
import dev.adapt.v3.event.impl.MoveInputEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.SprintEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.util.Mth;

public final class MovementFixModule extends Module {
    public final EnumProperty<MovementFixModule.Mode> mode = new EnumProperty<>("Mode", MovementFixModule.Mode.NORMAL);
    public final BooleanProperty packetOnly = (BooleanProperty)new BooleanProperty("Packet only", false)
        .hideIf(() -> this.mode.getValue() != MovementFixModule.Mode.SPRINT_ONLY);

    public MovementFixModule() {
        super("Movement Fix", "Locks your movement to your rotations.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode, this.packetOnly);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (this.mode.getValue() == MovementFixModule.Mode.NORMAL && mc.player != null && !MovementFixHelper.isForceFixActive()) {
            float f = event.getForward();
            float f1 = event.getSideways();
            if (f != 0.0F || f1 != 0.0F) {
                float f2 = (float)Math.toDegrees(MoveUtility.getDirectionRadiansFromInput(RotationHelper.getState().getCameraYaw(mc.player.getYRot()), f, f1));
                float f3 = 0.0F;
                float f4 = 0.0F;
                float f5 = Float.MAX_VALUE;

                for (float f6 = -1.0F; f6 <= 1.0F; f6++) {
                    for (float f7 = -1.0F; f7 <= 1.0F; f7++) {
                        if (f7 != 0.0F || f6 != 0.0F) {
                            float f8 = (float)Math.toDegrees(
                                MoveUtility.getDirectionRadiansFromInput(MovementFixHelper.resolveYaw(mc.player.getYRot()), f6, f7)
                            );
                            double d0 = Mth.degreesDifferenceAbs(f2, f8);
                            if (d0 < f5) {
                                f5 = (float)d0;
                                f3 = f6;
                                f4 = f7;
                            }
                        }
                    }
                }

                event.setForward(f3);
                event.setSideways(f4);
            }
        }
    }

    @Subscribe
    public void onSprint(SprintEvent event) {
        if (!this.packetOnly.getValue() && this.shouldBlockSprint()) {
            mc.player.setSprinting(false);
            event.setSprinting(false);
        }
    }

    @Subscribe
    public void onJump(JumpEvent event) {
        if (event.isSprinting() && !this.packetOnly.getValue() && this.shouldBlockSprint()) {
            event.setSprinting(false);
        }
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        if (this.packetOnly.getValue() && this.shouldBlockSprint()) {
            event.setSprinting(false);
        }
    }

    public boolean shouldBlockSprint() {
        if (this.mode.getValue() == MovementFixModule.Mode.SPRINT_ONLY && mc.player != null) {
            float f = MovementFixHelper.resolveYaw(mc.player.getYRot());
            float f1 = MoveUtility.getDirectionDegrees(RotationHelper.getState().getCameraYaw(mc.player.getYRot()));
            float f2 = Mth.degreesDifferenceAbs(f1, f);
            return f2 > 45.005F;
        } else {
            return false;
        }
    }

    public boolean isNormalFixActive() {
        return this.isEnabled() && this.mode.getValue() == MovementFixModule.Mode.NORMAL;
    }

    public enum Mode implements Displayable {
        NORMAL("Normal"),
        SPRINT_ONLY("Sprint only");

        public final String displayName;

        Mode(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
