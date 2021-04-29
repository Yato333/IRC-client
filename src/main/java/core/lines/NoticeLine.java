package core.lines;

import org.jetbrains.annotations.NotNull;

public class NoticeLine extends MessageLine {
    public NoticeLine(@NotNull String sender, @NotNull String message) {
        super(sender, message);
        senderLabel.setStyle("-fx-text-fill: darkred; -fx-font-style: italic");
        messageLabel.setStyle("-fx-text-fill: purple; -fx-font-style: italic");
    }
}
