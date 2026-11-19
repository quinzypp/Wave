package dev.adapt.v3.feature.module.impl.hud;

import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmorStatusHud extends HudModule {
    public static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };
    public static final float PAD = 5.0F;
    public static final float ICON_SIZE = 16.0F;
    public static final float VERTICAL_GAP = 4.0F;
    public static final float TEXT_GAP_X = 5.0F;
    public static final float TEXT_GAP_Y = 2.5F;
    public static final float HORIZONTAL_PAD_X = 7.0F;
    public static final float HORIZONTAL_GAP = 6.0F;
    public static final int COLOR_HIGH = -8462170;
    public static final int COLOR_MID = -671638;
    public static final int COLOR_LOW = -1022854;
    public final EnumProperty<ArmorStatusHud.Layout> layout = new EnumProperty<>("Layout", ArmorStatusHud.Layout.VERTICAL);
    public final Spring[] slotSprings = new Spring[SLOTS.length];
    public final ItemStack[] cachedStacks = new ItemStack[SLOTS.length];
    public float width = 26.0F;
    public float height = 26.0F;

    public ArmorStatusHud() {
        super("Armor Status", "Armour and held item, with the uses left on each.", 0.994F, 0.6F);
        this.addProperties(this.layout);

        for (int i = 0; i < SLOTS.length; i++) {
            this.slotSprings[i] = Spring.smooth(0.0F);
            this.cachedStacks[i] = ItemStack.EMPTY;
        }
    }

    public boolean isHorizontal() {
        return this.layout.getValue() == ArmorStatusHud.Layout.HORIZONTAL;
    }

    @Override
    public float getBaseScale() {
        return 0.85F;
    }

    @Override
    public float getWidth() {
        return this.width;
    }

    @Override
    public float getHeight() {
        return this.height;
    }

    public ItemStack getDisplayStack(EquipmentSlot equipmentSlot, boolean flag) {
        ItemStack itemstack = mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(equipmentSlot);
        if (itemstack.isEmpty() && flag) {
            ItemStack itemstack1 = new ItemStack(switch (equipmentSlot) {
                case HEAD -> Items.NETHERITE_HELMET;
                case CHEST -> Items.NETHERITE_CHESTPLATE;
                case LEGS -> Items.NETHERITE_LEGGINGS;
                case FEET -> Items.NETHERITE_BOOTS;
                default -> Items.NETHERITE_SWORD;
            });
            itemstack1.setDamageValue(itemstack1.getMaxDamage() / 4);
            return itemstack1;
        } else {
            return itemstack;
        }
    }

    @Override
    public boolean shouldRender() {
        for (int i = 0; i < SLOTS.length; i++) {
            if (this.slotSprings[i].get() > 0.01F || !this.getDisplayStack(SLOTS[i], false).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public static String getDurabilityText(ItemStack stack) {
        return stack.isDamageableItem() ? String.valueOf(stack.getMaxDamage() - stack.getDamageValue()) : null;
    }

    public static int getDurabilityColor(ItemStack stack) {
        float f = 1.0F - (float)stack.getDamageValue() / stack.getMaxDamage();
        return f > 0.5F ? ColorUtility.lerpColor(-671638, -8462170, (f - 0.5F) * 2.0F) : ColorUtility.lerpColor(-1022854, -671638, f * 2.0F);
    }

    public static float easeSlotProgress(Spring spring) {
        float f = Math.max(0.0F, Math.min(1.0F, spring.get()));
        return 1.0F - (1.0F - f) * (1.0F - f);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing) {
        float[] afloat = new float[SLOTS.length];
        String[] astring = new String[SLOTS.length];
        float[] afloat1 = new float[SLOTS.length];

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack itemstack = this.getDisplayStack(SLOTS[i], editing);
            if (!itemstack.isEmpty()) {
                this.cachedStacks[i] = itemstack;
            }

            this.slotSprings[i].target(itemstack.isEmpty() ? 0.0F : 1.0F).update();
            afloat[i] = easeSlotProgress(this.slotSprings[i]);
            if (afloat[i] < 0.005F && itemstack.isEmpty()) {
                this.cachedStacks[i] = ItemStack.EMPTY;
            }

            astring[i] = this.cachedStacks[i].isEmpty() ? null : getDurabilityText(this.cachedStacks[i]);
            afloat1[i] = astring[i] == null ? 0.0F : Fonts.LIST.getWidth(astring[i]);
        }

        if (this.isHorizontal()) {
            this.renderHorizontal(graphics, afloat, astring, afloat1);
        } else {
            this.renderVertical(graphics, afloat, astring, afloat1);
        }
    }

    public void renderVertical(GuiGraphicsExtractor graphics, float[] floats, String[] strings, float[] floats2) {
        float f = 0.0F;
        float f1 = 0.0F;

        for (int i = 0; i < SLOTS.length; i++) {
            f += 20.0F * floats[i];
            f1 = Math.max(f1, floats2[i] * floats[i]);
        }

        f = Math.max(0.0F, f - 4.0F);
        this.width = 26.0F + (f1 > 0.0F ? 5.0F + f1 + 1.0F : 0.0F);
        this.height = 10.0F + Math.max(0.0F, f);
        Glass.drawToastSurface(graphics, 0.0F, 0.0F, this.width, this.height, Math.min(Theme.getCornerRadius(), this.width / 2.0F), 1.0F);
        float f3 = 5.0F;

        for (int j = 0; j < SLOTS.length; j++) {
            if (!(floats[j] <= 0.01F)) {
                ItemStack itemstack = this.cachedStacks[j];
                float f2 = f3 + 16.0F * floats[j] / 2.0F;
                this.renderItem(graphics, itemstack, 5.0F, f2 - 8.0F, floats[j]);
                if (strings[j] != null) {
                    Fonts.LIST
                        .drawCached(
                            graphics, strings[j], 26.0F, f2 - Fonts.LIST.getHalfPointSize(), ColorUtility.scaleAlpha(getDurabilityColor(itemstack), floats[j] * floats[j])
                        );
                }

                f3 += 20.0F * floats[j];
            }
        }
    }

    public void renderHorizontal(GuiGraphicsExtractor graphics, float[] floats, String[] strings, float[] floats2) {
        float f = 16.0F;
        float f1 = 0.0F;
        boolean flag = false;

        for (int i = 0; i < SLOTS.length; i++) {
            if (!(floats[i] <= 0.01F)) {
                f = Math.max(f, floats2[i]);
                if (strings[i] != null) {
                    flag = true;
                }
            }
        }

        for (int k = 0; k < SLOTS.length; k++) {
            f1 += (f + 6.0F) * floats[k];
        }

        f1 = Math.max(0.0F, f1 - 6.0F);
        float f4 = Fonts.LIST.getPointSize();
        this.width = 14.0F + f1;
        this.height = 26.0F + (flag ? 2.5F + f4 : 0.0F);
        Glass.drawToastSurface(graphics, 0.0F, 0.0F, this.width, this.height, Math.min(Theme.getCornerRadius(), this.height / 2.0F), 1.0F);
        float f2 = 7.0F;

        for (int j = 0; j < SLOTS.length; j++) {
            if (!(floats[j] <= 0.01F)) {
                ItemStack itemstack = this.cachedStacks[j];
                float f3 = f2 + f * floats[j] / 2.0F;
                this.renderItem(graphics, itemstack, f3 - 8.0F, 5.0F, floats[j]);
                if (strings[j] != null) {
                    Fonts.LIST
                        .drawCached(
                            graphics,
                            strings[j],
                            Math.round((f3 - floats2[j] / 2.0F) * 2.0F) / 2.0F,
                            23.5F,
                            ColorUtility.scaleAlpha(getDurabilityColor(itemstack), floats[j] * floats[j])
                        );
                }

                f2 += (f + 6.0F) * floats[j];
            }
        }
    }

    public void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, float value, float value2, float value3) {
        if (!stack.isEmpty()) {
            boolean flag = value3 >= 0.999F;
            graphics.pose().pushMatrix();
            if (flag) {
                graphics.pose().translate(Math.round(value), Math.round(value2));
            } else {
                graphics.pose().translate(value + 8.0F, value2 + 8.0F);
                graphics.pose().scale(value3, value3);
                graphics.pose().translate(-8.0F, -8.0F);
            }

            graphics.item(stack, 0, 0);
            if (getDurabilityText(stack) == null && stack.getCount() > 1) {
                graphics.itemDecorations(mc.font, stack, 0, 0);
            }

            graphics.pose().popMatrix();
        }
    }

    public enum Layout implements Displayable {
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal");

        public final String displayName;

        Layout(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
