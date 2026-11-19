package dev.adapt.v3.feature.module.impl.world.scaffold.mode;

import dev.adapt.v3.event.impl.BlockPlaceEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.impl.SprintEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldSettings;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;

public final class WatchdogScaffold extends ModuleMode<ScaffoldModule> {
    public static final Minecraft MC = Minecraft.getInstance();
    public static final int DEFAULT_PLACE_AIR_TICK = 1;
    public static final int TOWER_PLACE_AIR_TICK = 2;
    public int airTicks;
    public boolean waitingForSlotSync;
    public boolean placedThisTick;

    public WatchdogScaffold(ScaffoldModule scaffoldModule) {
        super(scaffoldModule);
    }

    @Subscribe(priority = 2)
    public void onStartTick(PreTickEvent event) {
        this.placedThisTick = false;
        if (MC.player == null) {
            this.airTicks = 0;
        } else {
            this.airTicks = MC.player.onGround() ? 0 : this.airTicks + 1;
        }
    }

    public void updateSlotSync(int slot) {
        int i = MC.gameMode == null ? slot : ((MultiPlayerGameModeAccessor)MC.gameMode).adapt$getCarriedIndex();
        this.waitingForSlotSync = slot != -1 && slot != i;
    }

    public boolean canRotate() {
        return this.isPastCancelAirTicks();
    }

    public boolean canPlace() {
        return this.waitingForSlotSync ? false : this.isPastCancelAirTicks() && this.airTicks >= this.getPlaceAirTick();
    }

    public boolean isPastCancelAirTicks() {
        return !MC.player.onGround() && this.airTicks > this.getCancelAirTicks();
    }

    public static boolean isTowering() {
        return MC.options.keyJump.isDown();
    }

    public int getCancelAirTicks() {
        return isTowering() ? 0 : this.module.getSettings().getCancelRotationAirTicks();
    }

    public int getPlaceAirTick() {
        return isTowering() ? 2 : this.module.getSettings().getPlaceAirTick();
    }

    @Subscribe
    public void onBlockPlaced(BlockPlaceEvent event) {
        if (MC.getConnection() != null && MC.player != null) {
            MC.getConnection().send(ServerboundPunchPacket.INSTANCE);
            this.placedThisTick = true;
            MC.player.setSprinting(false);
        }
    }

    @Subscribe
    public void onSprint(SprintEvent event) {
        if (this.placedThisTick) {
            event.setSprinting(false);
        }
    }

    @Subscribe(priority = -10)
    public void onPreMotion(PreMovementPacketEvent event) {
        if (this.placedThisTick) {
            MC.player.setSprinting(false);
            event.setSprinting(false);
        }
    }

    @Override
    public void onEnable() {
        this.airTicks = 0;
        this.waitingForSlotSync = false;
        this.placedThisTick = false;
        super.onEnable();
    }

    @Override
    public Enum getMode() {
        return ScaffoldSettings.Mode.WATCHDOG;
    }
}
