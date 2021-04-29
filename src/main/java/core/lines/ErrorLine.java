package core.lines;

public class ErrorLine extends TextLine {
    public ErrorLine(String message) {
        super(message);
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }
}
