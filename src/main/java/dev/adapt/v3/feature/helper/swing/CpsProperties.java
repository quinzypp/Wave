package dev.adapt.v3.feature.helper.swing;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;
import java.util.function.BooleanSupplier;

public final class CpsProperties {
    public final EnumProperty mode;
    public final NumberProperty minCps;
    public final NumberProperty maxCps;
    public BooleanSupplier hideCondition = () -> false;
    public long nextDelayMs;

    public CpsProperties(Module module) {
        this(module, "CPS", true);
    }

    public CpsProperties(Module module, String text, boolean flag) {
        if (flag) {
            this.mode = (EnumProperty)new EnumProperty<>(text + " mode", CpsProperties.Mode.LEGACY).hideIf(() -> this.hideCondition.getAsBoolean());
        } else {
            this.mode = null;
        }

        this.minCps = (NumberProperty)new NumberProperty(text + " min", 10.0, 1.0, 20.0, 1.0)
            .hideIf(() -> this.hideCondition.getAsBoolean() || this.isModern());
        this.maxCps = (NumberProperty)new NumberProperty(text + " max", 10.0, 1.0, 20.0, 1.0)
            .hideIf(() -> this.hideCondition.getAsBoolean() || this.isModern());
        module.addProperties(this.mode, this.minCps, this.maxCps);
    }

    public CpsProperties hideIf(BooleanSupplier condition) {
        this.hideCondition = condition;
        return this;
    }

    public boolean isModern() {
        return this.mode != null && this.mode.getValue() == CpsProperties.Mode.MODERN;
    }

    public double randomCps() {
        double d0 = this.minCps.getValue();
        double d1 = Math.max(d0, this.maxCps.getValue());
        return d0 + Math.random() * (d1 - d0);
    }

    public long computeDelayMs() {
        return (long)(1000.0 / this.randomCps());
    }

    public void rollNextDelay() {
        this.nextDelayMs = this.computeDelayMs();
    }

    public long getNextDelayMs() {
        return this.nextDelayMs;
    }

    public enum Mode implements Displayable {
        LEGACY("Legacy"),
        MODERN("Modern");

        public final String name;

        Mode(String text) {
            this.name = text;
        }

        @Override
        public String getDisplayName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
