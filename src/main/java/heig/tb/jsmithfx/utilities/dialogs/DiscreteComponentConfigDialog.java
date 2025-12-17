package heig.tb.jsmithfx.utilities.dialogs;

import heig.tb.jsmithfx.model.CircuitElement.ElementType;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.ComponentEntry;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscreteComponentConfigDialog extends Dialog<List<ComponentEntry>> {

    private final static ObservableList<ComponentEntry> componentList = FXCollections.observableArrayList();
    private final ObservableList<ComponentEntry> componentListTemp = FXCollections.observableArrayList();
    private final ComboBox<ElementType> typeBox = new ComboBox<>();
    private final TextField valueField = new TextField();
    private final ComboBox<Enum<?>> unitBox = new ComboBox<>();
    private final TextField parasiticField = new TextField();
    private final Label parasiticLabel = new Label("Parasitic:");
    private final Label valueLabel = new Label("Value:");

    private final RadioButton rbESR = new RadioButton("ESR");
    private final RadioButton rbQ = new RadioButton("Q-Factor");
    private final ToggleGroup parasiticGroup = new ToggleGroup();
    private final HBox parasiticToggleBox = new HBox(10, rbESR, rbQ);

    public DiscreteComponentConfigDialog() {

        // Copy the existing list to avoid modification of static list
        componentListTemp.setAll(componentList);

        setTitle("Discrete Component Configuration");
        setHeaderText("Manage your library of discrete components.");
        setResizable(true);

        VBox leftPanel = createInputPanel();
        VBox rightPanel = createTablePanel();
        Separator separator = new Separator(Orientation.VERTICAL);

        HBox root = new HBox(15, leftPanel, separator, rightPanel);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        leftPanel.setMaxWidth(Double.MAX_VALUE);
        rightPanel.setMaxWidth(Double.MAX_VALUE);

        leftPanel.setPrefWidth(1);
        rightPanel.setPrefWidth(1);

        getDialogPane().setContent(root);

        getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        // Handle Cancel button with unsaved changes check
        final Button btCancel = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        btCancel.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Check if the lists are different (content comparison)
            if (!componentListTemp.equals(componentList)) {
                boolean confirm = DialogUtils.areYouSureDialog("Discard Changes",
                        "Are you sure you want to discard your changes?", getWindow());

                if (!confirm) {
                    event.consume();
                }
            }
        });

        setResultConverter(btn -> {
            if (btn == ButtonType.APPLY) {
                // Update the static list
                componentList.setAll(componentListTemp);
                return new ArrayList<>(componentList);
            }
            return null;
        });

        setOnShown(_ -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            stage.setMinWidth(600);
            stage.setMinHeight(400);
            stage.setWidth(900);
            stage.setHeight(500);
        });

    }

    private VBox createInputPanel() {
        typeBox.getItems().addAll(ElementType.values());
        typeBox.getItems().remove(ElementType.LINE);
        typeBox.setMaxWidth(Double.MAX_VALUE);

        typeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ElementType object) { return object == null ? "" : object.toString(); }
            @Override
            public ElementType fromString(String string) { return null; }
        });

        rbESR.setToggleGroup(parasiticGroup);
        rbQ.setToggleGroup(parasiticGroup);
        rbESR.setSelected(true);

        typeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateUIState());
        parasiticGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateUIState());
        typeBox.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints colLabels = new ColumnConstraints();
        ColumnConstraints colFields = new ColumnConstraints();
        colFields.setHgrow(Priority.ALWAYS);
        ColumnConstraints colUnits = new ColumnConstraints();
        grid.getColumnConstraints().addAll(colLabels, colFields, colUnits);

        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeBox, 1, 0, 2, 1);

        grid.add(valueLabel, 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(unitBox, 2, 1);

        grid.add(parasiticToggleBox, 1, 2, 2, 1);

        grid.add(parasiticLabel, 0, 3);
        grid.add(parasiticField, 1, 3, 2, 1);

        Button addButton = new Button("Add to List");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> addComponent());
        parasiticField.setOnAction(e -> addComponent());

        VBox box = new VBox(15, new Label("New Component"), grid, new Separator(), addButton);
        VBox.setVgrow(box, Priority.ALWAYS);
        box.setPadding(new Insets(10));

        return box;
    }

    private VBox createTablePanel() {
        TableView<ComponentEntry> table = new TableView<>(componentListTemp);
        table.setPlaceholder(new Label("No components added yet."));

        TableColumn<ComponentEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("typeName"));

        TableColumn<ComponentEntry, Double> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        valCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    ComponentEntry entry = getTableRow().getItem();
                    ElectronicUnit[] units = switch (entry.getType()) {
                        case RESISTOR -> ResistanceUnit.values();
                        case CAPACITOR -> CapacitanceUnit.values();
                        case INDUCTOR -> InductanceUnit.values();
                        default -> null;
                    };

                    if (units != null) {
                        setText(SmithUtilities.displayBestUnitAndFormattedValue(item, units));
                    } else {
                        setText(String.format("%.3g", item));
                    }
                }
            }
        });

        TableColumn<ComponentEntry, String> parCol = new TableColumn<>("Parasitic");
        parCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getParasiticDisplay()));

        table.getColumns().setAll(List.of(typeCol, valCol, parCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox.setVgrow(table, Priority.ALWAYS);

        Button removeButton = new Button("Remove Selected");
        removeButton.setDisable(true);
        removeButton.setOnAction(e -> componentListTemp.remove(table.getSelectionModel().getSelectedItem()));

        table.getSelectionModel().selectedItemProperty().addListener((o, old, newV) ->
                removeButton.setDisable(newV == null)
        );

        Button importButton = new Button("Import CSV");
        Button exportButton = new Button("Export CSV");
        importButton.setOnAction(e -> importFromFile());
        exportButton.setOnAction(e -> exportToFile());

        HBox ioBox = new HBox(10, importButton, exportButton, new Region(), removeButton);
        HBox.setHgrow(ioBox.getChildren().get(2), Priority.ALWAYS);
        ioBox.setAlignment(Pos.CENTER_LEFT);

        var toReturn = new VBox(10, table, ioBox);
        VBox.setVgrow(toReturn, Priority.ALWAYS);

        return toReturn;
    }

    private void updateUIState() {
        ElementType type = typeBox.getValue();
        if (type == null) return;

        Enum<?>[] values = switch (type) {
            case RESISTOR  -> ResistanceUnit.values();
            case CAPACITOR -> CapacitanceUnit.values();
            case INDUCTOR  -> InductanceUnit.values();
            default -> new Enum<?>[0];
        };

        unitBox.getItems().setAll(values);
        unitBox.getSelectionModel().selectFirst();

        boolean isResistor = type == ElementType.RESISTOR;

        parasiticToggleBox.setVisible(!isResistor);
        parasiticToggleBox.setManaged(!isResistor);

        if (isResistor) {
            valueLabel.setText("Resistance:");
            parasiticLabel.setVisible(false);
            parasiticLabel.setManaged(false);
            parasiticField.setVisible(false);
            parasiticField.setManaged(false);
        } else {
            parasiticLabel.setVisible(true);
            parasiticLabel.setManaged(true);
            parasiticField.setVisible(true);
            parasiticField.setManaged(true);
            if (type == ElementType.CAPACITOR) {
                valueLabel.setText("Capacitance:");
            } else {
                valueLabel.setText("Inductance:");
            }

            setupParasitic();
        }
    }

    private void setupParasitic() {
        if (rbQ.isSelected()) {
            parasiticLabel.setText("Q-Factor:");
            parasiticField.setText("10000"); // Default to high Q
        } else {
            parasiticLabel.setText("ESR (Ω):");
            parasiticField.setText("0.0"); // Default to 0 ESR
        }
    }

    private void addComponent() {
        ElementType type = typeBox.getValue();
        String valText = valueField.getText();
        String parText = parasiticField.getText();

        if (type == ElementType.RESISTOR && parText.isEmpty()) {
            parText = "0.0";
        }

        if (valText.isEmpty() || parText.isEmpty()) {
            DialogUtils.showErrorAlert("Missing Input", "Please enter both Value and Parasitic/Q parameters.", getWindow());
            return;
        }

        Optional<Double> valInputOpt = SmithUtilities.parseOptionalDouble(valText);
        Optional<Double> parOpt = SmithUtilities.parseOptionalDouble(parText);

        if (valInputOpt.isEmpty() || parOpt.isEmpty()) {
            DialogUtils.showErrorAlert("Invalid Input", "Numbers only, please.", getWindow());
            return;
        }

        double valInput = valInputOpt.get();
        double par = parOpt.get();

        double factor = 1.0;
        if (unitBox.getValue() instanceof ElectronicUnit u) {
            factor = u.getFactor();
        }
        double val = valInput * factor;

        boolean isQ = (type != ElementType.RESISTOR) && rbQ.isSelected();

        componentListTemp.add(new ComponentEntry(type, val, par, isQ));

        valueField.clear();
        setupParasitic();
        valueField.requestFocus();
    }

    private void importFromFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Components");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showOpenDialog(getWindow());

        if (f == null) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if(line.trim().isEmpty() || line.startsWith("#") || line.toLowerCase().startsWith("type")) continue;

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        ElementType type = ElementType.valueOf(parts[0].trim().toUpperCase());

                        Optional<Double> valOpt = SmithUtilities.parseOptionalDouble(parts[1].trim());
                        Optional<Double> parOpt = SmithUtilities.parseOptionalDouble(parts[2].trim());

                        if(valOpt.isEmpty() || parOpt.isEmpty()) continue;

                        boolean isQ = false;
                        if (parts.length >= 4) {
                            isQ = Boolean.parseBoolean(parts[3].trim());
                        } else {
                            if (type == ElementType.INDUCTOR) isQ = true;
                        }

                        componentListTemp.add(new ComponentEntry(type, valOpt.get(), parOpt.get(), isQ));
                        count++;
                    } catch (IllegalArgumentException ex) {
                        System.err.println("Skipping invalid line: " + line);
                    }
                }
            }
            if(count > 0) {
                new Alert(Alert.AlertType.INFORMATION, "Successfully imported " + count + " components.").show();
            }
        } catch (Exception e) {
            DialogUtils.showErrorAlert("Import Error", e.getMessage(), getWindow());
        }
    }

    private void exportToFile() {
        if(componentListTemp.isEmpty()) {
            DialogUtils.showErrorAlert("Empty List", "Nothing to export.", getWindow());
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Components");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("components.csv");
        File f = fc.showSaveDialog(getWindow());

        if (f == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("Type,Value,Parasitic,IsQFactor");
            for (ComponentEntry c : componentListTemp) {
                pw.println(c.getType().name() + "," + c.getValue() + "," + c.getParasitic() + "," + c.isQFactor());
            }
        } catch (IOException e) {
            DialogUtils.showErrorAlert("Export Error", e.getMessage(), getWindow());
        }
    }

    private Window getWindow() {
        Window w = getDialogPane().getScene().getWindow();
        return (w != null) ? w : SmithUtilities.getActiveStage();
    }
}