package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;


public class SmithController {


    // --- Important values ---
    private final double thickLineValue = 1;
    private final double thinLineValue = 0.4;
    private Font LABEL_FONT = new Font("Arial", 10);

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


    // --- ViewModel ---
    // The ViewModel is the brain of our UI. The controller just talks to it.
    private SmithChartViewModel viewModel; // TODO: Replace with your actual ViewModel
    // --- View State for Zoom and Pan ---
    private double currentScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    // For panning
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;

    // For mouse information
    private final double chartX = 0.0;
    private final double chartY = 0.0;

    /**
     * This method is called by the FXMLLoader after the FXML file has been loaded.
     * It's the perfect place to set up bindings and initialize the view.
     */
    @FXML
    public void initialize() {
        // 1. Instantiate the ViewModel
        this.viewModel = new SmithChartViewModel(); // TODO: Create this class

        // 2. Make the Canvas Resizable (Crucial for a good UX)
        setupResizableCanvas();

        // 3. Populate UI Controls with Data
        setupControls();

        // 4. Bind UI Components to ViewModel Properties (The heart of MVVM)
        bindViewModel();

        // 5. Perform the initial draw
        redrawCanvas();

        // Add a listener to track the selected item
        dataPointsTable.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> redrawCanvas());

        //Listen to mouse information changes
        returnLossLabel.textProperty().bind(viewModel.mouseReturnLossTextProperty());
        vswrLabel.textProperty().bind(viewModel.mouseVSWRTextProperty());
        qLabel.textProperty().bind(viewModel.mouseQualityFactorTextProperty());
        gammaLabel.textProperty().bind(viewModel.mouseGammaTextProperty());
        yLabel.textProperty().bind(viewModel.mouseAdmittanceYTextProperty());
        zLabel.textProperty().bind(viewModel.mouseImpedanceZTextProperty());

        //Enable editing of the values by double-clicking on them in the display point
        dataPointsTable.setOnMouseClicked(event -> {
            //Double click to edit the value
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

            double deltaY = event.getDeltaY();

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

        smithCanvas.setOnMouseClicked(event -> {
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
            double gammaY = -relativeY / mainRadius; // Invert Y-axis for standard mathematical representation

            // Pass the correct Gamma coordinates to the ViewModel.
            viewModel.calculateMouseInformations(gammaX, gammaY);
        });

    }

    /**
     * Updates the font size for the Smith chart labels based on the canvas size.
     */
    private void updateFontSize() {
        double newFontSize = Math.min(smithCanvas.getWidth(), smithCanvas.getHeight()) / 60; // Adjust divisor as needed
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

    private void updateUnitComboBox(Class<? extends Enum<?>> unitEnum) {
        unitComboBox.getItems().clear();
        unitComboBox.getItems().addAll(unitEnum.getEnumConstants());
        unitComboBox.getSelectionModel().selectFirst();
    }


    /**
     * Creates the data-bindings between the View (FXML controls) and the ViewModel.
     */
    private void bindViewModel() {
        // TODO: Uncomment and adapt these lines when your ViewModel is ready.

        dataPointsTable.itemsProperty().bind(viewModel.dataPointsProperty());

        // 2. Link each column to a property in the DataPoint model class
        labelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        impedanceColumn.setCellValueFactory(cellData -> cellData.getValue().impedanceProperty());
        vswrColumn.setCellValueFactory(cellData -> cellData.getValue().vswrProperty());
        returnLossColumn.setCellValueFactory(cellData -> cellData.getValue().returnLossProperty());

        // 3. (Optional but Recommended) Add custom formatting for numbers and complex values
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

        vswrColumn.setCellFactory(column -> new TableCell<>() {
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
        freqLabel.textProperty().bind(Bindings.createStringBinding(() -> { //TODO FIX THIS AND PUT IT IN THE VIEWMODEL
            double freq = viewModel.frequency.get();
            return switch ((int) Math.log10(freq)) {
                case 0, 1, 2 -> String.format("%.2f Hz", freq);
                case 3, 4, 5 -> String.format("%.2f kHz", freq / 1_000);
                case 6, 7, 8 -> String.format("%.2f MHz", freq / 1_000_000);
                default -> String.format("%.2f GHz", freq / 1_000_000_000);
            };
        }, viewModel.frequency));

        viewModel.measuresGammaProperty().addListener((_, _, _) -> redrawCanvas());

    }

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
        // Draw the impedances
        drawImpedancePoints(gc);

        gc.restore();
    }


    /**
     * Draws the static background grid of the Smith Chart.
     * @param gc The GraphicsContext of the canvas.
     */
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

        // --- 1. Save the current graphics state and apply clipping ---
        gc.save();
        gc.beginPath();
        gc.arc(centerX, centerY, mainRadius, mainRadius, 0, 360);
        gc.closePath();
        gc.clip(); // Anything drawn after this will be clipped to the circle

        gc.setLineWidth(1);

        // --- Draw the Outer Circle (r=0, g=0) ---
        gc.setStroke(Color.GRAY);
        gc.strokeOval(centerX - mainRadius, centerY - mainRadius, mainRadius * 2, mainRadius * 2);

        // --- Draw the Horizontal Line (x=0, b=0) ---
        gc.strokeLine(centerX - mainRadius, centerY, centerX + mainRadius, centerY);

        double[] stepValues = {0.2, 0.5, 1.0, 2.0, 4.0, 10.0};

        // --- ADMITTANCE (Y) GRID ---
        // First draw the admittance
        gc.setLineWidth(thinLineValue);

        // Draw Constant Conductance (g) Circles
        gc.setStroke(Color.CORNFLOWERBLUE);
        for (double g : stepValues) {
            double circleRadius = mainRadius / (g + 1);
            double circleCenterX = centerX - mainRadius * g / (g + 1);
            if (g == 1) gc.setLineWidth(thickLineValue); //If
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

        // --- IMPEDANCE (X) GRID ---
        // Then draw the impedance chart


        // Draw Constant Resistance (r) Circles
        gc.setStroke(Color.CORAL); // Color for resistance
        for (double r : stepValues) {
            double circleRadius = mainRadius / (r + 1);
            double circleCenterX = centerX + mainRadius * r / (r + 1);
            if (r == 1) gc.setLineWidth(thickLineValue);
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

        //Draw the labels on the outside of the circle
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
        gc.fillText(text, x, y + fontSize / 3); // Adjust y for vertical centering
    }

    /**
     * Draws the impedance path on the chart based on the circuit elements.
     *
     * @param gc   The GraphicsContext of the canvas.
     * @param path A list of complex impedance points to draw.
     */
    private void drawImpedancePath(GraphicsContext gc /*, List<Complex> path */) {
        // TODO: Loop through the list of points from the ViewModel
        // and draw lines/arcs connecting them.
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        // gc.strokeLine(startPointX, startPointY, endPointX, endPointY);
    }

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

    public void onAddComponentMouse(ActionEvent actionEvent) {

        //Magnetize the cursor on the last component added
        Complex lastGamma = viewModel.getLastGamma();
        if (lastGamma == null) return; // Exit if there's no component yet

        moveCursorToGamma(lastGamma);

        //Now check what type of value is selected and move accordingly depending on the value

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

        // This finds the screen position of the top-left corner of the canvas...
        javafx.geometry.Point2D canvasScreenPos = smithCanvas.localToScreen(0, 0);

        // ...and we add our calculated canvas offsets to get the final screen position.
        int screenX = (int) (canvasScreenPos.getX() + finalCanvasX);
        int screenY = (int) (canvasScreenPos.getY() + finalCanvasY);

        // 4. Move the cursor to the final screen position
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
            Robot robot = new Robot();
            robot.mouseMove(screenX, screenY);
        });
    }
}


