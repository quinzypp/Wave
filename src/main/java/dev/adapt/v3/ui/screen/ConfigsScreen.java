package dev.adapt.v3.ui.screen;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.config.ConfigManager;
import dev.adapt.v3.notification.NotificationType;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class ConfigsScreen extends BaseScreen {
    public static final float PANEL_WIDTH = 280.0F;
    public static final float PANEL_PAD = 12.0F;
    public static final float PANEL_RADIUS = 10.0F;
    public static final float HEADER_HEIGHT = 34.0F;
    public static final float INPUT_HEIGHT = 22.0F;
    public static final float ROW_HEIGHT = 36.0F;
    public static final float ROW_GAP = 5.0F;
    public static final float ROW_RADIUS = 7.0F;
    public static final float ROW_STEP = 41.0F;
    public static final int MAX_VISIBLE_ROWS = 5;
    public static final int SAVE_ACCENT_ALPHA = 71;
    public static final int DANGER_COLOR = -1027478;
    public static final int DANGER_TEXT_COLOR = -685413;
    public static final float CTX_WIDTH = 128.0F;
    public static final float CTX_ROW_HEIGHT = 19.0F;
    public static final float CTX_PAD = 4.0F;
    public final ConfigManager configManager = Adapt.getInstance().getConfigManager();
    public final Map<String, Spring> springs = new HashMap<>();
    public final Spring scrollSpring = new Spring(0.0F, 400.0F, 40.0F);
    public float scrollTarget;
    public List<String> configNames = List.of();
    public long lastRefreshAt;
    public final StringBuilder nameInput = new StringBuilder();
    public boolean inputFocused;
    public long inputLastEditAt;
    public final List<Long> inputCharTimes = new ArrayList<>();
    public final List<ConfigsScreen.DropChar> inputDroppedChars = new ArrayList<>();
    public final Spring inputScrollSpring = new Spring(0.0F, 400.0F, 40.0F);
    public final Spring inputCaretSpring = new Spring(0.0F, 700.0F, 53.0F);
    public boolean inputCaretInit;
    public ConfigsScreen.Rect inputRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Rect saveButtonRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Rect folderButtonRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Rect listViewportRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Rect panelRect = ConfigsScreen.Rect.NONE;
    public final List<ConfigsScreen.Rect> rowRects = new ArrayList<>();
    public final List<String> visibleConfigNames = new ArrayList<>();
    public String contextConfigName;
    public float contextMenuX;
    public float contextMenuY;
    public final Spring contextMenuSpring = new Spring(0.0F, 600.0F, 49.0F);
    public final List<ConfigsScreen.Rect> contextItemRects = new ArrayList<>();
    public ConfigsScreen.Rect contextMenuRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Confirm activeConfirm = ConfigsScreen.Confirm.NONE;
    public ConfigsScreen.Confirm renderedConfirm = ConfigsScreen.Confirm.NONE;
    public String confirmTargetName;
    public final Spring confirmModalSpring = new Spring(0.0F, 340.0F, 37.0F);
    public ConfigsScreen.Rect confirmCancelRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Rect confirmAcceptRect = ConfigsScreen.Rect.NONE;
    public ConfigsScreen.Rect confirmCardRect = ConfigsScreen.Rect.NONE;
    public final Map metaCache = new HashMap();
    public final Map<String, Spring> savedFlashSprings = new HashMap<>();
    public final List<ConfigsScreen.Rect> rowTrashRects = new ArrayList<>();
    public static final float LOAD_SHIMMER_MS = 520.0F;
    public static final int LOAD_SHIMMER_COLOR = 704643071;
    public final Map<String, Long> loadedShimmerTimes = new HashMap<>();
    public static final String[] CTX_LABELS = new String[]{"Load", "Overwrite", "Delete"};
    public static final String[] CTX_ICONS = new String[]{"\ue5ca", "\ue161", "\ue872"};

    public ConfigsScreen(Screen screen) {
        super("Configs", screen);
        this.refreshConfigs();
    }

    @Override
    public boolean usesTextInput() {
        return true;
    }

    @Override
    public boolean isTyping() {
        return this.inputFocused;
    }

    public void refreshConfigs() {
        this.configNames = this.configManager.listConfigs();
        this.lastRefreshAt = System.currentTimeMillis();
    }

    public Spring getSpring(String text) {
        return this.springs.computeIfAbsent(text, text2 -> new Spring(0.0F, 600.0F, 49.0F));
    }

    public static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public static void withPoseTransform(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, Runnable action) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        if (value != 1.0F) {
            graphics.pose().translate(width, height);
            graphics.pose().scale(value, value);
            graphics.pose().translate(-width, -height);
        }

        action.run();
        graphics.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int value, int value2, float value3) {
        if (this.minecraft.level == null && !renderingParentBackdrop) {
            this.renderNebulaOrPanorama(graphics, value3);
        }

        graphics.nextStratum();
    }

    @Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, float alpha) {
        if (System.currentTimeMillis() - this.lastRefreshAt > 1000L) {
            this.refreshConfigs();
        }

        boolean flag = this.activeConfirm != ConfigsScreen.Confirm.NONE || this.contextConfigName != null;
        int i = flag ? -1000 : mouseX;
        int j = flag ? -1000 : mouseY;
        int k = this.configNames.size();
        float f = Math.min(Math.max(k, 2), 5) * 41.0F - 5.0F;
        float f1 = 70.0F + f + 12.0F - 2.0F;
        f1 = Math.min(f1, this.height - 60);
        float f2 = Render2D.snapToPixel(this.width / 2.0F - 140.0F);
        float f3 = Render2D.snapToPixel(this.height / 2.0F - f1 / 2.0F + (1.0F - alpha) * 10.0F);
        this.panelRect = new ConfigsScreen.Rect(f2, f3, 280.0F, f1);
        Glass.drawBorderlessPanel(graphics, f2, f3, 280.0F, f1, 10.0F, alpha, 0.55F, 1.0F, -1509159920);
        this.renderHeader(graphics, f2, f3, i, j, alpha, k);
        this.renderSaveBar(graphics, f2, f3 + 34.0F + 6.0F, i, j, alpha);
        this.renderConfigList(graphics, f2, f3 + 34.0F + 6.0F + 22.0F + 8.0F, Math.min(f, f1 - 70.0F - 12.0F + 4.0F), i, j, alpha);
        String s = "Click to load   ·   Right-click for more   ·   Esc to go back";
        float f4 = this.minecraft.level != null ? this.height - 62 : this.height - 16;
        Fonts.SMALL
            .drawCenteredX(
                graphics, "Click to load   ·   Right-click for more   ·   Esc to go back", this.width / 2.0F, f4, ColorUtility.scaleAlpha(1946157055, alpha)
            );
        this.renderContextMenu(graphics, mouseX, mouseY, alpha);
        this.renderConfirmModal(graphics, mouseX, mouseY, alpha);
    }

    public void renderHeader(GuiGraphicsExtractor graphics, float value, float value2, int value3, int value4, float value5, int value6) {
        float f = value2 + 17.0F;
        Fonts.ICONS.drawIconCentered(graphics, "\ue53b", value + 12.0F + 5.0F, f, 1.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value5));
        Fonts.SEMIBOLD.draw(graphics, "Configs", value + 12.0F + 15.0F, f - Fonts.SEMIBOLD.getHalfPointSize(), ColorUtility.scaleAlpha(-1, value5));
        Fonts.SMALL
            .draw(
                graphics,
                String.valueOf(value6),
                value + 12.0F + 20.0F + Fonts.SEMIBOLD.getWidth("Configs"),
                f - Fonts.SMALL.getHalfPointSize(),
                ColorUtility.scaleAlpha(1509949439, value5)
            );
        float f1 = 18.0F;
        this.folderButtonRect = new ConfigsScreen.Rect(value + 280.0F - 12.0F - 18.0F + 3.0F, f - 9.0F, 18.0F, 18.0F);
        float f2 = clamp01(this.getSpring("folder").target(this.folderButtonRect.contains(value3, value4) ? 1.0F : 0.0F).update());
        if (f2 > 0.01F) {
            Render2D.fillSubpixelRoundedRect(
                graphics, this.folderButtonRect.x(), this.folderButtonRect.y(), 18.0F, 18.0F, 5.0F, ColorUtility.scaleAlpha(536870911, value5 * f2)
            );
        }

        Fonts.ICONS
            .drawIconCentered(
                graphics, "\ue89e", this.folderButtonRect.x() + 9.0F, f, 0.85F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-2130706433, -1, f2), value5)
            );
        if (f2 > 0.02F) {
            String s = "Open folder";
            float f3 = Fonts.SMALL.getWidth("Open folder") + 12.0F;
            float f4 = this.folderButtonRect.x() + 9.0F - f3 / 2.0F;
            float f5 = this.folderButtonRect.y() - 18.0F;
            Glass.drawPanel(graphics, f4, f5, f3, 14.0F, 5.0F, value5 * f2, 0.12F, 0.35F);
            Fonts.SMALL.drawCentered(graphics, "Open folder", f4 + f3 / 2.0F, f5 + 7.0F, ColorUtility.scaleAlpha(-419430401, value5 * f2));
        }

        Render2D.fillRect(graphics, value + 12.0F + 2.0F, value2 + 34.0F - 1.0F, 252.0F, 0.5F, ColorUtility.scaleAlpha(452984831, value5));
    }

    public void renderSaveBar(GuiGraphicsExtractor graphics, float value, float value2, int value3, int value4, float value5) {
        String s = this.nameInput.toString();
        boolean flag = this.configManager.exists(s);
        String s1 = flag ? "Overwrite" : "Save";
        float f = Fonts.SMALL.getWidth(s1) + 28.0F;
        float f1 = value + 12.0F;
        float f2 = 256.0F - f - 6.0F;
        this.inputRect = new ConfigsScreen.Rect(f1, value2, f2, 22.0F);
        this.saveButtonRect = new ConfigsScreen.Rect(f1 + f2 + 6.0F, value2, f, 22.0F);
        float f3 = clamp01(this.getSpring("field").target(this.inputFocused ? 1.0F : 0.0F).update());
        float f4 = clamp01(this.getSpring("fieldHover").target(this.inputRect.contains(value3, value4) ? 1.0F : 0.0F).update());
        Render2D.fillSubpixelRoundedRect(
            graphics, f1, value2, f2, 22.0F, 6.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(184549375, 352321535, Math.max(f3, f4 * 0.5F)), value5)
        );
        Render2D.drawRoundedBorderTextured(
            graphics, f1, value2, f2, 22.0F, 6.0F, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(452984831, 788529151, f3), value5)
        );
        float f5 = value2 + 11.0F;
        float f6 = f1 + 8.0F;
        float f7 = f2 - 16.0F;
        long i = System.currentTimeMillis();
        float f8 = clamp01(this.getSpring("typed").target(s.isEmpty() ? 0.0F : 1.0F).update());
        if (f8 < 0.99F) {
            int j = ColorUtility.scaleAlpha(ColorUtility.lerpColor(1509949439, 1090519039, f3), value5 * (1.0F - f8));
            withPoseTransform(graphics, f3 * 4.0F, 0.0F, 0.0F, 0.0F, 1.0F, () -> Fonts.REGULAR.drawVCentered(graphics, "New config name", f6, f5, j));
        }

        float f15 = Fonts.REGULAR.getWidth(s);
        float f9 = this.inputScrollSpring.target(Math.max(0.0F, f15 - f7)).update();
        graphics.enableScissor((int)(f6 - 2.0F), (int)value2, (int)Math.ceil(f1 + f2 - 4.0F), (int)Math.ceil(value2 + 22.0F));

        while (this.inputCharTimes.size() > s.length()) {
            this.inputCharTimes.remove(this.inputCharTimes.size() - 1);
        }

        while (this.inputCharTimes.size() < s.length()) {
            this.inputCharTimes.add(0L);
        }

        for (int k = 0; k < s.length(); k++) {
            float f10 = clamp01((float)(i - this.inputCharTimes.get(k)) / 170.0F);
            float f11 = 1.0F - (1.0F - f10) * (1.0F - f10) * (1.0F - f10);
            String s2 = s.substring(k, k + 1);
            float f12 = f6 - f9 + Fonts.REGULAR.getWidth(s.substring(0, k));
            float f13 = Fonts.REGULAR.getWidth(s2);
            int l = ColorUtility.scaleAlpha(-1, value5 * f11);
            withPoseTransform(
                graphics, 0.0F, (1.0F - f11) * 4.0F, f12 + f13 / 2.0F, f5, 0.85F + 0.15F * f11, () -> Fonts.REGULAR.drawVCentered(graphics, s2, f12, f5, l)
            );
        }

        this.inputDroppedChars.removeIf(dropChar -> i - dropChar.at() > 150L);

        for (ConfigsScreen.DropChar configsscreen$dropchar : this.inputDroppedChars) {
            float f18 = clamp01((float)(i - configsscreen$dropchar.at()) / 150.0F);
            float f19 = f6 - f9 + configsscreen$dropchar.x();
            int j1 = ColorUtility.scaleAlpha(-1, value5 * (1.0F - f18));
            withPoseTransform(
                graphics,
                0.0F,
                f18 * 3.0F,
                f19 + Fonts.REGULAR.getWidth(configsscreen$dropchar.ch()) / 2.0F,
                f5,
                1.0F - 0.15F * f18,
                () -> Fonts.REGULAR.drawVCentered(graphics, configsscreen$dropchar.ch(), f19, f5, j1)
            );
        }

        graphics.disableScissor();
        float f16 = s.isEmpty() ? f6 - 1.0F : f6 - f9 + f15 + 1.0F;
        if (!this.inputCaretInit) {
            this.inputCaretSpring.snap(f16);
            this.inputCaretInit = true;
        }

        float f17 = this.inputCaretSpring.target(f16).update();
        if (f3 > 0.01F) {
            long i1 = i - this.inputLastEditAt;
            float f21 = i1 < 500L ? 1.0F : clamp01(0.5F + 1.4F * (float)Math.cos((i1 - 500L) / 1060.0 * Math.PI * 2.0));
            Render2D.fillRect(graphics, f17, f5 - 5.0F, 0.75F, 10.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value5 * f3 * f21));
        }

        boolean flag1 = !s.isEmpty();
        float f20 = clamp01(this.getSpring("save").target(flag1 && this.saveButtonRect.contains(value3, value4) ? 1.0F : 0.0F).update());
        float f22 = clamp01(this.getSpring("saveReady").target(flag1 ? 1.0F : 0.0F).update());
        Render2D.fillSubpixelRoundedRect(
            graphics, this.saveButtonRect.x(), value2, f, 22.0F, 6.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(184549375, 352321535, f20), value5)
        );
        if (f22 > 0.01F) {
            int k1 = 71 + (int)(40.0F * f20);
            if (Theme.hasGradient()) {
                Render2D.fillHorizontalRoundedGradient(
                    graphics,
                    this.saveButtonRect.x(),
                    value2,
                    f,
                    22.0F,
                    6.0F,
                    ColorUtility.withAlpha(Theme.sampleGradient(0.0F), k1),
                    ColorUtility.withAlpha(Theme.sampleGradient(1.0F), k1),
                    value5 * f22
                );
            } else {
                Render2D.fillSubpixelRoundedRect(
                    graphics, this.saveButtonRect.x(), value2, f, 22.0F, 6.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(k1), value5 * f22)
                );
            }
        }

        int l1 = ColorUtility.lerpColor(1509949439, -1, f22);
        float f23 = Fonts.SMALL.getWidth(s1) + 12.0F;
        float f14 = this.saveButtonRect.x() + f / 2.0F - f23 / 2.0F;
        Fonts.ICONS.drawIconCentered(graphics, "\ue161", f14 + 4.0F, f5, 0.75F, ColorUtility.scaleAlpha(l1, value5));
        Fonts.SMALL.drawVCentered(graphics, s1, f14 + 12.0F, f5, ColorUtility.scaleAlpha(l1, value5));
    }

    public ConfigsScreen.Meta getConfigMeta(String name) {
        long i = this.configManager.getLastModified(name);
        ConfigsScreen.Meta configsscreen$meta = (ConfigsScreen.Meta)this.metaCache.get(name);
        if (configsscreen$meta != null && configsscreen$meta.modified() == i) {
            return configsscreen$meta;
        }

        ConfigsScreen.Meta configsscreen$meta1 = new ConfigsScreen.Meta(i, this.configManager.countEnabledModules(name));
        this.metaCache.put(name, configsscreen$meta1);
        return configsscreen$meta1;
    }

    public Spring getSavedFlashSpring(String text) {
        return this.savedFlashSprings.computeIfAbsent(text, text2 -> new Spring(0.0F, 60.0F, 9.0F));
    }

    public void pulseSaved(String text) {
        this.getSavedFlashSpring(text).snap(1.0F);
    }

    public void triggerLoadShimmer(String text) {
        this.loadedShimmerTimes.put(text, System.currentTimeMillis());
    }

    public void renderConfigList(GuiGraphicsExtractor graphics, float value, float value2, float value3, int value4, int value5, float value6) {
        this.listViewportRect = new ConfigsScreen.Rect(value, value2, 280.0F, value3);
        this.rowRects.clear();
        this.visibleConfigNames.clear();
        this.rowTrashRects.clear();
        if (this.configNames.isEmpty()) {
            float f21 = value + 140.0F;
            float f22 = value2 + value3 / 2.0F;
            Render2D.fillCircleAligned(graphics, f21, f22 - 14.0F, 11.0F, ColorUtility.scaleAlpha(268435455, value6));
            Fonts.ICONS.drawIconCentered(graphics, "\ue53b", f21, f22 - 14.0F, 1.0F, ColorUtility.scaleAlpha(1728053247, value6));
            Fonts.SMALL.drawCentered(graphics, "No configs yet", f21, f22 + 6.0F, ColorUtility.scaleAlpha(-1275068417, value6));
            Fonts.SMALL.drawCentered(graphics, "Name one above and press Save", f21, f22 + 17.0F, ColorUtility.scaleAlpha(1509949439, value6));
        } else {
            float f = Math.max(0.0F, this.configNames.size() * 41.0F - 5.0F - value3);
            this.scrollTarget = Math.max(0.0F, Math.min(f, this.scrollTarget));
            float f1 = this.scrollSpring.target(this.scrollTarget).update();
            String s = this.configManager.getActiveConfig();
            boolean flag = value5 >= value2 && value5 < value2 + value3;
            graphics.enableScissor((int)value, (int)value2, (int)Math.ceil(value + 280.0F), (int)Math.ceil(value2 + value3));

            for (int i = 0; i < this.configNames.size(); i++) {
                String s1 = this.configNames.get(i);
                float f2 = clamp01(value6 * 1.6F - i * 0.08F);
                float f3 = value2 + i * 41.0F - f1 + (1.0F - f2) * 6.0F;
                if (!(f3 + 36.0F < value2) && !(f3 > value2 + value3)) {
                    float f4 = value6 * f2;
                    float f5 = value + 12.0F - 2.0F;
                    float f6 = 260.0F;
                    ConfigsScreen.Rect configsscreen$rect = new ConfigsScreen.Rect(f5, f3, 260.0F, 36.0F);
                    boolean flag1 = flag && configsscreen$rect.contains(value4, value5);
                    float f7 = clamp01(this.getSpring("row" + s1).target(flag1 ? 1.0F : 0.0F).update());
                    float f8 = clamp01(this.getSpring("on" + s1).target(s1.equals(s) ? 1.0F : 0.0F).update());
                    float f9 = clamp01(this.getSavedFlashSpring(s1).target(0.0F).update());
                    float f10 = f3 + 18.0F;
                    Render2D.fillSubpixelRoundedRect(
                        graphics, f5, f3, 260.0F, 36.0F, 7.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(150994943, 318767103, Math.max(f7, f9)), f4)
                    );
                    if (f8 > 0.01F) {
                        byte b0 = 53;
                        if (Theme.hasGradient()) {
                            Render2D.fillHorizontalRoundedGradient(
                                graphics,
                                f5,
                                f3,
                                260.0F,
                                36.0F,
                                7.0F,
                                ColorUtility.withAlpha(Theme.sampleGradient(0.0F), 53),
                                ColorUtility.withAlpha(Theme.sampleGradient(1.0F), 53),
                                f4 * f8
                            );
                        } else {
                            Render2D.fillSubpixelRoundedRect(graphics, f5, f3, 260.0F, 36.0F, 7.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(53), f4 * f8));
                        }
                    }

                    int j = ColorUtility.lerpColor(ColorUtility.lerpColor(452984831, 654311423, f7), Theme.getAccentWithAlpha(110), f8);
                    Render2D.drawRoundedBorderTextured(graphics, f5, f3, 260.0F, 36.0F, 7.0F, 1.0F, ColorUtility.scaleAlpha(j, f4));
                    Long olong = this.loadedShimmerTimes.get(s1);
                    if (olong != null) {
                        float f11 = (float)(System.currentTimeMillis() - olong) / 520.0F;
                        if (f11 >= 1.0F) {
                            this.loadedShimmerTimes.remove(s1);
                        } else {
                            float f12 = 1.0F - (1.0F - f11) * (1.0F - f11) * (1.0F - f11);
                            float f13 = Math.min(22.0F, 78.0F);
                            float f14 = f5 - f13 + (260.0F + f13 * 2.0F) * f12;
                            float f15 = Math.min(1.0F, Math.min(f11, 1.0F - f11) * 5.0F) * f4;
                            Render2D.fillClippedRoundedHorizontalHighlight(
                                graphics, f5, f3, 260.0F, 36.0F, 7.0F, f3, 36.0F, f14, f13, ColorUtility.scaleAlpha(704643071, f15)
                            );
                        }
                    }

                    float f25 = 22.0F;
                    float f26 = f5 + 7.0F;
                    float f27 = f10 - 11.0F;
                    if (f8 > 0.01F) {
                        Render2D.fillSubpixelRoundedRect(graphics, f26, f27, 22.0F, 22.0F, 6.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(150), f4 * f8));
                    }

                    Render2D.fillSubpixelRoundedRect(
                        graphics, f26, f27, 22.0F, 22.0F, 6.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(352321535, 536870911, f7), f4 * (1.0F - f8))
                    );
                    Fonts.MEDIUM
                        .drawCentered(
                            graphics,
                            s1.substring(0, 1).toUpperCase(Locale.ROOT),
                            f26 + 11.0F,
                            f10,
                            ColorUtility.scaleAlpha(ColorUtility.lerpColor(-855638017, -1, Math.max(f8, f7)), f4)
                        );
                    float f28 = f5 + 260.0F - 8.0F;
                    if (f8 > 0.01F) {
                        String s4 = "Active";
                        float f16 = Fonts.SMALL.getWidth("Active") + 20.0F;
                        float f17 = 14.0F;
                        float f18 = f28 - f16;
                        Render2D.fillSubpixelRoundedRect(
                            graphics, f18, f10 - 7.0F, f16, 14.0F, 7.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(70), f4 * f8)
                        );
                        Fonts.ICONS.drawIconCentered(graphics, "\ue5ca", f18 + 7.5F, f10, 0.6F, ColorUtility.scaleAlpha(-1, f4 * f8));
                        Fonts.SMALL.drawVCentered(graphics, "Active", f18 + 13.0F, f10, ColorUtility.scaleAlpha(-1, f4 * f8));
                        f28 = f18 - 6.0F;
                    }

                    float f29 = 18.0F;
                    ConfigsScreen.Rect configsscreen$rect1 = new ConfigsScreen.Rect(f28 - 18.0F, f10 - 9.0F, 18.0F, 18.0F);
                    float f30 = clamp01(this.getSpring("trash" + s1).target(flag1 && configsscreen$rect1.contains(value4, value5) ? 1.0F : 0.0F).update());
                    if (f7 > 0.01F) {
                        if (f30 > 0.01F) {
                            Render2D.fillSubpixelRoundedRect(
                                graphics,
                                configsscreen$rect1.x(),
                                configsscreen$rect1.y(),
                                18.0F,
                                18.0F,
                                5.0F,
                                ColorUtility.scaleAlpha(ColorUtility.withAlpha(-1027478, 46), f4 * f30)
                            );
                        }

                        Fonts.ICONS
                            .drawIconCentered(
                                graphics,
                                "\ue872",
                                configsscreen$rect1.x() + 9.0F,
                                f10,
                                0.75F,
                                ColorUtility.scaleAlpha(ColorUtility.lerpColor(-2130706433, -685413, f30), f4 * f7)
                            );
                    }

                    this.rowTrashRects.add(f7 > 0.5F ? configsscreen$rect1 : ConfigsScreen.Rect.NONE);
                    ConfigsScreen.Meta configsscreen$meta = this.getConfigMeta(s1);
                    float f19 = f26 + 22.0F + 9.0F;
                    float f20 = f28 - 18.0F - 6.0F - f19;
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(f7 * 1.5F, 0.0F);
                    Fonts.MEDIUM
                        .drawVCentered(
                            graphics,
                            Fonts.MEDIUM.ellipsize(s1, f20),
                            f19,
                            f10 - 5.5F,
                            ColorUtility.scaleAlpha(ColorUtility.lerpColor(-419430401, -1, Math.max(f8, f7)), f4)
                        );
                    String s2 = formatRelativeTime(configsscreen$meta.modified());
                    String s3 = configsscreen$meta.enabled()
                        + (configsscreen$meta.enabled() == 1 ? " module on" : " modules on")
                        + (s2.isEmpty() ? "" : "  ·  " + s2);
                    Fonts.SMALL.drawVCentered(graphics, Fonts.SMALL.ellipsize(s3, f20), f19, f10 + 6.0F, ColorUtility.scaleAlpha(1728053247, f4));
                    graphics.pose().popMatrix();
                    this.rowRects.add(configsscreen$rect);
                    this.visibleConfigNames.add(s1);
                }
            }

            graphics.disableScissor();
            if (f > 0.0F) {
                float f23 = value3 - 4.0F;
                float f24 = Math.max(14.0F, f23 * value3 / (this.configNames.size() * 41.0F));
                Render2D.fillRoundedRect(
                    graphics, value + 280.0F - 3.5F, value2 + 2.0F + (f23 - f24) * (f1 / f), 1.5F, f24, 0.75F, ColorUtility.scaleAlpha(872415231, value6)
                );
            }
        }
    }

    public void openContextMenu(String text, double first, double second) {
        this.contextConfigName = text;
        float f = CTX_LABELS.length * 19.0F + 8.0F;
        this.contextMenuX = (float)Math.min(first, this.width - 128.0F - 4.0F);
        this.contextMenuY = (float)Math.min(second, this.height - f - 4.0F);
        this.contextMenuSpring.snap(0.0F);
    }

    public void closeContextMenu() {
        this.contextConfigName = null;
        this.contextItemRects.clear();
        this.contextMenuRect = ConfigsScreen.Rect.NONE;
    }

    public void renderContextMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.contextItemRects.clear();
        if (this.contextConfigName != null) {
            float f = clamp01(this.contextMenuSpring.target(1.0F).update()) * partialTick;
            float f1 = CTX_LABELS.length * 19.0F + 8.0F;
            float f2 = this.contextMenuX;
            float f3 = this.contextMenuY + (1.0F - f) * -4.0F;
            this.contextMenuRect = new ConfigsScreen.Rect(f2, f3, 128.0F, f1);
            graphics.nextStratum();
            Glass.drawPanel(graphics, f2, f3, 128.0F, f1, 8.0F, f, 0.6F);

            for (int i = 0; i < CTX_LABELS.length; i++) {
                float f4 = f3 + 4.0F + i * 19.0F;
                ConfigsScreen.Rect configsscreen$rect = new ConfigsScreen.Rect(f2 + 4.0F, f4, 120.0F, 19.0F);
                this.contextItemRects.add(configsscreen$rect);
                boolean flag = i == 2;
                float f5 = clamp01(this.getSpring("ctx" + i).target(configsscreen$rect.contains(mouseX, mouseY) ? 1.0F : 0.0F).update());
                if (f5 > 0.01F) {
                    Render2D.fillSubpixelRoundedRect(
                        graphics,
                        configsscreen$rect.x(),
                        f4 + 1.0F,
                        configsscreen$rect.w(),
                        17.0F,
                        4.0F,
                        ColorUtility.scaleAlpha(flag ? ColorUtility.withAlpha(-1027478, 46) : 536870911, f * f5)
                    );
                }

                int j = flag ? ColorUtility.lerpColor(-685413, -1, f5 * 0.5F) : ColorUtility.lerpColor(-1493172225, -1, f5);
                float f6 = f4 + 9.5F;
                Fonts.ICONS.drawIconCentered(graphics, CTX_ICONS[i], configsscreen$rect.x() + 9.0F, f6, 0.75F, ColorUtility.scaleAlpha(j, f));
                Fonts.ROW.drawVCentered(graphics, CTX_LABELS[i], configsscreen$rect.x() + 19.0F, f6, ColorUtility.scaleAlpha(j, f));
            }
        }
    }

    public void executeContextAction(int value) {
        String s = this.contextConfigName;
        this.closeContextMenu();
        switch (value) {
            case 0:
                this.loadConfig(s);
                break;
            case 1:
                this.openConfirmModal(ConfigsScreen.Confirm.OVERWRITE, s);
                break;
            case 2:
                this.openConfirmModal(ConfigsScreen.Confirm.DELETE, s);
        }
    }

    public void openConfirmModal(ConfigsScreen.Confirm confirm, String text) {
        this.activeConfirm = confirm;
        this.confirmTargetName = text;
        this.inputFocused = false;
    }

    public void closeConfirmModal() {
        this.activeConfirm = ConfigsScreen.Confirm.NONE;
    }

    public void renderConfirmModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float f = clamp01(this.confirmModalSpring.target(this.activeConfirm != ConfigsScreen.Confirm.NONE ? 1.0F : 0.0F).update());
        if (this.activeConfirm != ConfigsScreen.Confirm.NONE) {
            this.renderedConfirm = this.activeConfirm;
        }

        if (f <= 0.01F) {
            this.renderedConfirm = ConfigsScreen.Confirm.NONE;
            this.confirmCancelRect = this.confirmAcceptRect = this.confirmCardRect = ConfigsScreen.Rect.NONE;
        } else {
            boolean flag = this.renderedConfirm == ConfigsScreen.Confirm.DELETE;
            graphics.nextStratum();
            Render2D.fillRect(graphics, 0.0F, 0.0F, this.width, this.height, ColorUtility.scaleAlpha(-1946157056, f * partialTick));
            graphics.nextStratum();
            float f1 = 236.0F;
            float f2 = 132.0F;
            float f3 = Render2D.snapToPixel(this.width / 2.0F - 118.0F);
            float f4 = Render2D.snapToPixel(this.height / 2.0F - 66.0F);
            this.confirmCardRect = new ConfigsScreen.Rect(f3, f4, 236.0F, 132.0F);
            float f5 = f * partialTick;
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0F, Render2D.snapToPixel((1.0F - f) * 12.0F));
            Glass.drawPanel(graphics, f3, f4, 236.0F, 132.0F, 12.0F, f5, 1.0F);
            float f6 = f3 + 118.0F;
            float f7 = f4 + 28.0F;
            int i = flag ? -1027478 : Theme.getAccentColor();
            Render2D.fillCircleAligned(graphics, f6, f7, 13.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(i, 56), f5));
            Fonts.ICONS_LG.drawIconCentered(graphics, flag ? "\ue872" : "\ue161", f6, f7, 1.0F, ColorUtility.scaleAlpha(i, f5));
            Fonts.TITLE.drawCentered(graphics, flag ? "Delete config?" : "Overwrite config?", f6, f7 + 26.0F, ColorUtility.scaleAlpha(-1, f5));
            String s = flag
                ? "\"" + this.confirmTargetName + "\" will be deleted from this device."
                : "\"" + this.confirmTargetName + "\" will be replaced with your current setup.";
            Fonts.SMALL.drawCentered(graphics, Fonts.SMALL.ellipsize(s, 212.0F), f6, f7 + 40.0F, ColorUtility.scaleAlpha(-1711276033, f5));
            float f8 = 100.0F;
            float f9 = f4 + 132.0F - 32.0F;
            this.confirmCancelRect = new ConfigsScreen.Rect(f3 + 14.0F, f9, 100.0F, 22.0F);
            this.confirmAcceptRect = new ConfigsScreen.Rect(f3 + 22.0F + 100.0F, f9, 100.0F, 22.0F);
            this.renderConfirmButton(graphics, this.confirmCancelRect, "Cancel", null, false, false, mouseX, mouseY, f5);
            this.renderConfirmButton(graphics, this.confirmAcceptRect, flag ? "Delete" : "Overwrite", flag ? "\ue872" : "\ue161", !flag, flag, mouseX, mouseY, f5);
            graphics.pose().popMatrix();
        }
    }

    public void renderConfirmButton(
        GuiGraphicsExtractor graphics, ConfigsScreen.Rect rect, String text, String text2, boolean flag, boolean flag2, int value, int value2, float value3
    ) {
        float f = clamp01(this.getSpring("btn" + text).target(rect.contains(value, value2) ? 1.0F : 0.0F).update());
        int i = flag ? Theme.getAccentWithAlpha(110 + (int)(50.0F * f)) : ColorUtility.lerpColor(352321535, 654311423, f);
        if (flag2) {
            i = ColorUtility.lerpColor(ColorUtility.withAlpha(-1027478, 40), ColorUtility.withAlpha(-1027478, 80), f);
        }

        Render2D.fillRoundedRect(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 7.0F, ColorUtility.scaleAlpha(i, value3));
        int j = flag ? -1 : (flag2 ? ColorUtility.lerpColor(-685413, -1, f) : ColorUtility.lerpColor(-855638017, -1, f));
        float f1 = Fonts.SMALL.getWidth(text) + (text2 != null ? 12 : 0);
        float f2 = rect.x() + rect.w() / 2.0F - f1 / 2.0F;
        if (text2 != null) {
            Fonts.ICONS.drawIconCentered(graphics, text2, f2 + 4.0F, rect.y() + rect.h() / 2.0F, 0.75F, ColorUtility.scaleAlpha(j, value3));
            f2 += 12.0F;
        }

        Fonts.SMALL.drawVCentered(graphics, text, f2, rect.y() + rect.h() / 2.0F, ColorUtility.scaleAlpha(j, value3));
    }

    public void confirmModalAction() {
        String s = this.confirmTargetName;
        ConfigsScreen.Confirm configsscreen$confirm = this.activeConfirm;
        this.closeConfirmModal();
        if (configsscreen$confirm == ConfigsScreen.Confirm.DELETE) {
            if (this.configManager.deleteConfig(s)) {
                notifyToast(NotificationType.INFO, "Config deleted", s);
            } else {
                notifyToast(NotificationType.ERROR, "Couldn't delete", s);
            }
        } else if (configsscreen$confirm == ConfigsScreen.Confirm.OVERWRITE) {
            this.saveConfig(s);
            if (s.equals(this.nameInput.toString())) {
                this.clearInput();
            }
        }

        this.refreshConfigs();
    }

    public void loadConfig(String name) {
        if (this.configManager.loadConfig(name)) {
            notifyToast(NotificationType.SUCCESS, "Config loaded", name);
            this.triggerLoadShimmer(name);
        } else {
            notifyToast(NotificationType.ERROR, "Couldn't load", name);
        }

        this.refreshConfigs();
    }

    public void saveConfig(String name) {
        if (this.configManager.saveConfig(name)) {
            notifyToast(NotificationType.SUCCESS, "Config saved", name);
            this.pulseSaved(name);
        } else {
            notifyToast(NotificationType.ERROR, "Couldn't save", name);
        }

        this.refreshConfigs();
    }

    public void submitSaveInput() {
        String s = this.nameInput.toString();
        if (!s.isEmpty()) {
            if (this.configManager.exists(s)) {
                this.openConfirmModal(ConfigsScreen.Confirm.OVERWRITE, s);
            } else {
                this.saveConfig(s);
                this.clearInput();
                this.inputFocused = false;
            }
        }
    }

    public static void notifyToast(NotificationType notificationType, String text, String text2) {
        Adapt.getInstance().getNotificationManager().builder(notificationType).title(text).message(text2).duration(1600).post();
    }

    public static String formatRelativeTime(long delayMs) {
        if (delayMs <= 0L) {
            return "";
        } else {
            long i = Math.max(0L, (System.currentTimeMillis() - delayMs) / 1000L);
            if (i < 60L) {
                return "just now";
            } else if (i < 3600L) {
                return i / 60L + "m ago";
            } else {
                return i < 86400L ? i / 3600L + "h ago" : i / 86400L + "d ago";
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double d0 = event.x();
        double d1 = event.y();
        int i = event.button();
        if (this.activeConfirm != ConfigsScreen.Confirm.NONE) {
            if (this.confirmAcceptRect.contains(d0, d1)) {
                this.confirmModalAction();
            } else if (this.confirmCancelRect.contains(d0, d1) || !this.confirmCardRect.contains(d0, d1)) {
                this.closeConfirmModal();
            }

            return true;
        } else {
            if (this.contextConfigName != null) {
                for (int j = 0; j < this.contextItemRects.size(); j++) {
                    if (this.contextItemRects.get(j).contains(d0, d1)) {
                        this.executeContextAction(j);
                        return true;
                    }
                }

                this.closeContextMenu();
                if (i != 3) {
                    return true;
                }
            }

            this.inputFocused = this.inputRect.contains(d0, d1);
            if (this.inputFocused) {
                this.inputLastEditAt = System.currentTimeMillis();
                return true;
            }

            if (this.saveButtonRect.contains(d0, d1)) {
                this.submitSaveInput();
                return true;
            }

            if (this.folderButtonRect.contains(d0, d1)) {
                if (!this.configManager.openConfigsFolder()) {
                    notifyToast(NotificationType.ERROR, "Couldn't open folder", this.configManager.getConfigsDir().toString());
                }

                return true;
            } else {
                if (this.listViewportRect.contains(d0, d1)) {
                    for (int k = 0; k < this.rowRects.size(); k++) {
                        if (this.rowRects.get(k).contains(d0, d1)) {
                            String s = this.visibleConfigNames.get(k);
                            if (i == 1 && this.rowTrashRects.get(k).contains(d0, d1)) {
                                this.openConfirmModal(ConfigsScreen.Confirm.DELETE, s);
                                return true;
                            }

                            if (i == 3) {
                                this.openContextMenu(s, d0, d1);
                            } else if (i == 1) {
                                this.loadConfig(s);
                            }

                            return true;
                        }
                    }
                }

                if (!this.panelRect.contains(d0, d1) && i == 1) {
                    this.onClose();
                }

                return true;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeConfirm == ConfigsScreen.Confirm.NONE && this.contextConfigName == null && this.listViewportRect.contains(mouseX, mouseY)) {
            this.scrollTarget -= (float)scrollY * 41.0F;
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.activeConfirm != ConfigsScreen.Confirm.NONE) {
            if (event.isEscape()) {
                this.closeConfirmModal();
            } else if (event.key() == 40 || event.key() == 88) {
                this.confirmModalAction();
            }

            return true;
        } else {
            if (this.contextConfigName != null && event.isEscape()) {
                this.closeContextMenu();
                return true;
            }

            if (!this.inputFocused) {
                return super.keyPressed(event);
            }

            if (event.isEscape()) {
                if (this.nameInput.length() > 0) {
                    this.clearInput();
                } else {
                    this.inputFocused = false;
                }
            } else if (event.key() == 42) {
                if (event.hasControlDown()) {
                    this.clearInput();
                } else {
                    this.backspaceInput();
                }
            } else if (event.key() == 40 || event.key() == 88) {
                this.submitSaveInput();
            } else if (event.isPaste()) {
                for (char c0 : this.minecraft.keyboardHandler.getClipboard().toCharArray()) {
                    this.appendInputChar(c0);
                }
            }

            this.inputLastEditAt = System.currentTimeMillis();
            return true;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.inputFocused && this.activeConfirm == ConfigsScreen.Confirm.NONE) {
            this.appendInputChar((char)event.codepoint());
            this.inputLastEditAt = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }

    public void appendInputChar(char ch) {
        if (this.nameInput.length() < 32) {
            char c0 = ch == ' ' ? '-' : ch;
            if (Character.isLetterOrDigit(c0) && c0 < 128 || c0 == '-' || c0 == '_' || c0 == '.' && this.nameInput.length() > 0) {
                this.nameInput.append(c0);
                this.inputCharTimes.add(System.currentTimeMillis());
            }
        }
    }

    public void backspaceInput() {
        if (this.nameInput.length() != 0) {
            int i = this.nameInput.length() - 1;
            this.inputDroppedChars
                .add(
                    new ConfigsScreen.DropChar(this.nameInput.substring(i), Fonts.REGULAR.getWidth(this.nameInput.substring(0, i)), System.currentTimeMillis())
                );
            this.nameInput.setLength(i);
            if (!this.inputCharTimes.isEmpty()) {
                this.inputCharTimes.remove(this.inputCharTimes.size() - 1);
            }
        }
    }

    public void clearInput() {
        long i = System.currentTimeMillis();

        for (int j = 0; j < this.nameInput.length(); j++) {
            this.inputDroppedChars
                .add(new ConfigsScreen.DropChar(this.nameInput.substring(j, j + 1), Fonts.REGULAR.getWidth(this.nameInput.substring(0, j)), i));
        }

        this.nameInput.setLength(0);
        this.inputCharTimes.clear();
    }

    private enum Confirm {
        NONE,
        OVERWRITE,
        DELETE;
    }

    private record DropChar(String ch, float x, long at) {
    }

    private record Meta(long modified, int enabled) {
    }

    private record Rect(float x, float y, float w, float h) {
        public static final ConfigsScreen.Rect NONE = new ConfigsScreen.Rect(0.0F, 0.0F, 0.0F, 0.0F);

        public boolean contains(double first, double second) {
            return first >= this.x && first < this.x + this.w && second >= this.y && second < this.y + this.h;
        }
    }
}
