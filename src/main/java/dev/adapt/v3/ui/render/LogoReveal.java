package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice.MappedView;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.pipeline.ShaderSource.CachedIncludeSource;
import dev.adapt.v3.Adapt;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class LogoReveal {
    public static final float DISSOLVE_START = 2.4F;
    public static final float BAR_FADE_IN_END = 1.1F;
    public static final float MIN_REVEAL_DURATION = 3.5F;
    public static RenderPipeline pipeline;
    public static CompiledRenderPipeline compiledPipeline;
    public static MappableRingBuffer uniformBuffer;
    public static boolean unavailable;
    public static Object activeOverlay;
    public static long revealStartNanos;
    public static boolean pendingDraw;
    public static float coverAlpha;
    public static float progress;
    public static float barAlpha;
    public static float handoffT = -1.0F;
    public static float logoAlpha = 1.0F;
    public static float targetMarkAlpha = 1.0F;
    public static long lastSubmitNanos;
    public static float targetCenterX;
    public static float targetCenterY;
    public static float targetSize;
    public static long lastTargetNanos;
    public static final long TARGET_TIMEOUT_NANOS = 100000000L;
    public static final float SPRING_PERIOD = 0.42F;
    public static final float INITIAL_SCALE = 0.2F;
    public static final float SWEEP_EXTENT = 1.6F;
    public static final float SWEEP_END = 2.3F;
    public static final float BREATHE_AMPLITUDE = 0.05F;
    public static final float BASE_LOGO_HALF = 0.72F;
    public static final float MIN_BEVEL_WIDTH = 0.22F;
    public static final float BAR_FADE_IN_START = 0.5F;
    public static final float BREATHE_START = 0.5F;
    public static final float MAX_LIGHT_ANGLE = 0.9F;
    public static final float RIPPLE_START_TIME = solveSpringTime(0.85F, 0.42F) - 0.01F;
    public static final float MORPH_START_TIME = solveSpringTime(0.92F, 0.42F) - 0.01F;

    public static boolean isAvailable() {
        return !unavailable;
    }

    public static void submit(Object key, float value, float value2, float value3) {
        submitWithHandoff(key, value, value2, value3, -1.0F, 1.0F, 1.0F);
    }

    public static void submitWithHandoff(Object key, float value, float value2, float value3, float value4, float value5, float value6) {
        trackOverlay(key);
        pendingDraw = true;
        lastSubmitNanos = System.nanoTime();
        coverAlpha = value;
        progress = value2;
        barAlpha = value3;
        handoffT = value4;
        logoAlpha = value5;
        targetMarkAlpha = value6;
    }

    public static void reportTargetLogoBounds(float value, float value2, float value3) {
        targetCenterX = value;
        targetCenterY = value2;
        targetSize = value3;
        lastTargetNanos = System.nanoTime();
    }

    public static boolean hasTargetLogo() {
        return System.nanoTime() - lastTargetNanos < 100000000L;
    }

    public static float getTargetLogoAlpha() {
        return System.nanoTime() - lastSubmitNanos < 100000000L ? targetMarkAlpha : 1.0F;
    }

    public static boolean isRevealComplete(Object key) {
        if (unavailable) {
            return true;
        }

        trackOverlay(key);
        return getElapsedSeconds() >= 3.5F;
    }

    public static void trackOverlay(Object key) {
        if (key != activeOverlay) {
            activeOverlay = key;
            revealStartNanos = System.nanoTime();
        }
    }

    public static float getElapsedSeconds() {
        return (float)(System.nanoTime() - revealStartNanos) / 1.0E9F;
    }

    public static void flush() {
        if (pendingDraw) {
            pendingDraw = false;
            if (!unavailable) {
                try {
                    ensurePipeline();
                    renderPass();
                } catch (Throwable throwable) {
                    unavailable = true;
                    Adapt.LOGGER.warn("AdaptV3 loading screen shader unavailable, using the vanilla one", throwable);
                }
            }
        }
    }

    public static void renderPass() {
        RenderTarget rendertarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        float f = rendertarget.width;
        float f1 = rendertarget.height;
        float f2 = Math.min(f, f1) * 0.27F;
        float f3 = Math.min(getElapsedSeconds(), 3.5F);
        LogoReveal.Params logoreveal$params = evaluateParams(f3);
        float f4 = barAlpha * smoothstep(0.5F, 1.1F, f3);
        float f5 = 0.0F;
        float f6 = 0.0F;
        float f7 = 1.0F;
        boolean flag = handoffT >= 0.0F;
        if (flag) {
            float f8 = Minecraft.getInstance().getWindow().getGuiScale();
            float f9 = logoreveal$params.logoHalf * f2 / 2.0F;
            float f10 = Math.max(1.0F, targetSize * f8 / 2.0F);
            f5 = lerp(f / 2.0F, targetCenterX * f8, handoffT);
            f6 = lerp(f1 / 2.0F, f1 - targetCenterY * f8, handoffT);
            f7 = f9 * (float)Math.pow(f10 / f9, handoffT);
        }

        try (MappedView mappedview = uniformBuffer.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(mappedview.data())
                .putVec4(f, f1, f2, coverAlpha)
                .putVec4(progress, f4, logoreveal$params.plateScale, logoreveal$params.shapeN)
                .putVec4(logoreveal$params.optics, logoreveal$params.plateAlpha, logoreveal$params.logoHalf, logoreveal$params.sweep)
                .putVec4(logoreveal$params.lightAng, logoreveal$params.bold, logoreveal$params.blur, logoreveal$params.frost)
                .putVec4(logoreveal$params.dome, logoreveal$params.drop, logoreveal$params.rim, logoreveal$params.halo)
                .putVec4(logoreveal$params.ghost, logoreveal$params.ripR, logoreveal$params.ripA, logoreveal$params.bare)
                .putVec4(logoreveal$params.reach, logoreveal$params.cornerOn, logoreveal$params.dissolve, logoreveal$params.bevelW)
                .putVec4(f5, f6, f7, flag ? 1.0F : 0.0F)
                .putVec4(logoAlpha, flag ? saturate(handoffT) : 0.0F, 0.0F, 0.0F);
        }

        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

        try (RenderPass renderpass = commandencoder.createRenderPass(() -> "AdaptV3 loading screen", rendertarget.getColorTextureView(), Optional.empty())) {
            renderpass.setPipeline(compiledPipeline);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("LogoReveal", uniformBuffer.currentBuffer());
            renderpass.draw(3, 1, 0, 0);
        }

        uniformBuffer.rotate();
    }

    public static void ensurePipeline() {
        if (compiledPipeline == null) {
            pipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("adaptv3", "pipeline/logo_reveal"))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("adaptv3", "post/logo_reveal"))
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("LogoReveal", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .build();
            compiledPipeline = RenderSystem.getDevice()
                .compilePipeline(pipeline, new LogoReveal.ClasspathShaderSource(), Util.backgroundExecutor())
                .join()
                .finishCompile();
            if (compiledPipeline == null) {
                throw new IllegalStateException("Failed to compile the loading screen shader");
            }

            uniformBuffer = new MappableRingBuffer(() -> "AdaptV3 loading screen", 130, 144);
        }
    }

    public static LogoReveal.Params evaluateParams(float value) {
        float f = springResponse(value + 0.01F, 0.42F);
        float f1 = springResponse(value - MORPH_START_TIME, 0.4F);
        float f2 = 1.0F - smoothstep(0.05F, 1.0F, f1);
        float f3 = smoothstep(0.0F, 0.08F, value);
        float f4 = 0.05F * springResponse(value - 0.3F, 2.2F);
        float f5 = 0.25F * springResponse(value, 0.7F) + 0.75F * smootherstep(0.0F, 2.3F, value);
        float f6 = springResponse(value - 0.5F, 2.4F);
        float f7 = springResponse(value - 2.4F - 0.05F, 0.65F);
        float f8 = (1.0F + 0.6F * (1.0F - f5)) * (1.0F - 0.045F * f6);
        float f9 = lerp(0.22F, Math.max(0.22F, 0.4F), f2);
        float f10 = lerp(0.2F, 1.0F, f) * (1.0F + f4) * (1.0F + 0.14F * f7);
        return new LogoReveal.Params(
            f10,
            lerp(2.0F, 5.0F, f1),
            1.0F - smoothstep(0.55F, 1.0F, f7),
            f3 * (1.0F - smoothstep(0.45F, 1.0F, f7)),
            0.72F * f8,
            lerp(-1.6F, 1.6F, smootherstep(0.9F, 2.3000002F, value)),
            0.9F * smootherstep(0.4F, 2.8000002F, value),
            0.015F * (1.0F - f5),
            0.05F * (1.0F - smoothstep(0.3F, 0.95F, f)),
            1.0F - smoothstep(0.5F, 0.95F, f),
            0.15F * (1.0F - smoothstep(0.4F, 0.95F, f)),
            f2,
            1.0F + 0.8F * f2,
            0.5F + 0.5F * f2,
            smoothstep(0.0F, 0.03F, value) * (1.0F - smoothstep(0.04F, 0.08F, value)),
            1.0F + 0.9F * Math.max(0.0F, value - RIPPLE_START_TIME),
            0.5F * smoothstep(RIPPLE_START_TIME, RIPPLE_START_TIME + 0.06F, value) * (float)Math.exp(-Math.max(0.0F, value - RIPPLE_START_TIME) / 0.6F),
            smoothstep(2.15F, 2.4F, value),
            f9 * f10,
            smoothstep(0.35F, 1.0F, f1),
            f7,
            f9 * (1.0F - f7)
        );
    }

    public static float criticallyDampedSpring(float first, float second) {
        return springResponse(first, second);
    }

    public static float springResponse(float first, float second) {
        if (first <= 0.0F) {
            return 0.0F;
        }

        double d0 = (Math.PI * 2) / second;
        return (float)(1.0 - Math.exp(-d0 * first) * (1.0 + d0 * first));
    }

    public static float solveSpringTime(float first, float second) {
        float f = 0.0F;
        float f1 = 3.0F;

        for (int i = 0; i < 40; i++) {
            float f2 = (f + f1) / 2.0F;
            if (springResponse(f2, second) < first) {
                f = f2;
            } else {
                f1 = f2;
            }
        }

        return f1;
    }

    public static float saturate(float value) {
        return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
    }

    public static float lerp(float value, float value2, float value3) {
        return value + (value2 - value) * value3;
    }

    public static float smoothstep(float value, float value2, float value3) {
        float f = saturate((value3 - value) / (value2 - value));
        return f * f * (3.0F - 2.0F * f);
    }

    public static float smootherstep(float value, float value2, float value3) {
        float f = saturate((value3 - value) / (value2 - value));
        return f * f * f * (f * (f * 6.0F - 15.0F) + 10.0F);
    }

    private static final class ClasspathShaderSource implements ShaderSource {
        public ClasspathShaderSource() {
        }

        @Override
        public String getShader(Identifier id, ShaderType shaderType) {
            Identifier identifier = shaderType.idConverter().idToFile(id);

            try {
                if (identifier.getNamespace().equals("adaptv3")) {
                    try (InputStream inputstream = LogoReveal.class.getResourceAsStream("/assets/adaptv3/" + identifier.getPath())) {
                        return inputstream == null ? null : new String(inputstream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } else {
                    return Minecraft.getInstance().getVanillaPackResources().asResourceManager().getResourceOrThrow(identifier).readAllAsString();
                }
            } catch (Exception exception) {
                Adapt.LOGGER.warn("Couldn't read shader {}", identifier, exception);
                return null;
            }
        }

        @Override
        public CachedIncludeSource getInclude(Identifier id) {
            return null;
        }

        @Override
        public void close() {
        }
    }

    private record Params(
        float plateScale,
        float shapeN,
        float optics,
        float plateAlpha,
        float logoHalf,
        float sweep,
        float lightAng,
        float bold,
        float blur,
        float frost,
        float dome,
        float drop,
        float rim,
        float halo,
        float ghost,
        float ripR,
        float ripA,
        float bare,
        float reach,
        float cornerOn,
        float dissolve,
        float bevelW
    ) {
    }
}
