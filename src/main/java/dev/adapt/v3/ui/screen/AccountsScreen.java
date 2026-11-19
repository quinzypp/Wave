package dev.adapt.v3.ui.screen;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.util.UndashedUuid;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.account.AccountManager;
import dev.adapt.v3.account.MicrosoftAuth;
import dev.adapt.v3.notification.NotificationType;
import dev.adapt.v3.ui.Widgets;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class AccountsScreen extends BaseScreen {
    public static final float MARGIN = 18.0F;
    public static final float TOP_BAR_HEIGHT = 26.0F;
    public static final float PANEL_GAP = 10.0F;
    public static final float CARD_HEIGHT = 50.0F;
    public static final float CARD_GAP = 8.0F;
    public static final float MIN_CARD_WIDTH = 158.0F;
    public static final float CARD_PAD = 10.0F;
    public static final int DANGER_COLOR = -1027478;
    public static final int ACTIVE_COLOR = -14498421;
    public static final String KEY_LAUNCHER = "launcher";
    public static final String KEY_ADD = "add";
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());
    public final AccountManager accountManager = Adapt.getInstance().getAccountManager();
    public final Spring enterSpring = new Spring(0.0F, 140.0F, 24.0F);
    public final Map<String, Spring> snappySprings = new HashMap<>();
    public final Map<String, Spring> flashSprings = new HashMap<>();
    public final Spring scrollSpring = Spring.smooth(0.0F);
    public final Spring modalSpring = new Spring(0.0F, 340.0F, 37.0F);
    public final Spring searchFocusSpring = new Spring(0.0F, 300.0F, 35.0F);
    public final Spring autoLoginSpring;
    public final Spring linkStepSpring = Spring.snappy(0.0F);
    public float scrollTarget;
    public float contentHeight;
    public float viewportHeight;
    public final StringBuilder searchQuery = new StringBuilder();
    public boolean searchFocused;
    public final List<AccountsScreen.Slot> slots = new ArrayList<>();
    public AccountsScreen.Rect backButtonRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect searchBarRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect addButtonRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect listViewportRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect uuidCopyRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect removeCurrentRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect autoLoginToggleRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect tokenInputRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect modalCardRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect modalPrimaryLeftRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect modalPrimaryRightRect = AccountsScreen.Rect.NONE;
    public AccountsScreen.Rect modalAuxButtonRect = AccountsScreen.Rect.NONE;
    public String signingInKey;
    public AccountsScreen.Modal activeModal = AccountsScreen.Modal.NONE;
    public AccountsScreen.Modal renderedModal = AccountsScreen.Modal.NONE;
    public AccountManager.Account accountToRemove;
    public CompletableFuture<AccountManager.Account> linkFuture;
    public String linkUrl;
    public AccountsScreen.LinkState linkState = AccountsScreen.LinkState.WAITING;
    public String linkMessage = "";
    public long linkSuccessAt;
    public boolean linkWasNewAccount;
    public boolean linkBrowserOpened;
    public final StringBuilder tokenInput = new StringBuilder();
    public boolean tokenChecking;
    public String tokenError;
    public final Map<String, Spring> hoverSprings = new HashMap<>();
    public final List<Long> searchCharTimes = new ArrayList<>();
    public final List<AccountsScreen.DropChar> searchDroppedChars = new ArrayList<>();
    public final Spring searchScrollSpring = new Spring(0.0F, 400.0F, 40.0F);
    public final Spring searchCaretSpring = new Spring(0.0F, 700.0F, 53.0F);
    public boolean searchCaretInit;
    public final Spring searchTypedSpring = new Spring(0.0F, 300.0F, 35.0F);
    public long searchLastEditAt;
    public static final float DRAG_THRESHOLD = 4.0F;
    public String pressedSlotKey;
    public String draggedKey;
    public double pressMouseX;
    public double pressMouseY;
    public boolean draggingCard;
    public float dragOffsetX;
    public float dragOffsetY;
    public float dragCardWidth;
    public int dragSlotIndex;
    public AccountManager.Account draggedAccount;
    public final Spring dragLiftSpring = Spring.snappy(0.0F);
    public double lastMouseX;
    public double lastMouseY;
    public final Map<String, Spring> cardXSprings = new HashMap<>();
    public final Map<String, Spring> cardYSprings = new HashMap<>();
    public final Map<String, Spring> cardPresenceSprings = new HashMap<>();
    public final Map<String, AccountsScreen.Rect> renderedCardRects = new HashMap<>();
    public final Map<String, AccountsScreen.Exiting> exitingCards = new HashMap<>();
    public boolean initialLayoutDone;
    public float lastScrollY;
    public final Map<String, Spring> signingInSprings = new HashMap<>();
    public final Map<String, Spring> switchedPulseSprings = new HashMap<>();
    public static final int CARD_GLASS_TINT = 856427536;
    public final Spring modalWidthSpring = new Spring(0.0F, 400.0F, 40.0F);
    public final Spring modalHeightSpring = new Spring(0.0F, 400.0F, 40.0F);
    public final Spring modalContentAlphaSpring = new Spring(1.0F, 400.0F, 40.0F);
    public boolean snapModalSize;

    public AccountsScreen(Screen screen) {
        super("Accounts", screen);
        this.accountManager.getInitialUser();
        this.enterSpring.target(1.0F);
        this.autoLoginSpring = Spring.snappy(this.accountManager.isAutoLogin() ? 1.0F : 0.0F);
    }

    @Override
    public boolean usesTextInput() {
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int value, int value2, float value3) {
        if (this.minecraft.level != null) {
            super.extractBackground(graphics, value, value2, value3);
        } else {
            this.renderNebulaOrPanorama(graphics, value3);
            Render2D.fillVerticalGradient(graphics, 0.0F, this.height * 0.55F, this.width, this.height * 0.45F, 0, 805306368);
            Render2D.fillVerticalGradient(graphics, 0.0F, 0.0F, this.width, this.height * 0.25F, 469762048, 0);
            graphics.nextStratum();
        }
    }

    public Spring getHoverSpring(String text) {
        return this.hoverSprings.computeIfAbsent(text, text2 -> new Spring(0.0F, 750.0F, 55.0F));
    }

    public Spring getSnappySpring(String text) {
        return this.snappySprings.computeIfAbsent(text, text2 -> Spring.snappy(0.0F));
    }

    public Spring getFlashSpring(String text) {
        return this.flashSprings.computeIfAbsent(text, text2 -> new Spring(0.0F, 60.0F, 9.0F));
    }

    public float staggerAlpha(float alpha, float alpha2) {
        return Math.max(0.0F, Math.min(1.0F, alpha * 1.7F - alpha2 * 0.07F));
    }

    @Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, float alpha) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        float f = Math.max(0.0F, Math.min(Math.min(1.0F, this.enterSpring.update()), alpha));
        boolean flag = this.activeModal != AccountsScreen.Modal.NONE;
        int i = flag ? -1000 : mouseX;
        int j = flag ? -1000 : mouseY;
        this.renderTopBar(graphics, i, j, f);
        float f1 = 58.0F;
        float f2 = this.height - 18.0F - 14.0F;
        float f3 = Math.max(170.0F, Math.min(222.0F, this.width * 0.24F));
        this.renderCurrentAccountPanel(graphics, 18.0F, 58.0F, f3, f2 - 58.0F, i, j, this.staggerAlpha(f, 1.0F));
        float f4 = 18.0F + f3 + 10.0F;
        this.renderSavedAccountsPanel(graphics, f4, 58.0F, this.width - f4 - 18.0F, f2 - 58.0F, i, j, f);
        Fonts.SMALL
            .drawVCentered(graphics, "Saved accounts are stored only on this device", 18.0F, this.height - 18.0F + 2.0F, ColorUtility.scaleAlpha(1308622847, f));
        String s = this.accountManager.getAccounts().size() + (this.accountManager.getAccounts().size() == 1 ? " saved account" : " saved accounts");
        Fonts.SMALL.drawVCentered(graphics, s, this.width - 18.0F - Fonts.SMALL.getWidth(s), this.height - 18.0F + 2.0F, ColorUtility.scaleAlpha(1308622847, f));
        this.renderModalOverlay(graphics, mouseX, mouseY, alpha);
    }

    public void renderTopBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float f = 18.0F - (1.0F - partialTick) * 6.0F;
        this.backButtonRect = new AccountsScreen.Rect(18.0F, 18.0F, 62.0F, 26.0F);
        float f1 = this.getSnappySpring("back").target(this.backButtonRect.contains(mouseX, mouseY) ? 1.0F : 0.0F).update();
        Glass.drawSheenPanel(graphics, 18.0F, f, 62.0F, 26.0F, 13.0F, partialTick, 0.4F);
        Render2D.fillRoundedRect(graphics, 18.0F, f, 62.0F, 26.0F, 13.0F, ColorUtility.scaleAlpha(452984831, partialTick * f1));
        float f2 = f + 13.0F;
        withPoseTransform(
            graphics,
            -f1 * 1.5F,
            0.0F,
            0.0F,
            0.0F,
            1.0F,
            () -> Fonts.ICONS.drawIconCentered(graphics, "\ue5cb", 32.0F, f2, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-855638017, -1, f1), partialTick))
        );
        Fonts.REGULAR.drawVCentered(graphics, "Back", 41.0F, f + 13.0F, ColorUtility.scaleAlpha(-419430401, partialTick));
        float f3 = 94.0F;
        float f4 = 118.0F;
        this.addButtonRect = new AccountsScreen.Rect(this.width - 18.0F - 118.0F, 18.0F, 118.0F, 26.0F);
        float f5 = this.getHoverSpring("addBtn").target(this.addButtonRect.contains(mouseX, mouseY) ? 1.0F : 0.0F).update();
        float f6 = Render2D.snapToPixel(f - f5 * 1.5F);
        float f7 = 13.0F;
        Glass.drawSheenPanel(graphics, this.addButtonRect.x(), f6, 118.0F, 26.0F, 13.0F, partialTick, 0.3F, 0.5F);
        Render2D.fillSubpixelRoundedRect(
            graphics, this.addButtonRect.x(), f6, 118.0F, 26.0F, 13.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(184549375, 352321535, f5), partialTick)
        );
        int i = 71 + (int)(40.0F * f5);
        if (Theme.hasGradient()) {
            Render2D.fillHorizontalRoundedGradient(
                graphics,
                this.addButtonRect.x(),
                f6,
                118.0F,
                26.0F,
                13.0F,
                ColorUtility.withAlpha(Theme.sampleGradient(0.0F), i),
                ColorUtility.withAlpha(Theme.sampleGradient(1.0F), i),
                partialTick
            );
        } else {
            Render2D.fillSubpixelRoundedRect(graphics, this.addButtonRect.x(), f6, 118.0F, 26.0F, 13.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(i), partialTick));
        }

        String s = "Add account";
        float f8 = Fonts.MEDIUM.getWidth("Add account") + 16.0F;
        float f9 = this.addButtonRect.x() + 59.0F - f8 / 2.0F;
        Fonts.ICONS.drawIconCentered(graphics, "\ue145", f9 + 5.0F, f6 + 13.0F, 1.0F, ColorUtility.scaleAlpha(-1, partialTick));
        Fonts.MEDIUM.drawVCentered(graphics, "Add account", f9 + 16.0F, f6 + 13.0F, ColorUtility.scaleAlpha(-1, partialTick));
        float f10 = Fonts.TITLE.getWidth("Accounts");
        float f11 = Math.max(90.0F, Math.min(Math.min(200.0F, Math.max(120.0F, this.width * 0.18F)), this.addButtonRect.x() - 8.0F - (94.0F + f10 + 14.0F)));
        this.searchBarRect = new AccountsScreen.Rect(this.addButtonRect.x() - 8.0F - f11, 18.0F, f11, 26.0F);
        String s1 = "Click an account to switch to it";
        boolean flag = this.searchBarRect.x() - 10.0F > 94.0F + Fonts.SMALL.getWidth("Click an account to switch to it");
        Fonts.TITLE.drawVCentered(graphics, "Accounts", 94.0F, f + 13.0F - (flag ? 5 : 0), ColorUtility.scaleAlpha(-1, partialTick));
        if (flag) {
            Fonts.SMALL.drawVCentered(graphics, "Click an account to switch to it", 94.0F, f + 13.0F + 7.0F, ColorUtility.scaleAlpha(1946157055, partialTick));
        }

        float f12 = Math.max(0.0F, Math.min(1.0F, this.searchFocusSpring.target(!this.searchFocused && this.searchQuery.length() <= 0 ? 0.0F : 1.0F).update()));
        float f13 = Math.max(0.0F, Math.min(1.0F, this.getHoverSpring("searchHover").target(this.searchBarRect.contains(mouseX, mouseY) ? 1.0F : 0.0F).update()));
        float f14 = f + 13.0F;
        Glass.drawSheenPanel(graphics, this.searchBarRect.x(), f, f11, 26.0F, 13.0F, partialTick, 0.4F);
        Render2D.fillRoundedRect(graphics, this.searchBarRect.x(), f, f11, 26.0F, 13.0F, ColorUtility.scaleAlpha(268435455, partialTick * Math.max(f13, f12)));
        if (f12 > 0.01F) {
            Render2D.drawRoundedBorderTextured(
                graphics, this.searchBarRect.x(), f, f11, 26.0F, 13.0F, 1.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha((int)(150.0F * f12)), partialTick)
            );
        }

        Fonts.ICONS
            .drawIconCentered(
                graphics,
                "\ue8b6",
                this.searchBarRect.x() + 14.0F,
                f14,
                1.0F,
                ColorUtility.scaleAlpha(ColorUtility.lerpColor(-2130706433, Theme.getAccentColor(), f12), partialTick)
            );
        String s2 = this.searchQuery.toString();
        float f15 = this.searchBarRect.x() + 26.0F;
        float f16 = Math.max(0.0F, Math.min(1.0F, this.searchTypedSpring.target(s2.isEmpty() ? 0.0F : 1.0F).update()));
        if (f16 < 0.99F) {
            int j = ColorUtility.scaleAlpha(ColorUtility.lerpColor(1728053247, 1308622847, f12), partialTick * (1.0F - f16));
            withPoseTransform(graphics, f12 * 4.0F, 0.0F, 0.0F, 0.0F, 1.0F, () -> Fonts.REGULAR.drawVCentered(graphics, "Search accounts", f15, f14, j));
        }

        long i1 = System.currentTimeMillis();
        float f17 = Fonts.REGULAR.getWidth(s2);
        float f18 = f11 - 40.0F;
        float f19 = this.searchScrollSpring.target(Math.max(0.0F, f17 - f18)).update();
        graphics.enableScissor((int)(f15 - 2.0F), (int)f, (int)(this.searchBarRect.x() + f11 - 12.0F), (int)(f + 26.0F));

        while (this.searchCharTimes.size() > s2.length()) {
            this.searchCharTimes.remove(this.searchCharTimes.size() - 1);
        }

        while (this.searchCharTimes.size() < s2.length()) {
            this.searchCharTimes.add(0L);
        }

        for (int k = 0; k < s2.length(); k++) {
            float f20 = Math.max(0.0F, Math.min(1.0F, (float)(i1 - this.searchCharTimes.get(k)) / 170.0F));
            float f21 = 1.0F - (1.0F - f20) * (1.0F - f20) * (1.0F - f20);
            String s3 = s2.substring(k, k + 1);
            float f22 = f15 - f19 + Fonts.REGULAR.getWidth(s2.substring(0, k));
            float f23 = Fonts.REGULAR.getWidth(s3);
            int l = ColorUtility.scaleAlpha(-218103809, partialTick * f21);
            withPoseTransform(
                graphics, 0.0F, (1.0F - f21) * 4.0F, f22 + f23 / 2.0F, f14, 0.85F + 0.15F * f21, () -> Fonts.REGULAR.drawVCentered(graphics, s3, f22, f14, l)
            );
        }

        this.searchDroppedChars.removeIf(dropChar -> i1 - dropChar.at() > 150L);

        for (AccountsScreen.DropChar accountsscreen$dropchar : this.searchDroppedChars) {
            float f26 = Math.max(0.0F, Math.min(1.0F, (float)(i1 - accountsscreen$dropchar.at()) / 150.0F));
            float f27 = f15 - f19 + accountsscreen$dropchar.x();
            int k1 = ColorUtility.scaleAlpha(-218103809, partialTick * (1.0F - f26));
            withPoseTransform(
                graphics,
                0.0F,
                f26 * 3.0F,
                f27 + Fonts.REGULAR.getWidth(accountsscreen$dropchar.ch()) / 2.0F,
                f14,
                1.0F - 0.15F * f26,
                () -> Fonts.REGULAR.drawVCentered(graphics, accountsscreen$dropchar.ch(), f27, f14, k1)
            );
        }

        graphics.disableScissor();
        float f24 = s2.isEmpty() ? f15 - 1.0F : f15 - f19 + f17 + 1.0F;
        if (!this.searchCaretInit) {
            this.searchCaretSpring.snap(f24);
            this.searchCaretInit = true;
        }

        float f25 = this.searchCaretSpring.target(f24).update();
        if (f12 > 0.01F && this.searchFocused) {
            long j1 = i1 - this.searchLastEditAt;
            float f28 = j1 < 500L ? 1.0F : Math.max(0.0F, Math.min(1.0F, 0.5F + 1.4F * (float)Math.cos((j1 - 500L) / 1060.0 * Math.PI * 2.0)));
            Render2D.fillRect(graphics, f25, f14 - 5.0F, 0.75F, 10.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), partialTick * f12 * f28));
        }
    }

    public void appendSearchChar(String text) {
        this.searchQuery.append(text);
        this.searchCharTimes.add(System.currentTimeMillis());
        this.searchLastEditAt = System.currentTimeMillis();
    }

    public void backspaceSearch() {
        if (this.searchQuery.length() != 0) {
            int i = this.searchQuery.length() - 1;
            this.searchDroppedChars
                .add(
                    new AccountsScreen.DropChar(
                        this.searchQuery.substring(i), Fonts.REGULAR.getWidth(this.searchQuery.substring(0, i)), System.currentTimeMillis()
                    )
                );
            this.searchQuery.setLength(i);
            if (!this.searchCharTimes.isEmpty()) {
                this.searchCharTimes.remove(this.searchCharTimes.size() - 1);
            }

            this.searchLastEditAt = System.currentTimeMillis();
        }
    }

    public void clearSearch() {
        long i = System.currentTimeMillis();

        for (int j = 0; j < this.searchQuery.length(); j++) {
            this.searchDroppedChars
                .add(new AccountsScreen.DropChar(this.searchQuery.substring(j, j + 1), Fonts.REGULAR.getWidth(this.searchQuery.substring(0, j)), i));
        }

        this.searchQuery.setLength(0);
        this.searchCharTimes.clear();
    }

    public void renderCurrentAccountPanel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        float f = y + (1.0F - second) * 10.0F;
        Glass.drawSheenPanel(graphics, x, f, width, height, 10.0F, second, 1.0F);
        User user = this.minecraft.getUser();
        Optional<AccountManager.Account> optional = this.accountManager.getActiveAccount();
        boolean flag = optional.isEmpty();
        float f1 = x + width / 2.0F;
        float f2 = (float)System.currentTimeMillis() / 1000.0F;
        float f3 = (float)Math.sin(f2 * 1.6F) * 1.6F;
        float f4 = Math.max(26.0F, Math.min(52.0F, (height - 120.0F) * 0.45F));
        float f5 = f4 >= 40.0F ? 20.0F : 12.0F;
        float f6 = f + f5 + f4 / 2.0F;
        Render2D.drawBlob(graphics, f1 - f4 * 1.5F, f6 - f4 * 1.2F, f4 * 3.0F, f4 * 2.4F, ColorUtility.scaleAlpha(1929379840, second));
        Render2D.drawBlob(graphics, f1 - f4 * 0.7F, f6 + f4 * 0.35F, f4 * 1.4F, f4 * 0.5F, ColorUtility.scaleAlpha(1711276032, second));
        graphics.nextStratum();
        float f7 = Render2D.snapToPixel(f1 - f4 / 2.0F);
        float f8 = Render2D.snapToPixel(f6 - f4 / 2.0F + f3);
        Render2D.drawDropShadow(graphics, f7, f8 + 3.0F, f4, f4, getHeadCornerRadius(f4), 10.0F, ColorUtility.scaleAlpha(Integer.MIN_VALUE, second));
        graphics.nextStratum();
        drawPlayerHead(graphics, this.accountManager.getSkinTexture(user.getProfileId()), f7, f8, f4, second);
        graphics.nextStratum();
        Render2D.drawRoundedBorderTextured(
            graphics, f7 - 1.0F, f8 - 1.0F, f4 + 2.0F, f4 + 2.0F, getHeadCornerRadius(f4) + 1.0F, 1.0F, ColorUtility.scaleAlpha(654311423, second)
        );
        float f9 = f + f5 + f4 + 17.0F;
        Fonts.TITLE.drawCentered(graphics, Fonts.TITLE.ellipsize(user.getName(), width - 20.0F), f1, f9, ColorUtility.scaleAlpha(-1, second));
        f9 += 17.0F;
        String s = flag ? "Launcher session" : ((AccountManager.Account)optional.get()).type.label + " account";
        float f10 = Fonts.SMALL.getWidth(s) + 20.0F;
        float f11 = 13.0F;
        Render2D.fillRoundedRect(
            graphics, f1 - f10 / 2.0F, f9 - 6.5F, f10, 13.0F, 6.5F, ColorUtility.scaleAlpha(flag ? 452984831 : Theme.getAccentWithAlpha(40), second)
        );
        float f12 = 0.6F + 0.4F * (float)Math.abs(Math.sin(f2 * 2.0F));
        Render2D.fillCircleAligned(graphics, f1 - f10 / 2.0F + 8.0F, f9, 2.2F, ColorUtility.scaleAlpha(flag ? -1275068417 : -14498421, second * f12));
        Fonts.SMALL.drawVCentered(graphics, s, f1 - f10 / 2.0F + 13.0F, f9, ColorUtility.scaleAlpha(-637534209, second));
        f9 += 16.0F;
        String s1 = UndashedUuid.toString(user.getProfileId());
        String s2 = s1.substring(0, 8) + "…" + s1.substring(s1.length() - 4);
        float f13 = Fonts.SMALL.getWidth(s2) + 16.0F;
        this.uuidCopyRect = new AccountsScreen.Rect(f1 - f13 / 2.0F, f9 - 6.0F, f13, 12.0F);
        float f14 = this.getSnappySpring("uuid").target(this.uuidCopyRect.contains(first, startColor) ? 1.0F : 0.0F).update();
        Fonts.SMALL.drawVCentered(graphics, s2, this.uuidCopyRect.x(), f9, ColorUtility.scaleAlpha(ColorUtility.lerpColor(1509949439, -855638017, f14), second));
        Fonts.ICONS
            .drawIconCentered(
                graphics,
                "\ue14d",
                this.uuidCopyRect.x() + f13 - 5.0F,
                f9,
                0.7F,
                ColorUtility.scaleAlpha(ColorUtility.lerpColor(1509949439, Theme.getAccentColor(), f14), second)
            );
        f9 += 12.0F;
        float f15 = f + height - 10.0F;
        float f16 = 32.0F;
        float f17 = f15 - f9;
        boolean flag1 = f17 >= 36.0F;
        if (flag1) {
            f17 -= 40.0F;
        }

        boolean flag2 = !flag && f17 >= 28.0F;
        if (flag2) {
            f17 -= 28.0F;
        }

        float f18 = 18.0F;
        int i = (int)Math.max(0.0F, Math.min(3.0F, (f17 - 8.0F) / 18.0F));
        if (i > 0) {
            Render2D.fillRect(graphics, x + 14.0F, f9, width - 28.0F, 0.5F, ColorUtility.scaleAlpha(452984831, second));
            f9 += 6.0F;
            this.renderInfoRow(graphics, x, width, f9, "\ue9e0", "Type", flag ? "Launcher" : ((AccountManager.Account)optional.get()).type.label, second);
            f9 += 18.0F;
            if (i > 1) {
                this.renderInfoRow(
                    graphics,
                    x,
                    width,
                    f9,
                    "\ue8b5",
                    "Added",
                    optional.<String>map(account -> DATE_FORMAT.format(Instant.ofEpochMilli(account.added))).orElse("—"),
                    second
                );
                f9 += 18.0F;
            }

            if (i > 2) {
                this.renderInfoRow(graphics, x, width, f9, "\ue7ef", "Saved", String.valueOf(this.accountManager.getAccounts().size()), second);
                f9 += 18.0F;
            }

            f9 += 4.0F;
        }

        this.removeCurrentRect = AccountsScreen.Rect.NONE;
        if (flag2) {
            this.removeCurrentRect = new AccountsScreen.Rect(x + 12.0F, f9, width - 24.0F, 22.0F);
            float f19 = this.getSnappySpring("removeCurrent").target(this.removeCurrentRect.contains(first, startColor) ? 1.0F : 0.0F).update();
            Render2D.fillRoundedRect(
                graphics,
                this.removeCurrentRect.x(),
                f9,
                this.removeCurrentRect.w(),
                22.0F,
                7.0F,
                ColorUtility.scaleAlpha(ColorUtility.lerpColor(ColorUtility.withAlpha(-1027478, 22), ColorUtility.withAlpha(-1027478, 60), f19), second)
            );
            String s3 = "Remove account";
            float f20 = Fonts.SMALL.getWidth("Remove account") + 14.0F;
            int j = ColorUtility.lerpColor(-685413, -1, f19);
            Fonts.ICONS.drawIconCentered(graphics, "\ue872", f1 - f20 / 2.0F + 4.0F, f9 + 11.0F, 0.8F, ColorUtility.scaleAlpha(j, second));
            Fonts.SMALL.drawVCentered(graphics, "Remove account", f1 - f20 / 2.0F + 14.0F, f9 + 11.0F, ColorUtility.scaleAlpha(j, second));
        }

        float f21 = f15 - 32.0F;
        if (flag1) {
            this.autoLoginToggleRect = new AccountsScreen.Rect(x + 10.0F, f21, width - 20.0F, 32.0F);
            float f22 = this.getSnappySpring("toggle").target(this.autoLoginToggleRect.contains(first, startColor) ? 1.0F : 0.0F).update();
            Render2D.fillRoundedRect(
                graphics,
                this.autoLoginToggleRect.x(),
                f21,
                this.autoLoginToggleRect.w(),
                32.0F,
                8.0F,
                ColorUtility.scaleAlpha(ColorUtility.lerpColor(234881023, 452984831, f22), second)
            );
            Fonts.REGULAR
                .drawVCentered(graphics, "Sign in on launch", this.autoLoginToggleRect.x() + 10.0F, f21 + 11.0F, ColorUtility.scaleAlpha(-419430401, second));
            Fonts.SMALL
                .drawVCentered(graphics, "Restores your last account", this.autoLoginToggleRect.x() + 10.0F, f21 + 22.0F, ColorUtility.scaleAlpha(1728053247, second));
            Widgets.drawSwitch(
                graphics,
                this.autoLoginToggleRect.x() + this.autoLoginToggleRect.w() - 10.0F - 18.0F,
                f21 + 16.0F - 5.0F,
                this.autoLoginSpring.target(this.accountManager.isAutoLogin() ? 1.0F : 0.0F).update(),
                second
            );
        } else {
            this.autoLoginToggleRect = AccountsScreen.Rect.NONE;
        }
    }

    public void renderInfoRow(GuiGraphicsExtractor graphics, float value, float value2, float value3, String message, String message2, String message3, float value4) {
        float f = value3 + 9.0F;
        Fonts.ICONS.drawIconCentered(graphics, message, value + 22.0F, f, 0.85F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value4));
        Fonts.SMALL.drawVCentered(graphics, message2, value + 32.0F, f, ColorUtility.scaleAlpha(-2130706433, value4));
        Fonts.SMALL.drawVCentered(graphics, message3, value + value2 - 14.0F - Fonts.SMALL.getWidth(message3), f, ColorUtility.scaleAlpha(-419430401, value4));
    }

    public List getFilteredKeys() {
        List<String> arraylist = new ArrayList<>();
        String s = this.searchQuery.toString().toLowerCase(Locale.ROOT).trim();

        for (AccountManager.Account accountmanager$account : this.accountManager.getAccounts()) {
            if (s.isEmpty() || accountmanager$account.name.toLowerCase(Locale.ROOT).contains(s)) {
                arraylist.add(accountmanager$account.uuid);
            }
        }

        if (s.isEmpty()) {
            arraylist.add("add");
        }

        return arraylist;
    }

    public AccountManager.Account findAccountByUuid(String text) {
        for (AccountManager.Account accountmanager$account : this.accountManager.getAccounts()) {
            if (accountmanager$account.uuid.equals(text)) {
                return accountmanager$account;
            }
        }

        return null;
    }

    public void renderSavedAccountsPanel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        float f = this.staggerAlpha(second, 2.0F);
        float f1 = y + (1.0F - f) * 10.0F;
        Glass.drawSheenPanel(graphics, x, f1, width, height, 10.0F, f, 1.0F);
        Fonts.MEDIUM.drawVCentered(graphics, "Saved accounts", x + 14.0F, f1 + 16.0F, ColorUtility.scaleAlpha(-218103809, f));
        String s = "Click to switch · drag to reorder · right-click to remove";
        if (this.searchQuery.length() == 0
            && width > Fonts.MEDIUM.getWidth("Saved accounts") + Fonts.SMALL.getWidth("Click to switch · drag to reorder · right-click to remove") + 50.0F) {
            Fonts.SMALL
                .drawVCentered(
                    graphics,
                    "Click to switch · drag to reorder · right-click to remove",
                    x + width - 14.0F - Fonts.SMALL.getWidth("Click to switch · drag to reorder · right-click to remove"),
                    f1 + 16.0F,
                    ColorUtility.scaleAlpha(1509949439, f)
                );
        }

        List list = this.getFilteredKeys();
        int i = (int)list.stream().filter(key -> !key.equals("add")).count();
        String s1 = this.searchQuery.length() > 0 ? i + " match" + (i == 1 ? "" : "es") : "";
        if (!s1.isEmpty()) {
            Fonts.SMALL.drawVCentered(graphics, s1, x + 18.0F + Fonts.MEDIUM.getWidth("Saved accounts"), f1 + 16.0F, ColorUtility.scaleAlpha(1728053247, f));
        }

        Render2D.fillRect(graphics, x + 12.0F, f1 + 30.0F, width - 24.0F, 0.5F, ColorUtility.scaleAlpha(452984831, f));
        float f2 = width - 24.0F;
        int j = Math.max(1, (int)((f2 + 8.0F) / 166.0F));
        float f3 = (f2 - 8.0F * (j - 1)) / j;
        float f4 = y + 40.0F;
        this.viewportHeight = height - 40.0F - 10.0F;
        this.listViewportRect = new AccountsScreen.Rect(x, f4, width, this.viewportHeight);
        int k = (list.size() + j - 1) / j;
        this.contentHeight = k * 58.0F - 8.0F;
        this.scrollTarget = Math.max(0.0F, Math.min(Math.max(0.0F, this.contentHeight - this.viewportHeight), this.scrollTarget));
        float f5 = this.scrollSpring.target(this.scrollTarget).update();
        this.slots.clear();
        if (list.isEmpty()) {
            Fonts.ICONS_LG.drawIconCentered(graphics, "\ue8b6", x + width / 2.0F, f4 + 40.0F, 1.0F, ColorUtility.scaleAlpha(1090519039, f));
            Fonts.REGULAR
                .drawCentered(graphics, "No accounts match \"" + this.searchQuery + "\"", x + width / 2.0F, f4 + 62.0F, ColorUtility.scaleAlpha(-2130706433, f));
        } else {
            graphics.enableScissor((int)x, (int)(f1 + 31.0F), (int)(x + width), (int)(f1 + height - 4.0F));
            this.exitingCards
                .entrySet()
                .removeIf(
                    entry -> {
                        AccountsScreen.Exiting accountsscreen$exiting = entry.getValue();
                        float f19 = Math.max(0.0F, Math.min(1.0F, accountsscreen$exiting.presence().target(0.0F).update()));
                        if (f19 <= 0.02F) {
                            return true;
                        }

                        AccountsScreen.Rect accountsscreen$rect3 = new AccountsScreen.Rect(
                            accountsscreen$exiting.rect().x(),
                            accountsscreen$exiting.rect().y() + (accountsscreen$exiting.scroll() - f5),
                            accountsscreen$exiting.rect().w(),
                            50.0F
                        );
                        this.renderAccountCard(
                            graphics,
                            entry.getKey(),
                            accountsscreen$exiting.account(),
                            accountsscreen$rect3,
                            new AccountsScreen.Rect(accountsscreen$rect3.x() + accountsscreen$rect3.w() - 22.0F, accountsscreen$rect3.y() + 6.0F, 16.0F, 16.0F),
                            false,
                            -1000,
                            -1000,
                            f * f19
                        );
                        return false;
                    }
                );
            boolean flag = !this.initialLayoutDone;
            this.initialLayoutDone = true;

            for (int l = 0; l < list.size(); l++) {
                String s2 = (String)list.get(l);
                int i1 = l % j;
                int j1 = l / j;
                float f6 = x + 12.0F + i1 * (f3 + 8.0F);
                float f7 = f4 + j1 * 58.0F;
                AccountsScreen.Rect accountsscreen$rect = new AccountsScreen.Rect(f6, f7 - f5, f3, 50.0F);
                AccountsScreen.Rect accountsscreen$rect1 = new AccountsScreen.Rect(f6 + f3 - 22.0F, f7 - f5 + 6.0F, 16.0F, 16.0F);
                AccountManager.Account accountmanager$account = !s2.equals("launcher") && !s2.equals("add") ? this.findAccountByUuid(s2) : null;
                this.slots.add(new AccountsScreen.Slot(s2, accountmanager$account, accountsscreen$rect, accountsscreen$rect1));
                Spring spring = this.cardXSprings.computeIfAbsent(s2, text -> new Spring(f6, 420.0F, 41.0F));
                Spring spring1 = this.cardYSprings.computeIfAbsent(s2, text2 -> new Spring(f7, 420.0F, 41.0F));
                Spring spring2 = this.cardPresenceSprings.computeIfAbsent(s2, text3 -> new Spring(flag ? 1.0F : 0.0F, 220.0F, 30.0F));
                float f8 = spring.target(f6).update();
                float f9 = spring1.target(f7).update() - f5;
                float f10 = Math.max(0.0F, Math.min(1.0F, spring2.target(1.0F).update()));
                AccountsScreen.Rect accountsscreen$rect2 = new AccountsScreen.Rect(f8, f9, f3, 50.0F);
                this.renderedCardRects.put(s2, accountsscreen$rect2);
                if (this.draggingCard && s2.equals(this.draggedKey)) {
                    this.draggedAccount = accountmanager$account;
                    this.dragCardWidth = f3;
                    this.dragSlotIndex = l;
                } else if (!(f9 > f1 + height) && !(f9 + 50.0F < f4 - 10.0F)) {
                    float f11 = f * this.staggerAlpha(second, 3 + l) * f10;
                    boolean flag1 = accountsscreen$rect.contains(first, startColor) && this.listViewportRect.contains(first, startColor);
                    if (s2.equals("add")) {
                        this.renderAddAccountTile(graphics, accountsscreen$rect2, flag1, f11);
                    } else {
                        this.renderAccountCard(
                            graphics,
                            s2,
                            accountmanager$account,
                            accountsscreen$rect2,
                            new AccountsScreen.Rect(f8 + f3 - 22.0F, f9 + 6.0F, 16.0F, 16.0F),
                            flag1,
                            first,
                            startColor,
                            f11
                        );
                    }
                }
            }

            this.lastScrollY = f5;
            if (this.draggingCard && this.draggedAccount != null) {
                float f12 = Math.max(0.0F, Math.min(1.0F, this.dragLiftSpring.target(1.0F).update()));
                graphics.nextStratum();
                float f15 = (float)(this.lastMouseX - this.dragOffsetX);
                float f17 = (float)(this.lastMouseY - this.dragOffsetY);
                Render2D.drawDropShadow(graphics, f15, f17 + 6.0F, this.dragCardWidth, 50.0F, 8.0F, 16.0F, ColorUtility.scaleAlpha(1879048192, f * f12));
                graphics.nextStratum();
                this.renderAccountCard(
                    graphics,
                    this.draggedKey,
                    this.draggedAccount,
                    new AccountsScreen.Rect(f15, f17, this.dragCardWidth, 50.0F),
                    new AccountsScreen.Rect(f15 + this.dragCardWidth - 22.0F, f17 + 6.0F, 16.0F, 16.0F),
                    true,
                    -1000,
                    -1000,
                    f
                );
                graphics.nextStratum();
            }

            graphics.disableScissor();
            if (list.size() == 1 && ((String)list.get(0)).equals("add")) {
                float f13 = f4 + 50.0F + 26.0F;
                if (f13 + 20.0F < f1 + height) {
                    Fonts.REGULAR.drawCentered(graphics, "No saved accounts yet", x + width / 2.0F, f13, ColorUtility.scaleAlpha(-1711276033, f));
                    Fonts.SMALL
                        .drawCentered(
                            graphics,
                            "Accounts you add show up here, ready to switch to in one click.",
                            x + width / 2.0F,
                            f13 + 13.0F,
                            ColorUtility.scaleAlpha(1509949439, f)
                        );
                }
            }

            if (this.contentHeight > this.viewportHeight) {
                float f14 = this.viewportHeight - 6.0F;
                float f16 = Math.max(16.0F, f14 * this.viewportHeight / this.contentHeight);
                float f18 = f4 + 3.0F + (f14 - f16) * (f5 / (this.contentHeight - this.viewportHeight));
                Render2D.fillRoundedRect(graphics, x + width - 5.0F, f18, 2.0F, f16, 1.0F, ColorUtility.scaleAlpha(872415231, f));
            }
        }
    }

    public void renderAccountCard(
        GuiGraphicsExtractor graphics,
        String text,
        AccountManager.Account account,
        AccountsScreen.Rect rect,
        AccountsScreen.Rect rect2,
        boolean flag3,
        int value,
        int value2,
        float value3
    ) {
        if (!(value3 <= 0.01F)) {
            boolean flag = account == null;
            boolean flag1 = flag ? this.accountManager.isInitialSession() : this.accountManager.isActiveAccount(account);
            float f = this.getHoverSpring(text).target(flag3 ? 1.0F : 0.0F).update();
            float f1 = Math.max(0.0F, this.getFlashSpring(text).target(0.0F).update());
            float f2 = Render2D.snapToPixel(rect.x());
            float f3 = Render2D.snapToPixel(rect.y() - f * 2.0F + (1.0F - value3) * 6.0F);
            float f4 = rect.w();
            if (f > 0.01F && !flag1) {
                Render2D.drawDropShadow(graphics, f2, f3 + 3.0F, f4, 50.0F, 8.0F, 10.0F, ColorUtility.scaleAlpha(1342177280, value3 * f));
            }

            if (flag1) {
                Render2D.drawDropShadow(graphics, f2, f3, f4, 50.0F, 8.0F, 8.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(-14498421, 18), value3));
            }

            if (f1 > 0.01F) {
                Render2D.drawDropShadow(graphics, f2, f3, f4, 50.0F, 8.0F, 8.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha((int)(120.0F * f1)), value3));
            }

            graphics.nextStratum();
            renderCardGlass(graphics, f2, f3, f4, value3);
            int i = ColorUtility.lerpColor(268435455, 536870911, f);
            if (flag1) {
                i = ColorUtility.lerpColor(ColorUtility.withAlpha(-14498421, 24), ColorUtility.withAlpha(-14498421, 30), f);
            }

            Render2D.fillRoundedRect(graphics, f2, f3, f4, 50.0F, 8.0F, ColorUtility.scaleAlpha(i, value3));
            if (flag1 && f > 0.01F) {
                Render2D.fillRoundedRect(graphics, f2, f3, f4, 50.0F, 8.0F, ColorUtility.scaleAlpha(318767103, value3 * f));
            }

            boolean flag2 = text.equals(this.signingInKey);
            float f5 = Math.max(0.0F, Math.min(1.0F, this.getSigningInSpring(text).target(flag2 ? 1.0F : 0.0F).update()));
            float f6 = Math.max(0.0F, Math.min(1.0F, this.getSwitchedPulseSpring(text).target(0.0F).update()));
            float f7 = Math.max(f5 * 0.6F, f6);
            if (f7 > 0.01F) {
                Render2D.fillRoundedRect(graphics, f2, f3, f4, 50.0F, 8.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(-14498421, (int)(34.0F * f7)), value3));
            }

            int j = flag1 ? ColorUtility.withAlpha(-14498421, 70) : ColorUtility.lerpColor(268435455, 654311423, f);
            if (f7 > 0.01F) {
                j = ColorUtility.lerpColor(j, ColorUtility.withAlpha(-14498421, 190), f7);
            }

            Render2D.drawRoundedBorderTextured(graphics, f2, f3, f4, 50.0F, 8.0F, 1.0F, ColorUtility.scaleAlpha(j, value3));
            graphics.nextStratum();
            if (f5 > 0.01F) {
                graphics.enableScissor((int)Math.ceil(f2 + 1.0F), (int)Math.ceil(f3 + 1.0F), (int)(f2 + f4 - 1.0F), (int)(f3 + 50.0F - 1.0F));
                long k = System.currentTimeMillis();

                for (int l = 0; l < 2; l++) {
                    float f9 = (float)((k + l * 700L) % 1400L) / 1400.0F;
                    float f10 = f9 * f9 * (3.0F - 2.0F * f9);
                    float f11 = f4 * 0.55F;
                    float f12 = f2 - f11 + (f4 + f11) * f10;
                    float f13 = (float)Math.sin(Math.PI * f9);
                    Render2D.drawBlob(
                        graphics, f12, f3 + 4.0F, f11, 42.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(-14498421, (int)(120.0F * f13)), value3 * f5)
                    );
                    Render2D.drawBlob(
                        graphics,
                        f12 + f11 * 0.3F,
                        f3 + 12.5F,
                        f11 * 0.4F,
                        25.0F,
                        ColorUtility.scaleAlpha(ColorUtility.withAlpha(-8523584, (int)(70.0F * f13)), value3 * f5)
                    );
                }

                graphics.disableScissor();
                graphics.nextStratum();
            }

            float f16 = 30.0F;
            float f8 = f2 + 10.0F;
            float f17 = f3 + 10.0F;
            UUID uuid = flag ? this.accountManager.getInitialUser().getProfileId() : AccountManager.parseUuid(account);
            Render2D.drawDropShadow(graphics, f8, f17 + 1.5F, 30.0F, 30.0F, getHeadCornerRadius(30.0F), 4.0F, ColorUtility.scaleAlpha(1711276032, value3));
            graphics.nextStratum();
            drawPlayerHead(graphics, this.accountManager.getSkinTexture(uuid), f8, f17, 30.0F, value3);
            graphics.nextStratum();
            if (flag || account.type == AccountManager.Type.TOKEN) {
                Render2D.fillCircleAligned(graphics, f8 + 30.0F, f17 + 30.0F, 6.0F, ColorUtility.scaleAlpha(-14013904, value3));
                Fonts.ICONS.drawIconCentered(graphics, flag ? "\ue30a" : "\ue0da", f8 + 30.0F, f17 + 30.0F, 0.6F, ColorUtility.scaleAlpha(-637534209, value3));
            }

            float f18 = f8 + 30.0F + 10.0F;
            String s = flag ? this.accountManager.getInitialUser().getName() : account.name;
            float f19 = f4 - (f18 - f2) - (!flag ? 28 : 10);
            Fonts.MEDIUM.drawVCentered(graphics, Fonts.MEDIUM.ellipsize(s, f19), f18, f3 + 25.0F - 6.0F, ColorUtility.scaleAlpha(-1, value3));
            if (flag2) {
                int j1 = (int)(System.currentTimeMillis() / 300L % 4L);
                Fonts.SMALL.drawVCentered(graphics, "Signing in" + ".".repeat(j1), f18, f3 + 25.0F + 7.0F, ColorUtility.scaleAlpha(-10755160, value3));
            } else if (flag1) {
                Render2D.fillCircleAligned(graphics, f18 + 2.5F, f3 + 25.0F + 7.0F, 2.2F, ColorUtility.scaleAlpha(-14498421, value3));
                Fonts.SMALL.drawVCentered(graphics, "Active", f18 + 8.0F, f3 + 25.0F + 7.0F, ColorUtility.scaleAlpha(-14498421, value3));
            } else {
                String s1 = flag ? "Launcher" : account.type.label + (account.lastUsed > 0L ? " · " + formatRelativeTime(account.lastUsed) : "");
                Fonts.SMALL.drawVCentered(graphics, Fonts.SMALL.ellipsize(s1, f19), f18, f3 + 25.0F + 7.0F, ColorUtility.scaleAlpha(-2130706433, value3));
            }

            if (!flag2) {
                if (!flag) {
                    float f20 = Math.max(0.0F, Math.min(1.0F, this.getHoverSpring("trash:" + text).target(rect2.contains(value, value2) ? 1.0F : 0.0F).update()));
                    float f14 = f3 + 6.0F;
                    float f15 = f2 + f4 - 14.0F;
                    if (f20 > 0.01F) {
                        Render2D.fillCircleAligned(graphics, f15, f14 + 8.0F, 8.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(-1027478, 46), value3 * f20));
                    }

                    int i1 = ColorUtility.lerpColor(ColorUtility.lerpColor(1090519039, -1711276033, f), -1027478, f20);
                    Fonts.ICONS.drawIconCentered(graphics, "\ue872", f15, f14 + 8.0F, 0.85F, ColorUtility.scaleAlpha(i1, value3));
                }

                if (!flag1 && f > 0.01F) {
                    float f21 = f3 + 50.0F - 13.0F;
                    withPoseTransform(
                        graphics,
                        (1.0F - f) * -4.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        () -> Fonts.ICONS
                            .drawIconCentered(graphics, "\ue5cc", f2 + f4 - 12.0F, f21, 0.9F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value3 * f))
                    );
                }
            }
        }
    }

    public void removeAccountAnimated(AccountManager.Account account) {
        AccountsScreen.Rect accountsscreen$rect = this.renderedCardRects.get(account.uuid);
        if (accountsscreen$rect != null) {
            this.exitingCards.put(account.uuid, new AccountsScreen.Exiting(account, accountsscreen$rect, this.lastScrollY, new Spring(1.0F, 220.0F, 30.0F)));
        }

        this.cardXSprings.remove(account.uuid);
        this.cardYSprings.remove(account.uuid);
        this.cardPresenceSprings.remove(account.uuid);
        this.renderedCardRects.remove(account.uuid);
        this.accountManager.removeAccount(account);
    }

    public Spring getSigningInSpring(String text) {
        return this.signingInSprings.computeIfAbsent(text, text2 -> new Spring(0.0F, 120.0F, 22.0F));
    }

    public Spring getSwitchedPulseSpring(String text) {
        return this.switchedPulseSprings.computeIfAbsent(text, text2 -> new Spring(0.0F, 30.0F, 11.0F));
    }

    public static void renderCardGlass(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        Glass.drawBorderlessPanel(graphics, x, y, width, 50.0F, 8.0F, height, 0.2F, 0.5F, 856427536);
    }

    public void renderAddAccountTile(GuiGraphicsExtractor graphics, AccountsScreen.Rect rect, boolean flag, float value) {
        if (!(value <= 0.01F)) {
            float f = this.getHoverSpring("add").target(flag ? 1.0F : 0.0F).update();
            float f1 = Render2D.snapToPixel(rect.x());
            float f2 = Render2D.snapToPixel(rect.y() - f * 2.0F + (1.0F - value) * 6.0F);
            float f3 = rect.w();
            renderCardGlass(graphics, f1, f2, f3, value);
            Render2D.fillRoundedRect(
                graphics, f1, f2, f3, 50.0F, 8.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(100663295, Theme.getAccentWithAlpha(22), f), value)
            );
            Render2D.drawRoundedBorderTextured(
                graphics, f1, f2, f3, 50.0F, 8.0F, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(452984831, Theme.getAccentWithAlpha(150), f), value)
            );
            graphics.nextStratum();
            float f4 = f1 + 10.0F + 15.0F;
            float f5 = f2 + 25.0F;
            Render2D.fillCircleAligned(graphics, f4, f5, 15.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(352321535, Theme.getAccentColor(), f), value));
            withPoseTransform(
                graphics,
                0.0F,
                0.0F,
                f4,
                f5,
                0.9F + f * 0.1F,
                () -> Fonts.ICONS_LG
                    .drawIconCentered(
                        graphics, "\ue145", f4, f5, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-1275068417, Theme.getOnAccentText(), f), value)
                    )
            );
            Fonts.MEDIUM.drawVCentered(graphics, "Add account", f1 + 50.0F, f5 - 6.0F, ColorUtility.scaleAlpha(-218103809, value));
            Fonts.SMALL.drawVCentered(graphics, "Microsoft or access token", f1 + 50.0F, f5 + 7.0F, ColorUtility.scaleAlpha(-2130706433, value));
        }
    }

    public void renderModalOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float f = Math.max(0.0F, Math.min(1.0F, this.modalSpring.target(this.activeModal != AccountsScreen.Modal.NONE ? 1.0F : 0.0F).update()));
        if (this.activeModal != AccountsScreen.Modal.NONE) {
            this.renderedModal = this.activeModal;
        }

        if (f <= 0.01F) {
            this.renderedModal = AccountsScreen.Modal.NONE;
            this.modalCardRect = this.modalPrimaryLeftRect = this.modalPrimaryRightRect = this.modalAuxButtonRect = AccountsScreen.Rect.NONE;
        } else {
            if (this.linkState == AccountsScreen.LinkState.SUCCESS
                && this.activeModal == AccountsScreen.Modal.LINK
                && System.currentTimeMillis() - this.linkSuccessAt > 1400L) {
                this.closeModal();
            }

            graphics.nextStratum();
            Render2D.fillRect(graphics, 0.0F, 0.0F, this.width, this.height, ColorUtility.scaleAlpha(-1946157056, f * partialTick));
            graphics.nextStratum();

            float f1 = switch (this.renderedModal) {
                case CHOOSE, LINK, TOKEN -> 276.0F;
                default -> 236.0F;
            };

            float f2 = switch (this.renderedModal) {
                case CHOOSE -> 182.0F;
                case LINK -> 176.0F;
                case TOKEN -> 178.0F;
                default -> 150.0F;
            };
            if (this.snapModalSize) {
                this.modalWidthSpring.snap(f1);
                this.modalHeightSpring.snap(f2);
                this.snapModalSize = false;
            }

            float f3 = Render2D.snapToPixel(this.modalWidthSpring.target(f1).update());
            float f4 = Render2D.snapToPixel(this.modalHeightSpring.target(f2).update());
            float f5 = Render2D.snapToPixel(this.width / 2.0F - f3 / 2.0F);
            float f6 = Render2D.snapToPixel(this.height / 2.0F - f4 / 2.0F);
            this.modalCardRect = new AccountsScreen.Rect(f5, f6, f3, f4);
            float f7 = Math.max(0.0F, Math.min(1.0F, this.modalContentAlphaSpring.target(1.0F).update()));
            float f8 = f * partialTick * f7;
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0F, Render2D.snapToPixel((1.0F - f) * 12.0F));
            Glass.drawSheenPanel(graphics, f5, f6, f3, f4, 12.0F, f * partialTick, 1.0F);
            graphics.pose().translate(0.0F, Render2D.snapToPixel((1.0F - f7) * 5.0F));
            graphics.enableScissor((int)f5, (int)f6, (int)Math.ceil(f5 + f3), (int)Math.ceil(f6 + f4));
            if (this.renderedModal == AccountsScreen.Modal.REMOVE) {
                this.renderRemoveModal(graphics, f5, f6, f3, f4, mouseX, mouseY, f8);
            } else if (this.renderedModal == AccountsScreen.Modal.LINK) {
                this.renderLinkModal(graphics, f5, f6, f3, f4, mouseX, mouseY, f8);
            } else if (this.renderedModal == AccountsScreen.Modal.CHOOSE) {
                this.renderChooseModal(graphics, f5, f6, f3, f4, mouseX, mouseY, f8);
            } else if (this.renderedModal == AccountsScreen.Modal.TOKEN) {
                this.renderTokenModal(graphics, f5, f6, f3, f4, mouseX, mouseY, f8);
            }

            graphics.disableScissor();
            graphics.pose().popMatrix();
        }
    }

    public void renderChooseModal(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        float f = x + width / 2.0F;
        Fonts.TITLE.drawCentered(graphics, "Add an account", f, y + 24.0F, ColorUtility.scaleAlpha(-1, second));
        Fonts.SMALL.drawCentered(graphics, "Choose how you'd like to sign in", f, y + 38.0F, ColorUtility.scaleAlpha(-2130706433, second));
        float f1 = x + 14.0F;
        float f2 = width - 28.0F;
        float f3 = 46.0F;
        this.modalPrimaryLeftRect = new AccountsScreen.Rect(f1, y + 54.0F, f2, 46.0F);
        this.modalPrimaryRightRect = new AccountsScreen.Rect(f1, y + 54.0F + 46.0F + 8.0F, f2, 46.0F);
        this.renderChooseOption(
            graphics,
            this.modalPrimaryLeftRect,
            "opt:ms",
            "\ue80b",
            "Microsoft account",
            "Sign in through your browser · stays signed in",
            "Recommended",
            first,
            startColor,
            second
        );
        this.renderChooseOption(
            graphics, this.modalPrimaryRightRect, "opt:token", "\ue0da", "Access token", "Paste a Minecraft token · lasts about a day", null, first, startColor, second
        );
        String s = "Cancel";
        float f4 = Fonts.SMALL.getWidth("Cancel") + 16.0F;
        this.modalAuxButtonRect = new AccountsScreen.Rect(f - f4 / 2.0F, y + height - 22.0F, f4, 14.0F);
        float f5 = this.getSnappySpring("m:cancelChoose").target(this.modalAuxButtonRect.contains(first, startColor) ? 1.0F : 0.0F).update();
        Fonts.SMALL
            .drawCentered(
                graphics, "Cancel", f, this.modalAuxButtonRect.y() + 7.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(1728053247, -419430401, f5), second)
            );
    }

    public void renderChooseOption(
        GuiGraphicsExtractor graphics, AccountsScreen.Rect rect, String text, String text2, String text3, String text4, String text5, int value, int value2, float value3
    ) {
        float f = this.getSnappySpring(text).target(rect.contains(value, value2) ? 1.0F : 0.0F).update();
        Render2D.fillRoundedRect(
            graphics,
            rect.x(),
            rect.y(),
            rect.w(),
            rect.h(),
            9.0F,
            ColorUtility.scaleAlpha(ColorUtility.lerpColor(268435455, Theme.getAccentWithAlpha(34), f), value3)
        );
        Render2D.drawRoundedBorderTextured(
            graphics,
            rect.x(),
            rect.y(),
            rect.w(),
            rect.h(),
            9.0F,
            1.0F,
            ColorUtility.scaleAlpha(ColorUtility.lerpColor(352321535, Theme.getAccentWithAlpha(150), f), value3)
        );
        graphics.nextStratum();
        float f1 = rect.x() + 22.0F;
        float f2 = rect.y() + rect.h() / 2.0F;
        Render2D.fillCircleAligned(
            graphics, f1, f2, 13.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(Theme.getAccentWithAlpha(46), Theme.getAccentColor(), f), value3)
        );
        Fonts.ICONS_LG
            .drawIconCentered(
                graphics, text2, f1, f2, 0.85F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(Theme.getAccentColor(), Theme.getOnAccentText(), f), value3)
            );
        float f3 = rect.x() + 44.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(f * 1.5F, 0.0F);
        Fonts.MEDIUM.drawVCentered(graphics, text3, f3, f2 - 6.0F, ColorUtility.scaleAlpha(-1, value3));
        Fonts.SMALL.drawVCentered(graphics, Fonts.SMALL.ellipsize(text4, rect.w() - 70.0F), f3, f2 + 7.0F, ColorUtility.scaleAlpha(-2130706433, value3));
        graphics.pose().popMatrix();
        if (text5 != null) {
            Widgets.drawBadge(graphics, text5, f3 + Fonts.MEDIUM.getWidth(text3) + 8.0F, f2 - 11.0F, Theme.getAccentWithAlpha(60), -1, value3);
        }

        float f4 = rect.x() + rect.w() - 14.0F;
        withPoseTransform(
            graphics,
            f * 2.0F,
            0.0F,
            0.0F,
            0.0F,
            1.0F,
            () -> Fonts.ICONS
                .drawIconCentered(graphics, "\ue5cc", f4, f2, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(1090519039, Theme.getAccentColor(), f), value3))
        );
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

    public void renderTokenModal(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        float f = x + width / 2.0F;
        float f1 = (float)System.currentTimeMillis() / 1000.0F;
        float f2 = y + 28.0F;
        int i = this.tokenError != null ? -1027478 : Theme.getAccentColor();
        Render2D.fillCircleAligned(graphics, f, f2, 15.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(i, 30), second));
        Render2D.fillCircleAligned(graphics, f, f2, 13.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(i, 56), second));
        Fonts.ICONS_LG.drawIconCentered(graphics, "\ue0da", f, f2, 1.0F, ColorUtility.scaleAlpha(i, second));
        Fonts.TITLE.drawCentered(graphics, "Sign in with a token", f, f2 + 27.0F, ColorUtility.scaleAlpha(-1, second));
        Fonts.SMALL.drawCentered(graphics, "Paste a Minecraft access token. It lasts about a day.", f, f2 + 41.0F, ColorUtility.scaleAlpha(-2130706433, second));
        float f3 = x + 14.0F;
        float f4 = width - 28.0F;
        float f5 = f2 + 54.0F;
        float f6 = 26.0F;
        this.tokenInputRect = new AccountsScreen.Rect(f3, f5, f4, 26.0F);
        Render2D.fillRoundedRect(graphics, f3, f5, f4, 26.0F, 8.0F, ColorUtility.scaleAlpha(436207616, second));
        Render2D.fillRoundedRect(graphics, f3, f5, f4, 26.0F, 8.0F, ColorUtility.scaleAlpha(234881023, second));
        int j = this.tokenError != null ? ColorUtility.withAlpha(-1027478, 170) : Theme.getAccentWithAlpha(this.tokenChecking ? 90 : 150);
        Render2D.drawRoundedBorderTextured(graphics, f3, f5, f4, 26.0F, 8.0F, 1.0F, ColorUtility.scaleAlpha(j, second));
        String s = this.tokenInput.toString();
        float f7 = f3 + 10.0F;
        float f8 = f5 + 13.0F;
        float f9 = 52.0F;
        if (s.isEmpty()) {
            Fonts.REGULAR.drawVCentered(graphics, "Paste token (Ctrl+V)", f7, f8, ColorUtility.scaleAlpha(1509949439, second));
        } else {
            String s1 = s.length() > 30 ? s.substring(0, 16) + "…" + s.substring(s.length() - 10) : s;
            Fonts.REGULAR.drawVCentered(graphics, Fonts.REGULAR.ellipsize(s1, f4 - 52.0F - 70.0F), f7, f8, ColorUtility.scaleAlpha(-419430401, second));
            String s2 = s.length() + " chars";
            Fonts.SMALL.drawVCentered(graphics, s2, f3 + f4 - 52.0F - 8.0F - Fonts.SMALL.getWidth(s2), f8, ColorUtility.scaleAlpha(1308622847, second));
        }

        if (!this.tokenChecking && f1 % 1.06F < 0.53F) {
            float f14 = f7
                + (
                    s.isEmpty()
                        ? 0.0F
                        : Math.min(Fonts.REGULAR.getWidth(s.length() > 30 ? s.substring(0, 16) + "…" + s.substring(s.length() - 10) : s), f4 - 52.0F - 70.0F)
                )
                + 1.0F;
            Render2D.fillRect(graphics, f14, f8 - 5.0F, 0.75F, 10.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), second));
        }

        this.modalAuxButtonRect = new AccountsScreen.Rect(f3 + f4 - 52.0F - 4.0F, f5 + 4.0F, 52.0F, 18.0F);
        float f15 = this.getSnappySpring("m:pasteToken").target(this.modalAuxButtonRect.contains(first, startColor) ? 1.0F : 0.0F).update();
        Render2D.fillRoundedRect(
            graphics,
            this.modalAuxButtonRect.x(),
            this.modalAuxButtonRect.y(),
            this.modalAuxButtonRect.w(),
            this.modalAuxButtonRect.h(),
            5.0F,
            ColorUtility.scaleAlpha(ColorUtility.lerpColor(352321535, 788529151, f15), second)
        );
        boolean flag = s.isEmpty();
        String s3 = flag ? "Paste" : "Clear";
        float f10 = Fonts.SMALL.getWidth(s3) + 11.0F;
        float f11 = this.modalAuxButtonRect.x() + 26.0F - f10 / 2.0F;
        Fonts.ICONS.drawIconCentered(graphics, flag ? "\ue14f" : "\ue5cd", f11 + 4.0F, f8, 0.7F, ColorUtility.scaleAlpha(-855638017, second));
        Fonts.SMALL.drawVCentered(graphics, s3, f11 + 11.0F, f8, ColorUtility.scaleAlpha(-855638017, second));
        float f12 = f5 + 26.0F + 11.0F;
        if (this.tokenChecking) {
            int k = (int)(System.currentTimeMillis() / 300L % 4L);
            Fonts.SMALL.drawCentered(graphics, "Checking with Minecraft services" + ".".repeat(k), f, f12, ColorUtility.scaleAlpha(Theme.getAccentColor(), second));
        } else if (this.tokenError != null) {
            String s4 = Fonts.SMALL.ellipsize(this.tokenError, width - 40.0F);
            float f13 = Fonts.SMALL.getWidth(s4) + 12.0F;
            Fonts.ICONS.drawIconCentered(graphics, "\ue000", f - f13 / 2.0F + 4.0F, f12, 0.75F, ColorUtility.scaleAlpha(-1027478, second));
            Fonts.SMALL.drawVCentered(graphics, s4, f - f13 / 2.0F + 12.0F, f12, ColorUtility.scaleAlpha(-685413, second));
        } else {
            Fonts.SMALL.drawCentered(graphics, "Stored only on this device · Enter to sign in", f, f12, ColorUtility.scaleAlpha(1308622847, second));
        }

        float f16 = (width - 36.0F) / 2.0F;
        float f17 = y + height - 34.0F;
        this.modalPrimaryLeftRect = new AccountsScreen.Rect(x + 14.0F, f17, f16, 22.0F);
        this.modalPrimaryRightRect = new AccountsScreen.Rect(x + 22.0F + f16, f17, f16, 22.0F);
        this.renderModalButton(graphics, this.modalPrimaryLeftRect, "Back", "\ue5cb", false, false, first, startColor, this.tokenChecking ? second * 0.5F : second);
        this.renderModalButton(
            graphics,
            this.modalPrimaryRightRect,
            this.tokenChecking ? "Signing in…" : "Sign in",
            this.tokenChecking ? null : "\ue5ca",
            true,
            false,
            first,
            startColor,
            second
        );
    }

    public void renderRemoveModal(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        if (this.accountToRemove != null) {
            float f = x + width / 2.0F;
            float f1 = 34.0F;
            float f2 = f - 17.0F;
            float f3 = y + 18.0F;
            Render2D.drawDropShadow(graphics, f2, f3 + 2.0F, 34.0F, 34.0F, 3.0F, 6.0F, ColorUtility.scaleAlpha(Integer.MIN_VALUE, second));
            graphics.nextStratum();
            drawPlayerHead(graphics, this.accountManager.getSkinTexture(AccountManager.parseUuid(this.accountToRemove)), f2, f3, 34.0F, second);
            graphics.nextStratum();
            Render2D.fillCircleAligned(graphics, f2 + 34.0F, f3 + 34.0F, 7.0F, ColorUtility.scaleAlpha(-1027478, second));
            Render2D.fillRect(graphics, f2 + 34.0F - 3.5F, f3 + 34.0F - 0.6F, 7.0F, 1.2F, ColorUtility.scaleAlpha(-1, second));
            Fonts.TITLE.drawCentered(graphics, "Remove account?", f, f3 + 34.0F + 18.0F, ColorUtility.scaleAlpha(-1, second));
            Fonts.SMALL
                .drawCentered(
                    graphics, this.accountToRemove.name + " will be removed from this device.", f, f3 + 34.0F + 32.0F, ColorUtility.scaleAlpha(-1711276033, second)
                );
            Fonts.SMALL
                .drawCentered(
                    graphics,
                    this.accountManager.isActiveAccount(this.accountToRemove)
                        ? "You'll stay signed in until you switch or restart."
                        : "You can add it again any time.",
                    f,
                    f3 + 34.0F + 42.0F,
                    ColorUtility.scaleAlpha(1728053247, second)
                );
            float f4 = (width - 36.0F) / 2.0F;
            float f5 = y + height - 34.0F;
            this.modalPrimaryLeftRect = new AccountsScreen.Rect(x + 14.0F, f5, f4, 22.0F);
            this.modalPrimaryRightRect = new AccountsScreen.Rect(x + 22.0F + f4, f5, f4, 22.0F);
            this.modalAuxButtonRect = AccountsScreen.Rect.NONE;
            this.renderModalButton(graphics, this.modalPrimaryLeftRect, "Cancel", null, false, false, first, startColor, second);
            this.renderModalButton(graphics, this.modalPrimaryRightRect, "Remove", "\ue872", false, true, first, startColor, second);
        }
    }

    public void renderLinkModal(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        float f = x + width / 2.0F;
        float f1 = (float)System.currentTimeMillis() / 1000.0F;
        int i = this.linkState == AccountsScreen.LinkState.SUCCESS
            ? 3
            : (this.linkState == AccountsScreen.LinkState.FAILED ? -1 : (this.accountManager.loginStage == 1 ? 2 : (this.linkBrowserOpened ? 1 : 0)));
        float f2 = this.linkStepSpring.target(Math.max(0, i)).update();
        float f3 = y + 30.0F;
        int j = this.linkState == AccountsScreen.LinkState.FAILED
            ? -1027478
            : (this.linkState == AccountsScreen.LinkState.SUCCESS ? -14498421 : Theme.getAccentColor());
        float f4 = this.linkState == AccountsScreen.LinkState.WAITING ? 0.5F + 0.5F * (float)Math.sin(f1 * 3.0F) : 0.0F;
        Render2D.fillCircleAligned(graphics, f, f3, 15.0F + f4 * 3.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(j, (int)(26.0F + 20.0F * f4)), second));
        Render2D.fillCircleAligned(graphics, f, f3, 13.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(j, 56), second));
        String s = this.linkState == AccountsScreen.LinkState.FAILED ? "\ue000" : (this.linkState == AccountsScreen.LinkState.SUCCESS ? "\ue5ca" : "\ue89e");
        Fonts.ICONS_LG.drawIconCentered(graphics, s, f, f3, 1.0F, ColorUtility.scaleAlpha(j, second));

        String s1 = switch (this.linkState) {
            case WAITING -> this.accountManager.loginStage == 1
                ? "Linking your account"
                : (this.linkBrowserOpened ? "Sign in on your browser" : "Link a Microsoft account");
            case SUCCESS -> this.linkWasNewAccount ? "Account added" : "Already saved";
            case FAILED -> "Sign-in didn't finish";
        };
        Fonts.TITLE.drawCentered(graphics, s1, f, f3 + 28.0F, ColorUtility.scaleAlpha(-1, second));

        String s2 = switch (this.linkState) {
            case WAITING -> this.accountManager.loginStage == 1
                ? "Talking to Xbox Live and Minecraft…"
                : (this.linkBrowserOpened ? "Finish signing in on the page that opened." : "Open the Microsoft sign-in page to continue.");
            case SUCCESS -> this.linkWasNewAccount
                ? "Signed in as " + this.linkMessage + " · now your active account"
                : this.linkMessage + " was already saved, so you're signed in to it";
            case FAILED -> this.linkMessage;
        };
        Fonts.SMALL.drawCentered(graphics, Fonts.SMALL.ellipsize(s2, width - 24.0F), f, f3 + 42.0F, ColorUtility.scaleAlpha(-1711276033, second));
        String[] astring = new String[]{"Browser", "Sign in", "Link", "Done"};
        float f5 = x + 34.0F;
        float f6 = x + width - 34.0F;
        float f7 = f3 + 64.0F;
        float f8 = f6 - f5;
        Render2D.fillRoundedRect(graphics, f5, f7 - 1.0F, f8, 2.0F, 1.0F, ColorUtility.scaleAlpha(536870911, second));
        float f9 = Math.max(0.0F, Math.min(1.0F, f2 / 3.0F));
        int k = this.linkState == AccountsScreen.LinkState.FAILED ? -1027478 : Theme.getAccentColor();
        Render2D.fillRoundedRect(graphics, f5, f7 - 1.0F, Math.max(2.0F, f8 * f9), 2.0F, 1.0F, ColorUtility.scaleAlpha(k, second));

        for (int l = 0; l < astring.length; l++) {
            float f10 = f5 + f8 * l / 3.0F;
            boolean flag = f2 >= l - 0.01F && this.linkState != AccountsScreen.LinkState.FAILED;
            boolean flag1 = i == l;
            if (flag1 && this.linkState == AccountsScreen.LinkState.WAITING) {
                float f11 = f1 * 1.2F % 1.0F;
                Render2D.fillCircleAligned(
                    graphics, f10, f7, 4.0F + f11 * 6.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha((int)(90.0F * (1.0F - f11))), second)
                );
            }

            Render2D.fillCircleAligned(graphics, f10, f7, 4.0F, ColorUtility.scaleAlpha(flag ? k : -13355974, second));
            if (flag) {
                Render2D.fillCircleAligned(graphics, f10, f7, 1.6F, ColorUtility.scaleAlpha(-1, second));
            }

            Fonts.SMALL.drawCentered(graphics, astring[l], f10, f7 + 11.0F, ColorUtility.scaleAlpha(flag ? -637534209 : 1509949439, second));
        }

        float f12 = y + height - 32.0F;
        if (this.linkState == AccountsScreen.LinkState.WAITING) {
            float f13 = (width - 28.0F - 12.0F) / 3.0F;
            this.modalPrimaryLeftRect = new AccountsScreen.Rect(x + 14.0F, f12, f13, 22.0F);
            this.modalPrimaryRightRect = new AccountsScreen.Rect(x + 20.0F + f13, f12, f13, 22.0F);
            this.modalAuxButtonRect = new AccountsScreen.Rect(x + 26.0F + f13 * 2.0F, f12, f13, 22.0F);
            this.renderModalButton(graphics, this.modalPrimaryLeftRect, "Open", "\ue89e", !this.linkBrowserOpened, false, first, startColor, second);
            this.renderModalButton(graphics, this.modalPrimaryRightRect, "Copy link", "\ue14d", false, false, first, startColor, second);
            this.renderModalButton(graphics, this.modalAuxButtonRect, "Cancel", null, false, true, first, startColor, second);
        } else if (this.linkState == AccountsScreen.LinkState.FAILED) {
            float f14 = (width - 36.0F) / 2.0F;
            this.modalPrimaryLeftRect = new AccountsScreen.Rect(x + 14.0F, f12, f14, 22.0F);
            this.modalPrimaryRightRect = new AccountsScreen.Rect(x + 22.0F + f14, f12, f14, 22.0F);
            this.modalAuxButtonRect = AccountsScreen.Rect.NONE;
            this.renderModalButton(graphics, this.modalPrimaryLeftRect, "Close", null, false, false, first, startColor, second);
            this.renderModalButton(graphics, this.modalPrimaryRightRect, "Try again", "\ue5d5", true, false, first, startColor, second);
        } else {
            this.modalPrimaryLeftRect = this.modalPrimaryRightRect = this.modalAuxButtonRect = AccountsScreen.Rect.NONE;
        }
    }

    public void renderModalButton(
        GuiGraphicsExtractor graphics, AccountsScreen.Rect rect, String text, String text2, boolean flag, boolean flag2, int value, int value2, float value3
    ) {
        float f = this.getSnappySpring("m:" + text).target(rect.contains(value, value2) ? 1.0F : 0.0F).update();
        int i = flag ? Theme.getAccentColor() : ColorUtility.lerpColor(352321535, 654311423, f);
        if (flag) {
            i = ColorUtility.lighten(i, 0.08F * f);
        }

        if (flag2) {
            i = ColorUtility.lerpColor(ColorUtility.withAlpha(-1027478, 40), ColorUtility.withAlpha(-1027478, 80), f);
        }

        Render2D.fillRoundedRect(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 7.0F, ColorUtility.scaleAlpha(i, value3));
        int j = flag ? Theme.getOnAccentText() : (flag2 ? ColorUtility.lerpColor(-685413, -1, f) : ColorUtility.lerpColor(-855638017, -1, f));
        float f1 = Fonts.SMALL.getWidth(text) + (text2 != null ? 12 : 0);
        float f2 = rect.x() + rect.w() / 2.0F - f1 / 2.0F;
        if (text2 != null) {
            Fonts.ICONS.drawIconCentered(graphics, text2, f2 + 4.0F, rect.y() + rect.h() / 2.0F, 0.75F, ColorUtility.scaleAlpha(j, value3));
            f2 += 12.0F;
        }

        Fonts.SMALL.drawVCentered(graphics, text, f2, rect.y() + rect.h() / 2.0F, ColorUtility.scaleAlpha(j, value3));
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, Identifier id, float value, float value2, float value3, float value4) {
        if (!(value4 <= 0.01F)) {
            Render2D.drawPlayerHead(graphics, id, value, value2, value3, ColorUtility.scaleAlpha(-1, value4));
        }
    }

    public static float getHeadCornerRadius(float value) {
        return Render2D.getHeadCornerRadius(value);
    }

    public static String formatRelativeTime(long delayMs) {
        long i = Math.max(0L, (System.currentTimeMillis() - delayMs) / 1000L);
        if (i < 60L) {
            return "just now";
        } else if (i < 3600L) {
            return i / 60L + "m ago";
        } else {
            return i < 86400L ? i / 3600L + "h ago" : i / 86400L + "d ago";
        }
    }

    public void notifyToast(NotificationType notificationType, String text, String text2) {
        Adapt.getInstance().getNotificationManager().builder(notificationType).title(text).message(text2).duration(2200).post();
    }

    public static String unwrapErrorMessage(Throwable error) {
        while ((error instanceof CompletionException || error.getMessage() == null) && error.getCause() != null) {
            error = error.getCause();
        }

        String s = error.getMessage();
        return s != null && !s.isBlank() ? s : error.getClass().getSimpleName();
    }

    public static boolean isCancelled(Throwable error) {
        return error instanceof CancellationException || error.getCause() instanceof CancellationException;
    }

    public void openBrowserOrCopy(String text) {
        try {
            Blaze3D.openUri(URI.create(text));
        } catch (Exception exception) {
            this.minecraft.keyboardHandler.setClipboard(text);
            this.notifyToast(NotificationType.INFO, "Link copied", "Paste it into your browser to sign in");
        }
    }

    public void startMicrosoftLink() {
        this.linkState = AccountsScreen.LinkState.WAITING;
        this.linkMessage = "";
        this.linkUrl = null;
        this.linkBrowserOpened = false;
        this.linkStepSpring.snap(0.0F);
        this.openModal(AccountsScreen.Modal.LINK);
        CompletableFuture<AccountManager.Account> completablefuture = this.accountManager.loginWithMicrosoft(text -> this.linkUrl = text);
        this.linkFuture = completablefuture;
        completablefuture.whenComplete(
            (account, error) -> this.minecraft
                .execute(
                    () -> {
                        if (this.linkFuture == completablefuture) {
                            this.linkFuture = null;
                            if (error != null) {
                                if (isCancelled(error)) {
                                    return;
                                }

                                this.linkState = AccountsScreen.LinkState.FAILED;
                                this.linkMessage = unwrapErrorMessage(error);
                            } else {
                                this.linkState = AccountsScreen.LinkState.SUCCESS;
                                this.linkMessage = account.name;
                                this.linkWasNewAccount = account.newlyAdded;
                                if (!account.newlyAdded) {
                                    this.linkSuccessAt = System.currentTimeMillis() + 1600L;
                                }

                                this.linkSuccessAt = System.currentTimeMillis();
                                this.getSwitchedPulseSpring(account.uuid).snap(1.0F);
                                this.notifyToast(
                                    account.newlyAdded ? NotificationType.SUCCESS : NotificationType.INFO,
                                    account.newlyAdded ? "Account added" : "Already saved",
                                    "Signed in as " + account.name
                                );
                            }
                        }
                    }
                )
        );
    }

    public void cancelMicrosoftLink() {
        CompletableFuture completablefuture = this.linkFuture;
        this.linkFuture = null;
        this.accountManager.cancelMicrosoftLogin();
        if (completablefuture != null) {
            completablefuture.cancel(true);
        }
    }

    public void closeModal() {
        if (this.activeModal == AccountsScreen.Modal.LINK && this.linkFuture != null) {
            this.cancelMicrosoftLink();
        }

        this.activeModal = AccountsScreen.Modal.NONE;
        this.accountToRemove = null;
    }

    public void activateSlot(AccountsScreen.Slot slot) {
        if (this.signingInKey == null) {
            if (slot.account() == null) {
                if (!this.accountManager.isInitialSession()) {
                    this.accountManager.restoreInitialSession();
                    this.getFlashSpring("launcher").snap(1.0F);
                    this.notifyToast(NotificationType.SUCCESS, "Account switched", "Back on " + this.accountManager.getInitialUser().getName());
                }
            } else {
                AccountManager.Account accountmanager$account = slot.account();
                if (!this.accountManager.isActiveAccount(accountmanager$account)) {
                    this.signingInKey = slot.key();
                    this.accountManager.login(accountmanager$account).whenComplete((account, error) -> this.minecraft.execute(() -> {
                        this.signingInKey = null;
                        if (error != null) {
                            this.notifyToast(NotificationType.ERROR, "Couldn't sign in to " + accountmanager$account.name, unwrapErrorMessage(error));
                        } else {
                            this.getSwitchedPulseSpring(accountmanager$account.uuid).snap(1.0F);
                            this.notifyToast(NotificationType.SUCCESS, "Account switched", "Signed in as " + account.name);
                        }
                    }));
                }
            }
        }
    }

    public void openModal(AccountsScreen.Modal modal) {
        if (this.activeModal != AccountsScreen.Modal.NONE && this.activeModal != modal) {
            this.modalContentAlphaSpring.snap(0.0F);
        } else if (this.activeModal == AccountsScreen.Modal.NONE && this.modalSpring.get() < 0.05F) {
            this.snapModalSize = true;
            this.modalContentAlphaSpring.snap(1.0F);
        }

        if (modal == AccountsScreen.Modal.TOKEN) {
            this.tokenError = null;
        }

        this.activeModal = modal;
    }

    public void promptRemoveAccount(AccountManager.Account account) {
        this.accountToRemove = account;
        this.openModal(AccountsScreen.Modal.REMOVE);
    }

    public void pasteTokenFromClipboard() {
        String s = this.minecraft.keyboardHandler.getClipboard();
        if (s != null && !s.isBlank()) {
            this.tokenInput.setLength(0);
            this.tokenInput.append(MicrosoftAuth.cleanToken(s), 0, Math.min(8192, MicrosoftAuth.cleanToken(s).length()));
            this.tokenError = null;
        } else {
            this.tokenError = "Your clipboard is empty";
        }
    }

    public void submitTokenSignIn() {
        if (!this.tokenChecking) {
            if (this.tokenInput.length() == 0) {
                this.tokenError = "Paste a token first";
            } else {
                this.tokenChecking = true;
                this.tokenError = null;
                this.accountManager
                    .loginWithToken(this.tokenInput.toString())
                    .whenComplete(
                        (account, error) -> this.minecraft
                            .execute(
                                () -> {
                                    this.tokenChecking = false;
                                    if (error != null) {
                                        this.tokenError = unwrapErrorMessage(error);
                                    } else {
                                        this.tokenInput.setLength(0);
                                        this.getSwitchedPulseSpring(account.uuid).snap(1.0F);
                                        this.notifyToast(
                                            account.newlyAdded ? NotificationType.SUCCESS : NotificationType.INFO,
                                            account.newlyAdded ? "Account added" : account.name + " was already saved",
                                            "Signed in as " + account.name
                                        );
                                        this.closeModal();
                                    }
                                }
                            )
                    );
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double d0 = event.x();
        double d1 = event.y();
        if (this.activeModal != AccountsScreen.Modal.NONE) {
            if (this.activeModal == AccountsScreen.Modal.CHOOSE) {
                if (this.modalPrimaryLeftRect.contains(d0, d1)) {
                    this.onUiFeedback(1.0F);
                    this.startMicrosoftLink();
                } else if (this.modalPrimaryRightRect.contains(d0, d1)) {
                    this.onUiFeedback(1.0F);
                    this.openModal(AccountsScreen.Modal.TOKEN);
                } else if (this.modalAuxButtonRect.contains(d0, d1) || !this.modalCardRect.contains(d0, d1)) {
                    this.onUiFeedback(0.9F);
                    this.closeModal();
                }
            } else if (this.activeModal == AccountsScreen.Modal.TOKEN) {
                if (this.modalPrimaryLeftRect.contains(d0, d1) && !this.tokenChecking) {
                    this.onUiFeedback(0.9F);
                    this.openModal(AccountsScreen.Modal.CHOOSE);
                } else if (this.modalPrimaryRightRect.contains(d0, d1)) {
                    this.onUiFeedback(1.0F);
                    this.submitTokenSignIn();
                } else if (this.modalAuxButtonRect.contains(d0, d1) && !this.tokenChecking) {
                    this.onUiFeedback(1.1F);
                    if (this.tokenInput.length() == 0) {
                        this.pasteTokenFromClipboard();
                    } else {
                        this.tokenInput.setLength(0);
                    }
                }
            } else if (this.activeModal == AccountsScreen.Modal.REMOVE) {
                if (this.modalPrimaryLeftRect.contains(d0, d1)) {
                    this.onUiFeedback(0.9F);
                    this.closeModal();
                } else if (this.modalPrimaryRightRect.contains(d0, d1) && this.accountToRemove != null) {
                    this.onUiFeedback(0.8F);
                    String s = this.accountToRemove.name;
                    this.removeAccountAnimated(this.accountToRemove);
                    this.notifyToast(NotificationType.INFO, "Account removed", s);
                    this.closeModal();
                } else if (!this.modalCardRect.contains(d0, d1)) {
                    this.closeModal();
                }
            } else if (this.activeModal == AccountsScreen.Modal.LINK) {
                if (this.linkState == AccountsScreen.LinkState.WAITING) {
                    if (this.modalPrimaryLeftRect.contains(d0, d1)) {
                        this.onUiFeedback(1.0F);
                        if (this.linkUrl != null) {
                            this.openBrowserOrCopy(this.linkUrl);
                            this.linkBrowserOpened = true;
                        }
                    } else if (this.modalPrimaryRightRect.contains(d0, d1)) {
                        this.onUiFeedback(1.0F);
                        if (this.linkUrl != null) {
                            this.minecraft.keyboardHandler.setClipboard(this.linkUrl);
                            this.linkBrowserOpened = true;
                        }

                        this.notifyToast(NotificationType.INFO, "Link copied", "Paste it into any browser to sign in");
                    } else if (this.modalAuxButtonRect.contains(d0, d1)) {
                        this.onUiFeedback(0.9F);
                        this.closeModal();
                    }
                } else if (this.linkState == AccountsScreen.LinkState.FAILED) {
                    if (this.modalPrimaryLeftRect.contains(d0, d1)) {
                        this.onUiFeedback(0.9F);
                        this.closeModal();
                    } else if (this.modalPrimaryRightRect.contains(d0, d1)) {
                        this.onUiFeedback(1.0F);
                        this.startMicrosoftLink();
                    } else if (!this.modalCardRect.contains(d0, d1)) {
                        this.closeModal();
                    }
                } else if (!this.modalCardRect.contains(d0, d1)) {
                    this.closeModal();
                }
            }

            return true;
        } else {
            this.searchFocused = this.searchBarRect.contains(d0, d1);
            if (this.searchFocused) {
                this.searchLastEditAt = System.currentTimeMillis();
            }

            if (this.searchFocused) {
                return true;
            }

            if (this.backButtonRect.contains(d0, d1)) {
                this.onUiFeedback(1.0F);
                this.onClose();
                return true;
            }

            if (this.addButtonRect.contains(d0, d1)) {
                this.onUiFeedback(1.0F);
                this.openModal(AccountsScreen.Modal.CHOOSE);
                return true;
            }

            if (this.uuidCopyRect.contains(d0, d1)) {
                this.onUiFeedback(1.2F);
                this.minecraft.keyboardHandler.setClipboard(this.minecraft.getUser().getProfileId().toString());
                this.notifyToast(NotificationType.INFO, "UUID copied", this.minecraft.getUser().getName());
                return true;
            }

            if (this.removeCurrentRect.contains(d0, d1)) {
                this.onUiFeedback(0.8F);
                this.accountManager.getActiveAccount().ifPresent(this::promptRemoveAccount);
                return true;
            }

            if (this.autoLoginToggleRect.contains(d0, d1)) {
                this.onUiFeedback(1.0F);
                this.accountManager.setAutoLogin(!this.accountManager.isAutoLogin());
                return true;
            }

            if (this.listViewportRect.contains(d0, d1)) {
                for (AccountsScreen.Slot accountsscreen$slot : this.slots) {
                    if (accountsscreen$slot.rect().contains(d0, d1)) {
                        boolean flag = accountsscreen$slot.account() != null && !accountsscreen$slot.key().equals(this.signingInKey);
                        if (accountsscreen$slot.key().equals("add")) {
                            this.onUiFeedback(1.0F);
                            this.openModal(AccountsScreen.Modal.CHOOSE);
                        } else if (!flag || event.button() != 3 && !accountsscreen$slot.trash().contains(d0, d1)) {
                            if (event.button() == 1) {
                                this.pressedSlotKey = accountsscreen$slot.key();
                                this.pressMouseX = d0;
                                this.pressMouseY = d1;
                            }
                        } else {
                            this.onUiFeedback(0.8F);
                            this.promptRemoveAccount(accountsscreen$slot.account());
                        }

                        return true;
                    }
                }
            }

            return true;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        this.lastMouseX = event.x();
        this.lastMouseY = event.y();
        if (this.pressedSlotKey != null && this.activeModal == AccountsScreen.Modal.NONE) {
            if (!this.draggingCard) {
                if (this.searchQuery.length() > 0 || Math.hypot(event.x() - this.pressMouseX, event.y() - this.pressMouseY) < 4.0) {
                    return true;
                }

                AccountsScreen.Rect accountsscreen$rect = this.renderedCardRects.get(this.pressedSlotKey);
                if (accountsscreen$rect == null || this.findAccountByUuid(this.pressedSlotKey) == null) {
                    return true;
                }

                this.draggingCard = true;
                this.draggedKey = this.pressedSlotKey;
                this.draggedAccount = this.findAccountByUuid(this.pressedSlotKey);
                this.dragOffsetX = (float)(this.pressMouseX - accountsscreen$rect.x());
                this.dragOffsetY = (float)(this.pressMouseY - accountsscreen$rect.y());
                this.dragLiftSpring.snap(0.0F);
            }

            double d1 = event.x() - this.dragOffsetX + this.dragCardWidth / 2.0F;
            double d0 = event.y() - this.dragOffsetY + 25.0;

            for (AccountsScreen.Slot accountsscreen$slot : this.slots) {
                if (accountsscreen$slot.account() != null && !accountsscreen$slot.key().equals(this.draggedKey) && accountsscreen$slot.rect().contains(d1, d0)) {
                    int i = this.accountManager.getAccounts().indexOf(accountsscreen$slot.account());
                    if (i >= 0 && this.draggedAccount != null) {
                        this.accountManager.moveAccount(this.draggedAccount, i);
                    }
                    break;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        String s = this.pressedSlotKey;
        this.pressedSlotKey = null;
        if (this.draggingCard) {
            Spring spring = this.cardXSprings.get(this.draggedKey);
            Spring spring1 = this.cardYSprings.get(this.draggedKey);
            if (spring != null) {
                spring.snap((float)(event.x() - this.dragOffsetX));
            }

            if (spring1 != null) {
                spring1.snap((float)(event.y() - this.dragOffsetY + this.lastScrollY));
            }

            this.draggingCard = false;
            this.draggedKey = null;
            this.draggedAccount = null;
            this.onUiFeedback(1.2F);
            return true;
        } else {
            if (s != null && this.activeModal == AccountsScreen.Modal.NONE) {
                for (AccountsScreen.Slot accountsscreen$slot : this.slots) {
                    if (accountsscreen$slot.key().equals(s) && accountsscreen$slot.rect().contains(event.x(), event.y())) {
                        this.onUiFeedback(1.0F);
                        this.activateSlot(accountsscreen$slot);
                        break;
                    }
                }
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeModal != AccountsScreen.Modal.NONE) {
            return true;
        }

        this.scrollTarget -= (float)scrollY * 58.0F;
        this.scrollTarget = Math.max(0.0F, Math.min(Math.max(0.0F, this.contentHeight - this.viewportHeight), this.scrollTarget));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.activeModal != AccountsScreen.Modal.NONE) {
                this.closeModal();
                return true;
            } else if (!this.searchFocused && this.searchQuery.length() <= 0) {
                this.onClose();
                return true;
            } else {
                this.searchFocused = false;
                this.clearSearch();
                return true;
            }
        } else if (this.activeModal != AccountsScreen.Modal.TOKEN) {
            if (this.activeModal == AccountsScreen.Modal.NONE && event.key() == 42 && this.searchQuery.length() > 0) {
                if (event.hasControlDown()) {
                    this.clearSearch();
                } else {
                    this.backspaceSearch();
                }

                this.searchLastEditAt = System.currentTimeMillis();
                return true;
            } else {
                return super.keyPressed(event);
            }
        } else {
            if (event.isPaste()) {
                if (!this.tokenChecking) {
                    this.pasteTokenFromClipboard();
                }
            } else if (event.key() == 40 || event.key() == 88) {
                this.submitTokenSignIn();
            } else if (event.key() == 42 && !this.tokenChecking && this.tokenInput.length() > 0) {
                if (event.hasControlDown()) {
                    this.tokenInput.setLength(0);
                } else {
                    this.tokenInput.setLength(this.tokenInput.length() - 1);
                }

                this.tokenError = null;
            }

            return true;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.activeModal == AccountsScreen.Modal.TOKEN) {
            if (!this.tokenChecking && event.isAllowedChatCharacter() && this.tokenInput.length() < 8192 && !Character.isWhitespace(event.codepoint())) {
                this.tokenInput.append(event.codepointAsString());
                this.tokenError = null;
            }

            return true;
        } else if (this.activeModal == AccountsScreen.Modal.NONE && event.isAllowedChatCharacter() && this.searchQuery.length() < 24) {
            this.searchFocused = true;
            this.appendSearchChar(event.codepointAsString());
            this.scrollTarget = 0.0F;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClose() {
        if (this.linkFuture != null) {
            this.cancelMicrosoftLink();
        }

        super.onClose();
    }

    private record DropChar(String ch, float x, long at) {
    }

    private record Exiting(AccountManager.Account account, AccountsScreen.Rect rect, float scroll, Spring presence) {
    }

    private enum LinkState {
        WAITING,
        SUCCESS,
        FAILED;
    }

    private enum Modal {
        NONE,
        CHOOSE,
        LINK,
        TOKEN,
        REMOVE;
    }

    private record Rect(float x, float y, float w, float h) {
        public static final AccountsScreen.Rect NONE = new AccountsScreen.Rect(0.0F, 0.0F, 0.0F, 0.0F);

        public boolean contains(double first, double second) {
            return first >= this.x && first < this.x + this.w && second >= this.y && second < this.y + this.h;
        }
    }

    private record Slot(String key, AccountManager.Account account, AccountsScreen.Rect rect, AccountsScreen.Rect trash) {
    }
}
