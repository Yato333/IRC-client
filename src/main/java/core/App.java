package core;

import core.controller.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import static java.util.logging.Logger.getLogger;

public class App extends Application {
    public static final int MAX_NICK_LENGTH = 32;
    public static final int BUFFER_LENGTH = 4096;
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;
    public static final String FXML_PATH = "/fxml/";
    public static final List<String> ROOT_NAMES = List.of("start", "main");
    public static final boolean DEBUG = false;
    public static final Level LOG_LEVEL = DEBUG ? Level.FINEST : Level.INFO;

    private static final BooleanProperty running = new SimpleBooleanProperty(true);
    private static final HashMap<String, Parent> roots = new HashMap<>();
    private static final HashMap<String, Controller> rootControllers = new HashMap<>();
    private static String currentRoot;
    private static Stage primaryStage;

    @Override
    public void init() throws Exception {
        var threadPool = Executors.newCachedThreadPool();
        var logger = getLogger("core.App");
        logger.setLevel(LOG_LEVEL);

        // Loading the roots in multiple threads
        logger.fine("Loading the roots...");
        // Map all the root names to a runnable, and add each one of those to the thread pool
        ROOT_NAMES.stream().<Runnable>map(name -> () -> {
            try {
                var rootPath = FXML_PATH + name + ".fxml";
                var rootURL = getClass().getResource(rootPath);
                roots.put(name, FXMLLoader.load(Objects.requireNonNull(rootURL)));
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }).forEach(threadPool::submit);

        threadPool.shutdown();
        if (!threadPool.awaitTermination(10, TimeUnit.SECONDS))
            throw new TimeoutException("Root loading time out");

        ROOT_NAMES.forEach(name -> {
            String className = Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Controller";
            try {
                var clazz = Class.forName(Controller.class.getPackageName() + "." + className);
                var instance = (Controller) clazz.getMethod("getInstance").invoke(null);
                rootControllers.put(name, instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        int rootCount = ROOT_NAMES.size();
        assert (roots.size() == rootCount && rootControllers.size() == rootCount);

        logger.fine("Loading finished");
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static synchronized boolean isRunning() {
        return running.get();
    }

    public static ReadOnlyBooleanProperty runningProperty() {
        return running;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        App.primaryStage = primaryStage;

        Platform.setImplicitExit(true);
        primaryStage.setTitle("IRC Protocol Demo");
        showRoot("start");
    }

    @Override
    public synchronized void stop() throws Exception {
        super.stop();
        running.set(false);
    }

    public static void showRoot(@NotNull String name) {
        if(!isRunning() || Objects.equals(currentRoot, name))
            return;

        Platform.runLater(() -> {
            try {
                Parent p = Objects.requireNonNull(roots.get(name), "Root: " + name + " isn't in the root map.");
                if(currentRoot != null)
                    rootControllers.get(currentRoot).reset();
                currentRoot = name;

                if(primaryStage.isShowing())
                    primaryStage.hide();

                Scene scene;
                if ((scene = primaryStage.getScene()) != null)
                    scene.setRoot(p);
                else
                    primaryStage.setScene(new Scene(p));

                primaryStage.setHeight(DEFAULT_HEIGHT);
                primaryStage.setWidth(DEFAULT_WIDTH);
                primaryStage.setResizable(!name.equals("start"));
                primaryStage.show();
            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait());
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(App.class, args);
    }
}
