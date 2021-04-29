package core.lines;

import core.controller.MainController;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class TextLine extends HBox {
    protected final Label messageLabel;

    /**
     * Creates an empty label, used to skip a line
     */
    public TextLine() {
        messageLabel = new Label();
    }

    /**
     * Creates a node with a text in it
     */
    public TextLine(@NotNull String text) {
        super(10);
        messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        getChildren().add(messageLabel);
        setHgrow(messageLabel, Priority.SOMETIMES);
        VBox.setVgrow(this, Priority.SOMETIMES);
        maxWidthProperty().bind(MainController.getInstance().scrollPaneWidthProperty());
    }

    public final String getText() {
        return messageLabel.getText();
    }
    
    public final StringProperty textProperty() {
        return messageLabel.textProperty();
    }

    public final void setText(@NotNull String text) {
        messageLabel.setText(text);
    }
}


