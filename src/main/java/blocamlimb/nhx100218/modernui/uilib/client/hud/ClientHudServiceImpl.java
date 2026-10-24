package blocamlimb.nhx100218.modernui.uilib.client.hud;

import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.ClientHudService;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudAvoidanceProvider;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudRegistration;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudSpec;
import blocamlimb.nhx100218.modernui.uilib.ui.hud.api.HudWindowFactory;

/** Minecraft 客户端 HUD 服务实现。 */
public final class ClientHudServiceImpl extends ClientHudService {
    /** 违约站点标识：线程契约判定统一走 {@link HudClientThread}（headless 降级与告警去重同源）。 */
    private static final String THREAD_SITE = "HUD register/close";
    private static final ClientHudServiceImpl INSTANCE = new ClientHudServiceImpl();
    private final HudRegistry registry = new HudRegistry();
    private ClientHudServiceImpl() {}
    /** 返回客户端实现单例。 */
    public static ClientHudServiceImpl getInstance() { return INSTANCE; }

    @Override public HudRegistration register(HudSpec spec, HudWindowFactory factory) {
        HudClientThread.assertCurrent(THREAD_SITE); return threadChecked(registry.register(spec, factory));
    }
    @Override public HudRegistration registerAvoidance(String id, HudAvoidanceProvider provider) {
        HudClientThread.assertCurrent(THREAD_SITE); return threadChecked(registry.registerAvoidance(id, provider));
    }
    /** 返回内部注册表供唯一 Forge bridge 消费。 */
    HudRegistry registry() { return registry; }
    private static HudRegistration threadChecked(HudRegistration delegate) {
        return new HudRegistration() {
            @Override public void close() {
                HudClientThread.assertCurrent(THREAD_SITE);
                delegate.close();
            }
            @Override public boolean isClosed() { return delegate.isClosed(); }
        };
    }
}
