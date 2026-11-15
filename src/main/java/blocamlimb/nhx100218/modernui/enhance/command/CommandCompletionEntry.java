package blocamlimb.nhx100218.modernui.enhance.command;

/**
 * One row in the modern command-completion popup.
 *
 * <p>Prepared for the 26.3-style chat completion; not wired into GuiChat yet.</p>
 */
public final class CommandCompletionEntry {

    public enum Kind {
        COMMAND,
        SUBCOMMAND,
        ARGUMENT,
        PLAYER,
        LITERAL
    }

    public final String text;
    public final String description;
    public final Kind kind;
    public final int score;

    public CommandCompletionEntry(String text, String description, Kind kind, int score) {
        this.text = text == null ? "" : text;
        this.description = description == null ? "" : description;
        this.kind = kind == null ? Kind.LITERAL : kind;
        this.score = score;
    }
}
