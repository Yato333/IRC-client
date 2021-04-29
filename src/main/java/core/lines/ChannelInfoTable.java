package core.lines;

import core.records.Channel;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collection;

public class ChannelInfoTable extends GridPane {
    public ChannelInfoTable() {
        var column0 = new ColumnConstraints(60, 60, 100);
        var column1 = new ColumnConstraints(60, 60, 100);
        var column2 = new ColumnConstraints();
        column2.setHgrow(Priority.SOMETIMES);
        getColumnConstraints().addAll(column0, column1, column2);
        getRowConstraints().addListener((ListChangeListener<? super RowConstraints>) change -> {
            change.getAddedSubList().forEach(row -> row.setVgrow(Priority.SOMETIMES));
        });
    }
    
    public ChannelInfoTable(@NotNull Collection<Channel> channels) {
        this();
        setChannels(channels);
    }
    
    public void setChannels(@NotNull Collection<Channel> channels) {
        if (getChildren().size() > 0) {
            getChildren().clear();
            getRowConstraints().clear();
        }
        int i = 0;
        for (var channel : channels) {
            var nameLabel = new Label(MessageFormat.format("<{0}>", channel.name()));
            nameLabel.setStyle("-fx-text-fill: darkgreen; -fx-wrap-text: true; -fx-text-alignment: center;");
    
            var userLabel = new Label();
            if (channel.users() > 0) {
                userLabel.setText(String.format("[%d users]", channel.users()));
                userLabel.setStyle("-fx-text-fill: coral; -fx-wrap-text: true; -fx-text-alignment: center;");
            }
    
            var descLabel = new Label(channel.desc());
            descLabel.setWrapText(true);
    
            addRow(i++, nameLabel, userLabel, descLabel);
        }
    }
    
}
