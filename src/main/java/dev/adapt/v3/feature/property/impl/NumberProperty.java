package dev.adapt.v3.feature.property.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.util.MathUtility;

public final class NumberProperty extends Property<Double> {
    public final double min;
    public final double max;
    public final double step;
    public String suffix = "";

    public NumberProperty(String text, double value, double value2, double value3, double value4) {
        super(text);
        this.min = value2;
        this.max = value3;
        this.step = value4;
        this.setValue(value);
    }

    public NumberProperty(String text, String text2, double value, double value2, double value3, double value4) {
        this(text, value, value2, value3, value4);
        this.suffix = text2;
    }

    public double getStep() {
        return this.step;
    }

    public double getMax() {
        return this.max;
    }

    public double getMin() {
        return this.min;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public float getFloatValue() {
        return this.getValue().floatValue();
    }

    public int getIntValue() {
        return this.getValue().intValue();
    }

    public double getNormalized() {
        return (this.getValue() - this.min) / (this.max - this.min);
    }

    public void setNormalized(double normalized) {
        this.setValue(this.min + (this.max - this.min) * normalized);
    }

    public void setValue(Double value) {
        super.setValue(MathUtility.roundToStepAndClamp(value, this.min, this.max, this.step));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        this.setValue(element.getAsDouble());
    }

    @Override
    public String getDisplayValue() {
        double d0 = this.getValue();
        String s = this.step >= 1.0 ? String.valueOf((long)d0) : String.format("%.2f", d0).replaceAll("0+$", "").replaceAll("\\.$", "");
        return s + this.suffix;
    }
}
