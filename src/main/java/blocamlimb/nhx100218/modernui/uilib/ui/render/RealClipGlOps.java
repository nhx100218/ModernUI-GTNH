package blocamlimb.nhx100218.modernui.uilib.ui.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;

/**
 * 生产用 {@link ClipGlOps}，直接转发 LWJGL {@link GL11}。
 */
final class RealClipGlOps implements ClipGlOps {

    static final RealClipGlOps INSTANCE = new RealClipGlOps();

    /**
     * 布尔查询缓冲。
     *
     * <p>LWJGL2 的 {@code glGetBoolean(int, ByteBuffer)} 经 BufferChecks 恒定校验
     * {@code remaining >= 16}（issue #70），故固定分配 16 字节；复制只取 target 长度个元素。</p>
     */
    private static final ThreadLocal<ByteBuffer> BOOLEAN_QUERY_BUFFER = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        }
    };

    private RealClipGlOps() {
    }

    @Override
    public boolean isEnabled(int cap) {
        return GL11.glIsEnabled(cap);
    }

    @Override
    public void enable(int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void disable(int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public void getIntegers(int pname, IntBuffer params) {
        GL11.glGetInteger(pname, params);
    }

    @Override
    public int getInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    @Override
    public void getBooleans(int pname, boolean[] target) {
        ByteBuffer buffer = BOOLEAN_QUERY_BUFFER.get();
        buffer.clear();
        GL11.glGetBoolean(pname, buffer);
        for (int index = 0; index < target.length; index++) {
            target[index] = buffer.get(index) != 0;
        }
    }

    @Override
    public void scissor(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
    }

    @Override
    public void stencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    @Override
    public void stencilOp(int fail, int zfail, int zpass) {
        GL11.glStencilOp(fail, zfail, zpass);
    }

    @Override
    public void stencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    @Override
    public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void depthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }
}
