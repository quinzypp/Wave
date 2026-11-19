package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

public final class SmoothTexture extends DynamicTexture {
    public SmoothTexture(String text, NativeImage nativeImage) {
        this(text, nativeImage, false);
    }

    public SmoothTexture(String text, NativeImage nativeImage, boolean flag) {
        super(() -> text, nativeImage);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(flag ? FilterMode.NEAREST : FilterMode.LINEAR);
    }
}
