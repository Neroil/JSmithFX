package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.SmithChartViewModel;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.TypicalUnit.*;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircuitRenderer {

    private static final double PADDING = 25;
    private static final double DEFAULT_LINE_WIDTH = 2.0;

    // Dark mode colors
    private static final Color WIRE_COLOR = Color.rgb(200, 200, 200);
    private static final Color COMPONENT_COLOR = Color.rgb(180, 180, 255);
    private static final Color LABEL_COLOR = Color.rgb(220, 220, 220);
    private static final Color JUNCTION_COLOR = Color.rgb(255, 180, 100);
    private static final Color SELECTION_COLOR = Color.YELLOW;
    private static final Color HOVER_COLOR = Color.BLUEVIOLET;


    private final Canvas circuitCanvas;
    private final Font labelFont;

    // Hitbox map for editing components
    private final Map<CircuitElement, Rectangle2D> hitBoxes = new HashMap<>();
    private final Map<Integer, Rectangle2D> insertionHitBoxes = new HashMap<>();

    private static final double INSERTION_HITBOX_SIZE = 20;
    private static final double INSERTION_DOT_RADIUS = 5;
    private static final Color INSERTION_DOT_COLOR = Color.rgb(150, 150, 150);
    private static final Color INSERTION_SELECT_COLOR = Color.LIGHTGREEN;

    private static final double LINE_Y = 10;
    private static final double SOURCE_RADIUS = 20;

    private static final double GROUND_LINE_SPACING = 4;
    private static final double GROUND_TOP_WIDTH = 18;
    private static final double GROUND_MID_WIDTH = 12;
    private static final double GROUND_BOTTOM_WIDTH = 6;

    private static final double RESISTOR_WIDTH = 40;
    private static final double RESISTOR_HEIGHT = 16;
    private static final double RESISTOR_NUB_LENGTH = 6;

    private static final double LINE_WIDTH = 50;
    private static final double LINE_HEIGHT = 8;
    private static final double LINE_NUB_LENGTH = 6;

    private static final double CAPACITOR_WIDTH = 10;
    private static final double CAPACITOR_HEIGHT = 25;
    private static final double CAPACITOR_NUB_LENGTH = 6;

    private static final double INDUCTOR_WIDTH = 40;
    private static final double INDUCTOR_HEIGHT = 16;
    private static final double INDUCTOR_NUB_LENGTH = 6;
    private static final int INDUCTOR_LOOPS = 4;

    private static final double MAX_COMPONENT_W = 50;
    private static final double MAX_COMPONENT_H = 30;

    public CircuitRenderer(Canvas canvas) {
        this.circuitCanvas = canvas;
        this.labelFont = new Font("Segoe UI", 16);
    }

    /**
     * Checks if a user clicked on a specific circuit element.
     * @param x mouse X
     * @param y mouse Y
     * @return The element clicked, or null.
     */
    public CircuitElement getElementAt(double x, double y) {
        for (Map.Entry<CircuitElement, Rectangle2D> entry : hitBoxes.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Checks if a user clicked on an insertion point between elements.
     * @param x mouse X
     * @param y mouse Y
     * @return The insertion index, or -1.
     */
    public int getInsertionIndexAt(double x, double y) {
        for (Map.Entry<Integer, Rectangle2D> entry : insertionHitBoxes.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public void render(SmithChartViewModel viewModel) {
        GraphicsContext gc = circuitCanvas.getGraphicsContext2D();
        List<CircuitElement> elements = viewModel.circuitElements.get();

        // Reset hitboxes
        hitBoxes.clear();

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
        // Draw the source
        drawSource(canvasWidth - PADDING, (forkBottomY - lineY)/2 + lineY, gc);
        // Draw the load
        drawLoad(PADDING, (forkBottomY - lineY)/2 + lineY, gc);

        // Calculate component grid
        double slotWidth = (canvasWidth) / (elements.size() + 1) ;
        renderInsertionPoints(gc, elements.size(), slotWidth, lineY, viewModel);

        // Draw elements in between
        for (int i = 0; i < elements.size(); i++) {
            CircuitElement element = elements.get(i);
            var type = element.getType();
            ElectronicUnit[] units = new ElectronicUnit[0];
            double x = (i + 1) * slotWidth;
            double y = lineY;
            if (element.getPosition() == CircuitElement.ElementPosition.PARALLEL) {
                if(type == CircuitElement.ElementType.LINE) drawForkDown(x,lineY,forkBottomY,gc,((Line)element).getStubType() == Line.StubType.OPEN );
                else drawForkDown(x,lineY,forkBottomY,gc);
                gc.save();
                gc.translate(x, y + forkBottomY / 2);
                gc.rotate(90);

                registerHitBox(element, x, y + forkBottomY / 2, true);

                x = 0;
                y = 0;
            } else {
                registerHitBox(element, x, y, false);
            }

            // Draw a little Q or loss indicator if the element has a quality factor
            if (element.getQualityFactor().isPresent()) {
                gc.setFill(JUNCTION_COLOR);
                String text;
                if (element.getType().equals(CircuitElement.ElementType.LINE)) {
                    text = "Loss=";
                } else {
                    text = "Q=";
                }
                gc.fillText(text + String.format("%.1f", element.getQualityFactor().get()), x, labelFont.getSize() * 4);
            }

            switch (type) {
                case RESISTOR -> {
                    drawResistor(x, y, gc);
                    units = ResistanceUnit.values();
                }
                case CAPACITOR ->  {
                    drawCapacitor(x, y, gc);
                    units = CapacitanceUnit.values();
                }
                case INDUCTOR ->  {
                    drawInductor(x, y, gc);
                    units = InductanceUnit.values();
                }
                case LINE -> {
                    drawLine(x,y, ((Line)element).getStubType() == Line.StubType.OPEN, gc);
                    units = DistanceUnit.values();
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

        // Draw highlight for selected element
        CircuitElement selectedElement = viewModel.selectedElementProperty().get();
        CircuitElement hoveredElement = viewModel.hoveredElementProperty().get();
        if (selectedElement != null || hoveredElement != null) {
            if (selectedElement == null) {
                selectedElement = hoveredElement;
                gc.setFill(new Color(HOVER_COLOR.getRed(), HOVER_COLOR.getGreen(), HOVER_COLOR.getBlue(), 0.4));
            } else {
                gc.setFill(new Color(SELECTION_COLOR.getRed(), SELECTION_COLOR.getGreen(), SELECTION_COLOR.getBlue(), 0.4));
            }
            Rectangle2D hitBox = hitBoxes.get(selectedElement);
            if (hitBox != null) {
                double padding = 10;
                gc.fillRect(
                        hitBox.getMinX() - padding / 2,
                        hitBox.getMinY() - padding / 2,
                        hitBox.getWidth() + padding,
                        hitBox.getHeight() + padding
                );
            }
        }
    }

    private void renderInsertionPoints(GraphicsContext gc, int numElements, double slotWidth, double y, SmithChartViewModel viewModel) {
        // There are numElements + 1 insertion slots
        for (int i = 0; i <= numElements; i++) {
            double x = (i + 0.5) * slotWidth;

            // Register the hitbox for mouse detection
            double hitSize = INSERTION_HITBOX_SIZE;
            insertionHitBoxes.put(i, new Rectangle2D(x - hitSize/2, y - hitSize/2, hitSize, hitSize));

            // Determine visual style
            boolean isSelected = (i == viewModel.getSelectedInsertionIndexProperty().get());

            // Bigger dot if selected
            double r = isSelected ? INSERTION_DOT_RADIUS * 1.5 : INSERTION_DOT_RADIUS;
            Color c = isSelected ? INSERTION_SELECT_COLOR : INSERTION_DOT_COLOR;

            gc.setFill(c);
            gc.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
    }

    private void registerHitBox(CircuitElement element, double centerX, double centerY, boolean isRotated) {
        double w = isRotated ? MAX_COMPONENT_H : MAX_COMPONENT_W;
        double h = isRotated ? MAX_COMPONENT_W : MAX_COMPONENT_H;

        hitBoxes.put(element, new Rectangle2D(centerX - w/2, centerY - h/2, w, h));
    }

    private void drawForkDown(double x, double y1,double y2, GraphicsContext gc) {
        drawForkDown(x,y1,y2,gc,false);
    }

    private void drawForkDown(double x, double y1,double y2, GraphicsContext gc, boolean isOpenStub) {
        gc.setStroke(WIRE_COLOR);
        gc.strokeLine(x, y1, x, y2);
        if(!isOpenStub)drawGround(x,y2,gc);
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

    private void drawSource(double x, double y, GraphicsContext gc) {
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

    private void drawLoad(double x, double y, GraphicsContext gc) {
        double width = RESISTOR_HEIGHT;
        double height = RESISTOR_WIDTH;
        double left = x - width / 2;
        double top = y - height / 2;
        double nubLength = RESISTOR_NUB_LENGTH;

        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.clearRect(left, top, width, height);
        gc.strokeRect(left, top, width, height);
        gc.strokeLine(x, top - nubLength, x, top); // Top nub
        gc.strokeLine(x, top + height, x, top + height + nubLength); // Bottom nub
    }

    private void drawResistor(double x, double y, GraphicsContext gc) {
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

    private void drawLine(double x, double y, boolean isOpenStub, GraphicsContext gc) {
        double width = LINE_WIDTH;
        double height = LINE_HEIGHT;
        double left = x - width / 2;
        double top = y - height / 2;
        double nubLength = LINE_NUB_LENGTH;

        // Draw rectangle as resistor body
        gc.setStroke(COMPONENT_COLOR);
        gc.setLineWidth(DEFAULT_LINE_WIDTH);
        gc.clearRect(left, top, width, height);
        gc.strokeRect(left, top, width, height);
        gc.strokeLine(left - nubLength, y, left, y);
        if (!isOpenStub) gc.strokeLine(left + width, y, left + width + nubLength, y);
    }

    private void drawCapacitor(double x, double y, GraphicsContext gc) {
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

    private void drawInductor(double x, double y, GraphicsContext gc) {
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
