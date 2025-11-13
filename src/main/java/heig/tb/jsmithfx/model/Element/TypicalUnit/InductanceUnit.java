package heig.tb.jsmithfx.model.Element.TypicalUnit;

public enum InductanceUnit implements ElectronicUnit {
    HENRY("H", 1),
    MILLI_HENRY("mH", 1E-3),
    MICRO_HENRY("μH", 1E-6),
    NANO_HENRY("nH", 1E-9);


    private final String displayName;
    private final double factor;

    InductanceUnit(String displayName, double factor) {
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