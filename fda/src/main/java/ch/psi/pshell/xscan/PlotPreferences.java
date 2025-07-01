package ch.psi.pshell.xscan;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.PlotLayout;
import ch.psi.pshell.utils.Config;
import java.awt.Color;

/**
 * Entity class holder user Workbench displaying preferences.
 */
public class PlotPreferences extends Config {

    public PlotLayout layout = PlotLayout.Vertical;
    public Quality quality = Quality.High;
    public Colormap colormap = Colormap.Temperature;
    public int markerSize;
    public Color colorBackground = null; //Default
    public Color colorGrid = null;  //Default
    public Color colorOutline = null; //Default

    @Override
    protected Object convertStringToField(Class type, String str) {
        if (type == Color.class) {
            return getColorFromString(str);
        }
        return super.convertStringToField(type, str);
    }

    @Override
    protected String convertFieldToString(Object val) {
        if ((val != null) && (val.getClass() == Color.class)) {
            Color c = (Color) val;
            return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        }
        return super.convertFieldToString(val);
    }

    public static Color getColorFromString(String str) {
        if (str == null) {
            return null;
        }
        String[] tokens = str.split(",");
        if (tokens.length == 3) {
            return new Color(Integer.valueOf(tokens[0].trim()), Integer.valueOf(tokens[1].trim()), Integer.valueOf(tokens[2].trim()));
        }
        try {
            return (Color) Color.class.getField(str.trim()).get(null);
        } catch (Exception ex) {
            return null;
        }
    }
}
