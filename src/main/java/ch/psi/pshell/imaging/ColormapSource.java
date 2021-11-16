package ch.psi.pshell.imaging;

import ch.psi.utils.Range;
import ch.psi.utils.Convert;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 *
 */
public abstract class ColormapSource extends SourceBase {

    public static class ColormapSourceConfig extends SourceConfig {

        public boolean colormapAutomatic = false;
        public double colormapMin = Double.NaN;
        public double colormapMax = Double.NaN;
        public Colormap colormap = Colormap.Grayscale;
        public boolean colormapLogarithmic = false;

        public boolean isDefaultColormap() {
            return (Double.isNaN(colormapMin) || Double.isNaN(colormapMax)) && (colormapAutomatic == false);
        }
    }

    @Override
    public ColormapSourceConfig getConfig() {
        return (ColormapSourceConfig) super.getConfig();
    }

    public Range getColormapRange() {
        if (getConfig().colormapAutomatic) {
            return null;
        }
        if (getConfig().isDefaultColormap()) {
            return new Range(0.0, 255.0);
        }
        return new Range(getConfig().colormapMin, getConfig().colormapMax);
    }
    
    Range getCurrentColormapRange(){
        Range scale = getColormapRange();
        if (scale == null) {
            Data data = getData();
            scale = (data==null)? new Range(0.0, 255.0) : data.getProperties();
        }        
        return scale;
    }    

    protected ColormapSource(String name, ColormapSourceConfig config) {
        super(name, config);
    }

    protected void pushData(Object array1d, int width, int height) throws IOException {
        pushData(array1d, width, height, null);
    }

    protected void pushData(Object array1d, int width, int height, Boolean unsigned) throws IOException {
        if (array1d == null) {
            pushImage(null);
            return;
        }
        Data data = new Data(array1d, width, height, unsigned);
        pushImage(null, data);
    }

    protected void pushData(Object array2d) throws IOException {
        pushData(array2d, null);
    }

    protected void pushData(Object array2d, Boolean unsigned) throws IOException {
        if (array2d == null) {
            pushImage(null);
            return;
        }
        pushData(Convert.flatten(array2d), Array.getLength(Array.get(array2d, 0)), Array.getLength(array2d), unsigned);
    }

    @Override
    protected BufferedImage applyTransformations(BufferedImage image, Data data) {
        boolean defaultColormapRange = getConfig().isDefaultColormap();
        if (image == null) {
            if (data == null) {
                return null;
            }
            image = data.image;
        }
        if ((image == null) || (!defaultColormapRange)) {
            Range cr = getColormapRange();
            ImageFormat format = ImageFormat.Gray8;
            if (data.depth == 3) {
                format = ImageFormat.Rgb24;
            } else if ((data.getType() == short.class) && getConfig().isDefaultColormap()) {
                format = ImageFormat.Gray16;
            }
            if ((defaultColormapRange) && ((data.getType() == short.class) || (data.getType() == byte.class))) {
                image = Utils.newImage(data.array, new ImageDescriptor(format, data.width, data.height));
            } else {
                byte[] array = data.translateToByteArray(cr);
                image = Utils.newImage(array, new ImageDescriptor(format, data.width, data.height));
            }
        }

        image = super.applyTransformations(image, data);

        if (((getConfig().colormap != Colormap.Grayscale) && Utils.isGrayscale(image)) ||( getConfig().colormapLogarithmic)) {
            image = Utils.newImage(image, getConfig().colormap, getConfig().colormapLogarithmic);
        }
        return image;
    }
}
