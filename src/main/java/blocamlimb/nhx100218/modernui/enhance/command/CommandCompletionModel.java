package blocamlimb.nhx100218.modernui.enhance.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Selection state for the 26.3-style command-completion popup.
 *
 * <p>Chat integration is intentionally deferred; this model is the shared piece
 * the future GuiChat mixin/composer will drive.</p>
 */
public final class CommandCompletionModel {

    private final List<CommandCompletionEntry> entries = new ArrayList<CommandCompletionEntry>();
    private int selected;
    private boolean open;

    public void update(CommandCompletionProvider provider, String input, int cursor, int limit) {
        entries.clear();
        selected = 0;
        open = false;
        if (provider == null || input == null || !input.startsWith("/")) {
            return;
        }
        List<CommandCompletionEntry> next = provider.collect(input, cursor, limit);
        if (next == null || next.isEmpty()) {
            return;
        }
        entries.addAll(next);
        open = true;
    }

    public void close() {
        open = false;
        entries.clear();
        selected = 0;
    }

    public void move(int delta) {
        if (!open || entries.isEmpty()) {
            return;
        }
        selected = (selected + delta) % entries.size();
        if (selected < 0) {
            selected += entries.size();
        }
    }

    public boolean isOpen() {
        return open && !entries.isEmpty();
    }

    public List<CommandCompletionEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int selectedIndex() {
        return selected;
    }

    public CommandCompletionEntry selected() {
        return isOpen() ? entries.get(selected) : null;
    }
}
