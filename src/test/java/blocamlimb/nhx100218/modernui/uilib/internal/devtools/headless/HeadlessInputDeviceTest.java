package blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless;

import org.junit.Assert;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneInputFrame;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneKeyAction;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.SceneMouseButton;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.input.ScenePointerAction;

/**
 * headless 输入设备模型与输入脚本的帧划分契约。
 *
 * <p>这些断言替代一次实机验证：输入是「一次动作 → 哪一帧、带什么修饰键」的时序契约，
 * 时序错了 UI 会以极难排查的方式表现（点不中、修饰键丢失、动作被吞）。</p>
 */
public class HeadlessInputDeviceTest {

    /** click 必须跨帧：同一帧内按下并抬起对部分控件不可靠。 */
    @Test
    public void clickSpansTwoFrames() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(source.device(), "move 30 40; frame; click");

        SceneInputFrame moved = source.drainFrame();
        Assert.assertEquals(1, moved.getPointerEvents().size());
        Assert.assertEquals(ScenePointerAction.MOVE, moved.getPointerEvents().get(0).getAction());
        Assert.assertEquals(30, moved.getPointerX());
        Assert.assertEquals(40, moved.getPointerY());

        SceneInputFrame pressed = source.drainFrame();
        Assert.assertEquals(ScenePointerAction.BUTTON_DOWN, pressed.getPointerEvents().get(0).getAction());

        SceneInputFrame released = source.drainFrame();
        Assert.assertEquals(ScenePointerAction.BUTTON_UP, released.getPointerEvents().get(0).getAction());
        Assert.assertEquals(SceneMouseButton.LEFT, released.getPointerEvents().get(0).getButton());
    }

    /** 按住修饰键后，后续事件必须携带该修饰（否则 Ctrl/Shift 组合静默失效）。 */
    @Test
    public void modifiersFollowHeldKeys() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(source.device(), "keydown SHIFT_LEFT; frame; move 10 10");

        SceneInputFrame pressed = source.drainFrame();
        Assert.assertEquals(SceneKeyAction.PRESSED, pressed.getKeyEvents().get(0).getAction());

        SceneInputFrame moved = source.drainFrame();
        Assert.assertTrue("按住 Shift 之后的事件必须带 Shift 修饰", moved.isShiftDown());
    }

    /**
     * {@code wait N} 在<b>语句位置</b>等待，后续语句落到 N 帧之后。
     *
     * <p>用「wait 之后还有动作」的脚本钉住位置语义 —— 若把 wait 放在末尾，两种实现恰好等价，
     * 测试就失去区分力（旧实现是「等整个队列耗尽后再空转」，独立复核被它静默误导过：
     * {@code …; wait 10; click} 里的 click 不会等，连续点击被退场动画吞掉）。</p>
     */
    @Test
    public void waitHoldsItsStatementPosition() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(source.device(), "move 5 5; wait 2; move 9 9");

        Assert.assertEquals("wait 之前的动作在第一帧下发",
                1, source.drainFrame().getPointerEvents().size());
        // 关键断言：wait 让后续语句顺延。旧实现（等队列耗尽才空转）会把两个 move 挤进相邻两帧，
        // 即第二帧就能看到 move 9 9 —— 此处为空即证明 wait 生效在语句位置。
        Assert.assertTrue("wait 期间空转，后续语句不得提前下发", source.drainFrame().isEmpty());
        Assert.assertEquals("wait 之后的动作晚 2 帧下发",
                1, source.drainFrame().getPointerEvents().size());
    }

    /**
     * 指针事件的修饰位必须与按住的键一致 —— Ctrl / Shift 不得互换。
     *
     * <p>它守的是一处真实缺陷（独立审核 2026-09-18 发现）：{@code press} / {@code release} /
     * {@code scroll} / {@code cancelPointer} 四条路径调 {@code RawInputEvent.ofPointer} 时把
     * {@code (controlDown, shiftDown)} 写成了 {@code (shiftDown, controlDown)}，而 {@code moveTo} 与
     * {@code ofKey} 的顺序是对的 —— 于是「按住 Ctrl 点一下」在事件里变成「按住 Shift」，依赖修饰键的
     * 控件（{@code ChatInputSurface} 的 Shift 横滚、将来任何 Ctrl 多选）静默走错分支。
     * 既有 {@link #modifiersFollowHeldKeys()} 只覆盖 MOVE，故此前全绿。</p>
     */
    @Test
    public void pointerModifiersFollowHeldKeysWithoutSwapping() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(source.device(),
                "move 10 10; frame; keydown CONTROL_LEFT; frame; move 20 20; frame; down LEFT;"
                        + " frame; scroll 3; frame; up LEFT; frame; cancel");

        SceneInputFrame idleMove = source.drainFrame();
        Assert.assertFalse("未按键时的 MOVE 不得带修饰",
                idleMove.isControlDown() || idleMove.isShiftDown());
        Assert.assertEquals(SceneKeyAction.PRESSED, source.drainFrame().getKeyEvents().get(0).getAction());
        // 按住 Ctrl 后的 MOVE 也必须带 Ctrl —— 独立审核实测：这条此前无判据（moveTo 丢掉 Ctrl 不会变红）。
        assertModifiers(source.drainFrame(), "move(按住 Ctrl)", ScenePointerAction.MOVE, true);
        assertModifiers(source.drainFrame(), "down", ScenePointerAction.BUTTON_DOWN, true);
        assertModifiers(source.drainFrame(), "scroll", ScenePointerAction.SCROLL, true);
        assertModifiers(source.drainFrame(), "up", ScenePointerAction.BUTTON_UP, true);
        assertModifiers(source.drainFrame(), "cancel", ScenePointerAction.CANCEL, true);

        // 第二段：按住 Alt —— Alt 位必须保留、Meta 不得被误置。
        // 只按住 Ctrl 的段落覆盖不到这条：未按 Alt/Meta 时两者同为 false，换位不会改变读数
        // （独立审核的 alt/meta 换位变异因此存活），必须真的按住才看得见。
        HeadlessInputSource altSource = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(altSource.device(),
                "move 30 30; frame; keydown ALT_LEFT; frame; move 40 40");
        altSource.drainFrame();
        altSource.drainFrame();
        SceneInputFrame altMove = altSource.drainFrame();
        Assert.assertFalse("按下 Alt 后的 MOVE 必须有指针事件", altMove.getPointerEvents().isEmpty());
        Assert.assertTrue("Alt 修饰必须保留", altMove.isAltDown());
        Assert.assertFalse("Meta 不得被误置（alt/meta 换位回归）", altMove.isMetaDown());
        Assert.assertFalse("Ctrl 不得被误置", altMove.isControlDown());
        Assert.assertFalse("Shift 不得被误置", altMove.isShiftDown());
    }

    /**
     * 该帧必须：有指针事件、动作类型正确、带 Ctrl，且 Shift / Alt / Meta 都不得被误置。
     *
     * <p>Alt / Meta 的断言是独立审核指出的盲区：把五处 {@code ofPointer} 的 alt/meta 换位时，
     * 此前**没有任何判据变红**。</p>
     */
    private static void assertModifiers(SceneInputFrame frame, String what, ScenePointerAction expected,
            boolean ctrl) {
        Assert.assertFalse(what + "：该帧必须有指针事件", frame.getPointerEvents().isEmpty());
        Assert.assertEquals(what + "：指针动作类型", expected, frame.getPointerEvents().get(0).getAction());
        Assert.assertEquals(what + "：Ctrl 修饰", ctrl, frame.isControlDown());
        Assert.assertFalse(what + "：Shift 不得被误置", frame.isShiftDown());
        Assert.assertFalse(what + "：Alt 不得被误置", frame.isAltDown());
        Assert.assertFalse(what + "：Meta 不得被误置", frame.isMetaDown());
    }

    /**
     * 释放修饰键（{@code keyup}）后，后续事件不得再带该修饰。
     *
     * <p>独立审核指出的覆盖缺口：{@code keyup} 在 {@code src/test} 全树**零命中**，而使用指南明确承诺
     * 「{@code keydown} 按住、{@code keyup} 释放」。它守的是与 F46 同族的「修饰状态维护」——
     * 按下有判据、释放没有，等于只钉了一半。</p>
     */
    @Test
    public void releasingModifierClearsItFromLaterEvents() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(source.device(),
                "keydown CONTROL_LEFT; frame; keyup CONTROL_LEFT; frame; move 10 10");

        SceneInputFrame pressed = source.drainFrame();
        Assert.assertEquals(SceneKeyAction.PRESSED, pressed.getKeyEvents().get(0).getAction());
        SceneInputFrame released = source.drainFrame();
        Assert.assertEquals(SceneKeyAction.RELEASED, released.getKeyEvents().get(0).getAction());
        SceneInputFrame moved = source.drainFrame();
        Assert.assertFalse("keyup 之后的事件不得再带 Ctrl（修饰状态必须清除）", moved.isControlDown());
        Assert.assertFalse("keyup 之后的事件不得再带 Shift", moved.isShiftDown());
    }

    /** 整串文本（外部接管 / IME 语义）在一帧内以单条 TEXT 事件交付。 */
    @Test
    public void composeDeliversWholeTextInOneFrame() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        HeadlessInputScript.apply(source.device(), "compose 中文输入");

        SceneInputFrame frame = source.drainFrame();
        Assert.assertEquals(1, frame.getTextEvents().size());
        Assert.assertEquals("中文输入", frame.getTextEvents().get(0).getText());
    }

    /** 脚本语法错误必须显式失败，而不是静默跳过整条语句。 */
    @Test
    public void scriptRejectsUnknownStatement() {
        HeadlessInputSource source = new HeadlessInputSource(200, 100);
        try {
            HeadlessInputScript.apply(source.device(), "teleport 1 2");
            Assert.fail("未知语句必须显式失败");
        } catch (HeadlessFailure failure) {
            Assert.assertTrue(failure.getMessage().contains("teleport"));
        }
    }
}
