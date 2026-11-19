package dev.adapt.v3.feature.module.registry;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.UnknownModuleException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class ModuleRegistry {
    public final ClassToInstanceMap<Module> byClass;
    public final ImmutableMap<String, Module> byName;
    public final List<Module> modules;

    public ModuleRegistry(ClassToInstanceMap classToInstanceMap, ImmutableMap immutableMap, List list) {
        this.byClass = classToInstanceMap;
        this.byName = immutableMap;
        this.modules = list;
    }

    public void ifPresentOrElse(String text, Consumer<Module> consumer, Consumer<UnknownModuleException> consumer2) {
        try {
            consumer.accept(this.getByName(text));
        } catch (UnknownModuleException unknownmoduleexception) {
            consumer2.accept(unknownmoduleexception);
        }
    }

    public <T extends Module> T getByClass(Class<T> type) {
        return this.byClass.getInstance(type);
    }

    public Module getByName(String name) throws UnknownModuleException {
        Module module = this.byName.get(name);
        if (module == null) {
            throw new UnknownModuleException(name);
        } else {
            return module;
        }
    }

    public Collection<Module> getAll() {
        return this.modules;
    }

    public List<Module> getByCategory(ModuleCategory moduleCategory) {
        return this.modules.stream().filter(module -> moduleCategory.equals(module.getCategory())).toList();
    }

    public static ModuleRegistry of(Module... modules) {
        ModuleRegistry.Builder moduleregistry$builder = builder();

        for (Module module : modules) {
            moduleregistry$builder.register(module);
        }

        return moduleregistry$builder.build();
    }

    public static ModuleRegistry.Builder builder() {
        return new ModuleRegistry.Builder();
    }

    public static class Builder {
        public final ImmutableClassToInstanceMap.Builder byClass = ImmutableClassToInstanceMap.builder();
        public final ImmutableMap.Builder byName = ImmutableMap.builder();
        public final List ordered = new ArrayList();

        public ModuleRegistry.Builder register(Module module) {
            this.byClass.put(module.getClass(), module);
            this.byName.put(module.getId(), module);
            this.ordered.add(module);
            return this;
        }

        public ModuleRegistry build() {
            return new ModuleRegistry(this.byClass.build(), this.byName.build(), List.copyOf(this.ordered));
        }
    }
}
