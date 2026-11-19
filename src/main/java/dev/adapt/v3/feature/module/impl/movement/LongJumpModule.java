package dev.adapt.v3.feature.module.impl.movement;

import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class LongJumpModule extends Module {
    public static final double[] GLIDE_Y_MOTIONS = new double[]{
        0.396,
        -0.122,
        -0.1,
        0.423,
        0.35,
        0.28,
        0.217,
        0.15,
        0.025,
        -0.00625,
        -0.038,
        -0.0693,
        -0.102,
        -0.13,
        -0.018,
        -0.1,
        -0.117,
        -0.14532,
        -0.1334,
        -0.1581,
        -0.183141,
        -0.170695,
        -0.195653,
        -0.221,
        -0.209,
        -0.233,
        -0.25767,
        -0.314917,
        -0.371019,
        -0.426
    };
    public static final double[] MOVING_Y_MOTIONS = new double[]{0.345, 0.2699, 0.183, 0.103, 0.024, -0.008, -0.04, -0.072, -0.104, -0.13, -0.019, -0.097};
    public static final double[] IDLE_Y_MOTIONS = new double[]{0.345, 0.2699, 0.183, 0.103, 0.024, -0.008, -0.04, -0.072, -0.14, -0.17, -0.019, -0.13};
    public final BooleanProperty autoDisable = new BooleanProperty("Auto Disable", true);
    public final BooleanProperty glide = new BooleanProperty("Glide", false);
    public float speedDecay;
    public int jumpCount;
    public int airTicks;

    public LongJumpModule() {
        super("Long Jump", "Jumps much further than normal.", ModuleCategory.MOVEMENT);
        this.addProperties(this.autoDisable, this.glide);
    }

    @Override
    public void onEnable() {
        this.speedDecay = 0.0F;
        this.airTicks = 0;
        this.jumpCount = 0;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            stopHorizontalMotion(mc.player);
        }

        TimerHelper.getInstance().setTimerSpeed(1.0F);
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null) {
            int i = MoveUtility.getSpeedEffectLevel();
            boolean flag = this.glide.getValue();
            float f = 0.7F + i * 0.45F;
            if (MoveUtility.isMoving() && localplayer.onGround()) {
                if (this.autoDisable.getValue() && this.jumpCount > 0) {
                    this.jumpCount = 0;
                    this.setEnabled(false);
                    return;
                }

                this.jumpCount++;
                setStrafeSpeed(localplayer, 0.15);
                localplayer.jumpFromGround();
                setMotionY(localplayer, applyJumpBoost(localplayer, 0.425F));
                this.airTicks = 1;
            }

            if (MoveUtility.hasCollisionAtOffset(0.0, -0.001, 0.0)) {
                this.speedDecay = 0.0F;
            } else {
                if (localplayer.verticalCollision) {
                    this.airTicks = 0;
                }

                if (this.airTicks > 0 && this.airTicks <= 3 && flag) {
                    localplayer.setOnGround(true);
                    event.setOnGround(true);
                }

                double d0 = flag ? 1.1 + i * 0.2F - this.speedDecay / 20.0F : 0.95 + i * 0.2 - this.speedDecay / 25.0F;
                double d1 = getBaseMoveSpeed();
                if (d0 < d1 - 0.05) {
                    d0 = d1 - 0.05;
                }

                if (this.airTicks < 4 && flag) {
                    d0 = d1;
                }

                setStrafeSpeed(localplayer, d0);
                setMotionY(localplayer, flag ? getGlideMotionY(localplayer, this.airTicks) : getNormalMotionY(localplayer, this.airTicks));
                if (this.airTicks > 0) {
                    this.airTicks++;
                }

                this.speedDecay += f;
            }
        }
    }

    public static double getGlideMotionY(LocalPlayer player, int value) {
        value--;
        return value >= 0 && value < GLIDE_Y_MOTIONS.length ? GLIDE_Y_MOTIONS[value] : player.getDeltaMovement().y;
    }

    public static double getNormalMotionY(LocalPlayer player, int value) {
        value--;
        if (value >= 0 && value < MOVING_Y_MOTIONS.length) {
            return MoveUtility.isMoving() && !player.horizontalCollision ? MOVING_Y_MOTIONS[value] : IDLE_Y_MOTIONS[value];
        } else {
            return player.getDeltaMovement().y;
        }
    }

    public static void setStrafeSpeed(LocalPlayer player, double speed) {
        double d0 = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double d1 = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        float f = RotationHelper.getState().getCameraYaw(player.getYRot());
        if (d0 == 0.0 && d1 == 0.0) {
            stopHorizontalMotion(player);
        } else {
            if (d0 != 0.0) {
                if (d1 > 0.0) {
                    f += d0 > 0.0 ? -45.0F : 45.0F;
                } else if (d1 < 0.0) {
                    f += d0 > 0.0 ? 45.0F : -45.0F;
                }

                d1 = 0.0;
                d0 = d0 > 0.0 ? 1.0 : -1.0;
            }

            double d2 = Math.cos(Math.toRadians(f + 90.0F));
            double d3 = Math.sin(Math.toRadians(f + 90.0F));
            player.setDeltaMovement(d0 * speed * d2 + d1 * speed * d3, player.getDeltaMovement().y, d0 * speed * d3 - d1 * speed * d2);
        }
    }

    public static void setMotionY(LocalPlayer player, double value) {
        player.setDeltaMovement(player.getDeltaMovement().x, value, player.getDeltaMovement().z);
    }

    public static void stopHorizontalMotion(LocalPlayer player) {
        player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
    }

    public static double applyJumpBoost(LocalPlayer player, float value) {
        MobEffectInstance mobeffectinstance = player.getEffect(MobEffects.JUMP_BOOST);
        if (mobeffectinstance != null) {
            value += (mobeffectinstance.getAmplifier() + 1) * 0.1F;
        }

        return value;
    }

    public static double getBaseMoveSpeed() {
        return 0.2873 * (1.0 + 0.2 * MoveUtility.getSpeedEffectLevel());
    }

    @Override
    public String getSuffix() {
        return "Hypixel";
    }
}
