package dev.adapt.v3.feature.module.impl.visual.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Animations17 {
    public static void applyFirstPersonTransform(
        PoseStack poseStack, float value, float value2, float value3, int value4, int value5, Animations17.UseAction useAction, ItemStack stack
    ) {
        applyFirstPersonTransform(poseStack, value, value2, value3, value4, value5, useAction, stack, AnimationsModule.BlockStyle.V1_7);
    }

    public static void applyFirstPersonTransform(
        PoseStack poseStack, float value, float value2, float value3, int value4, int value5, Animations17.UseAction useAction, ItemStack stack, AnimationsModule.BlockStyle blockStyle
    ) {
        if (value4 > 0 && useAction == Animations17.UseAction.BLOCK && blockStyle != AnimationsModule.BlockStyle.V1_7) {
            applyStyledBlockTransform(poseStack, value2, value3, blockStyle);
        } else {
            float f = value3;
            float f1 = 0.8F;
            if (value4 > 0) {
                if (useAction == Animations17.UseAction.EAT || useAction == Animations17.UseAction.DRINK) {
                    float f2 = value4 - value + 1.0F;
                    float f3 = 1.0F - f2 / value5;
                    float f4 = 1.0F - f3;
                    f4 = f4 * f4 * f4;
                    f4 = f4 * f4 * f4;
                    f4 = f4 * f4 * f4;
                    float f5 = 1.0F - f4;
                    poseStack.translate(0.0F, Mth.abs(Mth.cos(f2 / 4.0F * 3.141593F) * 0.1F) * (f3 > 0.2 ? 1 : 0), 0.0F);
                    poseStack.translate(f5 * 0.6F, -f5 * 0.5F, 0.0F);
                    poseStack.rotateDegrees(Axis.YP, f5 * 90.0F);
                    poseStack.rotateDegrees(Axis.XP, f5 * 10.0F);
                    poseStack.rotateDegrees(Axis.ZP, f5 * 30.0F);
                }
            } else {
                float f9 = value2;
                float f11 = Mth.sin(f9 * 3.141593F);
                float f13 = Mth.sin(sqrt(f9) * 3.141593F);
                poseStack.translate(-f13 * 0.4F, Mth.sin(sqrt(f9) * 3.141593F * 2.0F) * 0.2F, -f11 * 0.2F);
            }

            poseStack.translate(0.56F, -0.52F - (1.0F - f) * 0.6F, -0.71999997F);
            poseStack.rotateDegrees(Axis.YP, 45.0F);
            float f10 = value2;
            float f12 = Mth.sin(f10 * f10 * 3.141593F);
            float f14 = Mth.sin(sqrt(f10) * 3.141593F);
            poseStack.rotateDegrees(Axis.YP, -f12 * 20.0F);
            poseStack.rotateDegrees(Axis.ZP, -f14 * 20.0F);
            poseStack.rotateDegrees(Axis.XP, -f14 * 80.0F);
            float f15 = 0.4F;
            poseStack.scale(0.4F, 0.4F, 0.4F);
            if (value4 > 0) {
                if (useAction == Animations17.UseAction.BLOCK) {
                    poseStack.translate(-0.5F, 0.2F, 0.0F);
                    poseStack.rotateDegrees(Axis.YP, 30.0F);
                    poseStack.rotateDegrees(Axis.XP, -80.0F);
                    poseStack.rotateDegrees(Axis.YP, 60.0F);
                } else if (useAction == Animations17.UseAction.BOW) {
                    poseStack.rotateDegrees(Axis.ZP, -18.0F);
                    poseStack.rotateDegrees(Axis.YP, -12.0F);
                    poseStack.rotateDegrees(Axis.XP, -8.0F);
                    poseStack.translate(-0.9F, 0.2F, 0.0F);
                    float f6 = value5 - (value4 - value + 1.0F);
                    float f7 = f6 / 20.0F;
                    f7 = (f7 * f7 + f7 * 2.0F) / 3.0F;
                    if (f7 > 1.0F) {
                        f7 = 1.0F;
                    }

                    if (f7 > 0.1F) {
                        poseStack.translate(0.0F, Mth.sin((f6 - 0.1F) * 1.3F) * 0.01F * (f7 - 0.1F), 0.0F);
                    }

                    poseStack.translate(0.0F, 0.0F, f7 * 0.1F);
                    poseStack.rotateDegrees(Axis.ZP, -335.0F);
                    poseStack.rotateDegrees(Axis.YP, -50.0F);
                    poseStack.translate(0.0F, 0.5F, 0.0F);
                    float f8 = 1.0F + f7 * 0.2F;
                    poseStack.scale(1.0F, 1.0F, f8);
                    poseStack.translate(0.0F, -0.5F, 0.0F);
                    poseStack.rotateDegrees(Axis.YP, 50.0F);
                    poseStack.rotateDegrees(Axis.ZP, 335.0F);
                }
            }

            if (stack.is(Items.FISHING_ROD) || stack.is(Items.CARROT_ON_A_STICK) || stack.is(Items.WARPED_FUNGUS_ON_A_STICK)) {
                poseStack.rotateDegrees(Axis.YP, 180.0F);
            }
        }
    }

    public static void applyStyledBlockTransform(PoseStack poseStack, float first, float second, AnimationsModule.BlockStyle blockStyle) {
        float f = Mth.sin(sqrt(first) * 3.141593F);
        float f1 = Mth.sin(first * first * 3.141593F);
        float f2 = Mth.sin(first * 3.141593F);
        float f3 = 0.8F;
        poseStack.translate(0.56F, -0.52F - (1.0F - second) * 0.6F, -0.71999997F);
        switch (blockStyle) {
            case SIGMA_SLIDE:
                applySlideBlock(poseStack, f, 0.0F, 0.0F, 45.0F, 0.0F, 20.0F);
                return;
            case SIGMA_SLIDE_SOFT:
                applySlideBlock(poseStack, f2 * f2, 0.0F, 0.0F, 45.0F, 0.0F, 20.0F);
                return;
            case SIGMA_SLIDE_SMOOTH:
                applySlideBlock(poseStack, f2 * f2, 0.065F, -0.06F, 48.0F, 6.0F, 20.0F);
                return;
            case SIGMA_SLIDE_LONG:
                float f6 = Mth.sin(Animations17.SmoothSwing.advance(first) * 3.141593F);
                applySlideBlock(poseStack, f6 * f6, 0.0F, 0.0F, 45.0F, 0.0F, 35.0F);
                return;
            default:
                switch (blockStyle) {
                    case SOFT:
                        applyAvatarAngles(poseStack, f1 * 10.0F, f * 10.0F, f * 40.0F);
                        break;
                    case PUSH:
                        poseStack.translate(-f2 * 0.08F, f2 * 0.02F, -f2 * 0.22F);
                        poseStack.rotateDegrees(Axis.YP, 45.0F);
                        poseStack.rotateDegrees(Axis.XP, -f2 * 8.0F);
                        break;
                    case PULSE:
                        poseStack.translate(f2 * 0.06F, f2 * 0.02F, -f2 * 0.2F);
                        poseStack.rotateDegrees(Axis.YP, 45.0F);
                        float f4 = 1.0F + f2 * 0.1F;
                        poseStack.scale(f4, f4, f4);
                        break;
                    default:
                        poseStack.rotateDegrees(Axis.YP, 45.0F);
                }

                float f5 = 0.4F;
                poseStack.scale(0.4F, 0.4F, 0.4F);
                poseStack.translate(-0.5F, 0.2F, 0.0F);
                poseStack.rotateDegrees(Axis.YP, 30.0F);
                poseStack.rotateDegrees(Axis.XP, -80.0F);
                poseStack.rotateDegrees(Axis.YP, 60.0F);
        }
    }

    public static void applySlideBlock(PoseStack poseStack, float value, float value2, float value3, float value4, float value5, float value6) {
        poseStack.translate(value3, value2, 0.0F);
        poseStack.rotateDegrees(Axis.YP, value4);
        poseStack.rotateDegrees(Axis.XP, value5);
        poseStack.scale(0.4F, 0.4F, 0.4F);
        poseStack.translate(-0.5F, 0.2F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 30.0F);
        poseStack.rotateDegrees(Axis.XP, -80.0F - value * value6);
        poseStack.rotateDegrees(Axis.YP, 60.0F);
    }

    public static void applyAvatarAngles(PoseStack poseStack, float value, float value2, float value3) {
        poseStack.rotateDegrees(Axis.YP, 45.0F);
        poseStack.rotateDegrees(Axis.YP, -value);
        poseStack.rotateDegrees(Axis.ZP, -value2);
        poseStack.rotateDegrees(Axis.XP, -value3);
    }

    public static void applyFlatItemTransform(PoseStack poseStack, boolean flag) {
        if (flag) {
            float f = 0.0F;
            float f1 = 0.3F;
            poseStack.translate(-0.0F, -0.3F, 0.0F);
            float f2 = 1.5F;
            poseStack.scale(1.5F, 1.5F, 1.5F);
            poseStack.rotateDegrees(Axis.YP, 50.0F);
            poseStack.rotateDegrees(Axis.ZP, 335.0F);
            poseStack.translate(-0.9375F, -0.0625F, 0.0F);
            poseStack.translate(0.5F, 0.5F, -0.03125F);
            poseStack.rotateDegrees(Axis.YP, 180.0F);
        }
    }

    public static void applyThirdPersonItemTransform(PoseStack poseStack, boolean flag, boolean flag2) {
        poseStack.translate(-0.0625F, 0.4375F, 0.0625F);
        float f = 0.625F;
        if (flag2) {
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            poseStack.translate(0.0F, -0.125F, 0.0F);
        }

        if (flag) {
            poseStack.translate(0.05F, 0.0F, -0.1F);
            poseStack.rotateDegrees(Axis.YP, -50.0F);
            poseStack.rotateDegrees(Axis.XP, -10.0F);
            poseStack.rotateDegrees(Axis.ZP, -60.0F);
        }

        poseStack.translate(0.0F, 0.1875F, 0.0F);
        poseStack.scale(0.625F, -0.625F, 0.625F);
        poseStack.rotateDegrees(Axis.XP, -100.0F);
        poseStack.rotateDegrees(Axis.YP, 45.0F);
    }

    public static float[] computeThirdPersonBlockingArmAngles(float value, float value2, float value3, float value4, float value5, boolean flag, boolean flag2) {
        float f = Mth.cos(value * 0.6662F + 3.141593F) * 2.0F * value2 * 0.5F;
        float f2 = 0.0F;
        if (flag) {
            f += -0.6283186F;
        }

        f = computeBlockingArmPitch(f);
        float f1 = 0.0F;
        if (value4 > -9990.0F) {
            float f3 = value4;
            float f4 = Mth.sin(sqrt(f3) * 3.141593F * 2.0F) * 0.2F;
            f1 += f4;
            f3 = 1.0F - value4;
            f3 *= f3;
            f3 *= f3;
            f3 = 1.0F - f3;
            float f5 = Mth.sin(f3 * 3.141593F);
            float f6 = Mth.sin(value4 * 3.141593F) * -(value5 - 0.7F) * 0.75F;
            f = (float)(f - (f5 * 1.2 + f6));
            f1 += f4 * 2.0F;
            f2 = Mth.sin(value4 * 3.141593F) * -0.4F;
        }

        if (flag2) {
            f += 0.4F;
        }

        f2 += Mth.cos(value3 * 0.09F) * 0.05F + 0.05F;
        f += Mth.sin(value3 * 0.067F) * 0.05F;
        return new float[]{f, f1, f2};
    }

    public static void flipThirdPersonZ(PoseStack poseStack) {
        poseStack.scale(1.0F, 1.0F, -1.0F);
    }

    public static float computeBlockingArmPitch(float pitch) {
        byte b0 = 3;
        return pitch * 0.5F - 0.94247794F;
    }

    public static float sqrt(float value) {
        return (float)Math.sqrt(value);
    }

    private static final class SmoothSwing {
        public static final float DURATION_SECONDS = 0.255F;
        public static float progress;
        public static float lastAttackAnim;
        public static boolean active;
        public static boolean queued;
        public static long lastUpdateNanos;

        public SmoothSwing() {
        }

        public static float advance(float value) {
            long i = System.nanoTime();
            float f = lastUpdateNanos == 0L ? 0.0F : Math.min((float)(i - lastUpdateNanos) / 1.0E9F, 0.05F);
            lastUpdateNanos = i;
            if (value < lastAttackAnim - 0.05F || lastAttackAnim <= 0.0F && value > 0.0F) {
                queued = true;
            }

            lastAttackAnim = value;
            if (!active && queued) {
                active = true;
                queued = false;
                progress = 0.0F;
            }

            if (active) {
                progress += f / 0.255F;
                if (progress >= 1.0F) {
                    if (queued) {
                        queued = false;
                        progress--;
                    } else {
                        active = false;
                        progress = 0.0F;
                    }
                }
            }

            return progress;
        }
    }

    public enum UseAction {
        NONE,
        EAT,
        DRINK,
        BLOCK,
        BOW;
    }
}
