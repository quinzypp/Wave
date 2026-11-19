package dev.adapt.v3.util.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class ProjectionUtility {
    public static final Matrix4f PROJECTION_MAT = new Matrix4f();
    public static final Matrix4f VIEW_ROT_MAT = new Matrix4f();
    public static final Matrix4f COMBINED_MAT = new Matrix4f();
    public static double camX;
    public static double camY;
    public static double camZ;
    public static float partialTicks = 1.0F;
    public static boolean valid;

    public static void capture(Matrix4f matrix4f, Matrix4f matrix4f2, Vec3 vec, float value) {
        PROJECTION_MAT.set(matrix4f);
        VIEW_ROT_MAT.set(matrix4f2);
        PROJECTION_MAT.mul(VIEW_ROT_MAT, COMBINED_MAT);
        camX = vec.x;
        camY = vec.y;
        camZ = vec.z;
        partialTicks = value;
        valid = true;
    }

    public static void invalidate() {
        valid = false;
    }

    public static float getPartialTicks() {
        return partialTicks;
    }

    public static float[] projectToScreen(double value, double value2, double value3, float value4, float value5) {
        if (!valid) {
            return null;
        }

        Vector4f vector4f = new Vector4f((float)(value - camX), (float)(value2 - camY), (float)(value3 - camZ), 1.0F).mul(COMBINED_MAT);
        if (vector4f.w <= 1.0E-4F) {
            return null;
        }

        float f = vector4f.x / vector4f.w;
        float f1 = vector4f.y / vector4f.w;
        float f2 = vector4f.z / vector4f.w;
        return !(f2 < -1.0F) && !(f2 >= 1.0F) ? new float[]{(f + 1.0F) / 2.0F * value4, (1.0F - f1) / 2.0F * value5} : null;
    }

    public static float[] projectEntityBounds(Entity entity, float value, float value2, float value3) {
        Vec3 vec3 = entity.getPosition(value);
        double d0 = entity.getBbWidth() / 1.5;
        double d1 = entity.getBbHeight() + 0.15;
        float f = Float.MAX_VALUE;
        float f1 = Float.MAX_VALUE;
        float f2 = -1.0F;
        float f3 = -1.0F;
        boolean flag = false;

        for (int i = 0; i < 8; i++) {
            double d2 = vec3.x + ((i & 1) == 0 ? -d0 : d0);
            double d3 = vec3.y + ((i & 2) == 0 ? 0.0 : d1);
            double d4 = vec3.z + ((i & 4) == 0 ? -d0 : d0);
            float[] afloat = projectToScreen(d2, d3, d4, value2, value3);
            if (afloat != null) {
                flag = true;
                f = Math.min(f, afloat[0]);
                f1 = Math.min(f1, afloat[1]);
                f2 = Math.max(f2, afloat[0]);
                f3 = Math.max(f3, afloat[1]);
            }
        }

        return flag ? new float[]{f, f1, f2, f3} : null;
    }
}
