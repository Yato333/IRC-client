package core.controller;

import core.ConnectionHandler;
import core.lines.ChannelInfoTable;
import core.lines.ErrorLine;
import core.lines.TextLine;
import core.lines.WarningLine;
import core.records.Channel;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;

/**
 * Controller for the {@link javafx.scene.Parent Parent} which will belong to the {@link javafx.scene.Scene Scene}
 * when the connection to the server is established
 */
public final class MainController extends Controller {
    private static final String HELP_MSG = """
        Available commands:
        /help - print this message
        /quit - close the application
        /list - list available channels
        /join <CHANNEL NAME> - join a channel by name
        /m <DESTINATION> <MESSAGE> - send a message to destination
        <MESSAGE> - send a message to a channel, if you have joined one
        """;
    private static MainController instance = null;
    private static final HashMap<String, Runnable> CLIENT_COMMANDS = new HashMap<>();
    static {
        CLIENT_COMMANDS.put("help", () -> instance.messageBox.getChildren().add(new TextLine(HELP_MSG)));
        CLIENT_COMMANDS.put("quit", ConnectionHandler::disconnect);
    }
    
    private final ChannelInfoTable channelTable = new ChannelInfoTable();
    
    @FXML private ScrollPane scrollPane;
    @FXML private VBox messageBox;
    
    @FXML private TextField textField;
    
    @FXML @Override
    protected void initialize() {
        instance = this;
        messageBox.maxWidthProperty().bind(scrollPane.widthProperty());
        textField.setOnAction(e -> onSendButton());
    }

    public static MainController getInstance() {
        return instance;
    }

    public static void print(@NotNull TextLine textLine) {
        Platform.runLater(() -> instance.messageBox.getChildren().add(textLine));
    }
    
    public static void printChannels(@NotNull Collection<Channel> channels) {
        Platform.runLater(() -> {
            instance.channelTable.setChannels(channels);
            instance.messageBox.getChildren().remove(instance.channelTable);
            instance.messageBox.getChildren().add(instance.channelTable);
        });
    }
    
    public ReadOnlyDoubleProperty scrollPaneWidthProperty() {
        return scrollPane.widthProperty();
    }

    @Override
    public void reset() {
        textField.clear();
        messageBox.getChildren().clear();
    }

    @FXML
    private void onSendButton() {
        final String text = textField.getText().strip();
        textField.clear();
        if (text.isEmpty())
            return;
        if(text.charAt(0) == '/') {
            final String[] cmd = text.substring(1).split(" ");
            if(cmd.length == 0) {
                Platform.runLater(() -> {
                    messageBox.getChildren().add(new ErrorLine(MessageFormat.format("Command {0} is not recognized!", text)));
                    messageBox.getChildren().add(new TextLine(HELP_MSG));
                });
                return;
            }

            Runnable action = CLIENT_COMMANDS.get(cmd[0]);
            if(action != null) {
                Platform.runLater(action);
                return;
            }
            if(!ConnectionHandler.sendCommand(cmd)) {
                Platform.runLater(() -> {
                    messageBox.getChildren().add(new ErrorLine(MessageFormat.format("Command {0} is not recognized!", text)));
                    messageBox.getChildren().add(new TextLine(HELP_MSG));
                });
            }
            return;
        }
        if(! ConnectionHandler.sendMessage(text)) {
            messageBox.getChildren().add(new WarningLine("""
            You haven't joined any channel yet.
            Enter /join <CHANNEL_NAME> to join a channel.
            Enter /m <USER_NAME or #CHANNEL_NAME> to send a direct message.
            Enter /channels to see all available channels.
            """));
        }
    }

}
