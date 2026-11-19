package dev.adapt.v3.feature.module.impl.utility;

import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.TimerUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class ChestLooterModule extends Module {
    public final NumberProperty delay = new NumberProperty("Delay", 50.0, 0.0, 300.0, 1.0);
    public final BooleanProperty skipTrashItems = new BooleanProperty("Skip Trash Items", true);
    public final BooleanProperty onlyRealChests = new BooleanProperty("Only Real Chests", true);
    public final TimerUtility lootTimer = new TimerUtility();
    public int openTicks;
    public int emptyTicks;

    public ChestLooterModule() {
        super("Chest Looter", "Takes everything worth having out of chests.", ModuleCategory.UTILITY);
        this.addProperties(this.delay, this.skipTrashItems, this.onlyRealChests);
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        if (mc.player != null && mc.gameMode != null) {
            if (mc.gui.screen() instanceof ContainerScreen containerscreen) {
                this.openTicks++;
                if (this.openTicks <= 5 || !this.lootTimer.hasTimeElapsed(this.delay.getValue().longValue())) {
                    return;
                }

                ChestMenu chestmenu = containerscreen.getMenu();
                Container container = chestmenu.getContainer();
                if (this.onlyRealChests.getValue() && !containerscreen.getTitle().getString().toLowerCase().contains("chest")) {
                    return;
                }

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack itemstack = container.getItem(i);
                    if (!itemstack.isEmpty() && !isGuiMenuItem(itemstack) && (!this.skipTrashItems.getValue() || InvManagerModule.isUsefulItem(itemstack))) {
                        mc.gameMode.handleContainerInput(chestmenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                        this.lootTimer.reset();
                        return;
                    }
                }

                this.emptyTicks++;
                if (this.emptyTicks > 1) {
                    mc.player.closeContainer();
                }
            } else {
                this.openTicks = this.emptyTicks = 0;
            }
        }
    }

    public static boolean isGuiMenuItem(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
        }

        String s = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        int i = s.indexOf(40);
        return i != -1 && s.indexOf(41, i) != -1;
    }
}
