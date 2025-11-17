package heig.tb.jsmithfx.model;

import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class DataPoint {

    private final SimpleStringProperty label;
    private final SimpleObjectProperty<Complex> impedance;
    private final SimpleObjectProperty<Complex> gamma;
    private final SimpleDoubleProperty vswr;
    private final SimpleDoubleProperty returnLoss;

    public DataPoint(String label, Complex impedance, Complex gamma, double vswr, double returnLoss) {
        this.label = new SimpleStringProperty(label);
        this.impedance = new SimpleObjectProperty<>(impedance);
        this.gamma = new SimpleObjectProperty<>(gamma);
        this.vswr = new SimpleDoubleProperty(vswr);
        this.returnLoss = new SimpleDoubleProperty(returnLoss);
    }

    // JavaFX Property Getters
    public SimpleStringProperty labelProperty() { return label; }
    public SimpleObjectProperty<Complex> impedanceProperty() { return impedance; }
    public SimpleObjectProperty<Complex> gammaProperty() { return gamma; }
    public SimpleDoubleProperty vswrProperty() { return vswr; }
    public SimpleDoubleProperty returnLossProperty() { return returnLoss; }

}