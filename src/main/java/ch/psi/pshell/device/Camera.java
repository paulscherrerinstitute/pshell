package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.utils.Convert;
import java.io.IOException;

/**
 * Interface for camera objects: 2D sensors with optional features of binning, roi, gain, exposure,
 * iterations, trigger mode and more.
 */
public interface Camera extends Device, Startable {

    /**
     * Typically the camera data is read as an 1D array Camera provide 1D data to CameraSource (and
     * not getDataMatrix) because conversions to BufferedImage is made with on flattened arrays.
     */
    ReadonlyRegisterArray getDataArray();

    /**
     * This matrix is created based on the array provided by getData.
     */
    ReadableMatrix getDataMatrix();

    public CameraImageDescriptor readImageDescriptor() throws IOException, InterruptedException;     //triggers data change events

    public String getInfo() throws IOException, InterruptedException;

    public int[] getSensorSize() throws IOException, InterruptedException;

    public void setBinningX(int value) throws IOException, InterruptedException;

    public int getBinningX() throws IOException, InterruptedException;

    public void setBinningY(int value) throws IOException, InterruptedException;

    public int getBinningY() throws IOException, InterruptedException;

    default public void setROI(int[] roi) throws IOException, InterruptedException{
        setROI(roi[0], roi[1], roi[2], roi[3]);
    }
    
    public void setROI(int x, int y, int w, int h) throws IOException, InterruptedException;

    public int[] getROI() throws IOException, InterruptedException;

    public void setGain(double value) throws IOException, InterruptedException;

    public double getGain() throws IOException, InterruptedException;

    //TODO: DataType/Color mode must be neutral: they are Area Detector oriented
    //Bust include notions of byte order & alpha : BRG, RGB, ARGB...
    public enum DataType {

        Int8,
        UInt8,
        Int16,
        UInt16,
        Int24,
        UInt24,
        Int32,
        UInt32,
        Float32,
        Float64;

        public int getSize() {
            switch (this) {
                case Int8:
                case UInt8:
                    return 1;
                case Int16:
                case UInt16:
                    return 2;
                case Int24:
                case UInt24:
                    return 3;
                case Int32:
                case UInt32:
                case Float32:
                    return 4;
                case Float64:
                    return 8;
            }
            return 0;
        }

        public Class getArrayType() {
            switch (this) {
                case Int8:
                case UInt8:
                    return byte[].class;
                case Int16:
                case UInt16:
                    return short[].class;
                case Int24:
                case UInt24:
                case Int32:
                case UInt32:
                    return int[].class;
                case Float32:
                    return float[].class;
                case Float64:
                    return double[].class;
            }
            return null;
        }
        
        public Class getElementType() {
            switch (this) {
                case Int8:
                case UInt8:
                    return byte.class;
                case Int16:
                case UInt16:
                    return short.class;
                case Int24:
                case UInt24:
                case Int32:
                case UInt32:
                    return int.class;
                case Float32:
                    return float.class;
                case Float64:
                    return double.class;
            }
            return null;
        }        

        public boolean isUnsigned() {
            switch (this) {
                case UInt8:
                case UInt16:
                case UInt24:
                case UInt32:
                    return true;
            }
            return false;
        }
    }

    public void setDataType(DataType type) throws IOException, InterruptedException;

    public DataType getDataType() throws IOException, InterruptedException;

    public enum ColorMode {

        Mono,
        RGB1, //Interleave Pixel
        RGB2, //Interleave Row
        RGB3;   //Interleave Plane//Interleave Plane

        public int getDepth() {
            switch (this) {
                case Mono:
                    return 1;
                default:
                    return 3;
            }
        }

    };

    public void setColorMode(ColorMode mode) throws IOException, InterruptedException;

    public ColorMode getColorMode() throws IOException, InterruptedException;

    /**
     * Total image size in bytes
     */
    public int[] getImageSize() throws IOException, InterruptedException;

    public void setExposure(double value) throws IOException, InterruptedException;

    public double getExposure() throws IOException, InterruptedException;

    public void setAcquirePeriod(double value) throws IOException, InterruptedException;

    public double getAcquirePeriod() throws IOException, InterruptedException;

    public void setNumImages(int value) throws IOException, InterruptedException;

    public int getNumImages() throws IOException, InterruptedException;

    public void setIterations(int value) throws IOException, InterruptedException;

    public int getIterations() throws IOException, InterruptedException;

    public int getImagesComplete() throws IOException, InterruptedException;

    public enum GrabMode {

        Single,
        Multiple,
        Continuous,
        Average
    };

    public void setGrabMode(GrabMode value) throws IOException, InterruptedException;

    public GrabMode getGrabMode() throws IOException, InterruptedException;

    public enum TriggerMode {

        Free_Run,
        Fixed_Rate,
        Software,
        Internal,
        External,
        External_2,
        External_3,
        External_4;

        public boolean isExternal() {
            return (this == External) || (this == External_2) || (this == External_3) || (this == External_4);
        }
    };

    public void setTriggerMode(TriggerMode value) throws IOException, InterruptedException;

    public TriggerMode getTriggerMode() throws IOException, InterruptedException;

    /**
     * Generates a software trigger
     */
    public void trigger() throws IOException, InterruptedException;

    
    
    public default Object takeStack() throws Exception {    
        int[] size = getImageSize();
        int depth = (size.length>2) ? size[2] : 1; 
        Object array = getDataArray().take();
        return Convert.reshape(array, new int[]{depth,size[1],size[0]});
    }
}
