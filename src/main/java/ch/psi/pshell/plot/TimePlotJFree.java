package ch.psi.pshell.plot;

import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.utils.Chrono;
import ch.psi.utils.IO;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.ImageTransferHandler;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
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
    Font tickLabelFont = TICK_LABEL_FONT;
    Font labelFont = LinePlotJFree.LABEL_FONT;
    

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
        plot.getDomainAxis().setLabelFont(labelFont);
        plot.getRangeAxis().setLabelFont(labelFont);
        plot.getDomainAxis().setTickLabelFont(tickLabelFont);
        plot.getRangeAxis().setTickLabelFont(tickLabelFont);

        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeMinimumSize(LinePlotJFree.AUTO_RANGE_MINIMUM_SIZE);
        //((NumberAxis)plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0.0######"));
        //((NumberAxis)plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("####0.000"));

        setLayout(new java.awt.BorderLayout());
        chartPanel = new ChartPanel(chart, getOffscreenBuffer()) {

            @Override
            public void restoreAutoRangeBounds() {
                super.restoreAutoRangeBounds();
                if (rangeY1 != null) {
                    ValueAxis axis = chart.getXYPlot().getRangeAxis();
                    axis.setRange(rangeY1);
                }
                if (rangeY2 != null) {
                    ValueAxis axis = chart.getXYPlot().getRangeAxis(1);
                    axis.setRange(rangeY2, true, true);
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
    public void setLabelFont(Font f){
        labelFont = f;
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getDomainAxis().setLabelFont(f);
        plot.getRangeAxis().setLabelFont(f);   
        if (plot.getRangeAxis(1) != null) {
            plot.getRangeAxis(1).setLabelFont(f);   
        }
    }
    
    @Override
    public void setTickLabelFont(Font f){
        tickLabelFont = f;
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getDomainAxis().setTickLabelFont(f);
        plot.getRangeAxis().setTickLabelFont(f);  
        if (plot.getRangeAxis(1) != null) {
            plot.getRangeAxis(1).setTickLabelFont(f);   
        }        
    }    

    public Font getLabelFont(){
        return labelFont;
    }
        
    public Font getTickLabelFont(){
        return tickLabelFont;
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
                    showException(ex);
                }
            }

        });
        try {
            JMenuItem menuCopy = (JMenuItem) chartPanel.getPopupMenu().getComponent(2);
            for (ActionListener al : menuCopy.getActionListeners()){
                menuCopy.removeActionListener(al);
            }
            menuCopy.addActionListener((ActionEvent e) -> {
                String data = getDataAsString();
                if (data != null) {
                    BufferedImage img = getSnapshot(null);
                    if (img != null) {
                        ImageTransferHandler imageSelection = new ImageTransferHandler(img, data);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(imageSelection, (Clipboard clipboard1, Transferable contents) -> {
                        });
                    }
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(TimePlotJFree.class.getName()).log(Level.INFO, null, ex);
        }        
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
            for (TimestampedValue<Double> item : values) {
                Double val = item.getValue();
                if (val == null) {
                    val = Double.NaN;
                }
                str.append(Chrono.getTimeStr(item.getTimestamp(), "dd/MM/YY HH:mm:ss.SSS "));
                str.append(String.valueOf(val));
                str.append(PlotBase.LINE_SEPARATOR);
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
            axis2.setLabelFont(labelFont);
            axis2.setTickLabelFont(tickLabelFont);
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
    protected void doClear(int graphIndex) {
        series.get(graphIndex).clear();
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
        if (series.getColor() != null) {
            getRenderer(series.axis).setSeriesPaint(dataset.getSeriesCount() - 1, series.getColor());
        }
        return ts;
    }

    @Override
    protected void onRemovedSeries(TimePlotSeries series) {
        int index = getSeriesIndex(series);
        TimeSeries ts = getTimeSeries(series);
        TimeSeriesCollection dataset = data;
        if (series.axis == 2) {
            createY2();
            dataset = dataY2;
        }
        dataset.removeSeries(index);
        this.series.remove(ts);
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
    protected void addDataPoint(int graphIndex, long time, double value, boolean drag) { 
        if (drag){
            series.get(graphIndex).addOrUpdate(new DragTimeSeriesDataItem(new FixedMillisecond(new Date(time)), (Double.isNaN(value)) ? null : value) );
        } else {
            series.get(graphIndex).addOrUpdate(new TimeSeriesDataItem(new FixedMillisecond(new Date(time)), (Double.isNaN(value)) ? null : value));
        }
    }
    
    class DragTimeSeriesDataItem extends TimeSeriesDataItem{
        public DragTimeSeriesDataItem(RegularTimePeriod period, Number value) {
            super(period, value);
        } 
    }    
    
    @Override
    protected void removeDataPoint(int graphIndex, int index, boolean update) { 
        series.get(graphIndex).delete(index, index, update); 
    }  

    @Override
    public List<TimestampedValue<Double>> getSeriestData(int index) {
        TimeSeries s = series.get(index);
        List<TimestampedValue<Double>> ret = new ArrayList<>();
        for (TimeSeriesDataItem item : (List<TimeSeriesDataItem>) s.getItems()) {
            //Filter points added for dragging
            if (!(item instanceof DragTimeSeriesDataItem) || ret.isEmpty()){
                Double val;
                if (item.getValue() != null) {
                    val = item.getValue().doubleValue();
                } else {
                    val = Double.NaN;
                }

                ret.add(new TimestampedValue<Double>(val, item.getPeriod().getMiddleMillisecond()));
            }
        }
        return ret;
    }

    @Override
    public TimestampedValue<Double> getItem(int index, int itemIndex) {
        TimeSeries s = series.get(index);
        if (itemIndex == -1) {
            itemIndex = s.getItemCount() - 1;
        }
        if ((itemIndex < 0) || (itemIndex >= s.getItemCount())) {
            return null;
        }
        TimeSeriesDataItem item = s.getDataItem(itemIndex);
        return new TimestampedValue<Double>((item.getValue() != null) ? item.getValue().doubleValue() : Double.NaN, item.getPeriod().getMiddleMillisecond());
    }
    
    @Override
    public int getItemCount(int index) {
       TimeSeries s = series.get(index);
       return s.getItemCount();
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
    public boolean isY1Logarithmic(){
        return isLogarithmic(0);
    }
        
    @Override
    public void setY1Logarithmic(boolean value){
        setLogarithmic(0, value);
    }   
    
    @Override
    public boolean isY2Logarithmic(){
        return isLogarithmic(1);
    }
        
    @Override
    public void setY2Logarithmic(boolean value){
        setLogarithmic(1, value);
    }      
    
    public boolean isLogarithmic(int axisIndex){
        ValueAxis axis = chart.getXYPlot().getRangeAxis(axisIndex);
        if (axis != null) {
            return axis instanceof LogarithmicAxis;
        }
        return false;
    }
        
    public void setLogarithmic(int axisIndex, boolean value){
        ValueAxis cur = chart.getXYPlot().getRangeAxis(axisIndex);
        if (cur != null){
            if (value != isLogarithmic(axisIndex)){
                XYPlot plot = (XYPlot) chart.getPlot();
                NumberAxis axis = value ? new LogarithmicAxis(cur.getLabel()) : new NumberAxis(cur.getLabel());
                if (value){
                    ((LogarithmicAxis)axis).setAllowNegativesFlag(true);
                }
                axis.setAutoRangeIncludesZero(false);
                axis.setLabelFont(labelFont);
                axis.setTickLabelFont(tickLabelFont);
                plot.setRangeAxis(axisIndex, axis);
                axis.setLabelPaint(PlotBase.getAxisTextColor());
                axis.setTickLabelPaint(PlotBase.getAxisTextColor());
                setAxisSize(axisSize);            
            }
        }
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
            if (quality.ordinal() >= Quality.Maximum.ordinal()) {
                chart.setTextAntiAlias(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            } else {
                chart.setTextAntiAlias(quality.ordinal() >= Quality.Medium.ordinal());
            }
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

    @Override
    public ch.psi.utils.Range getAxisRange(AxisId axisId) {
        Range r = null;
        switch (axisId) {
            case X:
                r = chart.getXYPlot().getDomainAxis().getRange();
                return new ch.psi.utils.Range(r.getLowerBound(), r.getUpperBound());
            case Y:
                r = chart.getXYPlot().getRangeAxis().getRange();
                return new ch.psi.utils.Range(r.getLowerBound(), r.getUpperBound());
            case Y2:
                if (dataY2 == null) {
                    return null;
                }
                r = chart.getXYPlot().getRangeAxis(1).getRange();
                return new ch.psi.utils.Range(r.getLowerBound(), r.getUpperBound());
            default:
                return null;
        }
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
    
    @Override
    public BufferedImage getSnapshot(Dimension size) {
        if (size==null){
            size = new Dimension(SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);
        }
        return chart.createBufferedImage(size.width, size.height);
    }     

    @Hidden
    public JFreeChart getChart() {
        return chart;
    }
}
