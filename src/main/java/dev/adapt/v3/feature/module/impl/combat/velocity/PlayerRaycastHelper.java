package dev.adapt.v3.feature.module.impl.combat.velocity;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.SendPacketEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class PlayerRaycastHelper implements Listener {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final PlayerRaycastHelper INSTANCE = new PlayerRaycastHelper();
    public static boolean serverOnGround;

    public PlayerRaycastHelper() {
        EventManager.register(this);
    }

    public static void init() {
    }

    @Subscribe
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket serverboundmoveplayerpacket) {
            serverOnGround = serverboundmoveplayerpacket.isOnGround();
        }
    }

    public static boolean isOnGround() {
        return mc.player != null && serverOnGround && mc.player.onGround();
    }

    public static Vec3 closestPointOnBox(Entity entity) {
        Vec3 vec3 = mc.player.getEyePosition();
        AABB aabb = entity.getBoundingBox();
        return new Vec3(Mth.clamp(vec3.x, aabb.minX, aabb.maxX), Mth.clamp(vec3.y, aabb.minY, aabb.maxY), Mth.clamp(vec3.z, aabb.minZ, aabb.maxZ));
    }

    public static double distanceToBox(Entity entity) {
        return mc.player.getEyePosition().distanceTo(closestPointOnBox(entity));
    }

    public static Vec2 getRotationsTo(Vec3 vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.y - mc.player.getEyePosition().y;
        double d2 = vec.z - mc.player.getZ();
        double d3 = Mth.sqrt((float)(d0 * d0 + d2 * d2));
        float f = (float)(Math.toDegrees(Math.atan2(d2, d0)) - 90.0);
        float f1 = (float)(-Math.toDegrees(Math.atan2(d1, d3)));
        return new Vec2(mc.player.getYRot() + Mth.wrapDegrees(f - mc.player.getYRot()), mc.player.getXRot() + Mth.wrapDegrees(f1 - mc.player.getXRot()));
    }

    public static EntityHitResult raycastEntity(float value, float value2, double value3) {
        Vec3 vec3 = mc.player.getEyePosition();
        float f = (float) (Math.PI / 180.0);
        float f1 = (float)Math.cos(-value * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = (float)Math.sin(-value * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f3 = (float)(-Math.cos(-value2 * (float) (Math.PI / 180.0)));
        float f4 = (float)Math.sin(-value2 * (float) (Math.PI / 180.0));
        Vec3 vec31 = new Vec3(f2 * f3, f4, f1 * f3);
        double d0 = value3 * value3;
        Vec3 vec32 = vec3.add(vec31.scale(value3));
        BlockHitResult blockhitresult = mc.level.clip(new ClipContext(vec3, vec32, Block.OUTLINE, Fluid.NONE, mc.player));
        double d1 = vec3.distanceToSqr(blockhitresult.getLocation());
        if (blockhitresult.getType() != Type.MISS) {
            d0 = d1;
            value3 = Math.sqrt(d0);
            vec32 = vec3.add(vec31.scale(value3));
        }

        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(
            mc.player,
            vec3,
            vec32,
            mc.player.getBoundingBox().expandTowards(vec31.scale(value3)).inflate(1.0),
            entity -> entity != mc.player && !entity.isSpectator() && entity.isPickable(),
            d0
        );
        return entityhitresult != null && entityhitresult.getLocation().distanceToSqr(vec3) < d1 ? entityhitresult : null;
    }
}
