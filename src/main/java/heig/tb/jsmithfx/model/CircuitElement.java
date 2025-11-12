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
        PARALLEL("Parallel"),
        SERIES("Series");

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
    protected Complex impedance;
    protected ElementPosition elementPosition;

    protected CircuitElement() {
    }

    public double getRealWorldValue() {
        return realWorldValue;
    }

    public void setRealWorldValue(double realWorldValue) {
        this.realWorldValue = realWorldValue;
    }

    protected CircuitElement(double realWorldValue, ElementPosition elementPosition) {
        this.realWorldValue = realWorldValue;
        this.elementPosition = elementPosition;
    }

    public abstract Complex getImpedance(double frequency);
    abstract protected void setImpedance(double frequency);

}
