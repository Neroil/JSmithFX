package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

public class Line extends CircuitElement {

    public enum StubType {
        NONE, // Not a stub, just a series line
        OPEN,
        SHORT
    }

    private final double characteristicImpedance;
    private final StubType stubType;
    private final double velocityFactor;

    // TODO ADD VELOCITY FACTOR TO CONSTRUCTOR

    // Constructor for a standard in-line transmission line
    public Line(double length, double characteristicImpedance) {
        super(length, ElementPosition.SERIES, ElementType.LINE); // Position is always SERIES for this type
        this.characteristicImpedance = characteristicImpedance;
        this.stubType = StubType.NONE;
        this.velocityFactor = 1.0;
    }

    // Constructor for a stub
    public Line(double length, double characteristicImpedance, StubType stubType) {
        super(length, ElementPosition.PARALLEL, ElementType.LINE); // Position is always PARALLEL for stubs
        this.characteristicImpedance = characteristicImpedance;
        this.stubType = stubType;
        this.velocityFactor = 1.0;
    }

    public double getCharacteristicImpedance() {
        return characteristicImpedance;
    }

    public StubType getStubType() {
        return stubType;
    }

    public double getVelocityFactor() {
        return velocityFactor;
    }

    @Override
    public Complex getImpedance(double frequency) {
        // This calculation is context-dependent and handled in the ViewModel
        throw new UnsupportedOperationException("Cannot get impedance of a Line element in isolation.");
    }

    @Override
    public String toString() {
        return "line";
    }
}