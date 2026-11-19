package dev.adapt.v3.binding;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.adapt.v3.binding.type.InputType;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;

public final class BindRegistry {
    public final Multimap<Bind, Bindable> binds = HashMultimap.create();

    public void bind(int value, Bindable bindable, InputType inputType) {
        this.binds.put(new Bind(value, inputType), bindable);
    }

    public void unbind(Bindable bindable) {
        this.binds.entries().removeIf(entry -> entry.getValue() == bindable);
    }

    public void trigger(int value, InputType inputType) {
        if (Minecraft.getInstance().gui.screen() == null) {
            for (Bindable bindable : List.copyOf(this.binds.get(new Bind(value, inputType)))) {
                bindable.onBind();
            }
        }
    }

    public Multimap<Bind, Bindable> getBinds() {
        return this.binds;
    }

    public Optional<Bind> getBind(Bindable bindable) {
        return this.binds.entries().stream().filter(entry -> entry.getValue().equals(bindable)).map(Entry::getKey).findFirst();
    }
}
