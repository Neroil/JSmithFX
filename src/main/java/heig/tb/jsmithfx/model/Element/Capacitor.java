package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.logic.SmithCalculator;
import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

import java.util.Optional;

public class Capacitor extends CircuitElement {

    protected Capacitor() {
        super();
    }

    public Capacitor(double capacitance, ElementPosition elementPosition,  ElementType elementType) {
        super(capacitance, elementPosition, elementType);
    }

    public static Complex getImpedanceStatic(double capacitance, double frequency, Optional<Double> qualityFactor, ElementPosition elementPosition) {
        if (capacitance == 0 || frequency == 0) {
            return new Complex(Double.POSITIVE_INFINITY, 0); // Open circuit
        }

        double omega = 2 * Math.PI * frequency;
        double reactance = -1.0 / (omega * capacitance);

        // If no Q is present, pure reactance
        if (qualityFactor.isEmpty() || qualityFactor.get() <= 0) {
            return new Complex(0, reactance);
        }

        double Q = qualityFactor.get();
        double absX = Math.abs(reactance);

        if (elementPosition == ElementPosition.PARALLEL) {
            // PARALLEL MODEL: Q = Rp / |X|  ->  Rp = Q * |X|
            double Rp = absX * Q;
            Complex zResistor = new Complex(Rp, 0);
            Complex zCapacitor = new Complex(0, reactance);

            return SmithCalculator.addParallelImpedance(zResistor, zCapacitor);
        } else {
            // SERIES MODEL: Q = |X| / Rs  ->  Rs = |X| / Q
            double Rs = absX / Q;
            return new Complex(Rs, reactance);
        }
    }

    @Override
    public Complex getImpedance(double frequency) {
        return getImpedanceStatic(realWorldValue.get(), frequency, Optional.ofNullable(qualityFactor), this.elementPosition);
    }

    @Override
    public String toString() {
        return "capacitor";
    }
}
