package dev.adapt.v3.command.impl;

import dev.adapt.v3.command.Command;
import dev.adapt.v3.command.CommandManager;

public final class HelpCommand extends Command {
    public final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        super("help", "help", "Lists every command.", "h", "?");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(String[] args) {
        CommandManager.sendInfo("Commands:");

        for (Command command : this.commandManager.getCommands()) {
            String s = command.getAliases().isEmpty() ? "" : " (." + String.join(", .", command.getAliases()) + ")";
            CommandManager.sendInfo("." + command.getUsage() + s + " - " + command.getDescription());
        }
    }
}
