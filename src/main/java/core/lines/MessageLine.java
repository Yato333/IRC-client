package core.lines;

import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class MessageLine extends TextLine {
    protected final Label senderLabel;

    public MessageLine(@NotNull String sender, @NotNull String message) {
        super(message);
        senderLabel = new Label(MessageFormat.format("<{0}>", sender));
        senderLabel.setStyle("-fx-text-fill: darkgreen");
        getChildren().add(0, senderLabel);
        setHgrow(senderLabel, Priority.NEVER);
    }
}
