package ch.psi.pshell.plot;

import ch.psi.utils.Reflection;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYDrawableAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.ComparableObjectSeries;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.Series;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.Drawable;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

public class LinePlotJFree extends LinePlotBase {

    static final Logger logger = Logger.getLogger(LinePlotJFree.class.getName());

    ChartPanel chartPanel;
    boolean legendVisible;
    boolean showTooltips;

    public static final Font TICK_LABEL_FONT = new Font(Font.SANS_SERIF, 0, 10);
    public static final Font LABEL_FONT = new Font(Font.SANS_SERIF, 0, 11);

    AbstractXYDataset dataY1;
    AbstractXYDataset dataY2;

    //TODO: if smaller there are plot repainting problem in scans in JFreeChart > 1.0.18
    static final double AUTO_RANGE_MINIMUM_SIZE = 1e-12;
    JFreeChart chart;

    boolean tooltips = false;
    Rectangle2D.Double seriesMarker;

    /**
     */
    public LinePlotJFree() {
        super();
        setMarkerSize(getMarkerSize());
    }

    void setMarkerSize(double size) {
        seriesMarker = new Rectangle2D.Double(-size / 2, -size / 2, size, size);
    }

    void changeMarkerSize(double size) {
        Rectangle2D.Double former = seriesMarker;
        setMarkerSize(size);
        for (LinePlotSeries series : getAllSeries()) {
            XYItemRenderer renderer = getRenderer(series.getAxisY());
            int index = getSeriesIndex(series);
            if (index >= 0) {
                //Only change markes if not manually set
                if (renderer.getSeriesShape(index) == former) {
                    renderer.setSeriesShape(index, seriesMarker);
                }
            }
        }
    }

    @Override
    public void setQuality(Quality quality) {
        super.setQuality(quality);
        if ((chart != null) && (quality != null)) {
            chart.setAntiAlias(quality.ordinal() >= Quality.High.ordinal());
        }
    }

    public static class CrossAnnotation implements Drawable {

        private final Color color;

        /**
         * Creates a new instance.
         */
        public CrossAnnotation(Color color) {
            this.color = color;
        }

        /**
         * Draws cross
         *
         * @param g2 the graphics device.
         * @param area the area in which to draw.
         */
        @Override
        public void draw(Graphics2D g2, Rectangle2D area) {

            // Draw red cross
            g2.setPaint(color);
            g2.setStroke(new BasicStroke(2)); // Stroke width = 2
            Line2D line1 = new Line2D.Double(area.getCenterX(), area.getMinY(), area.getCenterX(), area.getMaxY());
            Line2D line2 = new Line2D.Double(area.getMinX(), area.getCenterY(), area.getMaxX(), area.getCenterY());
            g2.draw(line1);
            g2.draw(line2);
        }
    }

    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
        if (chartPanel != null) {
            chartPanel.setBackground(c);
            chartPanel.getChart().setBackgroundPaint(c);
        }
    }
    
    @Override
    public void setPlotBackgroundColor(Color c) {
        chart.getPlot().setBackgroundPaint(c);
    }
    
    @Override
    public void setPlotGridColor(Color c) {
        ((XYPlot)chart.getPlot()).setDomainGridlinePaint(c);
        ((XYPlot)chart.getPlot()).setRangeGridlinePaint(c);
    }     
    
    @Override
    public void setPlotOutlineColor(Color c) {
        chart.getPlot().setOutlinePaint(c);
    }

    @Override
    protected void onAppendData(LinePlotSeries series, double x, double y) {
        XYSeries s = getXYSeries(series);
        s.add(new XYDataItem(x, y), false);
    }

    @Override
    protected void onSetData(final LinePlotSeries series, final double[] x, final double[] y) {
        //SwingUtilities.invokeLater(() -> {
        XYSeries s = getXYSeries(series);
        s.setNotify(false);
        if (!s.isEmpty()) {
            s.clear();
        }
        //Separated loop for performance (not checking inside the loop)
        if (x == null) {
            for (int i = 0; i < y.length; i++) {
                s.add(new XYDataItem(i, y[i]), false);
            }
        } else {
            for (int i = 0; i < y.length; i++) {
                s.add(new XYDataItem(x[i], y[i]), false);
            }
        }
        s.setNotify(true);
        //});
    }

    protected ChartPanel getChartPanel() {
        return chartPanel;
    }

    void createY2() {
        if (dataY2 == null) {
            XYPlot plot = (XYPlot) chart.getPlot();
            Series s;

            switch (getStyle()) {
                case ErrorX:
                    dataY2 = new YIntervalSeriesCollection();
                    break;
                case ErrorY:
                    dataY2 = new YIntervalSeriesCollection();
                    break;
                case ErrorXY:
                    dataY2 = new XYIntervalSeriesCollection();
                    break;
                default:
                    dataY2 = new XYSeriesCollection();
            }

            NumberAxis axis2 = new NumberAxis(getAxis(AxisId.Y2).getLabel());
            axis2.setAutoRangeIncludesZero(false);
            ((NumberAxis) plot.getRangeAxis()).setAutoRangeMinimumSize(AUTO_RANGE_MINIMUM_SIZE);
            axis2.setLabelFont(LABEL_FONT);
            axis2.setTickLabelFont(TICK_LABEL_FONT);
            plot.setRangeAxis(1, axis2);
            XYLineAndShapeRenderer renderer2 = getStyle().isError() ? new XYErrorRenderer() : new XYLineAndShapeRenderer();
            renderer2.setBaseShapesVisible(true);
            plot.setRenderer(1, renderer2);
            plot.setDataset(1, dataY2);
            plot.mapDatasetToRangeAxis(1, 1);
            axis2.setLabelPaint(getAxisTextColor());
            axis2.setTickLabelPaint(getAxisTextColor());
        }
    }

    AbstractXYDataset getYData(int axis) {
        //Axis2  is only created if a series on Y2 is added
        if (axis == 2) {
            createY2();
            return dataY2;
        }
        return dataY1;
    }

    XYLineAndShapeRenderer getRenderer(int axis) {
        XYPlot plot = chart.getXYPlot();
        if (axis == 2) {
            return (XYLineAndShapeRenderer) plot.getRenderer(1);
        }
        return (XYLineAndShapeRenderer) plot.getRenderer();
    }

    int getSeriesIndex(LinePlotSeries series) {
        if (getStyle().isError()) {
            //indexOf not working for XIntervalSeriesCollection
            int index = 0;
            for (LinePlotSeries s : this.getAllSeries()) {
                if (s == series) {
                    return index;
                }
                if (series.getAxisY() == s.getAxisY()) {
                    index++;
                }
            }
            return -1;
        }
        XYSeries xys = getXYSeries(series);
        if (xys == null) {
            return -1;
        }
        XYSeriesCollection data = (XYSeriesCollection) getYData(series.getAxisY());
        return data.indexOf(xys);
    }

    @Override
    protected Object onAddedSeries(LinePlotSeries series) {
        //TODO: LinePlotErrorSeries is specific to JFree
        Series s;
        AbstractXYDataset data = getYData(series.getAxisY());
        switch (getStyle()) {
            case ErrorX:
                s = new XIntervalSeries(series.getName(), false, true);
                ((XIntervalSeriesCollection) data).addSeries((XIntervalSeries) s);
                break;
            case ErrorY:
                s = new YIntervalSeries(series.getName(), false, true);
                ((YIntervalSeriesCollection) data).addSeries((YIntervalSeries) s);
                break;
            case ErrorXY:
                s = new XYIntervalSeries(series.getName(), false, true);
                ((XYIntervalSeriesCollection) data).addSeries((XYIntervalSeries) s);
                break;
            default:
                s = new XYSeries(series.getName(), false);
                ((XYSeriesCollection) data).addSeries((XYSeries) s);
        }

        if (series.getMaxItemCount() > 0) {
            if (s instanceof ComparableObjectSeries) {
                ((ComparableObjectSeries) s).setMaximumItemCount(series.getMaxItemCount());
            } else {
                ((XYSeries) s).setMaximumItemCount(series.getMaxItemCount());
            }
        }
        int index = data.getSeriesCount() - 1;
        XYItemRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesShape(index, seriesMarker);
        if (series.getColor() != null) {
            renderer.setSeriesPaint(index, series.getColor());
        }
        if (getStyle().isError()) {
            ((XYLineAndShapeRenderer) renderer).setSeriesLinesVisible(index, series.getLinesVisible());
        }
        return s;
    }

    @Override
    protected void onRemovedSeries(LinePlotSeries series) {
        AbstractXYDataset data = getYData(series.getAxisY());
        switch (getStyle()) {
            case ErrorX:
                ((XIntervalSeriesCollection) data).removeSeries((XIntervalSeries) (series.getToken()));
                break;
            case ErrorY:
                ((YIntervalSeriesCollection) data).removeSeries((YIntervalSeries) (series.getToken()));
                break;
            case ErrorXY:
                ((XYIntervalSeriesCollection) data).removeSeries((XYIntervalSeries) (series.getToken()));
                break;
            default:
                ((XYSeriesCollection) data).removeSeries((XYSeries) (series.getToken()));
        }
    }

    @Override
    public double[][] getSeriesData(final LinePlotSeries series) {
        double[] x = new double[0];
        double[] y = new double[0];

        switch (getStyle()) {
            case ErrorX: {
                XIntervalSeries s = (XIntervalSeries) getSeries(series);
                x = new double[s.getItemCount()];
                y = new double[s.getItemCount()];
                for (int i = 0; i < s.getItemCount(); i++) {
                    x[i] = s.getX(i).doubleValue();
                    y[i] = s.getYValue(i);
                }
            }
            break;
            case ErrorY: {
                YIntervalSeries s = (YIntervalSeries) getSeries(series);
                x = new double[s.getItemCount()];
                y = new double[s.getItemCount()];
                for (int i = 0; i < s.getItemCount(); i++) {
                    x[i] = s.getX(i).doubleValue();
                    y[i] = s.getYValue(i);
                }
            }
            break;
            case ErrorXY: {
                XYIntervalSeries s = (XYIntervalSeries) getSeries(series);
                x = new double[s.getItemCount()];
                y = new double[s.getItemCount()];
                for (int i = 0; i < s.getItemCount(); i++) {
                    x[i] = s.getX(i).doubleValue();
                    y[i] = s.getYValue(i);
                }
            }
            break;
            default:
                XYSeries s = getXYSeries(series);
                List<XYDataItem> items = s.getItems();
                x = new double[items.size()];
                y = new double[items.size()];
                for (int i = 0; i < y.length; i++) {
                    x[i] = items.get(i).getXValue();
                    y[i] = items.get(i).getYValue();
                }
        }
        return new double[][]{x, y};
    }

    @Override
    protected void onRemovedAllSeries() {
        switch (getStyle()) {
            case ErrorX:
                ((XIntervalSeriesCollection) dataY1).removeAllSeries();
                if (dataY2 != null) {
                    ((XIntervalSeriesCollection) dataY2).removeAllSeries();
                }
                break;
            case ErrorY:
                ((YIntervalSeriesCollection) dataY1).removeAllSeries();
                if (dataY2 != null) {
                    ((YIntervalSeriesCollection) dataY2).removeAllSeries();
                }
                break;
            case ErrorXY:
                ((XYIntervalSeriesCollection) dataY1).removeAllSeries();
                if (dataY2 != null) {
                    ((XYIntervalSeriesCollection) dataY2).removeAllSeries();
                }
                break;
            default:
                ((XYSeriesCollection) dataY1).removeAllSeries();
                if (dataY2 != null) {
                    ((XYSeriesCollection) dataY2).removeAllSeries();
                }
        }
    }

    @Override
    public void updateSeries(LinePlotSeries series) {
        Series s = getSeries(series);
        s.fireSeriesChanged();
    }

    XYSeries getXYSeries(LinePlotSeries series) {
        return (XYSeries) (series.getToken());
    }

    Series getSeries(LinePlotSeries series) {
        return (Series) (series.getToken());
    }

    /**
     * Get the chart panel of this plot. The chart panel will be lazily created the first time this
     * function is called
     *
     */
    @Override
    protected void createChart() {
        super.createChart();

        legendVisible = false;
        dataY1 = new XYSeriesCollection();
        // Create chart
        chart = newChart();

        // Customize legend
        chart.getLegend().setVisible(false);
        chart.getLegend().setBackgroundPaint(null);
        chart.getLegend().setBorder(0, 0, 0, 0);

        //Anti-aliasing
        setQuality(quality);

        // Lazy creation of the chart panel
        chartPanel = newChartPanel(chart);

        //Fonts will be distorted if greater than this value. Default values are 1024x728
        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMaximumDrawWidth(2000);

        // Remove border
        chartPanel.getChart().setBorderVisible(false);

        // Set size of chart
        chartPanel.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

        setBackground(getBackground());

        // Customize plot area look and feel
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(getPlotBackground());
        plot.setDomainGridlinePaint(getGridColor());
        plot.setRangeGridlinePaint(getGridColor());
        plot.setOutlinePaint(getOutlineColor());
        // Show data point
        ((XYLineAndShapeRenderer) plot.getRenderer()).setBaseShapesVisible(true);

        // Include zeros in range (x) axis
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);

        ((NumberAxis) plot.getRangeAxis()).setAutoRangeMinimumSize(AUTO_RANGE_MINIMUM_SIZE);
        ((NumberAxis) plot.getDomainAxis()).setAutoRangeMinimumSize(AUTO_RANGE_MINIMUM_SIZE);
        //((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(new StandardTickUnitSource()); //Scirntific notation
        //((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(new NumberTickUnitSource()); //Scirntific notation if more than 5 deciamals

        //This is to avoid an exception with JFreeChart 1.0.19 when using NumberTickUnitSource and zooming to dx=0
        ((NumberAxis) plot.getDomainAxis()).setStandardTickUnits(new NumberTickUnitSource() {
            @Override
            public TickUnit getCeilingTickUnit(double size) {
                if (Double.isInfinite(size)) {
                    return super.getCeilingTickUnit(AUTO_RANGE_MINIMUM_SIZE);
                }
                return super.getCeilingTickUnit(size);
            }
        });

        //Activate (arrow) keys
        addKeyBindings();

        setLayout(new BorderLayout());
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(getAxisTextColor());
        }
        chart.getLegend().setItemPaint(getAxisTextColor());
        plot.getDomainAxis().setTickLabelPaint(getAxisTextColor());
        plot.getDomainAxis().setLabelPaint(getAxisTextColor());
        plot.getRangeAxis().setLabelPaint(getAxisTextColor());
        plot.getRangeAxis().setTickLabelPaint(getAxisTextColor());
        plot.getDomainAxis().setLabelFont(LABEL_FONT);
        plot.getRangeAxis().setLabelFont(LABEL_FONT);
        plot.getDomainAxis().setTickLabelFont(TICK_LABEL_FONT);
        plot.getRangeAxis().setTickLabelFont(TICK_LABEL_FONT);
        add(chartPanel);
    }

    Style style;

    @Override
    public Style getStyle() {
        if (style == null) {
            return Style.Normal;
        }
        return style;
    }

    @Override
    public void setStyle(Style style) {
        class SeriesInfo {

            LinePlotSeries series;
            double[] x;
            double[] y;
        }
        if (style != getStyle()) {
            boolean hasY2 = (dataY2 != null);
            LinePlotSeries[] allSeries = getAllSeries();
            String labelX = getAxis(Plot.AxisId.X).getLabel();
            String labelY = getAxis(Plot.AxisId.Y).getLabel();
            String labelY2 = getAxis(Plot.AxisId.Y2).getLabel();
            SeriesInfo[] existing = new SeriesInfo[allSeries.length];
            for (int i = 0; i < allSeries.length; i++) {
                SeriesInfo info = new SeriesInfo();
                LinePlotSeries series = allSeries[i];
                info.series = series;
                info.x = series.getX();
                info.y = series.getY();
                existing[i] = info;
            }
            remove(chartPanel);
            for (LinePlotSeries series : allSeries) {
                removeSeries(series);
            }
            dataY2 = null;
            this.style = style;
            createChart();
            if (hasY2) {
                createY2();
            }
            createPopupMenu();
            getAxis(Plot.AxisId.X).setLabel(labelX);
            getAxis(Plot.AxisId.Y).setLabel(labelY);
            getAxis(Plot.AxisId.Y2).setLabel(labelY2);
            onTitleChanged();
            for (SeriesInfo info : existing) {
                if (!getStyle().isError()) {
                    addSeries(info.series);
                    info.series.setData(info.x, info.y);
                }
            }
        }
    }

    @Override
    protected void onTitleChanged() {
        chartPanel.setName(getTitle());
        chart.setTitle(getTitle());
        if ((getTitleFont() != null) && (chart.getTitle() != null)) {
            chart.getTitle().setFont(getTitleFont());
        }
    }

    @Override
    protected void onAxisLabelChanged(AxisId axis) {
        switch (axis) {
            case X:
                chart.getXYPlot().getDomainAxis().setLabel(getAxis(AxisId.X).getLabel());
                break;
            case Y:
                chart.getXYPlot().getRangeAxis().setLabel(getAxis(AxisId.Y).getLabel());
                break;
            case Y2:
                createY2();
                chart.getXYPlot().getRangeAxis(1).setLabel(getAxis(AxisId.Y2).getLabel());
                break;
        }
    }

    protected ValueAxis getValueAxis(AxisId axisId) {
        switch (axisId) {
            case X:
                return chart.getXYPlot().getDomainAxis();
            case Y:
                return chart.getXYPlot().getRangeAxis();
            case Y2:
                createY2();
                return chart.getXYPlot().getRangeAxis(1);
            default:
                return null;
        }
    }

    final HashMap<AxisId, Range> ranges = new HashMap<>();

    @Override
    protected void onAxisRangeChanged(AxisId axisId) {
        ValueAxis axis = getValueAxis(axisId);
        if (axis != null) {
            boolean isLog = axis instanceof LogarithmicAxis;
            if (isLog != getAxis(axisId).isLogarithmic()) {
                axis = (getAxis(axisId).isLogarithmic()) ? new LogarithmicAxis(getAxis(axisId).getLabel()) : new NumberAxis(getAxis(axisId).getLabel());
                XYPlot plot = (XYPlot) chart.getPlot();
                axis.setLabelFont(LABEL_FONT);
                axis.setTickLabelFont(TICK_LABEL_FONT);
                axis.setLabelPaint(getAxisTextColor());
                axis.setTickLabelPaint(getAxisTextColor());
                switch (getAxis(axisId).id) {
                    case X:
                        plot.setDomainAxis(0, axis);
                        break;
                    case Y:
                        plot.setRangeAxis(0, axis);
                        break;
                    case Y2:
                        plot.setRangeAxis(1, axis);
                        break;
                }
            }
            if (getAxis(axisId).isAutoRange()) {
                axis.setAutoRange(true);
                ranges.remove(axisId);
            } else {
                Range range = new Range(getAxis(axisId).getMin(), getAxis(axisId).getMax());
                axis.setRange(range, true, true);
                ranges.put(axisId, range);
            }
        }
    }

    protected ChartPanel newChartPanel(JFreeChart chart) {
        return new ChartPanel(chart) {
            //TODO: This is to fix JFreeChart bug zooming out when range has been set. 
            //JFreeChart is then performin an auto range which is unexpected.
            //http://www.jfree.org/phpBB2/viewtopic.php?t=23763
            @Override
            public void restoreAutoDomainBounds() {
                super.restoreAutoDomainBounds();
                for (AxisId axisId : ranges.keySet()) {
                    final ValueAxis axis = getValueAxis(axisId);
                    final Range range = ranges.get(axisId);
                    SwingUtilities.invokeLater(() -> {
                        axis.setRange(range);
                    });
                }
            }
        };
    }

    protected JFreeChart newChart() {
        JFreeChart ret = null;
        switch (getStyle()) {
            case Step:
                ret = ChartFactory.createXYStepChart(getTitle(), getAxis(AxisId.X).getLabel(), getAxis(AxisId.Y).getLabel(), dataY1, PlotOrientation.VERTICAL, true, tooltips, false);
                ret.getXYPlot().setDomainAxis(new NumberAxis());
                break;
            case Spline: {
                NumberAxis xAxis = new NumberAxis(getAxis(AxisId.X).getLabel());
                xAxis.setAutoRangeIncludesZero(false);
                NumberAxis yAxis = new NumberAxis(getAxis(AxisId.Y).getLabel());
                XYSplineRenderer renderer = new XYSplineRenderer();
                XYPlot plot = new XYPlot(dataY1, xAxis, yAxis, renderer);
                plot.setOrientation(PlotOrientation.VERTICAL);
                if (tooltips) {
                    renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
                }
                ret = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
                ChartFactory.getChartTheme().apply(ret);
            }
            break;
            case ErrorX:
            case ErrorY:
            case ErrorXY:
                if (getStyle() == Style.ErrorX) {
                    dataY1 = new XIntervalSeriesCollection();
                }
                if (getStyle() == Style.ErrorY) {
                    dataY1 = new YIntervalSeriesCollection();
                }
                if (getStyle() == Style.ErrorXY) {
                    dataY1 = new XYIntervalSeriesCollection();
                }

                NumberAxis xAxis = new NumberAxis(getAxis(AxisId.X).getLabel());
                xAxis.setAutoRangeIncludesZero(false);
                NumberAxis yAxis = new NumberAxis(getAxis(AxisId.Y).getLabel());
                XYErrorRenderer renderer = new XYErrorRenderer();
                XYPlot plot = new XYPlot(dataY1, xAxis, yAxis, renderer);
                plot.setOrientation(PlotOrientation.VERTICAL);
                if (tooltips) {
                    renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
                }
                ret = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
                ChartFactory.getChartTheme().apply(ret);
                break;
            default:
                ret = ChartFactory.createXYLineChart(getTitle(), getAxis(AxisId.X).getLabel(), getAxis(AxisId.Y).getLabel(), dataY1, PlotOrientation.VERTICAL, true, tooltips, false);
        }

        return ret;
    }

    /**
     * Change visible part of chart
     *
     * @param translationVector
     */
    void moverOverPlot(XYDataItem translationVector) {
        double translatedDomainIntervalMin = chart.getXYPlot().getDomainAxis().getRange().getLowerBound() + translationVector.getX().doubleValue();
        double translatedDomainIntervalMax = chart.getXYPlot().getDomainAxis().getRange().getUpperBound() + translationVector.getX().doubleValue();
        double translatedRangeIntervalMin = chart.getXYPlot().getRangeAxis().getRange().getLowerBound() + translationVector.getY().doubleValue();
        double translatedRangeIntervalMax = chart.getXYPlot().getRangeAxis().getRange().getUpperBound() + translationVector.getY().doubleValue();

        Range domainAxisRange = new Range(translatedDomainIntervalMin, translatedDomainIntervalMax);
        Range rangeAxisRange = new Range(translatedRangeIntervalMin, translatedRangeIntervalMax);
        //We set notify to false in the first call..
        chart.getXYPlot().getDomainAxis().setRange(domainAxisRange, true, false);
        //...and true in the last
        chart.getXYPlot().getRangeAxis().setRange(rangeAxisRange, true, true);

    }

    /**
     * Add key bindings to chart panel
     */
    protected void addKeyBindings() {

        final String moveUpKey = "move up";
        final String moveDownKey = "move down";
        final String moveRightKey = "move right";
        final String moveLeftKey = "move left";

        // Up arrow
        chartPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), moveUpKey);
        chartPanel.getActionMap().put(moveUpKey, new AbstractAction() {
            // Default serial id
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                moverOverPlot(new XYDataItem(0.0, ((NumberAxis) chart.getXYPlot().getRangeAxis()).getTickUnit().getSize()));
            }
        });

        // Down arrow
        chartPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), moveDownKey);
        chartPanel.getActionMap().put(moveDownKey, new AbstractAction() {
            // Default serial id
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                moverOverPlot(new XYDataItem(0.0, -((NumberAxis) chart.getXYPlot().getRangeAxis()).getTickUnit().getSize()));
            }
        });

        // Right arrow
        chartPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), moveRightKey);
        chartPanel.getActionMap().put(moveRightKey, new AbstractAction() {
            // Default serial id
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                moverOverPlot(new XYDataItem(((NumberAxis) chart.getXYPlot().getDomainAxis()).getTickUnit().getSize(), 0.0));
            }
        });

        // Left arrow
        chartPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), moveLeftKey);
        chartPanel.getActionMap().put(moveLeftKey, new AbstractAction() {
            // Default serial id
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                moverOverPlot(new XYDataItem(-((NumberAxis) chart.getXYPlot().getDomainAxis()).getTickUnit().getSize(), 0.0));
            }
        });

        chartPanel.setFocusable(true);
        //This is for the chart to answer keyboard commands
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                chartPanel.requestFocus();
            }
        });
        chartPanel.setMouseZoomable(true, true);
        //chartPanel.setMouseWheelEnabled(true);        
    }

    ArrayList averageMarkers;

    /**
     * Add additional items to the context menu of the plot
     */
    @Override
    protected void createPopupMenu() {
        //Remove copy/save as/print menus: copy is buggy, save is limited 
        for (int index = 6; index >= 2; index--) {
            chartPanel.getPopupMenu().remove(index);
        }

        JMenu toolsMenu = new JMenu("Tools");
        chartPanel.getPopupMenu().add(toolsMenu);

        // Mark average
        JMenuItem averageMenuItem = new JMenuItem("Average");
        averageMenuItem.addActionListener((ActionEvent e) -> {
            if (averageMarkers != null) {
                for (Object marker : averageMarkers) {
                    removeMarker(marker);
                }
            }
            averageMarkers = new ArrayList();
            for (LinePlotSeries series : getAllSeries()) {
                double a = series.getAverage();
                averageMarkers.add(addMarker(a, AxisId.Y, "Average: " + String.format("%.6f", a), getSeriesColor(series)));
            }
        });
        toolsMenu.add(averageMenuItem);

        // Mark minimum value
        JMenuItem minimumMenuItem = new JMenuItem("Minimum");
        minimumMenuItem.addActionListener((ActionEvent e) -> {
            // Remove all annotation for the series
            removePointers();
            for (Object o : chartPanel.getChart().getXYPlot().getAnnotations()) {
                chartPanel.getChart().getXYPlot().removeAnnotation((XYAnnotation) o);
            }
            for (LinePlotSeries series : getAllSeries()) {
                double[] min = series.getMinimum();
                XYDrawableAnnotation cross = new XYDrawableAnnotation(min[0], min[1], 10.0, 10.0, new CrossAnnotation(getSeriesColor(series)));
                cross.setToolTipText("Minimum: " + min[0] + " / " + min[1]);
                chartPanel.getChart().getXYPlot().addAnnotation(cross);
            }
        });
        toolsMenu.add(minimumMenuItem);

        // Mark maximum value
        JMenuItem maximumMenuItem = new JMenuItem("Maximum");
        maximumMenuItem.addActionListener((ActionEvent e) -> {
            // Remove all annotation for the series
            removePointers();
            for (Object o : chartPanel.getChart().getXYPlot().getAnnotations()) {
                chartPanel.getChart().getXYPlot().removeAnnotation((XYAnnotation) o);
            }
            for (LinePlotSeries series : getAllSeries()) {
                double[] max = series.getMaximum();
                XYDrawableAnnotation cross = new XYDrawableAnnotation(max[0], max[1], 10.0, 10.0, new CrossAnnotation(getSeriesColor(series)));
                cross.setToolTipText("Maximum: " + max[0] + " / " + max[1]);
                chartPanel.getChart().getXYPlot().addAnnotation(cross);
            }
        });
        toolsMenu.add(maximumMenuItem);

        // Show derivative
        JMenuItem derivativeMenuItem = new JMenuItem("Derivative");
        derivativeMenuItem.addActionListener((ActionEvent e) -> {
            LinePlotJFree p = new LinePlotJFree();
            String title = ((getTitle() != null) && (getTitle().length() > 0)) ? "Derivative - " + getTitle() : "Derivative";
            //p.setTitleFont(getTitleFont());
            //p.setTitle(((getTitle()!=null) && (getTitle().length()>0)) ? "Derivative - " + getTitle() : "Derivative");            
            p.setTitle(null);
            p.getAxis(AxisId.X).setLabel(getAxis(AxisId.X).getLabel());
            p.getAxis(AxisId.Y).setLabel(getAxis(AxisId.Y).getLabel());
            for (LinePlotSeries series : getAllSeries()) {
                double[][] derivative = series.getDerivative();
                LinePlotSeries dseries = new LinePlotSeries(series.getName() + "'");
                p.addSeries(dseries);
                dseries.setData(derivative[0], derivative[1]);
            }
            Frame frame = SwingUtils.getFrame(this);
            JDialog dlg = new JDialog(frame, title, false);
            p.setPreferredSize(new Dimension(DETACHED_WIDTH, DETACHED_HEIGHT));
            dlg.setContentPane(p);
            dlg.pack();
            SwingUtils.centerComponent(frame, dlg);
            dlg.setVisible(true);
            dlg.requestFocus();
        });
        toolsMenu.add(derivativeMenuItem);

        // Show integral
        JMenuItem integralMenuItem = new JMenuItem("Integral");
        integralMenuItem.addActionListener((ActionEvent e) -> {
            // Show new frame
            LinePlotJFree p = new LinePlotJFree();
            String title = ((getTitle() != null) && (getTitle().length() > 0)) ? "Integral - " + getTitle() : "Integral";
            //p.setTitleFont(getTitleFont());
            //p.setTitle(title);
            p.setTitle(null);
            p.getAxis(AxisId.X).setLabel(getAxis(AxisId.X).getLabel());
            p.getAxis(AxisId.Y).setLabel(getAxis(AxisId.Y).getLabel());
            for (LinePlotSeries series : getAllSeries()) {
                double[][] integral = series.getIntegral();
                LinePlotSeries dseries = new LinePlotSeries("integral-" + series.getName());
                p.addSeries(dseries);
                dseries.setData(integral[0], integral[1]);
            }
            Frame frame = SwingUtils.getFrame(this);
            JDialog dlg = new JDialog(frame, title, false);
            p.setPreferredSize(new Dimension(DETACHED_WIDTH, DETACHED_HEIGHT));
            dlg.setContentPane(p);
            dlg.pack();
            SwingUtils.centerComponent(frame, dlg);
            dlg.setVisible(true);
            dlg.requestFocus();
        });
        toolsMenu.add(integralMenuItem);

        //Show hide legend
        JCheckBoxMenuItem contextMenuItemColorLegendVisible = new JCheckBoxMenuItem("Show Legend");
        contextMenuItemColorLegendVisible.addActionListener((ActionEvent e) -> {
            setLegendVisible(contextMenuItemColorLegendVisible.isSelected());
        });
        chartPanel.getPopupMenu().add(contextMenuItemColorLegendVisible);

        // Show hide tooltips
        JCheckBoxMenuItem contextMenuItemToolTip = new JCheckBoxMenuItem("Show Tooltips");
        contextMenuItemToolTip.addActionListener((ActionEvent e) -> {
            setShowTooltips(contextMenuItemToolTip.isSelected());
        });
        chartPanel.getPopupMenu().add(contextMenuItemToolTip);

        // Show hide pointer
        JCheckBoxMenuItem contextMenuPointer = new JCheckBoxMenuItem("Show Pointer");
        contextMenuPointer.addActionListener((ActionEvent e) -> {
            setPointerVisible(contextMenuPointer.isSelected());
        });
        chartPanel.getPopupMenu().add(contextMenuPointer);

        chartPanel.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                contextMenuItemColorLegendVisible.setSelected(isLegendVisible());
                contextMenuItemToolTip.setSelected(getShowTooltips());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        super.createPopupMenu();
    }

    // http://www.jfree.org/phpBB2/viewtopic.php?t=12788&highlight=redraw+speed+performance+problem
    void showTooltips() {
        tooltips = true;
        DecimalFormat dm = new DecimalFormat("0.##########");
        getRenderer(1).setBaseToolTipGenerator(new StandardXYToolTipGenerator("x={1} y={2}", dm, dm));
        if (getRenderer(2) != null) {
            getRenderer(2).setBaseToolTipGenerator(new StandardXYToolTipGenerator("x={1} y={2}", dm, dm));
        }

        chartPanel.setDisplayToolTips(true);
        chartPanel.getChartRenderingInfo().setEntityCollection(new StandardEntityCollection());

        //If marker size smaller than 2 then tooltips are unusable
        if (getMarkerSize() < 3) {
            changeMarkerSize(3);
        }
    }

    void hideTooltips() {
        tooltips = false;
        chartPanel.getChartRenderingInfo().setEntityCollection(null);
        getRenderer(1).setBaseToolTipGenerator(null);
        if (getRenderer(2) != null) {
            getRenderer(2).setBaseToolTipGenerator(null);
        }
        if (getMarkerSize() < 3) {
            changeMarkerSize(getMarkerSize());
        }
    }

    public void setLegendVisible(boolean value) {
        if (value != legendVisible) {
            legendVisible = value;
            chart.getLegend().setVisible(legendVisible);
        }
    }

    public boolean isLegendVisible() {
        return legendVisible;
    }

    public void setShowTooltips(boolean value) {
        if (value != showTooltips) {
            showTooltips = value;
            if (value) {
                showTooltips();
            } else {
                hideTooltips();
            }
        }
    }

    public boolean getShowTooltips() {
        return showTooltips;
    }

    @Override
    public void addPopupMenuItem(final JMenuItem item) {
        if (item == null) {
            chartPanel.getPopupMenu().addSeparator();
        } else {
            chartPanel.getPopupMenu().add(item);
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        return chartPanel.print(g, pf, page);
    }

    @Override
    protected Color getSeriesColor(LinePlotSeries series) {
        XYItemRenderer renderer = getRenderer(series.getAxisY());
        Paint p = renderer.getSeriesPaint(getSeriesIndex(series));
        return p instanceof Color ? (Color) p : null;
    }

    @Override
    protected void setSeriesColor(LinePlotSeries series, Color color) {
        XYLineAndShapeRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesPaint(getSeriesIndex(series), color);
    }

    @Override
    protected void setLinesVisible(LinePlotSeries series, boolean value) {
        XYLineAndShapeRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesLinesVisible(getSeriesIndex(series), value);
    }

    @Override
    protected void setLineWidth(LinePlotSeries series, int value) {
        XYLineAndShapeRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesStroke(getSeriesIndex(series), new BasicStroke(value));
    }

    @Override
    protected void setPointsVisible(LinePlotSeries series, boolean value) {
        XYLineAndShapeRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesShapesVisible(getSeriesIndex(series), value);
    }

    @Override
    protected void setPointSize(LinePlotSeries series, int size) {
        XYItemRenderer renderer = getRenderer(series.getAxisY());
        int index = getSeriesIndex(series);
        if (index >= 0) {
            Rectangle2D.Double marker = new Rectangle2D.Double(-size / 2, -size / 2, size, size);
            renderer.setSeriesShape(index, marker);
        }
    }

    @Override
    public Object addMarker(double val, final AxisId axis, String label, Color color) {
        if (color == null) {
            color = Color.DARK_GRAY;
        }
        final ValueMarker marker = new ValueMarker(val, color, new BasicStroke(1));
        marker.setLabel(label);
        marker.setLabelPaint(color);
        marker.setLabelAnchor(RectangleAnchor.CENTER);
        marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
        SwingUtilities.invokeLater(() -> {
            if ((axis == null) || (axis == AxisId.X)) {
                chartPanel.getChart().getXYPlot().addDomainMarker(marker, Layer.FOREGROUND);
            } else {
                chartPanel.getChart().getXYPlot().addRangeMarker(marker, Layer.FOREGROUND);
            }
        });
        return marker;
    }

    @Override
    public Object addIntervalMarker(double start, double end, final AxisId axis, String label, Color color) {
        if (color == null) {
            color = Color.DARK_GRAY;
        }

        Color outlineColor = MainFrame.isDark() ? color.brighter().brighter() : color.darker().darker();

        final IntervalMarker marker = new IntervalMarker(start, end, color);

        if (label != null) {
            marker.setLabel(label);
        }

        marker.setLabelPaint(outlineColor);
        marker.setLabelAnchor(RectangleAnchor.CENTER);
        marker.setOutlineStroke(new BasicStroke(1f));
        marker.setStroke(new BasicStroke(1f));
        marker.setOutlinePaint(outlineColor);

        SwingUtilities.invokeLater(() -> {
            if ((axis == null) || (axis == AxisId.X)) {
                chartPanel.getChart().getXYPlot().addDomainMarker(marker, Layer.FOREGROUND);
            } else {
                chartPanel.getChart().getXYPlot().addRangeMarker(marker, Layer.FOREGROUND);
            }
        });
        return marker;
    }

    @Override
    public void removeMarker(final Object marker) {
        SwingUtilities.invokeLater(() -> {
            if (marker == null) {
                Collection<?> c = chartPanel.getChart().getXYPlot().getRangeMarkers(Layer.FOREGROUND);
                if (c != null) {
                    Marker[] markers = c.toArray(new Marker[0]);
                    for (Marker m : markers) {
                        chartPanel.getChart().getXYPlot().removeRangeMarker((Marker) m);
                    }
                }
                c = chartPanel.getChart().getXYPlot().getDomainMarkers(Layer.FOREGROUND);
                if (c != null) {
                    Marker[] markers = c.toArray(new Marker[0]);
                    for (Marker m : markers) {
                        chartPanel.getChart().getXYPlot().removeDomainMarker((Marker) m);
                    }
                }
            } else {
                chartPanel.getChart().getXYPlot().removeDomainMarker((Marker) marker, Layer.FOREGROUND);
                chartPanel.getChart().getXYPlot().removeRangeMarker((Marker) marker, Layer.FOREGROUND);
            }
        });
    }

    @Override
    public List getMarkers() {
        List ret = new ArrayList();
        Collection dm = chartPanel.getChart().getXYPlot().getDomainMarkers(Layer.FOREGROUND);
        Collection rm = chartPanel.getChart().getXYPlot().getRangeMarkers(Layer.FOREGROUND);
        if (dm != null) {
            ret.addAll(dm);
        }
        if (rm != null) {
            ret.addAll(rm);
        }
        return ret;
    }

    @Override
    public Object addText(double x, double y, String label, Color color) {
        XYTextAnnotation annotation = new XYTextAnnotation(label, x, y);
        if (color != null) {
            annotation.setPaint(color);
        }
        chartPanel.getChart().getXYPlot().addAnnotation(annotation);
        return annotation;
    }

    @Override
    public void removeText(Object text) {
        if ((text != null) && (text instanceof XYTextAnnotation)) {
            chartPanel.getChart().getXYPlot().removeAnnotation((XYTextAnnotation) text);
        }
    }

    @Override
    public List getTexts() {
        List ret = new ArrayList();
        ret.addAll(chartPanel.getChart().getXYPlot().getAnnotations());
        return ret;
    }

    ChartMouseListener mouseListener;
    XYPointerAnnotation[] pointers;

    void removePointers() {
        if (pointers != null) {
            for (XYPointerAnnotation pointer : pointers) {
                chartPanel.getChart().getXYPlot().removeAnnotation(pointer);
            }
            pointers = null;
        }

    }

    //TODO: Supporting multiple series, but not on Y2
    public void setPointerVisible(boolean value) {
        if (value != isPointerVisible()) {
            if (value) {
                mouseListener = new ChartMouseListener() {

                    @Override
                    public void chartMouseClicked(ChartMouseEvent event) {
                    }

                    @Override
                    public void chartMouseMoved(ChartMouseEvent event) {
                        Rectangle2D dataArea = chartPanel.getScreenDataArea();
                        JFreeChart chart = event.getChart();
                        XYPlot plot = (XYPlot) chart.getPlot();
                        int series = plot.getSeriesCount();
                        if ((pointers == null) || (pointers.length != series)) {
                            removePointers();
                            pointers = new XYPointerAnnotation[series];
                        }
                        ValueAxis xAxis = plot.getDomainAxis();
                        ValueAxis yAxis = plot.getRangeAxis();
                        double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
                        double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

                        for (int i = 0; i < series; i++) {
                            double f = DatasetUtilities.findYValue(plot.getDataset(), i, x);
                            String text = String.format("x=%s y=%s", getDisplayableValue(x), getDisplayableValue(f));
                            if (pointers[i] == null) {
                                if (mouseListener == null) {
                                    return;
                                }
                                pointers[i] = new XYPointerAnnotation(text, x, f, Math.toRadians(270));
                                pointers[i].setArrowPaint(Color.DARK_GRAY);
                                pointers[i].setBaseRadius(0);
                                pointers[i].setArrowLength(5);
                                pointers[i].setTipRadius(10);
                                pointers[i].setLabelOffset(25);
                                chartPanel.getChart().getXYPlot().addAnnotation(pointers[i]);
                            } else {
                                pointers[i].setText(text);
                                pointers[i].setX(x);
                                pointers[i].setY(f);
                                pointers[i].setAngle((y > f) ? Math.toRadians(270) : Math.toRadians(90));
                            }

                        }
                    }
                };
                chartPanel.addChartMouseListener(mouseListener);
            } else {
                chartPanel.removeChartMouseListener(mouseListener);
                mouseListener = null;
                removePointers();
            }
        }

    }

    public boolean isPointerVisible() {
        return mouseListener != null;
    }

    @Reflection.Hidden
    public JFreeChart getChart() {
        return chart;
    }

}
