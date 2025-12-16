package heig.tb.jsmithfx.utilities;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an immutable complex number with real and imaginary parts.
 * Uses a Java record for concise data structure and automatic implementations
 * of equals(), hashCode(), and toString().
 *
 * @param real The real part of the complex number.
 * @param imag The imaginary part of the complex number.
 */
public record Complex(double real, double imag) {

    /**
     * Provides a string representation of the complex number in the form "a +/- jb Ω".
     * Values are formatted to two decimal places.
     *
     * @return The formatted string representing the impedance.
     */
    @NotNull
    @Override
    public String toString() {
        // Use Math.abs(imag) to format the magnitude, and determine the sign (+ or -) from imag < 0
        return String.format("%.2f %s j%.2f Ω", real, imag < 0 ? "-" : "+", Math.abs(imag));
    }

    /**
     * Displays the complex value as an admittance in millisiemens (mS).
     * The real and imaginary parts are multiplied by 1000.
     *
     * @return The string displaying the admittance value.
     */
    public String toStringmS() {
        return String.format("%.2f %s j%.2f mS", real * 1000, imag < 0 ? "-" : "+", Math.abs(imag) * 1000);
    }

    /**
     * Performs complex division (z1 / z2).
     *
     * @param z The complex number (z2) that divides the current one (z1).
     * @return A new complex number representing the quotient.
     * @throws ArithmeticException if the divisor z has a magnitude of zero.
     */
    public Complex dividedBy(Complex z) {
        if (z.real == 0.0 && z.imag == 0.0)
            throw new ArithmeticException("Division by zero is not allowed!");

        // Formula: (a+jb) / (c+jd) = (ac+bd)/(c^2+d^2) + j(bc-ad)/(c^2+d^2)
        double c2d2 = Math.pow(z.real, 2) + Math.pow(z.imag, 2);
        double newReal = (real * z.real + imag * z.imag) / c2d2;
        double newImag = (imag * z.real - real * z.imag) / c2d2;

        return new Complex(newReal, newImag);
    }

    /**
     * Divides the complex number by a real scalar value.
     *
     * @param d The real scalar divisor.
     * @return A new complex number representing the quotient.
     * @throws ArithmeticException if the divisor d is zero.
     */
    public Complex dividedBy(double d) {
        if (d == 0.0)
            throw new ArithmeticException("Division by zero is not allowed!");
        return new Complex(real/d, imag/d);
    }

    /**
     * Adds a real scalar value to the complex number's real part.
     *
     * @param r The real value to add.
     * @return A new complex number.
     */
    public Complex addReal(double r) {
        return new Complex(real + r, imag);
    }

    /**
     * Adds a real scalar value to the complex number's imaginary part.
     *
     * @param r The real value to add to the imaginary part.
     * @return A new complex number.
     */
    public Complex addImag(double r) {
        return new Complex(real, imag + r);
    }

    /**
     * Subtracts a real scalar value from the complex number's real part.
     *
     * @param r The real value to subtract.
     * @return A new complex number.
     */
    public Complex subReal(double r) {
        return new Complex(real - r, imag);
    }

    /**
     * Subtracts a real scalar value from the complex number's imaginary part.
     *
     * @param r The real value to subtract from the imaginary part.
     * @return A new complex number.
     */
    public Complex subImag(double r) {
        return new Complex(real, imag - r);
    }

    /**
     * Adds another complex number to the current complex number (z1 + z2).
     *
     * @param z The complex number to add.
     * @return A new complex number representing the sum.
     */
    public Complex add(Complex z) {
        return new Complex(real + z.real, imag + z.imag);
    }

    /**
     * Calculates the multiplicative inverse (reciprocal) of the complex number (1/z).
     *
     * @return A new complex number representing the reciprocal.
     * @throws ArithmeticException if the magnitude of the complex number is zero.
     */
    public Complex inverse() {
        if(real == 0.0 && imag == 0.0)
            throw new ArithmeticException("Division by zero is not allowed!");

        // Formula: 1/(a+jb) = (a-jb)/(a^2+b^2)
        double denominator = real * real + imag * imag;
        return new Complex(real / denominator, (-imag) / denominator);
    }

    /**
     * Calculates the magnitude (absolute value) of the complex number (|z|).
     *
     * @return The magnitude as a double.
     */
    public double magnitude() {
        return Math.sqrt(real * real + imag * imag);
    }

    /**
     * Subtracts another complex number from the current complex number (z1 - z2).
     *
     * @param z The complex number to subtract.
     * @return A new complex number representing the difference.
     */
    public Complex subtract(Complex z) {
        return new Complex(real - z.real, imag - z.imag);
    }

    /**
     * Multiplies the current complex number by another (z1 * z2).
     *
     * @param z The complex number to multiply by.
     * @return A new complex number representing the product.
     */
    public Complex multiply(Complex z) {
        // Formula: (a+jb)(c+jd) = (ac-bd) + j(ad+bc)
        return new Complex(real * z.real - imag * z.imag, real * z.imag + imag * z.real);
    }

    /**
     * Multiplies the complex number by a real scalar value.
     *
     * @param d The real scalar value.
     * @return A new complex number representing the product.
     */
    public Complex multiply(double d) {
        return new Complex(real * d, imag * d);
    }

    /**
     * Calculates the phase angle (argument) of the complex number in radians.
     * The result is in the range $(-\pi, \pi]$.
     *
     * @return The angle in radians.
     */
    public double angle() {
        return Math.atan2(imag, real);
    }

    /**
     * Calculates the normalized complex number (z/|z|).
     *
     * @return A new complex number with a magnitude of 1.
     * @throws ArithmeticException if the magnitude of the complex number is zero.
     */
    public Complex normalize() {
        double abs = magnitude();
        if (abs == 0) {
            throw new ArithmeticException("Cannot normalize a zero-magnitude complex number.");
        }
        return new Complex(real/abs, imag/abs);
    }

    /**
     * Calculates the complex hyperbolic tangent of the complex number (tanh(z)).
     * Formula: tanh(x+jy) = (sinh(2x) + j*sin(2y)) / (cosh(2x) + cos(2y))
     *
     * @return A new complex number representing the hyperbolic tangent.
     */
    public Complex tanh() {
        double x = this.real;
        double y = this.imag;

        double sinh2x = Math.sinh(2 * x);
        double cosh2x = Math.cosh(2 * x);
        double sin2y = Math.sin(2 * y);
        double cos2y = Math.cos(2 * y);

        // Numerator: sinh(2x) + j*sin(2y)
        Complex numerator = new Complex(sinh2x, sin2y);

        // Denominator: cosh(2x) + cos(2y) (Real scalar)
        double denominator = cosh2x + cos2y;

        // Check for division by zero (occurs if cosh(2x) = -cos(2y))
        if (Math.abs(denominator) < 1e-9) {
            throw new ArithmeticException("Hyperbolic tangent denominator is zero for z = " + this);
        }

        return numerator.dividedBy(denominator);
    }
}