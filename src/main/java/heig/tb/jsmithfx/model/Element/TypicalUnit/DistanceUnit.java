package heig.tb.jsmithfx.model.Element.TypicalUnit;

public enum DistanceUnit implements ElectronicUnit{
    MM("mm", 1E-3),
    M("m", 1),
    KM("km", 1E3),;

    private final String displayName;
    private final double factor;

    DistanceUnit(String displayName, double factor) {
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
