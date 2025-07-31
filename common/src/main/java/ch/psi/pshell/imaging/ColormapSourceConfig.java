package ch.psi.pshell.imaging;

public class ColormapSourceConfig extends SourceConfig {

    public boolean colormapAutomatic = false;
    public double colormapMin = Double.NaN;
    public double colormapMax = Double.NaN;
    public Colormap colormap = Colormap.Grayscale;
    public boolean colormapLogarithmic = false;

    public boolean isDefaultColormap() {
        return (Double.isNaN(colormapMin) || Double.isNaN(colormapMax)) && (colormapAutomatic == false);
    }
}
