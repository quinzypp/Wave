package dev.adapt.v3.feature.module.impl.movement.noslow.mode;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.PostMovementPacketEvent;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.SlowdownEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.combat.killaura.KillAuraModule;
import dev.adapt.v3.feature.module.impl.movement.noslow.NoSlowModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.Vec2;

public final class NcpNoSlow extends ModuleMode<NoSlowModule> {
    public static final Minecraft mc = Minecraft.getInstance();

    public NcpNoSlow(NoSlowModule noSlowModule) {
        super(noSlowModule);
    }

    @Subscribe
    public void onSlowdown(SlowdownEvent event) {
        event.cancel();
    }

    @Subscribe
    public void onPreMotion(PreMovementPacketEvent event) {
        if (this.shouldSendPacketUnblock()) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        }
    }

    @Subscribe
    public void onPostMotion(PostMovementPacketEvent event) {
        if (this.shouldSendPacketUnblock()) {
            mc.getConnection().send(new ServerboundUseItemPacket(mc.player.getUsedItemHand(), 0, mc.player.getYRot(), mc.player.getXRot()));
        }
    }

    public boolean shouldSendPacketUnblock() {
        return this.isBlockingWithItem() && isMovingUnobstructed() && hasGroundBelow(0.42) && !isKillAuraAttacking();
    }

    public boolean isBlockingWithItem() {
        return mc.player != null && mc.getConnection() != null && mc.player.isUsingItem() && mc.player.getUseItem().getUseAnimation() == ItemUseAnimation.BLOCK;
    }

    public static boolean isMovingUnobstructed() {
        if (!mc.player.horizontalCollision && !mc.player.isShiftKeyDown()) {
            Vec2 vec2 = mc.player.input.getMoveVector();
            return vec2.x != 0.0F || vec2.y != 0.0F;
        } else {
            return false;
        }
    }

    public static boolean hasGroundBelow(double value) {
        return !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, -value, 0.0));
    }

    public static boolean isKillAuraAttacking() {
        Adapt adapt = Adapt.getInstance();
        if (adapt == null) {
            return false;
        }

        KillAuraModule killauramodule = adapt.getModuleManager().getByClass(KillAuraModule.class);
        return killauramodule != null && killauramodule.isEnabled() && killauramodule.getTargetFinder().hasAttackTarget();
    }

    @Override
    public Enum getMode() {
        return NoSlowModule.Mode.NCP;
    }
}
