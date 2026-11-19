package dev.adapt.v3.event.impl;

import net.minecraft.world.entity.Entity;

public final class AttackEvent {
    public final Entity target;

    public AttackEvent(Entity entity) {
        this.target = entity;
    }

    public Entity getTarget() {
        return this.target;
    }
}
