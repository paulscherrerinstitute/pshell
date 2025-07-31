package ch.psi.pshell.imaging;

import ch.psi.pshell.utils.Range;
import java.awt.image.BufferedImage;

/**
 *
 */
public interface ColormapOrigin { 
    ColormapSourceConfig getConfig();
    Data getData();
    BufferedImage getOutput();
    void refresh();

    default Range getColormapRange() {
        if (getConfig().colormapAutomatic) {
            return null;
        }
        if (getConfig().isDefaultColormap()) {
            return new Range(0.0, 255.0);
        }
        return new Range(getConfig().colormapMin, getConfig().colormapMax);
    }
    
    default Range getCurrentColormapRange(){
        Range scale = getColormapRange();
        if (scale == null) {
            Data data = getData();
            scale = (data==null)? new Range(0.0, 255.0) : data.getProperties();
        }        
        return scale;
    }    

    
}
