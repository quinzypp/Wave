package dev.adapt.v3.feature.helper.projectile;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class ProjectileSolver {
    public static final double SNOWBALL_SPEED = 1.5;
    public static final double SNOWBALL_GRAVITY = 0.03;
    public static final double SNOWBALL_DRAG = 0.99;
    public static final double ROD_GRAVITY = 0.03;
    public static final double ROD_DRAG = 0.92;
    public static final double ROD_SIDE_OFFSET = 0.3;
    public static final double ROD_MAX_TAN_PITCH = 5.0;
    public static final double ROD_BASE_SPEED = 0.6;
    public static final double ROD_EXTRA_SPEED = 0.5;
    public static final double SNOWBALL_EYE_OFFSET = 0.1;
    public static final double MAX_HITBOX_EXPANSION = 0.3;
    public static final int MAX_FLIGHT_TICKS = 50;
    public static final float MAX_PITCH = 89.0F;
    public static final float MIN_PITCH = -89.0F;
    public static final float PITCH_STEP = 3.0F;
    public static final double[] AIM_HEIGHT_FACTORS = new double[]{0.5, 0.85, 0.15};
    public static final int BISECT_ITERATIONS = 24;
    public static final int YAW_ITERATIONS = 12;
    public static final float YAW_EPSILON = 0.001F;
    public static final double MISS_HEIGHT_DIFF = -1.0E9;
    public final Box targetBox = new Box();
    public final double[] spawnPos = new double[3];
    public MotionPredictor predictor;
    public ProjectileSolver.Type type;
    public double tickOffset;
    public double posX;
    public double posY;
    public double posZ;
    public double velX;
    public double velY;
    public double velZ;
    public float yawError;
    public double aimHeightFactor;
    public int impactTicks;

    public boolean solve(ProjectileSolver.Type type, Vec3 vec1, Vec3 vec2, boolean flag, MotionPredictor motionPredictor, double value, boolean flag2, float[] floats) {
        this.type = type;
        this.predictor = motionPredictor;
        this.tickOffset = value;
        motionPredictor.getPredictedBox(value + 1.0, this.targetBox);
        float f = yawTo((this.targetBox.minX + this.targetBox.maxX) / 2.0 - vec1.x, (this.targetBox.minZ + this.targetBox.maxZ) / 2.0 - vec1.z);

        for (double d0 : AIM_HEIGHT_FACTORS) {
            this.aimHeightFactor = d0;
            if (this.solveForAimHeight(vec1, vec2, flag, f, flag2, floats)) {
                return true;
            }
        }

        return false;
    }

    public boolean canHit(
        ProjectileSolver.Type type, Vec3 vec1, Vec3 vec2, boolean flag, MotionPredictor motionPredictor, double value, boolean flag2, float value2, float value3
    ) {
        this.type = type;
        this.predictor = motionPredictor;
        this.tickOffset = value;
        return this.simulateHit(vec1, vec2, flag, value2, value3, flag2);
    }

    public int getImpactTicks() {
        return this.impactTicks;
    }

    public boolean solveForAimHeight(Vec3 vec1, Vec3 vec2, boolean flag, float value, boolean flag2, float[] floats) {
        float f = Float.NaN;

        for (int i = 0; i < 12; i++) {
            f = this.solvePitch(vec1, vec2, flag, value);
            if (Float.isNaN(f)) {
                return false;
            }

            if (Math.abs(this.yawError) < 0.001F) {
                break;
            }

            value = Mth.wrapDegrees(value + this.yawError);
        }

        if (Math.abs(this.yawError) >= 0.001F) {
            f = this.solvePitch(vec1, vec2, flag, value);
            if (Float.isNaN(f)) {
                return false;
            }
        }

        if (!this.simulateHit(vec1, vec2, flag, value, f, flag2)) {
            return false;
        }

        floats[0] = value;
        floats[1] = f;
        return true;
    }

    public float solvePitch(Vec3 vec1, Vec3 vec2, boolean flag, float pitch) {
        float f = 89.0F;

        for (float f1 = 86.0F; f1 >= -89.0F; f1 -= 3.0F) {
            if (this.evaluateHeightError(vec1, vec2, flag, pitch, f1) >= 0.0) {
                float f2 = f1;

                for (int i = 0; i < 24; i++) {
                    float f3 = (f + f2) / 2.0F;
                    if (this.evaluateHeightError(vec1, vec2, flag, pitch, f3) >= 0.0) {
                        f2 = f3;
                    } else {
                        f = f3;
                    }
                }

                this.evaluateHeightError(vec1, vec2, flag, pitch, f2);
                return f2;
            }

            f = f1;
        }

        return Float.NaN;
    }

    public double evaluateHeightError(Vec3 vec1, Vec3 vec2, boolean flag, float first, float second) {
        this.initTrajectory(vec1, vec2, flag, first, second);
        double d0 = Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);
        if (d0 < 1.0E-6) {
            return -1.0E9;
        }

        double d1 = this.velX / d0;
        double d2 = this.velZ / d0;

        for (int i = 1; i <= 50; i++) {
            this.preStepSnowballPhysics();
            this.predictor.getPredictedBox(this.tickOffset + i, this.targetBox);
            double d3 = (this.targetBox.minX + this.targetBox.maxX) / 2.0;
            double d4 = this.targetBox.minY + (this.targetBox.maxY - this.targetBox.minY) * this.aimHeightFactor;
            double d5 = (this.targetBox.minZ + this.targetBox.maxZ) / 2.0;
            double d6 = (d3 - this.spawnPos[0]) * d1 + (d5 - this.spawnPos[2]) * d2;
            double d7 = (this.posX - this.spawnPos[0]) * d1 + (this.posZ - this.spawnPos[2]) * d2;
            double d8 = d7 + this.velX * d1 + this.velZ * d2;
            if (d8 >= d6) {
                double d9 = d8 > d7 ? Math.max(0.0, Math.min(1.0, (d6 - d7) / (d8 - d7))) : 0.0;
                this.yawError = Mth.wrapDegrees(
                    yawTo(d3 - this.spawnPos[0], d5 - this.spawnPos[2])
                        - yawTo(this.posX + this.velX * d9 - this.spawnPos[0], this.posZ + this.velZ * d9 - this.spawnPos[2])
                );
                return this.posY + this.velY * d9 - d4;
            }

            if (this.posY < this.targetBox.minY - 16.0) {
                break;
            }

            this.stepPositionAndRodPhysics();
        }

        this.yawError = 0.0F;
        return -1.0E9;
    }

    public boolean simulateHit(Vec3 vec1, Vec3 vec2, boolean flag2, float first, float second, boolean flag22) {
        this.initTrajectory(vec1, vec2, flag2, first, second);

        for (int i = 1; i <= 50; i++) {
            this.preStepSnowballPhysics();
            double d0 = Math.sqrt(this.velX * this.velX + this.velY * this.velY + this.velZ * this.velZ);
            double d1 = this.velX / d0;
            double d2 = this.velY / d0;
            double d3 = this.velZ / d0;
            this.predictor.getPredictedBox(this.tickOffset + i, this.targetBox);
            this.targetBox.inflate(getHitboxExpansion(i));
            double d4 = this.targetBox.intersectRay(this.posX, this.posY, this.posZ, d1, d2, d3);
            boolean flag = d4 >= 0.0 && d4 <= d0;
            if (flag22 && isBlockedByTerrain(this.posX, this.posY, this.posZ, d1, d2, d3, flag ? d4 : d0)) {
                return false;
            }

            if (flag) {
                this.impactTicks = i;
                return true;
            }

            if (this.posY < this.targetBox.minY - 16.0) {
                return false;
            }

            this.stepPositionAndRodPhysics();
        }

        return false;
    }

    public static double getHitboxExpansion(int value) {
        return Math.max(0.0, Math.min(0.3, (value - 2) / 20.0));
    }

    public void preStepSnowballPhysics() {
        if (this.type == ProjectileSolver.Type.SNOWBALL) {
            this.velY = (this.velY - 0.03) * 0.99;
            this.velX *= 0.99;
            this.velZ *= 0.99;
        }
    }

    public void stepPositionAndRodPhysics() {
        if (this.type == ProjectileSolver.Type.ROD) {
            this.velY -= 0.03;
        }

        this.posX = this.posX + this.velX;
        this.posY = this.posY + this.velY;
        this.posZ = this.posZ + this.velZ;
        if (this.type == ProjectileSolver.Type.ROD) {
            this.velX *= 0.92;
            this.velY *= 0.92;
            this.velZ *= 0.92;
        }
    }

    public void initTrajectory(Vec3 vec1, Vec3 vec2, boolean flag, float first, float second) {
        double d0 = Math.toRadians(first);
        double d1 = Math.toRadians(second);
        if (this.type == ProjectileSolver.Type.ROD) {
            this.spawnPos[0] = vec1.x - Math.sin(d0) * 0.3;
            this.spawnPos[1] = vec1.y;
            this.spawnPos[2] = vec1.z + Math.cos(d0) * 0.3;
            double d2 = Math.max(-5.0, Math.min(5.0, -Math.tan(d1)));
            double d3 = Math.sqrt(1.0 + d2 * d2);
            double d4 = 0.6 / d3 + 0.5;
            this.velX = -Math.sin(d0) * d4;
            this.velY = d2 * d4;
            this.velZ = Math.cos(d0) * d4;
        } else {
            this.spawnPos[0] = vec1.x;
            this.spawnPos[1] = vec1.y - 0.1;
            this.spawnPos[2] = vec1.z;
            double d5 = Math.cos(d1);
            this.velX = -Math.sin(d0) * d5 * 1.5 + vec2.x;
            this.velY = -Math.sin(d1) * 1.5 + (flag ? 0.0 : vec2.y);
            this.velZ = Math.cos(d0) * d5 * 1.5 + vec2.z;
        }

        this.posX = this.spawnPos[0];
        this.posY = this.spawnPos[1];
        this.posZ = this.spawnPos[2];
    }

    public static boolean isBlockedByTerrain(double value, double value2, double value3, double value4, double value5, double value6, double value7) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null
            ? false
            : minecraft.level
                    .clip(
                        new ClipContext(
                            new Vec3(value, value2, value3),
                            new Vec3(value + value4 * value7, value2 + value5 * value7, value3 + value6 * value7),
                            Block.COLLIDER,
                            Fluid.NONE,
                            CollisionContext.empty()
                        )
                    )
                    .getType()
                == HitResult.Type.BLOCK;
    }

    public static float yawTo(double first, double second) {
        return (float)Math.toDegrees(Math.atan2(second, first)) - 90.0F;
    }

    public enum Type {
        SNOWBALL,
        ROD;
    }
}
