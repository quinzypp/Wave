package dev.adapt.v3.feature.module.impl.visual;

import java.util.List;
import dev.adapt.v3.Adapt;
// VENDOR-DISABLED: import dev.adapt.v3.chat.AdaptUsers;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.render.ColorUtility;
import dev.adapt.v3.util.render.ProjectionUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class NametagsModule extends Module {
    public final EnumProperty<NametagsModule.Style> style = new EnumProperty<>("Style", NametagsModule.Style.CLASSIC);
    public final BooleanProperty head = new BooleanProperty("Head", true);
    public final EnumProperty headSide = (EnumProperty)new EnumProperty<>("Head Side", NametagsModule.HeadSide.RIGHT)
        .hideIf(() -> !this.head.getValue() || this.isCapsule());
    public final BooleanProperty health = new BooleanProperty("Health", true);
    public final BooleanProperty distance = (BooleanProperty)new BooleanProperty("Distance", false).hideIf(() -> !this.isCapsule());
    public final BooleanProperty blur = new BooleanProperty("Blur", true);
    public final NumberProperty size = new NumberProperty("Size", "x", 1.0, 0.4, 1.9, 0.05);
    public static final float BASE_SCALE = 0.8F;
    public static final float CLASSIC_UNIT_SCALE = 0.30395138F;
    public static final float CLASSIC_HEIGHT_WITH_HEALTH = 95.0F;
    public static final float CLASSIC_CORNER_RADIUS = 10.0F;
    public static final float CLASSIC_BAR_HEIGHT = 8.5F;
    public static final float CLASSIC_PAD_NO_HEALTH = 16.5F;
    public static final float CLASSIC_HEIGHT_NO_HEALTH = 61.0F;
    public static final float CLASSIC_PAD_LEFT = 12.5F;
    public static final float CLASSIC_PAD_RIGHT = 17.0F;
    public static final float CLASSIC_NAME_Y = 16.5F;
    public static final float CLASSIC_NAME_SIZE = 19.5F;
    public static final float CLASSIC_HEALTH_Y = 51.5F;
    public static final float CLASSIC_HEALTH_SIZE = 16.5F;
    public static final float CLASSIC_HEART_GAP = 7.5F;
    public static final float CLASSIC_HEART_W = 20.0F;
    public static final float CLASSIC_HEART_H = 18.0F;
    public static final float CLASSIC_HEART_Y_OFFSET = -1.0F;
    public static final float CLASSIC_HEAD_SIZE_WITH_HEALTH = 62.0F;
    public static final float CLASSIC_HEAD_MARGIN_WITH_HEALTH = 13.0F;
    public static final float CLASSIC_HEAD_GAP_WITH_HEALTH = 25.0F;
    public static final float CLASSIC_HEAD_SIZE_NO_HEALTH = 34.5F;
    public static final float CLASSIC_HEAD_MARGIN_NO_HEALTH = 9.0F;
    public static final float CLASSIC_HEAD_GAP_NO_HEALTH = 9.5F;
    public static final float CLASSIC_HEAD_RADIUS = 5.0F;
    public static final float ANCHOR_Y_GAP = 2.0F;
    public static final Fonts NAME_FONT = Fonts.MEDIUM;
    public static final Fonts HEALTH_FONT = Fonts.REGULAR;
    public static final float CAP_HEIGHT_RATIO = 0.698F;
    public static final int CLASSIC_HEALTH_TEXT_COLOR = -3947581;
    public static final int CLASSIC_HEART_COLOR = -2551027;
    public static final int CLASSIC_BAR_BG_COLOR = 385875967;
    public static final float CLASSIC_BAR_SHADE = 0.7F;
    public static final String HEART_ICON = "\ue87d";
    public static final float CLASSIC_HEART_FONT_DIVISOR = 8.3F;
    public static final float CLASSIC_SHADOW_SCALE = 0.55F;
    public static final float CLASSIC_SHADOW_REF_HEIGHT = 39.6F;
    public static final float CAPSULE_HEIGHT = 11.0F;
    public static final float CAPSULE_RADIUS = 5.5F;
    public static final float CAPSULE_HEAD_INSET = 1.75F;
    public static final float CAPSULE_HEAD_SIZE = 7.5F;
    public static final float CAPSULE_PAD_X = 5.0F;
    public static final float CAPSULE_HEAD_GAP = 3.5F;
    public static final float CAPSULE_SECTION_GAP = 4.5F;
    public static final float CAPSULE_HEART_GAP = 1.5F;
    public static final float CAPSULE_NAME_SCALE = 0.72F;
    public static final float CAPSULE_META_SCALE = 0.8F;
    public static final float CAPSULE_HEART_SCALE = 0.5F;
    public static final int CAPSULE_NAME_COLOR = -218103809;
    public static final int CAPSULE_DISTANCE_COLOR = -2130706433;
    public static final int HEALTH_HIGH_COLOR = -8658784;
    public static final int HEALTH_MID_COLOR = -864149;
    public static final int HEALTH_LOW_COLOR = -890005;
    public static final int ADAPT_USER_COLOR = -990977;

    public float getScale() {
        return this.size.getFloatValue() * 0.8F;
    }

    public NametagsModule() {
        super("Nametags", "Glass nametags with head and health above players.", ModuleCategory.VISUAL);
        this.addProperties(this.style, this.head, this.headSide, this.health, this.distance, this.blur, this.size);
    }

    public boolean isCapsule() {
        return this.style.getValue() == NametagsModule.Style.CAPSULE;
    }

    public static NametagsModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(NametagsModule.class);
    }

    public static boolean shouldSuppressVanillaNametag(Entity entity) {
        NametagsModule nametagsmodule = getInstance();
        return nametagsmodule != null && nametagsmodule.isEnabled() && nametagsmodule.isValidTarget(entity);
    }

    public boolean isValidTarget(Entity entity) {
        return entity instanceof Player player
            && player != mc.player
            && player.isAlive()
            && player.getHealth() > 0.0F
            && !"BOT".equals(player.getName().getString());
    }

    public void render(GuiGraphicsExtractor graphics, float partialTick) {
        if (this.isEnabled() && mc.level != null && mc.player != null) {
            float f = graphics.guiWidth();
            float f1 = graphics.guiHeight();
            List<Player> arraylist = new ArrayList<>();

            for (Player player : mc.level.players()) {
                if (this.isValidTarget(player)) {
                    arraylist.add(player);
                }
            }

            arraylist.sort(Comparator.comparingDouble(player2 -> -mc.player.distanceToSqr(player2)));

            for (Player player1 : arraylist) {
                float[] afloat = ProjectionUtility.projectEntityBounds(player1, partialTick, f, f1);
                if (afloat != null) {
                    if (this.isCapsule()) {
                        this.renderCapsule(graphics, player1, (afloat[0] + afloat[2]) / 2.0F, afloat[1] - 2.0F);
                    } else {
                        this.renderClassic(graphics, player1, (afloat[0] + afloat[2]) / 2.0F, afloat[1] - 2.0F);
                    }
                }
            }
        }
    }

    public static int getHealthColor(float value) {
        return value > 0.5F ? ColorUtility.lerpColor(-864149, -8658784, (value - 0.5F) * 2.0F) : ColorUtility.lerpColor(-890005, -864149, value * 2.0F);
    }

    public void renderCapsule(GuiGraphicsExtractor graphics, Player player, float first, float second) {
        float f = this.getScale() / 0.7F;
        boolean flag = this.head.getValue() && player instanceof AbstractClientPlayer;
        String s = formatDisplayName(player);
        float f1 = 0.72F * f;
        float f2 = 0.8F * f;
        String s1 = this.distance.getValue() ? Math.round(mc.player.distanceTo(player)) + "m" : null;
        String s2 = this.health.getValue() ? formatHealth(player.getHealth()) : null;
        float f3 = Fonts.ICONS.getWidth("\ue87d", 0.5F * f);
        float f4 = flag ? 12.75F : 5.0F;
        float f5 = f4;
        if (s1 != null) {
            f4 += Fonts.SMALL.getWidth(s1, f2) / f + 4.5F;
        }

        float f6 = f4;
        f4 += Fonts.MEDIUM.getWidth(s, f1) / f;
        float f7 = f4 + 4.5F;
        if (s2 != null) {
            f4 = f7 + Fonts.SMALL.getWidth(s2, f2) / f + 1.5F + f3 / f;
        }

        f4 += 5.0F;
        float f8 = Render2D.snapToPixel(first - f4 * f / 2.0F);
        float f9 = Render2D.snapToPixel(second - 11.0F * f);
        float f10 = f4 * f;
        float f11 = 11.0F * f;
        float f12 = 5.5F * f;
        float f13 = f9 + f11 / 2.0F;
        float f14 = Math.max(0.0F, Math.min(1.0F, player.getHealth() / Math.max(1.0F, player.getMaxHealth())));
        graphics.nextStratum();
        Glass.drawHudSurface(graphics, f8, f9, f10, f11, f12, 1.0F, 0.3F * f, 1.0F, -2146694128, this.blur.getValue());
        if (flag) {
            Identifier identifier = ((AbstractClientPlayer)player).getSkin().body().texturePath();
            float f15 = f8 + 1.75F * f;
            float f16 = f9 + 1.75F * f;
            float f17 = 7.5F * f;
            float f18 = f17 / 2.0F;
            Render2D.blitRoundedTexture(graphics, identifier, f15, f16, f17, f17, f18, 0.125F, 0.125F, 0.25F, 0.25F, -1);
            graphics.nextStratum();
            Render2D.blitRoundedTexture(graphics, identifier, f15, f16, f17, f17, f18, 0.625F, 0.125F, 0.75F, 0.25F, -1);
        }

        int i = player.getTeamColor();
        int j = i != 16777215 && i != 0 ? 0xFF000000 | i : -218103809;
        drawPlayerName(graphics, Fonts.MEDIUM, player, f8 + f6 * f, f13 - Fonts.MEDIUM.getPointSize() * f1 / 2.0F, f1, j);
        float f20 = f13 - Fonts.SMALL.getPointSize() * f2 / 2.0F;
        if (s1 != null) {
            Fonts.SMALL.drawScaled(graphics, s1, f8 + f5 * f, f20, f2, -2130706433);
        }

        if (s2 != null) {
            int k = getHealthColor(f14);
            float f21 = f8 + f7 * f;
            Fonts.SMALL.drawScaled(graphics, s2, f21, f20, f2, k);
            float f19 = f21 + Fonts.SMALL.getWidth(s2, f2) + 1.5F * f;
            Fonts.ICONS.drawIconCentered(graphics, "\ue87d", f19 + f3 / 2.0F, f13, 0.5F * f, k);
        }
    }

    public static String formatDisplayName(Player player) {
        String s = player.getName().getString();
        String s1 = null; // VENDOR-DISABLED: AdaptUsers.getAdaptUsername(player.getUUID());
        return s1 == null ? s : s + " (" + s1 + ")";
    }

    public static void drawPlayerName(GuiGraphicsExtractor graphics, Fonts font, Player player, float value, float value2, float value3, int value4) {
        String s = player.getName().getString();
        String s1 = null; // VENDOR-DISABLED: AdaptUsers.getAdaptUsername(player.getUUID());
        if (s1 == null) {
            font.drawScaled(graphics, s, value, value2, value3, value4);
        } else {
            String s2 = s + " (";
            font.drawScaled(graphics, s2, value, value2, value3, value4);
            value += font.getWidth(s2, value3);
            font.drawScaled(graphics, s1, value, value2, value3, -990977);
            value += font.getWidth(s1, value3);
            font.drawScaled(graphics, ")", value, value2, value3, value4);
        }
    }

    public void renderClassic(GuiGraphicsExtractor graphics, Player player, float first, float second) {
        float f = 0.30395138F * this.getScale();
        boolean flag = this.health.getValue();
        boolean flag1 = this.head.getValue() && player instanceof AbstractClientPlayer;
        boolean flag2 = this.headSide.getValue() == NametagsModule.HeadSide.LEFT;
        String s = formatDisplayName(player);
        String s1 = "Health: " + formatHealth(player.getHealth());
        float f1 = 19.5F * f / 0.698F / NAME_FONT.getPointSize();
        float f2 = 16.5F * f / 0.698F / HEALTH_FONT.getPointSize();
        float f3 = NAME_FONT.getWidth(s, f1) / f;
        float f4 = flag ? HEALTH_FONT.getWidth(s1, f2) / f + 7.5F + 20.0F : 0.0F;
        float f5 = Math.max(f3, f4);
        float f6 = flag ? 95.0F : 61.0F;
        float f7 = flag ? 62.0F : 34.5F;
        float f8 = flag ? 13.0F : 9.0F;
        float f9 = flag ? 25.0F : 9.5F;
        float f10 = flag ? 16.5F : 16.5F;
        float f11 = flag ? 13.0F : f10 + 9.75F - 17.25F;
        float f12 = flag ? 12.5F : 16.5F;
        float f13 = flag ? 17.0F : 16.5F;
        float f14;
        float f15;
        float f16;
        if (!flag1) {
            f14 = f12;
            f15 = f12 + f5 + f13;
            f16 = 0.0F;
        } else if (flag2) {
            f16 = f8;
            f14 = f8 + f7 + (flag ? 12.5F : f9);
            f15 = f14 + f5 + f13;
        } else {
            f14 = f12;
            f16 = f12 + f5 + f9;
            f15 = f16 + f7 + f8;
        }

        float f17 = Render2D.snapToPixel(first - f15 * f / 2.0F);
        float f18 = Render2D.snapToPixel(second - f6 * f);
        float f19 = f15 * f;
        float f20 = f6 * f;
        float f21 = 10.0F * f;
        graphics.nextStratum();
        Glass.drawHudSurface(graphics, f17, f18, f19, f20, f21, 1.0F, 0.55F * f20 / 39.6F, 1.0F, -2146694128, this.blur.getValue());
        if (flag1) {
            Identifier identifier = ((AbstractClientPlayer)player).getSkin().body().texturePath();
            float f22 = f17 + f16 * f;
            float f23 = f18 + f11 * f;
            float f24 = f7 * f;
            float f25 = 5.0F * f;
            Render2D.blitRoundedTexture(graphics, identifier, f22, f23, f24, f24, f25, 0.125F, 0.125F, 0.25F, 0.25F, -1);
            graphics.nextStratum();
            Render2D.blitRoundedTexture(graphics, identifier, f22, f23, f24, f24, f25, 0.625F, 0.125F, 0.75F, 0.25F, -1);
        }

        int i = 0xFF000000 | player.getTeamColor();
        drawPlayerName(graphics, NAME_FONT, player, f17 + f14 * f, alignCapCenterY(NAME_FONT, f18 + f10 * f, f1), f1, i);
        if (flag) {
            float f26 = f17 + f14 * f;
            HEALTH_FONT.drawScaled(graphics, s1, f26, alignCapCenterY(HEALTH_FONT, f18 + 51.5F * f, f2), f2, -3947581);
            float f28 = f26 + HEALTH_FONT.getWidth(s1, f2) + 7.5F * f;
            float f30 = 20.0F * f;
            float f32 = 18.0F * f;
            Fonts.ICONS.drawIconCentered(graphics, "\ue87d", f28 + f30 / 2.0F, f18 + 50.5F * f + f32 / 2.0F, f30 / 8.3F, -2551027);
        }

        float f27 = 8.5F * f;
        float f29 = f18 + f20 - f27;
        Render2D.fillBottomRoundedProgressBar(graphics, f17, f29, f19, f27, f21, f19, 385875967, 385875967, 1.0F, 1.0F);
        float f31 = Math.max(0.0F, Math.min(1.0F, player.getHealth() / Math.max(1.0F, player.getMaxHealth())));
        if (f31 > 0.001F) {
            Render2D.fillBottomRoundedProgressBar(graphics, f17, f29, f19, f27, f21, f19 * f31, Theme.sampleGradient(0.0F), Theme.sampleGradient(1.0F), 0.7F, 1.0F);
        }
    }

    public static float alignCapCenterY(Fonts font, float first, float second) {
        float f = font.getPointSize() * second;
        return first + f * 0.698F / 2.0F - f / 2.0F;
    }

    public static String formatHealth(float value) {
        float f = Math.round(value * 10.0F) / 10.0F;
        return f == Math.round(f) ? String.valueOf(Math.round(f)) : String.format(Locale.ROOT, "%.1f", f);
    }

    public enum HeadSide implements Displayable {
        LEFT("Left"),
        RIGHT("Right");

        public final String displayName;

        HeadSide(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }
    }

    public enum Style implements Displayable {
        CLASSIC("Classic"),
        CAPSULE("Capsule");

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
