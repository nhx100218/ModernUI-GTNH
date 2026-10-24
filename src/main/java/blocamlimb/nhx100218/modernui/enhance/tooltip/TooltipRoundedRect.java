package blocamlimb.nhx100218.modernui.enhance.tooltip;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import blocamlimb.nhx100218.modernui.enhance.ui.render.TooltipGradientRenderer;

/**
 * 通用圆角渐变矩形绘制：优先用 SDF 着色器（抗锯齿），不可用时回退 CPU 光栅。
 *
 * <p>供 tooltip 与 Waila 等宿主叠层复用。填充与边框均为四角颜色（左上/右上/右下/左下），
 * 单色即四个角传同一值。</p>
 */
public final class TooltipRoundedRect {

    private TooltipRoundedRect() {}

    public static void draw(double left, double top, double right, double bottom, double radius,
            float borderWidth, int f0, int f1, int f2, int f3, int c0, int c1, int c2, int c3,
            double z) {
        if (!(left < right) || !(top < bottom)) {
            return;
        }
        int prevProgram = TooltipShaderProgram.currentProgram();
        TooltipGradientRenderer.begin();
        try {
            TooltipShaderProgram shader = TooltipShaderProgram.get();
            if (shader.bind()) {
                try {
                    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    float cx = (float) ((left + right) * 0.5);
                    float cy = (float) ((top + bottom) * 0.5);
                    float hw = (float) ((right - left) * 0.5);
                    float hh = (float) ((bottom - top) * 0.5);
                    float margin = 2.0F;
                    shader.setHalfSize(hw, hh);
                    shader.setRadius((float) radius);
                    shader.setBorderWidth(borderWidth);
                    shader.setFillCorners(f0, f1, f2, f3);
                    shader.setCorners(c0, c1, c2, c3);
                    float ex = hw + margin;
                    float ey = hh + margin;
                    GL11.glBegin(GL11.GL_QUADS);
                    try {
                        vertex(cx, cy, z, -ex, -ey);
                        vertex(cx, cy, z, ex, -ey);
                        vertex(cx, cy, z, ex, ey);
                        vertex(cx, cy, z, -ex, ey);
                    } finally {
                        GL11.glEnd();
                    }
                } finally {
                    TooltipShaderProgram.useProgram(prevProgram);
                }
            } else {
                // CPU 回退：先画边框色圆角，再内缩画填充色圆角。
                TooltipGradientRenderer.fillRoundedGradient(left, top, right, bottom, radius,
                        c0, c1, c2, c3, z);
                double innerRadius = Math.max(0.0, radius - borderWidth);
                TooltipGradientRenderer.fillRoundedGradient(left + borderWidth, top + borderWidth,
                        right - borderWidth, bottom - borderWidth, innerRadius, f0, f1, f2, f3, z);
            }
        } finally {
            TooltipGradientRenderer.end();
        }
    }

    private static void vertex(double centerX, double centerY, double z, double localX, double localY) {
        GL13.glMultiTexCoord2f(GL13.GL_TEXTURE0, (float) localX, (float) localY);
        GL11.glVertex3d(centerX + localX, centerY + localY, z);
    }
}
