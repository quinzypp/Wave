package dev.adapt.v3.feature.module.impl.hud;

import com.mojang.blaze3d.platform.Window;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.HiddenModule;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.Profiler;
import dev.adapt.v3.util.animation.Animation;
import dev.adapt.v3.util.animation.Easing;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.joml.Matrix3x2fStack;

public final class ModuleListHud extends HudModule {
    public final BooleanProperty showSuffix = new BooleanProperty("Show Suffix", true);
    public final BooleanProperty lowercase = new BooleanProperty("Lowercase", false);
    public final BooleanProperty offsetScoreboard = new BooleanProperty("Offset Scoreboard", true);
    public final Map<ModuleCategory, BooleanProperty> categoryToggles = new EnumMap<>(ModuleCategory.class);
    public final BooleanProperty accentNames = new BooleanProperty("Accent Names", true);
    public final BooleanProperty hideHudModules = new BooleanProperty("Hide HUD Modules", true);
    public final NumberProperty yPadding = new NumberProperty("Y Padding", "px", 0.0, 0.0, 8.0, 0.5);
    public final NumberProperty offset = new NumberProperty("Offset", "px", 0.0, 0.0, 20.0, 0.5);
    public final NumberProperty opacity = new NumberProperty("Opacity", "%", 100.0, 10.0, 100.0, 5.0);
    public final BooleanProperty blur = new BooleanProperty("Blur", true);
    public final BooleanProperty bloom = new BooleanProperty("Bloom", true);
    public final NumberProperty rounding = new NumberProperty("Rounding", "px", 3.0, 0.0, 6.0, 0.5);
    public static final float BASE_ROW_HEIGHT = 12.0F;
    public static final float SUFFIX_GAP = 3.0F;
    public static final float PAD_X = 4.0F;
    public static final float PAD_Y = 0.0F;
    public static final int SUFFIX_COLOR = -5592406;
    public float width = 80.0F;
    public float height = 12.0F;
    public final Map<Module, ModuleListHud.EntryState> entries = new LinkedHashMap<>();
    public final Map metricsCache = new IdentityHashMap();
    public static final float SHADOW_OFFSET = 0.5F;
    public static final float BG_ALPHA_FACTOR = 0.55F;
    public static final float[][] BLOOM_LAYERS = new float[][]{{12.1F, 2.2F, 0.4392157F}, {8.8F, 0.0F, 0.21960784F}};

    public ModuleListHud() {
        super("Module List", "Active modules, sorted and animated.", 1.0F, 0.0F);
        this.addProperties(this.showSuffix, this.lowercase, this.accentNames, this.offset, this.offsetScoreboard, this.hideHudModules);
        this.setPropertyGroup("Background");
        this.addProperties(this.opacity, this.blur, this.bloom, this.rounding);
        this.setPropertyGroup("Text");
        this.addProperties(this.yPadding);
        this.setPropertyGroup("Categories");

        for (ModuleCategory modulecategory : ModuleCategory.VALUES) {
            BooleanProperty booleanproperty = (BooleanProperty)new BooleanProperty(modulecategory.getDisplayName(), true)
                .withKey("category_" + modulecategory.name().toLowerCase());
            this.categoryToggles.put(modulecategory, booleanproperty);
            this.addProperties(booleanproperty);
        }

        this.setPropertyGroup(null);
        this.setEnabled(true);
    }

    public float getRowHeight() {
        return 12.0F + this.yPadding.getFloatValue();
    }

    public ModuleListHud.LabelMetrics getMetrics(Module module) {
        ModuleListHud.LabelMetrics modulelisthud$labelmetrics = (ModuleListHud.LabelMetrics)this.metricsCache.get(module);
        if (modulelisthud$labelmetrics == null) {
            String s = this.lowercase.getValue() ? module.getName().toLowerCase() : module.getName();
            String s1 = this.showSuffix.getValue() ? module.getSuffix() : null;
            if (s1 != null && s1.isEmpty()) {
                s1 = null;
            }

            if (s1 != null && this.lowercase.getValue()) {
                s1 = s1.toLowerCase();
            }

            float f = Fonts.LIST.getWidth(s);
            modulelisthud$labelmetrics = new ModuleListHud.LabelMetrics(s, s1, f, f + (s1 == null ? 0.0F : 3.0F + Fonts.LIST.getWidth(s1)));
            this.metricsCache.put(module, modulelisthud$labelmetrics);
        }

        return modulelisthud$labelmetrics;
    }

    public String getFormattedName(Module module) {
        return this.getMetrics(module).name();
    }

    public String getFormattedSuffix(Module module) {
        return this.getMetrics(module).suffix();
    }

    public float getTextWidth(Module module) {
        return this.getMetrics(module).textWidth();
    }

    public float getRowWidth(Module module) {
        return this.getTextWidth(module) + 8.0F;
    }

    @Override
    public float getBaseScale() {
        return 0.9F;
    }

    @Override
    public float getEdgeMargin() {
        return this.offset.getFloatValue();
    }

    @Override
    public boolean hasEdgeSnap() {
        return true;
    }

    public static int darkenForShadow(int value) {
        return (value & 16579836) >> 2 | value & 0xFF000000;
    }

    public void drawShadowedTexts(GuiGraphicsExtractor graphics, List<ModuleListHud.TextDraw> textDraws) {
        Matrix3x2fStack matrix3x2fstack = graphics.pose();
        int i = Minecraft.getInstance().getWindow().getGuiScale();
        float f = Math.abs(matrix3x2fstack.m00()) > 0.001F ? matrix3x2fstack.m00() : 1.0F;
        float f1 = Math.abs(matrix3x2fstack.m11()) > 0.001F ? matrix3x2fstack.m11() : 1.0F;
        float f2 = Math.max(1, Math.round(0.5F * f * i)) / (f * i);
        float f3 = Math.max(1, Math.round(0.5F * f1 * i)) / (f1 * i);
        List<ModuleListHud.TextDraw> arraylist = new ArrayList<>(textDraws.size());

        for (ModuleListHud.TextDraw modulelisthud$textdraw : textDraws) {
            float f4 = ((float)Math.round((f * modulelisthud$textdraw.x() + matrix3x2fstack.m20()) * i) / i - matrix3x2fstack.m20()) / f;
            float f5 = ((float)Math.round((f1 * modulelisthud$textdraw.y() + matrix3x2fstack.m21()) * i) / i - matrix3x2fstack.m21()) / f1;
            arraylist.add(new ModuleListHud.TextDraw(modulelisthud$textdraw.text(), f4, f5, modulelisthud$textdraw.color()));
        }

        graphics.nextStratum();

        for (ModuleListHud.TextDraw modulelisthud$textdraw1 : arraylist) {
            int j = darkenForShadow(modulelisthud$textdraw1.color());
            if (ColorUtility.getAlpha(j) > 0) {
                Fonts.LIST.drawCached(graphics, modulelisthud$textdraw1.text(), modulelisthud$textdraw1.x() + f2, modulelisthud$textdraw1.y() + f3, j);
            }
        }

        graphics.nextStratum();

        for (ModuleListHud.TextDraw modulelisthud$textdraw2 : arraylist) {
            Fonts.LIST
                .drawCached(graphics, modulelisthud$textdraw2.text(), modulelisthud$textdraw2.x(), modulelisthud$textdraw2.y(), modulelisthud$textdraw2.color());
        }

        graphics.nextStratum();
    }

    public boolean shouldShowModule(Module module) {
        if (this.hideHudModules.getValue() && module instanceof HudModule) {
            return false;
        }

        if (module instanceof HiddenModule) {
            return false;
        }

        BooleanProperty booleanproperty = this.categoryToggles.get(module.getCategory());
        return booleanproperty == null || booleanproperty.getValue();
    }

    @Override
    public float getWidth() {
        return this.width;
    }

    @Override
    public float getHeight() {
        return this.height;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing) {
        this.metricsCache.clear();
        boolean flag = this.getAnchorX() > 0.5F;
        List<Module> arraylist = new ArrayList<>();

        for (Module module : Adapt.getInstance().getModuleManager().getAll()) {
            if (module.isEnabled() && this.shouldShowModule(module)) {
                arraylist.add(module);
            }
        }

        arraylist.sort(Comparator.comparingDouble(this::getRowWidth).reversed());
        float f9 = this.getRowHeight();

        for (int k = 0; k < arraylist.size(); k++) {
            Module module1 = (Module)arraylist.get(k);
            float f = (this.getRowWidth(module1) + 8.0F + this.offset.getFloatValue()) * (flag ? 1 : -1);
            float f1 = k * f9;
            ModuleListHud.EntryState modulelisthud$entrystate = this.entries.computeIfAbsent(module1, module5 -> {
                ModuleListHud.EntryState modulelisthud$entrystate3 = new ModuleListHud.EntryState();
                modulelisthud$entrystate3.slideX.setValue(f);
                modulelisthud$entrystate3.slideY.setValue(f1);
                return modulelisthud$entrystate3;
            });
            modulelisthud$entrystate.targetY = f1;
        }

        List<Module> arraylist2 = new ArrayList<>(this.entries.keySet());
        arraylist2.sort(Comparator.comparingDouble(this::getRowWidth).reversed());
        float f10 = 40.0F;

        for (Module module4 : arraylist2) {
            f10 = Math.max(f10, this.getRowWidth(module4));
        }

        float f11 = 0.0F;
        float f12 = 0.0F;
        float f13 = 0.0F;
        List<Module> arraylist1 = new ArrayList<>();

        for (Module module2 : arraylist2) {
            ModuleListHud.EntryState modulelisthud$entrystate1 = this.entries.get(module2);
            boolean flag1 = module2.isEnabled() && this.shouldShowModule(module2);
            float f2 = (this.getRowWidth(module2) + 8.0F + this.offset.getFloatValue()) * (flag ? 1 : -1);
            modulelisthud$entrystate1.slideX.run(flag1 ? 0.0F : f2);
            modulelisthud$entrystate1.slideY.run(modulelisthud$entrystate1.targetY);
            modulelisthud$entrystate1.presence.run(flag1 ? 1.0F : 0.0F);
            if (!flag1 && modulelisthud$entrystate1.slideX.isFinished()) {
                this.entries.remove(module2);
            } else {
                f11 += f9 * modulelisthud$entrystate1.presence.getValue();
                if (flag1) {
                    f12 = Math.max(f12, this.getRowWidth(module2));
                }

                arraylist1.add(module2);
            }
        }

        long l = Profiler.beginSection();
        if (!arraylist1.isEmpty()) {
            this.renderBackground(graphics, arraylist1, f10, flag);
        }

        Profiler.endSection("list_bg", l);
        List<ModuleListHud.TextDraw> arraylist3 = new ArrayList<>();
        int i1 = 0;

        for (Module module3 : arraylist1) {
            ModuleListHud.EntryState modulelisthud$entrystate2 = this.entries.get(module3);
            float f3 = this.getRowWidth(module3);
            float f4 = (flag ? f10 - f3 : 0.0F) + modulelisthud$entrystate2.slideX.getValue() + 4.0F;
            float f5 = modulelisthud$entrystate2.slideY.getValue() + 0.0F;
            float f6 = f5 + f9 / 2.0F;
            int i = Theme.hasGradient() ? Theme.getAnimatedWaveColor(6, i1 * 20) : Theme.getAccentColor();
            boolean flag2 = this.accentNames.getValue();
            int j = flag2 ? i : -1;
            float f7 = f4;
            float f8 = f6 - Fonts.LIST.getPointSize() / 2.0F;
            String s = this.getFormattedName(module3);
            arraylist3.add(new ModuleListHud.TextDraw(s, f7, f8, j));
            String s1 = this.getFormattedSuffix(module3);
            if (s1 != null) {
                arraylist3.add(new ModuleListHud.TextDraw(s1, f7 + this.getMetrics(module3).nameWidth() + 3.0F, f8, -5592406));
            }

            i1++;
        }

        long j1 = Profiler.beginSection();
        if (!arraylist3.isEmpty()) {
            this.drawShadowedTexts(graphics, arraylist3);
        }

        Profiler.endSection("list_text", j1);
        this.width = f10;
        this.height = Math.max(f9, f11 + 0.0F);
        if (editing && this.entries.isEmpty()) {
            Fonts.REGULAR.drawWithGlowBlob(graphics, "Module List", 0.0F, 1.0F, 1946157055, 1426063360);
        }
    }

    public void renderBackground(GuiGraphicsExtractor graphics, List<Module> modules, float value, boolean flag2) {
        ArrayList arraylist = new ArrayList(modules);
        arraylist.sort(Comparator.comparingDouble(key -> this.entries.get(key).slideY.getValue()));
        int i = arraylist.size();
        float[][] afloat = new float[i][];

        for (int j = 0; j < i; j++) {
            Module module = (Module)arraylist.get(j);
            ModuleListHud.EntryState modulelisthud$entrystate = this.entries.get(module);
            float f = this.getRowWidth(module);
            float f1 = (flag2 ? value - f : 0.0F) + modulelisthud$entrystate.slideX.getValue();
            float f2 = modulelisthud$entrystate.slideY.getValue();
            float f3 = j + 1 < i
                ? this.entries.get(arraylist.get(j + 1)).slideY.getValue()
                : f2 + this.getRowHeight() * modulelisthud$entrystate.presence.getValue();
            afloat[j] = new float[]{f1, f1 + f, f2, Math.max(f2, f3)};
        }

        float f4 = this.opacity.getFloatValue() / 100.0F;
        float f5 = this.rounding.getFloatValue();
        boolean flag = this.blur.getValue();
        if (this.bloom.getValue()) {
            Render2D.drawMultiRectBloom(graphics, "module_list", afloat, BLOOM_LAYERS, ColorUtility.scaleAlpha(-16777216, f4));
        }

        Render2D.setContourFlags(this.computeFlushEdgeMask(flag2));

        try {
            graphics.nextStratum();
            if (flag) {
                Render2D.blurSteppedContour(graphics, afloat, f5, f4 * f4);
            }

            graphics.nextStratum();
            Render2D.drawCachedSteppedPanel(graphics, "module_list", afloat, f5, ColorUtility.scaleAlpha(-2146694128, f4), 0, 0.0F, false);
            graphics.nextStratum();
        } finally {
            Render2D.setContourFlags(0);
        }
    }

    public int computeFlushEdgeMask(boolean flag) {
        Window window = Minecraft.getInstance().getWindow();
        int i = window.getGuiScaledWidth();
        int j = window.getGuiScaledHeight();
        float f = this.resolveX(i);
        float f1 = this.resolveY(j);
        float f2 = 0.25F;
        int mask = 0;
        if (!flag && f <= 0.25F) {
            mask |= 1;
        }

        if (flag && f + this.getScaledWidth() >= i - 0.25F) {
            mask |= 2;
        }

        if (f1 <= 0.25F) {
            mask |= 4;
        }

        if (f1 + this.getScaledHeight() >= j - 0.25F) {
            mask |= 8;
        }

        return mask;
    }

    public float getScoreboardOffset(int first, int second) {
        if (this.isEnabled() && this.offsetScoreboard.getValue() && !(this.getAnchorX() <= 0.5F)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return 0.0F;
            }

            Objective objective = minecraft.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) {
                return 0.0F;
            }

            long i = Math.min(15L, minecraft.level.getScoreboard().listPlayerScores(objective).stream().filter(playerScoreEntry -> !playerScoreEntry.isHidden()).count());
            int j = (int)i * 9;
            int k = second / 2 + j / 3 - j;
            float f = this.resolveY(second) + this.getScaledHeight();
            return Math.max(0.0F, f + 20.0F - k);
        } else {
            return 0.0F;
        }
    }

    private static final class EntryState {
        public final Animation slideX = new Animation(Easing.EASE_OUT_EXPO, 400L);
        public final Animation slideY = new Animation(Easing.EASE_OUT_EXPO, 600L);
        public final Animation presence = new Animation(Easing.EASE_IN_OUT_CUBIC, 200L);
        public float targetY;

        public EntryState() {
        }
    }

    private record LabelMetrics(String name, String suffix, float nameWidth, float textWidth) {
    }

    private record TextDraw(String text, float x, float y, int color) {
    }
}
