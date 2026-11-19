package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.VelocityUpdateEvent;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Redirect(
        method = "handleSetEntityMotion",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private void adapt$velocityUpdate(Entity entity, Vec3 movement) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && entity == minecraft.player) {
            VelocityUpdateEvent velocityupdateevent = EventManager.post(new VelocityUpdateEvent(movement.x, movement.y, movement.z, false));
            if (!velocityupdateevent.isCancelled()) {
                entity.lerpMotion(new Vec3(velocityupdateevent.getX(), velocityupdateevent.getY(), velocityupdateevent.getZ()));
            }
        } else {
            entity.lerpMotion(movement);
        }
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void adapt$explosionVelocityUpdate(Optional<Vec3> knockback, Consumer<Vec3> consumer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            knockback.ifPresent(consumer);
        } else {
            knockback.ifPresent(push -> {
                Vec3 vec3 = minecraft.player.getDeltaMovement().add(push);
                VelocityUpdateEvent velocityupdateevent = EventManager.post(new VelocityUpdateEvent(vec3.x, vec3.y, vec3.z, true));
                if (!velocityupdateevent.isCancelled()) {
                    minecraft.player.setDeltaMovement(velocityupdateevent.getX(), velocityupdateevent.getY(), velocityupdateevent.getZ());
                }
            });
        }
    }
}
