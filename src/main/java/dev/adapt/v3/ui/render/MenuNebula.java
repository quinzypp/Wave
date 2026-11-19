package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice.MappedView;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import dev.adapt.v3.Adapt;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class MenuNebula {
    public static final long START_NANOS = System.nanoTime();
    public static RenderPipeline pipeline;
    public static MappableRingBuffer uniformBuffer;
    public static boolean requested;
    public static boolean unavailable;
    public static boolean verifiedCompiled;

    public static void request() {
        requested = true;
    }

    public static void flush() {
        if (requested) {
            requested = false;
            if (!unavailable && Minecraft.getInstance().isGameLoadFinished()) {
                try {
                    ensurePipeline();
                    if (!verifiedCompiled) {
                        verifiedCompiled = true;
                        if (RenderSystem.getCompiledPipelineNullable(pipeline) == null) {
                            throw new IllegalStateException("Failed to compile menu nebula");
                        }
                    }

                    renderPass();
                } catch (Throwable throwable) {
                    unavailable = true;
                    Adapt.LOGGER.warn("AdaptV3 menu backdrop shader unavailable", throwable);
                }
            }
        }
    }

    public static boolean isAvailable() {
        return !unavailable;
    }

    public static void renderPass() {
        RenderTarget rendertarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();

        try (MappedView mappedview = uniformBuffer.currentBuffer().map(false, true)) {
            float f = (float)(System.nanoTime() - START_NANOS) / 1.0E9F % 3600.0F;
            Std140Builder.intoBuffer(mappedview.data()).putVec4(rendertarget.width, rendertarget.height, f, 1.0F);
        }

        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

        try (RenderPass renderpass = commandencoder.createRenderPass(() -> "AdaptV3 menu nebula", rendertarget.getColorTextureView(), Optional.empty())) {
            renderpass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("MenuNebula", uniformBuffer.currentBuffer());
            renderpass.draw(3, 1, 0, 0);
        }

        uniformBuffer.rotate();
    }

    public static void ensurePipeline() {
        if (pipeline == null) {
            pipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("adaptv3", "pipeline/menu_nebula"))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("adaptv3", "post/menu_nebula"))
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("MenuNebula", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(ColorTargetState.DEFAULT)
                .build();
            uniformBuffer = new MappableRingBuffer(() -> "AdaptV3 menu nebula", 130, 16);
        }
    }
}
