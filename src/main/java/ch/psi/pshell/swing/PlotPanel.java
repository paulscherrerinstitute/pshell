package ch.psi.pshell.swing;

import ch.psi.pshell.plotter.PlotLayout;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.data.DataSlice;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.plot.Axis;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlot.Style;
import ch.psi.pshell.plot.LinePlotErrorSeries;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.SlicePlot;
import ch.psi.pshell.plot.SlicePlotDefault;
import ch.psi.pshell.plot.SlicePlotSeries;
import ch.psi.pshell.scan.PlotScan;
import ch.psi.pshell.scan.RegionScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Range;
import ch.psi.utils.swing.MonitoredPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/**
 *
 */
public class PlotPanel extends MonitoredPanel {

    public static final String PROPERTY_PLOT_IMPL_LINE = "ch.psi.pshell.plot.impl.line";
    public static final String PROPERTY_PLOT_IMPL_MATRIX = "ch.psi.pshell.plot.impl.matrix";
    public static final String PROPERTY_PLOT_IMPL_SURFACE = "ch.psi.pshell.plot.impl.surface";
    public static final String PROPERTY_PLOT_IMPL_TIME = "ch.psi.pshell.plot.impl.time";
    public static final String PROPERTY_PLOT_QUALITY = "ch.psi.pshell.plot.quality";
    public static final String PROPERTY_PLOT_LAYOUT = "ch.psi.pshell.plot.layout";

    static public Class DEFAULT_PLOT_IMPL_LINE = ch.psi.pshell.plot.LinePlotJFree.class;
    static public Class DEFAULT_PLOT_IMPL_MATRIX = ch.psi.pshell.plot.MatrixPlotJFree.class;
    static public Class DEFAULT_PLOT_IMPL_SLICE = ch.psi.pshell.plot.SlicePlotDefault.class;
    static public Quality DEFAULT_PLOT_QUALITY = Quality.High;
    static public PlotLayout DEFAULT_PLOT_LAYOUT = PlotLayout.Vertical;

    static Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final int DEFAULT_RANGE_STEPS = 199;

    public static final boolean offscreen = App.isOffscreenPlotting();
    String plotTitle;

    PlotPreferences prefs;

    public PlotPanel() {
        initComponents();
        LayoutManager layout = new GridBagLayout();
        pnGraphs.setLayout(layout);
        plots = new ArrayList<>();
        scanRecordBuffer = new ArrayList<>();
        readableIndexes = new ArrayList<>();
        writableIndexes = new ArrayList<>();
        updating = new AtomicBoolean(false);
        prefs = new PlotPreferences();
    }
    
    public static void setTitleFont(Font font) {
        TITLE_FONT = font;
    }

    public static  Font getTitleFont() {
        return TITLE_FONT;
    }    

    static public String getLinePlotImpl() {
        String impl = System.getProperty(PROPERTY_PLOT_IMPL_LINE);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_LINE.getName() : impl;
    }

    static public String getMatrixPlotImpl() {
        String impl = System.getProperty(PROPERTY_PLOT_IMPL_MATRIX);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_MATRIX.getName() : impl;
    }

    static public String getSurfacePlotImpl() {
        String impl = System.getProperty(PROPERTY_PLOT_IMPL_SURFACE);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? null : impl;
    }

    static public String getSlicePlotImpl() {
        String impl = System.getProperty("ch.psi.pshell.plot.impl.slice");
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_SLICE.getName() : impl;
    }

    static public Quality getQuality() {
        try {
            return Quality.valueOf(System.getProperty(PROPERTY_PLOT_QUALITY));
        } catch (Exception ex) {
            return DEFAULT_PLOT_QUALITY;
        }
    }

    static public PlotLayout getDefaultLayout() {
        try{
            return PlotLayout.valueOf(System.getProperty(PROPERTY_PLOT_LAYOUT));
        } catch (Exception ex) {
            return DEFAULT_PLOT_LAYOUT;
        }
    }    
    
    public PlotLayout getPlotLayout() {
        if (prefs.plotLayout != null) {
            return prefs.plotLayout;
        }
        return getDefaultLayout();
    }

    final ArrayList<Plot> plots;

    public void setPlotTitle(String plotTitle) {
        this.plotTitle = plotTitle;
    }

    public List<Plot> getPlots() {
        return (List<Plot>) plots.clone();
    }

    public void initialize() {
        //Redoing layout because it may have been changed by View's initComponents()
        removeAll();
        setLayout(new BorderLayout());
        add(scrollPane);

        setActive(false);
    }

    public void setActive(boolean value) {
        if (Context.getInstance()!=null){
            if (value) {
                Context.getInstance().addScanListener(scanListener);
            } else {
                Context.getInstance().removeScanListener(scanListener);
            }
        }
    }

    public boolean isActive() {
        
        return (Context.getInstance()!=null) && Context.getInstance().getScanListeners().contains(scanListener);
    }

    public PlotPreferences getPreferences() {
        return prefs;
    }

    public void setPreferences(PlotPreferences preferences) {
        prefs = preferences.clone();
    }
     
    public void clear() {
        plots.clear();
        if (!offscreen) {
            pnGraphs.removeAll();
            panelIndexY = 0;
            panelIndexX = 0;
            validate();
            repaint();
        }
    }

    int panelIndexX = 0;
    int panelIndexY = 0;

    public void addPlot(PlotBase plot) {
        plot.setBackground(getBackground());
        plot.setTitleFont(TITLE_FONT);
        plot.setQuality(getQuality());
        plots.add(plot);

        if (!offscreen) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridx = panelIndexX;
            c.gridy = panelIndexY;

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

    final AtomicBoolean updating;
    final ArrayList<ScanRecord> scanRecordBuffer;
    final ArrayList<Integer> readableIndexes;
    final ArrayList<Integer> writableIndexes;

    int domainAxisReadableIndex = -1;
    boolean changedScaleX;

    ScanListener scanListener = new ScanListener() {
        boolean isPlotting(String plot) {
            return ((PlotPanel.this.plotTitle == null) && (plot == null))
                    || ((PlotPanel.this.plotTitle != null) && (PlotPanel.this.plotTitle.equals(plot)));
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
            boolean accessDevice = Context.getInstance().getScriptManager().isThreaded();

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
                            addPlot(name, true, "Time", 1, null, null, null, null, w.getClass());
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

                        if (scan instanceof RegionScan) {
                            double[] range = ((RegionScan) scan).getRange();
                            start = new double[]{range[0]};
                            end = new double[]{range[1]};
                        }
                        if ((start.length > 1) && (scan.getDimensions() == 1)) { //Line scan with multiple positioners: plot against first positioner
                            start = new double[]{start[0]};
                            end = new double[]{end[0]};
                        }
                        int[] recordSize = null;
                        if (r instanceof Readable.ReadableMatrix) {
                            int width = accessDevice ? ((Readable.ReadableMatrix) r).getWidth() : (int)((Map<String,Map>)pars.get("attrs")).get(name).get("width");
                            int height = accessDevice ? ((Readable.ReadableMatrix) r).getHeight() : (int)((Map<String,Map>)pars.get("attrs")).get(name).get("height");
                            recordSize = new int[]{width, height};
                            start = null;
                            end = null;
                            if (r instanceof ReadableCalibratedMatrix) {
                                MatrixCalibration cal =accessDevice ?((ReadableCalibratedMatrix) r).getCalibration() :  (MatrixCalibration)((Map<String,Map>)pars.get("attrs")).get(name).get("calibration");
                                if (cal != null) {
                                    double startX = cal.getValueX(0);
                                    double endX = cal.getValueX(width - 1);
                                    double startY = cal.getValueY(0);
                                    double endY = cal.getValueY(height - 1);
                                    start = new double[]{startX, startY};
                                    end = new double[]{endX, endY};
                                }
                            }
                        } else if (r instanceof Readable.ReadableArray) {
                            int size = accessDevice ?((Readable.ReadableArray) r).getSize() : (int)((Map<String,Map>)pars.get("attrs")).get(name).get("size");
                            recordSize = new int[]{size};
                            if (r instanceof Readable.ReadableCalibratedArray) {
                                ArrayCalibration cal = accessDevice ? ((Readable.ReadableCalibratedArray) r).getCalibration() :  (ArrayCalibration)((Map<String,Map>)pars.get("attrs")).get(name).get("calibration");
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
                        Class type = r.getClass();
                        if (Averager.isAverager(r)) {
                            type = Averager.class;
                        }
                        addPlot(name, true, labelX, scan.getDimensions(), recordSize, start, end, scan.getNumberOfSteps(), type);
                        readableIndexes.add(i);
                    }
                }

            } catch (Exception ex) {
                Logger.getLogger(PlotPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!offscreen) {
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
                if (!Context.getInstance().getScriptManager().isThreaded()){
                    pars.put("writables", scan.getWritableNames());
                    pars.put("readables", scan.getReadableNames());
                    Map<String,Map> attrs = new HashMap<>();
                    for (Readable r : scan.getReadables()){
                         Map<String,Object> attr = new HashMap<>();
                         if (r instanceof Readable.ReadableArray){
                            attr.put("size", ((Readable.ReadableArray)r).getSize());
                            if (r instanceof Readable.ReadableCalibratedArray) {
                                attr.put("calibration", ((Readable.ReadableCalibratedArray) r).getCalibration());
                            }
                         } else if (r instanceof Readable.ReadableMatrix){
                             attr.put("width", ((Readable.ReadableMatrix)r).getWidth());
                             attr.put("height", ((Readable.ReadableMatrix)r).getHeight());
                             if (r instanceof Readable.ReadableCalibratedMatrix) {
                                 attr.put("calibration", ((Readable.ReadableCalibratedMatrix) r).getCalibration());
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
                if (!offscreen && !SwingUtilities.isEventDispatchThread()) {
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
                        if (offscreen) {
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

                if ((!(scan instanceof PlotScan) && (PlotPanel.this.isShowing())) || lastRecord) {
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
                    if (val instanceof List) {  //Jython lists
                        val = ((List) val).toArray();
                    } else if (val instanceof Map) { //JS lists
                        val = ((Map) val).values().toArray();
                    } else if (val instanceof Boolean) {
                        val = ((Boolean) val) ? 1.0 : 0.0;
                    }
                     
                    if ((readableIndex<unsigned.size())  && (unsigned.get(readableIndex))){
                        val = Convert.toUnsigned(val);
                    }                    
                        
                    if (scan.getDimensions() == 3) {
                        if (val instanceof Number) {
                            if (plot instanceof MatrixPlotBase) {
                                xValue = positions[2].doubleValue();
                                double yValue = positions[1].doubleValue();
                                /* 
                                if ((Math.abs(yValue - scan.getStart()[1]) < (scan.getStepSize()[1] / 10))
                                        && (Math.abs(xValue - scan.getStart()[2]) < (scan.getStepSize()[2] / 10))) {
                                    ((MatrixPlotBase) plot).getSeries(0).clear();
                                }
                                 */                                
                                ((MatrixPlotBase) plot).getSeries(0).appendData(xValue, yValue, ((Number) val).doubleValue());
                            } else if (plot instanceof LinePlotBase) {//###
                                ((LinePlotBase) plot).getSeries(0).appendData(xValue, ((Number) val).doubleValue());
                            }
                        } else if (val.getClass().isArray()) {
                            val = Convert.toDouble(val);
                            if (val instanceof double[]) {
                                for (int yValue = 0; yValue < Array.getLength(val); yValue++) {
                                    Object o = Array.get(val, yValue);
                                    double zValue = (Double) o;
                                    ((MatrixPlotBase) plot).getSeries(0).appendData(xValue, yValue, zValue);
                                }
                            } else if (val instanceof double[][]) {
                                ((MatrixPlotBase) plot).getSeries(0).setData((double[][]) val);
                            }
                        }
                    } else if (scan.getDimensions() == 2) {

                        if (val instanceof Number) {
                            double yValue = positions[1].doubleValue();
                            if (plot instanceof MatrixPlotBase) {
                                ((MatrixPlotBase) plot).getSeries(0).appendData(xValue, yValue, ((Number) val).doubleValue());
                            } else if (plot instanceof LinePlotBase) {//###
                                int series = index % (scan.getNumberOfSteps()[1] + 1);
                                if (newPass) {
                                    ((LinePlotBase) plot).getSeries(0).appendData(xValue, Double.NaN);
                                }
                                ((LinePlotBase) plot).getSeries(series).appendData(xValue, ((Number) val).doubleValue());

                            }
                        } else if (val.getClass().isArray()) {
                            val = Convert.toDouble(val);
                            if (val instanceof double[]) {
                                if (plot instanceof MatrixPlotBase) {
                                    MatrixPlotSeries series = ((MatrixPlotBase) plot).getSeries(0);
                                    double minY = series.getMinY();
                                    double maxY = series.getMaxY();
                                    double scale = ((maxY > minY) && (Array.getLength(val) > 1)) ? (maxY - minY) / (Array.getLength(val) - 1) : Double.NaN;

                                    for (int y = 0; y < Array.getLength(val); y++) {
                                        Object o = Array.get(val, y);
                                        double zValue = (Double) o;
                                        double yValue = Double.isNaN(scale) ? y : scale * y + minY;
                                        series.appendData(xValue, yValue, zValue);
                                    }
                                } else if (plot instanceof LinePlotBase) {
                                    ((LinePlotBase) plot).getSeries(0).setData((double[]) val);

                                }
                            } else if (val instanceof double[][]) {
                                ((MatrixPlotBase) plot).getSeries(0).setData((double[][]) val);
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
                                    } else if (plot instanceof LinePlotBase) {                                        
                                        Readable r = scan.getReadables()[readableIndex];
                                        val = Convert.toDouble(val);
                                        double[] xdata = null;
                                        if (r instanceof ReadableCalibratedArray) {
                                            ArrayCalibration cal = ((ReadableCalibratedArray) r).getCalibration();
                                            if (cal != null) {
                                                xdata = cal.getAxisX(((double[]) val).length);
                                            }
                                        }
                                        ((LinePlotBase) plot).getSeries(0).setData(xdata, (double[]) val);
                                    }
                                    break;
                            }

                        } else if (val instanceof Number) {
                            //Single plot 
                            LinePlotSeries series = ((LinePlotBase) plot).getSeries(0);
                            if (newPass) {
                                series.appendData(xValue, Double.NaN);
                            }
                            if (series instanceof LinePlotErrorSeries) {
                                DescStatsDouble d = (val instanceof DescStatsDouble) ? (DescStatsDouble) val : new DescStatsDouble(new Number[]{(Number) val}, -1);
                                if ((prefs.plotTypes != null) && (prefs.plotTypes.containsKey(series.getName()) && (prefs.plotTypes.get(series.getName()).equals("minmax")))) {
                                    ((LinePlotErrorSeries) ((LinePlotJFree) plot).getSeries(0)).appendData(xValue, d.doubleValue(), d.getMin(), d.getMax());
                                } else {
                                    ((LinePlotErrorSeries) ((LinePlotJFree) plot).getSeries(0)).appendData(xValue, d.doubleValue(), d.getStdev());
                                }
                            } else {
                                ((LinePlotBase) plot).getSeries(0).appendData(xValue, ((Number) val).doubleValue());
                            }
                        }
                    }
                }
            }
            currentPass = record.getPass();
        }
    };

    Class getPlotClass(Object plotType) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (plotType != null) {
            if (plotType instanceof Number) {
                switch (((Number) plotType).intValue()) {
                    case 1:
                        plotType = PlotPanel.getLinePlotImpl();
                        break;
                    case 2:
                        plotType = PlotPanel.getMatrixPlotImpl();
                        break;
                    case 3:
                        plotType = PlotPanel.getSlicePlotImpl();
                        break;
                }
            }
            if (plotType instanceof String) {
                if (Context.getInstance()==null){
                    try{
                        plotType =  Class.forName((String) plotType);
                    } catch (ClassNotFoundException ex) {                                                
                    }
                } else {                    
                    plotType = Context.getInstance().getClassByName((String) plotType);
                }
            }
            if (plotType instanceof Class) {
                return (Class) plotType;
            }
        }
        return null;
    }

    Plot newPlot(String name, boolean isScan, int dim, boolean allowLowerDim) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Plot requestedPlot = null;
        try {
            if (isScan && (prefs.plotTypes != null)) {
                Class type = getPlotClass(prefs.plotTypes.get(name));
                //If device name matches a Cacheable cache name, use the rule for the parent  
                if (type != null) {
                    requestedPlot = (Plot) type.newInstance();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
        }

        if (dim == 3) {
            return Plot.newPlot(getSlicePlotImpl());
        }

        if (dim == 2) {
            if ((requestedPlot != null)) {
                if (allowLowerDim && (requestedPlot instanceof LinePlot)) {
                    return requestedPlot;
                } else if (requestedPlot instanceof MatrixPlot) {
                    return requestedPlot;
                }
            }
            return Plot.newPlot(getMatrixPlotImpl());
        }
        if ((requestedPlot != null) && (requestedPlot instanceof LinePlot)) {
            return requestedPlot;
        }
        return Plot.newPlot(getLinePlotImpl());
    }

    protected Plot addPlot(String name, boolean isScan, String labelX, int rank, int[] recordSize, double[] start, double[] end, int[] steps, Class type) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Plot plot = null;
        int recordDimensions = (recordSize == null) ? 0 : recordSize.length;

        if (rank == 3) {
            //Don't use slice plot during scan
            if (isScan) {
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    if ((start != null) && (start.length > 0) && (end != null) && (end.length > 0)) {
                        if (prefs.range != null) {
                            plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        } else if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                            plot.getAxis(Plot.AxisId.X).setRange(Math.min(start[0], end[0]), Math.max(start[0], end[0]));
                        }
                    }
                    plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    plot.addSeries(new LinePlotSeries(name));
                } else {
                    MatrixPlotSeries series = null;
                    if (recordDimensions == 0) {
                        series = new MatrixPlotSeries(name, start[2], end[2], steps[2] + 1, start[1], end[1], steps[1] + 1);
                    } else if (recordDimensions == 1) {
                        int ySize = recordSize[0];
                        series = new MatrixPlotSeries(name, start[0], end[0], steps[0] + 1, 0, ySize - 1, ySize);
                    } else if (recordDimensions == 2) {
                        series = new MatrixPlotSeries(name, recordSize[0], recordSize[1]);
                    } else {
                        return null;
                    }
                    
                    if (prefs.range != null) {
                        if (series.getNumberOfBinsX()==0){
                            series.setNumberOfBinsX(DEFAULT_RANGE_STEPS);
                        }
                        plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        series.setRangeX(prefs.range.min, prefs.range.max);                        
                    }
                    if (prefs.rangeY != null) {
                        if (series.getNumberOfBinsY()==0){
                            series.setNumberOfBinsY(DEFAULT_RANGE_STEPS);
                        }
                        plot.getAxis(Plot.AxisId.Y).setRange(prefs.rangeY.min, prefs.rangeY.max);                        
                        series.setRangeY(prefs.rangeY.min, prefs.rangeY.max);
                    }                    
                        //}                
                    plot.addSeries(series);
                
                }
            } else {
                if ((recordSize == null) || (recordSize[0] == 1)) {

                    plot = newPlot(name, isScan, 3, false);
                    SlicePlotSeries series = new SlicePlotSeries(name, start[1], end[1], steps[1] + 1, start[2], end[2], steps[2] + 1, start[0], end[0], steps[0] + 1);
                    plot.addSeries(series);
                }
            }
        } else if (rank == 2) {
            if (recordDimensions == 2) {
                int xSize = recordSize[0];
                int ySize = recordSize[1];
                if ((start == null) || (start.length != 2)) {
                    start = new double[]{0, 0};
                }
                if ((end == null) || (end.length != 2)) {
                    end = new double[]{xSize - 1, ySize - 1};
                }

                MatrixPlotSeries series = new MatrixPlotSeries(name, start[0], end[0], xSize, start[1], end[1], ySize);
                plot = newPlot(name, isScan, 2, false);
                plot.addSeries(series);
            } else if (recordDimensions == 1) {
                int ySize = recordSize[0];
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    LinePlotSeries series = new LinePlotSeries(name);
                    plot.addSeries(series);
                } else {
                    //TODO: should be start[2] /end[2] in some cases?
                    double y_start = (start.length > 2) ? start[1] : 0;
                    double y_end = (end.length > 2) ? end[1] : (ySize - 1);
                    int nX = ((steps[0] < 0)||(steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : steps[0] + 1;
                    MatrixPlotSeries series = new MatrixPlotSeries(name, (prefs.range != null) ? prefs.range.min : start[0], (prefs.range != null) ? prefs.range.max : end[0], nX,
                            y_start, y_end, ySize);
                    plot.addSeries(series);
                }
            } else {
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    if ((start != null) && (start.length > 0) && (end != null) && (end.length > 0)) {
                        if (prefs.range != null) {
                            plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        } else if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                            plot.getAxis(Plot.AxisId.X).setRange(Math.min(start[0], end[0]), Math.max(start[0], end[0]));
                        }
                    }
                    plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    double step_size = (steps[1]!=0) ? ((end[1] - start[1]) / (steps[1])) : 0;
                    for (int i = 0; i <= steps[1]; i++) {
                        double y = start[1] + (i * step_size);
                        String seriesName = String.valueOf(Convert.roundDouble(y, 8));
                        while (Arr.containsEqual(plot.getSeriesNames(), seriesName)){
                            seriesName = seriesName + "'";
                        }                
                        LinePlotSeries series = new LinePlotSeries(seriesName);
                        plot.addSeries(series);
                    }
                } else {
                    MatrixPlotSeries series = null;
                    //if (isScan){
                    //    series =series = new MatrixPlotSeries(name, (prefs.range != null) ? prefs.range.min : start[0], (prefs.range != null) ? prefs.range.max : end[0], steps[0] + 1, start[1], end[1], steps[1] + 1);
                    //} else {
                    int nX = ((steps[0] < 0)|| (steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : (steps[0] + 1);
                    int nY = ((steps[1] < 0)|| (steps[1] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : (steps[1] + 1);
                    series = new MatrixPlotSeries(name, start[0], end[0], nX, start[1], end[1], nY);
                    if (prefs.range != null) {
                        plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        if (changedScaleX || ((steps[0] < 0))) {
                            series.setRangeX(prefs.range.min, prefs.range.max);
                        }
                    }
                    if (prefs.rangeY != null) {
                        plot.getAxis(Plot.AxisId.Y).setRange(prefs.rangeY.min, prefs.rangeY.max);
                        if ((steps[1] < 0)) {
                            series.setRangeY(prefs.rangeY.min, prefs.rangeY.max);
                        }
                    }
                    //}
                    plot.addSeries(series);
                }
            }
        }
        if (rank <= 1) {
            if (recordDimensions == 2) {
                plot = newPlot(name, isScan, 2, false);
                int xSize = recordSize[0];
                int ySize = recordSize[1];
                if ((start == null) || (start.length != 2)) {
                    start = new double[]{0, 0};
                }
                if ((end == null) || (end.length != 2)) {
                    end = new double[]{xSize - 1, ySize - 1};
                }

                MatrixPlotSeries series = new MatrixPlotSeries(name, start[0], end[0], xSize, start[1], end[1], ySize);
                plot.addSeries(series);
            } else if (recordDimensions == 1) {
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    plot.getAxis(Plot.AxisId.X).setLabel(null);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    LinePlotSeries series = new LinePlotSeries(name);
                    plot.addSeries(series);

                } else {
                    int ySize = recordSize[0];
                    double y_start = (start.length > 1) ? start[1] : 0;
                    double y_end = (end.length > 1) ? end[1] : (ySize - 1);
                    MatrixPlotSeries series = null;
                    //if (isScan){
                    //    series = new MatrixPlotSeries(name, (prefs.range != null) ? prefs.range.min : start[0], (prefs.range != null) ? prefs.range.max : end[0], steps[0] + 1, y_start, y_end, ySize);
                    //} else {
                    int nX = ((steps[0] < 0)||(steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : steps[0] + 1;
                    series = new MatrixPlotSeries(name, start[0], end[0], nX, y_start, y_end, ySize);
                    if (prefs.range != null) {
                        plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        if (changedScaleX) {
                            series.setRangeX(prefs.range.min, prefs.range.max);
                        }
                    }
                    //}
                    plot.addSeries(series);
                }
            } else {
                
                plot = newPlot(name, isScan, 1, true);
                if ((type == Averager.class) && (plot instanceof LinePlotJFree)) {
                    ((LinePlotJFree) plot).setStyle(Style.ErrorY);
                    ((LinePlotJFree) plot).addSeries(new LinePlotErrorSeries(name));
                } else {
                    plot.addSeries(new LinePlotSeries(name));
                }
                if (prefs.range != null) {
                    plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                } else if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                    if ((start != null) && (start.length > 0) && (end != null) && (end.length > 0)) {
                        plot.getAxis(Plot.AxisId.X).setRange(Math.min(start[0], end[0]), Math.max(start[0], end[0]));
                    }
                }
                if (prefs.rangeY != null) {
                    plot.getAxis(Plot.AxisId.Y).setRange(prefs.rangeY.min, prefs.rangeY.max);
                }                
                plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                plot.getAxis(Plot.AxisId.Y).setLabel(null);
            }
        }

        if (plot != null) {
            if (plot instanceof MatrixPlotBase) {
                addSurfacePlotMenu((MatrixPlotBase) plot);
            } else if (plot instanceof SlicePlotDefault) {
                addSurfacePlotMenu(((SlicePlotDefault) plot).getMatrixPlot());
            }
            plot.setTitle(name);
            addPlot((PlotBase) plot);
            plot.setUpdatesEnabled(offscreen);
        }
        return plot;
    }

    public static void addSurfacePlotMenu(final MatrixPlotBase plot) {
        if (getSurfacePlotImpl() != null) {
            JMenuItem detachPlotMenuItem = new JMenuItem("Surface Plot");
            detachPlotMenuItem.addActionListener((ActionEvent e) -> {
                plot.detach(getSurfacePlotImpl());
            });
            plot.addPopupMenuItem(detachPlotMenuItem);
        }
    }

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

    public Plot addPlot(PlotDescriptor descriptor) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object data = descriptor.data;
        if (data == null) {
            data = new double[0];
        }
        if (descriptor.unsigned) {
            data = Convert.toUnsigned(data);
        }
        data = validatePlotDataType(data);

        int[] shape = Arr.getShape(data);
        int dimensions = shape.length;
        int rank = descriptor.rank;
        double[] x = descriptor.x;
        double[] y = descriptor.y;
        double[] z = descriptor.z;
        int[] numberSteps = descriptor.steps;
        boolean multidimentional1dDataset = descriptor.isMultidimentional1dArray();
        boolean forceMatrixPlot=false;
        boolean sparseArrayData = false;

        if (rank < 0) {
            rank = dimensions;
        }

        double[] start = null;
        double[] end = null;
        int[] steps = null;

        if (dimensions == 1) {
            //2D-scan with 1d datasets
            if ((numberSteps != null) && (numberSteps.length == 2) && (y != null) && (x != null)
                    && (x.length == y.length) && (((double[]) data).length == x.length)) {
                start = new double[]{(Double) Arr.getMin(x), (Double) Arr.getMin(y)};
                end = new double[]{(Double) Arr.getMax(x), (Double) Arr.getMax(y)};
                steps = numberSteps;
                rank = 2;
                multidimentional1dDataset = true;
            }
        } else if (dimensions == 2) {
            if (rank == 3) {
                //z must be set
                double[][] array = (double[][]) data;
                start = new double[]{z[0], 0, 0};
                end = new double[]{z[z.length - 1], shape[1] - 1, shape[0] - 1};
                steps = new int[]{z.length - 1, array[0].length - 1, array.length - 1};
                if ((x != null) && (x.length > 0)) {
                    start[1] = x[0];
                    end[1] = x[x.length - 1];
                    steps[1] = x.length - 1;
                }
                if ((y != null) && (y.length > 0)) {
                    start[2] = y[0];
                    end[2] = y[y.length - 1];
                    steps[2] = y.length - 1;
                }
            } else {
                //2D-scan with 1d datasets of arrays 
                if (multidimentional1dDataset) {
                    sparseArrayData = (dimensions==2) && (numberSteps[0] < 0) && (numberSteps[1] < 0);
                    // Sparse table: don't try to make 3d plots of array data
                    if (sparseArrayData){
                        //data = Convert.transpose(data);
                        start = new double[]{(Double) Arr.getMin(x), 0};
                        end = new double[]{(Double) Arr.getMax(x), shape[1] - 1};
                        steps = new int[]{-1, shape[1] - 1};
                        rank = 2;
                    } else {
                        start = new double[]{(Double) Arr.getMin(x), (Double) Arr.getMin(y), 0};
                        end = new double[]{(Double) Arr.getMax(x), (Double) Arr.getMax(y), shape[1] - 1};
                        steps = new int[]{numberSteps[0], numberSteps[1], shape[1] - 1};
                        if (numberSteps.length==2){
                            if ((z != null) && (z.length > 0)) {
                                start[2] = z[0];
                                end[2] = z[z.length - 1];
                                steps[2] = z.length - 1;
                            }
                            rank = 3;
                        } else if (numberSteps.length>2){
                            start = new double[]{0, 0};
                            end = new double[]{z.length-1, shape[1] -1};
                            steps = new int[]{z.length-1, shape[1] -1};
                            rank = 2;
                            forceMatrixPlot = true;
                        }
                    }
                } else {
                    start = new double[2];
                    end = new double[2];
                    steps = new int[2];
                    if ((x == null) || (x.length == 0)) {
                        start[0] = 0;
                        end[0] = shape[1] - 1;
                        steps[0] = shape[1] - 1;
                    } else {
                        //TODO:Should create a 3d plot to support multipass scans(overlapping samples)?
                        int length = x.length;
                        if (descriptor.passes > 1) {
                            length /= descriptor.passes;
                        }
                        start[0] = x[0];
                        end[0] = x[length - 1];
                        steps[0] = length - 1;
                    }
                    if ((y == null) || (y.length == 0)) {
                        start[1] = 0;
                        end[1] = shape[0] - 1;
                        steps[1] = shape[0] - 1;
                    } else {
                        start[1] = y[0];
                        end[1] = y[y.length - 1];
                        steps[1] = y.length - 1;
                    }
                }
            }
        } else if (dimensions == 3) {
            double[][][] array = (double[][][]) data;

            start = new double[3];
            start[0] = 0;
            end = new double[3];
            end[0] = array.length - 1;
            steps = new int[3];
            steps[0] = array.length - 1;

            if ((y == null) || (y.length == 0)) {
                start[2] = 0;
                end[2] = shape[1] - 1;
                steps[2] = shape[1] - 1;
            } else {
                start[2] = y[0];
                end[2] = y[y.length - 1];
                steps[2] = y.length - 1;
            }

            if ((x == null) || (x.length == 0)) {
                start[1] = 0;
                end[1] = shape[2] - 1;
                steps[1] = shape[2] - 1;
            } else {
                start[1] = x[0];
                end[1] = x[x.length - 1];
                steps[1] = x.length - 1;
            }
        }

        Plot plot = addPlot(descriptor.name, false, descriptor.labelX, rank, null, start, end, steps, Double.class);
        if (plot != null) {
            if (plot instanceof LinePlotBase) {
                if ((descriptor.error != null) && (descriptor.error.length == ((double[]) data).length) && (plot instanceof LinePlotJFree)) {
                    ((LinePlotJFree) plot).setStyle(Style.ErrorY);
                    LinePlotErrorSeries series = new LinePlotErrorSeries(descriptor.name);
                    ((LinePlotJFree) plot).addSeries(series);
                    series.setData(x, (double[]) data, descriptor.error);
                } else {
                    ((LinePlotSeries) plot.getSeries(0)).setData(x, (double[]) data);
                }
            } else if (plot instanceof MatrixPlot) {
                MatrixPlotSeries series = ((MatrixPlotSeries) plot.getSeries(0));
                if (multidimentional1dDataset){
                    if (sparseArrayData){
                        double[][] array = (double[][]) data;
                        //Already checked array sizes                   
                        for (int i = 0; i < array.length; i++) {
                            for (int j = 0; j < array[i].length; j++) {
                                series.appendData(x[i], j, array[i][j]);
                            }
                        }                        
                    } else {
                        if (forceMatrixPlot){
                            double[][] array = (double[][]) data;
                            //Already checked array sizes                   
                            for (int i = 0; i < array.length; i++) {
                                for (int j = 0; j < array[i].length; j++) {
                                    series.appendData(i, j, array[i][j]);
                                }
                            }
                        } else {
                            double[] array = (double[]) data;
                            //Already checked array sizes                   
                            for (int i = 0; i < array.length; i++) {
                                series.appendData(x[i], y[i], array[i]);
                            }
                        }
                    }
                } else {
                    series.setData((double[][]) data);
                }
            } else if (plot instanceof SlicePlot) {
                final SlicePlotSeries series = ((SlicePlotSeries) plot.getSeries(0));
                if (multidimentional1dDataset) {
                    double[][] array = (double[][]) data;
                    final int[] _steps = steps;
                    ((SlicePlotSeries) plot.getSeries(0)).setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            int begin = page * (_steps[1] + 1);
                            series1.clear();
                            for (int i = begin; i < begin + (_steps[1] + 1); i++) {
                                for (int j = 0; j < (_steps[2] + 1); j++) {
                                    series1.appendData(y[i], (z == null) ? j : z[j], array[i][j]);
                                }
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                } else if (dimensions == 3) {
                    double[][][] array = (double[][][]) data;
                    series.setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            series1.setData(array[page]);
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                } else {
                    double[][] array = (double[][]) data;
                    series.setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            DataSlice slice = Context.getInstance().getDataManager().getData(descriptor.root, descriptor.path, page);
                            Object data1 = slice.sliceData;
                            if (slice.unsigned) {
                                data1 = Convert.toUnsigned(data1);
                            }
                            series1.setData((double[][]) Convert.toDouble(data1));
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                }
            }

            plot.update(true);
            plot.setUpdatesEnabled(true);//This plot is user general-purpose so disable scan optimization
            if (!offscreen) {                
                validate();
                repaint();
            }
        }
        return plot;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
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
