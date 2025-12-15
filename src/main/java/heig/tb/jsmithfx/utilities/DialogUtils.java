package heig.tb.jsmithfx.utilities;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

public class DialogUtils {

    public static void showErrorAlert(String header, String content, Window stage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static Optional<Double> showDoubleInputDialog(String title, String header, double currentVal, Window stage) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentVal));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setGraphic(null);
        dialog.initOwner(stage);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                return Optional.of(Double.parseDouble(result.get()));
            } catch (NumberFormatException e) {
                showErrorAlert("Error", "Invalid number format.", stage);
            }
        }
        return Optional.empty();
    }

    public static Boolean areYouSureDialog(String title, String content, Window stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle(title);

        // Setting header to null and putting text in content usually looks cleaner
        alert.setHeaderText(null);
        alert.setContentText(content);

        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }
}