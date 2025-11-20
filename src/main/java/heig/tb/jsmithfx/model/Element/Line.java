package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.utilities.Complex;

public class Line extends CircuitElement {

    public enum StubType {
        NONE("Series"), // Not a stub, just a series line
        OPEN("Open circuit stub"),
        SHORT("Short circuit stub");

        private String name;

        StubType(String name){
            this.name=name;
        }

        @Override
        public String toString(){return name;}

    }

    private final double characteristicImpedance;
    private final StubType stubType;
    private final double permittivity;


    // Constructor for a standard in-line transmission line
    public Line(double length, double characteristicImpedance, double permittivity) {
        super(length, ElementPosition.SERIES, ElementType.LINE); // Position is always SERIES for this type
        this.characteristicImpedance = characteristicImpedance;
        this.stubType = StubType.NONE;
        this.permittivity = permittivity;
    }

    // Constructor for a stub
    public Line(double length, double characteristicImpedance, double permittivity, StubType stubType) {
        super(length, ElementPosition.PARALLEL, ElementType.LINE); // Position is always PARALLEL for stubs
        this.characteristicImpedance = characteristicImpedance;
        this.stubType = stubType;
        this.permittivity = permittivity;
    }

    public double getCharacteristicImpedance() {
        return characteristicImpedance;
    }

    public StubType getStubType() {
        return stubType;
    }

    public double getPermittivity() {
        return permittivity;
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