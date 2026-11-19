package dev.adapt.v3.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Mutable
    @Accessor("x")
    void adapt$setX(double value2);

    @Mutable
    @Accessor("y")
    void adapt$setY(double value2);

    @Mutable
    @Accessor("z")
    void adapt$setZ(double value2);

    @Accessor("x")
    double adapt$getX();

    @Accessor("y")
    double adapt$getY();

    @Accessor("z")
    double adapt$getZ();
}
