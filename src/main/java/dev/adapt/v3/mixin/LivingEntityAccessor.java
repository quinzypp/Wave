package dev.adapt.v3.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("noJumpDelay")
    void adapt$setNoJumpDelay(int value2);

    @Invoker("getJumpPower")
    float adapt$getJumpPower();

    @Invoker("travelInAir")
    void adapt$travelInAir(Vec3 vec);
}
