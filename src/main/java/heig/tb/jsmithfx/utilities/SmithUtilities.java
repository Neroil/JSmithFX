package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import javafx.util.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SmithUtilities {

    /**
     * Determines the best unit and calculates the formatted display value for a given actual value.
     *
     * @param actualValue The actual value to format.
     * @param values      The array of unit enums to choose from.
     * @return A Pair containing the selected unit and the formatted display value.
     */
    public static Pair<ElectronicUnit, String> getBestUnitAndFormattedValue(double actualValue,ElectronicUnit[] values) {
        //Safety
        if (values == null || values.length == 0) {
            return new Pair<>(null, "0");
        }

        // Handle zero or negative values
        if (actualValue <= 0) {
            // Return with the base unit
            return new Pair<>(values[0], "0");
        }

        // Sort values from biggest to smallest
        List<ElectronicUnit> sortedUnits = Arrays.stream(values)
                .sorted(Comparator.comparingDouble(ElectronicUnit::getFactor).reversed())
                .toList();

        // Find the first value that makes it readable (>=1)
        for (ElectronicUnit unit : sortedUnits) {
            double displayValue = actualValue / unit.getFactor();
            if (displayValue >= 1.0) { //Why the order is descending
                String formattedValue = String.format("%.3g", displayValue);
                return new Pair<>(unit, formattedValue);
            }
        }

        // In case we don't have small enough values, get the smallest one
        ElectronicUnit smallestUnit = sortedUnits.getLast();
        double displayValue = actualValue / smallestUnit.getFactor();
        String formattedValue = String.format("%.3g", displayValue);

        return new Pair<>(smallestUnit, formattedValue);
    }

    public static Complex gammaToImpedance(Complex gamma, double z0) {
        Complex one = new Complex(1, 0);
        Complex numerator = one.add(gamma);
        Complex denominator = one.subtract(gamma);
        return numerator.dividedBy(denominator).multiply(z0);
    }

    /**
     * Calculates the center and radius (in the gamma plane) of the arc
     * corresponding to adding a circuit element.
     *
     * @param startImpedance The impedance before adding the element.
     * @param element        The circuit element being added.
     * @param z0             The characteristic impedance.
     * @return A Pair containing the arc's center (Complex) and radius (Double).
     */
    public static Pair<Complex, Double> getArcParameters(Complex startImpedance, CircuitElement element, double z0) {
        Complex center;
        double radius;

        Complex zNorm = startImpedance.dividedBy(z0);

        if (element.getType() == CircuitElement.ElementType.RESISTOR) {
            // Constant Reactance (Series) or Susceptance (Parallel) Circles
            if (element.getPosition() == CircuitElement.ElementPosition.SERIES) {
                double x = zNorm.imag();
                center = new Complex(1.0, 1.0 / x);
                radius = Math.abs(1.0 / x);
            } else { // PARALLEL
                Complex yNorm = new Complex(1.0, 0).dividedBy(zNorm);
                double b = yNorm.imag();
                center = new Complex(-1.0, -1.0 / b);
                radius = Math.abs(1.0 / b);
            }
        } else {
            // Constant Resistance (Series) or Conductance (Parallel) Circles
            if (element.getPosition() == CircuitElement.ElementPosition.SERIES) {
                double r = zNorm.real();
                center = new Complex(r / (r + 1.0), 0);
                radius = 1.0 / (r + 1.0);
            } else { // PARALLEL
                Complex yNorm = new Complex(1.0, 0).dividedBy(zNorm);
                double g = yNorm.real();
                center = new Complex(-g / (g + 1.0), 0);
                radius = 1.0 / (g + 1.0);
            }
        }
        return new Pair<>(center, radius);
    }


    public static int getExpectedDirection(CircuitElement element, Complex previousGamma) {
        int expectedDirection;
        CircuitElement.ElementType type = element.getType();
        CircuitElement.ElementPosition position = element.getPosition();

        expectedDirection = (type == CircuitElement.ElementType.CAPACITOR || type == CircuitElement.ElementType.RESISTOR) ? 1 : -1;
        expectedDirection *= (position ==  CircuitElement.ElementPosition.SERIES) ? 1 : -1;

        // Correct the direction if the last gamma's imag was negative for the resistor
        if(type == CircuitElement.ElementType.RESISTOR) expectedDirection *= (previousGamma.imag() < 0) ? -1 : 1;
        return expectedDirection;
    }
}
