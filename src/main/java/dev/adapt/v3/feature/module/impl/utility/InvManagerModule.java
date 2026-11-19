package dev.adapt.v3.feature.module.impl.utility;

import java.util.Map;
import dev.adapt.v3.event.impl.AttackEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.TimerUtility;
import dev.adapt.v3.util.player.InventoryUtility;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class InvManagerModule extends Module {
    public static final String[] MENU_ITEM_TAGS = new String[]{"(right click)"};
    public static final int HOTBAR_SIZE = 9;
    public static final int INVENTORY_SIZE = 40;
    public static final int ARMOR_SLOT_START = 36;
    public static final List<Holder<?>> GOOD_POTION_EFFECTS = List.of(
        MobEffects.INSTANT_HEALTH,
        MobEffects.REGENERATION,
        MobEffects.RESISTANCE,
        MobEffects.ABSORPTION,
        MobEffects.SATURATION,
        MobEffects.STRENGTH,
        MobEffects.SPEED,
        MobEffects.FIRE_RESISTANCE,
        MobEffects.INVISIBILITY,
        MobEffects.HASTE,
        MobEffects.WATER_BREATHING
    );
    public final BooleanProperty invOnly = new BooleanProperty("Inv Only", true);
    public final BooleanProperty dropFood = new BooleanProperty("Drop Food", false);
    public final BooleanProperty dropArchery = new BooleanProperty("Drop Archery", false);
    public final BooleanProperty dropProjectiles = new BooleanProperty("Drop Projectiles", false);
    public final BooleanProperty dropTrash = new BooleanProperty("Drop Trash", true);
    public final BooleanProperty hideSwing = new BooleanProperty("Hide Swing", true);
    public final NumberProperty delay = new NumberProperty("Delay", 50.0, 0.0, 300.0, 1.0);
    public final NumberProperty swordSlot = new NumberProperty("Sword", 1.0, 1.0, 10.0, 1.0);
    public final NumberProperty pickaxeSlot = new NumberProperty("Pickaxe", 2.0, 1.0, 10.0, 1.0);
    public final NumberProperty axeSlot = new NumberProperty("Axe", 3.0, 1.0, 10.0, 1.0);
    public final NumberProperty shovelSlot = new NumberProperty("Shovel", 4.0, 1.0, 10.0, 1.0);
    public final NumberProperty blocksSlot = new NumberProperty("Blocks", 5.0, 1.0, 10.0, 1.0);
    public final NumberProperty foodSlot = new NumberProperty("Food", 6.0, 1.0, 10.0, 1.0);
    public final NumberProperty potionsSlot = new NumberProperty("Potions", 7.0, 1.0, 10.0, 1.0);
    public final TimerUtility actionTimer = new TimerUtility();
    public boolean silentInventoryOpen;
    public boolean performedActionThisTick;
    public int pickupPauseTicks;
    public boolean busy = false;
    public static boolean suppressDropSwing;

    public static boolean shouldHideDropSwing() {
        return suppressDropSwing;
    }

    public InvManagerModule() {
        super("Inv Manager", "Drops junk, keeps your best gear and sorts your hotbar.", ModuleCategory.UTILITY);
        this.addProperties(
            this.invOnly,
            this.dropFood,
            this.dropArchery,
            this.dropProjectiles,
            this.dropTrash,
            this.hideSwing,
            this.delay,
            this.swordSlot,
            this.pickaxeSlot,
            this.axeSlot,
            this.shovelSlot,
            this.blocksSlot,
            this.foodSlot,
            this.potionsSlot
        );
    }

    public boolean isBusy() {
        return this.busy;
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (this.silentInventoryOpen) {
            this.closeSilentInventory();
        }
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player != null
            && event.getPacket() instanceof ClientboundTakeItemEntityPacket clientboundtakeitementitypacket
            && clientboundtakeitementitypacket.getPlayerId() == mc.player.getId()) {
            this.pickupPauseTicks = 2;
        }
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        if (mc.player != null && mc.gameMode != null) {
            if (this.pickupPauseTicks > 0) {
                this.pickupPauseTicks--;
            }

            if (mc.gui.screen() instanceof ContainerScreen) {
                this.pickupPauseTicks = 5;
            }

            this.busy = false;
            if ((!this.invOnly.getValue() || mc.gui.screen() instanceof InventoryScreen)
                && (this.invOnly.getValue() || mc.gui.screen() instanceof InventoryScreen || mc.gui.screen() == null)
                && !mc.player.isUsingItem()
                && this.pickupPauseTicks <= 0
                && !mc.player.isCreative()
                && this.actionTimer.hasTimeElapsed(this.delay.getValue().longValue())) {
                this.performedActionThisTick = false;
                Map<Integer, Integer> hashmap = new HashMap<>();
                Integer integer = null;
                Integer integer1 = null;
                Integer integer2 = null;
                Integer integer3 = null;
                Integer integer4 = null;
                Integer integer5 = null;
                Integer integer6 = null;

                for (int i = 0; i < 40; i++) {
                    ItemStack itemstack = getStack(i);
                    if (!itemstack.isEmpty() && !isLobbyMenuItem(itemstack)) {
                        if (isHumanoidArmor(itemstack)) {
                            int j = getArmorTypeIndex(itemstack);
                            if (!hashmap.containsKey(j) || scoreArmor(itemstack) > scoreArmor(getStack((Integer)hashmap.get(j)))) {
                                hashmap.put(j, i);
                            }
                        } else if (itemstack.is(ItemTags.SWORDS)) {
                            if (integer == null || isBetterOrInTargetSlot(scoreSword(itemstack), scoreSword(getStack(integer)), i, this.swordSlot)) {
                                integer = i;
                            }
                        } else if (itemstack.is(ItemTags.PICKAXES)) {
                            if (integer1 == null || isBetterOrInTargetSlot(scoreTool(itemstack), scoreTool(getStack(integer1)), i, this.pickaxeSlot)) {
                                integer1 = i;
                            }
                        } else if (itemstack.is(ItemTags.AXES)) {
                            if (integer2 == null || isBetterOrInTargetSlot(scoreTool(itemstack), scoreTool(getStack(integer2)), i, this.axeSlot)) {
                                integer2 = i;
                            }
                        } else if (itemstack.is(ItemTags.SHOVELS)) {
                            if (integer3 == null || isBetterOrInTargetSlot(scoreTool(itemstack), scoreTool(getStack(integer3)), i, this.shovelSlot)) {
                                integer3 = i;
                            }
                        } else if (itemstack.getItem() instanceof BlockItem) {
                            if (isValidScaffoldBlock(itemstack)) {
                                if (integer4 == null) {
                                    integer4 = i;
                                } else {
                                    ItemStack itemstack2 = getStack(integer4);
                                    if (!itemstack2.isEmpty() && isBetterOrInTargetSlot(itemstack.getCount(), itemstack2.getCount(), i, this.blocksSlot)) {
                                        integer4 = i;
                                    }
                                }
                            }
                        } else if (isFood(itemstack)) {
                            if (integer5 == null) {
                                integer5 = i;
                            } else {
                                ItemStack itemstack3 = getStack(integer5);
                                if (!itemstack3.isEmpty()
                                    && (!isFood(itemstack3) || isBetterOrInTargetSlot(scoreFood(itemstack), scoreFood(itemstack3), i, this.foodSlot))) {
                                    integer5 = i;
                                }
                            }
                        } else if (isPotion(itemstack)) {
                            if (integer6 == null) {
                                integer6 = i;
                            } else {
                                ItemStack itemstack4 = getStack(integer6);
                                if (!itemstack4.isEmpty()
                                    && (!isPotion(itemstack4) || isBetterOrInTargetSlot(scorePotion(itemstack), scorePotion(itemstack4), i, this.potionsSlot))) {
                                    integer6 = i;
                                }
                            }
                        }
                    }
                }

                for (int k = 0; k < 40; k++) {
                    ItemStack itemstack1 = getStack(k);
                    if (!itemstack1.isEmpty() && !isLobbyMenuItem(itemstack1)) {
                        boolean flag = false;
                        if (this.dropFood.getValue()
                            && isFood(itemstack1)
                            && !itemstack1.is(Items.GOLDEN_APPLE)
                            && !itemstack1.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                            flag = true;
                        }

                        if (this.dropArchery.getValue() && (itemstack1.is(Items.BOW) || itemstack1.is(Items.ARROW))) {
                            flag = true;
                        }

                        if (this.dropProjectiles.getValue() && isProjectileOrRod(itemstack1)) {
                            flag = true;
                        }

                        if (this.dropTrash.getValue() && !isUsefulItem(itemstack1)) {
                            flag = true;
                        }

                        if (isHumanoidArmor(itemstack1) && !hashmap.containsValue(k)) {
                            flag = true;
                        }

                        if (itemstack1.is(ItemTags.SWORDS) && integer != null && integer != k) {
                            flag = true;
                        }

                        if (itemstack1.is(ItemTags.PICKAXES) && integer1 != null && integer1 != k) {
                            flag = true;
                        }

                        if (itemstack1.is(ItemTags.AXES) && integer2 != null && integer2 != k) {
                            flag = true;
                        }

                        if (itemstack1.is(ItemTags.SHOVELS) && integer3 != null && integer3 != k) {
                            flag = true;
                        }

                        if (flag) {
                            this.dropSlot(k);
                        }
                    }
                }

                for (Entry<Integer, Integer> entry : hashmap.entrySet()) {
                    Integer integer8 = (Integer)entry.getKey();
                    Integer integer7 = (Integer)entry.getValue();
                    if (integer7 != null && integer7 != 36 + (3 - integer8)) {
                        this.shiftClickSlot(integer7);
                    }
                }

                if (this.swordSlot.getValue().intValue() != 10 && integer != null && integer != this.swordSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer, this.swordSlot.getValue().intValue() - 36 - 1);
                }

                if (this.pickaxeSlot.getValue().intValue() != 10 && integer1 != null && integer1 != this.pickaxeSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer1, this.pickaxeSlot.getValue().intValue() - 36 - 1);
                }

                if (this.axeSlot.getValue().intValue() != 10 && integer2 != null && integer2 != this.axeSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer2, this.axeSlot.getValue().intValue() - 36 - 1);
                }

                if (this.shovelSlot.getValue().intValue() != 10 && integer3 != null && integer3 != this.shovelSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer3, this.shovelSlot.getValue().intValue() - 36 - 1);
                }

                if (this.blocksSlot.getValue().intValue() != 10 && integer4 != null && integer4 != this.blocksSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer4, this.blocksSlot.getValue().intValue() - 36 - 1);
                }

                if (this.foodSlot.getValue().intValue() != 10 && integer5 != null && integer5 != this.foodSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer5, this.foodSlot.getValue().intValue() - 36 - 1);
                }

                if (this.potionsSlot.getValue().intValue() != 10 && integer6 != null && integer6 != this.potionsSlot.getValue().intValue() - 1) {
                    this.swapToHotbar(integer6, this.potionsSlot.getValue().intValue() - 36 - 1);
                }

                if (!this.performedActionThisTick) {
                    this.closeSilentInventory();
                }
            } else {
                this.closeSilentInventory();
            }
        }
    }

    public static ItemStack getStack(int value) {
        return mc.player.getInventory().getItem(value);
    }

    public static boolean isBetterOrInTargetSlot(float first, float second, int slot, NumberProperty property) {
        return first > second || first == second && slot == property.getValue().intValue() - 1;
    }

    public static boolean isLobbyMenuItem(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
        }

        String s = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();

        for (String s1 : MENU_ITEM_TAGS) {
            if (s.contains(s1)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isHumanoidArmor(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null
            && equippable.slot().getType() == Type.HUMANOID_ARMOR
            && getAttributeBonus(stack, Attributes.ARMOR, 0.0, equippable.slot()) > 0.0;
    }

    public static int getArmorTypeIndex(ItemStack stack) {
        return switch (stack.get(DataComponents.EQUIPPABLE).slot()) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            default -> 3;
        };
    }

    public static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    public static boolean isProjectileOrRod(ItemStack stack) {
        return stack.getItem() instanceof FishingRodItem || stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EggItem;
    }

    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    public static double getAttributeBonus(ItemStack stack, Holder holder, double value, EquipmentSlot equipmentSlot) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(holder, value, equipmentSlot) - value;
    }

    public static int getEnchantmentLevel(ItemStack stack, ResourceKey resourceKey) {
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (((Holder)entry.getKey()).is(resourceKey)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }

    public static float weightByDurability(ItemStack stack, float value) {
        float f = stack.getMaxDamage();
        float f1 = stack.getDamageValue();
        float f2 = f - f1;
        if (f > 0.0F) {
            float f3 = f2 / f;
            return value * (0.2F + 0.8F * f3);
        } else {
            return value;
        }
    }

    public static float scoreArmor(ItemStack stack) {
        if (!isHumanoidArmor(stack)) {
            return 0.0F;
        }

        float f = (float)getAttributeBonus(stack, Attributes.ARMOR, 0.0, stack.get(DataComponents.EQUIPPABLE).slot());
        int i = getEnchantmentLevel(stack, Enchantments.PROTECTION);
        int j = getEnchantmentLevel(stack, Enchantments.THORNS);
        int k = getEnchantmentLevel(stack, Enchantments.UNBREAKING);
        f += i;
        f += j * 0.1F;
        f += k * 0.3F;
        return weightByDurability(stack, f);
    }

    public static float scoreSword(ItemStack stack) {
        if (!stack.is(ItemTags.SWORDS)) {
            return 0.0F;
        }

        float f = (float)getAttributeBonus(stack, Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND) + 1.0F;
        float f1 = getEnchantmentLevel(stack, Enchantments.SHARPNESS);
        float f2 = getEnchantmentLevel(stack, Enchantments.UNBREAKING);
        float f3 = getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT);
        float f4 = getEnchantmentLevel(stack, Enchantments.LOOTING);
        float f5 = getEnchantmentLevel(stack, Enchantments.KNOCKBACK);
        if (f1 > 0.0F) {
            f += f1 * 0.5F + 0.5F;
        }

        f += f3 * 0.1F;
        f += f4 * 0.1F;
        f += f2 * 0.3F;
        f += f5 * 0.1F;
        return weightByDurability(stack, f);
    }

    public static float scoreTool(ItemStack stack) {
        BlockState blockstate;
        if (stack.is(ItemTags.PICKAXES)) {
            blockstate = Blocks.STONE.defaultBlockState();
        } else if (stack.is(ItemTags.SHOVELS)) {
            blockstate = Blocks.DIRT.defaultBlockState();
        } else {
            if (!stack.is(ItemTags.AXES)) {
                return 0.0F;
            }

            blockstate = Blocks.OAK_LOG.defaultBlockState();
        }

        Tool tool = stack.get(DataComponents.TOOL);
        float f = tool == null ? 0.0F : tool.getMiningSpeed(blockstate);
        float f1 = getEnchantmentLevel(stack, Enchantments.EFFICIENCY) * 1.2F;
        float f2 = getEnchantmentLevel(stack, Enchantments.UNBREAKING) * 0.3F;
        f += f1;
        f += f2;
        return weightByDurability(stack, f);
    }

    public static float scoreFood(ItemStack stack) {
        FoodProperties foodproperties = stack.get(DataComponents.FOOD);
        if (foodproperties == null) {
            return 0.0F;
        }

        float f = stack.getCount();
        return f + (foodproperties.nutrition() > 0 ? foodproperties.saturation() / (foodproperties.nutrition() * 2.0F) : 0.0F);
    }

    public static float scorePotion(ItemStack stack) {
        return 0.0F;
    }

    public static boolean isValidScaffoldBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockitem && InventoryUtility.isValidPlaceableBlock(blockitem.getBlock());
    }

    public static boolean isUsefulItem(ItemStack stack) {
        if (!stack.is(ItemTags.PICKAXES)
            && !stack.is(ItemTags.AXES)
            && !stack.is(ItemTags.SHOVELS)
            && !stack.is(ItemTags.SWORDS)
            && !isFood(stack)
            && !(stack.getItem() instanceof BlockItem)
            && !stack.is(Items.BOW)
            && !stack.is(Items.ARROW)
            && !stack.is(Items.ENDER_PEARL)
            && !isProjectileOrRod(stack)) {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot().getType() == Type.HUMANOID_ARMOR) {
                return true;
            }

            if (isPotion(stack)) {
                PotionContents potioncontents = stack.get(DataComponents.POTION_CONTENTS);
                if (potioncontents != null) {
                    for (MobEffectInstance mobeffectinstance : potioncontents.getAllEffects()) {
                        for (Holder holder : GOOD_POTION_EFFECTS) {
                            if (mobeffectinstance.is(holder)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public static int toContainerSlot(int slot) {
        return slot >= 36 ? 8 - (slot - 36) : (slot < 9 ? slot + 36 : slot);
    }

    public static void clickSlot(int slot, int slot2, ContainerInput containerInput) {
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, slot, slot2, containerInput, mc.player);
    }

    public void markSilentOpen() {
        if (!this.silentInventoryOpen && !this.invOnly.getValue() && !(mc.gui.screen() instanceof InventoryScreen)) {
            this.silentInventoryOpen = true;
        }
    }

    public void closeSilentInventory() {
        if (this.silentInventoryOpen && mc.getConnection() != null && mc.player != null) {
            mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
        }

        this.silentInventoryOpen = false;
        this.busy = false;
    }

    public boolean canPerformAction() {
        return true;
    }

    public void dropSlot(int slot) {
        if ((!this.performedActionThisTick || this.delay.getValue().intValue() == 0) && this.canPerformAction()) {
            this.markSilentOpen();
            suppressDropSwing = this.hideSwing.getValue();

            try {
                clickSlot(toContainerSlot(slot), 1, ContainerInput.THROW);
            } finally {
                suppressDropSwing = false;
            }

            this.actionTimer.reset();
            this.performedActionThisTick = true;
        }
    }

    public void swapToHotbar(int first, int second) {
        if ((!this.performedActionThisTick || this.delay.getValue().intValue() == 0) && this.canPerformAction()) {
            this.markSilentOpen();
            clickSlot(toContainerSlot(first), toContainerSlot(second), ContainerInput.SWAP);
            this.actionTimer.reset();
            this.performedActionThisTick = true;
        }
    }

    public void shiftClickSlot(int slot) {
        if ((!this.performedActionThisTick || this.delay.getValue().intValue() == 0) && this.canPerformAction()) {
            this.markSilentOpen();
            clickSlot(toContainerSlot(slot), 0, ContainerInput.QUICK_MOVE);
            this.actionTimer.reset();
            this.performedActionThisTick = true;
        }
    }
}
