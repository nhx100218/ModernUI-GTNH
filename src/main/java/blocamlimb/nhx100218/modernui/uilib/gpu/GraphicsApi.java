package blocamlimb.nhx100218.modernui.uilib.gpu;

/**
 * ModernUI 配置页可选的目标图形 API。
 *
 * <p>本枚举只描述<b>玩家想让它跑在什么后端</b>，不实现任何后端。真正的切换由 Angelica 的
 * SDL-GPU 后端完成（{@code RenderBackendChoice} + {@code SdlGpuDriver}），本枚举是那套
 * 开关的稳定投影：{@link #FOLLOW} 表示不碰 Angelica 的配置，其余值在玩家保存配置时写入
 * 对应字段（见 {@link AngelicaGraphicsApi#apply(GraphicsApi)}），重启后生效。</p>
 *
 * <p>id 同时是 {@code modernui.yaml} 里的落盘字面量与配置页选项文本，因此<b>不得改动既有 id</b>，
 * 新增项一律追加在末尾。</p>
 */
public enum GraphicsApi {

    /** 不接管：保持 Angelica 自己的选择（默认，保证既有用户零行为变化）。 */
    FOLLOW("follow"),

    /** 原生 OpenGL（Angelica 的 OPENGL 后端，即未启用 SDL-GPU）。 */
    OPENGL("opengl"),

    /** SDL-GPU + Vulkan。macOS 上需要 MoltenVK 才可用。 */
    VULKAN("vulkan"),

    /** SDL-GPU + Metal。仅 macOS。 */
    METAL("metal"),

    /** SDL-GPU + Direct3D 12。仅 Windows。 */
    DIRECT3D12("d3d12");

    private final String id;

    GraphicsApi(String id) {
        this.id = id;
    }

    /**
     * 配置落盘字面量 / 配置页选项文本。
     *
     * @return 小写 id
     */
    public String id() {
        return id;
    }

    /**
     * 是否走 Angelica 的 SDL-GPU 后端（而非 OpenGL）。
     *
     * @return 除 {@link #FOLLOW} 与 {@link #OPENGL} 外均为 true
     */
    public boolean isSdlGpu() {
        return this != FOLLOW && this != OPENGL;
    }

    /**
     * 写入 Angelica {@code SdlGpuDriver} 时用的枚举名；{@link #OPENGL} 不需要驱动，返回 null。
     *
     * @return {@code "VULKAN"} / {@code "METAL"} / {@code "D3D12"}；其余为 null
     */
    public String driverName() {
        switch (this) {
            case VULKAN:
                return "VULKAN";
            case METAL:
                return "METAL";
            case DIRECT3D12:
                return "D3D12";
            default:
                return null;
        }
    }

    /**
     * 按落盘字面量取枚举。
     *
     * <p>未知值、空值、大小写混杂一律回退 {@link #FOLLOW}：配置被手改坏时保持「不接管」，
     * 不把玩家游戏改到未知后端。</p>
     *
     * @param id 落盘字面量，可为 null
     * @return 对应枚举，缺省 {@link #FOLLOW}
     */
    public static GraphicsApi fromId(String id) {
        if (id != null) {
            String normalized = id.trim();
            for (GraphicsApi api : values()) {
                if (api.id.equalsIgnoreCase(normalized)) {
                    return api;
                }
            }
        }
        return FOLLOW;
    }
}
