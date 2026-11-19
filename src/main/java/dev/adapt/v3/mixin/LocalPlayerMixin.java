package dev.adapt.v3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PostMovementPacketEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.PushOutOfBlocksEvent;
import dev.adapt.v3.event.impl.SlowdownEvent;
import dev.adapt.v3.event.impl.SprintEvent;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.module.impl.movement.noslow.NoSlowModule;
import dev.adapt.v3.feature.module.impl.utility.SprintModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Unique
    private PreMovementPacketEvent adapt$preMovementPacketEvent;

    @Shadow
    protected abstract boolean isControlledCamera();

    @Inject(method = "applyInput", at = @At("TAIL"))
    private void adapt$tickCamera(CallbackInfo ci) {
        if (this.isControlledCamera()) {
            RotationHelper.getState().tickBobbing();
        }
    }

    @Inject(method = "canStartSprinting", at = @At("RETURN"), cancellable = true)
    private void adapt$canStartSprinting(CallbackInfoReturnable<Boolean> cir) {
        SprintEvent sprintevent = EventManager.post(new SprintEvent(cir.getReturnValueZ()));
        cir.setReturnValue(sprintevent.isSprinting());
    }

    @Redirect(
        method = {"canStartSprinting", "shouldStopRunSprinting", "shouldStopSwimSprinting"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z")
    )
    private boolean adapt$omniSprint(ClientInput input) {
        return SprintModule.isOmniSprint() ? input.getMoveVector().lengthSquared() > 0.0F : input.hasForwardImpulse();
    }

    @ModifyExpressionValue(
        method = {"canStartSprinting", "aiStep"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z")
    )
    private boolean adapt$allowSprint(boolean slowDueToUsingItem) {
        return slowDueToUsingItem && !NoSlowModule.canSprintWhileUsing();
    }

    @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"))
    private float adapt$useSlowdown(LocalPlayer instance) {
        SlowdownEvent slowdownevent = EventManager.post(new SlowdownEvent(this.itemUseSpeedMultiplier()));
        return slowdownevent.isCancelled() ? 1.0F : slowdownevent.getMultiplier();
    }

    @Shadow
    protected abstract float itemUseSpeedMultiplier();

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void adapt$pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (EventManager.post(new PushOutOfBlocksEvent()).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void adapt$sendPositionHead(CallbackInfo ci) {
        LocalPlayer localplayer = (LocalPlayer)(Object)this;
        this.adapt$preMovementPacketEvent = EventManager.post(
            new PreMovementPacketEvent(localplayer.getYRot(), localplayer.getXRot(), localplayer.isSprinting(), localplayer.onGround())
        );
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void adapt$sendPositionTail(CallbackInfo ci) {
        this.adapt$preMovementPacketEvent = null;
        EventManager.post(PostMovementPacketEvent.INSTANCE);
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float adapt$sentYaw(LocalPlayer instance) {
        return this.adapt$preMovementPacketEvent == null ? instance.getYRot() : this.adapt$preMovementPacketEvent.getYaw();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float adapt$sentPitch(LocalPlayer instance) {
        return this.adapt$preMovementPacketEvent == null ? instance.getXRot() : this.adapt$preMovementPacketEvent.getPitch();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean adapt$sentOnGround(LocalPlayer instance) {
        return this.adapt$preMovementPacketEvent == null ? instance.onGround() : this.adapt$preMovementPacketEvent.isOnGround();
    }

    @Redirect(method = "sendIsSprintingIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"))
    private boolean adapt$sentSprinting(LocalPlayer instance) {
        return this.adapt$preMovementPacketEvent == null ? instance.isSprinting() : this.adapt$preMovementPacketEvent.isSprinting();
    }
}
