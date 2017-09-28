package ch.psi.pshell.plotter;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.Axis;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.plot.SlicePlotBase;
import ch.psi.pshell.plot.SlicePlotSeries;
import ch.psi.pshell.plot.TimePlotJFree;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.utils.Convert;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class DesktopPlotter implements Plotter {

    final PlotPane pane;

    public DesktopPlotter(PlotPane pane) {
        this.pane = pane;
    }

    //Object acceess functions
    private String[] parseId(String id) {
        return id.split("/");
    }

    private PlotPanel getContext(String[] tokens, boolean create) {
        if (Convert.isInteger(tokens[0])) {
            return pane.getPanel(Integer.valueOf(tokens[0]));
        } else {
            return pane.getPanel(tokens[0], create);
        }
    }

    private PlotPanel getContext(String[] tokens) {
        return getContext(tokens, true);
    }

    PlotPanel getContext(String id, boolean create) {
        return getContext(parseId(id), create);
    }

    PlotPanel getContext(String id) {
        return getContext(parseId(id), true);
    }

    private Plot getPlot(String[] tokens) {
        if (tokens.length < 2) {
            throw new RuntimeException("Invalid plot id");
        }
        if (Convert.isInteger(tokens[1])) {
            return getContext(tokens).getPlot(Integer.valueOf(tokens[1]));
        } else {
            return getContext(tokens).getPlot(tokens[1]);
        }
    }

    Plot getPlot(String id) {
        return getPlot(parseId(id));
    }

    private Axis getAxis(String[] tokens) {
        if (tokens.length < 3) {
            throw new RuntimeException("Invalid axis id");
        }
        Plot plot = getPlot(tokens);
        return plot.getAxis(Plot.AxisId.valueOf(tokens[2].toUpperCase()));
    }

    Axis getAxis(String id) {
        return getAxis(parseId(id));
    }

    private PlotSeries getSeries(String[] tokens) {
        if (tokens.length < 3) {
            throw new RuntimeException("Invalid series id");
        }
        Plot plot = getPlot(tokens);
        if (Convert.isInteger(tokens[2])) {
            return getContext(tokens).getSeries(plot, Integer.valueOf(tokens[2]));
        } else {
            return getContext(tokens).getSeries(plot, tokens[2]);
        }
    }

    PlotSeries getSeries(String id) {
        return getSeries(parseId(id));
    }

    //Public interface
    @Override
    public void clearContexts() {
        pane.clear();
    }

    @Override
    public String[] getContexts() {
        return pane.getPanelsNames().toArray(new String[0]);
    }

    @Override
    public void setContextAttrs(String context, Plot.Quality quality, PlotLayout plotLayout) {
        PlotPanel panel = getContext(context);
        if (quality != null) {
            panel.quality = quality;
        }
        if (plotLayout != null) {
            panel.plotLayout = plotLayout;
        }
    }

    @Override
    public void clearPlots(String context) {
        PlotPanel panel = getContext(context);
        if (panel != null) {
            panel.clear();
        }
    }

    @Override
    public String addLinePlot(String context, String title, LinePlot.Style style) {
        PlotPanel panel = getContext(context);
        int index = panel.getPlots().size();
        panel.addLinePlot(title, style);
        return context + "/" + index;
    }

    @Override
    public String addMatrixPlot(String context, String title, MatrixPlot.Style style, Colormap colormap) {
        PlotPanel panel = getContext(context);
        int index = panel.getPlots().size();
        panel.addMatrixPlot(title, style, colormap);
        return context + "/" + index;
    }

    @Override
    public void setPlotAxisAttrs(String axis, String label, Boolean autoRange, Double min, Double max, Boolean inverted, Boolean logarithmic) {
        String[] tokens = parseId(axis);
        PlotPanel panel = getContext(tokens);
        panel.setPlotAxisAttrs(getAxis(tokens), label, autoRange, min, max, inverted, logarithmic);
    }

    @Override
    public String addMarker(String axis, Double val, String label, String color) {
        String[] tokens = parseId(axis);
        PlotPanel panel = getContext(tokens);
        Plot plot = getPlot(tokens);
        Axis a = getAxis(tokens);
        Object marker = panel.addMarker(plot, a.getId(), val, label, Preferences.getColorFromString(color));
        return tokens[0] + "/" + tokens[1] + "/" + marker.hashCode();
    }

    @Override
    public String addIntervalMarker(String axis, Double start, Double end, String label, String color) {
        String[] tokens = parseId(axis);
        PlotPanel panel = getContext(tokens);
        Plot plot = getPlot(tokens);
        Axis a = getAxis(tokens);
        Object marker = panel.addIntervalMarker(plot, a.getId(), start, end, label, Preferences.getColorFromString(color));
        return tokens[0] + "/" + tokens[1] + "/" + marker.hashCode();
    }

    @Override
    public void removeMarker(String marker) {
        try {
            String[] tokens = parseId(marker);
            PlotPanel panel = getContext(tokens);
            Plot p = getPlot(tokens);
            for (Object m : p.getMarkers()) {
                if (m.hashCode() == Integer.valueOf(tokens[2])) {
                    panel.removeMarker(p, m);
                    return;
                }
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public String addText(String plot, Double x, Double y, String label, String color) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        Plot p = getPlot(tokens);
        Object text = panel.addText(p, x, y, label, Preferences.getColorFromString(color));
        return tokens[0] + "/" + tokens[1] + "/" + text.hashCode();
    }

    @Override
    public void removeText(String text) {
        try {
            String[] tokens = parseId(text);
            PlotPanel panel = getContext(tokens);
            Plot p = getPlot(tokens);
            for (Object t : p.getTexts()) {
                if (t.hashCode() == Integer.valueOf(tokens[2])) {
                    panel.removeText(p, t);
                    return;
                }
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void setLinePlotAttrs(String plot, LinePlot.Style style) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        panel.setLinePlotAttrs((LinePlotBase) getPlot(tokens), style);
    }

    @Override
    public void setMatrixPlotAttrs(String plot, Colormap colormap) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        panel.setMatrixPlotAttrs((MatrixPlotBase) getPlot(tokens), colormap);
    }

    @Override
    public void clearPlot(String plot) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        panel.clearPlot(getPlot(tokens));
    }

    @Override
    public byte[] getPlotSnapshot(String plot, String type) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);

        File f;
        try {
            f = File.createTempFile("PlotServer", "." + type);
            String filename = f.getName();
            f.delete();
            getPlot(tokens).saveSnapshot(filename, type);
            Path p = Paths.get(filename);
            byte[] ret = Files.readAllBytes(p);
            Files.delete(p);
            return ret;
        } catch (IOException ex) {
            Logger.getLogger(DesktopPlotter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public void removeSeries(String series) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.removeSeries(getSeries(tokens));
    }

    @Override
    public String addLineSeries(String plot, String name, String color, Integer axisY, Integer markerSize, Integer lineWidth, Integer maxCount) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        LinePlotBase p = (LinePlotBase) getPlot(tokens);
        int index = p.getNumberOfSeries();
        panel.addLineSeries(p, name, Preferences.getColorFromString(color), axisY, markerSize, lineWidth, maxCount);
        return plot + "/" + index;
    }

    @Override
    public String addMatrixSeries(String plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        MatrixPlotBase p = (MatrixPlotBase) getPlot(tokens);
        int index = p.getNumberOfSeries();
        panel.addMatrixSeries(p, name, minX, maxX, nX, minY, maxY, nY);
        return plot + "/" + index;
    }

    @Override
    public void clearSeries(String series) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.clearSeries(getSeries(tokens));
    }

    @Override
    public void setLineSeriesAttrs(String series, String color, Integer markerSize, Integer lineWidth, Integer maxCount) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.setLineSeriesAttrs((LinePlotSeries) getSeries(tokens), Preferences.getColorFromString(color), markerSize, lineWidth, maxCount);
    }

    @Override
    public void setMatrixSeriesAttrs(String series, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.setMatrixSeriesAttrs((MatrixPlotSeries) getSeries(tokens), minX, maxX, nX, minY, maxY, nY);
    }

    @Override
    public void setLineSeriesData(String series, double[] x, double[] y, double[] error, double[] errorY) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.setLineSeriesData((LinePlotSeries) getSeries(tokens), x, y, error, errorY);
    }

    @Override
    public void setMatrixSeriesData(String series, double[][] data, double[][] x, double[][] y) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.setMatrixSeriesData((MatrixPlotSeries) getSeries(tokens), data, x, y);
    }

    @Override
    public void appendLineSeriesData(String series, double x, double y, double error) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.appendLineSeriesData((LinePlotSeries) getSeries(tokens), x, y, error);
    }

    @Override
    public void appendMatrixSeriesData(String series, double x, double y, double z) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.appendMatrixSeriesData((MatrixPlotSeries) getSeries(tokens), x, y, z);
    }

    @Override
    public void appendLineSeriesDataArray(String series, double[] x, double y[], double[] error) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.appendLineSeriesData((LinePlotSeries) getSeries(tokens), x, y, error);
    }

    @Override
    public void appendMatrixSeriesDataArray(String series, double[] x, double y[], double z[]) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.appendMatrixSeriesData((MatrixPlotSeries) getSeries(tokens), x, y, z);
    }

    @Override
    public void setProgress(String context, Double progress) {
        if (context == null) {
            for (PlotPanel panel : pane.getPanels()) {
                panel.setProgress(progress);
            }
        } else {
            PlotPanel panel = getContext(context);
            panel.setProgress(progress);
        }
    }

    @Override
    public void setStatus(String context, String status) {
        if (context == null) {
            for (PlotPanel panel : pane.getPanels()) {
                panel.setStatus(status);
            }
        } else {
            PlotPanel panel = getContext(context);
            panel.setStatus(status);
        }
    }

    @Override
    public String addTable(String context, String title) {
        PlotPanel panel = getContext(context);
        int index = panel.getPlots().size();
        panel.addTablePlot(title);
        return context + "/" + index;
    }

    @Override
    public void setTableData(String table, String[] header, String[][] data) {
        String[] tokens = parseId(table);
        PlotPanel panel = getContext(tokens);
        Plot plot = getPlot(tokens);
        panel.setTableData((TablePlot) plot, header, data);
    }

    @Override
    public String addTimePlot(String context, String title, Boolean started, Integer duration, Boolean markers) {
        PlotPanel panel = getContext(context);
        int index = panel.getPlots().size();
        panel.addTimePlot(title, started, duration, markers);
        return context + "/" + index;
    }

    @Override
    public String addTimeSeries(String plot, String name, String color, Integer axisY) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        TimePlotJFree p = (TimePlotJFree) getPlot(tokens);
        int index = p.getNumberOfSeries();
        panel.addTimeSeries(p, name, Preferences.getColorFromString(color), axisY);
        return plot + "/" + index;
    }

    @Override
    public void appendTimeSeriesData(String series, Long time, Double value) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.appendTimeSeriesData((TimePlotSeries) getSeries(tokens), (time == null) ? System.currentTimeMillis() : time, (value == null) ? Double.NaN : value);
    }

    @Override
    public void setTimePlotAttrs(String plot, Boolean started, Integer duration, Boolean markers) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        panel.setTimePlotAttrs((TimePlotJFree) getPlot(tokens), started, duration, markers);
    }

    @Override
    public void setTimeSeriesAttrs(String series, String color) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.setTimeSeriesAttrs((TimePlotSeries) getSeries(tokens), Preferences.getColorFromString(color));
    }

    @Override
    public String add3dPlot(String context, String title, Colormap colormap) {
        PlotPanel panel = getContext(context);
        int index = panel.getPlots().size();
        panel.add3dPlot(title, colormap);
        return context + "/" + index;
    }

    @Override
    public String add3dSeries(String plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY, Double minZ, Double maxZ, Integer nZ) {
        String[] tokens = parseId(plot);
        PlotPanel panel = getContext(tokens);
        SlicePlotBase p = (SlicePlotBase) getPlot(tokens);
        int index = p.getNumberOfSeries();
        panel.add3dSeries(p, name, minX, maxX, nX, minY, maxY, nY, minZ, maxZ, nZ);
        return plot + "/" + index;
    }

    @Override
    public void append3dSeriesData(String series, double[][] data) {
        String[] tokens = parseId(series);
        PlotPanel panel = getContext(tokens);
        panel.append3dSeriesData((SlicePlotSeries) getSeries(tokens), data);
    }

}
