package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import static heig.tb.jsmithfx.model.CircuitElement.ElementType.CAPACITOR;
import static heig.tb.jsmithfx.model.CircuitElement.ElementType.INDUCTOR;

public class ComponentEntry {
    private final SimpleObjectProperty<CircuitElement.ElementType> type;
    private final SimpleStringProperty typeName;
    private final double value;
    private final double parasitic;
    private final boolean isQFactor;

    public ComponentEntry(CircuitElement.ElementType type, double value, double parasitic, boolean isQFactor) {
        this.type = new SimpleObjectProperty<>(type);
        this.typeName = new SimpleStringProperty(type.toString());
        this.value = value;
        this.parasitic = parasitic;
        this.isQFactor = isQFactor;
    }

    public CircuitElement.ElementType getType() { return type.get(); }
    public String getTypeName() { return typeName.get(); }
    public double getValue() { return value; }
    public double getParasitic() { return parasitic; }
    public boolean isQFactor() { return isQFactor; }

    public String getParasiticDisplay() {
        if (isQFactor) {
            return String.format("%.3g (Q)", parasitic);
        }

        if (getType() == CircuitElement.ElementType.RESISTOR) {
            // Nothing to show for resistor parasitic
            return "-";
        } else {
            return SmithUtilities.displayBestUnitAndFormattedValue(parasitic, ResistanceUnit.values());
        }
    }

    public double getQualityFactor(double frequency) {
        if (isQFactor) {
            return parasitic;
        } else {
            // Convert ESR to Q factor: Q = |X| / ESR
            if (parasitic <= 1e-9) return Double.POSITIVE_INFINITY;

            double omega = 2 * Math.PI * frequency;
            double reactance = 0;

            switch (type.get()) {
                case INDUCTOR -> reactance = omega * value;
                case CAPACITOR -> {
                    if (value > 0 && omega > 0) {
                        reactance = 1.0 / (omega * value);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }

            return reactance / parasitic;
        }
    }
}