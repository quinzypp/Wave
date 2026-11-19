package dev.adapt.v3.feature.module.impl.movement.invmove.mode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.impl.movement.invmove.InvMoveModule;
import dev.adapt.v3.feature.module.impl.utility.SprintModule;
import dev.adapt.v3.feature.module.mode.ModuleMode;
import dev.adapt.v3.mixin.KeyMappingAccessor;
import dev.adapt.v3.ui.screen.BaseScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

public final class VanillaInvMove extends ModuleMode<InvMoveModule> {
    public static final Minecraft mc = Minecraft.getInstance();

    public VanillaInvMove(InvMoveModule invMoveModule) {
        super(invMoveModule);
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        if (mc.player != null && isAllowedScreen(mc.gui.screen())) {
            for (KeyMapping keymapping : new KeyMapping[]{
                mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump, mc.options.keySprint
            }) {
                Key key = ((KeyMappingAccessor)keymapping).adapt$getKey();
                if (!keymapping.isUnbound() && key.getType() != Type.MOUSE) {
                    if (keymapping == mc.options.keySprint && SprintModule.isSprintEnabled()) {
                        keymapping.setDown(true);
                    } else {
                        keymapping.setDown(InputConstants.isKeyDown(key.getValue()));
                    }
                }
            }
        }
    }

    public static boolean isAllowedScreen(Screen screen) {
        if (screen == null) {
            return false;
        } else if (!(screen instanceof ChatScreen)
            && !(screen instanceof AbstractSignEditScreen)
            && !(screen instanceof BookEditScreen)
            && !(screen instanceof AbstractCommandBlockEditScreen)) {
            return screen instanceof BaseScreen basescreen && basescreen.isTyping()
                ? false
                : !(screen.getFocused() instanceof EditBox editbox && editbox.isFocused());
        } else {
            return false;
        }
    }

    @Override
    public Enum getMode() {
        return InvMoveModule.Mode.VANILLA;
    }
}
