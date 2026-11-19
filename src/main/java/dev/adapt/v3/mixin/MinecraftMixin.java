package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.BlockPlaceEvent;
import dev.adapt.v3.event.impl.MouseInputEvent;
import dev.adapt.v3.event.impl.PreInputEvent;
import dev.adapt.v3.feature.helper.mouse.MouseButton;
import dev.adapt.v3.feature.helper.mouse.MouseHelper;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.module.impl.combat.BlockModule;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private void adapt$handleKeybindsMouse(CallbackInfo ci) {
        EventManager.post(MouseInputEvent.INSTANCE);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void adapt$handleKeybindsHead(CallbackInfo ci) {
        EventManager.post(PreInputEvent.INSTANCE);
    }

    @Inject(method = "handleKeybinds", at = @At("TAIL"))
    private void adapt$handleKeybindsTail(CallbackInfo ci) {
        MouseHelper.getInstance().tick();
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean adapt$redirectIsDown(KeyMapping instance) {
        if (instance == ((Minecraft)(Object)this).options.keyUse) {
            if (BlockModule.shouldSuppressUseDown()) {
                return false;
            }

            if (BlockModule.shouldForceUseDown()) {
                return true;
            }
        }

        MouseButton mousebutton = MouseHelper.getButton(instance);
        return mousebutton != null ? mousebutton.isDown() : instance.isDown();
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyUse:Lnet/minecraft/client/KeyMapping;", ordinal = 1),
            to = @At("TAIL")
        )
    )
    private boolean adapt$redirectConsumeClick(KeyMapping instance) {
        MouseButton mousebutton = MouseHelper.getButton(instance);
        return mousebutton != null ? mousebutton.consumeClick() : instance.consumeClick();
    }

    @Shadow
    private boolean startAttack() {
        throw new AssertionError();
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyUse:Lnet/minecraft/client/KeyMapping;", ordinal = 0),
            to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyUse:Lnet/minecraft/client/KeyMapping;", ordinal = 1)
        )
    )
    private boolean adapt$attackWhileBlocking(KeyMapping instance) {
        if (BlockModule.shouldCancelAttackClick()) {
            return false;
        }

        if (BlockModule.canAttackWhileBlocking()) {
            if (MouseHelper.getAttackButton().consumeClick()) {
                this.startAttack();
                return true;
            } else {
                return false;
            }
        } else {
            return instance.consumeClick();
        }
    }

    @Redirect(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean adapt$swingWhileUsing(LocalPlayer instance) {
        AnimationsModule animationsmodule = AnimationsModule.getInstance();
        return animationsmodule != null && animationsmodule.isEnabled() && animationsmodule.swingWhileUsing.getValue() ? false : instance.isUsingItem();
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
    private void adapt$redirectSelectedSlot(Inventory instance, int value) {
        SlotHelper slothelper = SlotHelper.getInstance();
        if (slothelper.isActive()) {
            if (slothelper.getMode() != SlotHelper.SpoofMode.NONE) {
                slothelper.setOriginalSlot(value);
            }
        } else {
            instance.setSelectedSlot(value);
        }
    }

    @Inject(
        method = "handleKeybinds",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyInventory:Lnet/minecraft/client/KeyMapping;", ordinal = 0, shift = Shift.BEFORE)
    )
    private void adapt$postSlotHandleInput(CallbackInfo ci) {
        SlotHelper.getInstance().syncSlot(false, false);
    }

    @Inject(
        method = "startUseItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/InteractionResult$Success;swingSource()Lnet/minecraft/world/InteractionResult$SwingSource;",
            ordinal = 1
        )
    )
    private void adapt$blockPlaced(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        if (minecraft.hitResult instanceof BlockHitResult blockhitresult) {
            EventManager.post(new BlockPlaceEvent(blockhitresult));
        }
    }

    @Redirect(
        method = "startUseItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;itemUsed(Lnet/minecraft/world/InteractionHand;)V", ordinal = 0)
    )
    private void adapt$redirectItemUsed(LocalPlayer instance, InteractionHand hand) {
        SlotHelper slothelper = SlotHelper.getInstance();
        if (hand != InteractionHand.MAIN_HAND || !slothelper.isActive() || slothelper.getVisualMainHandItem(instance).getItem() instanceof BlockItem) {
            instance.itemUsed(hand);
        }
    }

    @Redirect(
        method = "startAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"
        )
    )
    private boolean adapt$redirectAttackSwing(LocalPlayer instance, InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity) {
        return MouseHelper.getAttackButton().shouldSwing() ? instance.swing(hand, animation, sendToSwingingEntity) : false;
    }

    @Redirect(
        method = "startUseItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"
        )
    )
    private boolean adapt$redirectUseSwing(LocalPlayer instance, InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity) {
        return MouseHelper.getUseButton().shouldSwing() ? instance.swing(hand, animation, sendToSwingingEntity) : false;
    }
}
