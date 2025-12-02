package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.Resistor;
import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.model.TouchstoneS1P;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SmithChartViewModel {

    public final DoubleProperty zo = new SimpleDoubleProperty();
    public final ObjectProperty<Complex> loadImpedance = new SimpleObjectProperty<>();
    public final SimpleListProperty<CircuitElement> circuitElements = new SimpleListProperty<>(FXCollections.observableArrayList());
    //==========================
    //--- Mouse informations ---
    //==========================
    public final DoubleProperty mouseReturnLoss = new SimpleDoubleProperty(0);
    public final DoubleProperty mouseVSWR = new SimpleDoubleProperty(0);
    public final DoubleProperty mouseQualityFactor = new SimpleDoubleProperty(0);
    public final ObjectProperty<Complex> mouseGamma = new SimpleObjectProperty<>(new Complex(0.0, 0.0));
    public final ObjectProperty<Complex> mouseAdmittanceY = new SimpleObjectProperty<>(new Complex(0.0, 0.0));
    public final ObjectProperty<Complex> mouseImpedanceZ = new SimpleObjectProperty<>(new Complex(0.0, 0.0));
    // Ghost cursor
    public final ObjectProperty<Complex> ghostCursorGamma = new SimpleObjectProperty<>();
    public final BooleanProperty showGhostCursor = new SimpleBooleanProperty(false);
    private final DoubleProperty frequency = new SimpleDoubleProperty();
    private final ReadOnlyStringWrapper frequencyText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper zoText = new ReadOnlyStringWrapper("-");
    // A list of different datapoints
    private final SimpleListProperty<DataPoint> dataPoints = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ReadOnlyListWrapper<DataPoint> s1pDataPoints = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    // Logic to store transformed S1P points for drawing
    private final ReadOnlyListWrapper<DataPoint> transformedS1PPoints = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final List<DataPoint> cachedS1PPoints = new ArrayList<>();
    // Sweep points
    private final ReadOnlyListWrapper<DataPoint> sweepDataPoints = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final SimpleListProperty<Double> pointToSweep = new SimpleListProperty<>(FXCollections.observableArrayList());
    // Combined data points for display
    private final ReadOnlyListWrapper<DataPoint> combinedDataPoints = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    // A read-only list of the calculated gammas for drawing on the canvas.
    private final ReadOnlyListWrapper<Complex> measuresGamma = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    // Ui bindings
    private final ReadOnlyStringWrapper mouseReturnLossText = new ReadOnlyStringWrapper("- dB");
    private final ReadOnlyStringWrapper mouseVSWRText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper mouseQualityFactorText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper mouseGammaText = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper mouseAdmittanceYText = new ReadOnlyStringWrapper("Y: -");
    private final ReadOnlyStringWrapper mouseImpedanceZText = new ReadOnlyStringWrapper("Z: -");
    private final ReadOnlyListWrapper<DataPoint> previewTransformedS1PPoints = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final Stack<UndoRedoEntry> undoStack = new Stack<>();
    private final Stack<UndoRedoEntry> redoStack = new Stack<>();
    private final ObjectProperty<CircuitElement> previewElement = new SimpleObjectProperty<>();
    // Circle display options
    private final ReadOnlyListWrapper<Double> vswrCircles = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final ReadOnlyDoubleWrapper s1pPointSize = new ReadOnlyDoubleWrapper(4.0);
    // Display options
    private boolean showSweepInDataPoints = false;
    private boolean showS1PInDataPoints = false;
    // Memory property for the characteristic impedance
    private double savedFrequency = 1e9; // 1 GHz
    private Complex savedLoadImpedance = new Complex(100.0, 50.0); // 100 + j50 Ohm
    // RangeSlider private properties
    private double freqRangeMin;
    private double freqRangeMax;
    // S1P Load option
    private boolean useS1PAsLoad = false;
    // Sweep stuff
    private double currentSweepMin = 1e6;
    private double currentSweepMax = 100e6;
    private int currentSweepCount = 10;



    public SmithChartViewModel() {
        // When any sources change, trigger a full recalculation.
        zo.addListener((_, _, _) -> {
            zoText.set(zo.get() + " Ω");
            recalculateImpedanceChain();
        });
        frequency.addListener((_, _, _) -> {
            //Update the display for frequency
            double freq = frequency.get();
            String newFreqText = switch ((int) Math.log10(freq)) {
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
                        elem.realWorldValueProperty().addListener((_, _, _) -> {
                            recalculateImpedanceChain();
                            performFrequencySweep();
                        });
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
        dataPoints.addListener((ListChangeListener<DataPoint>) _ -> {
            if (useS1PAsLoad) recalculateS1PChain();
            recalculateAllGammas();
        });

        s1pDataPoints.addListener((ListChangeListener<DataPoint>) _ -> recalculateS1PChain());

        previewElement.addListener((_, _, _) -> {
            if (previewElement.get() == null) {
                previewTransformedS1PPoints.clear();
                cachedS1PPoints.clear();
                return;
            }
            recalculateS1PChain();
        });

        // Perform the initial calculation when the view model is created.
        recalculateImpedanceChain();

        // Listen for changes in the main data points and update combined points
        dataPoints.addListener((ListChangeListener<DataPoint>) _ -> updateCombinedDataPoints());
        s1pDataPoints.addListener((ListChangeListener<DataPoint>) _ -> updateCombinedDataPoints());
        sweepDataPoints.addListener((ListChangeListener<DataPoint>) _ -> updateCombinedDataPoints());

        // Ensure combined points are initialized
        updateCombinedDataPoints();
    }

    public ReadOnlyDoubleProperty s1pPointSizeProperty() {
        return s1pPointSize.getReadOnlyProperty();
    }

    public ReadOnlyListProperty<DataPoint> sweepDataPointsProperty() {
        return sweepDataPoints.getReadOnlyProperty();
    }

    public ReadOnlyListProperty<DataPoint> dataPointsProperty() {
        return combinedDataPoints.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<CircuitElement> previewElementProperty() {
        return previewElement;
    }

    public ReadOnlyListProperty<Double> vswrCirclesProperty() {
        return vswrCircles.getReadOnlyProperty();
    }

    public final ReadOnlyDoubleProperty frequencyProperty() {
        return frequency;
    }

    public ReadOnlyStringProperty frequencyTextProperty() {
        return frequencyText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty zoProperty() {
        return zoText.getReadOnlyProperty();
    }

    public final ReadOnlyListProperty<DataPoint> s1pDataPointsProperty() {
        return s1pDataPoints.getReadOnlyProperty();
    }

    public ReadOnlyListProperty<DataPoint> transformedS1PPointsProperty() {
        return transformedS1PPoints.getReadOnlyProperty();
    }

    // Binding getters
    public ReadOnlyStringProperty mouseReturnLossTextProperty() {
        return mouseReturnLossText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty mouseVSWRTextProperty() {
        return mouseVSWRText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty mouseQualityFactorTextProperty() {
        return mouseQualityFactorText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty mouseGammaTextProperty() {
        return mouseGammaText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty mouseAdmittanceYTextProperty() {
        return mouseAdmittanceYText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty mouseImpedanceZTextProperty() {
        return mouseImpedanceZText.getReadOnlyProperty();
    }

    public ReadOnlyListProperty<DataPoint> previewTransformedS1PPointsProperty() {
        return previewTransformedS1PPoints.getReadOnlyProperty();
    }

    public void addLiveComponentPreview(Double liveValue, double z0Line, double permittivity, Line.StubType stubType) {
        if (stubType == null || stubType == Line.StubType.NONE) {
            previewElement.set(new Line(liveValue, z0Line, permittivity));
        } else {
            previewElement.set(new Line(liveValue, z0Line, permittivity, stubType));
        }
    }

    public void addLiveComponentPreview(CircuitElement.ElementType type, Double liveValue, CircuitElement.ElementPosition position) {
        CircuitElement element = switch (type) {
            case INDUCTOR -> new Inductor(liveValue, position, type);
            case CAPACITOR -> new Capacitor(liveValue, position, type);
            case RESISTOR -> new Resistor(liveValue, position, type);
            default -> null;
        };
        previewElement.set(element);
    }

    public void clearLiveComponentPreview() {
        previewElement.set(null);
        cachedS1PPoints.clear();
        recalculateS1PChain();
    }

    public void setUseS1PAsLoad(Boolean newVal) {
        if (this.useS1PAsLoad == newVal) return; //No change

        this.useS1PAsLoad = newVal;
        if (useS1PAsLoad) {
            // Save current state
            savedFrequency = frequency.get();
            savedLoadImpedance = loadImpedance.get();

            updateMiddleRangePoint();

        } else {
            // Restore saved state
            loadImpedance.set(savedLoadImpedance);
            frequency.set(savedFrequency);
        }
    }

    public void updateMiddleRangePoint() {
        if (!useS1PAsLoad) return; //Only update if we are using S1P as load

        if (s1pDataPoints.isEmpty()) return; //No S1P data to use
        int s1pIndexMin = getS1PIndexAtRange(freqRangeMin);
        int s1pIndexMax = getS1PIndexAtRange(freqRangeMax);
        if (s1pIndexMin > s1pIndexMax) {
            // Invalid range, skip calculation
            return;
        }
        DataPoint middlePoint = s1pDataPoints.get((s1pIndexMin + s1pIndexMax) / 2);
        loadImpedance.set(middlePoint.getImpedance());
        frequency.set(middlePoint.getFrequency());
    }

    public void setFrequency(Double newFreq) {
        this.frequency.set(newFreq);
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
        double freq = frequency.get();

        Complex loadGamma = calculateGamma(currentImpedance);
        double loadVswr = calculateVswr(loadGamma);
        double loadRetLoss = calculateReturnLoss(loadGamma);

        newDataPoints.add(new DataPoint(freq, "LD", currentImpedance, loadGamma, loadVswr, loadRetLoss));

        // Sequentially add the effect of each component
        int index = 1;
        for (CircuitElement element : circuitElements) {

            if (element.getType() == CircuitElement.ElementType.LINE) {
                currentImpedance = ((Line) element).calculateImpedance(currentImpedance, freq);
            } else {
                Complex elementImpedance = element.getImpedance(freq);
                currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, element.getElementPosition());
            }
            //Calculate the values needed for the data points
            Complex elGamma = calculateGamma(currentImpedance);
            double elVswr = calculateVswr(elGamma);
            double elRetLoss = calculateReturnLoss(elGamma);

            newDataPoints.add(new DataPoint(freq, "DP" + index++, currentImpedance, elGamma, elVswr, elRetLoss));
        }

        // Atomically update the main 'measures' property with the new list.
        dataPoints.setAll(newDataPoints);
        // Also recalculate the S1P chain if needed
        recalculateS1PChain();
        // Finally, perform a frequency sweep if needed
        performFrequencySweep();
    }

    private int getS1PIndexAtRange(double freq) {
        for (int i = 0; i < s1pDataPoints.size(); i++) {
            if (s1pDataPoints.get(i).getFrequency() >= freq) {
                return i;
            }
        }
        return s1pDataPoints.size() - 1; // Return last index if freq is beyond range
    }

    /**
     * Calculate the VSWR using the reflection coefficients (Gamma)
     *
     * @param gamma the reflection coefficients
     * @return the VSWR
     */
    private double calculateVswr(Complex gamma) {
        double gammaNorm = gamma.magnitude();
        return (gammaNorm < 1e-9) ? Double.POSITIVE_INFINITY : (1 + gammaNorm) / (1 - gammaNorm);
    }

    /**
     * Calculate the return loss value using the reflection coefficients (Gamma)
     *
     * @param gamma the reflection coefficients
     * @return the return loss value
     */
    private double calculateReturnLoss(Complex gamma) {
        double gammaNorm = gamma.magnitude();
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
        double gammaMagnitude = gamma.magnitude();

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
        if (denominator.magnitude() < 1e-9) {
            // Handle open circuit - set to very high impedance or infinity
            mouseReturnLossText.set("0.00 dB");
            mouseVSWRText.set("∞");
            mouseQualityFactorText.set("∞");
            mouseGammaText.set(String.format("%.3f ∠ %.3f°", gamma.magnitude(), Math.toDegrees(gamma.angle())));
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
        mouseGammaText.set(String.format("%.3f ∠ %.3f°", gamma.magnitude(), Math.toDegrees(gamma.angle())));
        mouseImpedanceZText.set(impedanceZ.toString());
        mouseAdmittanceYText.set(admittanceY.toStringmS());
        mouseQualityFactorText.set(String.format("%.3f", qFactor));
    }

    /**
     * Calculates the reflection coefficient (Gamma) for a given impedance Z.
     *
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
        addComponent(type, value, 0.0, 0.0, position, null);
    }

    void addS1PDatapoints(List<DataPoint> dp) {
        s1pDataPoints.setAll(dp);
    }

    public void clearS1PDatapoints() {
        s1pDataPoints.clear();
    }

    public void clearSweepPoints() {
        sweepDataPoints.clear();
        pointToSweep.clear();
    }

    private void recalculateS1PChain() {
        if (!useS1PAsLoad || s1pDataPoints.isEmpty()) {
            transformedS1PPoints.clear();
            if (!useS1PAsLoad) transformedS1PPoints.setAll(s1pDataPoints);
            cachedS1PPoints.clear(); // Just in case
            return;
        }

        List<DataPoint> newTransformedPoints = new ArrayList<>();
        boolean isPreviewing = previewElement.get() != null;

        List<DataPoint> sourcePoints;

        if (isPreviewing) {
            if (cachedS1PPoints.isEmpty()) {
                sourcePoints = s1pDataPoints;
                isPreviewing = false; // No valid cache, do full calc
            } else {
                sourcePoints = cachedS1PPoints;
            }
        } else {
            sourcePoints = s1pDataPoints;
        }

        List<CircuitElement> elementsToApply;

        if (isPreviewing) {
            elementsToApply = new ArrayList<>();
            elementsToApply.add(previewElement.get()); // Only the preview element
        } else {
            elementsToApply = circuitElements.get();
        }

        for (DataPoint originalPoint : sourcePoints) {
            double freq = originalPoint.getFrequency();

            Complex currentImpedance = originalPoint.getImpedance();

            currentImpedance = propagateThroughElements(currentImpedance, freq, elementsToApply);

            Complex newGamma = calculateGamma(currentImpedance);
            newTransformedPoints.add(new DataPoint(freq, originalPoint.getLabel(), currentImpedance, newGamma, calculateVswr(newGamma), calculateReturnLoss(newGamma)));
        }

        transformedS1PPoints.setAll(newTransformedPoints);

        // Update cache only if not previewing (means we added a new component)
        if (!isPreviewing) {
            cachedS1PPoints.clear();
            cachedS1PPoints.addAll(newTransformedPoints);
        }
    }

    /**
     * Adds a new component to the circuit and triggers a full recalculation.
     */
    void addComponent(CircuitElement.ElementType type, double value, double characteristicImpedance, double permittivity, CircuitElement.ElementPosition position, Line.StubType stubType) {
        CircuitElement newElem = switch (type) {
            case INDUCTOR -> new Inductor(value, position, type);
            case CAPACITOR -> new Capacitor(value, position, type);
            case RESISTOR -> new Resistor(value, position, type);
            case LINE -> {
                if (stubType == null || stubType == Line.StubType.NONE) {
                    yield new Line(value, characteristicImpedance, permittivity);
                } else {
                    yield new Line(value, characteristicImpedance, permittivity, stubType);
                }
            }
        };

        int index = circuitElements.size();
        circuitElements.add(newElem);

        // Record the ADD operation
        undoStack.push(new UndoRedoEntry(Operation.ADD, index, newElem));
        redoStack.clear(); // New action clears redo history

        recalculateImpedanceChain();
    }

    /**
     * Remove the component using their index number, used to remove from the point list
     *
     * @param index of the component to remove
     */
    void removeComponentAt(int index) {
        try {
            if (index < 0 || index >= circuitElements.size()) return; //Boundary check

            CircuitElement removed = circuitElements.get(index);
            circuitElements.remove(index);

            // Record the REMOVE operation
            undoStack.push(new UndoRedoEntry(Operation.REMOVE, index, removed));
            redoStack.clear(); // New action clears redo history

            recalculateImpedanceChain();
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getLogger("Error").log(Level.SEVERE, e.getMessage());
        }

    }

    /**
     * Helper function to calculate the resulting impedance after adding a new component.
     *
     * @param previousImpedance The impedance at the previous point in the chain.
     * @param impedanceToAdd    The impedance of the new component.
     * @param position          Whether the component is in SERIES or PARALLEL.
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

    public Complex getLastGamma() {
        if (dataPoints.isEmpty()) return null;
        return dataPoints.getLast().gammaProperty().get();
    }

    public DataPoint getLastDataPoint() {
        if (dataPoints.isEmpty()) return null;
        return dataPoints.getLast();
    }

    public ReadOnlyListProperty<Complex> measuresGammaProperty() {
        return measuresGamma.getReadOnlyProperty();
    }


    private void updateCombinedDataPoints() {
        var combined = FXCollections.observableArrayList(dataPoints);
        if (showS1PInDataPoints) combined.addAll(s1pDataPoints);
        if (showSweepInDataPoints) combined.addAll(sweepDataPoints);
        combinedDataPoints.setAll(combined);
    }


    // --- Public Properties for Binding ---

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

    public void setFrequencyRangeMin(double v) {
        this.freqRangeMin = v;
    }

    public void setFrequencyRangeMax(double v) {
        this.freqRangeMax = v;
    }

    public boolean isFrequencyInRange(double freq) {
        return freq >= freqRangeMin && freq <= freqRangeMax;
    }

    public void setShowSweepDataPoints(boolean selected) {
        if (this.showSweepInDataPoints != selected) {
            this.showSweepInDataPoints = selected;
            updateCombinedDataPoints();
        }
    }

    public void setShowS1PDataPoints(boolean selected) {
        if (this.showS1PInDataPoints != selected) {
            this.showS1PInDataPoints = selected;
            updateCombinedDataPoints();
        }
    }

    public void exportSweepToS1P(File file, String fileName) {
        if (sweepDataPoints.isEmpty()) return;

        File outputFile = file;
        if (outputFile.isDirectory()) {
            outputFile = new File(outputFile, fileName + ".s1p");
        }

        int indexToTake = sweepDataPoints.getSize() / 2;
        FrequencyUnit frequencyUnit = (FrequencyUnit) SmithUtilities.getBestUnitAndFormattedValue(
                sweepDataPoints.get(indexToTake).getFrequency(), FrequencyUnit.values()
        ).getKey();

        try {
            TouchstoneS1P.export(sweepDataPoints, zo.get(), frequencyUnit, outputFile);
        } catch (Exception e) {
            Logger.getLogger("Error").log(Level.SEVERE, "Error exporting sweep to S1P: " + e.getMessage());
        }
    }


    public void setCircleDisplayOptions(List<Double> options) {
        vswrCircles.setAll(options);
    }

    private void performFrequencySweep() {
        System.out.println("Performing frequency sweep");
        performFrequencySweep(pointToSweep);
    }

    public void performFrequencySweep(List<Double> frequencies) {
        if (frequencies.isEmpty()) return;

        // Capture state for the buttons/text fields
        frequencies.sort(Double::compareTo); // Ensure sorted
        this.currentSweepMin = frequencies.getFirst();
        this.currentSweepMax = frequencies.getLast();
        this.currentSweepCount = frequencies.size();
        this.pointToSweep.setAll(frequencies); // Save for re-sweeps on circuit change

        List<DataPoint> sweepPoints = new ArrayList<>();
        Complex startingLoad = loadImpedance.get();

        for (Double freq : frequencies) {
            Complex finalImpedance = propagateThroughElements(startingLoad, freq, circuitElements.get());
            Complex gamma = calculateGamma(finalImpedance);
            double vswr = calculateVswr(gamma);
            double retLoss = calculateReturnLoss(gamma);

            sweepPoints.add(new DataPoint(freq, "SWEEP", finalImpedance, gamma, vswr, retLoss));
        }
        sweepDataPoints.setAll(sweepPoints);
    }

    public void updateSweepConfiguration(double minFreq, double maxFreq, int count) {
        if (minFreq >= maxFreq || count < 2) return; // Validation

        this.currentSweepMin = minFreq;
        this.currentSweepMax = maxFreq;
        this.currentSweepCount = count;

        List<Double> frequencies = new ArrayList<>();
        double step = (maxFreq - minFreq) / (count - 1);

        for (int i = 0; i < count; i++) {
            frequencies.add(minFreq + (i * step));
        }

        performFrequencySweep(frequencies);
    }

    public void incrementSweepStartFrequency() {
        double newMin = currentSweepMin * 1.05;
        if (newMin < currentSweepMax) {
            updateSweepConfiguration(newMin, currentSweepMax, currentSweepCount);
        }
    }

    public void decrementSweepStartFrequency() {
        updateSweepConfiguration(currentSweepMin * 0.95, currentSweepMax, currentSweepCount);
    }

    public void incrementSweepEndFrequency() {
        updateSweepConfiguration(currentSweepMin, currentSweepMax * 1.05, currentSweepCount);
    }

    public void decrementSweepEndFrequency() {
        double newMax = currentSweepMax * 0.95;
        if (newMax > currentSweepMin) {
            updateSweepConfiguration(currentSweepMin, newMax, currentSweepCount);
        }
    }


    private Complex propagateThroughElements(Complex startImpedance, double freq, List<CircuitElement> elements) {
        Complex currentImpedance = startImpedance;
        for (CircuitElement element : elements) {
            if (element.getType() == CircuitElement.ElementType.LINE) {
                currentImpedance = ((Line) element).calculateImpedance(currentImpedance, freq);
            } else {
                Complex elementImpedance = element.getImpedance(freq);
                currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, element.getElementPosition());
            }
        }
        return currentImpedance;
    }

    public void setS1PPointSize(double v) {
        s1pPointSize.set(v);
    }

    // Undo Redo logic
    private enum Operation {ADD, REMOVE}

    private record UndoRedoEntry(Operation operation, int index, CircuitElement element) {
    }
}
