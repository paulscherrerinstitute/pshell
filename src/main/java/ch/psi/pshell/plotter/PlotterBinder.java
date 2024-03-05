package ch.psi.pshell.plotter;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.ContextListener;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot.Style;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Arr;
import ch.psi.utils.State;
import java.util.Arrays;
import java.util.HashMap;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Convert;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PlotterBinder implements AutoCloseable {

    final ScanListener scanListener;
    final ContextListener contextListener;
    final Context context;
    final Plotter pm;

    public PlotterBinder(Context context, Plotter pm) {
        this.context = context;
        this.pm = pm;
        this.scanListener = new BinderScanListener();
        this.contextListener = new BinderContextListener();
        context.addScanListener(scanListener);
        context.addListener(contextListener);
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
                    } else if (r instanceof ch.psi.pshell.device.Readable.ReadableArray) {
                        pm.addMatrixPlot(plotTitle, name,  MatrixPlot.Style.Normal, Colormap.Temperature);
                        pm.addMatrixSeries(plotTitle + "/" + name, name, scan.getStart()[0], scan.getEnd()[0], scan.getNumberOfSteps()[0] + 1, 0.0, (double) ((ch.psi.pshell.device.Readable.ReadableArray) r).getSize() - 1, ((ch.psi.pshell.device.Readable.ReadableArray) r).getSize());
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
                            pm.appendLineSeriesData(series, x, y.doubleValue(), (y instanceof DescStatsDouble) ? ((DescStatsDouble) y).getStdev() : 0);
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
                if (App.getInstance().getRunningTask() == null) {
                    pm.setProgress(scans.get(scan), null);
                }
            }
            scans.remove(scan);
        }

    }

    class BinderContextListener extends ContextAdapter {

        @Override
        public void onContextStateChanged(State state, State former) {
            if (!state.isProcessing()) {
                pm.setProgress(null, null);
            }
            pm.setStatus(null, String.valueOf(state));
        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {
            switch (preference) {
                case STATUS:
                    pm.setStatus(null, String.valueOf(value));
                    break;
            }
        }

    };

    @Override
    public void close() throws Exception {
        context.removeScanListener(scanListener);
        context.removeListener(contextListener);
    }

}
