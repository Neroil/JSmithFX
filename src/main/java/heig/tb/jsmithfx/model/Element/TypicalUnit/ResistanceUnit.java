package heig.tb.jsmithfx.model.Element.TypicalUnit;

public enum ResistanceUnit implements ElectronicUnit {
    OHM("Ohm", 1),
    KILO_OHM("kΩ", 1E3),
    MEGA_OHM("MΩ", 1E6);

    private final String displayName;
    private final double factor;

    ResistanceUnit(String displayName, double factor) {
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
