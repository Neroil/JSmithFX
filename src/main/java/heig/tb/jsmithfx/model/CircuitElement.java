package heig.tb.jsmithfx.model;

import heig.tb.jsmithfx.model.Element.ElementPosition;
import heig.tb.jsmithfx.utilities.Complex;

public abstract class CircuitElement {
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
