package heig.tb.jsmithfx.utilities.dialogs;

import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SweepDialog extends Dialog<List<Double>> {

    Label lb1 = new Label("Sweep by frequency");
    Label lbMinFq = new Label("Min Frequency:");
    Label lbMaxFq = new Label("Max Frequency:");
    Label stepLabel = new Label("How many steps:");

    TextField tfMinFq = new TextField("1");
    TextField tfMaxFq = new TextField("500");
    TextField tfStep = new TextField("10");

    ComboBox<String> cbMinFq = new ComboBox<>();
    ComboBox<String> cbMaxFq = new ComboBox<>();


    public SweepDialog(DataPoint dp) {
        setTitle("Sweep Settings");
        setHeaderText("Configure sweep parameters.");

        var currentFreq = dp.getFrequency();
        var toDisplayMin = SmithUtilities.getBestUnitAndFormattedValue(currentFreq * 0.5, FrequencyUnit.values());
        var toDisplayMax = SmithUtilities.getBestUnitAndFormattedValue(currentFreq * 1.5, FrequencyUnit.values());

        String[] comboItems = Arrays.stream(FrequencyUnit.values()).map(Enum::toString).toArray(String[]::new);
        cbMinFq.getItems().addAll(comboItems);
        cbMinFq.getSelectionModel().selectFirst();
        cbMaxFq.getItems().addAll(comboItems);
        cbMaxFq.getSelectionModel().selectFirst();

        tfMinFq.setText(String.valueOf(toDisplayMin.getValue()));
        cbMinFq.setValue(toDisplayMin.getKey().toString());
        tfMaxFq.setText(String.valueOf(toDisplayMax.getValue()));
        cbMaxFq.setValue(toDisplayMax.getKey().toString());


        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(lbMinFq, 0, 0);
        grid.add(tfMinFq, 1, 0);
        grid.add(cbMinFq, 2, 0);

        grid.add(lbMaxFq, 0, 1);
        grid.add(tfMaxFq, 1, 1);
        grid.add(cbMaxFq, 2, 1);

        grid.add(stepLabel, 0, 2);
        grid.add(tfStep, 1, 2);

        VBox content = new VBox(20, lb1, grid);
        content.setPadding(new Insets(20, 20, 20, 20));

        getDialogPane().setContent(content);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return getValues();
            }
            return null;
        });
    }

    private List<Double> getValues() {
        List<Double> values = new ArrayList<>();
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        try {
            double minFreq = Double.parseDouble(tfMinFq.getText()) * FrequencyUnit.valueOf(cbMinFq.getValue().toUpperCase()).getFactor();
            double maxFreq = Double.parseDouble(tfMaxFq.getText()) * FrequencyUnit.valueOf(cbMaxFq.getValue().toUpperCase()).getFactor();
            double stepCount = Double.parseDouble(tfStep.getText());

            if (stepCount <= 1) {
                DialogUtils.showErrorAlert("Invalid Input", "Number of steps must be >= 2", stage);
                return values;
            }

            if (minFreq >= maxFreq) {
                DialogUtils.showErrorAlert("Invalid Input", "Min frequency must be less than max frequency.", stage);
                return values;
            }

            double step = (maxFreq - minFreq) / (stepCount - 1); // Calculate step size based on number of steps


            for (int i = 0; i < stepCount; i++) {
                values.add(minFreq + i * step);
            }

        } catch (NumberFormatException e) {
            DialogUtils.showErrorAlert("Invalid Input", "Please enter valid numeric values for frequencies and step.", stage);
        }
        return values;
    }
}
