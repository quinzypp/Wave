package dev.adapt.v3.command.impl;

import dev.adapt.v3.chat.AdaptUsers;
import dev.adapt.v3.command.Command;
import dev.adapt.v3.command.CommandManager;
import java.util.List;
import java.util.concurrent.CompletionException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class OnlineCommand extends Command {
    public OnlineCommand() {
        super("online", "online", "Shows the Adapt users playing right now.", "users", "who");
    }

    @Override
    public void execute(String[] args) {
        CommandManager.sendInfo("Checking who's online...");
        AdaptUsers.fetchOnlineUsers().whenComplete((onlineUsers, error) -> Minecraft.getInstance().execute(() -> {
            if (error == null) {
                printOnlineUsers(onlineUsers);
            } else {
                Throwable throwable = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                CommandManager.sendError(throwable instanceof IllegalStateException ? throwable.getMessage() : "Couldn't reach adaptclient.com.");
            }
        }));
    }

    public static void printOnlineUsers(List<AdaptUsers.OnlineUser> onlineUsers) {
        if (onlineUsers.isEmpty()) {
            CommandManager.sendInfo("No Adapt users are online right now.");
        } else {
            CommandManager.sendMessage(
                Component.literal(onlineUsers.size() + (onlineUsers.size() == 1 ? " Adapt user" : " Adapt users") + " online:").withStyle(ChatFormatting.WHITE)
            );

            for (AdaptUsers.OnlineUser adaptusers$onlineuser : onlineUsers) {
                MutableComponent mutablecomponent = Component.literal(" • ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(adaptusers$onlineuser.name()).withStyle(ChatFormatting.WHITE));
                if (adaptusers$onlineuser.minecraft() != null) {
                    mutablecomponent.append(Component.literal(" (" + adaptusers$onlineuser.minecraft() + ")").withStyle(ChatFormatting.GRAY));
                }

                CommandManager.sendMessage(mutablecomponent);
            }
        }
    }
}
