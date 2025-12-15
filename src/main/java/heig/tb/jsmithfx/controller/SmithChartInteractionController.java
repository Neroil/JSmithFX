package heig.tb.jsmithfx.controller;

import heig.tb.jsmithfx.SmithChartViewModel;
import heig.tb.jsmithfx.logic.SmithCalculator;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.Resistor;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import heig.tb.jsmithfx.view.ChartPoint;
import heig.tb.jsmithfx.view.SmithChartRenderer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.robot.Robot;
import javafx.util.Pair;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static heig.tb.jsmithfx.logic.SmithCalculator.addParallelImpedance;
import static heig.tb.jsmithfx.logic.SmithCalculator.calculateComponentValue;

public class SmithChartInteractionController {
    // Renderer and viewModel
    private final Pane smithChartPane;
    private final Canvas smithCanvas;
    private final Canvas cursorCanvas;
    // View model and Renderer
    private final SmithChartViewModel viewModel;
    private final SmithChartRenderer renderer;
    // UI Callbacks
    private final Supplier<CircuitElement.ElementType> typeSupplier;
    private final Supplier<CircuitElement.ElementPosition> positionSupplier;
    private final Supplier<Line.StubType> stubTypeSupplier;
    private final Supplier<Optional<Double>> zoLineSupplier;
    private final Supplier<Optional<Double>> permittivitySupplier;
    private final BiConsumer<String, Enum<?>> valueUpdater; // (value, unit)
    private final Consumer<String> buttonTextUpdater;
    private final Supplier<Optional<Double>> qualityFactorSupplier;

    //View State for zooming and panning
    private double currentScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;
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

    public SmithChartInteractionController(
            Pane smithChartPane,
            Canvas smithCanvas,
            Canvas cursorCanvas,
            SmithChartViewModel viewModel,
            Supplier<CircuitElement.ElementType> typeSupplier,
            Supplier<CircuitElement.ElementPosition> positionSupplier,
            Supplier<Line.StubType> stubTypeSupplier,
            Supplier<Optional<Double>> zoLineSupplier,
            Supplier<Optional<Double>> permittivitySupplier,
            BiConsumer<String, Enum<?>> valueUpdater,
            Consumer<String> buttonTextUpdater,
            Supplier<Optional<Double>> qualityFactorSupplier
    ) {
        this.smithChartPane = smithChartPane;
        this.smithCanvas = smithCanvas;
        this.cursorCanvas = cursorCanvas;
        this.viewModel = viewModel;

        // UI Callbacks
        this.typeSupplier = typeSupplier;
        this.positionSupplier = positionSupplier;
        this.stubTypeSupplier = stubTypeSupplier;
        this.zoLineSupplier = zoLineSupplier;
        this.permittivitySupplier = permittivitySupplier;
        this.valueUpdater = valueUpdater;
        this.buttonTextUpdater = buttonTextUpdater;
        this.qualityFactorSupplier = qualityFactorSupplier;


        this.renderer = new SmithChartRenderer(smithCanvas, cursorCanvas);

        setupResizableCanvas(); // Logique de binding width/height
        setupListeners(); // Logique des listeners
    }

    private void setupListeners() {
        // Whenever the preview element changes, re-render the chart
        viewModel.previewElementS1PProperty().addListener(_ -> redrawSmithCanvas());
        viewModel.previewElementProperty().addListener(_ -> redrawSmithCanvas());
        viewModel.sweepDataPointsProperty().addListener((ListChangeListener<DataPoint>) _ -> {
            redrawSmithCanvas();
        });
        viewModel.vswrCirclesProperty().addListener((ListChangeListener<Double>) _ -> {
            redrawSmithCanvas();
        });

        viewModel.getSelectedInsertionIndexProperty().addListener(_ -> {
            redrawSmithCanvas();
        });

        viewModel.getDpTableSelIndex().addListener(_ -> {
            redrawSmithCanvas();
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem selectAsLoadPoint = new MenuItem("Select as load point");
        contextMenu.getItems().add(selectAsLoadPoint);

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
            if (isAddingMouseComponent && event.getButton() == MouseButton.PRIMARY) { //If we're adding a component and click, adds the component
                finalizeMouseAddComponent();
                event.consume(); //Consumes it so it's not used for other stuff
                return;
            }

            if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.PRIMARY) {
                double mouseX = event.getX();
                double mouseY = event.getY();
                // Hit detection for data points
                ChartPoint hit = renderer.getActivePoints().stream().filter(p -> p.isHit(mouseX, mouseY, 6.0)).findFirst().orElse(null);

                if (hit != null && event.getButton() == MouseButton.SECONDARY) {
                    contextMenu.show(smithCanvas, event.getScreenX(), event.getScreenY());
                    selectAsLoadPoint.setOnAction(e -> {
                        viewModel.setS1PLoadValue(hit.frequency());
                        contextMenu.hide();
                    });
                } else {
                    contextMenu.hide();
                }
                event.consume();
            }

            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
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
            } else {
                renderer.handleTooltip(mouseX, mouseY, currentScale);
            }

            // Pass the correct Gamma coordinates to the ViewModel or if close enough to a component, "lock" onto it
            var gamma = renderer.getCurrentSelectedGamma();

            if (gamma != null) {
                gammaX = gamma.real();
                gammaY = gamma.imag();
            }

            viewModel.calculateMouseInformations(gammaX, gammaY);

        });

        smithCanvas.setOnKeyPressed(event -> {
            // Quit the component add event if we hit ESC
            if (isAddingMouseComponent && event.getCode() == KeyCode.ESCAPE) {
                cancelMouseAddComponent();
            }
        });

        viewModel.selectedElementProperty().addListener(_ -> {
            redrawSmithCanvas();
        });


    }

    /**
     * Clears and redraws the entire chart's canvas. This method will be called whenever the data
     * or the window size changes.
     */
    public void redrawSmithCanvas() {
        if (renderer != null) {
            int selectedIndex = viewModel.getDpTableSelIndex().get();
            renderer.render(viewModel, currentScale, offsetX, offsetY, selectedIndex);
        }
    }

    /**
     * Resets every variable and elements related to the mouse add component event
     */
    private void resetMouseAddComponentState() {
        viewModel.showGhostCursor.set(false);
        smithCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        renderer.clearCursor(cursorCanvas.getGraphicsContext2D());
        viewModel.clearLiveComponentPreview();
        isAddingMouseComponent = false;
        startGammaForMouseAdd = null;
        startImpedanceForMouseAdd = null;
        buttonTextUpdater.accept("Add with Mouse");
        circleCenterForMouseAdd = null;
        circleRadiusForMouseAdd = 0.0;
        startAngleForMouseAdd = 0.0;
        directionMultiplier = 0;
        previousAngle = null;
        allowedAngleTravel = null;
        totalAngleTraveled = null;
        viewModel.setDpTableSelIndex(-1);
        viewModel.clearLiveComponentPreview();
    }

    /**
     * Calculates the final component value and adds it to the ViewModel.
     */
    private void finalizeMouseAddComponent() {
        double z0 = viewModel.zo.get();
        double freq = viewModel.frequencyProperty().get();

        CircuitElement.ElementType type = typeSupplier.get();
        CircuitElement.ElementPosition position = positionSupplier.get();
        Line.StubType stubType = stubTypeSupplier.get();

        // Try to parse optional parameters for line calculations
        Optional<Double> permittivityOpt = permittivitySupplier.get();
        Optional<Double> z0_lineOpt = zoLineSupplier.get();

        Double componentValue = calculateComponentValue(
                snappedGammaForMouseAdd,
                startImpedanceForMouseAdd,
                type,
                position,
                stubType,
                z0,
                freq,
                z0_lineOpt,
                permittivityOpt
        );

        if (componentValue != null) {
            if (type == CircuitElement.ElementType.LINE && z0_lineOpt.isPresent() && permittivityOpt.isPresent()) {
                viewModel.addComponent(type, componentValue, z0_lineOpt.get(), permittivityOpt.get(), null, qualityFactorSupplier.get(), stubType);
            } else viewModel.addComponent(type, componentValue, position, qualityFactorSupplier.get());
        }

        resetMouseAddComponentState();
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

    private void setupResizableCanvas() {
        smithChartPane.setMinSize(0, 0);

        // --- SMITH CHART ---
        // Bind dimensions: Pane controls Canvas
        smithCanvas.widthProperty().bind(smithChartPane.widthProperty());
        smithCanvas.heightProperty().bind(smithChartPane.heightProperty());

        // Redraw when size changes
        smithCanvas.widthProperty().addListener((_, _, _) -> redrawSmithCanvas());
        smithCanvas.heightProperty().addListener((_, _, _) -> redrawSmithCanvas());

        // --- CURSOR RENDERING ---
        cursorCanvas.widthProperty().bind(smithCanvas.widthProperty());
        cursorCanvas.heightProperty().bind(smithCanvas.heightProperty());
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
        Complex idealGamma = circleCenterForMouseAdd.add(
                new Complex(Math.cos(currentAngle), Math.sin(currentAngle)).multiply(circleRadiusForMouseAdd)
        );

        Double liveValue = calculateComponentValue(
                idealGamma,
                startImpedanceForMouseAdd,
                typeSupplier.get(),
                positionSupplier.get(),
                stubTypeSupplier.get(),
                viewModel.zo.get(),
                viewModel.frequencyProperty().get(),
                zoLineSupplier.get(),
                permittivitySupplier.get()
        );

        if (liveValue != null) {
            // Update UI Text
            Pair<ElectronicUnit, String> result = SmithUtilities.getBestUnitAndFormattedValue(
                    liveValue,
                    (ElectronicUnit[]) typeSupplier.get().getUnitClass().getEnumConstants()
            );
            valueUpdater.accept(result.getValue(), (Enum<?>) result.getKey());

            // Update the live preview element
            if (typeSupplier.get() == CircuitElement.ElementType.LINE) {
                if (zoLineSupplier.get().isPresent() && permittivitySupplier.get().isPresent()) {
                    viewModel.addLiveComponentPreview(liveValue, zoLineSupplier.get().get(), permittivitySupplier.get().get(), stubTypeSupplier.get(), qualityFactorSupplier.get());
                }
            } else {
                viewModel.addLiveComponentPreview(typeSupplier.get(), liveValue, positionSupplier.get(), qualityFactorSupplier.get());
            }

            viewModel.updateTunedElementValue(liveValue, typeSupplier.get(), positionSupplier.get(), stubTypeSupplier.get(), permittivitySupplier.get(), zoLineSupplier.get(), qualityFactorSupplier.get());

            // Fetch the preview element to get its impedance and calculate the mouse snapped position
            CircuitElement currentElement = viewModel.previewElementProperty().get();

            if (currentElement != null) {
                double freq = viewModel.frequencyProperty().get();
                double z0 = viewModel.zo.get();

                // Get the physical impedance of the component
                Complex componentZ = currentElement.getImpedance(freq);

                Complex totalZ;

                // Combine with start impedance based on series/parallel
                if (positionSupplier.get() == CircuitElement.ElementPosition.SERIES) {
                    totalZ = startImpedanceForMouseAdd.add(componentZ);
                } else {
                    totalZ = addParallelImpedance(startImpedanceForMouseAdd, componentZ);
                }

                // Convert back to Gamma
                snappedGammaForMouseAdd = SmithCalculator.impedanceToGamma(totalZ, z0);
            }
        } else {
            // Fallback if calculation failed
            snappedGammaForMouseAdd = idealGamma;
        }

        viewModel.ghostCursorGamma.set(snappedGammaForMouseAdd); // Update the ghost cursor position
        renderer.renderCursor(viewModel, currentScale, offsetX, offsetY);
        moveCursorToGamma(snappedGammaForMouseAdd);
    }

    /**
     * Resets the state and UI after finishing or canceling the operation.
     */
    private void cancelMouseAddComponent() {
        resetMouseAddComponentState();
        redrawSmithCanvas();
    }

    public void onAddComponentMouse() {
        // If we click on it again, exit the mode.
        if (isAddingMouseComponent) {
            cancelMouseAddComponent();
            return;
        }

        Complex lastGamma = viewModel.getCurrentInteractionStartGamma();
        if (lastGamma == null) {
            var stage = smithCanvas.getScene().getWindow();
            DialogUtils.showErrorAlert("Operation Failed", "Cannot add a component without a starting point.", stage);
            return;
        }

        // Store starting impedance
        Complex startImpedance = viewModel.getCurrentInteractionStartImpedance();

        if (startImpedance == null) startImpedance = new Complex(0, 0); // Fallback but should not happen

        // Get the element's particularities from the UI
        CircuitElement.ElementType type = typeSupplier.get();
        CircuitElement.ElementPosition position = positionSupplier.get();
        double z0 = viewModel.zo.get();

        // Create a temporary element to calculate the drawing arc
        CircuitElement tempElement = switch (type) {
            case INDUCTOR -> new Inductor(0, position, type);
            case CAPACITOR -> new Capacitor(0, position, type);
            case RESISTOR -> new Resistor(0, position, type);
            case LINE -> {
                // Try to parse optional parameters for line calculations
                Optional<Double> permittivityOpt = permittivitySupplier.get();
                Optional<Double> z0_lineOpt = zoLineSupplier.get();
                Line.StubType stubType = stubTypeSupplier.get();
                if (z0_lineOpt.isPresent() && permittivityOpt.isPresent()) {
                    if (stubType == null || stubType == Line.StubType.NONE) {
                        yield new Line(0, z0_lineOpt.get(), permittivityOpt.get());
                    }
                    yield new Line(0, z0_lineOpt.get(), permittivityOpt.get(), stubType);
                }
                yield null;
            }
        };

        if (tempElement == null) {
            var stage = smithCanvas.getScene().getWindow();
            DialogUtils.showErrorAlert("Operation Failed", "Cannot add a Line without specifying Zo and Permittivity.", stage);
            return;
        }

        // Get direction (counter-clockwise or clockwise)
        int localDirectionMultiplier = SmithCalculator.getExpectedDirection(tempElement, lastGamma);

        Pair<Complex, Double> arcParams = SmithCalculator.getArcParameters(startImpedance, tempElement, z0);
        Complex localCircleCenter = arcParams.getKey();
        double localCircleRadius = arcParams.getValue();

        // Get the angle from which the last gamma is to the circle that we're following
        Complex startVector = lastGamma.subtract(localCircleCenter);
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

        this.startGammaForMouseAdd = lastGamma;
        this.startImpedanceForMouseAdd = startImpedance;
        this.snappedGammaForMouseAdd = lastGamma;
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
        buttonTextUpdater.accept("Cancel (ESC)");
        moveCursorToGamma(startGammaForMouseAdd);
        viewModel.setDpTableSelIndex(-1);
        smithCanvas.requestFocus();
    }

    public void onZoomIn() {
        offsetX = 0;
        offsetY = 0;
        currentScale *= 1.1;

        redrawSmithCanvas();
    }

    public void onZoomOut() {
        offsetX = 0;
        offsetY = 0;
        currentScale /= 1.1;
        redrawSmithCanvas();
    }

    public void onReset() {
        currentScale = 1.0;
        offsetX = 0.0;
        offsetY = 0.0;
        redrawSmithCanvas();
    }


}
