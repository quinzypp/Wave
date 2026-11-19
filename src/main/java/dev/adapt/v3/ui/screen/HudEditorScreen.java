package dev.adapt.v3.ui.screen;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class HudEditorScreen extends BaseScreen {
    public static final float SNAP_THRESHOLD = 5.0F;
    public HudModule draggingModule;
    public float dragOffsetX;
    public float dragOffsetY;
    public final List<float[]> snapGuides = new ArrayList<>();
    public final Map<Object, Spring> springs = new HashMap<>();
    public final List<HudEditorScreen.DockChip> dockChips = new ArrayList<>();
    public static final float CHIP_HEIGHT = 15.0F;
    public static final float CHIP_RADIUS = 5.0F;
    public static final float CHIP_PAD_X = 9.0F;
    public static final float CHIP_GAP = 4.0F;
    public static final float DOCK_PAD = 5.0F;
    public static final float DOCK_RADIUS = 8.0F;
    public static final int CHIP_ACTIVE_ALPHA = 71;

    public HudEditorScreen(Screen screen) {
        super("HUD Editor", screen);
    }

    public Spring getSpring(Object key) {
        return this.springs.computeIfAbsent(key, key2 -> Spring.smooth(0.0F));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int value, int value2, float value3) {
        float f = this.getOpenProgress();
        this.renderParentOrPanorama(graphics, value3);
        if (this.minecraft.level == null) {
            graphics.blurBeforeThisStratum();
        }

        Render2D.fillRect(graphics, 0.0F, 0.0F, this.width, this.height, ColorUtility.withAlpha(657936, (int)(90.0F * f)));
        int i = ColorUtility.withAlpha(16777215, (int)(34.0F * f));

        for (int x = 16; x < this.width; x += 16) {
            for (int y = 16; y < this.height; y += 16) {
                Render2D.fillRect(graphics, x, y, 0.75F, 0.75F, i);
            }
        }
    }

    @Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, float alpha) {
        List<HudModule> list = Adapt.getInstance().getHudManager().getHudModules();
        Adapt.getInstance().getHudManager().renderModules(graphics, partialTick, true);

        for (HudModule hudmodule : list) {
            if (hudmodule.isEnabled()) {
                float f = hudmodule.resolveX(this.width);
                float f1 = hudmodule.resolveY(this.height);
                float f2 = hudmodule.getScaledWidth();
                float f3 = hudmodule.getScaledHeight();
                boolean flag = this.draggingModule == hudmodule || this.draggingModule == null && Render2D.containsPoint(mouseX, mouseY, f, f1, f2, f3);
                float f4 = clamp01(this.getSpring(hudmodule).target(flag ? 1.0F : 0.0F).update());
                if (f4 > 0.01F) {
                    Render2D.fillSubpixelRoundedRect(graphics, f - 3.0F, f1 - 3.0F, f2 + 6.0F, f3 + 6.0F, 5.0F, ColorUtility.scaleAlpha(268435455, alpha * f4));
                }

                Render2D.drawRoundedBorderTextured(
                    graphics,
                    f - 3.0F,
                    f1 - 3.0F,
                    f2 + 6.0F,
                    f3 + 6.0F,
                    5.0F,
                    1.0F,
                    ColorUtility.scaleAlpha(
                        ColorUtility.lerpColor(788529151, Theme.getAccentColor(), this.draggingModule == hudmodule ? 1.0F : f4 * 0.6F), alpha
                    )
                );
            }
        }

        graphics.nextStratum();

        for (HudModule hudmodule1 : list) {
            if (hudmodule1.isEnabled()) {
                float f8 = clamp01(this.getSpring(hudmodule1).get());
                if (!(f8 <= 0.02F)) {
                    float f9 = hudmodule1.resolveX(this.width);
                    float f10 = hudmodule1.resolveY(this.height);
                    float f11 = hudmodule1.getScaledHeight();
                    String s1 = hudmodule1.getName();
                    String s2 = Math.round(hudmodule1.scale.getFloatValue() * 100.0F) + "%";
                    float f5 = Fonts.SMALL.getWidth(s1) + Fonts.SMALL.getWidth(s2) + 22.0F;
                    float f6 = f10 - 19.0F < 2.0F ? f10 + f11 + 6.0F : f10 - 19.0F;
                    Glass.drawPanel(graphics, f9 - 3.0F, f6, f5, 14.0F, 5.0F, alpha * f8, 0.12F, 0.35F);
                    Fonts.SMALL.drawVCentered(graphics, s1, f9 + 4.0F, f6 + 7.0F, ColorUtility.scaleAlpha(-218103809, alpha * f8));
                    Fonts.SMALL.drawVCentered(graphics, s2, f9 + 4.0F + Fonts.SMALL.getWidth(s1) + 8.0F, f6 + 7.0F, ColorUtility.scaleAlpha(1946157055, alpha * f8));
                }
            }
        }

        for (float[] afloat : this.snapGuides) {
            int i = Theme.getAccentWithAlpha((int)(200.0F * alpha));
            if (afloat[0] == 1.0F) {
                Render2D.fillRect(graphics, afloat[1] - 0.25F, 0.0F, 0.5F, this.height, i);
            } else {
                Render2D.fillRect(graphics, 0.0F, afloat[1] - 0.25F, this.width, 0.5F, i);
            }
        }

        float f7 = this.renderDock(graphics, list, mouseX, mouseY, alpha);
        String s = "Drag to move   ·   Scroll to resize   ·   Right-click to hide   ·   Esc when done";
        Fonts.SMALL
            .drawCenteredX(
                graphics,
                "Drag to move   ·   Scroll to resize   ·   Right-click to hide   ·   Esc when done",
                this.width / 2.0F,
                f7 - 14.0F,
                ColorUtility.scaleAlpha(1946157055, alpha)
            );
    }

    public static float getChipWidth(HudModule hudModule) {
        return Fonts.SMALL.getWidth(hudModule.getName()) + 18.0F;
    }

    public static float getDockRowWidth(List<HudModule> hudModules) {
        float f = -4.0F;

        for (HudModule hudmodule : hudModules) {
            f += getChipWidth(hudmodule) + 4.0F;
        }

        return f;
    }

    public float renderDock(GuiGraphicsExtractor graphics, List<HudModule> hudModules, int value, int value2, float value3) {
        this.dockChips.clear();
        float f = Math.min(this.width - 40, 560);
        List<List<HudModule>> arraylist = new ArrayList<>();
        List<HudModule> arraylist1 = new ArrayList<>();
        float f1 = 0.0F;

        for (HudModule hudmodule : hudModules) {
            float f2 = getChipWidth(hudmodule) + 4.0F;
            if (!arraylist1.isEmpty() && f1 + f2 > f) {
                arraylist.add(arraylist1);
                arraylist1 = new ArrayList<>();
                f1 = 0.0F;
            }

            arraylist1.add(hudmodule);
            f1 += f2;
        }

        if (!arraylist1.isEmpty()) {
            arraylist.add(arraylist1);
        }

        float f10 = 0.0F;

        for (List<HudModule> list : arraylist) {
            f10 = Math.max(f10, getDockRowWidth(list));
        }

        float f11 = arraylist.size() * 19.0F - 4.0F + 10.0F;
        float f12 = f10 + 10.0F;
        float f3 = this.width / 2.0F - f12 / 2.0F;
        float f4 = this.height - f11 - 10.0F + (1.0F - value3) * 24.0F;
        Glass.drawBorderlessPanel(graphics, f3, f4, f12, f11, 8.0F, value3, 0.55F, 1.0F, -1509159920);
        List<float[]> arraylist2 = new ArrayList<>();

        for (int i = 0; i < arraylist.size(); i++) {
            float f5 = this.width / 2.0F - getDockRowWidth((List<HudModule>)arraylist.get(i)) / 2.0F;
            float f6 = f4 + 5.0F + i * 19.0F;

            for (HudModule hudmodule1 : arraylist.get(i)) {
                float f7 = getChipWidth(hudmodule1);
                boolean flag = Render2D.containsPoint(value, value2, f5, f6, f7, 15.0F);
                float f8 = clamp01(this.getSpring("dock" + hudmodule1.getId()).target(flag ? 1.0F : 0.0F).update());
                float f9 = clamp01(hudmodule1.toggleSpring.target(hudmodule1.isEnabled() ? 1.0F : 0.0F).update());
                Render2D.fillSubpixelRoundedRect(
                    graphics, f5, f6, f7, 15.0F, 5.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(150994943, 486539263, f8 * 0.6F), value3)
                );
                if (f9 > 0.01F) {
                    if (Theme.hasGradient()) {
                        Render2D.fillHorizontalRoundedGradient(
                            graphics,
                            f5,
                            f6,
                            f7,
                            15.0F,
                            5.0F,
                            ColorUtility.withAlpha(Theme.sampleGradient(0.0F), 71),
                            ColorUtility.withAlpha(Theme.sampleGradient(1.0F), 71),
                            value3 * f9
                        );
                    } else {
                        Render2D.fillSubpixelRoundedRect(graphics, f5, f6, f7, 15.0F, 5.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(71), value3 * f9));
                    }
                }

                this.dockChips.add(new HudEditorScreen.DockChip(hudmodule1, f5, f6, f7, 15.0F));
                arraylist2.add(new float[]{f5, f6, f8, f9});
                f5 += f7 + 4.0F;
            }
        }

        graphics.nextStratum();

        for (int j = 0; j < this.dockChips.size(); j++) {
            HudEditorScreen.DockChip hudeditorscreen$dockchip = this.dockChips.get(j);
            float[] afloat = (float[])arraylist2.get(j);
            int k = ColorUtility.lerpColor(-2130706433, -855638017, afloat[2]);
            Fonts.SMALL
                .drawCentered(
                    graphics,
                    hudeditorscreen$dockchip.module().getName(),
                    hudeditorscreen$dockchip.x() + hudeditorscreen$dockchip.w() / 2.0F,
                    hudeditorscreen$dockchip.y() + 7.5F,
                    ColorUtility.scaleAlpha(ColorUtility.lerpColor(k, -1, afloat[3]), value3)
                );
        }

        return f4;
    }

    public static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double d0 = event.x();
        double d1 = event.y();

        for (HudEditorScreen.DockChip hudeditorscreen$dockchip : this.dockChips) {
            if (Render2D.containsPoint(
                d0, d1, hudeditorscreen$dockchip.x(), hudeditorscreen$dockchip.y(), hudeditorscreen$dockchip.w(), hudeditorscreen$dockchip.h()
            )) {
                hudeditorscreen$dockchip.module().toggle();
                return true;
            }
        }

        List<HudModule> list = Adapt.getInstance().getHudManager().getHudModules();

        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule hudmodule = (HudModule)list.get(i);
            if (hudmodule.isEnabled()) {
                float f = hudmodule.resolveX(this.width);
                float f1 = hudmodule.resolveY(this.height);
                if (Render2D.containsPoint(d0, d1, f, f1, hudmodule.getScaledWidth(), hudmodule.getScaledHeight())) {
                    if (event.button() == 3) {
                        hudmodule.setEnabled(false);
                    } else {
                        this.draggingModule = hudmodule;
                        this.dragOffsetX = (float)d0 - f;
                        this.dragOffsetY = (float)d1 - f1;
                        this.onUiFeedback(1.15F);
                    }

                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingModule == null) {
            return false;
        }

        float f = (float)event.x() - this.dragOffsetX;
        float f1 = (float)event.y() - this.dragOffsetY;
        float f2 = this.draggingModule.getScaledWidth();
        float f3 = this.draggingModule.getScaledHeight();
        this.snapGuides.clear();
        List<Float> arraylist = new ArrayList<>(List.of(4.0F, this.width / 2.0F, this.width - 4.0F));
        List<Float> arraylist1 = new ArrayList<>(List.of(4.0F, this.height / 2.0F, this.height - 4.0F));

        for (HudModule hudmodule : Adapt.getInstance().getHudManager().getHudModules()) {
            if (hudmodule != this.draggingModule && hudmodule.isEnabled()) {
                float f4 = hudmodule.resolveX(this.width);
                float f5 = hudmodule.resolveY(this.height);
                arraylist.add(f4);
                arraylist.add(f4 + hudmodule.getScaledWidth());
                arraylist.add(f4 + hudmodule.getScaledWidth() / 2.0F);
                arraylist1.add(f5);
                arraylist1.add(f5 + hudmodule.getScaledHeight());
                arraylist1.add(f5 + hudmodule.getScaledHeight() / 2.0F);
            }
        }

        float[] afloat = new float[]{f, f + f2 / 2.0F, f + f2};
        float f9 = 6.0F;
        float f10 = 0.0F;

        for (float f6 : arraylist) {
            for (float f7 : afloat) {
                if (Math.abs(f6 - f7) < Math.abs(f9)) {
                    f9 = f6 - f7;
                    f10 = f6;
                }
            }
        }

        if (Math.abs(f9) <= 5.0F) {
            f += f9;
            this.snapGuides.add(new float[]{1.0F, f10});
        }

        float[] afloat1 = new float[]{f1, f1 + f3 / 2.0F, f1 + f3};
        float f11 = 6.0F;
        float f12 = 0.0F;

        for (float f13 : arraylist1) {
            for (float f8 : afloat1) {
                if (Math.abs(f13 - f8) < Math.abs(f11)) {
                    f11 = f13 - f8;
                    f12 = f13;
                }
            }
        }

        if (Math.abs(f11) <= 5.0F) {
            f1 += f11;
            this.snapGuides.add(new float[]{0.0F, f12});
        }

        f = Math.max(0.0F, Math.min(this.width - f2, f));
        f1 = Math.max(0.0F, Math.min(this.height - f3, f1));
        this.draggingModule.setPositionFromScreen(f, f1, this.width, this.height);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingModule != null) {
            this.draggingModule = null;
            this.snapGuides.clear();
            this.onUiFeedback(1.4F);
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (HudModule hudmodule : Adapt.getInstance().getHudManager().getHudModules()) {
            if (hudmodule.isEnabled()
                && Render2D.containsPoint(
                    mouseX, mouseY, hudmodule.resolveX(this.width), hudmodule.resolveY(this.height), hudmodule.getScaledWidth(), hudmodule.getScaledHeight()
                )) {
                hudmodule.scale.setValue(hudmodule.scale.getValue() + (scrollY > 0.0 ? 0.05 : -0.05));
                return true;
            }
        }

        return true;
    }

    private record DockChip(HudModule module, float x, float y, float w, float h) {
    }
}
