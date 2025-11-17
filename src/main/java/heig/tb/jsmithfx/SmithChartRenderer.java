package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

import java.util.List;

public class SmithChartRenderer {

    private final Canvas smithCanvas;
    private final double thickLineValue = 1;
    private final double thinLineValue = 0.4;
    private Font LABEL_FONT = new Font("Arial", 10);

    public SmithChartRenderer(Canvas smithCanvas) {
        this.smithCanvas = smithCanvas;
    }

    /**
     * Clears and redraws the entire canvas. This method will be called whenever the data
     * or the window size changes.
     */
    public void render(SmithChartViewModel viewModel, double currentScale, double offsetX, double offsetY, int selectedIndex) {
        GraphicsContext gc = smithCanvas.getGraphicsContext2D();

        gc.save();

        // Clear the canvas before redrawing
        gc.clearRect(0, 0, smithCanvas.getWidth(), smithCanvas.getHeight());

        gc.translate(offsetX, offsetY);
        gc.scale(currentScale, currentScale);

        updateFontSize();

        // Draw the static parts of the chart
        drawSmithGrid(gc, viewModel);
        // Draw the path between the impedances
        drawImpedancePath(gc, viewModel);
        // Draw the impedances
        drawImpedancePoints(gc, viewModel, selectedIndex);

        gc.restore();
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
    private void drawSmithGrid(GraphicsContext gc, SmithChartViewModel viewModel) {
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
    private void drawImpedancePoints(GraphicsContext gc, SmithChartViewModel viewModel, int selectedItemIndex) {
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

                String labelText = (index == 0) ? "LP" : "DP" + index;
                Color labelColor;

                double pointX = centerX + gamma.real() * mainRadius;
                double pointY = centerY - gamma.imag() * mainRadius;

                gc.setLineWidth(2 * thickLineValue);
                double pointSize = 5;

                //Change the color of the selected position on the chart
                if (selectedItemIndex == index) {
                    gc.setStroke(Color.CORAL);
                    gc.setFill(Color.CORAL);
                    labelColor = Color.CORAL;
                } else {
                    gc.setStroke(Color.YELLOW);
                    gc.setFill(Color.YELLOW);
                    labelColor = Color.YELLOW;
                }

                //Draw the point
                gc.strokeRect(pointX - pointSize / 2, pointY - pointSize / 2, pointSize, pointSize);

                //Add the label on top of it
                drawLabel(gc, labelText, pointX, pointY - LABEL_FONT.getSize(), labelColor);

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
    private void drawImpedancePath(GraphicsContext gc, SmithChartViewModel viewModel) {
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
            Complex startImpedance = SmithUtilities.gammaToImpedance(previousGamma, viewModel.zo.get());
            Pair<Complex, Double> arcParams = SmithUtilities.getArcParameters(startImpedance, element, viewModel.zo.get());

            Complex arcCenter = arcParams.getKey();
            double arcRadius = arcParams.getValue() * mainRadius;

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

            // Check which direction the arc should be drawn in
            int expectedDirection = SmithUtilities.getExpectedDirection(element, previousGamma);

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

            previousGamma = currGamma;
        }

        gc.restore();
    }
}