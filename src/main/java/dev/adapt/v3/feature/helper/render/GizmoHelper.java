package dev.adapt.v3.feature.helper.render;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

public final class GizmoHelper {
    public static final GizmoHelper INSTANCE = new GizmoHelper();

    public static GizmoHelper getInstance() {
        return INSTANCE;
    }

    public void drawCuboid(GizmoHelper.Cuboid cuboid) {
        try {
            Gizmos.cuboid(cuboid.pos, createStyle(cuboid)).persistForMillis((int)cuboid.durationMs).fadeOut();
        } catch (IllegalStateException illegalstateexception) {
        }
    }

    public static void drawFilledBlock(BlockPos pos, int value) {
        try {
            Gizmos.cuboid(pos, GizmoStyle.fill(value));
        } catch (IllegalStateException illegalstateexception) {
        }
    }

    public static GizmoStyle createStyle(GizmoHelper.Cuboid cuboid) {
        return cuboid.outlineColor != 0 && cuboid.outlineColor >>> 24 != 0
            ? GizmoStyle.strokeAndFill(cuboid.outlineColor, 1.0F, cuboid.fillColor)
            : GizmoStyle.fill(cuboid.fillColor);
    }

    public static class Cuboid {
        public final BlockPos pos;
        public final int outlineColor;
        public final int fillColor;
        public final long durationMs;

        public Cuboid(BlockPos pos, int first, int second, long time) {
            this.pos = pos.immutable();
            this.outlineColor = first;
            this.fillColor = second;
            this.durationMs = time;
        }
    }
}
