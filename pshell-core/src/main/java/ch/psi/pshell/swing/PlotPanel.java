package ch.psi.pshell.swing;

import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.plot.Axis;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotErrorSeries;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.scan.RegionScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.Sequencer;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Range;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 */
public class PlotPanel extends ch.psi.pshell.plot.PlotPanel {
    final AtomicBoolean updating = new AtomicBoolean(false);;
    final ArrayList<ScanRecord> scanRecordBuffer = new ArrayList();
    final ArrayList<Integer> readableIndexes = new ArrayList();
    final ArrayList<Integer> writableIndexes = new ArrayList();        
    
    
    public void setActive(boolean value) {
        if (value) {
            Sequencer.getInstance().addScanListener(scanListener);
        } else {
            Sequencer.getInstance().removeScanListener(scanListener);
        }
    }

    public boolean isActive() {
        
        return Sequencer.getInstance().getScanListeners().contains(scanListener);
    }

    
    public void triggerScanStarted(final Scan scan, final String plotTitle) {
        scanListener.onScanStarted(scan, plotTitle);
    }

    public void triggerOnNewRecord(final Scan scan, final ScanRecord record) {
        scanListener.onNewRecord(scan, record);
    }

    public void triggerScanEnded(Scan scan, Exception ex) {
        scanListener.onScanEnded(scan, ex);
    }

    int domainAxisReadableIndex = -1;
    
    ScanListener scanListener = new ScanListener() {
        boolean isPlotting(String plot) {
            return ((getPlotTitle() == null) && (plot == null))
                    || ((getPlotTitle()!= null) && (getPlotTitle().equals(plot)));
        }
        volatile Scan currentScan;
        volatile int currentPass;
        final List<Boolean> unsigned = new ArrayList<>();
                 
        void startPlot(final Scan scan, Map pars) {
            clear();
            readableIndexes.clear();
            writableIndexes.clear();
            currentPass = 1;
            changedScaleX = false;
            boolean accessDevice = Sequencer.getInstance().getInterpreter().isThreaded();

            try {

                String labelX = null;
                domainAxisReadableIndex = -1;
                if (prefs.domainAxis != null) {
                    if (prefs.domainAxis.equals(ViewPreference.DOMAIN_AXIS_TIME)) {
                        labelX = ViewPreference.DOMAIN_AXIS_TIME;
                    } else if (prefs.domainAxis.equals(ViewPreference.DOMAIN_AXIS_INDEX)) {
                        labelX = ViewPreference.DOMAIN_AXIS_INDEX;
                        changedScaleX = true;
                        if (prefs.range == null) {
                            if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                                prefs.range = new Range(0, scan.getNumberOfRecords() - 1);
                            }
                        }
                    } else {
                        for (int j = 0; j < scan.getReadableNames().length; j++) {
                            if (scan.getReadableNames()[j].equals(prefs.domainAxis)) {
                                labelX = scan.getReadableNames()[j];
                                domainAxisReadableIndex = j;
                                changedScaleX = true;
                                break;
                            }
                        }
                    }
                }
                if (labelX == null) {
                    if (scan.getWritables().length >= 1){
                        labelX = accessDevice ? (scan.getWritables()[0]).getAlias() : ((String[])pars.get("writables"))[0];                        
                    }
                } else {
                    prefs.autoRange = (prefs.range == null) ? true : false;
                }

                //Positioner plot, if configured
                if (prefs.enabledPlots != null) {
                    Writable[] writables = scan.getWritables();
                    for (int i = 0; i < writables.length; i++) {
                        Writable w = writables[i];
                        String name = accessDevice ? w.getAlias() : ((String[])pars.get("writables"))[i];
                        if (prefs.enabledPlots.contains(name)) {
                            addPlot(name, true, "Time", 1, null, null, null, null,false);
                            writableIndexes.add(i);
                        }
                    }
                }
                
                //Single plot                          
                Readable[] readables = scan.getReadables();
                for (int i = 0; i < readables.length; i++) {
                    Readable r = readables[i];
                    String name = accessDevice ? r.getAlias() : ((String[])pars.get("readables"))[i];
                    if (((prefs.enabledPlots == null) || (prefs.enabledPlots.contains(name))) && (domainAxisReadableIndex != i)) {
                        double[] start = new double[scan.getStart().length];
                        System.arraycopy(scan.getStart(), 0, start, 0, start.length);
                        double[] end = new double[scan.getEnd().length];
                        System.arraycopy(scan.getEnd(), 0, end, 0, end.length);

                        if (scan instanceof RegionScan regionScan) {
                            double[] range = regionScan.getRange();
                            start = new double[]{range[0]};
                            end = new double[]{range[1]};
                        }
                        if ((start.length > 1) && (scan.getDimensions() == 1)) { //Line scan with multiple positioners: plot against first positioner
                            start = new double[]{start[0]};
                            end = new double[]{end[0]};
                        }
                        int[] recordSize = null;
                        if (r instanceof Readable.ReadableMatrix readableMatrix) {
                            int width = accessDevice ? readableMatrix.getWidth() : (int)((Map<String,Map>)pars.get("attrs")).get(name).get("width");
                            int height = accessDevice ? readableMatrix.getHeight() : (int)((Map<String,Map>)pars.get("attrs")).get(name).get("height");
                            recordSize = new int[]{width, height};
                            start = null;
                            end = null;
                            if (r instanceof ReadableCalibratedMatrix readableCalibratedMatrix) {
                                MatrixCalibration cal =accessDevice ? readableCalibratedMatrix.getCalibration() : (MatrixCalibration)((Map<String,Map>)pars.get("attrs")).get(name).get("calibration");
                                if (cal != null) {
                                    double startX = cal.getValueX(0);
                                    double endX = cal.getValueX(width - 1);
                                    double startY = cal.getValueY(0);
                                    double endY = cal.getValueY(height - 1);
                                    start = new double[]{startX, startY};
                                    end = new double[]{endX, endY};
                                }
                            }
                        } else if (r instanceof Readable.ReadableArray readableArray) {
                            int size = accessDevice ?readableArray.getSize() : (int)((Map<String,Map>)pars.get("attrs")).get(name).get("size");
                            recordSize = new int[]{size};
                            if (r instanceof Readable.ReadableCalibratedArray readableCalibratedArray) {
                                ArrayCalibration cal = accessDevice ? readableCalibratedArray.getCalibration() :  (ArrayCalibration)((Map<String,Map>)pars.get("attrs")).get(name).get("calibration");
                                if (cal != null) {
                                    double startVal = cal.getValue(0);
                                    double endVal = cal.getValue(size - 1);
                                    if (start.length == 1) {
                                        start = new double[]{start[0], startVal};
                                        end = new double[]{end[0], endVal};
                                    } else if (start.length == 2) {
                                        start = new double[]{start[0], startVal, start[1]};
                                        end = new double[]{end[0], endVal, end[1]};
                                    }
                                }
                            }
                        }
                        addPlot(name, true, labelX, scan.getDimensions(), recordSize, start, end, scan.getNumberOfSteps(), Averager.isAverager(r));
                        readableIndexes.add(i);
                    }
                }

            } catch (Exception ex) {
                Logger.getLogger(PlotPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!Plot.isOffscreen()) {
                validate();
                repaint();
            }
        }

        @Override
        public void onScanStarted(final Scan scan, final String plotTitle) {
            if (isPlotting(plotTitle)) {
                currentScan = scan;
                Map pars = new HashMap<>();
                synchronized (scanRecordBuffer) {
                    scanRecordBuffer.clear();
                    updating.set(false);
                }
                if (!Sequencer.getInstance().getInterpreter().isThreaded()){
                    pars.put("writables", scan.getWritableNames());
                    pars.put("readables", scan.getReadableNames());
                    Map<String,Map> attrs = new HashMap<>();
                    for (Readable r : scan.getReadables()){
                         Map<String,Object> attr = new HashMap<>();
                         if (r instanceof Readable.ReadableArray readableArray){
                            attr.put("size", readableArray.getSize());
                            if (r instanceof Readable.ReadableCalibratedArray readableCalibratedArray) {
                                attr.put("calibration", readableCalibratedArray.getCalibration());
                            }
                         } else if (r instanceof Readable.ReadableMatrix readableMatrix){
                             attr.put("width", readableMatrix.getWidth());
                             attr.put("height", readableMatrix.getHeight());
                             if (r instanceof Readable.ReadableCalibratedMatrix readableCalibratedMatrix) {
                                 attr.put("calibration", readableCalibratedMatrix.getCalibration());
                             } 
                         }
                         attrs.put(r.getAlias(), attr);                         
                    }
                    pars.put("attrs", attrs);                                        
                }
                unsigned.clear();
                for (Readable r : scan.getReadables()){
                    unsigned.add(Boolean.TRUE.equals(r.isElementUnsigned()));
                }
                if (!Plot.isOffscreen() && !SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeLater(() -> {
                        startPlot(scan, pars);
                    });
                } else {
                    startPlot(scan, pars);
                }
            }
        }

        @Override
        public void onNewRecord(final Scan scan, final ScanRecord record) {
            if (scan == currentScan) {
                synchronized (scanRecordBuffer) {
                    scanRecordBuffer.add(record);
                    if (updating.compareAndSet(false, true)) {
                        if (Plot.isOffscreen()) {
                            addRecords(scan);
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                addRecords(scan);
                            });
                        }
                    }
                }
            }
        }

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
        }

        void addRecords(final Scan scan) {
            if (scan != currentScan) {
                updating.set(false);
                return;
            }
            try {
                ArrayList<ScanRecord> records;
                synchronized (scanRecordBuffer) {
                    records = (ArrayList<ScanRecord>) scanRecordBuffer.clone();
                    scanRecordBuffer.clear();
                    updating.set(false);
                }
                boolean lastRecord = false;
                for (ScanRecord record : records) {
                    addRecord(scan, record);
                    lastRecord = (record.getIndex() == (scan.getNumberOfSteps()[0] - 1));
                }

                if (PlotPanel.this.isShowing() || lastRecord) {
                    for (Plot plot : plots) {
                        plot.update(true);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(PlotPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        void addRecord(final Scan scan, final ScanRecord record) {
            int writablesPlots = writableIndexes.size();
            Number[] positions = record.getPositions();
            int index = record.getIndexInPass();
            Object[] values = record.getReadables();
            boolean newPass = (currentPass != record.getPass());

            for (int i = 0; i < writablesPlots; i++) {
                int writableIndex = writableIndexes.get(i);
                Plot plot = plots.get(i);
                Object val = positions[writableIndex];
                long time_ms = ((record.getTimestamp() > 0) ? record.getTimestamp() : System.currentTimeMillis()) - scan.getStartTimestamp();
                double xdata = ((double) time_ms) / 1000;
                if (newPass) {
                    ((LinePlotBase) plot).getSeries(0).appendData(xdata, Double.NaN);
                }
                ((LinePlotBase) plot).getSeries(0).appendData(xdata, ((Number) val).doubleValue());
            }

            double xValue = index;
            if ((positions != null) && (positions.length > 0)) {
                xValue = positions[0].doubleValue();
            }
            if (domainAxisReadableIndex >= 0) {
                xValue = ((Number) values[domainAxisReadableIndex]).doubleValue();
            } else if (prefs.domainAxis != null) {
                if (prefs.domainAxis.equals(ViewPreference.DOMAIN_AXIS_TIME)) {
                    long time_ms = ((record.getTimestamp() > 0) ? record.getTimestamp() : System.currentTimeMillis()) - scan.getStartTimestamp();
                    xValue = ((double) time_ms) / 1000;
                } else if (prefs.domainAxis.equals(ViewPreference.DOMAIN_AXIS_INDEX)) {
                    xValue = record.getIndex();
                }
            }

            for (int i = writablesPlots; i < plots.size(); i++) {
                int readableIndex = readableIndexes.get(i - writablesPlots);
                Plot plot = plots.get(i);

                Object val = values[readableIndex];
                if (val != null) {
                    if (val instanceof List list) {  //Jython lists
                        val = list.toArray();
                    } else if (val instanceof Map map) { //JS lists
                        val = map.values().toArray();
                    } else if (val instanceof Boolean bool) {
                        val = bool ? 1.0 : 0.0;
                    }
                     
                    if ((readableIndex<unsigned.size())  && (unsigned.get(readableIndex))){
                        val = Convert.toUnsigned(val);
                    }                    
                        
                    if (scan.getDimensions() == 3) {
                        if (val instanceof Number number) {
                            if (plot instanceof MatrixPlotBase matrixPlotBase) {
                                xValue = positions[2].doubleValue();
                                double yValue = positions[1].doubleValue();
                                matrixPlotBase.getSeries(0).appendData(xValue, yValue, number.doubleValue());
                            } else if (plot instanceof LinePlotBase linePlotBase) {
                                linePlotBase.getSeries(0).appendData(xValue, number.doubleValue());
                            }
                        } else if (val.getClass().isArray()) {
                            val = Convert.toDouble(val);
                            if (val instanceof double[]) {
                                for (int yValue = 0; yValue < Array.getLength(val); yValue++) {
                                    Object o = Array.get(val, yValue);
                                    double zValue = (Double) o;
                                    ((MatrixPlotBase) plot).getSeries(0).appendData(xValue, yValue, zValue);
                                }
                            } else if (val instanceof double[][] dses) {
                                ((MatrixPlotBase) plot).getSeries(0).setData(dses);
                            }
                        }
                    } else if (scan.getDimensions() == 2) {

                        if (val instanceof Number number) {
                            double yValue = positions[1].doubleValue();
                            if (plot instanceof MatrixPlotBase matrixPlotBase) {
                                matrixPlotBase.getSeries(0).appendData(xValue, yValue, number.doubleValue());
                            } else if (plot instanceof LinePlotBase linePlotBase) {
                                int series = index % (scan.getNumberOfSteps()[1] + 1);
                                if (newPass) {
                                    linePlotBase.getSeries(0).appendData(xValue, Double.NaN);
                                }
                                linePlotBase.getSeries(series).appendData(xValue, number.doubleValue());

                            }
                        } else if (val.getClass().isArray()) {
                            val = Convert.toDouble(val);
                            if (val instanceof double[] ds) {
                                if (plot instanceof MatrixPlotBase matrixPlotBase) {
                                    MatrixPlotSeries series = matrixPlotBase.getSeries(0);
                                    double minY = series.getMinY();
                                    double maxY = series.getMaxY();
                                    double scale = ((maxY > minY) && (Array.getLength(val) > 1)) ? (maxY - minY) / (Array.getLength(val) - 1) : Double.NaN;

                                    for (int y = 0; y < Array.getLength(val); y++) {
                                        Object o = Array.get(val, y);
                                        double zValue = (Double) o;
                                        double yValue = Double.isNaN(scale) ? y : scale * y + minY;
                                        series.appendData(xValue, yValue, zValue);
                                    }
                                } else if (plot instanceof LinePlotBase linePlotBase) {
                                    linePlotBase.getSeries(0).setData(ds);

                                }
                            } else if (val instanceof double[][] dses) {
                                ((MatrixPlotBase) plot).getSeries(0).setData(dses);
                            }
                        }

                    } else {
                        if (val.getClass().isArray()) {
                            //Scanning 2D data
                            int rank = Arr.getRank(val);
                            switch (rank) {
                                case 2:
                                    val = Convert.toDouble(val);
                                    ((MatrixPlotBase) plot).getSeries(0).setData((double[][]) val);
                                    break;
                                case 1:
                                    if (plot instanceof MatrixPlotBase) {
                                        val = Convert.toDouble(val);
                                        MatrixPlotSeries series = ((MatrixPlotBase) plot).getSeries(0);
                                        double minY = series.getMinY();
                                        double maxY = series.getMaxY();
                                        double scale = ((maxY > minY) && (Array.getLength(val) > 1)) ? (maxY - minY) / (Array.getLength(val) - 1) : Double.NaN;

                                        if ((prefs.autoRange != null) && (prefs.autoRange)) {
                                            //TODO: improve perfoemance, avoiding copying
                                            Axis x = ((MatrixPlotBase) plot).getAxis(Plot.AxisId.X);
                                            if (record.getIndex() == 0) {
                                                series.setRangeX(xValue, xValue);
                                                x.setRange(xValue, xValue);
                                            } else {
                                                double min = Math.min(xValue, x.getMin());
                                                double max = Math.max(xValue, x.getMax());
                                                series.setRangeX(min, max);
                                                x.setRange(min, max);
                                            }
                                            double[][] data = series.getData();

                                            for (int y = 0; y < Array.getLength(val); y++) {
                                                Object o = Array.get(val, y);
                                                if (data[y].length <= record.getIndex()) {
                                                    double[] aux = new double[record.getIndex() + 1];
                                                    System.arraycopy(data[y], 0, aux, 0, data[y].length);
                                                    data[y] = aux;
                                                }
                                                data[y][record.getIndex()] = (Double) o;
                                            }
                                            series.setNumberOfBinsX(data[0].length);
                                            series.setNumberOfBinsY(data.length);
                                            series.setData(data);
                                        } else {
                                            for (int y = 0; y < Array.getLength(val); y++) {
                                                Object o = Array.get(val, y);
                                                double zValue = (Double) o;
                                                double yValue = Double.isNaN(scale) ? y : scale * y + minY;
                                                series.appendData(xValue, yValue, zValue);
                                            }
                                        }
                                    } else if (plot instanceof LinePlotBase linePlotBase) {                                        
                                        Readable r = scan.getReadables()[readableIndex];
                                        val = Convert.toDouble(val);
                                        double[] xdata = null;
                                        if (r instanceof ReadableCalibratedArray readableCalibratedArray) {
                                            ArrayCalibration cal = readableCalibratedArray.getCalibration();
                                            if (cal != null) {
                                                xdata = cal.getAxisX(((double[]) val).length);
                                            }
                                        }
                                        linePlotBase.getSeries(0).setData(xdata, (double[]) val);
                                    }
                                    break;
                            }

                        } else if (val instanceof Number number) {
                            //Single plot 
                            LinePlotSeries series = ((LinePlotBase) plot).getSeries(0);
                            if (newPass) {
                                series.appendData(xValue, Double.NaN);
                            }
                            if (series instanceof LinePlotErrorSeries) {
                                DescStatsDouble d = (val instanceof DescStatsDouble dsdv) ? dsdv : new DescStatsDouble(new Number[]{number}, -1);
                                if ((prefs.plotTypes != null) && (prefs.plotTypes.containsKey(series.getName()) && (prefs.plotTypes.get(series.getName()).equals("minmax")))) {
                                    ((LinePlotErrorSeries) ((LinePlotJFree) plot).getSeries(0)).appendData(xValue, d.doubleValue(), d.getMin(), d.getMax());
                                } else {
                                    ((LinePlotErrorSeries) ((LinePlotJFree) plot).getSeries(0)).appendData(xValue, d.doubleValue(), d.getStdev());
                                }
                            } else {
                                ((LinePlotBase) plot).getSeries(0).appendData(xValue, number.doubleValue());
                            }
                        }
                    }
                }
            }
            currentPass = record.getPass();
        }
    };
}