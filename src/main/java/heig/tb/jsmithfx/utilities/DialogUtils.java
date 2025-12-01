package heig.tb.jsmithfx.utilities;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

public class DialogUtils {

    public static void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static Optional<Double> showDoubleInputDialog(String title, String header, double currentVal) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentVal));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setGraphic(null);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                return Optional.of(Double.parseDouble(result.get()));
            } catch (NumberFormatException e) {
                showErrorAlert("Error", "Invalid number format.");
            }
        }
        return Optional.empty();
    }
}