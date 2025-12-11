package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

public class Inductor extends CircuitElement {

    public Inductor(double inductance, ElementPosition elementPosition,   ElementType elementType) {
        super(inductance, elementPosition,elementType);
    }

    @Override
    public Complex getImpedance(double frequency) {
        if (getRealWorldValue() == 0 || frequency == 0) {
            return new Complex(0, 0); // Short circuit
        }

        double reactance = 2 * Math.PI * frequency * getRealWorldValue();
        double resistiveLoss = 0;

        if (qualityFactor.isPresent() && qualityFactor.get() > 0) {
            resistiveLoss = reactance / qualityFactor.get();
        }

        // Z_L = R + jωL
        return new Complex(resistiveLoss, reactance);
    }

    @Override
    public String toString() {
        return "inductor";
    }

}
