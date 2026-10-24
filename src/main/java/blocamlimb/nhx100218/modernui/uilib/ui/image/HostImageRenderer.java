package blocamlimb.nhx100218.modernui.uilib.ui.image;

/**
 * 普通宿主图片渲染委托。
 *
 * <p>该轻量路径只处理 texture 与 bitmap；ItemStack 图标由 {@link ItemIconRenderer} 当帧直绘。
 * 自定义实现属于受信任的窄委托：架构禁令禁用原版包装类（Tessellator 等），绘制走直接 GL
 * 或 UILib 自有管线。</p>
 *
 * <p><strong>实现必须自己守恒状态</strong>：返回前不得遗留 program、VAO/VBO、client array、
 * matrix stack、attrib 组状态或其它可观测的宿主状态漂移，且不得遗留 GL error。判据是
 * 「实现自己能恢复」，不是「外层恰好兜住」——调用点持有的 {@code UiRenderTarget.begin()/end()}
 * attrib 帧与 {@link blocamlimb.nhx100218.modernui.uilib.ui.host.UiFrameGlStateFence} 只是额外保险。参考实现：
 * {@link MinecraftHostImageRenderer} 把整段 GL 写入包在 {@link GlStateScope} 内。</p>
 *
 * <p>两点实测口径（见 {@code docs/历史报告/审查/2026-09-19-GL使用自净审查.md} §一/§六）：
 * attrib 属性组栈不覆盖 program 绑定与矩阵栈内容，实现不能指望它；矩阵栈内容若被压入必须自己弹出。</p>
 */
public interface HostImageRenderer extends AutoCloseable {

    /**
     * 在指定区域渲染宿主图片。
     *
     * @param source 图片源
     * @param left 目标区域左边界
     * @param top 目标区域上边界
     * @param right 目标区域右边界
     * @param bottom 目标区域下边界
     */
    void render(HostImageSource source, int left, int top, int right, int bottom);

    /**
     * 释放 renderer 自身拥有的宿主资源。
     *
     * <p>状态归属与本接口的绘制契约分开：{@code close} 不保证在 {@link GlStateScope} 一类状态边界内执行，
     * 但删纹理可能把「恰好绑定着它」的当前单元绑定清 0（{@code glDeleteTextures} 的驱动语义）。
     * 实现若在 close 里做这种可能改变宿主绑定的删除，必须自己写清调用前提；调用方把它接到帧中路径时必须
     * 自带帧级围栏（{@link blocamlimb.nhx100218.modernui.uilib.ui.host.UiFrameGlStateFence}）。</p>
     */
    @Override
    default void close() {
        // 大多数普通图片 renderer 不持有资源。
    }
}
