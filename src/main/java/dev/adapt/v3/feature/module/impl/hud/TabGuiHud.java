package dev.adapt.v3.feature.module.impl.hud;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.KeyEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TabGuiHud extends HudModule {
    public static final float BASE_ROW_HEIGHT = 17.0F;
    public static final float PANEL_GAP = 5.0F;
    public static final float CATEGORY_WIDTH = 98.0F;
    public static final float CORNER_RADIUS = 9.0F;
    public static final float ROW_HEIGHT = 17.0F + 10.0F / ModuleCategory.VALUES.length;
    public static final int SELECTION_FILL_COLOR = 234881023;
    public static final int SELECTION_SHADOW_COLOR = 268435456;
    public static final float LOWER_SHADOW_SIZE = 4.0F;
    public static final float UPPER_SHADOW_SIZE = 3.0F;
    public static final float TOGGLE_FLASH_MS = 520.0F;
    public static final int TOGGLE_FLASH_COLOR = 704643071;
    public final Map<Module, Long> toggleFlashTimes = new HashMap<>();
    public int selectedCategoryIndex;
    public int selectedModuleIndex;
    public boolean expanded;
    public final Spring categoryCursorSpring = createCursorSpring();
    public final Spring moduleCursorSpring = createCursorSpring();
    public final Spring expandSpring = new Spring(0.0F, 600.0F, 49.0F);
    public final Spring[] categoryHoverSprings = new Spring[ModuleCategory.VALUES.length];
    public final Map<Integer, Spring> moduleHoverSprings = new HashMap<>();
    public final Map<Module, Spring> moduleEnabledSprings = new HashMap<>();

    public TabGuiHud() {
        super("TabGUI", "Arrow-key menu for categories and modules.", 0.006F, 0.14F);

        for (int i = 0; i < this.categoryHoverSprings.length; i++) {
            this.categoryHoverSprings[i] = createItemSpring();
        }

        this.setEnabled(true);
    }

    @Override
    public float getBaseScale() {
        return 0.85F;
    }

    public static Spring createCursorSpring() {
        return new Spring(0.0F, 3600.0F, 120.0F);
    }

    public static Spring createItemSpring() {
        return new Spring(0.0F, 1100.0F, 66.3F);
    }

    public List<Module> getSelectedCategoryModules() {
        return Adapt.getInstance().getModuleManager().getByCategory(ModuleCategory.VALUES[this.selectedCategoryIndex]);
    }

    @Override
    public float getWidth() {
        return 98.0F;
    }

    @Override
    public float getHeight() {
        return ModuleCategory.VALUES.length * ROW_HEIGHT;
    }

    @Subscribe
    public void onKeyPress(KeyEvent event) {
        if (mc.gui.screen() == null) {
            int i = event.getCode();
            int j = ModuleCategory.VALUES.length;
            if (!this.expanded) {
                if (i == 82) {
                    this.selectedCategoryIndex = (this.selectedCategoryIndex - 1 + j) % j;
                } else if (i == 81) {
                    this.selectedCategoryIndex = (this.selectedCategoryIndex + 1) % j;
                } else if (i == 79 || i == 40) {
                    this.expanded = true;
                    this.selectedModuleIndex = 0;
                    this.moduleCursorSpring.snap(0.0F);
                    this.moduleHoverSprings.clear();
                }
            } else {
                int k = this.getSelectedCategoryModules().size();
                if (k == 0) {
                    this.expanded = false;
                    return;
                }

                if (i == 82) {
                    this.selectedModuleIndex = (this.selectedModuleIndex - 1 + k) % k;
                } else if (i == 81) {
                    this.selectedModuleIndex = (this.selectedModuleIndex + 1) % k;
                } else if (i == 80) {
                    this.expanded = false;
                } else if (i == 79 || i == 40) {
                    Module module = this.getSelectedCategoryModules().get(this.selectedModuleIndex);
                    if (!module.canToggle()) {
                        return;
                    }

                    module.toggle();
                    if (module.isEnabled()) {
                        this.toggleFlashTimes.put(module, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing) {
        ModuleCategory[] amodulecategory = ModuleCategory.VALUES;
        float f = this.getHeight();
        Glass.drawBorderlessPanel(graphics, 0.0F, 0.0F, 98.0F, f, 9.0F, 1.0F, 0.55F, 1.0F, -2146694128);
        float f1 = this.categoryCursorSpring.target(this.selectedCategoryIndex * ROW_HEIGHT).update();
        renderSelectionHighlight(graphics, 0.0F, 0.0F, 98.0F, f, f1, 1.0F);

        for (int i = 0; i < amodulecategory.length; i++) {
            float f2 = Math.max(0.0F, Math.min(1.0F, this.categoryHoverSprings[i].target(i == this.selectedCategoryIndex ? 1.0F : 0.0F).update()));
            float f3 = i * ROW_HEIGHT + ROW_HEIGHT / 2.0F;
            int j = Theme.hasGradient() ? Theme.getAnimatedWaveColor(6, i * 20) : Theme.getAccentColor();
            Fonts.ICONS.drawIconCentered(graphics, amodulecategory[i].getIcon(), 15.0F, f3, 0.85F, ColorUtility.lerpColor(-1711276033, j, f2));
            Fonts.REGULAR.drawVCentered(graphics, amodulecategory[i].getDisplayName(), 26.0F + f2 * 1.5F, f3, ColorUtility.lerpColor(-637534209, -1, f2));
        }

        float f11 = Math.max(0.0F, Math.min(1.0F, this.expandSpring.target(this.expanded ? 1.0F : 0.0F).update()));
        if (!(f11 < 0.02F)) {
            List<Module> list = this.getSelectedCategoryModules();
            float f12 = 70.0F;

            for (Module module : list) {
                f12 = Math.max(f12, Fonts.REGULAR.getWidth(module.getName()) + 34.0F);
            }

            float f13 = list.size() * ROW_HEIGHT;
            float f14 = 103.0F + (1.0F - f11) * -3.0F;
            float f4 = Math.max(0.0F, this.selectedCategoryIndex * ROW_HEIGHT);
            Glass.drawBorderlessPanel(graphics, f14, f4, f12, f13, 9.0F, f11, 0.55F, 1.0F, -2146694128);
            float f5 = this.moduleCursorSpring.target(this.selectedModuleIndex * ROW_HEIGHT).update();
            renderSelectionHighlight(graphics, f14, f4, f12, f13, f4 + f5, f11);
            long k = System.currentTimeMillis();

            for (int l = 0; l < list.size(); l++) {
                Module module1 = (Module)list.get(l);
                float f6 = f4 + l * ROW_HEIGHT;
                float f7 = f6 + ROW_HEIGHT / 2.0F;
                Long olong = this.toggleFlashTimes.get(module1);
                if (olong != null) {
                    float f8 = (float)(k - olong) / 520.0F;
                    if (f8 >= 1.0F) {
                        this.toggleFlashTimes.remove(module1);
                    } else {
                        renderToggleFlash(graphics, f14, f4, f12, f13, f6, f8, f11);
                    }
                }

                float f15 = Math.max(0.0F, Math.min(1.0F, this.moduleEnabledSprings.computeIfAbsent(module1, module2 -> {
                    Spring spring = createItemSpring();
                    spring.snap(module2.isEnabled() ? 1.0F : 0.0F);
                    return spring;
                }).target(module1.isEnabled() ? 1.0F : 0.0F).update()));
                float f9 = Math.max(
                    0.0F,
                    Math.min(
                        1.0F,
                        this.moduleHoverSprings.computeIfAbsent(l, value -> createItemSpring()).target(l == this.selectedModuleIndex ? 1.0F : 0.0F).update()
                    )
                );
                float f10 = f14 + 5.0F + 7.0F + f9 * 1.5F;
                int i1 = ColorUtility.lerpColor(-1929379841, -855638017, f9);
                if (f15 < 0.99F) {
                    Fonts.REGULAR.drawVCentered(graphics, module1.getName(), f10, f7, ColorUtility.scaleAlpha(i1, f11 * (1.0F - f15)));
                }

                if (f15 > 0.01F) {
                    Fonts.MEDIUM.drawVCentered(graphics, module1.getName(), f10, f7, ColorUtility.scaleAlpha(-1, f11 * f15));
                }
            }
        }
    }

    public static void renderSelectionHighlight(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        int i = ColorUtility.withAlpha(268435456, 0);
        Render2D.fillClippedRoundedVerticalGradient(graphics, x, y, width, height, 9.0F, first + ROW_HEIGHT, 4.0F, ColorUtility.scaleAlpha(268435456, second), i);
        Render2D.fillClippedRoundedVerticalGradient(graphics, x, y, width, height, 9.0F, first - 3.0F, 3.0F, i, ColorUtility.scaleAlpha(268435456, second * 0.5F));
        Render2D.fillClippedRoundedBand(graphics, x, y, width, height, 9.0F, first, ROW_HEIGHT, ColorUtility.scaleAlpha(234881023, second));
    }

    public static void renderToggleFlash(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, float value2, float value3) {
        float f = 1.0F - (1.0F - value2) * (1.0F - value2) * (1.0F - value2);
        float f1 = Math.min(22.0F, width * 0.3F);
        float f2 = x - f1 + (width + f1 * 2.0F) * f;
        float f3 = Math.min(1.0F, Math.min(value2, 1.0F - value2) * 5.0F) * value3;
        Render2D.fillClippedRoundedHorizontalHighlight(graphics, x, y, width, height, 9.0F, value, ROW_HEIGHT, f2, f1, ColorUtility.scaleAlpha(704643071, f3));
    }
}
