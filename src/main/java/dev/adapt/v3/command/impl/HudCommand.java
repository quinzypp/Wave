package dev.adapt.v3.command.impl;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.command.Command;

public final class HudCommand extends Command {
    public HudCommand() {
        super("hud", "hud", "Opens the HUD editor.", "hudeditor");
    }

    @Override
    public void execute(String[] args) {
        Adapt.getInstance().getCommandManager().defer(() -> Adapt.getInstance().openHudEditor());
    }
}
