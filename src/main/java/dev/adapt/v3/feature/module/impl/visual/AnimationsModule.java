package dev.adapt.v3.feature.module.impl.visual;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public final class AnimationsModule extends Module {
    public final BooleanProperty swordBlock = new BooleanProperty("Sword block", true);
    public final BooleanProperty swingWhileUsing = new BooleanProperty("Swing while using", true);
    public final BooleanProperty noCooldownDip = new BooleanProperty("No cooldown dip", true);
    public final BooleanProperty alwaysShow = new BooleanProperty("Always show", true);
    public final EnumProperty<AnimationsModule.BlockStyle> blockStyle = new EnumProperty<>("Block style", AnimationsModule.BlockStyle.V1_7);

    public AnimationsModule() {
        super("Animations", "1.7 sword blocking, swinging while blocking / eating and item positions.", ModuleCategory.VISUAL);
        this.addProperties(this.swordBlock, this.blockStyle, this.swingWhileUsing, this.noCooldownDip, this.alwaysShow);
    }

    public static AnimationsModule.BlockStyle getBlockStyle() {
        AnimationsModule animationsmodule = getInstance();
        return animationsmodule == null ? AnimationsModule.BlockStyle.V1_7 : animationsmodule.blockStyle.getValue();
    }

    public static boolean isAlwaysShow() {
        AnimationsModule animationsmodule = getInstance();
        return animationsmodule != null && animationsmodule.isEnabled() && animationsmodule.alwaysShow.getValue();
    }

    public static AnimationsModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(AnimationsModule.class);
    }

    public static boolean isActive() {
        AnimationsModule animationsmodule = getInstance();
        return animationsmodule != null && animationsmodule.isEnabled();
    }

    public boolean isFakeSwordBlocking(LivingEntity living) {
        return this.isEnabled()
            && this.swordBlock.getValue()
            && living.getMainHandItem().is(ItemTags.SWORDS)
            && living.getOffhandItem().getItem() instanceof ShieldItem
            && living.isUsingItem()
            && living.getUsedItemHand() == InteractionHand.OFF_HAND;
    }

    public static boolean shouldHideOffhandShield(ItemStack stack) {
        AnimationsModule animationsmodule = getInstance();
        return animationsmodule != null && animationsmodule.isEnabled() && animationsmodule.swordBlock.getValue() && stack.getItem() instanceof ShieldItem;
    }

    public enum BlockStyle implements Displayable {
        V1_7("1.7"),
        SOFT("Avatar"),
        PUSH("Push"),
        PULSE("Pulse"),
        SIGMA_SLIDE("Slide"),
        SIGMA_SLIDE_SOFT("Slide 2"),
        SIGMA_SLIDE_SMOOTH("Slide 3"),
        SIGMA_SLIDE_LONG("Smooth");

        public final String displayName;

        BlockStyle(String text) {
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
