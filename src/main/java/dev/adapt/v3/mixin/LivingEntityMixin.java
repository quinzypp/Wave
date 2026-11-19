package dev.adapt.v3.mixin;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.JumpEvent;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.module.impl.movement.MovementFixModule;
import dev.adapt.v3.feature.module.impl.utility.InvManagerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private JumpEvent adapt$jumpEvent;

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void adapt$jumpHead(CallbackInfo ci) {
        if ((Object)this instanceof LocalPlayer localplayer && localplayer == Minecraft.getInstance().player) {
            this.adapt$jumpEvent = EventManager.post(new JumpEvent(localplayer.isSprinting()));
        } else {
            this.adapt$jumpEvent = null;
        }
    }

    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSprinting()Z"))
    private boolean adapt$jumpSprinting(LivingEntity instance) {
        return this.adapt$jumpEvent != null ? this.adapt$jumpEvent.isSprinting() : instance.isSprinting();
    }

    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float adapt$jumpYaw(LivingEntity instance) {
        if (instance instanceof LocalPlayer && instance == Minecraft.getInstance().player) {
            Adapt adapt = Adapt.getInstance();
            MovementFixModule movementfixmodule = adapt == null ? null : adapt.getModuleManager().getByClass(MovementFixModule.class);
            return !MovementFixHelper.isForceFixActive() && (movementfixmodule == null || !movementfixmodule.isNormalFixActive())
                ? RotationHelper.getState().getCameraYaw(instance.getYRot())
                : MovementFixHelper.resolveYaw(instance.getYRot());
        } else {
            return instance.getYRot();
        }
    }

    @Redirect(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"
        )
    )
    private boolean adapt$dropSwing(LivingEntity instance, InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity) {
        return InvManagerModule.shouldHideDropSwing() && instance == Minecraft.getInstance().player
            ? false
            : instance.swing(hand, animation, sendToSwingingEntity);
    }
}
