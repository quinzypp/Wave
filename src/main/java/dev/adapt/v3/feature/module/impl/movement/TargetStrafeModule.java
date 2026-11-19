package dev.adapt.v3.feature.module.impl.movement;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.PreMovementPacketEvent;
import dev.adapt.v3.event.impl.StrafeEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.module.impl.combat.killaura.KillAuraModule;
import dev.adapt.v3.feature.module.impl.combat.killaura.target.AimedTarget;
import dev.adapt.v3.feature.module.impl.movement.flight.FlightModule;
import dev.adapt.v3.feature.module.impl.movement.speed.SpeedModule;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.ui.render.Render2D;
import dev.adapt.v3.ui.theme.Theme;
import dev.adapt.v3.util.player.MoveUtility;
import dev.adapt.v3.util.render.ColorUtility;
import dev.adapt.v3.util.render.ProjectionUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TargetStrafeModule extends Module {
    public final NumberProperty radius = new NumberProperty("Radius", 3.0, 0.1, 6.0, 0.1);
    public final BooleanProperty onlyOnJump = new BooleanProperty("Only on Jump", true);
    public final BooleanProperty render = new BooleanProperty("Render", true);
    public Float strafeYaw;
    public LivingEntity target;
    public boolean collidingLastTick;
    public boolean clockwise;
    public boolean strafing;
    public static final int CIRCLE_SEGMENTS = 72;
    public static final float LINE_WIDTH = 2.0F;
    public static final int GLOW_RADIUS = 12;
    public static final float FADE_SPEED = 7.0F;
    public float visibilityAlpha;
    public float activeProgress;
    public long lastRenderNs;
    public LivingEntity lastRenderedTarget;

    public TargetStrafeModule() {
        super("Target Strafe", "Circles Kill Aura's target while you move.", ModuleCategory.MOVEMENT);
        this.addProperties(this.radius, this.onlyOnJump, this.render);
    }

    public static TargetStrafeModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(TargetStrafeModule.class);
    }

    public static float overrideYaw(float yaw) {
        return isStrafing() ? getInstance().strafeYaw : yaw;
    }

    public static boolean isStrafing() {
        TargetStrafeModule targetstrafemodule = getInstance();
        return targetstrafemodule != null
            && targetstrafemodule.isEnabled()
            && targetstrafemodule.strafing
            && targetstrafemodule.target != null
            && targetstrafemodule.strafeYaw != null;
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.strafeYaw = null;
        this.strafing = false;
    }

    @Subscribe(priority = 100)
    public void onPreMotion(PreMovementPacketEvent event) {
        if (mc.player != null) {
            Adapt adapt = Adapt.getInstance();
            ScaffoldModule scaffoldmodule = adapt.getModuleManager().getByClass(ScaffoldModule.class);
            SpeedModule speedmodule = adapt.getModuleManager().getByClass(SpeedModule.class);
            FlightModule flightmodule = adapt.getModuleManager().getByClass(FlightModule.class);
            boolean flag = mc.options.keyJump.isDown();
            boolean flag1 = speedmodule != null && speedmodule.isEnabled() && (flag || !this.onlyOnJump.getValue());
            boolean flag2 = flightmodule != null && flightmodule.isEnabled() && flag;
            if (MoveUtility.isMoving() && (scaffoldmodule == null || !scaffoldmodule.isEnabled()) && (flag1 || flag2)) {
                this.target = getKillAuraTarget();
                if (this.target == null) {
                    this.strafing = false;
                    this.strafeYaw = null;
                } else {
                    this.strafing = true;
                    if (!mc.player.horizontalCollision && !mc.player.isInWater() && !mc.player.isInLava()) {
                        this.collidingLastTick = false;
                    } else {
                        if (!this.collidingLastTick) {
                            this.clockwise = !this.clockwise;
                        }

                        this.collidingLastTick = true;
                    }

                    float f = this.radius.getValue().floatValue();
                    double d0 = Math.toRadians(getYawToPos(this.target.position()) + (this.clockwise ? 1 : -1) * 135);
                    Vec3 vec3 = this.target.position().add(new Vec3(-Mth.sin((float)d0) * f, 0.0, Mth.cos((float)d0) * f));
                    this.strafeYaw = getYawToPos(vec3);
                }
            } else {
                this.strafing = false;
                this.strafeYaw = null;
            }
        }
    }

    @Subscribe(priority = 100)
    public void onStrafe(StrafeEvent event) {
        if (this.target != null && this.strafing && this.strafeYaw != null) {
            event.setYaw(this.strafeYaw);
        }
    }

    public void renderRing(GuiGraphicsExtractor graphics, float value) {
        long i = System.nanoTime();
        float f = this.lastRenderNs == 0L ? 0.0F : Math.min(0.1F, (float)(i - this.lastRenderNs) / 1.0E9F);
        this.lastRenderNs = i;
        LivingEntity livingentity = this.isEnabled() && this.render.getValue() && mc.player != null ? getKillAuraTarget() : null;
        boolean flag = livingentity != null && !livingentity.isRemoved();
        if (flag) {
            this.lastRenderedTarget = livingentity;
        }

        this.visibilityAlpha = approach(this.visibilityAlpha, flag ? 1.0F : 0.0F, f);
        this.activeProgress = approach(this.activeProgress, flag && this.strafing ? 1.0F : 0.0F, f);
        LivingEntity livingentity1 = flag ? livingentity : this.lastRenderedTarget;
        if (!(this.visibilityAlpha <= 0.01F) && livingentity1 != null && !livingentity1.isRemoved() && mc.player != null) {
            double d0 = this.radius.getValue();
            Vec3 vec3 = livingentity1.getPosition(value);
            float[][] afloat = new float[73][];

            for (int j = 0; j <= 72; j++) {
                double d1 = j * Math.PI * 2.0 / 72.0;
                afloat[j] = ProjectionUtility.projectToScreen(
                    vec3.x + d0 * Math.cos(d1), vec3.y + 0.01, vec3.z + d0 * Math.sin(d1), graphics.guiWidth(), graphics.guiHeight()
                );
            }

            float f2 = easeOutCubic(this.activeProgress);
            float f3 = this.visibilityAlpha * (0.5F + 0.5F * f2);
            float f1 = this.visibilityAlpha * Theme.getBloom() * (0.25F + 0.45F * f2);

            for (int k = 1; k <= 72; k++) {
                float[] afloat1 = afloat[k - 1];
                float[] afloat2 = afloat[k];
                if (afloat1 != null && afloat2 != null) {
                    Render2D.drawGlowLine(graphics, afloat1[0], afloat1[1], afloat2[0], afloat2[1], 12, ColorUtility.withAlphaFloat(getSegmentColor(k, f2), f1));
                }
            }

            for (int l = 1; l <= 72; l++) {
                float[] afloat3 = afloat[l - 1];
                float[] afloat4 = afloat[l];
                if (afloat3 != null && afloat4 != null) {
                    Render2D.drawLine(
                        graphics, afloat3[0], afloat3[1], afloat4[0], afloat4[1], 2.0F, ColorUtility.withAlphaFloat(getSegmentColor(l, f2), f3), false
                    );
                }
            }
        } else {
            if (this.visibilityAlpha <= 0.01F) {
                this.lastRenderedTarget = null;
            }
        }
    }

    public static int getSegmentColor(int color, float value) {
        int i = Theme.getAnimatedWaveColor(10, color * 360 / 72);
        return ColorUtility.lerpColor(ColorUtility.lerpColor(-1, i, 0.35F), i, value);
    }

    public static float approach(float value, float value2, float value3) {
        float f = 7.0F * value3;
        return value < value2 ? Math.min(value2, value + f) : Math.max(value2, value - f);
    }

    public static float easeOutCubic(float value) {
        float f = 1.0F - value;
        return 1.0F - f * f * f;
    }

    public static LivingEntity getKillAuraTarget() {
        Adapt adapt = Adapt.getInstance();
        KillAuraModule killauramodule = adapt == null ? null : adapt.getModuleManager().getByClass(KillAuraModule.class);
        if (killauramodule != null && killauramodule.isEnabled()) {
            AimedTarget aimedtarget = killauramodule.getTargetFinder().getAttackTarget();
            return aimedtarget == null ? null : aimedtarget.getEntity();
        } else {
            return null;
        }
    }

    public static float getYawToPos(Vec3 vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.z - mc.player.getZ();
        return (float)(Math.toDegrees(Math.atan2(d1, d0)) - 90.0);
    }
}
