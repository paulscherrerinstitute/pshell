package ch.psi.pshell.plotter;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.Axis;
import ch.psi.pshell.plot.LinePlot.Style;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotErrorSeries;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotJFree;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.plot.SlicePlotBase;
import ch.psi.pshell.plot.SlicePlotDefault;
import ch.psi.pshell.plot.SlicePlotSeries;
import ch.psi.pshell.plot.TimePlot;
import ch.psi.pshell.plot.TimePlotJFree;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.utils.Convert;
import ch.psi.utils.swing.MonitoredPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class PlotPanel extends MonitoredPanel {

    static final Logger logger = Logger.getLogger(PlotPanel.class.getName());
    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    final ArrayList<Plot> plots;
    final HashMap<SlicePlotSeries, ArrayList<double[][]>> sliceSeriesData;
    String plotTitle;

    public PlotPanel() {
        initComponents();
        pnGraphs.setLayout(new GridBagLayout());
        plots = new ArrayList<>();
        sliceSeriesData = new HashMap<>();
    }

    public List<Plot> getPlots() {
        return (ArrayList<Plot>) plots.clone();
    }

    Quality quality = null;

    Quality getQuality() {
        return (quality != null) ? quality : getPreferences().quality;
    }

    PlotLayout plotLayout = null;

    PlotLayout getPlotLayout() {
        return (plotLayout != null) ? plotLayout : getPreferences().layout;
    }

    public void setPlotTitle(String plotTitle) {
        this.plotTitle = plotTitle;
    }

    Double progress;

    public void setProgress(Double progress) {
        this.progress = progress;
        ((View) getTopLevelAncestor()).updatePanel();
    }

    String status;

    public void setStatus(String status) {
        this.status = status;
        ((View) getTopLevelAncestor()).updatePanel();
    }

    public void initialize() {
        //Redoing layout because it may have been changed by View's initComponents()
        removeAll();
        setLayout(new BorderLayout());
        add(scrollPane);
    }

    public Preferences getPreferences() {
        return PlotPane.preferences;
    }

    public void clear() {
        plots.clear();
        sliceSeriesData.clear();
        pnGraphs.removeAll();
        panelIndexY = 0;
        panelIndexX = 0;
        validate();
        repaint();
    }

    int panelIndexX = 0;
    int panelIndexY = 0;

    public void addPlot(PlotBase plot) {
        plot.setBackground(getBackground());
        plot.setTitleFont(TITLE_FONT);
        plot.setQuality(getQuality());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = panelIndexX;
        c.gridy = panelIndexY;

        plots.add(plot);
        switch (getPlotLayout()) {
            case Horizontal:
                panelIndexX++;
                break;
            case Vertical:
                panelIndexY++;
                break;
            default:
                panelIndexX++;
                if (panelIndexX > 1) {
                    panelIndexX = 0;
                    panelIndexY++;
                }
        }
        plot.setVisible(true);
        pnGraphs.add(plot, c);
    }

    public LinePlotBase addLinePlot(String title, Style style) {
        LinePlotJFree plot = new LinePlotJFree();
        plot.setQuality(getPreferences().quality);
        plot.setTitle(title);
        //plot.setUpdatesEnabled(false); 
        setLinePlotAttrs(plot, style);
        addPlot(plot);
        validate();
        repaint();
        return plot;
    }

    public MatrixPlotBase addMatrixPlot(String title, MatrixPlot.Style style, Colormap colormap) {
        MatrixPlotBase plot;
        try {
            plot = (MatrixPlotBase) MatrixPlotBase.newPlot(style); //new MatrixPlotJFree();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        plot.setQuality(getPreferences().quality);
        plot.setTitle(title);
        setMatrixPlotAttrs(plot, colormap);
        addPlot(plot);
        validate();
        repaint();
        return plot;
    }

    public TablePlot addTablePlot(String title) {
        TablePlot plot = new TablePlot();
        plot.setTitle(title);
        addPlot(plot);
        validate();
        repaint();
        return plot;
    }

    public void setTableData(TablePlot plot, String[] header, String[][] data) {
        plot.setData(header, data);
    }

    public Plot getPlot(int index) {
        if ((index < 0) || (index >= plots.size())) {
            return null;
        }
        return plots.get(index);
    }

    public Plot getPlot(String title) {
        if (title != null) {
            for (Plot plot : plots) {
                if (title.equals(plot.getTitle())) {
                    return plot;
                }
            }
        }
        return null;
    }

    void setLinePlotAttrs(LinePlotBase plot, Style style) {
        if (style != null) {
            plot.setStyle(style);
        }
    }

    void setMatrixPlotAttrs(MatrixPlotBase plot, Colormap colormap) {
        colormap = (colormap != null) ? colormap : getPreferences().colormap;
        if (plot instanceof MatrixPlotJFree) {
            ((MatrixPlotJFree) plot).setColormap(colormap);
        }
    }

    void setPlotAxisAttrs(Axis axis, String label, Boolean autoRange, Double min, Double max, Boolean inverted, Boolean logarithmic) {
        if ((autoRange != null) && (autoRange == true)) {
            axis.setAutoRange();
        } else {
            if (min != null) {
                axis.setMin(min);
            }
            if (max != null) {
                axis.setMax(max);
            }
        }
        if (inverted != null) {
            axis.setInverted(inverted);
        }
        if (label != null) {
            axis.setLabel(label);
        }
        if (logarithmic != null) {
            axis.setLogarithmic(logarithmic);
        }
    }

    public Object addMarker(Plot plot, Plot.AxisId axis, Double val, String label, Color color) {
        if ((val == null) || (label == null) || (color == null)) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        return plot.addMarker(val, axis, label, color);
    }

    public Object addIntervalMarker(Plot plot, Plot.AxisId axis, Double start, Double end, String label, Color color) {
        if ((start == null) || (end == null) || (label == null) || (color == null)) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        return plot.addIntervalMarker(start, end, axis, label, color);
    }

    public void removeMarker(Plot plot, Object marker) {
        plot.removeMarker(marker);
    }

    public Object addText(Plot plot, Double x, Double y, String label, Color color) {
        if ((x == null) || (y == null) || (label == null)) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        return plot.addText(x, y, label, color);
    }

    public void removeText(Plot plot, Object text) {
        plot.removeText(text);
    }

    Plot assertValidPlot(int index, PlotType type) {
        Plot plot = getPlot(index);
        if (plot == null) {
            throw new IllegalArgumentException("Invalid plot index: " + index);
        }
        if (plot.getClass() != type.getImplementation()) {
            throw new IllegalArgumentException("Invalid plot type: " + type);
        }
        return plot;
    }

    Plot assertValidPlot(String title, PlotType type) {
        Plot plot = getPlot(title);
        if (plot == null) {
            throw new IllegalArgumentException("Invalid plot title: " + title);
        }
        if (type != null) {
            if (plot.getClass() != type.getImplementation()) {
                throw new IllegalArgumentException("Invalid plot type: " + type);
            }
        }
        return plot;
    }

    PlotSeries assertValidSeries(Plot plot, int index) {
        if (plot.getSeries(index) == null) {
            throw new IllegalArgumentException("Invalid plot series: " + index);
        }
        return plot.getSeries(index);
    }

    PlotSeries assertValidSeries(Plot plot, String name) {
        if (plot.getSeries(name) == null) {
            throw new IllegalArgumentException("Invalid plot series: " + name);
        }
        return plot.getSeries(name);
    }

    public LinePlotBase getLinePlot(int index) {
        return (LinePlotBase) assertValidPlot(index, PlotType.Line);
    }

    public MatrixPlotBase getMatrixPlot(int index) {
        return (MatrixPlotBase) assertValidPlot(index, PlotType.Matrix);
    }

    public LinePlotBase getLinePlot(String title) {
        return (LinePlotBase) assertValidPlot(title, PlotType.Line);
    }

    public MatrixPlotBase getMatrixPlot(String title) {
        return (MatrixPlotBase) assertValidPlot(title, PlotType.Matrix);
    }

    public void clearPlot(Plot plot) {
        for (PlotSeries series : plot.getAllSeries()) {
            onSeriesRemoved(series);
        }
        plot.clear();
    }

    public void removeSeries(PlotSeries series) {
        series.getPlot().removeSeries(series);
    }

    public LinePlotSeries addLineSeries(LinePlotBase plot, String name, Color color, Integer axisY, Integer markerSize, Integer lineWidth, Integer maxCount) {
        axisY = (axisY == null) ? 1 : axisY;
        LinePlotSeries series = plot.getStyle().isError() ? new LinePlotErrorSeries(name, color, axisY) : new LinePlotSeries(name, color, axisY);
        plot.addSeries(series);
        setLineSeriesAttrs(series, null, markerSize, lineWidth, maxCount);
        return series;
    }

    public MatrixPlotSeries addMatrixSeries(MatrixPlotBase plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY) {
        MatrixPlotSeries series = new MatrixPlotSeries(name);
        setMatrixSeriesAttrs(series, minX, maxX, nX, minY, maxY, nY);
        plot.addSeries(series);
        return series;
    }

    public PlotSeries getSeries(Plot plot, int seriesIndex) {
        return assertValidSeries(plot, seriesIndex);
    }

    public PlotSeries getSeries(Plot plot, String name) {
        return assertValidSeries(plot, name);
    }

    public LinePlotSeries getLineSeries(LinePlotBase plot, int seriesIndex) {
        return (LinePlotSeries) assertValidSeries(plot, seriesIndex);
    }

    public MatrixPlotSeries getMatrixSeries(MatrixPlotBase plot, int seriesIndex) {
        return (MatrixPlotSeries) assertValidSeries(plot, seriesIndex);
    }

    public LinePlotSeries getLineSeries(LinePlotBase plot, String name) {
        return (LinePlotSeries) assertValidSeries(plot, name);
    }

    public MatrixPlotSeries getMatrixSeries(MatrixPlotBase plot, String name) {
        return (MatrixPlotSeries) assertValidSeries(plot, name);
    }

    public void clearSeries(PlotSeries series) {
        series.clear();
        onSeriesRemoved(series);
    }

    void onSeriesRemoved(PlotSeries series) {
        if (series instanceof SlicePlotSeries) {
            sliceSeriesData.remove((SlicePlotSeries) series);
        }
    }

    public void setLineSeriesAttrs(LinePlotSeries series, Color color, Integer markerSize, Integer lineWidth, Integer maxCount) {
        if (color != null) {
            series.setColor(color);
        }
        if (markerSize != null) {
            if (markerSize >= 0) {
                series.setPointsVisible(true);
                series.setPointSize(markerSize);
            } else {
                series.setPointsVisible(false);
            }
        } else {
            series.setPointSize(getPreferences().markerSize);
        }
        if (lineWidth != null) {
            if (lineWidth >= 0) {
                series.setLinesVisible(true);
                series.setLineWidth(markerSize);
            } else {
                series.setLinesVisible(false);
            }
        }
        if (maxCount != null) {
            series.setMaxItemCount(maxCount);
        }
    }

    public void setMatrixSeriesAttrs(MatrixPlotSeries series, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY) {
        if (nX != null) {
            series.setNumberOfBinsX(nX);
        }
        if (nY != null) {
            series.setNumberOfBinsY(nY);
        }
        if ((minX != null) && (maxX != null)) {
            series.setRangeX(minX, maxX);
        }
        if ((minY != null) && (maxY != null)) {
            series.setRangeY(minY, maxY);
        }
    }

    public void setLineSeriesData(LinePlotSeries series, double[] x, double[] y, double[] error, double[] errorY) {
        if (series instanceof LinePlotErrorSeries) {
            ((LinePlotErrorSeries) series).setData(x, y, error, errorY);
        } else {
            series.setData(x, y);
        }
    }

    public void setMatrixSeriesData(MatrixPlotSeries series, double[][] data, double[][] x, double[][] y) {
        series.setData(data, x, y);
        series.getPlot().update(true);
        validate();
        repaint();
    }

    public void appendLineSeriesData(LinePlotSeries series, double x, double y, double error) {
        if (series instanceof LinePlotErrorSeries) {
            ((LinePlotErrorSeries) series).appendData(x, y, error);
        } else {
            series.appendData(x, y);
        }
    }

    public void appendLineSeriesData(LinePlotSeries series, double[] x, double[] y, double[] error) {
        if ((x == null) || (y == null) || (x.length != y.length)) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        if ((error != null) && (x.length != error.length)) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        for (int i = 0; i < x.length; i++) {
            appendLineSeriesData(series, x[i], y[i], (error == null) ? 0.0 : error[i]);
        }
    }

    public void appendMatrixSeriesData(MatrixPlotSeries series, double x, double y, double z) {
        series.appendData(x, y, z);
    }

    public void appendMatrixSeriesData(MatrixPlotSeries series, double[] x, double[] y, double[] z) {
        if ((x == null) || (y == null) || (z == null) || (x.length != y.length) || (x.length != z.length)) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        for (int i = 0; i < x.length; i++) {
            appendMatrixSeriesData(series, x[i], y[i], z[i]);
        }
    }

    public TimePlot addTimePlot(String title, Boolean started, Integer duration, Boolean markers) {
        TimePlotJFree plot = new TimePlotJFree();
        plot.setQuality(getPreferences().quality);
        plot.setTitle(title);
        //plot.setUpdatesEnabled(false); 
        setTimePlotAttrs(plot, started, duration, markers);
        addPlot(plot);
        validate();
        repaint();
        return plot;
    }

    public TimePlotSeries addTimeSeries(TimePlot plot, String name, Color color, Integer axisY) {
        axisY = (axisY == null) ? 1 : axisY;
        TimePlotSeries series = new TimePlotSeries(name, color, axisY);
        plot.addSeries(series);
        setTimeSeriesAttrs(series, null);
        return series;
    }

    public void appendTimeSeriesData(TimePlotSeries series, long time, double value) {
        series.appendData(time, value);
    }

    public void setTimePlotAttrs(TimePlotJFree plot, Boolean started, Integer duration, Boolean markers) {
        if (duration != null) {
            plot.setDurationMillis(duration);
        }
        if (markers != null) {
            plot.setMarkersVisible(markers);
        }
        if (started != null) {
            if (started) {
                plot.start();
            } else {
                plot.start();
            }
        }
    }

    public void setTimeSeriesAttrs(TimePlotSeries series, Color color) {
        if (color != null) {
            series.setColor(color);
        }
    }

    public SlicePlotBase add3dPlot(String title, Colormap colormap) {
        SlicePlotDefault plot = new SlicePlotDefault();
        plot.setColormap(colormap);
        plot.setQuality(getPreferences().quality);
        plot.setTitle(title);
        addPlot(plot);
        validate();
        repaint();
        return plot;
    }

    public SlicePlotSeries add3dSeries(SlicePlotBase plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY, Double minZ, Double maxZ, Integer nZ) {
        SlicePlotSeries series = new SlicePlotSeries(name);

        if (nX != null) {
            series.setNumberOfBinsX(nX);
        }
        if (nY != null) {
            series.setNumberOfBinsY(nY);
        }
        if ((minX != null) && (maxX != null)) {
            series.setRangeX(minX, maxX);
        }
        if ((minY != null) && (maxY != null)) {
            series.setRangeY(minY, maxY);
        }
        plot.addSeries(series);
        series.setListener((SlicePlotSeries s, int page) -> {
            if (page >= sliceSeriesData.get(s).size()) {
                s.clear();
            } else {
                double[][] data = sliceSeriesData.get(s).get(page);
                s.setData(data);
            }
        });

        if (nZ != null) {
            series.setNumberOfBinsZ(nZ);
        }
        if ((minZ != null) && (maxZ != null)) {
            series.setRangeZ(minZ, maxZ);
        }
        return series;
    }

    public void append3dSeriesData(SlicePlotSeries series, double[][] data) {
        if (!sliceSeriesData.keySet().contains(series)) {
            sliceSeriesData.put(series, new ArrayList<>());
        }
        ArrayList d = sliceSeriesData.get(series);
        d.add(data);
        int frames = d.size();
        if (series.getNumberOfBinsZ() < frames) {
            series.setNumberOfBinsZ(frames);
        }
        boolean firstFrame = (frames == 1);
        boolean currentFrame = ((frames - 1) == series.getPage());
        if (firstFrame || currentFrame) {
            series.setData(data);
        }
    }

//After setting data    
//            plot.update(true);
//            plot.setUpdatesEnabled(true);//This plots is user general-purpose so disable scan optimization
//            validate();
//            repaint();
    Object validatePlotDataType(Object data) {
        try {
            data = Convert.toDouble(data);
            if (data != null) {
                return data;
            }
        } catch (Exception ex) {
        }
        throw new IllegalArgumentException("Invalid array type: " + data.getClass());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        pnGraphs = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout pnGraphsLayout = new javax.swing.GroupLayout(pnGraphs);
        pnGraphs.setLayout(pnGraphsLayout);
        pnGraphsLayout.setHorizontalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 354, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 301, Short.MAX_VALUE)
        );

        scrollPane.setViewportView(pnGraphs);

        add(scrollPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pnGraphs;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
