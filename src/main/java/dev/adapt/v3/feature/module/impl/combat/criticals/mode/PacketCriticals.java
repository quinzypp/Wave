package dev.adapt.v3.feature.module.impl.combat.criticals.mode;

import dev.adapt.v3.event.impl.AttackEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.combat.CriticalsModule;
import dev.adapt.v3.feature.module.impl.movement.flight.FlightModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.util.TimerUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public final class PacketCriticals extends ModuleMode<CriticalsModule> {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final double[] OFFSETS = new double[]{0.0625, 0.0, 1.1E-5, 0.0};
    public final TimerUtility delayTimer = new TimerUtility();

    public PacketCriticals(CriticalsModule criticalsModule) {
        super(criticalsModule);
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null
            && mc.getConnection() != null
            && localplayer.onGround()
            && !localplayer.isInWater()
            && !localplayer.isInLava()
            && !localplayer.onClimbable()
            && !localplayer.isPassenger()
            && !FlightModule.isFlightEnabled()
            && this.delayTimer.hasTimeElapsed(this.module.getDelay().getValue().longValue())) {
            if (!mc.hasSingleplayerServer()) {
                for (double d0 : OFFSETS) {
                    mc.getConnection().send(new Pos(localplayer.getX(), localplayer.getY() + d0, localplayer.getZ(), false, localplayer.horizontalCollision));
                }

                this.delayTimer.reset();
            }
        }
    }

    @Override
    public Enum getMode() {
        return CriticalsModule.Mode.PACKET;
    }
}
