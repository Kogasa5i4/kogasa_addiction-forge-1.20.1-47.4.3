package net.kogasachan.danmaku.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 弹幕"实心柱体"渲染工具。
 *
 * <p>每个柱体由两层嵌套的棱柱组成：
 * <ul>
 *   <li>内芯：亮白色（混入 25% 自身颜色），近似不透明；</li>
 *   <li>外壳：弹幕自身颜色、半透明。</li>
 * </ul>
 * 先画内芯再画半透明外壳，就能得到"外圈是自身颜色、中间透出亮白"的霓虹柱效果，
 * 比平面贴片在任何视角都更容易辨认。
 *
 * <p>几何直接在世界/实体局部坐标里构建，不依赖相机。
 */
public final class DanmakuPrismRenderer {

    /** 纯白贴图：柱体只用顶点色，不需要形状蒙版 */
    public static final ResourceLocation SOLID_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KogasaDanmaku.MODID, "textures/entity/bullet/solid.png");

    /** 内芯白度（0.75 = 亮白微带自身颜色） */
    private static final float CORE_WHITENESS = 0.75F;
    private static final float CORE_ALPHA = 0.95F;
    private static final float SHELL_ALPHA = 0.55F;

    private DanmakuPrismRenderer() {
    }

    /**
     * 画一根沿 axis 的实心柱体（内芯 + 外壳）。
     *
     * @param consumer   顶点消费者
     * @param matrix     当前 PoseStack 矩阵
     * @param center     柱体中心（实体局部坐标）
     * @param axis       柱体轴向（单位向量）
     * @param up         截面基准轴之一（与 axis 垂直）
     * @param side       截面另一轴（= axis × up）
     * @param halfLength 半长
     * @param radius     外壳半径
     * @param sides      截面边数（8 已足够圆润）
     * @param color      弹幕颜色
     * @param alphaScale 整体透明度倍率（淡出用）
     * @param caps       是否画两端封盖
     */
    public static void prism(VertexConsumer consumer, Matrix4f matrix,
                             Vec3 center, Vec3 axis, Vec3 up, Vec3 side,
                             float halfLength, float radius, int sides,
                             DanmakuColor color, float alphaScale, boolean caps) {
        // 内芯（亮白微带本身颜色）
        float cr = mix(color.red(), 1.0F, CORE_WHITENESS);
        float cg = mix(color.green(), 1.0F, CORE_WHITENESS);
        float cb = mix(color.blue(), 1.0F, CORE_WHITENESS);
        tube(consumer, matrix, center, axis, up, side,
                halfLength * 0.9F, radius * 0.55F, sides, cr, cg, cb, CORE_ALPHA * alphaScale, caps);

        // 外壳（自身颜色、半透明）
        tube(consumer, matrix, center, axis, up, side,
                halfLength, radius, sides,
                color.red(), color.green(), color.blue(), SHELL_ALPHA * alphaScale, caps);
    }

    private static void tube(VertexConsumer consumer, Matrix4f matrix,
                             Vec3 center, Vec3 axis, Vec3 up, Vec3 side,
                             float halfLength, float radius, int sides,
                             float red, float green, float blue, float alpha, boolean caps) {
        Vec3 start = center.subtract(axis.scale(halfLength));
        Vec3 end = center.add(axis.scale(halfLength));

        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2.0D * i / sides;
            double a1 = Math.PI * 2.0D * (i + 1) / sides;
            Vec3 r0 = radial(up, side, a0, radius);
            Vec3 r1 = radial(up, side, a1, radius);

            // 侧面：法线朝外
            quad(consumer, matrix, start.add(r0), start.add(r1), end.add(r1), end.add(r0),
                    red, green, blue, alpha);

            if (caps) {
                // 端盖（退化成三角形使用）：正反两面都画，避免绕序/背面剔除导致端面消失
                quad(consumer, matrix, end, end, end.add(r1), end.add(r0), red, green, blue, alpha);
                quad(consumer, matrix, end, end, end.add(r0), end.add(r1), red, green, blue, alpha);
                quad(consumer, matrix, start, start, start.add(r0), start.add(r1), red, green, blue, alpha);
                quad(consumer, matrix, start, start, start.add(r1), start.add(r0), red, green, blue, alpha);
            }
        }
    }

    private static Vec3 radial(Vec3 up, Vec3 side, double angle, float radius) {
        return up.scale(Math.cos(angle) * radius).add(side.scale(Math.sin(angle) * radius));
    }

    private static float mix(float value, float target, float amount) {
        return value * (1.0F - amount) + target * amount;
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float red, float green, float blue, float alpha) {
        vertex(consumer, matrix, p0, red, green, blue, alpha);
        vertex(consumer, matrix, p1, red, green, blue, alpha);
        vertex(consumer, matrix, p2, red, green, blue, alpha);
        vertex(consumer, matrix, p3, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0x00F000F0)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    /** 计算与 axis 垂直的两个基准轴（up 尽量取世界竖直方向，side = axis × up） */
    public static Vec3[] basis(Vec3 axis) {
        Vec3 reference = Math.abs(axis.y) > 0.98D ? new Vec3(0.0D, 0.0D, 1.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = axis.cross(reference).normalize();
        Vec3 up = side.cross(axis).normalize();
        return new Vec3[]{up, side};
    }
}
