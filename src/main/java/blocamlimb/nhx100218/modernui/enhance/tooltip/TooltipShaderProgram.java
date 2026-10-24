package blocamlimb.nhx100218.modernui.enhance.tooltip;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import blocamlimb.nhx100218.modernui.enhance.gl.ShaderProgramSupport;

/**
 * tooltip 圆角 SDF 着色器程序（GLSL 120）。立即模式配合 {@code gl_Vertex}/{@code gl_MultiTexCoord0}
 * 提交一个覆盖外缘的四边形，片元按 SDF 求覆盖率，得到平滑抗锯齿圆角与四角渐变边框。
 */
public final class TooltipShaderProgram {

    private static final Logger LOG = LogManager.getLogger("ModernUI/tooltip");

    private static final TooltipShaderProgram INSTANCE = new TooltipShaderProgram();

    private static final String VERT = "shader/tooltipV.vert";
    private static final String FRAG = "shader/tooltipF.frag";

    private boolean initialized;
    private boolean unavailable;
    private int program;

    private int uHalfSize;
    private int uRadius;
    private int uBorderWidth;
    private int uF0;
    private int uF1;
    private int uF2;
    private int uF3;
    private int uC0;
    private int uC1;
    private int uC2;
    private int uC3;

    private TooltipShaderProgram() {}

    public static TooltipShaderProgram get() {
        return INSTANCE;
    }

    /** 惰性初始化并绑定；不可用时返回 false，调用方回退 CPU 渲染。 */
    public boolean bind() {
        if (unavailable) {
            return false;
        }
        if (!initialized) {
            initialize();
        }
        if (program == 0) {
            return false;
        }
        GL20.glUseProgram(program);
        return true;
    }

    public void setHalfSize(float halfWidth, float halfHeight) {
        if (uHalfSize != -1) {
            GL20.glUniform2f(uHalfSize, halfWidth, halfHeight);
        }
    }

    public void setRadius(float radius) {
        if (uRadius != -1) {
            GL20.glUniform1f(uRadius, radius);
        }
    }

    public void setBorderWidth(float width) {
        if (uBorderWidth != -1) {
            GL20.glUniform1f(uBorderWidth, width);
        }
    }

    public void setFillCorners(int c0, int c1, int c2, int c3) {
        setColor(uF0, c0);
        setColor(uF1, c1);
        setColor(uF2, c2);
        setColor(uF3, c3);
    }

    public void setCorners(int c0, int c1, int c2, int c3) {
        setColor(uC0, c0);
        setColor(uC1, c1);
        setColor(uC2, c2);
        setColor(uC3, c3);
    }

    private static void setColor(int location, int argb) {
        if (location == -1) {
            return;
        }
        GL20.glUniform4f(location, ((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
    }

    private void initialize() {
        initialized = true;
        int vs = 0;
        int fs = 0;
        int p = 0;
        try {
            vs = ShaderProgramSupport.compileShader(
                    ShaderProgramSupport.readText(getClass(), VERT, "读取 tooltip 顶点着色器失败: "),
                    GL20.GL_VERTEX_SHADER, "tooltip 顶点着色器编译失败: ");
            fs = ShaderProgramSupport.compileShader(
                    ShaderProgramSupport.readText(getClass(), FRAG, "读取 tooltip 片元着色器失败: "),
                    GL20.GL_FRAGMENT_SHADER, "tooltip 片元着色器编译失败: ");
            p = GL20.glCreateProgram();
            GL20.glAttachShader(p, vs);
            GL20.glAttachShader(p, fs);
            ShaderProgramSupport.linkAndValidateProgram(p, "tooltip 着色器链接失败: ",
                    "tooltip 着色器验证失败: ");
            program = p;
            p = 0;
            uHalfSize = GL20.glGetUniformLocation(program, "uHalfSize");
            uRadius = GL20.glGetUniformLocation(program, "uRadius");
            uBorderWidth = GL20.glGetUniformLocation(program, "uBorderWidth");
            uF0 = GL20.glGetUniformLocation(program, "uF0");
            uF1 = GL20.glGetUniformLocation(program, "uF1");
            uF2 = GL20.glGetUniformLocation(program, "uF2");
            uF3 = GL20.glGetUniformLocation(program, "uF3");
            uC0 = GL20.glGetUniformLocation(program, "uC0");
            uC1 = GL20.glGetUniformLocation(program, "uC1");
            uC2 = GL20.glGetUniformLocation(program, "uC2");
            uC3 = GL20.glGetUniformLocation(program, "uC3");
        } catch (Throwable failure) {
            if (p != 0) {
                GL20.glDeleteProgram(p);
            }
            unavailable = true;
            LOG.warn("tooltip 着色器不可用，回退 CPU 圆角渲染", failure);
        } finally {
            if (vs != 0) {
                GL20.glDeleteShader(vs);
            }
            if (fs != 0) {
                GL20.glDeleteShader(fs);
            }
        }
    }

    /** 是否可用（初始化成功过）。 */
    public boolean isAvailable() {
        if (!initialized) {
            initialize();
        }
        return program != 0;
    }

    public static int currentProgram() {
        return GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    }

    public static void useProgram(int programId) {
        GL20.glUseProgram(programId);
    }
}
