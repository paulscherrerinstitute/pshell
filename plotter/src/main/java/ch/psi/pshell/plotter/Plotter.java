package ch.psi.pshell.plotter;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotLayout;

/**
 *
 */
public interface Plotter {

    String addIntervalMarker(String axis, Double start, Double end, String label, String color);

    String addLinePlot(String context, String title, LinePlot.Style style);

    String addLineSeries(String plot, String name, String color, Integer axisY, Integer markerSize, Integer lineWidth, Integer maxCount);

    String addMarker(String axis, Double val, String label, String color);

    String addMatrixPlot(String context, String title, MatrixPlot.Style style, Colormap colormap);

    String addMatrixSeries(String plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY);

    String add3dPlot(String context, String title, Colormap colormap);

    String add3dSeries(String plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY, Double minZ, Double maxZ, Integer nZ);

    String addTimePlot(String context, String title, Boolean started, Integer duration, Boolean markers);

    String addTimeSeries(String plot, String name, String color, Integer axisY);

    String addTable(String context, String title);

    String addText(String plot, Double x, Double y, String label, String color);

    void appendLineSeriesData(String series, double x, double y, double error);

    void appendLineSeriesDataArray(String series, double[] x, double[] y, double[] error);

    /**
     * If value == null add terminator. If time == null uses System.currentTimeMillis
     */
    void appendTimeSeriesData(String series, Long time, Double value);

    void appendMatrixSeriesData(String series, double x, double y, double z);

    void appendMatrixSeriesDataArray(String series, double[] x, double[] y, double[] z);

    void append3dSeriesData(String series, double[][] data);

    void clearContexts();

    void clearPlot(String plot);

    void clearPlots(String context);

    void clearSeries(String series);

    String[] getContexts();

    byte[] getPlotSnapshot(String plot, String type, Integer width, Integer height);

    void removeSeries(String series);

    void removeMarker(String marker);

    void removeText(String text);

    void setContextAttrs(String context, Plot.Quality quality, PlotLayout plotLayout);

    void setLinePlotAttrs(String plot, LinePlot.Style style);

    void setLineSeriesAttrs(String series, String color, Integer markerSize, Integer lineWidth, Integer maxCount);

    void setLineSeriesData(String series, double[] x, double[] y, double[] error, double[] errorY);

    void setMatrixPlotAttrs(String plot, Colormap colormap);

    void setMatrixSeriesAttrs(String series, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY);

    void setMatrixSeriesData(String series, double[][] data, double[][] x, double[][] y);

    void setTimePlotAttrs(String plot, Boolean started, Integer duration, Boolean markers);

    void setTimeSeriesAttrs(String series, String color);

    void setPlotAxisAttrs(String axis, String label, Boolean autoRange, Double min, Double max, Boolean inverted, Boolean logarithmic);

    void setProgress(String context, Double progress);

    void setStatus(String context, String status);

    void setTableData(String table, String[] header, String[][] data);

}
