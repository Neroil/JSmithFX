package heig.tb.jsmithfx;

import javafx.beans.property.*;

public class SmithChartViewModel {

    public final DoubleProperty zo = new SimpleDoubleProperty(50.0);
    public final SimpleListProperty<Complex> measures = new SimpleListProperty<>();
    public final ObjectProperty<Complex> loadImpedance = new SimpleObjectProperty<>();


}

