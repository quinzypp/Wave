package dev.adapt.v3.feature.helper.mouse;

import dev.adapt.v3.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;

public final class MouseButton {
    public final KeyMapping keyMapping;
    public boolean pendingClick;
    public boolean suppressed;
    public int holdTicks;
    public boolean shouldSwing = true;

    public MouseButton(KeyMapping keyMapping) {
        this.keyMapping = keyMapping;
    }

    public void suppress() {
        this.pendingClick = false;
        this.holdTicks = 0;
        this.suppressed = true;
    }

    public void click() {
        this.press(true, 0);
    }

    public void press(boolean flag, int value) {
        this.pendingClick = flag;
        this.holdTicks = value;
    }

    public boolean consumeClick() {
        if (this.suppressed) {
            return false;
        }

        boolean flag = false;
        if (this.pendingClick) {
            flag = true;
            this.pendingClick = false;
        }

        return this.keyMapping.consumeClick() || flag;
    }

    public boolean isDown() {
        return this.suppressed ? false : this.keyMapping.isDown() || this.pendingClick || this.holdTicks > 0;
    }

    public boolean hasPendingClick() {
        return this.pendingClick;
    }

    public void tick() {
        if (this.holdTicks > 0) {
            this.holdTicks--;
        }

        if (this.keyMapping.isDown() || this.holdTicks == 0) {
            this.shouldSwing = true;
        }

        this.pendingClick = false;
        this.suppressed = false;
    }

    public int getHoldTicks() {
        return this.holdTicks;
    }

    public boolean isSuppressed() {
        return this.suppressed;
    }

    public boolean shouldSwing() {
        return this.shouldSwing || ((KeyMappingAccessor)this.keyMapping).adapt$getClickCount() > 0;
    }

    public void setShouldSwing(boolean shouldSwing) {
        this.shouldSwing = shouldSwing;
    }

    public KeyMapping getKeyMapping() {
        return this.keyMapping;
    }
}
