package blocamlimb.nhx100218.modernui.uilib.internal.devtools;

import net.minecraftforge.client.ClientCommandHandler;

/**
 * 内部开发工具客户端装配入口。
 *
 * <p>本类随包排除出发布产物，只由 {@code ClientProxy} 在开发环境探测调用；一旦被调用，
 * 注册的就是开发环境完整命令（含 test/glass 场地）与网络自检端点集。</p>
 */
public final class DevToolsClientBootstrap {

    private static final ModernUiClientCommand QZ_UI_LIB_CLIENT_COMMAND = new ModernUiClientCommand();

    private DevToolsClientBootstrap() {}

    /**
     * 注册内部开发工具所需的客户端能力。
     *
     * <p>自检端点的唯一驱动者就是本命令，因此端点注册与命令注册同生共死：一并放在这里，
     * 避免发布产物出现「有命令无端点」或反过来的半装配状态。</p>
     */
    public static void registerClientDevTools() {
        ClientCommandHandler.instance.registerCommand(QZ_UI_LIB_CLIENT_COMMAND);
        NetRuntimeSelfChecks.register();
    }
}
