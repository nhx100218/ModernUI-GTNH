package blocamlimb.nhx100218.modernui.uilib.font.page;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import blocamlimb.nhx100218.modernui.uilib.util.GlStateDiagnostics;

/**
 * LWJGL 实现的 {@link GlApi}（渲染主线程专用，包内单例）。
 */
final class LwjglGlApi implements GlApi {

    static final LwjglGlApi INSTANCE = new LwjglGlApi();

    private LwjglGlApi() {}

    // N13 插桩：字符页的 attrib 帧全部经这里，失败点与 mask 直接进日志（不改控制流）。
    @Override
    public void pushAttrib(int mask) {
        GlStateDiagnostics.pushAttrib(mask, "GlyphPage");
    }

    @Override
    public void pushClientAttrib(int mask) {
        GlStateDiagnostics.pushClientAttrib(mask, "GlyphPage");
    }

    @Override
    public void popClientAttrib() {
        GlStateDiagnostics.popClientAttrib("GlyphPage");
    }

    @Override
    public void popAttrib() {
        GlStateDiagnostics.popAttrib("GlyphPage");
    }

    @Override
    public int genTexture() {
        return GL11.glGenTextures();
    }

    @Override
    public void bindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
    }

    @Override
    public void pixelStore(int parameter, int value) {
        GL11.glPixelStorei(parameter, value);
    }

    @Override
    public void texImage2D(int target, int level, int internalFormat, int width, int height, int border,
            int format, int type, ByteBuffer pixels) {
        GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }

    @Override
    public void texParameter(int target, int parameter, int value) {
        GL11.glTexParameteri(target, parameter, value);
    }

    @Override
    public void texSubImage2D(int target, int level, int x, int y, int width, int height, int format, int type,
            ByteBuffer pixels) {
        GL11.glTexSubImage2D(target, level, x, y, width, height, format, type, pixels);
    }

    @Override
    public void generateMipmap(int target) {
        GL30.glGenerateMipmap(target);
    }

    @Override
    public boolean isTexture(int texture) {
        return GL11.glIsTexture(texture);
    }

    @Override
    public void deleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
    }

    @Override
    public int getError() {
        return GL11.glGetError();
    }
}
