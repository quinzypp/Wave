package dev.adapt.v3.feature.helper.rotation;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.MouseUpdateEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.util.player.RotationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;

public final class RotationState implements Listener {
    public static final Minecraft MC = Minecraft.getInstance();
    public Vec2 cameraRotation;
    public boolean updatedThisFrame;
    public float yBobO;
    public float yBob;
    public float xBobO;
    public float xBob;

    public RotationState() {
        EventManager.register(this);
    }

    @Subscribe(priority = 1)
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (MC.player != null && !event.isUnlockedCursor()) {
            if (this.cameraRotation == null) {
                this.captureFromPlayer();
            }

            double d0 = event.getSensitivityMultiplier();
            double d1 = event.getDeltaX() * d0;
            double d2 = event.getDeltaY() * d0;
            byte b0 = 1;
            if (MC.options.invertMouseY().get()) {
                b0 = -1;
            }

            byte b1 = 1;
            if (MC.options.invertMouseX().get()) {
                b1 = -1;
            }

            float f = (float)(d1 * b1) * 0.15F;
            float f1 = (float)(d2 * b0) * 0.15F;
            float f2 = this.cameraRotation.x + f;
            float f3 = this.cameraRotation.y + f1;
            this.cameraRotation = new Vec2(f2, Math.clamp(f3 % 360.0F, -90.0F, 90.0F));
        }

        this.updatedThisFrame = true;
    }

    public void captureFromPlayer() {
        this.cameraRotation = RotationUtility.getPlayerRotation();
        this.yBobO = MC.player.yBobO;
        this.yBob = MC.player.yBob;
        this.xBobO = MC.player.xBobO;
        this.xBob = MC.player.xBob;
    }

    public void tickBobbing() {
        if (this.cameraRotation != null) {
            this.yBobO = this.yBob;
            this.xBobO = this.xBob;
            this.xBob = this.xBob + (this.cameraRotation.y - this.xBob) * 0.5F;
            this.yBob = this.yBob + (this.cameraRotation.x - this.yBob) * 0.5F;
        }
    }

    public void onTurnPlayerTail() {
        this.updatedThisFrame = false;
    }

    public void checkReset() {
        if (!this.updatedThisFrame) {
            this.cameraRotation = null;
        }
    }

    public float getCameraYaw(float yaw) {
        return this.cameraRotation == null ? yaw : this.cameraRotation.x;
    }

    public float getCameraPitch(float pitch) {
        return this.cameraRotation == null ? pitch : this.cameraRotation.y;
    }

    public float getYBobO(float value) {
        return this.cameraRotation == null ? value : this.yBobO;
    }

    public float getXBobO(float value) {
        return this.cameraRotation == null ? value : this.xBobO;
    }

    public float getYBob(float value) {
        return this.cameraRotation == null ? value : this.yBob;
    }

    public float getXBob(float value) {
        return this.cameraRotation == null ? value : this.xBob;
    }

    public Vec2 getCameraRotation() {
        return this.cameraRotation;
    }

    public void setCameraRotation(Vec2 cameraRotation) {
        this.cameraRotation = cameraRotation;
    }

    public void setUpdatedThisFrame(boolean updatedThisFrame) {
        this.updatedThisFrame = updatedThisFrame;
    }
}
