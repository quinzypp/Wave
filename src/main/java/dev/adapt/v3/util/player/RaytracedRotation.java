package dev.adapt.v3.util.player;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;

public record RaytracedRotation(Vec2 rotation, HitResult hitResult) {
}
