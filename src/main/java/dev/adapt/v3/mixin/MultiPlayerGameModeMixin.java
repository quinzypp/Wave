package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.AttackEvent;
import dev.adapt.v3.feature.helper.slot.SlotHelper;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void adapt$tickSlotHelper(CallbackInfo ci) {
        SlotHelper.getInstance().tick();
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void adapt$attack(Player player, Entity target, CallbackInfo ci) {
        EventManager.post(new AttackEvent(target));
    }
}
