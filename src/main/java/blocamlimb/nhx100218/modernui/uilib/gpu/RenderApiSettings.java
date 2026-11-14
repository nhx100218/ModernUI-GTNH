package blocamlimb.nhx100218.modernui.uilib.gpu;

/**
 * 图形 API 选择的运行态镜像（由 {@code modernui.yaml} 的 {@code renderer.api} 回灌）。
 *
 * <p>只存玩家的<b>意图</b>，不代表当前实际后端——实际后端以
 * {@link AngelicaGraphicsApi#currentOrNull()} 为准。两者分开是有意的：意图没被 Angelica 接受
 * （未装 Angelica、版本过旧、平台不支持）时，配置页仍要如实显示「你选了 X，但当前跑在 Y」。</p>
 */
public final class RenderApiSettings {

    /** 玩家选择的图形 API，默认 {@link GraphicsApi#FOLLOW}（不接管 Angelica）。 */
    public static volatile GraphicsApi api = GraphicsApi.FOLLOW;

    private RenderApiSettings() {
    }
}
