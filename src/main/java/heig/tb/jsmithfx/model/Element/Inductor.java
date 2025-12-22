package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.logic.SmithCalculator;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

import java.util.Optional;

public class Inductor extends CircuitElement {

    // Default constructor for Jackson
    protected Inductor(){
        super();
    }

    public Inductor(double inductance, ElementPosition elementPosition,   ElementType elementType) {
        super(inductance, elementPosition,elementType);
    }

    public static Complex getImpedanceStatic(double inductance, double frequency,  Optional<Double> qualityFactor, ElementPosition elementPosition) {
        if (inductance == 0 || frequency == 0) {
            return new Complex(0, 0); // Short circuit
        }

        double reactance = 2 * Math.PI * frequency * inductance;

        // If no Q is present, pure reactance
        if (qualityFactor.isEmpty() || qualityFactor.get() <= 0) {
            return new Complex(0, reactance);
        }

        double Q = qualityFactor.get();

        if (elementPosition == ElementPosition.PARALLEL) {
            // PARALLEL MODEL: Q = Rp / X  ->  Rp = Q * X
            double Rp = reactance * Q;

            Complex Z_Rp = new Complex(Rp, 0);
            Complex Z_L  = new Complex(0, reactance);

            return SmithCalculator.addParallelImpedance(Z_Rp, Z_L);
        } else {
            // SERIES MODEL: Q = X / Rs  ->  Rs = X / Q
            double Rs = reactance / Q;
            return new Complex(Rs, reactance);
        }
    }

    @Override
    public Complex getImpedance(double frequency) {
        return getImpedanceStatic(realWorldValue.get(), frequency, getQualityFactor(), this.elementPosition);
    }

    @Override
    public String toString() {
        return "inductor";
    }

}
