package dev.adapt.v3.feature.helper.target.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public abstract class EntityTarget {
    public LivingEntity entity;
    public boolean marked;

    public EntityTarget(LivingEntity living) {
        this.entity = living;
    }

    public void tick() {
    }

    public abstract boolean matches(int value);

    public LivingEntity getEntity() {
        return this.entity;
    }

    public void setEntity(Entity entity) {
        this.entity = (LivingEntity)entity;
    }

    public int getEntityId() {
        return this.entity.getId();
    }

    public boolean isLocalPlayer() {
        return false;
    }

    public boolean isMarked() {
        return this.marked;
    }

    public void mark() {
        this.marked = true;
    }
}
