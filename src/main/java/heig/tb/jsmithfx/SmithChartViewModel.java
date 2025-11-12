package heig.tb.jsmithfx;

import heig.tb.jsmithfx.utilities.Complex;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

public class SmithChartViewModel {

    public final DoubleProperty frequency = new SimpleDoubleProperty(1e9); //Initialized at 1MHz
    public final DoubleProperty zo = new SimpleDoubleProperty(50.0);
    public final ObjectProperty<Complex> loadImpedance = new SimpleObjectProperty<>(new Complex(50.0, 0.0));

    // The master list of impedances. The load is always at index 0.
    public final SimpleListProperty<Complex> measures = new SimpleListProperty<>(FXCollections.observableArrayList());

    // A read-only list of the calculated gammas for drawing on the canvas.
    private final ReadOnlyListWrapper<Complex> measuresGamma = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());


    public SmithChartViewModel() {
        // When the characteristic impedance changes, recalculate everything.
        zo.addListener((obs, oldVal, newVal) -> recalculateAllGammas());

        // When the load impedance object changes, update the first element in our master list.
        loadImpedance.addListener((obs, oldVal, newVal) -> updateLoadInMeasuresList());

        // When the master list of impedances changes, recalculate all the gammas.
        // This is the primary listener that will keep the canvas in sync.
        measures.addListener((ListChangeListener<Complex>) c -> recalculateAllGammas());

        // Initialize the measures list with the starting load impedance.
        updateLoadInMeasuresList();
    }

    /**
     * This is the single source of truth for recalculating the gamma values used for drawing.
     * It clears the old list and builds a new one from the master `measures` list.
     */
    private void recalculateAllGammas() {
        // 1. Clear the previous results to prevent duplication.
        measuresGamma.clear();

        // 2. Iterate through the master list of impedances and calculate the gamma for each.
        for (Complex measure : measures) {
            if (measure != null) {
                measuresGamma.add(calculateGamma(measure));
            }
        }
    }

    /**
     * Ensures the load impedance is always the first element of the `measures` list.
     */
    private void updateLoadInMeasuresList() {
        Complex currentLoad = loadImpedance.get();
        if (currentLoad == null) return;

        if (measures.isEmpty()) {
            measures.add(currentLoad);
        } else {
            measures.set(0, currentLoad);
        }
    }

    /**
     * Calculates the reflection coefficient (Gamma) for a given impedance Z.
     * @param z The complex impedance.
     * @return The complex reflection coefficient.
     */
    private Complex calculateGamma(Complex z) {
        double z0 = zo.get();
        if (z0 <= 0) {
            return new Complex(0, 0); // Avoid division by zero
        }
        // Normalize impedance: z_n = Z / Z0
        Complex zNorm = z.dividedBy(new Complex(z0, 0));

        // Calculate gamma = (z_n - 1) / (z_n + 1)
        return (zNorm.addReal(-1)).dividedBy(zNorm.addReal(1));
    }


    // --- Public Properties for Binding ---

    public ReadOnlyListProperty<Complex> measuresGammaProperty() {
        return measuresGamma.getReadOnlyProperty();
    }

    // In your controller, you will bind to this property
    public SimpleListProperty<Complex> measuresProperty() {
        return measures;
    }
}