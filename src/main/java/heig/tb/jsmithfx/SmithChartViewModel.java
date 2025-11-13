package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SmithChartViewModel {

    public final DoubleProperty frequency = new SimpleDoubleProperty(1e9); // e.g., 1 GHz
    public final DoubleProperty zo = new SimpleDoubleProperty(50.0);
    public final ObjectProperty<Complex> loadImpedance = new SimpleObjectProperty<>(new Complex(50.0, 0.0));
    public final SimpleListProperty<CircuitElement> circuitElements = new SimpleListProperty<>(FXCollections.observableArrayList());

    // A list of different datapoints
    private final SimpleListProperty<DataPoint> dataPoints = new SimpleListProperty<>(FXCollections.observableArrayList());

    // A read-only list of the calculated gammas for drawing on the canvas.
    private final ReadOnlyListWrapper<Complex> measuresGamma = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    //==========================
    //--- Mouse informations ---
    //==========================
    public final DoubleProperty mouseReturnLoss = new SimpleDoubleProperty(0);
    public final DoubleProperty mouseVSWR = new SimpleDoubleProperty(0);
    public final DoubleProperty mouseQualityFactor = new SimpleDoubleProperty(0);
    public final ObjectProperty<Complex> mouseGamma = new SimpleObjectProperty<>(new Complex(0.0,0.0));
    public final ObjectProperty<Complex> mouseAdmittanceY = new SimpleObjectProperty<>(new Complex(0.0,0.0));
    public final ObjectProperty<Complex> mouseImpedanceZ = new SimpleObjectProperty<>(new Complex(0.0,0.0));

    // Ui bindings
    private final ReadOnlyStringWrapper mouseReturnLossText = new ReadOnlyStringWrapper("- dB");
    private final ReadOnlyStringWrapper mouseVSWRText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper mouseQualityFactorText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper mouseGammaText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper mouseAdmittanceYText = new ReadOnlyStringWrapper("Y: -");
    private final ReadOnlyStringWrapper mouseImpedanceZText = new ReadOnlyStringWrapper("Z: -");

    // Binding getters
    public ReadOnlyStringProperty mouseReturnLossTextProperty() { return mouseReturnLossText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty mouseVSWRTextProperty() { return mouseVSWRText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty mouseQualityFactorTextProperty() { return mouseQualityFactorText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty mouseGammaTextProperty() { return mouseGammaText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty mouseAdmittanceYTextProperty() { return mouseAdmittanceYText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty mouseImpedanceZTextProperty() { return mouseImpedanceZText.getReadOnlyProperty(); }



    public SmithChartViewModel() {
        // When any sources change, trigger a full recalculation.
        zo.addListener((obs, oldVal, newVal) -> recalculateImpedanceChain());
        frequency.addListener((obs, oldVal, newVal) -> recalculateImpedanceChain());
        loadImpedance.addListener((obs, oldVal, newVal) -> recalculateImpedanceChain());

        // When the list of derived impedances changes, automatically update the gamma values.
        dataPoints.addListener((ListChangeListener<DataPoint>) c -> recalculateAllGammas());

        // Perform the initial calculation when the view model is created.
        recalculateImpedanceChain();
    }

    /**
     * This is the single source of truth for recalculating the entire impedance path.
     * It clears the old 'measures' list and builds a new one from the load and all circuit elements.
     */
    private void recalculateImpedanceChain() {
        Complex currentImpedance = loadImpedance.get();
        if (currentImpedance == null) return; //We have no main impedance, gg

        // Use a temporary list to build the new state.
        List<DataPoint> newDataPoints = new ArrayList<>();

        //Handle the initial impedance
        Complex loadGamma = calculateGamma(currentImpedance);
        double loadVswr = calculateVswr(loadGamma);
        double loadRetLoss = calculateReturnLoss(loadGamma);

        newDataPoints.add(new DataPoint("LD", currentImpedance, loadGamma, loadVswr, loadRetLoss));

        // Sequentially add the effect of each component
        int index = 1;
        for (CircuitElement element : circuitElements) {
            Complex elementImpedance = element.getImpedance(frequency.get());
            currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, element.getElementPosition());

            //Calculate the values needed for the data points
            Complex elGamma = calculateGamma(currentImpedance);
            double elVswr = calculateVswr(elGamma);
            double elRetLoss = calculateReturnLoss(elGamma);

            newDataPoints.add(new DataPoint("DP" + index++, currentImpedance, elGamma, elVswr, elRetLoss));
        }

        // Atomically update the main 'measures' property with the new list.
        dataPoints.setAll(newDataPoints);
    }

    /**
     * Calculate the VSWR using the reflection coefficients (Gamma)
     * @param gamma the reflection coefficients
     * @return the VSWR
     */
    private double calculateVswr(Complex gamma) {
        double gammaNorm = gamma.abs();
        return (gammaNorm < 1e-9) ? Double.POSITIVE_INFINITY : (1 + gammaNorm) / (1 - gammaNorm);
    }

    /**
     * Calculate the return loss value using the reflection coefficients (Gamma)
     * @param gamma the reflection coefficients
     * @return the return loss value
     */
    private double calculateReturnLoss(Complex gamma){
        double gammaNorm = gamma.abs();
        return (gammaNorm < 1e-9) ? Double.POSITIVE_INFINITY : -20 * Math.log10(gammaNorm);
    }

    /**
     * Converts the entire measures list (impedances) into the measuresGamma list (reflection coefficients).
     */
    private void recalculateAllGammas() {
        List<Complex> newGammas = dataPoints.stream()
                .map(dp -> dp.gammaProperty().get())
                .collect(Collectors.toList());
        measuresGamma.setAll(newGammas);
    }

    public void calculateMouseInformations(double gammaX, double gammaY) {
        Complex gamma = new Complex(gammaX, gammaY);
        double gammaMagnitude = gamma.abs();

        //Update raw data
        this.mouseGamma.set(gamma);

        // Only perform calculations for points within the Smith Chart (|Gamma| <= 1)
        if (gammaMagnitude > 1.0) {
            mouseReturnLossText.set("- dB");
            mouseVSWRText.set("∞");
            mouseQualityFactorText.set("-");
            mouseGammaText.set("-");
            mouseAdmittanceYText.set("-");
            mouseImpedanceZText.set("-");
            return;
        }

        // Update properties based on Gamma
        double returnLoss = calculateReturnLoss(gamma);
        double vswr = calculateVswr(gamma);

        // Calculate mouse pos impedance
        Complex one = new Complex(1, 0);
        Complex z0Complex = new Complex(zo.get(), 0);

        // Calculate normalized impedance: Z_norm = (1 + Gamma) / (1 - Gamma)
        Complex zNorm = one.add(gamma).dividedBy(one.subtract(gamma));

        // Calculate characteristic impedance: Z = Z_norm * Z0
        Complex impedanceZ = zNorm.multiply(z0Complex);

        // Calculate characteristic admittance: Y = (1 / Z)
        Complex admittanceY = impedanceZ.inverse();

        // Calculate Q-Factor, Q = |X| / R
        double qFactor = (impedanceZ.real() < 1e-9) ? Double.POSITIVE_INFINITY : Math.abs(impedanceZ.imag()) / impedanceZ.real();

        //Update properties
        mouseReturnLoss.set(returnLoss);
        mouseVSWR.set(vswr);
        mouseImpedanceZ.set(impedanceZ);
        mouseAdmittanceY.set(admittanceY);
        mouseQualityFactor.set(qFactor);

        //Format strings for controller
        mouseReturnLossText.set(String.format("%.3f dB", returnLoss));
        mouseVSWRText.set(String.format("%.3f", vswr));
        mouseGammaText.set(String.format("%.3f ∠ %.3f°", gamma.abs(), Math.toDegrees(gamma.angle())));
        mouseImpedanceZText.set(impedanceZ.toString());
        mouseAdmittanceYText.set(admittanceY.toStringmS());
        mouseQualityFactorText.set(String.format("%.3f", qFactor));
    }
    /**
     * Calculates the reflection coefficient (Gamma) for a given impedance Z.
     * @param z The complex impedance.
     * @return The complex reflection coefficient.
     */
    private Complex calculateGamma(Complex z) {
        double z0 = zo.get();
        if (z0 <= 0) return new Complex(0, 0);

        Complex zNorm = z.dividedBy(new Complex(z0, 0));
        return (zNorm.addReal(-1)).dividedBy(zNorm.addReal(1));
    }

    /**
     * Adds a new component to the circuit and triggers a full recalculation.
     */
    void addComponent(CircuitElement.ElementType type, double value, CircuitElement.ElementPosition position) {
        CircuitElement newElem = switch (type) {
            case INDUCTOR -> new Inductor(value, position);
            case CAPACITOR -> new Capacitor(value, position);
            case RESISTOR -> null; //TODO MAKE THE OTHER CIRCUIT COMPONENTS
        };

        if (newElem == null) return;

        circuitElements.add(newElem);
        recalculateImpedanceChain();
    }

    /**
     * Remove the component using their index number, used to remove from the point list
     * @param index of the component to remove
     */
    void removeComponentAt(int index){
        try{
            circuitElements.remove(index);
            recalculateImpedanceChain();
        } catch (ArrayIndexOutOfBoundsException e){
            Logger.getLogger("Error").log(Level.SEVERE, e.getMessage());
        }

    }

    /**
     * Helper function to calculate the resulting impedance after adding a new component.
     * @param previousImpedance The impedance at the previous point in the chain.
     * @param impedanceToAdd The impedance of the new component.
     * @param position Whether the component is in SERIES or PARALLEL.
     * @return The new total impedance.
     */
    private Complex calculateNextImpedance(Complex previousImpedance, Complex impedanceToAdd, CircuitElement.ElementPosition position) {
        if (previousImpedance == null || impedanceToAdd == null) return null;

        if (position == CircuitElement.ElementPosition.SERIES) {
            return previousImpedance.add(impedanceToAdd);
        } else { // PARALLEL
            Complex previousAdmittance = previousImpedance.inverse();
            Complex newElemAdmittance = impedanceToAdd.inverse();
            return previousAdmittance.add(newElemAdmittance).inverse();
        }
    }

    public Complex getLastGamma(){
        return measuresGamma.getLast();
    }


    // --- Public Properties for Binding ---

    public ReadOnlyListProperty<Complex> measuresGammaProperty() {
        return measuresGamma.getReadOnlyProperty();
    }

    public SimpleListProperty<DataPoint> dataPointsProperty() {
        return dataPoints;
    }
}