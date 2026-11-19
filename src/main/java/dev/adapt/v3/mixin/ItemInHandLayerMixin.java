package dev.adapt.v3.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import dev.adapt.v3.feature.module.impl.visual.animations.Animations17;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin extends RenderLayer {
    @Unique
    private final ItemStackRenderState adapt$noneRenderState = new ItemStackRenderState();

    protected ItemInHandLayerMixin(RenderLayerParent renderer) {
        super(renderer);
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void adapt$blocking17(
        ArmedEntityRenderState state,
        ItemStackRenderState item,
        ItemStack itemStack,
        HumanoidArm arm,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        CallbackInfo ci
    ) {
        if (arm == HumanoidArm.LEFT
            && state instanceof AvatarRenderState avatarrenderstate
            && avatarrenderstate.mainArm == HumanoidArm.RIGHT
            && AnimationsModule.shouldHideOffhandShield(itemStack)) {
            ci.cancel();
        } else if (arm == HumanoidArm.RIGHT
            && !item.isEmpty()
            && state instanceof AvatarRenderState avatarrenderstate1
            && avatarrenderstate1.rightArmPose == ArmPose.BLOCK
            && !(itemStack.getItem() instanceof ShieldItem)
            && AnimationsModule.isActive()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                poseStack.pushPose();
                ((ArmedModel)this.getParentModel()).translateToHand(state, arm, poseStack);
                boolean flag = itemStack.is(Items.FISHING_ROD) || itemStack.is(Items.CARROT_ON_A_STICK) || itemStack.is(Items.WARPED_FUNGUS_ON_A_STICK);
                Animations17.applyThirdPersonItemTransform(poseStack, true, flag);
                ItemStackRenderState itemstackrenderstate = this.adapt$noneRenderState;
                itemstackrenderstate.clear();
                minecraft.getItemModelResolver().updateForTopItem(itemstackrenderstate, itemStack, ItemDisplayContext.NONE, minecraft.level, null, 0);
                boolean flag1 = !(itemStack.getItem() instanceof BlockItem) || !itemstackrenderstate.usesBlockLight();
                Animations17.applyFlatItemTransform(poseStack, flag1);
                Animations17.flipThirdPersonZ(poseStack);
                itemstackrenderstate.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
                ci.cancel();
            }
        }
    }
}
