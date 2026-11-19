package dev.adapt.v3.util.player;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RaytraceUtility {
    public static final Minecraft mc = Minecraft.getInstance();

    public static HitResult raytraceBlock(double value, float value2, boolean flag, float value3, float value4) {
        Vec3 vec3 = getInterpolatedEyePos(value2, mc.player);
        return raytraceBlockFromVec(value, flag, value3, value4, vec3);
    }

    public static HitResult raytraceBlockFromVec(double value, boolean flag, float value2, float value3, Vec3 vec) {
        Vec3 vec3 = RotationUtility.getLookVector(value3, value2);
        Vec3 vec31 = vec.add(vec3.x * value, vec3.y * value, vec3.z * value);
        return mc.level.clip(new ClipContext(vec, vec31, Block.OUTLINE, flag ? Fluid.ANY : Fluid.NONE, mc.player));
    }

    public static EntityHitResult raytraceEntity(double value, float value2, float value3, float value4, Predicate<Entity> predicate) {
        return raytraceEntityFromVec(value, getInterpolatedEyePos(value2, mc.player), value3, value4, predicate);
    }

    public static EntityHitResult raytraceEntityFromVec(double value, Vec3 vec, float value2, float value3, Predicate<Entity> predicate) {
        Vec3 vec3 = RotationUtility.getLookVector(value3, value2);
        Vec3 vec31 = vec.add(vec3.x * value, vec3.y * value, vec3.z * value);
        AABB aabb = mc.player.getBoundingBox().expandTowards(vec3.scale(value)).inflate(1.0, 1.0, 1.0);
        return ProjectileUtil.getEntityHitResult(mc.player, vec, vec31, aabb, predicate, Mth.square(value));
    }

    public static Vec3 getInterpolatedEyePos(float value, Entity entity) {
        double d0 = Mth.lerp(value, entity.xo, entity.getX());
        double d1 = Mth.lerp(value, entity.yo, entity.getY()) + entity.getEyeHeight();
        double d2 = Mth.lerp(value, entity.zo, entity.getZ());
        return new Vec3(d0, d1, d2);
    }
}
