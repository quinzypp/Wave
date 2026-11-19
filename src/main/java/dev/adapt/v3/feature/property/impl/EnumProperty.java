package dev.adapt.v3.feature.property.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.util.Displayable;

public final class EnumProperty<E extends Enum<E>> extends Property<E> {
    public final E[] values;

    public EnumProperty(String text, E value) {
        super(text);
        this.setValue((E)value);
        this.values = (E[])value.getDeclaringClass().getEnumConstants();
    }

    public E[] getValues() {
        return this.values;
    }

    public void cycle(boolean flag) {
        int i = this.getValue().ordinal();
        int j = (i + (flag ? 1 : this.values.length - 1)) % this.values.length;
        this.setValue(this.values[j]);
    }

    public boolean is(E value) {
        return this.getValue() == value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.getValue().name());
    }

    @Override
    public void fromJson(JsonElement element) {
        String s = element.getAsString();

        for (Enum oenum : this.values) {
            if (oenum.name().equals(s)) {
                this.setValue((E)oenum);
                break;
            }
        }
    }

    @Override
    public String getDisplayValue() {
        return this.getValue() instanceof Displayable displayable ? displayable.getDisplayName() : this.getValue().name();
    }
}
