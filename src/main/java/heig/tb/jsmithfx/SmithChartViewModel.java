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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SmithChartViewModel {

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
    private final IntegerProperty dpTableSelIndex = new SimpleIntegerProperty(-1);
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
    private final Deque<UndoRedoEntry> undoStack = new ArrayDeque<>();
    private final Deque<UndoRedoEntry> redoStack = new ArrayDeque<>();
    private final ObjectProperty<CircuitElement> previewElementS1P = new SimpleObjectProperty<>();
    // Circle display options
    private final ReadOnlyListWrapper<Double> vswrCircles = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final ReadOnlyDoubleWrapper s1pPointSize = new ReadOnlyDoubleWrapper(4.0);
    // Selected element for tuning
    private final ObjectProperty<CircuitElement> selectedElement = new SimpleObjectProperty<>();
    // Modify component logic vars
    public BooleanProperty isModifyingComponent = new SimpleBooleanProperty(false);
    // Display options
    private boolean showSweepInDataPoints = false;
    private boolean showS1PInDataPoints = false;
    // Memory property for the characteristic impedance
    private double savedFrequency = -1; //-1 means no saved state
    private Complex savedLoadImpedance;
    // RangeSlider private properties
    private double freqRangeMinF1;
    private double freqRangeMaxF1;
    private double freqRangeMinF2;
    private double freqRangeMaxF2;
    private double freqRangeMinF3;
    private double freqRangeMaxF3;
    // S1P Load option for each filters
    private final BooleanProperty useS1PAsLoadF1 = new SimpleBooleanProperty(false);
    private final BooleanProperty useS1PAsLoadF2 = new SimpleBooleanProperty(false);
    private final BooleanProperty useS1PAsLoadF3 = new SimpleBooleanProperty(false);

    public BooleanProperty useS1PAsLoadF1Property() { return useS1PAsLoadF1; }
    public BooleanProperty useS1PAsLoadF2Property() { return useS1PAsLoadF2; }
    public BooleanProperty useS1PAsLoadF3Property() { return useS1PAsLoadF3; }

    private final BooleanProperty filter1Enabled = new SimpleBooleanProperty(true);
    private final BooleanProperty filter2Enabled = new SimpleBooleanProperty(false);
    private final BooleanProperty filter3Enabled = new SimpleBooleanProperty(false);

    public BooleanProperty filter1EnabledProperty() { return filter1Enabled; }
    public BooleanProperty filter2EnabledProperty() { return filter2Enabled; }
    public BooleanProperty filter3EnabledProperty() { return filter3Enabled; }

    // Component hovering
    private final ObjectProperty<CircuitElement> hoveredElement = new SimpleObjectProperty<>();
    public ReadOnlyObjectProperty<CircuitElement> hoveredElementProperty() {
        return hoveredElement;
    }
    private final IntegerProperty selectedInsertionIndex = new SimpleIntegerProperty(0); // 0 will select the only position
    public ReadOnlyIntegerProperty getSelectedInsertionIndexProperty() {
        return selectedInsertionIndex;
    }
    public void setSelectedInsertionIndex(int index) {
        this.selectedInsertionIndex.set(index);
    }


    // Sweep stuff
    private double currentSweepMin = 1e6;
    private double currentSweepMax = 100e6;
    private int currentSweepCount = 10;
    // Storing the original value in case the client cancel or does something else
    private CircuitElement originalElement;
    // Preview while adding new component
    private final ObjectProperty<CircuitElement> previewElement = new SimpleObjectProperty<>();

    // Quality factor stuff
    private final BooleanProperty isUsingQualityFactor = new SimpleBooleanProperty(false);
    public ReadOnlyBooleanProperty isUsingQualityFactorProperty() {
        return isUsingQualityFactor;
    }

    public boolean isAnyUseS1PAsLoad() {
        return useS1PAsLoadF1.get() || useS1PAsLoadF2.get() || useS1PAsLoadF3.get();
    }

    private SmithChartViewModel() {
        // When any sources change, trigger a full recalculation.
        zo.addListener((_, _, _) -> {
            zoText.set(zo.get() + " Ω");
            recalculateImpedanceChain();
        });
        frequency.addListener((_, _, _) -> {
            //Update the display for frequency
            double freq = frequency.get();
            String newFreqText = SmithUtilities.displayBestUnitAndFormattedValue(freq, FrequencyUnit.values());
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
            if (isAnyUseS1PAsLoad()) recalculateS1PChain();
            recalculateAllGammas();
        });

        s1pDataPoints.addListener((ListChangeListener<DataPoint>) _ -> recalculateS1PChain());

        previewElementS1P.addListener((_, _, _) -> {
            if (previewElementS1P.get() == null) {
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

    public static SmithChartViewModel getInstance() {
        return Holder.INSTANCE;
    }

    public ReadOnlyIntegerProperty getDpTableSelIndex() {
        return dpTableSelIndex;
    }

    public void setDpTableSelIndex(int dpTableSelIndex) {
        this.dpTableSelIndex.set(dpTableSelIndex);
    }

    public boolean isShowS1PAsLoad() {
        return isAnyUseS1PAsLoad();
    }

    public ReadOnlyObjectProperty<CircuitElement> previewElementProperty() {
        return previewElement;
    }

    public Complex getPreviewElementGamma() {
        CircuitElement preview = previewElement.get();
        if (preview == null) return null;

        Complex currentImpedance = getCurrentInteractionStartImpedance();

        if (currentImpedance == null) return null;

        double freq = frequency.get();
        Complex finalImpedance;

        // Apply only the preview element
        if (preview.getType() == CircuitElement.ElementType.LINE) {
            finalImpedance = ((Line) preview).calculateImpedance(currentImpedance, freq);
        } else {
            Complex elementImpedance = preview.getImpedance(freq);
            finalImpedance = calculateNextImpedance(currentImpedance, elementImpedance, preview.getElementPosition());
        }

        return calculateGamma(finalImpedance);
    }

    public ReadOnlyObjectProperty<CircuitElement> selectedElementProperty() {
        return selectedElement;
    }

    public void selectElement(CircuitElement element) {
        // Cancel if the user switch from one to another element without applying
        if (selectedElement.get() != null) {
            cancelTuningAdjustments();
        }

        if (element != null && circuitElements.contains(element)) {
            selectedElement.set(element);
            // Save the state BEFORE tuning starts
            originalElement = element.copy();
        } else {
            selectedElement.set(null);
        }
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

    public ReadOnlyObjectProperty<CircuitElement> previewElementS1PProperty() {
        return previewElementS1P;
    }

    public ReadOnlyListProperty<Double> vswrCirclesProperty() {
        return vswrCircles.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty frequencyProperty() {
        return frequency;
    }

    public ReadOnlyStringProperty frequencyTextProperty() {
        return frequencyText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty zoProperty() {
        return zoText.getReadOnlyProperty();
    }

    public ReadOnlyListProperty<DataPoint> s1pDataPointsProperty() {
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

    public void addLiveComponentPreview(Double liveValue, double z0Line, double permittivity, Line.StubType stubType, Optional<Double> qualityFactor) {
        Line newLine;
        if (stubType == null || stubType == Line.StubType.NONE) {
            if (isShowS1PAsLoad()) previewElementS1P.set(new Line(liveValue, z0Line, permittivity));
            newLine = new Line(liveValue, z0Line, permittivity);

        } else {
            if (isShowS1PAsLoad()) previewElementS1P.set(new Line(liveValue, z0Line, permittivity, stubType));
            newLine = new Line(liveValue, z0Line, permittivity, stubType);
        }
        qualityFactor.ifPresent(newLine::setQualityFactor);
        previewElement.set(newLine);
    }

    public void addLiveComponentPreview(CircuitElement.ElementType type, Double liveValue, CircuitElement.ElementPosition position, Optional<Double> qualityFactor) {
        CircuitElement element = switch (type) {
            case INDUCTOR -> new Inductor(liveValue, position, type);
            case CAPACITOR -> new Capacitor(liveValue, position, type);
            case RESISTOR -> new Resistor(liveValue, position, type);
            default -> null;
        };
        if (element != null) qualityFactor.ifPresent(element::setQualityFactor);
        if (isShowS1PAsLoad()) previewElementS1P.set(element);
        previewElement.set(element);
    }

    public void clearLiveComponentPreview() {
        previewElementS1P.set(null);
        previewElement.set(null);
        cachedS1PPoints.clear();
        recalculateS1PChain();
    }

    public void setUseS1PAsLoadF1(Boolean newVal) {
        if (newVal == null || newVal == useS1PAsLoadF1.get()) return;
        setUseS1PAsLoad(newVal, 1);
    }

    public void setUseS1PAsLoadF2(Boolean newVal) {
        if (newVal == null || newVal == useS1PAsLoadF2.get()) return;
        setUseS1PAsLoad(newVal, 2);
    }

    public void setUseS1PAsLoadF3(Boolean newVal) {
        if (newVal == null || newVal == useS1PAsLoadF3.get()) return;
        setUseS1PAsLoad(newVal, 3);
    }

    private void setUseS1PAsLoad(Boolean newVal, int filterNumber) {
        switch(filterNumber) {
            case 1:
                useS1PAsLoadF1.set(newVal);
                useS1PAsLoadF2.set(false);
                useS1PAsLoadF3.set(false);
                break;
            case 2:
                useS1PAsLoadF1.set(false);
                useS1PAsLoadF2.set(newVal);
                useS1PAsLoadF3.set(false);
                break;
            case 3:
                useS1PAsLoadF1.set(false);
                useS1PAsLoadF2.set(false);
                useS1PAsLoadF3.set(newVal);
                break;
            default:
                return; //Invalid filter number
        }

        if (savedFrequency == -1 && savedLoadImpedance == null) {
            // Save current state
            savedFrequency = frequency.get();
            savedLoadImpedance = loadImpedance.get();

        } else if (!isAnyUseS1PAsLoad()){
            // Restore saved state
            loadImpedance.set(savedLoadImpedance);
            frequency.set(savedFrequency);
            savedFrequency = -1;
            savedLoadImpedance = null;
            return; //No need to update middle point
        }

        updateMiddleRangePoint();
    }

    public void setS1PLoadValue(Double newValue) {
        if (!isAnyUseS1PAsLoad()) return; //Only update if we are using S1P as load

        if (s1pDataPoints.isEmpty()) return; //No S1P data to use
        DataPoint targetPoint = s1pDataPoints.get(getS1PIndexAtRange(newValue));
        loadImpedance.set(targetPoint.getImpedance());
        frequency.set(targetPoint.getFrequency());
    }

    private int whichFilterIsUsingS1PAsLoad() {
        if (useS1PAsLoadF1.get()) return 1;
        if (useS1PAsLoadF2.get()) return 2;
        if (useS1PAsLoadF3.get()) return 3;
        return -1; //None
    }

    public void updateMiddleRangePoint() {
        if (!isAnyUseS1PAsLoad()) return; //Only update if we are using S1P as load

        if (s1pDataPoints.isEmpty()) return; //No S1P data to use
        int s1pIndexMin;
        int s1pIndexMax;

        switch (whichFilterIsUsingS1PAsLoad()) {
            case 1:
                s1pIndexMin = getS1PIndexAtRange(freqRangeMinF1);
                s1pIndexMax = getS1PIndexAtRange(freqRangeMaxF1);
                break;
            case 2:
                s1pIndexMin = getS1PIndexAtRange(freqRangeMinF2);
                s1pIndexMax = getS1PIndexAtRange(freqRangeMaxF2);
                break;
            case 3:
                s1pIndexMin = getS1PIndexAtRange(freqRangeMinF3);
                s1pIndexMax = getS1PIndexAtRange(freqRangeMaxF3);
                break;
            default:
                return; //Invalid filter number
        }

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

    public void addComponent(CircuitElement.ElementType type, double value, CircuitElement.ElementPosition position, Optional<Double> qualityFactor) {
        addComponent(type, value, 0.0, 0.0, position, qualityFactor, null);
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
        if (!isAnyUseS1PAsLoad() || s1pDataPoints.isEmpty()) {
            transformedS1PPoints.clear();
            if (!isAnyUseS1PAsLoad()) transformedS1PPoints.setAll(s1pDataPoints);
            cachedS1PPoints.clear(); // Just in case
            return;
        }

        List<DataPoint> newTransformedPoints = new ArrayList<>();
        boolean isPreviewing = previewElementS1P.get() != null;

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
            elementsToApply.add(previewElementS1P.get()); // Only the preview element
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

    public Complex getCurrentInteractionStartImpedance() {
        // If we are modifying an existing component
        if (isModifyingComponent.get() && selectedElement.get() != null) {
            // Find the index of the element we are modifying
            int index = circuitElements.indexOf(selectedElement.get());

            // Safety check
            if (index != -1 && index < dataPoints.size()) {
                return dataPoints.get(index).impedanceProperty().get();
            }
        } else if (selectedInsertionIndex.get() >= 0 && selectedInsertionIndex.get() < dataPoints.size()) {
            return dataPoints.get(selectedInsertionIndex.get()).impedanceProperty().get();
        }
        // If we add a new component, just return the last gamma in the chain
        return getLastImpedance();
    }

    public Complex getCurrentInteractionStartGamma() {
        if (isModifyingComponent.get() && selectedElement.get() != null) {
            // Find the index of the element we are modifying
            int index = circuitElements.indexOf(selectedElement.get());

            // Safety check
            if (index != -1 && index < dataPoints.size()) {
                return dataPoints.get(index).gammaProperty().get();
            }
        } else if (selectedInsertionIndex.get() >= 0 && selectedInsertionIndex.get() < dataPoints.size()) {
            return dataPoints.get(selectedInsertionIndex.get()).gammaProperty().get();
        }

        return getLastGamma();
    }

    /**
     * Adds a new component to the circuit and triggers a full recalculation.
     */
    public void addComponent(CircuitElement.ElementType type, double value, double characteristicImpedance, double permittivity, CircuitElement.ElementPosition position, Optional<Double> qualityFactor, Line.StubType stubType) {
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

        if (isUsingQualityFactor.get() && qualityFactor.isPresent()) newElem.setQualityFactor(qualityFactor.get());

        int index;

        if (isModifyingComponent.get() && selectedElement.get() != null) {
            index = circuitElements.indexOf(selectedElement.get());
            if (index == -1) {
                System.out.println("Error: Selected element not found in the circuit elements list.");
                return;
            }

            CircuitElement oldElem = circuitElements.get(index);
            circuitElements.set(index, newElem);
            undoStack.push(new UndoRedoEntry(Operation.MODIFY, index, oldElem));
        } else if (selectedInsertionIndex.get() >= 0 && selectedInsertionIndex.get() <= circuitElements.size()) {
            index = selectedInsertionIndex.get();
            circuitElements.add(index, newElem);

            // Record the ADD operation
            undoStack.push(new UndoRedoEntry(Operation.ADD, index, newElem));
        } else {
            index = circuitElements.size();
            circuitElements.add(newElem);

            // Record the ADD operation
            undoStack.push(new UndoRedoEntry(Operation.ADD, index, newElem));
        }

        redoStack.clear(); // New action clears redo history
        selectedElement.set(null); // Clear selection after adding/modifying

        // Update the selected insertion index to the end
        selectedInsertionIndex.set(circuitElements.size());
        recalculateImpedanceChain();
    }

    public List<Complex> getProjectedGammas() {
        CircuitElement preview = previewElement.get();
        if (preview == null) return new ArrayList<>();

        List<Complex> projectedGammas = new ArrayList<>();
        int insertIndex = selectedInsertionIndex.get();
        List<CircuitElement> elements = circuitElements.get();

        // Safety check for insertion index
        if (insertIndex < 0 || insertIndex >= elements.size()) {
            return projectedGammas;
        }

        double freq = frequency.get();

        // Determine the starting impedance for the chain
        Complex currentImpedance = getCurrentInteractionStartImpedance(); // Start Z (before preview)

        // Apply preview element
        if (preview.getType() == CircuitElement.ElementType.LINE) {
            currentImpedance = ((Line) preview).calculateImpedance(currentImpedance, freq);
        } else {
            Complex elementImpedance = preview.getImpedance(freq);
            currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, preview.getElementPosition());
        }

        // Loop from the insertion index to the end of the existing list
        for (int i = insertIndex; i < elements.size(); i++) {
            CircuitElement existingElement = elements.get(i);

            if (existingElement.getType() == CircuitElement.ElementType.LINE) {
                currentImpedance = ((Line) existingElement).calculateImpedance(currentImpedance, freq);
            } else {
                Complex elementImpedance = existingElement.getImpedance(freq);
                currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, existingElement.getElementPosition());
            }

            projectedGammas.add(calculateGamma(currentImpedance));
        }

        return projectedGammas;
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

            if (isModifyingComponent.get() && selectedElement.get() != null) {
                // If we were modifying this element, clear the selection
                if (removed == selectedElement.get()) {
                    selectedElement.set(null);
                }
            }

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

    public Complex getLastImpedance() {
        if (dataPoints.isEmpty()) return null;
        return dataPoints.getLast().impedanceProperty().get();
    }


    /**
     * Undo logic for the components ONLY
     */
    public void undo() {
        processHistoryStep(undoStack, redoStack, false);
    }

    /**
     * Redo logic for the components ONLY
     */
    public void redo() {
        processHistoryStep(redoStack, undoStack, true);
    }

    /**
     * Core logic to handle both Undo and Redo operations.
     *
     * @param sourceStack The stack to take the action FROM.
     * @param destStack   The stack to push the inverse action TO.
     * @param isRedo      True if redoing (ADD=ADD), False if undoing (ADD=REMOVE).
     */
    private void processHistoryStep(Deque<UndoRedoEntry> sourceStack, Deque<UndoRedoEntry> destStack, boolean isRedo) {
        cancelTuningAdjustments();
        if (sourceStack.isEmpty()) return;

        UndoRedoEntry entry = sourceStack.pop();

        if (entry.operation == Operation.MODIFY) {
            CircuitElement currentElem = circuitElements.get(entry.index);
            circuitElements.set(entry.index, entry.element);

            destStack.push(new UndoRedoEntry(Operation.MODIFY, entry.index, currentElem));

        } else {
            // For ADD/REMOVE, the action depends on direction
            boolean shouldAdd;

            if (entry.operation == Operation.ADD) {
                shouldAdd = isRedo; // If Undo, ADD becomes Remove (false)
            } else { // Operation.REMOVE
                shouldAdd = !isRedo; // If Undo, REMOVE becomes Add (true)
            }

            if (shouldAdd) {
                circuitElements.add(entry.index, entry.element);
            } else {
                circuitElements.remove(entry.index);
            }

            destStack.push(entry);
        }

        recalculateImpedanceChain();
    }

    public void setFrequencyRangeMinF1(double v) {
        this.freqRangeMinF1 = v;
    }

    public void setFrequencyRangeMaxF1(double v) {
        this.freqRangeMaxF1 = v;
    }

    public void setFrequencyRangeMinF2(double v) {
        this.freqRangeMinF2 = v;
    }

    public void setFrequencyRangeMaxF2(double v) {
        this.freqRangeMaxF2 = v;
    }

    public void setFrequencyRangeMinF3(double v) {
        this.freqRangeMinF3 = v;
    }

    public void setFrequencyRangeMaxF3(double v) {
        this.freqRangeMaxF3 = v;
    }

    public boolean isFrequencyInRange(double freq) {
        boolean inF1 = filter1Enabled.get() && isFrequencyInRangeF1(freq);
        boolean inF2 = filter2Enabled.get() && isFrequencyInRangeF2(freq);
        boolean inF3 = filter3Enabled.get() && isFrequencyInRangeF3(freq);

        return inF1 || inF2 || inF3;
    }

    public boolean isFrequencyInRangeF1(double freq) {
        return freq >= freqRangeMinF1 && freq <= freqRangeMaxF1;
    }

    public boolean isFrequencyInRangeF2(double freq) {
        return freq >= freqRangeMinF2 && freq <= freqRangeMaxF2;
    }

    public boolean isFrequencyInRangeF3(double freq) {
        return freq >= freqRangeMinF3 && freq <= freqRangeMaxF3;
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
        // Create a copy to avoid an issue with the setAll in the main function
        performFrequencySweep(new ArrayList<>(pointToSweep));
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

    /**
     * Called by the Slider in the View to update the value live or when modifying a component
     *
     * @param newValue The new value from the slider
     */
    public void updateTunedElementValue(double newValue) {
        updateTunedElementValue(newValue, null, null, null, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Called by the View when modifying a component to update type/position/value live
     * @param newValue The new value from the modif
     * @param elementType The new type of the element
     * @param elementPosition The new position of the element
     */
    public void updateTunedElementValue(double newValue, CircuitElement.ElementType elementType, CircuitElement.ElementPosition elementPosition, Line.StubType stubType, Optional<Double> permittivity, Optional<Double> z0Line, Optional<Double> qualityFactor) {
        CircuitElement current = selectedElement.get();

        if (current != null) {
            int index = circuitElements.indexOf(current);
            if (index == -1) return;

            // Check if Type changed OR if it's a Line and the StubType changed
            boolean typeChanged = (elementType != null && elementType != current.getType());
            boolean stubChanged = false;

            if (current instanceof Line line && stubType != null) {
                // If the current line has a different stub type than the one requested, we must recreate it
                stubChanged = (line.getStubType() != stubType);
            }

            // If Type or StubType changed, we MUST recreate the object to ensure correct constructor logic (Series vs Shunt)
            if (typeChanged || stubChanged) {
                CircuitElement newElement = switch (elementType) {
                    case INDUCTOR -> new Inductor(newValue, current.getElementPosition(), elementType);
                    case CAPACITOR -> new Capacitor(newValue, current.getElementPosition(), elementType);
                    case RESISTOR -> new Resistor(newValue, current.getElementPosition(), elementType);
                    case LINE -> {
                        if (z0Line.isEmpty() || permittivity.isEmpty()) {
                            yield null;
                        }
                        if (stubType == null || stubType == Line.StubType.NONE) {
                            yield new Line(newValue, z0Line.get(), permittivity.get());
                        } else {
                            yield new Line(newValue, z0Line.get(), permittivity.get(), stubType);
                        }
                    }
                };

                if (newElement != null) {
                    circuitElements.set(index, newElement);
                    selectedElement.set(newElement);
                    current = newElement;
                }
            }

            if (current == null) return;

            if (elementPosition != null && current.getType() != CircuitElement.ElementType.LINE) {
                current.setPosition(elementPosition);
            }

            // Even if we didn't recreate, ensure properties are set
            if (current instanceof Line line) {
                if (stubType != null) line.setStubType(stubType);
                permittivity.ifPresent(line::setPermittivity);
                z0Line.ifPresent(line::setCharacteristicImpedance);
            }

            if (isUsingQualityFactor.get() && qualityFactor.isPresent()) {
                current.setQualityFactor(qualityFactor.get());
            }

            current.setRealWorldValue(newValue);
        }
    }

    /**
     * User clicked "Apply". We commit the change.
     */
    public void applyTuningAdjustments() {
        if (selectedElement.get() == null) return;

        // Record the MODIFY operation
        int index = circuitElements.indexOf(selectedElement.get());

        selectedElement.set(null);
        undoStack.push(new UndoRedoEntry(Operation.MODIFY, index, originalElement.copy()));
    }

    /**
     * User clicked "Cancel". We revert the value.
     */
    public void cancelTuningAdjustments() {
        CircuitElement current = selectedElement.get();
        if (current != null) {
            int index = circuitElements.indexOf(current);
            circuitElements.set(index, originalElement);
            recalculateImpedanceChain();
        }
        selectedElement.set(null);
    }

    public void removeComponent(CircuitElement element) {
        int index = circuitElements.indexOf(element);
        if (index != -1) {
            removeComponentAt(index);
        }
    }

    public void setHoveredElement(CircuitElement hoveredElement) {
        this.hoveredElement.set(hoveredElement);
    }

    public void setUseQualityFactor(Boolean newVal) {
        this.isUsingQualityFactor.set(newVal);
    }

    // Undo Redo logic
    private enum Operation {ADD, REMOVE, MODIFY}

    private static class Holder {
        private static final SmithChartViewModel INSTANCE = new SmithChartViewModel();
    }

    private record UndoRedoEntry(Operation operation, int index, CircuitElement element) {
    }
}
