package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.SmithUtilities;

/**
 * @param frequency Optional, only if it's an S1P point
 * @param label     E.g., "DP1" or "1.5 GHz"
 */
public record ChartPoint(double screenX, double screenY, Complex gamma, double frequency, String label) {

    public String getTooltipText() {
        var toDisplay = SmithUtilities.getBestUnitAndFormattedValue(frequency, FrequencyUnit.values());
        return toDisplay.getValue() + " " + toDisplay.getKey().toString();
    }

    // Check if mouse is near this point (hit detection)
    public boolean isHit(double mouseX, double mouseY, double threshold) {
        double dist = Math.sqrt(Math.pow(mouseX - screenX, 2) + Math.pow(mouseY - screenY, 2));
        return dist <= threshold;
    }
}