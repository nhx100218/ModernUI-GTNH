package blocamlimb.nhx100218.modernui.uilib.gpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * {@link AngelicaGraphicsApi} 的映射面与 JVM 覆盖优先级。
 *
 * <p>这里只测纯函数与属性读取，不触反射写入：{@link AngelicaGraphicsApi#apply(GraphicsApi)}
 * 会改宿主 mod 的配置文件，属于只能在真机验证的路径，单测不伪造它。</p>
 */
public class AngelicaGraphicsApiTest {

    private static final String ENABLE = "angelica.sdlgpu.enable";
    private static final String DRIVER = "angelica.sdlgpu.driver";

    @Test
    public void mapBackendMapsOpenglBackend() {
        assertEquals(GraphicsApi.OPENGL, AngelicaGraphicsApi.mapBackend("OPENGL", "AUTO"));
    }

    @Test
    public void mapBackendMapsSdlGpuDriver() {
        assertEquals(GraphicsApi.VULKAN, AngelicaGraphicsApi.mapBackend("SDL_GPU", "VULKAN"));
        assertEquals(GraphicsApi.METAL, AngelicaGraphicsApi.mapBackend("SDL_GPU", "METAL"));
        assertEquals(GraphicsApi.DIRECT3D12, AngelicaGraphicsApi.mapBackend("SDL_GPU", "D3D12"));
    }

    @Test
    public void mapBackendTreatsSdlGpuAutoAsFollow() {
        // AUTO 是「让 Angelica 按平台挑」，显示成具体 API 会误导玩家（Windows 上是 Vulkan，macOS 上是 Metal）。
        assertEquals(GraphicsApi.FOLLOW, AngelicaGraphicsApi.mapBackend("SDL_GPU", "AUTO"));
        assertEquals(GraphicsApi.FOLLOW, AngelicaGraphicsApi.mapBackend("SDL_GPU", null));
    }

    @Test
    public void mapBackendReturnsNullForUnknownBackend() {
        assertNull(AngelicaGraphicsApi.mapBackend(null, "VULKAN"));
        assertNull(AngelicaGraphicsApi.mapBackend("GLES", "VULKAN"));
    }

    @Test
    public void mapDriverNameAcceptsAngelicaAndSdlSpellings() {
        assertEquals(GraphicsApi.DIRECT3D12, AngelicaGraphicsApi.mapDriverName("d3d12"));
        assertEquals(GraphicsApi.DIRECT3D12, AngelicaGraphicsApi.mapDriverName("direct3d12"));
        assertNull(AngelicaGraphicsApi.mapDriverName("AUTO"));
        assertNull(AngelicaGraphicsApi.mapDriverName(null));
    }

    @Test
    public void jvmOverrideWinsOverConfigFile() {
        String oldEnable = System.getProperty(ENABLE);
        String oldDriver = System.getProperty(DRIVER);
        try {
            System.setProperty(ENABLE, "false");
            System.clearProperty(DRIVER);
            assertEquals("显式关闭 SDL-GPU 必须显示为 OPENGL",
                    GraphicsApi.OPENGL, AngelicaGraphicsApi.currentOrNull());

            System.setProperty(ENABLE, "true");
            System.setProperty(DRIVER, "vulkan");
            assertEquals(GraphicsApi.VULKAN, AngelicaGraphicsApi.currentOrNull());

            System.setProperty(DRIVER, "metal");
            assertEquals(GraphicsApi.METAL, AngelicaGraphicsApi.currentOrNull());

            System.clearProperty(DRIVER);
            assertEquals("开了 SDL-GPU 但没指定驱动 = 交给 Angelica 自选",
                    GraphicsApi.FOLLOW, AngelicaGraphicsApi.currentOrNull());
        } finally {
            restore(ENABLE, oldEnable);
            restore(DRIVER, oldDriver);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
