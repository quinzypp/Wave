package dev.adapt.v3.command;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.command.impl.BindCommand;
import dev.adapt.v3.command.impl.ConfigCommand;
import dev.adapt.v3.command.impl.HelpCommand;
import dev.adapt.v3.command.impl.HudCommand;
// VENDOR-DISABLED: import dev.adapt.v3.command.impl.OnlineCommand;
import dev.adapt.v3.command.impl.ToggleCommand;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.util.player.ChatUtility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class CommandManager implements Listener {
    public static final String PREFIX = ".";
    public final List<Command> commands = new ArrayList<>();
    public final List<Runnable> deferredActions = new ArrayList<>();

    public CommandManager() {
        this.commands.add(new HelpCommand(this));
        this.commands.add(new BindCommand());
        this.commands.add(new ToggleCommand());
        this.commands.add(new ConfigCommand());
        this.commands.add(new HudCommand());
        // VENDOR-DISABLED: this.commands.add(new OnlineCommand());
        EventManager.register(this);
    }

    public List<Command> getCommands() {
        return this.commands;
    }

    public boolean handleChatInput(String message) {
        if (message.startsWith(".") && message.length() != ".".length()) {
            String[] astring = message.substring(".".length()).trim().split("\\s+");
            String s = astring[0];
            String[] astring1 = Arrays.copyOfRange(astring, 1, astring.length);

            for (Command command : this.commands) {
                if (command.matches(s)) {
                    try {
                        command.execute(astring1);
                    } catch (CommandException commandexception) {
                        sendError(commandexception.getMessage());
                    } catch (Exception exception) {
                        sendError("Something went wrong running ." + command.getName() + ".");
                        Adapt.LOGGER.error("Command failed: " + message, exception);
                    }

                    return true;
                }
            }

            sendError("Unknown command. Type .help for a list.");
            return true;
        } else {
            return false;
        }
    }

    public void defer(Runnable action) {
        this.deferredActions.add(action);
    }

    @Subscribe
    public void onPostTick(PostTickEvent event) {
        if (!this.deferredActions.isEmpty()) {
            List<Runnable> list = List.copyOf(this.deferredActions);
            this.deferredActions.clear();
            list.forEach(Runnable::run);
        }
    }

    public static Module findModule(String name) {
        String s = normalizeName(name);

        for (Module module : Adapt.getInstance().getModuleManager().getAll()) {
            if (normalizeName(module.getName()).equals(s) || normalizeName(module.getId()).equals(s)) {
                return module;
            }
        }

        return null;
    }

    public static Module requireModule(String name) throws CommandException {
        Module module = findModule(name);
        if (module == null) {
            throw new CommandException("No module called \"" + name + "\".");
        } else {
            return module;
        }
    }

    public static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    public static void sendInfo(String message) {
        sendMessage(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }

    public static void sendSuccess(String message) {
        sendMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    public static void sendError(String message) {
        sendMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static void sendMessage(Component component) {
        ChatUtility.printPrefixed(component);
    }
}
