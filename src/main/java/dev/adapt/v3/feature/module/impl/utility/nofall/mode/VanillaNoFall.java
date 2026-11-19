package dev.adapt.v3.feature.module.impl.utility.nofall.mode;

import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.utility.nofall.NoFallModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.Minecraft;

public final class VanillaNoFall extends ModuleMode<NoFallModule> {
    public static final Minecraft mc = Minecraft.getInstance();

    public VanillaNoFall(NoFallModule noFallModule) {
        super(noFallModule);
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        if (mc.player != null && mc.player.fallDistance > 3.0 && MoveUtility.isBlockUnderToVoid()) {
            event.setOnGround(true);
        }
    }

    @Override
    public Enum getMode() {
        return NoFallModule.Mode.VANILLA;
    }
}
