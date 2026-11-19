package dev.adapt.v3.feature.property;

import com.google.gson.JsonElement;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Property<T> {
    public final String name;
    public String key;
    public T value;
    public BooleanSupplier hiddenSupplier;
    public String description;
    public List<Listener<T>> listeners;

    public Property(String text) {
        this.name = text;
        this.key = text.toLowerCase().replace(' ', '_');
    }

    public final Property<T> hideIf(BooleanSupplier condition) {
        this.hiddenSupplier = condition;
        return this;
    }

    public final Property<T> describe(String text) {
        this.description = text;
        return this;
    }

    public final String getDescription() {
        return this.description;
    }

    public final String getName() {
        return this.name;
    }

    public final String getKey() {
        return this.key;
    }

    public final Property<T> withKey(String key) {
        this.key = key;
        return this;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        Object object = this.value;
        this.value = (T)value;
        EventManager.post(new PropertyChangeEvent(this));
        this.notifyListeners((T)object, (T)value);
    }

    public final boolean isHidden() {
        return this.hiddenSupplier != null && this.hiddenSupplier.getAsBoolean();
    }

    public final Property<T> addListener(Listener<T> listener) {
        if (this.listeners == null) {
            this.listeners = new ArrayList<>(1);
        }

        this.listeners.add(listener);
        return this;
    }

    public final void notifyListeners(T first, T second) {
        if (this.listeners != null) {
            this.listeners.forEach(listener -> listener.onChange((T)first, (T)second));
        }
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);

    public String getDisplayValue() {
        return String.valueOf(this.getValue());
    }
}
