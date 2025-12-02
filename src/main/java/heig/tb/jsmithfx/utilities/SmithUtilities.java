package heig.tb.jsmithfx.utilities;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.model.Element.TypicalUnit.ElectronicUnit;
import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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

        if (element.getType() == CircuitElement.ElementType.LINE) {
            if (element.getPosition() == CircuitElement.ElementPosition.SERIES) {
                Line line = (Line) element;
                double zL = line.getCharacteristicImpedance();
                // Simple case if the impedance of the line matches the impedance of the current system
                if (Math.abs(zL - z0) < 1e-9) {
                    Complex gamma = startImpedance.subReal(z0).dividedBy(startImpedance.addReal(z0));
                    center = new Complex(0, 0);
                    radius = gamma.magnitude();
                }
                // If mismatch
                else {
                    // Find the reflexion coef of the impedance to the impedance of the line
                    Complex gammaRelToLine = startImpedance.subReal(zL).dividedBy(startImpedance.addReal(zL));
                    double rhoLine = gammaRelToLine.magnitude();

                    // Find the two extremities of the gammaRelToLine circle to the X axis using:
                    // Z = zL * (1+Gamma)/(1-Gamma)
                    // Using the magnitude of the Gamma makes it possible to find the pure real points
                    double rMin = zL * (1.0 - rhoLine) / (1.0 + rhoLine);
                    double rMax = zL * (1.0 + rhoLine) / (1.0 - rhoLine);

                    // Convert those two point to the z0 plan
                    double gammaSysMin = (rMin - z0) / (rMin + z0);
                    double gammaSysMax = (rMax - z0) / (rMax + z0);

                    // Find the center of the circle using those two points
                    double centerX = (gammaSysMin + gammaSysMax) / 2.0;

                    center = new Complex(centerX, 0);
                    radius = Math.abs(gammaSysMax - gammaSysMin) / 2.0;
                }
            } else { //Parallel so it's an open stub or a short stub, the center or of the arc does not change for either stubs

                //We know that the circle is tangent the point -1 + j0 and to startImpedance
                //The center will be exactly at the middle of those two points
                Complex gamma = startImpedance.subReal(z0).dividedBy(startImpedance.addReal(z0));
                double denom = 2 * (1 + gamma.real());
                double nom = Math.pow(gamma.magnitude(),2) - 1;
                double centerX = nom/denom;

                center = new Complex(centerX, 0);
                radius = Math.abs(centerX - (-1.0));
            }
        }
        else if (element.getType() == CircuitElement.ElementType.RESISTOR) {
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

        if (type == CircuitElement.ElementType.LINE){
            expectedDirection = 1;
            expectedDirection *= ((Line)element).getStubType() == Line.StubType.SHORT ? 1 : -1;

            return expectedDirection;
        }

        expectedDirection = (type == CircuitElement.ElementType.CAPACITOR || type == CircuitElement.ElementType.RESISTOR) ? 1 : -1;
        expectedDirection *= (position ==  CircuitElement.ElementPosition.SERIES) ? 1 : -1;

        // Correct the direction if the last gamma's imag was negative for the resistor
        if(type == CircuitElement.ElementType.RESISTOR) expectedDirection *= (previousGamma.imag() < 0) ? -1 : 1;
        return expectedDirection;
    }

    public static double parseValueWithUnit(String text, FrequencyUnit[] values) {
        for (FrequencyUnit unit : values) {
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
}
