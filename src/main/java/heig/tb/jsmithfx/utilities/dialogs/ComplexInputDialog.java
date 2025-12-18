package heig.tb.jsmithfx.utilities.dialogs;

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
import java.util.Locale;

public class ComplexInputDialog extends Dialog<Pair<ComplexInputDialog.MessageType, Complex>> {

    private enum DataType { IMPEDANCE, ADMITTANCE, REFLECTION }
    private enum DataFormat { CARTESIAN, POLAR }
    public enum MessageType {DATA, USEMOUSE};

    private final TextField field1;
    private final TextField field2;
    private final Label label1;
    private final Label label2;
    private final Label unitLabel;
    private final Label suffixLabel1;
    private final Label suffixLabel2;

    private final ToggleGroup dataTypeGroup;
    private final ToggleGroup formatGroup;

    public ComplexInputDialog(String title, Complex current) {
        setTitle(title);
        // Header text is set dynamically in updateLabels()

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType useMouseButton = new ButtonType("Use Mouse", ButtonBar.ButtonData.OTHER);
        getDialogPane().getButtonTypes().addAll(useMouseButton, okButton, ButtonType.CANCEL);

        // --- Init Controls ---
        field1 = new TextField();
        field2 = new TextField();
        label1 = new Label();
        label2 = new Label();
        unitLabel = new Label("Ω");
        suffixLabel1 = new Label();
        suffixLabel2 = new Label();

        // Radio Groups
        dataTypeGroup = new ToggleGroup();
        RadioButton impRad = createRadio("Impedance (Z)", DataType.IMPEDANCE, dataTypeGroup, true);
        RadioButton admRad = createRadio("Admittance (Y)", DataType.ADMITTANCE, dataTypeGroup, false);
        RadioButton refRad = createRadio("Reflection (Gamma)", DataType.REFLECTION, dataTypeGroup, false);

        formatGroup = new ToggleGroup();
        RadioButton cartRad = createRadio("Cartesian (Re + j Im)", DataFormat.CARTESIAN, formatGroup, true);
        RadioButton polRad = createRadio("Polar (Mag ∠ Ang)", DataFormat.POLAR, formatGroup, false);

        // --- Layout ---
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        HBox typeBox = new HBox(15, impRad, admRad, refRad);
        HBox fmtBox = new HBox(15, cartRad, polRad);

        grid.add(new Label("Data Type:"), 0, 0);
        grid.add(typeBox, 1, 0, 2, 1);
        grid.add(new Label("Format:"), 0, 1);
        grid.add(fmtBox, 1, 1, 2, 1);

        grid.add(label1, 0, 2);
        HBox box1 = new HBox(5, field1, suffixLabel1);
        box1.setAlignment(Pos.CENTER_LEFT);
        grid.add(box1, 1, 2);

        grid.add(label2, 0, 3);
        HBox box2 = new HBox(5, field2, suffixLabel2);
        box2.setAlignment(Pos.CENTER_LEFT);
        grid.add(box2, 1, 3);

        // --- Logic ---
        // Listener to update UI state
        Runnable updateUI = () -> updateLabelsAndFields(current);
        dataTypeGroup.selectedToggleProperty().addListener(_ -> updateUI.run());
        formatGroup.selectedToggleProperty().addListener(_ -> updateUI.run());

        // Initial run
        updateUI.run();

        getDialogPane().setContent(grid);
        Platform.runLater(field1::requestFocus);

        // --- Result Converter ---
        setResultConverter(btn -> {
            if (btn == okButton) {
                return new Pair<>(MessageType.DATA,convertResult());
            } else if (btn == useMouseButton){
                return new Pair<>(MessageType.USEMOUSE, null);
            }
            return null;
        });
    }

    private RadioButton createRadio(String text, Object userData, ToggleGroup group, boolean selected) {
        RadioButton rb = new RadioButton(text);
        rb.setUserData(userData);
        rb.setToggleGroup(group);
        rb.setSelected(selected);
        return rb;
    }

    private void updateLabelsAndFields(Complex current) {

        if (current == null) {
            current = new Complex(0,0);
        }

        DataFormat format = (DataFormat) formatGroup.getSelectedToggle().getUserData();
        DataType type = (DataType) dataTypeGroup.getSelectedToggle().getUserData();

        Complex displayVal = current;

        // Determine Unit and Value transformation
        if (type == DataType.ADMITTANCE) {
            displayVal = current.inverse(); // Show Y instead of Z
            unitLabel.setText("mS");
        } else if (type == DataType.IMPEDANCE) {
            unitLabel.setText("Ω");
        } else {
            unitLabel.setText("");
        }

        DecimalFormat df = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));

        if (format == DataFormat.CARTESIAN) {
            setHeaderText("Enter Real and Imaginary parts:");
            label1.setText(type == DataType.IMPEDANCE ? "Real (R):" : type == DataType.ADMITTANCE ? "Real (G):" : "Real:");
            label2.setText(type == DataType.IMPEDANCE ? "Imag. (jX):" : type == DataType.ADMITTANCE ? "Imag. (jB):" : "Imag.:");

            double multiplier = (type == DataType.ADMITTANCE) ? 1000 : 1;
            field1.setText(df.format(displayVal.real() * multiplier));
            field2.setText(df.format(displayVal.imag() * multiplier));

            suffixLabel1.setText(unitLabel.getText());
            suffixLabel2.setText(type == DataType.IMPEDANCE ? "j" + unitLabel.getText() : unitLabel.getText());
        } else {
            setHeaderText("Enter Magnitude and Angle:");
            label1.setText("Magnitude (M):");
            label2.setText("Angle (θ):");

            double multiplier = (type == DataType.ADMITTANCE) ? 1000 : 1;
            field1.setText(df.format(displayVal.magnitude() * multiplier));
            field2.setText(df.format(Math.toDegrees(displayVal.angle())));

            suffixLabel1.setText(unitLabel.getText());
            suffixLabel2.setText("°");
        }
    }

    private Complex convertResult() {
        try {
            double v1 = Double.parseDouble(field1.getText());
            double v2 = Double.parseDouble(field2.getText());
            DataFormat format = (DataFormat) formatGroup.getSelectedToggle().getUserData();
            DataType type = (DataType) dataTypeGroup.getSelectedToggle().getUserData();

            Complex result;
            if (format == DataFormat.CARTESIAN) {
                result = new Complex(v1, v2);
            } else {
                double rads = Math.toRadians(v2);
                result = new Complex(v1 * Math.cos(rads), v1 * Math.sin(rads));
            }

            if (type == DataType.ADMITTANCE) {
                return result.dividedBy(1000.0).inverse(); // mS -> S -> Z
            }
            return result;
        } catch (NumberFormatException e) {
            return null; // Or show error
        }
    }
}