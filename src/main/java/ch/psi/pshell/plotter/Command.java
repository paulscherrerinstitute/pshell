package ch.psi.pshell.plotter;

import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot;
import java.io.IOException;

/**
 *
 */
public abstract class Command {

    public Class returnType = String.class;

    abstract public String invoke(Plotter pm) throws Exception;

    /**
     * Default return type is string String
     */
    public Object translate(String rx) throws Exception {
        if ((returnType == String.class) || (rx == null)) {
            return rx;
        }
        return JsonSerializer.decode(rx, returnType);
    }

    public static class AddIntervalMarker extends Command {

        public String axis;
        public Double start;
        public Double end;
        public String label;
        public String color;

        public AddIntervalMarker() {
        }

        public AddIntervalMarker(String axis, Double start, Double end, String label, String color) {
            //super(AddLinePlot.class.getSimpleName());
            this.axis = axis;
            this.start = start;
            this.end = end;
            this.label = label;
            this.color = color;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addIntervalMarker(axis, start, end, label, color);
        }
    }

    public static class AddLinePlot extends Command {

        public String context;
        public String title;
        public LinePlot.Style style;

        public AddLinePlot() {
        }

        public AddLinePlot(String context, String title, LinePlot.Style style) {
            this.context = context;
            this.title = title;
            this.style = style;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addLinePlot(context, title, style);
        }
    }

    public static class AddLineSeries extends Command {

        public String plot;
        public String name;
        public String color;
        public Integer axisY;
        public Integer markerSize;
        public Integer lineWidth;
        public Integer maxCount;

        public AddLineSeries() {
        }

        public AddLineSeries(String plot, String name, String color, Integer axisY, Integer markerSize, Integer lineWidth, Integer maxCount) {
            this.plot = plot;
            this.name = name;
            this.color = color;
            this.axisY = axisY;
            this.markerSize = markerSize;
            this.lineWidth = lineWidth;
            this.maxCount = maxCount;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addLineSeries(plot, name, color, axisY, markerSize, lineWidth, maxCount);
        }
    }

    public static class AddMarker extends Command {

        public String axis;
        public Double val;
        public String label;
        public String color;

        public AddMarker() {
        }

        public AddMarker(String axis, Double val, String label, String color) {
            this.axis = axis;
            this.val = val;
            this.label = label;
            this.color = color;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addMarker(axis, val, label, color);
        }
    }

    public static class AddMatrixPlot extends Command {

        public String context;
        public String title;
        public MatrixPlot.Style style;
        public Colormap colormap;

        public AddMatrixPlot() {
        }

        public AddMatrixPlot(String context, String title, MatrixPlot.Style style, Colormap colormap) {
            //super(AddLinePlot.class.getSimpleName());
            this.context = context;
            this.title = title;
            this.colormap = colormap;
            this.style = style;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addMatrixPlot(context, title, style, colormap);
        }
    }

    public static class AddMatrixSeries extends Command {

        public String plot;
        public String name;
        public Double minX;
        public Double maxX;
        public Integer nX;
        public Double minY;
        public Double maxY;
        public Integer nY;

        public AddMatrixSeries() {
        }

        public AddMatrixSeries(String plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY) {
            //super(AddLinePlot.class.getSimpleName());
            this.plot = plot;
            this.name = name;
            this.minX = minX;
            this.maxX = maxX;
            this.nX = nX;
            this.minY = minY;
            this.maxY = maxY;
            this.nY = nY;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addMatrixSeries(plot, name, minX, maxX, nX, minY, maxY, nY);
        }
    }

    public static class Add3dPlot extends Command {

        public String context;
        public String title;
        public Colormap colormap;

        public Add3dPlot() {
        }

        public Add3dPlot(String context, String title, Colormap colormap) {
            //super(AddLinePlot.class.getSimpleName());
            this.context = context;
            this.title = title;
            this.colormap = colormap;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.add3dPlot(context, title, colormap);
        }
    }

    public static class Add3dSeries extends Command {

        public String plot;
        public String name;
        public Double minX;
        public Double maxX;
        public Integer nX;
        public Double minY;
        public Double maxY;
        public Integer nY;
        public Double minZ;
        public Double maxZ;
        public Integer nZ;

        public Add3dSeries() {
        }

        public Add3dSeries(String plot, String name, Double minX, Double maxX, Integer nX, Double minY, Double maxY, Integer nY, Double minZ, Double maxZ, Integer nZ) {
            //super(AddLinePlot.class.getSimpleName());
            this.plot = plot;
            this.name = name;
            this.minX = minX;
            this.maxX = maxX;
            this.nX = nX;
            this.minY = minY;
            this.maxY = maxY;
            this.nY = nY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.nZ = nZ;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.add3dSeries(plot, name, minX, maxX, nX, minY, maxY, nY, minZ, maxZ, nZ);
        }
    }

    public static class AddText extends Command {

        public String plot;
        public Double x;
        public Double y;
        public String label;
        public String color;

        public AddText() {
        }

        public AddText(String plot, Double x, Double y, String label, String color) {
            this.plot = plot;
            this.x = x;
            this.y = y;
            this.label = label;
            this.color = color;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addText(plot, x, y, label, color);
        }
    }

    public static class AppendLineSeriesData extends Command {

        public String series;
        public Double x;
        public Double y;
        public Double error;

        public AppendLineSeriesData() {
        }

        public AppendLineSeriesData(String series, Double x, Double y, Double error) {
            this.series = series;
            this.x = x;
            this.y = y;
            this.error = error;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.appendLineSeriesData(series, x, y, (error == null) ? 0.0 : error);
            return null;
        }
    }

    public static class AppendLineSeriesDataArray extends Command {

        public String series;
        public double[] x;
        public double[] y;
        public double[] error;

        public AppendLineSeriesDataArray() {
        }

        public AppendLineSeriesDataArray(String series, double[] x, double[] y, double[] error) {
            this.series = series;
            this.x = x;
            this.y = y;
            this.error = error;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.appendLineSeriesDataArray(series, x, y, error);
            return null;
        }
    }

    public static class AppendMatrixSeriesData extends Command {

        public String series;
        public Double x;
        public Double y;
        public Double z;

        public AppendMatrixSeriesData() {
        }

        public AppendMatrixSeriesData(String series, Double x, Double y, Double z) {
            this.series = series;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.appendMatrixSeriesData(series, x, y, z);
            return null;
        }
    }

    public static class AppendMatrixSeriesDataArray extends Command {

        public String series;
        public double[] x;
        public double[] y;
        public double[] z;

        public AppendMatrixSeriesDataArray() {
        }

        public AppendMatrixSeriesDataArray(String series, double[] x, double[] y, double[] z) {
            this.series = series;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.appendMatrixSeriesDataArray(series, x, y, z);
            return null;
        }
    }

    public static class Append3dSeriesData extends Command {

        public String series;
        public double[][] data;

        public Append3dSeriesData() {
        }

        public Append3dSeriesData(String series, double[][] data) {
            this.series = series;
            this.data = data;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.append3dSeriesData(series, data);
            return null;
        }
    }

    public static class ClearContexts extends Command {

        @Override
        public String invoke(Plotter pm) {
            pm.clearContexts();
            return null;
        }
    }

    public static class ClearPlot extends Command {

        public String plot;

        public ClearPlot() {
        }

        public ClearPlot(String plot) {
            this.plot = plot;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.clearPlot(plot);
            return null;
        }
    }

    public static class ClearPlots extends Command {

        public String context;

        public ClearPlots() {
        }

        public ClearPlots(String context) {
            this.context = context;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.clearPlots(context);
            return null;
        }
    }

    public static class ClearSeries extends Command {

        public String series;

        public ClearSeries() {
        }

        public ClearSeries(String series) {
            this.series = series;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.clearSeries(series);
            return null;
        }
    }

    public static class GetContexts extends Command {

        public GetContexts() {
            returnType = String[].class;
        }

        //public String dummy;
        @Override
        public String invoke(Plotter pm) throws IOException {
            return JsonSerializer.encode(pm.getContexts());
        }
    }

    public static class RemoveSeries extends Command {

        public String series;

        public RemoveSeries() {
        }

        public RemoveSeries(String series) {
            this.series = series;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.removeSeries(series);
            return null;
        }
    }

    public static class RemoveMarker extends Command {

        public String marker;

        public RemoveMarker() {
        }

        public RemoveMarker(String marker) {
            this.marker = marker;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.removeMarker(marker);
            return null;
        }
    }

    public static class RemoveText extends Command {

        public String text;

        public RemoveText() {
        }

        public RemoveText(String text) {
            this.text = text;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.removeText(text);
            return null;
        }
    }

    public static class SetContextAttrs extends Command {

        public String context;
        public Plot.Quality quality;
        public ch.psi.pshell.plotter.PlotLayout plotLayout;

        public SetContextAttrs() {
        }

        public SetContextAttrs(String context, Plot.Quality quality, ch.psi.pshell.plotter.PlotLayout plotLayout) {
            this.context = context;
            this.quality = quality;
            this.plotLayout = plotLayout;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setContextAttrs(context, quality, plotLayout);
            return null;
        }
    }

    public static class SetLinePlotAttrs extends Command {

        public String plot;
        public LinePlot.Style style;

        public SetLinePlotAttrs() {
        }

        public SetLinePlotAttrs(String plot, LinePlot.Style style) {
            this.plot = plot;
            this.style = style;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setLinePlotAttrs(plot, style);
            return null;
        }
    }

    public static class SetLineSeriesAttrs extends Command {

        public String series;
        public String color;
        public Integer markerSize;
        public Integer lineWidth;
        public Integer maxCount;

        public SetLineSeriesAttrs() {
        }

        public SetLineSeriesAttrs(String series, String color, Integer markerSize, Integer lineWidth, Integer maxCount) {
            this.series = series;
            this.color = color;
            this.markerSize = markerSize;
            this.lineWidth = lineWidth;
            this.maxCount = maxCount;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setLineSeriesAttrs(series, color, markerSize, lineWidth, maxCount);
            return null;
        }
    }

    public static class SetLineSeriesData extends Command {

        public String series;
        public double[] x;
        public double[] y;
        public double[] error;
        public double[] errorY;

        public SetLineSeriesData() {
        }

        public SetLineSeriesData(String series, double[] x, double[] y, double[] error, double[] errorY) {
            this.series = series;
            this.x = x;
            this.y = y;
            this.error = error;
            this.errorY = errorY;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setLineSeriesData(series, x, y, error, errorY);
            return null;
        }
    }

    public static class SetMatrixPlotAttrs extends Command {

        public String plot;
        public Colormap colormap;

        public SetMatrixPlotAttrs() {
        }

        public SetMatrixPlotAttrs(String series, Colormap colormap) {
            this.plot = series;
            this.colormap = colormap;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setMatrixPlotAttrs(plot, colormap);
            return null;
        }
    }

    public static class SetMatrixSeriesAttrs extends Command {

        public String series;
        public Double minX;
        public Double maxX;
        public Integer nX;
        public Double minY;
        public Double maxY;
        public Integer nY;

        public SetMatrixSeriesAttrs() {
        }

        public SetMatrixSeriesAttrs(String series, double minX, double maxX, int nX, double minY, double maxY, int nY) {
            this.series = series;
            this.minX = minX;
            this.maxX = maxX;
            this.nX = nX;
            this.minY = minY;
            this.maxY = maxY;
            this.nY = nY;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setMatrixSeriesAttrs(series, minX, maxX, nX, minY, maxY, nY);
            return null;
        }
    }

    public static class SetMatrixSeriesData extends Command {

        public String series;
        public double[][] x;
        public double[][] y;
        public double[][] data;

        public SetMatrixSeriesData() {
        }

        public SetMatrixSeriesData(String series, double[][] data, double[][] x, double[][] y) {
            this.series = series;
            this.x = x;
            this.y = y;
            this.data = data;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setMatrixSeriesData(series, data, x, y);
            return null;
        }
    }

    public static class SetPlotAxisAttrs extends Command {

        public String axis;
        public String label;
        public Boolean autoRange;
        public Double min;
        public Double max;
        public Boolean inverted;
        public Boolean logarithmic;

        public SetPlotAxisAttrs() {
        }

        public SetPlotAxisAttrs(String axis, String label, Boolean autoRange, Double min, Double max, Boolean inverted, Boolean logarithmic) {
            this.axis = axis;
            this.label = label;
            this.autoRange = autoRange;
            this.min = min;
            this.max = max;
            this.inverted = inverted;
            this.logarithmic = logarithmic;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setPlotAxisAttrs(axis, label, autoRange, min, max, inverted, logarithmic);
            return null;
        }
    }

    public static class SetProgress extends Command {

        public String context;
        public Double progress;

        public SetProgress() {
        }

        public SetProgress(String context, Double progress) {
            this.context = context;
            this.progress = progress;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setProgress(context, progress);
            return null;
        }
    }

    public static class SetStatus extends Command {

        public String context;
        public String status;

        public SetStatus() {
        }

        public SetStatus(String context, String progress) {
            this.context = context;
            this.status = progress;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setStatus(context, status);
            return null;
        }
    }

    public static class AddTable extends Command {

        public String context;
        public String title;

        public AddTable() {
        }

        public AddTable(String context, String title) {
            this.context = context;
            this.title = title;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addTable(context, title);
        }
    }

    public static class SetTableData extends Command {

        public String table;
        public String[] header;
        public String[][] data;

        public SetTableData() {
        }

        public SetTableData(String table, String[] header, String[][] data) {
            this.table = table;
            this.header = header;
            this.data = data;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setTableData(table, header, data);
            return null;
        }
    }

    public static class GetPlotSnapshot extends Command {

        public String plot;
        public String type;

        public GetPlotSnapshot() {
            returnType = byte[].class;
        }

        public GetPlotSnapshot(String plot, String type) {
            this();
            this.plot = plot;
            this.type = type;
        }

        @Override
        public String invoke(Plotter pm) throws IOException {
            byte[] ret = pm.getPlotSnapshot(plot, type);
            //return new String(ret);
            //return new String(ret, "UTF-8"); 
            return JsonSerializer.encode(ret);
        }
    }

    public static class AddTimePlot extends Command {

        public String context;
        public String title;
        public Boolean started;
        public Integer duration;
        public Boolean markers;

        public AddTimePlot() {
        }

        public AddTimePlot(String context, String title, Boolean started, Integer duration, Boolean markers) {
            this.context = context;
            this.title = title;
            this.started = started;
            this.duration = duration;
            this.markers = markers;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addTimePlot(context, title, started, duration, markers);
        }
    }

    public static class AddTimeSeries extends Command {

        public String plot;
        public String name;
        public String color;
        public Integer axisY;

        public AddTimeSeries() {
        }

        public AddTimeSeries(String plot, String name, String color, Integer axisY) {
            this.plot = plot;
            this.name = name;
            this.color = color;
            this.axisY = axisY;
        }

        @Override
        public String invoke(Plotter pm) {
            return pm.addTimeSeries(plot, name, color, axisY);
        }
    }

    public static class AppendTimeSeriesData extends Command {

        public String series;
        public Long time;
        public Double value;

        public AppendTimeSeriesData() {
        }

        public AppendTimeSeriesData(String series, Long time, Double value) {
            this.series = series;
            this.time = time;
            this.value = value;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.appendTimeSeriesData(series, time, value);
            return null;
        }
    }

    public static class SetTimePlotAttrs extends Command {

        public String plot;
        public Boolean started;
        public Integer duration;
        public Boolean markers;

        public SetTimePlotAttrs() {
        }

        public SetTimePlotAttrs(String plot, Boolean started, Integer duration, Boolean markers) {
            this.plot = plot;
            this.started = started;
            this.duration = duration;
            this.markers = markers;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setTimePlotAttrs(plot, started, duration, markers);
            return null;
        }
    }

    public static class SetTimeSeriesAttrs extends Command {

        public String series;
        public String color;

        public SetTimeSeriesAttrs() {
        }

        public SetTimeSeriesAttrs(String series, String color) {
            this.series = series;
            this.color = color;
        }

        @Override
        public String invoke(Plotter pm) {
            pm.setTimeSeriesAttrs(series, color);
            return null;
        }
    }
}
