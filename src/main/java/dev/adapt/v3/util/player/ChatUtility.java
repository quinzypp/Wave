package dev.adapt.v3.util.player;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.HoverEvent.ShowText;

public final class ChatUtility {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final String CLIENT_PREFIX = "Adapt";

    public static MutableComponent createPrefix() {
        return Component.literal("[")
            .withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal("Adapt").withStyle(ChatFormatting.WHITE))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void printRaw(String text) {
        addClientMessage(Component.literal(text));
    }

    public static void addClientMessage(Component component) {
        if (mc.player != null) {
            mc.gui.hud.getChat().addClientSystemMessage(component);
        }
    }

    public static void print(String text) {
        printPrefixed(Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static void printPrefixed(Component component) {
        addClientMessage(createPrefix().append(component));
    }

    public static void printError(String message) {
        addClientMessage(
            Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("Error").withStyle(ChatFormatting.RED))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE))
        );
    }

    public static void sendChat(String message) {
        if (mc.player != null && mc.getConnection() != null) {
            mc.getConnection().sendChat(message);
        }
    }

    public static void sendCommand(String message) {
        if (mc.player != null && mc.getConnection() != null) {
            mc.getConnection().sendCommand(message.startsWith("/") ? message.substring(1) : message);
        }
    }

    public static void printWithHover(String text, Component component) {
        printPrefixed(Component.literal(text).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withHoverEvent(new ShowText(component))));
    }

    public static void printClickable(String text, Runnable action) {
        printPrefixed(Component.literal(text).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withClickEvent(new ChatUtility.RunnableClick(action))));
    }

    public static void printRawClickable(String text, Runnable action) {
        addClientMessage(Component.literal(text).withStyle(Style.EMPTY.withClickEvent(new ChatUtility.RunnableClick(action))));
    }

    public record RunnableClick(Runnable runnable) implements ClickEvent {
        @Override
        public Action action() {
            return Action.CUSTOM;
        }
    }
}
