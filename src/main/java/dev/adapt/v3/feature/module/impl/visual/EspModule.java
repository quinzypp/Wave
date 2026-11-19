package dev.adapt.v3.feature.module.impl.visual;

import java.util.List;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.ui.render.MaskOutline;
import dev.adapt.v3.util.render.ProjectionUtility;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EspModule extends Module {
    public static final int OUTLINE_COLOR = 1728053247;
    public static final int OUTLINE_WIDTH = 1;
    public static final float GLOW_RADIUS = 15.0F;
    public static final float GLOW_ALPHA = 0.6F;
    public static final double INFLATE_XZ = 0.15;
    public static final double INFLATE_Y = 0.1;

    public EspModule() {
        super("ESP", "Outlines players through walls with a soft glow.", ModuleCategory.VISUAL);
    }

    public static EspModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(EspModule.class);
    }

    public static boolean isValidTarget(Entity entity) {
        return entity instanceof Player player && player != mc.player && player.isAlive() && !"BOT".equals(player.getName().getString());
    }

    public void render(GuiGraphicsExtractor graphics, float partialTick) {
        if (this.isEnabled() && mc.level != null && mc.player != null) {
            float f = graphics.guiWidth();
            float f1 = graphics.guiHeight();
            List<float[]> arraylist = new ArrayList<>();

            for (Player player : mc.level.players()) {
                if (isValidTarget(player)) {
                    float[] afloat = projectEntityHull(player, partialTick, f, f1);
                    if (afloat != null) {
                        arraylist.add(afloat);
                    }
                }
            }

            MaskOutline.submit(arraylist, 1728053247, 1.0F, 15.0F, 0.6F);
        }
    }

    public static float[] projectEntityHull(Entity entity, float value, float value2, float value3) {
        Vec3 vec3 = entity.getPosition(value);
        AABB aabb = entity.getBoundingBox().inflate(0.15, 0.1, 0.15).move(vec3.x - entity.getX(), vec3.y - entity.getY(), vec3.z - entity.getZ());
        float[][] afloat = new float[8][];

        for (int i = 0; i < 8; i++) {
            double d0 = (i & 1) == 0 ? aabb.minX : aabb.maxX;
            double d1 = (i & 2) == 0 ? aabb.minY : aabb.maxY;
            double d2 = (i & 4) == 0 ? aabb.minZ : aabb.maxZ;
            float[] afloat1 = ProjectionUtility.projectToScreen(d0, d1, d2, value2, value3);
            if (afloat1 == null) {
                return null;
            }

            afloat[i] = afloat1;
        }

        return convexHull(afloat);
    }

    public static float[] convexHull(float[][] floatArrays) {
        float[][] afloat = (float[][])floatArrays.clone();
        Arrays.sort(afloat, (a, b) -> a[0] != b[0] ? Float.compare(a[0], b[0]) : Float.compare(a[1], b[1]));
        float[][] afloat1 = new float[afloat.length * 2][];
        int i = 0;

        for (float[] afloat2 : afloat) {
            while (i >= 2 && cross(afloat1[i - 2], afloat1[i - 1], afloat2) <= 0.0F) {
                i--;
            }

            afloat1[i++] = afloat2;
        }

        int j = afloat.length - 2;
        int k = i + 1;

        while (j >= 0) {
            float[] afloat4 = afloat[j];

            while (i >= k && cross(afloat1[i - 2], afloat1[i - 1], afloat4) <= 0.0F) {
                i--;
            }

            afloat1[i++] = afloat4;
            j--;
        }

        if (i < 4) {
            return null;
        }

        float[] afloat3 = new float[(i - 1) * 2];

        for (int l = 0; l < i - 1; l++) {
            afloat3[l * 2] = afloat1[l][0];
            afloat3[l * 2 + 1] = afloat1[l][1];
        }

        return afloat3;
    }

    public static float cross(float[] floats, float[] floats2, float[] floats3) {
        return (floats2[0] - floats[0]) * (floats3[1] - floats[1]) - (floats2[1] - floats[1]) * (floats3[0] - floats[0]);
    }
}
