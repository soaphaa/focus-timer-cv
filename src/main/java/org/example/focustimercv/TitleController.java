package org.example.focustimercv;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

//Titlescreen controller
public class TitleController {

    @FXML private VBox card1, card2, card3;
    @FXML private Label check1, check2, check3;

    private VBox selectedCard = null;
    private Label selectedCheck = null;
    private String selectedMode = null; // your variable
    private static final String UNSELECTED = "-fx-background-color: white; -fx-background-radius: 12px; -fx-padding: 15 20; -fx-cursor: hand;";
    private static final String SELECTED   = "-fx-background-color: #f0f0f0; -fx-background-radius: 12px; -fx-padding: 15 20; -fx-cursor: hand;";

    @FXML
    public void onSelectMode(MouseEvent event) {
        VBox clicked = (VBox) event.getSource();

        // deselect previous
        if (selectedCard != null) {
            selectedCard.setStyle(UNSELECTED);
            selectedCheck.setOpacity(0);
        }

        // select new
        clicked.setStyle(SELECTED);

        if (clicked == card1) {
            check1.setOpacity(1);
            selectedCheck = check1;
            selectedMode = "10Min";
        } else if (clicked == card2) {
            check2.setOpacity(1);
            selectedCheck = check2;
            selectedMode = "25Min";
        } else if (clicked == card3) {
            check3.setOpacity(1);
            selectedCheck = check3;
            selectedMode = "45Min";
        }

        selectedCard = clicked;
        System.out.println("Selected: " + selectedMode); //ensure the correct one is selected corresponding to options.
    }
    
    @FXML
    private void handleStartButton(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml")); // main fxml file
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Get the SceneController and pass the selected mode
        SceneController sceneController = loader.getController();
        sceneController.setPomodoroMode(selectedMode);

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
