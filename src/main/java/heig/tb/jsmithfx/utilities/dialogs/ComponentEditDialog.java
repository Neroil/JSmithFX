package heig.tb.jsmithfx.utilities.dialogs;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.*;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;

public class ComponentEditDialog extends Dialog<Double> {

    private final TextField valueField;
    private final ComboBox<Enum<?>> unitBox;

    public ComponentEditDialog(CircuitElement element) {
        setTitle("Edit Component");
        setHeaderText("Edit " + element + " value:");
        setGraphic(null);

        // Buttons
        ButtonType okButton = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Initialize Fields
        valueField = new TextField();
        unitBox = new ComboBox<>();

        // Populate Units based on Element Type
        Enum<?>[] values = switch (element.getType()) {
            case RESISTOR  -> ResistanceUnit.values();
            case CAPACITOR -> CapacitanceUnit.values();
            case INDUCTOR  -> InductanceUnit.values();
            case LINE      -> DistanceUnit.values();
        };

        unitBox.getItems().addAll(values);

        // Format current value
        ElectronicUnit[] elValues = (ElectronicUnit[]) values;
        double actualValue = element.getRealWorldValue();
        Pair<ElectronicUnit, String> result = SmithUtilities.getBestUnitAndFormattedValue(actualValue, elValues);

        unitBox.setValue((Enum<?>) result.getKey());
        valueField.setText(result.getValue());

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Value:"), 0, 0);
        grid.add(valueField, 1, 0);
        grid.add(unitBox, 2, 0);

        getDialogPane().setContent(grid);
        Platform.runLater(valueField::requestFocus);

        // Converter
        setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                try {
                    double val = Double.parseDouble(valueField.getText());
                    double factor = 1.0;
                    if (unitBox.getValue() instanceof ElectronicUnit u) {
                        factor = u.getFactor();
                    }
                    return val * factor;
                } catch (NumberFormatException e) {
                    Stage stage = (Stage) getDialogPane().getScene().getWindow();
                    DialogUtils.showErrorAlert("Invalid Input", "Please enter a valid numeric value.",stage);
                }
            }
            return null;
        });
    }
}