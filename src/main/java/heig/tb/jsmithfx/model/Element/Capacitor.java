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
            return new Complex(Double.POSITIVE_INFINITY, 0); // Open circuit
        }

        double omega = 2 * Math.PI * frequency;
        double reactance = -1.0 / (omega * getRealWorldValue());
        double resistiveLoss = 0;

        if (qualityFactor.isPresent() && qualityFactor.get() > 0) {
            resistiveLoss = Math.abs(reactance) / qualityFactor.get();
        }

        // Z_C = ESR - j/(ωC)
        return new Complex(resistiveLoss, reactance);
    }

    @Override
    public String toString() {
        return "capacitor";
    }
}
