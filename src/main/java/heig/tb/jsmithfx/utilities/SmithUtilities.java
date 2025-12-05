package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import javafx.collections.ObservableList;
import javafx.stage.Window;
import javafx.util.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SmithUtilities {

    public static String displayBestUnitAndFormattedValue(double actualValue, ElectronicUnit[] values) {
        return displayBestUnitAndFormattedValue(actualValue, values, "%.3f");
    }

    public static String displayBestUnitAndFormattedValue(double actualValue, ElectronicUnit[] values, String format) {
        Pair<ElectronicUnit, String> result = getBestUnitAndFormattedValue(actualValue, values, format);
        ElectronicUnit unit = result.getKey();
        String formattedValue = result.getValue();
        return formattedValue + " " + (unit != null ? unit.toString() : "");
    }

    public static Pair<ElectronicUnit, String> getBestUnitAndFormattedValue(double actualValue,ElectronicUnit[] values) {
        return getBestUnitAndFormattedValue(actualValue, values, "%.3f");
    }

    /**
     * Determines the best unit and calculates the formatted display value for a given actual value.
     *
     * @param actualValue The actual value to format.
     * @param values      The array of unit enums to choose from.
     * @param format      The string format to use for the display value.
     * @return A Pair containing the selected unit and the formatted display value.
     */
    public static Pair<ElectronicUnit, String> getBestUnitAndFormattedValue(double actualValue,ElectronicUnit[] values, String format) {
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
                String formattedValue = String.format(format, displayValue);
                return new Pair<>(unit, formattedValue);
            }
        }

        // In case we don't have small enough values, get the smallest one
        ElectronicUnit smallestUnit = sortedUnits.getLast();
        double displayValue = actualValue / smallestUnit.getFactor();
        String formattedValue = String.format(format, displayValue);

        return new Pair<>(smallestUnit, formattedValue);
    }

    public static double parseValueWithUnit(String text, ElectronicUnit[] values) {
        for (ElectronicUnit unit : values) {
            String[] splitText = text.split(" "); // SplitText[1] should be the unit
            if (splitText.length == 2 && splitText[1].equalsIgnoreCase(unit.toString())) {
                String numberPart = splitText[0];
                double numericValue = Double.parseDouble(numberPart);
                return numericValue * unit.getFactor();
            }
        }
        // If no unit is found, try to parse directly as a double (will throw exception if invalid)
        return Double.parseDouble(text);
    }

    /**
     * Get the frequency range (min and max) from a list of DataPoints
     * @param dp the list of DataPoints
     * @return a Pair containing the min (key) and max (value) frequency
     */
    public static Pair<Double, Double> getFrequencyRangeFromDataPoints(ObservableList<DataPoint> dp) {
        return dp.stream()
                .collect(Collectors.teeing(
                        Collectors.minBy(Comparator.comparingDouble(DataPoint::getFrequency)),
                        Collectors.maxBy(Comparator.comparingDouble(DataPoint::getFrequency)),
                        (minOpt, maxOpt) -> new Pair<>(
                                minOpt.map(DataPoint::getFrequency).orElse(0.0),
                                maxOpt.map(DataPoint::getFrequency).orElse(0.0)
                        )
                ));
    }

    public static Window getActiveStage() {
        return javafx.stage.Stage.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
    }

    public static Optional<Double> parseOptionalDouble(String text) {
        try {
            return Optional.of(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
