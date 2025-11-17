package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogFactory;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

import java.util.List;


public class SmithController {

    //General important vars
    private final double thickLineValue = 1;
    private final double thinLineValue = 0.4;
    private Font LABEL_FONT = new Font("Arial", 10);

    //Mouse Add related vars
    private Complex startGammaForMouseAdd;
    private Complex startImpedanceForMouseAdd;
    private Complex snappedGammaForMouseAdd;
    private Complex circleCenterForMouseAdd;
    private boolean isAddingMouseComponent = false;
    private double circleRadiusForMouseAdd;
    private double startAngleForMouseAdd;
    private int directionMultiplier;
    private Double previousAngle;
    private Double allowedAngleTravel;
    private Double totalAngleTraveled;

    private boolean isProgrammaticallyMovingCursor = false;

    // --- FXML Fields ---
    @FXML
    public Button addMouseButton;
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
    private TableView<DataPoint> dataPointsTable;
    @FXML
    private ComboBox<CircuitElement.ElementType> typeComboBox; // Use ElementType enum later
    @FXML
    private ComboBox<CircuitElement.ElementPosition> positionComboBox; // Use ElementPosition enum later
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

    //Viewmodel
    private SmithChartViewModel viewModel;

    //View State for zooming and panning
    private double currentScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;


    /**
     * This method is called by the FXMLLoader after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        this.viewModel = new SmithChartViewModel();
        setupResizableCanvas();
        setupControls();
        bindViewModel();
        redrawCanvas(); //Initial chart drawing

        dataPointsTable.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> redrawCanvas());

        //Bindings to display mouse related informations
        returnLossLabel.textProperty().bind(viewModel.mouseReturnLossTextProperty());
        vswrLabel.textProperty().bind(viewModel.mouseVSWRTextProperty());
        qLabel.textProperty().bind(viewModel.mouseQualityFactorTextProperty());
        gammaLabel.textProperty().bind(viewModel.mouseGammaTextProperty());
        yLabel.textProperty().bind(viewModel.mouseAdmittanceYTextProperty());
        zLabel.textProperty().bind(viewModel.mouseImpedanceZTextProperty());

        //Enable editing of the values by double-clicking on them in the display point
        dataPointsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = dataPointsTable.getSelectionModel().getSelectedIndex();

                // if index <= 0, then it's either no selection or selecting the load value
                if (selectedIndex > 0) {
                    CircuitElement component = viewModel.circuitElements.get(selectedIndex - 1);

                    // Call the Factory
                    DialogFactory.showComponentEditDialog(component).ifPresent(newValue -> {
                        component.setRealWorldValue(newValue);
                        redrawCanvas();
                    });
                }
            }
        });

        //Add the delete button in the factory
        deleteColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("X");

            {
                deleteButton.setOnAction(_ -> {
                    viewModel.removeComponentAt(getIndex() - 1); //-1 since the load is in a different container
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 1)  {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                    setAlignment(Pos.CENTER);
                }
            }
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

    private void setupResizableCanvas() {
        // Bind the canvas size to the size of its parent pane
        smithCanvas.widthProperty().bind(smithChartPane.widthProperty());
        smithCanvas.heightProperty().bind(smithChartPane.heightProperty());

        // Add listeners to redraw the chart and update font size whenever the size changes
        smithCanvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateFontSize();
            redrawCanvas();
        });
        smithCanvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateFontSize();
            redrawCanvas();
        });

        smithCanvas.setOnScroll(event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double zoomFactor = 1.1;

            double deltaY = event.getDeltaY(); //Check if it's zoom in or zoom out

            if (deltaY > 0) {
                currentScale *= zoomFactor;
            } else {
                currentScale /= zoomFactor;
            }

            offsetX = mouseX - (mouseX - offsetX) * (deltaY > 0 ? zoomFactor : 1 / zoomFactor);
            offsetY = mouseY - (mouseY - offsetY) * (deltaY > 0 ? zoomFactor : 1 / zoomFactor);

            redrawCanvas();
            event.consume();
        });

        // Get the coordinates where we start the panning
        smithCanvas.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });

        // Drag the view by calculating the delta from the last position
        smithCanvas.setOnMouseDragged(event -> {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;

            offsetX += deltaX;
            offsetY += deltaY;

            // Update the last position for the next drag event
            lastMouseX = event.getX();
            lastMouseY = event.getY();

            redrawCanvas();
        });

        // Stuff that happens when we click on the chart
        smithCanvas.setOnMouseClicked(event -> {
            if (isAddingMouseComponent){ //If we're adding a component and click, adds the component
                finalizeMouseAddComponent();
                event.consume(); //Consumes it so it's not used for other stuff
                return;
            }

            if (event.getClickCount() == 2) {
                // Reset view on double-click
                currentScale = 1.0;
                offsetX = 0.0;
                offsetY = 0.0;
                redrawCanvas();
            }
        });


        smithCanvas.setOnMouseMoved(event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();

            // Get the center of the Smith chart
            double centerX = smithCanvas.getWidth() / 2;
            double centerY = smithCanvas.getHeight() / 2;
            double mainRadius = Math.min(centerX, centerY) - 10;

            // Avoid division by zero if the canvas is too small to draw anything
            if (mainRadius <= 0) {
                return;
            }

            // Calculate the coordinates relative to the Smith chart
            double logicalX = (mouseX - offsetX) / currentScale;
            double logicalY = (mouseY - offsetY) / currentScale;

            double relativeX = logicalX - centerX;
            double relativeY = logicalY - centerY;

            double gammaX = relativeX / mainRadius;
            double gammaY = -relativeY / mainRadius;

            if (isAddingMouseComponent){
                if (isProgrammaticallyMovingCursor) {
                    isProgrammaticallyMovingCursor = false;
                    return;
                }
                handleMouseMagnetization(new Complex(gammaX, gammaY));
            }

            // Pass the correct Gamma coordinates to the ViewModel.
            viewModel.calculateMouseInformations(gammaX, gammaY);

        });

        smithCanvas.setOnKeyPressed(event -> {
            // Quit the component add event if we hit ESC
            if (isAddingMouseComponent && event.getCode() == KeyCode.ESCAPE) {
                cancelMouseAddComponent();
            }
        });

    }

    /**
     * Handles the real-time snapping of the mouse to the component path.
     * All setup calculations are assumed to have been done already.
     *
     * @param rawMouseGamma The unprocessed gamma from the mouse cursor's position.
     */
    private void handleMouseMagnetization(Complex rawMouseGamma) {
        // Get the current angle of the mouse relative to the center of the circle it will follow
        Complex mouseVector = rawMouseGamma.subtract(circleCenterForMouseAdd);
        double mouseAngle = mouseVector.angle();

        // On the very first movement, initialize previousAngle
        if (previousAngle == null) {
            previousAngle = startAngleForMouseAdd;
        }

        // Calculate the incremental angle change and normalize it
        double angleDifference = mouseAngle - previousAngle;
        while (angleDifference <= -Math.PI) angleDifference += 2 * Math.PI;
        while (angleDifference > Math.PI)  angleDifference -= 2 * Math.PI;

        //Movement logic for Capacitors and Inductors
        if (typeComboBox.getValue() != CircuitElement.ElementType.RESISTOR) {
            // Convert angle change to travel distance.
            // This is positive for correct movement, negative for incorrect.
            double incrementalTravel = angleDifference * directionMultiplier;

            // Update the total angle traveled
            double newTotalAngleTraveled = totalAngleTraveled + incrementalTravel;

            if (newTotalAngleTraveled < 0) {
                // Moved backward past the start, snap back to the start
                mouseAngle = startAngleForMouseAdd;
                totalAngleTraveled = 0.0;
            } else if (newTotalAngleTraveled > allowedAngleTravel) {
                // Moved forward past the end, revert to the previous valid position
                mouseAngle = previousAngle;
                // totalAngleTraveled is not updated because the move was rejected
            } else {
                // Movement is valid, update the total travel distance
                totalAngleTraveled = newTotalAngleTraveled;
            }
        }

        // Calculate the new snapped gamma based on the corrected angle
        snappedGammaForMouseAdd = circleCenterForMouseAdd.add(
                new Complex(Math.cos(mouseAngle), Math.sin(mouseAngle)).multiply(circleRadiusForMouseAdd)
        );

        // Store the final angle for the next frame's calculation
        previousAngle = mouseAngle;

        // Calculate the value of the component for displaying
        Double liveValue = calculateComponentValue(
                snappedGammaForMouseAdd,
                startImpedanceForMouseAdd,
                typeComboBox.getValue(),
                positionComboBox.getValue(),
                viewModel.zo.get(),
                viewModel.frequency.get()
        );

        // Display the value correctly of the graphically measured component
        if (liveValue != null) {
            Pair<ElectronicUnit, String> result = SmithUtilities.getBestUnitAndFormattedValue(
                    liveValue,
                    unitComboBox.getItems().toArray(new ElectronicUnit[0])
            );
            valueTextField.setText(result.getValue());
            unitComboBox.getSelectionModel().select((Enum<?>) result.getKey());
        } else {
            valueTextField.setText("");
        }

        moveCursorToGamma(snappedGammaForMouseAdd);
    }

    /**
     * Calculates the value of the component we're plotting on the chart
     * @param gamma position of the component on the chart
     * @param startImpedance where the last component was
     * @param type of the component (resistor, capacitor, etc.)
     * @param position if the component is in series or parallel
     * @param z0 the base impedance
     * @param frequency of the circuit
     * @return the calculated value of the component
     */
    private Double calculateComponentValue(Complex gamma,
                                           Complex startImpedance,
                                           CircuitElement.ElementType type,
                                           CircuitElement.ElementPosition position,
                                           double z0,
                                           double frequency) {
        final double EPS = 1e-12;

        if (gamma == null || startImpedance == null) return null;
        if (!(Double.isFinite(z0) && z0 > 0)) return null;
        if (!(Double.isFinite(frequency) && frequency > 0)) return null;

        double omega = 2.0 * Math.PI * frequency;
        Complex one = new Complex(1, 0);

        // Protect against division by (1 - gamma) ~= 0
        Complex denom = one.subtract(gamma);
        if (Math.hypot(denom.real(), denom.imag()) < EPS) return null;

        Complex zNormFinal = one.add(gamma).dividedBy(denom);
        Complex finalImpedance = zNormFinal.multiply(z0);

        double componentValue;

        if (position == CircuitElement.ElementPosition.SERIES) {
            Complex addedImpedance = finalImpedance.subtract(startImpedance);
            double imagZ = addedImpedance.imag();

            if (type == CircuitElement.ElementType.INDUCTOR) {
                componentValue = imagZ / omega; // L = Im(Z) / ω
            } else if (type == CircuitElement.ElementType.CAPACITOR) {
                if (Math.abs(imagZ) < EPS) return null;
                componentValue = -1.0 / (imagZ * omega); // C = -1/(Im(Z)*ω)
            } else {
                return null; // TODO: HANDLE THE OTHER POSSIBLE COMPONENTS
            }
        } else { // PARALLEL
            if (Math.hypot(startImpedance.real(), startImpedance.imag()) < EPS ||
                    Math.hypot(finalImpedance.real(), finalImpedance.imag()) < EPS) {
                return null;
            }

            Complex startY = one.dividedBy(startImpedance);
            Complex finalY = one.dividedBy(finalImpedance);
            Complex addedY = finalY.subtract(startY);
            double imagY = addedY.imag();

            if (type == CircuitElement.ElementType.INDUCTOR) {
                if (Math.abs(imagY) < EPS) return null;
                componentValue = -1.0 / (imagY * omega); // L = -1/(Im(Y)*ω)
            } else if (type == CircuitElement.ElementType.CAPACITOR) {
                componentValue = imagY / omega; // C = Im(Y)/ω
            } else {
                return null; // TODO: HANDLE THE OTHER POSSIBLE COMPONENTS
            }
        }

        if (!Double.isFinite(componentValue) || componentValue <= 0.0) return null;
        return componentValue;
    }

    /**
     * Calculates the final component value and adds it to the ViewModel.
     */
    private void finalizeMouseAddComponent() {
        double z0 = viewModel.zo.get();
        double freq = viewModel.frequency.get();

        CircuitElement.ElementType type = typeComboBox.getValue();
        CircuitElement.ElementPosition position = positionComboBox.getValue();

        Double componentValue = calculateComponentValue(
                snappedGammaForMouseAdd,
                startImpedanceForMouseAdd,
                type,
                position,
                z0,
                freq
        );

        if (componentValue != null) {
            viewModel.addComponent(type, componentValue, position);
        }

        resetMouseAddComponentState();
    }

    /**
     * Resets the state and UI after finishing or canceling the operation.
     */
    private void cancelMouseAddComponent() {
        resetMouseAddComponentState();
        redrawCanvas();
    }

    /**
     * Resets every variables and elements related to the mouse add component event
     */
    private void resetMouseAddComponentState() {
        isAddingMouseComponent = false;
        startGammaForMouseAdd = null;
        startImpedanceForMouseAdd = null;
        addMouseButton.setText("Add with Mouse");
        dataPointsTable.setMouseTransparent(false);
        circleCenterForMouseAdd = null;
        circleRadiusForMouseAdd = 0.0;
        startAngleForMouseAdd = 0.0;
        directionMultiplier = 0;
        previousAngle = null;
        allowedAngleTravel = null;
        totalAngleTraveled = null;
    }

    /**
     * Updates the font size for the Smith chart labels based on the canvas size.
     */
    private void updateFontSize() {
        double newFontSize = Math.min(smithCanvas.getWidth(), smithCanvas.getHeight()) / 60;
        LABEL_FONT = new Font("Arial", newFontSize);
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

        typeComboBox.valueProperty().addListener((ChangeListener<Enum<?>>) (_, _, _) -> {
            CircuitElement.ElementType selectedType = typeComboBox.getValue();
            switch (selectedType) {
                case CircuitElement.ElementType.RESISTOR -> updateUnitComboBox(ResistanceUnit.class);
                case CircuitElement.ElementType.CAPACITOR -> updateUnitComboBox(CapacitanceUnit.class);
                case CircuitElement.ElementType.INDUCTOR -> updateUnitComboBox(InductanceUnit.class);
                default -> unitComboBox.getItems().clear();
            }
        });


    }

    /**
     * Populates the unit combo box with the given enum
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
        freqLabel.textProperty().bind(viewModel.frequencyProperty());

        viewModel.measuresGammaProperty().addListener((_, _, _) -> redrawCanvas());

    }

    /**
     * Logic needed when we add a component using the keyboard (and not the mouse)
     */
    @FXML
    protected void onAddComponent() {
        try {
            CircuitElement.ElementType type = typeComboBox.getValue();
            CircuitElement.ElementPosition position = positionComboBox.getValue();
            double value = Double.parseDouble(valueTextField.getText());
            viewModel.addComponent(type, value * getSelectedUnitFactor(), position);
        } catch (NumberFormatException e) {
            showError("Invalid value format.");
        }
    }

    private double getSelectedUnitFactor() {
        Enum<?> selectedUnit = unitComboBox.getValue();
        if (selectedUnit instanceof ElectronicUnit) {
            return ((ElectronicUnit) selectedUnit).getFactor();
        }
        return 1.0; // Default factor if the enum isn't right (shouldn't happen)
    }


    // --- Drawing Logic ---
    // This logic is called by the initialize() method and the resize listeners.

    /**
     * Clears and redraws the entire canvas. This method will be called whenever the data
     * or the window size changes.
     */
    private void redrawCanvas() {
        GraphicsContext gc = smithCanvas.getGraphicsContext2D();

        gc.save();

        // Clear the canvas before redrawing
        gc.clearRect(0, 0, smithCanvas.getWidth(), smithCanvas.getHeight());

        gc.translate(offsetX, offsetY);
        gc.scale(currentScale, currentScale);

        // Draw the static parts of the chart
        drawSmithGrid(gc);
        // Draw the path between the impedances
        drawImpedancePath(gc);
        // Draw the impedances
        drawImpedancePoints(gc);

        gc.restore();
    }

    /**
     * Draws the static background grid of the Smith Chart.
     *
     * @param gc The GraphicsContext of the canvas.
     */
    private void drawSmithGrid(GraphicsContext gc) {
        double width = smithCanvas.getWidth();
        double height = smithCanvas.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double mainRadius = Math.min(centerX, centerY) - 10;

        //Save the current graphics state and apply clipping so the lines don't get out of the circle of the chart
        gc.save();
        gc.beginPath();
        gc.arc(centerX, centerY, mainRadius, mainRadius, 0, 360);
        gc.closePath();
        gc.clip(); // Anything drawn after this will be clipped to the circle

        gc.setLineWidth(1);

        // Draw the Outer Circle (r=0, g=0)
        gc.setStroke(Color.GRAY);
        gc.strokeOval(centerX - mainRadius, centerY - mainRadius, mainRadius * 2, mainRadius * 2);

        // Draw the Horizontal Line (x=0, b=0)
        gc.strokeLine(centerX - mainRadius, centerY, centerX + mainRadius, centerY);

        double[] stepValues = {0.2, 0.5, 1.0, 2.0, 4.0, 10.0};

        // ADMITTANCE (Y) GRID

        // Draw Constant Conductance (g) Circles
        gc.setLineWidth(thinLineValue);
        gc.setStroke(Color.CORNFLOWERBLUE);
        for (double g : stepValues) {
            double circleRadius = mainRadius / (g + 1);
            double circleCenterX = centerX - mainRadius * g / (g + 1);
            if (g == 1) gc.setLineWidth(thickLineValue); //If it's the circle that leads to the center of the chart, making the line thicker
            gc.strokeOval(circleCenterX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
            if (g == 1) gc.setLineWidth(thinLineValue);

            drawLabel(gc, String.format("%.1f mS", g / viewModel.zo.get() * 1000), circleCenterX, centerY - circleRadius, Color.WHITE);
        }

        // Draw Constant Susceptance (b) Arcs
        gc.setStroke(Color.DARKGREEN); // New color for susceptance
        for (double b : stepValues) {
            double arcRadius = mainRadius / b;
            double arcCenterX = centerX - mainRadius;

            // Positive Susceptance Arcs (upper half)
            double arcCenterY = centerY - arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);

            // Negative Susceptance Arcs (lower half)
            arcCenterY = centerY + arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);
        }

        // IMPEDANCE (X) GRID
        // Draw Constant Resistance (r) Circles
        gc.setStroke(Color.CORAL); // Color for resistance
        for (double r : stepValues) {
            double circleRadius = mainRadius / (r + 1);
            double circleCenterX = centerX + mainRadius * r / (r + 1);
            if (r == 1) gc.setLineWidth(thickLineValue); //Same logic than for the admittance
            gc.strokeOval(circleCenterX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
            if (r == 1) gc.setLineWidth(thinLineValue);

            drawLabel(gc, String.format("%.1f", r * viewModel.zo.get()), circleCenterX, centerY + circleRadius, Color.WHITE);
        }

        // Draw Constant Reactance (x) Arcs
        gc.setStroke(Color.BROWN); // Color for reactance
        for (double x : stepValues) {
            double arcRadius = mainRadius / x;
            // Positive Reactance Arcs (upper half)
            double arcCenterX = centerX + mainRadius;
            double arcCenterY = centerY - arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);

            // Negative Reactance Arcs (lower half)
            arcCenterY = centerY + arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);
        }


        //Restore the graphics state to remove the clipping
        gc.restore();

        //Draw the labels on the circles reprensenting the values
        for (double b : stepValues) {
            double angle = 2 * Math.atan(1.0 / b);
            double labelX = centerX + mainRadius * Math.cos(Math.PI - angle);
            double labelY = centerY + mainRadius * Math.sin(Math.PI - angle);
            String label = String.format("+%.1f", viewModel.zo.get() / b);
            drawLabel(gc, label, labelX, labelY, Color.DARKGREEN);

        }

        for (double x : stepValues) {
            double angle = 2 * Math.atan(1.0 / x);
            double labelX = centerX + mainRadius * Math.cos(-angle);
            double labelY = centerY + mainRadius * Math.sin(-angle);
            String label = String.format("%.1f", x * viewModel.zo.get());
            drawLabel(gc, label, labelX, labelY, Color.BROWN);
        }


    }

    /**
     * Draw impedance points based on the gammas calculated in the viewModel
     *
     * @param gc the graphic context on which we'll draw the points
     */
    private void drawImpedancePoints(GraphicsContext gc) {
        List<Complex> gammas = viewModel.measuresGammaProperty().get();

        if (gammas != null) {
            int index = 0;

            //Get the necessary information about the canvas
            double width = smithCanvas.getWidth();
            double height = smithCanvas.getHeight();
            double centerX = width / 2;
            double centerY = height / 2;
            double mainRadius = Math.min(centerX, centerY) - 10;

            //Draw each impedance on the chart
            for (Complex gamma : gammas) {

                double pointX = centerX + gamma.real() * mainRadius;
                double pointY = centerY - gamma.imag() * mainRadius;

                gc.setLineWidth(2 * thickLineValue);
                double pointSize = 5;

                //Change the color of the selected position on the chart
                int selectedItemIndex = dataPointsTable.getSelectionModel().getSelectedIndex();
                if (selectedItemIndex == index) {
                    gc.setStroke(Color.CORAL);
                    gc.setFill(Color.CORAL);
                } else {
                    gc.setStroke(Color.YELLOW);
                    gc.setFill(Color.YELLOW);
                }

                //Draw the point
                gc.strokeRect(pointX - pointSize / 2, pointY - pointSize / 2, pointSize, pointSize);

                ++index;
            }
        }
    }

    /**
     * A helper utility to draw a text label with a clean background.
     * It clears a small circular area behind the text to ensure readability.
     *
     * @param gc    The graphics context.
     * @param text  The string to draw.
     * @param x     The center x-coordinate for the text.
     * @param y     The center y-coordinate for the text.
     * @param color The color of the text.
     */
    private void drawLabel(GraphicsContext gc, String text, double x, double y, Color color) {
        gc.setFont(LABEL_FONT);
        gc.setTextAlign(TextAlignment.CENTER);

        // Calculate the rectangle size based on the font size
        double fontSize = LABEL_FONT.getSize();
        double rectWidth = text.length() * fontSize * 0.6; // Approximate width per character
        double rectHeight = fontSize * 1.2; // Slightly larger than the font size for padding

        // Clear a rectangle behind the text for readability
        gc.clearRect(x - rectWidth / 2, y - rectHeight / 2, rectWidth, rectHeight);

        // Draw the text
        gc.setFill(color);
        gc.fillText(text, x, y + fontSize / 3);
    }

    /**
     * Draws the impedance path on the chart based on the circuit elements.
     *
     * @param gc   The GraphicsContext of the canvas.
     */
    private void drawImpedancePath(GraphicsContext gc) {
        List<Complex> gammas = viewModel.measuresGammaProperty().get();
        if (gammas == null || gammas.size() < 2) return;

        double width = smithCanvas.getWidth();
        double height = smithCanvas.getHeight();
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double mainRadius = Math.min(centerX, centerY) - 10;

        // Clip to the Smith circle so lines don't spill outside
        gc.save();
        gc.beginPath();
        gc.arc(centerX, centerY, mainRadius, mainRadius, 0, 360);
        gc.closePath();
        gc.clip();

        gc.setStroke(Color.RED);
        gc.setLineWidth(2);

        Complex previousGamma = gammas.getFirst();

        for (int i = 1; i < gammas.size(); i++) {
            Complex currGamma = gammas.get(i);

            // For each transition, determine the arc's circle center
            CircuitElement element = viewModel.circuitElements.get(i - 1);
            Complex arcCenter = getArcCenter(previousGamma, element);

            // Calculate arc radius in canvas coordinates
            Complex radiusVector = previousGamma.subtract(arcCenter);
            double arcRadius = radiusVector.abs() * mainRadius;

            // Convert arc center to canvas coordinates
            double arcCenterX = centerX + arcCenter.real() * mainRadius;
            double arcCenterY = centerY - arcCenter.imag() * mainRadius;

            // Calculate start and end angles
            double startAngle = Math.toDegrees(Math.atan2(
                    previousGamma.imag() - arcCenter.imag(),
                    previousGamma.real() - arcCenter.real()
            ));

            double endAngle = Math.toDegrees(Math.atan2(
                    currGamma.imag() - arcCenter.imag(),
                    currGamma.real() - arcCenter.real()
            ));

            // Calculate arc extent (angle swept)
            double arcExtent = endAngle - startAngle;

            // Normalize to sweep in the correct direction
            if (arcExtent > 180) arcExtent -= 360;
            if (arcExtent < -180) arcExtent += 360;

            // Draw the arc
            gc.strokeArc(
                    arcCenterX - arcRadius,
                    arcCenterY - arcRadius,
                    arcRadius * 2,
                    arcRadius * 2,
                    startAngle,
                    arcExtent,
                    javafx.scene.shape.ArcType.OPEN
            );

            previousGamma = currGamma;
        }

        gc.restore();
    }

    private Complex getArcCenter(Complex gamma, CircuitElement element) {
        Complex impedance = SmithUtilities.gammaToImpedance(gamma, viewModel.zo.get());

        if (element.getPosition() == CircuitElement.ElementPosition.SERIES) {
            double r = impedance.real() / viewModel.zo.get();
            return new Complex(r / (r + 1), 0);
        } else { // PARALLEL
            Complex admittance = new Complex(viewModel.zo.get(), 0).dividedBy(impedance);
            double g = admittance.real();
            return new Complex(-g / (g + 1), 0);
        }
    }

    /**
     * Set what will be the center point of the chart
     * @param actionEvent unused here
     */
    public void setCharacteristicImpedance(ActionEvent actionEvent) {
        DialogFactory.showDoubleInputDialog("Characteristic Impedance", "Enter Zo (Ohms):", viewModel.zo.get())
                .ifPresent(zo -> {
                    if (zo > 0) {
                        viewModel.zo.setValue(zo);
                        redrawCanvas();
                    } else {
                        DialogFactory.showErrorAlert("Invalid Input", "Zo must be positive.");
                    }
                });
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Input Error");
        alert.showAndWait();
    }

    public void onChangeLoad(ActionEvent actionEvent) {
        DialogFactory.showComplexInputDialog("Change Load", viewModel.loadImpedance.get())
                .ifPresent(newLoad -> {
                    viewModel.loadImpedance.setValue(newLoad);
                    redrawCanvas();
                });
    }

    public void onChangeFreq(ActionEvent actionEvent) {
        DialogFactory.showFrequencyInputDialog("Change Frequency", viewModel.frequency.get())
                .ifPresent(newFreq -> {
                    if (newFreq > 0) {
                        viewModel.frequency.setValue(newFreq);
                    } else {
                        DialogFactory.showErrorAlert("Invalid Input", "Frequency must be a positive value.");
                    }
                });
    }

    @FXML
    private void onZoomIn() {
        offsetX = 0;
        offsetY = 0;
        currentScale *= 1.1;

        redrawCanvas();
    }

    @FXML
    private void onZoomOut() {
        offsetX = 0;
        offsetY = 0;
        currentScale /= 1.1;
        redrawCanvas();
    }

    @FXML
    private void onReset() {
        currentScale = 1.0;
        offsetX = 0.0;
        offsetY = 0.0;
        redrawCanvas();
    }

    @FXML
    public void toggleNavButton(ActionEvent actionEvent) {
        boolean isSelected = toggleNavButton.isSelected();
        buttonSmithHBox.setVisible(isSelected);
        buttonSmithHBox.setManaged(isSelected); // Ensures layout adjusts when hidden
    }

    @FXML
    public void onAddComponentMouse(ActionEvent actionEvent) {

        //If we click on it again, exit
        if (isAddingMouseComponent) {
            cancelMouseAddComponent();
            return;
        }

        Complex lastGamma = viewModel.getLastGamma();
        if (lastGamma == null) {
            DialogFactory.showErrorAlert("Operation Failed", "Cannot add a component without a starting point.");
            return;
        }

        //Entering mouse mode
        isAddingMouseComponent = true;
        startGammaForMouseAdd = lastGamma;
        startImpedanceForMouseAdd = viewModel.getLastImpedance();
        snappedGammaForMouseAdd = lastGamma;

        addMouseButton.setText("Cancel (ESC)");
        dataPointsTable.setMouseTransparent(true);

        //Get the element's particularities
        CircuitElement.ElementType type = typeComboBox.getValue();
        CircuitElement.ElementPosition position = positionComboBox.getValue();
        double z0 = viewModel.zo.get();

        //Get direction (counter-clockwise or clockwise)
        int baseDirection = (type == CircuitElement.ElementType.CAPACITOR) ? 1 : -1; // CCW for Cap, CW for Inductor
        directionMultiplier = (position == CircuitElement.ElementPosition.SERIES) ? baseDirection : -baseDirection;

        //Get the circle the mouse will follow when adding component
        if (position == CircuitElement.ElementPosition.SERIES) {
            Complex normalizedStart = startImpedanceForMouseAdd.dividedBy(z0);
            double r = normalizedStart.real();
            circleCenterForMouseAdd = new Complex(r / (r + 1), 0);
            circleRadiusForMouseAdd = 1 / (r + 1);
        } else { // PARALLEL
            Complex startAdmittance = new Complex(z0, 0).dividedBy(startImpedanceForMouseAdd);
            double g = startAdmittance.real();
            circleCenterForMouseAdd = new Complex(-g / (g + 1), 0);
            circleRadiusForMouseAdd = 1 / (g + 1);
        }

        //Get the angle from which the last gamma is to the circle that we're following
        Complex startVector = startGammaForMouseAdd.subtract(circleCenterForMouseAdd);
        startAngleForMouseAdd = startVector.angle();

        //We want to only able to move until we arrive to either extremities of the X axis
        Complex targetGamma = (position == CircuitElement.ElementPosition.SERIES)
                ? new Complex(1, 0)  // Open Circuit
                : new Complex(-1, 0); // Short Circuit

        //Compute how much the user can move in angle
        Complex targetVector = targetGamma.subtract(circleCenterForMouseAdd);
        double endAngle = targetVector.angle();
        double travel = endAngle - startAngleForMouseAdd;

        if (directionMultiplier == 1) { // For CCW, we want the positive angle difference
            while (travel <= 0) travel += 2 * Math.PI;
        } else { // For CW, we want the negative angle difference, the abs it
            while (travel >= 0) travel -= 2 * Math.PI;
            travel = Math.abs(travel);
        }
        allowedAngleTravel = travel - 0.001; // Epsilon for floating-point safety

        //Initialize state variables
        previousAngle = null;
        totalAngleTraveled = 0.0;

        moveCursorToGamma(startGammaForMouseAdd);
        smithCanvas.requestFocus();
    }

    /**
     * Move the cursor to a specified reflection coefficient on the chart
     *
     * @param gamma to move to
     */
    private void moveCursorToGamma(Complex gamma) {
        // Get the basic properties of the canvas
        double centerX = smithCanvas.getWidth() / 2;
        double centerY = smithCanvas.getHeight() / 2;
        double mainRadius = Math.min(centerX, centerY) - 10;

        // Calculate the point's position on a non-zoomed, non-panned chart
        double baseCanvasX = centerX + gamma.real() * mainRadius;
        double baseCanvasY = centerY - gamma.imag() * mainRadius;

        // Now, apply the current zoom and pan to find where it is visually
        double finalCanvasX = (baseCanvasX * currentScale) + offsetX;
        double finalCanvasY = (baseCanvasY * currentScale) + offsetY;

        // This finds the screen position of the top-left corner of the canvas
        Point2D canvasScreenPos = smithCanvas.localToScreen(0, 0);

        // We add our calculated canvas offsets to get the final screen position.
        int screenX = (int) (canvasScreenPos.getX() + finalCanvasX);
        int screenY = (int) (canvasScreenPos.getY() + finalCanvasY);

        // Move the cursor to the final screen position
        moveCursor(screenX, screenY);
    }


    /**
     * Move the mouse to the specific screen position
     *
     * @param screenX the position where the mouse will go (X)
     * @param screenY the position where the mouse will go (Y)
     */
    private void moveCursor(int screenX, int screenY) {
        Platform.runLater(() -> {
            isProgrammaticallyMovingCursor = true;
            Robot robot = new Robot();
            robot.mouseMove(screenX, screenY);
        });
    }
}


