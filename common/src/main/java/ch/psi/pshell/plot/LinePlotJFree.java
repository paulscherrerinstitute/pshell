package ch.psi.pshell.plot;

import ch.psi.pshell.app.MainFrame;
import static ch.psi.pshell.plot.Plot.AxisId.Y2;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Reflection;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
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
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import static org.jfree.chart.plot.Plot.DEFAULT_OUTLINE_STROKE;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.Drawable;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.ComparableObjectSeries;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.general.Series;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

public class LinePlotJFree extends LinePlotBase {

    static final Logger logger = Logger.getLogger(LinePlotJFree.class.getName());

    ChartPanel chartPanel;
    boolean legendVisible;
    boolean showTooltips;

    AbstractXYDataset dataY1;
    AbstractXYDataset dataY2;
    NumberAxis axisX2;

    Font tickLabelFont = TICK_LABEL_FONT;
    Font labelFont = LABEL_FONT;

    //TODO: if smaller there are plot repainting problem in scans in JFreeChart > 1.0.18
    static final double AUTO_RANGE_MINIMUM_SIZE = 1e-12;
    static final double AUTO_RANGE_LOG_MINIMUM_SIZE = 1e-32;
            
    JFreeChart chart;

    Rectangle2D.Double seriesMarker;

    /**
     */
    public LinePlotJFree() {
        super();
        setMarkerSize(getMarkerSize());
        setErrorRangeAlpha(255);
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
            if (quality.ordinal() >= Quality.Maximum.ordinal()) {
                chart.setTextAntiAlias(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            } else {
                chart.setTextAntiAlias(quality.ordinal() >= Quality.Medium.ordinal());
            }
            if (chartPanel!=null) {
                chartPanel.setRefreshBuffer(true);
            }
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
      
    
    
    int errorRangeAlpha;
    Color defaultErrorPaint;
    
    public int 
        RangeAlpha(){
        return errorRangeAlpha;
    }
    
    public void setErrorRangeAlpha(int value){
        errorRangeAlpha = value;
        defaultErrorPaint =  new Color(0xA0, 0xA0, 0xA0, errorRangeAlpha);
    }
    
    
    class ErrorRenderer extends XYErrorRenderer {              
        Color[] errorColors = new Color[0];       
         //@Override
        public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info){                        
            Paint errorPaint = getErrorPaint();
            if (errorColors.length<data.getSeriesCount()){
                errorColors = new Color[data.getSeriesCount()];  
            }            
            for (int i=0; i<data.getSeriesCount(); i++){
                    Paint p= getSeriesPaint(i); 
                    Color c = defaultErrorPaint;
                    if (errorPaint instanceof Color ec){
                        c = ec;
                    } else if (p instanceof Color pc){
                        c = new Color(pc.getRed(), pc.getGreen(), pc.getBlue(), errorRangeAlpha);                                    
                    }
                    errorColors[i] = c;                        
            }
            return super.initialise(g2, dataArea, plot, data, info);
        }
                

        //@Override
        public void drawItem(Graphics2D g2, XYItemRendererState state,
            Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
            ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
            int series, int item, CrosshairState crosshairState, int pass) {        
            // do nothing if item is not visible
            if (!getItemVisible(series, item)) {
                return;
            }
            Color errorColor = (series < errorColors.length) ? errorColors[series] : defaultErrorPaint;
            
            
            if ((pass == 0) && dataset instanceof IntervalXYDataset ixyd) {                                    
                PlotOrientation orientation = plot.getOrientation();
                if (getDrawXError()) {
                    // draw the error bar for the x-interval
                    double x0 = ixyd.getStartXValue(series, item);
                    double x1 = ixyd.getEndXValue(series, item);
                    double y = ixyd.getYValue(series, item);
                    RectangleEdge edge = plot.getDomainAxisEdge();
                    double xx0 = domainAxis.valueToJava2D(x0, dataArea, edge);
                    double xx1 = domainAxis.valueToJava2D(x1, dataArea, edge);
                    double yy = rangeAxis.valueToJava2D(y, dataArea,
                            plot.getRangeAxisEdge());
                    Line2D line;
                    Line2D cap1;
                    Line2D cap2;
                    double adj = this.getCapLength() / 2.0;
                    if (orientation == PlotOrientation.VERTICAL) {
                        line = new Line2D.Double(xx0, yy, xx1, yy);
                        cap1 = new Line2D.Double(xx0, yy - adj, xx0, yy + adj);
                        cap2 = new Line2D.Double(xx1, yy - adj, xx1, yy + adj);
                    }
                    else {  // PlotOrientation.HORIZONTAL
                        line = new Line2D.Double(yy, xx0, yy, xx1);
                        cap1 = new Line2D.Double(yy - adj, xx0, yy + adj, xx0);
                        cap2 = new Line2D.Double(yy - adj, xx1, yy + adj, xx1);
                    }                    
                    g2.setPaint(errorColor);
                    if (this.getErrorStroke() != null) {
                        g2.setStroke(this.getErrorStroke());
                    }
                    else {
                        g2.setStroke(getItemStroke(series, item));
                    }
                    g2.draw(line);
                    g2.draw(cap1);
                    g2.draw(cap2);
                }
                if (getDrawYError()) {
                    // draw the error bar for the y-interval
                    double y0 = ixyd.getStartYValue(series, item);
                    double y1 = ixyd.getEndYValue(series, item);
                    double x = ixyd.getXValue(series, item);
                    RectangleEdge edge = plot.getRangeAxisEdge();
                    double yy0 = rangeAxis.valueToJava2D(y0, dataArea, edge);
                    double yy1 = rangeAxis.valueToJava2D(y1, dataArea, edge);
                    double xx = domainAxis.valueToJava2D(x, dataArea,
                            plot.getDomainAxisEdge());
                    Line2D line;
                    Line2D cap1;
                    Line2D cap2;
                    double adj = this.getCapLength() / 2.0;
                    if (orientation == PlotOrientation.VERTICAL) {
                        line = new Line2D.Double(xx, yy0, xx, yy1);
                        cap1 = new Line2D.Double(xx - adj, yy0, xx + adj, yy0);
                        cap2 = new Line2D.Double(xx - adj, yy1, xx + adj, yy1);
                    }
                    else {  // PlotOrientation.HORIZONTAL
                        line = new Line2D.Double(yy0, xx, yy1, xx);
                        cap1 = new Line2D.Double(yy0, xx - adj, yy0, xx + adj);
                        cap2 = new Line2D.Double(yy1, xx - adj, yy1, xx + adj);
                    }
                    g2.setPaint(errorColor);
                    if (this.getErrorStroke() != null) {
                        g2.setStroke(this.getErrorStroke());
                    }
                    else {
                        g2.setStroke(getItemStroke(series, item));
                    }
                    g2.draw(line);
                    g2.draw(cap1);
                    g2.draw(cap2);
                }
            }
            //super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);            
            

            // first pass draws the background (lines, for instance)
            if (isLinePass(pass)) {
                if (getItemLineVisible(series, item)) {
                    if (this.getDrawSeriesLineAsPath()) {
                        drawPrimaryLineAsPath(state, g2, plot, dataset, pass,
                                series, item, domainAxis, rangeAxis, dataArea);
                    }
                    else {
                        drawPrimaryLine(state, g2, plot, dataset, pass, series,
                                item, domainAxis, rangeAxis, dataArea);
                    }
                }
                if (getLineExtension()){
                    // If this is the last item in the series, extend the line to the plot's edge
                    if (item == dataset.getItemCount(series) - 1) {
                        double x1 = dataset.getXValue(series, item);
                        double y1 = dataset.getYValue(series, item);

                        // Transform the last data point to Java2D space
                        double xx1 = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
                        double yy1 = rangeAxis.valueToJava2D(y1, dataArea, plot.getRangeAxisEdge());

                        // Extend the series
                        Double xEnd = getExtensionThreshold();
                        if (xEnd==null){
                            xEnd = getAxis(AxisId.X).isAutoRange() ? domainAxis.getUpperBound() : getAxis(AxisId.X).getMax();                
                        }
                        double xx2 = domainAxis.valueToJava2D(xEnd, dataArea, plot.getDomainAxisEdge());

                        g2.setPaint(getSeriesPaint(series));
                        g2.setStroke(getSeriesStroke(series));
                        Line2D line = new Line2D.Double(xx1, yy1, xx2, yy1);
                        g2.draw(line);
                    }                           
                }
            }
            // second pass adds shapes where the items are ..
            else if (isItemPass(pass)) {

                // setup for collecting optional entity info...
                EntityCollection entities = null;
                if (info != null && info.getOwner() != null) {
                    entities = info.getOwner().getEntityCollection();
                }

                drawSecondaryPass(g2, plot, dataset, pass, series, item,
                        domainAxis, dataArea, rangeAxis, crosshairState, entities);
            }      
        }                        
    }
    
  class LineAndShapeRenderer extends XYLineAndShapeRenderer {

      public LineAndShapeRenderer() {
          this(true, false);
      }

      public LineAndShapeRenderer(boolean lines, boolean shapes) {
          super(lines, shapes);
      }

      @Override
        public void drawItem(Graphics2D g2, 
                             XYItemRendererState state, 
                             Rectangle2D dataArea, 
                             PlotRenderingInfo info, 
                             XYPlot plot, 
                             ValueAxis domainAxis, 
                             ValueAxis rangeAxis, 
                             XYDataset dataset, 
                             int series, 
                             int item, 
                             CrosshairState crosshairState, 
                             int pass) {
            // Draw the usual item
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
            if (getLineExtension()){
                // If this is the last item in the series, extend the line to the plot's edge
                if ((item == dataset.getItemCount(series) - 1) && isLinePass(pass)) {
                    double x1 = dataset.getXValue(series, item);
                    double y1 = dataset.getYValue(series, item);

                    // Transform the last data point to Java2D space
                    double xx1 = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
                    double yy1 = rangeAxis.valueToJava2D(y1, dataArea, plot.getRangeAxisEdge());

                    // Extend the series
                    Double xEnd = getExtensionThreshold();
                    if (xEnd==null){
                        xEnd = getAxis(AxisId.X).isAutoRange() ? domainAxis.getUpperBound() : getAxis(AxisId.X).getMax();                
                    }
                    double xx2 = domainAxis.valueToJava2D(xEnd, dataArea, plot.getDomainAxisEdge());

                    g2.setPaint(getSeriesPaint(series));
                    g2.setStroke(getSeriesStroke(series));
                    g2.draw(new Line2D.Double(xx1, yy1, xx2, yy1));        
                }
            }
        }
    }    
  
    boolean lineExtension;
    Double extensionThreshold;
    
    
    public void setLineExtension(boolean value){
        lineExtension = value;
    }
        

    public boolean getLineExtension(){      
        return lineExtension;
    }
    
    public void setExtensionThreshold(Double value){
        extensionThreshold = value;
    }
        

    public Double getExtensionThreshold(){
        if (Double.isNaN(extensionThreshold)){
            return null;
        }
        return extensionThreshold;
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
    public void setPlotBackgroundColor(Color c) {
        chart.getPlot().setBackgroundPaint(c);
    }
    
    @Override
    public Color getPlotBackgroundColor() {
        Paint ret =chart.getPlot().getBackgroundPaint();
        return (ret instanceof Color c) ? c : null;
    }    

    @Override
    public void setPlotGridColor(Color c) {
        ((XYPlot) chart.getPlot()).setDomainGridlinePaint(c);
        ((XYPlot) chart.getPlot()).setRangeGridlinePaint(c);
    }
    
    @Override
    public Color getPlotGridColor() {
        Paint ret = ((XYPlot) chart.getPlot()).getDomainGridlinePaint();
        return (ret instanceof Color c) ? c : null;
    }    
    
    @Override
    public void setPlotOutlineColor(Color c) {
        chart.getPlot().setOutlinePaint((c==null) ? getOutlineColor() : c);        
    }   
    
    @Override
    public Color getPlotOutlineColor() {
        Paint ret = chart.getPlot().getOutlinePaint();
        return (ret instanceof Color c) ? c : null;
    }

    @Override
    public void setPlotOutlineWidth(int width) {
        chart.getPlot().setOutlineStroke(width<0 ? DEFAULT_OUTLINE_STROKE : new BasicStroke(width));
    }
        
     @Override
    public int getPlotOutlineWidth() {
        Stroke s = chart.getPlot().getOutlineStroke();
        if ((s==DEFAULT_OUTLINE_STROKE) || (!(s instanceof BasicStroke))){
            return -1;
        }
        return Math.round(((BasicStroke)s).getLineWidth());
    }  

    @Override
    protected void onAppendData(LinePlotSeries series, double x, double y) {
        XYSeries s = getXYSeries(series);
        if (Double.isInfinite(y)){
            y=Double.NaN;
        }
        s.add(new XYDataItem(x, y), false);
    }

    @Override
    protected void onSetData(final LinePlotSeries series, final double[] x, final double[] y) {
        //invokeLater(() -> {
        XYSeries s = getXYSeries(series);
        s.setNotify(false);
        if (!s.isEmpty()) {
            s.clear();
        }
        //Separated loop for performance (not checking inside the loop)
        if (x == null) {
            for (int i = 0; i < y.length; i++) {
                if (Double.isInfinite(y[i])){
                    s.add(new XYDataItem(i, Double.NaN), false);
                } else {
                    s.add(new XYDataItem(i, y[i]), false);
                }
            }
        } else {
            for (int i = 0; i < y.length; i++) {
                if (Double.isInfinite(y[i])){
                    s.add(new XYDataItem(x[i], Double.NaN), false);
                } else {
                    s.add(new XYDataItem(x[i], y[i]), false);
                }
            }
        }
        s.setNotify(true);
        //});
    }

    @Reflection.Hidden
    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    void createY2() {
        if (dataY2 == null) {
            XYPlot plot = (XYPlot) chart.getPlot();
            Series s;

            dataY2 = switch (getStyle()) {
                case ErrorX -> new YIntervalSeriesCollection();
                case ErrorY -> new YIntervalSeriesCollection();
                case ErrorXY -> new XYIntervalSeriesCollection();
                default -> new XYSeriesCollection();
            };

            NumberAxis axis2 = new NumberAxis(getAxis(AxisId.Y2).getLabel());
            axis2.setAutoRangeIncludesZero(false);
            ((NumberAxis) plot.getRangeAxis()).setAutoRangeMinimumSize(AUTO_RANGE_MINIMUM_SIZE);
            axis2.setLabelFont(labelFont);
            axis2.setTickLabelFont(tickLabelFont);
            plot.setRangeAxis(1, axis2);
            XYLineAndShapeRenderer renderer2 = getStyle().isError() ? new ErrorRenderer() : new LineAndShapeRenderer();
            if (getStyle()==Style.ErrorY){
                ((XYErrorRenderer)renderer2).setDrawXError(false);
            } else if (getStyle()==Style.ErrorX){
                ((XYErrorRenderer)renderer2).setDrawYError(false);
            }            
            plot.setRenderer(1, renderer2);
            plot.setDataset(1, dataY2);
            plot.mapDatasetToRangeAxis(1, 1);
            axis2.setLabelPaint(getAxisTextColor());
            axis2.setTickLabelPaint(getAxisTextColor());
        }
    }
    
    void createX2() {
        if (!hasX2()) {
            boolean log = getAxis(AxisId.X2).isLogarithmic();
            ValueAxis axis = log ? new LogarithmicAxis(getAxis(AxisId.X2).getLabel()) : new NumberAxis(getAxis(AxisId.X2).getLabel());
            if (log){
                ((LogarithmicAxis)axis).setAllowNegativesFlag(false);
                ((LogarithmicAxis)axis).setStrictValuesFlag(false);
                ((LogarithmicAxis)axis).setLog10TickLabelsFlag(true);
                ((LogarithmicAxis)axis).setAutoRangeMinimumSize(AUTO_RANGE_LOG_MINIMUM_SIZE);                
            }                
            XYPlot plot = (XYPlot) chart.getPlot();
            axis.setLabelFont(labelFont);
            axis.setTickLabelFont(tickLabelFont);
            axis.setLabelPaint(getAxisTextColor());
            axis.setTickLabelPaint(getAxisTextColor());
            plot.setDomainAxis(1, axis);
            
            axis.setInverted(getAxis(AxisId.X2).inverted);
            if (getAxis(AxisId.X2).isAutoRange()) {
                axis.setAutoRange(true);
                ranges.remove(AxisId.X2);
            } else {
                Range range = new Range(getAxis(AxisId.X2).getMin(), getAxis(AxisId.X2).getMax());
                axis.setRange(range, true, true);
                ranges.put(AxisId.X2, range);
            }
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
    

    public XYDataset getDataset(int axis) {
        XYPlot plot = (XYPlot) chart.getPlot(); 
        return plot.getDataset(axis-1);
    }       

    public int getSeriesIndex(LinePlotSeries series) {
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
    
    public LinePlotSeries getSeriesByAxisIndex(int axis, int index){
        int count = 0;        
        for (LinePlotSeries s : this.getAllSeries()) {           
            if (axis == s.getAxisY()){
                if (count == index) {
                    return s;
                }
                count++;
            }
        }
        return null;        
    }
    
    protected LinePlotSeries getSeriesByAxisIndex(ValueAxis axis, int index){
        ValueAxis y1 = chart.getXYPlot().getRangeAxis(1);               
        ValueAxis y2 =chart.getXYPlot().getRangeAxisCount() >1 ? chart.getXYPlot().getRangeAxis(1) : null;
        int axisId = (axis==y2) ? 2 : 1;   
        return getSeriesByAxisIndex(axisId, index);
    }    
    

    public XYLineAndShapeRenderer getSeriesRenderer(LinePlotSeries series) {
        return getRenderer(series.getAxisY());
    }     

    
    @Override
    protected Object onAddedSeries(LinePlotSeries series) {
        //TODO: LinePlotErrorSeries is specific to JFree
        Series s;
        AbstractXYDataset data = getYData(series.getAxisY());
        Style style = getStyle();
        switch (style) {
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

        if (series.getMaxItemCount() >= 0) {
            if (s instanceof ComparableObjectSeries cs) {
                cs.setMaximumItemCount(series.getMaxItemCount());
            } else {
                ((XYSeries) s).setMaximumItemCount(series.getMaxItemCount());
            }
        }
        int index = data.getSeriesCount() - 1;
        XYLineAndShapeRenderer renderer = getRenderer(series.getAxisY());
        renderer.setSeriesShape(index, seriesMarker);        
        renderer.setSeriesShapesVisible(index, true);
        if (showTooltips) {
            renderer.setSeriesToolTipGenerator(index, tooltipGenerator);               
            //renderer.setSeriesCreateEntities(index, true); 
        } else {
           //renderer.setSeriesCreateEntities(index, false); 
        }
        if (series.getColor() != null) {
            renderer.setSeriesPaint(index, series.getColor());
        }
        if (getStyle().isError()) {
            ((XYLineAndShapeRenderer) renderer).setSeriesLinesVisible(index, series.getLinesVisible());
        }               
        return s;
    }

    @Override
    public ch.psi.pshell.utils.Range getAxisRange(AxisId axisId) {
        Range r = null;
        switch (axisId) {
            case X:
                r = chart.getXYPlot().getDomainAxis().getRange();
                return new ch.psi.pshell.utils.Range(r.getLowerBound(), r.getUpperBound());
            case X2:
                r = chart.getXYPlot().getDomainAxis(1).getRange();
                return new ch.psi.pshell.utils.Range(r.getLowerBound(), r.getUpperBound());                
            case Y:
                r = chart.getXYPlot().getRangeAxis().getRange();
                return new ch.psi.pshell.utils.Range(r.getLowerBound(), r.getUpperBound());
            case Y2:
                if (dataY2 == null) {
                    return null;
                }
                r = chart.getXYPlot().getRangeAxis(1).getRange();
                return new ch.psi.pshell.utils.Range(r.getLowerBound(), r.getUpperBound());
            default:
                return null;
        }
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

    @Reflection.Hidden
    public XYSeries getXYSeries(LinePlotSeries series) {
        return (XYSeries) (series.getToken());
    }

    @Reflection.Hidden
    public Series getSeries(LinePlotSeries series) {
        return (Series) (series.getToken());
    }

    public void renameSeries(LinePlotSeries series, String newName) {
        series.setName(newName);
        getSeries(series).setKey(newName);        
    }
    
    /**
     * Get the chart panel of this plot. The chart panel will be lazily created
     * the first time this function is called
     *
     */
    @Override
    protected void createChart() {
        super.createChart();
        createAxis(AxisId.X2, ""); //JFree supportes second domain too
        
        tickLabelFont = TICK_LABEL_FONT;
        labelFont = LABEL_FONT;
        legendVisible = false;
        dataY1 = new XYSeriesCollection();
        // Create chart
        chart = newChart();
        chart.setBackgroundPaint(new Color(0,0,0,0));                  
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
        chart.setBorderVisible(false);

        // Set size of chart
        chartPanel.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

        setBackground(getBackground());

        // Customize plot area look and feel
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(getPlotBackground());
        plot.setDomainGridlinePaint(getGridColor());
        plot.setRangeGridlinePaint(getGridColor());
        plot.setOutlinePaint(getOutlineColor());
        
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

        if (!isOffscreen()) {
            //Activate (arrow) keys
            addKeyBindings();
            setLayout(new BorderLayout());
        }

        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(getAxisTextColor());
        }
        chart.getLegend().setItemPaint(getAxisTextColor());
        chart.getLegend().setItemFont(tickLabelFont);
        plot.getDomainAxis().setTickLabelPaint(getAxisTextColor());
        plot.getDomainAxis().setLabelPaint(getAxisTextColor());
        plot.getRangeAxis().setLabelPaint(getAxisTextColor());
        plot.getRangeAxis().setTickLabelPaint(getAxisTextColor());        
        plot.getDomainAxis().setLabelFont(labelFont);
        plot.getRangeAxis().setLabelFont(labelFont);
        plot.getDomainAxis().setTickLabelFont(tickLabelFont);
        plot.getRangeAxis().setTickLabelFont(tickLabelFont);
        if (!isOffscreen()) {
            add(chartPanel);
        }
    }

    @Override
    public void setLabelFont(Font f) {
        labelFont = f;
        XYPlot plot = (XYPlot) chart.getPlot();        
        plot.getDomainAxis().setLabelFont(f);
        plot.getRangeAxis().setLabelFont(f);
        if (dataY2 != null) {
            plot.getRangeAxis(1).setLabelFont(f);
        }
    }

    @Override
    public void setTickLabelFont(Font f) {
        tickLabelFont = f;
        XYPlot plot = (XYPlot) chart.getPlot();
        chart.getLegend().setItemFont(f);
        plot.getDomainAxis().setTickLabelFont(f);
        plot.getRangeAxis().setTickLabelFont(f);
        if (dataY2 != null) {
            plot.getRangeAxis(1).setTickLabelFont(f);
        }
    }

    public Font getLabelFont() {
        return labelFont;
    }

    public Font getTickLabelFont() {
        return tickLabelFont;
    }
    
    public boolean hasY2(){
        return (dataY2 != null);
    }
    
    public boolean hasX2(){
        return (chart!=null) && chart.getXYPlot().getDomainAxis(1) != null;
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
            boolean hasX2 = hasX2();
            boolean hasY2 = hasY2();
            HashMap<AxisId, Axis> formerAxisList = (HashMap<AxisId, Axis>) axisList.clone();
            LinePlotSeries[] allSeries = getAllSeries();
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
            if (hasX2) {
                createX2();
            }              
            if (!isOffscreen()) {
                createPopupMenu();
            }
            onTitleChanged();
            axisList.clear();
            axisList.putAll(formerAxisList);
            onAxisLabelChanged(Plot.AxisId.X);
            onAxisRangeChanged(Plot.AxisId.X);            
            onAxisLabelChanged(Plot.AxisId.Y);
            onAxisRangeChanged(Plot.AxisId.Y);    
            if(hasX2){
                onAxisLabelChanged(Plot.AxisId.X2);
                onAxisRangeChanged(Plot.AxisId.X2);
            }            
            if(hasY2){
                onAxisLabelChanged(Plot.AxisId.Y2);
                onAxisRangeChanged(Plot.AxisId.Y2);
            }            
            for (SeriesInfo info : existing) {
                if (!getStyle().isError()) {
                    addSeries(info.series);
                    info.series.setData(info.x, info.y);
                }
            }
            updateUI();
        }
    }

    @Override
    protected void onTitleChanged() {
        chartPanel.setName(getTitle());
        chart.setTitle(getTitle());
        if (chart.getTitle() != null){
            if (getTitleFont() != null) {
                chart.getTitle().setFont(getTitleFont());
            }
            if (chart.getTitle() != null) {
                chart.getTitle().setPaint(getAxisTextColor());
            }       
        }
    }

    @Override
    protected void onAxisLabelChanged(AxisId axis) {
        switch (axis) {
            case X:
                chart.getXYPlot().getDomainAxis().setLabel(getAxis(AxisId.X).getLabel());
                break;
            case X2:
                createX2();
                chart.getXYPlot().getDomainAxis(1).setLabel(getAxis(AxisId.X2).getLabel());      
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
            case X2:
                createX2();
                return chart.getXYPlot().getDomainAxis(1);                
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
                axis = (getAxis(axisId).isLogarithmic()) ? new LogarithmicAxis(getAxis(axisId).getLabel()) {                    
                    Range getLogRange(){                        
                        double min = AUTO_RANGE_LOG_MINIMUM_SIZE;
                        double max = AUTO_RANGE_LOG_MINIMUM_SIZE;
                        double aux=Double.POSITIVE_INFINITY;
                        for (LinePlotSeries series : getAllSeries()){
                            if (series.getAxisY() ==  axisId.ordinal()-1){
                                for (double d : series.getY()){
                                    if (!Double.isNaN(d)){
                                        if ((d>AUTO_RANGE_LOG_MINIMUM_SIZE) && (d<aux)){
                                            aux=d;
                                        } 
                                        if (d>max){
                                            max=d;
                                        }
                                    }
                                }
                            }
                        }
                        if (!Double.isInfinite(aux)){
                            min = aux;
                        }
                        return new Range(min, Math.max(max, 10*min));                               
                    }
                    @Override
                    public Range getRange() {
                        Range range = super.getRange();
                        if (range.getLowerBound() <= 0){
                            range = getLogRange();
                            setRange(range);
                        }
                        return range;
                    }
                    
                }: new NumberAxis(getAxis(axisId).getLabel());
                if (getAxis(axisId).isLogarithmic()){
                    ((LogarithmicAxis)axis).setAllowNegativesFlag(false);
                    ((LogarithmicAxis)axis).setStrictValuesFlag(false);
                    ((LogarithmicAxis)axis).setLog10TickLabelsFlag(true); //TODO: only used to axis Y
                    ((LogarithmicAxis)axis).setAutoRangeMinimumSize(AUTO_RANGE_LOG_MINIMUM_SIZE);
                }                
                XYPlot plot = (XYPlot) chart.getPlot();                
                axis.setLabelFont(labelFont);
                axis.setTickLabelFont(tickLabelFont);
                axis.setLabelPaint(getAxisTextColor());
                axis.setTickLabelPaint(getAxisTextColor());
                switch (getAxis(axisId).id) {
                    case X -> plot.setDomainAxis(0, axis);
                    case X2 -> plot.setDomainAxis(1, axis);
                    case Y -> plot.setRangeAxis(0, axis);
                    case Y2 -> plot.setRangeAxis(1, axis);
                }
            }
            axis.setInverted(getAxis(axisId).inverted);
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
    
    
    void notifyZoomListener(){
        if (zoomListener!=null){
            try{
                Range rangeX = getValueAxis(AxisId.X).getRange();
                Range rangeY = getValueAxis(AxisId.Y).getRange();
                super.notifyZoomListener( new ch.psi.pshell.utils.Range(rangeX.getLowerBound(), rangeX.getUpperBound()),
                                          new ch.psi.pshell.utils.Range(rangeY.getLowerBound(), rangeY.getUpperBound()));
            } catch (Exception ex){          
                logger.log(Level.WARNING, null, ex);
            }

        }        
    }
    
    protected ChartPanel newChartPanel(JFreeChart chart) {
        return new ChartPanel(chart, isBufferedRendering()) {
            //TODO: This is to fix JFreeChart bug zooming out when range has been set. 
            //JFreeChart is then performing an auto range which is unexpected.
            //http://www.jfree.org/phpBB2/viewtopic.php?t=23763
            @Override
            public void restoreAutoRangeBounds() {
                super.restoreAutoRangeBounds();
                for (AxisId axisId : ranges.keySet()) {
                    final ValueAxis axis = getValueAxis(axisId);
                    final Range range = ranges.get(axisId);
                    axis.setRange(range);
                }                
                notifyZoomListener();
            }
            @Override
            public void zoom(Rectangle2D selection) {
                super.zoom(selection);
                notifyZoomListener();
            }            
        };
    }

    protected JFreeChart newChart() {
        JFreeChart ret = null;
        switch (getStyle()) {
            case Step:
                ret = ChartFactory.createXYStepChart(getTitle(), getAxis(AxisId.X).getLabel(), getAxis(AxisId.Y).getLabel(), dataY1, PlotOrientation.VERTICAL, true, showTooltips, false);
                NumberAxis axis = new NumberAxis();
                axis.setAutoRangeIncludesZero(false);
                ret.getXYPlot().setDomainAxis(axis);
                break;
            case Spline: {
                NumberAxis xAxis = new NumberAxis(getAxis(AxisId.X).getLabel());
                xAxis.setAutoRangeIncludesZero(false);
                NumberAxis yAxis = new NumberAxis(getAxis(AxisId.Y).getLabel());
                XYSplineRenderer renderer = new XYSplineRenderer();
                XYPlot plot = new XYPlot(dataY1, xAxis, yAxis, renderer);
                plot.setOrientation(PlotOrientation.VERTICAL);
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
                XYErrorRenderer renderer = new ErrorRenderer();
                    if (getStyle()==Style.ErrorY){
                    renderer.setDrawXError(false);
                } else if (getStyle()==Style.ErrorX){
                    renderer.setDrawYError(false);
                }
                
                XYPlot plot = new XYPlot(dataY1, xAxis, yAxis, renderer);
                plot.setOrientation(PlotOrientation.VERTICAL);
                ret = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
                ChartFactory.getChartTheme().apply(ret);
                break;
            default:
                //ret = ChartFactory.createXYLineChart(getTitle(), getAxis(AxisId.X).getLabel(), getAxis(AxisId.Y).getLabel(), dataY1, PlotOrientation.VERTICAL, true, showTooltips, false);
                NumberAxis xa = new NumberAxis(getAxis(AxisId.X).getLabel());
                xa.setAutoRangeIncludesZero(false);
                NumberAxis ya = new NumberAxis(getAxis(AxisId.Y).getLabel());
                XYLineAndShapeRenderer r = new LineAndShapeRenderer();                
                XYPlot xyplot = new XYPlot(dataY1, xa, ya, r);
                xyplot.setOrientation(PlotOrientation.VERTICAL);
                if (showTooltips) {
                    r.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
                }
                ret = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, xyplot, true);
                ChartFactory.getChartTheme().apply(ret);                
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
                if (chart.getXYPlot().getDomainAxis() instanceof DateAxis dateAxis){
                    moverOverPlot(new XYDataItem(dateAxis.getTickUnit().getSize(), 0.0));
                } else {
                    moverOverPlot(new XYDataItem(((NumberAxis) chart.getXYPlot().getDomainAxis()).getTickUnit().getSize(), 0.0));
                }
            }
        });

        // Left arrow
        chartPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), moveLeftKey);
        chartPanel.getActionMap().put(moveLeftKey, new AbstractAction() {
            // Default serial id
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (chart.getXYPlot().getDomainAxis() instanceof DateAxis dateAxis){
                    moverOverPlot(new XYDataItem(-dateAxis.getTickUnit().getSize(), 0.0));
                } else {
                    moverOverPlot(new XYDataItem(-((NumberAxis) chart.getXYPlot().getDomainAxis()).getTickUnit().getSize(), 0.0));
                }
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
            for (Object o : chart.getXYPlot().getAnnotations()) {
                chart.getXYPlot().removeAnnotation((XYAnnotation) o);
            }
            for (LinePlotSeries series : getAllSeries()) {
                double[] min = series.getMinimum();
                XYDrawableAnnotation cross = new XYDrawableAnnotation(min[0], min[1], 10.0, 10.0, new CrossAnnotation(getSeriesColor(series)));
                cross.setToolTipText("Minimum: " + min[0] + " / " + min[1]);
                chart.getXYPlot().addAnnotation(cross);
            }
        });
        toolsMenu.add(minimumMenuItem);

        // Mark maximum value
        JMenuItem maximumMenuItem = new JMenuItem("Maximum");
        maximumMenuItem.addActionListener((ActionEvent e) -> {
            // Remove all annotation for the series
            removePointers();
            for (Object o : chart.getXYPlot().getAnnotations()) {
                chart.getXYPlot().removeAnnotation((XYAnnotation) o);
            }
            for (LinePlotSeries series : getAllSeries()) {
                double[] max = series.getMaximum();
                XYDrawableAnnotation cross = new XYDrawableAnnotation(max[0], max[1], 10.0, 10.0, new CrossAnnotation(getSeriesColor(series)));
                cross.setToolTipText("Maximum: " + max[0] + " / " + max[1]);
                chart.getXYPlot().addAnnotation(cross);
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
            Frame frame = getFrame();
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
            Frame frame = getFrame();
            JDialog dlg = new JDialog(frame, title, false);
            p.setPreferredSize(new Dimension(DETACHED_WIDTH, DETACHED_HEIGHT));
            dlg.setContentPane(p);
            dlg.pack();
            SwingUtils.centerComponent(frame, dlg);
            dlg.setVisible(true);
            dlg.requestFocus();
        });
        toolsMenu.add(integralMenuItem);

        //Select plot style
        JMenu menuStyle = new JMenu("Style");
        for (Style style : new Style[]{Style.Normal, Style.Spline, Style.Step}){
            JRadioButtonMenuItem popupMenuStyle = new JRadioButtonMenuItem(style.toString());
            popupMenuStyle.addActionListener((ActionEvent e) -> {
                setStyle(style);

            });
            menuStyle.add(popupMenuStyle);
        }
        chartPanel.getPopupMenu().add(menuStyle);
        
        
        //Logarithmic axis
        JMenu menuScales= new JMenu("Logarithmic Scale");
        JCheckBoxMenuItem menuScaleLogX= new JCheckBoxMenuItem("X");
        JCheckBoxMenuItem menuScaleLogX2= new JCheckBoxMenuItem("X2");
        JCheckBoxMenuItem menuScaleLogY1= new JCheckBoxMenuItem("Y1");
        JCheckBoxMenuItem menuScaleLogY2= new JCheckBoxMenuItem("Y2");
        menuScaleLogX.addActionListener((ActionEvent e) -> {
            getAxis(AxisId.X).setLogarithmic(menuScaleLogX.isSelected());
        });
        menuScaleLogX2.addActionListener((ActionEvent e) -> {
            getAxis(AxisId.X2).setLogarithmic(menuScaleLogX2.isSelected());
        });        
        menuScaleLogY1.addActionListener((ActionEvent e) -> {
            getAxis(AxisId.Y).setLogarithmic(menuScaleLogY1.isSelected());
        });
        menuScaleLogY2.addActionListener((ActionEvent e) -> {
            getAxis(AxisId.Y2).setLogarithmic(menuScaleLogY2.isSelected());
        });          
        menuScales.add(menuScaleLogX);
        menuScales.add(menuScaleLogX2);
        menuScales.add(menuScaleLogY1);
        menuScales.add(menuScaleLogY2);
        chartPanel.getPopupMenu().add(menuScales);    
        
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

        // Show hide pointer
        JCheckBoxMenuItem contextMenuTimeAxis = new JCheckBoxMenuItem("Time Domain Axis");        
        contextMenuTimeAxis.addActionListener((ActionEvent e) -> {
            boolean timeDomain = contextMenuTimeAxis.isSelected();
            ValueAxis domainAxis = null;
            if (timeDomain){
                domainAxis = new DateAxis(null); //("Time");
            } else {
                domainAxis = new NumberAxis(null);
                ((NumberAxis)domainAxis).setAutoRangeIncludesZero(false);
            }
            domainAxis.setLabelFont(getLabelFont());
            domainAxis.setTickLabelFont(getTickLabelFont());
            domainAxis.setLabelPaint(getAxisTextColor());
            domainAxis.setTickLabelPaint(getAxisTextColor());           
           
            getChart().getXYPlot().setDomainAxis(domainAxis);       
                        
            if (getShowTooltips()){
                showTooltips();
            }
            onAxisRangeChanged(Plot.AxisId.X);
        });
        chartPanel.getPopupMenu().add(contextMenuTimeAxis);
        
        
        
        chartPanel.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                contextMenuItemColorLegendVisible.setSelected(isLegendVisible());
                contextMenuItemToolTip.setSelected(getShowTooltips());                
                menuStyle.setEnabled(!getStyle().isError());
                for (Component c : menuStyle.getMenuComponents()) {
                    ((JRadioButtonMenuItem) c).setSelected(getStyle() == Style.valueOf(((JMenuItem) c).getText()));                
                }               
                menuScaleLogX.setSelected(getAxis(AxisId.X).isLogarithmic());
                menuScaleLogY1.setSelected(getAxis(AxisId.Y).isLogarithmic());
                menuScaleLogY2.setVisible(dataY2 != null);
                menuScaleLogY2.setSelected(getAxis(AxisId.Y2).isLogarithmic());
                menuScaleLogX2.setVisible(hasX2());
                menuScaleLogX2.setSelected(hasX2() && getAxis(AxisId.X2).isLogarithmic());      
                ValueAxis domainAxis = getChart().getXYPlot().getDomainAxis();
                Range range = domainAxis.getRange();                                  
                contextMenuTimeAxis.setVisible((range.getLowerBound()>=TIMESTAMP_2000) && (range.getUpperBound()<=TIMESTAMP_2100));
                contextMenuTimeAxis.setSelected( (domainAxis!=null) && (domainAxis instanceof DateAxis));
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
    
    
    static StandardXYToolTipGenerator tooltipGenerator = new StandardXYToolTipGenerator("x={1} y={2}", displayFormatValue, displayFormatValue);
    static StandardXYToolTipGenerator tooltipGeneratorDate = new StandardXYToolTipGenerator("x={1} y={2}", displayFormatDate, displayFormatValue);
    static int MINIMUM_TOOLTIP_MARKER_SIZE = 3;

    void showTooltips() {
        XYPlot plot = (XYPlot) chart.getPlot(); 
        chartPanel.setDisplayToolTips(true);        
        chartPanel.getChartRenderingInfo().setEntityCollection(new StandardEntityCollection());
        StandardXYToolTipGenerator generator = (chart.getXYPlot().getDomainAxis() instanceof DateAxis)? 
                tooltipGeneratorDate : tooltipGenerator;
        for (int series = 0; series < plot.getDataset(0).getSeriesCount(); series++) {
            getRenderer(1).setSeriesToolTipGenerator(series,generator);
            //getRenderer(1).setSeriesCreateEntities(series, true); 
        }
        if (getRenderer(2) != null) {
            for (int series = 0; series < plot.getDataset(1).getSeriesCount(); series++) {
                getRenderer(2).setSeriesToolTipGenerator(series,generator);
                //getRenderer(2).setSeriesCreateEntities(series, true); 
            }            
        }


        //If marker size smaller than 2 then tooltips are unusable
        if (getMarkerSize() < MINIMUM_TOOLTIP_MARKER_SIZE) {
            changeMarkerSize(MINIMUM_TOOLTIP_MARKER_SIZE);
        }
    }

    void hideTooltips() {
        XYPlot plot = (XYPlot) chart.getPlot(); 
        chartPanel.getChartRenderingInfo().setEntityCollection(null);
        for (int series = 0; series < plot.getDataset(0).getSeriesCount(); series++) {
            getRenderer(1).setSeriesToolTipGenerator(series, null);
            //getRenderer(1).setSeriesCreateEntities(series, false); 
        }
        if (getRenderer(2) != null) {
            for (int series = 0; series < plot.getDataset(1).getSeriesCount(); series++) {
                getRenderer(2).setSeriesToolTipGenerator(series, null);
                //getRenderer(2).setSeriesCreateEntities(series, false); 
            }            
        }
        chartPanel.setDisplayToolTips(false);
        if (getMarkerSize() < MINIMUM_TOOLTIP_MARKER_SIZE) {
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
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        return chartPanel.print(g, pf, page);
    }

    @Override
    protected Color getSeriesColor(LinePlotSeries series) {
        XYItemRenderer renderer = getRenderer(series.getAxisY());
        Paint p = renderer.getSeriesPaint(getSeriesIndex(series));
        return p instanceof Color c ? c : null;
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
    protected void setMaxItemCount(LinePlotSeries series, int value) {
        Series s = getSeries(series);
        if (value<0){
            value=Integer.MAX_VALUE;
        }
        if (s instanceof ComparableObjectSeries cs) {
            cs.setMaximumItemCount(value);
        } else {
            ((XYSeries) s).setMaximumItemCount(value);
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
        invokeLater(() -> {
            if ((axis == null) || (axis == AxisId.X)) {
                chart.getXYPlot().addDomainMarker(marker, Layer.FOREGROUND);
            } else if (axis == AxisId.X2) {
                chart.getXYPlot().addDomainMarker(1, marker, Layer.FOREGROUND);
            } else if (axis == AxisId.Y2) {
                chart.getXYPlot().addRangeMarker(1, marker, Layer.FOREGROUND);   
            } else {
                chart.getXYPlot().addRangeMarker(marker, Layer.FOREGROUND);
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

        invokeLater(() -> {
            if ((axis == null) || (axis == AxisId.X)) {
                chart.getXYPlot().addDomainMarker(marker, Layer.FOREGROUND);
            } else if (axis == AxisId.X2) {
                chart.getXYPlot().addDomainMarker(1, marker, Layer.FOREGROUND);
            } else if (axis == AxisId.Y2) {
                chart.getXYPlot().addRangeMarker(1, marker, Layer.FOREGROUND);
            } else {
                chart.getXYPlot().addRangeMarker(marker, Layer.FOREGROUND);
            }
        });
        return marker;
    }

    @Override
    public void removeMarker(final Object marker) {
        invokeLater(() -> {
            if (marker == null) {
                for (int axis =0; axis<=1; axis++){
                    Collection<?> markers = chart.getXYPlot().getRangeMarkers(axis,Layer.FOREGROUND);
                    if (markers != null) {
                        for (Marker m : markers.toArray(new Marker[0])) {
                            chart.getXYPlot().removeRangeMarker((Marker) m);
                        }
                    }
                    markers = chart.getXYPlot().getDomainMarkers(axis, Layer.FOREGROUND);
                    if (markers != null) {
                        for (Marker m : markers.toArray(new Marker[0])) {
                            chart.getXYPlot().removeDomainMarker((Marker) m);
                        }
                    }                    
                }
            } else {
                chart.getXYPlot().removeDomainMarker((Marker) marker, Layer.FOREGROUND);
                chart.getXYPlot().removeRangeMarker((Marker) marker, Layer.FOREGROUND);
                chart.getXYPlot().removeDomainMarker(1, (Marker) marker, Layer.FOREGROUND);
                chart.getXYPlot().removeRangeMarker(1, (Marker) marker, Layer.FOREGROUND);                
            }
        });
    }

    @Override
    public List getMarkers() {
        List ret = new ArrayList();
        for (int axis =0; axis<=1; axis++){
            Collection dm = chart.getXYPlot().getDomainMarkers(axis, Layer.FOREGROUND);
            Collection rm = chart.getXYPlot().getRangeMarkers(axis, Layer.FOREGROUND);
            if (dm != null) {
                ret.addAll(dm);
            }
            if (rm != null) {
                ret.addAll(rm);
            }
        }
        return ret;
    }

    @Override
    public Object addText(double x, double y, String label, Color color) {
        XYTextAnnotation annotation = new XYTextAnnotation(label, x, y);
        if (color != null) {
            annotation.setPaint(color);
        }
        chart.getXYPlot().addAnnotation(annotation);
        return annotation;
    }

    @Override
    public void removeText(Object text) {
        if (text instanceof XYTextAnnotation ta) {
            chart.getXYPlot().removeAnnotation(ta);
        }
    }

    @Override
    public List getTexts() {
        List ret = new ArrayList();
        ret.addAll(chart.getXYPlot().getAnnotations());
        return ret;
    }

    ChartMouseListener mouseListener;
    XYPointerAnnotation[] pointers;

    void removePointers() {
        if (pointers != null) {
            for (XYPointerAnnotation pointer : pointers) {
                chart.getXYPlot().removeAnnotation(pointer);
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
                            double f = DatasetUtils.findYValue(plot.getDataset(), i, x);
                            String strX = (chart.getXYPlot().getDomainAxis() instanceof DateAxis)? 
                                    getDisplayDateValue(x) : //Time.getTimeString(x, false, false): 
                                    getDisplayValue(x);
                            String text = String.format("x=%s y=%s", strX, getDisplayValue(f));
                            if (pointers[i] == null) {
                                if (mouseListener == null) {
                                    return;
                                }
                                pointers[i] = new XYPointerAnnotation(text, x, f, Math.toRadians(270));
                                pointers[i].setArrowPaint(getAxisTextColor());
                                pointers[i].setPaint(getAxisTextColor());
                                pointers[i].setBaseRadius(0);
                                pointers[i].setArrowLength(5);
                                pointers[i].setTipRadius(10);
                                pointers[i].setLabelOffset(25);
                                pointers[i].setFont(tickLabelFont);
                                chart.getXYPlot().addAnnotation(pointers[i]);
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
    

    @Reflection.Hidden
    public JFreeChart getChart() {
        return chart;
    }

    @Override
    public BufferedImage getSnapshot(Dimension size) {
        if (size == null) {
            size = new Dimension(SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);
        }
        return chart.createBufferedImage(size.width, size.height);
    }

    
    public static JDialog showDialog(Frame parent, String title, double[] x, double[] y) {
        LinePlotJFree p = new LinePlotJFree();
        p.setTitle(null);
        p.getAxis(AxisId.X).setLabel(null);
        p.getAxis(AxisId.Y).setLabel(null);
        p.setName("plot");
        LinePlotSeries series = new LinePlotSeries(title);        
        p.addSeries(series);
        series.setData(x, y);        
        JDialog dlg = new JDialog(parent, title, false);
        p.setPreferredSize(new Dimension(DETACHED_WIDTH, DETACHED_HEIGHT));
        dlg.setContentPane(p);
        dlg.pack();
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        SwingUtils.centerComponent(parent, dlg);
        dlg.setVisible(true);
        dlg.requestFocus();       
        return dlg;
    }    
}