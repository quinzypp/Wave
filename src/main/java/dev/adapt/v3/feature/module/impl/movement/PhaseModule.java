package dev.adapt.v3.feature.module.impl.movement;

import java.util.Set;
import dev.adapt.v3.event.impl.BlockAABBEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.PushOutOfBlocksEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.impl.SendPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.movement.MovementHelper;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.mixin.LocalPlayerAccessor;
import dev.adapt.v3.mixin.ServerboundMovePlayerPacketAccessor;
import dev.adapt.v3.util.TimerUtility;
import dev.adapt.v3.util.player.MoveUtility;
import java.util.EnumSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PhaseModule extends Module {
    public final TimerUtility sneakPhaseTimer = new TimerUtility();
    public boolean clipping;
    public double phaseTicks;
    public double jitterStep;
    public float lockedYaw;
    public float lockedPitch;
    public float yawJitter;
    public float pitchJitter;
    public int collidePacketCount;

    public PhaseModule() {
        super("Phase", "Lets you clip through walls.", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        LocalPlayer localplayer = mc.player;
        this.yawJitter = this.pitchJitter = 0.0F;
        if (localplayer != null && mc.level != null) {
            this.clipping = isInsideBlock();
            if (hasHorizontalBlockCollision(0.001) || localplayer.horizontalCollision) {
                localplayer.setOnGround(false);
                localplayer.noPhysics = true;
                localplayer.setDeltaMovement(0.0, localplayer.getDeltaMovement().y, 0.0);
                MovementHelper.setAirSpeed(0.0F);
                moveForward(0.006000000238415);
                this.phaseTicks = 0.0;
                this.jitterStep = 0.0;
            }
        }
    }

    @Override
    public void onDisable() {
        TimerHelper.getInstance().reset();
    }

    @Subscribe
    public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        event.cancel();
    }

    @Subscribe
    public void onBlockAABB(BlockAABBEvent event) {
        AABB aabb = event.getBoundingBox();
        if (aabb != null && aabb.maxY > mc.player.getBoundingBox().minY) {
            event.cancel();
        }
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!event.isCancelled()
            && event.getPacket() instanceof ClientboundPlayerPositionPacket clientboundplayerpositionpacket
            && mc.player != null
            && mc.getConnection() != null) {
            EnumSet enumset = clientboundplayerpositionpacket.relatives().isEmpty()
                ? EnumSet.noneOf(Relative.class)
                : EnumSet.copyOf(clientboundplayerpositionpacket.relatives());
            enumset.remove(Relative.Y_ROT);
            enumset.remove(Relative.X_ROT);
            ClientboundPlayerPositionPacket clientboundplayerpositionpacket1 = new ClientboundPlayerPositionPacket(
                clientboundplayerpositionpacket.id(), clientboundplayerpositionpacket.change().withRotation(mc.player.getYRot(), mc.player.getXRot()), enumset
            );
            event.cancel();
            this.handlePacket(clientboundplayerpositionpacket1);
            this.clipping = true;
        }
    }

    public void handlePacket(Packet packet) {
        packet.handle(mc.getConnection());
    }

    @Subscribe
    public void onSendPacket(SendPacketEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null
            && event.getPacket() instanceof ServerboundMovePlayerPacket serverboundmoveplayerpacket
            && serverboundmoveplayerpacket.hasPosition()
            && !isInsideBlock()
            && localplayer.horizontalCollision) {
            if (++this.collidePacketCount >= 5) {
                double[] adouble = computeDirectionalOffset(0.2);
                ServerboundMovePlayerPacketAccessor serverboundmoveplayerpacketaccessor = (ServerboundMovePlayerPacketAccessor)serverboundmoveplayerpacket;
                serverboundmoveplayerpacketaccessor.adapt$setX(serverboundmoveplayerpacketaccessor.adapt$getX() + adouble[0]);
                serverboundmoveplayerpacketaccessor.adapt$setY(serverboundmoveplayerpacketaccessor.adapt$getY() - 1.0);
                serverboundmoveplayerpacketaccessor.adapt$setZ(serverboundmoveplayerpacketaccessor.adapt$getZ() + adouble[1]);
                this.collidePacketCount = 0;
            }
        }
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null && mc.level != null) {
            LocalPlayerAccessor localplayeraccessor = (LocalPlayerAccessor)localplayer;
            boolean flag = isInsideBlock();
            if (!this.clipping) {
                if (flag) {
                    event.setYaw(this.lockedYaw);
                    event.setPitch(this.lockedPitch);
                } else {
                    this.lockedYaw = localplayer.getYRot();
                    this.lockedPitch = localplayer.getXRot();
                }
            }

            if (this.clipping || flag) {
                if (!localplayer.isShiftKeyDown()) {
                    localplayeraccessor.adapt$setYLast(0.0);
                }

                localplayeraccessor.adapt$setXRotLast(999.0F);
                localplayer.setOnGround(false);
                localplayer.noPhysics = true;
                localplayer.setDeltaMovement(0.0, localplayer.getDeltaMovement().y, 0.0);
                if (mc.options.keyJump.isDown() && localplayer.getY() == (int)localplayer.getY()) {
                    localplayer.jumpFromGround();
                }

                MovementHelper.setAirSpeed(0.0F);
            }

            this.phaseTicks++;
            if (this.phaseTicks < 3.0) {
                this.lockedPitch = this.lockedPitch + (this.phaseTicks == 1.0 ? 15.0F : -15.0F);
            }

            if (mc.options.keyShift.isDown()) {
                localplayeraccessor.adapt$setXRotLast(999.0F);
                double d0 = localplayer.getX();
                double d1 = localplayer.getY();
                double d2 = localplayer.getZ();
                if (!MoveUtility.isMoving()) {
                    if (MoveUtility.hasCollisionAtOffset(0.0, -0.001, 0.0) && !flag) {
                        localplayeraccessor.adapt$setYLast(-99.0);
                        localplayer.setPos(d0, d1 - 1.0, d2);
                        this.sneakPhaseTimer.reset();
                        localplayer.setDeltaMovement(localplayer.getDeltaMovement().x, 0.0, localplayer.getDeltaMovement().z);
                    } else if (this.sneakPhaseTimer.hasTimeElapsed(100L) && localplayer.getY() == (int)localplayer.getY()) {
                        localplayer.setPos(d0, d1 - 0.3, d2);
                    }
                }
            }

            if (flag && this.phaseTicks >= 3.0) {
                if (this.clipping) {
                    moveForward(0.617);
                    this.yawJitter = this.yawJitter + (float)Math.sin(this.jitterStep) * 0.1F;
                    this.pitchJitter = this.pitchJitter + (float)Math.cos(this.jitterStep) * 0.1F;
                    this.jitterStep++;
                } else {
                    moveForward(0.031);
                }
            }

            event.setYaw(event.getYaw() + this.yawJitter);
            event.setPitch(Mth.clamp(event.getPitch() + this.pitchJitter, -90.0F, 90.0F));
        }
    }

    public static Vec2 getMoveVector() {
        return mc.player.input.getMoveVector();
    }

    public static double[] computeDirectionalOffset(double value) {
        LocalPlayer localplayer = mc.player;
        double d0 = Math.cos(Math.toRadians(localplayer.getYRot() + 90.0F));
        double d1 = Math.sin(Math.toRadians(localplayer.getYRot() + 90.0F));
        double d2 = getMoveVector().y;
        double d3 = getMoveVector().x;
        return new double[]{d2 * value * d0 + d3 * value * d1, d2 * value * d1 - d3 * value * d0};
    }

    public static void moveForward(double value) {
        LocalPlayer localplayer = mc.player;
        double d0 = getMoveVector().y;
        double d1 = getMoveVector().x;
        float f = localplayer.getYRot();
        if (d0 != 0.0) {
            if (d1 > 0.0) {
                f += d0 > 0.0 ? -45.0F : 45.0F;
            } else if (d1 < 0.0) {
                f += d0 > 0.0 ? 45.0F : -45.0F;
            }

            d1 = 0.0;
            d0 = d0 > 0.0 ? 1.0 : -1.0;
        }

        double d2 = Math.toRadians(f + 90.0F);
        localplayer.setPos(
            localplayer.getX() + d0 * value * Math.cos(d2) + d1 * value * Math.sin(d2),
            localplayer.getY(),
            localplayer.getZ() + d0 * value * Math.sin(d2) - d1 * value * Math.cos(d2)
        );
    }

    public static boolean hasHorizontalBlockCollision(double value) {
        return MoveUtility.hasCollisionAtOffset(value, 0.0, 0.0)
            || MoveUtility.hasCollisionAtOffset(-value, 0.0, 0.0)
            || MoveUtility.hasCollisionAtOffset(0.0, 0.0, value)
            || MoveUtility.hasCollisionAtOffset(0.0, 0.0, -value);
    }

    public static boolean isInsideBlock() {
        AABB aabb = mc.player.getBoundingBox();

        for (int i = Mth.floor(aabb.minX); i < Mth.floor(aabb.maxX) + 1; i++) {
            for (int j = Mth.floor(aabb.minY); j < Mth.floor(aabb.maxY) + 1; j++) {
                for (int k = Mth.floor(aabb.minZ); k < Mth.floor(aabb.maxZ) + 1; k++) {
                    BlockPos blockpos = new BlockPos(i, j, k);
                    BlockState blockstate = mc.level.getBlockState(blockpos);
                    if (!blockstate.isAir()) {
                        if (blockstate.getBlock() instanceof HopperBlock) {
                            if (aabb.intersects(new AABB(i, j, k, i + 1, j + 1, k + 1))) {
                                return true;
                            }
                        } else {
                            VoxelShape voxelshape = blockstate.getCollisionShape(mc.level, blockpos);
                            if (!voxelshape.isEmpty()) {
                                for (AABB aabb1 : voxelshape.toAabbs()) {
                                    if (aabb.intersects(aabb1.move(blockpos))) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public String getSuffix() {
        return "NCP";
    }
}
