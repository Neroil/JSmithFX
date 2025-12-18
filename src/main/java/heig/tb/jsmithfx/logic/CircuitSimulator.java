package heig.tb.jsmithfx.logic;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.utilities.Complex;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless service responsible for simulating the circuit behavior.
 * Transforms Input Impedance + Elements -> List of DataPoints.
 */
public class CircuitSimulator {

    /**
     * Calculate the impedance chain through the circuit elements.
     * @param loadImpedance The load impedance to start from
     * @param frequency the frequency of operation
     * @param z0 the characteristic impedance
     * @param elements the list of circuit elements
     * @return the list of DataPoints representing the impedance at each stage
     */
    public List<DataPoint> calculateChain(Complex loadImpedance, double frequency, double z0, List<CircuitElement> elements) {
        List<DataPoint> dataPoints = new ArrayList<>();
        if (loadImpedance == null) return dataPoints;

        Complex currentImpedance = loadImpedance;

        // Add Load Point
        addDataPoint(dataPoints, frequency, "LD", currentImpedance, z0);

        // Process Elements
        int index = 1;
        for (CircuitElement element : elements) {
            currentImpedance = propagateOne(currentImpedance, element, frequency);
            addDataPoint(dataPoints, frequency, "DP" + index++, currentImpedance, z0);
        }
        return dataPoints;
    }

    public List<DataPoint> calculateTransformedS1P(List<DataPoint> originalS1P, List<CircuitElement> elements, double z0) {
        List<DataPoint> transformed = new ArrayList<>();

        for (DataPoint point : originalS1P) {
            double freq = point.getFrequency();
            Complex currentZ = point.getImpedance();

            // Apply all elements to this single frequency point
            for (CircuitElement element : elements) {
                currentZ = propagateOne(currentZ, element, freq);
            }

            // Create new point with transformed impedance
            Complex gamma = SmithCalculator.impedanceToGamma(currentZ, z0);
            transformed.add(new DataPoint(freq, point.getLabel(), currentZ, gamma, SmithCalculator.calculateVswr(gamma), SmithCalculator.calculateReturnLoss(gamma)));
        }
        return transformed;
    }

    // =============================================================================================
    // Frequency Sweep Logic
    // =============================================================================================

    public List<DataPoint> performSweep(Complex startLoad, List<Double> frequencies, List<CircuitElement> elements, double z0) {
        List<DataPoint> sweepPoints = new ArrayList<>();

        for (Double freq : frequencies) {
            Complex currentZ = startLoad;

            // Propagate through all elements for this frequency
            for (CircuitElement element : elements) {
                currentZ = propagateOne(currentZ, element, freq);
            }

            Complex gamma = SmithCalculator.impedanceToGamma(currentZ, z0);
            sweepPoints.add(new DataPoint(freq, "SWEEP", currentZ, gamma, SmithCalculator.calculateVswr(gamma), SmithCalculator.calculateReturnLoss(gamma)));
        }
        return sweepPoints;
    }

    // =============================================================================================
    // Helpers
    // =============================================================================================

    /**
     * Propagate the current impedance through one circuit element.
     * @param currentZ the current impedance
     * @param element the circuit element to propagate through
     * @param freq the frequency of operation
     * @return the new impedance after the element
     */
    private Complex propagateOne(Complex currentZ, CircuitElement element, double freq) {
        if (element.getType() == CircuitElement.ElementType.LINE) {
            return ((Line) element).calculateImpedance(currentZ, freq);
        } else {
            Complex elementZ = element.getImpedance(freq);
            return calculateNextImpedance(currentZ, elementZ, element.getElementPosition());
        }
    }

    /**
     * Add a data point to the list.
     * @param list the list to add to
     * @param freq the frequency
     * @param label the label
     * @param z the impedance
     * @param z0 the characteristic impedance
     */
    private void addDataPoint(List<DataPoint> list, double freq, String label, Complex z, double z0) {
        Complex gamma = SmithCalculator.impedanceToGamma(z, z0);
        double vswr = SmithCalculator.calculateVswr(gamma);
        double rl = SmithCalculator.calculateReturnLoss(gamma);
        list.add(new DataPoint(freq, label, z, gamma, vswr, rl));
    }

    /**
     * Calculate the next impedance based on the previous impedance, the element impedance, and the element position.
     * @param prevZ the previous impedance
     * @param elemZ the element's impedance
     * @param pos the element position (series or parallel)
     * @return the new impedance
     */
    private Complex calculateNextImpedance(Complex prevZ, Complex elemZ, CircuitElement.ElementPosition pos) {
        if (pos == CircuitElement.ElementPosition.SERIES) {
            return prevZ.add(elemZ);
        } else {
            return SmithCalculator.addParallelImpedance(prevZ, elemZ);
        }
    }

}