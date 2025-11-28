package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.*;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Pair;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public class DialogFactory {

    enum DataType {
        IMPEDANCE,
        ADMITTANCE,
        REFLECTION
    }

    enum DataFormat {
        CARTESIAN,
        POLAR
    }

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
            case LINE      ->  DistanceUnit.values();
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
        dialog.setHeaderText("Enter complex value (Impedance, Admittance or Reflection):");

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        ToggleGroup dataTypeGroup = new ToggleGroup();
        RadioButton impedanceRadio = new RadioButton("Impedance (Z)");
        impedanceRadio.setUserData(DataType.IMPEDANCE);
        RadioButton admittanceRadio = new RadioButton("Admittance (Y)");
        admittanceRadio.setUserData(DataType.ADMITTANCE);
        RadioButton reflectionRadio = new RadioButton("Reflection (Gamma)");
        reflectionRadio.setUserData(DataType.REFLECTION);
        dataTypeGroup.getToggles().addAll(impedanceRadio, admittanceRadio, reflectionRadio);
        impedanceRadio.setSelected(true); // Impedance default

        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton cartesianRadio = new RadioButton("Cartesian (Re + j Im)");
        cartesianRadio.setUserData(DataFormat.CARTESIAN);
        RadioButton polarRadio = new RadioButton("Polar (Mag ∠ Ang)");
        polarRadio.setUserData(DataFormat.POLAR);
        formatGroup.getToggles().addAll(cartesianRadio, polarRadio);
        cartesianRadio.setSelected(true); // Cartesian default

        // Dynamic fields and labels
        TextField field1 = new TextField(String.valueOf(current.real())); // Initialized to real part
        TextField field2 = new TextField(String.valueOf(current.imag())); // Initialized to imaginary part
        Label label1 = new Label("Real (Ω):");
        Label label2 = new Label("Imaginary (jΩ):");
        Label unitLabel = new Label("Ω");

        // Layout (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        // Row 0: Data Type (Z, Y, Gamma)
        HBox dataTypeBox = new HBox(15, impedanceRadio, admittanceRadio, reflectionRadio);
        dataTypeBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("Data Type:"), 0, 0);
        grid.add(dataTypeBox, 1, 0, 2, 1);

        // Row 1: Format (Cartesian/Polar)
        HBox formatBox = new HBox(15, cartesianRadio, polarRadio);
        formatBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("Format:"), 0, 1);
        grid.add(formatBox, 1, 1, 2, 1);

        // Row 2 & 3: Dynamic input fields
        grid.add(label1, 0, 2);
        HBox field1Box = new HBox(5, field1, new Label(unitLabel.getText())); // Field 1 with unit
        field1Box.setAlignment(Pos.CENTER_LEFT);
        grid.add(field1Box, 1, 2);
        grid.add(label2, 0, 3);
        HBox field2Box = new HBox(5, field2, new Label("°")); // Field 2 with '°' for angle or 'jΩ' for imaginary
        field2Box.setAlignment(Pos.CENTER_LEFT);
        grid.add(field2Box, 1, 3);

        // This function updates labels and displayed values
        Runnable updateLabels = () -> {
            DataFormat format = (DataFormat) formatGroup.getSelectedToggle().getUserData();
            DataType type = (DataType) dataTypeGroup.getSelectedToggle().getUserData();
            Complex valueToDisplay = current;

            // If type is Admittance, we must invert the value before display
            if (type == DataType.ADMITTANCE) {
                valueToDisplay = current.inverse();
                unitLabel.setText("mS"); // Siemens (conductance/susceptance)
            } else if (type == DataType.IMPEDANCE) {
                unitLabel.setText("Ω"); // Ohms (resistance/reactance)
            } else { // REFLECTION
                unitLabel.setText(""); // No unit
            }

            // Use US Locale to ensure dots are used as separators, matching Double.parseDouble
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat df = new DecimalFormat("0.###", symbols);

            // Update labels and values in fields
            if (format == DataFormat.CARTESIAN) {
                dialog.setHeaderText("Enter Real and Imaginary parts:");
                label1.setText(type == DataType.IMPEDANCE ? "Real (R):" : type == DataType.ADMITTANCE ? "Real (G):" : "Real:");
                label2.setText(type == DataType.IMPEDANCE ? "Imag. (jX):" : type == DataType.ADMITTANCE ? "Imag. (jB):" : "Imag.:");
                field1.setText(df.format(valueToDisplay.real() * (type == DataType.ADMITTANCE ? 1000 : 1))); // Convert to mS if Admittance
                field2.setText(df.format(valueToDisplay.imag() * (type == DataType.ADMITTANCE ? 1000 : 1)));
                ((Label) field2Box.getChildren().get(1)).setText(type == DataType.IMPEDANCE ? "j" + unitLabel.getText() : unitLabel.getText());
                ((Label) field1Box.getChildren().get(1)).setText(unitLabel.getText());

            } else { // POLAR
                dialog.setHeaderText("Enter Magnitude and Angle:");
                label1.setText("Magnitude (M):");
                label2.setText("Angle (θ):");
                field1.setText(df.format(valueToDisplay.magnitude() * (type == DataType.ADMITTANCE ? 1000 : 1)));
                field2.setText(df.format(Math.toDegrees(valueToDisplay.angle())));
                ((Label) field2Box.getChildren().get(1)).setText("°"); // Angle in degrees
                ((Label) field1Box.getChildren().get(1)).setText(unitLabel.getText());
            }
        };

        // Attach listener to type and format change
        dataTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateLabels.run());
        formatGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateLabels.run());

        // Execute once to initialize UI
        updateLabels.run();

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(field1::requestFocus);

        // Convert result back to Complex
        dialog.setResultConverter(button -> {
            if (button == okButton) {
                try {
                    double val1 = Double.parseDouble(field1.getText());
                    double val2 = Double.parseDouble(field2.getText());

                    Complex result;

                    // Convert fields to a complex number (Z, Y or Gamma)
                    DataFormat format = (DataFormat) formatGroup.getSelectedToggle().getUserData();
                    if (format == DataFormat.CARTESIAN) {
                        result = new Complex(val1, val2);
                    } else { // POLAR (Mag ∠ Ang)
                        double angleRadians = Math.toRadians(val2);
                        result = new Complex(val1 * Math.cos(angleRadians), val1 * Math.sin(angleRadians));
                    }

                    // If selected type was Admittance (Y), invert to get Z.
                    DataType type = (DataType) dataTypeGroup.getSelectedToggle().getUserData();
                    if (type == DataType.ADMITTANCE) {
                        return result.dividedBy(1000.0).inverse(); // Convert mS back to S
                    }

                    return result; // Returns Z or Gamma
                } catch (NumberFormatException e) {
                    showErrorAlert("Format Error", "Please enter valid numbers.");
                } catch (NullPointerException e) {
                    showErrorAlert("Error", "Please select a format and data type.");
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
        // Use a general format specifier to avoid unnecessary trailing zeros, forcing US locale for dots
        valueField.setText(String.format(Locale.US, "%s", displayValue));

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