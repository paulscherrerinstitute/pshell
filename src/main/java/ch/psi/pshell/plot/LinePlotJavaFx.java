package ch.psi.pshell.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 */
public class LinePlotJavaFx extends LinePlotBase {

    final boolean HOVERING_SYMBOLS = true;

    public LinePlotJavaFx() {
        super();
        setRequireUpdateOnAppend(false);
    }

    @Override
    protected void onTitleChanged() {
        Platform.runLater(() -> {
            chart.setId(getTitle());
            chart.setTitle(getTitle());
        });
    }

    @Override
    protected void onAxisLabelChanged(final AxisId axis) {
        //TODO
    }

    public void setVisible(boolean value) {
        if (!value) {
            Platform.runLater(() -> {
                close();
            });
        }
        super.setVisible(value);
    }

    @Override
    protected void onAxisRangeChanged(AxisId axis_id) {
        NumberAxis axis = null;
        switch (axis_id) {
            case X:
                axis = (NumberAxis) chart.getXAxis();
                break;
            case Y:
                axis = (NumberAxis) chart.getYAxis();
                break;
            default:
                return;
        }
        axis.setAutoRanging(getAxis(axis_id).isAutoRange());
        axis.setLowerBound(getAxis(axis_id).getMin());
        axis.setUpperBound(getAxis(axis_id).getMax());
    }

    final LinkedHashMap<XYChart.Series, ConcurrentLinkedQueue<Data<Number, Number>>> seriesList = new LinkedHashMap<>();

    @Override
    protected Object onAddedSeries(final LinePlotSeries series) {
        XYChart.Series chartSeries = new XYChart.Series<>();
        ConcurrentLinkedQueue<Data<Number, Number>> queue = new ConcurrentLinkedQueue<>();
        chartSeries.setName(series.getName());
        synchronized (seriesList) {
            seriesList.put(chartSeries, queue);
        }
        update(true);
        return chartSeries;
    }

    @Override
    protected void onRemovedSeries(LinePlotSeries series) {
        XYChart.Series chartSeries = getChartSeries(series);
        if (chartSeries != null) {
            synchronized (seriesList) {
                seriesList.remove(chartSeries);
            }
            update(true);
        }
    }

    @Override
    protected void onAppendData(final LinePlotSeries series, final double x, final double y) {
        XYChart.Series chartSeries = getChartSeries(series);
        if (chartSeries != null) {
            synchronized (seriesList) {
                ConcurrentLinkedQueue<Data<Number, Number>> queue = seriesList.get(chartSeries);
                Data<Number, Number> dataPoint = new Data<>(x, y);
                queue.add(dataPoint);
            }
        }
    }

    @Override
    protected void onSetData(LinePlotSeries series, double[] x, double[] y) {
        XYChart.Series chartSeries = getChartSeries(series);
        synchronized (seriesList) {
            ConcurrentLinkedQueue<Data<Number, Number>> queue = seriesList.get(chartSeries);
            //Separated loop for pperformance (not checking inside the loop)
            if (x == null) {
                for (int i = 0; i < y.length; i++) {
                    Data<Number, Number> dataPoint = new Data<>(i, y[i]);
                    queue.add(dataPoint);
                }
            } else {
                for (int i = 0; i < x.length; i++) {
                    Data<Number, Number> dataPoint = new Data<>(x[i], y[i]);
                    queue.add(dataPoint);
                }
            }
        }
    }

    @Override
    public void updateSeries(LinePlotSeries series) {
    }

    @Override
    public double[][] getSeriesData(LinePlotSeries series) {
        XYChart.Series s = getChartSeries(series);
        ObservableList<Data<Number, Number>> list = s.getData();
        double[] x = new double[list.size()];
        double[] y = new double[list.size()];
        int index = 0;
        for (Data<Number, Number> item : list) {
            x[index] = item.getXValue().doubleValue();
            y[index++] = item.getYValue().doubleValue();
        }
        return new double[][]{x, y};

    }

    XYChart.Series getChartSeries(LinePlotSeries series) {
        return (XYChart.Series) (series.getToken());
    }

    LinePlotSeries getLinePlotSeries(XYChart.Series s) {
        for (LinePlotSeries series : this.getAllSeries()) {
            if (series.getToken() == s) {
                return series;
            }
        }
        return null;
    }

    @Override
    public void doUpdate() {
        if (mTimerFX != null) {
            Platform.runLater(() -> {
                updateFX();
            });
        }
    }

    private JFXPanel fxContainer;

    @Override
    protected void createChart() {
        super.createChart();

        fxContainer = new JFXPanel();   //Must do bedore any JavaFX call
        //xAxis = new NumberAxis(lower,upper,tick);
        NumberAxis xAxis = new NumberAxis();
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(true);

        xAxis.setTickLabelsVisible(true);
        xAxis.setTickMarkVisible(true);
        xAxis.setMinorTickVisible(true);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(false);
        yAxis.setAutoRanging(true);

        chart = new MyLineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setCursor(Cursor.CROSSHAIR);

        fxContainer.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setLayout(new BorderLayout());
        add(fxContainer);
        // create JavaFX scene        
        Platform.runLater(() -> {
            createScene();
        });
    }

    class MyLineChart<X extends Object, Y extends Object> extends LineChart<X, Y> {

        public MyLineChart(Axis<X> axis, Axis<Y> axis1) {
            super(axis, axis1);
        }

        public void addNode(Node node) {
            if (node != null) {
                getPlotChildren().add(node);
            }
        }

        public void removeNode(Node node) {
            if (node != null) {
                getPlotChildren().remove(node);
            }
        }

        @Override
        protected void requestChartLayout() {
            super.requestChartLayout();
        }
    }

    private MyLineChart<Number, Number> chart;

    boolean drawSymbols = false;

    public void setDrawSymbols(boolean value) {
        if (value != drawSymbols) {
            drawSymbols = value;
            if (chart != null) {
                if (HOVERING_SYMBOLS) {
                    for (Iterator<Series<Number, Number>> it = chart.getData().iterator(); it.hasNext();) {
                        XYChart.Series series = it.next();
                        ObservableList<Data<Number, Number>> list = series.getData();
                        for (Data<Number, Number> item : list) {
                            Node symbol = item.getNode();
                            if (symbol != null) {
                                item.setNode(null);
                                chart.removeNode(symbol);
                            }
                            if (drawSymbols) {
                                symbol = new HoveredThresholdNode(series, item.getXValue(), item.getYValue());
                                //symbol = chart.createSymbol(series,chart.getData().indexOf(series), item, itemIndex);
                                item.setNode(symbol);
                                chart.addNode(symbol);
                            }
                        }
                    }
                } else {
                    chart.setCreateSymbols(getDrawSymbols());
                }

                chart.requestChartLayout();
            }
        }
    }

    public boolean getDrawSymbols() {
        return drawSymbols;
    }

    Rectangle zoomRect;

    private void createScene() {
        chart.setId(getTitle());
        chart.setTitle(getTitle());

        final StackPane chartContainer = new StackPane();
        chartContainer.getChildren().add(chart);
        zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
        chartContainer.getChildren().add(zoomRect);

        setUpZooming(zoomRect, chart);
        setUpContextMenu();
        fxContainer.setScene(new Scene(chartContainer));

        synchronized (seriesList) {
            addSeriesToChart();
        }

        // Every frame to take any data from queue and add to chart
        mTimerFX = new AnimationTimer() {
            @Override
            public void handle(long now) {
                onTimerFX(now);
            }
        };
        mTimerFX.start();

    }

    private void close() {
        mTimerFX.stop();
    }
    //Timeline mTimerFX;
    AnimationTimer mTimerFX;

    void onTimerFX(long start) {
        synchronized (seriesList) {
            for (XYChart.Series series : seriesList.keySet()) {
                LinePlotSeries lps = getLinePlotSeries(series);
                int maxItemCount = (lps == null) ? -1 : lps.getMaxItemCount();
                if (isPlottingSeries(series)) {
                    ConcurrentLinkedQueue<Data<Number, Number>> queue = seriesList.get(series);
                    if (!queue.isEmpty()) {

                        if (getDrawSymbols()) {
                            for (Data<Number, Number> d : queue) {
                                d.setNode(new HoveredThresholdNode(series, d.getXValue(), d.getYValue()));
                            }
                        }
                        series.getData().addAll(queue);
                        if ((maxItemCount > 0) && (series.getData().size() > maxItemCount)) {
                            series.getData().remove(0, series.getData().size() - maxItemCount);
                        }
                        queue.clear();
                    }
                }
                if (getElapsedMillis(start) > 200) {
                    return;
                }
            }
        }
    }

    long getElapsedMillis(long start) {
        return (System.nanoTime() - start) / 1000000;
    }

    void updateFX() {
        synchronized (seriesList) {
            addSeriesToChart();
            removeSeriesFromChart();
        }
    }

    void addSeriesToChart() {
        for (XYChart.Series series : seriesList.keySet()) {
            if (!isPlottingSeries(series)) {
                chart.getData().add(series);

//Attempt to have a floating label that could speed up rendering of symbol coordinates
//Didn't manage to display the label.
                /*
                
                 final Label label = new Label("");
                 label.setLabelFor(series.getNode());
                 label.getStyleClass().addAll("chart-line-symbol");
                 label.getStyleClass().addAll(series.getNode().getStyleClass());
                 label.setStyle("-fx-font-size: 13; -fx-font-weight: normal;"); ;
                 label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
                 chart.addNode(label);
                
                 series.getNode().setOnMouseEntered(new EventHandler<MouseEvent>() {
                 @Override public void handle(MouseEvent mouseEvent) {
                 final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
                 final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
                 Point2D xAxisInScene = xAxis.localToScene(0, 0);
                 double xOffset = mouseEvent.getSceneX() -  xAxisInScene.getX();
                 double yOffset = mouseEvent.getSceneY() - xAxisInScene.getY();
                 double xAxisScale = xAxis.getScale();
                 double yAxisScale = yAxis.getScale();
                 double x = xAxis.getLowerBound() + xOffset / xAxisScale;
                 double y = yAxis.getLowerBound() + yOffset / yAxisScale;
                 String str="(" + format.format(x) + ", " + format.format(y) +")";
                
                 //System.out.println(str);
                 label.setText(str);
                 label.setVisible(true);
                 label.toFront();
                
                
                 }
                 });
                 series.getNode().setOnMouseExited(new EventHandler<MouseEvent>() {
                 @Override public void handle(MouseEvent mouseEvent) {
                 label.setVisible(false);
                 }
                 });
                
                 */
            }
        }
    }

    void removeSeriesFromChart() {
        ArrayList<XYChart.Series> list = new ArrayList<>();
        for (XYChart.Series series : chart.getData()) {
            if (!seriesList.keySet().contains(series)) {
                list.add(series);
            }
        }
        for (XYChart.Series series : list) {
            chart.getData().remove(series);
        }
    }

    boolean isPlottingSeries(Series series) {
        return chart.getData().contains(series);
    }

    boolean zooming;

    private void setUpZooming(final Rectangle rect, final Node zoomingNode) {
        final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
        zoomingNode.setOnMousePressed((MouseEvent event) -> {
            if (event.isControlDown() && (event.getButton() == MouseButton.PRIMARY)) {
                mouseAnchor.set(new Point2D(event.getX(), event.getY()));
                zooming = true;
                rect.setWidth(0);
                rect.setHeight(0);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(zoomingNode, event.getScreenX(), event.getScreenY());

            }
        });
        zoomingNode.setOnMouseDragged((MouseEvent event) -> {
            if (event.isControlDown() && zooming) {
                double x1 = event.getX();
                double y1 = event.getY();
                rect.setX(Math.min(x1, mouseAnchor.get().getX()));
                rect.setY(Math.min(y1, mouseAnchor.get().getY()));
                rect.setWidth(Math.abs(x1 - mouseAnchor.get().getX()));
                rect.setHeight(Math.abs(y1 - mouseAnchor.get().getY()));
            }
        });

        zoomingNode.setOnMouseReleased((MouseEvent event) -> {
            if (event.isControlDown() && zooming) {
                doZoom(zoomRect, chart);
            }
            rect.setWidth(0);
            rect.setHeight(0);
            zooming = false;
        });

    }

    private void doZoom(Rectangle zoomRect, LineChart<Number, Number> chart) {
        Point2D zoomTopLeft = new Point2D(zoomRect.getX(), zoomRect.getY());
        Point2D zoomBottomRight = new Point2D(zoomRect.getX() + zoomRect.getWidth(), zoomRect.getY() + zoomRect.getHeight());
        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        Point2D yAxisInScene = yAxis.localToScene(0, 0);
        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        if (zoomTopLeft.getX() < yAxisInScene.getX() && zoomTopLeft.getY() < yAxisInScene.getY()) {
            resetZoom();
        } else {
            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);
            Point2D xAxisInScene = xAxis.localToScene(0, 0);
            double xOffset = zoomTopLeft.getX() - yAxisInScene.getX();
            double yOffset = zoomBottomRight.getY() - xAxisInScene.getY();
            double xAxisScale = xAxis.getScale();
            double yAxisScale = yAxis.getScale();
            xAxis.setLowerBound(xAxis.getLowerBound() + xOffset / xAxisScale);
            xAxis.setUpperBound(xAxis.getLowerBound() + zoomRect.getWidth() / xAxisScale);
            yAxis.setLowerBound(yAxis.getLowerBound() + yOffset / yAxisScale);
            yAxis.setUpperBound(yAxis.getLowerBound() - zoomRect.getHeight() / yAxisScale);
            //   System.out.println(xAxis.getLowerBound() + " " + xAxis.getUpperBound() + "    " + yAxis.getLowerBound() + " " + yAxis.getUpperBound());
        }

        zoomRect.setWidth(0);
        zoomRect.setHeight(0);
    }

    private void resetZoom() {

        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();

        if (getAxis(AxisId.Y).isAutoRange()) {
            yAxis.setLowerBound(getAxis(AxisId.Y).getMin());
            yAxis.setUpperBound(getAxis(AxisId.Y).getMax());
            yAxis.setAutoRanging(true);

        } else {
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(getAxis(AxisId.Y).getMin());
            yAxis.setUpperBound(getAxis(AxisId.Y).getMax());
        }

        if (getAxis(AxisId.X).isAutoRange()) {
            xAxis.setLowerBound(getAxis(AxisId.X).getMin());
            xAxis.setUpperBound(getAxis(AxisId.X).getMax());
            xAxis.setAutoRanging(true);
        } else {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(getAxis(AxisId.X).getMin());
            xAxis.setUpperBound(getAxis(AxisId.X).getMax());
        }
    }

    ContextMenu contextMenu;

    public void setUpContextMenu() {
        contextMenu = new ContextMenu();
        final CheckMenuItem item1 = new CheckMenuItem("Draw Symbols");
        item1.setSelected(getDrawSymbols());
        item1.setOnAction((ActionEvent e) -> {
            setDrawSymbols(item1.isSelected());
        });
        final CheckMenuItem item2 = new CheckMenuItem("Show Legend");
        item2.setSelected(chart.isLegendVisible());
        item2.setOnAction((ActionEvent e) -> {
            chart.setLegendVisible(item2.isSelected());
        });
        MenuItem item3 = new MenuItem("Reset Zoom");
        item3.setOnAction((ActionEvent e) -> {
            resetZoom();
        });
        contextMenu.getItems().addAll(item1, item2, item3);
    }

    DecimalFormat format = new DecimalFormat("#.######");

    class HoveredThresholdNode extends StackPane {

        HoveredThresholdNode(XYChart.Series series, Number x, Number y) {
            setPrefSize(8, 8);
            getStyleClass().addAll("chart-line-symbol");
            getStyleClass().addAll(series.getNode().getStyleClass());

            final Label label = new Label("(" + format.format(x) + ", " + format.format(y) + ")");
            label.getStyleClass().addAll("chart-line-symbol");
            label.getStyleClass().addAll(series.getNode().getStyleClass());
            label.setStyle("-fx-font-size: 13; -fx-font-weight: normal;");
            //label.setTextFill(Color.FORESTGREEN);
            label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
            setOnMouseEntered((MouseEvent mouseEvent) -> {
                getChildren().setAll(label);
                setCursor(Cursor.NONE);
                toFront();
            });
            setOnMouseExited((MouseEvent mouseEvent) -> {
                getChildren().clear();
                setCursor(Cursor.CROSSHAIR);
            });
        }

    }

    void addSwingMenuItem(final ObservableList<MenuItem> menu, final JMenuItem item) {
        if (item instanceof JMenu) {
            Menu menuFx = new Menu(item.getText());
            for (Component menuItem : ((JMenu) item).getMenuComponents()) {
                if (menuItem instanceof JMenu) {
                    addSwingMenuItem(menuFx.getItems(), ((JMenu) menuItem));
                } else if (menuItem instanceof JPopupMenu.Separator) {
                    menuFx.getItems().add(new SeparatorMenuItem());
                } else if (menuItem instanceof JMenuItem) {
                    addSwingMenuItem(menuFx.getItems(), ((JMenuItem) menuItem));
                }
            }
            menu.add(menuFx);
        } else if (item instanceof JMenuItem) {
            MenuItem itemFX = new MenuItem(item.getText());
            itemFX.setOnAction((ActionEvent e) -> {
                item.doClick();
            });
            menu.add(itemFX);
        }
    }

    @Override
    public void addPopupMenuItem(final JMenuItem item) {
        Platform.runLater(() -> {
            if (item == null) {
                contextMenu.getItems().add(new SeparatorMenuItem());
            } else {
                addSwingMenuItem(contextMenu.getItems(), item);
            }
        });
    }

}
