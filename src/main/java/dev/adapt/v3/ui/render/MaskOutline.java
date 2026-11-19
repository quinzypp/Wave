package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice.MappedView;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import dev.adapt.v3.Adapt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;

public final class MaskOutline {
    public static final int MAX_SHAPES = 32;
    public static final int MAX_EDGES = 256;
    public static final int SHAPES_UBO_SIZE = 4624;
    public static RenderPipeline maskPipeline;
    public static RenderPipeline blurPipeline;
    public static RenderPipeline compositePipeline;
    public static TextureTarget maskTarget;
    public static TextureTarget pingTarget;
    public static TextureTarget pongTarget;
    public static MappableRingBuffer shapesUbo;
    public static MappableRingBuffer blurHUbo;
    public static MappableRingBuffer blurVUbo;
    public static MappableRingBuffer compositeUbo;
    public static boolean unavailable;
    public static boolean initialized;
    public static final List<float[]> queuedPolygons = new ArrayList<>();
    public static int outlineColor;
    public static float outlineWidth;
    public static float glowRadius;
    public static float glowAlpha;

    public static void submit(List<float[]> floatArrays, int value, float value2, float value3, float value4) {
        queuedPolygons.clear();
        if (!unavailable) {
            int i = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());

            for (float[] afloat : floatArrays) {
                if (afloat.length >= 6) {
                    float[] afloat1 = new float[afloat.length];

                    for (int j = 0; j < afloat.length; j++) {
                        afloat1[j] = afloat[j] * i;
                    }

                    queuedPolygons.add(afloat1);
                }
            }

            outlineColor = value;
            outlineWidth = value2;
            glowRadius = value3 * i;
            glowAlpha = value4;
        }
    }

    public static void flush() {
        if (!unavailable && Minecraft.getInstance().isGameLoadFinished()) {
            if (!initialized) {
                initialized = true;

                try {
                    RenderTarget rendertarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                    ensureResources(rendertarget.width, rendertarget.height);

                    for (RenderPipeline renderpipeline : new RenderPipeline[]{maskPipeline, blurPipeline, compositePipeline}) {
                        if (RenderSystem.getCompiledPipelineNullable(renderpipeline) == null) {
                            throw new IllegalStateException("Failed to compile " + renderpipeline.getLocation());
                        }
                    }

                    Adapt.LOGGER.info("AdaptV3 ESP outline shaders ready");
                } catch (Throwable throwable1) {
                    unavailable = true;
                    Adapt.LOGGER.warn("AdaptV3 ESP outline shader unavailable", throwable1);
                    return;
                }
            }

            if (!queuedPolygons.isEmpty()) {
                try {
                    executePasses();
                } catch (Throwable throwable) {
                    unavailable = true;
                    Adapt.LOGGER.warn("AdaptV3 ESP outline shader unavailable", throwable);
                } finally {
                    queuedPolygons.clear();
                }
            }
        }
    }

    public static void executePasses() {
        RenderTarget rendertarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int i = rendertarget.width;
        int j = rendertarget.height;
        ensureResources(i, j);
        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler gpusampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler gpusampler1 = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        uploadShapesUniform(i, j);

        try (RenderPass renderpass = commandencoder.createRenderPass(
                () -> "AdaptV3 ESP mask", maskTarget.getColorTextureView(), Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F))
            )) {
            renderpass.setPipeline(RenderSystem.getCompiledPipeline(maskPipeline));
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("EspShapes", shapesUbo.currentBuffer());
            renderpass.draw(3, 1, 0, 0);
        }

        shapesUbo.rotate();
        float f = Math.max(1, Math.round(glowRadius));
        runBlurPass(commandencoder, blurHUbo, maskTarget, pingTarget, 1.0F / i, 0.0F, f, gpusampler);
        runBlurPass(commandencoder, blurVUbo, pingTarget, pongTarget, 0.0F, 1.0F / j, f, gpusampler);

        try (MappedView mappedview = compositeUbo.currentBuffer().map(false, true)) {
            Std140Builder std140builder = Std140Builder.intoBuffer(mappedview.data());
            std140builder.putVec4(
                (outlineColor >> 16 & 0xFF) / 255.0F,
                (outlineColor >> 8 & 0xFF) / 255.0F,
                (outlineColor & 0xFF) / 255.0F,
                (outlineColor >>> 24 & 0xFF) / 255.0F
            );
            std140builder.putVec4(1.0F / i, 1.0F / j, Math.max(1, Math.round(outlineWidth)), glowAlpha);
        }

        try (RenderPass renderpass1 = commandencoder.createRenderPass(() -> "AdaptV3 ESP composite", rendertarget.getColorTextureView(), Optional.empty())) {
            renderpass1.setPipeline(RenderSystem.getCompiledPipeline(compositePipeline));
            RenderSystem.bindDefaultUniforms(renderpass1);
            renderpass1.setUniform("EspComposite", compositeUbo.currentBuffer());
            renderpass1.setUniform("MaskSampler", maskTarget.getColorTextureView(), gpusampler);
            renderpass1.setUniform("GlowSampler", pongTarget.getColorTextureView(), gpusampler);
            renderpass1.draw(3, 1, 0, 0);
        }

        compositeUbo.rotate();
    }

    public static void runBlurPass(
        CommandEncoder commandEncoder, MappableRingBuffer mappableRingBuffer, RenderTarget renderTarget, RenderTarget renderTarget2, float value, float value2, float value3, GpuSampler gpuSampler
    ) {
        try (MappedView mappedview = mappableRingBuffer.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(mappedview.data()).putVec4(value, value2, value3, 0.0F);
        }

        try (RenderPass renderpass = commandEncoder.createRenderPass(
                () -> "AdaptV3 ESP blur", renderTarget2.getColorTextureView(), Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F))
            )) {
            renderpass.setPipeline(RenderSystem.getCompiledPipeline(blurPipeline));
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("EspBlur", mappableRingBuffer.currentBuffer());
            renderpass.setUniform("InSampler", renderTarget.getColorTextureView(), gpuSampler);
            renderpass.draw(3, 1, 0, 0);
        }

        mappableRingBuffer.rotate();
    }

    public static void uploadShapesUniform(int first, int second) {
        ArrayList arraylist = new ArrayList();
        ArrayList arraylist1 = new ArrayList();

        for (float[] afloat : queuedPolygons) {
            if (arraylist1.size() >= 32) {
                break;
            }

            int i = afloat.length / 2;
            if (arraylist.size() + i > 256) {
                break;
            }

            float f = 0.0F;

            for (int j = 0; j < i; j++) {
                int k = (j + 1) % i;
                f += afloat[j * 2] * afloat[k * 2 + 1] - afloat[k * 2] * afloat[j * 2 + 1];
            }

            float f4 = f >= 0.0F ? 1.0F : -1.0F;
            int l1 = arraylist.size();

            for (int l = 0; l < i; l++) {
                int i1 = (l + 1) % i;
                float f1 = afloat[i1 * 2] - afloat[l * 2];
                float f2 = afloat[i1 * 2 + 1] - afloat[l * 2 + 1];
                float f3 = (float)Math.sqrt(f1 * f1 + f2 * f2);
                if (!(f3 < 1.0E-4F)) {
                    arraylist.add(new float[]{afloat[l * 2], afloat[l * 2 + 1], f4 * f2 / f3, -f4 * f1 / f3});
                }
            }

            if (arraylist.size() - l1 >= 3) {
                arraylist1.add(new int[]{l1, arraylist.size() - l1});
            } else {
                while (arraylist.size() > l1) {
                    arraylist.remove(arraylist.size() - 1);
                }
            }
        }

        try (MappedView mappedview = shapesUbo.currentBuffer().map(false, true)) {
            Std140Builder std140builder = Std140Builder.intoBuffer(mappedview.data());
            std140builder.putVec4(arraylist1.size(), first, second, 0.0F);

            for (int j1 = 0; j1 < 32; j1++) {
                if (j1 < arraylist1.size()) {
                    std140builder.putVec4(((int[])arraylist1.get(j1))[0], ((int[])arraylist1.get(j1))[1], 0.0F, 0.0F);
                } else {
                    std140builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
                }
            }

            for (int k1 = 0; k1 < 256; k1++) {
                if (k1 < arraylist.size()) {
                    float[] afloat1 = (float[])arraylist.get(k1);
                    std140builder.putVec4(afloat1[0], afloat1[1], afloat1[2], afloat1[3]);
                } else {
                    std140builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
                }
            }
        }
    }

    public static void ensureResources(int first, int second) {
        if (maskPipeline == null) {
            maskPipeline = buildPipeline(
                "esp_mask", BindGroupLayout.builder().withUniform("EspShapes", UniformType.UNIFORM_BUFFER).build(), ColorTargetState.DEFAULT
            );
            blurPipeline = buildPipeline(
                "esp_blur",
                BindGroupLayout.builder()
                    .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("EspBlur", UniformType.UNIFORM_BUFFER)
                    .build(),
                ColorTargetState.DEFAULT
            );
            compositePipeline = buildPipeline(
                "esp_composite",
                BindGroupLayout.builder()
                    .withUniform("MaskSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("GlowSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("EspComposite", UniformType.UNIFORM_BUFFER)
                    .build(),
                new ColorTargetState(BlendFunction.TRANSLUCENT)
            );
            short short1 = 130;
            shapesUbo = new MappableRingBuffer(() -> "AdaptV3 ESP shapes", 130, 4624);
            blurHUbo = new MappableRingBuffer(() -> "AdaptV3 ESP blur H", 130, 16);
            blurVUbo = new MappableRingBuffer(() -> "AdaptV3 ESP blur V", 130, 16);
            compositeUbo = new MappableRingBuffer(() -> "AdaptV3 ESP composite", 130, 32);
        }

        if (maskTarget == null) {
            maskTarget = new TextureTarget("adaptv3_esp_mask", first, second, GpuFormat.RGBA8_UNORM, null);
            pingTarget = new TextureTarget("adaptv3_esp_ping", first, second, GpuFormat.RGBA8_UNORM, null);
            pongTarget = new TextureTarget("adaptv3_esp_pong", first, second, GpuFormat.RGBA8_UNORM, null);
        } else if (maskTarget.width != first || maskTarget.height != second) {
            maskTarget.resize(first, second);
            pingTarget.resize(first, second);
            pongTarget.resize(first, second);
        }
    }

    public static RenderPipeline buildPipeline(String text, BindGroupLayout bindGroupLayout, ColorTargetState colorTargetState) {
        return RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("adaptv3", "pipeline/" + text))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("adaptv3", "post/" + text))
            .withBindGroupLayout(bindGroupLayout)
            .withColorTargetState(colorTargetState)
            .build();
    }
}
