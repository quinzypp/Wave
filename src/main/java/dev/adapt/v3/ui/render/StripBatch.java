package dev.adapt.v3.ui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class StripBatch implements GuiElementRenderState {
    public final RenderPipeline pipeline;
    public final TextureSetup textureSetup;
    public final Matrix3x2f pose;
    public final ScreenRectangle scissor;
    public float[] data = new float[144];
    public int count;
    public int minX = Integer.MAX_VALUE;
    public int minY = Integer.MAX_VALUE;
    public int maxX = Integer.MIN_VALUE;
    public int maxY = Integer.MIN_VALUE;
    public final boolean textured;

    public StripBatch(RenderPipeline pipeline, TextureSetup textureSetup, boolean textured, Matrix3x2fc pose, ScreenRectangle scissor) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.textured = textured;
        this.pose = new Matrix3x2f(pose);
        this.scissor = scissor;
    }

    public static StripBatch begin(GuiGraphicsExtractor g, RenderPipeline pipeline, GpuTextureView view, GpuSampler sampler) {
        if (!StripBatch.Access.ok()) {
            return null;
        }

        try {
            return new StripBatch(pipeline, TextureSetup.singleTexture(view, sampler), true, g.pose(), StripBatch.Access.scissor(g));
        } catch (Throwable throwable) {
            StripBatch.Access.fail();
            return null;
        }
    }

    public static StripBatch beginFill(GuiGraphicsExtractor g) {
        if (!StripBatch.Access.ok()) {
            return null;
        }

        try {
            return new StripBatch(RenderPipelines.GUI, TextureSetup.noTexture(), false, g.pose(), StripBatch.Access.scissor(g));
        } catch (Throwable throwable) {
            StripBatch.Access.fail();
            return null;
        }
    }

    public void addFill(int x0, int y0, int x1, int y1, int color) {
        if (color >>> 24 != 0) {
            this.add(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1), 0.0F, 0.0F, 0.0F, 0.0F, color);
        }
    }

    public void add(int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color) {
        if (x1 > x0 && y1 > y0) {
            int i = this.count * 9;
            if (i + 9 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }

            this.data[i] = x0;
            this.data[i + 1] = y0;
            this.data[i + 2] = x1;
            this.data[i + 3] = y1;
            this.data[i + 4] = u0;
            this.data[i + 5] = u1;
            this.data[i + 6] = v0;
            this.data[i + 7] = v1;
            this.data[i + 8] = Float.intBitsToFloat(color);
            this.count++;
            this.minX = Math.min(this.minX, x0);
            this.minY = Math.min(this.minY, y0);
            this.maxX = Math.max(this.maxX, x1);
            this.maxY = Math.max(this.maxY, y1);
        }
    }

    public void submit(GuiGraphicsExtractor g) {
        if (this.count != 0) {
            try {
                StripBatch.Access.state(g).addGuiElement(this);
            } catch (Throwable throwable) {
                StripBatch.Access.fail();
            }
        }
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (int i = 0; i < this.count; i++) {
            int j = i * 9;
            float f = this.data[j];
            float f1 = this.data[j + 1];
            float f2 = this.data[j + 2];
            float f3 = this.data[j + 3];
            float f4 = this.data[j + 4];
            float f5 = this.data[j + 5];
            float f6 = this.data[j + 6];
            float f7 = this.data[j + 7];
            int k = Float.floatToRawIntBits(this.data[j + 8]);
            if (this.textured) {
                consumer.addVertexWith2DPose(this.pose, f, f1).setUv(f4, f6).setColor(k);
                consumer.addVertexWith2DPose(this.pose, f, f3).setUv(f4, f7).setColor(k);
                consumer.addVertexWith2DPose(this.pose, f2, f3).setUv(f5, f7).setColor(k);
                consumer.addVertexWith2DPose(this.pose, f2, f1).setUv(f5, f6).setColor(k);
            } else {
                consumer.addVertexWith2DPose(this.pose, f, f1).setColor(k);
                consumer.addVertexWith2DPose(this.pose, f, f3).setColor(k);
                consumer.addVertexWith2DPose(this.pose, f2, f3).setColor(k);
                consumer.addVertexWith2DPose(this.pose, f2, f1).setColor(k);
            }
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return this.scissor;
    }

    @Override
    public ScreenRectangle bounds() {
        if (this.count == 0) {
            return null;
        }

        ScreenRectangle screenrectangle = new ScreenRectangle(this.minX, this.minY, this.maxX - this.minX, this.maxY - this.minY).transformMaxBounds(this.pose);
        return this.scissor != null ? this.scissor.intersection(screenrectangle) : screenrectangle;
    }

    private static final class Access {
        public static MethodHandle state;
        public static MethodHandle scissorStack;
        public static MethodHandle peek;
        public static boolean tried;
        public static boolean failed;

        public Access() {
        }

        public static boolean ok() {
            if (!tried) {
                tried = true;

                try {
                    Lookup lookup = MethodHandles.lookup();
                    Field field = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
                    field.setAccessible(true);
                    state = lookup.unreflectGetter(field);
                    Field field1 = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
                    field1.setAccessible(true);
                    scissorStack = lookup.unreflectGetter(field1);
                    Method method = field1.getType().getDeclaredMethod("peek");
                    method.setAccessible(true);
                    peek = lookup.unreflect(method);
                } catch (Throwable throwable) {
                    failed = true;
                }
            }

            return !failed;
        }

        public static void fail() {
            failed = true;
        }

        public static GuiRenderState state(GuiGraphicsExtractor g) throws Throwable {
            return (GuiRenderState)state.invoke((GuiGraphicsExtractor)g);
        }

        public static ScreenRectangle scissor(GuiGraphicsExtractor g) throws Throwable {
            return (ScreenRectangle)peek.invoke((Object)(Object)scissorStack.invoke((GuiGraphicsExtractor)g));
        }
    }
}
