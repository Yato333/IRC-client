package core.controller;

import core.App;
import core.ConnectionHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import util.TextFieldCharSkipper;

import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller for the {@link javafx.scene.Parent Parent} which will belong to the {@link javafx.scene.Scene Scene}
 * when the app is just started
 */
public final class StartController extends Controller {
    private static StartController instance;

    private final AtomicReference<ConnectionHandler> serverListener = new AtomicReference<>(null);

    @FXML private Pane root;

    @FXML private GridPane inputPane;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField nickField;
    @FXML private PasswordField passField;
    @FXML private Label passwordRepeatLabel;
    @FXML private PasswordField passRepeatField;
    @FXML private Button connectButton;

    @FXML private VBox loadingPane;

    @FXML @Override
    protected void initialize() {
        instance = this;
        // TODO: bug with caret
        hostField.setOnKeyTyped(new TextFieldCharSkipper(64, "\\s"));
        portField.setOnKeyTyped(new TextFieldCharSkipper(5, "\\D"));
        passField.setOnKeyTyped(new TextFieldCharSkipper(30));
        passRepeatField.setOnKeyTyped(new TextFieldCharSkipper(30));

        // Ask to repeat the password only if it was specified
        passwordRepeatLabel.disableProperty().bind(passField.textProperty().isEmpty());
        passRepeatField.disableProperty().bind(passField.textProperty().isEmpty());
        // Check if the repeated password matches
        passRepeatField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue == null || newValue.length() == 0)
                return;
            if(newValue.equals(passField.getText()))
                passRepeatField.setStyle("");
            else
                passRepeatField.setStyle("-fx-border-color: red");
        }));

        // Make the connect button pressable only if certain conditions are met
        connectButton.disableProperty().bind(
            hostField.textProperty().isEmpty().or(
            passField.textProperty().isNotEmpty().and(
                    passField.textProperty().isNotEqualTo(passRepeatField.textProperty())
            )
        ));

        // Make the loading pane appear when the input pane is hidden and vice versa
        loadingPane.visibleProperty().bind(inputPane.visibleProperty().not());
        
        if(App.DEBUG) {
            hostField.setText("irc.vub.lt");
            nickField.setText("test");
        }
    }

    public static StartController getInstance() {
        return instance;
    }

    @FXML
    private void onEnter() {
        if(!connectButton.isDisabled())
            connectButton.fire();
    }

    @FXML
    private void onConnectButton() {
        inputPane.setVisible(false);

        // Starting a new thread for a connection attempt
        // If we don't do that, the window will freeze
        new Thread(() -> {
            try {
                // Try to connect
                serverListener.set(new ConnectionHandler(hostField.getText(),
                        portField.getText().isEmpty() ? 6667 : Integer.parseInt(portField.getText()),
                        nickField.getText(),
                        passField.textProperty().getValueSafe()));
                // If success - start the event loop
                serverListener.get().start();
                // Switch to a different scene root
                Platform.runLater(() -> App.showRoot("main"));
            } catch (IllegalArgumentException e) { // Port number is invalid
                e.printStackTrace();
                portField.setStyle("-fx-border-color: red");
            } catch (UnknownHostException e) { // Host is invalid or not found
                e.printStackTrace();
                hostField.setStyle("-fx-border-color: red");
            } catch (Exception e) { // Other error, probably failed to connect
                // Show error dialog
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait());
                e.printStackTrace();
            } finally {
                // Restore the default view if errors were encountered
                Platform.runLater(() -> inputPane.setVisible(true));
            }
        }).start();
    }

    @Override
    public void reset() {
        for (Node node : inputPane.getChildren()) {
            if(node instanceof TextField textField) {
                textField.clear();
                textField.setStyle("");
            }
        }
        portField.setText(String.valueOf(ConnectionHandler.DEFAULT_PORT));
        if(App.DEBUG) {
            hostField.setText(ConnectionHandler.DEBUG_HOST);
            nickField.setText(ConnectionHandler.DEBUG_NICK);
        }
        inputPane.setVisible(true);
    }
}
