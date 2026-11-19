package dev.adapt.v3.feature.module.impl.utility.disabler.mode;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.JoinWorldEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.impl.SendPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.utility.disabler.DisablerModule;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.mixin.ServerboundMovePlayerPacketAccessor;
import dev.adapt.v3.util.PacketUtility;
import dev.adapt.v3.util.RandomUtility;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CubeCraftDisabler extends ModuleMode<DisablerModule> {
    public static final Minecraft mc = Minecraft.getInstance();
    public final Queue<PacketUtility.TimedPacket> keepAliveQueue = new ConcurrentLinkedQueue<>();
    public int pongThreshold;
    public int pongStage;
    public int keepAliveDelayMs;
    public int lastPongId = -1;
    public boolean highPingDetected;

    public CubeCraftDisabler(DisablerModule disablerModule) {
        super(disablerModule);
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        Packet packet = event.getPacket();
        if (packet instanceof ClientboundKeepAlivePacket clientboundkeepalivepacket) {
            if (mc.player == null || mc.player.isDeadOrDying()) {
                return;
            }

            long i = System.currentTimeMillis() + this.keepAliveDelayMs;
            this.keepAliveQueue.add(new PacketUtility.TimedPacket(new ServerboundKeepAlivePacket(clientboundkeepalivepacket.getId()), i));
            event.cancel();
        } else if (packet instanceof ClientboundPlayerPositionPacket
            || packet instanceof ClientboundSetHealthPacket clientboundsethealthpacket && clientboundsethealthpacket.getHealth() <= 0.0F) {
            this.flushAllKeepAlives();
        }
    }

    @Subscribe
    public void onSendPacket(SendPacketEvent event) {
        Packet packet = event.getPacket();
        if (packet instanceof ServerboundKeepAlivePacket) {
            event.cancel();
        }

        if (mc.player != null && mc.player.tickCount > 20) {
            if (packet instanceof ServerboundMovePlayerPacket serverboundmoveplayerpacket) {
                ScaffoldModule scaffoldmodule = Adapt.getInstance().getModuleManager().getByClass(ScaffoldModule.class);
                if (mc.gui.screen() == null && !mc.player.isUsingItem() && (scaffoldmodule == null || !scaffoldmodule.isEnabled())) {
                    BlockPos blockpos = mc.player.blockPosition().below();
                    InteractionHand interactionhand = mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;
                    BlockHitResult blockhitresult = new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.UP, blockpos, false);
                    PacketUtility.sendPredicted(value -> new ServerboundUseItemOnPacket(interactionhand, blockhitresult, value));
                }

                double d0 = serverboundmoveplayerpacket.getY(mc.player.getY());
                d0 -= d0 % 0.015625;
                ((ServerboundMovePlayerPacketAccessor)serverboundmoveplayerpacket).adapt$setY(d0);
            } else if (packet instanceof ServerboundPongPacket serverboundpongpacket) {
                this.lastPongId = serverboundpongpacket.getId();
                if (this.pongStage == 1) {
                    this.pongStage++;
                }

                if (mc.level == null) {
                    return;
                }

                if (this.lastPongId >= 0 || this.pongStage < 3 && this.pongStage != 0) {
                    if (this.lastPongId != 0) {
                        if (this.pongStage >= this.pongThreshold || this.pongStage == 2) {
                            PacketUtility.sendNoEvent(new ServerboundPongPacket(this.lastPongId + 3));
                            this.pongStage = 3;
                        }

                        event.cancel();
                    }
                } else {
                    this.pongStage++;
                    PacketUtility.sendNoEvent(new ServerboundPongPacket(this.lastPongId + 1));
                }
            }
        }
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        if (mc.player != null && mc.getConnection() != null) {
            if (!this.highPingDetected) {
                PlayerInfo playerinfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (playerinfo != null && playerinfo.getLatency() > 500) {
                    this.highPingDetected = true;
                }
            }

            this.flushDueKeepAlives();
        }
    }

    @Subscribe
    public void onWorldChange(JoinWorldEvent event) {
        this.highPingDetected = false;
    }

    public void flushAllKeepAlives() {
        PacketUtility.TimedPacket packetutility$timedpacket;
        while ((packetutility$timedpacket = this.keepAliveQueue.poll()) != null) {
            PacketUtility.sendNoEvent(packetutility$timedpacket.packet());
        }
    }

    public void flushDueKeepAlives() {
        long i = System.currentTimeMillis();
        Iterator iterator = this.keepAliveQueue.iterator();

        while (iterator.hasNext()) {
            PacketUtility.TimedPacket packetutility$timedpacket = (PacketUtility.TimedPacket)iterator.next();
            if (i >= packetutility$timedpacket.timestamp()) {
                PacketUtility.sendNoEvent(packetutility$timedpacket.packet());
                iterator.remove();
            }
        }
    }

    @Override
    public void onEnable() {
        this.pongThreshold = RandomUtility.nextInt(4900, 5100);
        this.keepAliveDelayMs = RandomUtility.nextInt(4900, 5100);
        this.highPingDetected = false;
        this.pongStage = 0;
        this.lastPongId = -1;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (this.isListening()) {
            super.onDisable();
            this.highPingDetected = false;
            if (mc.player == null) {
                this.keepAliveQueue.clear();
            } else {
                this.flushAllKeepAlives();
                if (this.lastPongId != -1) {
                    PacketUtility.sendNoEvent(new ServerboundPongPacket(this.lastPongId));
                }
            }
        }
    }

    @Override
    public Enum getMode() {
        return DisablerModule.Mode.CUBECRAFT;
    }
}
