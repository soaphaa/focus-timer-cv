package org.example.focustimercv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));

        // Wrap the content in a StackPane for centering
        StackPane centeredRoot = new StackPane(root);
        Scene scene = new Scene(centeredRoot);

        stage.setTitle("Pomodoro Timer");
        stage.setScene(scene);
        stage.setX(20);
        stage.setY(20);
        stage.show();
    }

    public static void main(String[] args) {
        nu.pattern.OpenCV.loadLocally();
        System.out.println(Core.VERSION);

        launch();
    }
}