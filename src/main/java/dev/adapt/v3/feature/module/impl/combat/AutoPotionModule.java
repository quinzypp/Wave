package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.PostMovementPacketEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.mixin.MultiPlayerGameModeAccessor;
import dev.adapt.v3.util.TimerUtility;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public final class AutoPotionModule extends Module {
    public final BooleanProperty instantHealth = new BooleanProperty("Instant Health", true);
    public final NumberProperty healHealth = (NumberProperty)new NumberProperty("Heal Health", 10.0, 1.0, 19.0, 0.5)
        .hideIf(() -> !this.instantHealth.getValue());
    public final BooleanProperty regeneration = new BooleanProperty("Regeneration", true);
    public final NumberProperty regenHealth = (NumberProperty)new NumberProperty("Regen Health", 14.0, 1.0, 19.0, 0.5)
        .hideIf(() -> !this.regeneration.getValue());
    public final BooleanProperty speed = new BooleanProperty("Speed", true);
    public final NumberProperty delay = new NumberProperty("Delay", 500.0, 100.0, 2000.0, 50.0);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", true);
    public final BooleanProperty useInventory = new BooleanProperty("Use Inventory", true);
    public final NumberProperty hotbarSlot = (NumberProperty)new NumberProperty("Hotbar Slot", 9.0, 1.0, 9.0, 1.0).hideIf(() -> !this.useInventory.getValue());
    public final BooleanProperty swapBack = (BooleanProperty)new BooleanProperty("Swap Back", true).hideIf(() -> !this.useInventory.getValue());
    public final TimerUtility delayTimer = new TimerUtility();
    public int pendingThrowSlot = -1;
    public int swapBackInventorySlot = -1;

    public AutoPotionModule() {
        super("Auto Potion", "Throws splash potions at your feet when you need them.", ModuleCategory.COMBAT);
        this.addProperties(
            this.instantHealth,
            this.healHealth,
            this.regeneration,
            this.regenHealth,
            this.speed,
            this.delay,
            this.groundOnly,
            this.useInventory,
            this.hotbarSlot,
            this.swapBack
        );
    }

    @Override
    public void onEnable() {
        this.pendingThrowSlot = -1;
        this.swapBackInventorySlot = -1;
    }

    @Subscribe(priority = -100)
    public void onPreMotion(PreMovementPacketEvent event) {
        this.pendingThrowSlot = -1;
        this.swapBackInventorySlot = -1;
        if (mc.player != null && mc.gameMode != null && this.canThrowPotion()) {
            int i = this.findNeededPotionSlot();
            if (i != -1) {
                if (i >= 9) {
                    int j = findEmptyHotbarSlot();
                    int k = j != -1 ? j : this.hotbarSlot.getValue().intValue() - 1;
                    swapSlots(i, k);
                    if (j == -1 && this.swapBack.getValue()) {
                        this.swapBackInventorySlot = i;
                    }

                    i = k;
                }

                event.setPitch(90.0F);
                this.pendingThrowSlot = i;
            }
        }
    }

    @Subscribe
    public void onPostMotion(PostMovementPacketEvent event) {
        if (this.pendingThrowSlot != -1 && mc.player != null && mc.getConnection() != null) {
            int i = ((MultiPlayerGameModeAccessor)mc.gameMode).adapt$getCarriedIndex();
            ItemStack itemstack = mc.player.getInventory().getItem(this.pendingThrowSlot);
            if (!itemstack.isEmpty()) {
                if (this.pendingThrowSlot != i) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(this.pendingThrowSlot));
                }

                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), 90.0F));
                if (this.pendingThrowSlot != i) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(i));
                }

                this.delayTimer.reset();
            }

            if (this.swapBackInventorySlot != -1) {
                swapSlots(this.swapBackInventorySlot, this.pendingThrowSlot);
            }

            this.pendingThrowSlot = -1;
            this.swapBackInventorySlot = -1;
        }
    }

    public static void swapSlots(int slot, int slot2) {
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, slot, slot2, ContainerInput.SWAP, mc.player);
    }

    public static int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public boolean canThrowPotion() {
        if (!this.delayTimer.hasTimeElapsed(this.delay.getValue().longValue())) {
            return false;
        }

        if (mc.gui.screen() instanceof AbstractContainerScreen) {
            return false;
        }

        if (mc.player.isUsingItem()) {
            return false;
        }

        if (this.groundOnly.getValue() && !mc.player.onGround()) {
            return false;
        }

        ScaffoldModule scaffoldmodule = Adapt.getInstance().getModuleManager().getByClass(ScaffoldModule.class);
        return scaffoldmodule == null || !scaffoldmodule.isEnabled();
    }

    public int findNeededPotionSlot() {
        float f = mc.player.getHealth();
        if (this.instantHealth.getValue() && f <= this.healHealth.getValue().floatValue()) {
            int i = this.findSplashPotionWithEffect(MobEffects.INSTANT_HEALTH);
            if (i != -1) {
                return i;
            }
        }

        if (this.regeneration.getValue() && f <= this.regenHealth.getValue().floatValue() && !mc.player.hasEffect(MobEffects.REGENERATION)) {
            int j = this.findSplashPotionWithEffect(MobEffects.REGENERATION);
            if (j != -1) {
                return j;
            }
        }

        return this.speed.getValue() && !mc.player.hasEffect(MobEffects.SPEED) ? this.findSplashPotionWithEffect(MobEffects.SPEED) : -1;
    }

    public int findSplashPotionWithEffect(Holder holder) {
        int i = this.useInventory.getValue() ? 36 : 9;

        for (int j = 0; j < i; j++) {
            ItemStack itemstack = mc.player.getInventory().getItem(j);
            if (!itemstack.isEmpty() && itemstack.is(Items.SPLASH_POTION)) {
                PotionContents potioncontents = itemstack.get(DataComponents.POTION_CONTENTS);
                if (potioncontents != null) {
                    for (MobEffectInstance mobeffectinstance : potioncontents.getAllEffects()) {
                        if (mobeffectinstance.is(holder)) {
                            return j;
                        }
                    }
                }
            }
        }

        return -1;
    }
}
