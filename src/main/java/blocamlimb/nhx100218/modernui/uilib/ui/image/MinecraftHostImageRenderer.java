package blocamlimb.nhx100218.modernui.uilib.ui.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

/**
 * 基于 Minecraft 运行时的普通 texture/bitmap 渲染实现。
 *
 * <p><strong>GL 状态自净（N23）</strong>：本实现自己守恒全部 GL 写入——{@code prepareHostImageState} /
 * {@code preparePlainTextureQuadState} / {@code applyImageBlendState} 改写的 FFP 状态
 * （depth test/func/mask、cull、lighting、{@code GL_RESCALE_NORMAL}、colorMask、color、blend）
 * 与宿主纹理绑定，连同动态位图上传，全部 GL 写入路径都跑在自有 {@link GlStateScope} 内
 * （attrib + client attrib 帧，program/VAO/active texture/矩阵模式的显式恢复，异常路径同样恢复）；
 * 零写入的可用性早退判定留在 scope 之外。调用点
 * {@code UiRenderContext.drawUncachedHostImage} 持有的 {@code UiRenderTarget.begin()} attrib 帧
 * 只是外层额外保险，不作为回收依据。</p>
 */
public final class MinecraftHostImageRenderer implements HostImageRenderer {

    private final Map<String, ResourceLocation> dynamicImageTextures = new HashMap<String, ResourceLocation>();
    private final HostTextureResourceChecker textureResourceChecker;
    private final DynamicImageTextureAccess dynamicImageTextureAccess;
    private final GlStateScope glStateScope;

    /**
     * 创建使用 Minecraft 资源管理器检查纹理可用性的宿主图片渲染器。
     */
    public MinecraftHostImageRenderer() {
        this(new MinecraftTextureResourceChecker(), new MinecraftDynamicImageTextureAccess());
    }

    /**
     * 创建注入纹理资源检查器的宿主图片渲染器。
     *
     * @param textureResourceChecker 纹理资源检查器
     */
    MinecraftHostImageRenderer(HostTextureResourceChecker textureResourceChecker) {
        this(textureResourceChecker, new MinecraftDynamicImageTextureAccess());
    }

    /** 创建可注入动态纹理生命周期访问器的测试实例。 */
    MinecraftHostImageRenderer(HostTextureResourceChecker textureResourceChecker,
            DynamicImageTextureAccess dynamicImageTextureAccess) {
        this(textureResourceChecker, dynamicImageTextureAccess, new GlStateScope());
    }

    /** 创建可注入 GL 状态 scope 的测试实例（N23：全部 GL 写入路径在自有 scope 内运行）。 */
    MinecraftHostImageRenderer(HostTextureResourceChecker textureResourceChecker,
            DynamicImageTextureAccess dynamicImageTextureAccess, GlStateScope glStateScope) {
        this.textureResourceChecker = textureResourceChecker == null
                ? new MinecraftTextureResourceChecker()
                : textureResourceChecker;
        this.dynamicImageTextureAccess = dynamicImageTextureAccess == null
                ? new MinecraftDynamicImageTextureAccess()
                : dynamicImageTextureAccess;
        this.glStateScope = Objects.requireNonNull(glStateScope, "glStateScope");
    }

    @Override
    public void render(HostImageSource source, int left, int top, int right, int bottom) {
        if (source == null || right <= left || bottom <= top) {
            return;
        }
        // N23：一次委托的全部 GL 写入都跑在自有 scope 内（动态位图上传与四边形绘制同属写入，
        // 只包后者会漏掉前者）；scope 恢复 attrib/client attrib 栈、program、VAO、active/client-active
        // texture 与矩阵模式，异常路径同样恢复。
        //
        // 前置判定刻意留在 scope 之外：纹理可用性查询会读资源流（在持有 attrib 帧期间做 I/O 不合适），
        // 且零写入出口不该付 push/pop + 数次 getInteger 的代价。
        if (source.getKind() == HostImageSource.Kind.BUFFERED_IMAGE) {
            BufferedImage image = source.getBufferedImage();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return;
            }
            glStateScope.run(() -> renderBufferedImageRegion(source, image, left, top, right, bottom));
            return;
        }
        if (source.getKind() == HostImageSource.Kind.TEXTURE) {
            ResourceLocation texture = source.getTexture();
            if (texture == null || !textureResourceChecker.isTextureAvailable(texture)) {
                return;
            }
            glStateScope.run(() -> renderTextureRegion(texture, source.getRegionU(), source.getRegionV(),
                    source.getRegionWidth(), source.getRegionHeight(), source.getTextureWidth(),
                    source.getTextureHeight(), left, top, right, bottom));
        }
    }

    /** scope 内的 bitmap 路径：先解析/上传动态纹理，再绘制区域。 */
    private void renderBufferedImageRegion(HostImageSource source, BufferedImage image, int left, int top, int right,
            int bottom) {
        ResourceLocation texture = resolveDynamicImageTexture(source);
        if (texture == null) {
            return;
        }
        renderTextureRegion(texture, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight(),
                left, top, right, bottom);
    }

    ResourceLocation resolveDynamicImageTexture(HostImageSource source) {
        String imageKey = source.getImageKey();
        ResourceLocation cachedTexture = dynamicImageTextures.get(imageKey);
        if (cachedTexture != null) {
            return cachedTexture;
        }
        BufferedImage image = source.getBufferedImage();
        if (image == null) {
            return null;
        }
        ResourceLocation texture = dynamicImageTextureAccess.create("modernui_img", image);
        dynamicImageTextures.put(imageKey, texture);
        return texture;
    }

    /**
     * 删除本 renderer 上传的全部动态位图纹理。
     *
     * <p>本方法属资源生命周期路径而非绘制委托，不在自有 {@link GlStateScope} 边界内：{@code glDeleteTextures}
     * 不改 attrib 组状态，但若被删纹理恰好是当前单元绑定，该绑定会被驱动清 0。生产唯一调用链是屏幕关闭边界
     * （{@code McScreenBridge.onGuiClosed → UiRuntimeAdapters.close → OwnedResources.close}）——该边界之后同一
     * GL context 仍继续渲染，所以「宿主状态随 context 消失」并不成立；实际风险为零的原因是本 renderer 的自建
     * 动态纹理只在自有 scope 内被绑定且随即恢复，close 时不可能处于绑定态。若将来把它接到帧中路径，
     * 调用点必须自带帧级围栏（{@link blocamlimb.nhx100218.modernui.uilib.ui.host.UiFrameGlStateFence}）。</p>
     */
    @Override
    public void close() {
        clearDynamicImageTextures();
    }

    private void clearDynamicImageTextures() {
        Throwable firstFailure = null;
        Iterator<Map.Entry<String, ResourceLocation>> iterator = dynamicImageTextures.entrySet().iterator();
        while (iterator.hasNext()) {
            ResourceLocation texture = iterator.next().getValue();
            try {
                dynamicImageTextureAccess.delete(texture);
                iterator.remove();
            } catch (RuntimeException exception) {
                firstFailure = appendFailure(firstFailure, exception);
            } catch (LinkageError error) {
                firstFailure = appendFailure(firstFailure, error);
            } catch (Error error) {
                firstFailure = appendFailure(firstFailure, error);
            }
        }
        if (firstFailure instanceof RuntimeException) {
            throw (RuntimeException) firstFailure;
        }
        if (firstFailure instanceof LinkageError) {
            throw (LinkageError) firstFailure;
        }
        if (firstFailure instanceof Error) {
            throw (Error) firstFailure;
        }
    }

    private static Throwable appendFailure(Throwable firstFailure, Throwable nextFailure) {
        if (firstFailure == null) {
            return nextFailure;
        }
        if (isFatal(nextFailure) && !isFatal(firstFailure)) {
            if (firstFailure != nextFailure) nextFailure.addSuppressed(firstFailure);
            return nextFailure;
        }
        if (firstFailure != nextFailure) firstFailure.addSuppressed(nextFailure);
        return firstFailure;
    }

    private static boolean isFatal(Throwable failure) {
        return failure instanceof Error && !(failure instanceof LinkageError);
    }

    private void renderTextureRegion(ResourceLocation texture, int regionU, int regionV, int regionWidth,
            int regionHeight, int textureWidth, int textureHeight, int left, int top, int right, int bottom) {
        float u0 = (float) regionU / (float) textureWidth;
        float v0 = (float) regionV / (float) textureHeight;
        float u1 = (float) (regionU + regionWidth) / (float) textureWidth;
        float v1 = (float) (regionV + regionHeight) / (float) textureHeight;
        // 绘制体：由 render 的 scope 边界保护（调用点 UiRenderTarget 的 attrib 帧只是外层额外保险）。
        drawTexturedRegionQuad(texture, u0, v0, u1, v1, left, top, right, bottom);
    }

    /** 实际写入 GL 的绘制体；只允许在 {@code render} 的 {@link GlStateScope} 边界内调用。 */
    private static void drawTexturedRegionQuad(ResourceLocation texture, float u0, float v0, float u1, float v1,
            int left, int top, int right, int bottom) {
        prepareHostImageState();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        preparePlainTextureQuadState();
        applyImageBlendState();
        // 架构禁令:不使用原版包装类(Tessellator),直接 GL 立即模式
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, v1);
        GL11.glVertex2f((float) left, (float) bottom);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex2f((float) right, (float) bottom);
        GL11.glTexCoord2f(u1, v0);
        GL11.glVertex2f((float) right, (float) top);
        GL11.glTexCoord2f(u0, v0);
        GL11.glVertex2f((float) left, (float) top);
        GL11.glEnd();
    }

    private static void preparePlainTextureQuadState() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void prepareHostImageState() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glColorMask(true, true, true, true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void applyImageBlendState() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
}

/** 动态 bitmap texture 的最小创建/删除访问面。 */
interface DynamicImageTextureAccess {
    ResourceLocation create(String key, BufferedImage image);
    void delete(ResourceLocation texture);
}

/** Minecraft TextureManager 的动态纹理生命周期访问器。 */
final class MinecraftDynamicImageTextureAccess implements DynamicImageTextureAccess {
    @Override
    public ResourceLocation create(String key, BufferedImage image) {
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            DynamicTexture texture = new DynamicTexture(image);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
            // DynamicTexture 上传默认使用最近邻，HUD 的非整数缩放会把图标曲边采成阶梯。
            // 只配置本 renderer 拥有的完整位图；资源纹理/图集仍由其资源采样设置管理。
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            return Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(key, texture);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }

    @Override
    public void delete(ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().deleteTexture(texture);
    }
}

/**
 * 宿主纹理资源可用性检查。
 */
interface HostTextureResourceChecker {

    /**
     * 判断指定纹理是否可由宿主资源系统解析。
     *
     * @param texture 纹理资源位置
     * @return 纹理是否可用
     */
    boolean isTextureAvailable(ResourceLocation texture);
}

/**
 * 基于 Minecraft 资源管理器的纹理可用性检查。
 */
final class MinecraftTextureResourceChecker implements HostTextureResourceChecker {

    @Override
    public boolean isTextureAvailable(ResourceLocation texture) {
        if (texture == null) {
            return false;
        }
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager == null) {
            return false;
        }
        InputStream stream = null;
        try {
            IResource resource = resourceManager.getResource(texture);
            if (resource == null) {
                return false;
            }
            stream = resource.getInputStream();
            return true;
        } catch (IOException ignored) {
            return false;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
