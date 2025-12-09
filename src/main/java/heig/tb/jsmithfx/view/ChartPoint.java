package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.SmithUtilities;

/**
 * @param frequency Optional, only if it's an S1P point
 * @param label     E.g., "DP1" or "1.5 GHz"
 */
public record ChartPoint(double screenX, double screenY, Complex gamma, double frequency, String label, double pointSize, boolean isS1P) {

    public String getTooltipText() {
        var freqDisplay = SmithUtilities.getBestUnitAndFormattedValue(frequency, FrequencyUnit.values());
        double re = gamma.real();
        double im = gamma.imag();
        double mag = Math.sqrt(re * re + im * im);
        double ang = Math.toDegrees(Math.atan2(im, re));

        String line1 = (label != null && !label.isEmpty()) ? label : "Point";
        String line2 = String.format("%s %s", freqDisplay.getValue(), freqDisplay.getKey());
        String line3 = String.format("Γ = %.3f, ∠ %.1f°", mag, ang);

        return line1 + "\n" + line2 + "\n" + line3;
    }

    // Hit detection using squared distance for efficiency. pointSize is stored in actual screen pixels.
    public boolean isHit(double mouseX, double mouseY, double padding) {
        double dx = mouseX - screenX;
        double dy = mouseY - screenY;
        double radius = (pointSize / 2.0) + padding; // effective radius (point visual radius + padding)
        return (dx * dx + dy * dy) <= radius * radius;
    }
}