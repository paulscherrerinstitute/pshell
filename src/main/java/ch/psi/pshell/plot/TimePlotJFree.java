package ch.psi.pshell.plot;

import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.utils.Chrono;
import ch.psi.utils.IO;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

/**
 *
 */
public class TimePlotJFree extends TimePlotBase {

    final ArrayList<TimeSeries> series;
    final JFreeChart chart;
    final TimeSeriesCollection data;
    final ChartPanel chartPanel;
    final Shape marker;

    public TimePlotJFree() {
        super();
        data = new TimeSeriesCollection();
        marker = new Rectangle2D.Double(-1, -1, 2, 2);
        chart = ChartFactory.createTimeSeriesChart(null,
                "Time",
                null,
                data,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setVisible(false);
        chart.getLegend().setBackgroundPaint(null);
        chart.getLegend().setBorder(0, 0, 0, 0);

        //Anti-aliasing
        setQuality(quality);

        final XYPlot plot = chart.getXYPlot();
        ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);  // 60 seconds
        axis = plot.getRangeAxis();
        axis.setAutoRange(true);
        plot.setBackgroundPaint(LinePlotBase.getPlotBackground());
        plot.setDomainGridlinePaint(LinePlotBase.getGridColor());
        plot.setRangeGridlinePaint(LinePlotBase.getGridColor());
        plot.setOutlinePaint(LinePlotBase.getOutlineColor());
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(PlotBase.getAxisTextColor());
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemPaint(LinePlotBase.getAxisTextColor());
        }
        plot.getDomainAxis().setTickLabelPaint(PlotBase.getAxisTextColor());
        plot.getDomainAxis().setLabelPaint(PlotBase.getAxisTextColor());
        plot.getRangeAxis().setLabelPaint(PlotBase.getAxisTextColor());
        plot.getRangeAxis().setTickLabelPaint(PlotBase.getAxisTextColor());
        plot.getDomainAxis().setLabelFont(LinePlotJFree.LABEL_FONT);
        plot.getRangeAxis().setLabelFont(LinePlotJFree.LABEL_FONT);
        plot.getDomainAxis().setTickLabelFont(LinePlotJFree.TICK_LABEL_FONT);
        plot.getRangeAxis().setTickLabelFont(LinePlotJFree.TICK_LABEL_FONT);

        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeMinimumSize(LinePlotJFree.AUTO_RANGE_MINIMUM_SIZE);
        //((NumberAxis)plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0.0######"));
        //((NumberAxis)plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("####0.000"));

        setLayout(new java.awt.BorderLayout());
        chartPanel = new ChartPanel(chart) {

            @Override
            public void restoreAutoDomainBounds() {
                super.restoreAutoDomainBounds();
                if (rangeY1 != null) {
                    SwingUtilities.invokeLater(() -> {
                        ValueAxis axis = chart.getXYPlot().getRangeAxis();
                        axis.setRange(rangeY1);
                    });
                }
                if (rangeY2 != null) {
                    SwingUtilities.invokeLater(() -> {
                        ValueAxis axis = chart.getXYPlot().getRangeAxis(1);
                        axis.setRange(rangeY2, true, true);
                    });
                }
            }
        };
        add(chartPanel);
        chartPanel.setMaximumDrawHeight(2000); //Fonts will be distorted if greater than this value 
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setBackground(getBackground());
        series = new ArrayList<>();
        setAxisSize(axisSize);
        setup();
        //chartPanel.setMouseZoomable(true, true);
    }
        
    @Override
    protected void setupMenus() {
        super.setupMenus();
        JMenuItem menuSaveTxt = new JMenuItem("TXT...");
        menuSaveTxt.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(new ExtensionFileFilter("TXT files (*.txt)", new String[]{"txt"}));
            chooser.setAcceptAllFileFilterUsed(true);
            if (chooser.showSaveDialog(TimePlotJFree.this) == JFileChooser.APPROVE_OPTION) {
                String fileName = chooser.getSelectedFile().getAbsolutePath();
                if (IO.getExtension(fileName).isEmpty()) {
                    fileName += ".txt";
                }
                try {
                    saveData(fileName);
                } catch (IOException ex) {
                    SwingUtils.showException(TimePlotJFree.this, ex);
                }
            }

        });
        try {
            JMenu menuSave = (JMenu) chartPanel.getPopupMenu().getComponent(3);
            menuSave.add(menuSaveTxt);
        } catch (Exception ex) {
            Logger.getLogger(TimePlotJFree.class.getName()).log(Level.INFO, null, ex);
        }
    }

    @Override
    public void saveData(String filename) throws IOException {
        String data = getDataAsString();
        if (data != null) {
            Files.write(Paths.get(filename), data.getBytes());
        }
    }

    @Override
    protected String getDataAsString() {
        StringBuilder str = new StringBuilder(1024);
        for (int i = 0; i < series.size(); i++) {
            List<TimestampedValue<Double>> values = getSeriestData(i);
            str.append("#Series: ").append(getSeriesName(i)).append(PlotBase.LINE_SEPARATOR);
            Double last = null;
            for (TimestampedValue<Double> item : values) {
                Double val = item.getValue();
                if (val == null) {
                    val = Double.NaN;
                }
                if (!val.equals(last)) {
                    last = val;
                    str.append(Chrono.getTimeStr(item.getTimestamp(), "dd/MM/YY HH:mm:ss.SSS "));
                    str.append(String.valueOf(val));
                    str.append(PlotBase.LINE_SEPARATOR);
                }
            }
            str.append(PlotBase.LINE_SEPARATOR);
        }
        return str.toString();
    }

    TimeSeriesCollection dataY2;

    void createY2() {
        if (dataY2 == null) {
            XYPlot plot = (XYPlot) chart.getPlot();
            dataY2 = new TimeSeriesCollection();
            NumberAxis axis2 = new NumberAxis();
            axis2.setAutoRangeIncludesZero(false);
            ((NumberAxis) plot.getRangeAxis()).setAutoRangeMinimumSize(LinePlotJFree.AUTO_RANGE_MINIMUM_SIZE);
            axis2.setLabelFont(LinePlotJFree.LABEL_FONT);
            axis2.setTickLabelFont(LinePlotJFree.TICK_LABEL_FONT);
            plot.setRangeAxis(1, axis2);
            XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
            renderer2.setBaseShapesVisible(isMarkersVisible());
            plot.setRenderer(1, renderer2);
            plot.setDataset(1, dataY2);
            plot.mapDatasetToRangeAxis(1, 1);
            axis2.setLabelPaint(PlotBase.getAxisTextColor());
            axis2.setTickLabelPaint(PlotBase.getAxisTextColor());
            setAxisSize(axisSize);
        }
    }

    int axisSize = -1;

    @Override
    public void setAxisSize(int size) {
        axisSize = size;
        chart.getXYPlot().getRangeAxis(0).setFixedDimension(size);
        if (dataY2 != null) {
            chart.getXYPlot().getRangeAxis(1).setFixedDimension(size);
        }
    }

    @Override
    public int getAxisSize() {
        return axisSize;
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
        chart.getXYPlot().setBackgroundPaint(c);
    }
    
    @Override
    public void setPlotGridColor(Color c) {
        chart.getXYPlot().setDomainGridlinePaint(c);
        chart.getXYPlot().setRangeGridlinePaint(c);
    }    
    
    @Override
    public void setPlotOutlineColor(Color c) {
        chart.getXYPlot().setOutlinePaint(c);
    }
    
        
    @Override
    protected void doClear() {
        for (TimeSeries ts : series) {
            ts.clear();
        }
    }

    @Override
    protected Object onAddedSeries(TimePlotSeries series) {
        TimeSeries ts = new TimeSeries(series.name);
        this.series.add(ts);
        TimeSeriesCollection dataset = data;
        if (series.axis == 2) {
            createY2();
            dataset = dataY2;
        }
        checkSeriesDuration();
        dataset.addSeries(ts);
        XYPlot plot = chart.getXYPlot();
        getRenderer(series.axis).setSeriesShape(dataset.getSeriesCount() - 1, marker);
        return ts;
    }

    @Override
    protected void onRemovedSeries(TimePlotSeries series) {
        int index = getSeriesIndex(series);
        TimeSeriesCollection dataset = data;
        if (series.axis == 2) {
            createY2();
            dataset = dataY2;
        }
        dataset.removeSeries(index);
        this.series.remove(series);
    }

    @Override
    public void updateSeries(TimePlotSeries series) {
        if (isShowing()) {
            repaint();
        }
    }

    @Override
    protected void onRemovedAllSeries() {
        data.removeAllSeries();
        series.clear();
    }

    @Override
    protected void addDataPoint(int graphIndex, long time, double value) {
        series.get(graphIndex).addOrUpdate(new FixedMillisecond(new Date(time)), (Double.isNaN(value)) ? null : value);
    }

    @Override
    public List<TimestampedValue<Double>> getSeriestData(int index) {
        TimeSeries s = series.get(index);
        List<TimestampedValue<Double>> ret = new ArrayList<>();
        Number last = null;
        for (TimeSeriesDataItem item : (List<TimeSeriesDataItem>) s.getItems()) {
            Double val;
            if (item.getValue() != null) {
                val = item.getValue().doubleValue();
            } else {
                val = Double.NaN;
            }

            if (!val.equals(last)) {
                last = val;
                ret.add(new TimestampedValue<Double>(val, item.getPeriod().getMiddleMillisecond()));
            }
        }
        return ret;
    }

    @Override
    public String getSeriesName(int index) {
        TimeSeries s = series.get(index);
        return String.valueOf(s.getKey());
    }

    @Override
    public void addPopupMenuItem(final JMenuItem item) {
        if (chartPanel != null) {
            if (item == null) {
                chartPanel.getPopupMenu().addSeparator();
            } else {
                chartPanel.getPopupMenu().add(item);
            }
        }
    }

    @Override
    protected JPopupMenu getPopupMenu() {
        return chartPanel.getPopupMenu();
    }

    @Override
    protected void doSetMarkersVisible(boolean visible) {
        XYPlot plot = (XYPlot) chart.getPlot();
        getRenderer(1).setBaseShapesVisible(visible);
        if (dataY2 != null) {
            getRenderer(2).setBaseShapesVisible(visible);
        }
    }

    @Override
    protected void doSetLegendVisible(boolean visible) {
        if (chart.getLegend() != null) {
            chart.getLegend().setVisible(visible);
        }
    }

    @Override
    public int getDurationMillis() {
        final XYPlot plot = chart.getXYPlot();
        ValueAxis axis = plot.getDomainAxis();
        return (int) axis.getFixedAutoRange();
    }

    @Override
    public void setDurationMillis(int duration) {
        final XYPlot plot = chart.getXYPlot();
        ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(duration);
        checkSeriesDuration();
    }

    @Override
    public void start() {
        super.start();
        checkSeriesDuration();
    }

    @Override
    public void stop() {
        super.stop();
        checkSeriesDuration();
    }

    void checkSeriesDuration() {
        int duration = getDurationMillis();
        if (isStarted() && (duration > 0)) {
            for (TimeSeries ts : series) {
                ts.setMaximumItemAge(duration + 10000); //Can connect to last removed point, but only up to 10s 
            }
        } else {
            //Preserve points if paused
            for (TimeSeries ts : series) {
                ts.setMaximumItemAge(Long.MAX_VALUE);
            }
        }
    }

    @Override
    public void setTimeAxisLabel(String label) {
        final XYPlot plot = chart.getXYPlot();
        ValueAxis axis = plot.getDomainAxis();
        axis.setLabel(label);
    }

    @Override
    public String getTimeAxisLabel() {
        final XYPlot plot = chart.getXYPlot();
        ValueAxis axis = plot.getDomainAxis();
        return axis.getLabel();
    }

    Range rangeY1;

    @Override
    public void setY1AxisAutoScale() {
        rangeY1 = null;
        ValueAxis axis = chart.getXYPlot().getRangeAxis();
        axis.setAutoRange(true);
    }

    @Override
    public void setY1AxisScale(double min, double max) {
        rangeY1 = new Range(min, max);
        ValueAxis axis = chart.getXYPlot().getRangeAxis();
        axis.setRange(rangeY1, true, true);
    }

    Range rangeY2;

    @Override
    public void setY2AxisAutoScale() {
        createY2();
        rangeY2 = null;
        ValueAxis axis = chart.getXYPlot().getRangeAxis(1);
        axis.setAutoRange(true);
    }

    @Override
    public void setY2AxisScale(double min, double max) {
        createY2();
        rangeY2 = new Range(min, max);
        ValueAxis axis = chart.getXYPlot().getRangeAxis(1);
        axis.setRange(rangeY2, true, true);
    }

    XYLineAndShapeRenderer getRenderer(int axis) {
        XYPlot plot = chart.getXYPlot();
        if (axis == 2) {
            return (XYLineAndShapeRenderer) plot.getRenderer(1);
        }
        return (XYLineAndShapeRenderer) plot.getRenderer();
    }

    @Override
    public void setQuality(Quality quality) {
        super.setQuality(quality);
        if ((chart != null) && (quality != null)) {
            chart.setAntiAlias(quality.ordinal() >= Quality.High.ordinal());
        }
    }

    @Override
    protected Color getSeriesColor(TimePlotSeries series) {
        XYItemRenderer renderer = getRenderer(series.getAxisY());
        Paint p = renderer.getSeriesPaint(getSeriesIndex(series));
        return p instanceof Color ? (Color) p : null;
    }

    @Override
    protected void setSeriesColor(TimePlotSeries series, Color color) {
        XYLineAndShapeRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesPaint(getSeriesIndex(series), color);
    }

    int getSeriesIndex(TimePlotSeries series) {
        //return this.series.indexOf(series);

        TimeSeries xys = getTimeSeries(series);
        if (xys == null) {
            return -1;
        }
        TimeSeriesCollection data = (TimeSeriesCollection) getYData(series.getAxisY());
        return data.indexOf(xys);

    }

    TimeSeries getTimeSeries(TimePlotSeries series) {
        return (TimeSeries) (series.getToken());
    }

    TimeSeriesCollection getYData(int axis) {
        //Axis2  is only created if a series on Y2 is added
        if (axis == 2) {
            createY2();
            return dataY2;
        }
        return data;
    }

    @Hidden
    public JFreeChart getChart() {
        return chart;
    }
}
