package net.kogasachan.danmaku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.kogasachan.danmaku.entity.DanmakuBeamEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 激光束渲染：沿射线路径把每一段画成实心柱体（内芯亮白 + 半透明外壳自身颜色），
 * 并随寿命淡出。比细带状更容易看清，也符合"实心柱体"的统一表现。
 */
public class DanmakuBeamRenderer extends EntityRenderer<DanmakuBeamEntity> {

    /** 光束截面边数（越大越圆润） */
    private static final int BEAM_SIDES = 12;

    public DanmakuBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull DanmakuBeamEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        DanmakuType type = entity.danmakuType();
        DanmakuColor color = entity.danmakuColor();

        List<Vec3> points = entity.pathPoints();
        if (points.size() < 2) {
            return;
        }

        // 淡出
        float progress = Math.min(1.0F, (entity.tickCount + partialTick) / (float) DanmakuBeamEntity.MAX_LIFE_TICKS);
        float alpha = Math.max(0.0F, 1.0F - progress);
        if (alpha <= 0.01F) {
            return;
        }

        Vec3 origin = entity.position();
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(DanmakuPrismRenderer.SOLID_TEXTURE));

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i).subtract(origin);
            Vec3 b = points.get(i + 1).subtract(origin);
            Vec3 segment = b.subtract(a);
            double length = segment.length();
            if (length < 1.0E-4D) {
                continue;
            }

            Vec3 axis = segment.scale(1.0D / length);
            Vec3[] basis = DanmakuPrismRenderer.basis(axis);
            Vec3 center = a.add(b).scale(0.5D);

            DanmakuPrismRenderer.prism(consumer, matrix, center, axis, basis[0], basis[1],
                    (float) (length * 0.5D), DanmakuBeamEntity.BEAM_HALF_WIDTH, BEAM_SIDES,
                    color, alpha, false);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DanmakuBeamEntity entity) {
        return DanmakuPrismRenderer.SOLID_TEXTURE;
    }
}
