package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.helper.slot.SlotHelper;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public abstract class HudSlotMixin {
    @Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
    private int adapt$hotbarSlot(Inventory instance) {
        return SlotHelper.getInstance().getVisualSelectedSlot(instance);
    }

    @Redirect(
        method = "extractItemHotbar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack adapt$hideShieldSlot(Player instance) {
        ItemStack itemstack = instance.getOffhandItem();
        return AnimationsModule.shouldHideOffhandShield(itemstack) ? ItemStack.EMPTY : itemstack;
    }

    @Redirect(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack adapt$selectedItemName(Inventory instance) {
        SlotHelper slothelper = SlotHelper.getInstance();
        return slothelper.isActive() && slothelper.getMode() == SlotHelper.SpoofMode.FULL
            ? slothelper.getVisualMainHandItem(Minecraft.getInstance().player)
            : instance.getSelectedItem();
    }
}
