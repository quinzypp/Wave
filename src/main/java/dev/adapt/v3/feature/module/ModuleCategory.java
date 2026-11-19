package dev.adapt.v3.feature.module;

import dev.adapt.v3.util.Displayable;

public enum ModuleCategory implements Displayable {
    COMBAT("Combat", "\ue1b3"),
    MOVEMENT("Movement", "\ue566"),
    UTILITY("Utility", "\ue869"),
    VISUAL("Visual", "\ue8f4"),
    WORLD("World", "\ue80b");

    public final String displayName;
    public final String icon;
    public static final ModuleCategory[] VALUES = values();

    ModuleCategory(String text, String text2) {
        this.displayName = text;
        this.icon = text2;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public String getIcon() {
        return this.icon;
    }
}
