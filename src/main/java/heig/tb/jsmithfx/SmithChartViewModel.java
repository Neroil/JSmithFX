package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.Resistor;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SmithChartViewModel {

    public final DoubleProperty frequency = new SimpleDoubleProperty();
    private final ReadOnlyStringWrapper frequencyText = new ReadOnlyStringWrapper("-");
    public ReadOnlyStringProperty frequencyProperty() { return frequencyText.getReadOnlyProperty(); }

    public final DoubleProperty zo = new SimpleDoubleProperty();
    private final ReadOnlyStringWrapper zoText = new ReadOnlyStringWrapper("-");
    public ReadOnlyStringProperty zoProperty() { return zoText.getReadOnlyProperty(); }

    public final ObjectProperty<Complex> loadImpedance = new SimpleObjectProperty<>();
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

    // Undo Redo logic
    private enum Operation { ADD, REMOVE }
    private final Stack<UndoRedoEntry> undoStack = new Stack<>();
    private final Stack<UndoRedoEntry> redoStack = new Stack<>();

    private record UndoRedoEntry(Operation operation, int index, CircuitElement element) {}



    public SmithChartViewModel() {
        // When any sources change, trigger a full recalculation.
        zo.addListener((_, _, _) -> {
            zoText.set(String.valueOf(zo.get()) + " Ω");
            recalculateImpedanceChain();
        });
        frequency.addListener((_, _, _) -> {
            //Update the display for frequency
            double freq = frequency.get();
            String newFreqText =  switch ((int) Math.log10(freq)) {
                case 0, 1, 2 -> String.format("%.2f Hz", freq);
                case 3, 4, 5 -> String.format("%.2f kHz", freq / 1_000);
                case 6, 7, 8 -> String.format("%.2f MHz", freq / 1_000_000);
                default -> String.format("%.2f GHz", freq / 1_000_000_000);
            };
            frequencyText.set(newFreqText);
            recalculateImpedanceChain();
        });

        circuitElements.addListener((ListChangeListener<CircuitElement>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (CircuitElement elem : change.getAddedSubList()) {
                        elem.realWorldValueProperty().addListener((_, _, _) -> recalculateImpedanceChain());
                    }
                }
            }
        });

        //Set the initial frequency at 1GHz
        frequency.set(1e9);
        //Set the characteristic impedance to 50 Ohm
        zo.set(50.0);
        //Set a dummy impedance value
        loadImpedance.set(new Complex(zo.get() * 2, zo.get() * 3));

        loadImpedance.addListener((_, _, _) -> recalculateImpedanceChain());

        // When the list of derived impedances changes, automatically update the gamma values.
        dataPoints.addListener((ListChangeListener<DataPoint>) _ -> recalculateAllGammas());

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

        final double SPEED_OF_LIGHT = 299792458.0;

        // Sequentially add the effect of each component
        int index = 1;
        for (CircuitElement element : circuitElements) {

            if (element.getType() == CircuitElement.ElementType.LINE){
                Line line = (Line) element;
                double z0 = line.getCharacteristicImpedance(); //Safe since we checked earlier
                double length = element.getRealWorldValue();
                //beta = 2pi / lambda, lambda being the wavelength in the medium
                double vf = line.getVelocityFactor();
                if (vf <= 0 || vf > 1.0) {
                    vf = 1.0;
                }
                double beta = (2 * Math.PI * frequency.get()) / SPEED_OF_LIGHT * vf;
                double electricalLength = beta * length;
                System.out.println("Length : " + length + "electrical l : " + electricalLength);
                Complex j = new Complex(0, 1);
                double tan_bl = Math.tan(electricalLength);

                // Z_in = Z_0 * [(Z_L + jZ_0 * tan bl) / (Z_0 + jZ_L * tan bl)]
                if (element.getElementPosition() == CircuitElement.ElementPosition.SERIES) {
                    Complex zl = currentImpedance;
                    Complex z0Complex = new Complex(z0, 0);
                    Complex numerator = zl.add(j.multiply(z0Complex).multiply(tan_bl));
                    Complex denominator = z0Complex.add(j.multiply(zl).multiply(tan_bl));

                    System.out.println("numerator: " + numerator + " denominator: " + denominator);

                    if (denominator.abs() > 1e-9) { //Limit the values to not be too small
                        currentImpedance = z0Complex.multiply(numerator.dividedBy(denominator));
                        System.out.println("currentImpedance: " + currentImpedance);
                    } else {
                        currentImpedance = new Complex(1e12, 0);
                        System.out.println("currentImpedance: " + currentImpedance);
                    }
                } else { //STUB component
                    double y0 = 1.0 / z0;
                    Complex stubAdmittance;

                    if(line.getStubType() == Line.StubType.SHORT){
                        // Y_in = -j * Y₀ * cot(βl)
                        double cot_bl = 1.0 / Math.tan(electricalLength);
                        stubAdmittance = j.multiply(-1).multiply(y0).multiply(cot_bl);
                    } else { // OPEN Stub
                        // Y_in = j * Y₀ * tan(βl)
                        stubAdmittance = j.multiply(y0).multiply(tan_bl);
                    }

                    // Convert current impedance to admittance, add the stub's admittance, then convert back
                    Complex currentAdmittance = currentImpedance.inverse();
                    Complex newTotalAdmittance = currentAdmittance.add(stubAdmittance);
                    currentImpedance = newTotalAdmittance.inverse();
                }
            } else {
                Complex elementImpedance = element.getImpedance(frequency.get());
                currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, element.getElementPosition());
            }
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

        Complex denominator = one.subtract(gamma);

        // Check for open circuit condition (Gamma ≈ 1)
        if (denominator.abs() < 1e-9) {
            // Handle open circuit - set to very high impedance or infinity
            mouseReturnLossText.set("0.00 dB");
            mouseVSWRText.set("∞");
            mouseQualityFactorText.set("∞");
            mouseGammaText.set(String.format("%.3f ∠ %.3f°", gamma.abs(), Math.toDegrees(gamma.angle())));
            mouseAdmittanceYText.set("0.00 + j0.00 mS");
            mouseImpedanceZText.set("∞");
            return;
        }

        // Calculate normalized impedance: Z_norm = (1 + Gamma) / (1 - Gamma)
        Complex zNorm = one.add(gamma).dividedBy(denominator);

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

    void addComponent(CircuitElement.ElementType type, double value, CircuitElement.ElementPosition position) {
        addComponent(type, value, 0.0, position);
    }

    /**
     * Adds a new component to the circuit and triggers a full recalculation.
     */
    void addComponent(CircuitElement.ElementType type, double value, double characteristicImpedance, CircuitElement.ElementPosition position) {
        CircuitElement newElem = switch (type) {
            case INDUCTOR -> new Inductor(value, position, type);
            case CAPACITOR -> new Capacitor(value, position, type);
            case RESISTOR -> new Resistor(value, position, type);
            case LINE ->  new Line(value, characteristicImpedance); //TODO : Add stub implementation
        };

        if (newElem == null) return;

        int index = circuitElements.size();
        circuitElements.add(newElem);

        // Record the ADD operation
        undoStack.push(new UndoRedoEntry(Operation.ADD, index, newElem));
        redoStack.clear(); // New action clears redo history

        recalculateImpedanceChain();
    }

    /**
     * Remove the component using their index number, used to remove from the point list
     * @param index of the component to remove
     */
    void removeComponentAt(int index){
        try{
            if (index < 0 || index >= circuitElements.size()) return; //Boundary check

            CircuitElement removed = circuitElements.get(index);
            circuitElements.remove(index);

            // Record the REMOVE operation
            undoStack.push(new UndoRedoEntry(Operation.REMOVE, index, removed));
            redoStack.clear(); // New action clears redo history

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

    public Complex getLastImpedance() {
        if (dataPoints.isEmpty()) return null;
        return dataPoints.getLast().impedanceProperty().get();
    }

    /**
     * Undo logic for the components ONLY
     */
    public void undo() {
        if (undoStack.isEmpty()) return;

        UndoRedoEntry entry = undoStack.pop();

        if (entry.operation == Operation.ADD) {
            circuitElements.remove(entry.index);
        } else { // Operation.REMOVE
            circuitElements.add(entry.index, entry.element);
        }

        redoStack.push(entry);
        recalculateImpedanceChain();
    }

    /**
     * Redo logic for the components ONLY
     */
    public void redo() {
        if (redoStack.isEmpty()) return;

        UndoRedoEntry entry = redoStack.pop();

        if (entry.operation == Operation.ADD) {
            circuitElements.add(entry.index, entry.element);
        } else { // Operation.REMOVE
            circuitElements.remove(entry.index);
        }

        undoStack.push(entry);
        recalculateImpedanceChain();
    }
}