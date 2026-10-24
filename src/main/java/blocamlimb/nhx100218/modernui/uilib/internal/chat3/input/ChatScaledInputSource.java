package blocamlimb.nhx100218.modernui.uilib.internal.chat3.input;

import blocamlimb.nhx100218.modernui.uilib.ui.scene.host.lwjgl.LwjglInputSource;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.host.lwjgl.PlatformStateReader;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneInputFrame;

/** 聊天宿主输入边界：采集仍为物理坐标，封帧时与绘制使用同一倍率。 */
final class ChatScaledInputSource extends LwjglInputSource {
    private float scale = 1F;

    ChatScaledInputSource(PlatformStateReader reader) { super(reader); }

    void setScale(float value) { scale = value; }

    @Override
    public int logicalWidth() { return Math.max(1, (int) Math.floor(super.logicalWidth() / scale)); }

    @Override
    public int logicalHeight() { return Math.max(1, (int) Math.floor(super.logicalHeight() / scale)); }

    @Override
    public SceneInputFrame drainFrame() {
        return super.drainFrame().scalePointerCoordinates(1F / scale);
    }
}
