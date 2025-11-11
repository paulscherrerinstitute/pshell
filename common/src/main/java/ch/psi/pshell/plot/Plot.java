package ch.psi.pshell.plot;

import ch.psi.pshell.utils.Range;
import ch.psi.pshell.utils.Reflection.Hidden;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 *
 */
public interface Plot<T extends PlotSeries> {        
    
    //Request update on event loop
    public void update(boolean deferred);

    public void setUpdatesEnabled(boolean value);

    
    default public void disableUpdates(){
        setUpdatesEnabled(false);        
    }

    default public void reenableUpdates(){
        update(true);
        setUpdatesEnabled(true);        
    }


    public boolean isUpdatesEnabled();

    //Generic properties
    public void setTitle(String title);

    public String getTitle();

    public void setTitleFont(Font font);

    public Font getTitleFont();

    public enum Quality {

        Low,
        Medium,
        High,
        Maximum
    }

    public void setQuality(Quality quality);

    public Quality getQuality();

    //Generic operations
    public void saveData(String filename) throws IOException;

    default public BufferedImage getSnapshot() throws IOException{
        return getSnapshot(null);
    }
    public BufferedImage getSnapshot(Dimension size);

    default public void saveSnapshot(String filename, String format) throws IOException{
        saveSnapshot(filename, format, null);
    }
            
    public void saveSnapshot(String filename, String format, Dimension size) throws IOException;

    public void copy();

    //Series list management
    public void addSeries(T series);

    public void addSeries(T[] series);

    public void removeSeries(T series);

    public T getSeries(String name);

    public T getSeries(int index);
    
    public int getSeriesIndex(T series);

    public int getNumberOfSeries();

    public void updateSeries(T series);

    public void requestSeriesUpdate(final T series);

    public double[][] getSeriesData(final T series);

    public T[] getAllSeries();

    public String[] getSeriesNames();
    
    public void clear();    //remove all series

    //Axis
    public enum AxisId {

        X, X2, Y, Y2, Z;
    };

    public Axis getAxis(AxisId id);
        
    public Range getAxisRange(AxisId axis);
    
    //Markers and text are optional
    public Object addMarker(double val, AxisId axis, String label, Color color);

    public Object addIntervalMarker(double start, double end, final AxisId axis, String label, Color color);

    public void removeMarker(final Object marker);

    public List getMarkers();

    public Object addText(double x, double y, String label, Color color);

    public void removeText(Object text);

    public List getTexts();
    
    public final static DateFormat displayFormatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public final static NumberFormat displayFormatValue =  new NumberFormat(){
        final DecimalFormat simpleFormat = new DecimalFormat("#,##0.######");
        final DecimalFormat scientificFormat = new DecimalFormat("0.######E0");
        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            // Adaptive formatting logic: No scientific notation for simple values
            String formatted;
            double aux = Math.abs(number);
            if (aux >= 0.0001 && aux < 1_000_000) {
                formatted = simpleFormat.format(number);
            } else {
                formatted = scientificFormat.format(number);
            }
            return toAppendTo.append(formatted);
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return format((double) number, toAppendTo, pos);
        }
        
        @Override
        public Number parse(String source, ParsePosition pos) {
            return new DecimalFormat().parse(source, pos); // Fallback parsing
        }        
    };       

    default public String getDisplayValue(double value) {        
        return displayFormatValue.format(value);        
    }

    default public String getDisplayDateValue(double value) {
        Instant instant = Instant.ofEpochMilli(Double.valueOf(value).longValue());       
        return displayFormatDate.format(Date.from(instant));
    }

    default public String getPersistenceValue(double value) {
        return String.valueOf(value);
    }            
    
    default void setPlotBackgroundColor(Color c) {};

    default void setPlotGridColor(Color c) {};    
    
    default void setPlotOutlineColor(Color c) {};      
    
    default void setPlotOutlineWidth(int width) {};

    default Color getPlotOutlineColor() {return null;};      
    
    default Color getPlotBackgroundColor() {return null;};      
    
    default Color getPlotGridColor() {return null;};                
    
    default int getPlotOutlineWidth() {return -1;};
    
    default void setLabelFont(Font f) {}

    default void setTickLabelFont(Font f) {}
    
    
    
    @Hidden
    public static Plot newPlot(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return (Plot) Class.forName(className).newInstance();
    }    
    
    
    default public boolean isOffscreen(){return false;}
    
    default public boolean isBufferedRendering(){return true;}
    
}
