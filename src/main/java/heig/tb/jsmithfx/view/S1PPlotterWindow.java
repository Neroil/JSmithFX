package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.SmithChartViewModel;
import heig.tb.jsmithfx.model.DataPoint;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class S1PPlotterWindow {

    // Singleton stuff
    private static volatile S1PPlotterWindow instance;

    public static S1PPlotterWindow getInstance() {
        if (instance == null) {
            synchronized (S1PPlotterWindow.class) {
                if (instance == null) {
                    instance = new S1PPlotterWindow();
                }
            }
        }
        return instance;
    }

    private static final int MAX_RENDER_POINTS = 1500;
    private static final double MIN_DB_THRESHOLD = -100.0;
    private static final double MAGNITUDE_ZERO_TOLERANCE = 1e-12;

    private final Stage stage;
    private final XYChart.Series<Number, Number> baseSeries; // Full S1P PLOT
    private final XYChart.Series<Number, Number> f1Series;   // Filter 1 Highlight
    private final XYChart.Series<Number, Number> f2Series;   // Filter 2 Highlight
    private final XYChart.Series<Number, Number> f3Series;   // Filter 3 Highlight
    private final NumberAxis xAxis;
    private final ListChangeListener<DataPoint> s1pListener;
    private final ChangeListener<Boolean> redrawListener;
    private boolean isUpdatePending = false;

    private S1PPlotterWindow() {
        stage = new Stage();
        stage.setTitle("S1P LogMag");

        xAxis = new NumberAxis();
        xAxis.setLabel("Frequency (Hz)");
        xAxis.setAutoRanging(true);
        xAxis.setAnimated(false);

        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Magnitude (dB)");
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("S1P Log Magnitude Plot");
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);

        // Initialize Series
        baseSeries = new XYChart.Series<>();
        baseSeries.setName("Raw Data");

        f1Series = new XYChart.Series<>();
        f1Series.setName("Filter 1");

        f2Series = new XYChart.Series<>();
        f2Series.setName("Filter 2");

        f3Series = new XYChart.Series<>();
        f3Series.setName("Filter 3");

        // Add them to chart
        lineChart.getData().add(baseSeries);
        lineChart.getData().add(f1Series);
        lineChart.getData().add(f2Series);
        lineChart.getData().add(f3Series);

        // Properly apply the colors to each series
        styleSeries(baseSeries, "rgba(30, 144, 255, 0.3)", 1.0);
        styleSeries(f1Series, "indianred", 2.0);
        styleSeries(f2Series, "darkorange", 2.0);
        styleSeries(f3Series, "forestgreen", 2.0);

        // Scene Setup
        StackPane root = new StackPane(lineChart);
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);

        SmithChartViewModel viewModel = SmithChartViewModel.getInstance();

        // Listener for S-Parameter data changes
        this.s1pListener = _ -> requestUpdate();
        viewModel.transformedS1PPointsProperty().addListener(s1pListener);

        // Listener for Redraw events
        this.redrawListener = (obs, oldVal, newVal) -> {
            if (!newVal && oldVal) requestUpdate();
        };
        viewModel.isRedrawing.addListener(redrawListener);

        // Trigger initial paint
        requestUpdate();

        // Cleanup on close
        stage.setOnCloseRequest(_ -> cleanup());
    }

    /**
     * Helper to enforce style on Series nodes.
     */
    private void styleSeries(XYChart.Series<Number, Number> series, String colorWebString, double width) {
        series.nodeProperty().addListener((_, _, node) -> {
            if (node != null) {
                node.setStyle("-fx-stroke: " + colorWebString + "; -fx-stroke-width: " + width + "px;");
            }
        });

        // Apply immediately if node exists
        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: " + colorWebString + "; -fx-stroke-width: " + width + "px;");
        }
    }

    private void cleanup() {
        SmithChartViewModel.getInstance().transformedS1PPointsProperty().removeListener(s1pListener);
        SmithChartViewModel.getInstance().isRedrawing.removeListener(redrawListener);
        clearAllSeries();

        // Clear singleton instance
        synchronized (S1PPlotterWindow.class) {
            instance = null;
        }

        System.out.println("S1P Window cleaned up.");
    }

    /**
     * Debounce logic to prevent unnecessary multiple updates.
     */
    private void requestUpdate() {
        if (isUpdatePending) return;
        isUpdatePending = true;
        Platform.runLater(this::performUpdate);
    }

    private void performUpdate() {
        try {
            SmithChartViewModel vm = SmithChartViewModel.getInstance();
            var rawList = vm.transformedS1PPointsProperty().get();

            if (rawList == null || rawList.isEmpty()) {
                clearAllSeries();
                return;
            }

            // Update the xAxis label to properly display frequency unit
            var properFreqUnit = vm.getProperFrequencyUnitS1P();
            xAxis.setLabel("Frequency (" + properFreqUnit + ")");

            // Prepare temporary lists
            List<XYChart.Data<Number, Number>> baseData = new ArrayList<>();
            List<XYChart.Data<Number, Number>> f1Data = new ArrayList<>();
            List<XYChart.Data<Number, Number>> f2Data = new ArrayList<>();
            List<XYChart.Data<Number, Number>> f3Data = new ArrayList<>();

            // Downsampling Logic to limit rendering points
            List<DataPoint> listCopy = new ArrayList<>(rawList);
            int totalPoints = listCopy.size();
            int step = Math.max(1, totalPoints / MAX_RENDER_POINTS);

            // Process Points
            for (int i = 0; i < totalPoints; i += step) {
                processPoint(listCopy.get(i), vm, baseData, f1Data, f2Data, f3Data, properFreqUnit.getFactor());
            }

            // Ensure the very last point is drawn to complete the line
            if ((totalPoints - 1) % step != 0) {
                processPoint(listCopy.getLast(), vm, baseData, f1Data, f2Data, f3Data, properFreqUnit.getFactor());
            }

            // Batch Update Chart Series
            baseSeries.getData().setAll(baseData);

            if (vm.filter1EnabledProperty().get() && !f1Data.isEmpty()) f1Series.getData().setAll(f1Data);
            else f1Series.getData().clear();

            if (vm.filter2EnabledProperty().get() && !f2Data.isEmpty()) f2Series.getData().setAll(f2Data);
            else f2Series.getData().clear();

            if (vm.filter3EnabledProperty().get() && !f3Data.isEmpty()) f3Series.getData().setAll(f3Data);
            else f3Series.getData().clear();

        } finally {
            isUpdatePending = false;
        }
    }

    private void processPoint(DataPoint dp,
                              SmithChartViewModel vm,
                              List<XYChart.Data<Number, Number>> baseData,
                              List<XYChart.Data<Number, Number>> f1Data,
                              List<XYChart.Data<Number, Number>> f2Data,
                              List<XYChart.Data<Number, Number>> f3Data,
                              double freqFactor) {

        if (dp.gammaProperty().get() == null) return;

        double freq = dp.getFrequency() / freqFactor;
        double mag = dp.gammaProperty().get().magnitude();
        double db = (mag < MAGNITUDE_ZERO_TOLERANCE) ? MIN_DB_THRESHOLD : 20 * Math.log10(mag);


        // Always add to base series
        baseData.add(new XYChart.Data<>(freq, db));

        // Add to specific filter series
        if (vm.filter1EnabledProperty().get() && vm.isFrequencyInRangeF1(dp.getFrequency())) {
            f1Data.add(new XYChart.Data<>(freq, db));
        }
        if (vm.filter2EnabledProperty().get() && vm.isFrequencyInRangeF2(dp.getFrequency())) {
            f2Data.add(new XYChart.Data<>(freq, db));
        }
        if (vm.filter3EnabledProperty().get() && vm.isFrequencyInRangeF3(dp.getFrequency())) {
            f3Data.add(new XYChart.Data<>(freq, db));
        }
    }

    private void clearAllSeries() {
        baseSeries.getData().clear();
        f1Series.getData().clear();
        f2Series.getData().clear();
        f3Series.getData().clear();
    }

    public void show() {
        if (stage.isIconified()) stage.setIconified(false);
        stage.show();
        stage.toFront();
    }
}