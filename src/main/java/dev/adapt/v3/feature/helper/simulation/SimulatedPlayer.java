package dev.adapt.v3.feature.helper.simulation;

import com.mojang.authlib.GameProfile;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.module.impl.movement.MovementFixModule;
import dev.adapt.v3.mixin.LivingEntityAccessor;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SimulatedPlayer {
    public static final Minecraft MC = Minecraft.getInstance();
    public RemotePlayer simulatedPlayer;
    public final Player sourcePlayer;

    public SimulatedPlayer(Player player) {
        if (MC.level == null) {
            this.sourcePlayer = null;
        } else {
            GameProfile gameprofile = new GameProfile(UUID.randomUUID(), "Simulated Player");
            this.simulatedPlayer = new RemotePlayer(MC.level, gameprofile) {
                @Override
                public void push(Entity entity) {
                }

                @Override
                public void doPush(Entity entity2) {
                }
            };
            this.sourcePlayer = player;
            this.copyState();
        }
    }

    public void copyState() {
        this.simulatedPlayer.noPhysics = this.sourcePlayer.noPhysics;
        this.simulatedPlayer.xo = this.sourcePlayer.xo;
        this.simulatedPlayer.yo = this.sourcePlayer.yo;
        this.simulatedPlayer.zo = this.sourcePlayer.zo;
        this.simulatedPlayer.yRotO = this.sourcePlayer.yRotO;
        this.simulatedPlayer.xRotO = this.sourcePlayer.xRotO;
        this.simulatedPlayer.setPos(this.sourcePlayer.position());
        this.simulatedPlayer.setBoundingBox(this.sourcePlayer.getBoundingBox());
        this.simulatedPlayer.setDeltaMovement(this.sourcePlayer.getDeltaMovement());
        MovementFixModule movementfixmodule = Adapt.getInstance().getModuleManager().getByClass(MovementFixModule.class);
        float f = (movementfixmodule == null || !movementfixmodule.isNormalFixActive()) && this.sourcePlayer == MC.player
            ? RotationHelper.getState().getCameraYaw(this.sourcePlayer.getYRot())
            : this.sourcePlayer.getYRot();
        this.simulatedPlayer.setYRot(f);
        this.simulatedPlayer.setXRot(RotationHelper.getState().getCameraPitch(this.sourcePlayer.getXRot()));
        this.simulatedPlayer.setShiftKeyDown(this.sourcePlayer.isShiftKeyDown());
        this.simulatedPlayer.setOnGround(this.sourcePlayer.onGround());
        this.simulatedPlayer.setSprinting(this.sourcePlayer.isSprinting());

        for (MobEffectInstance mobeffectinstance : this.sourcePlayer.getActiveEffects()) {
            this.simulatedPlayer.addEffect(new MobEffectInstance(mobeffectinstance));
        }

        this.simulatedPlayer.setSpeed(this.sourcePlayer.getSpeed());
        this.simulatedPlayer.fallDistance = this.sourcePlayer.fallDistance;
    }

    public void simulateTicks(int value) {
        LivingEntityAccessor livingentityaccessor = (LivingEntityAccessor)this.simulatedPlayer;

        for (int i = 0; i < value; i++) {
            livingentityaccessor.adapt$travelInAir(new Vec3(this.sourcePlayer.xxa, this.sourcePlayer.yya, this.sourcePlayer.zza));
        }
    }

    public void simulateTick() {
        this.simulateTicks(1);
    }

    public RemotePlayer getSimulatedPlayer() {
        return this.simulatedPlayer;
    }
}
