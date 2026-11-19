package dev.adapt.v3.feature.helper.slot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class SlotHelper {
    public static final Minecraft MC = Minecraft.getInstance();
    public int originalSlot;
    public int spoofedSlot;
    public boolean active;
    public int setTick;
    public int currentTick;
    public SlotHelper.SpoofMode mode = SlotHelper.SpoofMode.DEFAULT;
    public static SlotHelper instance;

    public SlotHelper switchTo(int value) {
        value = Mth.clamp(value, 0, 8);
        if (!this.active) {
            this.originalSlot = MC.player.getInventory().getSelectedSlot();
        }

        this.spoofedSlot = value;
        this.setTick = this.currentTick;
        this.active = true;
        this.syncSlot(true, true);
        return this;
    }

    public SlotHelper withMode(SlotHelper.SpoofMode mode) {
        this.mode = mode;
        return this;
    }

    public void setOriginalSlot(int originalSlot) {
        this.originalSlot = originalSlot;
    }

    public void restoreNow() {
        this.setTick = -1;
        this.syncSlot(true, true);
    }

    public void syncSlot(boolean flag, boolean flag2) {
        if (this.active && MC.player != null && (!flag2 || MC.gui.overlay() == null && MC.gui.screen() == null)) {
            if (flag && this.setTick != this.currentTick) {
                MC.player.getInventory().setSelectedSlot(this.originalSlot);
                this.mode = SlotHelper.SpoofMode.DEFAULT;
                this.active = false;
            } else {
                MC.player.getInventory().setSelectedSlot(this.spoofedSlot);
            }
        }
    }

    public void tick() {
        this.syncSlot(true, true);
        this.currentTick++;
    }

    public ItemStack getVisualMainHandItem(LocalPlayer player) {
        return this.active && this.mode != SlotHelper.SpoofMode.NONE
            ? player.getInventory().getNonEquipmentItems().get(this.originalSlot)
            : player.getMainHandItem();
    }

    public int getVisualSelectedSlot(Inventory inventory) {
        return this.active && this.mode == SlotHelper.SpoofMode.FULL ? this.originalSlot : inventory.getSelectedSlot();
    }

    public boolean isActive() {
        return this.active;
    }

    public int getOriginalSlot() {
        return this.originalSlot;
    }

    public SlotHelper.SpoofMode getMode() {
        return this.mode;
    }

    public static SlotHelper getInstance() {
        if (instance == null) {
            instance = new SlotHelper();
        }

        return instance;
    }

    public static SlotHelper spoof(int value) {
        return getInstance().switchTo(value);
    }

    public enum SpoofMode {
        NONE,
        DEFAULT,
        FULL;
    }
}
