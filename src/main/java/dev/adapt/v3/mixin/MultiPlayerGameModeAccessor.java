package dev.adapt.v3.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("carriedIndex")
    int adapt$getCarriedIndex();

    @Invoker("startPrediction")
    void adapt$startPrediction(ClientLevel clientLevel, PredictiveAction predictiveAction);

    @Invoker("ensureHasSentCarriedItem")
    void adapt$ensureHasSentCarriedItem();
}
