package ch.psi.pshell.xscan.plot;

import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.utils.EventBusListener;
import ch.psi.pshell.utils.Message;
import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.EndOfStreamMessage;
import ch.psi.pshell.xscan.StreamDelimiterMessage;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visualizer for visualizing data
 */
public class Visualizer implements EventBusListener {

    private static Logger logger = Logger.getLogger(Visualizer.class.getName());

    private boolean updateAtStreamElement = true;
    private boolean updateAtStreamDelimiter = false;
    private boolean updateAtEndOfStream = false;

    private int ecount;
    private boolean clearPlot;
    private List<SeriesDataFilter> filters;

    int subsampling;
    int subsamplingCounter = 0;

    public static final Font TITLE_FONT = new Font("Tahoma", Font.BOLD, 18);

    private boolean first = true;

    final List<Plot> plots = new ArrayList<>();
    

    //!!! Set this instead of using preferences.
    public static String linePlotImpl = ch.psi.pshell.plot.LinePlotJFree.class.getName();
    public static  String matrixPlotImpl = ch.psi.pshell.plot.MatrixPlotJFree.class.getName();
    public static  String surfacePlotImpl = null;
    

    public Visualizer(VDescriptor vdescriptor) {
        filters = new ArrayList<SeriesDataFilter>();

        try {
            for (ch.psi.pshell.xscan.plot.Plot vplot : vdescriptor.getPlots()) {
                if (vplot instanceof ch.psi.pshell.xscan.plot.LinePlot lp) {
                    // Create plot for visualization
                    LinePlot plot = (LinePlot) Plot.newPlot(linePlotImpl);
                    plot.setTitle(lp.getTitle());
                    plot.getAxis(Plot.AxisId.X).setLabel(null);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    plot.setTitleFont(TITLE_FONT);
                    plot.update(true);
                    plot.setUpdatesEnabled(false);
                    plots.add(plot);

                    for (Series s : lp.getData()) {
                        if (s instanceof XYSeries sxy) {
                            XYSeriesDataFilter filter = new XYSeriesDataFilter(sxy.getX(), sxy.getY(), plot);
                            filter.setSeriesName(sxy.getY());
                            if (sxy.getMaxItemCount() >= 0) {
                                filter.setMaxNumberOfPoints(sxy.getMaxItemCount());
                            }
                            filters.add(filter);
                        } else if (s instanceof YSeries sy) {
                            XYSeriesArrayDataFilter filter = new XYSeriesArrayDataFilter(sy.getY(), plot);
                            //						filter.setMaxSeries(lp.getMaxSeries()*lp.getY().size()); // Workaround - keep for each array max series
                            //						filter.setOffset(lp.getOffset());
                            //						filter.setSize(lp.getSize());
                            filter.setMaxSeries(lp.getMaxSeries());
                            filter.setOffset(lp.getMinX().intValue());
                            filter.setSize(Double.valueOf(lp.getMaxX() - lp.getMinX()).intValue());
                            filter.setSeriesName(sy.getY());
                            filters.add(filter);
                        }
                    }

                } else if (vplot instanceof ch.psi.pshell.xscan.plot.MatrixPlot lp) {
                    // MatrixPlot does currently not support RegionPositioners because of the
                    // plotting problems this would cause. If regions of the positioner have different
                    // step sizes it is not easily possible (without (specialized) rasterization) to plot the data.
                    MatrixPlotSeries data = new MatrixPlotSeries("", lp.getMinX(), lp.getMaxX(), lp.getnX(), lp.getMinY(), lp.getMaxY(), lp.getnY());
                    if (("3D".equals(lp.getType()) && (surfacePlotImpl != null) && (!surfacePlotImpl.isEmpty()))) {
                        matrixPlotImpl = surfacePlotImpl;
                    }
                    MatrixPlot plot = (MatrixPlot) Plot.newPlot(matrixPlotImpl);
                    plot.setTitle(lp.getTitle());
                    plot.setTitleFont(TITLE_FONT);
                    plot.addSeries(data);
                    plot.update(true);
                    plot.setUpdatesEnabled(false);
                    plots.add(plot);

                    for (Series s : lp.getData()) {
                        if (s instanceof XYZSeries sxyz) {
                            XYZSeriesDataFilter filter = new XYZSeriesDataFilter(sxyz.getX(), sxyz.getY(), sxyz.getZ(), plot);
                            filter.setSeries(data);
                            filters.add(filter);

                        } else if (s instanceof YZSeries syz) {
                            XYZSeriesArrayDataFilter filter = new XYZSeriesArrayDataFilter(syz.getY(), syz.getZ(), 0, 0, plot);
                            filter.setSeries(data);
                            filters.add(filter);

                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Ignore if something goes wrong while adding a datapoint
            logger.log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void onMessage(final Message message) {
        if (message instanceof DataMessage dataMessage) {
            onDataMessage(dataMessage);
        } else if (message instanceof StreamDelimiterMessage streamDelimiterMessage) {
            onStreamDelimiterMessage(streamDelimiterMessage);
        } else if (message instanceof EndOfStreamMessage endOfStreamMessage) {
            onEndOfStreamMessage(endOfStreamMessage);
        }
    }

    //@Subscribe
    public void onDataMessage(final DataMessage message) {

        if (first) {
            first = false;
//				filters = VisMapper.mapVisualizations(visualizations);
            // TODO somehow handle dimension / clear
        }

        // Clear is here as the plot should not be cleared after the last point is plotted
        // but just before the first point of the next plot (cycle)
        if (clearPlot) {
            for (SeriesDataFilter f : filters) {
                Plot plot = f.getPlot();
                if (plot instanceof MatrixPlot) {
                    f.getSeries().clear();
                }
            }
            clearPlot = false;
        }

        if (subsampling > 1) {
            if ((subsamplingCounter++) > subsampling) {
                subsamplingCounter = 0;
            } else {
                return;
            }
        }
        for (SeriesDataFilter filter : filters) {
            if (filter instanceof XYSeriesDataFilter xyfilter) {
                if (xyfilter.getSeries() == null || xyfilter.isNewseries()) {
                    // First series that is filled by this filter!
                    LinePlotSeries s = new LinePlotSeries(xyfilter.getSeriesName() + " " + ecount + "-" + xyfilter.getCount());
                    if (xyfilter.getMaxNumberOfPoints() >= 0) {
                        s.setMaxItemCount(xyfilter.getMaxNumberOfPoints());
                    }
                    ((LinePlot) xyfilter.getPlot()).addSeries(s);
                    xyfilter.setSeries(s);
                    xyfilter.setNewseries(false);
                }

                LinePlotSeries series = xyfilter.getSeries(); // TODO Does not work with multiple series filter per plot !!!!					

                // There might be other values than double in the data, therefore we have to check for it
                Object dX = message.getData(xyfilter.getIdX());
                Object dY = message.getData(xyfilter.getIdY());
                Double dataX = getDoubleValue(dX);
                Double dataY = getDoubleValue(dY);

                // Add Data to the series
                //((LinePlot)xyfilter.getPlot()).setUpdatesEnabled(updateAtStreamElement);
                series.appendData(dataX, dataY);
                //((LinePlot)xyfilter.getPlot()).setUpdatesEnabled(true);                                        
            }
            if (filter instanceof XYSeriesArrayDataFilter xyfilter) {
                // Ensure that there is no concurrent modification exception or synchronization problems with the
                // Swing update task
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        LinePlotSeries series = new LinePlotSeries(xyfilter.getSeriesName() + "-" + xyfilter.getCount()); // Series name must be unique
                        xyfilter.incrementCount();

//								((LinePlot)xyfilter.getPlot()).getData().removeAllSeries(); // Remove all series from the data
                        // If we can agree only to display one series at a time also a clear() on the actual series is better
                        ((LinePlot) xyfilter.getPlot()).addSeries(series);
                        xyfilter.setSeries(series);

                        // Remove outdated series
                        if (((LinePlot) xyfilter.getPlot()).getNumberOfSeries() > xyfilter.getMaxSeries()) {
                            // Remove oldest series
                            LinePlotSeries s = ((LinePlot) xyfilter.getPlot()).getSeries(0);
                            ((LinePlot) xyfilter.getPlot()).removeSeries(s);
                        }

                        double[] data = message.getData(xyfilter.getIdY());

                        // Copy data starting from offset to size
                        int size = data.length;
                        int offset = xyfilter.getOffset();
                        if (xyfilter.getSize() > 0 && offset + xyfilter.getSize() < data.length) {
                            size = offset + xyfilter.getSize();
                        }

                        //((LinePlot)xyfilter.getPlot()).setUpdatesEnabled(updateAtStreamElement);
                        for (int i = offset; i < size; i++) {
                            //series.add(i,data[i], false); // Do not fire change event - this would degrade performance drastically
                            series.appendData(i, data[i]);
                        }
                        //((LinePlot)xyfilter.getPlot()).setUpdatesEnabled(true);
                    }

                });
            } else if (filter instanceof XYZSeriesDataFilter xyzfilter) {
                try {
//                                            xyzfilter.getPlot().setUpdatesEnabled(updateAtStreamElement);
                    xyzfilter.getPlot().setUpdatesEnabled(false);
                    xyzfilter.getSeries().appendData((Double) message.getData(xyzfilter.getIdX()), (Double) message.getData(xyzfilter.getIdY()), (Double) message.getData(xyzfilter.getIdZ()));
                    //xyzfilter.getPlot().setUpdatesEnabled(true);
                } catch (Exception e) {
                    // Ignore if something goes wrong while adding a datapoint
                    logger.log(Level.WARNING, "Unable to plot datapoint in matrix plot", e);
                }
            } else if (filter instanceof XYZSeriesArrayDataFilter xyzfilter) {
                try {
                    double[] data = (double[]) message.getData(xyzfilter.getIdZ());
                    double y = (Double) message.getData(xyzfilter.getIdY());
//						int offset = xyzfilter.getOffset();
//						int size = xyzfilter.getSize();
//						for(int i=offset;i<offset+size; i++){
                    //xyzfilter.getPlot().setUpdatesEnabled(updateAtStreamElement);
                    for (int i = 0; i < data.length; i++) {
                        ((MatrixPlot) xyzfilter.getPlot()).getSeries("").appendData(i, y, data[i]);
                    }
                    //xyzfilter.getPlot().setUpdatesEnabled(true);                                                
                } catch (Exception e) {
                    // Ignore if something goes wrong while adding a datapoint
                    logger.log(Level.WARNING, "Unable to plot datapoint in matrix plot", e);
                }
            }
        }
        if (updateAtStreamElement) {
            for (Plot plot : plots) {
                plot.update(true);
            }
        }
    }

    Double getDoubleValue(Object value) {
        if (value instanceof Double val) {
            return val;
        }
        if (value instanceof Integer val) {
            return (double) val;
        } else if (value instanceof Short val) {
            return (double) val;
        } else if (value instanceof Long val) {
            return (double) val;
        } else if (value instanceof Float val) {
            return (double) val;
        } else if (value instanceof Boolean val) {
            return val ? 1.0 : 0.0;
        }
        return Double.NaN;
    }

    //@Subscribe
    public void onStreamDelimiterMessage(StreamDelimiterMessage message) {
        for (SeriesDataFilter filter : filters) {
            if (filter instanceof XYSeriesDataFilter xyfilter) {
                if (message.getNumber() == 0) { // TODO need to check this - here we assume that always at the lowest level we create a new series 
                    // Indicate to create new series at the next message
                    xyfilter.setCount(xyfilter.getCount() + 1); // Increment count of the filter
                    xyfilter.setNewseries(true);
                }
            }
        }

        // Update matrix plot at the end of each line
        if (updateAtStreamDelimiter) {
            for (Plot plot : getPlots()) {
                plot.update(true);
            }
        }

        // Clear matrix plot if iflag is encountered
        // TODO: One need to check whether the iflag belongs to a delimiter
        // of a higher dimension than the highest dimension (axis) that
        // is involved in the plot
        if (message.isIflag()) {
            clearPlot = true;
        }
    }

    //@Subscribe
    public void onEndOfStreamMessage(EndOfStreamMessage message) {
        ecount++;

        // Update plots if updateAtEndOfStream flag is set
        if (updateAtEndOfStream) {
            // Update matrix plots
            for (Plot plot : getPlots()) {
                plot.update(true);
            }
        }
    }

    public void configure() {
        ecount = 0;
        clearPlot = false;
    }

    /**
     * Get the plot panels for the visualizations
     *
     * @return	List of configured JPanels
     */
    public List<JPanel> getPlotPanels() {
        List<JPanel> panels = new ArrayList<JPanel>();
        for (SeriesDataFilter f : filters) {
            JPanel panel = (JPanel) f.getPlot();
            if ((panel != null) && (!panels.contains(panel))) {
                panels.add(panel);
            }
        }

        return panels;
    }

    public List<Plot> getPlots() {
        List<Plot> plots = new ArrayList<Plot>();
        for (SeriesDataFilter f : filters) {
            Plot plot = f.getPlot();
            if ((plot != null) && (!plots.contains(plot))) {
                plots.add(plot);
            }
        }
        return plots;
    }

    public boolean isUpdateAtStreamElement() {
        return updateAtStreamElement;
    }

    public void setUpdateAtStreamElement(boolean updateAtStreamElement) {
        this.updateAtStreamElement = updateAtStreamElement;
    }

    public boolean isUpdateAtStreamDelimiter() {
        return updateAtStreamDelimiter;
    }

    public void setUpdateAtStreamDelimiter(boolean updateAtStreamDelimiter) {
        this.updateAtStreamDelimiter = updateAtStreamDelimiter;
    }

    public boolean isUpdateAtEndOfStream() {
        return updateAtEndOfStream;
    }

    public void setUpdateAtEndOfStream(boolean updateAtEndOfStream) {
        this.updateAtEndOfStream = updateAtEndOfStream;
    }

    public int getSubsampling() {
        return subsampling;
    }

    public void setSubsampling(int factor) {
        this.subsampling = factor;
        subsamplingCounter = subsampling;//To plot first point
    }
}
