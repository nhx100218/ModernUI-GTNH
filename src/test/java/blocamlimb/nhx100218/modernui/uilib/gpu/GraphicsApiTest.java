package blocamlimb.nhx100218.modernui.uilib.gpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * {@link GraphicsApi} 的落盘字面量契约。
 *
 * <p>id 同时是 {@code modernui.yaml} 的值、配置页选项文本与 Angelica 驱动名的来源，
 * 三者任一处漂移都会让玩家选中的后端静默变成别的后端，因此这里把映射钉死。</p>
 */
public class GraphicsApiTest {

    @Test
    public void idsAreStableAndUniqueInDeclarationOrder() {
        GraphicsApi[] values = GraphicsApi.values();
        String[] expected = {"follow", "opengl", "vulkan", "metal", "d3d12"};
        assertEquals("枚举项数量变化必须同步更新本断言与 yaml 迁移", expected.length, values.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values[i].id());
        }
    }

    @Test
    public void fromIdRoundTripsEveryConstant() {
        for (GraphicsApi api : GraphicsApi.values()) {
            assertEquals(api, GraphicsApi.fromId(api.id()));
        }
    }

    @Test
    public void fromIdToleratesCaseAndSurroundingWhitespace() {
        assertEquals(GraphicsApi.VULKAN, GraphicsApi.fromId(" VULKAN "));
        assertEquals(GraphicsApi.DIRECT3D12, GraphicsApi.fromId("D3D12"));
        assertEquals(GraphicsApi.METAL, GraphicsApi.fromId("Metal"));
    }

    @Test
    public void fromIdFallsBackToFollowForUnknownOrNull() {
        assertEquals("未知值不得把游戏切到未知后端", GraphicsApi.FOLLOW, GraphicsApi.fromId("opengl3"));
        assertEquals(GraphicsApi.FOLLOW, GraphicsApi.fromId(""));
        assertEquals(GraphicsApi.FOLLOW, GraphicsApi.fromId(null));
    }

    @Test
    public void isSdlGpuExcludesFollowAndOpengl() {
        assertFalse(GraphicsApi.FOLLOW.isSdlGpu());
        assertFalse(GraphicsApi.OPENGL.isSdlGpu());
        assertTrue(GraphicsApi.VULKAN.isSdlGpu());
        assertTrue(GraphicsApi.METAL.isSdlGpu());
        assertTrue(GraphicsApi.DIRECT3D12.isSdlGpu());
    }

    @Test
    public void driverNameMatchesAngelicaSdlGpuDriverConstants() {
        assertNull("OPENGL 后端不写驱动字段", GraphicsApi.OPENGL.driverName());
        assertNull(GraphicsApi.FOLLOW.driverName());
        assertEquals("VULKAN", GraphicsApi.VULKAN.driverName());
        assertEquals("METAL", GraphicsApi.METAL.driverName());
        assertEquals("必须与 Angelica SdlGpuDriver.D3D12 同名", "D3D12", GraphicsApi.DIRECT3D12.driverName());
    }
}
