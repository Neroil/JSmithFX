package heig.tb.jsmithfx.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.Resistor;
import heig.tb.jsmithfx.model.Element.TypicalUnit.CapacitanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.DistanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.InductanceUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ResistanceUnit;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.Optional;

// Setup Jackson annotations for polymorphic serialization/deserialization
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "elementType"
)

@JsonSubTypes({
        @JsonSubTypes.Type(value = Resistor.class, name = "RESISTOR"),
        @JsonSubTypes.Type(value = Inductor.class, name = "INDUCTOR"),
        @JsonSubTypes.Type(value = Capacitor.class, name = "CAPACITOR"),
        @JsonSubTypes.Type(value = Line.class, name = "LINE")
})
public abstract class CircuitElement {

    public enum ElementType {
        CAPACITOR("Capacitor", CapacitanceUnit.class),
        INDUCTOR("Inductor", InductanceUnit.class),
        RESISTOR("Resistor", ResistanceUnit.class),
        LINE("Line", DistanceUnit.class),;

        private final String displayName;
        private final Class<?> unitClass;

        ElementType(String displayName, Class<?> unitClass) {
            this.displayName = displayName;
            this.unitClass = unitClass;
        }

        public Class<?> getUnitClass() {
            return unitClass;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ElementPosition {
        SERIES("Series"),
        PARALLEL("Parallel");

        private final String displayName;
        ElementPosition(String displayName) {
            this.displayName = displayName;
        }
        @Override
        public String toString() {
            return displayName;
        }

    }
    protected Double qualityFactor = null;

    public Optional<Double> getQualityFactor() {
        return Optional.ofNullable(qualityFactor);
    }

    public void setQualityFactor(Double qualityFactor) {
        this.qualityFactor = qualityFactor;
    }

    protected DoubleProperty realWorldValue = new SimpleDoubleProperty();
    protected ElementPosition elementPosition;
    protected ElementType elementType;

    public DoubleProperty realWorldValueProperty() {
        return realWorldValue;
    }

    public double getRealWorldValue() {
        return realWorldValue.get();
    }

    public void setRealWorldValue(double realWorldValue) {
        this.realWorldValue.set(realWorldValue);
    }

    public ElementType getType() {
        return this.elementType;
    }

    public void setPosition(ElementPosition position) {
        this.elementPosition = position;
    }

    public void setType(ElementType type) {
        this.elementType = type;
    }

    public ElementPosition getPosition() {
        return this.elementPosition;
    }

    // Default constructor for Jackson deserialization
    protected CircuitElement() {
    }

    protected CircuitElement(double realWorldValue, ElementPosition elementPosition,  ElementType elementType) {
        this.realWorldValue.set(realWorldValue);
        this.elementPosition = elementPosition;
        this.elementType = elementType;
    }


    public CircuitElement copy() {
        return switch (this.elementType) {
            case CAPACITOR -> {
                var newEl = new Capacitor(this.getRealWorldValue(), this.elementPosition, this.elementType);
                newEl.setQualityFactor(qualityFactor);
                yield newEl;
            }
            case INDUCTOR -> {
                var newEl = new Inductor(this.getRealWorldValue(), this.elementPosition, this.elementType);
                newEl.setQualityFactor(qualityFactor);
                yield newEl;
            }
            case RESISTOR -> new Resistor(this.getRealWorldValue(), this.elementPosition, this.elementType);
            case LINE -> null;
        };
    }

    public abstract Complex getImpedance(double frequency);

    public ElementPosition getElementPosition() {
        return elementPosition;
    }
}
