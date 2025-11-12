package heig.tb.jsmithfx.utilities;

import org.jetbrains.annotations.NotNull;

public record Complex(double real, double imag) {

    @NotNull
    @Override
    public String toString() {
        return real + " + j" + imag + " Ω";
    }

    /**
     * Divide the current complex by another
     * @param z the complex number that divide the current one
     * @return a new complex number
     */
    public Complex dividedBy(Complex z) {
        if (z.real == 0.0 && z.imag == 0.0)
            throw new ArithmeticException("Division by zero is not allowed!");

        double c2d2 = Math.pow(z.real, 2) + Math.pow(z.imag, 2);
        double newReal = (real * z.real + imag * z.imag) / c2d2;
        double newImag = (imag * z.real - real * z.imag) / c2d2;

        return new Complex(newReal, newImag);
    }

    public Complex addReal(double r) {
        return new Complex(real + r, imag);
    }

    public Complex addImag(double r) {
        return new Complex(imag, real + r);
    }
}
