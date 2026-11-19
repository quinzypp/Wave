package dev.adapt.v3.feature.module.impl.movement.invmove;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.movement.invmove.mode.VanillaInvMove;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.util.Displayable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

public final class InvMoveModule extends Module {
    public final EnumProperty<InvMoveModule.Mode> mode = new EnumProperty<>("Mode", InvMoveModule.Mode.VANILLA);
    public final List<ModuleMode<?>> modes = new ArrayList<>();

    public InvMoveModule() {
        super("Inv Move", "Lets you move while an inventory or menu is open.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode);
        this.registerModes(new VanillaInvMove(this));
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

    @Override
    public void onEnable() {
        this.enableCurrentMode();
    }

    @Override
    public void onDisable() {
        this.modes.forEach(ModuleMode::onDisable);
    }

    public static boolean canMoveInScreen(Screen screen) {
        Adapt adapt = Adapt.getInstance();
        if (adapt != null && screen != null) {
            InvMoveModule invmovemodule = adapt.getModuleManager().getByClass(InvMoveModule.class);
            return invmovemodule != null && invmovemodule.isEnabled() && VanillaInvMove.isAllowedScreen(screen);
        } else {
            return false;
        }
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    public enum Mode implements Displayable {
        VANILLA("Vanilla");

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
