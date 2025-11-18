package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class CircuitRenderer {

    private static final double PADDING_RATIO = 0.05;
    private static final double PADDING = 25;
    private static final double DEFAULT_LINE_WIDTH = 2.0;
    private static final double MAX_PADDING = 64;
    private static final double GRID_SPACING = 20;

    // Dark mode colors
    private static final Color GRID_COLOR = Color.rgb(255, 255, 255, 0.3);
    private static final Color WIRE_COLOR = Color.rgb(200, 200, 200);
    private static final Color COMPONENT_COLOR = Color.rgb(180, 180, 255);
    private static final Color COMPONENT_FILL = Color.rgb(40, 40, 60, 0.3);
    private static final Color LABEL_COLOR = Color.rgb(220, 220, 220);
    private static final Color JUNCTION_COLOR = Color.rgb(255, 180, 100);

    private final Canvas circuitCanvas;
    private Font labelFont;

    public CircuitRenderer(Canvas canvas) {
        this.circuitCanvas = canvas;
        this.labelFont = new Font("Segoe UI", 12);
    }

    public void render(SmithChartViewModel viewModel) {
        GraphicsContext gc = circuitCanvas.getGraphicsContext2D();
        List<CircuitElement> elements = viewModel.circuitElements.get();

        double canvasWidth = circuitCanvas.getWidth();
        double canvasHeight = circuitCanvas.getHeight();

        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        gc.setStroke(WIRE_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(labelFont);

        double forkBottomY = canvasHeight / 2 + canvasHeight / 3;
        double lineY = 10;

        // Draw the wire
        gc.strokeLine(PADDING, lineY, canvasWidth - PADDING, lineY);
        // Draw the forks down
        // Left fork
        gc.strokeLine(PADDING, lineY, PADDING, forkBottomY);
        drawGround(PADDING,forkBottomY,gc);
        // Right fork
        gc.setStroke(WIRE_COLOR);
        gc.strokeLine(canvasWidth - PADDING, lineY, canvasWidth - PADDING, forkBottomY);
        drawGround(canvasWidth - PADDING,forkBottomY,gc);

        //Draw the source
        drawSource(gc, PADDING, (forkBottomY - lineY)/2 + lineY );

        int gridElements = elements.size() + 2; // source + elements + load

        // Draw grid lines for visual aid
        double slotWidth = canvasWidth / (gridElements - 1);
        for (int i = 0; i < gridElements; i++) {
            double x = i * slotWidth;
            gc.setStroke(GRID_COLOR);
            gc.setLineWidth(1.0);
            gc.strokeLine(x, 0, x, canvasHeight);
        }


        // Draw elements in between
        for (int i = 0; i < elements.size(); i++) {
            CircuitElement element = elements.get(i);
            double x = (i + 1) * slotWidth;
            switch (element.getType()) {
                case RESISTOR -> drawResistor(gc,x,lineY);
                case CAPACITOR ->  drawCapacitor(gc,x,lineY);
                case INDUCTOR ->  drawInductor(gc,x,lineY);
            }
        }
    }

    private void drawGround(double x, double y, GraphicsContext gc) {
        double lineSpacing = 4;
        double topWidth = 18;
        double midWidth = 12;
        double botWidth = 6;

        gc.setStroke(JUNCTION_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);

        // Top line
        gc.strokeLine(x - topWidth / 2, y, x + topWidth / 2, y);
        // Middle line
        gc.strokeLine(x - midWidth / 2, y + lineSpacing, x + midWidth / 2, y + lineSpacing);
        // Bottom line
        gc.strokeLine(x - botWidth / 2, y + 2 * lineSpacing, x + botWidth / 2, y + 2 * lineSpacing);
    }

    private void drawSource(GraphicsContext gc, double x, double y) {
        double radius = 20;
        double innerPadding = radius / 2;
        double diameter = radius * 2;

        gc.clearRect(x - radius, y - radius, diameter, diameter);

        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.strokeOval(x - radius, y - radius, diameter, diameter);

        gc.beginPath();
        gc.moveTo(x - innerPadding, y);
        gc.quadraticCurveTo(x - innerPadding / 2, y - innerPadding, x, y);
        gc.quadraticCurveTo(x + innerPadding / 2, y + innerPadding, x + innerPadding, y);
        gc.stroke();
    }

    private void drawResistor(GraphicsContext gc,double x, double y) {
        double width = 40;
        double height = 16;
        double left = x - width / 2;
        double top = y - height / 2;
        double nubLength = 6;

        // Draw rectangle as resistor body
        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.clearRect(left, top, width, height);
        gc.strokeRect(left, top, width, height);
        gc.strokeLine(left - nubLength, y, left, y);
        gc.strokeLine(left + width, y, left + width + nubLength, y);
    }

    private void drawCapacitor(GraphicsContext gc, double x, double y) {
        double width = 10;
        double height = 25;
        double left = x - width / 2;
        double top = y - height / 2;
        double bottom = y + height / 2;
        double nubLength = 6;

        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.clearRect(left, top, width, height);

        gc.strokeLine(left, top, left, bottom);
        gc.strokeLine(left + width, top, left + width, bottom);
        gc.strokeLine(left - nubLength, y, left, y);
        gc.strokeLine(left + width, y, left + width + nubLength, y);
    }

    private void drawInductor(GraphicsContext gc, double x, double y) {
        double width = 40;
        double height = 16;
        double left = x - width / 2;
        double top = y - height / 2;
        double nubLength = 6;

        gc.clearRect(left, top, width, height);
        double start = left;
        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        for (int i = 0; i < 4 ; ++i){
            gc.strokeOval(start, top, width/4, height);
            start += width/4;
        }

        gc.clearRect(left - DEFAULT_LINE_WIDTH, top + height / 2, width + 2 * DEFAULT_LINE_WIDTH, height / 2 + DEFAULT_LINE_WIDTH);
        gc.strokeLine(left - nubLength, y, left, y);
        gc.strokeLine(left + width, y, left + width + nubLength, y);

    }



}
