package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.combat.criticals.mode.PacketCriticals;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CriticalsModule extends Module {
    public final EnumProperty<CriticalsModule.Mode> mode = new EnumProperty<>("Mode", CriticalsModule.Mode.PACKET);
    public final NumberProperty delay = (NumberProperty)new NumberProperty("Delay", 0.0, 0.0, 1000.0, 10.0)
        .hideIf(() -> this.mode.getValue() != CriticalsModule.Mode.PACKET);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public CriticalsModule() {
        super("Criticals", "Makes your hits critical hits.", ModuleCategory.COMBAT);
        this.addProperties(this.mode, this.delay);
        this.registerModes(new PacketCriticals(this));
    }

    @SafeVarargs
    public final void registerModes(ModuleMode<?>... modes) {
        this.modes.addAll(Arrays.asList(modes));
        this.mode.addListener((mode, mode2) -> {
            if (this.isEnabled()) {
                this.modes.forEach(ModuleMode::onDisable);
                this.enableCurrentMode();
            }
        });
    }

    public ModuleMode getCurrentMode() {
        for (ModuleMode modulemode : this.modes) {
            if (modulemode.getMode() == this.mode.getValue()) {
                return modulemode;
            }
        }

        return null;
    }

    public void enableCurrentMode() {
        ModuleMode modulemode = this.getCurrentMode();
        if (modulemode != null) {
            modulemode.onEnable();
        }
    }

    public NumberProperty getDelay() {
        return this.delay;
    }

    @Override
    public void onEnable() {
        this.enableCurrentMode();
    }

    @Override
    public void onDisable() {
        this.modes.forEach(ModuleMode::onDisable);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    public enum Mode implements Displayable {
        PACKET("Packet");

        public final String displayName;

        Mode(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
