package dev.adapt.v3.feature.property.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.adapt.v3.feature.property.Property;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiBooleanProperty extends Property<Map<String, Boolean>> {
    public final List<String> options;

    public MultiBooleanProperty(String text, Object... objects) {
        super(text);
        Map<String, Boolean> linkedhashmap = new LinkedHashMap<>();

        for (byte b0 = 0; b0 + 1 < objects.length; b0 += 2) {
            linkedhashmap.put((String)objects[b0], (Boolean)objects[b0 + 1]);
        }

        this.options = List.copyOf(linkedhashmap.keySet());
        this.setMap(linkedhashmap);
    }

    public List<String> getOptions() {
        return this.options;
    }

    public boolean isEnabled(String text) {
        return this.getValue().getOrDefault(text, false);
    }

    public void setEnabled(String text, boolean enabled) {
        if (this.options.contains(text) && this.isEnabled(text) != enabled) {
            Map<String, Boolean> linkedhashmap = new LinkedHashMap<>(this.getValue());
            linkedhashmap.put(text, enabled);
            this.setMap(linkedhashmap);
        }
    }

    public void toggle(String text) {
        this.setEnabled(text, !this.isEnabled(text));
    }

    public void setMap(Map<String, Boolean> map) {
        super.setValue(Collections.unmodifiableMap(new LinkedHashMap(map)));
    }

    @Override
    public JsonElement toJson() {
        JsonObject jsonobject = new JsonObject();
        this.getValue().forEach(jsonobject::addProperty);
        return jsonobject;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject jsonobject = element.getAsJsonObject();
            Map<String, Boolean> linkedhashmap = new LinkedHashMap<>(this.getValue());

            for (String s : this.options) {
                if (jsonobject.has(s)) {
                    linkedhashmap.put(s, jsonobject.get(s).getAsBoolean());
                }
            }

            this.setMap(linkedhashmap);
        }
    }

    @Override
    public String getDisplayValue() {
        StringBuilder stringbuilder = new StringBuilder();

        for (String s : this.options) {
            if (this.isEnabled(s)) {
                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(s);
            }
        }

        return stringbuilder.length() == 0 ? "None" : stringbuilder.toString();
    }
}
