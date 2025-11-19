package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

import java.util.List;

public class CircuitRenderer {

    private static final double PADDING = 25;
    private static final double DEFAULT_LINE_WIDTH = 2.0;

    // Dark mode colors
    private static final Color WIRE_COLOR = Color.rgb(200, 200, 200);
    private static final Color COMPONENT_COLOR = Color.rgb(180, 180, 255);
    private static final Color LABEL_COLOR = Color.rgb(220, 220, 220);
    private static final Color JUNCTION_COLOR = Color.rgb(255, 180, 100);

    private final Canvas circuitCanvas;
    private final Font labelFont;

    private static final double LINE_Y = 10;
    private static final double SOURCE_RADIUS = 20;

    private static final double GROUND_LINE_SPACING = 4;
    private static final double GROUND_TOP_WIDTH = 18;
    private static final double GROUND_MID_WIDTH = 12;
    private static final double GROUND_BOTTOM_WIDTH = 6;

    private static final double RESISTOR_WIDTH = 40;
    private static final double RESISTOR_HEIGHT = 16;
    private static final double RESISTOR_NUB_LENGTH = 6;

    private static final double CAPACITOR_WIDTH = 10;
    private static final double CAPACITOR_HEIGHT = 25;
    private static final double CAPACITOR_NUB_LENGTH = 6;

    private static final double INDUCTOR_WIDTH = 40;
    private static final double INDUCTOR_HEIGHT = 16;
    private static final double INDUCTOR_NUB_LENGTH = 6;
    private static final int INDUCTOR_LOOPS = 4;

    public CircuitRenderer(Canvas canvas) {
        this.circuitCanvas = canvas;
        this.labelFont = new Font("Segoe UI", 16);
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
        double lineY = LINE_Y;

        // Draw the wire
        gc.strokeLine(PADDING, lineY, canvasWidth - PADDING, lineY);
        // Draw the forks down
        // Left fork
        drawForkDown(PADDING,lineY,forkBottomY,gc);
        // Right fork
        drawForkDown(canvasWidth - PADDING, lineY,forkBottomY,gc);

        //Draw the source
        drawSource(gc, PADDING, (forkBottomY - lineY)/2 + lineY );

        // Calculate component grid
        double slotWidth = (canvasWidth) / (elements.size() + 1) ;

        // Draw elements in between
        for (int i = 0; i < elements.size(); i++) {
            CircuitElement element = elements.get(i);
            var type = element.getType();
            ElectronicUnit[] units = new ElectronicUnit[0];
            double x = (i + 1) * slotWidth;
            double y = lineY;
            if (element.getPosition() == CircuitElement.ElementPosition.PARALLEL) {
                drawForkDown(x,lineY,forkBottomY,gc);
                gc.save();
                gc.translate(x, y + forkBottomY / 2);
                gc.rotate(90);
                x = 0;
                y = 0;
            }
            switch (type) {
                case RESISTOR -> {
                    drawResistor(gc,x,y);
                    units = ResistanceUnit.values();
                }
                case CAPACITOR ->  {
                    drawCapacitor(gc,x,y);
                    units = CapacitanceUnit.values();
                }
                case INDUCTOR ->  {
                    drawInductor(gc,x,y);
                    units = InductanceUnit.values();
                }
            }
            if (units.length > 0) {

                Pair<ElectronicUnit, String> res = SmithUtilities.getBestUnitAndFormattedValue(element.getRealWorldValue(),units);


                //Draw value
                gc.setFill(LABEL_COLOR);
                gc.fillText(res.getValue() + " " + res.getKey(),x, labelFont.getSize() * 3);
            }

            if (element.getPosition() == CircuitElement.ElementPosition.PARALLEL) gc.restore();

        }
    }

    private void drawForkDown(double x, double y1,double y2, GraphicsContext gc) {
        gc.setStroke(WIRE_COLOR);
        gc.strokeLine(x, y1, x, y2);
        drawGround(x,y2,gc);
    }

    private void drawGround(double x, double y, GraphicsContext gc) {
        double lineSpacing = GROUND_LINE_SPACING;
        double topWidth = GROUND_TOP_WIDTH;
        double midWidth = GROUND_MID_WIDTH;
        double botWidth = GROUND_BOTTOM_WIDTH;

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
        double radius = SOURCE_RADIUS;
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
        double width = RESISTOR_WIDTH;
        double height = RESISTOR_HEIGHT;
        double left = x - width / 2;
        double top = y - height / 2;
        double nubLength = RESISTOR_NUB_LENGTH;

        // Draw rectangle as resistor body
        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.clearRect(left, top, width, height);
        gc.strokeRect(left, top, width, height);
        gc.strokeLine(left - nubLength, y, left, y);
        gc.strokeLine(left + width, y, left + width + nubLength, y);
    }

    private void drawCapacitor(GraphicsContext gc, double x, double y) {
        double width = CAPACITOR_WIDTH;
        double height = CAPACITOR_HEIGHT;
        double left = x - width / 2;
        double top = y - height / 2;
        double bottom = y + height / 2;
        double nubLength = CAPACITOR_NUB_LENGTH;

        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.clearRect(left, top, width, height);

        gc.strokeLine(left, top, left, bottom);
        gc.strokeLine(left + width, top, left + width, bottom);
        gc.strokeLine(left - nubLength, y, left, y);
        gc.strokeLine(left + width, y, left + width + nubLength, y);
    }

    private void drawInductor(GraphicsContext gc, double x, double y) {
        double width = INDUCTOR_WIDTH;
        double height = INDUCTOR_HEIGHT;
        double left = x - width / 2;
        double top = y - height / 2;
        double nubLength = INDUCTOR_NUB_LENGTH;

        gc.clearRect(left, top, width, height);
        double start = left;
        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        for (int i = 0; i < INDUCTOR_LOOPS ; ++i){
            gc.strokeOval(start, top, width/INDUCTOR_LOOPS, height);
            start += width/INDUCTOR_LOOPS;
        }

        gc.clearRect(left - DEFAULT_LINE_WIDTH, top + height / 2, width + 2 * DEFAULT_LINE_WIDTH, height / 2 + DEFAULT_LINE_WIDTH);
        gc.strokeLine(left - nubLength, y, left, y);
        gc.strokeLine(left + width, y, left + width + nubLength, y);

    }



}
