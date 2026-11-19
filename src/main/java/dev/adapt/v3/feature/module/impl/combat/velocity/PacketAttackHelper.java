package dev.adapt.v3.feature.module.impl.combat.velocity;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

public final class PacketAttackHelper implements Listener {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final AtomicBoolean attackedThisTick = new AtomicBoolean();
    public static final PacketAttackHelper INSTANCE = new PacketAttackHelper();

    public PacketAttackHelper() {
        EventManager.register(this);
    }

    public static void init() {
    }

    @Subscribe(priority = 1000)
    public void onPreTick(PreTickEvent event) {
        attackedThisTick.set(false);
    }

    public static boolean claimAttackThisTick() {
        return attackedThisTick.compareAndSet(false, true);
    }

    public static boolean sendReduceAttack(PacketAttackHelper.SwingMode swingMode, Entity entity) {
        if (mc.player != null && mc.gameMode != null && entity != null && mc.gameMode.getPlayerMode() != GameType.SPECTATOR && claimAttackThisTick()) {
            mc.getConnection().send(new ServerboundAttackPacket(entity.getId()));
            if (swingMode == PacketAttackHelper.SwingMode.PACKET_ONLY) {
                mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
            } else {
                mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
                mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
            }

            mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            mc.player.setSprinting(false);
            return true;
        } else {
            return false;
        }
    }

    public enum SwingMode {
        PACKET_ONLY,
        PACKET_SWING;
    }
}
