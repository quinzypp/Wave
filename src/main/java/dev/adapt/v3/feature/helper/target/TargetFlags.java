package dev.adapt.v3.feature.helper.target;

public final class TargetFlags {
    public static final int PLAYERS = 1;
    public static final int HOSTILE = 2;
    public static final int PASSIVE = 4;
    public static final int TEAMMATES = 8;
    public static final int LOCAL_PLAYER = 16;

    public static int pack(boolean flag, boolean flag2, boolean flag3, boolean flag4) {
        int flags = 0;
        if (flag) {
            flags |= 1;
        }

        if (flag2) {
            flags |= 2;
        }

        if (flag3) {
            flags |= 4;
        }

        if (flag4) {
            flags |= 8;
        }

        return flags;
    }
}
