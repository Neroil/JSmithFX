package heig.tb.jsmithfx.utilities.dialogs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CircleDialog extends Dialog<List<Double>> {

    private final CheckBox VSWR10 = new CheckBox("10.0");
    private final CheckBox VSWR5 = new CheckBox("5.0");
    private final CheckBox VSWR3 = new CheckBox("3.0");
    private final CheckBox VSWR2 = new CheckBox("2.0");
    private final CheckBox VSWR1_5 = new CheckBox("1.5");
    private final CheckBox VSWR1_2 = new CheckBox("1.2");
    private final Button ClearAllButton = new Button("Clear All");
    private static final List<Double> previousSelections = new ArrayList<>();

    public static CircleDialog getInstance() {
        // When getting instance, save the current selections (in case of a cancel)
        return new CircleDialog();
    }

    private CircleDialog() {
        setTitle("Add VSWR Circles");
        setHeaderText("Select VSWR circles to display on the Smith chart:");

        setSelectedVswr(previousSelections);

        ClearAllButton.setOnAction(event -> {
            VSWR10.setSelected(false);
            VSWR5.setSelected(false);
            VSWR3.setSelected(false);
            VSWR2.setSelected(false);
            VSWR1_5.setSelected(false);
            VSWR1_2.setSelected(false);
        });

        VBox contentLeft = new VBox(
                10,
                VSWR10, VSWR5, VSWR3, VSWR2, VSWR1_5, VSWR1_2
        );

        VBox contentRight = new VBox(
                10,
                ClearAllButton
        );

        HBox content = new HBox(10, contentLeft, contentRight);

        content.setPadding(new Insets(10));

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                var toReturn = getSelectedVswrValues();
                previousSelections.clear();
                previousSelections.addAll(toReturn);
                return toReturn;
            }
            return null;
        });
    }

    public List<Double> getSelectedVswrValues() {
        List<Double> selected = new ArrayList<>();
        if (VSWR10.isSelected()) selected.add(10.0);
        if (VSWR5.isSelected()) selected.add(5.0);
        if (VSWR3.isSelected()) selected.add(3.0);
        if (VSWR2.isSelected()) selected.add(2.0);
        if (VSWR1_5.isSelected()) selected.add(1.5);
        if (VSWR1_2.isSelected()) selected.add(1.2);
        return selected;
    }

    public void setSelectedVswr(List<Double> selected) {
        VSWR10.setSelected(selected.contains(10.0));
        VSWR5.setSelected(selected.contains(5.0));
        VSWR3.setSelected(selected.contains(3.0));
        VSWR2.setSelected(selected.contains(2.0));
        VSWR1_5.setSelected(selected.contains(1.5));
        VSWR1_2.setSelected(selected.contains(1.2));
    }
}