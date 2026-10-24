package blocamlimb.nhx100218.modernui.uilib.ui.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Minecraft 普通 texture/bitmap renderer 的降级、资源生命周期与 N23 自净边界测试。
 *
 * <p>纯 JVM 测试运行时不含 LWJGL（{@code GL11} 只在编译期可见），绘制体无法真正执行：用例只验证
 * 「GL 写入体跑在自有 {@link GlStateScope} 内」与「失败路径照样逐项恢复入口状态」；真实 context 下的
 * 状态守恒由实机覆盖（审查报告 §六）。</p>
 */
public class MinecraftHostImageRendererTest {

    @Test
    public void plainRendererIsPhysicallySeparateFromItemRenderer() {
        Assert.assertFalse(ItemIconRenderer.class.isAssignableFrom(MinecraftHostImageRenderer.class));
    }

    /**
     * N23：整个 render 入口必须在自有 {@link GlStateScope} 内运行，且失败路径同样逐项恢复入口状态。
     *
     * <p>纯 JVM 运行时没有 LWJGL，绘制体在首个 GL 调用即失败（{@link LinkageError}）——这正好覆盖
     * 「异常路径恢复」。替身的入口值刻意取非默认值并记录恢复调用实际带的参数，因此删掉任何一段恢复
     * （纹理绑定 / program / VAO / client-active texture / 矩阵模式）都会变红。</p>
     */
    @Test
    public void glWritesRunInsideOwnedScopeAndRestoreOnFailurePath() {
        RecordingGlAccess gl = new RecordingGlAccess();
        MinecraftHostImageRenderer renderer = new MinecraftHostImageRenderer(
                new RecordingTextureResourceChecker(true), new RecordingDynamicImageTextureAccess(),
                new GlStateScope(gl));

        assertRenderFailsInsideScope(renderer,
                HostImageSource.texture(new ResourceLocation("test", "plain.png"), 16, 16));

        Assert.assertEquals("GL 写入必须在自有 scope 内", 1, gl.pushAttribCount);
        Assert.assertEquals("失败路径同样必须弹出 attrib 帧", 1, gl.popAttribCount);
        Assert.assertEquals(1, gl.pushClientAttribCount);
        Assert.assertEquals(1, gl.popClientAttribCount);
        Assert.assertEquals("unit0 纹理绑定必须回到入口值", 11, gl.unitTextureBindings[0]);
        Assert.assertEquals("入口 active unit 纹理绑定必须回到入口值", 77, gl.unitTextureBindings[1]);
        Assert.assertEquals("active unit 必须回到入口值", GL13.GL_TEXTURE1, gl.activeTexture);
        Assert.assertEquals("client-active texture 必须回到入口值", GL13.GL_TEXTURE1, gl.lastClientActiveTexture);
        Assert.assertEquals("program 必须回到入口值", 5, gl.lastProgram);
        Assert.assertEquals("VAO 必须回到入口值", 9, gl.lastVertexArray);
        Assert.assertEquals("矩阵模式必须回到入口值", GL11.GL_PROJECTION, gl.lastMatrixMode);
    }

    /**
     * N23：动态位图上传路径（{@code resolveDynamicImageTexture} → TextureManager/DynamicTexture）
     * 也必须在同一 scope 内——只把四边形绘制包进 scope 是漏的。
     */
    @Test
    public void dynamicBitmapUploadAlsoRunsInsideOwnedScope() {
        RecordingGlAccess gl = new RecordingGlAccess();
        RecordingDynamicImageTextureAccess textures = new RecordingDynamicImageTextureAccess();
        textures.observer = gl;
        MinecraftHostImageRenderer renderer = new MinecraftHostImageRenderer(
                new RecordingTextureResourceChecker(true), textures, new GlStateScope(gl));

        assertRenderFailsInsideScope(renderer, HostImageSource.bufferedImage(
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "inside-scope"));

        Assert.assertEquals("动态位图必须真的上传过", 1, textures.created);
        Assert.assertEquals("上传时刻必须已在 scope 内（pushAttrib 已执行、尚未 pop）", 1,
                textures.attribDepthAtCreate);
        Assert.assertEquals(1, gl.pushAttribCount);
        Assert.assertEquals(1, gl.popAttribCount);
    }

    /**
     * 断言一次 render 在 scope 内失败。
     *
     * <p>纯 JVM 运行时没有 LWJGL（{@link LinkageError}）；若换了具备 LWJGL 但仍无宿主单例的环境，则停在
     * TextureManager 读取（{@link NullPointerException}）——两种都发生在 scope 边界内。render 意外成功时
     * {@code fail} 抛 {@link AssertionError}，不会被本方法吞掉（fail-loud）。</p>
     */
    private static void assertRenderFailsInsideScope(MinecraftHostImageRenderer renderer, HostImageSource source) {
        try {
            renderer.render(source, 0, 0, 16, 16);
            Assert.fail("纯 JVM 运行时缺少 LWJGL/宿主单例，render 不应成功");
        } catch (LinkageError | NullPointerException expected) {
            // 预期：绘制体在 scope 内失败；状态恢复断言由调用方给出。
        }
    }

    /** 缺失纹理源在绑定前跳过，避免 Minecraft 自动绘制紫黑 missing texture。 */
    @Test
    public void shouldSkipMissingTextureBeforeMinecraftBindsDefaultMissingTexture() {
        RecordingTextureResourceChecker checker = new RecordingTextureResourceChecker(false);
        // 用真实 scope 构造：缺失纹理必须在进入 GL 边界之前就返回（纯 JVM 运行时一进边界即 NoClassDefFoundError，
        // 故这条用例同时钉住「零写入出口不触碰 GL」）。
        MinecraftHostImageRenderer renderer = new MinecraftHostImageRenderer(checker);

        renderer.render(HostImageSource.texture(new ResourceLocation("missing", "nonexistent.png"), 16, 16),
                0, 0, 16, 16);

        Assert.assertEquals(1, checker.checkCount);
        Assert.assertEquals("missing", checker.lastTexture.getResourceDomain());
        Assert.assertEquals("nonexistent.png", checker.lastTexture.getResourcePath());
    }

    @Test
    public void closeDeletesEveryUploadedDynamicBitmapTextureAndClearsCache() {
        RecordingDynamicImageTextureAccess textures = new RecordingDynamicImageTextureAccess();
        MinecraftHostImageRenderer renderer = new MinecraftHostImageRenderer(
                new RecordingTextureResourceChecker(true), textures);
        HostImageSource first = HostImageSource.bufferedImage(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB),
                "first");
        HostImageSource second = HostImageSource.bufferedImage(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB),
                "second");

        ResourceLocation firstTexture = renderer.resolveDynamicImageTexture(first);
        Assert.assertSame(firstTexture, renderer.resolveDynamicImageTexture(first));
        renderer.resolveDynamicImageTexture(second);
        Assert.assertEquals(2, textures.created);

        renderer.close();
        Assert.assertEquals(2, textures.deleted.size());
        renderer.close();
        Assert.assertEquals("close 必须幂等", 2, textures.deleted.size());

        Assert.assertNotSame(firstTexture, renderer.resolveDynamicImageTexture(first));
        Assert.assertEquals("close 后重新使用会重新上传", 3, textures.created);
        renderer.close();
        Assert.assertEquals(3, textures.deleted.size());
    }

    @Test
    public void failedTextureDeletionKeepsOnlyThatTextureForRetry() {
        RecordingDynamicImageTextureAccess textures = new RecordingDynamicImageTextureAccess();
        MinecraftHostImageRenderer renderer = new MinecraftHostImageRenderer(
                new RecordingTextureResourceChecker(true), textures);
        HostImageSource first = HostImageSource.bufferedImage(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB),
                "retry-first");
        HostImageSource second = HostImageSource.bufferedImage(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB),
                "retry-second");
        ResourceLocation firstTexture = renderer.resolveDynamicImageTexture(first);
        ResourceLocation secondTexture = renderer.resolveDynamicImageTexture(second);
        textures.failOnce = firstTexture;

        try {
            renderer.close();
            Assert.fail("expected");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("delete-once", expected.getMessage());
        }
        Assert.assertEquals(1, attemptsFor(textures, firstTexture));
        Assert.assertEquals(1, attemptsFor(textures, secondTexture));
        Assert.assertTrue(textures.deleted.contains(secondTexture));

        renderer.close();

        Assert.assertEquals(2, attemptsFor(textures, firstTexture));
        Assert.assertEquals("已成功删除的纹理不得重复删除", 1, attemptsFor(textures, secondTexture));
        Assert.assertTrue(textures.deleted.contains(firstTexture));
    }

    private static int attemptsFor(RecordingDynamicImageTextureAccess textures, ResourceLocation texture) {
        int count = 0;
        for (ResourceLocation attempted : textures.deleteAttempts) {
            if (texture.equals(attempted)) count++;
        }
        return count;
    }

    private static final class RecordingTextureResourceChecker implements HostTextureResourceChecker {
        private final boolean available;
        private int checkCount;
        private ResourceLocation lastTexture;

        private RecordingTextureResourceChecker(boolean available) {
            this.available = available;
        }

        @Override
        public boolean isTextureAvailable(ResourceLocation texture) {
            checkCount++;
            lastTexture = texture;
            return available;
        }
    }

    /**
     * 状态型 {@code GlAccess} 替身：模拟 scope 会读写的项，并记录恢复调用实际带的参数。
     *
     * <p>入口值刻意取非默认值（PROJECTION / TEXTURE1 / 纹理 11 与 77 / program 5 / VAO 9）。只记
     * 「调用了几次」会让「删掉某段恢复」「恢复写死默认值」这类变异存活（独立审核 B6），故断言取参数值。</p>
     */
    private static final class RecordingGlAccess implements GlStateScope.GlAccess {
        private int pushAttribCount;
        private int popAttribCount;
        private int pushClientAttribCount;
        private int popClientAttribCount;
        private int matrixMode = GL11.GL_PROJECTION;
        private int activeTexture = GL13.GL_TEXTURE1;
        private int clientActiveTexture = GL13.GL_TEXTURE1;
        /** 每个纹理单元各自的 TEXTURE_2D 绑定；下标 = unit - GL_TEXTURE0。 */
        private final int[] unitTextureBindings = { 11, 77 };
        private final int program = 5;
        private final int vertexArray = 9;
        private int lastClientActiveTexture = Integer.MIN_VALUE;
        private int lastProgram = Integer.MIN_VALUE;
        private int lastVertexArray = Integer.MIN_VALUE;
        private int lastMatrixMode = Integer.MIN_VALUE;

        @Override
        public void pushAttrib(int mask) {
            pushAttribCount++;
        }

        @Override
        public void popAttrib() {
            popAttribCount++;
        }

        @Override
        public void pushClientAttrib(int mask) {
            pushClientAttribCount++;
        }

        @Override
        public void popClientAttrib() {
            popClientAttribCount++;
        }

        @Override
        public int getInteger(int name) {
            if (name == GL11.GL_MATRIX_MODE) {
                return matrixMode;
            }
            if (name == GL13.GL_ACTIVE_TEXTURE) {
                return activeTexture;
            }
            if (name == GL13.GL_CLIENT_ACTIVE_TEXTURE) {
                return clientActiveTexture;
            }
            if (name == GL20.GL_CURRENT_PROGRAM) {
                return program;
            }
            if (name == GL30.GL_VERTEX_ARRAY_BINDING) {
                return vertexArray;
            }
            if (name == GL11.GL_TEXTURE_BINDING_2D) {
                return unitTextureBindings[activeTexture - GL13.GL_TEXTURE0];
            }
            return 0;
        }

        @Override
        public void activeTexture(int unit) {
            activeTexture = unit;
        }

        @Override
        public void bindTexture2d(int texture) {
            unitTextureBindings[activeTexture - GL13.GL_TEXTURE0] = texture;
        }

        @Override
        public void clientActiveTexture(int unit) {
            clientActiveTexture = unit;
            lastClientActiveTexture = unit;
        }

        @Override
        public void useProgram(int programId) {
            lastProgram = programId;
        }

        @Override
        public void bindVertexArray(int vertexArrayId) {
            lastVertexArray = vertexArrayId;
        }

        @Override
        public void matrixMode(int mode) {
            matrixMode = mode;
            lastMatrixMode = mode;
        }
    }

    private static final class RecordingDynamicImageTextureAccess implements DynamicImageTextureAccess {
        private int created;
        /** 上传时刻的 scope 深度观察器（pushAttrib - popAttrib）。 */
        private RecordingGlAccess observer;
        private int attribDepthAtCreate = -1;
        private final List<ResourceLocation> deleted = new ArrayList<ResourceLocation>();
        private final List<ResourceLocation> deleteAttempts = new ArrayList<ResourceLocation>();
        private ResourceLocation failOnce;

        @Override
        public ResourceLocation create(String key, BufferedImage image) {
            if (observer != null) {
                attribDepthAtCreate = observer.pushAttribCount - observer.popAttribCount;
            }
            created++;
            return new ResourceLocation("test", key + "/" + created);
        }

        @Override
        public void delete(ResourceLocation texture) {
            deleteAttempts.add(texture);
            if (texture.equals(failOnce)) {
                failOnce = null;
                throw new IllegalStateException("delete-once");
            }
            deleted.add(texture);
        }
    }
}
