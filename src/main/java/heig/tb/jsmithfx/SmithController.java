package heig.tb.jsmithfx;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

// TODO: Import your model classes when they are created
// import heig.tb.jsmithfx.model.ElementType;
// import heig.tb.jsmithfx.model.ElementPosition;
// import heig.tb.jsmithfx.model.Complex;

// TODO: Import your ViewModel class when it is created
// import heig.tb.jsmithfx.viewmodel.SmithChartViewModel;


public class SmithController {

    public Label returnLossLabel;
    public Label vswrLabel;
    public Label qLabel;
    public Label gammaLabel;
    public Label yLabel;
    public Label zLabel;
    public Label zoLabel;
    public Label freqLabel;
    // --- FXML Fields ---
    // These are injected by the FXML loader
    @FXML
    private Pane smithChartPane;
    @FXML
    private Canvas smithCanvas;
    @FXML
    private ListView<String> elementListView; // Use a specific type like CircuitElement later
    @FXML
    private ComboBox<String> typeComboBox; // Use ElementType enum later
    @FXML
    private ComboBox<String> positionComboBox; // Use ElementPosition enum later
    @FXML
    private TextField valueTextField;
    @FXML
    private Button addButton;

    // --- ViewModel ---
    // The ViewModel is the brain of our UI. The controller just talks to it.
    private SmithChartViewModel viewModel; // TODO: Replace with your actual ViewModel

    private double thickLineValue = 1;
    private double thinLineValue = 0.4;

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
    }

    /**
     * Sets up the canvas to resize automatically with its parent pane.
     */
    private void setupResizableCanvas() {
        // Bind the canvas size to the size of its parent pane
        smithCanvas.widthProperty().bind(smithChartPane.widthProperty());
        smithCanvas.heightProperty().bind(smithChartPane.heightProperty());

        // Add listeners to redraw the chart whenever the size changes
        smithCanvas.widthProperty().addListener((obs, oldVal, newVal) -> redrawCanvas());
        smithCanvas.heightProperty().addListener((obs, oldVal, newVal) -> redrawCanvas());
    }

    /**
     * Populates ComboBoxes and sets default values.
     */
    private void setupControls() {
        // TODO: Replace String with your ElementType and ElementPosition enums
        // typeComboBox.getItems().addAll(ElementType.values());
        // positionComboBox.getItems().addAll(ElementPosition.values());

        // Example with strings for now:
        typeComboBox.getItems().addAll("Capacitor", "Inductor", "Resistor");
        positionComboBox.getItems().addAll("Series", "Parallel");

        // Select the first item by default
        typeComboBox.getSelectionModel().selectFirst();
        positionComboBox.getSelectionModel().selectFirst();
    }

    /**
     * Creates the data-bindings between the View (FXML controls) and the ViewModel.
     */
    private void bindViewModel() {
        // TODO: Uncomment and adapt these lines when your ViewModel is ready.
        // This is where the magic happens. The UI will now update automatically.

        // Bind the list of elements in the UI to the list in the ViewModel
        // elementListView.itemsProperty().bind(viewModel.circuitElementsProperty());

        // Bind the user input fields to the ViewModel properties.
        // This allows the ViewModel to always have the latest user input.
        // viewModel.newElementTypeProperty().bind(typeComboBox.valueProperty());
        // viewModel.newElementPositionProperty().bind(positionComboBox.valueProperty());
        // viewModel.newElementValueProperty().bindBidirectional(valueTextField.textProperty());
    }


    // --- Event Handlers ---
    // These methods are called by user actions (e.g., button clicks) defined in the FXML.
    // The controller's job is to simply delegate the action to the ViewModel.

    @FXML
    protected void onAddComponent() {
        System.out.println("Add Component button clicked!");
        // The controller does NOT contain logic. It just calls the ViewModel.
        // viewModel.addNewElement(); // TODO: Uncomment when ViewModel is ready
    }


    // --- Drawing Logic ---
    // This logic is called by the initialize() method and the resize listeners.

    /**
     * Clears and redraws the entire canvas. This method will be called whenever the data
     * or the window size changes.
     */
    private void redrawCanvas() {
        GraphicsContext gc = smithCanvas.getGraphicsContext2D();

        // Clear the canvas before redrawing
        gc.clearRect(0, 0, smithCanvas.getWidth(), smithCanvas.getHeight());

        // Draw the static parts of the chart
        drawSmithGrid(gc);

        // Get the dynamic data from the ViewModel and draw it
        // List<Complex> path = viewModel.getImpedancePath(); // TODO: Get data from ViewModel
        // drawImpedancePath(gc, path);
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

        double[] stepValues = {0.2, 0.5, 1.0, 2.0, 5.0};

        // --- ADMITTANCE (Y) GRID ---
        // First draw the admittance
        gc.setLineWidth(thinLineValue);

        // Draw Constant Conductance (g) Circles
        gc.setStroke(Color.CORNFLOWERBLUE);
        for (double g : stepValues) {
            double circleRadius = mainRadius / (g + 1);
            double circleCenterX = centerX - mainRadius * g / (g + 1);
            if(g == 1) gc.setLineWidth(thickLineValue); //If
            gc.strokeOval(circleCenterX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
            if(g == 1) gc.setLineWidth(thinLineValue);
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
            if(r == 1) gc.setLineWidth(thickLineValue);
            gc.strokeOval(circleCenterX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
            if(r == 1) gc.setLineWidth(thinLineValue);
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


        // --- 3. Restore the graphics state to remove the clipping ---
        gc.restore();

        // --- 4. Add label that shows the value of each circle or arc
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
}