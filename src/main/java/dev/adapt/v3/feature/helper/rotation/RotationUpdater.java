package dev.adapt.v3.feature.helper.rotation;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.MouseUpdateEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import dev.adapt.v3.util.player.RotationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;

public final class RotationUpdater implements Listener {
    public static final Minecraft MC = Minecraft.getInstance();
    public RotationModel activeModel;
    public Vec2 targetRotation;
    public boolean rotating;
    public boolean keepTargetThisTick;
    public Vec2 startRotation;
    public boolean tickStep;
    public boolean allowWhileCursorUnlocked;

    public RotationUpdater() {
        EventManager.register(this);
    }

    @Subscribe
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (this.startRotation != null && this.targetRotation != null && MC.player != null && this.rotating) {
            if (!this.keepTargetThisTick) {
                this.targetCameraRotation();
                if (this.targetRotation == null) {
                    this.tickStep = false;
                    return;
                }
            }

            float f;
            if (this.tickStep) {
                f = 1.0F;
                this.tickStep = false;
            } else {
                f = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            }

            double d0 = event.getSensitivityMultiplier();
            Vec2 vec2 = this.activeModel.interpolate(this.startRotation, this.targetRotation, f);
            double d1 = vec2.x - MC.player.getYRot();
            double d2 = RotationUtility.toMouseSteps(d1, d0);
            if (MC.options.invertMouseX().get()) {
                d2 *= -1.0;
            }

            double d3 = vec2.y - MC.player.getXRot();
            double d4 = RotationUtility.toMouseSteps(d3, d0);
            if (MC.options.invertMouseY().get()) {
                d4 *= -1.0;
            }

            event.setDeltaX(d2);
            event.setDeltaY(d4);
            event.markModified();
            if (!this.keepTargetThisTick && RotationUtility.getAngleDifference(vec2, this.targetRotation) == 0.0) {
                this.activeModel = null;
                this.targetRotation = null;
                this.rotating = false;
            }
        } else {
            this.tickStep = false;
        }
    }

    @Subscribe(priority = 8)
    public void onPreTick(PreTickEvent event) {
        if (MC.player != null) {
            this.applyTickRotation();
            this.updateReturnToCamera();
            this.setStartRotation(RotationUtility.getPlayerRotation());
            this.allowWhileCursorUnlocked = false;
        }
    }

    public boolean shouldRunUnlockedCursor() {
        return this.allowWhileCursorUnlocked && this.tickStep;
    }

    public void setStartRotation(Vec2 startRotation) {
        this.startRotation = startRotation;
    }

    public void updateReturnToCamera() {
        if (this.keepTargetThisTick) {
            this.targetCameraRotation();
            this.keepTargetThisTick = false;
        }
    }

    public void applyTickRotation() {
        if (this.rotating) {
            this.tickStep = true;
            MC.mouseHandler.handleAccumulatedMovement();
            this.tickStep = false;
        }
    }

    public void targetCameraRotation() {
        RotationState rotationstate = RotationHelper.getState();
        this.targetRotation = rotationstate.getCameraRotation();
    }

    public void setRotation(Vec2 rotation, RotationModel rotationModel) {
        this.targetRotation = rotation;
        this.activeModel = rotationModel;
        this.keepTargetThisTick = true;
        this.rotating = true;
        this.applyTickRotation();
    }

    public void allowUnlockedCursor() {
        this.allowWhileCursorUnlocked = true;
    }

    public Vec2 getTargetRotation() {
        return this.targetRotation;
    }

    public RotationModel getActiveModel() {
        return this.activeModel;
    }

    public boolean isRotating() {
        return this.rotating;
    }

    public boolean isKeepingTarget() {
        return this.keepTargetThisTick;
    }
}
