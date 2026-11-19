package dev.adapt.v3.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.binding.Bind;
import dev.adapt.v3.binding.BindRegistry;
import dev.adapt.v3.binding.BindingManager;
import dev.adapt.v3.binding.type.InputType;
// VENDOR-DISABLED: import dev.adapt.v3.chat.ChatService;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.client.ThemeModule;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.ColorProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.MultiBooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.notification.NotificationType;
import dev.adapt.v3.ui.Widgets;
import dev.adapt.v3.ui.render.Fonts;
import dev.adapt.v3.ui.render.Glass;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Accent;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.animation.Spring;
import dev.adapt.v3.util.render.ColorUtility;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class DashboardScreen extends BaseScreen {
    public static final float PANEL_WIDTH = 132.0F;
    public static final float HEADER_HEIGHT = 28.0F;
    public static final float MODULE_ROW_HEIGHT = 21.0F;
    public static final float PANEL_PAD_X = 9.0F;
    public static final float PANEL_RADIUS = 10.0F;
    public static final float ROW_INSET = 5.0F;
    public static final float ROW_RADIUS = 4.0F;
    public static final float ROW_PAD_Y = 1.5F;
    public static final int ACCENT_FILL_ALPHA = 71;
    public static final Map<ModuleCategory, float[]> categoryPositions = new EnumMap<>(ModuleCategory.class);
    public final StringBuilder searchQuery = new StringBuilder();
    public final Map<Object, Spring> springs = new HashMap<>();
    public final Map<ModuleCategory, Spring> categoryScrollSprings = new EnumMap<>(ModuleCategory.class);
    public final Map<ModuleCategory, Float> categoryScrollTargets = new EnumMap<>(ModuleCategory.class);
    public final Map<ModuleCategory, float[]> categoryBounds = new EnumMap<>(ModuleCategory.class);
    public final Map<ModuleCategory, Spring[]> categoryHoverTrackers = new EnumMap<>(ModuleCategory.class);
    public Module expandedModule;
    public Module bindingModule;
    public ModuleCategory draggingCategory;
    public float categoryDragOffX;
    public float categoryDragOffY;
    public Object activeDragOwner;
    public static final List<ModuleCategory> categoryZOrder = new ArrayList<>(List.of(ModuleCategory.VALUES));
    public final List<DashboardScreen.Hit> hitRegions = new ArrayList<>();
    public DashboardScreen.Drag activeDragHandler;
    public final Map<KeyMapping, Boolean> movementKeyStates = new HashMap<>();
    public boolean searchFocused;
    public long searchLastEditAt;
    public final List<Long> searchCharTimes = new ArrayList<>();
    public final List<DashboardScreen.DropChar> searchDroppedChars = new ArrayList<>();
    public final Spring searchScrollSpring = new Spring(0.0F, 400.0F, 40.0F);
    public final Spring searchCaretSpring = new Spring(0.0F, 700.0F, 53.0F);
    public boolean searchCaretInit;
    public static final float SETTINGS_PAD_Y = 2.5F;
    public static final float SETTINGS_INSET_X = 8.0F;
    public static final float HOVER_RADIUS = 4.0F;
    public static final float GROUP_OUTER_PAD = 5.0F;
    public static float rowHoverPadLeft = 8.0F;
    public static float rowHoverPadRight = 4.0F;
    public static final float GROUP_TOP_GAP = 4.0F;
    public static final float GROUP_HEADER_HEIGHT = 14.0F;
    public static final float GROUP_EXPAND_GAP = 1.0F;
    public static final Set<String> expandedGroups = new HashSet<>();
    public static final float PROP_WIDTH = 102.0F;
    public static final float GROUP_PROP_WIDTH = 96.0F;
    public static final float CHIP_HEIGHT = 13.0F;
    public static final float CHIP_GAP = 3.0F;
    public static final float CHIP_RADIUS = 5.0F;
    public static final float CHIP_ROW_GAP = 3.0F;
    public static final float CHIP_MAX_TEXT_SCALE = 0.85F;
    public static final float GROUP_BODY_PAD_TOP = 4.0F;
    public static final float GROUP_BODY_PAD_BOTTOM = 3.0F;
    public static final int ACTIVE_ROW_SHADOW = 1308622848;
    public static final float ACTIVE_SHADOW_BLUR = 4.0F;
    public static final float ACTIVE_SHADOW_OFFSET_Y = 1.5F;
    public int mouseX = -1000;
    public int mouseY = -1000;
    public static final float DROPDOWN_ROW_HEIGHT = 16.0F;
    public static final float DROPDOWN_PAD = 4.0F;
    public static final float SWATCH_RADIUS = 2.5F;
    public static final float DROPDOWN_TEXT_PAD = 5.0F;
    public static final int DROPDOWN_MAX_VISIBLE = 8;
    public EnumProperty<?> openEnumProp;
    public boolean enumDropdownOpen;
    public final Spring enumDropdownSpring = new Spring(0.0F, 420.0F, 41.0F);
    public static final float HIGHLIGHT_STIFFNESS = 6400.0F;
    public static final float HIGHLIGHT_DAMPING = 160.0F;
    public final Spring enumHoverYSpring = new Spring(0.0F, 6400.0F, 160.0F);
    public final Spring enumHoverAlphaSpring = createUiSpring(0.0F);
    public final Spring enumScrollSpring = createUiSpring(0.0F);
    public float enumScrollTarget;
    public float[] enumTriggerBounds = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    public float[] enumPopupBounds = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    public boolean enumTriggerVisible;
    public static final float PICKER_WIDTH = 172.0F;
    public static final float PICKER_PAD = 8.0F;
    public static final float SV_HEIGHT = 84.0F;
    public static final float HUE_HEIGHT = 8.0F;
    public static final float HEX_ROW_HEIGHT = 16.0F;
    public static final int[] COLOR_PRESETS = new int[]{-41073, -7639809, -11760129, -15154532, -24006, -1};
    public ColorProperty openColorProp;
    public boolean colorPickerOpen;
    public final Spring colorPickerSpring = new Spring(0.0F, 420.0F, 41.0F);
    public float[] colorTriggerBounds = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    public float[] colorPopupBounds = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    public boolean colorTriggerVisible;
    public boolean hexInputFocused;
    public final StringBuilder hexInput = new StringBuilder();
    public long hexLastEditAt;
    public final Map<String, String> keybindLabels = new HashMap<>();
    public final Map<String, String> prevKeybindLabels = new HashMap<>();
    public static final float CHAT_WIDTH = 140.0F;
    public static final float CHAT_TAB_HEIGHT = 15.0F;
    public static final float CHAT_INPUT_HEIGHT = 18.0F;
    public static final float CHAT_LINE_HEIGHT = 9.5F;
    public static final String ICON_CHAT = "\ue0bf";
    public static final String ICON_SEND = "\ue163";
    public static final String ICON_REPLY = "\ue15e";
    public static final String ICON_FROM_GAME = "\uea28";
    public static final float[] chatCustomPos = new float[2];
    public static boolean chatOpen;
    public static boolean chatCustomMoved;
    public static final Set<ModuleCategory> pinnedCategories = EnumSet.noneOf(ModuleCategory.class);
    public float columnsRightEdge;
    public float columnsChatShift;
    public float columnGap;
    public final Spring chatOpenSpring;
    public final Spring chatShiftSpring;
    public static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    public final StringBuilder chatInput;
    public boolean chatInputFocused;
    public boolean draggingChat;
    public boolean chatBroughtToFront;
    public boolean chatSubmitting;
    public float chatDragOffX;
    public float chatDragOffY;
    public long chatLastEditAt;
    public float chatScrollTarget;
    public final Spring chatScrollSpring;
    public final Spring chatInputScrollSpring;
    public final float[] chatPanelBounds;
    public Object replyingToMessage; // VENDOR-DISABLED: public ChatService.Message replyingToMessage;
    public String chatErrorBanner;
    public long chatErrorAt;
    public static final float CHAT_AVATAR_SIZE = 13.0F;
    public static final float CHAT_GROUP_GAP = 5.0F;
    public static final float CHAT_REPLY_HEIGHT = 11.0F;
    public static final float CHAT_NAME_HEIGHT = 10.5F;
    public static final String ICON_EDIT = "\ue3c9";
    public long hoveredMessageId;
    public final float[] messageToolbarBounds;
    public long copiedMessageId;
    public long copiedMessageAt;
    public long confirmDeleteMsgId;
    public long confirmDeleteAt;
    public Object editingMessage; // VENDOR-DISABLED: public ChatService.Message editingMessage;
    public final List<Long> chatCharTimes;
    public final List<DashboardScreen.DropChar> chatDroppedChars;
    public final Spring chatCaretSpring;
    public boolean chatCaretInit;

    public DashboardScreen(Screen screen) {
        super("AdaptV3", screen);
        this.chatOpenSpring = new Spring(chatOpen ? 1.0F : 0.0F, 230.0F, 25.0F);
        this.chatShiftSpring = new Spring(chatOpen ? 1.0F : 0.0F, 230.0F, 30.0F);
        this.chatInput = new StringBuilder();
        this.chatScrollSpring = createUiSpring(0.0F);
        this.chatInputScrollSpring = new Spring(0.0F, 400.0F, 40.0F);
        this.chatPanelBounds = new float[4];
        this.hoveredMessageId = -1L;
        this.messageToolbarBounds = new float[4];
        this.copiedMessageId = -1L;
        this.confirmDeleteMsgId = -1L;
        this.chatCharTimes = new ArrayList<>();
        this.chatDroppedChars = new ArrayList<>();
        this.chatCaretSpring = new Spring(0.0F, 700.0F, 53.0F);
    }

    @Override
    public boolean usesTextInput() {
        return true;
    }

    @Override
    public boolean isTyping() {
        return this.searchFocused || this.bindingModule != null || this.hexInputFocused || this.chatInputFocused;
    }

    @Override
    public void added() {
        super.added();
        // VENDOR-DISABLED: ChatService.getInstance().setPolling(chatOpen);
    }

    public KeyMapping[] getPassthroughKeys() {
        Options options = this.minecraft.options;
        return new KeyMapping[]{options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump, options.keySprint, options.keyShift};
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft.level != null && this.minecraft.player != null) {
            boolean flag = this.searchFocused || this.bindingModule != null || this.hexInputFocused || this.chatInputFocused;

            for (KeyMapping keymapping : this.getPassthroughKeys()) {
                Key key = KeyMappingHelper.getBoundKeyOf(keymapping);
                boolean flag1 = !flag && key.getType() == Type.KEYBOARD && InputConstants.isKeyDown(key.getValue());
                boolean flag2 = this.movementKeyStates.getOrDefault(keymapping, false);
                if (flag1 != flag2) {
                    keymapping.setDown(flag1);
                    this.movementKeyStates.put(keymapping, flag1);
                }
            }
        }
    }

    @Override
    public void removed() {
        for (Entry entry : this.movementKeyStates.entrySet()) {
            if ((Boolean)entry.getValue()) {
                ((KeyMapping)entry.getKey()).setDown(false);
            }
        }

        this.movementKeyStates.clear();
        // VENDOR-DISABLED: ChatService.getInstance().setPolling(false);
        this.chatInputFocused = false;
        super.removed();
    }

    @Override
    public void init() {
        super.init();
        ModuleCategory[] amodulecategory = ModuleCategory.VALUES;
        float f = 14.0F;
        int i = Math.max(1, Math.min(amodulecategory.length, (int)((this.width - 16 + 14.0F) / 146.0F)));
        float f1 = i * 132.0F + (i - 1) * 14.0F;
        float f2 = Math.max(8.0F, this.width / 2.0F - f1 / 2.0F);
        this.columnsRightEdge = f2 + f1;
        this.columnsChatShift = i == amodulecategory.length ? Math.max(0.0F, Math.min(f2 - 8.0F, 77.0F)) : 0.0F;
        this.columnGap = 14.0F;
        chatCustomPos[0] = Math.max(0.0F, Math.min(chatCustomPos[0], this.width - 140.0F));
        chatCustomPos[1] = Math.max(0.0F, Math.min(chatCustomPos[1], this.height - 28.0F));

        for (int j = 0; j < amodulecategory.length; j++) {
            float[] afloat = categoryPositions.computeIfAbsent(amodulecategory[j], moduleCategory -> new float[3]);
            if (afloat[0] == 0.0F && afloat[1] == 0.0F) {
                int k = j % i;
                int l = j / i;
                int i1 = (amodulecategory.length + i - 1) / i;
                float f3 = (float)(this.height - 46 - 30) / i1;
                afloat[0] = f2 + k * 146.0F;
                afloat[1] = 46.0F + l * f3;
                afloat[2] = i1 > 1 ? f3 - 10.0F : 0.0F;
            }

            afloat[0] = Math.min(afloat[0], Math.max(0.0F, this.width - 132.0F));
            afloat[1] = Math.min(afloat[1], Math.max(0.0F, this.height - 28.0F));
            this.categoryScrollSprings.computeIfAbsent(amodulecategory[j], moduleCategory2 -> createUiSpring(0.0F));
        }
    }

    public static Spring createUiSpring(float value) {
        return new Spring(value, 600.0F, 49.0F);
    }

    public Spring getSpring(Object key) {
        return this.springs.computeIfAbsent(key, key2 -> createUiSpring(0.0F));
    }

    public Spring getModuleToggleSpring(Module module) {
        return this.springs.computeIfAbsent("on" + module.getId(), key -> createUiSpring(module.isEnabled() ? 1.0F : 0.0F));
    }

    public boolean matchesSearch(Module module) {
        String s = this.searchQuery.toString().trim().toLowerCase(Locale.ROOT);
        return s.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(s) || module.getDescription().toLowerCase(Locale.ROOT).contains(s);
    }

    @Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, float alpha) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.hitRegions.clear();
        this.chatOpenSpring.target(chatOpen ? 1.0F : 0.0F).update();
        this.chatShiftSpring.target(chatOpen ? 1.0F : 0.0F).update();
        this.renderSearchBar(graphics, alpha);
        this.enumTriggerVisible = false;
        this.colorTriggerVisible = false;
        if (!this.chatBroughtToFront) {
            // VENDOR-DISABLED: this.renderChatPanel(graphics, mouseX, mouseY, alpha);
        }

        for (ModuleCategory modulecategory : List.copyOf(categoryZOrder)) {
            this.renderCategoryPanel(graphics, modulecategory, modulecategory.ordinal(), mouseX, mouseY, alpha);
        }

        if (this.chatBroughtToFront) {
            // VENDOR-DISABLED: this.renderChatPanel(graphics, mouseX, mouseY, alpha);
        }

        this.renderCornerButtons(graphics, mouseX, mouseY, alpha);
        this.renderEnumDropdownPopup(graphics, mouseX, mouseY, alpha);
        this.renderColorPickerPopup(graphics, mouseX, mouseY, alpha);
        String s = this.bindingModule != null
            ? "Press a key to bind " + this.bindingModule.getName() + "   ·   Esc clears it"
            : "Click to toggle   ·   Right-click for settings and keybind   ·   Drag headers to move";
        float f = this.minecraft.level != null ? this.height - 62 : this.height - 16;
        Fonts.SMALL.drawCenteredX(graphics, s, this.width / 2.0F, f, ColorUtility.scaleAlpha(1946157055, alpha));
    }

    public void renderCornerButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float f = 20.0F;
        float f1 = 6.0F;
        float f2 = this.height - 12 - 20.0F + (1.0F - partialTick) * 10.0F;
        float f3 = Fonts.SMALL.getWidth("Configs") + 32.0F;
        float f4 = Fonts.SMALL.getWidth("HUD Layout") + 32.0F;
        this.renderCornerButton(
            graphics, "configsBtn", "\ue53b", "Configs", this.width - 12 - f3, f2, f3, 20.0F, mouseX, mouseY, partialTick, () -> this.openScreen(new ConfigsScreen(this))
        );
        this.renderCornerButton(
            graphics,
            "hudBtn",
            "\ue89f",
            "HUD Layout",
            this.width - 12 - f4,
            f2 - 20.0F - 6.0F,
            f4,
            20.0F,
            mouseX,
            mouseY,
            partialTick,
            () -> this.openScreen(new HudEditorScreen(this))
        );
        float f5 = Fonts.SMALL.getWidth("Online Chat") + 32.0F;
        // VENDOR-DISABLED: this.renderOnlineChatButton(graphics, this.width - 12 - f5, f2 - 52.0F, f5, 20.0F, mouseX, mouseY, partialTick);
    }

    public void renderCornerButton(
        GuiGraphicsExtractor graphics,
        String text,
        String text2,
        String text3,
        float value,
        float value2,
        float value3,
        float value4,
        int value5,
        int value6,
        float value7,
        Runnable action
    ) {
        boolean flag = this.draggingCategory == null && Render2D.containsPoint(value5, value6, value, value2, value3, value4);
        float f = clamp01(this.getSpring(text).target(flag ? 1.0F : 0.0F).update());
        Glass.drawPanel(graphics, value, value2, value3, value4, 7.0F, value7, 0.5F);
        if (f > 0.01F) {
            Render2D.fillSubpixelRoundedRect(graphics, value, value2, value3, value4, 7.0F, ColorUtility.scaleAlpha(268435455, value7 * f));
            Render2D.drawRoundedBorderTextured(graphics, value, value2, value3, value4, 7.0F, 1.0F, ColorUtility.scaleAlpha(788529151, value7 * f));
        }

        float f1 = value2 + value4 / 2.0F;
        Fonts.ICONS
            .drawIconCentered(
                graphics, text2, value + 12.0F, f1, 0.9F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-1711276033, Theme.getAccentColor(), f), value7)
            );
        Fonts.SMALL.drawVCentered(graphics, text3, value + 21.0F, f1, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-855638017, -1, f), value7));
        this.hitRegions.add(new DashboardScreen.Hit(value, value2, value3, value4, (button, mouseX, mouseY) -> {
            if (button == 1) {
                action.run();
            }
        }, null));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int value, int value2, float value3) {
        float f = this.getOpenProgress();
        if (this.minecraft.level == null && !renderingParentBackdrop) {
            this.renderNebulaOrPanorama(graphics, value3);
        }

        graphics.nextStratum();
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
                    new DashboardScreen.DropChar(
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
                .add(new DashboardScreen.DropChar(this.searchQuery.substring(j, j + 1), Fonts.REGULAR.getWidth(this.searchQuery.substring(0, j)), i));
        }

        this.searchQuery.setLength(0);
        this.searchCharTimes.clear();
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

    public void setSearchFocused(boolean searchFocused) {
        this.searchFocused = searchFocused;
        if (searchFocused) {
            this.searchLastEditAt = System.currentTimeMillis();
        }
    }

    public void renderSearchBar(GuiGraphicsExtractor graphics, float value) {
        boolean flag = this.searchQuery.length() > 0;
        float f = clamp01(this.getSpring("searchFocus").target(this.searchFocused ? 1.0F : 0.0F).update());
        float f1 = this.getSpring("searchW").target(!flag && !this.searchFocused ? 150.0F : 190.0F).update();
        float f2 = this.width / 2.0F - f1 / 2.0F;
        float f3 = 14.0F - (1.0F - value) * 10.0F;
        float f4 = 20.0F;
        Glass.drawPanel(graphics, f2, f3, f1, 20.0F, 7.0F, value, 0.5F);
        if (f > 0.01F) {
            Render2D.fillSubpixelRoundedRect(graphics, f2, f3, f1, 20.0F, 7.0F, ColorUtility.scaleAlpha(268435455, value * f));
            Render2D.drawRoundedBorderTextured(graphics, f2, f3, f1, 20.0F, 7.0F, 1.0F, ColorUtility.scaleAlpha(788529151, value * f));
        }

        Fonts.ICONS
            .drawIconCentered(
                graphics,
                "\ue8b6",
                f2 + 13.0F,
                f3 + 10.0F,
                1.0F,
                ColorUtility.scaleAlpha(ColorUtility.lerpColor(-1711276033, Theme.getAccentColor(), Math.max(f, flag ? 1.0F : 0.0F)), value)
            );
        String s = this.searchQuery.toString();
        float f5 = f3 + 10.0F;
        float f6 = f2 + 22.0F;
        float f7 = f1 - 34.0F;
        long i = System.currentTimeMillis();
        float f8 = clamp01(this.getSpring("searchTyped").target(s.isEmpty() ? 0.0F : 1.0F).update());
        if (f8 < 0.99F) {
            int j = ColorUtility.scaleAlpha(ColorUtility.lerpColor(1509949439, 1090519039, f), value * (1.0F - f8));
            withPoseTransform(graphics, f * 4.0F, 0.0F, 0.0F, 0.0F, 1.0F, () -> Fonts.REGULAR.drawVCentered(graphics, "Search modules", f6, f5, j));
        }

        float f14 = Fonts.REGULAR.getWidth(s);
        float f9 = this.searchScrollSpring.target(Math.max(0.0F, f14 - f7)).update();
        graphics.enableScissor((int)(f6 - 2.0F), (int)f3, (int)(f2 + f1 - 8.0F), (int)(f3 + 20.0F));

        while (this.searchCharTimes.size() > s.length()) {
            this.searchCharTimes.remove(this.searchCharTimes.size() - 1);
        }

        while (this.searchCharTimes.size() < s.length()) {
            this.searchCharTimes.add(0L);
        }

        for (int k = 0; k < s.length(); k++) {
            float f10 = clamp01((float)(i - this.searchCharTimes.get(k)) / 170.0F);
            float f11 = 1.0F - (1.0F - f10) * (1.0F - f10) * (1.0F - f10);
            String s1 = s.substring(k, k + 1);
            float f12 = f6 - f9 + Fonts.REGULAR.getWidth(s.substring(0, k));
            float f13 = Fonts.REGULAR.getWidth(s1);
            int l = ColorUtility.scaleAlpha(-1, value * f11);
            withPoseTransform(
                graphics, 0.0F, (1.0F - f11) * 4.0F, f12 + f13 / 2.0F, f5, 0.85F + 0.15F * f11, () -> Fonts.REGULAR.drawVCentered(graphics, s1, f12, f5, l)
            );
        }

        this.searchDroppedChars.removeIf(dropChar -> i - dropChar.at() > 150L);

        for (DashboardScreen.DropChar dashboardscreen$dropchar : this.searchDroppedChars) {
            float f17 = clamp01((float)(i - dashboardscreen$dropchar.at()) / 150.0F);
            float f18 = f6 - f9 + dashboardscreen$dropchar.x();
            int j1 = ColorUtility.scaleAlpha(-1, value * (1.0F - f17));
            withPoseTransform(
                graphics,
                0.0F,
                f17 * 3.0F,
                f18 + Fonts.REGULAR.getWidth(dashboardscreen$dropchar.ch()) / 2.0F,
                f5,
                1.0F - 0.15F * f17,
                () -> Fonts.REGULAR.drawVCentered(graphics, dashboardscreen$dropchar.ch(), f18, f5, j1)
            );
        }

        graphics.disableScissor();
        float f15 = s.isEmpty() ? f6 - 1.0F : f6 - f9 + f14 + 1.0F;
        if (!this.searchCaretInit) {
            this.searchCaretSpring.snap(f15);
            this.searchCaretInit = true;
        }

        float f16 = this.searchCaretSpring.target(f15).update();
        if (this.searchFocused) {
            long i1 = i - this.searchLastEditAt;
            float f19 = i1 < 500L ? 1.0F : clamp01(0.5F + 1.4F * (float)Math.cos((i1 - 500L) / 1060.0 * Math.PI * 2.0));
            Render2D.fillRect(graphics, f16, f5 - 5.0F, 0.75F, 10.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value * f * f19));
        }

        this.hitRegions.add(new DashboardScreen.Hit(f2, f3, f1, 20.0F, (button, mouseX, mouseY) -> this.setSearchFocused(true), null));
    }

    public float calcModuleSettingsHeight(Module module) {
        float f = 22.5F;
        float f1 = 1.0F;
        boolean flag = false;

        for (Entry<String, List<Property<?>>> entry : groupVisibleProperties(module).entrySet()) {
            if (!entry.getValue().isEmpty()) {
                if (entry.getKey() == null) {
                    for (Property<?> property : entry.getValue()) {
                        f += calcPropertyHeight(property, 102.0F);
                    }

                    f1 = 1.0F;
                } else {
                    float f2 = this.getGroupExpandSpring(module, (String)entry.getKey()).get();
                    f += 18.0F + (1.0F + calcGroupBodyHeight((List<Property<?>>)entry.getValue())) * f2;
                    flag = true;
                }
            }
        }

        return (flag ? f + 3.0F : f + 2.0F - f1) + 2.5F;
    }

    public static float calcPropertyHeight(Property<?> property, float value) {
        if (property instanceof MultiBooleanProperty multibooleanproperty) {
            return calcMultiSelectHeight(multibooleanproperty, value);
        } else {
            return property instanceof NumberProperty ? 24.0F : 16.0F;
        }
    }

    public static boolean shouldShowMultiSelectLabel(MultiBooleanProperty property) {
        return property.getDescription() == null || !property.getDescription().equalsIgnoreCase(property.getName());
    }

    public static float[][] layoutMultiSelectChips(MultiBooleanProperty property, float value) {
        List<String> list = property.getOptions();
        int i = list.size();
        float f = 0.0F;

        for (String s : list) {
            f = Math.max(f, Fonts.SMALL.getWidth(s) + 6.0F);
        }

        int l = Math.max(Math.min(2, i), Math.min(i, (int)((value + 3.0F) / (f + 3.0F))));
        int i1 = l;

        for (int j = l; j >= 2; j--) {
            if (i % j == 0) {
                i1 = j;
                break;
            }
        }

        float f1 = (value - 3.0F * (i1 - 1)) / i1;
        float[][] afloat = new float[i][];

        for (int k = 0; k < i; k++) {
            afloat[k] = new float[]{k % i1 * (f1 + 3.0F), k / i1 * 16.0F, f1};
        }

        return afloat;
    }

    public static float getMultiSelectHeaderHeight(MultiBooleanProperty property) {
        return shouldShowMultiSelectLabel(property) ? 15.0F : 2.5F;
    }

    public static float calcMultiSelectHeight(MultiBooleanProperty property, float value) {
        float[][] afloat = layoutMultiSelectChips(property, value);
        float f = afloat.length == 0 ? 0.0F : afloat[afloat.length - 1][1] + 13.0F;
        return getMultiSelectHeaderHeight(property) + f + 3.5F;
    }

    public float renderMultiSelectProperty(
        GuiGraphicsExtractor graphics, MultiBooleanProperty property, String text, float value, float value2, float value3, float value4, float value5, float value6
    ) {
        if (shouldShowMultiSelectLabel(property)) {
            float f = this.updateRowHover(springKeyOf(property) + "lbl", value, value2, value3, 15.0F, value5, value6);
            Fonts.SMALL.drawVCentered(graphics, Fonts.SMALL.ellipsize(text, value3), value, value2 + 7.5F, ColorUtility.scaleAlpha(getSettingLabelColor(f), value4));
        }

        List<String> list = property.getOptions();
        float[][] afloat = layoutMultiSelectChips(property, value3);
        float f1 = value2 + getMultiSelectHeaderHeight(property);
        float f2 = 1.0F;

        for (String s : list) {
            f2 = Math.max(f2, Fonts.SMALL.getWidth(s));
        }

        float f9 = Math.min(0.85F, (afloat.length > 0 ? afloat[0][2] - 12.0F : 1.0F) / f2);
        float[] afloat2 = new float[list.size()];
        float[] afloat1 = new float[list.size()];

        for (int i = 0; i < list.size(); i++) {
            String s1 = (String)list.get(i);
            float f3 = value + afloat[i][0];
            float f4 = f1 + afloat[i][1];
            float f5 = afloat[i][2];
            String s2 = springKeyOf(property) + "/" + s1;
            float f6 = afloat2[i] = this.updateRowHover(s2, f3, f4, f5, 13.0F, value5, value6);
            float f7 = afloat1[i] = clamp01(this.getSpring("mb" + s2).target(property.isEnabled(s1) ? 1.0F : 0.0F).update());
            Spring spring = this.getClickFlashSpring(s2);
            float f8 = clamp01(spring.target(0.0F).update());
            Glass.drawPanel(graphics, f3, f4, f5, 13.0F, 5.0F, value4, 0.12F, 0.35F);
            Render2D.fillSubpixelRoundedRect(
                graphics, f3, f4, f5, 13.0F, 5.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(150994943, 486539263, Math.max(f6 * 0.6F, f8)), value4)
            );
            if (f7 > 0.01F) {
                if (Theme.hasGradient()) {
                    Render2D.fillHorizontalRoundedGradient(
                        graphics,
                        f3,
                        f4,
                        f5,
                        13.0F,
                        5.0F,
                        ColorUtility.withAlpha(Theme.sampleGradient(0.0F), 71),
                        ColorUtility.withAlpha(Theme.sampleGradient(1.0F), 71),
                        value4 * f7
                    );
                } else {
                    Render2D.fillSubpixelRoundedRect(graphics, f3, f4, f5, 13.0F, 5.0F, ColorUtility.scaleAlpha(Theme.getAccentWithAlpha(71), value4 * f7));
                }
            }

            this.addClippedHitRegion(f3, f4, f5, 13.0F, value5, value6, (button, mouseX, mouseY) -> {
                property.toggle(s1);
                spring.snap(1.0F);
            }, null);
        }

        graphics.nextStratum();

        for (int j = 0; j < list.size(); j++) {
            String s3 = (String)list.get(j);
            float f10 = value + afloat[j][0];
            float f11 = f1 + afloat[j][1];
            float f12 = afloat[j][2];
            int k = ColorUtility.lerpColor(-2130706433, -855638017, afloat2[j]);
            float f13 = Fonts.SMALL.getWidth(s3, f9);
            Fonts.SMALL
                .drawScaled(
                    graphics,
                    s3,
                    f10 + (f12 - f13) / 2.0F,
                    f11 + 6.5F - Fonts.SMALL.getPointSize() * f9 * 0.5F,
                    f9,
                    ColorUtility.scaleAlpha(ColorUtility.lerpColor(k, -1, afloat1[j]), value4)
                );
        }

        return value2 + calcMultiSelectHeight(property, value3);
    }

    public static Map<String, List<Property<?>>> groupVisibleProperties(Module module) {
        Map<String, List<Property<?>>> linkedhashmap = new LinkedHashMap<>();
        linkedhashmap.put(null, new ArrayList<>());

        for (Property property : module.getProperties()) {
            if (!property.isHidden()) {
                linkedhashmap.computeIfAbsent(property.getDescription(), text -> new ArrayList<>()).add(property);
            }
        }

        return linkedhashmap;
    }

    public static float calcGroupBodyHeight(List<Property<?>> properties) {
        float f = 0.0F;

        for (Property property : properties) {
            f += calcPropertyHeight(property, 96.0F);
        }

        boolean flag = properties.get(properties.size() - 1) instanceof NumberProperty;
        return 4.0F + f + (flag ? 0.5F : 3.0F);
    }

    public Spring getGroupExpandSpring(Module module, String text) {
        String s = module.getId() + "/" + text;
        return this.springs.computeIfAbsent("grp" + s, key -> createUiSpring(expandedGroups.contains(s) ? 1.0F : 0.0F));
    }

    public static String stripGroupPrefix(Property<?> property) {
        String s = property.getName();
        String s1 = property.getDescription();
        if (s1 == null) {
            return s;
        }

        for (String s2 : new String[]{s1, s1.endsWith("s") ? s1.substring(0, s1.length() - 1) : s1}) {
            if (s.length() > s2.length() + 1 && s.regionMatches(true, 0, s2 + " ", 0, s2.length() + 1)) {
                String s3 = s.substring(s2.length() + 1);
                return Character.toUpperCase(s3.charAt(0)) + s3.substring(1);
            }
        }

        return s;
    }

    public void renderCategoryPanel(GuiGraphicsExtractor graphics, ModuleCategory moduleCategory, int value, int value2, int value3, float value4) {
        float[] afloat = categoryPositions.get(moduleCategory);
        float f = Math.max(0.0F, Math.min(1.0F, this.openSpring.get() * 1.4F - value * 0.1F));
        float f1 = value4 * f;
        float f2 = afloat[0] - (pinnedCategories.contains(moduleCategory) ? 0.0F : this.getChatShiftOffset());
        float f3 = afloat[1] + (1.0F - f) * -16.0F;
        List<Module> list = Adapt.getInstance().getModuleManager().getByCategory(moduleCategory);
        float f4 = 0.0F;

        for (Module module : list) {
            float f5 = this.getSpring("vis" + module.getId()).target(this.matchesSearch(module) ? 1.0F : 0.0F).update();
            float f6 = this.getSpring("exp" + module.getId()).target(this.expandedModule == module ? 1.0F : 0.0F).update();
            f4 += 21.0F * f5 + this.calcModuleSettingsHeight(module) * Math.max(0.0F, f6) * f5;
        }

        f4 += 5.0F;
        float f18 = Math.max(60.0F, this.height - f3 - 28.0F - 34.0F);
        if (afloat[2] > 0.0F) {
            f18 = Math.max(40.0F, Math.min(f18, afloat[2] - 28.0F));
        }

        float f19 = Math.min(f4, f18);
        float f20 = 28.0F + f19;
        float f21 = 10.0F;
        this.categoryBounds.put(moduleCategory, new float[]{f2, f3, 132.0F, f20});
        float f7 = Math.max(0.0F, f4 - f18);
        this.categoryScrollTargets.put(moduleCategory, Math.max(0.0F, Math.min(f7, this.categoryScrollTargets.getOrDefault(moduleCategory, 0.0F))));
        float f8 = this.categoryScrollSprings.get(moduleCategory).target(this.categoryScrollTargets.get(moduleCategory)).update();
        float f9 = this.getSpring("lift" + moduleCategory).target(this.draggingCategory == moduleCategory ? 1.0F : 0.0F).update();
        if (f9 > 0.01F) {
            Render2D.drawDropShadow(graphics, f2, f3 + 6.0F, 132.0F, f20, 10.0F, 26.0F, ColorUtility.scaleAlpha(1610612736, f1 * f9));
        }

        Render2D.drawDropShadow(graphics, f2, f3 + 2.0F, 132.0F, f20, 10.0F, 14.0F, ColorUtility.scaleAlpha(1207959552, f1));
        Glass.drawBorderlessPanel(graphics, f2, f3, 132.0F, f20, 10.0F, f1, 0.55F, 1.0F, -1509159920);
        int i = (int)list.stream().filter(module2 -> module2.canToggle() && module2.isEnabled()).count();
        int j = (int)list.stream().filter(Module::canToggle).count();
        float f10 = 14.0F;
        Fonts.ICONS.drawIconCentered(graphics, getCategoryIcon(moduleCategory), f2 + 9.0F + 5.0F, f3 + 14.0F, 1.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), f1));
        Fonts.SEMIBOLD.draw(graphics, moduleCategory.getDisplayName(), f2 + 9.0F + 15.0F, f3 + 14.0F - Fonts.SEMIBOLD.getHalfPointSize(), ColorUtility.scaleAlpha(-1, f1));
        Fonts.SMALL
            .drawRightAligned(graphics, i + "/" + j, f2 + 132.0F - 9.0F, f3 + 14.0F - Fonts.SMALL.getHalfPointSize(), ColorUtility.scaleAlpha(1509949439, f1));
        Render2D.fillRect(graphics, f2 + 9.0F + 2.0F, f3 + 28.0F - 1.0F, 110.0F, 0.5F, ColorUtility.scaleAlpha(452984831, f1));
        this.hitRegions.add(new DashboardScreen.Hit(f2, f3, 132.0F, 28.0F, (button, mouseX, mouseY) -> {
            if (!pinnedCategories.contains(moduleCategory)) {
                afloat[0] = f2;
                pinnedCategories.add(moduleCategory);
            }

            this.draggingCategory = moduleCategory;
            this.chatBroughtToFront = false;
            categoryZOrder.remove(moduleCategory);
            categoryZOrder.add(moduleCategory);
            this.categoryDragOffX = (float)mouseX - afloat[0];
            this.categoryDragOffY = (float)mouseY - afloat[1];
        }, null));
        float f11 = f3 + 28.0F;
        float f12 = f3 + f20 - 3.0F;
        graphics.enableScissor((int)f2, (int)f11, (int)(f2 + 132.0F), (int)Math.ceil(f12));
        this.renderModuleHoverHighlight(graphics, moduleCategory, list, f2, f11 + 2.0F - f8, f1, value2, value3, f11, f12);
        float f13 = f11 + 2.0F - f8;

        for (Module module1 : list) {
            float f14 = this.getSpring("vis" + module1.getId()).get();
            if (!(f14 < 0.01F)) {
                float f15 = 21.0F * f14;
                this.renderModuleRow(graphics, module1, f2, f13, f15, f14 * f1, value2, value3, f11, f12);
                f13 += f15;
                float f16 = Math.max(0.0F, this.getSpring("exp" + module1.getId()).get());
                if (f16 > 0.01F) {
                    float f17 = this.calcModuleSettingsHeight(module1) * f16 * f14;
                    int k = (int)Math.max(f11, f13);
                    int l = (int)Math.min(f12, f13 + f17);
                    if (l > k) {
                        graphics.enableScissor((int)f2, k, (int)(f2 + 132.0F), l);
                        Render2D.fillSubpixelRoundedRect(graphics, f2 + 5.0F, f13 + 1.5F, 122.0F, f17 - 3.0F, 6.666667F, ColorUtility.scaleAlpha(234881023, f1));
                        this.renderModuleSettings(graphics, module1, f2 + 9.0F + 8.0F, f13 + 3.0F + 2.5F, 102.0F, Math.min(1.0F, f16) * f1, k, l);
                        graphics.disableScissor();
                    }

                    f13 += f17;
                }
            }
        }

        graphics.disableScissor();
        if (f7 > 0.0F) {
            float f22 = f19 - 8.0F;
            float f23 = Math.max(16.0F, f22 * f18 / f4);
            Render2D.fillRoundedRect(
                graphics, f2 + 132.0F - 3.5F, f11 + 4.0F + (f22 - f23) * (f8 / f7), 1.5F, f23, 0.75F, ColorUtility.scaleAlpha(Theme.getTrackColor(), f1)
            );
        }
    }

    public void renderModuleHoverHighlight(
        GuiGraphicsExtractor graphics, ModuleCategory moduleCategory, List<Module> modules, float value, float value2, float value3, int value4, int value5, float value6, float value7
    ) {
        Spring[] aspring = this.categoryHoverTrackers
            .computeIfAbsent(moduleCategory, moduleCategory2 -> new Spring[]{new Spring(0.0F, 6400.0F, 160.0F), Spring.snappy(0.0F), new Spring(0.0F, 6400.0F, 160.0F)});
        float f = value2;
        float f1 = -1.0F;
        float f2 = 0.0F;
        boolean flag = this.draggingCategory == null && value5 >= value6 && value5 < value7;

        for (Module module : modules) {
            float f3 = this.getSpring("vis" + module.getId()).get();
            if (!(f3 < 0.01F)) {
                float f4 = 21.0F * f3;
                if (flag && Render2D.containsPoint(value4, value5, value, f, 132.0F, f4)) {
                    f1 = f - value2;
                    f2 = f4;
                }

                f += f4;
                float f5 = Math.max(0.0F, this.getSpring("exp" + module.getId()).get());
                if (f5 > 0.01F) {
                    f += this.calcModuleSettingsHeight(module) * f5 * f3;
                }
            }
        }

        boolean flag1 = f1 >= 0.0F;
        if (flag1) {
            if (aspring[1].get() < 0.02F) {
                aspring[0].snap(f1);
                aspring[2].snap(f2);
            }

            aspring[0].target(f1);
            aspring[2].target(f2);
        }

        float f6 = Math.max(0.0F, Math.min(1.0F, aspring[1].target(flag1 ? 1.0F : 0.0F).update()));
        float f7 = value2 + aspring[0].update();
        float f8 = aspring[2].update();
        if (f6 > 0.01F && f8 > 2.0F) {
            Render2D.fillSubpixelRoundedRect(graphics, value + 5.0F, f7 + 1.5F, 122.0F, f8 - 3.0F, 4.0F, ColorUtility.scaleAlpha(536870911, value3 * f6));
        }
    }

    public static String getCategoryIcon(ModuleCategory moduleCategory) {
        return moduleCategory.getIcon();
    }

    public static void renderActiveModuleFill(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value) {
        int i = Theme.sampleGradient(0.0F);
        int j = Theme.hasGradient() ? Theme.sampleGradient(1.0F) : i;
        Render2D.drawDropShadow(graphics, x, y + 1.5F, width, height, 4.0F, 4.0F, ColorUtility.scaleAlpha(1308622848, value));
        graphics.nextStratum();
        if (Theme.hasGradient()) {
            Render2D.fillHorizontalRoundedGradient(graphics, x, y, width, height, 4.0F, ColorUtility.withAlpha(i, 71), ColorUtility.withAlpha(j, 71), value);
        } else {
            Render2D.fillSubpixelRoundedRect(graphics, x, y, width, height, 4.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(i, 71), value));
        }

        graphics.nextStratum();
    }

    public void renderModuleRow(
        GuiGraphicsExtractor graphics, Module module, float value, float value2, float value3, float value4, int value5, int value6, float value7, float value8
    ) {
        boolean flag = value2 + value3 > value7 && value2 < value8;
        boolean flag1 = flag && Render2D.containsPoint(value5, value6, value, value2, 132.0F, value3) && value6 >= value7 && value6 < value8 && this.draggingCategory == null;
        float f = Math.max(0.0F, Math.min(1.0F, this.getSpring("hov" + module.getId()).target(flag1 ? 1.0F : 0.0F).update()));
        float f1 = Math.max(0.0F, Math.min(1.0F, this.getModuleToggleSpring(module).target(module.isEnabled() ? 1.0F : 0.0F).update()));
        float f2 = Render2D.snapToPixel(value2);
        float f3 = value2 - f2;
        float f4 = f2 + value3 / 2.0F;
        if (f1 > 0.01F) {
            renderActiveModuleFill(graphics, value + 5.0F, value2 + 1.5F, 122.0F, value3 - 3.0F, value4 * f1);
        }

        int i = ColorUtility.lerpColor(-1929379841, -637534209, f);
        graphics.pose().pushMatrix();
        graphics.pose().translate(f * 1.5F, f3);
        Fonts.ROW.drawVCentered(graphics, module.getName(), value + 9.0F + 3.0F, f4, ColorUtility.scaleAlpha(ColorUtility.lerpColor(i, -1, f1), value4));
        graphics.pose().popMatrix();
        float f5 = Math.max(0.0F, Math.min(1.0F, this.getSpring("exp" + module.getId()).get()));
        float f6 = Fonts.ICONS.getWidth("\ue5cf");
        float f7 = value + 132.0F - 9.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(f7 - f6 / 2.0F, value2 + value3 / 2.0F);
        graphics.pose().rotate((float) Math.PI * f5);
        int j = ColorUtility.lerpColor(Theme.getIconMuted(), -419430401, f1);
        Fonts.ICONS.drawIconCentered(graphics, "\ue5cf", 0.0F, 0.0F, 1.0F, ColorUtility.scaleAlpha(j, value4 * Math.max(f, f5)));
        graphics.pose().popMatrix();
        f7 -= f6 + 3.0F;
        String s = Adapt.getInstance().getBindingManager().getBindName(module);
        if (s != null || this.springs.containsKey("kw" + module.getId())) {
            this.renderKeybindBadge(graphics, module.getId(), s, false, f7, value2 + value3 / 2.0F, value4, s != null ? f : 0.0F, 0.0F);
        }

        if (flag && value3 > 6.0F) {
            this.addClippedHitRegion(value, value2, 132.0F, value3, value7, value8, (button, mouseX, mouseY) -> {
                if (button == 1 && !module.canToggle()) {
                    this.expandedModule = this.expandedModule == module ? null : module;
                    this.onUiFeedback(1.1F);
                } else if (button == 1) {
                    module.toggle();
                } else if (button == 3) {
                    this.expandedModule = this.expandedModule == module ? null : module;
                    this.onUiFeedback(1.1F);
                }
            }, null);
        }
    }

    public float updateRowHover(Object key, float value, float value2, float value3, float value4, float value5, float value6) {
        boolean flag = this.draggingCategory == null
            && this.mouseY >= value5
            && this.mouseY < value6
            && Render2D.containsPoint(this.mouseX, this.mouseY, value, value2, value3, value4);
        return clamp01(this.getSpring("sh" + springKeyOf(key)).target(flag ? 1.0F : 0.0F).update());
    }

    public static String springKeyOf(Object key) {
        return key instanceof String s ? s : System.identityHashCode(key) + ":" + key;
    }

    public static void renderSettingRowHover(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float first, float second) {
        if (first > 0.01F) {
            Render2D.fillSubpixelRoundedRect(
                graphics, x - rowHoverPadLeft, y, width + rowHoverPadLeft + rowHoverPadRight, height, 4.0F, ColorUtility.scaleAlpha(268435455, second * first)
            );
        }
    }

    public static int getSettingLabelColor(float value) {
        return ColorUtility.lerpColor(Theme.getMutedLabel(), -218103809, value);
    }

    public Spring getClickFlashSpring(Object key) {
        return this.springs.computeIfAbsent("cf" + springKeyOf(key), key2 -> new Spring(0.0F, 90.0F, 19.0F));
    }

    public void renderModuleSettings(GuiGraphicsExtractor graphics, Module module, float value, float value2, float value3, float value4, float value5, float value6) {
        float f = this.updateRowHover(module.getId() + "kb", value, value2, value3, 15.0F, value5, value6);
        renderSettingRowHover(graphics, value, value2, value3, 15.0F, f, value4);
        Fonts.SMALL.drawVCentered(graphics, "Keybind", value, value2 + 7.5F, ColorUtility.scaleAlpha(getSettingLabelColor(f), value4));
        this.renderKeybindBadge(
            graphics,
            module.getId() + "s",
            Adapt.getInstance().getBindingManager().getBindName(module),
            this.bindingModule == module,
            value + value3,
            value2 + 7.5F,
            value4,
            1.0F,
            f
        );
        this.addClippedHitRegion(value, value2, value3, 15.0F, value5, value6, (button, mouseX, mouseY) -> {
            this.bindingModule = this.bindingModule == module ? null : module;
            this.onUiFeedback(1.4F);
        }, null);
        value2 += 16.0F;

        for (Entry<String, List<Property<?>>> entry : groupVisibleProperties(module).entrySet()) {
            if (!entry.getValue().isEmpty()) {
                if (entry.getKey() == null) {
                    for (Property<?> property : entry.getValue()) {
                        value2 = this.renderProperty(graphics, property, value, value2, value3, value4, value5, value6);
                    }
                } else {
                    value2 = this.renderPropertyGroup(graphics, module, (String)entry.getKey(), (List<Property<?>>)entry.getValue(), value, value2, value3, value4, value5, value6);
                }
            }
        }
    }

    public float renderPropertyGroup(
        GuiGraphicsExtractor graphics, Module module, String text, List<Property<?>> properties, float value, float value2, float value3, float value4, float value5, float value6
    ) {
        value2 += 4.0F;
        String s = module.getId() + "/" + text;
        float f = clamp01(this.getGroupExpandSpring(module, text).target(expandedGroups.contains(s) ? 1.0F : 0.0F).update());
        float f1 = this.updateRowHover("gh" + s, value, value2, value3, 14.0F, value5, value6);
        float f2 = value2 + 7.0F;
        Fonts.SMALL.drawVCentered(graphics, text, value, f2, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-1929379841, -218103809, f1), value4));
        float f3 = value + Fonts.SMALL.getWidth(text) + 6.0F;
        float f4 = value + value3 - 13.0F;
        if (f4 > f3) {
            Render2D.fillRect(graphics, f3, f2, f4 - f3, 0.5F, ColorUtility.scaleAlpha(452984831, value4));
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(value + value3 - 4.0F, f2);
        graphics.pose().rotate((float) (-Math.PI / 2) * (1.0F - f));
        Fonts.ICONS.drawIconCentered(graphics, "\ue5cf", 0.0F, 0.0F, 0.8F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-2130706433, -1, f1), value4));
        graphics.pose().popMatrix();
        this.addClippedHitRegion(value, value2, value3, 14.0F, value5, value6, (button, mouseX, mouseY) -> {
            if (!expandedGroups.remove(s)) {
                expandedGroups.add(s);
            }
        }, null);
        value2 += 14.0F + 1.0F * f;
        float f5 = calcGroupBodyHeight(properties) * f;
        if (f5 > 0.5F) {
            Render2D.fillSubpixelRoundedRect(graphics, value - 5.0F, value2, value3 + 10.0F, f5, 5.0F, ColorUtility.scaleAlpha(184549375, value4));
            Render2D.drawRoundedBorderTextured(graphics, value - 5.0F, value2, value3 + 10.0F, f5, 5.0F, 1.0F, ColorUtility.scaleAlpha(268435455, value4));
            graphics.nextStratum();
            float f6 = Math.max(value5, value2);
            float f7 = Math.min(value6, value2 + f5);
            if (f7 > f6) {
                graphics.enableScissor((int)Math.floor(value - 6.0F), (int)f6, (int)Math.ceil(value + value3 + 6.0F), (int)Math.ceil(f7));
                float f8 = value2 + 4.0F;
                float f9 = value4 * clamp01(f * 1.4F);
                float f10 = value + 3.0F;
                float f11 = value3 - 6.0F;
                rowHoverPadLeft = rowHoverPadRight = 5.0F;

                try {
                    for (Property property : properties) {
                        f8 = this.renderProperty(graphics, property, f10, f8, f11, f9, f6, f7);
                    }
                } finally {
                    rowHoverPadLeft = 8.0F;
                    rowHoverPadRight = 4.0F;
                }

                graphics.disableScissor();
            }
        }

        return value2 + f5;
    }

    public float renderProperty(GuiGraphicsExtractor graphics, Property<?> property, float value, float value2, float value3, float value4, float value5, float value6) {
        String s = stripGroupPrefix(property);
        if (property instanceof MultiBooleanProperty multibooleanproperty) {
            return this.renderMultiSelectProperty(graphics, multibooleanproperty, s, value, value2, value3, value4, value5, value6);
        } else if (property instanceof BooleanProperty booleanproperty) {
            return this.renderBooleanRow(graphics, s, booleanproperty.getValue(), booleanproperty, value, value2, value3, value4, value5, value6, booleanproperty::toggle);
        } else if (!(property instanceof NumberProperty numberproperty)) {
            if (property instanceof EnumProperty enumproperty) {
                float f6 = this.updateRowHover(enumproperty, value, value2, value3, 15.0F, value5, value6);
                renderSettingRowHover(graphics, value, value2, value3, 15.0F, f6, value4);
                float f8 = this.renderEnumTrigger(graphics, enumproperty, value + value3, value2 + 7.5F, f6, value4);
                Fonts.SMALL
                    .drawVCentered(graphics, Fonts.SMALL.ellipsize(s, value3 - f8 - 6.0F), value, value2 + 7.5F, ColorUtility.scaleAlpha(getSettingLabelColor(f6), value4));
                this.addClippedHitRegion(value, value2, value3, 15.0F, value5, value6, (button, mouseX, mouseY) -> this.toggleEnumDropdown(enumproperty), null);
                return value2 + 16.0F;
            } else if (property instanceof ColorProperty colorproperty) {
                float f5 = this.updateRowHover(colorproperty, value, value2, value3, 15.0F, value5, value6);
                renderSettingRowHover(graphics, value, value2, value3, 15.0F, f5, value4);
                float f7 = this.renderColorTrigger(graphics, colorproperty, value + value3, value2 + 7.5F, f5, value4);
                Fonts.SMALL
                    .drawVCentered(graphics, Fonts.SMALL.ellipsize(s, value3 - f7 - 6.0F), value, value2 + 7.5F, ColorUtility.scaleAlpha(getSettingLabelColor(f5), value4));
                this.addClippedHitRegion(value, value2, value3, 15.0F, value5, value6, (button2, mouseX2, mouseY2) -> this.toggleColorPicker(colorproperty), null);
                return value2 + 16.0F;
            } else {
                return value2;
            }
        } else {
            float f = this.updateRowHover(numberproperty, value, value2, value3, 23.0F, value5, value6);
            renderSettingRowHover(graphics, value, value2, value3, 23.0F, f, value4);
            String s1 = numberproperty.getDisplayValue();
            Fonts.SMALL
                .drawVCentered(
                    graphics,
                    Fonts.SMALL.ellipsize(s, value3 - Fonts.SMALL.getWidth(s1) - 6.0F),
                    value,
                    value2 + 7.0F,
                    ColorUtility.scaleAlpha(getSettingLabelColor(f), value4)
                );
            Fonts.SMALL
                .drawRightAligned(
                    graphics,
                    s1,
                    value + value3,
                    value2 + 7.0F - Fonts.SMALL.getHalfPointSize(),
                    ColorUtility.scaleAlpha(ColorUtility.lerpColor(Theme.getValueText(), -1, f), value4)
                );
            float f1 = this.getSpring(numberproperty).target((float)numberproperty.getNormalized()).update();
            float f2 = this.getSpring("act" + System.identityHashCode(numberproperty))
                .target(this.activeDragHandler != null && this.activeDragOwner == numberproperty ? 1.0F : 0.0F)
                .update();
            Widgets.drawSlider(graphics, value + 1.0F, value2 + 17.0F, value3 - 2.0F, f1, f2, f, value4);
            float f3 = value;
            float f4 = value3;
            DashboardScreen.Drag dashboardscreen$drag = (mouseX3, mouseY3) -> numberproperty.setNormalized(Math.max(0.0, Math.min(1.0, (mouseX3 - f3) / f4)));
            this.addClippedHitRegion(value - 2.0F, value2 + 10.0F, value3 + 4.0F, 14.0F, value5, value6, (button3, mouseX4, mouseY4) -> {
                this.activeDragOwner = numberproperty;
                dashboardscreen$drag.onDrag(mouseX4, mouseY4);
            }, dashboardscreen$drag);
            return value2 + 24.0F;
        }
    }

    public static Integer getAccentPreviewColor(Object key) {
        if (key instanceof Accent accent) {
            if (accent == Accent.SPECTRUM) {
                return ColorUtility.fromHsb((float)(System.currentTimeMillis() % 6000L) / 6000.0F, 0.6F, 1.0F);
            } else if (accent == Accent.CUSTOM) {
                ThemeModule thememodule = Adapt.getInstance().getModuleManager().getByClass(ThemeModule.class);
                float f1 = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 1000.0 * 1.5);
                return ColorUtility.lerpColor(thememodule.primary.getValue(), thememodule.secondary.getValue(), f1);
            } else if (accent.secondaryColor != 0) {
                float f = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 1000.0 * 1.5);
                return ColorUtility.lerpColor(accent.primaryColor, accent.secondaryColor, f);
            } else {
                return accent.primaryColor;
            }
        } else {
            return null;
        }
    }

    public static String getEnumDisplayName(Object key) {
        return key instanceof Displayable displayable ? displayable.getDisplayName() : ((Enum)key).name();
    }

    public void toggleEnumDropdown(EnumProperty property) {
        if (this.openEnumProp == property && this.enumDropdownOpen) {
            this.enumDropdownOpen = false;
        } else {
            if (this.openEnumProp != property) {
                this.enumDropdownSpring.snap(0.0F);
                this.enumHoverAlphaSpring.snap(0.0F);
                int i = ((Enum)property.getValue()).ordinal();
                this.enumScrollTarget = Math.max(0.0F, (i - 4.0F) * 16.0F);
                this.enumScrollSpring.snap(this.enumScrollTarget);
            }

            this.openEnumProp = property;
            this.enumDropdownOpen = true;
            this.closeColorPicker();
        }
    }

    public void closeEnumDropdown() {
        this.enumDropdownOpen = false;
    }

    public void selectEnumValue(EnumProperty property, Object key) {
        if (property.getValue() != key) {
            property.setValue((Enum)key);
            this.getSpring(property).snap(-2.0F);
            this.getSpring(property).target(0.0F);
        }

        this.closeEnumDropdown();
    }

    public float renderEnumTrigger(GuiGraphicsExtractor graphics, EnumProperty property, float value, float value2, float value3, float value4) {
        String s = property.getDisplayValue();
        Integer integer = getAccentPreviewColor(property.getValue());
        float f = 0.0F;
        float f1 = 5.0F;
        float f2 = 5.0F;
        float f3 = integer != null ? 10.0F : 0.0F;
        float f4 = 0.0F + f3 + Fonts.SMALL.getWidth(s) + 4.0F + 5.0F;
        String s1 = springKeyOf(property);
        float f5 = this.springs.computeIfAbsent("dw" + s1, key -> createUiSpring(f4)).target(f4).update();
        float f6 = clamp01(this.getSpring("dc" + s1).target(this.openEnumProp == property && this.enumDropdownOpen ? 1.0F : 0.0F).update());
        float f7 = 12.0F;
        float f8 = value - f5;
        float f9 = value2 - 6.0F;
        if (this.openEnumProp == property) {
            this.enumTriggerBounds = new float[]{f8, f9, f5, 12.0F};
            this.enumTriggerVisible = true;
        }

        float f10 = f8 + 0.0F;
        if (integer != null) {
            Render2D.fillCircle(graphics, f10 + 2.5F, value2, 3.3F, ColorUtility.scaleAlpha(1090519039, value4));
            Render2D.fillCircle(graphics, f10 + 2.5F, value2, 2.5F, ColorUtility.scaleAlpha(integer, value4));
            f10 += f3;
        }

        float f11 = this.getSpring(property).update();
        graphics.pose().pushMatrix();
        graphics.pose().translate(f11 * 2.0F, 0.0F);
        Fonts.SMALL.drawVCentered(graphics, s, f10, value2, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-855638017, -1, Math.max(value3, f6)), value4));
        graphics.pose().popMatrix();
        graphics.pose().pushMatrix();
        graphics.pose().translate(value - 2.5F, value2);
        graphics.pose().rotate((float) Math.PI * f6);
        Fonts.ICONS
            .drawIconCentered(graphics, "\ue5cf", 0.0F, 0.0F, 0.8F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-2130706433, -1, Math.max(value3, f6)), value4));
        graphics.pose().popMatrix();
        return f5;
    }

    public void renderEnumDropdownPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.openEnumProp != null) {
            if (!this.enumTriggerVisible) {
                this.enumDropdownOpen = false;
            }

            float f = clamp01(this.enumDropdownSpring.target(this.enumDropdownOpen ? 1.0F : 0.0F).update());
            if (!this.enumDropdownOpen && f < 0.01F) {
                this.openEnumProp = null;
            } else {
                Enum[] aenum = this.openEnumProp.getValues();
                boolean flag = getAccentPreviewColor(aenum[0]) != null;
                float f1 = 0.0F;

                for (Enum oenum : aenum) {
                    f1 = Math.max(f1, Fonts.ROW.getWidth(getEnumDisplayName(oenum)));
                }

                float f15 = Math.max(this.enumTriggerBounds[2] + 20.0F, 12 + (flag ? 12 : 0) + f1 + 26.0F);
                int i1 = Math.min(aenum.length, 8);
                float f16 = i1 * 16.0F + 8.0F;
                float f17 = Math.max(4.0F, Math.min(this.width - f15 - 4.0F, this.enumTriggerBounds[0] + this.enumTriggerBounds[2] - f15));
                float f2 = this.enumTriggerBounds[1] + this.enumTriggerBounds[3] + 4.0F;
                boolean flag1 = f2 + f16 > this.height - 4;
                if (flag1) {
                    f2 = this.enumTriggerBounds[1] - 4.0F - f16;
                }

                float f3 = f16 * (0.4F + 0.6F * f);
                float f4 = (1.0F - f) * (flag1 ? 4 : -4);
                float f5 = (flag1 ? f2 + f16 - f3 : f2) + f4;
                this.enumPopupBounds = new float[]{f17, f5, f15, f3};
                float f6 = f * partialTick;
                graphics.nextStratum();
                Glass.drawPanel(graphics, f17, f5, f15, f3, 8.0F, f6, 0.6F);
                this.hitRegions.add(new DashboardScreen.Hit(f17, f5, f15, f3, (button, mouseX2, mouseY2) -> {}, null));
                float f7 = Math.max(0.0F, aenum.length * 16.0F - i1 * 16.0F);
                this.enumScrollTarget = Math.max(0.0F, Math.min(f7, this.enumScrollTarget));
                float f8 = this.enumScrollSpring.target(this.enumScrollTarget).update();
                float f9 = f2 + 4.0F + f4;
                graphics.enableScissor((int)f17, (int)Math.max(f5, f9 - 0.5F), (int)Math.ceil(f17 + f15), (int)Math.min(f5 + f3, f9 + i1 * 16.0F + 0.5F));
                int i = -1;
                if (this.enumDropdownOpen && Render2D.containsPoint(mouseX, mouseY, f17, f5, f15, f3)) {
                    int j = (int)Math.floor((mouseY - f9 + f8) / 16.0F);
                    if (j >= 0 && j < aenum.length) {
                        i = j;
                    }
                }

                if (i >= 0) {
                    if (this.enumHoverAlphaSpring.get() < 0.02F) {
                        this.enumHoverYSpring.snap(i * 16.0F);
                    }

                    this.enumHoverYSpring.target(i * 16.0F);
                }

                float f18 = clamp01(this.enumHoverAlphaSpring.target(i >= 0 ? 1.0F : 0.0F).update());
                float f10 = f9 + this.enumHoverYSpring.update() - f8;
                if (f18 > 0.01F) {
                    Render2D.fillSubpixelRoundedRect(graphics, f17 + 4.0F, f10 + 1.0F, f15 - 8.0F, 14.0F, 4.0F, ColorUtility.scaleAlpha(536870911, f6 * f18));
                }

                graphics.nextStratum();

                for (int k = 0; k < aenum.length; k++) {
                    Enum oenum1 = aenum[k];
                    float f11 = f9 + k * 16.0F - f8;
                    if (!(f11 + 16.0F < f5) && !(f11 > f5 + f3)) {
                        float f12 = f11 + 8.0F;
                        boolean flag2 = this.openEnumProp.getValue() == oenum1;
                        float f13 = clamp01(this.getSpring("ddi" + springKeyOf(this.openEnumProp) + k).target(k == i ? 1.0F : 0.0F).update());
                        float f14 = f17 + 12.0F;
                        Integer integer = getAccentPreviewColor(oenum1);
                        if (integer != null) {
                            Render2D.fillCircle(graphics, f14 + 3.0F, f12, 3.8F + 0.4F * f13, ColorUtility.scaleAlpha(1090519039, f6));
                            Render2D.fillCircle(graphics, f14 + 3.0F, f12, 3.0F + 0.4F * f13, ColorUtility.scaleAlpha(integer, f6));
                            f14 += 12.0F;
                        }

                        int l = flag2 ? -1 : ColorUtility.lerpColor(-1493172225, -1, f13);
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(f13 * 1.5F, 0.0F);
                        Fonts.ROW.drawVCentered(graphics, getEnumDisplayName(oenum1), f14, f12, ColorUtility.scaleAlpha(l, f6));
                        graphics.pose().popMatrix();
                        if (flag2) {
                            Fonts.ICONS.drawIconCentered(graphics, "\ue5ca", f17 + f15 - 13.0F, f12, 0.85F, ColorUtility.scaleAlpha(-637534209, f6));
                        }

                        if (this.enumDropdownOpen && f11 >= f5 - 1.0F && f11 + 16.0F <= f5 + f3 + 1.0F) {
                            EnumProperty enumproperty = this.openEnumProp;
                            this.hitRegions
                                .add(
                                    new DashboardScreen.Hit(
                                        f17, Math.max(f11, f5), f15, 16.0F, (button2, mouseX3, mouseY3) -> this.selectEnumValue(enumproperty, oenum1), null
                                    )
                                );
                        }
                    }
                }

                graphics.disableScissor();
                if (f7 > 0.0F) {
                    float f19 = i1 * 16.0F - 4.0F;
                    float f20 = Math.max(12.0F, f19 * i1 / aenum.length);
                    Render2D.fillRoundedRect(
                        graphics, f17 + f15 - 3.5F, f9 + 2.0F + (f19 - f20) * (f8 / f7), 1.5F, f20, 0.75F, ColorUtility.scaleAlpha(872415231, f6)
                    );
                }
            }
        }
    }

    public void toggleColorPicker(ColorProperty property) {
        this.hexInputFocused = false;
        if (this.openColorProp == property && this.colorPickerOpen) {
            this.colorPickerOpen = false;
        } else {
            if (this.openColorProp != property) {
                this.colorPickerSpring.snap(0.0F);
            }

            this.openColorProp = property;
            this.colorPickerOpen = true;
            this.closeEnumDropdown();
        }
    }

    public void closeColorPicker() {
        this.colorPickerOpen = false;
        this.hexInputFocused = false;
    }

    public static String formatHexRgb(int color) {
        return String.format("%06X", color & 16777215);
    }

    public float renderColorTrigger(GuiGraphicsExtractor graphics, ColorProperty property, float value, float value2, float value3, float value4) {
        String s = "#" + formatHexRgb(property.getValue());
        float f = 7.0F;
        float f1 = 5.0F;
        float f2 = 12.0F + Fonts.SMALL.getWidth(s) + 4.0F + 5.0F;
        float f3 = clamp01(this.getSpring("pc" + springKeyOf(property)).target(this.openColorProp == property && this.colorPickerOpen ? 1.0F : 0.0F).update());
        float f4 = value - f2;
        float f5 = 12.0F;
        float f6 = value2 - 6.0F;
        if (this.openColorProp == property) {
            this.colorTriggerBounds = new float[]{f4, f6, f2, 12.0F};
            this.colorTriggerVisible = true;
        }

        float f7 = Math.max(value3, f3);
        float f8 = f4 + 3.5F;
        Render2D.fillCircle(graphics, f8, value2, 4.3F, ColorUtility.scaleAlpha(1090519039, value4));
        Render2D.fillCircle(graphics, f8, value2, 3.5F, ColorUtility.scaleAlpha(property.getValue(), value4));
        graphics.nextStratum();
        Fonts.SMALL.drawVCentered(graphics, s, f4 + 7.0F + 5.0F, value2, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-855638017, -1, f7), value4));
        graphics.pose().pushMatrix();
        graphics.pose().translate(value - 2.5F, value2);
        graphics.pose().rotate((float) Math.PI * f3);
        Fonts.ICONS.drawIconCentered(graphics, "\ue5cf", 0.0F, 0.0F, 0.8F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-2130706433, -1, f7), value4));
        graphics.pose().popMatrix();
        return f2;
    }

    public static void renderSvGradientBox(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value, float value2, float value3) {
        Render2D.fillSampledRoundedRect(graphics, x, y, width, height, value2, value3, (first, second) -> ColorUtility.fromHsb(value, first, 1.0F - second));
        Render2D.drawRoundedBorderTextured(graphics, x, y, width, height, value2, 1.0F, ColorUtility.scaleAlpha(452984831, value3));
    }

    public static void renderHueBar(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float value) {
        float f = height / 2.0F;
        Render2D.fillSampledRoundedRect(
            graphics,
            x,
            y,
            width,
            height,
            f,
            value,
            (first, second) -> ColorUtility.fromHsb(Math.max(0.0F, Math.min(1.0F, (first * width - f) / (width - height))), 1.0F, 1.0F)
        );
    }

    public void renderColorPickerPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.openColorProp != null) {
            if (!this.colorTriggerVisible) {
                this.closeColorPicker();
            }

            float f = clamp01(this.colorPickerSpring.target(this.colorPickerOpen ? 1.0F : 0.0F).update());
            if (!this.colorPickerOpen && f < 0.01F) {
                this.openColorProp = null;
            } else {
                ColorProperty colorproperty = this.openColorProp;
                float[] afloat = colorproperty.getHsv();
                float f1 = 172.0F;
                float f2 = 142.0F;
                float f3 = Math.max(4.0F, Math.min(this.width - 172.0F - 4.0F, this.colorTriggerBounds[0] + this.colorTriggerBounds[2] - 172.0F));
                float f4 = this.colorTriggerBounds[1] + this.colorTriggerBounds[3] + 4.0F;
                boolean flag = f4 + 142.0F > this.height - 4;
                if (flag) {
                    f4 = this.colorTriggerBounds[1] - 4.0F - 142.0F;
                }

                float f5 = 142.0F * (0.4F + 0.6F * f);
                float f6 = (1.0F - f) * (flag ? 4 : -4);
                float f7 = (flag ? f4 + 142.0F - f5 : f4) + f6;
                this.colorPopupBounds = new float[]{f3, f7, 172.0F, f5};
                float f8 = f * partialTick;
                float f9 = f4 + f6;
                graphics.nextStratum();
                Glass.drawPanel(graphics, f3, f7, 172.0F, f5, 8.0F, f8, 0.6F);
                this.hitRegions.add(new DashboardScreen.Hit(f3, f7, 172.0F, f5, (button, mouseX2, mouseY2) -> this.hexInputFocused = false, null));
                graphics.enableScissor((int)f3, (int)f7, (int)Math.ceil(f3 + 172.0F), (int)Math.ceil(f7 + f5));
                float f10 = f3 + 8.0F;
                float f11 = f9 + 8.0F;
                float f12 = 156.0F;
                float f13 = 84.0F;
                renderSvGradientBox(graphics, f10, f11, 156.0F, 84.0F, afloat[0], 5.0F, f8);
                graphics.nextStratum();
                float f14 = clamp01(this.getSpring("cpsv").target(this.activeDragHandler != null && this.activeDragOwner == "sv" ? 1.0F : 0.0F).update());
                float f15 = f10 + afloat[1] * 156.0F;
                float f16 = f11 + (1.0F - afloat[2]) * 84.0F;
                float f17 = 5.0F + f14;
                Render2D.drawCircleShadow(graphics, f15, f16, f17, 2.5F, ColorUtility.scaleAlpha(1711276032, f8));
                Render2D.fillCircle(graphics, f15, f16, f17, ColorUtility.scaleAlpha(-1, f8));
                Render2D.fillCircle(graphics, f15, f16, f17 - 1.6F, ColorUtility.scaleAlpha(colorproperty.getValue(), f8));
                if (this.colorPickerOpen) {
                    DashboardScreen.Drag dashboardscreen$drag = (mouseX3, mouseY3) -> colorproperty.setSaturationBrightness(
                        (float)((mouseX3 - f10) / 156.0), (float)(1.0 - (mouseY3 - f11) / 84.0)
                    );
                    this.hitRegions.add(new DashboardScreen.Hit(f10 - 3.0F, f11 - 3.0F, 162.0F, 90.0F, (button2, mouseX4, mouseY4) -> {
                        this.activeDragOwner = "sv";
                        this.hexInputFocused = false;
                        dashboardscreen$drag.onDrag(mouseX4, mouseY4);
                    }, dashboardscreen$drag));
                }

                float f30 = f10;
                float f18 = f11 + 84.0F + 8.0F;
                float f19 = 156.0F;
                renderHueBar(graphics, f30, f18, 156.0F, 8.0F, f8);
                graphics.nextStratum();
                float f20 = clamp01(
                    this.getSpring("cphue")
                        .target(
                            (this.activeDragHandler == null || this.activeDragOwner != "hue")
                                    && !Render2D.containsPoint(mouseX, mouseY, f30, f18 - 3.0F, 156.0F, 14.0F)
                                ? 0.0F
                                : 1.0F
                        )
                        .update()
                );
                float f21 = f30 + 4.0F + afloat[0] * 148.0F;
                float f22 = f18 + 4.0F;
                float f23 = 5.5F + 0.7F * f20;
                Render2D.drawCircleShadow(graphics, f21, f22, f23, 2.5F, ColorUtility.scaleAlpha(1711276032, f8));
                Render2D.fillCircle(graphics, f21, f22, f23, ColorUtility.scaleAlpha(-1, f8));
                Render2D.fillCircle(graphics, f21, f22, f23 - 1.6F, ColorUtility.scaleAlpha(ColorUtility.fromHsb(afloat[0], 1.0F, 1.0F), f8));
                if (this.colorPickerOpen) {
                    DashboardScreen.Drag dashboardscreen$drag1 = (mouseX5, mouseY5) -> colorproperty.setHue(
                        (float)Math.max(0.0, Math.min(1.0, (mouseX5 - f30 - 4.0) / 148.0))
                    );
                    this.hitRegions.add(new DashboardScreen.Hit(f30 - 3.0F, f18 - 4.0F, 162.0F, 16.0F, (button3, mouseX6, mouseY6) -> {
                        this.activeDragOwner = "hue";
                        this.hexInputFocused = false;
                        dashboardscreen$drag1.onDrag(mouseX6, mouseY6);
                    }, dashboardscreen$drag1));
                }

                float f31 = f18 + 8.0F + 10.0F;
                float f24 = f31 + 8.0F;
                Render2D.fillSubpixelRoundedRect(graphics, f10, f31, 16.0F, 16.0F, 4.0F, ColorUtility.scaleAlpha(colorproperty.getValue(), f8));
                Render2D.drawRoundedBorderTextured(graphics, f10, f31, 16.0F, 16.0F, 4.0F, 1.0F, ColorUtility.scaleAlpha(872415231, f8));
                float f25 = f10 + 16.0F + 6.0F;
                float f26 = 56.0F;
                float f27 = clamp01(
                    this.getSpring("cphex").target(!this.hexInputFocused && !Render2D.containsPoint(mouseX, mouseY, f25, f31, 56.0F, 16.0F) ? 0.0F : 1.0F).update()
                );
                Render2D.fillSubpixelRoundedRect(
                    graphics, f25, f31 + 1.0F, 56.0F, 14.0F, 4.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(268435455, 536870911, f27), f8)
                );
                Render2D.drawRoundedBorderTextured(
                    graphics, f25, f31 + 1.0F, 56.0F, 14.0F, 4.0F, 1.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(452984831, 872415231, f27), f8)
                );
                graphics.nextStratum();
                String s = "#" + (this.hexInputFocused ? this.hexInput.toString() : formatHexRgb(colorproperty.getValue()));
                Fonts.SMALL.drawVCentered(graphics, s, f25 + 6.0F, f24, ColorUtility.scaleAlpha(this.hexInputFocused ? -1 : -855638017, f8));
                if (this.hexInputFocused) {
                    long i = System.currentTimeMillis() - this.hexLastEditAt;
                    float f28 = i < 500L ? 1.0F : clamp01(0.5F + 1.4F * (float)Math.cos((i - 500L) / 1060.0 * Math.PI * 2.0));
                    Render2D.fillRect(
                        graphics, f25 + 6.0F + Fonts.SMALL.getWidth(s) + 1.0F, f24 - 4.0F, 0.75F, 8.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), f8 * f28)
                    );
                }

                if (this.colorPickerOpen) {
                    this.hitRegions.add(new DashboardScreen.Hit(f25, f31, 56.0F, 16.0F, (button4, mouseX7, mouseY7) -> {
                        this.hexInputFocused = true;
                        this.hexInput.setLength(0);
                        this.hexInput.append(formatHexRgb(colorproperty.getValue()));
                        this.hexLastEditAt = System.currentTimeMillis();
                    }, null));
                }

                float f32 = f10 + 156.0F - 4.5F;

                for (int j = COLOR_PRESETS.length - 1; j >= 0; j--) {
                    int k = COLOR_PRESETS[j];
                    boolean flag1 = Render2D.containsPoint(mouseX, mouseY, f32 - 6.0F, f24 - 6.0F, 12.0F, 12.0F);
                    float f29 = clamp01(this.getSpring("cpp" + j).target(flag1 ? 1.0F : 0.0F).update());
                    boolean flag2 = (colorproperty.getValue() & 16777215) == (k & 16777215);
                    if (flag2) {
                        Render2D.fillCircle(graphics, f32, f24, 6.2F, ColorUtility.scaleAlpha(-2130706433, f8));
                    }

                    Render2D.fillCircle(graphics, f32, f24, (flag2 ? 5.0F : 4.5F) + 0.8F * f29, ColorUtility.scaleAlpha(k, f8));
                    if (this.colorPickerOpen) {
                        this.hitRegions.add(new DashboardScreen.Hit(f32 - 6.0F, f24 - 6.0F, 12.0F, 12.0F, (button5, mouseX8, mouseY8) -> {
                            colorproperty.setRgb(k);
                            this.hexInputFocused = false;
                        }, null));
                    }

                    f32 -= 12.5F;
                }

                graphics.disableScissor();
            }
        }
    }

    public void commitHexColor() {
        if (this.openColorProp != null && this.hexInput.length() == 6) {
            try {
                this.openColorProp.setRgb(0xFF000000 | Integer.parseInt(this.hexInput.toString(), 16));
            } catch (NumberFormatException numberformatexception) {
            }
        }

        this.hexInputFocused = false;
    }

    public static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public void renderKeybindBadge(
        GuiGraphicsExtractor graphics, String key, String key2, boolean flag, float value, float value2, float value3, float value4, float value5
    ) {
        String s = flag ? "Press a key" : (key2 == null ? "None" : key2);
        float f = Fonts.SMALL.getWidth(s) + 10.0F;
        float f1 = this.springs.computeIfAbsent("kw" + key, key3 -> createUiSpring(f)).target(f).update();
        float f2 = clamp01(this.getSpring("kl" + key).target(flag ? 1.0F : 0.0F).update());
        float f3 = clamp01(this.springs.computeIfAbsent("kf" + key, key4 -> new Spring(0.0F, 90.0F, 19.0F)).target(0.0F).update());
        float f4 = value3 * clamp01(Math.max(value4, Math.max(f2, f3)));
        Spring spring = this.springs.computeIfAbsent("kx" + key, key5 -> createUiSpring(1.0F));
        String s1 = this.keybindLabels.put(key, s);
        if (s1 != null && !s1.equals(s)) {
            this.prevKeybindLabels.put(key, s1);
            spring.snap(0.0F);
        }

        float f5 = clamp01(spring.target(1.0F).update());
        if (!(f4 <= 0.01F)) {
            float f6 = 12.0F;
            float f7 = value - f1;
            float f8 = value2 - 6.0F;
            float f9 = Math.max(Math.max(f2 * 0.8F, f3), value5 * 0.6F);
            Glass.drawPanel(graphics, f7, f8, f1, 12.0F, 5.0F, f4, 0.12F, 0.35F);
            Render2D.fillSubpixelRoundedRect(graphics, f7, f8, f1, 12.0F, 5.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(150994943, 486539263, f9), f4));
            graphics.nextStratum();
            float f10 = f7 + f1 / 2.0F;
            float f11 = Render2D.snapToPixel(f10);
            float f12 = Render2D.snapToPixel(value2);
            float f13 = flag ? 0.7F + 0.3F * (float)(0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1000.0 * 4.0)) : 1.0F;
            int i = ColorUtility.scaleAlpha(flag ? -1 : (key2 == null ? -1711276033 : -1073741825), f13);
            graphics.enableScissor((int)Math.floor(f7), (int)Math.floor(f8), (int)Math.ceil(f7 + f1), (int)Math.ceil(f8 + 12.0F));
            String s2 = this.prevKeybindLabels.get(key);
            if (s2 != null && f5 < 0.99F) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(f10 - f11, value2 - f12 - f5 * 3.0F);
                Fonts.SMALL.drawCentered(graphics, s2, f11, f12, ColorUtility.scaleAlpha(-1073741825, f4 * (1.0F - f5) * (1.0F - f5)));
                graphics.pose().popMatrix();
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(f10 - f11, value2 - f12 + (1.0F - f5) * 3.0F);
            Fonts.SMALL.drawCentered(graphics, s, f11, f12, ColorUtility.scaleAlpha(i, f4 * f5 * f5));
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }
    }

    public void flashModuleKeybind(Module module) {
        this.springs.computeIfAbsent("kf" + module.getId() + "s", key -> new Spring(0.0F, 90.0F, 19.0F)).snap(1.0F);
    }

    public float renderBooleanRow(
        GuiGraphicsExtractor graphics,
        String text,
        boolean flag,
        Object key,
        float value,
        float value2,
        float value3,
        float value4,
        float value5,
        float value6,
        Runnable action
    ) {
        float f = this.updateRowHover(key, value, value2, value3, 15.0F, value5, value6);
        renderSettingRowHover(graphics, value, value2, value3, 15.0F, f, value4);
        Fonts.SMALL
            .drawVCentered(graphics, Fonts.SMALL.ellipsize(text, value3 - 18.0F - 6.0F), value, value2 + 7.5F, ColorUtility.scaleAlpha(getSettingLabelColor(f), value4));
        float f1 = this.getSpring(key).target(flag ? 1.0F : 0.0F).update();
        Spring spring = this.getClickFlashSpring(key);
        Widgets.drawSwitch(graphics, value + value3 - 18.0F, value2 + 2.5F, f1, f, clamp01(spring.target(0.0F).update()), value4);
        this.addClippedHitRegion(value, value2, value3, 15.0F, value5, value6, (button, mouseX, mouseY) -> {
            action.run();
            spring.snap(1.0F);
        }, null);
        return value2 + 16.0F;
    }

    public void addClippedHitRegion(
        float value, float value2, float value3, float value4, float value5, float value6, DashboardScreen.Click click, DashboardScreen.Drag drag
    ) {
        float f = Math.max(value2, value5);
        float f1 = Math.min(value2 + value4, value6);
        if (f1 > f) {
            this.hitRegions.add(new DashboardScreen.Hit(value, f, value3, f1 - f, click, drag));
        }
    }

    public float getChatShiftOffset() {
        return this.chatShiftSpring.get() * this.columnsChatShift;
    }

    public float getChatPanelX() {
        return chatCustomMoved
            ? chatCustomPos[0]
            : Math.max(0.0F, Math.min(this.width - 140.0F - 8.0F, this.columnsRightEdge - this.columnsChatShift + this.columnGap));
    }

    public float getChatPanelY() {
        return chatCustomMoved ? chatCustomPos[1] : 46.0F;
    }

    public static String formatChatTimestamp(long delayMs) {
        return Instant.ofEpochMilli(delayMs).atZone(ZoneId.systemDefault()).format(CHAT_TIME_FORMAT);
    }

    public static List wrapTextLines(Fonts font, String text, float value) {
        ArrayList arraylist = new ArrayList();

        for (String s : text.split("\n", -1)) {
            StringBuilder stringbuilder = new StringBuilder();

            for (String s1 : s.split(" ", -1)) {
                String s2 = stringbuilder.isEmpty() ? s1 : stringbuilder + " " + s1;
                if (font.getWidth(s2) <= value) {
                    stringbuilder.setLength(0);
                    stringbuilder.append(s2);
                } else {
                    if (!stringbuilder.isEmpty()) {
                        arraylist.add(stringbuilder.toString());
                        stringbuilder.setLength(0);
                    }

                    String s3 = s1;

                    while (font.getWidth(s3) > value && s3.length() > 1) {
                        int i = s3.length() - 1;

                        while (i > 1 && font.getWidth(s3.substring(0, i)) > value) {
                            i--;
                        }

                        arraylist.add(s3.substring(0, i));
                        s3 = s3.substring(i);
                    }

                    stringbuilder.append(s3);
                }
            }

            arraylist.add(stringbuilder.toString());
        }

        return arraylist;
    }

    // VENDOR-DISABLED: Online chat panel UI methods (renderChatPanel..renderChatInputBox) referencing ChatService
    /*
    public void renderChatPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float f = this.chatOpenSpring.get();
        if (f < 0.005F && !chatOpen) {
            this.chatPanelBounds[2] = 0.0F;
            this.chatInputFocused = false;
        } else {
            int i = this.hitRegions.size();
            ChatService chatservice = ChatService.getInstance();
            ChatService.Channel chatservice$channel = chatservice.getActiveChannel();
            int j = ModuleCategory.VALUES.length;
            float f1 = Math.max(0.0F, Math.min(1.0F, this.openSpring.get() * 1.4F - j * 0.1F));
            float f2 = partialTick * f1 * clamp01(f);
            float f3 = 140.0F;
            float f4 = this.getChatPanelY();
            float f5 = Math.max(140.0F, Math.min(220.0F, this.height - f4 - 72.0F));
            float f6 = this.getChatPanelX();
            float f7 = f4 + (1.0F - f1) * -16.0F + (1.0F - f) * 70.0F;
            this.chatPanelBounds[0] = f6;
            this.chatPanelBounds[1] = f7;
            this.chatPanelBounds[2] = 140.0F;
            this.chatPanelBounds[3] = f5;
            float f8 = this.getSpring("liftChat").target(this.draggingChat ? 1.0F : 0.0F).update();
            if (f8 > 0.01F) {
                Render2D.drawDropShadow(graphics, f6, f7 + 6.0F, 140.0F, f5, 10.0F, 26.0F, ColorUtility.scaleAlpha(1610612736, f2 * f8));
            }

            Render2D.drawDropShadow(graphics, f6, f7 + 2.0F, 140.0F, f5, 10.0F, 14.0F, ColorUtility.scaleAlpha(1207959552, f2));
            Glass.drawBorderlessPanel(graphics, f6, f7, 140.0F, f5, 10.0F, f2, 0.55F, 1.0F, -1509159920);
            this.hitRegions.add(new DashboardScreen.Hit(f6, f7, 140.0F, f5, (button, mouseX2, mouseY2) -> this.chatBroughtToFront = true, null));
            float f9 = 14.0F;
            Fonts.ICONS.drawIconCentered(graphics, "\ue0bf", f6 + 9.0F + 5.0F, f7 + 14.0F, 1.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), f2));
            Fonts.SEMIBOLD.draw(graphics, "Chat", f6 + 9.0F + 15.0F, f7 + 14.0F - Fonts.SEMIBOLD.getHalfPointSize(), ColorUtility.scaleAlpha(-1, f2));
            int k = chatservice.getOnlineCount();
            if (k >= 0) {
                String s = String.valueOf(k);
                float f10 = Fonts.SMALL.getWidth(s);
                Fonts.SMALL.drawRightAligned(graphics, s, f6 + 140.0F - 9.0F, f7 + 14.0F - Fonts.SMALL.getHalfPointSize(), ColorUtility.scaleAlpha(1509949439, f2));
                Render2D.fillCircleAligned(graphics, f6 + 140.0F - 9.0F - f10 - 5.0F, f7 + 14.0F, 1.8F, ColorUtility.scaleAlpha(-10825066, f2));
            }

            Render2D.fillRect(graphics, f6 + 9.0F + 2.0F, f7 + 28.0F - 1.0F, 118.0F, 0.5F, ColorUtility.scaleAlpha(452984831, f2));
            this.hitRegions.add(new DashboardScreen.Hit(f6, f7, 140.0F, 28.0F, (button2, mouseX3, mouseY3) -> {
                if (!chatCustomMoved) {
                    chatCustomPos[0] = this.getChatPanelX();
                    chatCustomPos[1] = this.getChatPanelY();
                    chatCustomMoved = true;
                }

                this.draggingChat = true;
                this.chatBroughtToFront = true;
                this.chatDragOffX = (float)mouseX3 - chatCustomPos[0];
                this.chatDragOffY = (float)mouseY3 - chatCustomPos[1];
            }, null));
            float f20 = f6 + 9.0F - 2.0F;
            float f21 = f7 + 28.0F + 5.0F;
            float f11 = 126.0F;
            Glass.drawPanel(graphics, f20, f21, 126.0F, 15.0F, 5.0F, f2, 0.12F, 0.35F);
            float f12 = 63.0F;
            float f13 = clamp01(this.getSpring("chatTab").target(chatservice$channel == ChatService.Channel.WEB ? 1.0F : 0.0F).update());
            Render2D.fillSubpixelRoundedRect(graphics, f20 + 1.5F + 61.5F * f13, f21 + 1.5F, 61.5F, 12.0F, 4.0F, ColorUtility.scaleAlpha(536870911, f2));

            for (ChatService.Channel chatservice$channel1 : ChatService.Channel.values()) {
                int l = chatservice$channel1.ordinal();
                float f14 = f20 + 63.0F * l + 31.5F;
                float f15 = f21 + 7.5F;
                boolean flag = chatservice$channel1 == chatservice$channel;
                float f16 = clamp01(
                    this.getSpring("chatTabHov" + l)
                        .target(!flag && Render2D.containsPoint(mouseX, mouseY, f20 + 63.0F * l, f21, 63.0F, 15.0F) ? 1.0F : 0.0F)
                        .update()
                );
                int i1 = flag ? -1 : ColorUtility.lerpColor(-2130706433, -855638017, f16);
                Fonts.SMALL.drawCentered(graphics, chatservice$channel1.label, f14, f15, ColorUtility.scaleAlpha(i1, f2));
                int j1 = chatservice.getUnreadCount(chatservice$channel1);
                if (!flag && j1 > 0) {
                    String s1 = j1 > 9 ? "9+" : String.valueOf(j1);
                    float f17 = f14 + Fonts.SMALL.getWidth(chatservice$channel1.label) / 2.0F + 4.0F;
                    float f18 = Math.max(9.0F, Fonts.SMALL.getWidth(s1) + 5.0F);
                    Render2D.fillSubpixelRoundedRect(graphics, f17, f15 - 4.5F, f18, 9.0F, 4.5F, ColorUtility.scaleAlpha(Theme.getAccentColor(), f2));
                    Fonts.SMALL.drawCentered(graphics, s1, f17 + f18 / 2.0F, f15, ColorUtility.scaleAlpha(-15724524, f2));
                }

                this.hitRegions.add(new DashboardScreen.Hit(f20 + 63.0F * l, f21, 63.0F, 15.0F, (button3, mouseX4, mouseY4) -> {
                    this.chatBroughtToFront = true;
                    if (chatservice.getActiveChannel() != chatservice$channel1) {
                        chatservice.setActiveChannel(chatservice$channel1);
                        this.chatScrollTarget = 0.0F;
                        this.replyingToMessage = null;
                        if (this.editingMessage != null) {
                            this.cancelEditMessage();
                        }

                        this.onUiFeedback(1.5F);
                    }
                }, null));
            }

            float f22 = f7 + f5 - 7.0F - 18.0F;
            boolean flag2 = this.chatErrorBanner != null && System.currentTimeMillis() - this.chatErrorAt < 5000L;
            boolean flag3 = this.replyingToMessage != null || this.editingMessage != null || flag2;
            float f23 = flag3 ? 14.0F : 0.0F;
            float f24 = f6 + 9.0F - 2.0F;
            float f25 = 126.0F;
            float f26 = f22 - f23;
            float f27 = f21 + 15.0F + 4.0F;
            float f28 = f26 - 6.0F;
            this.renderChatMessages(graphics, chatservice, chatservice$channel, f6, 140.0F, f27, f28, f2, mouseX, mouseY);
            Glass.drawPanel(graphics, f24, f26, 126.0F, f22 + 18.0F - f26, 7.0F, f2, 0.12F, 0.35F);
            if (flag3) {
                Render2D.fillRect(graphics, f24 + 6.0F, f22 - 0.25F, 114.0F, 0.5F, ColorUtility.scaleAlpha(452984831, f2));
            }

            if (flag3) {
                float f29 = f26 + f23 / 2.0F;
                boolean flag4 = this.editingMessage != null;
                String s2 = flag2 ? "\ue002" : (flag4 ? "\ue3c9" : "\ue15e");
                String s3 = flag2 ? this.chatErrorBanner : (flag4 ? "Editing message" : "Replying to " + this.replyingToMessage.username());
                int k1 = flag2 ? -25950 : -1493172225;
                Fonts.ICONS.drawIconCentered(graphics, s2, f24 + 7.5F, f29, 0.7F, ColorUtility.scaleAlpha(flag2 ? k1 : -1711276033, f2));
                Fonts.SMALL.drawVCentered(graphics, Fonts.SMALL.ellipsize(s3, 99.0F), f24 + 14.0F, f29, ColorUtility.scaleAlpha(k1, f2));
                float f19 = f24 + 126.0F - 7.5F;
                boolean flag1 = Render2D.containsPoint(mouseX, mouseY, f19 - 6.0F, f26, 12.0F, f23);
                Fonts.ICONS.drawIconCentered(graphics, "\ue5cd", f19, f29, 0.7F, ColorUtility.scaleAlpha(flag1 ? -1 : -2130706433, f2));
                this.hitRegions.add(new DashboardScreen.Hit(f19 - 6.0F, f26, 12.0F, f23, (button4, mouseX5, mouseY5) -> {
                    if (flag2) {
                        this.chatErrorBanner = null;
                    } else {
                        if (this.editingMessage != null) {
                            this.cancelEditMessage();
                        }

                        this.replyingToMessage = null;
                    }
                }, null));
            }

            this.renderChatInputBox(graphics, chatservice, chatservice$channel, f24, f22, 126.0F, f2, mouseX, mouseY);
            if (!this.isChatInteractive()) {
                this.hitRegions.subList(i, this.hitRegions.size()).clear();
            }
        }
    }

    public void renderOnlineChatButton(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int first, int startColor, float second) {
        boolean flag = this.draggingCategory == null && Render2D.containsPoint(first, startColor, x, y, width, height);
        float f = clamp01(this.getSpring("chatBtnHov").target(flag ? 1.0F : 0.0F).update());
        float f1 = clamp01(this.getSpring("chatBtnOn").target(chatOpen ? 1.0F : 0.0F).update());
        int i = -10825066;
        int j = -34686;
        int k = ColorUtility.lerpColor(-10825066, -34686, f);
        Glass.drawPanel(graphics, x, y, width, height, 7.0F, second, 0.5F);
        if (f * (1.0F - f1) > 0.01F) {
            Render2D.fillSubpixelRoundedRect(graphics, x, y, width, height, 7.0F, ColorUtility.scaleAlpha(268435455, second * f * (1.0F - f1)));
            Render2D.drawRoundedBorderTextured(graphics, x, y, width, height, 7.0F, 1.0F, ColorUtility.scaleAlpha(788529151, second * f * (1.0F - f1)));
        }

        if (f1 > 0.01F) {
            Render2D.fillSubpixelRoundedRect(
                graphics, x, y, width, height, 7.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(k, 46 + (int)(12.0F * f)), second * f1)
            );
            Render2D.drawRoundedBorderTextured(graphics, x, y, width, height, 7.0F, 1.0F, ColorUtility.scaleAlpha(ColorUtility.withAlpha(k, 140), second * f1));
        }

        float f2 = y + height / 2.0F;
        int l = ColorUtility.lerpColor(ColorUtility.lerpColor(-1711276033, Theme.getAccentColor(), f), k, f1);
        Fonts.ICONS.drawIconCentered(graphics, "\ue0bf", x + 12.0F, f2, 0.9F, ColorUtility.scaleAlpha(l, second));
        Fonts.SMALL
            .drawVCentered(graphics, "Online Chat", x + 21.0F, f2, ColorUtility.scaleAlpha(ColorUtility.lerpColor(-855638017, -1, Math.max(f, f1)), second));
        this.hitRegions.add(new DashboardScreen.Hit(x, y, width, height, (button, mouseX, mouseY) -> {
            if (button == 1) {
                chatOpen = !chatOpen;
                ChatService.getInstance().setPolling(chatOpen);
                if (chatOpen) {
                    this.chatBroughtToFront = true;
                } else {
                    this.chatInputFocused = false;
                    this.replyingToMessage = null;
                    if (this.editingMessage != null) {
                        this.cancelEditMessage();
                    }
                }

                this.onUiFeedback(1.5F);
            }
        }, null));
    }

    public void renderChatMessages(
        GuiGraphicsExtractor graphics, ChatService chatService, ChatService.Channel channel, float value, float value2, float value3, float value4, float value5, int value6, int value7
    ) {
        List<ChatService.Message> list = chatService.getMessages(channel);
        String s = chatService.getErrorMessage();
        float f = value + 5.0F;
        float f1 = value2 - 10.0F;
        float f2 = f + 4.0F;
        float f3 = f2 + 13.0F + 5.0F;
        float f4 = f + f1 - 4.0F - f3;
        if (list.isEmpty()) {
            float f20 = value2 - 18.0F - 10.0F;
            String s2 = s != null
                ? s
                : (
                    !chatService.isLoaded(channel)
                        ? "Loading…"
                        : (channel == ChatService.Channel.IRC ? "No messages yet. Say hi to other Adapt players." : "No messages yet.")
                );
            List<String> list2 = wrapTextLines(Fonts.SMALL, s2, f20);
            float f22 = (value3 + value4) / 2.0F - list2.size() * 9.5F / 2.0F;

            for (String s3 : list2) {
                Fonts.SMALL.drawCenteredX(graphics, s3, value + value2 / 2.0F, f22, ColorUtility.scaleAlpha(s != null ? -855663966 : 1728053247, value5));
                f22 += 9.5F;
            }
        } else {
            List<DashboardScreen.ChatRow> arraylist = new ArrayList<>();
            float f5 = 0.0F;
            ChatService.Message chatservice$message = null;

            for (ChatService.Message chatservice$message1 : list) {
                boolean flag = chatservice$message == null
                    || chatservice$message.userId() != chatservice$message1.userId()
                    || chatservice$message1.isReply()
                    || chatservice$message1.createdAt() - chatservice$message.createdAt() > 300000L;
                List<String> list1 = wrapTextLines(Fonts.SMALL, chatservice$message1.body() + (chatservice$message1.edited() ? " (edited)" : ""), f4);
                float f6 = list1.size() * 9.5F;
                float f7 = flag ? (chatservice$message1.isReply() ? 11.0F : 0.0F) + Math.max(13.0F, 10.5F + f6) : f6;
                float f8 = f7 + (flag ? 6 : 3);
                float f9 = chatservice$message == null ? 0.0F : (flag ? 5.0F : 0.0F);
                arraylist.add(new DashboardScreen.ChatRow(chatservice$message1, flag, list1, f8, f9));
                f5 += f8 + f9;
                chatservice$message = chatservice$message1;
            }

            float f21 = value4 - value3;
            float f23 = Math.max(0.0F, f5 - f21 + 4.0F);
            this.chatScrollTarget = Math.max(0.0F, Math.min(f23, this.chatScrollTarget));
            float f24 = this.chatScrollSpring.target(this.chatScrollTarget).update();
            long k1 = chatService.getSelfUserId();
            long l1 = System.currentTimeMillis();
            boolean flag6 = !this.draggingChat && this.isChatInteractive() && value7 >= value3 && value7 < value4 && value6 >= f && value6 < f + f1;
            boolean flag1 = this.hoveredMessageId >= 0L
                && Render2D.containsPoint(
                    value6, value7, this.messageToolbarBounds[0], this.messageToolbarBounds[1], this.messageToolbarBounds[2], this.messageToolbarBounds[3]
                );
            long i = flag1 ? this.hoveredMessageId : -1L;
            float f10 = value4 - 2.0F - f5 + f24;
            float[] afloat = new float[arraylist.size()];

            for (int j = 0; j < arraylist.size(); j++) {
                f10 += ((DashboardScreen.ChatRow)arraylist.get(j)).gapAbove();
                afloat[j] = f10;
                if (i < 0L && flag6 && value7 >= f10 && value7 < f10 + ((DashboardScreen.ChatRow)arraylist.get(j)).h()) {
                    i = ((DashboardScreen.ChatRow)arraylist.get(j)).m().id();
                }

                f10 += ((DashboardScreen.ChatRow)arraylist.get(j)).h();
            }

            this.hoveredMessageId = i;
            graphics.enableScissor((int)value, (int)value3, (int)(value + value2), (int)Math.ceil(value4));
            DashboardScreen.ChatRow dashboardscreen$chatrow1 = null;
            float f11 = 0.0F;

            for (int k = 0; k < arraylist.size(); k++) {
                DashboardScreen.ChatRow dashboardscreen$chatrow = (DashboardScreen.ChatRow)arraylist.get(k);
                ChatService.Message chatservice$message2 = dashboardscreen$chatrow.m();
                float f12 = afloat[k];
                if (!(f12 + dashboardscreen$chatrow.h() < value3 - 4.0F) && !(f12 > value4 + 4.0F)) {
                    boolean flag2 = chatservice$message2.userId() == k1;
                    boolean flag3 = chatservice$message2.id() == i;
                    float f13 = clamp01(this.getSpring("chatMsg" + chatservice$message2.id()).target(flag3 ? 1.0F : 0.0F).update());
                    boolean flag4 = !flag2 && chatservice$message2.isReply() && chatservice$message2.replyUserId() == k1;
                    boolean flag5 = this.replyingToMessage != null && this.replyingToMessage.id() == chatservice$message2.id()
                        || this.editingMessage != null && this.editingMessage.id() == chatservice$message2.id();
                    if (flag4 || flag5) {
                        Render2D.fillSubpixelRoundedRect(
                            graphics,
                            f,
                            f12,
                            f1,
                            dashboardscreen$chatrow.h(),
                            4.0F,
                            ColorUtility.scaleAlpha(ColorUtility.withAlpha(Theme.getAccentColor(), flag5 ? 38 : 23), value5)
                        );
                        Render2D.fillRoundedRect(
                            graphics, f, f12 + 1.0F, 1.2F, dashboardscreen$chatrow.h() - 2.0F, 0.6F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value5)
                        );
                    }

                    if (f13 > 0.01F) {
                        Render2D.fillSubpixelRoundedRect(graphics, f, f12, f1, dashboardscreen$chatrow.h(), 4.0F, ColorUtility.scaleAlpha(268435455, value5 * f13));
                    }

                    float f14 = f12 + (dashboardscreen$chatrow.head() ? 3.0F : 1.5F);
                    if (dashboardscreen$chatrow.head()) {
                        if (chatservice$message2.isReply()) {
                            float f15 = f14 + 5.5F - 0.5F;
                            float f16 = f2 + 6.5F - 0.4F;
                            int l = ColorUtility.scaleAlpha(956301311, value5);
                            float f17 = 3.5F;
                            int i1 = (int)Math.max(Math.floor(f15 - 1.0F), value3);
                            int j1 = (int)Math.min(Math.ceil(f15 + 3.5F), value4);
                            if (j1 > i1) {
                                graphics.enableScissor((int)Math.floor(f16 - 1.0F), i1, (int)Math.ceil(f16 + 3.5F), j1);
                                Render2D.drawRoundedBorderTextured(graphics, f16, f15, 14.0F, 14.0F, 3.5F, 0.8F, l);
                                graphics.disableScissor();
                            }

                            Render2D.fillRect(graphics, f16, f15 + 3.5F, 0.8F, f14 + 11.0F + 1.0F - (f15 + 3.5F), l);
                            Render2D.fillRect(graphics, f16 + 3.5F, f15, f3 - 3.0F - (f16 + 3.5F), 0.8F, l);
                            if (chatservice$message2.replyUser() != null) {
                                Render2D.fillSubpixelRoundedRect(graphics, f3, f15 - 3.5F, 7.0F, 7.0F, 2.0F, ColorUtility.scaleAlpha(-2306070, value5));
                                Fonts.SMALL
                                    .drawIconCentered(
                                        graphics,
                                        chatservice$message2.replyUser().substring(0, 1).toUpperCase(Locale.ROOT),
                                        f3 + 3.5F,
                                        f15,
                                        0.62F,
                                        ColorUtility.scaleAlpha(-15264737, value5)
                                    );
                                float f18 = f3 + 9.0F;
                                Fonts.SMALL.drawVCentered(graphics, chatservice$message2.replyUser(), f18, f15, ColorUtility.scaleAlpha(-1711276033, value5));
                                float f19 = f18 + Fonts.SMALL.getWidth(chatservice$message2.replyUser()) + 3.0F;
                                String s1 = chatservice$message2.replyBody() == null ? "" : chatservice$message2.replyBody().replace('\n', ' ');
                                Fonts.SMALL.drawVCentered(graphics, Fonts.SMALL.ellipsize(s1, f3 + f4 - f19), f19, f15, ColorUtility.scaleAlpha(1728053247, value5));
                            } else {
                                Fonts.SMALL.drawVCentered(graphics, "Original message was deleted", f3, f15, ColorUtility.scaleAlpha(1509949439, value5));
                            }

                            f14 += 11.0F;
                        }

                        Render2D.fillSubpixelRoundedRect(graphics, f2, f14, 13.0F, 13.0F, 3.5F, ColorUtility.scaleAlpha(-2306070, value5));
                        Fonts.SMALL
                            .drawIconCentered(
                                graphics,
                                chatservice$message2.username().substring(0, 1).toUpperCase(Locale.ROOT),
                                f2 + 6.5F,
                                f14 + 6.5F,
                                1.0F,
                                ColorUtility.scaleAlpha(-15264737, value5)
                            );
                        Fonts.SMALL.draw(graphics, chatservice$message2.username(), f3, f14, ColorUtility.scaleAlpha(flag2 ? Theme.getAccentColor() : -1, value5));
                        float f34 = f3 + Fonts.SMALL.getWidth(chatservice$message2.username()) + 3.5F;
                        if (channel == ChatService.Channel.WEB && chatservice$message2.fromGame()) {
                            Fonts.ICONS
                                .drawIconCentered(
                                    graphics, "\uea28", f34 + 3.0F, f14 + 4.75F - 0.5F, 0.6F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value5 * 0.85F)
                                );
                            f34 += 8.5F;
                        }

                        Fonts.SMALL
                            .draw(
                                graphics,
                                Fonts.SMALL.ellipsize(formatChatTimestamp(chatservice$message2.createdAt()), f3 + f4 - f34),
                                f34,
                                f14,
                                ColorUtility.scaleAlpha(1308622847, value5)
                            );
                        f14 += 10.5F;
                    } else if (f13 > 0.01F) {
                        String s4 = formatChatTimestamp(chatservice$message2.createdAt()).replaceAll(" [AP]M$", "");
                        Fonts.SMALL
                            .drawScaled(
                                graphics, s4, f2 + 6.5F - Fonts.SMALL.getWidth(s4, 0.8F) / 2.0F, f14 + 1.0F, 0.8F, ColorUtility.scaleAlpha(1728053247, value5 * f13)
                            );
                    }

                    int i2 = ColorUtility.lerpColor(-939524097, -1, f13);

                    for (int l2 = 0; l2 < dashboardscreen$chatrow.lines().size(); l2++) {
                        String s6 = (String)dashboardscreen$chatrow.lines().get(l2);
                        if (chatservice$message2.edited() && l2 == dashboardscreen$chatrow.lines().size() - 1 && s6.endsWith("(edited)")) {
                            String s7 = s6.substring(0, s6.length() - "(edited)".length());
                            Fonts.SMALL.draw(graphics, s7, f3, f14, ColorUtility.scaleAlpha(i2, value5));
                            Fonts.SMALL.draw(graphics, "(edited)", f3 + Fonts.SMALL.getWidth(s7), f14, ColorUtility.scaleAlpha(1308622847, value5));
                        } else {
                            Fonts.SMALL.draw(graphics, s6, f3, f14, ColorUtility.scaleAlpha(i2, value5));
                        }

                        f14 += 9.5F;
                    }

                    if (flag3) {
                        dashboardscreen$chatrow1 = dashboardscreen$chatrow;
                        f11 = f12;
                    }
                }
            }

            graphics.disableScissor();
            if (dashboardscreen$chatrow1 != null) {
                ChatService.Message chatservice$message3 = dashboardscreen$chatrow1.m();
                boolean flag7 = chatservice$message3.userId() == k1;
                ArrayList arraylist1 = new ArrayList<>(List.of("reply"));
                if (flag7) {
                    arraylist1.add("edit");
                }

                arraylist1.add("copy");
                if (flag7 || chatService.isStaff()) {
                    arraylist1.add("delete");
                }

                float f27 = 11.0F;
                float f28 = 11.0F;
                float f29 = 2.0F;
                float f30 = arraylist1.size() * 11.0F + 4.0F;
                float f31 = 15.0F;
                float f32 = f + f1 - f30 - 3.0F;
                float f33 = Math.max(value3 - 2.0F, Math.min(value4 - 15.0F, f11 - 7.5F));
                this.messageToolbarBounds[0] = f32;
                this.messageToolbarBounds[1] = f33;
                this.messageToolbarBounds[2] = f30;
                this.messageToolbarBounds[3] = 15.0F;
                Glass.drawPanel(graphics, f32, f33, f30, 15.0F, 4.0F, value5, 0.2F, 0.5F);

                for (int j2 = 0; j2 < arraylist1.size(); j2++) {
                    String s5 = (String)arraylist1.get(j2);
                    float f35 = f32 + 2.0F + j2 * 11.0F;
                    boolean flag8 = Render2D.containsPoint(value6, value7, f35, f33 + 2.0F, 11.0F, 11.0F);
                    boolean flag9 = s5.equals("delete") && this.confirmDeleteMsgId == chatservice$message3.id() && l1 - this.confirmDeleteAt < 2500L;
                    boolean flag10 = s5.equals("delete") && (flag8 || flag9);
                    if (flag8 || flag9) {
                        Render2D.fillSubpixelRoundedRect(
                            graphics, f35, f33 + 2.0F, 11.0F, 11.0F, 3.0F, ColorUtility.scaleAlpha(flag10 ? 1090484354 : 352321535, value5)
                        );
                    }

                    boolean flag11 = s5.equals("copy") && this.copiedMessageId == chatservice$message3.id() && l1 - this.copiedMessageAt < 1300L;

                    String s8 = switch (s5) {
                        case "reply" -> "\ue15e";
                        case "edit" -> "\ue3c9";
                        case "copy" -> flag11 ? "\ue5ca" : "\ue14d";
                        default -> "\ue872";
                    };
                    int i3 = flag10 ? -19271 : (flag11 ? -6297408 : (flag8 ? -1 : -1493172225));
                    Fonts.ICONS.drawIconCentered(graphics, s8, f35 + 5.5F, f33 + 2.0F + 5.5F, 0.68F, ColorUtility.scaleAlpha(i3, value5));
                    this.hitRegions
                        .add(
                            new DashboardScreen.Hit(
                                f35, f33 + 2.0F, 11.0F, 11.0F, (button, mouseX, mouseY) -> this.handleChatMessageAction(s5, chatservice$message3), null
                            )
                        );
                }

                this.hitRegions.add(new DashboardScreen.Hit(f32, f33, f30, 15.0F, (button2, mouseX2, mouseY2) -> this.chatBroughtToFront = true, null));
                int k2 = arraylist1.size();
                ArrayList arraylist2 = new ArrayList<>(this.hitRegions.subList(this.hitRegions.size() - 1 - k2, this.hitRegions.size() - 1));
                this.hitRegions.subList(this.hitRegions.size() - 1 - k2, this.hitRegions.size() - 1).clear();
                this.hitRegions.addAll(arraylist2);
            } else {
                this.messageToolbarBounds[2] = 0.0F;
            }

            if (f23 > 0.0F) {
                float f25 = f21 - 8.0F;
                float f26 = Math.max(16.0F, f25 * f21 / (f5 + 4.0F));
                Render2D.fillRoundedRect(
                    graphics,
                    value + value2 - 3.5F,
                    value3 + 4.0F + (f25 - f26) * (1.0F - f24 / f23),
                    1.5F,
                    f26,
                    0.75F,
                    ColorUtility.scaleAlpha(Theme.getTrackColor(), value5)
                );
            }
        }
    }

    public boolean isChatInteractive() {
        return this.chatOpenSpring.get() > 0.9F;
    }

    public void handleChatMessageAction(String message, ChatService.Message message2) {
        this.chatBroughtToFront = true;
        ChatService chatservice = ChatService.getInstance();
        switch (message) {
            case "reply":
                this.editingMessage = null;
                this.replyingToMessage = message2;
                this.chatInputFocused = true;
                this.chatLastEditAt = System.currentTimeMillis();
                break;
            case "edit":
                this.beginEditMessage(message2);
                break;
            case "copy":
                this.minecraft.keyboardHandler.setClipboard(message2.body());
                this.copiedMessageId = message2.id();
                this.copiedMessageAt = System.currentTimeMillis();
                break;
            default:
                if (message2.userId() != chatservice.getSelfUserId()
                    && (this.confirmDeleteMsgId != message2.id() || System.currentTimeMillis() - this.confirmDeleteAt >= 2500L)) {
                    this.confirmDeleteMsgId = message2.id();
                    this.confirmDeleteAt = System.currentTimeMillis();
                    return;
                }

                this.confirmDeleteMsgId = -1L;
                if (this.replyingToMessage != null && this.replyingToMessage.id() == message2.id()) {
                    this.replyingToMessage = null;
                }

                if (this.editingMessage != null && this.editingMessage.id() == message2.id()) {
                    this.cancelEditMessage();
                }

                chatservice.deleteMessage(chatservice.getActiveChannel(), message2.id()).thenAccept(text -> this.minecraft.execute(() -> {
                    if (text != null) {
                        this.chatErrorBanner = text;
                        this.chatErrorAt = System.currentTimeMillis();
                    }
                }));
        }
    }

    public void beginEditMessage(ChatService.Message message) {
        this.replyingToMessage = null;
        this.editingMessage = message;
        this.truncateChatInput(0);
        this.appendChatInput(message.body().replace('\n', ' '));
        this.chatInputFocused = true;
        this.chatLastEditAt = System.currentTimeMillis();
    }

    public void cancelEditMessage() {
        this.editingMessage = null;
        this.truncateChatInput(0);
    }

    public void renderChatInputBox(
        GuiGraphicsExtractor graphics, ChatService chatService, ChatService.Channel channel, float value, float value2, float value3, float value4, int value5, int value6
    ) {
        float f = 18.0F;
        float f1 = value2 + 9.0F;
        float f2 = clamp01(this.getSpring("chatFocus").target(this.chatInputFocused ? 1.0F : 0.0F).update());
        float f3 = clamp01(this.getSpring("chatBoxHov").target(Render2D.containsPoint(value5, value6, value, value2, value3, 18.0F) ? 1.0F : 0.0F).update());
        Render2D.fillSubpixelRoundedRect(graphics, value, value2, value3, 18.0F, 7.0F, ColorUtility.scaleAlpha(ColorUtility.lerpColor(150994943, 318767103, f3), value4));
        if (f2 > 0.01F) {
            Render2D.fillSubpixelRoundedRect(graphics, value, value2, value3, 18.0F, 7.0F, ColorUtility.scaleAlpha(268435455, value4 * f2));
            Render2D.drawRoundedBorderTextured(graphics, value, value2, value3, 18.0F, 7.0F, 1.0F, ColorUtility.scaleAlpha(788529151, value4 * f2));
        }

        String s = this.chatInput.toString();
        float f4 = value + 7.0F;
        float f5 = value3 - 26.0F;
        long i = System.currentTimeMillis();
        boolean flag = false;
        float f6 = clamp01(this.getSpring("chatTyped").target(s.isEmpty() ? 0.0F : 1.0F).update());
        if (f6 < 0.99F) {
            String s1 = chatService.getErrorMessage() != null ? "Chat is unavailable" : (channel == ChatService.Channel.IRC ? "Message IRC" : "Message the website");
            int j = ColorUtility.lerpColor(1509949439, 1090519039, f2);
            int k = ColorUtility.scaleAlpha(j, value4 * (1.0F - f6));
            String s2 = Fonts.SMALL.ellipsize(s1, f5 - 4.0F);
            withPoseTransform(graphics, f2 * 4.0F, 0.0F, 0.0F, 0.0F, 1.0F, () -> Fonts.SMALL.drawVCentered(graphics, s2, f4, f1, k));
        }

        float f11 = Fonts.SMALL.getWidth(s);
        float f12 = this.chatInputScrollSpring.target(Math.max(0.0F, f11 - f5)).update();
        graphics.enableScissor((int)(f4 - 2.0F), (int)value2, (int)(f4 + f5 + 1.0F), (int)(value2 + 18.0F));

        while (this.chatCharTimes.size() > s.length()) {
            this.chatCharTimes.remove(this.chatCharTimes.size() - 1);
        }

        while (this.chatCharTimes.size() < s.length()) {
            this.chatCharTimes.add(0L);
        }

        float f13 = f4 - f12;
        int j1 = 0;

        while (j1 < s.length()) {
            int l = Character.charCount(s.codePointAt(j1));
            String s3 = s.substring(j1, j1 + l);
            float f7 = Fonts.SMALL.getWidth(s3);
            if (f13 + f7 >= f4 - 4.0F && f13 <= f4 + f5 + 4.0F) {
                float f8 = clamp01((float)(i - this.chatCharTimes.get(j1)) / 170.0F);
                float f9 = 1.0F - (1.0F - f8) * (1.0F - f8) * (1.0F - f8);
                int i1 = ColorUtility.scaleAlpha(-1, value4 * f9);
                float f10 = f13;
                withPoseTransform(
                    graphics, 0.0F, (1.0F - f9) * 4.0F, f10 + f7 / 2.0F, f1, 0.85F + 0.15F * f9, () -> Fonts.SMALL.drawVCentered(graphics, s3, f10, f1, i1)
                );
            }

            f13 += f7;
            j1 += l;
        }

        this.chatDroppedChars.removeIf(dropChar -> i - dropChar.at() > 150L);

        for (DashboardScreen.DropChar dashboardscreen$dropchar : this.chatDroppedChars) {
            float f16 = clamp01((float)(i - dashboardscreen$dropchar.at()) / 150.0F);
            float f18 = f4 - f12 + dashboardscreen$dropchar.x();
            int l1 = ColorUtility.scaleAlpha(-1, value4 * (1.0F - f16));
            withPoseTransform(
                graphics,
                0.0F,
                f16 * 3.0F,
                f18 + Fonts.SMALL.getWidth(dashboardscreen$dropchar.ch()) / 2.0F,
                f1,
                1.0F - 0.15F * f16,
                () -> Fonts.SMALL.drawVCentered(graphics, dashboardscreen$dropchar.ch(), f18, f1, l1)
            );
        }

        graphics.disableScissor();
        float f14 = s.isEmpty() ? f4 - 1.0F : f4 - f12 + f11 + 1.0F;
        if (!this.chatCaretInit) {
            this.chatCaretSpring.snap(f14);
            this.chatCaretInit = true;
        }

        float f15 = this.chatCaretSpring.target(f14).update();
        if (this.chatInputFocused) {
            long k1 = i - this.chatLastEditAt;
            float f19 = k1 < 500L ? 1.0F : clamp01(0.5F + 1.4F * (float)Math.cos((k1 - 500L) / 1060.0 * Math.PI * 2.0));
            Render2D.fillRect(graphics, f15, f1 - 4.5F, 0.75F, 9.0F, ColorUtility.scaleAlpha(Theme.getAccentColor(), value4 * f2 * f19));
        }

        float f17 = value + value3 - 10.0F;
        boolean flag1 = !s.isBlank() && !this.chatSubmitting;
        boolean flag2 = Render2D.containsPoint(value5, value6, f17 - 8.0F, value2, 16.0F, 18.0F);
        int i2 = flag1 ? (flag2 ? -1 : Theme.getAccentColor()) : 1090519039;
        Fonts.ICONS.drawIconCentered(graphics, this.editingMessage != null ? "\ue5ca" : "\ue163", f17, f1, 0.85F, ColorUtility.scaleAlpha(i2, value4));
        this.hitRegions.add(new DashboardScreen.Hit(value, value2, value3 - 18.0F, 18.0F, (button, mouseX, mouseY) -> {
            this.chatBroughtToFront = true;
            this.chatInputFocused = true;
            this.chatLastEditAt = System.currentTimeMillis();
        }, null));
        this.hitRegions.add(new DashboardScreen.Hit(f17 - 8.0F, value2, 16.0F, 18.0F, (button2, mouseX2, mouseY2) -> {
            this.chatBroughtToFront = true;
            this.submitChatMessage();
        }, null));
    }
    */

    public void appendChatInput(String message) {
        long i = System.currentTimeMillis();
        this.chatInput.append(message);

        for (int j = 0; j < message.length(); j++) {
            this.chatCharTimes.add(i);
        }
    }

    public void truncateChatInput(int value) {
        long i = System.currentTimeMillis();
        String s = this.chatInput.toString();
        int j = value;

        while (j < s.length()) {
            int k = Character.charCount(s.codePointAt(j));
            this.chatDroppedChars.add(new DashboardScreen.DropChar(s.substring(j, j + k), Fonts.SMALL.getWidth(s.substring(0, j)), i));
            j += k;
        }

        this.chatInput.setLength(value);

        while (this.chatCharTimes.size() > value) {
            this.chatCharTimes.remove(this.chatCharTimes.size() - 1);
        }
    }

    // VENDOR-DISABLED: Online chat submission and key handler (submitChatMessage..handleChatKeyPressed) referencing ChatService
    /*
    public void submitChatMessage() {
        String s = this.chatInput.toString().strip();
        if (!s.isEmpty() && !this.chatSubmitting) {
            ChatService chatservice = ChatService.getInstance();
            if (this.editingMessage != null) {
                ChatService.Message chatservice$message = this.editingMessage;
                if (chatservice$message.body().replace('\n', ' ').strip().equals(s)) {
                    this.cancelEditMessage();
                } else {
                    this.chatSubmitting = true;
                    chatservice.editMessage(chatservice.getActiveChannel(), chatservice$message.id(), s).thenAccept(text -> this.minecraft.execute(() -> {
                        this.chatSubmitting = false;
                        if (text == null) {
                            if (this.editingMessage != null && this.editingMessage.id() == chatservice$message.id()) {
                                this.cancelEditMessage();
                            }

                            this.chatErrorBanner = null;
                        } else {
                            this.chatErrorBanner = text;
                            this.chatErrorAt = System.currentTimeMillis();
                        }
                    }));
                }
            } else {
                Long olong = this.replyingToMessage != null ? this.replyingToMessage.id() : null;
                this.chatSubmitting = true;
                chatservice.sendMessage(chatservice.getActiveChannel(), s, olong).thenAccept(text2 -> this.minecraft.execute(() -> {
                    this.chatSubmitting = false;
                    if (text2 == null) {
                        this.truncateChatInput(0);
                        this.replyingToMessage = null;
                        this.chatScrollTarget = 0.0F;
                        this.chatErrorBanner = null;
                    } else {
                        this.chatErrorBanner = text2;
                        this.chatErrorAt = System.currentTimeMillis();
                    }
                }));
            }
        }
    }

    public boolean handleChatKeyPressed(KeyEvent event) {
        int i = event.key();
        if (i == 40 || i == 88) {
            this.submitChatMessage();
        } else if (i == 42) {
            if (event.hasControlDown()) {
                int j = this.chatInput.length();

                while (j > 0 && this.chatInput.charAt(j - 1) == ' ') {
                    j--;
                }

                while (j > 0 && this.chatInput.charAt(j - 1) != ' ') {
                    j--;
                }

                this.truncateChatInput(j);
            } else if (!this.chatInput.isEmpty()) {
                int l = this.chatInput.length() - 1;
                this.truncateChatInput(Character.isLowSurrogate(this.chatInput.charAt(l)) && l > 0 ? l - 1 : l);
            }
        } else if (event.isEscape()) {
            if (this.editingMessage != null) {
                this.cancelEditMessage();
            } else if (this.replyingToMessage != null) {
                this.replyingToMessage = null;
            } else {
                this.chatInputFocused = false;
            }
        } else if (i == 82 && this.chatInput.isEmpty() && this.editingMessage == null) {
            ChatService chatservice = ChatService.getInstance();
            List list = chatservice.getMessages(chatservice.getActiveChannel());

            for (int k = list.size() - 1; k >= 0; k--) {
                if (((ChatService.Message)list.get(k)).userId() == chatservice.getSelfUserId()) {
                    this.beginEditMessage((ChatService.Message)list.get(k));
                    break;
                }
            }
        } else {
            if (!event.isPaste()) {
                return true;
            }

            String s = this.minecraft.keyboardHandler.getClipboard().replaceAll("[\\r\\n\\t]+", " ");
            this.appendChatInput(s.substring(0, Math.min(s.length(), 500 - this.chatInput.length())));
        }

        this.chatLastEditAt = System.currentTimeMillis();
        return true;
    }
    */

    public DashboardScreen.Hit findHitRegion(double first, double second) {
        for (int i = this.hitRegions.size() - 1; i >= 0; i--) {
            DashboardScreen.Hit dashboardscreen$hit = this.hitRegions.get(i);
            if (Render2D.containsPoint(first, second, dashboardscreen$hit.x(), dashboardscreen$hit.y(), dashboardscreen$hit.w(), dashboardscreen$hit.h())) {
                return dashboardscreen$hit;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double d0 = event.x();
        double d1 = event.y();
        if (this.bindingModule != null && event.button() > 3) {
            this.applyModuleBind(this.bindingModule, event.button(), InputType.MOUSE);
            return true;
        }

        if (this.openEnumProp != null
            && this.enumDropdownOpen
            && !Render2D.containsPoint(d0, d1, this.enumPopupBounds[0], this.enumPopupBounds[1], this.enumPopupBounds[2], this.enumPopupBounds[3])
            && !Render2D.containsPoint(d0, d1, this.enumTriggerBounds[0], this.enumTriggerBounds[1], this.enumTriggerBounds[2], this.enumTriggerBounds[3])) {
            this.closeEnumDropdown();
            return true;
        }

        if (this.openColorProp != null
            && this.colorPickerOpen
            && !Render2D.containsPoint(d0, d1, this.colorPopupBounds[0], this.colorPopupBounds[1], this.colorPopupBounds[2], this.colorPopupBounds[3])
            && !Render2D.containsPoint(d0, d1, this.colorTriggerBounds[0], this.colorTriggerBounds[1], this.colorTriggerBounds[2], this.colorTriggerBounds[3])) {
            this.closeColorPicker();
            return true;
        }

        DashboardScreen.Hit dashboardscreen$hit = this.findHitRegion(d0, d1);
        this.setSearchFocused(false);
        this.chatInputFocused = false;
        if (dashboardscreen$hit != null) {
            dashboardscreen$hit.click().onClick(event.button(), d0, d1);
            if (dashboardscreen$hit.drag() != null) {
                this.activeDragHandler = dashboardscreen$hit.drag();
            }

            return true;
        } else {
            this.bindingModule = null;
            return true;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingChat) {
            chatCustomPos[0] = (float)Math.max(0.0, Math.min(this.width - 140.0F, event.x() - this.chatDragOffX));
            chatCustomPos[1] = (float)Math.max(0.0, Math.min(this.height - 28.0F, event.y() - this.chatDragOffY));
            return true;
        } else if (this.draggingCategory != null) {
            float[] afloat = categoryPositions.get(this.draggingCategory);
            afloat[2] = 0.0F;
            afloat[0] = (float)Math.max(0.0, Math.min(this.width - 132.0F, event.x() - this.categoryDragOffX));
            afloat[1] = (float)Math.max(0.0, Math.min(this.height - 28.0F, event.y() - this.categoryDragOffY));
            return true;
        } else if (this.activeDragHandler != null) {
            this.activeDragHandler.onDrag(event.x(), event.y());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.activeDragHandler != null) {
            this.onUiFeedback(1.5F);
        }

        this.draggingCategory = null;
        this.draggingChat = false;
        this.activeDragHandler = null;
        this.activeDragOwner = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.openEnumProp != null
            && this.enumDropdownOpen
            && Render2D.containsPoint(mouseX, mouseY, this.enumPopupBounds[0], this.enumPopupBounds[1], this.enumPopupBounds[2], this.enumPopupBounds[3])) {
            this.enumScrollTarget -= (float)scrollY * 16.0F;
            return true;
        }

        boolean flag = Render2D.containsPoint(mouseX, mouseY, this.chatPanelBounds[0], this.chatPanelBounds[1], this.chatPanelBounds[2], this.chatPanelBounds[3]);
        if (!flag
            || !this.chatBroughtToFront
                && !this.categoryBounds.values().stream().noneMatch(floats -> Render2D.containsPoint(mouseX, mouseY, floats[0], floats[1], floats[2], floats[3]))) {
            for (Entry entry : this.categoryBounds.entrySet()) {
                float[] afloat = (float[])entry.getValue();
                if (Render2D.containsPoint(mouseX, mouseY, afloat[0], afloat[1], afloat[2], afloat[3])) {
                    this.categoryScrollTargets
                        .put((ModuleCategory)entry.getKey(), this.categoryScrollTargets.getOrDefault(entry.getKey(), 0.0F) - (float)scrollY * 22.0F);
                    return true;
                }
            }

            return true;
        } else {
            this.chatScrollTarget += (float)scrollY * 20.0F;
            return true;
        }
    }

    public void applyModuleBind(Module module, int value, InputType inputType) {
        BindRegistry bindregistry = Adapt.getInstance().getBindingManager().getRegistry();
        bindregistry.unbind(module);
        if (value >= 0) {
            bindregistry.bind(value, module, inputType);
        }

        this.bindingModule = null;
        this.flashModuleKeybind(module);
        this.onUiFeedback(1.5F);
        Adapt.getInstance()
            .getNotificationManager()
            .builder(NotificationType.INFO)
            .title(module.getName())
            .message(value < 0 ? "Keybind cleared" : "Bound to " + BindingManager.formatBind(new Bind(value, inputType)))
            .duration(1600)
            .post();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.bindingModule != null) {
            if (!event.isEscape() && event.key() != 42) {
                this.applyModuleBind(this.bindingModule, event.key(), InputType.KEYBOARD);
            } else {
                this.applyModuleBind(this.bindingModule, -1, InputType.KEYBOARD);
            }

            return true;
        } else if (this.hexInputFocused) {
            if (event.isEscape()) {
                this.hexInputFocused = false;
            } else if (event.key() == 40 || event.key() == 88) {
                this.commitHexColor();
            } else if (event.key() == 42 && this.hexInput.length() > 0) {
                this.hexInput.setLength(this.hexInput.length() - 1);
            } else if (event.isPaste()) {
                this.hexInput.setLength(0);

                for (char c0 : this.minecraft.keyboardHandler.getClipboard().toCharArray()) {
                    if (Character.digit(c0, 16) >= 0 && this.hexInput.length() < 6) {
                        this.hexInput.append(Character.toUpperCase(c0));
                    }
                }
            }

            this.hexLastEditAt = System.currentTimeMillis();
            return true;
        } else {
            if (this.chatInputFocused) {
                return false; // VENDOR-DISABLED: return this.handleChatKeyPressed(event);
            }

            if (this.openColorProp != null && this.colorPickerOpen && event.isEscape()) {
                this.closeColorPicker();
                return true;
            }

            if (this.openEnumProp != null && this.enumDropdownOpen && event.isEscape()) {
                this.closeEnumDropdown();
                return true;
            }

            if (event.key() == 9 && event.hasControlDown()) {
                this.setSearchFocused(true);
                return true;
            }

            if (this.searchFocused) {
                if (event.key() == 42) {
                    if (event.hasControlDown()) {
                        this.clearSearch();
                    } else {
                        this.backspaceSearch();
                    }

                    this.searchLastEditAt = System.currentTimeMillis();
                    return true;
                }

                if (event.isEscape()) {
                    if (this.searchQuery.length() > 0) {
                        this.clearSearch();
                    } else {
                        this.setSearchFocused(false);
                    }

                    return true;
                }

                if (event.key() == 40 || event.key() == 88) {
                    this.setSearchFocused(false);
                    return true;
                }
            } else if (event.isEscape() && this.searchQuery.length() > 0) {
                this.clearSearch();
                return true;
            }

            if (event.key() == 229 && this.searchQuery.length() == 0 && !this.searchFocused) {
                this.onClose();
                return true;
            } else {
                return super.keyPressed(event);
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.bindingModule != null) {
            return true;
        }

        if (this.hexInputFocused) {
            char c0 = (char)event.codepoint();
            if (Character.digit(c0, 16) >= 0 && this.hexInput.length() < 6) {
                this.hexInput.append(Character.toUpperCase(c0));
            }

            this.hexLastEditAt = System.currentTimeMillis();
            return true;
        } else if (this.chatInputFocused) {
            if (event.isAllowedChatCharacter() && this.chatInput.length() < 500) {
                this.appendChatInput(event.codepointAsString());
                this.chatLastEditAt = System.currentTimeMillis();
            }

            return true;
        } else {
            if (!this.searchFocused) {
                return false;
            }

            if (event.isAllowedChatCharacter() && this.searchQuery.length() < 24) {
                this.appendSearchChar(event.codepointAsString());
            }

            return true;
        }
    }

    private record ChatRow(Object m, boolean head, List<String> lines, float h, float gapAbove) { // VENDOR-DISABLED: ChatService.Message m
    }

    @FunctionalInterface
    private interface Click {
        void onClick(int value, double value2, double value3);
    }

    @FunctionalInterface
    private interface Drag {
        void onDrag(double first, double second);
    }

    private record DropChar(String ch, float x, long at) {
    }

    private record Hit(float x, float y, float w, float h, DashboardScreen.Click click, DashboardScreen.Drag drag) {
    }
}
