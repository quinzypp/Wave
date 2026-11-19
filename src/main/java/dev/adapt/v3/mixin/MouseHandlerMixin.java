package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.MousePressEvent;
import dev.adapt.v3.event.impl.MouseScrollEvent;
import dev.adapt.v3.event.impl.MouseUpdateEvent;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.util.MouseUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Unique
    private MouseUpdateEvent adapt$updateEvent;
    @Unique
    private boolean adapt$unlockCursorRun;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void adapt$onButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (handle == minecraft.getWindow().handle()) {
            MouseUtility.setButtonDown(info.button(), action != 0);
        }

        if (action == 1 && handle == minecraft.getWindow().handle() && minecraft.gui.screen() == null) {
            EventManager.post(new MousePressEvent(info.button()));
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void adapt$onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (handle == minecraft.getWindow().handle()
            && minecraft.gui.screen() == null
            && yoffset != 0.0
            && EventManager.post(new MouseScrollEvent(yoffset)).isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
    private void adapt$scrollSlot(Inventory instance, int slot) {
        SlotHelper slothelper = SlotHelper.getInstance();
        if (slothelper.isActive()) {
            if (slothelper.getMode() != SlotHelper.SpoofMode.NONE) {
                int i = slot - instance.getSelectedSlot();
                slothelper.setOriginalSlot(Math.floorMod(slothelper.getOriginalSlot() + i, Inventory.getSelectionSize()));
            }
        } else {
            instance.setSelectedSlot(slot);
        }
    }

    @Redirect(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean adapt$redirectMouseGrabbed(MouseHandler instance) {
        if (instance.isMouseGrabbed()) {
            return true;
        } else if (RotationHelper.getUpdater().shouldRunUnlockedCursor()) {
            this.adapt$unlockCursorRun = true;
            return true;
        } else {
            return false;
        }
    }

    @Redirect(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z", opcode = 180))
    private boolean adapt$dispatchMouseUpdate(Options options) {
        double d0 = options.sensitivity().get() * 0.6F + 0.2F;
        double d1 = d0 * d0 * d0 * 8.0;
        MouseHandlerAccessor mousehandleraccessor = (MouseHandlerAccessor)this;
        this.adapt$updateEvent = new MouseUpdateEvent(
            mousehandleraccessor.adapt$getAccumulatedDX(), mousehandleraccessor.adapt$getAccumulatedDY(), d1, this.adapt$unlockCursorRun
        );
        EventManager.post(this.adapt$updateEvent);
        return options.smoothCamera && !this.adapt$updateEvent.isModified();
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isScoping()Z"))
    private boolean adapt$redirectScoping(LocalPlayer instance) {
        return instance.isScoping() && (this.adapt$updateEvent == null || !this.adapt$updateEvent.isModified());
    }

    @Redirect(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;accumulatedDX:D", opcode = 180))
    private double adapt$redirectDeltaX(MouseHandler instance) {
        return this.adapt$updateEvent == null ? 0.0 : this.adapt$updateEvent.getDeltaX();
    }

    @Redirect(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;accumulatedDY:D", opcode = 180))
    private double adapt$redirectDeltaY(MouseHandler instance) {
        return this.adapt$updateEvent == null ? 0.0 : this.adapt$updateEvent.getDeltaY();
    }

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void adapt$turnPlayerTail(double mousea, CallbackInfo ci) {
        RotationHelper.getState().onTurnPlayerTail();
        this.adapt$unlockCursorRun = false;
        this.adapt$updateEvent = null;
    }
}
