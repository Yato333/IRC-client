module IRC.client.main {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;

    requires static org.jetbrains.annotations;
    requires java.logging;

    opens core.controller to javafx.fxml;
    exports core;
}