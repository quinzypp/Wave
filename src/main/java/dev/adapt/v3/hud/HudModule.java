package dev.adapt.v3.hud;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.animation.Spring;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudModule extends Module {
    public final NumberProperty scale = new NumberProperty("Scale", "x", 1.0, 0.5, 2.0, 0.05);
    public float anchorX;
    public float anchorY;
    public final Spring visibilitySpring = Spring.smooth(0.0F);

    public HudModule(String text, String text2, float first, float second) {
        this(text, text2, ModuleCategory.VISUAL, first, second);
    }

    public HudModule(String text, String text2, ModuleCategory moduleCategory, float first, float second) {
        super(text, text2, moduleCategory);
        this.anchorX = first;
        this.anchorY = second;
        this.addProperties(this.scale);
    }

    public abstract float getWidth();

    public abstract float getHeight();

    public abstract void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing);

    public float getAnchorX() {
        return this.anchorX;
    }

    public float getAnchorY() {
        return this.anchorY;
    }

    public void setAnchor(float first, float second) {
        this.anchorX = Math.max(0.0F, Math.min(1.0F, first));
        this.anchorY = Math.max(0.0F, Math.min(1.0F, second));
    }

    public float getBaseScale() {
        return 1.0F;
    }

    public float getEffectiveScale() {
        return this.scale.getFloatValue() * this.getBaseScale();
    }

    public float getScaledWidth() {
        return this.getWidth() * this.getEffectiveScale();
    }

    public float getScaledHeight() {
        return this.getHeight() * this.getEffectiveScale();
    }

    public float getEdgeMargin() {
        return 0.0F;
    }

    public boolean hasEdgeSnap() {
        return this.getEdgeMargin() > 0.0F;
    }

    public float snapAnchor(float value) {
        if (!this.hasEdgeSnap()) {
            return value;
        } else {
            return value <= 0.02F ? 0.0F : (value >= 0.98F ? 1.0F : value);
        }
    }

    public float resolveCoordinate(float value, int value2, float value3) {
        float f = Math.max(0.0F, Math.min(this.getEdgeMargin(), (value2 - value3) / 2.0F));
        return Math.max(0.0F, Math.min(value2 - value3, f + this.snapAnchor(value) * (value2 - value3 - 2.0F * f)));
    }

    public float resolveX(int value) {
        return this.resolveCoordinate(this.anchorX, value, this.getScaledWidth());
    }

    public float resolveY(int value) {
        return this.resolveCoordinate(this.anchorY, value, this.getScaledHeight());
    }

    public void setPositionFromScreen(float value, float value2, int value3, int value4) {
        float f = this.getScaledWidth();
        float f1 = this.getScaledHeight();
        float f2 = Math.max(0.0F, Math.min(this.getEdgeMargin(), (value3 - f) / 2.0F));
        float f3 = Math.max(0.0F, Math.min(this.getEdgeMargin(), (value4 - f1) / 2.0F));
        float f4 = value3 - f - 2.0F * f2 <= 0.0F ? 0.0F : (value - f2) / (value3 - f - 2.0F * f2);
        float f5 = value4 - f1 - 2.0F * f3 <= 0.0F ? 0.0F : (value2 - f3) / (value4 - f1 - 2.0F * f3);
        this.setAnchor(f4, f5);
    }

    public boolean shouldRender() {
        return true;
    }
}
