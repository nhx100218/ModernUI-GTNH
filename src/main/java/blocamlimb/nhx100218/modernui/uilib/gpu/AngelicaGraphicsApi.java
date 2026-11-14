package blocamlimb.nhx100218.modernui.uilib.gpu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Angelica 渲染后端桥：读取/写入 Angelica 的 {@code renderBackend} 与 {@code sdlGpuDriver}。
 *
 * <h3>为什么是「桥」而不是自己实现后端</h3>
 * <p>图形 API 的切换发生在 Angelica 的 SDL-GPU 后端内部（SDL3 GPU + 一层模拟 OpenGL 4.6 Core 的
 * GLSM 状态机）。ModernUI 作为 GL 消费方没有任何 swapchain，无法自行决定 Vulkan / Metal / D3D12；
 * 它能做的是把玩家的选择准确写进 Angelica 的配置，并如实报告当前实际后端。</p>
 *
 * <h3>契约</h3>
 * <ul>
 *   <li><b>只写两个字段</b>：{@code AngelicaConfig.renderBackend}（OPENGL / SDL_GPU）与
 *       {@code AngelicaConfig.sdlGpuDriver}（AUTO / VULKAN / METAL / D3D12），随后调用 GTNHLib
 *       {@code ConfigurationManager.save(AngelicaConfig.class)} 让 Angelica 自己的配置框架落盘——
 *       本类不直接改 {@code config/angelica-modules.cfg} 的字节。</li>
 *   <li><b>只经反射</b>：Angelica 是 compileOnly 依赖且版本会漂移，直接引用其类型会把二进制耦合
 *       钉死；类/字段缺失一律降级为「不可切换」，绝不抛到调用方。</li>
 *   <li><b>只在显式保存时写</b>：{@link #apply(GraphicsApi)} 由配置页保存事件触发，加载/启动不写，
 *       避免 ModernUI 的旧意图覆盖玩家后来在 Angelica 自带设置页做的选择。</li>
 *   <li><b>写入即需重启</b>：Angelica 给这两个字段标了 {@code @Config.RequiresMcRestart}，
 *       本类不尝试热切换。</li>
 * </ul>
 */
public final class AngelicaGraphicsApi {

    private static final Logger LOG = LogManager.getLogger("ModernUI/GraphicsApi");

    /** Angelica 后端选择所在的配置类。 */
    private static final String CONFIG_CLASS = "com.gtnewhorizons.angelica.config.AngelicaConfig";
    /** GTNHLib 配置框架入口（Angelica 自身即用它落盘）。 */
    private static final String CONFIG_MANAGER_CLASS = "com.gtnewhorizon.gtnhlib.config.ConfigurationManager";
    /** 后端类别字段：OPENGL / SDL_GPU。 */
    private static final String BACKEND_FIELD = "renderBackend";
    /** SDL-GPU 驱动字段：AUTO / VULKAN / METAL / D3D12。 */
    private static final String DRIVER_FIELD = "sdlGpuDriver";
    /** JVM 覆盖开关；存在时优先于配置文件。 */
    private static final String SDL_GPU_ENABLE_PROPERTY = "angelica.sdlgpu.enable";
    /** JVM 驱动覆盖。 */
    private static final String SDL_GPU_DRIVER_PROPERTY = "angelica.sdlgpu.driver";

    /** 写入结果，供调用方决定是否提示玩家。 */
    public enum Result {
        /** 已写入并落盘，需重启生效。 */
        APPLIED,
        /** Angelica 当前已处于该状态，未做任何写入。 */
        ALREADY_CURRENT,
        /** 选择为 {@link GraphicsApi#FOLLOW}，按契约不接管。 */
        SKIPPED,
        /** 未安装 Angelica。 */
        NO_ANGELICA,
        /** Angelica 存在但版本过旧，没有 SDL-GPU 配置字段。 */
        VERSION_UNSUPPORTED,
        /** 目标 API 在当前平台不可用（D3D12 非 Windows / Metal 非 macOS）。 */
        OS_UNSUPPORTED,
        /** 反射写入或落盘失败。 */
        FAILED
    }

    /** 解析结果：命中即为可读写，未命中为 null。 */
    private static volatile boolean resolveAttempted;
    private static volatile Class<?> configClass;
    private static volatile Field backendField;
    private static volatile Field driverField;

    private AngelicaGraphicsApi() {
    }

    /**
     * Angelica 是否在运行时中存在（能找到配置类即可）。
     *
     * @return 存在返回 true
     */
    public static boolean isPresent() {
        resolve();
        return configClass != null;
    }

    /**
     * 该 Angelica 版本是否提供 SDL-GPU 配置字段（{@code renderBackend} + {@code sdlGpuDriver}）。
     *
     * <p>两者缺一即视为不可切换：老版本只认 {@code -Dangelica.sdlgpu.enable} JVM 参数，
     * 没有可写的配置面。</p>
     *
     * @return 可切换返回 true
     */
    public static boolean canSwitch() {
        resolve();
        return configClass != null && backendField != null && driverField != null;
    }

    /**
     * 读取当前实际后端（尽力而为，只用于展示与日志）。
     *
     * <p>顺序：JVM 覆盖属性 → Angelica 配置文件 → 读不到返回 null。JVM 属性优先是因为
     * Angelica 自身也是这么覆盖配置的，页面显示必须与实际生效值一致。</p>
     *
     * @return 当前后端；Angelica 缺席或读不出时返回 null
     */
    public static GraphicsApi currentOrNull() {
        GraphicsApi fromProperty = currentFromProperties();
        if (fromProperty != null) {
            return fromProperty;
        }
        resolve();
        if (configClass == null || backendField == null || driverField == null) {
            return null;
        }
        try {
            Object backend = backendField.get(null);
            Object driver = driverField.get(null);
            return mapBackend(enumName(backend), enumName(driver));
        } catch (Throwable t) {
            LOG.debug("读取 Angelica 渲染后端失败", t);
            return null;
        }
    }

    /**
     * 把玩家选择写入 Angelica 配置并落盘。
     *
     * <p>任何失败都只返回枚举并记一条日志，不抛异常：配置页保存流程不应因宿主 mod 缺席而中断。</p>
     *
     * @param api 目标 API，null 视为 {@link GraphicsApi#FOLLOW}
     * @return 写入结果
     */
    public static Result apply(GraphicsApi api) {
        GraphicsApi target = api == null ? GraphicsApi.FOLLOW : api;
        if (target == GraphicsApi.FOLLOW) {
            return Result.SKIPPED;
        }
        GraphicsApiSupport.Os os = GraphicsApiSupport.currentOs();
        if (!GraphicsApiSupport.supportedOn(os, target)) {
            LOG.warn("未写入 Angelica 渲染后端：{}", GraphicsApiSupport.unsupportedReason(os, target));
            return Result.OS_UNSUPPORTED;
        }
        if (!isPresent()) {
            LOG.info("未安装 Angelica，跳过渲染后端写入（目标 {}）", target.id());
            return Result.NO_ANGELICA;
        }
        if (!canSwitch()) {
            LOG.info("当前 Angelica 未提供 SDL-GPU 配置字段（需要含 renderBackend/sdlGpuDriver 的版本），"
                    + "跳过渲染后端写入（目标 {}）；可改用 -Dangelica.sdlgpu.enable=true 启动", target.id());
            return Result.VERSION_UNSUPPORTED;
        }
        if (currentOrNull() == target) {
            return Result.ALREADY_CURRENT;
        }
        try {
            setEnumField(backendField, target == GraphicsApi.OPENGL ? "OPENGL" : "SDL_GPU");
            String driverName = target.driverName();
            if (driverName != null) {
                setEnumField(driverField, driverName);
            }
            saveConfig();
            LOG.info("已写入 Angelica 渲染后端 = {}（重启游戏后生效）", target.id());
            return Result.APPLIED;
        } catch (Throwable t) {
            LOG.warn("写入 Angelica 渲染后端失败（目标 {}）", target.id(), t);
            return Result.FAILED;
        }
    }

    /**
     * 诊断用描述：当前后端 id（{@code opengl} / {@code vulkan} / …）；读不到返回 {@code unknown}。
     *
     * <p>只用于日志与现场排障，不参与任何渲染分支——ModernUI 不按后端改渲染路径，
     * 差异由 Angelica 的 GLSM 层吸收。</p>
     *
     * @return 后端 id 或 {@code unknown}
     */
    public static String describeCurrent() {
        GraphicsApi current = currentOrNull();
        return current == null ? "unknown" : current.id();
    }

    // ---------- 内部 ----------

    private static void resolve() {
        if (resolveAttempted) {
            return;
        }
        synchronized (AngelicaGraphicsApi.class) {
            if (resolveAttempted) {
                return;
            }
            Class<?> resolvedClass = null;
            Field resolvedBackend = null;
            Field resolvedDriver = null;
            try {
                resolvedClass = Class.forName(CONFIG_CLASS, false, AngelicaGraphicsApi.class.getClassLoader());
                resolvedBackend = fieldOrNull(resolvedClass, BACKEND_FIELD);
                resolvedDriver = fieldOrNull(resolvedClass, DRIVER_FIELD);
            } catch (ClassNotFoundException e) {
                LOG.debug("未找到 Angelica 配置类（未安装 Angelica，正常组合）");
            } catch (Throwable t) {
                LOG.debug("解析 Angelica 配置类失败", t);
            }
            configClass = resolvedClass;
            backendField = resolvedBackend;
            driverField = resolvedDriver;
            resolveAttempted = true;
            if (resolvedClass != null && (resolvedBackend == null || resolvedDriver == null)) {
                LOG.info("Angelica 已安装但无 SDL-GPU 配置字段，渲染后端切换不可用");
            }
        }
    }

    private static Field fieldOrNull(Class<?> owner, String name) {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static GraphicsApi currentFromProperties() {
        String enable = System.getProperty(SDL_GPU_ENABLE_PROPERTY);
        if (enable == null) {
            return null;
        }
        String normalized = enable.trim().toLowerCase(java.util.Locale.ROOT);
        if ("false".equals(normalized)) {
            return GraphicsApi.OPENGL;
        }
        if (!"true".equals(normalized)) {
            return null;
        }
        String driver = System.getProperty(SDL_GPU_DRIVER_PROPERTY);
        GraphicsApi mapped = mapDriverName(driver);
        return mapped == null ? GraphicsApi.FOLLOW : mapped;
    }

    private static String enumName(Object value) {
        return value instanceof Enum ? ((Enum<?>) value).name() : null;
    }

    /**
     * Angelica 的（后端, 驱动）→ ModernUI 的目标 API。
     *
     * <p>{@code SDL_GPU + AUTO} 映射为 {@link GraphicsApi#FOLLOW}：AUTO 的含义是「让 Angelica 按平台挑」
     * （Windows/Linux 默认 Vulkan，macOS 默认 Metal），把它显示成某一个具体 API 会误导玩家。</p>
     */
    static GraphicsApi mapBackend(String backendName, String driverName) {
        if (backendName == null) {
            return null;
        }
        if ("OPENGL".equals(backendName)) {
            return GraphicsApi.OPENGL;
        }
        if (!"SDL_GPU".equals(backendName)) {
            return null;
        }
        GraphicsApi mapped = mapDriverName(driverName);
        return mapped == null ? GraphicsApi.FOLLOW : mapped;
    }

    static GraphicsApi mapDriverName(String driverName) {
        if (driverName == null) {
            return null;
        }
        String name = driverName.trim().toUpperCase(java.util.Locale.ROOT);
        if ("VULKAN".equals(name)) {
            return GraphicsApi.VULKAN;
        }
        if ("METAL".equals(name)) {
            return GraphicsApi.METAL;
        }
        if ("D3D12".equals(name) || "DIRECT3D12".equals(name)) {
            return GraphicsApi.DIRECT3D12;
        }
        return null;
    }

    private static void setEnumField(Field field, String constantName) {
        Class<?> enumType = field.getType();
        if (!enumType.isEnum()) {
            throw new IllegalStateException(field.getName() + " 不是枚举字段");
        }
        try {
            field.set(null, constantOf(enumType, constantName));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法写入 " + field.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object constantOf(Class<?> enumType, String constantName) {
        return Enum.valueOf((Class<? extends Enum>) enumType, constantName);
    }

    private static void saveConfig() throws Exception {
        Class<?> managerClass = Class.forName(CONFIG_MANAGER_CLASS, true, AngelicaGraphicsApi.class.getClassLoader());
        Method save = managerClass.getMethod("save", Class[].class);
        save.invoke(null, (Object) new Class<?>[]{configClass});
    }
}
