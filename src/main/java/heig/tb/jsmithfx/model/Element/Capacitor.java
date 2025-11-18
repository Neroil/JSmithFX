package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

public class Capacitor extends CircuitElement {

    public Capacitor(double capacitance, ElementPosition elementPosition,  ElementType elementType) {
        super(capacitance, elementPosition, elementType);
    }

    @Override
    public Complex getImpedance(double frequency) {
        if (getRealWorldValue() == 0 || frequency == 0) {
            return new Complex(Double.POSITIVE_INFINITY, 0); // Open circuit for zero capacitance or frequency
        }
        // X_C = -1 / 2 * pi * f * C
        return new Complex(0, -1 / (2 * Math.PI * frequency * getRealWorldValue()));
    }

    @Override
    public String toString() {
        return "capacitor";
    }
}
