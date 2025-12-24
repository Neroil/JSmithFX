package heig.tb.jsmithfx.view;

import heig.tb.jsmithfx.SmithChartViewModel;
import heig.tb.jsmithfx.logic.SmithCalculator;
import heig.tb.jsmithfx.model.DataPoint;
import heig.tb.jsmithfx.model.Element.TypicalUnit.FrequencyUnit;
import heig.tb.jsmithfx.utilities.Complex;
import heig.tb.jsmithfx.utilities.SmithUtilities;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class S1PPlotterWindow {

    private static S1PPlotterWindow instance;

    public static S1PPlotterWindow getInstance() {
        if (instance == null) {
            instance = new S1PPlotterWindow();
        }
        return instance;
    }

    private static final int MAX_RENDER_POINTS = 1500;

    // To be drawn
    private final Stage stage;
    private final XYChart.Series<Number, Number> baseSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> f1Series = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> f2Series = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> f3Series = new XYChart.Series<>();
    private final Circle loadPoint = new Circle(4, Color.YELLOW);

    // Chart components
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final LineChart<Number, Number> lineChart;

    // Listeners
    private final ListChangeListener<DataPoint> s1pListener;
    private final ChangeListener<Boolean> redrawListener;
    private boolean isUpdatePending = false;

    // Cursor UI elements
    private final Line vLine = new Line();
    private final Line hLine = new Line();
    private final Circle highlightPoint = new Circle(4, Color.RED);
    private final Label infoBox = new Label();

    /**
     * Private constructor for singleton pattern.
     * Initializes the chart window and its components.
     */
    private S1PPlotterWindow() {
        stage = new Stage();
        stage.setTitle("S1P LogMag");

        xAxis = new NumberAxis();
        xAxis.setLabel("Frequency (Hz)");
        xAxis.setAutoRanging(true);
        xAxis.setAnimated(false);

        yAxis = new NumberAxis();
        yAxis.setLabel("Magnitude (dB)");
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("S1P Log Magnitude Plot");
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);

        setupSeries();
        setupCursorOverlay();

        StackPane root = new StackPane(lineChart, createCursorLayer());
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        SmithChartViewModel viewModel = SmithChartViewModel.getInstance();

        setupInteractions();

        this.s1pListener = _ -> requestUpdate();
        viewModel.transformedS1PPointsProperty().addListener(s1pListener);

        this.redrawListener = (_, oldVal, newVal) -> {
            if (!newVal && oldVal) requestUpdate();
        };
        viewModel.isRedrawing.addListener(redrawListener);

        requestUpdate();
        stage.setOnCloseRequest(_ -> cleanup());
    }

    /**
     * Sets up the data series for the chart.
     * It includes the base S1P data and up to three filter series.
     */
    private void setupSeries() {
        baseSeries.setName("S1P Data");
        f1Series.setName("Filter 1");
        f2Series.setName("Filter 2");
        f3Series.setName("Filter 3");

        lineChart.getData().add(baseSeries);
        lineChart.getData().add(f1Series);
        lineChart.getData().add(f2Series);
        lineChart.getData().add(f3Series);

        styleSeries(baseSeries, "rgba(30, 144, 255, 0.3)", 1.0);
        styleSeries(f1Series, "indianred", 2.0);
        styleSeries(f2Series, "darkorange", 2.0);
        styleSeries(f3Series, "forestgreen", 2.0);
    }

    /**
     * Creates the cursor overlay layer with crosshair lines, highlight point, load point, and info box.
     * @return the cursor overlay pane
     */
    private Pane createCursorLayer() {
        Pane cursorLayer = new Pane();
        cursorLayer.setMouseTransparent(true);
        cursorLayer.getChildren().addAll(vLine, hLine, highlightPoint, loadPoint, infoBox);
        return cursorLayer;
    }

    /**
     * Sets up the style and initial visibility of the cursor overlay elements.
     */
    private void setupCursorOverlay() {
        String lineStyle = "-fx-stroke: rgba(200,200,200,0.8); -fx-stroke-dash-array: 5 5;";
        vLine.setStyle(lineStyle);
        hLine.setStyle(lineStyle);
        vLine.setVisible(false);
        hLine.setVisible(false);
        highlightPoint.setVisible(false);

        infoBox.setStyle("""
            -fx-background-color: rgba(30,30,30,0.8);
            -fx-text-fill: white;
            -fx-padding: 5;
            -fx-font-family: monospace;
            -fx-font-size: 13px;
            -fx-background-radius: 3;
            -fx-border-color: rgba(255,255,255,0.3);
            -fx-border-radius: 3;
        """);
        infoBox.setVisible(false);
    }

    /**
     *
     */
    private void setupInteractions() {
        Platform.runLater(() -> {
            Node plotArea = lineChart.lookup(".chart-plot-background");
            if (plotArea == null) return;

            plotArea.layoutBoundsProperty().addListener(_ -> updateLoadPointPosition());

            plotArea.setOnMouseMoved(e -> {
                if (e.isShiftDown()) return;

                double mouseX = e.getX();

                // Convert mouse X to frequency
                Number freqVal = xAxis.getValueForDisplay(mouseX);
                if (freqVal == null) return;

                var point = findNearest(freqVal.doubleValue());
                if (point == null) return;

                updateCursor(plotArea, point);
            });

            plotArea.setOnMouseExited(_ -> {
                vLine.setVisible(false);
                hLine.setVisible(false);
                highlightPoint.setVisible(false);
                infoBox.setVisible(false);
            });
        });
    }

    private void updateCursor(Node plotArea, XYChart.Data<Number, Number> p) {
        double xVal = xAxis.getDisplayPosition(p.getXValue());
        double yVal = yAxis.getDisplayPosition(p.getYValue());

        // Transform coordinates from plot area local space to scene, then to cursor layer local space.
        Point2D pointInLayer = vLine.getParent().sceneToLocal(plotArea.localToScene(xVal, yVal));

        double x = pointInLayer.getX();
        double y = pointInLayer.getY();

        // Calculate plot area bounds in cursor layer for lines
        Bounds areaBounds = vLine.getParent().sceneToLocal(plotArea.localToScene(plotArea.getBoundsInLocal()));

        // Vertical Line
        vLine.setStartX(x);
        vLine.setEndX(x);
        vLine.setStartY(areaBounds.getMinY());
        vLine.setEndY(areaBounds.getMaxY());
        vLine.setVisible(true);

        // Horizontal Line
        hLine.setStartX(areaBounds.getMinX());
        hLine.setEndX(areaBounds.getMaxX());
        hLine.setStartY(y);
        hLine.setEndY(y);
        hLine.setVisible(true);

        // Highlight Point
        highlightPoint.setCenterX(x);
        highlightPoint.setCenterY(y);
        highlightPoint.setVisible(true);

        // Info Box
        infoBox.setText(formatInfo(p));
        double labelOffset = 10;
        double labelX = x + labelOffset;
        double labelY = y + labelOffset;

        // Prevent the info box from going outside the window
        if (labelX + 150 > stage.getScene().getWidth()) labelX = x - 160;
        if (labelY + 50 > stage.getScene().getHeight()) labelY = y - 60;

        infoBox.setLayoutX(labelX);
        infoBox.setLayoutY(labelY);
        infoBox.setVisible(true);
    }

    /**
     * Formats the information string for the info box.
     * @param p the data point to display
     * @return the formatted info string
     */
    private String formatInfo(XYChart.Data<Number, Number> p) {
        return String.format("Freq: %s\nMag : %.2f dB",
                SmithUtilities.displayBestUnitAndFormattedValue(p.getXValue().doubleValue(), FrequencyUnit.values()),
                p.getYValue().doubleValue());
    }


    private XYChart.Data<Number, Number> findNearest(double targetX) {
        var data = baseSeries.getData();
        if (data.isEmpty()) return null;

        // Binary search for nearest
        int idx = Collections.binarySearch(data, new XYChart.Data<>(targetX, 0), Comparator.comparingDouble(o -> o.getXValue().doubleValue())
        );

        if (idx >= 0) return data.get(idx);

        // If not exact match, binarySearch returns (-(insertion point) - 1)
        int insertionPoint = -idx - 1;
        if (insertionPoint == 0) return data.getFirst();
        if (insertionPoint == data.size()) return data.getLast();

        // Check which neighbor is closer
        var p1 = data.get(insertionPoint - 1);
        var p2 = data.get(insertionPoint);
        double d1 = Math.abs(p1.getXValue().doubleValue() - targetX);
        double d2 = Math.abs(p2.getXValue().doubleValue() - targetX);

        return (d1 < d2) ? p1 : p2;
    }

    /**
     * Styles a data series with the specified color and line width
     * @param series the data series to style
     * @param colorWebString the color in web string format (e.g., "rgba(255,0,0,1.0)")
     * @param width the line width in pixels
     */
    private void styleSeries(XYChart.Series<Number, Number> series, String colorWebString, double width) {
        String style = "-fx-stroke: " + colorWebString + "; -fx-stroke-width: " + width + "px;";
        series.nodeProperty().addListener((_, _, node) -> {
            if (node != null) node.setStyle(style);
        });
        if (series.getNode() != null) series.getNode().setStyle(style);
    }

    /**
     * Cleans up resources and listeners when the window is closed.
     */
    private void cleanup() {
        SmithChartViewModel.getInstance().transformedS1PPointsProperty().removeListener(s1pListener);
        SmithChartViewModel.getInstance().isRedrawing.removeListener(redrawListener);
        lineChart.getData().forEach(s -> s.getData().clear());
        instance = null;
        System.out.println("S1P Window cleaned up.");
    }

    /**
     * Requests an update to the chart data.
     * Ensures that only one update is pending at a time, saves A LOT of resources.
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
                lineChart.getData().forEach(s -> s.getData().clear());
                return;
            }

            var properFreqUnit = vm.getProperFrequencyUnitS1P();
            xAxis.setLabel("Frequency (" + properFreqUnit + ")");
            double freqFactor = properFreqUnit.getFactor();

            List<XYChart.Data<Number, Number>> baseData = new ArrayList<>();
            List<XYChart.Data<Number, Number>> f1Data = new ArrayList<>();
            List<XYChart.Data<Number, Number>> f2Data = new ArrayList<>();
            List<XYChart.Data<Number, Number>> f3Data = new ArrayList<>();

            int totalPoints = rawList.size();
            int step = Math.max(1, totalPoints / MAX_RENDER_POINTS);

            for (int i = 0; i < totalPoints; i += step) {
                addDataPoint(rawList.get(i), vm, freqFactor, baseData, f1Data, f2Data, f3Data);
            }
            // Ensure last point
            if ((totalPoints - 1) % step != 0) {
                addDataPoint(rawList.getLast(), vm, freqFactor, baseData, f1Data, f2Data, f3Data);
            }

            // Update Series Data
            baseSeries.getData().setAll(baseData);
            updateFilterSeries(f1Series, f1Data, vm.filter1EnabledProperty().get());
            updateFilterSeries(f2Series, f2Data, vm.filter2EnabledProperty().get());
            updateFilterSeries(f3Series, f3Data, vm.filter3EnabledProperty().get());

            // Update Load Point
            updateLoadPoint(vm, freqFactor);

        } finally {
            isUpdatePending = false;
        }
    }

    private void updateLoadPoint(SmithChartViewModel vm, double freqFactor) {
        Complex gamma;
        Complex previewGamma = vm.getPreviewElementGamma();

        if(previewGamma != null) {
            gamma = previewGamma;
        } else {
            gamma = vm.getCurrentInteractionStartGamma();
        }

        if (!vm.isAnyUseS1PAsLoad()) {
            loadPoint.setVisible(false);
            return;
        }

        // Store the current values for repositioning
        double db = SmithCalculator.calculateReflectionCoefficientDb(gamma);
        double loadFreq = vm.frequencyProperty().get() / freqFactor;

        // Store these for resize updates
        loadPoint.setUserData(new double[]{loadFreq, db});

        positionLoadPoint(loadFreq, db);
    }


    private void positionLoadPoint(double freq, double db) {
        Node plotArea = lineChart.lookup(".chart-plot-background");
        if (plotArea == null) {
            loadPoint.setVisible(false);
            return;
        }

        double xPos = xAxis.getDisplayPosition(freq);
        double yPos = yAxis.getDisplayPosition(db);

        if (Double.isNaN(xPos) || Double.isNaN(yPos) || !Double.isFinite(xPos) || !Double.isFinite(yPos)) {
            loadPoint.setVisible(false);
            return;
        }

        Point2D pointInScene = plotArea.localToScene(xPos, yPos);
        Point2D pointInLayer = loadPoint.getParent().sceneToLocal(pointInScene);

        loadPoint.setCenterX(pointInLayer.getX());
        loadPoint.setCenterY(pointInLayer.getY());
        loadPoint.setVisible(true);
    }

    private void updateLoadPointPosition() {
        if (!loadPoint.isVisible() && loadPoint.getUserData() == null) return;

        double[] coords = (double[]) loadPoint.getUserData();
        if (coords != null) {
            Platform.runLater(() -> positionLoadPoint(coords[0], coords[1]));
        }
    }

    private void addDataPoint(DataPoint dp, SmithChartViewModel vm, double freqFactor,
                              List<XYChart.Data<Number, Number>> base,
                              List<XYChart.Data<Number, Number>> f1,
                              List<XYChart.Data<Number, Number>> f2,
                              List<XYChart.Data<Number, Number>> f3) {

        if (dp.gammaProperty().get() == null) return;

        double freq = dp.getFrequency() / freqFactor;
        double db = SmithCalculator.calculateReflectionCoefficientDb(dp.gammaProperty().get());

        var dataNode = new XYChart.Data<Number, Number>(freq, db);
        base.add(dataNode);

        double rawFreq = dp.getFrequency();
        if (vm.filter1EnabledProperty().get() && vm.isFrequencyInRangeF1(rawFreq)) f1.add(new XYChart.Data<>(freq, db));
        if (vm.filter2EnabledProperty().get() && vm.isFrequencyInRangeF2(rawFreq)) f2.add(new XYChart.Data<>(freq, db));
        if (vm.filter3EnabledProperty().get() && vm.isFrequencyInRangeF3(rawFreq)) f3.add(new XYChart.Data<>(freq, db));
    }

    private void updateFilterSeries(XYChart.Series<Number, Number> series, List<XYChart.Data<Number, Number>> data, boolean enabled) {
        if (enabled && !data.isEmpty()) series.getData().setAll(data);
        else series.getData().clear();
    }

    public void show() {
        if (stage.isIconified()) stage.setIconified(false);
        stage.show();
        stage.toFront();
    }
}
