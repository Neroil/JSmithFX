package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Pair;

import java.util.Arrays;
import java.util.Optional;

public class DialogFactory {

    /**
     * Shows a dialog to edit a component's value with a Unit selector.
     */
    public static Optional<Double> showComponentEditDialog(CircuitElement element) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Edit Component");
        dialog.setHeaderText("Edit " + element + " value:");
        dialog.setGraphic(null);

        // Button Types
        ButtonType okButton = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Fields
        TextField valueField = new TextField();
        ComboBox<Enum<?>> unitBox = new ComboBox<>();
        Enum<?>[] values;

        values = switch (element.getType()) {
            case RESISTOR  ->  ResistanceUnit.values();
            case CAPACITOR ->  CapacitanceUnit.values();
            case INDUCTOR  ->  InductanceUnit.values();
        };

        //Select the right unit and display the previous value
        unitBox.getItems().addAll(values);

        ElectronicUnit[] elValues = (ElectronicUnit[]) values;

        double actualValue = element.getRealWorldValue();
        Pair<ElectronicUnit, String> result = SmithUtilities.getBestUnitAndFormattedValue(actualValue, elValues);

        // Set the selected unit and display value
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

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(valueField::requestFocus);

        // Result Converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                try {
                    double val = Double.parseDouble(valueField.getText());
                    double factor = 1.0;
                    // Extract factor from Enum
                    if (unitBox.getValue() instanceof ElectronicUnit u) {
                        factor = u.getFactor();
                    }
                    return val * factor;
                } catch (NumberFormatException e) {
                    showErrorAlert("Invalid Format", "Please enter a valid number.");
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Shows a dialog to edit Complex numbers (Load Impedance).
     */
    public static Optional<Complex> showComplexInputDialog(String title, Complex current) {
        Dialog<Complex> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter Real and Imaginary parts:");
        dialog.setGraphic(null);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        TextField reField = new TextField(String.valueOf(current.real()));
        TextField imField = new TextField(String.valueOf(current.imag()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Real (Ω):"), 0, 0);
        grid.add(reField, 1, 0);
        grid.add(new Label("Imaginary (jΩ):"), 0, 1);
        grid.add(imField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                try {
                    return new Complex(
                            Double.parseDouble(reField.getText()),
                            Double.parseDouble(imField.getText())
                    );
                } catch (NumberFormatException e) {
                    showErrorAlert("Error", "Invalid number format.");
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Shows a simple single value input dialog.
     */
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

    public static void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows a dialog to edit the frequency with common unit prefixes (kHz, MHz, GHz).
     * Automatically selects the most appropriate unit for the current value.
     *
     * @param title The title of the dialog window.
     * @param currentFrequencyHz The current frequency value in base units (Hertz).
     * @return An Optional containing the new frequency in Hertz if the user clicks OK, otherwise empty.
     */
    public static Optional<Double> showFrequencyInputDialog(String title, double currentFrequencyHz) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter the new frequency value and select its unit.");

        // Button Types
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // --- UI Controls ---
        TextField valueField = new TextField();
        ToggleGroup unitGroup = new ToggleGroup();

        // Create radio buttons and store their multipliers in userData
        RadioButton hzButton = new RadioButton("Hz");
        hzButton.setUserData(1.0);
        hzButton.setToggleGroup(unitGroup);

        RadioButton khzButton = new RadioButton("kHz");
        khzButton.setUserData(1e3);
        khzButton.setToggleGroup(unitGroup);

        RadioButton mhzButton = new RadioButton("MHz");
        mhzButton.setUserData(1e6);
        mhzButton.setToggleGroup(unitGroup);

        RadioButton ghzButton = new RadioButton("GHz");
        ghzButton.setUserData(1e9);
        ghzButton.setToggleGroup(unitGroup);

        // --- Pre-population logic ---
        // Determine the best unit to display the current value
        double displayValue;
        if (currentFrequencyHz >= 1e9) {
            displayValue = currentFrequencyHz / 1e9;
            ghzButton.setSelected(true);
        } else if (currentFrequencyHz >= 1e6) {
            displayValue = currentFrequencyHz / 1e6;
            mhzButton.setSelected(true);
        } else if (currentFrequencyHz >= 1e3) {
            displayValue = currentFrequencyHz / 1e3;
            khzButton.setSelected(true);
        } else {
            displayValue = currentFrequencyHz;
            hzButton.setSelected(true);
        }
        // Use a general format specifier to avoid unnecessary trailing zeros
        valueField.setText(String.format("%s", displayValue));

        // --- Layout ---
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Frequency:"), 0, 0);
        grid.add(valueField, 1, 0);

        // Use an HBox for a cleaner button layout
        HBox unitBox = new HBox(10, hzButton, khzButton, mhzButton, ghzButton);
        grid.add(new Label("Unit:"), 0, 1);
        grid.add(unitBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(valueField::requestFocus);

        // --- Result Converter ---
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                try {
                    double value = Double.parseDouble(valueField.getText());
                    // Get the multiplier directly from the selected toggle's user data
                    double multiplier = (double) unitGroup.getSelectedToggle().getUserData();
                    return value * multiplier; // Return final value in Hz
                } catch (NumberFormatException | NullPointerException e) {
                    // This catch handles both bad text and no selected unit
                    return null; // Prevents dialog from closing, user can correct input
                }
            }
            return null; // For cancel or closing the dialog
        });

        return dialog.showAndWait();
    }
}