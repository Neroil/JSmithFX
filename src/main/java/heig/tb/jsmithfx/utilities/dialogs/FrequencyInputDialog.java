package heig.tb.jsmithfx.utilities.dialogs;

import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.utilities.DialogUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.Locale;

public class FrequencyInputDialog extends Dialog<Double> {

    public FrequencyInputDialog(String title, double currentFreqHz) {
        setTitle(title);
        setHeaderText("Enter frequency value and select unit.");

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Fields
        TextField valueField = new TextField();
        ToggleGroup unitGroup = new ToggleGroup();
        FrequencyUnit selectedUnit = FrequencyUnit.bestFit(currentFreqHz);
        HBox unitBox = new HBox(10);

        for (FrequencyUnit unit : FrequencyUnit.values()) {
            RadioButton rb = new RadioButton(unit.toString());
            rb.setUserData(unit);
            rb.setToggleGroup(unitGroup);
            if (unit == selectedUnit) {
                rb.setSelected(true);
            }
            unitBox.getChildren().add(rb);
        }

        valueField.setText(String.format(Locale.US, "%s", currentFreqHz / selectedUnit.getFactor()));

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Frequency:"), 0, 0);
        grid.add(valueField, 1, 0);
        grid.add(new Label("Unit:"), 0, 1);
        grid.add(unitBox, 1, 1);

        getDialogPane().setContent(grid);
        Platform.runLater(valueField::requestFocus);

        setResultConverter(btn -> {
            if (btn == okButton) {
                try {
                    double val = Double.parseDouble(valueField.getText());
                    FrequencyUnit selectedUnitInDialog = (FrequencyUnit) unitGroup.getSelectedToggle().getUserData();
                    return val * selectedUnitInDialog.getFactor();
                } catch (Exception e) {
                    Stage stage = (Stage) getDialogPane().getScene().getWindow();
                    DialogUtils.showErrorAlert("Invalid Input", "Please enter a valid numeric value.", stage);
                }
            }
            return null;
        });
    }
}