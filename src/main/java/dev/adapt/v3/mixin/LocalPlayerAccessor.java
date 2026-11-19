package dev.adapt.v3.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor("wasSprinting")
    boolean adapt$getWasSprinting();

    @Accessor("yLast")
    void adapt$setYLast(double value2);

    @Accessor("xRotLast")
    void adapt$setXRotLast(float value2);
}
