package dev.adapt.v3.mixin;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.ui.screen.AdaptMainMenuScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen adapt$replaceTitle(Screen screen) {
        return screen instanceof TitleScreen && Adapt.getInstance() != null ? new AdaptMainMenuScreen() : screen;
    }
}
