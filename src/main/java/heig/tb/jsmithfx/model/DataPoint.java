package heig.tb.jsmithfx.model;

import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class DataPoint {

    private final SimpleDoubleProperty frequency;
    private final SimpleStringProperty label;
    private final SimpleObjectProperty<Complex> impedance;
    private final SimpleObjectProperty<Complex> gamma;
    private final SimpleDoubleProperty vswr;
    private final SimpleDoubleProperty returnLoss;
    private final SimpleDoubleProperty qualityFactor;

    public DataPoint(double frequency, String label, Complex impedance, Complex gamma, double vswr, double returnLoss) {
        this.frequency = new SimpleDoubleProperty(frequency);
        this.label = new SimpleStringProperty(label);
        this.impedance = new SimpleObjectProperty<>(impedance);
        this.gamma = new SimpleObjectProperty<>(gamma);
        this.vswr = new SimpleDoubleProperty(vswr);
        this.returnLoss = new SimpleDoubleProperty(returnLoss);
        // Quality Factor Calculation
        double realPart = impedance.real();
        double quality = (realPart == 0) ? 0 : Math.abs(impedance.imag()) / realPart;
        this.qualityFactor = new SimpleDoubleProperty(quality);
    }

    // JavaFX Property Accessors
    public SimpleDoubleProperty frequencyProperty() { return frequency; }
    public SimpleStringProperty labelProperty() { return label; }
    public SimpleObjectProperty<Complex> impedanceProperty() { return impedance; }
    public SimpleObjectProperty<Complex> gammaProperty() { return gamma; }
    public SimpleDoubleProperty vswrProperty() { return vswr; }
    public SimpleDoubleProperty returnLossProperty() { return returnLoss; }
    public SimpleDoubleProperty qualityFactorProperty() { return qualityFactor; }

    // Standard Getters
    public double getFrequency() { return frequency.get(); }
    public String getLabel() { return label.get(); }
    public Complex getImpedance() { return impedance.get(); }
    public Complex getGamma() { return gamma.get(); }
    public double getVswr() { return vswr.get(); }
    public double getReturnLoss() { return returnLoss.get(); }
    public double getQualityFactor() { return qualityFactor.get(); }

    @Override
    public String toString() {
        return String.format("Freq: %.2e Hz, VSWR: %.2f", getFrequency(), getVswr());
    }
}