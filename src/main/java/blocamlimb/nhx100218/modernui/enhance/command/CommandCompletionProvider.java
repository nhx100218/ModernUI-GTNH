package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.List;

/** Supplies 26.3-style command suggestions for a chat input. */
public interface CommandCompletionProvider {

    /**
     * @param input  full chat input, including the leading slash
     * @param cursor caret position in the input
     * @param limit  maximum number of suggestions
     * @return ordered suggestions; never null
     */
    List<CommandCompletionEntry> collect(String input, int cursor, int limit);
}
