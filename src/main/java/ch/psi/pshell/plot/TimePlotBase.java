package ch.psi.pshell.plot;

import ch.psi.pshell.device.TimestampedValue;
import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
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

    @Override
    protected void createChart() {
        super.createChart();
        createAxis(AxisId.X, "Time");
        createAxis(AxisId.Y, "");
        createAxis(AxisId.Y2, "");
    }

    JCheckBoxMenuItem menuMarkers;
    JCheckBoxMenuItem menuLegend;
    JMenuItem menuStopStart;
    JMenuItem menuReset;
    JMenu menuSeries;
    Map<Integer, Boolean> empty = new HashMap<>();

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
        
        JMenu menuScales= new JMenu("Logarithmic Scale");
        
        JCheckBoxMenuItem menuLogarithmicY1 = new JCheckBoxMenuItem("Y1");
        menuLogarithmicY1.addActionListener((ActionEvent e) -> {
            setY1Logarithmic(menuLogarithmicY1.isSelected());
        });  
        
        JCheckBoxMenuItem menuLogarithmicY2 = new JCheckBoxMenuItem("Y2");
        menuLogarithmicY2.addActionListener((ActionEvent e) -> {
            setY2Logarithmic(menuLogarithmicY2.isSelected());
        });   

        menuScales.add(menuLogarithmicY1);
        menuScales.add(menuLogarithmicY2);

        menuSeries = new JMenu("Series Visibility");

        JMenuItem menuDisplayDuration = new JMenuItem("Duration...");
        menuDisplayDuration.addActionListener((ActionEvent e) -> {
            try {
                String str = getString("Enter time window (display duration) in seconds:", String.valueOf(getDurationMillis() / 1000));
                if (str != null) {
                    setDurationMillis(Integer.valueOf(str) * 1000);
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });

        getPopupMenu().addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menuMarkers.setSelected(isMarkersVisible());
                menuLegend.setSelected(isLegendVisible());
                menuSeries.removeAll();
                for (TimePlotSeries series : getAllSeries()) {
                    JCheckBoxMenuItem item = new JCheckBoxMenuItem(series.getName(), !isSeriesHidden(series));
                    item.addActionListener((ActionEvent ae) -> {
                        setSeriesHidden(series, !item.isSelected());
                    });
                    menuSeries.add(item);
                }
                menuLogarithmicY1.setSelected(isY1Logarithmic());
                menuLogarithmicY2.setSelected(isY2Logarithmic());
                menuLogarithmicY1.setVisible(started);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        addPopupMenuItem(null);
        addPopupMenuItem(menuScales);    
        addPopupMenuItem(menuMarkers);
        addPopupMenuItem(menuLegend);
        addPopupMenuItem(menuStopStart);
        addPopupMenuItem(menuReset);
        addPopupMenuItem(menuSeries);
        addPopupMenuItem(null);
        addPopupMenuItem(menuDisplayDuration);

    }

    abstract protected JPopupMenu getPopupMenu();

    abstract public void setAxisSize(int size);

    abstract public int getAxisSize();

    public boolean isSeriesHidden(TimePlotSeries series) {
        if (series!=null){
            Color color = getSeriesColor(series);
            if (color!=null){
                return getSeriesColor(series).equals(TRANSPARENT);
            }
        }
        return false;
    }

    Color TRANSPARENT = new Color(1, 0, 0, 0);
    Map<TimePlotSeries, Color> hiddenPlots = new HashMap<>();

    public void setSeriesHidden(TimePlotSeries series, boolean hidden) {
        if (hidden != isSeriesHidden(series)) {
            if (hidden) {
                hiddenPlots.put(series, getSeriesColor(series));
                setSeriesColor(series, TRANSPARENT);
            } else {
                setSeriesColor(series, hiddenPlots.get(series));
            }
        }
    }

    @Override
    public void clear() {
        for (int i=0; i< getNumberOfSeries(); i++) {
            doClear(i);
        }
        if (isShowing()) {
            repaint();
        }
    }

    @Override
    public void clear(int graphIndex) {
        doClear(graphIndex);
        if (isShowing()) {
            repaint();
        }
    }

    abstract protected void doClear(int graphIndex);
    
    
    

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
        addTerminator(index, (timestamp == null) ? System.currentTimeMillis() : timestamp + 1);
    }

    @Override
    public void addTerminators() {
        for (int i = 0; i < getNumberOfSeries(); i++) {
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
            addDataPoint(index, time, value, false);
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
                    addDataPoint(i, time, values[i], false);
                }
            }
        }
    }
    
    public void delete(int index, int pointIndex) {
        if (isStarted()) {
            removeDataPoint(index, pointIndex, true);
        }
    }    

    @Override
    public void drag(int index, long time, Double value) {
        if (isStarted()) {
            if (getItemCount(index)>0){
                if (value==null){
                   value = getLastValue(index);
                }
                addDataPoint(index, time, (value==null) ? Double.NaN : value, true);
            } else {
                addDataPoint(index, time,Double.NaN , true);
                removeDataPoint(index, 0, false);
            }
        }
    }
    
    public boolean isEmpty(int index){
        return Boolean.TRUE.equals(empty.get(index));
    }
    
    abstract protected void addDataPoint(int graphIndex, long time, double value, boolean drag);
    
    abstract protected void removeDataPoint(int graphIndex, int index, boolean update);

    abstract public List<TimestampedValue<Double>> getSeriestData(int index);

    abstract public String getSeriesName(int index);

    abstract public TimestampedValue<Double> getItem(int index, int itemIndex);

    public TimestampedValue<Double> getLastItem(int index) {
        return getItem(index, -1);
    }

    public Double getLastValue(int index) {
        TimestampedValue<Double> ret = getLastItem(index);
        return (ret == null) ? null : ret.getValue();
    }

    public Long getLastTimestamp(int index) {
        TimestampedValue<Double> ret = getLastItem(index);
        return (ret == null) ? null : ret.getTimestamp();
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
        legendVisible = visible;
        doSetLegendVisible(visible);
        if (isShowing()) {
            repaint();
        }
    }

    boolean legendVisible = true;

    @Override
    public boolean isLegendVisible() {
        return legendVisible;
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
    public int getItemCount(int index) {
       return getSeriestData(index).size();
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
    
    @Override
    public boolean isY1Logarithmic(){
        return false;
    }
    
    @Override
    public void setY1Logarithmic(boolean value){
    }    
    
    @Override
    public boolean isY2Logarithmic(){
        return false;
    }
    
    @Override
    public void setY2Logarithmic(boolean value){
    }        

}
