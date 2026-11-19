package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.BlockAABBEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void adapt$blockAABB(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entitycollisioncontext && entitycollisioncontext.getEntity() != null) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.player != null && entitycollisioncontext.getEntity() == minecraft.player) {
                BlockAABBEvent blockaabbevent = EventManager.post(new BlockAABBEvent(pos.immutable(), (BlockState)(Object)this, cir.getReturnValue()));
                if (blockaabbevent.isCancelled()) {
                    cir.setReturnValue(Shapes.empty());
                } else if (blockaabbevent.getShape() != cir.getReturnValue()) {
                    cir.setReturnValue(blockaabbevent.getShape());
                }
            }
        }
    }
}
