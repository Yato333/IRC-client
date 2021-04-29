package core.controller;

import javafx.fxml.FXML;

/**
 * A JavaFX {@link javafx.scene.Node Node} FXML controller
 * @implSpec A constructor with no parameters
 * @see javafx.fxml.FXMLLoader
 */
public abstract class Controller {
    @FXML
    protected abstract void initialize();
    public abstract void reset();
}
