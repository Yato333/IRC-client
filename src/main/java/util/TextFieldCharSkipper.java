package util;

import javafx.event.EventHandler;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import org.intellij.lang.annotations.RegExp;

public class TextFieldCharSkipper implements EventHandler<KeyEvent> {
    private final int maxLength;
    private final String forbiddenChars;

    public TextFieldCharSkipper(int maxLength) {
        this(maxLength, null);
    }
    public TextFieldCharSkipper(int maxLength, @RegExp String forbiddenChars) {
        this.maxLength = maxLength;
        this.forbiddenChars = forbiddenChars;
    }

    @Override
    public void handle(KeyEvent event) {
        String characterString = event.getCharacter();
        char c = characterString.charAt(0);
        // if it is a control character or it is undefined, ignore it
        if (Character.isISOControl(c) || characterString.contentEquals(KeyEvent.CHAR_UNDEFINED))
            return;

        var source = (TextInputControl) event.getSource();
        String text = source.getText();

        if(text.length() > maxLength ||
            (forbiddenChars != null && characterString.matches(forbiddenChars)))
        {
            source.deletePreviousChar();
        }
    }
}
