package dev.adapt.v3.mixin;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.impl.world.WorldEditorModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public abstract class ClientClockInstanceMixin {
    @Inject(method = "totalTicks", at = @At("RETURN"), cancellable = true)
    private void adapt$time(CallbackInfoReturnable<Long> cir) {
        WorldEditorModule worldeditormodule = editor();
        if (worldeditormodule != null && worldeditormodule.isTimeChangeActive()) {
            long i = cir.getReturnValue();
            cir.setReturnValue(Math.floorDiv(i, 24000L) * 24000L + worldeditormodule.getTimeTicks());
        }
    }

    @Inject(method = "partialTick", at = @At("RETURN"), cancellable = true)
    private void adapt$partial(CallbackInfoReturnable<Float> cir) {
        WorldEditorModule worldeditormodule = editor();
        if (worldeditormodule != null && worldeditormodule.isTimeChangeActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    private static WorldEditorModule editor() {
        return Adapt.getInstance() == null ? null : Adapt.getInstance().getModuleManager().getByClass(WorldEditorModule.class);
    }
}
