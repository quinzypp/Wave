package dev.adapt.v3.ui.screen;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.ui.render.MenuNebula;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public abstract class BaseScreen extends Screen {
    public static boolean renderingParentBackdrop;
    public final Screen parent;
    public final Spring openSpring = new Spring(0.0F, 170.0F, 22.0F);
    public boolean closing;

    public BaseScreen(String text, Screen screen) {
        super(Component.literal(text));
        this.parent = screen;
        this.openSpring.target(1.0F);
    }

    public float getOpenProgress() {
        return Math.max(0.0F, Math.min(1.0F, this.openSpring.get()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean usesTextInput() {
        return false;
    }

    public boolean isTyping() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        if (this.parent != null && this.minecraft.level == null && (this.parent.width != this.width || this.parent.height != this.height)) {
            this.parent.init(this.width, this.height);
        }
    }

    @Override
    public void added() {
        super.added();
        this.closing = false;
        this.openSpring.snap(0.0F);
        this.openSpring.target(1.0F);
        if (this.usesTextInput()) {
            this.minecraft.textInputManager().startTextInput(this);
        }
    }

    @Override
    public void removed() {
        if (this.usesTextInput()) {
            this.minecraft.textInputManager().stopTextInput(this);
        }

        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int value, int value2, float value3) {
        float f = this.getOpenProgress();
        this.renderParentOrPanorama(graphics, value3);
        if (f > 0.05F && !renderingParentBackdrop) {
            graphics.blurBeforeThisStratum();
        }

        int i = Theme.getScreenScrim();
        Render2D.fillRect(graphics, 0.0F, 0.0F, this.width, this.height, ColorUtility.withAlpha(i, (int)Math.min(255.0F, ColorUtility.getAlpha(i) * f)));
    }

    public void renderNebulaOrPanorama(GuiGraphicsExtractor graphics, float value) {
        if (MenuNebula.isAvailable()) {
            MenuNebula.request();
        } else {
            this.extractPanorama(graphics, value);
        }
    }

    public void renderParentOrPanorama(GuiGraphicsExtractor graphics, float value) {
        if (this.minecraft.level == null && !renderingParentBackdrop) {
            if (this.parent instanceof BaseScreen) {
                renderingParentBackdrop = true;

                try {
                    this.parent.extractBackground(graphics, -1, -1, value);
                    this.parent.extractRenderState(graphics, -1, -1, value);
                } finally {
                    renderingParentBackdrop = false;
                }
            } else {
                this.extractPanorama(graphics, value);
            }

            graphics.nextStratum();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.openSpring.update();
        if (this.closing && this.openSpring.get() < 0.04F) {
            this.closing = false;
            this.finishClose();
        } else {
            this.renderContent(graphics, mouseX, mouseY, partialTick, this.getOpenProgress());
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    public abstract void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, float alpha);

    public void onUiFeedback(float value) {
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public void onClose() {
        if (!this.closing) {
            this.closing = true;
            this.openSpring.target(0.0F);
        }
    }

    public void finishClose() {
        Adapt.getInstance().getConfigManager().save();
        this.minecraft.gui.setScreen(this.parent);
    }

    public void openScreen(Screen screen) {
        Adapt.getInstance().getConfigManager().save();
        this.minecraft.gui.setScreen(screen);
    }
}
