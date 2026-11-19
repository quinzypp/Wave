package dev.adapt.v3.event.impl;

import dev.adapt.v3.event.CancellableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlockAABBEvent extends CancellableEvent {
    public final BlockPos pos;
    public final BlockState state;
    public VoxelShape shape;

    public BlockAABBEvent(BlockPos pos, BlockState state, VoxelShape voxelShape) {
        this.pos = pos;
        this.state = state;
        this.shape = voxelShape;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockState getState() {
        return this.state;
    }

    public VoxelShape getShape() {
        return this.shape;
    }

    public void setShape(VoxelShape shape) {
        this.shape = shape;
    }

    public AABB getBoundingBox() {
        return this.shape.isEmpty() ? null : this.shape.bounds().move(this.pos);
    }
}
