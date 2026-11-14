package blocamlimb.nhx100218.modernui.uilib.gpu;

import java.util.Locale;

/**
 * 图形 API 的平台可用性判定（纯函数，无 GL / 无 Angelica 依赖）。
 *
 * <p>SDL-GPU 的驱动并非跨平台等价：Direct3D 12 只有 Windows 有，Metal 只有 macOS 有，
 * Vulkan 在 macOS 上必须经 MoltenVK。配置页不按平台裁剪选项（选项集合是 schema 的一部分，
 * 存进 yaml 后跨机器迁移仍要能解析），而是在写入前用本类判定并给出拒绝原因，
 * 避免把 Angelica 配到一个它无法创建的后端上。</p>
 */
public final class GraphicsApiSupport {

    /** 平台族。只区分对驱动可用性有影响的三类，其余归 {@link #OTHER}。 */
    public enum Os {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }

    private GraphicsApiSupport() {
    }

    /**
     * 当前平台族，读 {@code os.name}。
     *
     * @return 平台族，读不到时 {@link Os#OTHER}
     */
    public static Os currentOs() {
        return osOf(System.getProperty("os.name"));
    }

    /**
     * 由 {@code os.name} 字面量判定平台族（拆出来是为了可测）。
     *
     * <p>判定顺序有讲究：{@code Darwin} 里含子串 {@code win}，所以 macOS 必须先于 Windows 判定，
     * 否则 macOS 会被认成 Windows，Metal 被误拒而 D3D12 被误放行。</p>
     *
     * @param osName 平台名，可为 null
     * @return 平台族
     */
    public static Os osOf(String osName) {
        if (osName == null) {
            return Os.OTHER;
        }
        String name = osName.toLowerCase(Locale.ROOT);
        if (name.contains("mac") || name.contains("darwin")) {
            return Os.MACOS;
        }
        if (name.contains("win")) {
            return Os.WINDOWS;
        }
        if (name.contains("linux")) {
            return Os.LINUX;
        }
        return Os.OTHER;
    }

    /**
     * 该 API 能否在当前平台写入。
     *
     * <p>{@link GraphicsApi#FOLLOW} 与 {@link GraphicsApi#OPENGL} 永远可用（不需要 SDL-GPU 驱动）；
     * Vulkan 在所有平台可用，macOS 上由 Angelica 自行尝试 MoltenVK。</p>
     *
     * @param os  平台族
     * @param api 目标 API
     * @return 可用返回 true
     */
    public static boolean supportedOn(Os os, GraphicsApi api) {
        if (api == null || os == null) {
            return false;
        }
        switch (api) {
            case FOLLOW:
            case OPENGL:
            case VULKAN:
                return true;
            case METAL:
                return os == Os.MACOS;
            case DIRECT3D12:
                return os == Os.WINDOWS;
            default:
                return false;
        }
    }

    /**
     * 不可用原因（写日志 / 提示用），可用时返回 null。
     *
     * @param os  平台族
     * @param api 目标 API
     * @return 中文原因，或 null
     */
    public static String unsupportedReason(Os os, GraphicsApi api) {
        if (supportedOn(os, api)) {
            return null;
        }
        if (api == GraphicsApi.METAL) {
            return "Metal 仅 macOS 可用";
        }
        if (api == GraphicsApi.DIRECT3D12) {
            return "Direct3D 12 仅 Windows 可用";
        }
        return "当前平台不支持 " + (api == null ? "null" : api.id());
    }
}
