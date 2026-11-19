package dev.adapt.v3.mixin;

import dev.adapt.v3.feature.module.impl.combat.BacktrackModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.Gizmos.TemporaryCollection;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Unique
    private static final float ADAPT$OUTLINE_WIDTH = 2.0F;

    @Shadow
    public abstract TemporaryCollection collectPerFrameMainThreadGizmos();

    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void adapt$extractBacktrackBox(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo ci) {
        BacktrackModule backtrackmodule = BacktrackModule.getInstance();
        BacktrackModule.EspData backtrackmodule$espdata = backtrackmodule == null
            ? null
            : backtrackmodule.getEspData(deltaTracker.getGameTimeDeltaPartialTick(false));
        if (backtrackmodule$espdata != null && !backtrackmodule$espdata.entity().isRemoved()) {
            EntityDimensions entitydimensions = backtrackmodule$espdata.entity().getDimensions(backtrackmodule$espdata.entity().getPose());
            double d0 = entitydimensions.width() / 2.0;
            AABB aabb = new AABB(-d0, 0.0, -d0, d0, entitydimensions.height(), d0).inflate(0.05).move(backtrackmodule$espdata.pos());

            try (TemporaryCollection temporarycollection = this.collectPerFrameMainThreadGizmos()) {
                Gizmos.cuboid(aabb, GizmoStyle.strokeAndFill(backtrackmodule$espdata.boxOutline(), 2.0F, backtrackmodule$espdata.boxFill()));
            }
        }
    }
}
