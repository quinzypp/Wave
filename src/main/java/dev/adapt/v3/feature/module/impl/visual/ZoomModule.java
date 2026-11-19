package dev.adapt.v3.feature.module.impl.visual;

import com.mojang.blaze3d.platform.InputConstants;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.binding.Bind;
import dev.adapt.v3.binding.type.InputType;
import dev.adapt.v3.event.impl.MouseScrollEvent;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.util.MouseUtility;
import dev.adapt.v3.util.animation.Spring;

public final class ZoomModule extends Module {
    public final NumberProperty zoom = new NumberProperty("Zoom", "x", 4.0, 1.5, 12.0, 0.5);
    public final BooleanProperty scrollAdjust = new BooleanProperty("Scroll Adjust", true);
    public final BooleanProperty smoothCamera = new BooleanProperty("Smooth Camera", false);
    public final Spring zoomSpring = new Spring(1.0F, 160.0F, 20.0F);
    public float scrollMultiplier = 1.0F;
    public boolean wasZooming;
    public boolean prevSmoothCamera;

    public ZoomModule() {
        super("Zoom", "Hold the zoom key for a buttery spring zoom. Scroll to magnify.", ModuleCategory.UTILITY);
        this.addProperties(this.zoom, this.scrollAdjust, this.smoothCamera);
    }

    @Override
    public void onBind() {
        this.setEnabled(true);
    }

    @Subscribe
    public void onEndTick(PostTickEvent event) {
        if (!this.isBindDown()) {
            this.setEnabled(false);
        }
    }

    public boolean isBindDown() {
        Bind bind = Adapt.getInstance().getBindingManager().getRegistry().getBind(this).orElse(null);
        if (bind == null) {
            return false;
        } else {
            return bind.type() == InputType.MOUSE ? MouseUtility.isButtonDown(bind.code()) : InputConstants.isKeyDown(bind.code());
        }
    }

    public boolean isZooming() {
        return this.isEnabled() && mc.gui.screen() == null;
    }

    public float modifyFov(float fov) {
        boolean flag = this.isZooming();
        if (flag != this.wasZooming) {
            if (flag) {
                this.scrollMultiplier = 1.0F;
                this.prevSmoothCamera = mc.options.smoothCamera;
                if (this.smoothCamera.getValue()) {
                    mc.options.smoothCamera = true;
                }
            } else if (this.smoothCamera.getValue()) {
                mc.options.smoothCamera = this.prevSmoothCamera;
            }

            this.wasZooming = flag;
        }

        this.zoomSpring.target(flag ? this.zoom.getFloatValue() * this.scrollMultiplier : 1.0F);
        float f = Math.max(1.0F, this.zoomSpring.update());
        return fov / f;
    }

    @Subscribe
    public void onMouseScroll(MouseScrollEvent event) {
        if (this.scrollAdjust.getValue() && this.isZooming()) {
            this.scrollMultiplier = Math.max(0.4F, Math.min(4.0F, this.scrollMultiplier * (event.getVerticalAmount() > 0.0 ? 1.18F : 0.84745765F)));
            event.cancel();
        }
    }
}
