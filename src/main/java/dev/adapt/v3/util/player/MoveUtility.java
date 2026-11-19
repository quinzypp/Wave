package dev.adapt.v3.util.player;

import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.impl.movement.TargetStrafeModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;

public final class MoveUtility {
    public static final Minecraft mc = Minecraft.getInstance();

    public static double[] getMotion(float first, double second) {
        return new double[]{-Mth.sin(first) * second, Mth.cos(first) * second};
    }

    public static void setMotion(Entity entity, double first, double second) {
        if (first == 0.0) {
            entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
        } else {
            entity.setDeltaMovement(-Mth.sin((float)second) * first, entity.getDeltaMovement().y, Mth.cos((float)second) * first);
        }
    }

    public static void strafe(double value) {
        float f = TargetStrafeModule.overrideYaw(RotationHelper.getState().getCameraYaw(mc.player.getYRot()));
        double d0 = getDirectionRadians(f);
        setMotion(mc.player, value, d0);
    }

    public static void strafeWithStrength(double first, double second) {
        second /= 100.0;
        second = Math.min(1.0, Math.max(0.0, second));
        double d0 = mc.player.getDeltaMovement().x;
        double d1 = mc.player.getDeltaMovement().z;
        strafe(first);
        Vec3 vec3 = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(d0 + (vec3.x - d0) * second, vec3.y, d1 + (vec3.z - d1) * second);
    }

    public static void strafeOrStop(double value) {
        if (!isMoving()) {
            setMotion(mc.player, 0.0, 0.0);
        } else {
            strafe(value);
        }
    }

    public static void strafeCurrentSpeed() {
        strafeOrStop(getHorizontalSpeed());
    }

    public static void setMotionWithYaw(double value, float yaw) {
        mc.player.setDeltaMovement(-Mth.sin(yaw) * value, mc.player.getDeltaMovement().y, Mth.cos(yaw) * value);
    }

    public static double applySpeedEffect(double speed, double speed2) {
        MobEffectInstance mobeffectinstance = mc.player.getEffect(MobEffects.SPEED);
        return mobeffectinstance == null ? speed : speed * (1.0 + speed2 * (mobeffectinstance.getAmplifier() + 1));
    }

    public static double getBaseSpeedWithEffect(double speed) {
        return applySpeedEffect(speed, 0.2);
    }

    public static boolean hasCollisionAtOffset(double value, double value2, double value3) {
        return mc.player != null && mc.level != null && !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(value, value2, value3));
    }

    public static boolean isBlockUnder(double value) {
        for (int offset = 0; offset < value; offset += 2) {
            if (hasCollisionAtOffset(0.0, -offset, 0.0)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isBlockUnderToVoid() {
        return mc.player != null && mc.level != null ? isBlockUnder((int)(mc.player.getY() - mc.level.getMinY()) + 2) : false;
    }

    public static boolean hasSpeedEffect() {
        return mc.player != null && mc.player.hasEffect(MobEffects.SPEED);
    }

    public static int getSpeedEffectLevel() {
        if (mc.player == null) {
            return 0;
        }

        MobEffectInstance mobeffectinstance = mc.player.getEffect(MobEffects.SPEED);
        return mobeffectinstance == null ? 0 : mobeffectinstance.getAmplifier() + 1;
    }

    public static double getHorizontalSpeed() {
        return Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
    }

    public static float getYawBetween(Vector2d vector2d2, Vector2d vector2d22) {
        Vector2d vector2d = new Vector2d(vector2d22.x - vector2d2.x, vector2d22.y - vector2d2.y);
        return (float)Math.toDegrees(Math.atan2(-vector2d.x, vector2d.y));
    }

    public static float getMotionYaw() {
        float f = (float)mc.player.getX() - (float)mc.player.xo;
        float f1 = (float)mc.player.getZ() - (float)mc.player.zo;
        return (float)Math.toDegrees((Math.atan2(-f, f1) + (float) (Math.PI * 2)) % (float) (Math.PI * 2));
    }

    public static float getMoveDirectionDegrees() {
        return getDirectionDegrees(RotationHelper.getState().getCameraYaw(mc.player.getYRot()));
    }

    public static double getMoveDirectionRadians() {
        return getDirectionRadians(RotationHelper.getState().getCameraYaw(mc.player.getYRot()));
    }

    public static double getDirectionRadians(float value) {
        return Math.toRadians(getDirectionDegrees(value));
    }

    public static float getDirectionDegrees(float value) {
        if (mc.options.keyDown.isDown()) {
            value += 180.0F;
        }

        float f = 1.0F;
        if (mc.options.keyDown.isDown()) {
            f = -0.5F;
        } else if (mc.options.keyUp.isDown()) {
            f = 0.5F;
        }

        if (mc.options.keyLeft.isDown()) {
            value -= 90.0F * f;
        } else if (mc.options.keyRight.isDown()) {
            value += 90.0F * f;
        }

        return value;
    }

    public static boolean isMoving() {
        return mc.player == null ? false : mc.player.zza != 0.0F || mc.player.xxa != 0.0F;
    }

    public static double getBlocksPerSecond() {
        if (mc.player == null) {
            return 0.0;
        }

        double d0 = Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo) * 20.0 * TimerHelper.getInstance().getTimerSpeed();
        return Math.round(d0 * 100.0) / 100.0;
    }

    public static double getDirectionRadiansFromInput(float value, double value2, double value3) {
        if (value2 < 0.0) {
            value += 180.0F;
        }

        float f = 1.0F;
        if (value2 < 0.0) {
            f = -0.5F;
        } else if (value2 > 0.0) {
            f = 0.5F;
        }

        if (value3 > 0.0) {
            value -= 90.0F * f;
        }

        if (value3 < 0.0) {
            value += 90.0F * f;
        }

        return Math.toRadians(value);
    }
}
