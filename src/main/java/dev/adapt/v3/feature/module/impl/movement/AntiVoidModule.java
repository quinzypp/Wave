package dev.adapt.v3.feature.module.impl.movement;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.movement.flight.FlightModule;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public final class AntiVoidModule extends Module {
    public final BooleanProperty voidOnly = new BooleanProperty("Void Only", true);
    public final NumberProperty distance = new NumberProperty("Distance", 5.0, 4.0, 10.0, 1.0);

    public AntiVoidModule() {
        super("AntiVoid", "Gets you set back when you fall into the void.", ModuleCategory.MOVEMENT);
        this.addProperties(this.voidOnly, this.distance);
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null && mc.level != null && mc.getConnection() != null) {
            FlightModule flightmodule = Adapt.getInstance().getModuleManager().getByClass(FlightModule.class);
            if (!(localplayer.fallDistance <= this.distance.getValue().intValue()) && (flightmodule == null || !flightmodule.isEnabled())) {
                if (!this.voidOnly.getValue() || !(localplayer.getY() >= mc.level.getMinY()) || !MoveUtility.isBlockUnderToVoid()) {
                    localplayer.fallDistance = 0.0;
                    mc.getConnection().send(new Pos(localplayer.getX(), localplayer.getY() + 12.0, localplayer.getZ(), false, localplayer.horizontalCollision));
                }
            }
        }
    }

    @Override
    public String getSuffix() {
        return "Hypixel";
    }
}
