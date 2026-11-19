package dev.adapt.v3.feature.helper.target;

import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.MultiBooleanProperty;

public final class TargetProperties {
    public static final String PLAYERS = "Players";
    public static final String HOSTILE = "Hostile";
    public static final String PASSIVE = "Passive";
    public static final String TEAMMATES = "Teammates";
    public final MultiBooleanProperty targets;
    public final BooleanProperty targetLocalPlayer;
    public final boolean allowLocalPlayerOption;

    public TargetProperties(Module module, boolean flag, boolean flag2, boolean flag3, boolean flag4, boolean flag5, boolean flag6) {
        this.allowLocalPlayerOption = flag2;
        this.targets = new MultiBooleanProperty("Targets", "Players", flag, "Hostile", flag4, "Passive", flag5, "Teammates", flag6);
        if (flag2) {
            this.targetLocalPlayer = (BooleanProperty)new BooleanProperty("Target local player", flag3).hideIf(() -> !this.targets.isEnabled("Players"));
        } else {
            this.targetLocalPlayer = null;
        }

        module.addProperties(this.targets, this.targetLocalPlayer);
    }

    public int getFlags() {
        return TargetFlags.pack(
            this.targets.isEnabled("Players"), this.targets.isEnabled("Hostile"), this.targets.isEnabled("Passive"), this.targets.isEnabled("Teammates")
        );
    }

    public boolean shouldTargetLocalPlayer() {
        return this.allowLocalPlayerOption && this.targetLocalPlayer != null && this.targetLocalPlayer.getValue();
    }
}
