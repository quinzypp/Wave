package dev.adapt.v3.feature.module.impl.client;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.HiddenModule;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.ColorProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.ui.theme.Accent;

public final class ThemeModule extends Module implements HiddenModule {
    public final EnumProperty<Accent> accent = new EnumProperty<>("Accent", Accent.PEARL);
    public final ColorProperty primary = (ColorProperty)new ColorProperty("Primary", -41073).hideIf(() -> !this.isCustomAccent());
    public final ColorProperty secondary = (ColorProperty)new ColorProperty("Secondary", -7639809).hideIf(() -> !this.isCustomAccent());
    public final BooleanProperty animateGradient = (BooleanProperty)new BooleanProperty("Animate Gradient", false).hideIf(() -> !this.hasGradient());
    public final NumberProperty gradientSpeed = (NumberProperty)new NumberProperty("Gradient Speed", "x", 1.0, 0.25, 3.0, 0.05)
        .hideIf(() -> !this.hasGradient() || !this.animateGradient.getValue());
    public final NumberProperty lensZoom = new NumberProperty("Lens Zoom", "x", 1.12, 1.0, 1.4, 0.02);
    public final NumberProperty bloom = new NumberProperty("Bloom", "%", 45.0, 0.0, 100.0, 5.0);
    public final NumberProperty cornerRadius = new NumberProperty("Corner Radius", "px", 6.0, 2.0, 14.0, 1.0);

    public ThemeModule() {
        super("Theme", "Accent colour, glass, bloom and rounding.", ModuleCategory.VISUAL);
        this.addProperties(this.accent, this.primary, this.secondary, this.animateGradient, this.gradientSpeed, this.lensZoom, this.bloom, this.cornerRadius);
        this.setEnabled(true);
    }

    @Override
    public boolean canToggle() {
        return false;
    }

    @Override
    public void onBind() {
        Adapt.getInstance().openDashboard();
    }

    public boolean isCustomAccent() {
        return this.accent.getValue() == Accent.CUSTOM;
    }

    public boolean hasGradient() {
        return this.isCustomAccent() || this.accent.getValue().secondaryColor != 0;
    }

    @Override
    public String getSuffix() {
        return this.accent.getDisplayValue();
    }
}
