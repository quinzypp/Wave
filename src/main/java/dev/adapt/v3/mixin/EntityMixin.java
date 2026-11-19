package dev.adapt.v3.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.MoveEvent;
import dev.adapt.v3.event.impl.PostStrafeEvent;
import dev.adapt.v3.event.impl.StepEvent;
import dev.adapt.v3.event.impl.StrafeEvent;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.module.impl.movement.MovementFixModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private float adapt$stepHeight = Float.NaN;

    @Inject(method = "collide", at = @At("HEAD"))
    private void adapt$stepPre(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (this.adapt$isLocalPlayer()) {
            Entity entity = (Entity)(Object)this;
            StepEvent stepevent = EventManager.post(new StepEvent(StepEvent.Stage.PRE, entity.maxUpStep(), 0.0));
            this.adapt$stepHeight = (float)stepevent.getStepHeight();
        }
    }

    @Redirect(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maxUpStep()F"))
    private float adapt$stepHeight(Entity instance) {
        return Float.isNaN(this.adapt$stepHeight) ? instance.maxUpStep() : this.adapt$stepHeight;
    }

    @Inject(method = "collide", at = @At("RETURN"))
    private void adapt$stepPost(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (!Float.isNaN(this.adapt$stepHeight)) {
            float f = this.adapt$stepHeight;
            this.adapt$stepHeight = Float.NaN;
            Vec3 vec3 = cir.getReturnValue();
            if (vec3.y > movement.y + 1.0E-7) {
                EventManager.post(new StepEvent(StepEvent.Stage.POST, f, vec3.y));
            }
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 adapt$moveEvent(Vec3 movement, @Local(argsOnly = true) MoverType type) {
        if (type == MoverType.SELF && this.adapt$isLocalPlayer()) {
            MoveEvent moveevent = EventManager.post(new MoveEvent(movement.x, movement.y, movement.z));
            return moveevent.getX() == movement.x && moveevent.getY() == movement.y && moveevent.getZ() == movement.z
                ? movement
                : new Vec3(moveevent.getX(), moveevent.getY(), moveevent.getZ());
        } else {
            return movement;
        }
    }

    @Unique
    private boolean adapt$isLocalPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.player != null && (Object)this == minecraft.player;
    }

    @Inject(method = "setYRot", at = @At("HEAD"))
    private void adapt$setYRot(float yRot, CallbackInfo ci) {
        this.adapt$checkRotation();
    }

    @Inject(method = "setXRot", at = @At("HEAD"))
    private void adapt$setXRot(float xRot, CallbackInfo ci) {
        this.adapt$checkRotation();
    }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void adapt$moveRelative(float speed, Vec3 input, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = (Entity)(Object)this;
        if (minecraft.player != null && entity == minecraft.player) {
            ci.cancel();
            float f = adapt$isFixMovement() ? MovementFixHelper.resolveYaw(entity.getYRot()) : RotationHelper.getState().getCameraYaw(entity.getYRot());
            StrafeEvent strafeevent = EventManager.post(new StrafeEvent((float)input.z, (float)input.x, speed, f));
            if (!strafeevent.isCancelled()) {
                Vec3 vec3 = new Vec3(strafeevent.getStrafe(), input.y, strafeevent.getForward());
                double d0 = vec3.lengthSqr();
                if (d0 >= 1.0E-7) {
                    Vec3 vec31 = (d0 > 1.0 ? vec3.normalize() : vec3).scale(strafeevent.getSpeed());
                    float f1 = Mth.sin(strafeevent.getYaw() * (float) (Math.PI / 180.0));
                    float f2 = Mth.cos(strafeevent.getYaw() * (float) (Math.PI / 180.0));
                    entity.setDeltaMovement(entity.getDeltaMovement().add(vec31.x * f2 - vec31.z * f1, vec31.y, vec31.z * f2 + vec31.x * f1));
                }
            }

            EventManager.post(PostStrafeEvent.INSTANCE);
        }
    }

    @Unique
    private void adapt$checkRotation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && (Object)this == minecraft.player && ((Entity)(Object)this).level().isClientSide()) {
            RotationHelper.getState().checkReset();
        }
    }

    @Unique
    private static boolean adapt$isFixMovement() {
        if (MovementFixHelper.isForceFixActive()) {
            return true;
        }

        Adapt adapt = Adapt.getInstance();
        if (adapt == null) {
            return false;
        }

        MovementFixModule movementfixmodule = adapt.getModuleManager().getByClass(MovementFixModule.class);
        return movementfixmodule != null && movementfixmodule.isNormalFixActive();
    }
}
