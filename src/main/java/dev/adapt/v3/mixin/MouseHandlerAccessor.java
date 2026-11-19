package dev.adapt.v3.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("accumulatedDX")
    double adapt$getAccumulatedDX();

    @Accessor("accumulatedDY")
    double adapt$getAccumulatedDY();
}
