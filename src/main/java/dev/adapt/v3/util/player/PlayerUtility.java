package dev.adapt.v3.util.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PlayerUtility {
    public static final Minecraft mc = Minecraft.getInstance();

    public static boolean canCrit() {
        return !mc.player.isInWater() && !mc.player.onClimbable() && !mc.player.hasEffect(MobEffects.BLINDNESS) && !mc.player.isPassenger();
    }

    public static Vec3 clampVecToBox(Vec3 vec, AABB box) {
        double d0 = Math.max(box.minX, Math.min(vec.x, box.maxX));
        double d1 = Math.max(box.minY, Math.min(vec.y, box.maxY));
        double d2 = Math.max(box.minZ, Math.min(vec.z, box.maxZ));
        return new Vec3(d0, d1, d2);
    }

    public static Vec3 getClosestPointOnEntity(Vec3 vec, LivingEntity living) {
        return clampVecToBox(vec, living.getBoundingBox().inflate(living.getPickRadius()));
    }

    public static double getDistanceToEntityBox(LivingEntity living) {
        Vec3 vec3 = mc.player.getEyePosition();
        return vec3.distanceTo(clampVecToBox(vec3, living.getBoundingBox().inflate(living.getPickRadius())));
    }

    public static Block getTargetedBlock() {
        if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)mc.hitResult).getBlockPos();
            return mc.level.getBlockState(blockpos).getBlock();
        } else {
            return null;
        }
    }

    public static boolean isBoxClearOfBlocks(AABB box) {
        return BlockPos.betweenClosedStream(box).noneMatch(pos -> {
            BlockState blockstate = mc.level.getBlockState(pos);
            VoxelShape voxelshape = blockstate.getCollisionShape(mc.level, pos);
            return !voxelshape.isEmpty() && Shapes.joinIsNotEmpty(voxelshape.move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(box), BooleanOp.AND);
        });
    }

    public static boolean isPhysicalKeyDown(KeyMapping keyMapping) {
        return InputConstants.isKeyDown(keyMapping.getDefaultKey().getValue());
    }

    public static boolean isSameTeam(LivingEntity living, LivingEntity living2) {
        return living.getDisplayName() != null && living2.getDisplayName() != null ? living.getTeamColor() == living2.getTeamColor() : false;
    }

    public static double getItemAttackDamage(ItemStack stack) {
        double d0 = 0.0;
        ItemAttributeModifiers itemattributemodifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        for (Entry entry : itemattributemodifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) && entry.slot() == EquipmentSlotGroup.MAINHAND) {
                d0 += entry.modifier().amount();
            }
        }

        return d0;
    }
}
