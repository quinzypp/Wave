package dev.adapt.v3.feature.module.impl.movement.speed.mode;

import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.movement.speed.SpeedModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.Minecraft;

public final class LegitSpeed extends ModuleMode<SpeedModule> {
    public static final Minecraft mc = Minecraft.getInstance();

    public LegitSpeed(SpeedModule speedModule) {
        super(speedModule);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        if (mc.player != null && MoveUtility.isMoving()) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }

    @Override
    public Enum getMode() {
        return SpeedModule.Mode.LEGIT;
    }
}
