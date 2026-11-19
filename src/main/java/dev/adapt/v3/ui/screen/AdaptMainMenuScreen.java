package dev.adapt.v3.ui.screen;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.account.AccountManager;
import dev.adapt.v3.notification.NotificationType;
// VENDOR-DISABLED: import dev.adapt.v3.security.LaunchGuard;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.LogoReveal;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class AdaptMainMenuScreen extends BaseScreen {
    public static final float CARD_WIDTH = 214.0F;
    public static final float ROW_HEIGHT = 26.0F;
    public static final float CARD_PAD = 6.0F;
    public static final float QUIT_DIVIDER_GAP = 9.0F;
    public static final float CARD_RADIUS = 10.0F;
    public final List<AdaptMainMenuScreen.MenuItem> items = new ArrayList<>();
    public final List<Spring> itemHoverSprings = new ArrayList<>();
    public final Spring highlightYSpring = new Spring(0.0F, 6400.0F, 160.0F);
    public final Spring highlightAlphaSpring = Spring.snappy(0.0F);
    public final Spring enterSpring = new Spring(0.0F, 140.0F, 24.0F);
    public int lastHoveredIndex = -1;
    public static boolean shownUnauthorisedToast;
    public static final String VERSION_TAG = "v3";
    public static final int TAG_GRAD_START = 1723115462;
    public static final int TAG_GRAD_END = 1502577318;
    public static final float TAG_HEIGHT = 13.0F;
    public static final float TAG_PAD_X = 6.0F;
    public static final float TAG_GAP = 6.0F;
    public static final float TAG_RADIUS = 4.0F;
    public static final float WORDMARK_GAP = 12.0F;
    public static final float GREETING_X = 14.0F;
    public static final float GREETING_Y = 13.0F;
    public static final float GREETING_GAP = 3.0F;
    public static final float ACCOUNT_PILL_HEIGHT = 30.0F;
    public static final float ACCOUNT_HEAD_SIZE = 20.0F;
    public static final float ACCOUNT_PILL_RADIUS = 9.0F;
    public float accountPillX;
    public float accountPillY;
    public float accountPillWidth;
    public final Spring accountPillHoverSpring = new Spring(0.0F, 500.0F, 45.0F);
    public static final int TAG_GLASS_TINT = -2146694128;

    public AdaptMainMenuScreen() {
        super("AdaptV3", null);
        this.enterSpring.target(1.0F);
        this.items.add(new AdaptMainMenuScreen.MenuItem("\ue7fd", "Singleplayer", "Worlds", () -> this.openScreen(new SelectWorldScreen(this))));
        this.items.add(new AdaptMainMenuScreen.MenuItem("\ue80b", "Multiplayer", "Servers", () -> this.openScreen(new JoinMultiplayerScreen(this))));
        this.items.add(new AdaptMainMenuScreen.MenuItem("\ue7ef", "Accounts", "Switch", () -> this.openScreen(new AccountsScreen(this))));
        this.items.add(new AdaptMainMenuScreen.MenuItem("\ue8b8", "Options", "", () -> this.openScreen(new OptionsScreen(this, this.minecraft.options))));
        this.items.add(new AdaptMainMenuScreen.MenuItem("\ue8ac", "Quit", "", () -> this.minecraft.stop()));

        for (int i = 0; i < this.items.size(); i++) {
            this.itemHoverSprings.add(Spring.snappy(0.0F));
        }

        Adapt.getInstance()
            .getAccountManager()
            .tryAutoLogin(
                account -> Adapt.getInstance()
                    .getNotificationManager()
                    .builder(NotificationType.SUCCESS)
                    .title("Welcome back")
                    .message("Signed in as " + account.name)
                    .duration(2200)
                    .post()
            );
        // VENDOR-DISABLED: if (!LaunchGuard.isAuthorised() && !shownUnauthorisedToast) { shownUnauthorisedToast = true; LaunchGuard.notifySessionEnded(); }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            return true;
        }

        if (event.key() == 229) {
            // VENDOR-DISABLED: if (LaunchGuard.isAuthorised()) ... else LaunchGuard.notifySessionEnded();
            this.openScreen(new DashboardScreen(this));

            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int value, int value2, float value3) {
        this.renderNebulaOrPanorama(graphics, value3);
        Render2D.fillVerticalGradient(graphics, 0.0F, this.height * 0.55F, this.width, this.height * 0.45F, 0, 805306368);
        Render2D.fillVerticalGradient(graphics, 0.0F, 0.0F, this.width, this.height * 0.25F, 469762048, 0);
    }

    public boolean isQuitRow(int value) {
        return value == this.items.size() - 1;
    }

    public float getCardHeight() {
        return 12.0F + this.items.size() * 26.0F + 9.0F;
    }

    public float getWordmarkBlockHeight() {
        return Fonts.DISPLAY.getPointSize() + 12.0F;
    }

    public float getContentTopY() {
        return this.height / 2.0F - (this.getWordmarkBlockHeight() + this.getCardHeight()) / 2.0F;
    }

    public float getCardX() {
        return this.width / 2.0F - 107.0F;
    }

    public float getCardY() {
        return this.getContentTopY() + this.getWordmarkBlockHeight();
    }

    public float getRowY(int value) {
        float f = this.getCardY() + 6.0F + value * 26.0F;
        if (value >= this.items.size() - 1) {
            f += 9.0F;
        }

        return f;
    }

    public float staggerAlpha(float alpha, int color) {
        return Math.max(0.0F, Math.min(1.0F, alpha * 1.6F - color * 0.08F));
    }

    @Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, float alpha) {
        float f = Math.max(0.0F, this.enterSpring.update());
        float f1 = this.width / 2.0F;
        float f2 = this.staggerAlpha(f, 0);
        float f3 = this.getContentTopY() + Fonts.DISPLAY.getPointSize() / 2.0F + (1.0F - f2) * 6.0F;
        float f4 = Fonts.DISPLAY.getWidth("Adapt");
        float f5 = Fonts.SEMIBOLD.getWidth("v3") + 12.0F;
        float f6 = f4 + 6.0F + f5;
        float f7 = f1 - f6 / 2.0F + f4 / 2.0F;
        Render2D.drawBlob(
            graphics,
            f7 - f4 * 0.85F,
            f3 - Fonts.DISPLAY.getPointSize() * 0.85F,
            f4 * 1.7F,
            Fonts.DISPLAY.getPointSize() * 1.7F,
            ColorUtility.scaleAlpha(1426063360, f2)
        );
        float f8 = f7 - f4 / 2.0F;
        float f9 = Fonts.DISPLAY.getWidth("A");
        LogoReveal.reportTargetLogoBounds(f8 + f9 / 2.0F, f3, Fonts.DISPLAY.getCapHeight());
        Fonts.DISPLAY.drawVCentered(graphics, "A", f8, f3, ColorUtility.scaleAlpha(-1, f2 * LogoReveal.getTargetLogoAlpha()));
        Fonts.DISPLAY.drawVCentered(graphics, "dapt", f8 + f9, f3, ColorUtility.scaleAlpha(-1, f2));
        float f10 = this.staggerAlpha(f, 1);
        float f11 = Render2D.snapToPixel(f7 + f4 / 2.0F + 6.0F);
        float f12 = Render2D.snapToPixel(f3 - Fonts.DISPLAY.getPointSize() * 0.36F + (1.0F - f10) * 3.0F);
        renderBadgeGlass(graphics, f11, f12, f5, 13.0F, 4.0F, f10, 0.45F);
        Render2D.fillHorizontalRoundedGradient(graphics, f11, f12, f5, 13.0F, 4.0F, 1723115462, 1502577318, f10);
        Fonts.SEMIBOLD.drawCentered(graphics, "v3", f11 + f5 / 2.0F, f12 + 6.5F, ColorUtility.scaleAlpha(-218103809, f10));
        float f13 = this.staggerAlpha(f, 2);
        float f14 = this.getCardX();
        float f15 = this.getCardY() + (1.0F - f13) * 8.0F;
        float f16 = f15 - this.getCardY();
        Glass.drawSheenPanel(graphics, f14, f15, 214.0F, this.getCardHeight(), 10.0F, f13, 1.0F);
        int i = -1;

        for (int j = 0; j < this.items.size(); j++) {
            if (Render2D.containsPoint(mouseX, mouseY, f14 + 6.0F, this.getRowY(j), 202.0F, 26.0F)) {
                i = j;
            }
        }

        if (i >= 0) {
            if (this.lastHoveredIndex < 0) {
                this.highlightYSpring.snap(this.getRowY(i));
            }

            this.highlightYSpring.target(this.getRowY(i));
            this.lastHoveredIndex = i;
        }

        float f22 = Math.max(0.0F, Math.min(1.0F, this.highlightAlphaSpring.target(i >= 0 ? 1.0F : 0.0F).update()));
        float f17 = this.highlightYSpring.update() + f16;
        if (f22 > 0.01F) {
            Render2D.fillRoundedRect(graphics, f14 + 6.0F, f17, 202.0F, 26.0F, 6.0F, ColorUtility.scaleAlpha(536870911, f13 * f22));
        } else {
            this.lastHoveredIndex = -1;
        }

        for (int k = 0; k < this.items.size(); k++) {
            AdaptMainMenuScreen.MenuItem adaptmainmenuscreen$menuitem = this.items.get(k);
            float f18 = f13 * this.staggerAlpha(f, 3 + k);
            float f19 = this.getRowY(k) + f16;
            float f20 = Math.max(0.0F, Math.min(1.0F, this.itemHoverSprings.get(k).target(k == i ? 1.0F : 0.0F).update()));
            if (this.isQuitRow(k)) {
                Render2D.fillRect(graphics, f14 + 6.0F + 8.0F, f19 - 4.5F - 0.25F, 186.0F, 0.5F, ColorUtility.scaleAlpha(452984831, f18));
            }

            float f21 = f19 + 13.0F;
            boolean flag = k == this.items.size() - 1;
            int l = flag ? ColorUtility.lerpColor(-1711276033, -1027478, f20) : ColorUtility.lerpColor(-1711276033, Theme.getAccentColor(), f20);
            Fonts.ICONS.drawIconCentered(graphics, adaptmainmenuscreen$menuitem.icon(), f14 + 6.0F + 14.0F, f21, 1.0F, ColorUtility.scaleAlpha(l, f18));
            Fonts.REGULAR
                .drawVCentered(
                    graphics,
                    adaptmainmenuscreen$menuitem.label(),
                    f14 + 6.0F + 28.0F + f20 * 1.5F,
                    f21,
                    ColorUtility.scaleAlpha(ColorUtility.lerpColor(-637534209, -1, f20), f18)
                );
            if (!adaptmainmenuscreen$menuitem.hint().isEmpty()) {
                Fonts.SMALL
                    .drawVCentered(
                        graphics,
                        adaptmainmenuscreen$menuitem.hint(),
                        f14 + 214.0F - 6.0F - 10.0F - Fonts.SMALL.getWidth(adaptmainmenuscreen$menuitem.hint()),
                        f21,
                        ColorUtility.scaleAlpha(1509949439, f18)
                    );
            }
        }

        float f23 = this.staggerAlpha(f, 2);
        this.renderAccountPill(graphics, mouseX, mouseY, f23);
        this.renderGreeting(graphics, f23);
        Fonts.SMALL.drawVCentered(graphics, "© 2026 Adapt. All rights reserved.", 12.0F, this.height - 11, ColorUtility.scaleAlpha(1946157055, f23));
        String s = "Minecraft " + SharedConstants.getCurrentVersion().name();
        Fonts.SMALL.drawVCentered(graphics, s, this.width - 12 - Fonts.SMALL.getWidth(s), this.height - 11, ColorUtility.scaleAlpha(1946157055, f23));
    }

    public void renderGreeting(GuiGraphicsExtractor graphics, float value) {
        if (!(value <= 0.01F)) {
            String s = null; // VENDOR-DISABLED: LaunchGuard.getAccountName();
            String s1 = s != null && !s.isBlank() ? s : this.minecraft.getUser().getName();
            LocalDateTime localdatetime = LocalDateTime.now();
            String s2 = getTimeOfDayGreeting(localdatetime.getHour()) + ", " + s1;
            String s3 = getDaySubtitle(localdatetime.getDayOfWeek());
            float f = 14.0F - (1.0F - value) * 6.0F;
            float f1 = 13.0F + Fonts.GREETING.getPointSize() / 2.0F;
            float f2 = 13.0F + Fonts.GREETING.getPointSize() + 3.0F + Fonts.GREETING_SUB.getPointSize() / 2.0F;
            float f3 = Math.max(Fonts.GREETING.getWidth(s2), Fonts.GREETING_SUB.getWidth(s3));
            float f4 = f2 + Fonts.GREETING_SUB.getPointSize() / 2.0F - 13.0F;
            Render2D.drawBlob(graphics, f - 24.0F, -5.0F, f3 + 48.0F, f4 + 36.0F, ColorUtility.scaleAlpha(1073741824, value));
            Fonts.GREETING.drawVCentered(graphics, s2, f, f1, ColorUtility.scaleAlpha(-1, value));
            Fonts.GREETING_SUB.drawVCentered(graphics, s3, f + 1.0F, f2, ColorUtility.scaleAlpha(-1929379841, value));
        }
    }

    public static String getTimeOfDayGreeting(int value) {
        if (value >= 5 && value < 12) {
            return "Good morning";
        } else if (value >= 12 && value < 17) {
            return "Good afternoon";
        } else {
            return value >= 17 ? "Good evening" : "Up late";
        }
    }

    public static String getDaySubtitle(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case FRIDAY -> "Welcome back. Happy Friday";
            case SATURDAY, SUNDAY -> "Welcome back. Enjoy your weekend";
            default -> "Welcome back";
        };
    }

    public void renderAccountPill(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!(partialTick <= 0.01F)) {
            AccountManager accountmanager = Adapt.getInstance().getAccountManager();
            User user = this.minecraft.getUser();
            Optional<AccountManager.Account> optional = accountmanager.getActiveAccount();
            String s = user.getName();
            String s1 = optional.<String>map(account -> account.type.label + " account").orElse("Launcher session");
            boolean flag = optional.isPresent();
            float f = 5.0F;
            float f1 = Math.max(Fonts.MEDIUM.getWidth(s), Fonts.SMALL.getWidth(s1) + 8.0F);
            float f2 = Render2D.snapToPixel(33.0F + f1 + 12.0F + 12.0F);
            float f3 = Render2D.snapToPixel(this.width - f2 - 12.0F);
            float f4 = 12.0F;
            this.accountPillX = f3;
            this.accountPillY = 12.0F;
            this.accountPillWidth = f2;
            float f5 = Math.max(
                0.0F, Math.min(1.0F, this.accountPillHoverSpring.target(Render2D.containsPoint(mouseX, mouseY, f3, 12.0F, f2, 30.0F) ? 1.0F : 0.0F).update())
            );
            Glass.drawSheenPanel(graphics, f3, 12.0F, f2, 30.0F, 9.0F, partialTick, 0.45F);
            if (f5 > 0.01F) {
                Render2D.fillRoundedRect(graphics, f3, 12.0F, f2, 30.0F, 9.0F, ColorUtility.scaleAlpha(536870911, partialTick * f5));
            }

            graphics.nextStratum();
            float f6 = f3 + 5.0F;
            float f7 = 17.0F;
            Render2D.drawBlob(graphics, f6 - 8.0F, 11.0F, 36.0F, 32.0F, ColorUtility.scaleAlpha(1493172224, partialTick));
            Render2D.drawDropShadow(graphics, f6, 18.0F, 20.0F, 20.0F, Render2D.getHeadCornerRadius(20.0F), 3.0F, ColorUtility.scaleAlpha(1711276032, partialTick));
            graphics.nextStratum();
            Render2D.drawPlayerHead(graphics, accountmanager.getSkinTexture(user.getProfileId()), f6, 17.0F, 20.0F, ColorUtility.scaleAlpha(-1, partialTick));
            graphics.nextStratum();
            float f8 = f6 + 20.0F + 8.0F;
            float f9 = 27.0F;
            Fonts.MEDIUM.drawVCentered(graphics, s, f8, 22.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-419430401, -1, f5), partialTick));
            Render2D.fillCircleAligned(graphics, f8 + 2.0F, 33.0F, 1.8F, ColorUtility.scaleAlpha(flag ? -14498421 : -2130706433, partialTick));
            Fonts.SMALL.drawVCentered(graphics, s1, f8 + 7.0F, 33.0F, ColorUtility.scaleAlpha(1946157055, partialTick));
            float f10 = f3 + f2 - 13.0F;
            graphics.pose().pushMatrix();
            graphics.pose().translate((1.0F - f5) * -2.5F, 0.0F);
            Fonts.ICONS
                .drawIconCentered(
                    graphics, "\ue8d4", f10, 27.0F, 0.9F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(1090519039, Theme.getAccentColor(), f5), partialTick)
                );
            graphics.pose().popMatrix();
        }
    }

    public static void renderBadgeGlass(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float first, float second) {
        Glass.drawPanel(graphics, x, y, width, height, radius, first, second, 1.0F, -2146694128);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double d0 = event.x();
        double d1 = event.y();
        if (Render2D.containsPoint(d0, d1, this.accountPillX, this.accountPillY, this.accountPillWidth, 30.0F)) {
            this.openScreen(new AccountsScreen(this));
            return true;
        }

        for (int i = 0; i < this.items.size(); i++) {
            if (Render2D.containsPoint(d0, d1, this.getCardX() + 6.0F, this.getRowY(i), 202.0F, 26.0F)) {
                this.onUiFeedback(1.0F);
                this.items.get(i).action().run();
                return true;
            }
        }

        return true;
    }

    private record MenuItem(String icon, String label, String hint, Runnable action) {
    }
}
