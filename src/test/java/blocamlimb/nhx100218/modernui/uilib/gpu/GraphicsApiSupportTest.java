package blocamlimb.nhx100218.modernui.uilib.gpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * {@link GraphicsApiSupport} 的平台判定。
 *
 * <p>这层判定的意义是「别把 Angelica 配到一个它创建不出设备的后端上」：Direct3D 12 只有 Windows
 * 有，Metal 只有 macOS 有，Vulkan 全平台但有 MoltenVK 前提。判定错了的后果不是报错，而是玩家
 * 重启后黑屏或回退 OpenGL。</p>
 */
public class GraphicsApiSupportTest {

    @Test
    public void osOfRecognisesTheThreeRelevantFamilies() {
        assertEquals(GraphicsApiSupport.Os.WINDOWS, GraphicsApiSupport.osOf("Windows 11"));
        assertEquals(GraphicsApiSupport.Os.WINDOWS, GraphicsApiSupport.osOf("Windows Server 2019"));
        assertEquals(GraphicsApiSupport.Os.MACOS, GraphicsApiSupport.osOf("Mac OS X"));
        assertEquals(GraphicsApiSupport.Os.MACOS, GraphicsApiSupport.osOf("Darwin"));
        assertEquals(GraphicsApiSupport.Os.LINUX, GraphicsApiSupport.osOf("Linux"));
        assertEquals(GraphicsApiSupport.Os.OTHER, GraphicsApiSupport.osOf("FreeBSD"));
        assertEquals(GraphicsApiSupport.Os.OTHER, GraphicsApiSupport.osOf(null));
    }

    @Test
    public void followAndOpenglAreAlwaysSupported() {
        for (GraphicsApiSupport.Os os : GraphicsApiSupport.Os.values()) {
            assertTrue(os.name(), GraphicsApiSupport.supportedOn(os, GraphicsApi.FOLLOW));
            assertTrue(os.name(), GraphicsApiSupport.supportedOn(os, GraphicsApi.OPENGL));
        }
    }

    @Test
    public void vulkanIsSupportedEverywhere() {
        for (GraphicsApiSupport.Os os : GraphicsApiSupport.Os.values()) {
            assertTrue(os.name(), GraphicsApiSupport.supportedOn(os, GraphicsApi.VULKAN));
        }
    }

    @Test
    public void metalIsMacOnly() {
        assertTrue(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.MACOS, GraphicsApi.METAL));
        assertFalse(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.WINDOWS, GraphicsApi.METAL));
        assertFalse(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.LINUX, GraphicsApi.METAL));
        assertFalse(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.OTHER, GraphicsApi.METAL));
    }

    @Test
    public void direct3d12IsWindowsOnly() {
        assertTrue(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.WINDOWS, GraphicsApi.DIRECT3D12));
        assertFalse(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.MACOS, GraphicsApi.DIRECT3D12));
        assertFalse(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.LINUX, GraphicsApi.DIRECT3D12));
    }

    @Test
    public void nullInputsAreRejectedInsteadOfAllowed() {
        assertFalse(GraphicsApiSupport.supportedOn(null, GraphicsApi.VULKAN));
        assertFalse(GraphicsApiSupport.supportedOn(GraphicsApiSupport.Os.WINDOWS, null));
    }

    @Test
    public void unsupportedReasonMatchesSupportMatrix() {
        assertNull(GraphicsApiSupport.unsupportedReason(GraphicsApiSupport.Os.MACOS, GraphicsApi.VULKAN));
        assertNull(GraphicsApiSupport.unsupportedReason(GraphicsApiSupport.Os.WINDOWS, GraphicsApi.DIRECT3D12));
        assertNotNull(GraphicsApiSupport.unsupportedReason(GraphicsApiSupport.Os.MACOS, GraphicsApi.DIRECT3D12));
        assertNotNull(GraphicsApiSupport.unsupportedReason(GraphicsApiSupport.Os.LINUX, GraphicsApi.METAL));
    }
}
