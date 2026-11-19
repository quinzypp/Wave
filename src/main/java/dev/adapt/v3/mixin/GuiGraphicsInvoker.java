package dev.adapt.v3.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsInvoker {
    @Invoker("innerBlit")
    void adapt$innerBlit(
        RenderPipeline renderPipeline,
        GpuTextureView gpuTextureView,
        GpuSampler gpuSampler,
        int value2,
        int value22,
        int value3,
        int value4,
        float value5,
        float value6,
        float value7,
        float value8,
        int value9
    );
}
