package dev.adapt.v3.ui.theme;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.impl.client.ThemeModule;
import dev.adapt.v3.util.render.ColorUtility;

public final class Theme {
    public static double gradientPhase;
    public static long lastPhaseTimeMs;

    public static ThemeModule getThemeModule() {
        return Adapt.getInstance() == null ? null : Adapt.getInstance().getModuleManager().getByClass(ThemeModule.class);
    }

    public static boolean isDark() {
        return true;
    }

    public static float timeSeconds() {
        return (float)(System.currentTimeMillis() % 3600000L) / 1000.0F;
    }

    public static int getAccentColor() {
        ThemeModule thememodule = getThemeModule();
        Accent accent = thememodule == null ? Accent.PEARL : thememodule.accent.getValue();
        if (accent == Accent.SPECTRUM) {
            return ColorUtility.fromHsb(timeSeconds() * 0.04F % 1.0F, 0.55F, 1.0F);
        } else {
            return accent == Accent.CUSTOM ? thememodule.primary.getValue() : accent.primaryColor;
        }
    }

    public static int sampleGradient(float value) {
        ThemeModule thememodule = getThemeModule();
        return !hasGradient()
            ? getAccentColor()
            : ColorUtility.lerpColor(getPrimaryColor(thememodule), getSecondaryColor(thememodule), Math.max(0.0F, Math.min(1.0F, value)));
    }

    public static int getPrimaryColor(ThemeModule themeModule) {
        Accent accent = themeModule.accent.getValue();
        return accent == Accent.CUSTOM ? themeModule.primary.getValue() : accent.primaryColor;
    }

    public static int getSecondaryColor(ThemeModule themeModule) {
        Accent accent = themeModule.accent.getValue();
        return accent == Accent.CUSTOM ? themeModule.secondary.getValue() : (accent.secondaryColor != 0 ? accent.secondaryColor : accent.primaryColor);
    }

    public static int getAnimatedWaveColor(int color, int color2) {
        ThemeModule thememodule = getThemeModule();
        if (!hasGradient()) {
            return getAccentColor();
        }

        double d0 = thememodule.animateGradient.getValue() ? advanceGradientPhase(thememodule.gradientSpeed.getFloatValue()) : 0.0;
        int i = (int)((((long)(d0 / color) - color2) % 360L + 360L) % 360L);
        i = (i >= 180 ? 360 - i : i) * 2;
        return ColorUtility.lerpColor(getPrimaryColor(thememodule), getSecondaryColor(thememodule), i / 360.0F);
    }

    public static double advanceGradientPhase(float value) {
        long i = System.currentTimeMillis();
        if (lastPhaseTimeMs == 0L) {
            gradientPhase = i;
        } else {
            gradientPhase = gradientPhase + (double)(i - lastPhaseTimeMs) * value;
        }

        lastPhaseTimeMs = i;
        return gradientPhase;
    }

    public static boolean hasGradient() {
        ThemeModule thememodule = getThemeModule();
        if (thememodule == null) {
            return false;
        }

        Accent accent = thememodule.accent.getValue();
        return accent == Accent.CUSTOM || accent.secondaryColor != 0;
    }

    public static int getAccentWithAlpha(int color) {
        return ColorUtility.withAlpha(getAccentColor(), color);
    }

    public static float getBloom() {
        ThemeModule thememodule = getThemeModule();
        return thememodule == null ? 0.6F : thememodule.bloom.getFloatValue() / 100.0F;
    }

    public static float getCornerRadius() {
        ThemeModule thememodule = getThemeModule();
        return thememodule == null ? 8.0F : thememodule.cornerRadius.getFloatValue();
    }

    public static float getLensZoom() {
        ThemeModule thememodule = getThemeModule();
        return thememodule == null ? 1.12F : thememodule.lensZoom.getFloatValue();
    }

    public static int getCardBg() {
        return isDark() ? -1710223340 : -1258817542;
    }

    public static int getCardBorder() {
        return isDark() ? 385875967 : 1090519039;
    }

    public static int getSubtleText() {
        return isDark() ? -803990503 : -587202561;
    }

    public static int getPrimaryText() {
        return isDark() ? -233170399 : -117440513;
    }

    public static int getSurfaceHover() {
        return isDark() ? 352321535 : 167772160;
    }

    public static int getSurfaceFill() {
        return isDark() ? 318767103 : 201326592;
    }

    public static int getTrackColor() {
        return isDark() ? 721420287 : 469762048;
    }

    public static int getDividerColor() {
        return isDark() ? 352321535 : 301989888;
    }

    public static int getScreenScrim() {
        return isDark() ? 1342506248 : 955577848;
    }

    public static int getShadowColor() {
        return isDark() ? -1879048192 : 706354995;
    }

    public static int getValueText() {
        return isDark() ? -723721 : -15263972;
    }

    public static int getMutedLabel() {
        return isDark() ? -5855566 : -9737352;
    }

    public static int getIconMuted() {
        return isDark() ? -9539972 : -5987152;
    }

    public static int getOnAccentText() {
        return -1;
    }
}
