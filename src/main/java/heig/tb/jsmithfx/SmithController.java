package heig.tb.jsmithfx;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

// TODO: Import your model classes when they are created
// import heig.tb.jsmithfx.model.ElementType;
// import heig.tb.jsmithfx.model.ElementPosition;
// import heig.tb.jsmithfx.model.Complex;

// TODO: Import your ViewModel class when it is created
// import heig.tb.jsmithfx.viewmodel.SmithChartViewModel;


public class SmithController {

    // --- FXML Fields ---
    // These are injected by the FXML loader
    @FXML private Pane smithChartPane;
    @FXML private Canvas smithCanvas;
    @FXML private ListView<String> elementListView; // Use a specific type like CircuitElement later
    @FXML private ComboBox<String> typeComboBox; // Use ElementType enum later
    @FXML private ComboBox<String> positionComboBox; // Use ElementPosition enum later
    @FXML private TextField valueTextField;
    @FXML private Button addButton;

    // --- ViewModel ---
    // The ViewModel is the brain of our UI. The controller just talks to it.
    private SmithChartViewModel viewModel; // TODO: Replace with your actual ViewModel

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
    private void drawSmithGrid(GraphicsContext gc) {
        // TODO: Put your detailed grid drawing logic here.
        // This is a simple placeholder.
        double width = smithCanvas.getWidth();
        double height = smithCanvas.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) / 2 - 10;

        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);

        // Outer circle (r=1)
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Horizontal line
        gc.strokeLine(centerX - radius, centerY, centerX + radius, centerY);

        // Example of a resistance circle (e.g., r=0.5)
        double r = 0.5;
        double circleRadius = radius * (1 / (r + 1));
        double circleCenterX = centerX + radius - circleRadius;
        gc.strokeOval(circleCenterX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
    }

    /**
     * Draws the impedance path on the chart based on the circuit elements.
     * @param gc The GraphicsContext of the canvas.
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