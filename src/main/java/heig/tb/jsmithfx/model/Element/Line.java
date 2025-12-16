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

    // The quality factor here is used to model losses in the transmission line
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

        // We use the quality factor variable but here it's the loss in dB per meter
        double lossDbPerMeter = this.getQualityFactor().orElse(0.0);

        double alpha = lossDbPerMeter * SmithCalculator.getDbmToNeperConversionFactor(); // Convert dB/m to Np/m
        double beta = (2 * Math.PI * frequency) / SmithCalculator.getSpeedOfLight() * Math.sqrt(permittivity);

        Complex gamma = new Complex(alpha, beta);
        Complex gammaL = gamma.multiply(getRealWorldValue());
        Complex z0Complex = new Complex(characteristicImpedance, 0);
        Complex tanhGammaL = gammaL.tanh();

        Complex j = new Complex(0, 1);

        if (stubType == StubType.NONE) { // Series line
            // Numerator: Z_L + Z0 * tanh(γl)
            Complex numerator = currentImpedance.add(z0Complex.multiply(tanhGammaL));

            // Denominator: Z0 + Z_L * tanh(γl)
            Complex denominator = z0Complex.add(currentImpedance.multiply(tanhGammaL));

            // Z_in = Z0 * (Numerator / Denominator)
            return z0Complex.multiply(numerator.dividedBy(denominator));
        } else { // Stub
            Complex y0Complex = z0Complex.inverse();
            Complex stubAdmittance;

            if (stubType == StubType.SHORT) {
                // Short Circuit (Yin = Y0 * coth(γl) = Y0 / tanh(γl))
                stubAdmittance = y0Complex.dividedBy(tanhGammaL);
            } else {
                // Open Circuit (Yin = Y0 * tanh(γl))
                stubAdmittance = y0Complex.multiply(tanhGammaL);
            }

            // Calculate overall admittance and convert back to impedance
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

    /**
     * Calculates the electrical length of the transmission line in degrees.
     * Formula: Theta = Beta * Physical_Length
     * where Beta = (2 * pi * f * sqrt(permittivity)) / c
     *
     * @param frequency The operating frequency in Hz
     * @return The electrical length in degrees
     */
    public double calculateElectricalLengthDegrees(double frequency) {
        // 1. Calculate Beta (Phase Constant) in rad/m
        // Note: This matches the logic inside calculateImpedance
        double beta = (2 * Math.PI * frequency) / SmithCalculator.getSpeedOfLight() * Math.sqrt(permittivity);

        // 2. Calculate Electrical Length in Radians (Theta = β * l)
        double electricalLengthRadians = beta * getRealWorldValue();

        // 3. Convert to Degrees
        return Math.toDegrees(electricalLengthRadians);
    }

    @Override
    public String toString() {
        return "line";
    }

    @Override
    public CircuitElement copy() {
        Line newLine;
        if (this.stubType == StubType.NONE) {
            newLine = new Line(this.getRealWorldValue(), this.getCharacteristicImpedance(), this.getPermittivity());
        } else {
            newLine = new Line(this.getRealWorldValue(), this.getCharacteristicImpedance(), this.getPermittivity(), this.getStubType());
        }

        this.getQualityFactor().ifPresent(newLine::setQualityFactor);

        return newLine;
    }
}