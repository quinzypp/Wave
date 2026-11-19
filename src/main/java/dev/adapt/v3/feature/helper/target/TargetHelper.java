package dev.adapt.v3.feature.helper.target;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.target.impl.EntityTarget;
import dev.adapt.v3.feature.helper.target.impl.LivingTarget;
import dev.adapt.v3.feature.helper.target.impl.PlayerTarget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class TargetHelper implements Listener {
    public static final Minecraft MC = Minecraft.getInstance();
    public static TargetHelper instance;
    public final Map<Integer, EntityTarget> targets = new HashMap<>();

    public TargetHelper() {
        EventManager.register(this);
    }

    public static TargetHelper getInstance() {
        if (instance == null) {
            instance = new TargetHelper();
        }

        return instance;
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        this.update();
    }

    public void update() {
        if (MC.level != null && MC.player != null) {
            this.pruneInvalid();
            this.collectEntities();
        } else {
            this.targets.clear();
        }
    }

    public void pruneInvalid() {
        Iterator iterator = this.targets.values().iterator();

        while (iterator.hasNext()) {
            EntityTarget entitytarget = (EntityTarget)iterator.next();
            LivingEntity livingentity = entitytarget.getEntity();
            if (livingentity.isRemoved() || livingentity.level() != MC.level || !livingentity.isAlive() || livingentity.deathTime > 0) {
                iterator.remove();
            }
        }
    }

    public void collectEntities() {
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
                int i = entity.getId();
                EntityTarget entitytarget = this.getTarget(i, null);
                if (entitytarget == null || entitytarget.getEntity() != entity) {
                    entitytarget = this.createTarget(entity);
                    this.targets.put(i, entitytarget);
                }

                entitytarget.setEntity(entity);
            }
        }
    }

    public EntityTarget createTarget(Entity entity) {
        return switch (entity) {
            case Player player -> new PlayerTarget(player);
            case LivingEntity livingentity -> new LivingTarget(livingentity);
            default -> throw new IllegalStateException("Not a living entity: " + entity);
        };
    }

    public <T extends EntityTarget> T getTarget(int value, @Nullable Class<T> type) {
        EntityTarget entitytarget = this.targets.get(value);
        return (T)(type != null && !type.isInstance(entitytarget) ? null : entitytarget);
    }

    public boolean isTracked(int value) {
        return this.targets.containsKey(value);
    }

    public <T extends EntityTarget> List<T> getTargets(int value, @Nullable Class<T> type) {
        List<T> arraylist = new ArrayList<>();

        for (EntityTarget entitytarget : this.targets.values()) {
            if ((type == null || type.isAssignableFrom(entitytarget.getClass())) && entitytarget.matches(value)) {
                arraylist.add((T)entitytarget);
            }
        }

        return arraylist;
    }
}
