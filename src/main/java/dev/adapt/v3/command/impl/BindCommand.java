package dev.adapt.v3.command.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.binding.Bind;
import dev.adapt.v3.binding.BindRegistry;
import dev.adapt.v3.binding.BindingManager;
import dev.adapt.v3.binding.type.InputType;
import dev.adapt.v3.command.Command;
import dev.adapt.v3.command.CommandException;
import dev.adapt.v3.command.CommandManager;
import dev.adapt.v3.feature.module.Module;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BindCommand extends Command {
    public static final Map<String, String> KEY_ALIASES = Map.ofEntries(
        Map.entry("rshift", "right.shift"),
        Map.entry("lshift", "left.shift"),
        Map.entry("shift", "left.shift"),
        Map.entry("rctrl", "right.control"),
        Map.entry("lctrl", "left.control"),
        Map.entry("ctrl", "left.control"),
        Map.entry("rcontrol", "right.control"),
        Map.entry("lcontrol", "left.control"),
        Map.entry("control", "left.control"),
        Map.entry("ralt", "right.alt"),
        Map.entry("lalt", "left.alt"),
        Map.entry("alt", "left.alt"),
        Map.entry("esc", "escape"),
        Map.entry("del", "delete"),
        Map.entry("ins", "insert"),
        Map.entry("pageup", "page.up"),
        Map.entry("pgup", "page.up"),
        Map.entry("pagedown", "page.down"),
        Map.entry("pgdn", "page.down"),
        Map.entry("capslock", "caps.lock"),
        Map.entry("caps", "caps.lock"),
        Map.entry("return", "enter"),
        Map.entry("up", "up"),
        Map.entry("down", "down"),
        Map.entry("left", "left"),
        Map.entry("right", "right")
    );

    public BindCommand() {
        super("bind", "bind <module> <key|none> | bind list", "Binds a module to a key or mouse button.", "b");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) {
            throw this.usageError();
        }

        BindRegistry bindregistry = Adapt.getInstance().getBindingManager().getRegistry();
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            boolean flag = false;

            for (Module module1 : Adapt.getInstance().getModuleManager().getAll()) {
                Optional optional = bindregistry.getBind(module1);
                if (optional.isPresent()) {
                    CommandManager.sendInfo(module1.getName() + ": " + BindingManager.formatBind((Bind)optional.get()));
                    flag = true;
                }
            }

            if (!flag) {
                CommandManager.sendInfo("Nothing is bound.");
            }
        } else {
            if (args.length < 2) {
                throw this.usageError();
            }

            String s = args[args.length - 1];
            Module module = CommandManager.requireModule(String.join(" ", Arrays.copyOf(args, args.length - 1)));
            if (!s.equalsIgnoreCase("none") && !s.equalsIgnoreCase("clear") && !s.equalsIgnoreCase("unbind")) {
                Bind bind = parseBind(s);
                if (bind == null) {
                    throw new CommandException("Unknown key \"" + s + "\".");
                }

                bindregistry.unbind(module);
                bindregistry.bind(bind.code(), module, bind.type());
                Adapt.getInstance().getConfigManager().save();
                CommandManager.sendSuccess("Bound " + module.getName() + " to " + BindingManager.formatBind(bind) + ".");
            } else {
                bindregistry.unbind(module);
                Adapt.getInstance().getConfigManager().save();
                CommandManager.sendSuccess("Unbound " + module.getName() + ".");
            }
        }
    }

    public static Bind parseBind(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        switch (s) {
            case "lmb":
                return new Bind(1, InputType.MOUSE);
            case "rmb":
                return new Bind(3, InputType.MOUSE);
            case "mmb":
                return new Bind(2, InputType.MOUSE);
            default:
                for (String s1 : new String[]{"mouse", "mb", "button"}) {
                    if (s.startsWith(s1)) {
                        try {
                            int i = Integer.parseInt(s.substring(s1.length()));
                            if (i >= 1 && i <= 8) {
                                return new Bind(i - 1, InputType.MOUSE);
                            }
                        } catch (NumberFormatException numberformatexception) {
                        }
                    }
                }

                String s2 = KEY_ALIASES.getOrDefault(s, s);

                try {
                    Key key = InputConstants.getKey("key.keyboard." + s2);
                    return key != null && key != InputConstants.UNKNOWN && key.getType() == Type.KEYBOARD ? new Bind(key.getValue(), InputType.KEYBOARD) : null;
                } catch (Exception exception) {
                    return null;
                }
        }
    }
}
