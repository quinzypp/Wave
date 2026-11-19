package dev.adapt.v3.feature.module.impl.movement;

import dev.adapt.v3.event.impl.StepEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.TimerUtility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public final class StepModule extends Module {
    public static final double[] STEP_1_5_OFFSETS = new double[]{0.42, 0.333, 0.248, 0.083, -0.078};
    public static final double[] STEP_2_0_OFFSETS = new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869};
    public static final double[] STEP_2_5_OFFSETS = new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907};
    public final EnumProperty<StepModule.Mode> mode = new EnumProperty<>("Mode", StepModule.Mode.NCP);
    public final NumberProperty height = (NumberProperty)new NumberProperty("Height", 2.5, 1.0, 2.5, 0.5).hideIf(() -> !this.isNcpMode());
    public final NumberProperty timer = (NumberProperty)new NumberProperty("Timer", 0.37, 0.2, 1.0, 0.01).hideIf(() -> !this.isNcpMode());
    public final NumberProperty delay = (NumberProperty)new NumberProperty("Delay", 0.5, 0.0, 2.0, 0.1).hideIf(() -> !this.isNcpMode());
    public final TimerUtility stepDelayTimer = new TimerUtility();
    public boolean timerModified;

    public StepModule() {
        super("Step", "Lets you step up blocks.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode, this.height, this.timer, this.delay);
    }

    @Override
    public void onEnable() {
        this.timerModified = false;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            if (this.timerModified) {
                TimerHelper.getInstance().reset();
                this.timerModified = false;
            }
        }
    }

    @Subscribe
    public void onStep(StepEvent event) {
        if (this.isNcpMode()) {
            LocalPlayer localplayer = mc.player;
            if (this.timerModified) {
                this.timerModified = false;
                TimerHelper.getInstance().reset();
            }

            if (!localplayer.isInWater() && !localplayer.isInLava()) {
                if (event.getStage() == StepEvent.Stage.PRE) {
                    if (localplayer.verticalCollision
                        && !mc.options.keyJump.isDown()
                        && this.stepDelayTimer.hasTimeElapsed((long)(this.delay.getValue() * 1000.0))) {
                        event.setStepHeight(this.height.getValue());
                    }
                } else {
                    double d0 = event.getSteppedHeight();
                    if (!(d0 < 0.625)) {
                        this.stepDelayTimer.reset();
                        if (!mc.hasSingleplayerServer()) {
                            float f = this.timer.getValue().floatValue();
                            TimerHelper.getInstance().setTimerSpeed(Math.max(0.05F, f - (d0 >= 1.0 ? Math.abs(1.0F - (float)d0) * (f * 0.55F) : 0.0F)));
                            this.timerModified = true;
                            this.sendNcpStepPackets(d0);
                        }
                    }
                }
            }
        }
    }

    public void sendNcpStepPackets(double value) {
        double d0 = mc.player.getY();
        if (value < 1.1) {
            double d1 = 0.42;
            double d2 = 0.75;
            if (value != 1.0) {
                d1 = Math.min(d1 * value, 0.425);
                d2 = Math.max(Math.min(d2 * value, 0.78), 0.49);
            }

            sendPosY(d0 + (d1 == 0.42 ? 0.41999998688698 : d1));
            if (d0 + d2 < d0 + value) {
                sendPosY(d0 + d2);
            }
        } else if (value < 1.6) {
            double d4 = d0;

            for (double d3 : STEP_1_5_OFFSETS) {
                sendPosY(d4 += d3);
            }
        } else {
            for (double d5 : value < 2.1 ? STEP_2_0_OFFSETS : STEP_2_5_OFFSETS) {
                sendPosY(d0 + d5);
            }
        }
    }

    public static void sendPosY(double value) {
        mc.getConnection().send(new Pos(mc.player.getX(), value, mc.player.getZ(), false, mc.player.horizontalCollision));
    }

    public boolean isNcpMode() {
        return this.mode.getValue() == StepModule.Mode.NCP;
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    public enum Mode implements Displayable {
        NCP("NCP");

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
