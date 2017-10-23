package ch.psi.pshell.plot;

import javax.swing.JMenuItem;

/**
 *
 */
public interface TimePlot extends Plot<TimePlotSeries> {

    void clear();

    int getDurationMillis();

    boolean isMarkersVisible();

    boolean isStarted();

    void addPopupMenuItem(final JMenuItem item);

    void setDurationMillis(int duration);

    void setMarkersVisible(boolean visible);

    void setLegendVisible(boolean visible);

    boolean isLegendVisible();

    void setY1AxisAutoScale();

    void setTimeAxisLabel(String label);

    String getTimeAxisLabel();

    void setY1AxisScale(double min, double max);

    void setY2AxisAutoScale();

    void setY2AxisScale(double min, double max);

    void start();

    void stop();

    void add(double value);

    void add(int index, double value);

    void add(int index, long time, double value);

    void add(double[] values);

    void add(long time, double[] values);

    void addTerminator(int index);

    void addTerminators();
        
}
