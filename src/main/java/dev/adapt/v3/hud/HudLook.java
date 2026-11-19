package dev.adapt.v3.hud;

import dev.adapt.v3.util.Displayable;

public enum HudLook implements Displayable {
    CLASSIC("Classic"),
    GLOW("Glow"),
    PILL("Bold"),
    CAPSULE("Capsule");

    public final String displayName;

    HudLook(String text) {
        this.displayName = text;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }
}
