package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.adapt.v3.Adapt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class BackdropBlur {
    public static final int BLUR_PASSES = 2;
    public static final Identifier BLUR_CHAIN_ID = Identifier.withDefaultNamespace("blur");
    public static final Identifier OPAQUE_CHAIN_ID = Identifier.fromNamespaceAndPath("adaptv3", "opaque");
    public static TextureTarget backdropTarget;
    public static CrossFrameResourcePool resourcePool;
    public static boolean requested;
    public static boolean unavailableLogged;
    public static final int BLUR_RADIUS = 5;
    public static GlobalSettingsUniform globalUniform;
    public static int capturedWidth;
    public static int capturedHeight;
    public static int capturedMenuBlurRadius;
    public static double capturedGlintAlpha;
    public static long capturedGameTime;
    public static float capturedPartialTicks;
    public static Vec3 capturedCameraPos = Vec3.ZERO;
    public static boolean capturedUseRgss;
    public static boolean reentrantUpdate;

    public static void captureGlobalSettings(
        GlobalSettingsUniform globalSettingsUniform, int value, int value2, double value3, long time, float value4, int value5, Vec3 vec, boolean flag
    ) {
        if (!reentrantUpdate) {
            globalUniform = globalSettingsUniform;
            capturedWidth = value;
            capturedHeight = value2;
            capturedGlintAlpha = value3;
            capturedGameTime = time;
            capturedPartialTicks = value4;
            capturedMenuBlurRadius = value5;
            capturedCameraPos = vec;
            capturedUseRgss = flag;
        }
    }

    public static void overrideBlurRadius(int value) {
        reentrantUpdate = true;

        try {
            globalUniform.update(
                capturedWidth, capturedHeight, capturedGlintAlpha, capturedGameTime, capturedPartialTicks, value, capturedCameraPos, capturedUseRgss
            );
        } finally {
            reentrantUpdate = false;
        }
    }

    public static void request() {
        requested = true;
    }

    public static GpuTextureView getTextureView() {
        request();
        RenderTarget rendertarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        ensureTarget(rendertarget.width, rendertarget.height);
        return backdropTarget == null ? null : backdropTarget.getColorTextureView();
    }

    public static GpuSampler getSampler() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    public static int getWidth() {
        return backdropTarget == null ? 1 : backdropTarget.width;
    }

    public static int getHeight() {
        return backdropTarget == null ? 1 : backdropTarget.height;
    }

    public static void ensureTarget(int first, int second) {
        if (backdropTarget == null) {
            backdropTarget = new TextureTarget("adaptv3_backdrop", first, second, GpuFormat.RGBA8_UNORM, null);
            resourcePool = new CrossFrameResourcePool(3);
        } else if (backdropTarget.width != first || backdropTarget.height != second) {
            backdropTarget.resize(first, second);
        }
    }

    public static void process() {
        if (requested && backdropTarget != null) {
            requested = false;

            try {
                Minecraft minecraft = Minecraft.getInstance();
                RenderTarget rendertarget = minecraft.gameRenderer.mainRenderTarget();
                ensureTarget(rendertarget.width, rendertarget.height);
                backdropTarget.copyColorFrom(rendertarget);
                PostChain postchain = minecraft.getShaderManager().getPostChain(OPAQUE_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
                if (postchain != null) {
                    postchain.process(backdropTarget, resourcePool);
                }

                PostChain postchain1 = minecraft.getShaderManager().getPostChain(BLUR_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
                if (postchain1 != null) {
                    boolean flag = globalUniform != null && capturedMenuBlurRadius != 5;
                    if (flag) {
                        overrideBlurRadius(5);
                    }

                    try {
                        for (int i = 0; i < 2; i++) {
                            postchain1.process(backdropTarget, resourcePool);
                        }
                    } finally {
                        if (flag) {
                            overrideBlurRadius(capturedMenuBlurRadius);
                        }
                    }
                }

                resourcePool.endFrame();
            } catch (Throwable throwable) {
                if (!unavailableLogged) {
                    unavailableLogged = true;
                    Adapt.LOGGER.warn("AdaptV3 backdrop blur unavailable", throwable);
                }
            }
        }
    }
}
