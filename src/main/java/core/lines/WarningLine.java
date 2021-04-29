package core.lines;

import org.jetbrains.annotations.NotNull;

public class WarningLine extends TextLine {
    public WarningLine(@NotNull String message) {
        super(message);
        messageLabel.setStyle("-fx-text-fill: #aa5b00; -fx-font-style: italic");
    }
}
