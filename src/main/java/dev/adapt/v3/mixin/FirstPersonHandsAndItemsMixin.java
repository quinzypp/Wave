package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float adapt$noCooldownDip(LocalPlayer instance, float a) {
        AnimationsModule animationsmodule = AnimationsModule.getInstance();
        return animationsmodule != null && animationsmodule.isEnabled() && animationsmodule.noCooldownDip.getValue() ? 1.0F : instance.getItemSwapScale(a);
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void adapt$alwaysShowSameItem(ItemStack currentlyVisibleItem, ItemStack expectedItem, LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (AnimationsModule.isAlwaysShow() && ItemStack.isSameItem(currentlyVisibleItem, expectedItem)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void adapt$alwaysShowOnUse(InteractionHand hand, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND && AnimationsModule.isAlwaysShow()) {
            ci.cancel();
        }
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack adapt$visualMainHand(LocalPlayer instance) {
        return SlotHelper.getInstance().getVisualMainHandItem(instance);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"))
    private float adapt$itemYaw(LocalPlayer instance, float partialTicks) {
        return RotationHelper.getState().getCameraYaw(instance.getViewYRot(partialTicks));
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"))
    private float adapt$itemPitch(LocalPlayer instance, float partialTicks) {
        return RotationHelper.getState().getCameraPitch(instance.getViewXRot(partialTicks));
    }

    @Redirect(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;yBobO:F", opcode = 180))
    private float adapt$itemLastRenderYaw(LocalPlayer instance) {
        return RotationHelper.getState().getYBobO(instance.yBobO);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;xBobO:F", opcode = 180))
    private float adapt$itemLastRenderPitch(LocalPlayer instance) {
        return RotationHelper.getState().getXBobO(instance.xBobO);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;yBob:F", opcode = 180))
    private float adapt$itemRenderYaw(LocalPlayer instance) {
        return RotationHelper.getState().getYBob(instance.yBob);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;xBob:F", opcode = 180))
    private float adapt$itemRenderPitch(LocalPlayer instance) {
        return RotationHelper.getState().getXBob(instance.xBob);
    }
}
