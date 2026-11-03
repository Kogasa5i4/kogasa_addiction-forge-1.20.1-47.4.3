package net.kogasachan.danmaku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.kogasachan.danmaku.entity.DanmakuBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * 弹幕渲染。
 *
 * <ul>
 *   <li><b>规则形状</b>（{@link DanmakuType#multiAngle()} 为 true）：渲染成沿飞行方向的**实心柱体**
 *       （内芯亮白 + 半透明外壳自身颜色），任意视角都是一个清楚的发光柱/胶囊；</li>
 *   <li><b>平面图案</b>（蝶/心/箭/刀/音符/休止符/札）：仍然用单片贴图，正对飞行方向。</li>
 * </ul>
 * 朝向在发射瞬间由飞行方向确定，不随相机变化。
 */
public class DanmakuBulletRenderer extends EntityRenderer<DanmakuBulletEntity> {

    private static final int FULL_BRIGHT = 0xF000F0;
    /** 细长弹型柱体的截面边数（越大越圆润） */
    private static final int PRISM_SIDES = 12;

    public DanmakuBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull DanmakuBulletEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        DanmakuType type = entity.danmakuType();
        DanmakuColor color = entity.danmakuColor();

        Vec3 dir = entity.direction();
        Vec3 axis = dir.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
        Vec3[] basis = DanmakuPrismRenderer.basis(axis);
        Vec3 up = basis[0];
        Vec3 side = basis[1];

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        if (!type.multiAngle()) {
            // 平面图案：单片贴图
            renderPlaneSprite(bufferSource, poseStack, type, color, axis, up);
        } else if (isRound(type)) {
            // 圆形弹型：正对相机的双层圆盘（外圈自身颜色 + 内芯亮白），保证是正圆
            renderRoundDiscs(bufferSource, poseStack, this.entityRenderDispatcher.cameraOrientation(), type, color);
        } else {
            // 细长弹型：沿飞行方向的实心柱体
            renderPrism(bufferSource, matrix, type, color, axis, up, side);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** 长宽比接近 1 视为圆形弹型（用圆盘渲染，避免多边形棱角） */
    private static boolean isRound(DanmakuType type) {
        float max = Math.max(type.width(), type.height());
        float min = Math.min(type.width(), type.height());
        return max / min <= 1.25F;
    }

    /** 圆形弹型：两层正对相机的圆盘（外圈半透明自身颜色 + 内芯亮白） */
    private static void renderRoundDiscs(MultiBufferSource bufferSource, PoseStack poseStack,
                                         org.joml.Quaternionf cameraRotation,
                                         DanmakuType type, DanmakuColor color) {
        poseStack.pushPose();
        poseStack.mulPose(cameraRotation);

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(DanmakuPrismRenderer.SOLID_TEXTURE));

        float radius = Math.max(0.08F, Math.min(type.width(), type.height()) * 0.5F);

        // 外圈：自身颜色，略微半透明
        disc(consumer, matrix, radius, 0.0F,
                color.red(), color.green(), color.blue(), 0.85F);
        // 内芯：亮白微带自身颜色，往前挪一点避免深度冲突
        float core = 0.8F;
        disc(consumer, matrix, radius * 0.55F, 0.01F,
                mix(color.red(), core), mix(color.green(), core), mix(color.blue(), core), 1.0F);

        poseStack.popPose();
    }

    private static void disc(VertexConsumer consumer, Matrix4f matrix, float radius, float z,
                             float red, float green, float blue, float alpha) {
        final int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            float x0 = (float) (Math.cos(a0) * radius);
            float y0 = (float) (Math.sin(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float y1 = (float) (Math.sin(a1) * radius);

            // 正反两面都画：不依赖绕序，保证圆盘一定可见
            vertex(consumer, matrix, x0, y0, z, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, x1, y1, z, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, 0.0F, 0.0F, z, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, 0.0F, 0.0F, z, 0.0F, 0.0F, red, green, blue, alpha);

            vertex(consumer, matrix, x1, y1, z, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, x0, y0, z, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, 0.0F, 0.0F, z, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, 0.0F, 0.0F, z, 0.0F, 0.0F, red, green, blue, alpha);
        }
    }

    private static float mix(float value, float target) {
        return value * 0.25F + target * 0.75F;
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                               float u, float v, float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** 规则形状：实心柱体（细长弹型沿飞行方向拉长，圆弹型近似胶囊/圆盘） */
    private static void renderPrism(MultiBufferSource bufferSource, Matrix4f matrix, DanmakuType type,
                                    DanmakuColor color, Vec3 axis, Vec3 up, Vec3 side) {
        boolean elongated = isElongated(type);
        float longSize = Math.max(type.width(), type.height());
        float shortSize = Math.min(type.width(), type.height());

        float halfLength = Math.max(0.06F, (elongated ? longSize : shortSize * 0.7F) * 0.5F);
        float radius = Math.max(0.06F, shortSize * 0.5F);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(DanmakuPrismRenderer.SOLID_TEXTURE));
        DanmakuPrismRenderer.prism(consumer, matrix, Vec3.ZERO, axis, up, side,
                halfLength, radius, PRISM_SIDES, color, 1.0F, true);
    }

    /** 平面图案：单片贴图，长轴对齐飞行方向 */
    private static void renderPlaneSprite(MultiBufferSource bufferSource, PoseStack poseStack, DanmakuType type,
                                          DanmakuColor color, Vec3 axis, Vec3 up) {
        // 以飞行方向为局部 +X、上方向为 +Y 建立朝向
        Matrix4f rotation = new Matrix4f().identity();
        rotation.setColumn(0, new org.joml.Vector4f((float) axis.x, (float) axis.y, (float) axis.z, 0.0F));
        rotation.setColumn(1, new org.joml.Vector4f((float) up.x, (float) up.y, (float) up.z, 0.0F));
        rotation.setColumn(2, new org.joml.Vector4f(
                (float) (axis.y * up.z - axis.z * up.y),
                (float) (axis.z * up.x - axis.x * up.z),
                (float) (axis.x * up.y - axis.y * up.x), 0.0F));

        poseStack.pushPose();
        poseStack.mulPose(rotation.getNormalizedRotation(new org.joml.Quaternionf()));
        Matrix4f matrix = poseStack.last().pose();

        boolean verticalArt = type.height() > type.width();
        float alongMotion = Math.max(0.06F, (verticalArt ? type.height() : type.width()) * 0.5F);
        float across = Math.max(0.06F, (verticalArt ? type.width() : type.height()) * 0.5F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(type.texture()));
        if (verticalArt) {
            vertex(consumer, matrix, -alongMotion, -across, 1.0F, 0.0F, color);
            vertex(consumer, matrix, -alongMotion, across, 1.0F, 1.0F, color);
            vertex(consumer, matrix, alongMotion, across, 0.0F, 1.0F, color);
            vertex(consumer, matrix, alongMotion, -across, 0.0F, 0.0F, color);
        } else {
            vertex(consumer, matrix, -alongMotion, -across, 0.0F, 1.0F, color);
            vertex(consumer, matrix, -alongMotion, across, 0.0F, 0.0F, color);
            vertex(consumer, matrix, alongMotion, across, 1.0F, 0.0F, color);
            vertex(consumer, matrix, alongMotion, -across, 1.0F, 1.0F, color);
        }

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y,
                               float u, float v, DanmakuColor color) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(color.red(), color.green(), color.blue(), 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** 长宽比大于 1.6 视为细长弹型 */
    private static boolean isElongated(DanmakuType type) {
        float max = Math.max(type.width(), type.height());
        float min = Math.min(type.width(), type.height());
        return max / min > 1.6F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DanmakuBulletEntity entity) {
        return DanmakuPrismRenderer.SOLID_TEXTURE;
    }
}
