package dev.adapt.v3.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import dev.adapt.v3.feature.module.impl.visual.animations.Animations17;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
    @Unique
    private final ItemStackRenderState adapt$noneRenderState = new ItemStackRenderState();

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void adapt$render17(
        PlayerRenderState playerState,
        FirstPersonHandsAndItemsRenderState state,
        float partialTicks,
        float xRot,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        CallbackInfo ci
    ) {
        AnimationsModule animationsmodule = AnimationsModule.getInstance();
        if (animationsmodule != null && animationsmodule.isEnabled() && !state.isScoping) {
            AvatarRenderState avatarrenderstate = playerState.avatarRenderState;
            Minecraft minecraft = Minecraft.getInstance();
            if (hand == InteractionHand.OFF_HAND && AnimationsModule.shouldHideOffhandShield(itemStack)) {
                ci.cancel();
            } else if (avatarrenderstate != null
                && minecraft.player != null
                && hand == InteractionHand.MAIN_HAND
                && avatarrenderstate.mainArm == HumanoidArm.RIGHT
                && !itemStack.isEmpty()
                && !itemStack.has(DataComponents.MAP_ID)
                && !itemStack.is(Items.CROSSBOW)
                && !avatarrenderstate.isAutoSpinAttack) {
                int i = 0;
                Animations17.UseAction animations17$useaction = Animations17.UseAction.NONE;
                if (avatarrenderstate.isUsingItem && state.useItemRemainingTicks > 0 && avatarrenderstate.useItemHand == InteractionHand.MAIN_HAND) {
                    ItemUseAnimation itemuseanimation = itemStack.getUseAnimation();
                    switch (itemuseanimation) {
                        case EAT:
                            animations17$useaction = Animations17.UseAction.EAT;
                            break;
                        case DRINK:
                            animations17$useaction = Animations17.UseAction.DRINK;
                            break;
                        case BLOCK:
                            if (itemStack.getItem() instanceof ShieldItem) {
                                return;
                            }

                            animations17$useaction = Animations17.UseAction.BLOCK;
                            break;
                        case BOW:
                            animations17$useaction = Animations17.UseAction.BOW;
                            break;
                        case NONE:
                            animations17$useaction = Animations17.UseAction.NONE;
                            break;
                        default:
                            return;
                    }

                    i = state.useItemRemainingTicks;
                } else if (animationsmodule.isFakeSwordBlocking(minecraft.player)) {
                    animations17$useaction = Animations17.UseAction.BLOCK;
                    i = 1;
                }

                poseStack.pushPose();
                Animations17.applyFirstPersonTransform(
                    poseStack,
                    partialTicks,
                    attack,
                    1.0F - inverseArmHeight,
                    i,
                    state.mainHandUseDuration,
                    animations17$useaction,
                    itemStack,
                    animationsmodule.blockStyle.getValue()
                );
                ItemStackRenderState itemstackrenderstate = this.adapt$noneRenderState;
                itemstackrenderstate.clear();
                minecraft.getItemModelResolver()
                    .updateForTopItem(
                        itemstackrenderstate, itemStack, ItemDisplayContext.NONE, minecraft.player.level(), minecraft.player, minecraft.player.getId()
                    );
                boolean flag = !(itemStack.getItem() instanceof BlockItem) || !itemstackrenderstate.usesBlockLight();
                Animations17.applyFlatItemTransform(poseStack, flag);
                itemstackrenderstate.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
                ci.cancel();
            }
        }
    }
}
