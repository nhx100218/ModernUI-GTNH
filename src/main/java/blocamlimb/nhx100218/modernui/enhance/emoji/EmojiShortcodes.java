package blocamlimb.nhx100218.modernui.enhance.emoji;

import java.util.HashMap;
import java.util.Map;

/** {@code :name:} 短代码到 Unicode emoji 码点的映射与替换。字形由 ModernUI 字体回退链（系统 emoji 字体）渲染。 */
public final class EmojiShortcodes {

    private static final Map<String, Integer> MAP = new HashMap<String, Integer>();

    private static void put(String name, int codePoint) {
        MAP.put(name, codePoint);
    }

    static {
        put("smile", 0x1F604);
        put("grin", 0x1F601);
        put("joy", 0x1F602);
        put("rofl", 0x1F923);
        put("wink", 0x1F609);
        put("heart_eyes", 0x1F60D);
        put("sunglasses", 0x1F60E);
        put("thinking", 0x1F914);
        put("neutral_face", 0x1F610);
        put("sweat_smile", 0x1F605);
        put("cry", 0x1F622);
        put("sob", 0x1F62D);
        put("angry", 0x1F620);
        put("rage", 0x1F621);
        put("skull", 0x1F480);
        put("ghost", 0x1F47B);
        put("poop", 0x1F4A9);
        put("eyes", 0x1F440);
        put("heart", 0x2764);
        put("broken_heart", 0x1F494);
        put("thumbsup", 0x1F44D);
        put("+1", 0x1F44D);
        put("thumbsdown", 0x1F44E);
        put("-1", 0x1F44E);
        put("ok_hand", 0x1F44C);
        put("clap", 0x1F44F);
        put("pray", 0x1F64F);
        put("wave", 0x1F44B);
        put("point_right", 0x1F449);
        put("muscle", 0x1F4AA);
        put("fire", 0x1F525);
        put("star", 0x2B50);
        put("sparkles", 0x2728);
        put("zap", 0x26A1);
        put("boom", 0x1F4A5);
        put("warning", 0x26A0);
        put("bulb", 0x1F4A1);
        put("rocket", 0x1F680);
        put("tada", 0x1F389);
        put("100", 0x1F4AF);
        put("check", 0x2705);
        put("x", 0x274C);
        put("heavy_check_mark", 0x2714);
        put("cross", 0x2716);
        put("coffee", 0x2615);
        put("beer", 0x1F37A);
        put("apple", 0x1F34E);
        put("eyes", 0x1F440);
        put("cowboy", 0x1F920);
        put("party", 0x1F973);
    }

    /** 将文本中的已知 {@code :name:} 替换为对应 emoji；未知短代码原样保留。 */
    public static String replace(String text) {
        if (text == null || text.indexOf(':') < 0) {
            return text;
        }
        int n = text.length();
        StringBuilder sb = null;
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c == ':') {
                int end = -1;
                int limit = Math.min(n, i + 34);
                for (int j = i + 1; j < limit; j++) {
                    char d = text.charAt(j);
                    if (d == ':') {
                        end = j;
                        break;
                    }
                    if (d == ' ' || d == '\n' || d == '\u00a7') {
                        break;
                    }
                }
                if (end > i + 1) {
                    String name = text.substring(i + 1, end);
                    Integer cp = MAP.get(name);
                    if (cp != null) {
                        if (sb == null) {
                            sb = new StringBuilder(n);
                            sb.append(text, 0, i);
                        }
                        sb.appendCodePoint(cp.intValue());
                        i = end + 1;
                        continue;
                    }
                }
            }
            if (sb != null) {
                sb.append(c);
            }
            i++;
        }
        return sb == null ? text : sb.toString();
    }

    private EmojiShortcodes() {}
}
