package dev.adapt.v3.feature.module.impl.hud;

import dev.adapt.v3.event.impl.AttackEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.hud.HudModule;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public final class TargetDisplayHud extends HudModule {
    public final EnumProperty<TargetDisplayHud.Style> style = new EnumProperty<>("Style", TargetDisplayHud.Style.GLASS);
    public final NumberProperty linger = new NumberProperty("Linger", "s", 2.0, 0.0, 6.0, 0.5);
    public static final float CLASSIC_MIN_WIDTH = 136.0F;
    public static final float CLASSIC_HEIGHT = 39.6F;
    public static final float CLASSIC_RADIUS = 6.0F;
    public static final float CLASSIC_HEAD_X = 4.34F;
    public static final float CLASSIC_HEAD_Y = 3.2F;
    public static final float CLASSIC_HEAD_SIZE = 28.65F;
    public static final float CLASSIC_HEAD_RADIUS = 3.5F;
    public static final float CLASSIC_TEXT_X = 36.95F;
    public static final float CLASSIC_NAME_Y = 4.6F;
    public static final float CLASSIC_HP_Y = 16.04F;
    public static final float CLASSIC_DIST_Y = 23.94F;
    public static final float CLASSIC_TITLE_PT = 7.85F;
    public static final float CLASSIC_SUB_PT = 5.41F;
    public static final float CLASSIC_ASCENT_RATIO = 0.727F;
    public static final float CLASSIC_BAR_HEIGHT = 3.55F;
    public static final float CLASSIC_BAR_GLOW = 0.7F;
    public static final int CLASSIC_NAME_COLOR = -1;
    public static final int CLASSIC_SUB_COLOR = -3947581;
    public static final int CLASSIC_BAR_BG_COLOR = 385875967;
    public static final int CLASSIC_MOB_BG_COLOR = -14803420;
    public static final int CLASSIC_HEART_COLOR = -3921641;
    public static final float CLASSIC_HEART_GAP = 2.9F;
    public static final float CLASSIC_HEART_WIDTH = 5.55F;
    public static final float CLASSIC_HEART_HEIGHT = 4.86F;
    public static final float CLASSIC_HEART_OFFSET_Y = 0.3F;
    public static final float GLASS_WIDTH = 136.0F;
    public static final float GLASS_HEIGHT = 40.0F;
    public static final float GLASS_HEAD_POS = 6.0F;
    public static final float GLASS_HEAD_SIZE = 28.0F;
    public static final float GLASS_RADIUS = 9.0F;
    public static final float GLASS_MOB_HEAD_RADIUS = 5.0F;
    public static final float GLASS_TEXT_X = 41.0F;
    public static final float GLASS_NAME_Y = 11.5F;
    public static final float GLASS_HP_Y = 21.5F;
    public static final float GLASS_BAR_Y = 28.5F;
    public static final float GLASS_BAR_HEIGHT = 3.0F;
    public static final int COLOR_HP_LOW = -1027478;
    public static final int COLOR_ABSORPTION = -14003;
    public static final int COLOR_HP_HIGH = -13315175;
    public LivingEntity target;
    public long lastAttackTimeMs;
    public final Spring visibilitySpring = new Spring(0.0F, 300.0F, 34.6F);
    public final Spring healthSpring = new Spring(1.0F, 120.0F, 21.9F);
    public final Spring absorptionSpring = new Spring(0.0F, 120.0F, 21.9F);
    public LivingEntity lastRenderedTarget;

    public TargetDisplayHud() {
        super("Target Display", "Shows who you're fighting: head, health and distance.", ModuleCategory.VISUAL, 0.56F, 0.6F);
        this.addProperties(this.style, this.linger);
        this.setEnabled(true);
    }

    @Override
    public float getBaseScale() {
        return 0.85F;
    }

    public boolean isClassicStyle() {
        return this.style.getValue() == TargetDisplayHud.Style.CLASSIC;
    }

    @Override
    public float getWidth() {
        return this.isClassicStyle() ? computeClassicWidth(this.getPreviewOrTarget()) : 136.0F;
    }

    public LivingEntity getPreviewOrTarget() {
        return this.target != null ? this.target : mc.player;
    }

    public static float computeClassicWidth(LivingEntity living) {
        return living == null ? 136.0F : Math.max(136.0F, 36.95F + Fonts.GLOW_TAG.getWidth(living.getName().getString(), getClassicNameScale()) + 12.0F);
    }

    public static float getClassicNameScale() {
        return 10.797799F / Fonts.GLOW_TAG.getPointSize();
    }

    public static float getClassicSubScale() {
        return 7.4415402F / Fonts.GLOW_TAG.getPointSize();
    }

    public static float alignClassicTextY(float first, float second) {
        float f = Fonts.GLOW_TAG.getPointSize() * second;
        return first + f * 0.727F / 2.0F - f / 2.0F;
    }

    @Override
    public float getHeight() {
        return this.isClassicStyle() ? 39.6F : 40.0F;
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (this.isValidTarget(event.getTarget())) {
            this.target = (LivingEntity)event.getTarget();
            this.lastAttackTimeMs = System.currentTimeMillis();
        }
    }

    public boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity livingentity
            && livingentity.isAlive()
            && !livingentity.isInvisible()
            && livingentity != mc.player
            && (livingentity instanceof Player || livingentity instanceof Mob);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTick, boolean editing) {
        if (this.target != null && (this.target.isRemoved() || mc.level == null || this.target.level() != mc.level)) {
            this.target = null;
        }

        boolean flag = this.target != null
            && this.target.isAlive()
            && !this.target.isInvisible()
            && System.currentTimeMillis() - this.lastAttackTimeMs < this.linger.getValue() * 1000.0 + 50.0;
        LivingEntity livingentity = flag ? this.target : null;
        if (livingentity == null && editing) {
            livingentity = mc.player;
        }

        float f = Math.max(0.0F, Math.min(1.0F, this.visibilitySpring.target(livingentity != null ? 1.0F : 0.0F).update()));
        if (livingentity == null) {
            livingentity = this.target;
        }

        if (!(f < 0.02F) && livingentity != null) {
            if (livingentity != this.lastRenderedTarget) {
                this.healthSpring.snap(getHealthFraction(livingentity.getHealth(), livingentity));
                this.absorptionSpring.snap(getHealthFraction(livingentity.getAbsorptionAmount(), livingentity));
                this.lastRenderedTarget = livingentity;
            }

            boolean flag1 = f < 0.999F;
            if (flag1) {
                float f1 = 0.94F + 0.06F * f;
                float f2 = this.getWidth();
                float f3 = this.getHeight();
                graphics.pose().pushMatrix();
                graphics.pose().translate(f2 / 2.0F, f3 / 2.0F);
                graphics.pose().scale(f1, f1);
                graphics.pose().translate(-f2 / 2.0F, -f3 / 2.0F);
            }

            if (this.isClassicStyle()) {
                this.renderClassic(graphics, livingentity, f);
            } else {
                this.renderGlass(graphics, livingentity, f);
            }

            if (flag1) {
                graphics.pose().popMatrix();
            }
        }
    }

    public void renderClassic(GuiGraphicsExtractor graphics, LivingEntity living, float value) {
        float f = computeClassicWidth(living);
        float f1 = 39.6F;
        Glass.drawBorderlessPanel(graphics, 0.0F, 0.0F, f, 39.6F, 6.0F, value, 0.55F, 1.0F, -2146694128);
        if (living instanceof AbstractClientPlayer abstractclientplayer) {
            Identifier identifier = abstractclientplayer.getSkin().body().texturePath();
            int i = ColorUtility.scaleAlpha(-1, value);
            Render2D.blitRoundedTexture(graphics, identifier, 4.34F, 3.2F, 28.65F, 28.65F, 3.5F, 0.125F, 0.125F, 0.25F, 0.25F, i);
            graphics.nextStratum();
            Render2D.blitRoundedTexture(graphics, identifier, 4.34F, 3.2F, 28.65F, 28.65F, 3.5F, 0.625F, 0.125F, 0.75F, 0.25F, i);
        } else {
            Render2D.fillSubpixelRoundedRect(graphics, 4.34F, 3.2F, 28.65F, 28.65F, 3.5F, ColorUtility.scaleAlpha(-14803420, value));
        }

        float f5 = getClassicNameScale();
        float f6 = getClassicSubScale();
        Fonts.GLOW_TAG.drawScaled(graphics, living.getName().getString(), 36.95F, alignClassicTextY(4.6F, f5), f5, ColorUtility.scaleAlpha(-1, value));
        String s = formatCompactNumber(living.getHealth()) + "hp";
        Fonts.GLOW_TAG.drawScaled(graphics, s, 36.95F, alignClassicTextY(16.04F, f6), f6, ColorUtility.scaleAlpha(-3947581, value));
        float f2 = 36.95F + Fonts.GLOW_TAG.getWidth(s, f6) + 2.9F;
        Fonts.ICONS.drawIconCentered(graphics, "\ue87d", f2 + 2.775F, 18.77F, 0.6686747F, ColorUtility.scaleAlpha(-3921641, value));
        if (mc.player != null) {
            Fonts.GLOW_TAG
                .drawScaled(
                    graphics,
                    formatCompactNumber(mc.player.distanceTo(living)) + "m",
                    36.95F,
                    alignClassicTextY(23.94F, f6),
                    f6,
                    ColorUtility.scaleAlpha(-3947581, value)
                );
        }

        float f3 = 36.05F;
        float f4 = Math.max(0.0F, Math.min(1.0F, this.healthSpring.target(getHealthFraction(living.getHealth(), living)).update()));
        this.absorptionSpring.target(getHealthFraction(living.getAbsorptionAmount(), living)).update();
        Render2D.fillBottomRoundedProgressBar(graphics, 0.0F, 36.05F, f, 3.55F, 6.0F, f, 385875967, 385875967, 1.0F, value);
        if (f4 > 0.001F) {
            Render2D.fillBottomRoundedProgressBar(
                graphics, 0.0F, 36.05F, f, 3.55F, 6.0F, f * f4, Theme.sampleGradient(0.0F), Theme.sampleGradient(1.0F), 0.7F, value
            );
        }
    }

    public static String formatCompactNumber(float value) {
        float f = Math.round(value * 10.0F) / 10.0F;
        return f == Math.round(f) ? String.valueOf(Math.round(f)) : String.format(Locale.ROOT, "%.1f", f);
    }

    public static String getInitialLetter(LivingEntity living) {
        String s = living.getName().getString();
        return s.isEmpty() ? "?" : s.substring(0, s.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }

    public void renderGlass(GuiGraphicsExtractor graphics, LivingEntity living, float value) {
        Glass.drawBorderlessPanel(graphics, 0.0F, 0.0F, 136.0F, 40.0F, 9.0F, value, 0.55F, 1.0F, -2146694128);
        if (living instanceof AbstractClientPlayer abstractclientplayer) {
            Render2D.drawPlayerHead(graphics, abstractclientplayer.getSkin().body().texturePath(), 6.0F, 6.0F, 28.0F, ColorUtility.scaleAlpha(-1, value));
        } else {
            Render2D.fillSubpixelRoundedRect(graphics, 6.0F, 6.0F, 28.0F, 28.0F, 5.0F, ColorUtility.scaleAlpha(536870911, value));
            Fonts.TITLE.drawIconCentered(graphics, getInitialLetter(living), 20.0F, 20.0F, 1.0F, ColorUtility.scaleAlpha(-1, value));
        }

        float f6 = 128.0F;
        String s = null;
        if (mc.player != null && living != mc.player) {
            s = String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(living));
        }

        float f = s == null ? 0.0F : Fonts.SMALL.getWidth(s) + 5.0F;
        Fonts.SEMIBOLD.drawVCentered(graphics, Fonts.SEMIBOLD.ellipsize(living.getName().getString(), 87.0F - f), 41.0F, 11.5F, ColorUtility.scaleAlpha(-1, value));
        if (s != null) {
            Fonts.SMALL.drawVCentered(graphics, s, 128.0F - Fonts.SMALL.getWidth(s), 11.5F, ColorUtility.scaleAlpha(1946157055, value));
        }

        float f1 = living.getHealth();
        float f2 = living.getAbsorptionAmount();
        String s1 = String.format(Locale.ROOT, "%.1f", f1 + f2);
        Fonts.SMALL.drawVCentered(graphics, s1, 41.0F, 21.5F, ColorUtility.scaleAlpha(f2 > 0.0F ? -14003 : -1493172225, value));
        Fonts.ICONS.drawIconCentered(graphics, "\ue87d", 41.0F + Fonts.SMALL.getWidth(s1) + 6.0F, 21.5F, 0.8F, ColorUtility.scaleAlpha(-1027478, value));
        float f3 = 87.0F;
        float f4 = Math.max(0.0F, Math.min(1.0F, this.healthSpring.target(getHealthFraction(f1, living)).update()));
        float f5 = Math.max(0.0F, Math.min(1.0F, this.absorptionSpring.target(getHealthFraction(f2, living)).update()));
        Render2D.fillSubpixelRoundedRect(graphics, 41.0F, 28.5F, 87.0F, 3.0F, 1.5F, ColorUtility.scaleAlpha(654311423, value));
        if (f4 > 0.001F) {
            Render2D.fillSubpixelRoundedRect(
                graphics, 41.0F, 28.5F, Math.max(3.0F, 87.0F * f4), 3.0F, 1.5F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-1027478, -13315175, f4), value)
            );
        }

        if (f5 > 0.001F) {
            Render2D.fillSubpixelRoundedRect(graphics, 41.0F, 28.5F, Math.max(3.0F, 87.0F * f5), 3.0F, 1.5F, ColorUtility.scaleAlpha(-14003, value * 0.85F));
        }
    }

    public static float getHealthFraction(float value, LivingEntity living) {
        return Math.max(0.0F, Math.min(1.0F, value / Math.max(1.0F, living.getMaxHealth())));
    }

    public enum Style implements Displayable {
        GLASS("Glass"),
        CLASSIC("Classic");

        public final String displayName;

        Style(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }
    }
}
