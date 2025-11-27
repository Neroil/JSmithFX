package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.Resistor;
import heig.tb.jsmithfx.model.Element.TypicalUnit.*;
import heig.tb.jsmithfx.model.TouchstoneS1P;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogFactory;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.controlsfx.control.RangeSlider;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SmithController {


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
    private double lastScreenXForAdd;
    private double lastScreenYForAdd;

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
    private Canvas cursorCanvas;
    @FXML
    private Canvas circuitCanvas;
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
    @FXML
    private Label z0Label;
    @FXML
    private AnchorPane circuitPane;
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
    private RangeSlider frequencyRangeSlider;
    @FXML
    private TextField minFreqTextField;
    @FXML
    private TextField s1pFileNameField;
    @FXML
    private CheckBox useS1PAsLoadCheckBox;


    //Viewmodel
    private SmithChartViewModel viewModel;

    //Renderer
    private SmithChartRenderer smithChartRenderer;
    private CircuitRenderer circuitRenderer;

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
        this.smithChartRenderer = new SmithChartRenderer(smithCanvas, cursorCanvas);
        circuitRenderer = new CircuitRenderer(circuitCanvas);

        setupResizableCanvas();
        setupControls();
        bindViewModel();
        redrawSmithCanvas(); //Initial chart drawing

        // Whenever your circuit elements change, re-render the circuit diagram
        viewModel.circuitElements.addListener((ListChangeListener<CircuitElement>) c -> circuitRenderer.render(viewModel));

        // Initial render
        circuitRenderer.render(viewModel);

        dataPointsTable.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> redrawSmithCanvas());

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
                if (selectedIndex > 0) {
                    CircuitElement component = viewModel.circuitElements.get(selectedIndex - 1);
                    promptEditForComponent(component);
                }
            }
        });

        // Circuit Diagram Editing
        circuitCanvas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                CircuitElement clickedElement = circuitRenderer.getElementAt(event.getX(), event.getY());
                if (clickedElement != null) {
                    promptEditForComponent(clickedElement);
                }
            }
        });

        //Add the delete button in the factory
        deleteColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("X");

            {
                deleteButton.setOnAction(_ -> {
                    dataPointsTable.getSelectionModel().clearSelection();
                    viewModel.removeComponentAt(getIndex() - 1);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 1) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Listen for changes in the range slider
        frequencyRangeSlider.lowValueProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setFrequencyRangeMin(newVal.doubleValue());
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(
                    newVal.doubleValue(),
                    FrequencyUnit.values());

            minFreqTextField.setText(toDisplay.getValue() + " " + toDisplay.getKey().toString());
            redrawSmithCanvas();
        });

        frequencyRangeSlider.highValueProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setFrequencyRangeMax(newVal.doubleValue());
            var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(
                    newVal.doubleValue(),
                    FrequencyUnit.values());

            maxFreqTextField.setText(toDisplay.getValue() + " " + toDisplay.getKey().toString());
            redrawSmithCanvas();
        });

        minFreqTextField.setOnAction(event -> {
            String text = minFreqTextField.getText();
            try {
                double freqInHz = SmithUtilities.parseValueWithUnit(text, FrequencyUnit.values());
                frequencyRangeSlider.setLowValue(freqInHz);
            } catch (IllegalArgumentException e) {
                showError("Invalid frequency input: " + e.getMessage());
            }
        });

        maxFreqTextField.setOnAction(event -> {
            String text = maxFreqTextField.getText();
            try {
                double freqInHz = SmithUtilities.parseValueWithUnit(text, FrequencyUnit.values());
                frequencyRangeSlider.setHighValue(freqInHz);
            } catch (IllegalArgumentException e) {
                showError("Invalid frequency input: " + e.getMessage());
            }
        });

        // Tells the renderer to use the S1P data as load
        useS1PAsLoadCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setUseS1PAsLoad(newVal);
            redrawSmithCanvas();
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
        // --- SMITH CHART ---
        smithCanvas.widthProperty().bind(smithChartPane.widthProperty());
        smithCanvas.heightProperty().bind(smithChartPane.heightProperty());
        smithCanvas.widthProperty().addListener((_, _, _) -> redrawSmithCanvas());
        smithCanvas.heightProperty().addListener((_, _, _) -> redrawSmithCanvas());

        // --- CURSOR RENDERING ---
        cursorCanvas.widthProperty().bind(smithCanvas.widthProperty());
        cursorCanvas.heightProperty().bind(smithCanvas.heightProperty());

        // --- CIRCUIT RENDERING ---
        circuitCanvas.widthProperty().bind(circuitPane.widthProperty());
        circuitCanvas.heightProperty().bind(circuitPane.heightProperty());
        circuitCanvas.widthProperty().addListener(_ -> circuitRenderer.render(viewModel));
        circuitCanvas.heightProperty().addListener(_ -> circuitRenderer.render(viewModel));

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

            redrawSmithCanvas();
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

            redrawSmithCanvas();
        });

        // Stuff that happens when we click on the chart
        smithCanvas.setOnMouseClicked(event -> {
            if (isAddingMouseComponent && event.getButton() == MouseButton.PRIMARY ) { //If we're adding a component and click, adds the component
                finalizeMouseAddComponent();
                event.consume(); //Consumes it so it's not used for other stuff
                return;
            }

            if (event.getClickCount() == 2) {
                // Reset view on double-click
                currentScale = 1.0;
                offsetX = 0.0;
                offsetY = 0.0;
                redrawSmithCanvas();
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

            if (isAddingMouseComponent) {
                if (isProgrammaticallyMovingCursor) {
                    isProgrammaticallyMovingCursor = false;
                    return;
                }
                // Calculate the movement in pixels on the screen
                double currentScreenX = event.getScreenX();
                double currentScreenY = event.getScreenY();

                double dx = currentScreenX - lastScreenXForAdd;
                double dy = currentScreenY - lastScreenYForAdd;

                // Update the "Last" position to current to avoid accumulation errors
                lastScreenXForAdd = currentScreenX;
                lastScreenYForAdd = currentScreenY;

                handleMouseMagnetization(dx, dy);
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
     * Handles the real-time snapping using Tangential Delta Mapping.
     * This ensures smooth movement even near chart singularities.
     *
     * @param dx Pixel movement in X
     * @param dy Pixel movement in Y
     */
    private void handleMouseMagnetization(double dx, double dy) {

        // Get the tangent vector of the circle
        double tanX = -Math.sin(previousAngle);
        double tanY = Math.cos(previousAngle);

        // Get the value of what a pixel is in gamma
        double mainRadius = Math.min(smithCanvas.getWidth(), smithCanvas.getHeight()) / 2 - 10;
        double pixelToGammaScale = 1.0 / (mainRadius * currentScale);

        // Invert DY because Screen Y is opposite to Cartesian/Smith Y
        double dGammaX = dx * pixelToGammaScale;
        double dGammaY = -dy * pixelToGammaScale;

        // Project the mouse movement onto the tangent vector (Dot Product)
        double tangentialMove = (dGammaX * tanX) + (dGammaY * tanY);

        // Convert linear distance to angular change
        // ArcLength = Radius * Angle  =>  Angle = ArcLength / Radius
        double effectiveRadius = Math.max(circleRadiusForMouseAdd, 0.001);
        double angleChange = tangentialMove / effectiveRadius;

        // Apply the change
        double newTotalAngleTraveled = totalAngleTraveled + (angleChange * directionMultiplier);

        // Clamping the movement
        if (newTotalAngleTraveled < 0 && allowedAngleTravel <= 360) {
            newTotalAngleTraveled = 0;
        } else if (newTotalAngleTraveled > allowedAngleTravel) {
            newTotalAngleTraveled = allowedAngleTravel;
        }

        totalAngleTraveled = newTotalAngleTraveled;

        // Re-calculate the absolute angle based on the clamped travel
        double currentAngle = startAngleForMouseAdd + (totalAngleTraveled * directionMultiplier);

        // Normalize angle
        while (currentAngle <= -Math.PI) currentAngle += 2 * Math.PI;
        while (currentAngle > Math.PI) currentAngle -= 2 * Math.PI;

        previousAngle = currentAngle;

        // Calculate position and Value
        snappedGammaForMouseAdd = circleCenterForMouseAdd.add(
                new Complex(Math.cos(currentAngle), Math.sin(currentAngle)).multiply(circleRadiusForMouseAdd)
        );

        Double liveValue = calculateComponentValue(
                snappedGammaForMouseAdd,
                startImpedanceForMouseAdd,
                typeComboBox.getValue(),
                positionComboBox.getValue(),
                stubComboBox.getValue(),
                viewModel.zo.get(),
                viewModel.frequencyProperty().get()
        );

        if (liveValue != null) {
            Pair<ElectronicUnit, String> result = SmithUtilities.getBestUnitAndFormattedValue(
                    liveValue,
                    unitComboBox.getItems().toArray(new ElectronicUnit[0])
            );
            valueTextField.setText(result.getValue());
            unitComboBox.getSelectionModel().select((Enum<?>) result.getKey());
        }

        viewModel.ghostCursorGamma.set(snappedGammaForMouseAdd); // Update the ghost cursor position
        smithChartRenderer.renderCursor(viewModel, currentScale, offsetX, offsetY);
        moveCursorToGamma(snappedGammaForMouseAdd);
    }

    /**
     * Calculates the value of the component we're plotting on the chart
     *
     * @param gamma          position of the component on the chart
     * @param startImpedance where the last component was
     * @param type           of the component (resistor, capacitor, etc.)
     * @param position       if the component is in series or parallel
     * @param z0             the base impedance
     * @param frequency      of the circuit
     * @return the calculated value of the component
     */
    private Double calculateComponentValue(Complex gamma,
                                           Complex startImpedance,
                                           CircuitElement.ElementType type,
                                           CircuitElement.ElementPosition position,
                                           Line.StubType stubType,
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

        double componentValue = 0;

        if (type == CircuitElement.ElementType.LINE){
            //Get the current values of εr and z_L
            double z0_line;
            double permittivity;
            try {
                z0_line = Double.parseDouble(zoInputField.getText());
                permittivity = Double.parseDouble(permittivityField.getText());
                if (z0_line <= 0 || permittivity < 1.0) return null; // Basic validation
            } catch (NumberFormatException e) {
                showError("Please make sure the z0 and εr field are filled");
                return null; // A value hasn't been entered yet
            }

            final double C = 299792458.0; // Speed of light in m/s
            // β = 2πf/pv, will be used for the calculations in the two branches
            double phase_velocity = C / Math.sqrt(permittivity);
            double beta = (2.0 * Math.PI * frequency) / phase_velocity;

            if (stubType == Line.StubType.NONE){
                    // Based on the reflection propagation formula: Γ(L) = Γ(0) * e^(-j2βL)
                    // Multiplying by e^(-j2βL) rotates Γ by an angle -2βL on the complex plane.
                    // So the physical length L can be recovered from the change in angle of Γ.

                    // So we transform the equation like this: ∠Γ(L) = ∠Γ(0) +  ∠(e^(-j2βL))
                    // And then we rearrange and rewrite it to: ∠Γ(L) - ∠Γ(0) = -2βL
                    Complex gamma_start_line = startImpedance.subtract(new Complex(z0_line,0)).dividedBy(startImpedance.add(new Complex(z0_line,0)));
                    Complex gamma_final_line = finalImpedance.subtract(new Complex(z0_line,0)).dividedBy(finalImpedance.add(new Complex(z0_line,0)));

                    // Calculate the change in angle. (Δθ)
                    double startAngle = gamma_start_line.angle();
                    double finalAngle = gamma_final_line.angle();
                    double angleChange = finalAngle - startAngle;

                    // The rotation for adding a line is always clockwise.
                    if (angleChange > 0) {
                        angleChange -= 2.0 * Math.PI;
                    }
                    // L = ∣Δθ∣ / 2β
                    double electricalRotation = Math.abs(angleChange);

                    // Avoid division by zero if beta is somehow zero
                    if (Math.abs(beta) < EPS) return null;

                    // Calculate the final physical length.
                    componentValue  = electricalRotation / (2.0 * beta);
                } else {
                // Here we use the basic formula of Z_in = Z_0 * (Z_L + jZ_0tan(βL)) / (Z_0 + jZ_Ltan(βL))
                // If it's a short circuit, Z_L becomes 0 so Z_in = jZ_0tan(βL)
                // If it's an open circuit, Z_L becomes infinity, so we simplify the equation to get Z_in = Z_0 / jtan(βL)

                // Since a stub is in parallel, we have to use admittance
                Complex yStart = startImpedance.inverse();
                Complex yFinal = finalImpedance.inverse();

                Complex yDiff = yFinal.subtract(yStart);
                double targetSusceptance = yDiff.imag();
                double y0 = 1.0 / z0_line;
                double L;

                if (stubType == Line.StubType.SHORT){
                    // For a short-circuited stub:
                    // Y_in = 1 / (j Z_0 tan(βL)) = -j Y_0 / tan(βL)
                    // So, tan(βL) = -Y_0 / (j Y_in) => for susceptance B, tan(βL) = -Y_0 / B
                    // L = arctan(-Y_0 / targetSusceptance) / β
                    L = Math.atan(-y0 / targetSusceptance) / beta;
                } else {
                    // For an open-circuited stub:
                    // Y_in = j Y_0 tan(βL)
                    // So, tan(βL) = B / Y_0
                    // L = arctan(targetSusceptance / y0) / β
                    L = Math.atan(targetSusceptance / y0) / beta;
                }
                // Get a positive value
                while (L < 0) {
                    L += Math.PI / beta;
                }
                componentValue = L;
            }
        }

        //Logic for "normal" elements (RLC)
        if (position == CircuitElement.ElementPosition.SERIES) {
            Complex addedImpedance = finalImpedance.subtract(startImpedance);
            double imagZ = addedImpedance.imag();

            if (type == CircuitElement.ElementType.INDUCTOR) {
                componentValue = imagZ / omega; // L = Im(Z) / ω
            } else if (type == CircuitElement.ElementType.CAPACITOR) {
                if (Math.abs(imagZ) < EPS) return null;
                componentValue = -1.0 / (imagZ * omega); // C = -1/(Im(Z)*ω)
            } else if (type == CircuitElement.ElementType.RESISTOR) {
                componentValue = addedImpedance.real(); // R = Re(Z)
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
            } else if (type == CircuitElement.ElementType.RESISTOR) {
                componentValue = 1.0 / addedY.real(); // R = 1/Re(ΔY)
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
        double freq = viewModel.frequencyProperty().get();

        CircuitElement.ElementType type = typeComboBox.getValue();
        CircuitElement.ElementPosition position = positionComboBox.getValue();
        Line.StubType stubType = stubComboBox.getValue();

        Double componentValue = calculateComponentValue(
                snappedGammaForMouseAdd,
                startImpedanceForMouseAdd,
                type,
                position,
                stubType,
                z0,
                freq
        );

        if (componentValue != null) {
            if (type == CircuitElement.ElementType.LINE) {
                double z0_line = Double.parseDouble(zoInputField.getText());
                double permittivity = Double.parseDouble(permittivityField.getText());
                viewModel.addComponent(type,componentValue,z0_line,permittivity,null,stubType);
            }else viewModel.addComponent(type, componentValue, position);
        }

        resetMouseAddComponentState();
    }

    /**
     * Resets the state and UI after finishing or canceling the operation.
     */
    private void cancelMouseAddComponent() {
        resetMouseAddComponentState();
        redrawSmithCanvas();
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
        viewModel.showGhostCursor.set(false);
        smithCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        smithChartRenderer.clearCursor(cursorCanvas.getGraphicsContext2D());
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

        viewModel.measuresGammaProperty().addListener((_, _, _) -> redrawSmithCanvas());

    }

    /**
     * Logic needed when we add a component using the keyboard (and not the mouse)
     */
    @FXML
    protected void onAddComponent() {
        try {
            CircuitElement.ElementType type = typeComboBox.getValue();
            double value = Double.parseDouble(valueTextField.getText());

            if (type != CircuitElement.ElementType.LINE){
                CircuitElement.ElementPosition position = positionComboBox.getValue();
                viewModel.addComponent(type, value * getSelectedUnitFactor(), position);
            } else { //It's a line, we need to get the line's characterstic impedance
                double impValue = Double.parseDouble(zoInputField.getText());
                double permittivityValue = Double.parseDouble(permittivityField.getText());
                Line.StubType stubType = stubComboBox.getValue();

                if (permittivityValue < 1.0) { // La permittivité relative est >= 1
                    showError("Permittivity (εr) must be >= 1.");
                    return;
                }
                viewModel.addComponent(
                        type,
                        value * getSelectedUnitFactor(), // Longueur physique
                        impValue,                        // Z0
                        permittivityValue,               // εr
                        null,
                        stubType
                );
            }
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
     * Clears and redraws the entire chart's canvas. This method will be called whenever the data
     * or the window size changes.
     */
    private void redrawSmithCanvas() {
        if (smithChartRenderer != null) {
            int selectedIndex = dataPointsTable.getSelectionModel().getSelectedIndex();
            smithChartRenderer.render(viewModel, currentScale, offsetX, offsetY, selectedIndex);

        }
    }

    /**
     * Set what will be the center point of the chart
     *
     * @param actionEvent unused here
     */
    public void setCharacteristicImpedance(ActionEvent actionEvent) {
        DialogFactory.showDoubleInputDialog("Characteristic Impedance", "Enter Zo (Ohms):", viewModel.zo.get())
                .ifPresent(zo -> {
                    if (zo > 0) {
                        viewModel.zo.setValue(zo);
                        redrawSmithCanvas();
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
                    redrawSmithCanvas();
                });
    }

    public void onChangeFreq(ActionEvent actionEvent) {
        DialogFactory.showFrequencyInputDialog("Change Frequency", viewModel.frequencyProperty().get())
                .ifPresent(newFreq -> {
                    if (newFreq > 0) {
                        viewModel.setFrequency(newFreq);
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

        redrawSmithCanvas();
    }

    @FXML
    private void onZoomOut() {
        offsetX = 0;
        offsetY = 0;
        currentScale /= 1.1;
        redrawSmithCanvas();
    }

    @FXML
    private void onReset() {
        currentScale = 1.0;
        offsetX = 0.0;
        offsetY = 0.0;
        redrawSmithCanvas();
    }

    @FXML
    public void toggleNavButton(ActionEvent actionEvent) {
        boolean isSelected = toggleNavButton.isSelected();
        buttonSmithHBox.setVisible(isSelected);
        buttonSmithHBox.setManaged(isSelected); // Ensures layout adjusts when hidden
    }

    @FXML
    public void onAddComponentMouse(ActionEvent actionEvent) {

        // If we click on it again, exit the mode.
        if (isAddingMouseComponent) {
            cancelMouseAddComponent();
            return;
        }

        Complex lastGamma = viewModel.getLastGamma();
        if (lastGamma == null) {
            DialogFactory.showErrorAlert("Operation Failed", "Cannot add a component without a starting point.");
            return;
        }

        // Store starting impedance/gamma
        Complex startImpedance = viewModel.getLastImpedance();
        Complex startGamma = lastGamma;

        // Get the element's particularities from the UI
        CircuitElement.ElementType type = typeComboBox.getValue();
        CircuitElement.ElementPosition position = positionComboBox.getValue();
        double z0 = viewModel.zo.get();

        // Create a temporary element to calculate the drawing arc
        CircuitElement tempElement = switch (type) {
            case INDUCTOR -> new Inductor(0, position, type);
            case CAPACITOR -> new Capacitor(0, position, type);
            case RESISTOR -> new Resistor(0, position, type);
            case LINE ->  {
                double impValue = Double.parseDouble(zoInputField.getText());
                double permittivityValue = Double.parseDouble(permittivityField.getText());
                Line.StubType stubType = stubComboBox.getValue();
                if (stubType == null || stubType == Line.StubType.NONE) {
                    yield new Line(0, impValue,permittivityValue);
                }
                yield new Line(0, impValue,permittivityValue,stubType);
            }
        };

        // Get direction (counter-clockwise or clockwise)
        int localDirectionMultiplier = SmithUtilities.getExpectedDirection(tempElement, startGamma);

        Pair<Complex, Double> arcParams = SmithUtilities.getArcParameters(startImpedance, tempElement, z0);
        Complex localCircleCenter = arcParams.getKey();
        double localCircleRadius = arcParams.getValue();

        // Get the angle from which the last gamma is to the circle that we're following
        Complex startVector = startGamma.subtract(localCircleCenter);
        double localStartAngle = startVector.angle();

        // We want to only be able to move until we arrive at either extremity of the X-axis
        Complex targetGamma = (position == CircuitElement.ElementPosition.SERIES)
                ? new Complex(1, 0)  // Open Circuit
                : new Complex(-1, 0); // Short Circuit

        // Compute how much the user can move in angle
        Complex targetVector = targetGamma.subtract(localCircleCenter);
        double endAngle = targetVector.angle();
        double travel = endAngle - localStartAngle;

        double localAllowedAngleTravel = 400; //More than 360, should allow for unlimited rotations

        //Bypass this if we're on a line
        if (type != CircuitElement.ElementType.LINE) {
            if (localDirectionMultiplier == 1) { // For CCW, we want the positive angle difference
                while (travel <= 0) travel += 2 * Math.PI;
            } else { // For CW, we want the negative angle difference, then take its absolute value
                while (travel >= 0) travel -= 2 * Math.PI;
                travel = Math.abs(travel);
            }
            localAllowedAngleTravel = travel - 0.001; // Epsilon for floating-point safety
        }

        this.startGammaForMouseAdd = startGamma;
        this.startImpedanceForMouseAdd = startImpedance;
        this.snappedGammaForMouseAdd = startGamma;
        this.circleCenterForMouseAdd = localCircleCenter;
        this.circleRadiusForMouseAdd = localCircleRadius;
        this.startAngleForMouseAdd = localStartAngle;
        this.directionMultiplier = localDirectionMultiplier;
        this.allowedAngleTravel = localAllowedAngleTravel;

        // Initialize state variables for the mouse handler
        this.previousAngle = localStartAngle;
        this.totalAngleTraveled = 0.0;

        // Activate mouse mode after initialization
        this.isAddingMouseComponent = true;

        viewModel.ghostCursorGamma.set(startGammaForMouseAdd);
        viewModel.showGhostCursor.set(true);
        smithCanvas.setCursor(Cursor.NONE);

        // Update UI and move cursor
        addMouseButton.setText("Cancel (ESC)");
        dataPointsTable.setMouseTransparent(true);
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
            // Update our reference to where the cursor currently IS
            lastScreenXForAdd = screenX;
            lastScreenYForAdd = screenY;

            isProgrammaticallyMovingCursor = true;
            Robot robot = new Robot();
            robot.mouseMove(screenX, screenY);
        });
    }

    public void importS1P(ActionEvent actionEvent) {
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

                var minDisplayValue = SmithUtilities.getBestUnitAndFormattedValue(
                        minFreq,
                        FrequencyUnit.values()
                );
                var maxDisplayValue = SmithUtilities.getBestUnitAndFormattedValue(
                        maxFreq,
                        FrequencyUnit.values()
                );


                minFreqTextField.setText(minDisplayValue.getValue() + " " + minDisplayValue.getKey().toString());
                maxFreqTextField.setText(maxDisplayValue.getValue() + " " + maxDisplayValue.getKey().toString());

                // Set the slider's overall range
                frequencyRangeSlider.setMin(minFreq);
                frequencyRangeSlider.setMax(maxFreq);

                // Set the thumbs to the full range initially
                frequencyRangeSlider.setLowValue(minFreq);
                frequencyRangeSlider.setHighValue(maxFreq);

                //Display the S1P controls
                s1pTitledPane.setVisible(true);
                s1pTitledPane.setExpanded(true);
                s1pTitledPane.setManaged(true);

                redrawSmithCanvas();
            } catch (IllegalArgumentException e) {
                showError("Invalid S1P file: " + e.getMessage());
            }
        }
    }

    public void exportS1P(ActionEvent actionEvent) {
    }

    public void changeS1P(ActionEvent actionEvent) {
        importS1P(actionEvent);
    }

    public void removeS1P(ActionEvent actionEvent) {
        viewModel.clearS1PDatapoints();
        s1pFileNameField.setText("");

        //Hide the S1P controls
        s1pTitledPane.setVisible(false);
        s1pTitledPane.setManaged(false);

        redrawSmithCanvas();
    }

    private void promptEditForComponent(CircuitElement component) {
        DialogFactory.showComponentEditDialog(component).ifPresent(newValue -> {
            component.setRealWorldValue(newValue);
            redrawSmithCanvas();
        });
    }
}