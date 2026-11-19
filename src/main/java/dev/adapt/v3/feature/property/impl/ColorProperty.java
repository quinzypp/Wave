package dev.adapt.v3.feature.property.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.util.render.ColorUtility;

public final class ColorProperty extends Property<Integer> {
    public final float[] hsv = new float[3];

    public ColorProperty(String text, int value) {
        super(text);
        this.setValue(value);
        float[] afloat = ColorUtility.toHsb(value);
        System.arraycopy(afloat, 0, this.hsv, 0, 3);
    }

    public float[] getHsv() {
        return this.hsv;
    }

    public void setHue(float hue) {
        this.hsv[0] = hue;
        this.syncFromHsv();
    }

    public void setSaturationBrightness(float first, float second) {
        this.hsv[1] = Math.max(0.0F, Math.min(1.0F, first));
        this.hsv[2] = Math.max(0.0F, Math.min(1.0F, second));
        this.syncFromHsv();
    }

    public void setRgb(int rgb) {
        float[] afloat = ColorUtility.toHsb(rgb);
        if (afloat[1] > 0.001F && afloat[2] > 0.001F) {
            this.hsv[0] = afloat[0];
        }

        this.hsv[1] = afloat[1];
        this.hsv[2] = afloat[2];
        this.setValue(0xFF000000 | rgb);
    }

    public void syncFromHsv() {
        this.setValue(ColorUtility.fromHsb(this.hsv[0], this.hsv[1], this.hsv[2]));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        int i = element.getAsInt();
        float[] afloat = ColorUtility.toHsb(i);
        System.arraycopy(afloat, 0, this.hsv, 0, 3);
        this.setValue(0xFF000000 | i);
    }

    @Override
    public String getDisplayValue() {
        return String.format("#%06X", this.getValue() & 16777215);
    }
}
