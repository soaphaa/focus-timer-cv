package org.example.focustimercv;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.io.IOException;

//Titlescreen controller
public class TitleController {
    @FXML
    private void handleStartButton(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml")); // main fxml file
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    // ---------- GUI METHODS ------------
    @FXML
    private void onOvalButtonHover(MouseEvent event) {
        Node button = (Node) event.getSource();
        button.setScaleX(0.95); //button will shrink a bit when hovered over to indicate button active.
        button.setScaleY(0.95);
    }

    @FXML
    private void onOvalButtonExit(MouseEvent event) {
        Node button = (Node) event.getSource();
        button.setScaleX(1.0);
        button.setScaleY(1.0);
    }
}
