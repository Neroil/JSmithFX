package heig.tb.jsmithfx;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.Capacitor;
import heig.tb.jsmithfx.model.Element.Inductor;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import java.util.ArrayList;
import java.util.List;

public class SmithChartViewModel {

    public final DoubleProperty frequency = new SimpleDoubleProperty(1e9); // e.g., 1 GHz
    public final DoubleProperty zo = new SimpleDoubleProperty(50.0);
    public final ObjectProperty<Complex> loadImpedance = new SimpleObjectProperty<>(new Complex(50.0, 0.0));
    public final SimpleListProperty<CircuitElement> circuitElements = new SimpleListProperty<>(FXCollections.observableArrayList());

    // The master list of impedances. The load is always at index 0.
    public final SimpleListProperty<Complex> measures = new SimpleListProperty<>(FXCollections.observableArrayList());
    // A read-only list of the calculated gammas for drawing on the canvas.
    private final ReadOnlyListWrapper<Complex> measuresGamma = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());


    public SmithChartViewModel() {
        // When any sources change, trigger a full recalculation.
        zo.addListener((obs, oldVal, newVal) -> recalculateImpedanceChain());
        frequency.addListener((obs, oldVal, newVal) -> recalculateImpedanceChain());
        loadImpedance.addListener((obs, oldVal, newVal) -> recalculateImpedanceChain());

        // When the list of derived impedances changes, automatically update the gamma values.
        measures.addListener((ListChangeListener<Complex>) c -> recalculateAllGammas());

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
        List<Complex> newImpedancePoints = new ArrayList<>();
        newImpedancePoints.add(currentImpedance); // Start with the load impedance

        // Sequentially add the effect of each component
        for (CircuitElement element : circuitElements) {
            Complex elementImpedance = element.getImpedance(frequency.get());
            currentImpedance = calculateNextImpedance(currentImpedance, elementImpedance, element.getElementPosition());
            newImpedancePoints.add(currentImpedance);
        }

        // Atomically update the main 'measures' property with the new list.
        // This will automatically trigger the 'measures.addListener' to update the gammas.
        measures.setAll(newImpedancePoints);
    }

    /**
     * Converts the entire measures list (impedances) into the measuresGamma list (reflection coefficients).
     */
    private void recalculateAllGammas() {
        List<Complex> newGammas = new ArrayList<>();
        for (Complex measure : measures) {
            if (measure != null) {
                newGammas.add(calculateGamma(measure));
            }
        }
        measuresGamma.setAll(newGammas);
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
        // Calculate the last impedance
        Complex lastImpedance = measures.isEmpty() ? loadImpedance.get() : measures.getLast();
        measures.add(calculateNextImpedance(lastImpedance, newElem.getImpedance(frequency.get()), position)); //Will trigger the gamma recalculation
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


    // --- Public Properties for Binding ---

    public ReadOnlyListProperty<Complex> measuresGammaProperty() {
        return measuresGamma.getReadOnlyProperty();
    }

    public SimpleListProperty<Complex> measuresProperty() {
        return measures;
    }
}