package heig.tb.jsmithfx.model.Element.TypicalUnit;

public enum CapacitanceUnit {
    PICO_FARAD("pF", 1E-12),
    NANO_FARAD("nF", 1E-9),
    MICRO_FARAD("μF", 1E-6),
    MILLI_FARAD("mF", 1E-3);

    private final String displayName;
    private final double factor;

    CapacitanceUnit(String displayName, double factor) {
        this.displayName = displayName;
        this.factor = factor;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public double getFactor() {
        return factor;
    }
}
