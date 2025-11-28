package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.utilities.Complex;
import javafx.geometry.Point2D;

public class SmithChartLayout {

    private final double width;
    private final double height;
    private final double centerX;
    private final double centerY;
    private final double radius;

    // Define margin constant here
    private static final double MARGIN = 10.0;

    public SmithChartLayout(double width, double height) {
        this.width = width;
        this.height = height;
        this.centerX = width / 2;
        this.centerY = height / 2;
        this.radius = Math.min(centerX, centerY) - MARGIN;
    }

    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getRadius() { return radius; }

    /**
     * Converts a Gamma complex value to an X pixel coordinate.
     */
    public double toScreenX(Complex gamma) {
        return centerX + gamma.real() * radius;
    }

    /**
     * Converts a Gamma complex value to a Y pixel coordinate.
     * Note: Inverts Y because JavaFX canvas Y increases downwards.
     */
    public double toScreenY(Complex gamma) {
        return centerY - gamma.imag() * radius;
    }

    /**
     * Returns a JavaFX Point2D for a given Gamma.
     */
    public Point2D toScreenPoint(Complex gamma) {
        return new Point2D(toScreenX(gamma), toScreenY(gamma));
    }

    /**
     * Helper to get screen X for a raw scalar value relative to center (for circles)
     */
    public double scalarToScreenX(double value) {
        return centerX + value * radius;
    }
}