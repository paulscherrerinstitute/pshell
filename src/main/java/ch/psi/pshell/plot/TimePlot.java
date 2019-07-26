package ch.psi.pshell.plot;

import javax.swing.JMenuItem;

/**
 *
 */
public interface TimePlot extends Plot<TimePlotSeries> {

    void clear();
    
    void clear(int graphIndex);

    int getDurationMillis();

    boolean isMarkersVisible();

    boolean isStarted();

    void addPopupMenuItem(final JMenuItem item);

    void setDurationMillis(int duration);

    void setMarkersVisible(boolean visible);

    void setLegendVisible(boolean visible);

    boolean isLegendVisible();

    void setTimeAxisLabel(String label);

    String getTimeAxisLabel();
    
    void setY1AxisAutoScale();   
     
    void setY1AxisScale(double min, double max);

    void setY2AxisAutoScale();

    void setY2AxisScale(double min, double max);
    
    boolean isY1Logarithmic();
    
    void setY1Logarithmic(boolean value);
    
    boolean isY2Logarithmic();
    
    void setY2Logarithmic(boolean value);

    void start();

    void stop();

    void add(double value);

    void add(int index, double value);

    void add(int index, long time, double value);

    void add(double[] values);

    void add(long time, double[] values);
    
    void drag(int index, long time, Double value);

    void addTerminator(int index);

    void addTerminators();
    
    void addTerminator(int index, long time);

    void addTerminators(long time);    
    
    int getItemCount(int index);
        
}
