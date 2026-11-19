package dev.adapt.v3.event.impl;

import net.minecraft.world.phys.BlockHitResult;

public final class BlockPlaceEvent {
    public final BlockHitResult hitResult;

    public BlockPlaceEvent(BlockHitResult blockHitResult) {
        this.hitResult = blockHitResult;
    }

    public BlockHitResult getHitResult() {
        return this.hitResult;
    }
}
