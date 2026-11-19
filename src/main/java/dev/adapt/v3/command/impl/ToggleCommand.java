package dev.adapt.v3.command.impl;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.command.Command;
import dev.adapt.v3.command.CommandException;
import dev.adapt.v3.command.CommandManager;
import dev.adapt.v3.feature.module.Module;

public final class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "toggle <module>", "Turns a module on or off.", "t");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) {
            throw this.usageError();
        }

        Module module = CommandManager.requireModule(String.join(" ", args));
        if (!module.canToggle()) {
            throw new CommandException(module.getName() + " is always on and can't be toggled.");
        }

        module.toggle();
        if (module.isEnabled()) {
            CommandManager.sendSuccess(module.getName() + " enabled.");
        } else {
            CommandManager.sendInfo(module.getName() + " disabled.");
        }

        Adapt.getInstance().getConfigManager().save();
    }
}
