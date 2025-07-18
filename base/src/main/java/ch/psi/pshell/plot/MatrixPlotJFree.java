package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Colormap;
import static ch.psi.pshell.plot.PlotBase.SNAPSHOT_WIDTH;
import static ch.psi.pshell.plot.PlotBase.getOutlineColor;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Reflection.Hidden;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import static org.jfree.chart.plot.Plot.DEFAULT_OUTLINE_STROKE;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYZDataset;

/**
 * Returns a matrix plot panel
 */
public class MatrixPlotJFree extends MatrixPlotBase {

    static final Logger logger = Logger.getLogger(MatrixPlotJFree.class.getName());
    private boolean legendVisible;
    private boolean showTooltips;
    private double scaleMin;
    private double scaleMax;

    private MatrixPlotSeries series;
    private DefaultXYZDataset data;
    private JFreeChart chart;
    private ChartPanel chartPanel;

    double[] xvalues = new double[0];
    double[] yvalues = new double[0];
    double[] zvalues = new double[0];

    XYPlot plot;
    NumberAxis xAxis, yAxis;
    XYBlockRenderer renderer;

    public MatrixPlotJFree() {
        super();
    }

    double xMin, xMax, yMin, yMax;

    @Override
    protected Object onAddedSeries(MatrixPlotSeries series) {
        this.series = series;
        int arraylength = series.getNumberOfBinsX() * series.getNumberOfBinsY();
        xvalues = new double[arraylength];
        yvalues = new double[arraylength];
        zvalues = new double[arraylength];
        Arrays.fill(xvalues, Double.NaN);
        Arrays.fill(yvalues, Double.NaN);
        Arrays.fill(zvalues, Double.NaN);
        double[][] dataArray = new double[][]{xvalues, yvalues, zvalues};
        //Create the XYDataset (org.jfree), not to be confused with the ch.psi dataSet)
        data = new DefaultXYZDataset();
        data.addSeries("Data", dataArray);

        onAxisRangeChanged(AxisId.X);
        onAxisRangeChanged(AxisId.Y);

        if (series.getBinWidthX() > 0) {
            renderer.setBlockWidth(series.getBinWidthX()); // If this is not set the default block size is 1
        }
        if (series.getBinWidthY() > 0) {
            renderer.setBlockHeight(series.getBinWidthY()); // If this is not set the default block size is 1
        }
        renderer.setBlockAnchor(RectangleAnchor.CENTER);
                
        if (showTooltips){
            renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
            renderer.setSeriesCreateEntities(0, true);            
        } else {
            renderer.setSeriesCreateEntities(0, false);
        }

        // Remove background paint/color
        plot.setBackgroundPaint(getPlotBackground()); //Always transparent
        plot.setDomainGridlinePaint(getGridColor());
        plot.setRangeGridlinePaint(getGridColor());
        plot.setOutlinePaint(getOutlineColor());
        plot.setDataset(data);

        return data;
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
    protected void onTitleChanged() {
        chartPanel.setName(getTitle());
        chart.setTitle(getTitle());
        if (getTitleFont() != null) {
            chart.getTitle().setFont(getTitleFont());
        }
    }

    @Override
    protected void onAxisLabelChanged(AxisId axis) {
        switch (axis) {
            case X -> xAxis.setLabel(getAxis(AxisId.X).getLabel());
            case Y -> yAxis.setLabel(getAxis(AxisId.Y).getLabel());
        }
    }

    @Override
    protected void onAxisRangeChanged(AxisId axisId) {
        // The axis min and max values need to be half a bin larger. Otherwise the outer pixels
        // will not be plotted correctly (half of the pixel is missing)        
        if (axisId == AxisId.X) {
            if (getAxis(AxisId.X).isAutoRange()) {
                if (series != null) {
                    xMin = series.getMinX() - 0.5 * series.getBinWidthX();
                    xMax = series.getMaxX() + 0.5 * series.getBinWidthX();
                }
            } else {
                xMin = getAxis(AxisId.X).getMin();
                xMax = getAxis(AxisId.X).getMax();
            }
            if (xMax > xMin) {
                xAxis.setRange(xMin, xMax);
            }
            if ((series != null) && (series.getBinWidthX() > 0)) {
                renderer.setBlockWidth(series.getBinWidthX());
            }
            xAxis.setInverted(getAxis(AxisId.X).inverted);
        }
        if (axisId == AxisId.Y) {
            if (getAxis(AxisId.Y).isAutoRange()) {
                if (series != null) {
                    yMin = series.getMinY() - 0.5 * series.getBinWidthY();
                    yMax = series.getMaxY() + 0.5 * series.getBinWidthY();
                }
            } else {
                yMin = getAxis(AxisId.Y).getMin();
                yMax = getAxis(AxisId.Y).getMax();
            }
            if (yMax > yMin) {
                yAxis.setRange(yMin, yMax);
            }
            if ((series != null) && (series.getBinWidthY() > 0)) {
                renderer.setBlockHeight(series.getBinWidthY()); // If this is not set the default block size is 1
            }
            yAxis.setInverted(getAxis(AxisId.Y).inverted);
        }
    }

    @Override
    protected void onRemovedSeries(MatrixPlotSeries s) {
        series = null;
        doUpdate();
    }

    @Override
    protected void onAppendData(MatrixPlotSeries series, int indexX, int indexY, double x, double y, double z) {
        int index = indexY * series.getNumberOfBinsX() + indexX;
        //x Value is column, y value is row
        //Aviod JFreeChart bug: https://github.com/jfree/jfreechart/issues/67
        //Freeze event thread for value = 4.393590014e-315  
        if (Math.abs(z) < 1e-100) {
            z = 0;
        }
        xvalues[index] = x;
        yvalues[index] = y;
        zvalues[index] = z;
    }

    @Override
    protected void onSetData(MatrixPlotSeries series, double[][] data, double[][] xdata, double[][] ydata) {
        int index = 0;
        for (int y = 0; y < data.length; y++) {
            for (int x = 0; x < data[0].length; x++) {
                //final int index = y * series.getNumberOfBinsX() + x;
                xvalues[index] = (xdata == null) ? series.getMinX() + series.getBinWidthX() * x : xdata[y][x];
                yvalues[index] = (ydata == null) ? series.getMinY() + series.getBinWidthY() * y : ydata[y][x];
                zvalues[index++] = data[y][x];
            }
        }
    }

    @Override
    public void clear() {
        if (series != null) {
            removeSeries(series);
            doUpdate();
        }
    }

    @Override
    public double[][] getSeriesData(MatrixPlotSeries s) {
        double[][] ret = new double[series.getNumberOfBinsY()][series.getNumberOfBinsX()];
        int index = 0;
        for (int i = 0; i < series.getNumberOfBinsY(); i++) {
            for (int j = 0; j < series.getNumberOfBinsX(); j++) {
                ret[i][j] = data.getZValue(0, index++);
            }
        }
        return ret;
    }

    @Override
    public double[][] getSeriesX(final MatrixPlotSeries series) {
        double[][] ret = new double[series.getNumberOfBinsY()][series.getNumberOfBinsX()];
        int index = 0;
        for (int i = 0; i < series.getNumberOfBinsY(); i++) {
            for (int j = 0; j < series.getNumberOfBinsX(); j++) {
                ret[i][j] = data.getXValue(0, index++);
            }
        }
        return ret;
    }

    @Override
    public double[][] getSeriesY(final MatrixPlotSeries series) {
        double[][] ret = new double[series.getNumberOfBinsY()][series.getNumberOfBinsX()];
        int index = 0;
        for (int i = 0; i < series.getNumberOfBinsY(); i++) {
            for (int j = 0; j < series.getNumberOfBinsX(); j++) {
                ret[i][j] = data.getYValue(0, index++);
            }
        }
        return ret;
    }
    
    public void removePopupMenuItem(int index) {
        chartPanel.getPopupMenu().remove(index);
    }    

    public void removePopupMenuItem(JMenuItem item) {
        chartPanel.getPopupMenu().remove(item);
    }    

    public void removePopupMenuItem(String text) {
        for (Component c : chartPanel.getPopupMenu().getComponents().clone()) {
            if (c instanceof AbstractButton ab){
                if (ab.getText().equals(text)){
                    chartPanel.getPopupMenu().remove(c);
                    return;
                }
            }
        }
    }          

    @Override
    protected void createPopupMenu() {
        //Remove copy/save as/print menus: copy is buggy, save is limited 
        for (int index = 6; index >= 2; index--) {
            removePopupMenuItem(index);
        }

        // Colormap configuration
        JRadioButtonMenuItem popupMenuAutoScale = new JRadioButtonMenuItem("Automatic");
        popupMenuAutoScale.addActionListener((ActionEvent e) -> {
            setAutoScale();
        });

        JRadioButtonMenuItem popupMenuManualScale = new JRadioButtonMenuItem("Manual");
        popupMenuManualScale.addActionListener((ActionEvent e) -> {
            ManualScaleDialog d = new ManualScaleDialog();
            SwingUtils.centerComponent(chartPanel, d);
            Double low = ((PaintScaleLegend) chart.getSubtitles().get(0)).getScale().getLowerBound();
            Double high = ((PaintScaleLegend) chart.getSubtitles().get(0)).getScale().getUpperBound();
            Boolean auto = isAutoScale();
            d.setLow(low);
            d.setHigh(high);
            d.setScaleChangeListener(MatrixPlotJFree.this);

            d.showDialog();
            if (d.getSelectedOption() == JOptionPane.OK_OPTION) {
                setScale(d.getLow(), d.getHigh());
            } else {
                if (auto) {
                    setAutoScale();
                } else {
                    setScale(low, high);
                }
            }
        });

        JMenu popupMenuChooseColormap = new JMenu("Colormap");
        for (Colormap c : Colormap.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(c.toString());
            item.addActionListener((ActionEvent e) -> {
                setColormap(Colormap.valueOf(item.getText()));
            });
            popupMenuChooseColormap.add(item);
        }
        JCheckBoxMenuItem menuLogarithmic = new JCheckBoxMenuItem("Logarithmic");
        menuLogarithmic.addActionListener((ActionEvent e) -> {
            setColormapLogarithmic(menuLogarithmic.isSelected());
        });

        JMenu popupMenuChooseScale = new JMenu("Scale");
        popupMenuChooseScale.add(popupMenuAutoScale);
        popupMenuChooseScale.add(popupMenuManualScale);

        popupMenuChooseColormap.addSeparator();
        popupMenuChooseColormap.add(popupMenuChooseScale);
        popupMenuChooseColormap.add(menuLogarithmic);

        // Group colormap related menu items
        chartPanel.getPopupMenu().add(popupMenuChooseColormap);

        //Show hide legend
        JCheckBoxMenuItem popupMenuItemColorLegendVisible = new JCheckBoxMenuItem("Show Legend");
        popupMenuItemColorLegendVisible.addActionListener((ActionEvent e) -> {
            setLegendVisible(popupMenuItemColorLegendVisible.isSelected());
        });
        chartPanel.getPopupMenu().add(popupMenuItemColorLegendVisible);

        // Show hide tooltips
        JCheckBoxMenuItem popupMenuItemToolTip = new JCheckBoxMenuItem("Show Tooltips");
        popupMenuItemToolTip.addActionListener((ActionEvent e) -> {
            setShowTooltips(popupMenuItemToolTip.isSelected());
        });
        chartPanel.getPopupMenu().add(popupMenuItemToolTip);

        chartPanel.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                popupMenuItemColorLegendVisible.setSelected(isLegendVisible());
                popupMenuManualScale.setSelected(!isAutoScale());
                popupMenuAutoScale.setSelected(isAutoScale());
                for (Component c : popupMenuChooseColormap.getMenuComponents()) {
                    if (c instanceof JRadioButtonMenuItem rb) {
                        rb.setSelected(getColormap() == Colormap.valueOf(((JMenuItem) c).getText()));
                    } else if (c instanceof JCheckBoxMenuItem cb) {
                        cb.setSelected(isColormapLogarithmic());
                    }
                }
                popupMenuItemToolTip.setSelected(getShowTooltips());
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

    static XYZToolTipGenerator tooltipGenerator = new StandardXYZToolTipGenerator("x={1} y={2} z={3}", displayFormatValue, displayFormatValue, displayFormatValue);
    private void showTooltips() {
        //Tooltips are quit expensive
        if (series!=null){
            chart.getXYPlot().getRenderer().setSeriesToolTipGenerator(0, tooltipGenerator);
            chart.getXYPlot().getRenderer().setSeriesCreateEntities(0, true);
        }
    }

    @Override
    public Object addText(double x, double y, String label, Color color) {
        XYTextAnnotation annotation = new XYTextAnnotation(label, x, y);
        chartPanel.getChart().getXYPlot().addAnnotation(annotation);
        if (color != null) {
            annotation.setPaint(color);
        }
        return annotation;
    }

    @Override
    public List getTexts() {
        List ret = new ArrayList();
        ret.addAll(chartPanel.getChart().getXYPlot().getAnnotations());
        return ret;
    }

    @Override
    public void removeText(Object text) {
        if (text instanceof XYTextAnnotation ta) {
            chartPanel.getChart().getXYPlot().removeAnnotation(ta);
        }
    }

    /**
     * Clear Tooltips
     */
    private void hideTooltips() {
        if (series!=null){
            chart.getXYPlot().getRenderer().setSeriesToolTipGenerator(0, null);
            chart.getXYPlot().getRenderer().setSeriesCreateEntities(0, false);
        }
    }

    /**
     * Adapt the lower and upper color map scale to the min and max data values
     * of the currently selected region Need to be called AFTER the chart panel
     * is created
     */
    public void setLegendVisible(boolean value) {
        if (value != legendVisible) {
            legendVisible = value;
            updateColorScale(scaleMin, scaleMax);
        }
    }

    public boolean isLegendVisible() {
        return legendVisible;
    }

    @Override
    public void setColormap(Colormap value) {
        if (value != getColormap()) {
            super.setColormap(value);
            if (isAutoScale()) {
                setAutoScale();
            } else {
                setScale(scaleMin, scaleMax);
            }
        }
    }

    @Override
    public void setColormapLogarithmic(boolean value) {
        if (value != isColormapLogarithmic()) {
            super.setColormapLogarithmic(value);
            if (isAutoScale()) {
                setAutoScale();
            } else {
                setScale(scaleMin, scaleMax);
            }
        }
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
    public void setScale(double scaleMin, double scaleMax) {
        super.setScale(scaleMin, scaleMax);
        updateColorScale(scaleMin, scaleMax);
    }

    @Override
    public void setAutoScale() {
        super.setAutoScale();
        if (series != null) {
            double[] v = series.minMaxZValue();
            if (v[0] != Double.NaN && v[1] != Double.NaN && v[0] <= v[1]) {
                updateColorScale(v[0], v[1]);
            }
        }
    }
    
    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
        if (chartPanel != null) {
            chartPanel.setBackground(c);
            chart.setBackgroundPaint(c);
        }
    }    
    
    @Override
    public void setPlotOutlineColor(Color c) {
        chart.getXYPlot().setOutlinePaint((c==null) ? getOutlineColor() : c);        
    }   
    
    @Override
    public Color getPlotOutlineColor() {
        Paint ret = chart.getXYPlot().getOutlinePaint();
        return (ret instanceof Color c) ? c : null;
    }

    @Override
    public void setPlotOutlineWidth(int width) {
        chart.getXYPlot().setOutlineStroke(width<0 ? DEFAULT_OUTLINE_STROKE : new BasicStroke(width));
    }
        
     @Override
    public int getPlotOutlineWidth() {
        Stroke s = chart.getXYPlot().getOutlineStroke();
        if ((s==DEFAULT_OUTLINE_STROKE) || (!(s instanceof BasicStroke))){
            return -1;
        }
        return Math.round(((BasicStroke)s).getLineWidth());
    }  
       

    /**
     * Set the min and max of color map scale to scaleMin and scaleMax
     *
     * @param scaleMin	min value of scale
     * @param scaleMax	max value of scale
     */
    void updateColorScale(double scaleMin, double scaleMax) {
        // Create scale for legend
        if (scaleMin >= scaleMax) {
            scaleMax = scaleMin + 0.01;
        }
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        LookupPaintScale legendScale = new LookupPaintScale(scaleMin, scaleMax, Color.GRAY);
        setScaleColors(legendScale, scaleMin, scaleMax);

        // Create legend
        PaintScaleLegend legend = new PaintScaleLegend(legendScale, new NumberAxis());
        legend.setPadding(new RectangleInsets(5, 5, 5, 5));
        // Width of the legend (colorbar)
        legend.setStripWidth(20);
        // Position of the legend
        legend.setPosition(RectangleEdge.RIGHT);
        // Remove background paint/color
        legend.setBackgroundPaint(null);
        legend.getAxis().setTickLabelPaint(getAxisTextColor());

        // Add legend to plot panel
        chart.clearSubtitles();
        if (isLegendVisible()) {
            chart.addSubtitle(legend);
        }

        //We need a second scale for rendering only, where the 'infinities' are in correct color 
        LookupPaintScale rendererScale = new LookupPaintScale(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Color.GRAY);
        setScaleColors(rendererScale, scaleMin, scaleMax);
        // Make the paint scale range from - to + infinity
        if (isColormapLogarithmic()) {
            rendererScale.add(Double.NEGATIVE_INFINITY, getColormap().getColorLogarithmic(scaleMin, scaleMin, scaleMax));
            rendererScale.add(Double.POSITIVE_INFINITY, getColormap().getColorLogarithmic(scaleMax, scaleMin, scaleMax));
        } else {
            rendererScale.add(Double.NEGATIVE_INFINITY, getColormap().getColor(scaleMin, scaleMin, scaleMax));
            rendererScale.add(Double.POSITIVE_INFINITY, getColormap().getColor(scaleMax, scaleMin, scaleMax));
        }
        ((XYBlockRenderer) chart.getXYPlot().getRenderer()).setPaintScale(rendererScale);
    }

    /**
     * Set the colors for the colored Paint Scale (either single color or
     * temperature color)
     *
     * @param paintScale
     * @param scaleMin
     * @param scaleMax
     */
    private void setScaleColors(PaintScale paintScale, double scaleMin, double scaleMax) {
        if (isColormapLogarithmic()) {
            for (int i = 0; i < 256; i++) {
                double value = scaleMin + (i / 255.0) * (scaleMax - scaleMin);
                ((LookupPaintScale) paintScale).add(value, getColormap().getColorLogarithmic(value, scaleMin, scaleMax));
            }
        } else {
            for (int i = 0; i < 256; i++) {
                double value = scaleMin + (i / 255.0) * (scaleMax - scaleMin);
                ((LookupPaintScale) paintScale).add(value, getColormap().getColor(value, scaleMin, scaleMax));
            }
        }
    }

    
    void notifyZoomListener(){
        if (zoomListener!=null){
            try{
                Range rangeX = chart.getXYPlot().getDomainAxis().getRange();
                Range rangeY = chart.getXYPlot().getRangeAxis().getRange();
                super.notifyZoomListener( new ch.psi.pshell.utils.Range(rangeX.getLowerBound(), rangeX.getUpperBound()),
                                          new ch.psi.pshell.utils.Range(rangeY.getLowerBound(), rangeY.getUpperBound()));
            } catch (Exception ex){          
                logger.log(Level.WARNING, null, ex);
            }

        }        
    }
        
    /**
     * Get the chart panel
     *
     */
    @Override
    protected void createChart() {
        super.createChart();
        legendVisible = true;

        // Create matrix chart
        // Init size of plot area according to min and max set in the datas metadata
        // Set axis: (plus half bin size on both sides), since we plot the bins centered.
        xAxis = new NumberAxis("");
        yAxis = new NumberAxis("");
        xAxis.setTickLabelFont(TICK_LABEL_FONT);
        yAxis.setTickLabelFont(TICK_LABEL_FONT);
        xAxis.setLabelFont(LinePlotJFree.LABEL_FONT);
        yAxis.setLabelFont(LinePlotJFree.LABEL_FONT);
        xAxis.setLabelPaint(getAxisTextColor());
        yAxis.setLabelPaint(getAxisTextColor());
        xAxis.setTickLabelPaint(getAxisTextColor());
        yAxis.setTickLabelPaint(getAxisTextColor());
        final Range initRangeX = xAxis.getRange();
        final Range initRangeY = xAxis.getRange();
        // Configure block renderer to have the blocks rendered in the correct size
        renderer = new XYBlockRenderer();
        setBackground(getBackground());
        plot = new XYPlot(null, xAxis, yAxis, renderer);

        // Remove background paint/color
        plot.setBackgroundPaint(getPlotBackground());        
        plot.setDomainGridlinePaint(getGridColor());
        plot.setRangeGridlinePaint(getGridColor());        
        plot.setOutlinePaint(getOutlineColor());
                
        // Set the maximum zoom out to the initial zoom rate This also 
        // provides a workaround for dynamic plots because there zoom out does not work correctly (zoom out to infinity) 
        plot.getRangeAxis().addChangeListener((AxisChangeEvent event) -> {
            if (series == null) {
                return;
            }
            ValueAxis axis = ((ValueAxis) event.getAxis());
            boolean equals = initRangeY == axis.getRange();
            if (axis.getLowerBound() < yMin || axis.getUpperBound() > yMax) {
                Range range = new Range(yMin, yMax);
                axis.setRange(range, true, false);
            }
        });
        plot.getDomainAxis().addChangeListener((AxisChangeEvent event) -> {
            if (series == null) {
                return;
            }
            ValueAxis axis = ((ValueAxis) event.getAxis());
            boolean equals = initRangeX == axis.getRange();
            if (axis.getLowerBound() < xMin || axis.getUpperBound() > xMax) {
                Range range = new Range(xMin, xMax);
                axis.setRange(range, true, false);
            }
        });

        chart = new JFreeChart(getTitle(), plot);

        //Remove the series label (also called legend)
        chart.removeLegend();
        //AntiAliasing is used to speed up rendering
//			chart.setAntiAlias(false);
        
        //Anti-aliasing
        setQuality(quality);

        // Init PaintScale
        updateColorScale(0, 1);

        //Create the Chartpanel where the chart will be plotted
        chartPanel = new ChartPanel(chart, getOffscreenBuffer()){
            @Override
            public void restoreAutoRangeBounds() {
                super.restoreAutoRangeBounds();             
                notifyZoomListener();
            }
            
            @Override
            public void zoom(Rectangle2D selection) {
                super.zoom(selection);
                notifyZoomListener();
            }            
        };

        //Fonts will be distorted if greater than this value. Default values are 1024x728
        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMaximumDrawWidth(2000);

        chartPanel.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(getAxisTextColor());
        }
        //chart.setBackgroundPaint(chartPanel.getBackground());          
        chart.setBackgroundPaint(new Color(0,0,0,0));          
        
        setLayout(new BorderLayout());
        add(chartPanel);
    }

    @Override
    public void doUpdate() {
        if (chart != null) {
            chart.fireChartChanged();
            if (isAutoScale()) {
                setAutoScale();
            }
        }
    }

    @Override
    public void updateSeries(MatrixPlotSeries series) {
        doUpdate();
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

    //public JFreeChart getChart() {
    //	return chart;
    //}
    //@Override
    //public void setData(Object data){
    //    super.setData(data);
    //    this.data=(MatrixPlotSeries) data;
    //}
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
        invokeLater(() -> {
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
        Color outlineColor = color.darker();

        final IntervalMarker marker = new IntervalMarker(start, end, color);

        if (label != null) {
            marker.setLabel(label);
        }
        marker.setLabelPaint(outlineColor);
        marker.setLabelAnchor(RectangleAnchor.CENTER);
        marker.setOutlineStroke(new BasicStroke(1f));
        marker.setStroke(new BasicStroke(1f));
        marker.setOutlinePaint(outlineColor);

        invokeLater(() -> {
            if ((axis == null) || (axis == AxisId.X)) {
                chartPanel.getChart().getXYPlot().addDomainMarker(marker, Layer.FOREGROUND);
            } else {
                chartPanel.getChart().getXYPlot().addRangeMarker(marker, Layer.FOREGROUND);
            }
        });
        return marker;
    }

    public void resetZoom(){
        chartPanel.restoreAutoBounds();
    }
    
    public void zoom(Rectangle2D selection){
        chartPanel.zoom(selection);
    }

    public void zoomDomain(ch.psi.pshell.utils.Range range){
        XYPlot plot = chart.getXYPlot();        
        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setRange(range.min, range.max);        
    }
    
    @Override
    public void removeMarker(final Object marker) {
        invokeLater(() -> {
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
    
    

    @Hidden
    public JFreeChart getChart() {
        return chart;
    }

    @Hidden
    public ChartPanel getChartPanel() {
        return chartPanel;
    }
    
    @Override
    public BufferedImage getSnapshot(Dimension size) {
        if (size == null) {
            size = new Dimension(SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);
        }
        return chart.createBufferedImage(size.width, size.height);
    }
}
