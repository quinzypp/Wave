package dev.adapt.v3.feature.helper.mouse;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class MouseHelper {
    public final Map<KeyMapping, MouseButton> buttons = new HashMap<>();
    public final MouseButton attackButton;
    public final MouseButton useButton;
    public static MouseHelper instance;

    public MouseHelper() {
        Minecraft minecraft = Minecraft.getInstance();
        this.attackButton = this.register(new MouseButton(minecraft.options.keyAttack));
        this.useButton = this.register(new MouseButton(minecraft.options.keyUse));
    }

    public MouseButton register(MouseButton mouseButton) {
        this.buttons.put(mouseButton.getKeyMapping(), mouseButton);
        return mouseButton;
    }

    public void tick() {
        this.attackButton.tick();
        this.useButton.tick();
    }

    public static MouseButton getButton(KeyMapping keyMapping) {
        return getInstance().buttons.get(keyMapping);
    }

    public static MouseButton getAttackButton() {
        return getInstance().attackButton;
    }

    public static MouseButton getUseButton() {
        return getInstance().useButton;
    }

    public static MouseHelper getInstance() {
        if (instance == null) {
            instance = new MouseHelper();
        }

        return instance;
    }
}
