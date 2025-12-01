package heig.tb.jsmithfx.model.Element.TypicalUnit;

import heig.tb.jsmithfx.model.TouchstoneS1P;

public enum FrequencyUnit implements ElectronicUnit {
    HZ("Hz", 1),
    KHZ("kHz", 1E3),
    MHZ("MHz", 1E6),
    GHZ("GHz", 1E9);

    private final String displayName;
    private final double factor;
    public static final FrequencyUnit DEFAULT = GHZ; // Default frequency unit for Touchstone files

    FrequencyUnit(String displayName, double factor) {
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

    public static FrequencyUnit bestFit(double frequencyHz) {
        double absHz = Math.abs(frequencyHz);
        if (absHz >= GHZ.factor) return GHZ;
        if (absHz >= MHZ.factor) return MHZ;
        if (absHz >= KHZ.factor) return KHZ;
        return HZ;
    }
}