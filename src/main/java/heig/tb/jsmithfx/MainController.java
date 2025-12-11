package heig.tb.jsmithfx;

import heig.tb.jsmithfx.controller.SmithChartInteractionController;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.TypicalUnit.*;
import heig.tb.jsmithfx.model.TouchstoneS1P;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import heig.tb.jsmithfx.utilities.dialogs.*;
import heig.tb.jsmithfx.view.CircuitRenderer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.controlsfx.control.RangeSlider;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController {

    // FXML Bindings
    @FXML
    private CheckMenuItem enableQualityFactorInput;
    @FXML
    private HBox addComponentButtonHBox;
    @FXML
    private TitledPane addComponentText;
    @FXML
    private VBox appBox;
    @FXML
    private Label returnLossLabel;
    @FXML
    private Label vswrLabel;
    @FXML
    private Label qLabel;
    @FXML
    private Label gammaLabel;
    @FXML
    private Label yLabel;
    @FXML
    private Label zLabel;
    @FXML
    private Label zoLabel;
    @FXML
    private Label freqLabel;
    @FXML
    private MenuItem setCharacteristicImpedanceButton;
    @FXML
    private Button changeLoadButton;
    @FXML
    private Label loadImpedanceLabel;
    @FXML
    private Button changeFreqButton;
    @FXML
    private Button zoomInButton;
    @FXML
    private Button zoomOutButton;
    @FXML
    private Button resetButton;
    @FXML
    private HBox buttonSmithHBox;
    @FXML
    private CheckMenuItem toggleNavButton;
    @FXML
    private Pane smithChartPane;
    @FXML
    private Canvas smithCanvas;
    @FXML
    private Canvas cursorCanvas;
    @FXML
    private Canvas circuitCanvas;
    @FXML
    private TableView<DataPoint> dataPointsTable;
    @FXML
    private ComboBox<CircuitElement.ElementType> typeComboBox;
    @FXML
    private ComboBox<CircuitElement.ElementPosition> positionComboBox;
    @FXML
    private ComboBox<Enum<?>> unitComboBox;
    @FXML
    private TextField valueTextField;
    @FXML
    private Button addButton;
    @FXML
    private TableColumn<DataPoint, String> labelColumn;
    @FXML
    private TableColumn<DataPoint, Void> deleteColumn;
    @FXML
    private TableColumn<DataPoint, Complex> impedanceColumn;
    @FXML
    private TableColumn<DataPoint, Number> vswrColumn;
    @FXML
    private TableColumn<DataPoint, Number> returnLossColumn;
    @FXML
    private Label z0Label;
    @FXML
    private Pane circuitPane;
    @FXML
    private Label zoInputLabel;
    @FXML
    private TextField zoInputField;
    @FXML
    private Label permittivityLabel;
    @FXML
    private TextField permittivityField;
    @FXML
    private ComboBox<Line.StubType> stubComboBox;
    @FXML
    private TableColumn<DataPoint, String> frequencyColumn;
    @FXML
    private MenuItem importS1PButton;
    @FXML
    private MenuItem exportS1PButton;
    @FXML
    private TitledPane s1pTitledPane;
    @FXML
    private VBox s1pLoadedView;
    @FXML
    private TextField maxFreqTextField;
    @FXML
    private TextField minFreqTextField;
    @FXML
    private TextField s1pFileNameField;
    @FXML
    private CheckBox useS1PAsLoadCheckBoxF1;
    @FXML
    private CheckBox useS1PAsLoadCheckBoxF2;
    @FXML
    private CheckBox useS1PAsLoadCheckBoxF3;

    @FXML private CheckMenuItem toggle1FilterButton, toggle2FilterButton, toggle3FilterButton;
    @FXML private VBox filter1Box, filter2Box, filter3Box;
    @FXML private TextField minFreqTextField1, maxFreqTextField1, minFreqTextField2, maxFreqTextField2, minFreqTextField3, maxFreqTextField3;
    @FXML private RangeSlider frequencyRangeSlider1, frequencyRangeSlider2, frequencyRangeSlider3;
    @FXML
    private MenuItem setDisplayCirclesOptionsButton;
    @FXML
    private Button sweepButton;
    @FXML
    private CheckMenuItem toggleSweepInDataPointsButton;
    @FXML
    private CheckMenuItem toggleS1PInDataPointsButton;
    @FXML
    private Button clearSweepButton;
    @FXML
    private TextField sweepPointsCountText;
    @FXML
    private TextField sweepStartFreqField;
    @FXML
    private Button sweepStartFreqMinusButton;
    @FXML
    private Button sweepStartFreqPlusButton;
    @FXML
    private TextField sweepEndFreqField;
    @FXML
    private Button sweepEndFreqMinusButton;
    @FXML
    private Button exportSweepButton;
    @FXML
    private Button sweepEndFreqPlusButton;
    @FXML
    private TitledPane sweepManagementTitledPane;
    @FXML
    private Slider s1pPointSizeSlider;
    @FXML
    private Button addMouseButton;
    @FXML
    private HBox tuningPane;
    @FXML
    private Slider tuningSlider;
    @FXML
    private TextField tuningValueField;
    @FXML
    private Label tuningUnitLabel;
    @FXML
    private Button applyTuningButton;
    @FXML
    private Button cancelTuningButton;
    @FXML
    private TableColumn<DataPoint, Number> qualityFactorColumn;

    //Viewmodel
    private SmithChartViewModel viewModel;

    //Renderer
    private CircuitRenderer circuitRenderer;
    private SmithChartInteractionController smithInteractionController;

    /**
     * This method is called by the FXMLLoader after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        this.viewModel = SmithChartViewModel.getInstance();
        circuitRenderer = new CircuitRenderer(circuitCanvas);

        smithInteractionController = new SmithChartInteractionController(
                smithChartPane,
                smithCanvas,
                cursorCanvas,
                viewModel,
                () -> typeComboBox.getValue(),
                () -> positionComboBox.getValue(),
                () -> stubComboBox.getValue(),
                () -> SmithUtilities.parseOptionalDouble(zoInputField.getText()),
                () -> SmithUtilities.parseOptionalDouble(permittivityField.getText()),
                (value, unit) -> {
                    valueTextField.setText(value);
                    unitComboBox.getSelectionModel().select(unit);
                },
                text -> addMouseButton.setText(text)
        );

        // filter enabled bindings
        viewModel.filter1EnabledProperty().bind(
                toggle1FilterButton.selectedProperty()
                        .or(toggle2FilterButton.selectedProperty())
                        .or(toggle3FilterButton.selectedProperty())
        );

        viewModel.filter2EnabledProperty().bind(
                toggle2FilterButton.selectedProperty()
                        .or(toggle3FilterButton.selectedProperty())
        );

        viewModel.filter3EnabledProperty().bind(
                toggle3FilterButton.selectedProperty()
        );

        // Force a redraw when the user switches between filters (Filter 1 -> Filter 2)
        viewModel.filter1EnabledProperty().addListener((obs, oldVal, newVal) -> smithInteractionController.redrawSmithCanvas());
        viewModel.filter2EnabledProperty().addListener((obs, oldVal, newVal) -> smithInteractionController.redrawSmithCanvas());
        viewModel.filter3EnabledProperty().addListener((obs, oldVal, newVal) -> smithInteractionController.redrawSmithCanvas());


        setupResizableCanvas();
        setupControls();
        bindViewModel();

        // Whenever the circuit elements change, re-render the circuit diagram
        viewModel.circuitElements.addListener((ListChangeListener<CircuitElement>) _ -> circuitRenderer.render(viewModel));

        // Initial render
        circuitRenderer.render(viewModel);

        dataPointsTable.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> {
            viewModel.setDpTableSelIndex(dataPointsTable.getSelectionModel().getSelectedIndex());
            smithInteractionController.redrawSmithCanvas();
        });

        viewModel.getDpTableSelIndex().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.intValue() >= 0 && newVal.intValue() < dataPointsTable.getItems().size()) {
                dataPointsTable.getSelectionModel().select(newVal.intValue());
            } else {
                dataPointsTable.getSelectionModel().clearSelection();
            }
        });

        //Bindings to display mouse related information
        returnLossLabel.textProperty().bind(viewModel.mouseReturnLossTextProperty());
        vswrLabel.textProperty().bind(viewModel.mouseVSWRTextProperty());
        qLabel.textProperty().bind(viewModel.mouseQualityFactorTextProperty());
        gammaLabel.textProperty().bind(viewModel.mouseGammaTextProperty());
        yLabel.textProperty().bind(viewModel.mouseAdmittanceYTextProperty());
        zLabel.textProperty().bind(viewModel.mouseImpedanceZTextProperty());

        z0Label.textProperty().bind(viewModel.zoProperty());

        //Enable editing of the values by double-clicking on them in the display point
        dataPointsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = dataPointsTable.getSelectionModel().getSelectedIndex();
                // if index > 0 (skipping Load), edit the element
                if (selectedIndex > 0 && selectedIndex <= viewModel.circuitElements.size()) {
                    CircuitElement component = viewModel.circuitElements.get(selectedIndex - 1);
                    promptEditForComponent(component);
                }
            }
        });

        // Hover highlight for circuit elements
        circuitCanvas.setOnMouseMoved(event -> {
            CircuitElement hoveredElement = circuitRenderer.getElementAt(event.getX(), event.getY());
            viewModel.setHoveredElement(hoveredElement);
            circuitRenderer.render(viewModel);
        });

        // Circuit Diagram Editing
        circuitCanvas.setOnMouseClicked(event -> {
            int selectedIndex = circuitRenderer.getInsertionIndexAt(event.getX(), event.getY());
            CircuitElement clickedElement = circuitRenderer.getElementAt(event.getX(), event.getY());
            viewModel.setSelectedInsertionIndex(selectedIndex);

            if (clickedElement != null) {
                if (event.getClickCount() == 2) {
                    viewModel.cancelTuningAdjustments();
                    promptEditForComponent(clickedElement);
                    event.consume();
                } else if (event.getClickCount() == 1) {
                    viewModel.selectElement(clickedElement);
                }
            }
        });

        viewModel.getSelectedInsertionIndexProperty().addListener(_ -> circuitRenderer.render(viewModel));

        viewModel.selectedElementProperty().addListener((_, _, selectedElement) -> {
            if (selectedElement != null) {
                tuningPane.setVisible(true);
                tuningPane.setManaged(true);
                setupTuningPaneForElement(selectedElement);
                setupModifyElement(selectedElement);
            } else {
                tuningPane.setVisible(false);
                tuningPane.setManaged(false);
                setupAddElement();
            }
            circuitRenderer.render(viewModel);

        });

        tuningSlider.valueProperty().addListener((_1, _2, newValue) -> {
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue((Double) newValue, (ElectronicUnit[]) viewModel.selectedElementProperty().get().getType().getUnitClass().getEnumConstants());
            tuningValueField.setText(toDisplay.getValue());
            tuningUnitLabel.setText(toDisplay.getKey().toString());
            viewModel.updateTunedElementValue(newValue.doubleValue());
        });

        Runnable updateTuningValue = () -> {
            var unitClassValues = (ElectronicUnit[]) viewModel.selectedElementProperty().get().getType().getUnitClass().getEnumConstants();
            String valueString = tuningValueField.getText() + " " + tuningUnitLabel.getText();
            System.out.println(valueString);
            double newValue = SmithUtilities.parseValueWithUnit(valueString, unitClassValues);
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(newValue, unitClassValues);
            tuningValueField.setText(toDisplay.getValue());
            tuningUnitLabel.setText(toDisplay.getKey().toString());
            viewModel.updateTunedElementValue(newValue);
        };

        // Trigger on ENTER
        tuningValueField.setOnAction(event -> updateTuningValue.run());

        // Trigger when focus is lost (clicking away)
        tuningValueField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) updateTuningValue.run();
        });

        //Add the delete button in the factory
        deleteColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("X");

            {
                deleteButton.setOnAction(_ -> {
                    dataPointsTable.getSelectionModel().clearSelection();
                    if (getIndex() - 1 < viewModel.circuitElements.size()) {
                        viewModel.removeComponentAt(getIndex() - 1);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                int componentCount = viewModel.circuitElements.size();

                // The first few elements are the circuits elements, editable
                if (empty || getIndex() < 1 || getIndex() > componentCount) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        setupFilterControl(
                frequencyRangeSlider1, minFreqTextField1, maxFreqTextField1,
                viewModel::setFrequencyRangeMinF1, viewModel::setFrequencyRangeMaxF1
        );

        // Filter 2
        setupFilterControl(
                frequencyRangeSlider2, minFreqTextField2, maxFreqTextField2,
                viewModel::setFrequencyRangeMinF2, viewModel::setFrequencyRangeMaxF2
        );

        // Filter 3
        setupFilterControl(
                frequencyRangeSlider3, minFreqTextField3, maxFreqTextField3,
                viewModel::setFrequencyRangeMinF3, viewModel::setFrequencyRangeMaxF3
        );

        s1pPointSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setS1PPointSize(newVal.doubleValue());
            smithInteractionController.redrawSmithCanvas();
        });

        viewModel.sweepDataPointsProperty().addListener((obs, oldVal, newVal) -> {
            var sdp = viewModel.sweepDataPointsProperty().get();
            Pair<Double, Double> freqRange = SmithUtilities.getFrequencyRangeFromDataPoints(sdp);
            sweepStartFreqField.setText(SmithUtilities.displayBestUnitAndFormattedValue(freqRange.getKey(), FrequencyUnit.values()));
            sweepEndFreqField.setText(SmithUtilities.displayBestUnitAndFormattedValue(freqRange.getValue(), FrequencyUnit.values()));
            sweepPointsCountText.setText(String.valueOf(sdp.size()));
            smithInteractionController.redrawSmithCanvas();
        });

        sweepStartFreqMinusButton.setOnAction(_ -> viewModel.decrementSweepStartFrequency());
        sweepStartFreqPlusButton.setOnAction(_ -> viewModel.incrementSweepStartFrequency());
        sweepEndFreqMinusButton.setOnAction(_ -> viewModel.decrementSweepEndFrequency());
        sweepEndFreqPlusButton.setOnAction(_ -> viewModel.incrementSweepEndFrequency());

        // 2. Logic for LIVE updates on text fields

        // Helper to get current values safely
        Runnable updateSweepFromTextFields = () -> {
            try {
                // Parse Frequency Fields (handles "1 GHz", "500 MHz" etc.)
                double minFreq = SmithUtilities.parseValueWithUnit(sweepStartFreqField.getText(), FrequencyUnit.values());
                double maxFreq = SmithUtilities.parseValueWithUnit(sweepEndFreqField.getText(), FrequencyUnit.values());

                // Parse Count Field
                int count = Integer.parseInt(sweepPointsCountText.getText());

                // Send to ViewModel
                viewModel.updateSweepConfiguration(minFreq, maxFreq, count);
            } catch (Exception e) {
                // Ignore parsing errors while typing (e.g., user deleted the number)
            }
        };

        // Add listeners to trigger update when ENTER is pressed
        sweepStartFreqField.setOnAction(_ -> updateSweepFromTextFields.run());
        sweepEndFreqField.setOnAction(_ -> updateSweepFromTextFields.run());
        sweepPointsCountText.setOnAction(_ -> updateSweepFromTextFields.run());

        // Add listeners to trigger update when FOCUS is lost (clicking away)
        sweepStartFreqField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) updateSweepFromTextFields.run();
        });
        sweepEndFreqField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) updateSweepFromTextFields.run();
        });
        sweepPointsCountText.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) updateSweepFromTextFields.run();
        });

        // Tells the renderer to use the S1P data as load
        useS1PAsLoadCheckBoxF1.selectedProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setUseS1PAsLoadF1(newVal);
            smithInteractionController.redrawSmithCanvas();
        });

        useS1PAsLoadCheckBoxF2.selectedProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setUseS1PAsLoadF2(newVal);
            smithInteractionController.redrawSmithCanvas();
        });

        useS1PAsLoadCheckBoxF3.selectedProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setUseS1PAsLoadF3(newVal);
            smithInteractionController.redrawSmithCanvas();
        });

        // Update checkboxes depending on viewmodel changes
        viewModel.useS1PAsLoadF1Property().addListener((obs, oldVal, newVal) -> {
            useS1PAsLoadCheckBoxF1.setSelected(newVal);
        });
        viewModel.useS1PAsLoadF2Property().addListener((obs, oldVal, newVal) -> {
            useS1PAsLoadCheckBoxF2.setSelected(newVal);
        });
        viewModel.useS1PAsLoadF3Property().addListener((obs, oldVal, newVal) -> {
            useS1PAsLoadCheckBoxF3.setSelected(newVal);
        });


        //Enable CTRL Z (undo) and CTRL Y (redo)
        Platform.runLater(() -> {
            smithCanvas.getScene().setOnKeyPressed(event -> {
                if (event.isControlDown()) {
                    if (event.getCode() == KeyCode.Z) {
                        // Undo logic
                        viewModel.undo();
                        event.consume();
                    } else if (event.getCode() == KeyCode.Y) {
                        // Redo logic
                        viewModel.redo();
                        event.consume();
                    }
                }
            });
        });
    }

    /**
     * Helper to wire up a Slider, its TextFields, and the ViewModel
     */
    private void setupFilterControl(RangeSlider slider, TextField minField, TextField maxField,
                                    java.util.function.Consumer<Double> minSetter,
                                    java.util.function.Consumer<Double> maxSetter) {

        // 1. Slider -> ViewModel & Text
        slider.lowValueProperty().addListener((obs, oldVal, newVal) -> {
            minSetter.accept(newVal.doubleValue());
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(newVal.doubleValue(), FrequencyUnit.values());
            minField.setText(toDisplay.getValue() + " " + toDisplay.getKey().toString());
            viewModel.updateMiddleRangePoint();
            smithInteractionController.redrawSmithCanvas();
        });

        slider.highValueProperty().addListener((obs, oldVal, newVal) -> {
            maxSetter.accept(newVal.doubleValue());
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(newVal.doubleValue(), FrequencyUnit.values());
            maxField.setText(toDisplay.getValue() + " " + toDisplay.getKey().toString());
            viewModel.updateMiddleRangePoint();
            smithInteractionController.redrawSmithCanvas();
        });

        // 2. Text -> Slider
        setupFrequencyField(minField, (val) -> slider.setLowValue(val));
        setupFrequencyField(maxField, (val) -> slider.setHighValue(val));
    }

    /**
     * Helper to parse text input and update the slider
     */
    private void setupFrequencyField(TextField field, java.util.function.Consumer<Double> sliderUpdater) {
        Runnable updateAction = () -> {
            String text = field.getText();
            try {
                double freqInHz = SmithUtilities.parseValueWithUnit(text, FrequencyUnit.values());
                sliderUpdater.accept(freqInHz);
            } catch (IllegalArgumentException e) {
                Logger.getLogger(MainController.class.getName()).log(Level.WARNING, "Invalid frequency input: " + text, e);
                DialogUtils.showErrorAlert("Invalid Input", "Please enter a valid frequency value.", appBox.getScene().getWindow());
            }
        };

        field.setOnAction(event -> updateAction.run());
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) updateAction.run(); // Update on lost focus
        });
    }

    private void setupModifyElement(CircuitElement el) {
        viewModel.isModifyingComponent.set(true);
        addComponentText.setText("Modify Component");
        addButton.setText("Apply");
        addMouseButton.setText("Mouse edit");
        var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(el.getRealWorldValue(), (ElectronicUnit[]) el.getType().getUnitClass().getEnumConstants());
        valueTextField.setText(toDisplay.getValue());
        typeComboBox.setValue(el.getType());
        if (el.getType() != CircuitElement.ElementType.LINE) {
            positionComboBox.setValue(el.getPosition());
        } else {
            Line line = (Line) el;
            stubComboBox.setValue(line.getStubType());
            zoInputField.setText(String.valueOf(line.getCharacteristicImpedance()));
            permittivityField.setText(String.valueOf(line.getPermittivity()));
        }

        var deleteButton = new Button("Delete");
        deleteButton.setOnAction(_ -> { viewModel.removeComponent(el); });
        // Add delete button to the hbox
        addComponentButtonHBox.getChildren().clear();
        addComponentButtonHBox.getChildren().addAll(addButton, addMouseButton, deleteButton);


        unitComboBox.getSelectionModel().select((Enum<?>) toDisplay.getKey());
    }

    private void setupAddElement() {
        viewModel.isModifyingComponent.set(false);
        addComponentText.setText("Add Component");
        addButton.setText("Add");
        addMouseButton.setText("Mouse add");
        valueTextField.clear();
        typeComboBox.getSelectionModel().selectFirst();
        unitComboBox.getSelectionModel().selectFirst();
        // Remove delete button from the hbox
        addComponentButtonHBox.getChildren().clear();
        addComponentButtonHBox.getChildren().addAll(addButton, addMouseButton);
    }

    private void setupTuningPaneForElement(CircuitElement el) {
        var value = el.getRealWorldValue();
        var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(value, (ElectronicUnit[]) el.getType().getUnitClass().getEnumConstants());
        tuningValueField.setText(toDisplay.getValue());
        tuningUnitLabel.setText(toDisplay.getKey().toString());
        // Set a range to +-50%
        tuningSlider.setMin(value * 0.5);
        tuningSlider.setMax(value * 1.5);
        tuningSlider.setValue(value);
    }

    private void setupResizableCanvas() {
        circuitPane.setMinSize(0, 0);

        circuitCanvas.widthProperty().bind(circuitPane.widthProperty());
        circuitCanvas.heightProperty().bind(circuitPane.heightProperty());

        circuitCanvas.widthProperty().addListener(_ -> circuitRenderer.render(viewModel));
        circuitCanvas.heightProperty().addListener(_ -> circuitRenderer.render(viewModel));
    }

    /**
     * Populates ComboBoxes and sets default values.
     */
    private void setupControls() {
        typeComboBox.getItems().addAll(CircuitElement.ElementType.values());
        positionComboBox.getItems().addAll(CircuitElement.ElementPosition.values());
        unitComboBox.getItems().addAll(CapacitanceUnit.values());

        typeComboBox.getSelectionModel().selectFirst();
        positionComboBox.getSelectionModel().selectFirst();
        unitComboBox.getSelectionModel().selectFirst();
        stubComboBox.getItems().addAll(Line.StubType.values());
        stubComboBox.getSelectionModel().selectFirst();

        typeComboBox.valueProperty().addListener((_, _, newType) -> {
            boolean isLineType = newType == CircuitElement.ElementType.LINE;

            // Make the additional info visible if it's a line
            zoInputLabel.setVisible(isLineType);
            zoInputField.setVisible(isLineType);
            permittivityLabel.setVisible(isLineType);
            permittivityField.setVisible(isLineType);
            positionComboBox.setVisible(!isLineType);
            stubComboBox.setVisible(isLineType);

            if (isLineType) {
                updateUnitComboBox(DistanceUnit.class);
                if (zoInputField.getText().isEmpty()) {
                    zoInputField.setText("50");
                }
                if (permittivityField.getText().isEmpty()) {
                    permittivityField.setText("4");
                }
            } else if (newType == CircuitElement.ElementType.RESISTOR) {
                updateUnitComboBox(ResistanceUnit.class);
            } else if (newType == CircuitElement.ElementType.CAPACITOR) {
                updateUnitComboBox(CapacitanceUnit.class);
            } else if (newType == CircuitElement.ElementType.INDUCTOR) {
                updateUnitComboBox(InductanceUnit.class);
            }
        });

        zoInputLabel.managedProperty().bind(zoInputLabel.visibleProperty());
        zoInputField.managedProperty().bind(zoInputField.visibleProperty());
        permittivityLabel.managedProperty().bind(permittivityLabel.visibleProperty());
        permittivityField.managedProperty().bind(permittivityField.visibleProperty());
        positionComboBox.managedProperty().bind(positionComboBox.visibleProperty());
        stubComboBox.managedProperty().bind(stubComboBox.visibleProperty());
    }

    /**
     * Populates the unit combo box with the given enum
     *
     * @param unitEnum must be an ElectronicUnit derived enum to make sense
     */
    private void updateUnitComboBox(Class<? extends Enum<?>> unitEnum) {
        unitComboBox.getItems().clear();
        unitComboBox.getItems().addAll(unitEnum.getEnumConstants());
        unitComboBox.getSelectionModel().selectFirst();
    }


    /**
     * Creates the data-bindings between the View (FXML controls) and the ViewModel.
     */
    private void bindViewModel() {
        dataPointsTable.itemsProperty().bind(viewModel.dataPointsProperty());

        labelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        impedanceColumn.setCellValueFactory(cellData -> cellData.getValue().impedanceProperty());
        vswrColumn.setCellValueFactory(cellData -> cellData.getValue().vswrProperty());
        returnLossColumn.setCellValueFactory(cellData -> cellData.getValue().returnLossProperty());
        frequencyColumn.setCellValueFactory(cellData -> {
            double freq = cellData.getValue().frequencyProperty().get();
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(
                    freq,
                    FrequencyUnit.values()
            );
            String display = toDisplay.getValue() + " " + toDisplay.getKey().toString();
            return new javafx.beans.property.SimpleStringProperty(display);
        });
        qualityFactorColumn.setCellValueFactory(cellData -> cellData.getValue().qualityFactorProperty());

        qualityFactorColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                }
            }
        });

        impedanceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Complex item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f + j%.2f Ω", item.real(), item.imag()));
                }
            }
        });

        vswrColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                }
            }
        });

        returnLossColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                }
            }
        });

        loadImpedanceLabel.textProperty().bind(viewModel.loadImpedance.asString());
        freqLabel.textProperty().bind(viewModel.frequencyTextProperty());

        viewModel.measuresGammaProperty().addListener((_, _, _) -> smithInteractionController.redrawSmithCanvas());

    }

    /**
     * Logic needed when we add a component using the keyboard (and not the mouse)
     */
    @FXML
    protected void onAddComponent() {
        try {
            CircuitElement.ElementType type = typeComboBox.getValue();
            double value = Double.parseDouble(valueTextField.getText());

            if (type != CircuitElement.ElementType.LINE) {
                CircuitElement.ElementPosition position = positionComboBox.getValue();
                viewModel.addComponent(type, value * getSelectedUnitFactor(), position);
            } else { //It's a line, we need to get the line's characterstic impedance
                double impValue = Double.parseDouble(zoInputField.getText());
                double permittivityValue = Double.parseDouble(permittivityField.getText());
                Line.StubType stubType = stubComboBox.getValue();

                if (permittivityValue < 1.0) { // La permittivité relative est >= 1
                    DialogUtils.showErrorAlert("Error in input", "Permittivity (εr) must be >= 1.", smithCanvas.getScene().getWindow());
                    return;
                }
                viewModel.addComponent(
                        type,
                        value * getSelectedUnitFactor(),
                        impValue,
                        permittivityValue,
                        null,
                        stubType
                );
            }
        } catch (NumberFormatException e) {
            DialogUtils.showErrorAlert("Invalid value format.", "Please enter a valid numeric value for the component.", smithCanvas.getScene().getWindow());
        }
    }

    private double getSelectedUnitFactor() {
        Enum<?> selectedUnit = unitComboBox.getValue();
        if (selectedUnit instanceof ElectronicUnit) {
            return ((ElectronicUnit) selectedUnit).getFactor();
        }
        return 1.0; // Default factor if the enum isn't right (shouldn't happen)
    }

    /**
     * Set what will be the center point of the chart
     */
    public void setCharacteristicImpedance() {
        var stage = smithCanvas.getScene().getWindow();
        DialogUtils.showDoubleInputDialog("Characteristic Impedance", "Enter Zo (Ohms):", viewModel.zo.get(), stage)
                .ifPresent(zo -> {
                    if (zo > 0) {
                        viewModel.zo.setValue(zo);
                        smithInteractionController.redrawSmithCanvas();
                    } else {
                        DialogUtils.showErrorAlert("Invalid Input", "Zo must be positive.", stage);
                    }
                });
    }

    public void onChangeLoad() {
        var stage = smithCanvas.getScene().getWindow();
        ComplexInputDialog dialog = new ComplexInputDialog("Change Load", viewModel.loadImpedance.get());
        dialog.initOwner(stage);
        dialog.showAndWait()
                .ifPresent(newLoad -> {
                    viewModel.loadImpedance.setValue(newLoad);
                    smithInteractionController.redrawSmithCanvas();
                });
    }

    public void onChangeFreq() {
        var stage = smithCanvas.getScene().getWindow();
        FrequencyInputDialog dialog = new FrequencyInputDialog("Change Frequency", viewModel.frequencyProperty().get());
        dialog.initOwner(stage);
        dialog.showAndWait()
                .ifPresent(newFreq -> {
                    if (newFreq > 0) {
                        viewModel.setFrequency(newFreq);
                    } else {
                        DialogUtils.showErrorAlert("Invalid Input", "Frequency must be a positive value.", stage);
                    }
                });
    }

    @FXML
    private void onZoomIn() {
        smithInteractionController.onZoomIn();
    }

    @FXML
    private void onZoomOut() {
        smithInteractionController.onZoomOut();
    }

    @FXML
    public void onAddComponentMouse() {
        smithInteractionController.onAddComponentMouse();
    }

    @FXML
    private void onReset() {
        smithInteractionController.onReset();
    }

    @FXML
    public void toggleNavButton() {
        boolean isSelected = toggleNavButton.isSelected();
        buttonSmithHBox.setVisible(isSelected);
        buttonSmithHBox.setManaged(isSelected); // Ensures layout adjusts when hidden
    }

    public void importS1P() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(smithCanvas.getScene().getWindow());
        if (selectedFile != null) {
            try {
                List<DataPoint> importedElements = TouchstoneS1P.parse(selectedFile);
                viewModel.addS1PDatapoints(importedElements);
                s1pFileNameField.setText(selectedFile.getName());

                Pair<Double, Double> minMax = importedElements.stream()
                        .collect(Collectors.teeing(
                                Collectors.minBy(Comparator.comparingDouble(DataPoint::getFrequency)),
                                Collectors.maxBy(Comparator.comparingDouble(DataPoint::getFrequency)),
                                (minOpt, maxOpt) -> new Pair<>(
                                        minOpt.map(DataPoint::getFrequency).orElse(0.0),
                                        maxOpt.map(DataPoint::getFrequency).orElse(0.0)
                                )
                        ));

                double minFreq = minMax.getKey();
                double maxFreq = minMax.getValue();

                updateSliderBounds(frequencyRangeSlider1, minFreq, maxFreq);
                updateSliderBounds(frequencyRangeSlider2, minFreq, maxFreq);
                updateSliderBounds(frequencyRangeSlider3, minFreq, maxFreq);

                //Display the S1P controls
                s1pTitledPane.setVisible(true);
                s1pTitledPane.setExpanded(true);
                s1pTitledPane.setManaged(true);

                viewModel.setUseS1PAsLoadF1(useS1PAsLoadCheckBoxF1.isSelected());
                viewModel.setUseS1PAsLoadF2(useS1PAsLoadCheckBoxF2.isSelected());
                viewModel.setUseS1PAsLoadF3(useS1PAsLoadCheckBoxF3.isSelected());

                smithInteractionController.redrawSmithCanvas();
            } catch (IllegalArgumentException e) {
                DialogUtils.showErrorAlert("Can't open file", "Invalid S1P file: " + e.getMessage(), smithCanvas.getScene().getWindow());
            }
        }
    }

    private void updateSliderBounds(RangeSlider slider, double min, double max) {
        slider.setMin(min);
        slider.setMax(max);
        slider.setLowValue(min);
        slider.setHighValue(max);
    }

    public void exportS1P() {
    }

    public void changeS1P() {
        importS1P();
    }

    public void removeS1P() {
        viewModel.clearS1PDatapoints();
        s1pFileNameField.setText("");

        //Hide the S1P controls
        s1pTitledPane.setVisible(false);
        s1pTitledPane.setManaged(false);

        viewModel.setUseS1PAsLoadF1(false); // Doesn't matter which one we disable, will disable the others too

        smithInteractionController.redrawSmithCanvas();
    }

    private void promptEditForComponent(CircuitElement component) {
        var stage = smithCanvas.getScene().getWindow();
        ComponentEditDialog dialog = new ComponentEditDialog(component);
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(newValue -> {
            component.setRealWorldValue(newValue);
            smithInteractionController.redrawSmithCanvas();
            circuitRenderer.render(viewModel);
        });
    }

    public void onSweep() {
        var stage = smithCanvas.getScene().getWindow();

        var lastDataPoint = viewModel.getLastDataPoint();
        if (lastDataPoint == null) {
            DialogUtils.showErrorAlert("Sweep error",
                    "No data point available to base the sweep on. Please add a data point first."
                    , stage);
            return;
        }

        SweepDialog dialog = new SweepDialog(lastDataPoint);
        dialog.initOwner(stage);
        dialog.showAndWait()
                .ifPresent(sweepValues -> {
                    viewModel.performFrequencySweep(sweepValues);
                    sweepManagementTitledPane.setVisible(true);
                    sweepManagementTitledPane.setExpanded(true);
                    sweepManagementTitledPane.setManaged(true);
                });
    }

    public void setDisplayCirclesOptions() {
        var stage = smithCanvas.getScene().getWindow();
        CircleDialog dialog = CircleDialog.getInstance();
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(options -> {
            viewModel.setCircleDisplayOptions(options);
        });
    }

    public void toggleSweepInDataPoints() {
        viewModel.setShowSweepDataPoints(toggleSweepInDataPointsButton.isSelected());
    }

    public void toggleS1PInDataPoints() {
        viewModel.setShowS1PDataPoints(toggleS1PInDataPointsButton.isSelected());
    }

    public void onClearSweep() {
        viewModel.clearSweepPoints();
        sweepManagementTitledPane.setVisible(false);
        sweepManagementTitledPane.setExpanded(false);
        sweepManagementTitledPane.setManaged(false);
    }

    public void onExportSweepToS1P() {
        if (viewModel.sweepDataPointsProperty().isEmpty()) {
            DialogUtils.showErrorAlert("Export error", "No sweep data points to export.", smithCanvas.getScene().getWindow());
            return;
        }
        var stage = smithCanvas.getScene().getWindow();
        FileExportDialog dialog = new FileExportDialog();
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(namefilepair -> {
            try {
                viewModel.exportSweepToS1P(namefilepair.getValue(), namefilepair.getKey());
            } catch (Exception e) {
                Logger.getLogger("Error").log(Level.SEVERE, "Failed to export S1P: " + e.getMessage());
            }
        });
    }

    public void onSweepStartFreqMinus() {
    }

    public void onSweepStartFreqPlus() {
    }

    public void onSweepEndFreqMinus() {
    }

    public void onSweepEndFreqPlus() {
    }

    public void onApplyTuning() {
        viewModel.applyTuningAdjustments();
    }

    public void onCancelTuning() {
        viewModel.cancelTuningAdjustments();
    }

    @FXML
    public void toggle1Filter() {
        boolean selected = toggle1FilterButton.isSelected();
        toggle2FilterButton.setSelected(false);
        toggle3FilterButton.setSelected(false);
        filter1Box.setVisible(selected);
        filter1Box.setManaged(selected);
        filter2Box.setVisible(false);
        filter2Box.setManaged(false);
        filter3Box.setVisible(false);
        filter3Box.setManaged(false);
    }

    @FXML
    public void toggle2Filter() {
        boolean selected = toggle2FilterButton.isSelected();
        toggle1FilterButton.setSelected(false);
        toggle3FilterButton.setSelected(false);
        filter1Box.setVisible(selected);
        filter1Box.setManaged(selected);
        filter2Box.setVisible(selected);
        filter2Box.setManaged(selected);
        filter3Box.setVisible(false);
        filter3Box.setManaged(false);
    }

    @FXML
    public void toggle3Filter() {
        boolean selected = toggle3FilterButton.isSelected();
        toggle1FilterButton.setSelected(false);
        toggle2FilterButton.setSelected(false);
        filter1Box.setVisible(selected);
        filter1Box.setManaged(selected);
        filter2Box.setVisible(selected);
        filter2Box.setManaged(selected);
        filter3Box.setVisible(selected);
        filter3Box.setManaged(selected);
    }

    @FXML
    public void setOnQualityFactorEnable() {
    }
}

