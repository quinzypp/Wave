package dev.adapt.v3.feature.helper.projectile;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MotionPredictor {
    public static final int MAX_PREDICT_TICKS = 80;
    public static final double GRAVITY = 0.08;
    public static final double AIR_DRAG = 0.98;
    public static final double STEP_HEIGHT = 0.6;
    public static final int MAX_AIR_TICKS = 60;
    public static final int HISTORY_SIZE = 8;
    public static final int REGRESSION_WINDOW_TICKS = 8;
    public static final double MIN_REVERSE_SPEED = 0.05;
    public static final int STATIONARY_TICKS = 4;
    public static final double TELEPORT_DISTANCE = 8.0;
    public static final double GROUND_PROBE_OFFSET = 0.01;
    public static final double[] KNOWN_LAUNCH_VELOCITIES = new double[]{0.42, 0.52, 0.62, 0.4, 0.36, -0.0784, 0.0};
    public static final double MIN_JUMP_VELOCITY = 0.41;
    public static final double MAX_JUMP_VELOCITY = 0.75;
    public static final double SINGLE_SAMPLE_PENALTY = 0.1;
    public static final double MULTI_SAMPLE_PENALTY = 0.01;
    public static final double TICK_GAP_PENALTY = 0.004;
    public static final double AIRBORNE_PRIOR_PENALTY = 0.02;
    public static final double MAX_FIT_ERROR = 0.08;
    public static final double MAX_COLLISION_SWEEP = 24.0;
    public static final int NO_LANDING_TICK = -1073741824;
    public static final double[] DRAG_POWERS = new double[61];
    public static final double[] GRAVITY_VEL_OFFSETS = new double[61];
    public static final double[] DRAG_POS_SUMS = new double[61];
    public static final double[] GRAVITY_POS_SUMS = new double[61];
    public final List<Box> collisionBoxes = new ArrayList<>();
    public final Box queryBox = new Box();
    public final Box currentBox = new Box();
    public final Box stepBox = new Box();
    public final int[] sampleTicks = new int[8];
    public final double[] historyX = new double[8];
    public final double[] historyY = new double[8];
    public final double[] historyZ = new double[8];
    public final boolean[] historyOnGround = new boolean[8];
    public int sampleCount;
    public Object target;
    public int tickCounter;
    public double width;
    public double height;
    public double lastGroundY;
    public boolean hasGroundReference;
    public int lastLandingTick = -1073741824;
    public double velocityY;
    public boolean jumping;
    public double jumpVelocity = 0.42;
    public final double[] predictedX = new double[81];
    public final double[] predictedY = new double[81];
    public final double[] predictedZ = new double[81];
    public boolean collidedX;
    public boolean collidedY;
    public boolean collidedZ;

    public void reset() {
        this.target = null;
        this.sampleCount = 0;
        this.hasGroundReference = false;
        this.jumping = false;
        this.lastLandingTick = -1073741824;
    }

    public void recordSample(Object key, double value, double value2, double value3, double value4, double value5) {
        if (key != this.target) {
            this.reset();
            this.target = key;
        }

        this.tickCounter++;
        this.width = value4;
        this.height = value5;
        if (this.sampleCount > 0) {
            double d0 = value - this.historyX[0];
            double d1 = value2 - this.historyY[0];
            double d2 = value3 - this.historyZ[0];
            if (d0 == 0.0 && d1 == 0.0 && d2 == 0.0) {
                return;
            }

            if (d0 * d0 + d1 * d1 + d2 * d2 > 64.0) {
                this.reset();
                this.target = key;
            }
        }

        boolean flag = this.checkOnGround(value, value2, value3);
        int i = Math.min(this.sampleCount, 7);
        System.arraycopy(this.sampleTicks, 0, this.sampleTicks, 1, i);
        System.arraycopy(this.historyX, 0, this.historyX, 1, i);
        System.arraycopy(this.historyY, 0, this.historyY, 1, i);
        System.arraycopy(this.historyZ, 0, this.historyZ, 1, i);
        System.arraycopy(this.historyOnGround, 0, this.historyOnGround, 1, i);
        this.sampleTicks[0] = this.tickCounter;
        this.historyX[0] = value;
        this.historyY[0] = value2;
        this.historyZ[0] = value3;
        this.historyOnGround[0] = flag;
        this.sampleCount = i + 1;
        if (flag) {
            if (this.sampleCount > 1 && !this.historyOnGround[1]) {
                this.lastLandingTick = this.tickCounter;
            }

            this.lastGroundY = value2;
            this.hasGroundReference = true;
            this.velocityY = 0.0;
        } else {
            this.estimateVerticalVelocity();
        }
    }

    public Object getTarget() {
        return this.target;
    }

    public boolean isReady() {
        return this.sampleCount >= 3 || this.sampleCount >= 1 && this.getTicksSinceUpdate() >= 4;
    }

    public int getTicksSinceUpdate() {
        return this.tickCounter - this.sampleTicks[0];
    }

    public void simulate() {
        boolean flag = this.getTicksSinceUpdate() >= 4;
        double d0 = 0.0;
        double d1 = 0.0;
        if (!flag) {
            d0 = this.estimateHorizontalVelocity(this.historyX);
            d1 = this.estimateHorizontalVelocity(this.historyZ);
            if (this.sampleCount >= 2) {
                int i = this.sampleTicks[0] - this.sampleTicks[1];
                if (i < 4) {
                    double d2 = (this.historyX[0] - this.historyX[1]) / i;
                    double d4 = (this.historyZ[0] - this.historyZ[1]) / i;
                    if (d2 * d0 + d4 * d1 < 0.0 && d2 * d2 + d4 * d4 >= 0.0025000000000000005) {
                        d0 = d2;
                        d1 = d4;
                    }
                }
            }
        }

        double d12 = this.historyX[0];
        double d3 = this.historyY[0];
        double d5 = this.historyZ[0];
        boolean flag1 = this.historyOnGround[0];
        double d6 = flag1 ? 0.0 : this.velocityY;
        boolean flag2 = this.jumping && !flag && (!flag1 || this.tickCounter - this.lastLandingTick <= 2);
        double d7 = Math.min(24.0, 80.0 * Math.sqrt(d0 * d0 + d1 * d1));
        double d8 = Math.sqrt(d0 * d0 + d1 * d1);
        double d9 = d8 > 0.0 ? d0 / d8 * d7 : 0.0;
        double d10 = d8 > 0.0 ? d1 / d8 * d7 : 0.0;
        double d11 = this.width / 2.0;
        this.queryBox
            .set(
                Math.min(d12, d12 + d9) - d11 - 1.0,
                d3 - 12.0,
                Math.min(d5, d5 + d10) - d11 - 1.0,
                Math.max(d12, d12 + d9) + d11 + 1.0,
                d3 + this.height + 0.6 + 2.0,
                Math.max(d5, d5 + d10) + d11 + 1.0
            );
        this.collisionBoxes.clear();
        collectBlockCollisions(this.queryBox, this.collisionBoxes);
        this.predictedX[0] = d12;
        this.predictedY[0] = d3;
        this.predictedZ[0] = d5;

        for (int j = 1; j <= 80; j++) {
            if (flag1 && flag2) {
                d6 = this.jumpVelocity;
            }

            boolean flag3 = flag1;
            this.moveWithCollisions(d12, d3, d5, d0, d6, d1, flag3);
            d12 = (this.currentBox.minX + this.currentBox.maxX) / 2.0;
            d3 = this.currentBox.minY;
            d5 = (this.currentBox.minZ + this.currentBox.maxZ) / 2.0;
            flag1 = d6 <= 0.0 && this.clipY(this.currentBox, -0.01) > -0.01;
            if (this.collidedX) {
                d0 = 0.0;
            }

            if (this.collidedZ) {
                d1 = 0.0;
            }

            if (this.collidedY) {
                d6 = 0.0;
            }

            d6 = (d6 - 0.08) * 0.98;
            this.predictedX[j] = d12;
            this.predictedY[j] = d3;
            this.predictedZ[j] = d5;
        }
    }

    public void getPredictedBox(double value, Box box) {
        double d0 = Math.max(0.0, Math.min(80.0, value + this.getTicksSinceUpdate()));
        int i = Math.min(79, (int)d0);
        double d1 = d0 - i;
        double d2 = this.predictedX[i] + (this.predictedX[i + 1] - this.predictedX[i]) * d1;
        double d3 = this.predictedY[i] + (this.predictedY[i + 1] - this.predictedY[i]) * d1;
        double d4 = this.predictedZ[i] + (this.predictedZ[i + 1] - this.predictedZ[i]) * d1;
        double d5 = this.width / 2.0;
        box.set(d2 - d5, d3, d4 - d5, d2 + d5, d3 + this.height, d4 + d5);
    }

    public double estimateHorizontalVelocity(double[] doubles) {
        double d0 = 0.0;
        double d1 = 0.0;
        int i = 0;

        for (int j = 0; j < this.sampleCount && this.sampleTicks[0] - this.sampleTicks[j] <= 8; j++) {
            d0 += this.sampleTicks[j];
            d1 += doubles[j];
            i++;
        }

        if (i < 2) {
            return 0.0;
        }

        d0 /= i;
        d1 /= i;
        double d4 = 0.0;
        double d2 = 0.0;

        for (int k = 0; k < i; k++) {
            double d3 = this.sampleTicks[k] - d0;
            d4 += d3 * (doubles[k] - d1);
            d2 += d3 * d3;
        }

        return d2 == 0.0 ? 0.0 : d4 / d2;
    }

    public void estimateVerticalVelocity() {
        int i = this.sampleCount > 1 ? this.sampleTicks[0] - this.sampleTicks[1] : 2;
        boolean flag = this.sampleCount > 1 && !this.historyOnGround[1];
        if (!this.hasGroundReference) {
            this.estimateFallbackVerticalVelocity(i, flag);
        } else {
            double d0 = this.historyY[0] - this.lastGroundY;
            double d1 = Double.MAX_VALUE;
            double d2 = 0.0;
            int j = 0;
            if (flag) {
                double d3 = this.historyY[1] - this.lastGroundY;

                for (int k = 2; k <= 60; k++) {
                    double d5 = (d0 - GRAVITY_POS_SUMS[k]) / DRAG_POS_SUMS[k];

                    for (int l = 1; l <= 3 && l < k; l++) {
                        int i1 = k - l;
                        double d6 = Math.abs(d5 * DRAG_POS_SUMS[i1] + GRAVITY_POS_SUMS[i1] - d3)
                            + 0.004 * Math.abs(l - i)
                            + 0.01 * distanceToKnownLaunchVelocity(d5);
                        if (d6 < d1) {
                            d1 = d6;
                            d2 = d5;
                            j = k;
                        }
                    }
                }
            }

            for (int j1 = 1; j1 <= 3; j1++) {
                double d4 = (d0 - GRAVITY_POS_SUMS[j1]) / DRAG_POS_SUMS[j1];
                double d7 = 0.02 * (flag ? 1 : 0) + 0.1 * distanceToKnownLaunchVelocity(d4) + 0.004 * Math.max(0, j1 - i);
                if (d7 < d1) {
                    d1 = d7;
                    d2 = d4;
                    j = j1;
                }
            }

            if (d1 > 0.08) {
                this.estimateFallbackVerticalVelocity(i, flag);
            } else {
                this.velocityY = d2 * DRAG_POWERS[j] + GRAVITY_VEL_OFFSETS[j];
                this.jumping = d2 >= 0.41 && d2 <= 0.75;
                if (this.jumping) {
                    this.jumpVelocity = d2;
                }
            }
        }
    }

    public void estimateFallbackVerticalVelocity(int value, boolean flag) {
        int i = Math.max(1, Math.min(2, value));
        if (this.sampleCount < 2) {
            this.velocityY = 0.0;
        } else if (flag) {
            double d0 = (this.historyY[0] - this.historyY[1] - GRAVITY_POS_SUMS[i]) / DRAG_POS_SUMS[i];
            this.velocityY = d0 * DRAG_POWERS[i] + GRAVITY_VEL_OFFSETS[i];
        } else {
            this.velocityY = (this.historyY[0] - this.historyY[1]) / i;
        }

        this.jumping = false;
    }

    public static double distanceToKnownLaunchVelocity(double range) {
        double d0 = Double.MAX_VALUE;

        for (double d1 : KNOWN_LAUNCH_VELOCITIES) {
            d0 = Math.min(d0, Math.abs(range - d1));
        }

        return d0;
    }

    public boolean checkOnGround(double value, double value2, double value3) {
        double d0 = this.width / 2.0;
        this.currentBox.set(value - d0, value2, value3 - d0, value + d0, value2 + this.height, value3 + d0);
        this.queryBox.set(this.currentBox.minX, value2 - 1.0, this.currentBox.minZ, this.currentBox.maxX, value2 + 0.5, this.currentBox.maxZ);
        this.collisionBoxes.clear();
        collectBlockCollisions(this.queryBox, this.collisionBoxes);
        return this.clipY(this.currentBox, -0.01) > -0.01;
    }

    public static void collectBlockCollisions(Box box, List list) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            for (VoxelShape voxelshape : minecraft.level.getBlockCollisions(null, new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ))) {
                for (AABB aabb : voxelshape.toAabbs()) {
                    list.add(new Box().set(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ));
                }
            }
        }
    }

    public void moveWithCollisions(double value, double value2, double value3, double value4, double value5, double value6, boolean flag2) {
        double d0 = this.width / 2.0;
        this.currentBox.set(value - d0, value2, value3 - d0, value + d0, value2 + this.height, value3 + d0);
        double d1 = this.clipY(this.currentBox, value5);
        offsetBox(this.currentBox, 0.0, d1, 0.0);
        double d2 = this.clipX(this.currentBox, value4);
        offsetBox(this.currentBox, d2, 0.0, 0.0);
        double d3 = this.clipZ(this.currentBox, value6);
        offsetBox(this.currentBox, 0.0, 0.0, d3);
        this.collidedX = d2 != value4;
        this.collidedY = d1 != value5;
        this.collidedZ = d3 != value6;
        boolean flag = this.collidedY && value5 < 0.0;
        if ((flag2 || flag) && (this.collidedX || this.collidedZ)) {
            this.stepBox.set(value - d0, value2, value3 - d0, value + d0, value2 + this.height, value3 + d0);
            double d4 = this.clipY(this.stepBox, 0.6);
            offsetBox(this.stepBox, 0.0, d4, 0.0);
            double d5 = this.clipX(this.stepBox, value4);
            offsetBox(this.stepBox, d5, 0.0, 0.0);
            double d6 = this.clipZ(this.stepBox, value6);
            offsetBox(this.stepBox, 0.0, 0.0, d6);
            offsetBox(this.stepBox, 0.0, this.clipY(this.stepBox, -d4), 0.0);
            if (d5 * d5 + d6 * d6 > d2 * d2 + d3 * d3) {
                this.currentBox.set(this.stepBox.minX, this.stepBox.minY, this.stepBox.minZ, this.stepBox.maxX, this.stepBox.maxY, this.stepBox.maxZ);
                this.collidedX = d5 != value4;
                this.collidedZ = d6 != value6;
                this.collidedY = true;
            }
        }
    }

    public double clipY(Box box2, double value) {
        for (int i = 0; i < this.collisionBoxes.size(); i++) {
            Box box = this.collisionBoxes.get(i);
            if (box.maxX > box2.minX && box.minX < box2.maxX && box.maxZ > box2.minZ && box.minZ < box2.maxZ) {
                if (value > 0.0 && box.minY >= box2.maxY) {
                    value = Math.min(value, box.minY - box2.maxY);
                } else if (value < 0.0 && box.maxY <= box2.minY) {
                    value = Math.max(value, box.maxY - box2.minY);
                }
            }
        }

        return value;
    }

    public double clipX(Box box2, double value) {
        for (int i = 0; i < this.collisionBoxes.size(); i++) {
            Box box = this.collisionBoxes.get(i);
            if (box.maxY > box2.minY && box.minY < box2.maxY && box.maxZ > box2.minZ && box.minZ < box2.maxZ) {
                if (value > 0.0 && box.minX >= box2.maxX) {
                    value = Math.min(value, box.minX - box2.maxX);
                } else if (value < 0.0 && box.maxX <= box2.minX) {
                    value = Math.max(value, box.maxX - box2.minX);
                }
            }
        }

        return value;
    }

    public double clipZ(Box box2, double value) {
        for (int i = 0; i < this.collisionBoxes.size(); i++) {
            Box box = this.collisionBoxes.get(i);
            if (box.maxX > box2.minX && box.minX < box2.maxX && box.maxY > box2.minY && box.minY < box2.maxY) {
                if (value > 0.0 && box.minZ >= box2.maxZ) {
                    value = Math.min(value, box.minZ - box2.maxZ);
                } else if (value < 0.0 && box.maxZ <= box2.minZ) {
                    value = Math.max(value, box.maxZ - box2.minZ);
                }
            }
        }

        return value;
    }

    public static void offsetBox(Box box, double value, double value2, double value3) {
        box.set(box.minX + value, box.minY + value2, box.minZ + value3, box.maxX + value, box.maxY + value2, box.maxZ + value3);
    }

    static {
        DRAG_POWERS[0] = 1.0;

        for (int i = 1; i <= 60; i++) {
            DRAG_POS_SUMS[i] = DRAG_POS_SUMS[i - 1] + DRAG_POWERS[i - 1];
            GRAVITY_POS_SUMS[i] = GRAVITY_POS_SUMS[i - 1] + GRAVITY_VEL_OFFSETS[i - 1];
            DRAG_POWERS[i] = DRAG_POWERS[i - 1] * 0.98;
            GRAVITY_VEL_OFFSETS[i] = (GRAVITY_VEL_OFFSETS[i - 1] - 0.08) * 0.98;
        }
    }
}
