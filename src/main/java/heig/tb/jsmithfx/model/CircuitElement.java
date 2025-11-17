package heig.tb.jsmithfx.model;

import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public abstract class CircuitElement {

    public enum ElementType {
        CAPACITOR("Capacitor"),
        INDUCTOR("Inductor"),
        RESISTOR("Resistor");

        private final String displayName;

        ElementType(String displayName) {
            this.displayName = displayName;
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

    public ElementPosition getPosition() {
        return this.elementPosition;
    }

    protected CircuitElement(double realWorldValue, ElementPosition elementPosition,  ElementType elementType) {
        this.realWorldValue.set(realWorldValue);
        this.elementPosition = elementPosition;
        this.elementType = elementType;
    }

    public abstract Complex getImpedance(double frequency);

    public ElementPosition getElementPosition() {
        return elementPosition;
    }
}
