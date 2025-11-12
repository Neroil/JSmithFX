package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Import your model classes when they are created
// import heig.tb.jsmithfx.model.ElementType;
// import heig.tb.jsmithfx.model.ElementPosition;
// import heig.tb.jsmithfx.model.Complex;

// TODO: Import your ViewModel class when it is created
// import heig.tb.jsmithfx.viewmodel.SmithChartViewModel;


public class SmithController {

    private static Font LABEL_FONT = new Font("Arial", 10);
    public Label returnLossLabel;
    public Label vswrLabel;
    public Label qLabel;
    public Label gammaLabel;
    public Label yLabel;
    public Label zLabel;
    public Label zoLabel;
    public Label freqLabel;
    public MenuItem setCharacteristicImpedanceButton;
    public Button changeLoadButton;
    public Label loadImpedanceLabel;
    public Button changeFreqButton;

    // --- FXML Fields ---
    // These are injected by the FXML loader
    @FXML
    private Pane smithChartPane;
    @FXML
    private Canvas smithCanvas;
    @FXML
    private ListView<Complex> dataPointsList; // Use a specific type like CircuitElement later
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

    // --- ViewModel ---
    // The ViewModel is the brain of our UI. The controller just talks to it.
    private SmithChartViewModel viewModel; // TODO: Replace with your actual ViewModel

    // --- Important values ---
    private final double thickLineValue = 1;
    private final double thinLineValue = 0.4;

    // --- View State for Zoom and Pan ---
    private double currentScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    // For panning
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;

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
        dataPointsList.getSelectionModel().selectedItemProperty().addListener((_,_,_) -> redrawCanvas());

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

        typeComboBox.valueProperty().addListener(new ChangeListener<Enum<?>>() {

            @Override
            public void changed(ObservableValue<? extends Enum<?>> observable, Enum<?> oldValue, Enum<?> newValue) {
                CircuitElement.ElementType selectedType = typeComboBox.getValue();
                switch (selectedType) {
                    case CircuitElement.ElementType.RESISTOR -> updateUnitComboBox(ResistanceUnit.class);
                    case CircuitElement.ElementType.CAPACITOR -> updateUnitComboBox(CapacitanceUnit.class);
                    case CircuitElement.ElementType.INDUCTOR -> updateUnitComboBox(InductanceUnit.class);
                    default -> unitComboBox.getItems().clear();
                }
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

        dataPointsList.setCellFactory(param -> new ListCell<Complex>() {



            @Override
            protected void updateItem(Complex item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null); // Don't display anything if the cell is empty
                } else {
                    int index = getIndex();

                    if (index == 0) {
                        setText(String.format("LD: %.2f + j%.2f Ω", item.real(), item.imag()));
                    } else {
                        setText(String.format("DP%d: %.2f + j%.2f Ω", index, item.real(), item.imag()));
                    }
                }
            }
        });

        dataPointsList.itemsProperty().bind(viewModel.measuresProperty());
        loadImpedanceLabel.textProperty().bind(viewModel.loadImpedance.asString());
        freqLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            double freq = viewModel.frequency.get();
            return switch ((int) Math.log10(freq)){
                case 0, 1, 2 -> String.format("%.2f Hz", freq);
                case 3, 4, 5 -> String.format("%.2f kHz", freq / 1_000);
                case 6, 7, 8 -> String.format("%.2f MHz", freq / 1_000_000);
                default -> String.format("%.2f GHz", freq / 1_000_000_000);
            };
        }, viewModel.frequency));

        viewModel.measuresGammaProperty().addListener((_,_,_) -> redrawCanvas());

    }

    @FXML
    protected void onAddComponent() {
        try {
            CircuitElement.ElementType type = typeComboBox.getValue();
            CircuitElement.ElementPosition position = positionComboBox.getValue();
            double value = Double.parseDouble(valueTextField.getText());
            viewModel.addComponent(type,value *  getSelectedUnitFactor() ,position);
        } catch (NumberFormatException e) {
            showError("Invalid value format.");
        }
    }

    private double getSelectedUnitFactor() {
        Enum<?> selectedUnit = unitComboBox.getValue();
        if (selectedUnit instanceof CapacitanceUnit || selectedUnit instanceof ResistanceUnit || selectedUnit instanceof InductanceUnit) {
            try {
                return (double) selectedUnit.getClass().getMethod("getFactor").invoke(selectedUnit);
            } catch (Exception e) {
                showError("Error retrieving unit factor.");
            }
        }
        return 1.0; // Default factor if none is found or an error is caught
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
            if(g == 1) gc.setLineWidth(thickLineValue); //If
            gc.strokeOval(circleCenterX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
            if(g == 1) gc.setLineWidth(thinLineValue);

            drawLabel(gc, String.format("%.1f mS", g / viewModel.zo.get() * 1000), circleCenterX,centerY - circleRadius, Color.WHITE);
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

            drawLabel(gc, String.format("%.1f", r* viewModel.zo.get()), circleCenterX,centerY + circleRadius, Color.WHITE);
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

        drawImpedancePoints(gc);

    }

    /**
     * Draw impedance points based on the gammas calculated in the viewModel
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
                int selectedItemIndex = dataPointsList.getSelectionModel().getSelectedIndex();
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
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Set Characteristic Impedance");
        dialog.setHeaderText("Enter new Zo value (Ohms):");
        dialog.setContentText("Zo:");
        dialog.setGraphic(null);

        dialog.showAndWait().ifPresent(input -> {
            try {
                double zo = Double.parseDouble(input);
                if (zo > 0) {
                    // Update in ViewModel
                    viewModel.zo.setValue(zo);
                    redrawCanvas();
                } else {
                    showError("Zo must be positive.");
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format.");
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Input Error");
        alert.showAndWait();
    }

    public void onChangeLoad(ActionEvent actionEvent) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Change Load Value");
        dialog.setHeaderText("Enter new load values:");

        // Set the button types
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Create the input fields
        TextField reField = new TextField();
        reField.setPromptText("Re");
        TextField imField = new TextField();
        imField.setPromptText("Im");

        // Create a layout for the inputs
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Re:"), 0, 0);
        grid.add(reField, 1, 0);
        grid.add(new Label("Im:"), 0, 1);
        grid.add(imField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a pair of values when OK is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new Pair<>(reField.getText(), imField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double re = Double.parseDouble(result.getKey());
                double im = Double.parseDouble(result.getValue());
                // Handle the new load values (re, im)
                viewModel.loadImpedance.setValue(new Complex(re, im));
                System.out.println("Impedance: " + viewModel.loadImpedance.getValue());
                redrawCanvas();
            } catch (NumberFormatException e) {
                showError("Invalid number format.");
            }
        });
    }

    public void onChangeFreq(ActionEvent actionEvent) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Change Frequency Value");
        dialog.setHeaderText("Enter new frequency value and select the unit:");

        // Set the button types
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Create the input field
        TextField freqField = new TextField();
        freqField.setPromptText("Frequency");

        // Create toggle buttons for units
        ToggleGroup unitGroup = new ToggleGroup();
        RadioButton hzButton = new RadioButton("Hz");
        hzButton.setToggleGroup(unitGroup);
        hzButton.setSelected(true); // Default unit
        RadioButton mhzButton = new RadioButton("MHz");
        mhzButton.setToggleGroup(unitGroup);
        RadioButton khzButton = new RadioButton("kHz");
        khzButton.setToggleGroup(unitGroup);
        RadioButton ghzButton = new RadioButton("GHz");
        ghzButton.setToggleGroup(unitGroup);

        // Create a layout for the inputs
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Frequency:"), 0, 0);
        grid.add(freqField, 1, 0);
        grid.add(new Label("Unit:"), 0, 1);
        grid.add(hzButton, 1, 1);
        grid.add(mhzButton, 2, 1);
        grid.add(khzButton, 3, 1);
        grid.add(ghzButton, 4, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a pair of values when OK is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                String selectedUnit = ((RadioButton) unitGroup.getSelectedToggle()).getText();
                return new Pair<>(freqField.getText(), selectedUnit);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double freq = Double.parseDouble(result.getKey());
                String unit = result.getValue();

                // Convert frequency to Hz based on the selected unit
                switch (unit) {
                    case "MHz":
                        freq *= 1_000_000;
                        break;
                    case "kHz":
                        freq *= 1_000;
                        break;
                    case "GHz":
                        freq *= 1_000_000_000;
                        break;
                    case "Hz":
                    default:
                        break;
                }

                if (freq > 0) {
                    // Update in ViewModel
                    viewModel.frequency.setValue(freq);
                    redrawCanvas();
                } else {
                    showError("Frequency must be positive.");
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format.");
            }
        });
    }
}


