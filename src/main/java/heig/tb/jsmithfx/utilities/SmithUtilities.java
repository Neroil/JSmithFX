package heig.tb.jsmithfx.utilities;

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
}
