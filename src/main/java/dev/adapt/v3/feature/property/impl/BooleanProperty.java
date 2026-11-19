package dev.adapt.v3.feature.property.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.adapt.v3.feature.property.Property;

public final class BooleanProperty extends Property<Boolean> {
    public BooleanProperty(String text, boolean flag) {
        super(text);
        this.setValue(flag);
    }

    public void toggle() {
        this.setValue(!this.getValue());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        this.setValue(element.getAsBoolean());
    }

    @Override
    public String getDisplayValue() {
        return this.getValue() ? "On" : "Off";
    }
}
