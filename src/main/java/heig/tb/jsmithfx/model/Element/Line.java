package heig.tb.jsmithfx.model.Element;

import heig.tb.jsmithfx.logic.SmithCalculator;
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

    private double characteristicImpedance;
    private StubType stubType;
    private double permittivity;


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
        throw new UnsupportedOperationException("Cannot get impedance of a Line element in isolation.");
    }

    public Complex calculateImpedance(Complex currentImpedance, double frequency) {

        double beta = (2 * Math.PI * frequency) / SmithCalculator.getSpeedOfLight() * Math.sqrt(permittivity);
        double electricalLength = beta * getRealWorldValue();

        Complex j = new Complex(0, 1);

        if (stubType == StubType.NONE) { // Series line
            Complex z0Complex = new Complex(characteristicImpedance, 0);
            double tan_bl = Math.tan(electricalLength);
            Complex numerator = currentImpedance.add(j.multiply(z0Complex).multiply(tan_bl));
            Complex denominator = z0Complex.add(j.multiply(currentImpedance).multiply(tan_bl));
            return denominator.magnitude() > 1e-9 ? z0Complex.multiply(numerator.dividedBy(denominator)) : new Complex(Double.POSITIVE_INFINITY, 0);
        } else { // Stub
            double y0 = 1.0 / characteristicImpedance;
            Complex stubAdmittance = stubType == StubType.SHORT
                    ? j.multiply(-1).multiply(y0).multiply(1.0 / Math.tan(electricalLength))
                    : j.multiply(y0).multiply(Math.tan(electricalLength));
            Complex currentAdmittance = currentImpedance.inverse();
            return currentAdmittance.add(stubAdmittance).inverse();
        }
    }

    public void setStubType(StubType stubType) {
        this.stubType = stubType;
    }

    public void setCharacteristicImpedance(double characteristicImpedance) {
        this.characteristicImpedance = characteristicImpedance;
    }

    public void setPermittivity(double permittivity) {
        this.permittivity = permittivity;
    }

    @Override
    public String toString() {
        return "line";
    }

    @Override
    public CircuitElement copy() {
        if (this.stubType == StubType.NONE) {
            return new Line(this.getRealWorldValue(), this.getCharacteristicImpedance(), this.getPermittivity());
        } else {
            return new Line(this.getRealWorldValue(), this.getCharacteristicImpedance(), this.getPermittivity(), this.getStubType());
        }
    }
}