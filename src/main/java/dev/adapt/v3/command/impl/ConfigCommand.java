package dev.adapt.v3.command.impl;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.command.Command;
import dev.adapt.v3.command.CommandException;
import dev.adapt.v3.command.CommandManager;
import dev.adapt.v3.config.ConfigManager;
import java.util.List;
import java.util.Locale;

public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "config <save|load|delete> <name> | config list | config folder", "Saves and loads named configs.", "c", "cfg");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) {
            throw this.usageError();
        }

        ConfigManager configmanager = Adapt.getInstance().getConfigManager();
        String s = args[0].toLowerCase(Locale.ROOT);
        if (s.equals("list") || s.equals("ls")) {
            List list = configmanager.listConfigs();
            if (list.isEmpty()) {
                CommandManager.sendInfo("No configs yet. Save one with .config save <name>.");
            } else {
                CommandManager.sendInfo("Configs (" + list.size() + "): " + String.join(", ", list));
            }

            CommandManager.sendInfo("Folder: " + configmanager.getConfigsDir());
        } else if (!s.equals("folder") && !s.equals("dir") && !s.equals("open")) {
            if (args.length < 2) {
                throw this.usageError();
            }

            String s1 = args[1];
            if (!ConfigManager.isValidConfigName(s1)) {
                throw new CommandException("Config names can only use letters, numbers, '-', '_' and '.'.");
            }

            switch (s) {
                case "save":
                case "s":
                    boolean flag = configmanager.exists(s1);
                    if (!configmanager.saveConfig(s1)) {
                        throw new CommandException("Couldn't save config \"" + s1 + "\".");
                    }

                    CommandManager.sendSuccess((flag ? "Overwrote" : "Saved") + " config \"" + s1 + "\".");
                    break;
                case "load":
                case "l":
                    if (!configmanager.exists(s1)) {
                        throw new CommandException("No config called \"" + s1 + "\".");
                    }

                    if (!configmanager.loadConfig(s1)) {
                        throw new CommandException("Couldn't load config \"" + s1 + "\".");
                    }

                    CommandManager.sendSuccess("Loaded config \"" + s1 + "\".");
                    break;
                case "delete":
                case "del":
                case "remove":
                case "rm":
                    if (!configmanager.exists(s1)) {
                        throw new CommandException("No config called \"" + s1 + "\".");
                    }

                    if (!configmanager.deleteConfig(s1)) {
                        throw new CommandException("Couldn't delete config \"" + s1 + "\".");
                    }

                    CommandManager.sendSuccess("Deleted config \"" + s1 + "\".");
                    break;
                default:
                    throw this.usageError();
            }
        } else {
            if (!configmanager.openConfigsFolder()) {
                throw new CommandException("Couldn't open the configs folder.");
            }

            CommandManager.sendSuccess("Opened the configs folder.");
        }
    }
}
