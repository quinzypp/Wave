package dev.adapt.v3.feature.helper.target.impl;

import dev.adapt.v3.util.player.PlayerUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Player;

public final class PlayerTarget extends LivingTarget {
    public static final Minecraft MC = Minecraft.getInstance();
    public boolean flagged;

    public PlayerTarget(Player player) {
        super(player);
    }

    @Override
    public boolean matches(int value) {
        if (this.isLocalPlayer()) {
            return true;
        }

        if ("BOT".equals(this.entity.getName().getString())) {
            ClientPacketListener clientpacketlistener = MC.getConnection();
            if (clientpacketlistener != null) {
                ServerData serverdata = clientpacketlistener.getServerData();
                if (serverdata != null && "localhost".equals(serverdata.ip)) {
                    return false;
                }
            }
        }

        return (value & 8) == 0 && PlayerUtility.isSameTeam(MC.player, this.entity) ? false : (value & 1) != 0;
    }

    public boolean isFlagged() {
        return this.flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    @Override
    public boolean isLocalPlayer() {
        return MC.player != null && this.getEntityId() == MC.player.getId();
    }
}
