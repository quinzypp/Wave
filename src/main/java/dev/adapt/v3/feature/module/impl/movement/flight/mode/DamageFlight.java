package dev.adapt.v3.feature.module.impl.movement.flight.mode;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.movement.MovementHelper;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.timer.TimerHelper;
import dev.adapt.v3.feature.module.impl.movement.MovementFixModule;
import dev.adapt.v3.feature.module.impl.movement.TargetStrafeModule;
import dev.adapt.v3.feature.module.impl.movement.flight.FlightModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.mixin.LocalPlayerAccessor;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class DamageFlight extends ModuleMode<FlightModule> {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final double[] HOVER_OFFSETS = new double[]{0.0, 1.9999E-8, 1.9999E-8, 0.0, 1.0E-8, 1.99999E-8};
    public final NumberProperty boost = (NumberProperty)new NumberProperty("Boost", 4.8, 1.0, 20.0, 0.1).hideIf(this::isNotSelected);
    public final NumberProperty boostDuration = (NumberProperty)new NumberProperty("Boost Duration", 1.0, 0.1, 5.0, 0.1).hideIf(this::isNotSelected);
    public final BooleanProperty waitForDamage = (BooleanProperty)new BooleanProperty("Wait For Damage", true).hideIf(this::isNotSelected);
    public final BooleanProperty disableOnFlag = (BooleanProperty)new BooleanProperty("Disable On Flag", true).hideIf(this::isNotSelected);
    public final BooleanProperty timer = (BooleanProperty)new BooleanProperty("Timer", true).hideIf(this::isNotSelected);
    public final NumberProperty timerAmount = (NumberProperty)new NumberProperty("Timer Amount", 2.0, 1.0, 5.0, 0.1)
        .hideIf(() -> this.isNotSelected() || !this.timer.getValue());
    public final NumberProperty timerDuration = (NumberProperty)new NumberProperty("Timer Duration", 20.0, 1.0, 100.0, 1.0)
        .hideIf(() -> this.isNotSelected() || !this.timer.getValue());
    public double boostSpeed;
    public double pendingBoostSpeed;
    public int hoverTickIndex;
    public int timerTicks;
    public int lastHurtTime;
    public boolean damaged;

    public DamageFlight(FlightModule flightModule) {
        super(flightModule);
        flightModule.addProperties(this.boost, this.boostDuration, this.waitForDamage, this.disableOnFlag, this.timer, this.timerAmount, this.timerDuration);
    }

    public boolean isNotSelected() {
        return this.module.getCurrentMode() != this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.hoverTickIndex = 0;
        this.timerTicks = 0;
        this.damaged = false;
        this.lastHurtTime = mc.player != null ? mc.player.hurtTime : 0;
        this.boostSpeed = 0.0;
        this.pendingBoostSpeed = 0.0;
        LocalPlayer localplayer = mc.player;
        boolean flag = localplayer != null
            && mc.getConnection() != null
            && localplayer.onGround()
            && localplayer.verticalCollision
            && MoveUtility.hasCollisionAtOffset(0.0, -0.01, 0.0);
        if (flag) {
            if (!mc.hasSingleplayerServer()) {
                for (int i = 0; i < 49; i++) {
                    sendPosPacket(localplayer.getY() + 0.06249, false);
                    sendPosPacket(localplayer.getY(), false);
                }

                sendPosPacket(localplayer.getY(), true);
            }

            setStrafeSpeed(0.3 + getEffectAmplifier(true) * 0.05F);
            localplayer.setDeltaMovement(localplayer.getDeltaMovement().x, 0.42F + getEffectAmplifier(false) * 0.1, localplayer.getDeltaMovement().z);
            if (this.waitForDamage.getValue()) {
                this.pendingBoostSpeed = 13.0 + this.boost.getValue();
            } else {
                this.boostSpeed = 13.0 + this.boost.getValue();
            }
        }
    }

    @Override
    public void onDisable() {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null) {
            localplayer.setDeltaMovement(0.0, localplayer.getDeltaMovement().y, 0.0);
            MovementHelper.setAirSpeed(0.1F);
            if (!localplayer.isCreative() && !localplayer.isSpectator()) {
                localplayer.getAbilities().flying = false;
                localplayer.getAbilities().mayfly = false;
            }
        }

        TimerHelper.getInstance().reset();
        super.onDisable();
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player != null) {
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket && this.disableOnFlag.getValue()) {
                this.module.setEnabled(false);
            } else {
                if (!(
                    event.getPacket() instanceof ClientboundSetEntityMotionPacket clientboundsetentitymotionpacket
                        ? clientboundsetentitymotionpacket.id() != mc.player.getId()
                        : !(event.getPacket() instanceof ClientboundExplodePacket)
                )) {
                    event.cancel();
                }
            }
        }
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null && mc.level != null) {
            int i = localplayer.hurtTime;
            if (!this.damaged && i > this.lastHurtTime) {
                this.damaged = true;
                if (this.pendingBoostSpeed > 0.0) {
                    this.boostSpeed = this.pendingBoostSpeed;
                    this.pendingBoostSpeed = 0.0;
                }
            }

            this.lastHurtTime = i;
            if (this.timer.getValue() && this.damaged) {
                TimerHelper.getInstance()
                    .setTimerSpeed(++this.timerTicks <= this.timerDuration.getValue().intValue() ? this.timerAmount.getValue().floatValue() : 1.0F);
            }

            BlockPos blockpos = BlockPos.containing(localplayer.getX(), localplayer.getY() - 0.2, localplayer.getZ());
            BlockState blockstate = mc.level.getBlockState(blockpos);
            if (!MoveUtility.hasCollisionAtOffset(0.0, -1.0E-7, 0.0)
                && !blockstate.isCollisionShapeFullBlock(mc.level, blockpos)
                && !(blockstate.getBlock() instanceof TransparentBlock)) {
                double d0 = 0.29F + getEffectAmplifier(true) * 0.06F;
                if (this.boostSpeed > 0.0) {
                    if (!MoveUtility.isMoving() || localplayer.horizontalCollision) {
                        this.boostSpeed = 0.0;
                    }

                    d0 += this.boostSpeed / 18.0;
                    this.boostSpeed = this.boostSpeed - (0.155 + getEffectAmplifier(true) * 0.006) / this.boostDuration.getValue();
                }

                setStrafeSpeed(d0);
                localplayer.setDeltaMovement(localplayer.getDeltaMovement().x, 0.0, localplayer.getDeltaMovement().z);
                MovementHelper.setAirSpeed(0.0F);
                localplayer.setOnGround(false);
                ((LocalPlayerAccessor)localplayer).adapt$setYLast(0.0);
                this.hoverTickIndex = this.hoverTickIndex % 5 + 1;
                if (HOVER_OFFSETS[this.hoverTickIndex] != 0.0) {
                    localplayer.setPos(localplayer.getX(), localplayer.getY() + HOVER_OFFSETS[this.hoverTickIndex], localplayer.getZ());
                }
            }
        }
    }

    public static void setStrafeSpeed(double strafeSpeed) {
        LocalPlayer localplayer = mc.player;
        float f = Math.signum(localplayer.input.getMoveVector().y);
        float f1 = Math.signum(localplayer.input.getMoveVector().x);
        if (f == 0.0F && f1 == 0.0F) {
            localplayer.setDeltaMovement(0.0, localplayer.getDeltaMovement().y, 0.0);
        } else {
            double d0 = Math.toRadians(getMovementYaw() + 90.0F - (f != 0.0F ? f * f1 * 45.0F : 0.0F));
            double d1 = f;
            double d2 = f != 0.0F ? 0.0 : localplayer.input.getMoveVector().x * (localplayer.input.keyPresses.shift() ? 0.3 : 1.0);
            localplayer.setDeltaMovement(
                (d1 * Math.cos(d0) + d2 * Math.sin(d0)) * strafeSpeed, localplayer.getDeltaMovement().y, (d1 * Math.sin(d0) - d2 * Math.cos(d0)) * strafeSpeed
            );
        }
    }

    public static float getMovementYaw() {
        LocalPlayer localplayer = mc.player;
        if (TargetStrafeModule.isStrafing()) {
            return TargetStrafeModule.overrideYaw(localplayer.getYRot());
        }

        Adapt adapt = Adapt.getInstance();
        MovementFixModule movementfixmodule = adapt == null ? null : adapt.getModuleManager().getByClass(MovementFixModule.class);
        return !MovementFixHelper.isForceFixActive() && (movementfixmodule == null || !movementfixmodule.isNormalFixActive())
            ? RotationHelper.getState().getCameraYaw(localplayer.getYRot())
            : localplayer.getYRot();
    }

    public static int getEffectAmplifier(boolean flag) {
        if (flag) {
            return MoveUtility.getSpeedEffectLevel();
        }

        MobEffectInstance mobeffectinstance = mc.player.getEffect(MobEffects.JUMP_BOOST);
        return mobeffectinstance == null ? 0 : mobeffectinstance.getAmplifier() + 1;
    }

    public static void sendPosPacket(double value, boolean flag) {
        mc.getConnection().send(new Pos(mc.player.getX(), value, mc.player.getZ(), flag, mc.player.horizontalCollision));
    }

    @Override
    public Enum getMode() {
        return FlightModule.Mode.DAMAGE;
    }
}
