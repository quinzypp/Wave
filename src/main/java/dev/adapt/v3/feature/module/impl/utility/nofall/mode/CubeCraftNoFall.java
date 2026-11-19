package dev.adapt.v3.feature.module.impl.utility.nofall.mode;

import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.utility.nofall.NoFallModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.util.PacketUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class CubeCraftNoFall extends ModuleMode<NoFallModule> {
    public static final Minecraft mc = Minecraft.getInstance();

    public CubeCraftNoFall(NoFallModule noFallModule) {
        super(noFallModule);
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        LocalPlayer localplayer = mc.player;
        if (localplayer != null && !localplayer.getAbilities().flying && !localplayer.getAbilities().mayfly) {
            if (this.module.getRemainingFallDistance() > localplayer.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)) {
                this.module.markFallDistanceHandled();
                PacketUtility.sendNoEvent(new Pos(localplayer.xo, localplayer.yo, localplayer.zo, true, localplayer.horizontalCollision));
                localplayer.setDeltaMovement(localplayer.getDeltaMovement().add(0.0, 0.1, 0.0));
            }
        }
    }

    @Override
    public Enum getMode() {
        return NoFallModule.Mode.CUBECRAFT;
    }
}
