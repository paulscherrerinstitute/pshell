package ch.psi.pshell.plot;

import ch.psi.utils.Convert;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface Plot<T extends PlotSeries> {

    //Request update on event loop
    public void update(boolean deferred);

    public void setUpdatesEnabled(boolean value);

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

    public BufferedImage getSnapshot();

    public void saveSnapshot(String filename, String format) throws IOException;

    public void copy();

    //Series list management
    public void addSeries(T series);

    public void addSeries(T[] series);

    public void removeSeries(T series);

    public T getSeries(String name);

    public T getSeries(int index);

    public int getNumberOfSeries();

    public void updateSeries(T series);

    public void requestSeriesUpdate(final T series);

    public double[][] getSeriesData(final T series);

    public T[] getAllSeries();

    public void clear();    //remove all series

    //Axis
    public enum AxisId {

        X, Y, Y2, Z;
    };

    public Axis getAxis(AxisId id);

    //Markers and text are optional
    public Object addMarker(double val, AxisId axis, String label, Color color);

    public Object addIntervalMarker(double start, double end, final AxisId axis, String label, Color color);

    public void removeMarker(final Object marker);

    public List getMarkers();

    public Object addText(double x, double y, String label, Color color);

    public void removeText(Object text);

    public List getTexts();

    default public String getDisplayableValue(double value) {
        if ((value > 100000) || ((Math.abs(value) < 0.0001) && (Math.abs(value) > 0))) {
            return String.format("%1.6e", value);
        } else {
            value = Convert.roundDouble(value, 4);
            return String.format("%1.4f", value);
        }
    }
}
