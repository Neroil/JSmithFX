package heig.tb.jsmithfx.logic;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.utilities.Complex;
import javafx.util.Pair;

public class SmithCalculator {

    /**
     * Converts the reflection coefficient Gamma to its complex impedance
     * @param gamma the reflection coefficient
     * @param z0 the characteristic impedance
     * @return the complex impedance
     */
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


    /**
     * Determines the expected direction of movement on the Smith chart depending on the type and position of the circuit element.
     * @param element the circuit element being added
     * @param previousGamma the previous reflection coefficient before adding the element
     * @return 1 for counter-clockwise, -1 for clockwise
     */
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
}
