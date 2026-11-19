package dev.adapt.v3.feature.module.impl.movement.speed.mode;

import dev.adapt.v3.event.impl.MoveEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.impl.StepEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.impl.movement.TargetStrafeModule;
import dev.adapt.v3.feature.module.impl.movement.speed.SpeedModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class NcpSpeed extends ModuleMode<SpeedModule> {
    public static final Minecraft mc = Minecraft.getInstance();
    public double moveSpeed;
    public double bunnySlope;
    public double halfStepCooldown;
    public boolean slowHop;
    public boolean collided;
    public boolean flaggedRecently;
    public int stage;
    public long lastLagBackMs;

    public NcpSpeed(SpeedModule speedModule) {
        super(speedModule);
    }

    @Override
    public void onEnable() {
        this.collided = mc.player != null && mc.player.horizontalCollision;
        this.slowHop = false;
        if (mc.player != null) {
            this.moveSpeed = getBaseMoveSpeed();
        }

        this.bunnySlope = 0.0;
        this.stage = 2;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        TimerHelper.getInstance().setTimerSpeed(1.0F);
        super.onDisable();
    }

    @Subscribe
    public void onStep(StepEvent event) {
        if (event.getStage() == StepEvent.Stage.POST) {
            double d0 = event.getSteppedHeight();
            if (d0 > 0.7) {
                this.bunnySlope = 0.0;
            }

            if (d0 == 0.5) {
                this.halfStepCooldown = 0.75;
            }
        }
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (mc.player != null && mc.level != null) {
            TimerHelper.getInstance().setTimerSpeed(1.2F);
            if (mc.player.horizontalCollision) {
                this.collided = true;
            }

            if (this.collided) {
                this.stage = -1;
            }

            if (this.halfStepCooldown > 0.0) {
                this.halfStepCooldown -= 0.25;
            }

            this.bunnySlope = this.bunnySlope - (this.bunnySlope > 1.0 ? 0.12 : 0.11);
            if (this.bunnySlope < 0.0) {
                this.bunnySlope = 0.0;
            }

            if (!isOnLiquid() && hasGroundBelow(0.01) && hasMovementInput()) {
                this.collided = mc.player.horizontalCollision;
                if (this.stage >= 0 || this.collided) {
                    this.stage = 0;
                    double d0 = 0.407 + getJumpBoostAmplifier() * 0.1;
                    if (this.halfStepCooldown == 0.0) {
                        mc.player.jumpFromGround();
                        Vec3 vec3 = mc.player.getDeltaMovement();
                        mc.player.setDeltaMovement(vec3.x, d0, vec3.z);
                        event.setY(d0);
                    }

                    this.bunnySlope++;
                    this.slowHop = this.bunnySlope > 1.0 && !this.slowHop;
                    if (this.bunnySlope > 1.12) {
                        this.bunnySlope = 1.12;
                    }
                }
            }

            this.moveSpeed = this.calculateStageSpeed(this.stage) + 0.0331;
            this.moveSpeed *= 0.91;
            if (this.halfStepCooldown > 0.0) {
                this.moveSpeed = this.moveSpeed * (0.7 - getSpeedAmplifier() * 0.1);
            }

            if (this.stage < 0) {
                this.moveSpeed = getBaseMoveSpeed();
            }

            if (this.slowHop) {
                this.moveSpeed *= 0.95;
            }

            if (isOnLiquid()) {
                this.moveSpeed = 0.12;
            }

            if (hasMovementInput()) {
                setMoveEventSpeed(event, this.moveSpeed);
                this.stage++;
            }
        }
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket && mc.player != null) {
            if (this.module.isLagBackDisable()) {
                mc.player.setOnGround(false);
                Vec3 vec3 = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(0.0, vec3.y, 0.0);
                this.module.toggle();
            }

            this.stage = -4;
            this.lastLagBackMs = System.currentTimeMillis();
        }
    }

    public boolean timeSinceLagBack(long delayMs) {
        return System.currentTimeMillis() - this.lastLagBackMs >= delayMs;
    }

    public double calculateStageSpeed(int value) {
        double d0 = getBaseMoveSpeed() + 0.028 * getSpeedAmplifier() + getSpeedAmplifier() / 15.0;
        double d1 = 0.4145 + getSpeedAmplifier() / 12.5;
        double d2 = value / 500.0 * 2.0;
        if (value == 0) {
            this.flaggedRecently = !this.timeSinceLagBack(500L);
            d0 = 0.64 + (getSpeedAmplifier() + 0.028 * getSpeedAmplifier()) * 0.134;
        } else if (value == 1) {
            d0 = d1;
        } else if (value >= 2) {
            d0 = d1 - d2;
        }

        if (this.flaggedRecently || !this.timeSinceLagBack(500L) || this.collided) {
            d0 = 0.2;
            if (value == 0) {
                d0 = 0.0;
            }
        }

        return Math.max(d0, this.flaggedRecently ? d0 : getBaseMoveSpeed() + 0.028 * getSpeedAmplifier());
    }

    public static void setMoveEventSpeed(MoveEvent event, double speed) {
        Vec2 vec2 = mc.player.input.getMoveVector();
        double d0 = vec2.y;
        double d1 = vec2.x;
        float f = TargetStrafeModule.overrideYaw(RotationHelper.getState().getCameraYaw(mc.player.getYRot()));
        if (d0 == 0.0 && d1 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
        } else {
            if (d0 != 0.0) {
                if (d1 > 0.0) {
                    f = f + (d0 > 0.0 ? -45 : 45);
                } else if (d1 < 0.0) {
                    f = f + (d0 > 0.0 ? 45 : -45);
                }

                d1 = 0.0;
                if (d0 > 0.0) {
                    d0 = 1.0;
                } else if (d0 < 0.0) {
                    d0 = -1.0;
                }
            }

            event.setX(d0 * speed * Math.cos(Math.toRadians(f + 90.0F)) + d1 * speed * Math.sin(Math.toRadians(f + 90.0F)));
            event.setZ(d0 * speed * Math.sin(Math.toRadians(f + 90.0F)) - d1 * speed * Math.cos(Math.toRadians(f + 90.0F)));
        }
    }

    public static boolean hasMovementInput() {
        Vec2 vec2 = mc.player.input.getMoveVector();
        return vec2.x != 0.0F || vec2.y != 0.0F;
    }

    public static double getBaseMoveSpeed() {
        double d0 = 0.2873;
        MobEffectInstance mobeffectinstance = mc.player.getEffect(MobEffects.SPEED);
        if (mobeffectinstance != null) {
            d0 *= 1.0 + 0.2 * (mobeffectinstance.getAmplifier() + 1);
        }

        return d0;
    }

    public static int getSpeedAmplifier() {
        MobEffectInstance mobeffectinstance = mc.player.getEffect(MobEffects.SPEED);
        return mobeffectinstance == null ? 0 : mobeffectinstance.getAmplifier() + 1;
    }

    public static int getJumpBoostAmplifier() {
        MobEffectInstance mobeffectinstance = mc.player.getEffect(MobEffects.JUMP_BOOST);
        return mobeffectinstance == null ? 0 : mobeffectinstance.getAmplifier() + 1;
    }

    public static boolean hasGroundBelow(double value) {
        return !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, -value, 0.0));
    }

    public static boolean isOnLiquid() {
        if (mc.player.isInWater()) {
            return true;
        }

        boolean flag = false;
        AABB aabb = mc.player.getBoundingBox();
        int i = (int)aabb.minY;

        for (int j = Mth.floor(aabb.minX); j < Mth.floor(aabb.maxX) + 1; j++) {
            for (int k = Mth.floor(aabb.minZ); k < Mth.floor(aabb.maxZ) + 1; k++) {
                BlockState blockstate = mc.level.getBlockState(new BlockPos(j, i, k));
                if (!blockstate.isAir()) {
                    if (!(blockstate.getBlock() instanceof LiquidBlock)) {
                        return false;
                    }

                    flag = true;
                }
            }
        }

        return flag;
    }

    @Override
    public Enum getMode() {
        return SpeedModule.Mode.NCP;
    }
}
