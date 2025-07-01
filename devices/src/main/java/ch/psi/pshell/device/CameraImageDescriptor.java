package ch.psi.pshell.device;

import ch.psi.pshell.device.Camera.ColorMode;
import ch.psi.pshell.device.Camera.DataType;

/**
 * Entity class holding the attributes of the data provided by a Camera device.
 */
public class CameraImageDescriptor {    
    public final int width;
    public final int height;
    public final int stack;
    public final ColorMode colorMode;
    public final DataType dataType;
    
    public CameraImageDescriptor(DataType dataType, int width, int height){
        this(ColorMode.Mono, dataType, width, height, 1);
    }    

    public CameraImageDescriptor(ColorMode colorMode, DataType dataType, int width, int height){
        this(colorMode, dataType, width, height, 1);
    }    

    public CameraImageDescriptor(ColorMode colorMode, DataType dataType, int width, int height, int stack){
        this.width=width;
        this.height=height;
        this.stack=Math.max(stack,1);
        this.colorMode=colorMode;
        this.dataType=dataType;
    }    

    public MatrixCalibration calibration;

    public int getPixelSize() {
        return colorMode.getDepth() * dataType.getSize();
    }

    public int getDepth() {
        return colorMode.getDepth();
    }

    public int getPixels() {
        return width * height;
    }
    
    public int getStackSize() {
        return Math.max(stack,1);
    }    

    public int getTotalSize() {
        return getPixelSize() * getPixels() * getStackSize();
    }
    
    @Override
    public String toString() {
        return dataType + " [" + width + "x" + height + "]";
        
    }    
}
