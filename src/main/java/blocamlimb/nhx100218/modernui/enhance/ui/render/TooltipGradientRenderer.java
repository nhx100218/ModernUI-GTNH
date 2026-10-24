package blocamlimb.nhx100218.modernui.enhance.ui.render;

import org.lwjgl.opengl.GL11;

import blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics;


/**
 * 立即模式圆角四角渐变填充器。
 *
 * <p>宿主叠层（tooltip 背景）需要在 Minecraft GUI 的当前正交坐标系里直接绘制一个带四角颜色的
 * 圆角矩形，但 {@link UiRoundedBandRasterizer} 只支持"每侧一色"的带色。本类用逐物理像素行的
 * 水平条带 + 四顶点双线性采样，得到平滑的四角渐变；行高为 1px，三角剖分不会产生对角缝。</p>
 *
 * <p>GL 状态由 {@link #begin()}/{@link #end()} 成对包裹，通过 attrib 栈整体恢复，不污染 MC
 * 与其它 mod 的后续渲染（符合 UILib"replay/backend 必须恢复其触碰的 GL 状态"红线）。</p>
 */
public final class TooltipGradientRenderer {

    private static final String SITE = "TooltipGradientRenderer";
    private static final float INV255 = 1.0F / 255.0F;

    private TooltipGradientRenderer() {}

    /** 压入 attrib 并设置本渲染器所需的固定管线状态。 */
    public static void begin() {
        GlStateDiagnostics.pushAttrib(GL11.GL_ALL_ATTRIB_BITS, "TooltipGradientRenderer");
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glColorMask(true, true, true, true);
    }

    /** 弹出 attrib，恢复调用前状态。 */
    public static void end() {
        GlStateDiagnostics.popAttrib("TooltipGradientRenderer");
    }

    /**
     * 绘制一个四角渐变的圆角矩形。
     *
     * @param left   左边界（logical px）
     * @param top    上边界
     * @param right  右边界
     * @param bottom 下边界
     * @param radius 圆角半径，内部按短边一半夹取
     * @param ul     左上颜色 ARGB
     * @param ur     右上颜色 ARGB
     * @param lr     右下颜色 ARGB
     * @param ll     左下颜色 ARGB
     * @param z      顶点 z（深度测试关闭时仅影响投影裁剪，需落在 GUI 正交范围内）
     */
    public static void fillRoundedGradient(double left, double top, double right, double bottom,
            double radius, int ul, int ur, int lr, int ll, double z) {
        if (!(left < right) || !(top < bottom)) {
            return;
        }
        if (!Double.isFinite(left) || !Double.isFinite(top) || !Double.isFinite(right)
                || !Double.isFinite(bottom)) {
            return;
        }
        radius = Math.max(0.0, Math.min(radius, Math.min((right - left) * 0.5, (bottom - top) * 0.5)));
        int firstRow = (int) Math.floor(top);
        int lastRow = (int) Math.ceil(bottom);
        GL11.glBegin(GL11.GL_QUADS);
        try {
            for (int y = firstRow; y < lastRow; y++) {
                double rowTop = Math.max(top, y);
                double rowBottom = Math.min(bottom, y + 1);
                if (rowTop >= rowBottom) {
                    continue;
                }
                double mid = (rowTop + rowBottom) * 0.5;
                double inset = cornerInset(radius, mid - top, bottom - mid);
                double l = left + inset;
                double r = right - inset;
                if (l >= r) {
                    continue;
                }
                vertex(l, rowTop, z, sample(left, top, right, bottom, ul, ur, lr, ll, l, rowTop));
                vertex(r, rowTop, z, sample(left, top, right, bottom, ul, ur, lr, ll, r, rowTop));
                vertex(r, rowBottom, z, sample(left, top, right, bottom, ul, ur, lr, ll, r, rowBottom));
                vertex(l, rowBottom, z, sample(left, top, right, bottom, ul, ur, lr, ll, l, rowBottom));
            }
        } finally {
            GL11.glEnd();
        }
    }

    /** 用单色绘制一个普通矩形（标题分隔线等）。 */
    public static void fillRect(double left, double top, double right, double bottom, int color, double z) {
        fillRoundedGradient(left, top, right, bottom, 0.0, color, color, color, color, z);
    }

    private static void vertex(double x, double y, double z, int argb) {
        float a = ((argb >>> 24) & 255) * INV255;
        float r = ((argb >>> 16) & 255) * INV255;
        float g = ((argb >>> 8) & 255) * INV255;
        float b = (argb & 255) * INV255;
        GL11.glColor4f(r, g, b, a);
        GL11.glVertex3d(x, y, z);
    }

    /** 圆角在给定纵向距离处的水平内缩量（椭圆圆角近似，与 UiRoundedBandRasterizer 同式）。 */
    private static double cornerInset(double radius, double dTop, double dBottom) {
        if (radius <= 0.0) {
            return 0.0;
        }
        return Math.max(inset(radius, dTop), inset(radius, dBottom));
    }

    private static double inset(double radius, double distance) {
        if (radius <= 0.0 || distance >= radius) {
            return 0.0;
        }
        double dy = radius - Math.max(0.0, distance);
        return radius - Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
    }

    private static int sample(double left, double top, double right, double bottom,
            int ul, int ur, int lr, int ll, double x, double y) {
        double u = (x - left) / (right - left);
        double v = (y - top) / (bottom - top);
        if (u < 0.0) u = 0.0; else if (u > 1.0) u = 1.0;
        if (v < 0.0) v = 0.0; else if (v > 1.0) v = 1.0;
        int a = lerp2(ul >>> 24, ur >>> 24, lr >>> 24, ll >>> 24, u, v);
        int r = lerp2((ul >> 16) & 255, (ur >> 16) & 255, (lr >> 16) & 255, (ll >> 16) & 255, u, v);
        int g = lerp2((ul >> 8) & 255, (ur >> 8) & 255, (lr >> 8) & 255, (ll >> 8) & 255, u, v);
        int b = lerp2(ul & 255, ur & 255, lr & 255, ll & 255, u, v);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp2(int cUL, int cUR, int cLR, int cLL, double u, double v) {
        double top = cUL + (cUR - cUL) * u;
        double bot = cLL + (cLR - cLL) * u;
        return (int) Math.round(top + (bot - top) * v);
    }
}
