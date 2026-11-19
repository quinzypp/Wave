package dev.adapt.v3.command;

import java.util.List;

public abstract class Command {
    public final String name;
    public final String usage;
    public final String description;
    public final List<String> aliases;

    public Command(String text, String text2, String text3, String... strings) {
        this.name = text;
        this.usage = text2;
        this.description = text3;
        this.aliases = List.of(strings);
    }

    public abstract void execute(String[] args) throws CommandException;

    public final String getName() {
        return this.name;
    }

    public final String getUsage() {
        return this.usage;
    }

    public final String getDescription() {
        return this.description;
    }

    public final List<String> getAliases() {
        return this.aliases;
    }

    public final boolean matches(String text) {
        return this.name.equalsIgnoreCase(text) || this.aliases.stream().anyMatch(text2 -> text2.equalsIgnoreCase(text));
    }

    public final CommandException usageError() {
        return new CommandException("Usage: ." + this.usage);
    }
}
