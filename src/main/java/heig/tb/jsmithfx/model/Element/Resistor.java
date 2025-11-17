package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

public class Resistor extends CircuitElement {

    public Resistor(double realWorldValue, ElementPosition elementPosition, ElementType elementType) {
        super(realWorldValue, elementPosition, elementType);
    }

    @Override
    public Complex getImpedance(double frequency) {
        return new Complex(getRealWorldValue(), 0);
    }

    @Override
    public String toString(){
        return "resistor";
    }
}
