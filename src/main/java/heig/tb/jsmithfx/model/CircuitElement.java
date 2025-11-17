package heig.tb.jsmithfx.model;

import heig.tb.jsmithfx.utilities.Complex;

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

    protected double realWorldValue;
    protected ElementPosition elementPosition;
    protected ElementType elementType;


    public double getRealWorldValue() {
        return realWorldValue;
    }

    public void setRealWorldValue(double realWorldValue) {
        this.realWorldValue = realWorldValue;
    }

    public ElementType getType() {
        return this.elementType;
    }

    public ElementPosition getPosition() {
        return this.elementPosition;
    }

    protected CircuitElement(double realWorldValue, ElementPosition elementPosition,  ElementType elementType) {
        this.realWorldValue = realWorldValue;
        this.elementPosition = elementPosition;
        this.elementType = elementType;
    }

    public abstract Complex getImpedance(double frequency);

    public ElementPosition getElementPosition() {
        return elementPosition;
    }
}
