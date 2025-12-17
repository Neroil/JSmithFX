package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.SmithChartViewModel;
import heig.tb.jsmithfx.logic.SmithCalculator;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SmithChartRenderer {

    private final Canvas smithCanvas;
    private final Canvas cursorCanvas;

    private final double thickLineValue = 1;
    private final double thinLineValue = 0.4;
    private Font LABEL_FONT = new Font("Arial", 10);

    private final List<ChartPoint> activePoints = new ArrayList<>();
    private int currentSelectedIndex = -1;

    public SmithChartRenderer(Canvas smithCanvas, Canvas cursorCanvas) {
        this.smithCanvas = smithCanvas;
        this.cursorCanvas = cursorCanvas;
    }

    /**
     * Clears and redraws the entire canvas. This method will be called whenever the data
     * or the window size changes.
     */
    public void render(SmithChartViewModel viewModel, double currentScale, double offsetX, double offsetY, int selectedIndex) {
        GraphicsContext gc = smithCanvas.getGraphicsContext2D();
        SmithChartLayout layout = new SmithChartLayout(smithCanvas.getWidth(), smithCanvas.getHeight());

        activePoints.clear();

        gc.setImageSmoothing(true);

        gc.save();
        // Clear the canvas before redrawing
        gc.clearRect(0, 0, smithCanvas.getWidth(), smithCanvas.getHeight());

        gc.translate(offsetX, offsetY);
        gc.scale(currentScale, currentScale);

        updateFontSize();

        // Draw the static parts of the chart
        drawSmithGrid(gc, viewModel, layout);
        drawVSWRCircles(gc, viewModel, layout);
        drawS1PPoints(gc, viewModel, layout, currentScale, offsetX, offsetY);
        drawImpedancePath(gc, viewModel, layout);
        drawImpedancePoints(gc, viewModel, layout, selectedIndex, currentScale, offsetX, offsetY);
        drawDiscreteComponentPreviews(gc, viewModel, layout);
        drawSweepPoints(gc, viewModel, layout, currentScale, offsetX, offsetY);

        gc.restore();
    }

    private void drawDiscreteComponentPreviews(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout) {
        List<Complex> previews = viewModel.getDiscreteComponentGammas();
        if (previews == null || previews.isEmpty()) return;

        gc.setStroke(Color.AQUA);

        gc.setLineWidth(1.5);
        for (Complex gamma : previews) {
            double localX = layout.toScreenX(gamma);
            double localY = layout.toScreenY(gamma);

            double size = 8.0;
            gc.strokeOval(localX - size / 2, localY - size / 2, size, size);
        }
    }

    private void drawSweepPoints(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout, double currentScale, double offsetX, double offsetY) {

        List<DataPoint> sweepPoints = viewModel.sweepDataPointsProperty();

        if (sweepPoints == null || sweepPoints.isEmpty()) return;

        // Visual styling
        gc.setStroke(Color.MAGENTA);
        gc.setFill(Color.MAGENTA);

        // Make line width and point size invariant to the zoom level
        double lineWidth = 1.0;
        double pointSize = 2.5; // logical size

        gc.setLineWidth(lineWidth);

        // We will draw a connected line (path) through the sweep points
        gc.beginPath();

        boolean isFirst = true;

        int index = 0;
        for (DataPoint point : sweepPoints) {
            Complex gamma = point.getGamma();

            // Convert from Smith Chart math coordinates to Canvas coordinates
            double localX = layout.toScreenX(gamma);
            double localY = layout.toScreenY(gamma);

            if (isFirst) {
                gc.moveTo(localX, localY);
                isFirst = false;
            } else {
                gc.lineTo(localX, localY);
            }

            // Draw the dots
            gc.fillOval(localX - pointSize / 2, localY - pointSize / 2, pointSize, pointSize);

            // Calculate ABSOLUTE coordinates for hit testing
            double absoluteX = (localX * currentScale) + offsetX;
            double absoluteY = (localY * currentScale) + offsetY;

            String label = "SWP" + index++;

            activePoints.add(new ChartPoint(absoluteX, absoluteY, gamma, point.getFrequency(), label, pointSize * currentScale, false));
        }

        // Draw the connected line
        gc.stroke();
    }

    private void drawVSWRCircles(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout) {
        List<Double> vswrValues = viewModel.vswrCirclesProperty().get();
        if (vswrValues == null || vswrValues.isEmpty()) return;

        double centerX = layout.getCenterX();
        double centerY = layout.getCenterY();
        double mainRadius = layout.getRadius();

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(thinLineValue);

        for (double vswr : vswrValues) {
            if (vswr < 1.0) continue; // Invalid VSWR value

            double r = (vswr - 1) / (vswr + 1); // Reflection coefficient magnitude
            double circleRadius = r * mainRadius;
            gc.strokeOval(centerX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);

            drawLabel(gc, String.format("VSWR %.1f", vswr), centerX, centerY - circleRadius, Color.LIGHTGRAY);
        }
    }

    public void renderCursor(SmithChartViewModel viewModel, double currentScale, double offsetX, double offsetY) {
        GraphicsContext gc = cursorCanvas.getGraphicsContext2D();

        clearCursor(gc);

        if (viewModel.showGhostCursor.get()) {
            gc.save();

            gc.translate(offsetX, offsetY);
            gc.scale(currentScale, currentScale);

            drawGhostCursor(gc, viewModel, currentScale, new SmithChartLayout(smithCanvas.getWidth(), smithCanvas.getHeight()));

            gc.restore();
        }
    }

    /**
     * Updates the font size for the Smith chart labels based on the canvas size.
     */
    private void updateFontSize() {
        double newFontSize = Math.min(smithCanvas.getWidth(), smithCanvas.getHeight()) / 60;
        LABEL_FONT = new Font("Arial", newFontSize);
    }

    /**
     * Draws the static background grid of the Smith Chart.
     *
     * @param gc The GraphicsContext of the canvas.
     */
    private void drawSmithGrid(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout) {

        //Save the current graphics state and apply clipping so the lines don't get out of the circle of the chart
        gc.save();
        gc.beginPath();
        gc.arc(layout.getCenterX(), layout.getCenterY(), layout.getRadius(), layout.getRadius(), 0, 360);
        gc.closePath();
        gc.clip(); // Anything drawn after this will be clipped to the circle

        gc.setLineWidth(1);

        // Draw the Outer Circle (r=0, g=0)
        gc.setStroke(Color.GRAY);
        gc.strokeOval(layout.getCenterX() - layout.getRadius(), layout.getCenterY() - layout.getRadius(), layout.getRadius() * 2, layout.getRadius() * 2);

        // Draw the Horizontal Line (x=0, b=0)
        gc.strokeLine(layout.getCenterX() - layout.getRadius(), layout.getCenterY(), layout.getCenterX() + layout.getRadius(), layout.getCenterY());

        double[] stepValues = {0.2, 0.5, 1.0, 2.0, 4.0, 10.0};

        // ADMITTANCE (Y) GRID

        // Draw Constant Conductance (g) Circles
        gc.setLineWidth(thinLineValue);
        gc.setStroke(Color.CORNFLOWERBLUE);
        for (double g : stepValues) {
            double circleRadius = layout.getRadius() / (g + 1);
            double circleCenterX = layout.getCenterX() - layout.getRadius() * g / (g + 1);
            if (g == 1) gc.setLineWidth(thickLineValue); //If it's the circle that leads to the center of the chart, making the line thicker
            gc.strokeOval(circleCenterX - circleRadius, layout.getCenterY() - circleRadius, circleRadius * 2, circleRadius * 2);
            if (g == 1) gc.setLineWidth(thinLineValue);

            drawLabel(gc, String.format("%.1f mS", g / viewModel.zo.get() * 1000), circleCenterX, layout.getCenterY() - circleRadius, Color.WHITE);
        }

        // Draw Constant Susceptance (b) Arcs
        gc.setStroke(Color.DARKGREEN); // New color for susceptance
        for (double b : stepValues) {
            double arcRadius = layout.getRadius() / b;
            double arcCenterX = layout.getCenterX() - layout.getRadius();

            // Positive Susceptance Arcs (upper half)
            double arcCenterY = layout.getCenterY() - arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);

            // Negative Susceptance Arcs (lower half)
            arcCenterY = layout.getCenterY() + arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);
        }

        // IMPEDANCE (X) GRID
        // Draw Constant Resistance (r) Circles
        gc.setStroke(Color.CORAL); // Color for resistance
        for (double r : stepValues) {
            double circleRadius = layout.getRadius() / (r + 1);
            double circleCenterX = layout.getCenterX() + layout.getRadius() * r / (r + 1);
            if (r == 1) gc.setLineWidth(thickLineValue); //Same logic than for the admittance
            gc.strokeOval(circleCenterX - circleRadius, layout.getCenterY() - circleRadius, circleRadius * 2, circleRadius * 2);
            if (r == 1) gc.setLineWidth(thinLineValue);

            drawLabel(gc, String.format("%.1f", r * viewModel.zo.get()), circleCenterX, layout.getCenterY() + circleRadius, Color.WHITE);
        }

        // Draw Constant Reactance (x) Arcs
        gc.setStroke(Color.BROWN); // Color for reactance
        for (double x : stepValues) {
            double arcRadius = layout.getRadius() / x;
            // Positive Reactance Arcs (upper half)
            double arcCenterX = layout.getCenterX() + layout.getRadius();
            double arcCenterY = layout.getCenterY() - arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);

            // Negative Reactance Arcs (lower half)
            arcCenterY = layout.getCenterY() + arcRadius;
            gc.strokeOval(arcCenterX - arcRadius, arcCenterY - arcRadius, arcRadius * 2, arcRadius * 2);
        }


        //Restore the graphics state to remove the clipping
        gc.restore();

        //Draw the labels on the circles reprensenting the values
        for (double b : stepValues) {
            double angle = 2 * Math.atan(1.0 / b);
            double labelX = layout.getCenterX() + layout.getRadius() * Math.cos(Math.PI - angle);
            double labelY = layout.getCenterY() + layout.getRadius() * Math.sin(Math.PI - angle);
            String label = String.format("+%.1f", viewModel.zo.get() / b);
            drawLabel(gc, label, labelX, labelY, Color.DARKGREEN);

        }

        for (double x : stepValues) {
            double angle = 2 * Math.atan(1.0 / x);
            double labelX = layout.getCenterX() + layout.getRadius() * Math.cos(-angle);
            double labelY = layout.getCenterY() + layout.getRadius() * Math.sin(-angle);
            String label = String.format("%.1f", x * viewModel.zo.get());
            drawLabel(gc, label, labelX, labelY, Color.BROWN);
        }
    }

    /**
     * Draw impedance points based on the gammas calculated in the viewModel
     *
     * @param gc the graphic context on which we'll draw the points
     */
    private void drawImpedancePoints(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout,
                                     int selectedItemIndex, double scale, double offX, double offY) {

        List<Complex> pointsToDraw = new ArrayList<>();

        // Determine which points to draw
        CircuitElement previewElement = viewModel.previewElementProperty().get();
        boolean isPreviewing = previewElement != null && !viewModel.isModifyingComponent.get();

        if (isPreviewing) {
            List<Complex> committedGammas = viewModel.measuresGammaProperty().get();
            int insertionIndex = viewModel.getSelectedInsertionIndexProperty().get();

            if (insertionIndex < 0 || insertionIndex >= committedGammas.size()) {
                insertionIndex = committedGammas.size() - 1;
            }

            // Add all the points before the insertion point
            if (!committedGammas.isEmpty()) {
                pointsToDraw.addAll(committedGammas.subList(0, insertionIndex + 1));
            }

            // Add the PREVIEW point
            Complex previewGamma = viewModel.getPreviewElementGamma();
            if (previewGamma != null) {
                pointsToDraw.add(previewGamma);
            }

            // Add the PROJECTED points
            List<Complex> projectedGammas = viewModel.getProjectedGammas();
            pointsToDraw.addAll(projectedGammas);

        } else {
            pointsToDraw.addAll(viewModel.measuresGammaProperty().get());
        }

        // Draw the points
        if (!pointsToDraw.isEmpty()) {
            int index = 0;
            int selectedInsertionPoint = viewModel.getSelectedInsertionIndexProperty().get();

            for (Complex gamma : pointsToDraw) {
                String labelText = (index == 0) ? "LD" : "DP" + index;

                double localX = layout.toScreenX(gamma);
                double localY = layout.toScreenY(gamma);

                // Store absolute position for Tooltip
                double absoluteX = (localX * scale) + offX;
                double absoluteY = (localY * scale) + offY;

                double pointSize = 5; // logical size

                activePoints.add(new ChartPoint(absoluteX, absoluteY, gamma, viewModel.frequencyProperty().get(), labelText, pointSize * scale, false));

                if (index == selectedInsertionPoint) {
                    // Draw a green circle around the insertion point
                    gc.setStroke(Color.LIME);
                    gc.setLineWidth(2);
                    gc.strokeOval(localX - 2*pointSize, localY - 2*pointSize, pointSize * 4, pointSize * 4);
                }

                // Color Logic
                if (isPreviewing && index == viewModel.getSelectedInsertionIndexProperty().get() + 1) {
                    gc.setStroke(Color.ORANGE);
                    gc.setFill(Color.ORANGE);
                } else if (selectedItemIndex == index) {
                    // Selected in table
                    gc.setStroke(Color.CORAL);
                    gc.setFill(Color.CORAL);
                } else {
                    // Standard point
                    gc.setStroke(Color.YELLOW);
                    gc.setFill(Color.YELLOW);
                }

                gc.strokeRect(localX - pointSize / 2, localY - pointSize / 2, pointSize, pointSize);
                drawLabel(gc, labelText, localX, localY - LABEL_FONT.getSize(), (Color) gc.getFill());

                ++index;
            }
        }
    }

    /**
     * Draw S1P data points using a distinct style.
     *
     * @param gc the graphic context on which we'll draw the points
     */
    private void drawS1PPoints(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout,
                               double scale, double offX, double offY) {
        List<DataPoint> dataPoints = viewModel.transformedS1PPointsProperty().get();

        if (dataPoints != null && !dataPoints.isEmpty()) {
            double pointSize = viewModel.s1pPointSizeProperty().get(); // logical size
            double strokeWidth = viewModel.s1pPointSizeProperty().get() / 4.0;

            int index = 0;
            for (DataPoint dataPoint : dataPoints) {
                Complex gamma = dataPoint.getGamma();

                // Local coordinates (relative to the transformed canvas)
                double localX = layout.toScreenX(gamma);
                double localY = layout.toScreenY(gamma);

                // Calculate ABSOLUTE coordinates for hit testing
                // Formula: (Local * Scale) + Translate
                double absoluteX = (localX * scale) + offX;
                double absoluteY = (localY * scale) + offY;

                // Create and store the ChartPoint
                String label = "S1P" + index++;
                activePoints.add(new ChartPoint(absoluteX, absoluteY, gamma, dataPoint.getFrequency(), label, pointSize * scale, true));

                // Drawing logic
                if(viewModel.isFrequencyInRange(dataPoint.getFrequency())) {
                    gc.setStroke(Color.INDIANRED);
                    gc.setLineWidth(strokeWidth);
                    gc.strokeOval(localX - pointSize / 2, localY - pointSize / 2, pointSize, pointSize);
                } else {
                    gc.setStroke(new Color(
                            Color.DODGERBLUE.getRed(),
                            Color.DODGERBLUE.getGreen(),
                            Color.DODGERBLUE.getBlue(),
                            0.2
                    ));
                    strokeWidth /= 2;
                    gc.setLineWidth(strokeWidth);
                    gc.strokeOval(localX - pointSize / 2, localY - pointSize / 2, pointSize, pointSize);
                    strokeWidth *= 2;
                }
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

    private void drawGhostCursor(GraphicsContext gc, SmithChartViewModel viewModel, double currentScale, SmithChartLayout layout) {
        Complex gamma = viewModel.ghostCursorGamma.get();
        if (gamma == null) return;

        double pointX = layout.toScreenX(gamma);
        double pointY = layout.toScreenY(gamma);

        gc.setStroke(Color.WHITESMOKE);
        gc.setLineWidth(1.5 / currentScale); // Constant thickness regardless of zoom

        double size = 15.0 / currentScale; // Constant size regardless of zoom

        gc.strokeLine(pointX - size, pointY, pointX + size, pointY);
        gc.strokeLine(pointX, pointY - size, pointX, pointY + size);

        double radius = 6.0 / currentScale;
        gc.setLineWidth(2.0 / currentScale);
        gc.strokeOval(pointX - radius, pointY - radius, radius * 2, radius * 2);
    }

    /**
     * Draws the impedance path on the chart based on the circuit elements.
     *
     * @param gc   The GraphicsContext of the canvas.
     */
    private void drawImpedancePath(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout) {
        List<Complex> committedGammas = List.copyOf(viewModel.measuresGammaProperty().get());
        CircuitElement previewElement = viewModel.previewElementProperty().get();
        Complex previewGamma = viewModel.getPreviewElementGamma();

        // If empty and no preview, nothing to draw
        if (committedGammas.isEmpty() && previewElement == null) return;

        double centerX = layout.getCenterX();
        double centerY = layout.getCenterY();
        double mainRadius = layout.getRadius();

        // Setup Clipping
        gc.save();
        gc.beginPath();
        gc.arc(centerX, centerY, mainRadius, mainRadius, 0, 360);
        gc.closePath();
        gc.clip();

        gc.setLineWidth(2);

        // Determine where we stop drawing the pre-existing path
        int insertionIndex = viewModel.getSelectedInsertionIndexProperty().get();

        // Safety clamp
        if (insertionIndex > committedGammas.size() - 1) insertionIndex = committedGammas.size() - 1;
        if (insertionIndex < 0) insertionIndex = committedGammas.size() - 1;

        Complex previousGamma = committedGammas.getFirst(); // Start at Load

        // Draw the circuit up to the insertion point
        previousGamma = drawPath(committedGammas, viewModel, layout, gc, mainRadius, 1, insertionIndex + 1, previousGamma);

        // Draw the preview
        if (previewElement != null && previewGamma != null && !viewModel.isModifyingComponent.get()) {

            // Draw as dotted orange line
            gc.setStroke(Color.ORANGE);
            gc.setLineDashes(5, 5);

            // previousGamma is currently at the insertion point
            drawArcSegment(gc, viewModel, layout, mainRadius, previousGamma, previewGamma, previewElement);

            gc.setLineDashes(0,0); // Reset dashes

            // Draw the projected path after the preview
            List<Complex> projectedGammas = viewModel.getProjectedGammas();
            List<CircuitElement> allElements = viewModel.circuitElements.get();

            if (!projectedGammas.isEmpty()) {
                gc.setStroke(Color.RED);

                // Start from the end of the preview
                Complex tailStartGamma = previewGamma;

                for (int i = 0; i < projectedGammas.size(); i++) {
                    Complex tailEndGamma = projectedGammas.get(i);

                    int elementIndex = insertionIndex + i;

                    if (elementIndex < allElements.size()) {
                        CircuitElement element = allElements.get(elementIndex);
                        drawArcSegment(gc, viewModel, layout, mainRadius, tailStartGamma, tailEndGamma, element);
                    }

                    tailStartGamma = tailEndGamma;
                }
            }
        } else { // No preview, just draw the rest of the committed path
            drawPath(committedGammas, viewModel, layout, gc, mainRadius, insertionIndex + 1, committedGammas.size(), previousGamma);
        }

        gc.restore();
    }

    private Complex drawPath(List<Complex> committedGammas, SmithChartViewModel viewModel, SmithChartLayout layout,
                             GraphicsContext gc, double mainRadius, int startIndex, int endIndex, Complex startingGamma) {
        Complex current = startingGamma;
        for (int i = startIndex; i < endIndex; i++) {
            Complex currGamma = committedGammas.get(i);
            if (viewModel.circuitElements.size() < i) break; // Safety check
            CircuitElement element = viewModel.circuitElements.get(i - 1);

            if (viewModel.selectedElementProperty().isNotNull().get() && viewModel.selectedElementProperty().get().equals(element)) {
                gc.setStroke(Color.LIME);
            } else  gc.setStroke(Color.RED);

            drawArcSegment(gc, viewModel, layout, mainRadius, current, currGamma, element);
            current = currGamma;
        }
        return current;
    }

    private void drawArcSegment(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout,
                                double mainRadius, Complex startGamma, Complex endGamma, CircuitElement element) {

        boolean hasQ = element.getQualityFactor().isPresent()
                && Double.isFinite(element.getQualityFactor().get())
                && element.getQualityFactor().get() > 0;

        if (hasQ && (element.getType() == CircuitElement.ElementType.CAPACITOR ||
                element.getType() == CircuitElement.ElementType.INDUCTOR ||
                element.getType() == CircuitElement.ElementType.LINE)) {

            drawLossyPath(gc, viewModel, layout, startGamma, element);

        } else {
            Complex startImpedance = SmithCalculator.gammaToImpedance(startGamma, viewModel.zo.get());
            Pair<Complex, Double> arcParams = SmithCalculator.getArcParameters(startImpedance, element, viewModel.zo.get());

            Complex arcCenter = arcParams.getKey();
            double arcRadius = arcParams.getValue() * mainRadius;

            // Convert arc center to canvas coordinates
            double arcCenterX = layout.toScreenX(arcCenter);
            double arcCenterY = layout.toScreenY(arcCenter);

            // Calculate start and end angles
            double startAngle = Math.toDegrees(Math.atan2(
                    startGamma.imag() - arcCenter.imag(),
                    startGamma.real() - arcCenter.real()
            ));

            double endAngle = Math.toDegrees(Math.atan2(
                    endGamma.imag() - arcCenter.imag(),
                    endGamma.real() - arcCenter.real()
            ));

            // Check which direction the arc should be drawn in
            int expectedDirection = SmithCalculator.getExpectedDirection(element, startGamma);

            // Calculate the raw angle difference
            double arcExtent = endAngle - startAngle;

            // Normalize to the shortest path first (-180 to 180)
            while (arcExtent <= -180) arcExtent += 360;
            while (arcExtent > 180) arcExtent -= 360;

            // Correct which path to take
            if (arcExtent != 0 && Math.signum(arcExtent) != expectedDirection) {
                // If the shortest path was positive (CCW) but we expected negative (CW), subtract 360.
                // If the shortest path was negative (CW) but we expected positive (CCW), add 360.
                arcExtent = (arcExtent > 0) ? arcExtent - 360 : arcExtent + 360;
            }

            // Draw the arc
            gc.strokeArc(
                    arcCenterX - arcRadius,
                    arcCenterY - arcRadius,
                    arcRadius * 2,
                    arcRadius * 2,
                    startAngle,
                    arcExtent,
                    ArcType.OPEN
            );
        }
    }

    private void drawLossyPath(GraphicsContext gc, SmithChartViewModel viewModel, SmithChartLayout layout,
                               Complex startGamma, CircuitElement element) {
        List<Complex> points = SmithCalculator.getLossyComponentPath(
                startGamma,
                element,
                viewModel.zo.get(),
                viewModel.frequencyProperty().get()
        );

        if (points.size() < 2) return;

        double[] xPoints = new double[points.size()];
        double[] yPoints = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            xPoints[i] = layout.toScreenX(points.get(i));
            yPoints[i] = layout.toScreenY(points.get(i));
        }

        gc.strokePolyline(xPoints, yPoints, points.size());
    }

    /**
     * Clears the cursor canvas.
     *
     * @param gc The GraphicsContext of the cursor canvas.
     */
    public void clearCursor(GraphicsContext gc) {
        gc.clearRect(0, 0, cursorCanvas.getWidth(), cursorCanvas.getHeight());
    }

    public List<ChartPoint> getActivePoints() {
        return activePoints;
    }

    public Complex getCurrentSelectedGamma() {
        if (currentSelectedIndex < 0 || currentSelectedIndex >= activePoints.size()) {
            return null;
        }

        return activePoints.get(currentSelectedIndex).gamma();
    }

    public void handleTooltip(double mouseX, double mouseY, double scale) {
        // Constant padding independent of zoom for consistent UX
        double padding = 6.0;
        ChartPoint hitPoint = null;

        // Find the closest point
        int index = 0;
        for (ChartPoint p : activePoints) {
            if (p.isHit(mouseX, mouseY, padding)) {
                hitPoint = p;
                break; // Stop at first hit
            }
            ++index;
        }

        GraphicsContext gc = cursorCanvas.getGraphicsContext2D();

        if (index == currentSelectedIndex) return;

        clearCursor(gc);
        if (hitPoint != null) {
            drawTooltip(gc, hitPoint, hitPoint.screenX(), hitPoint.screenY());
            currentSelectedIndex = index;
        } else {
            currentSelectedIndex = -1;
        }
    }
    private void drawTooltip(GraphicsContext gc, ChartPoint point, double mx, double my) {
        Font font = new Font("Arial", 12);
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.LEFT);

        String[] lines = point.getTooltipText().split("\n");
        double padding = 6.0;

        double maxLineWidth = 0.0;
        double lineHeight = 0.0;
        for (String line : lines) {
            Text txt = new Text(line);
            txt.setFont(font);
            double w = txt.getLayoutBounds().getWidth();
            if (w > maxLineWidth) maxLineWidth = w;
            if (lineHeight == 0.0) lineHeight = txt.getLayoutBounds().getHeight();
        }

        double textWidth = maxLineWidth;
        double textHeight = lineHeight * lines.length;
        double boxWidth = textWidth + padding * 2;
        double boxHeight = textHeight + padding * 2;

        double boxX = mx + 12;
        double boxY = my + 12;

        // Ensure tooltip stays within canvas bounds
        if (boxX + boxWidth > cursorCanvas.getWidth()) {
            boxX = cursorCanvas.getWidth() - boxWidth - 4;
        }
        if (boxY + boxHeight > cursorCanvas.getHeight()) {
            boxY = cursorCanvas.getHeight() - boxHeight - 4;
        }

        gc.setFill(Color.rgb(0, 0, 0, 0.65));
        gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);

        gc.setFill(Color.WHITE);
        double yCursor = boxY + padding + lineHeight * 0.8; // baseline adjustment
        for (String line : lines) {
            gc.fillText(line, boxX + padding, yCursor);
            yCursor += lineHeight;
        }
    }


}

