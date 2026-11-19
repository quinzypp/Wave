package dev.adapt.v3.mixin;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.MoveInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void adapt$moveInput(CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            Input input = this.keyPresses;
            float f = input.forward() == input.backward() ? 0.0F : (input.forward() ? 1.0F : -1.0F);
            float f1 = input.left() == input.right() ? 0.0F : (input.left() ? 1.0F : -1.0F);
            MoveInputEvent moveinputevent = EventManager.post(new MoveInputEvent(f, f1, input.jump()));
            boolean flag = moveinputevent.getForward() != f || moveinputevent.getSideways() != f1;
            if (flag || moveinputevent.isJump() != input.jump()) {
                this.keyPresses = new Input(
                    moveinputevent.getForward() > 0.0F,
                    moveinputevent.getForward() < 0.0F,
                    moveinputevent.getSideways() > 0.0F,
                    moveinputevent.getSideways() < 0.0F,
                    moveinputevent.isJump(),
                    input.shift(),
                    input.sprint()
                );
                if (flag) {
                    this.moveVector = new Vec2(moveinputevent.getSideways(), moveinputevent.getForward()).normalized();
                }
            }
        }
    }
}
