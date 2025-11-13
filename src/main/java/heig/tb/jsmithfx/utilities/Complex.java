package heig.tb.jsmithfx.utilities;

import org.jetbrains.annotations.NotNull;

public record Complex(double real, double imag) {

    @NotNull
    @Override
    public String toString() {
        return String.format("%.2f %s j%.2f Ω", real, imag < 0 ? "-" : "+", Math.abs(imag));
    }

    /**
     * Displays the complex value as an admittance
     * @return the string displaying the admittance value
     */
    public String toStringmS() {
        return String.format("%.2f %s j%.2f mS", real * 1000, imag < 0 ? "-" : "+", Math.abs(imag) * 1000);
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

    public Complex add(Complex z) {
        return new Complex(real + z.real, imag + z.imag);
    }

    public Complex inverse() {
        double denominator = real * real + imag * imag;
        return new Complex(real / denominator, (-imag) / denominator);
    }

    public double abs() {
        return Math.sqrt(real * real + imag * imag);
    }

    public Complex subtract(Complex z) {
        return new Complex(real - z.real, imag - z.imag);
    }

    public Complex multiply(Complex z) {
        return new Complex(real * z.real - imag * z.imag, real * z.imag + imag * z.real);
    }

    public double angle() {
        return Math.atan2(imag, real);
    }
}
