package dev.adapt.v3.feature.module.impl.world;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.animation.Spring;

public final class WorldEditorModule extends Module {
    public final BooleanProperty brightness = new BooleanProperty("Brightness", true);
    public final NumberProperty brightnessStrength = (NumberProperty)new NumberProperty("Brightness Strength", "%", 100.0, 10.0, 100.0, 5.0)
        .hideIf(() -> !this.brightness.getValue());
    public final BooleanProperty timeChange = new BooleanProperty("Time Change", false);
    public final NumberProperty time = (NumberProperty)new NumberProperty("Time", "h", 12.0, 0.0, 23.5, 0.5).hideIf(() -> !this.timeChange.getValue());
    public final Spring brightnessSpring = Spring.gentle(0.0F);

    public WorldEditorModule() {
        super("World Editor", "Client-side changes to how the world looks: brightness and time of day.", ModuleCategory.WORLD);
        this.addProperties(this.brightness, this.brightnessStrength, this.timeChange, this.time);
    }

    public float getBrightnessFactor() {
        this.brightnessSpring.target(this.isEnabled() && this.brightness.getValue() ? this.brightnessStrength.getFloatValue() / 100.0F : 0.0F);
        return Math.max(0.0F, this.brightnessSpring.update());
    }

    public boolean isTimeChangeActive() {
        return this.isEnabled() && this.timeChange.getValue();
    }

    public long getTimeTicks() {
        return Math.floorMod(Math.round((this.time.getValue() - 6.0) * 1000.0), 24000);
    }
}
