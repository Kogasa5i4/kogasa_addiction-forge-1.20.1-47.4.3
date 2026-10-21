package net.kogasachan.kogasa_addiction.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kogasachan.kogasa_addiction.entity.projectile.SakuyaKnifeEntity;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SakuyaKnifeRenderer extends EntityRenderer<SakuyaKnifeEntity> {
    private final ItemRenderer itemRenderer;

    public SakuyaKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0;
        this.shadowStrength = 0;
    }

    @Override
    public void render(SakuyaKnifeEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        ItemStack stack = new ItemStack(ModItems.SAKUYA_KNIFE.get());
        CompoundTag tag = entity.getPickupItem().getTag();
        if (tag != null) {
            if (!tag.isEmpty()) {
                stack.setTag(tag.copy());
            }
        }

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));
        poseStack.translate(-0.1D, 0, -0.1D);

        this.itemRenderer.renderStatic(
                stack, ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId()
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SakuyaKnifeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");// 返回一个任意有效路径即可
    }
}