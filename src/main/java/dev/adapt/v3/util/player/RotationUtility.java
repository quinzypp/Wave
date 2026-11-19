package dev.adapt.v3.util.player;

import java.util.List;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.util.RandomUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jspecify.annotations.Nullable;

public final class RotationUtility {
    public static final Minecraft mc = Minecraft.getInstance();

    public static float getAngleDifference(Vec2 rotation, Vec2 rotation2) {
        return Mth.degreesDifferenceAbs(rotation.x, rotation2.x) + Math.abs(rotation.y - rotation2.y);
    }

    public static double toMouseSteps(double first, double second) {
        return (float)(first / second) / 0.15F;
    }

    public static double getSensitivityMultiplier() {
        double d0 = mc.options.sensitivity().get() * 0.6F + 0.2F;
        return d0 * d0 * d0 * 8.0;
    }

    public static Vec2 applyGcdStep(Vec2 rotation, Vec2 rotation2) {
        double d0 = getSensitivityMultiplier() * 0.15F;
        float f = rotation.x - rotation2.x;
        float f1 = rotation.y - rotation2.y;
        float f2 = rotation2.x + (float)(Math.round(f / d0) * d0);
        float f3 = rotation2.y + (float)(Math.round(f1 / d0) * d0);
        return new Vec2(f2, f3);
    }

    public static float quantizeAngle(double value) {
        double d0 = getSensitivityMultiplier();
        return (float)(toMouseSteps(value, d0) * d0) * 0.15F;
    }

    public static Vec2 applySensitivityToPlayer(Vec2 rotation) {
        return quantizeRotation(applyGcdStep(rotation, getPlayerRotation()));
    }

    public static Vec2 quantizeRotation(Vec2 rotation) {
        return new Vec2(quantizeAngle(rotation.x), quantizeAngle(rotation.y));
    }

    public static Vec2 fixRotation(Vec2 rotation) {
        Vec2 vec2 = applySensitivityToPlayer(rotation);
        float f = unwrapYaw(vec2.x, mc.player.getYRot());
        return new Vec2(f, vec2.y);
    }

    public static float unwrapYaw(float yaw, float yaw2) {
        return yaw2 + Mth.wrapDegrees(yaw - yaw2);
    }

    public static Vec2 getPlayerRotation() {
        return new Vec2(mc.player.getYRot(), mc.player.getXRot());
    }

    public static Vec2 getSnapScaffoldRotation(Vec2 rotation, float value, boolean flag, boolean flag2) {
        float f;
        if (flag) {
            float f1 = 45.0F / value;
            float f2 = Math.round(MoveUtility.getMoveDirectionDegrees() / f1) * f1;
            float f3 = (float)Math.toRadians(f2);
            float f4 = 10.0F;
            float f5 = -Mth.sin(f3) * 10.0F;
            float f6 = Mth.cos(f3) * 10.0F;
            Vec3 vec3 = mc.player.position();
            double d0 = Math.floor(vec3.x) + f5 + 0.5;
            double d1 = Math.floor(vec3.z) + f6 + 0.5;
            double d2 = d0 - vec3.x;
            double d3 = d1 - vec3.z;
            f = (float)Math.toDegrees(Math.atan2(-d2, d3));
        } else {
            f = MoveUtility.getMoveDirectionDegrees();
        }

        float f7 = f + RandomUtility.nextFloat(-0.01F, 0.01F);
        List<Float> arraylist = new ArrayList<>(4);
        if (!flag2) {
            arraylist.add(f7);
            arraylist.add(f7 + 180.0F);
        }

        for (int angle = 45; angle < 180; angle += 90) {
            arraylist.add(f7 + angle);
            arraylist.add(f7 - angle);
        }

        arraylist.sort(Comparator.comparingDouble(value2 -> Mth.degreesDifferenceAbs(value2, rotation.x)));
        return new Vec2((Float)arraylist.getFirst(), rotation.y);
    }

    public static @Nullable RaytracedRotation findBlockFaceRotation(BlockPos pos, Direction direction, Vec2 rotation, Vec3 vec) {
        AABB aabb = new AABB(pos);
        Vec3 vec3 = aabb.getCenter();
        double d0 = aabb.getXsize();
        double d1 = aabb.getYsize();
        double d2 = aabb.getZsize();
        List<RaytracedRotation> arraylist = new ArrayList<>();
        float f = 8.0F;
        double d3 = d0;

        for (double d4 = -d3; d4 < d3; d4 += d3 / 8.0) {
            double d5 = d1;

            for (double d6 = -d5; d6 < d5; d6 += d5 / 8.0) {
                double d7 = d2;

                for (double d8 = -d7; d8 < d7; d8 += d7 / 8.0) {
                    Vec3 vec31 = vec3.add(d4, d6, d8);
                    Vec2 vec2 = fixRotation(getRotationsToVec(vec31));
                    HitResult hitresult = RaytraceUtility.raytraceBlockFromVec(mc.player.blockInteractionRange(), false, vec2.x, vec2.y, vec);
                    if (hitresult != null && hitresult.getType() == Type.BLOCK) {
                        BlockHitResult blockhitresult = (BlockHitResult)hitresult;
                        if (blockhitresult.getBlockPos().equals(pos) && blockhitresult.getDirection() == direction) {
                            arraylist.add(new RaytracedRotation(vec2, hitresult));
                        }
                    }
                }
            }
        }

        if (arraylist.isEmpty()) {
            return null;
        }

        arraylist.sort(Comparator.comparingDouble(raytracedRotation -> getAngleDifference(raytracedRotation.rotation(), rotation)));
        return (RaytracedRotation)arraylist.getFirst();
    }

    public static @Nullable RaytracedRotation findEntityRotation(LivingEntity living, Vec3 vec, double value) {
        Predicate predicate = key -> key == living;
        AABB aabb = living.getBoundingBox().inflate(living.getPickRadius());
        Vec3 vec3 = aabb.getCenter();
        double d0 = aabb.getXsize();
        double d1 = aabb.getYsize();
        double d2 = aabb.getZsize();
        List<RaytracedRotation> arraylist = new ArrayList<>();
        Vec2 vec2 = getRotationsToVec(vec);
        float f = (float)RandomUtility.sampleTickDouble(0.01, 0.05);
        Vec2 vec21 = new Vec2(RandomUtility.nextFloat(-f, f), RandomUtility.nextFloat(-f, f));
        Vec2 vec22 = vec2.add(vec21);
        Vec2 vec23 = fixRotation(vec22);
        EntityHitResult entityhitresult = RaytraceUtility.raytraceEntity(value, 1.0F, vec23.x, vec23.y, predicate);
        if (entityhitresult != null) {
            return new RaytracedRotation(vec23, entityhitresult);
        }

        float f1 = 8.0F - RandomUtility.RANDOM.nextFloat() * 0.25F;
        double d3 = d0;

        for (double d4 = -d3; d4 < d3; d4 += d3 / f1) {
            double d5 = d1;

            for (double d6 = -d5; d6 < d5; d6 += d5 / f1) {
                double d7 = d2;

                for (double d8 = -d7; d8 < d7; d8 += d7 / f1) {
                    Vec3 vec31 = vec3.add(d4, d6, d8);
                    Vec2 vec24 = fixRotation(getRotationsToVec(vec31));
                    EntityHitResult entityhitresult1 = RaytraceUtility.raytraceEntity(value, 1.0F, vec24.x, vec24.y, predicate);
                    if (entityhitresult1 != null) {
                        arraylist.add(new RaytracedRotation(vec24, entityhitresult1));
                    }
                }
            }
        }

        if (arraylist.isEmpty()) {
            return null;
        }

        arraylist.sort(Comparator.comparingDouble(raytracedRotation -> getAngleDifference(raytracedRotation.rotation(), vec23)));
        return (RaytracedRotation)arraylist.getFirst();
    }

    public static Vec2 getRotationsToBlockFace(BlockPos pos, Direction direction) {
        float f = (float)(pos.getX() + 0.5 - mc.player.getX() + direction.getStepX() * 0.5);
        float f1 = (float)(mc.player.getY() + mc.player.getEyeHeight() - pos.getY() - direction.getStepY() * 0.5);
        float f2 = (float)(pos.getZ() + 0.5 - mc.player.getZ() + direction.getStepZ() * 0.5);
        double d0 = Math.sqrt(f * f + f2 * f2);
        float f3 = (float)Math.toDegrees(-Math.atan2(f, f2));
        float f4 = (float)Math.toDegrees(Math.atan(f1 / d0));
        return new Vec2(f3, f4);
    }

    public static Vec2 getRotationsToVec(Vec3 vec) {
        return getRotationsBetween(mc.player.getEyePosition(), vec);
    }

    public static Vec2 getRotationsBetween(Vec3 vec1, Vec3 vec2) {
        double d0 = vec2.x - vec1.x;
        double d1 = vec2.y - vec1.y;
        double d2 = vec2.z - vec1.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float f = (float)Math.toDegrees(-Math.atan2(d0, d2));
        float f1 = (float)(-Math.toDegrees(Math.atan2(d1, d3)));
        return new Vec2(f, f1);
    }

    public static Vec3 getLookVector(float first, float second) {
        float f = first * (float) (Math.PI / 180.0);
        float f1 = -second * (float) (Math.PI / 180.0);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }

    public static double getYawDeltaToEntity(Entity entity) {
        double d0 = (RotationHelper.getState().getCameraYaw(mc.player.getYRot()) - getRotationsToVec(entity.position()).x) % 360.0 + 540.0;
        return d0 % 360.0 - 180.0;
    }

    public static boolean isWithinFov(Entity entity, float fov) {
        return fov >= 180.0F ? true : Math.abs(getYawDeltaToEntity(entity)) < fov;
    }
}
