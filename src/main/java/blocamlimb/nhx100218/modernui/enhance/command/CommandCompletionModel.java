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

    /** Replaces the popup contents (used by the classic Tab response path). */
    public void setEntries(List<CommandCompletionEntry> next, String currentText) {
        entries.clear();
        selected = 0;
        if (next == null || next.isEmpty()) {
            open = false;
            return;
        }
        entries.addAll(next);
        open = true;
        selectText(currentText);
    }

    /** Highlights the entry that matches the committed input. */
    public void selectText(String currentText) {
        if (currentText == null || entries.isEmpty()) {
            return;
        }
        String folded = currentText.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < entries.size(); i++) {
            String text = entries.get(i).text.toLowerCase(java.util.Locale.ROOT);
            if (folded.endsWith(text) || text.endsWith(folded)) {
                selected = i;
                return;
            }
        }
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

    /** Forces the highlighted row (used by the vanilla Tab cycle). */
    public void selectIndex(int index) {
        if (entries.isEmpty()) {
            selected = 0;
            return;
        }
        selected = index % entries.size();
        if (selected < 0) {
            selected += entries.size();
        }
    }

    public CommandCompletionEntry selected() {
        return isOpen() ? entries.get(selected) : null;
    }
}
