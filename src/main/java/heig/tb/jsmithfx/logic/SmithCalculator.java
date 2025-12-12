package heig.tb.jsmithfx.logic;

import heig.tb.jsmithfx.model.CircuitElement;
import heig.tb.jsmithfx.model.Element.Line;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.DialogUtils;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SmithCalculator {

    private static double SPEED_OF_LIGHT = 299792458.0; // Speed of light in m/s
    public static double getSpeedOfLight() {
        return SPEED_OF_LIGHT;
    }

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

    public static Complex impedanceToGamma(Complex impedance, double z0) {
        Complex numerator = impedance.subReal(z0);
        Complex denominator = impedance.addReal(z0);
        return numerator.dividedBy(denominator);
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

    /**
     * Calculates the value of the component we're plotting on the chart
     *
     * @param gamma          position of the component on the chart
     * @param startImpedance where the last component was
     * @param type           of the component (resistor, capacitor, etc.)
     * @param position       if the component is in series or parallel
     * @param z0             the base impedance
     * @param frequency      of the circuit
     * @return the calculated value of the component
     */
    public static Double calculateComponentValue(Complex gamma,
                                                 Complex startImpedance,
                                                 CircuitElement.ElementType type,
                                                 CircuitElement.ElementPosition position,
                                                 Line.StubType stubType,
                                                 double z0,
                                                 double frequency,
                                                 Optional<Double> z0_line,
                                                 Optional<Double> permittivity) {
        final double EPS = 1e-12;

        if (gamma == null || startImpedance == null) return null;
        if (!(Double.isFinite(z0) && z0 > 0)) return null;
        if (!(Double.isFinite(frequency) && frequency > 0)) return null;

        double omega = 2.0 * Math.PI * frequency;
        Complex one = new Complex(1, 0);

        // Protect against division by (1 - gamma) ~= 0
        Complex denom = one.subtract(gamma);
        if (Math.hypot(denom.real(), denom.imag()) < EPS) return null;

        Complex zNormFinal = one.add(gamma).dividedBy(denom);
        Complex finalImpedance = zNormFinal.multiply(z0);

        double componentValue = 0;

        if (type == CircuitElement.ElementType.LINE) {
            //Get the current values of εr and z_L
            try {
                if (z0_line.isEmpty() || permittivity.isEmpty() || z0_line.get() <= 0 || permittivity.get() < 1.0 ) return null; // Basic validation
            } catch (NumberFormatException e) {
                DialogUtils.showErrorAlert( "Invalid Input", "Please make sure the z0 and εr field are filled", SmithUtilities.getActiveStage());
                return null; // A value hasn't been entered yet
            }

            // β = 2πf/pv, will be used for the calculations in the two branches
            double phase_velocity = getSpeedOfLight() / Math.sqrt(permittivity.get());
            double beta = (2.0 * Math.PI * frequency) / phase_velocity;

            if (beta < EPS) return null; // Avoid division by zero

            if (stubType == Line.StubType.NONE) {
                // Based on the reflection propagation formula: Γ(L) = Γ(0) * e^(-j2βL)
                // Multiplying by e^(-j2βL) rotates Γ by an angle -2βL on the complex plane.
                // So the physical length L can be recovered from the change in angle of Γ.

                // So we transform the equation like this: ∠Γ(L) = ∠Γ(0) +  ∠(e^(-j2βL))
                // And then we rearrange and rewrite it to: ∠Γ(L) - ∠Γ(0) = -2βL
                Complex gamma_start_line = startImpedance.subtract(new Complex(z0_line.get(), 0)).dividedBy(startImpedance.add(new Complex(z0_line.get(), 0)));
                Complex gamma_final_line = finalImpedance.subtract(new Complex(z0_line.get(), 0)).dividedBy(finalImpedance.add(new Complex(z0_line.get(), 0)));

                // Calculate the change in angle. (Δθ)
                double startAngle = gamma_start_line.angle();
                double finalAngle = gamma_final_line.angle();
                double angleChange = finalAngle - startAngle;

                // The rotation for adding a line is always clockwise.
                if (angleChange > 0) {
                    angleChange -= 2.0 * Math.PI;
                }
                // L = ∣Δθ∣ / 2β
                double electricalRotation = Math.abs(angleChange);

                // Avoid division by zero if beta is somehow zero
                if (Math.abs(beta) < EPS) return null;

                // Calculate the final physical length.
                componentValue = electricalRotation / (2.0 * beta);
            } else {
                // Here we use the basic formula of Z_in = Z_0 * (Z_L + jZ_0tan(βL)) / (Z_0 + jZ_Ltan(βL))
                // If it's a short circuit, Z_L becomes 0 so Z_in = jZ_0tan(βL)
                // If it's an open circuit, Z_L becomes infinity, so we simplify the equation to get Z_in = Z_0 / jtan(βL)

                // Since a stub is in parallel, we have to use admittance
                Complex yStart = startImpedance.inverse();
                Complex yFinal = finalImpedance.inverse();

                Complex yDiff = yFinal.subtract(yStart);
                double targetSusceptance = yDiff.imag();
                double y0 = 1.0 / z0_line.get();
                double L;

                if (stubType == Line.StubType.SHORT) {
                    // For a short-circuited stub:
                    // Y_in = 1 / (j Z_0 tan(βL)) = -j Y_0 / tan(βL)
                    // So, tan(βL) = -Y_0 / (j Y_in) => for susceptance B, tan(βL) = -Y_0 / B
                    // L = arctan(-Y_0 / targetSusceptance) / β
                    L = Math.atan(-y0 / targetSusceptance) / beta;
                } else {
                    // For an open-circuited stub:
                    // Y_in = j Y_0 tan(βL)
                    // So, tan(βL) = B / Y_0
                    // L = arctan(targetSusceptance / y0) / β
                    L = Math.atan(targetSusceptance / y0) / beta;
                }
                // Get a positive value
                while (L < 0) {
                    L += Math.PI / beta;
                }
                componentValue = L;
            }
        }

        //Logic for "normal" elements (RLC)
        if (position == CircuitElement.ElementPosition.SERIES) {
            Complex addedImpedance = finalImpedance.subtract(startImpedance);
            double imagZ = addedImpedance.imag();

            if (type == CircuitElement.ElementType.INDUCTOR) {
                componentValue = imagZ / omega; // L = Im(Z) / ω
            } else if (type == CircuitElement.ElementType.CAPACITOR) {
                if (Math.abs(imagZ) < EPS) return null;
                componentValue = -1.0 / (imagZ * omega); // C = -1/(Im(Z)*ω)
            } else if (type == CircuitElement.ElementType.RESISTOR) {
                componentValue = addedImpedance.real(); // R = Re(Z)
            }
        } else { // PARALLEL
            if (Math.hypot(startImpedance.real(), startImpedance.imag()) < EPS ||
                    Math.hypot(finalImpedance.real(), finalImpedance.imag()) < EPS) {
                return null;
            }

            Complex startY = one.dividedBy(startImpedance);
            Complex finalY = one.dividedBy(finalImpedance);
            Complex addedY = finalY.subtract(startY);
            double imagY = addedY.imag();

            if (type == CircuitElement.ElementType.INDUCTOR) {
                if (Math.abs(imagY) < EPS) return null;
                componentValue = -1.0 / (imagY * omega); // L = -1/(Im(Y)*ω)
            } else if (type == CircuitElement.ElementType.CAPACITOR) {
                componentValue = imagY / omega; // C = Im(Y)/ω
            } else if (type == CircuitElement.ElementType.RESISTOR) {
                componentValue = 1.0 / addedY.real(); // R = 1/Re(ΔY)
            }
        }

        if (!Double.isFinite(componentValue) || componentValue <= 0.0) return null;
        return componentValue;
    }

    /**
     * Safely add two impedance in parallel.
     *
     * @param zStart The starting impedance.
     * @param zComp  The component impedance to add in parallel.
     * @return The resulting impedance after adding in parallel.
     */
    public static Complex addParallelImpedance(Complex zStart, Complex zComp) {
        if (zStart.magnitude() < 1e-12 || zComp.magnitude() < 1e-12) {
            return new Complex(0, 0);
        }
        // If start is Open Circuit, result is just the component
        if (zStart.real() > 1e12) return zComp;

        // Z_total = 1 / ( (1/Z_start) + (1/Z_comp) )
        return zStart.inverse().add(zComp.inverse()).inverse();
    }

    /**
     * Generates a list of points representing the path of adding a lossy component on the Smith chart.
     * @param startGamma    The starting reflection coefficient (Gamma).
     * @param element       The circuit element being added.
     * @param z0            The characteristic impedance.
     * @param frequency     The operating frequency.
     * @param points        The number of points to generate along the path.
     * @return A list of Complex numbers representing the path on the Smith chart.
     */
    public static List<Complex> getLossyComponentPath(Complex startGamma, CircuitElement element, double z0, double frequency, int points) {
        List<Complex> path = new ArrayList<>();

        Complex startImpedance = gammaToImpedance(startGamma, z0);
        Complex startAdmittance = startImpedance.inverse(); // Used for parallel

        Complex finalComponentZ = element.getImpedance(frequency);

        for (int i = 0; i <= points; i++) {
            double fraction = (double) i / points;
            Complex stepTotalZ;

            if (element.getPosition() == CircuitElement.ElementPosition.SERIES) {
                // Z_step = Z_start + (Z_final_component * fraction)
                stepTotalZ = startImpedance.add(finalComponentZ.multiply(fraction));
            } else {
                // PARALLEL
                // Y_final_component = 1 / Z_final_component
                Complex finalComponentY = finalComponentZ.inverse();

                // Y_step = Y_start + (Y_final_component * fraction)
                Complex stepTotalY = startAdmittance.add(finalComponentY.multiply(fraction));

                // Convert back to Z for gamma calculation
                stepTotalZ = stepTotalY.inverse();
            }

            // Convert to Gamma and add to path
            path.add(impedanceToGamma(stepTotalZ, z0));
        }

        return path;
    }

}
