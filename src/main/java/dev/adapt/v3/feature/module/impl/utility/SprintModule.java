package dev.adapt.v3.feature.module.impl.utility;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.movement.invmove.InvMoveModule;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.ui.screen.DashboardScreen;
import dev.adapt.v3.util.Displayable;
import net.minecraft.client.gui.screens.Screen;

public final class SprintModule extends Module {
    public final EnumProperty<SprintModule.Mode> mode = new EnumProperty<>("Mode", SprintModule.Mode.NORMAL);

    public SprintModule() {
        super("Sprint", "Keeps the sprint key held for you.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode);
        this.setEnabled(true);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    @Subscribe
    public void onPostTick(PostTickEvent event) {
        if (mc.player != null && canSprintInScreen(mc.gui.screen())) {
            mc.options.keySprint.setDown(true);
        }
    }

    public static boolean canSprintInScreen(Screen screen) {
        return screen == null || screen instanceof DashboardScreen || InvMoveModule.canMoveInScreen(screen);
    }

    public static boolean isSprintEnabled() {
        Adapt adapt = Adapt.getInstance();
        if (adapt == null) {
            return false;
        }

        SprintModule sprintmodule = adapt.getModuleManager().getByClass(SprintModule.class);
        return sprintmodule != null && sprintmodule.isEnabled();
    }

    @Override
    public void onDisable() {
        mc.options.keySprint.setDown(false);
    }

    public static boolean isOmniSprint() {
        Adapt adapt = Adapt.getInstance();
        if (adapt == null) {
            return false;
        }

        SprintModule sprintmodule = adapt.getModuleManager().getByClass(SprintModule.class);
        return sprintmodule != null && sprintmodule.isEnabled() && sprintmodule.mode.getValue() == SprintModule.Mode.OMNI;
    }

    public enum Mode implements Displayable {
        NORMAL("Normal"),
        OMNI("Omni");

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
