package heig.tb.jsmithfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SmithController {

    @FXML
    public Canvas smithCanvas;


    @FXML
    private Label welcomeText;

    @FXML
    public void initialize() {
        // Initialization code can go here
        GraphicsContext gc = smithCanvas.getGraphicsContext2D();

        // Draw a simple Smith chart grid (placeholder)
        gc.strokeOval(10, 10, smithCanvas.getWidth() - 20, smithCanvas.getHeight() - 20);
        gc.strokeLine(smithCanvas.getWidth() / 2, 10, smithCanvas.getWidth() / 2, smithCanvas.getHeight() - 10);
        gc.strokeLine(10, smithCanvas.getHeight() / 2, smithCanvas.getWidth() - 10, smithCanvas.getHeight() / 2);
    }


    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @FXML
    private void onExitButtonClick(ActionEvent event) {
        // A more robust way to get the stage and close it
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onMinimizeButtonClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

}
