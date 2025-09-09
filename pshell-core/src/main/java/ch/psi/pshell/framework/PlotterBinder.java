package ch.psi.pshell.framework;

import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlot.Style;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plotter.Plotter;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.PlotListener;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.State;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.pshell.sequencer.SequencerListener;

/**
 *
 */
public class PlotterBinder implements AutoCloseable {

    final ScanListener scanListener;
    final PlotListener plotListener;
    final SequencerListener sequencerListener;
    final Plotter pm;

    public PlotterBinder(Plotter pm) {
        this.pm = pm;
        this.scanListener = new BinderScanListener();
        this.sequencerListener = new BinderSequencerListener();
        this.plotListener = new BinderPlotListener();
        Context.getSequencer().addScanListener(scanListener);
        Context.getSequencer().addListener(sequencerListener);
        Context.getSequencer().setPlotListener(plotListener);
    }

    class BinderScanListener implements ScanListener {

        HashMap<Scan, String> scans = new HashMap<>();

        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            plotTitle = (plotTitle == null) ? "0" : plotTitle;
            pm.clearPlots(plotTitle);
            pm.setProgress(plotTitle, 0.0);
            scans.put(scan, plotTitle);
            for (ch.psi.pshell.device.Readable r : scan.getReadables()) {
                try{
                    String name = r.getAlias();
                    if (r instanceof ch.psi.pshell.device.Readable.ReadableMatrix) {
                        pm.addMatrixPlot(plotTitle, name,  MatrixPlot.Style.Normal, Colormap.Temperature);
                        pm.addMatrixSeries(plotTitle + "/" + name, name, null, null, null, null, null, null);
                    } else if (r instanceof ch.psi.pshell.device.Readable.ReadableArray readableArray) {
                        pm.addMatrixPlot(plotTitle, name,  MatrixPlot.Style.Normal, Colormap.Temperature);
                        pm.addMatrixSeries(plotTitle + "/" + name, name, scan.getStart()[0], scan.getEnd()[0], scan.getNumberOfSteps()[0] + 1, 0.0, (double) readableArray.getSize() - 1, readableArray.getSize());
                    } else {
                        pm.addLinePlot(plotTitle, name, Averager.isAverager(r) ? Style.ErrorY : Style.Normal);
                        pm.addLineSeries(plotTitle + "/" + name, name, null, null, null, null, null);
                    }
                } catch (Exception ex){
                    Logger.getLogger(PlotterBinder.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (scans.containsKey(scan)) {
                String title = scans.get(scan);
                pm.setProgress(title, record.getIndex() * (1.0 / scan.getNumberOfRecords()));
                double x = (record.getPositions().length > 0) ? record.getPositions()[0].doubleValue() : record.getIndex();
                for (int i = 0; i < scan.getReadables().length; i++) {
                    try{
                        ch.psi.pshell.device.Readable r = scan.getReadables()[i];
                        String name = r.getAlias();
                        String series = title + "/" + name + "/0";
                        if (r instanceof ch.psi.pshell.device.Readable.ReadableMatrix) {
                            pm.setMatrixSeriesData(series, (double[][]) Convert.toDouble(record.getReadables()[i]), null, null);
                        } else if (r instanceof ch.psi.pshell.device.Readable.ReadableArray) {
                            double[] z = (double[]) record.getReadables()[i];
                            double[] ax = new double[z.length];
                            Arrays.fill(ax, x);
                            double[] ay = Arr.indexesDouble(z.length);
                            pm.appendMatrixSeriesDataArray(series, ax, ay, z);
                        } else {
                            Number y = ((Number) record.getReadables()[i]);
                            pm.appendLineSeriesData(series, x, y.doubleValue(), (y instanceof DescStatsDouble dsd) ? dsd.getStdev() : 0);
                        }
                    } catch (Exception ex){
                        Logger.getLogger(PlotterBinder.class.getName()).log(Level.FINER, null, ex);
                    }
                }                
            }
        }

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
            if (scans.containsKey(scan)) {
                if (Context.getApp().getRunningTask() == null) {
                    pm.setProgress(scans.get(scan), null);
                }
            }
            scans.remove(scan);
        }

    }

    class BinderSequencerListener implements SequencerListener {

        @Override
        public void onStateChanged(State state, State former) {
            if (!state.isProcessing()) {
                pm.setProgress(null, null);
            }
            pm.setStatus(null, String.valueOf(state));
        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {
            switch (preference) {
                case STATUS -> pm.setStatus(null, String.valueOf(value));
            }
        }

    };
    
    class BinderPlotListener implements PlotListener {
        @Override
        public List<Plot> plot(String title, PlotDescriptor[] plots) throws Exception {
            title = (title == null) ? "0" : title;
            pm.clearPlots(title);            
            if ((plots != null) && (plots.length > 0)) {
                for (PlotDescriptor plot : plots) {
                    try {
                        if (plot != null) {
                           switch(plot.rank){
                               case 1: {
                                   String plotName = pm.addLinePlot(title, plot.name, (plot.error==null) ? LinePlot.Style.Normal : LinePlot.Style.ErrorY);
                                   String seriesName = pm.addLineSeries(plotName, plot.name, null, null, null, null, null);
                                   pm.setLineSeriesData(seriesName, plot.x, (double[])Convert.toDouble(plot.data), null, plot.error);
                                   break;
                                }
                               case 2: {
                                   String plotName = pm.addMatrixPlot(title, plot.name, MatrixPlot.Style.Normal, null);                                   
                                   double[][] data = (double[][]) Convert.toDouble(plot.data);
                                   double minX = (plot.x==null) ? 0 : plot.x[0];
                                   double maxX = (plot.x==null) ? data[0].length-1 : plot.x[plot.x.length-1];
                                   int nX = (plot.x==null) ? data[0].length : plot.x.length;
                                   double minY = (plot.y==null) ? 0 : plot.y[0];
                                   double maxY = (plot.y==null) ? data.length-1 : plot.y[plot.y.length-1];
                                   int nY =  (plot.y==null) ? data.length : plot.y.length;
                                   double stepX = (nX > 1) ? (maxX-minX) / (nX - 1) : 0;
                                   double stepY = (nY > 1) ? (maxY-minY) / (nY - 1) : 0;
                                   String seriesName = pm.addMatrixSeries(plotName, plot.name, minX, maxX, nX, minY, maxY, nY);
                                   for (int i = 0; i < data.length; i++) {
                                        for (int j = 0; j < data[0].length; j++) {                                            
                                            pm.appendMatrixSeriesData(seriesName, minX + j*stepX, minY + i*stepY , data[i][j]);
                                        }
                                    }  
                                   break;
                               }
                               default:
                                   throw new Exception ("Unsupported rank: " + plot.rank);
                           }
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(PlotterBinder.class.getName()).warning("Error creating plot: " + String.valueOf((plot != null) ? plot.name : null));
                    }
                }
            }
            return null;
        }

        @Override
        public List<Plot> getPlots(String title) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onTitleClosed(String title) {
            title = (title == null) ? "0" : title;
            pm.clearPlots(title);
        }

        @Override
        public List<String> getTitles() {
            return Arrays.asList(pm.getContexts());
        }
    };
    
    

    @Override
    public void close() throws Exception {
        Context.getSequencer().removeScanListener(scanListener);
        Context.getSequencer().removeListener(sequencerListener);
    }

}