package com.dosboxeditor;

import com.dosboxeditor.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Entry point for the DOSBox Config Editor application.
 */
public class App extends Application {

    public static final String APP_NAME = "DOSBox Conf Editor";

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow(primaryStage);
        Scene scene = new Scene(mainWindow, 900, 650);

        // Apply stylesheet
        String css = getClass().getResource("/com/dosboxeditor/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle(APP_NAME);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(mainWindow::handleCloseRequest);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
