package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

public class Inductor extends CircuitElement {

    public Inductor(double inductance, ElementPosition elementPosition,   ElementType elementType) {
        super(inductance, elementPosition,elementType);
    }

    @Override
    public Complex getImpedance(double frequency) {
        if (realWorldValue == 0 || frequency == 0) {
            return new Complex(0, 0); // Short circuit for zero inductance or frequency
        }
        // X_L = 2 * pi * f * L
        return new Complex(0, 2 * Math.PI * frequency * realWorldValue);
    }

    @Override
    public String toString() {
        return "inductor";
    }

}
