package ch.psi.pshell.plot;

import ch.psi.pshell.device.TimestampedValue;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import java.util.Arrays;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 */
public abstract class TimePlotBase extends PlotBase<TimePlotSeries> implements TimePlot, AutoCloseable {

    int duration;
    int markerSize = 4;

    protected TimePlotBase() {
        this(null);
    }

    protected TimePlotBase(String title) {
        super(TimePlotSeries.class, title);
    }

    JCheckBoxMenuItem menuMarkers;
    JCheckBoxMenuItem menuLegend;
    JMenuItem menuStopStart;
    JMenuItem menuReset;

    protected void setup() {
        setupMenus();
        setMarkersVisible(true);
        setLegendVisible(false);
    }

    protected void setupMenus() {
        menuMarkers = new JCheckBoxMenuItem("Show Markers");
        menuMarkers.addActionListener((ActionEvent e) -> {
            setMarkersVisible(menuMarkers.isSelected());
        });
        menuLegend = new JCheckBoxMenuItem("Show Legend");
        menuLegend.addActionListener((ActionEvent e) -> {
            setLegendVisible(menuLegend.isSelected());
        });

        menuStopStart = new JMenuItem("Stop");
        menuStopStart.addActionListener((ActionEvent e) -> {
            if (isStarted()) {
                stop();
            } else {
                start();
            }
        });

        menuReset = new JMenuItem("Reset");
        menuReset.addActionListener((ActionEvent e) -> {
            clear();
        });

        JMenuItem menuDisplayDuration = new JMenuItem("Duration...");
        menuDisplayDuration.addActionListener((ActionEvent e) -> {
            try {
                String str = SwingUtils.getString(TimePlotBase.this, "Enter time window (display duration) in seconds:", String.valueOf(getDurationMillis() / 1000));
                if (str != null) {
                    setDurationMillis(Integer.valueOf(str) * 1000);
                }
            } catch (Exception ex) {
                SwingUtils.showException(TimePlotBase.this, ex);
            }
        });

        getPopupMenu().addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menuMarkers.setSelected(isMarkersVisible());
                menuLegend.setSelected(isLegendVisible());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        addPopupMenuItem(null);
        addPopupMenuItem(menuMarkers);
        addPopupMenuItem(menuLegend);
        addPopupMenuItem(menuStopStart);
        addPopupMenuItem(menuReset);
        addPopupMenuItem(null);
        addPopupMenuItem(menuDisplayDuration);

    }

    abstract protected JPopupMenu getPopupMenu();

    abstract public void setAxisSize(int size);

    abstract public int getAxisSize();

    @Override
    public void clear() {
        doClear();
        if (isShowing()) {
            repaint();
        }
    }

    abstract protected void doClear();

    //State
    private boolean started = true;
    private boolean closed = false;

    @Override
    public void start() {
        started = true;
        menuStopStart.setText("Stop");
    }

    @Override
    public void stop() {
        addTerminators();
        started = false;
        menuStopStart.setText("Start");
    }

    @Override
    public boolean isStarted() {
        if (isClosed()) {
            return false;
        }
        return started;
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed == false) {
            closed = true;
            clear();
        }
    }

    protected Color getSeriesColor(TimePlotSeries series) {
        return null;
    }

    protected void setSeriesColor(TimePlotSeries series, Color color) {
    }

    //Appending elements
    @Override
    public void addTerminator(int index) {
        Long timestamp = getLastTimestamp(index);
        addTerminator(index, (timestamp==null)? System.currentTimeMillis() : timestamp+1);
    }

    @Override
    public void addTerminators() {
        for (int i=0; i< getNumberOfSeries(); i++){
            addTerminator(i);
        }
    }
    
    @Override
    public void addTerminator(int index, long time) {
        add(index, time, Double.NaN);
    }

    @Override
    public void addTerminators(long time) {
        double[] terminators = new double[getNumberOfSeries()];
        Arrays.fill(terminators, Double.NaN);
        add(time, terminators);
    }    

    @Override
    public void add(double value) {
        add(0, value);
    }

    @Override
    public void add(int index, double value) {
        add(index, System.currentTimeMillis(), value);
    }

    @Override
    public void add(int index, long time, double value) {
        if (isStarted()) {
            addDataPoint(index, time, value);
        }
    }

    @Override
    public void add(double[] values) {
        add(System.currentTimeMillis(), values);
    }

    @Override
    public void add(long time, double[] values) {
        if (isStarted()) {
            if ((values != null) && (getNumberOfSeries() == values.length)) {
                for (int i = 0; i < values.length; i++) {
                    addDataPoint(i, time, values[i]);
                }
            }
        }
    }

    abstract protected void addDataPoint(int graphIndex, long time, double value);

    abstract public List<TimestampedValue<Double>> getSeriestData(int index);

    abstract public String getSeriesName(int index);
    
    abstract public TimestampedValue<Double> getItem(int index, int itemIndex) ;

    public TimestampedValue<Double> getLastItem(int index) {
        return getItem(index, -1);
    }    
    
    public Double getLastValue(int index) {
        TimestampedValue<Double> ret = getLastItem(index);
        return (ret==null) ? null : ret.getValue();
    }      
    public Long getLastTimestamp(int index) {
        TimestampedValue<Double> ret = getLastItem(index);
        return (ret==null) ? null : ret.getTimestamp();
    }     

    //Configuration
    @Override
    public void setMarkersVisible(boolean visible) {
        markersVisible = visible;
        doSetMarkersVisible(visible);
        if (isShowing()) {
            repaint();
        }
    }

    boolean markersVisible = true;

    @Override
    public boolean isMarkersVisible() {
        return markersVisible;
    }

    abstract protected void doSetMarkersVisible(boolean visible);

    @Override
    public void setLegendVisible(boolean visible) {
        labelsVisible = visible;
        doSetLegendVisible(visible);
        if (isShowing()) {
            repaint();
        }
    }

    boolean labelsVisible = true;

    @Override
    public boolean isLegendVisible() {
        return labelsVisible;
    }

    abstract protected void doSetLegendVisible(boolean visible);

    protected TimePlotSeries getSeriesByName(String name) {
        int index = getIndex(name);
        if (index == -1) {
            return null;
        }
        return getSeries(index);
    }

    protected int getIndex(String name) {
        for (int i = 0; i < getNumberOfSeries(); i++) {
            if (name.equals(getSeries(i).name)) {
                return i;
            }
        }
        return -1;
    }

    protected int getIndex(TimePlotSeries series) {
        for (int i = 0; i < getNumberOfSeries(); i++) {
            if (series == getSeries(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return TimePlot.class.getSimpleName();
    }

    @Override
    public void detach(String className) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[][] getSeriesData(TimePlotSeries series) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
