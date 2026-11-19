package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class StrafeEvent extends CancellableEvent {
    public float forward;
    public float strafe;
    public float speed;
    public float yaw;

    public StrafeEvent(float value, float value2, float value3, float value4) {
        this.forward = value;
        this.strafe = value2;
        this.speed = value3;
        this.yaw = value4;
    }

    public void setMotionPartial(double first, double second) {
        this.setSpeed((float)(this.forward != 0.0F && this.strafe != 0.0F ? first * 0.98F : first));
        LocalPlayer localplayer = Minecraft.getInstance().player;
        Vec3 vec3 = localplayer.getDeltaMovement();
        localplayer.setDeltaMovement(vec3.x * second, vec3.y, vec3.z * second);
    }

    public void setMotion(double motion) {
        this.setSpeed((float)(this.forward != 0.0F && this.strafe != 0.0F ? motion * 0.98F : motion));
        LocalPlayer localplayer = Minecraft.getInstance().player;
        localplayer.setDeltaMovement(0.0, localplayer.getDeltaMovement().y, 0.0);
    }

    public float getForward() {
        return this.forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
