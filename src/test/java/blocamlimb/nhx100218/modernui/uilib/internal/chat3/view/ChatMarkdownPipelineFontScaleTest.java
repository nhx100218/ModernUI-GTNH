package blocamlimb.nhx100218.modernui.uilib.internal.chat3.view;

import blocamlimb.nhx100218.modernui.uilib.ui.scene.testkit.SceneTestEnvironments;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import blocamlimb.nhx100218.modernui.uilib.font.layout.TextSegment;
import blocamlimb.nhx100218.modernui.uilib.internal.chat3.ChatMarkdownSettings;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.FixedTextMeasurer;
import blocamlimb.nhx100218.modernui.uilib.ui.scene.runtime.SceneRuntime;

/**
 * S5-3（task-21）RC-06 可证伪断言：倍率/字号必须进入 markdown 段流缓存 key 与换行几何。
 *
 * <p>同一 text、同一定行宽，仅基准字号不同 ⇒ L2 视觉行必须重排（断行点变），
 * 且缓存不得把 100% 的结果回给 150%（{@code ChatMarkdownPipeline#layout} 的 key 含 fontSizePx）。</p>
 */
public class ChatMarkdownPipelineFontScaleTest {

    private static final String BODY = buildBody();

    private static String buildBody() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            builder.append("word ");
        }
        return builder.toString();
    }

    /** 换算公式必须与 scene 解析出口同式：clamp(round(设计值 × 倍率))。 */
    @Test
    public void effectiveFontSizeFormulaMatchesSceneResolutionExit() {
        SceneRuntime rt = SceneTestEnvironments.runtime(new FixedTextMeasurer(8, 16));
        try {
            Assert.assertEquals("100%：有效值 = 设计值",
                    ChatMarkdownSettings.getChatFontSizePx(), ChatFontMetrics.chatFontSizePx(rt));
            Assert.assertEquals("100%：行高 = 设计行高",
                    ChatMarkdownSettings.getChatLineHeightPx(), ChatFontMetrics.chatLineHeightPx(rt));

            rt.setFontScale(150);
            Assert.assertEquals("150%：有效字号 = round(设计字号 × 1.5)",
                    Math.round(ChatMarkdownSettings.getChatFontSizePx() * 1.5F),
                    ChatFontMetrics.chatFontSizePx(rt));
            Assert.assertEquals("150%：有效行高 = round(设计行高 × 1.5)",
                    Math.round(ChatMarkdownSettings.getChatLineHeightPx() * 1.5F),
                    ChatFontMetrics.chatLineHeightPx(rt));

            rt.setFontScale(200);
            Assert.assertEquals("200%：有效字号 = 设计字号 × 2",
                    Math.round(ChatMarkdownSettings.getChatFontSizePx() * 2.0F),
                    ChatFontMetrics.chatFontSizePx(rt));
        } finally {
            rt.dispose();
        }
    }

    /** 字号入 key：同参命中同一对象；仅字号变化必须重算并重排（行数变多）。 */
    @Test
    public void fontSizeIsPartOfLayoutCacheKeyAndRewraps() {
        ChatMarkdownPipeline pipeline = new ChatMarkdownPipeline();
        int design = ChatMarkdownSettings.getChatFontSizePx();
        int scaled = Math.round(design * 1.5F);

        List<ChatMarkdownPipeline.RenderedLine> first =
                pipeline.layout(BODY, -1, 200, design, 1.0F, null, null);
        List<ChatMarkdownPipeline.RenderedLine> again =
                pipeline.layout(BODY, -1, 200, design, 1.0F, null, null);
        Assert.assertSame("同参必须命中同一缓存对象（缓存仍生效）", first, again);

        List<ChatMarkdownPipeline.RenderedLine> bigger =
                pipeline.layout(BODY, -1, 200, scaled, 1.0F, null, null);
        Assert.assertNotSame("字号不同不得命中旧缓存（字号/倍率必须入 key）", first, bigger);
        Assert.assertTrue("字号变大必须重排：行数 " + first.size() + " → " + bigger.size(),
                bigger.size() > first.size());
    }

    /**
     * 行内 code 字号必须随倍率换算。
     *
     * <p>它守的是 F36 的 chat3 侧残留：{@code ChatMarkdownPipeline.chatStyleTable()} 此前是静态无参、
     * 恒取 {@code MarkdownStyleTable.defaults()} ⇒ 行内 code 字号停在设计值，倍率 200% 下正文放大到
     * 26（13\u00d72）而 code 仍是 12（出图肉眼可见：code 被挤成小字并折行）。修法是让倍率经 {@code layout} /
     * {@code layoutContent} <b>显式</b>流进样式表，并进两级缓存 key。</p>
     *
     * <p>判据取<b>管道层</b>而不是样式表 getter：钉「倍率真的流到了段流」，而不只是「某个换算函数
     * 算对了」—— 后者在漏传倍率时照样能绿。</p>
     */
    @Test
    public void inlineCodeFontSizeFollowsFontScale() {
        ChatMarkdownPipeline pipeline = new ChatMarkdownPipeline();
        int design = ChatMarkdownSettings.getChatFontSizePx();
        String text = "前 " + (char) 96 + "code" + (char) 96 + " 后";
        int codeAt100 = codeSegmentFontSize(pipeline.layout(text, -1, 400, design, 1.0F, null, null));
        int codeAt200 = codeSegmentFontSize(pipeline.layout(text, -1, 400,
                Math.round(design * 2.0F), 2.0F, null, null));
        Assert.assertTrue("行内 code 字号必须为正（未取到段流样式）", codeAt100 > 0);
        Assert.assertEquals("200%：行内 code 字号 = 100% 的 2 倍（恒为设计值即红）",
                codeAt100 * 2, codeAt200);
    }

    /** 取渲染行里行内 code 段的字号（测试文本固定，按段文本定位）。 */
    private static int codeSegmentFontSize(List<ChatMarkdownPipeline.RenderedLine> lines) {
        for (ChatMarkdownPipeline.RenderedLine line : lines) {
            for (TextSegment segment : line.segments()) {
                if ("code".equals(segment.getText())) {
                    return segment.getStyle().getFontSizePx();
                }
            }
        }
        throw new AssertionError("未找到行内 code 段");
    }
}
