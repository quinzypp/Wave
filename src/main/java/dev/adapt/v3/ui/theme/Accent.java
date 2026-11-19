package dev.adapt.v3.ui.theme;

import dev.adapt.v3.util.Displayable;

public enum Accent implements Displayable {
    AZURE("Azure", -11760129),
    VIOLET("Violet", -7639809),
    MINT("Mint", -15154532),
    ROSE("Rose", -41073),
    AMBER("Amber", -24006),
    GRAPHITE("Graphite", -9735552),
    SPECTRUM("Spectrum", -11760129),
    PEARL("Pearl", -990977, -5134915),
    ADAPT("Adapt", -9107375, -5626512),
    FRESH("Fresh", -10107963, -921103),
    VOID("Void", -13948068, -10197816),
    LAVENDER("Lavender", -6786131, -2313741),
    SPEARMINT("Spearmint", -15305394, -13719416),
    GLACIER("Glacier", -6259983, -12103503),
    WATERMELON("Watermelon", -2736835, -14241139),
    LEMON_SODA("Lemon Soda", -12724609, -4808634),
    ORANGE_JUICE("Orange Juice", -5736687, -2772433),
    COCONUT_BERRY("Coconut Berry", -328966, -10446890),
    WHITE_PEACH("White Peach", -1815901, -470064),
    OCEAN_BREEZE("Ocean Breeze", -16745561, -16730920),
    ROYAL_MIDNIGHT("Royal Midnight", -7722014, -15132304),
    NEON("Neon", -12976364, -65281),
    ARCTIC_ICE("Arctic Ice", -9848367, -2031617),
    CRIMSON_ASH("Crimson Ash", -2354116, -13949402),
    CITRUS("Citrus", -4720512, -13251193),
    CUSTOM("Custom", -41073);

    public final String displayName;
    public final int primaryColor;
    public final int secondaryColor;

    Accent(String text, int value) {
        this(text, value, 0);
    }

    Accent(String text, int first, int second) {
        this.displayName = text;
        this.primaryColor = first;
        this.secondaryColor = second;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }
}
