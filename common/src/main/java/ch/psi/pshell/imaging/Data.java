package ch.psi.pshell.imaging;

import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.ArrayProperties;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Range;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.Serializable;
import java.lang.reflect.Array;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Redundant, not implemented with reflection for performance.
 */
public class Data implements Serializable {

    public final Object array;
    public final int length;
    public final int width;
    public final int height;
    public final int depth;
    public final boolean unsigned;
    public final boolean rgb;
    public final BufferedImage image;
    public final long timestamp;
    public final DataBuffer dataBuffer;

    public Data(Object array, int width, int height, Boolean unsigned) {
        this(array, width, height, unsigned, 1);
    }

    public Data(Object array, int width, int height, Boolean unsigned, int depth) {
        if ((array == null)
                || (!array.getClass().isArray())
                || (!array.getClass().getComponentType().isPrimitive())) {
            throw new IllegalArgumentException();
        }
        this.array = array;
        this.length = Array.getLength(array);
        this.width = width;
        this.height = height;
        if (unsigned == null) {
            Class type = array.getClass().getComponentType();
            this.unsigned = ((type == byte.class) || (type == short.class));
        } else {
            this.unsigned = unsigned;
        }
        this.rgb = false;
        this.depth = depth;
        this.timestamp = System.currentTimeMillis();
        this.image = null;
        this.dataBuffer = null;
    }

    public Data(Object array, int width, int height) {
        this(array, width, height, null);
    }

    public Data(Object array2d) {
        this(array2d, false);
    }

    public Data(Object array2d, boolean unsigned) {
        this(Convert.flatten(array2d), Array.getLength(Array.get(array2d, 0)), Array.getLength(array2d), unsigned);
    }
            
    public Data(DataBuffer buffer, int width, int height) {
        this.dataBuffer = buffer;
        this.array = Utils.getDataBufferArray(buffer);
        this.unsigned = Utils.isDataBufferUnsigned(buffer);
        this.length = Array.getLength(array);
        this.width = width;
        this.height = height;
        this.rgb = false;
        this.depth = 1;
        this.image = null;
        timestamp = System.currentTimeMillis();
    }
    public Data(BufferedImage image) {
        this(image, false);
    }
    
    public Data(BufferedImage image, boolean grayscale) {
        if (grayscale){
            image = Utils.grayscale(image);
        }
        this.dataBuffer = image.getRaster().getDataBuffer();
        this.array = Utils.getDataBufferArray(dataBuffer);
        this.unsigned = Utils.isDataBufferUnsigned(dataBuffer);
        this.length = Array.getLength(array);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.rgb = !Utils.isGrayscale(image);
        this.depth = image.getColorModel().getNumComponents();
        this.image = image;
        timestamp = System.currentTimeMillis();
    }
    
    public Data(Data data, Rectangle roi) {
        this(data, roi, false);
    }

    public Data(Data data, Rectangle roi, boolean transformed) {
        if (transformed) {
            roi = getInverseRect(roi);
        }
        array = Array.newInstance(data.array.getClass().getComponentType(), Array.getLength(data.array));
        length = Array.getLength(array);
        width = roi.width;
        height = roi.height;
        depth = data.depth;
        unsigned = data.unsigned;
        rgb = data.rgb;
        timestamp = data.timestamp;
        dataBuffer = null;
        for (int row = 0; row < roi.height; row++) {
            System.arraycopy(data.array, (row + roi.y) * data.width + roi.x, array, row * width, roi.width);
        }
        image = (data.image == null) ? null : data.image.getSubimage(roi.x, roi.y, roi.width, roi.height);
    }

    public Data(Data data) {
        this(data, new Rectangle(0, 0, data.width, data.height), false);
    }

    public Data(Class type, boolean unsigned, int width, int height) {
        this(Array.newInstance(type, width * height), width, height, unsigned, 1);
    }

    public Data(Data data, Class type, boolean unsigned) {
        this(type, unsigned, data.width, data.height);
        if (type == data.getType()) {
            System.arraycopy(data.array, 0, array, 0, length);
        } else {
            for (int i = 0; i < length; i++) {
                setElement(i, data.getElement(i));
            }
        }
    }
    
    Calibration calibration;

    public Data getRoi(Rectangle roi) {
        return new Data(this, roi, false);
    }

    public void clear() {
        for (int i = 0; i < length; i++) {
            setElement(i, 0);
        }
    }

    public void setCalibration(Calibration calibration) {
        this.calibration = calibration;
    }

    public Calibration getCalibration() {
        return calibration;
    }

    SourceConfig sourceConfig;

    public void setSourceConfig(SourceConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
        setCalibration((sourceConfig == null) ? null : sourceConfig.getCalibration());
    }

    public SourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public ArrayProperties getProperties() {
        int minIndex = -1;
        int maxIndex = -1;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        if (array instanceof byte[] data) {
            for (int i = 0; i < data.length; i++) {
                double item = unsigned ? Convert.toUnsigned(data[i]) : data[i];
                if (item < min) {
                    min = item;
                    minIndex = i;
                }
                if (item > max) {
                    max = item;
                    maxIndex = i;
                }
            }
        } else if (array instanceof short[] data) {
            for (int i = 0; i < data.length; i++) {
                double item = unsigned ? Convert.toUnsigned(data[i]) : data[i];
                if (item < min) {
                    min = item;
                    minIndex = i;
                }
                if (item > max) {
                    max = item;
                    maxIndex = i;
                }
            }
        } else if (array instanceof double[] data) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] < min) {
                    min = data[i];
                    minIndex = i;
                }
                if (data[i] > max) {
                    max = data[i];
                    maxIndex = i;
                }
            }
        } else if (array instanceof float[] data) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] < min) {
                    min = (double) data[i];
                    minIndex = i;
                }
                if (data[i] > max) {
                    max = (double) data[i];
                    maxIndex = i;
                }
            }
        } else if (array instanceof long[] data) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] < min) {
                    min = (double) data[i];
                    minIndex = i;
                }
                if (data[i] > max) {
                    max = (double) data[i];
                    maxIndex = i;
                }
            }
        } else if (array instanceof int[] data) {
            for (int i = 0; i < data.length; i++) {
                double item = unsigned ? Convert.toUnsigned(data[i]) : data[i];
                if (item < min) {
                    min = item;
                    minIndex = i;
                }
                if (item > max) {
                    max = item;
                    maxIndex = i;
                }
            }
        }
        return new ArrayProperties(min, max, minIndex, maxIndex);
    }

    public byte[] translateToByteArray(Range range) {
        if (range == null) {
            range = getProperties();
        }
        double rangeExtent = range.getExtent();
        double scale = (rangeExtent > 0) ? 255.0 / rangeExtent : 1.0;
        byte[] ret = new byte[length];
        int val;
        if (array instanceof byte[] data) {
            for (int i = 0; i < length; i++) {
                val = (int) ((unsigned) ? (scale * (Convert.toUnsigned(data[i]) - range.min)) : (scale * (data[i] - range.min)));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof short[] data) {
            for (int i = 0; i < length; i++) {
                val = (int) ((unsigned) ? (scale * (Convert.toUnsigned(data[i]) - range.min)) : (scale * (data[i] - range.min)));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof double[] data) {
            for (int i = 0; i < length; i++) {
                val = (int) (scale * (data[i] - range.min));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof float[] data) {
            for (int i = 0; i < length; i++) {
                val = (int) (scale * (data[i] - range.min));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof long[] data) {
            for (int i = 0; i < length; i++) {
                val = (int) (scale * (data[i] - range.min));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof int[] data) {
            for (int i = 0; i < length; i++) {
                val = (int) ((unsigned) ? (scale * (Convert.toUnsigned(data[i]) - range.min)) : (scale * (data[i] - range.min)));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        }
        return ret;
    }

    public Number getElement(int index) {
        if ((index < 0) || (index > length)) {
            return null;
        } else if (array instanceof byte[] data) {
            if (unsigned) {
                return getUnsigned(data[index]);
            }
            return data[index];
        } else if (array instanceof double[] data) {
            return data[index];
        } else if (array instanceof short[] data) {
            if (unsigned) {
                return getUnsigned(data[index]);
            }
            return data[index];
        } else if (array instanceof int[] data) {
            if (unsigned) {
                return getUnsigned(data[index]);
            }
            return data[index];
        } else if (array instanceof long[] data) {
            return data[index];
        } else if (array instanceof float[] data) {
            return data[index];
        }
        return null;
    }

    public void setElement(int row, int col, Number number) {
        int index = row * width + col;
        setElement(index, number);
    }
    
    public void setElement(int index, Number number) {
        if ((index < 0) || (index > length)) {
            throw new IllegalArgumentException();
        } else if (array instanceof byte[] data) {
            data[index] = number.byteValue();
        } else if (array instanceof double[] data) {
            data[index] = number.doubleValue();
        } else if (array instanceof short[] data) {
            data[index] = number.shortValue();
        } else if (array instanceof int[] data) {
            data[index]= number.intValue();
        } else if (array instanceof long[] data) {
            data[index] = number.longValue();
        } else if (array instanceof float[] data) {
            data[index] = number.floatValue();
        }
    }

    public double getX(int col) {
        return getX(col, calibration);
    }

    public double getX(int col, Calibration calibration) {
        if (calibration == null) {
            return col;
        }
        return calibration.convertToAbsoluteX(col);
    }

    public String getXStr(int col) {
        return getXStr(col, calibration);
    }

    public String getXStr(int col, Calibration calibration) {
        double x = getX(col, calibration);
        final int precision = 6;
        if (Math.abs(x - (long) x) < Math.pow(10.0, -precision)) {
            return String.format("%d", (long) x);
        } else {
            String ret = String.format("%s", Convert.roundDouble(x, precision));
            return (ret.length() > 8) ? String.format("%." + precision + "G", x) : ret;
        }
    }

    public double getY(int row) {
        return getY(row, calibration);
    }

    public double getY(int row, Calibration calibration) {
        if (calibration == null) {
            return row;
        }
        return calibration.convertToAbsoluteY(row);
    }

    public String getYStr(int row) {
        return getYStr(row, calibration);
    }

    public String getYStr(int row, Calibration calibration) {
        double y = getY(row, calibration);
        final int precision = 6;
        if (Math.abs(y - (long) y) < Math.pow(10.0, -precision)) {
            return String.format("%d", (long) y);
        } else {
            String ret = String.format("%s", Convert.roundDouble(y, precision));
            return (ret.length() > 8) ? String.format("%." + precision + "G", y) : ret;
        }
    }

    Number getUnsigned(Number value) {
        if (value instanceof Integer data) {
            value = Convert.toUnsigned((int) data);
        } else if (value instanceof Short data) {
            value = Convert.toUnsigned((short) data);
        } else if (value instanceof Byte data) {
            value = Convert.toUnsigned((byte) data);
        }
        return value;
    }

    public int getLength() {
        return length;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Class getType() {
        if (array == null) {
            return null;
        }
        return array.getClass().getComponentType();
    }

    public BufferedImage getSourceImage() {
        return image;
    }
    
    public DataBuffer getDataBuffer() {
        if (dataBuffer != null){
            return dataBuffer;
        }                
        if (array instanceof byte[] data) {
            return new DataBufferByte(data, data.length);
        } else if (array instanceof short[] data) {
            if (unsigned) {
                return new DataBufferUShort(data, data.length);
            } else {
                return new DataBufferShort(data, data.length);
            }
        } else if (array instanceof int[] data) {
            return new DataBufferInt(data, data.length);
        } else if (array instanceof double[] data) {
            return new DataBufferDouble(data, data.length);
        } else if (array instanceof float[] data) {
            return new DataBufferFloat(data, data.length);
        } else if (array instanceof long[] data) {
        }
        return null;          
    }    

    public boolean isRgb() {
        return rgb;
    }

    public Object getMatrix() {
        return Convert.reshape(array, height, width);
    }

    public Object getArray() {
        return array;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public Object getTransformedMatrix() {
        if (sourceConfig == null) {
            return getMatrix();
        }
        return getRectSelection(null, true);
    }

    //Selection
    public Object getElement(Point p) {
        return getElement(p, false);
    }
    
    public Object getElement(Point p, boolean transformed) {
        return getElement(p.y, p.x, transformed);
    }
    
    public Object getElement(int row, int col) {
        return getElement(row, col, false);
    }

    public Object getElement(int row, int col, boolean transformed) {
        if (transformed) {
            Point p = getInversePoint(new Point(col, row));
            row = p.y;
            col = p.x;
        }

        if ((row < 0) || (row >= height)) {
            return null;
        }
        if ((col < 0) || (col >= width)) {
            return null;
        }
        if (rgb) {
            return new Color(image.getRGB(col, row));
        } else if (depth == 3) {
            int index = (row * width + col) * depth;
            return new Color(Convert.toUnsigned(getElement(index).byteValue()),
                    Convert.toUnsigned(getElement(index + 1).byteValue()),
                    Convert.toUnsigned(getElement(index + 2).byteValue()));
        }
        int index = row * width + col;
        return getElement(index);
    }
    
    public Object getElementDbl(Point p) {
        return getElementDbl(p, false);
    }    

    public Double getElementDbl(Point p, boolean transformed) {
        return getElementDbl(p.y, p.x, transformed);
    }

    public Object getElementDbl(int row, int col) {
        return getElementDbl(row, col, false);
    }
    
    public Double getElementDbl(int row, int col, boolean transformed) {
        Object val = getElement(row, col, transformed);
        if (val == null) {
            return Double.NaN;
        }
        if (val instanceof Color color) {
            return (double) SwingUtils.getPerceivedLuminance(color);
        }
        return ((Number) val).doubleValue();
    }

    public String getElementStr(Point p) {
        return getElementStr(p, false);
    }
        
    public String getElementStr(Point p, boolean transformed) {
        return getElementStr(p.y, p.x, transformed);
    }

    public String getElementStr(int row, int col) {
        return getElementStr(row, col, false);
    }
    
    public String getElementStr(int row, int col, boolean transformed) {
        Object e = getElement(row, col, transformed);
        if (e == null) {
            return "";
        }
        if (e instanceof Color c) {
            return SwingUtils.getPerceivedLuminance(c) + " (" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")";
        } else if (isUnsigned()) {
            e = getUnsigned((Number) e);
        }
        if ((e instanceof Double) || (e instanceof Float)) {
            //e = Convert.roundDouble(((Number) e).doubleValue(), 8);
            e = String.format("%.8G", e);  //Set 8 significant digits
        }
        return String.valueOf(e);
    }

    public double[][] getRectSelection(Rectangle rect) {
        return getRectSelection(rect, false);
    }
    
    public double[][] getRectSelection(Rectangle rect, boolean transformed) {
        if (rect == null) {
            rect = new Rectangle(getSize(transformed));
        }
        return getRectSelection(rect.x, rect.y, rect.width, rect.height, transformed);
    }
    
    
    public double[][] getRectSelection(int x, int y, int width, int height) {
        return getRectSelection(x, y, width, height, false);
    }

    public double[][] getRectSelection(int x, int y, int width, int height, boolean transformed) {
        double[][] ret = new double[height][width];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                ret[j][i] = getElementDbl(j + y, i + x, transformed);
            }
        }
        return ret;
    }

    public double[] getLineSelection(Point p1, Point p2) {
        return getLineSelection(p1, p2, false);
    }
    
    public double[] getLineSelection(Point p1, Point p2, boolean transformed) {
        return getLineSelection(p1.x, p1.y, p2.x, p2.y, transformed);
    }

    public double[] getLineSelection(int x1, int y1, int x2, int y2) {
        return getLineSelection(x1, y1, x2, y2, false);
    }
    
    public double[] getLineSelection(int x1, int y1, int x2, int y2, boolean transformed) {
        int length = (int) Math.hypot((x2 - x1), (y2 - y1));
        double[] ret = new double[length];
        double a = ((double) x2 - x1) / length;
        double b = ((double) y2 - y1) / length;
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getElementDbl(y1 + (int) (i * b + 0.5), x1 + (int) (i * a + 0.5), transformed);
        }
        return ret;
    }

    public double[] getRowSelection(int row) {
        return getRowSelection(row, false);
    }
    
    public double[] getRowSelection(int row, boolean transformed) {
        double[] ret = new double[getSize(true).width];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getElementDbl(row, i, transformed);
        }
        return ret;
    }

    public double[] getRowSelectionX() {
        return getRowSelectionX(false);
    }
    
    public double[] getRowSelectionX(boolean transformed) {
        double[] ret = new double[getSize(true).width];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getX(i);
        }
        return ret;
    }
    
    public double[] getColSelection(int col) {
        return getColSelection(col, false);
    }

    public double[] getColSelection(int col, boolean transformed) {
        double[] ret = new double[getSize(true).height];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getElementDbl(i, col, transformed);
        }
        return ret;
    }

    public double[] getColSelectionX() {
        return getColSelectionX(false);
    }
    
    public double[] getColSelectionX(boolean transformed) {
        double[] ret = new double[getSize(true).height];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getY(i);
        }
        return ret;
    }

    //Transformation
    public Dimension getSize() {
        return getSize(false);
    }
     
    public Dimension getSize(boolean transformed) {
        double width = getWidth();
        double height = getHeight();

        if ((transformed) && (sourceConfig != null)) {
            if (sourceConfig.transpose) {
                double ow = width;
                double oh = height;
                width = oh;
                height = ow;
            }
            if (sourceConfig.rotation != 0) {
                if (!sourceConfig.rotationCrop) {
                    double ow = width;
                    double oh = height;
                    double rotation = sourceConfig.rotation * Math.PI / 180.0;
                    width = (int) (Math.abs(Math.cos(rotation) * ow) + Math.abs(Math.sin(rotation) * oh));
                    height = (int) (Math.abs(Math.sin(rotation) * ow) + Math.abs(Math.cos(rotation) * oh));
                }
            }
            if (sourceConfig.scale != 1.0) {
                width *= sourceConfig.scale;
                height *= sourceConfig.scale;
            }
            if (sourceConfig.roiWidth >= 0) {
                width = Math.min(width, sourceConfig.roiWidth);
                height = Math.min(height, sourceConfig.roiHeight);
            }
        }
        return new Dimension((int) width, (int) height);
    }

    public Rectangle getTransformedRect(Rectangle rect) {
        Point p1 = getTransformedPoint(rect.getLocation());
        Point p2 = getTransformedPoint(new Point(rect.x + rect.width, rect.y + rect.height));
        return new Rectangle(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.abs(p2.x - p1.x), Math.abs(p2.y - p1.y));
    }

    public Point getTransformedPoint(Point p) {
        if (sourceConfig == null) {
            return p;
        }

        double x = p.x;
        double y = p.y;
        double height = getHeight();
        double width = getWidth();

        if (sourceConfig.transpose) {
            double ow = width;
            double oh = height;
            double ox = x;
            double oy = y;
            width = oh;
            height = ow;
            x = oy;
            y = ox;
        }
        if (sourceConfig.flipHorizontally) {
            x = width - x - 1;
        }
        if (sourceConfig.flipVertically) {
            y = height - y - 1;
        }
        if (sourceConfig.rotation != 0) {
            double rotation = sourceConfig.rotation * Math.PI / 180.0;
            double rw = (sourceConfig.rotationCrop) ? width : (Math.abs(Math.cos(rotation) * width) + Math.abs(Math.sin(rotation) * height));
            double rh = (sourceConfig.rotationCrop) ? height : (Math.abs(Math.sin(rotation) * width) + Math.abs(Math.cos(rotation) * height));
            double ox = x - (width / 2);
            double oy = y - (height / 2);
            x = ox * Math.cos(rotation) - oy * Math.sin(rotation) + rw / 2;
            y = oy * Math.cos(rotation) + ox * Math.sin(rotation) + rh / 2;
        }

        if (sourceConfig.scale != 1.0) {
            x *= sourceConfig.scale;
            y *= sourceConfig.scale;
        }

        if (sourceConfig.roiY > 0) {
            y -= sourceConfig.roiY;
        }

        if (sourceConfig.roiX > 0) {
            x -= sourceConfig.roiX;
        }

        return new Point((int) x, (int) y);
    }

    public Rectangle getInverseRect(Rectangle rect) {
        Point p1 = getInversePoint(rect.getLocation());
        Point p2 = getInversePoint(new Point(rect.x + rect.width, rect.y + rect.height));
        return new Rectangle(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.abs(p2.x - p1.x), Math.abs(p2.y - p1.y));
    }

    public Point getInversePoint(Point p) {
        if (sourceConfig == null) {
            return p;
        }
        double x = p.x;
        double y = p.y;
        double height = sourceConfig.transpose ? getWidth() : getHeight();
        double width = sourceConfig.transpose ? getHeight() : getWidth();

        if (sourceConfig.roiY > 0) {
            y += sourceConfig.roiY;
        }

        if (sourceConfig.roiX > 0) {
            x += sourceConfig.roiX;
        }

        if (sourceConfig.scale != 1.0) {
            x /= sourceConfig.scale;
            y /= sourceConfig.scale;
        }
        if (sourceConfig.rotation != 0) {
            double rotation = -sourceConfig.rotation * Math.PI / 180.0;
            double rw = (sourceConfig.rotationCrop) ? width : (Math.abs(Math.cos(rotation) * width) + Math.abs(Math.sin(rotation) * height));
            double rh = (sourceConfig.rotationCrop) ? height : (Math.abs(Math.sin(rotation) * width) + Math.abs(Math.cos(rotation) * height));
            double ox = x - (rw / 2);
            double oy = y - (rh / 2);
            x = ox * Math.cos(rotation) - oy * Math.sin(rotation) + width / 2;
            y = oy * Math.cos(rotation) + ox * Math.sin(rotation) + height / 2;
        }

        if (sourceConfig.flipHorizontally) {
            x = width - x - 1;
        }
        if (sourceConfig.flipVertically) {
            y = height - y - 1;
        }
        if (sourceConfig.transpose) {
            double ox = x;
            double oy = y;
            x = oy;
            y = ox;
        }
        return new Point((int) x, (int) y);
    }

    //Operations
    public double getGradientVariance() {
        return getGradientVariance(false);
    }
    
    public double getGradientVariance(Rectangle roi) {
        return getGradientVariance(false, roi);
    }
    
    public double getGradientVariance(boolean transformed) {
        return getGradientVariance(transformed, null);
    }
    
    public double getGradientVariance(boolean transformed, Rectangle roi) {
        if (height <= 0) {
            return Double.NaN;
        }
        double ret = 0.0;

        if (roi != null) {
            double[][] data = getRectSelection(roi, transformed);
            for (int i = 0; i < data.length; i++) {
                DescriptiveStatistics stats = new DescriptiveStatistics(Arr.abs(Arr.gradient(data[i])));
                ret += stats.getVariance();
            }
            return ret / data.length;
        } else {
            for (int i = 0; i < height; i++) {
                double[] row = getRowSelection(i, transformed);
                DescriptiveStatistics stats = new DescriptiveStatistics(Arr.abs(Arr.gradient(row)));
                ret += stats.getVariance();
            }
            return ret / height;
        }
    }
    
    public BufferedImage toBufferedImage() {
        return toBufferedImage(false);
    }

    public BufferedImage toBufferedImage(boolean transformed) {
        BufferedImage ret = Utils.newImage((getType() == byte.class) ? array : translateToByteArray(null), new ImageDescriptor(ImageFormat.Gray8, width, height));
        if (transformed) {
            ret=applyTransformatiuons(ret);
        }
        return ret;
    }
    
    public BufferedImage toBufferedImage(float scaleMin, float scaleMax, Colormap colormap, boolean logarithmic) {
        return toBufferedImage(scaleMin, scaleMax, colormap, logarithmic, false);
    }

    public BufferedImage toBufferedImage(float scaleMin, float scaleMax, Colormap colormap, boolean logarithmic, boolean transformed) {
        BufferedImage ret = Utils.newImage(this, scaleMin, scaleMax, colormap, logarithmic);
        if (transformed) {
            ret=applyTransformatiuons(ret);
        }
        return ret;
    }
    
    
    public BufferedImage applyTransformatiuons(BufferedImage img){
        if ((img != null) && (sourceConfig != null)) {
            if (sourceConfig.transpose) {
                img = Utils.transpose(img);
            }
            if ((sourceConfig.flipVertically) || (sourceConfig.flipHorizontally)) {
                img = Utils.flip(img, sourceConfig.flipVertically, sourceConfig.flipHorizontally);
            }
            if (sourceConfig.rotation != 0) {
                img = Utils.rotate(img, sourceConfig.rotation, sourceConfig.rotationCrop);
            }
            if (sourceConfig.scale != 1.0) {
                img = Utils.scale(img, sourceConfig.scale);
            }
            if ((sourceConfig.roiX > 0) || (sourceConfig.roiY > 0) || (sourceConfig.roiWidth >= 0) || (sourceConfig.roiHeight >= 0)) {
                img = img.getSubimage(sourceConfig.roiX, sourceConfig.roiY, sourceConfig.roiWidth, sourceConfig.roiHeight);
            }
        }
        return img;
    }
    

    public double[] integrateVertically() {
          return integrateVertically(false);
    }
    
    public double[] integrateVertically(boolean transformed) {
        //TODO: integration is slow mainly because the double loop calling getInversePoint. 
        //Should find a faster solution.
        Dimension size = getSize(transformed);
        double[] ret = new double[size.width];
        for (int x = 0; x < size.width; x++) {
            double aux = 0;
            for (int y = 0; y < size.height; y++) {
                Double val = getElementDbl(y, x, transformed);
                if (!Double.isNaN(val)) {
                    aux += val;
                }
            }
            ret[x] = aux;
        }
        return ret;
    }

    public double[] integrateHorizontally() {
          return integrateHorizontally(false);
    }
    
    public double[] integrateHorizontally(boolean transformed) {
        Dimension size = getSize(transformed);
        double[] ret = new double[size.height];
        for (int y = 0; y < size.height; y++) {
            double aux = 0;
            for (int x = 0; x < size.width; x++) {
                Double val = getElementDbl(y, x, transformed);
                if (!Double.isNaN(val)) {
                    aux += val;
                }
            }
            ret[y] = aux;
        }
        return ret;
    }
    
    public double integrate() {
        return integrate(false);
    }
    
    public double integrate(boolean transformed) {
        double ret = 0;
        double[] arr = integrateHorizontally(transformed);
        for (double d : arr) {
            ret += d;
        }
        return ret;
    }    

    void checkValidOpertator(Data op) {
        if ((width != op.width) || (height != op.height) ||(getType() != op.getType()) || (unsigned != op.unsigned)) {
            throw new IllegalArgumentException();
        }
    }

    public void sub(Data op) {
        checkValidOpertator(op);
        if (array instanceof byte[] data) {
            int length = Math.min(this.length, op.length);
            byte[] opdata = (byte[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.max(Convert.toUnsigned(data[i]) - Convert.toUnsigned(opdata[i]), 0);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.max(data[i] - opdata[i], -0x80);
                }
            }
        } else if (array instanceof short[] data) {
            short[] opdata = (short[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(Convert.toUnsigned(data[i]) - Convert.toUnsigned(opdata[i]), 0);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(data[i] - opdata[i], -0x8000);
                }
            }
        } else if (array instanceof double[] data) {
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] - opdata[i]);
            }
        } else if (array instanceof float[] data) {
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] - opdata[i]);
            }
        } else if (array instanceof long[] data) {
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] - opdata[i]);
            }
        } else if (array instanceof int[] data) {
            int[] opdata = (int[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.max(Convert.toUnsigned(data[i]) - Convert.toUnsigned(opdata[i]), 0);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.max(data[i] - opdata[i], -0x80000000);
                }
            }
        }
    }

    public void sum(Data op) {
        checkValidOpertator(op);
        int length = Math.min(this.length, op.length);
        if (array instanceof byte[] data) {
            byte[] opdata = (byte[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(Convert.toUnsigned(data[i]) + Convert.toUnsigned(opdata[i]), 0xFF);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(data[i] + opdata[i], 0x7F);
                }
            }
        } else if (array instanceof short[] data) {
            short[] opdata = (short[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.min(Convert.toUnsigned(data[i]) + Convert.toUnsigned(opdata[i]), 0xFFFF);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.min(data[i] + opdata[i], 0x7FFF);
                }
            }
        } else if (array instanceof double[] data) {
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] + opdata[i]);
            }
        } else if (array instanceof float[] data) {
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] + opdata[i]);
            }
        } else if (array instanceof long[] data) {
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] + opdata[i]);
            }
        } else if (array instanceof int[] data) {
            int[] opdata = (int[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.min(Convert.toUnsigned(data[i]) + Convert.toUnsigned(opdata[i]), 0xFFFFFFFF);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.min(data[i] + opdata[i], 0x7FFFFFFF);
                }
            }
        }
    }

    public void div(Data op) {
        checkValidOpertator(op);
        int length = Math.min(this.length, op.length);
        if (array instanceof byte[] data) {
            byte[] opdata = (byte[]) (op.array);
            if (unsigned) {
                byte zeroDix=Byte.MAX_VALUE;
                for (int i = 0; i < length; i++) {
                    short d = Convert.toUnsigned(opdata[i]);
                    data[i] = (d==0) ? (byte)0xFF :(byte) Math.max(Math.min(Convert.toUnsigned(data[i]) / d, 0xFF), 0);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (opdata[i] == 0) ? Byte.MAX_VALUE :  (byte) Math.max(Math.min(data[i] / opdata[i], 0x7F), -0x80);
                }
            }
        } else if (array instanceof short[]data) {
            short[] opdata = (short[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    int d = Convert.toUnsigned(opdata[i]);
                    data[i] = (d==0) ? (short)0xFFFF : (short) Math.max(Math.min(Convert.toUnsigned(data[i]) / d, 0xFFFF), 0);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (opdata[i]==0) ? Short.MAX_VALUE : (short) Math.max(Math.min(data[i] / opdata[i], 0x7FFF), -0x8000);
                }
            }
        } else if (array instanceof double[] data) {
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (opdata[i]==0) ? Double.NaN : (double) (data[i] / opdata[i]);
            }
        } else if (array instanceof float[] data) {
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (opdata[i]==0) ? Float.NaN :(float) (data[i] / opdata[i]);
            }
        } else if (array instanceof long[] data) {
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (opdata[i]==0) ? Long.MAX_VALUE :(long) (data[i] / opdata[i]);
            }
        } else if (array instanceof int[] data) {
            int[] opdata = (int[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    Long d = Convert.toUnsigned(opdata[i]);
                    data[i] = (d==0) ? (int)0xFFFFFFFF : (int) Math.max(Math.min(Convert.toUnsigned(data[i]) / d, 0xFFFFFFFF), 0);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (opdata[i]==0) ? Integer.MAX_VALUE : (int) Math.max(Math.min(data[i] / opdata[i], 0x7FFFFFFF), -0x80000000);
                }
            }
        }
    }

    public void mult(Data op) {
        checkValidOpertator(op);
        int length = Math.min(this.length, op.length);
        if (array instanceof byte[]data) {
            byte[] opdata = (byte[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(Convert.toUnsigned(data[i]) * Convert.toUnsigned(opdata[i]), 0xFF);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(data[i] * opdata[i], 0x7F);
                }
            }
        } else if (array instanceof short[] data) {
            short[] opdata = (short[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(Convert.toUnsigned(data[i]) * Convert.toUnsigned(opdata[i]), 0xFFFF);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(data[i] * opdata[i], 0x7FFF);
                }
            }
        } else if (array instanceof double[] data) {
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] * opdata[i]);
            }
        } else if (array instanceof float[] data) {
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] * opdata[i]);
            }
        } else if (array instanceof long[] data) {
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] * opdata[i]);
            }
        } else if (array instanceof int[] data) {
            int[] opdata = (int[]) (op.array);
            if (unsigned) {
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.max(Convert.toUnsigned(data[i]) * Convert.toUnsigned(opdata[i]), 0xFFFFFFFF);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.max(data[i] * opdata[i], 0x7FFFFFFF);
                }
            }
        }
    }

    public void min(Data op) {
        checkValidOpertator(op);
        int length = Math.min(this.length, op.length);
        if (array instanceof byte[] data) {
            byte[] opdata = (byte[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof short[] data) {
            short[] opdata = (short[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof double[] data) {
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof float[] data) {
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof long[] data) {
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof int[] data) {
            int[] opdata = (int[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        }
    }

    public void max(Data op) {
        checkValidOpertator(op);
        int length = Math.min(this.length, op.length);
        if (array instanceof byte[] data) {
            byte[] opdata = (byte[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof short[] data) {
            short[] opdata = (short[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof double[] data) {
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof float[] data) {
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof long[] data) {
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof int[] data) {
            int[] opdata = (int[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        }
    }

    public void sub(Number op) {
        if (array instanceof byte[] data) {
            if (unsigned) {
                short val = Convert.toUnsigned(op.byteValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.max(Convert.toUnsigned(data[i]) - val, 0);
                }
            } else {
                byte val = op.byteValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.max(data[i] - val, -0x80);
                }
            }
        } else if (array instanceof short[] data) {
            if (unsigned) {
                int val = Convert.toUnsigned(op.shortValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(Convert.toUnsigned(data[i]) - val, 0);
                }
            } else {
                short val = op.shortValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(data[i] - val, -0x8000);
                }
            }
        } else if (array instanceof double[] data) {
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] - val);
            }
        } else if (array instanceof float[] data) {
            double val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] - val);
            }
        } else if (array instanceof long[] data) {
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] - val);
            }
        } else if (array instanceof int[] data) {
            if (unsigned) {
                long val = Convert.toUnsigned(op.intValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.max(Convert.toUnsigned(data[i]) - val, 0);
                }
            } else {
                int val = op.intValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.max(data[i] - val, -0x80000000);
                }
            }
        }
    }

    public void sum(Number op) {
        if (array instanceof byte[] data) {
            if (unsigned) {
                short val = Convert.toUnsigned(op.byteValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(Convert.toUnsigned(data[i]) + val, 0xFF);
                }
            } else {
                byte val = op.byteValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(data[i] + val, 0x7F);
                }
            }
        } else if (array instanceof short[] data) {
            if (unsigned) {
                int val = Convert.toUnsigned(op.shortValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(Convert.toUnsigned(data[i]) + val, 0xFFFF);
                }
            } else {
                int val = op.shortValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(data[i] + val, 0x7FFF);
                }
            }
        } else if (array instanceof double[] data) {
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] + val);
            }
        } else if (array instanceof float[] data) {
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] + val);
            }
        } else if (array instanceof long[] data) {
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] + val);
            }
        } else if (array instanceof int[] data) {
            if (unsigned) {
                long val = Convert.toUnsigned(op.intValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.min(Convert.toUnsigned(data[i]) + val, 0xFFFFFFFF);
                }
            } else {
                int val = op.intValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.min(data[i] + val, 0x7FFFFFFF);
                }
            }
        }
    }

    public void div(Number op) {
        if (array instanceof byte[] data) {
            if (unsigned) {
                short val = Convert.toUnsigned(op.byteValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (val==0) ? (byte)0xFF : (byte) Math.max(Math.min(Convert.toUnsigned(data[i]) / val, 0xFF), 0);
                }
            } else {
                byte val = op.byteValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (val==0) ? Byte.MAX_VALUE : (byte) Math.max(Math.min(data[i] / val, 0x7F), -0x80);
                }
            }
        } else if (array instanceof short[] data) {
            if (unsigned) {
                int val = Convert.toUnsigned(op.shortValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (val==0) ? (short)0xFFFF :(short) Math.max(Math.min(Convert.toUnsigned(data[i]) / val, 0xFFFF), 0);
                }
            } else {
                short val = op.shortValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (val==0) ? Short.MAX_VALUE :(short) Math.max(Math.min(data[i] / val, 0x7FFF), -0x8000);
                }
            }
        } else if (array instanceof double[] data) {
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (val==0) ? Double.NaN :(double) (data[i] / val);
            }
        } else if (array instanceof float[] data) {
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (val==0) ? Float.NaN :(float) (data[i] / val);
            }
        } else if (array instanceof long[] data) {
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (val==0) ? Long.MAX_VALUE : (long) (data[i] / val);
            }
        } else if (array instanceof int[] data) {
            if (unsigned) {
                long val = Convert.toUnsigned(op.intValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (val==0) ? (int)0xFFFFFFFF :(int) Math.max(Math.min(Convert.toUnsigned(data[i]) / val, 0xFFFFFFFF), 0);
                }
            } else {
                int val = op.intValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (val==0) ? Integer.MAX_VALUE :(int) Math.max(Math.min(data[i] / val, 0x7FFFFFFF), -0x80000000);
                }
            }
        }
    }

    public void mult(Number op) {
        if (array instanceof byte[] data) {
            if (unsigned) {
                short val = Convert.toUnsigned(op.byteValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(Convert.toUnsigned(data[i]) * val, 0xFF);
                }
            } else {
                byte val = op.byteValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (byte) Math.min(data[i] * val, 0x7F);
                }
            }
        } else if (array instanceof short[] data) {
            if (unsigned) {
                int val = Convert.toUnsigned(op.shortValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(Convert.toUnsigned(data[i]) * val, 0xFFFF);
                }
            } else {
                int val = op.shortValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (short) Math.max(data[i] * val, 0x7FFF);
                }
            }
        } else if (array instanceof double[] data) {
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] * val);
            }
        } else if (array instanceof float[] data) {
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] * val);
            }
        } else if (array instanceof long[] data) {
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] * val);
            }
        } else if (array instanceof int[] data) {
            if (unsigned) {
                long val = Convert.toUnsigned(op.intValue());
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.min(Convert.toUnsigned(data[i]) * val, 0xFFFFFFFF);
                }
            } else {
                int val = op.intValue();
                for (int i = 0; i < length; i++) {
                    data[i] = (int) Math.min(data[i] * val, 0x7FFFFFFF);
                }
            }
        }
    }

    public void min(Number op) {
        if (array instanceof byte[] data) {
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.min(data[i], op.byteValue());
            }
        } else if (array instanceof short[] data) {
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.min(data[i], op.shortValue());
            }
        } else if (array instanceof double[] data) {
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        } else if (array instanceof float[] data) {
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        } else if (array instanceof long[] data) {
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        } else if (array instanceof int[] data) {
            int val = op.intValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        }
    }

    public void max(Number op) {
        if (array instanceof byte[] data) {
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.max(data[i], op.byteValue());
            }
        } else if (array instanceof short[] data) {
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.max(data[i], op.shortValue());
            }
        } else if (array instanceof double[] data) {
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        } else if (array instanceof float[] data) {
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        } else if (array instanceof long[] data) {
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        } else if (array instanceof int[] data) {
                int val = op.intValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        }
    }
    
    public void threshold(Number threshold, boolean less, Number replacement) {
        if (array instanceof byte[] data) {
            byte val = threshold.byteValue();
            byte rep = (replacement==null) ? 0 : replacement.byteValue();
            if (less){
                for (int i = 0; i < length; i++) {
                    if ((data[i] < val)){
                        data[i] = rep;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if ((data[i] > val)){
                        data[i] = rep;
                    }
                } 
            }
        } else if (array instanceof short[] data) {
            short val = threshold.shortValue();
            short rep = (replacement==null) ? 0 : replacement.shortValue();
            if (less){
                for (int i = 0; i < length; i++) {
                    if ((data[i] < val)){
                        data[i] = rep;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if ((data[i] > val)){
                        data[i] = rep;
                    }
                } 
            }
        } else if (array instanceof double[] data) {
            double rep = (replacement==null) ? Double.NaN : replacement.doubleValue();
            double val = threshold.doubleValue();
            if (less){
                for (int i = 0; i < length; i++) {
                    if ((data[i] < val)){
                        data[i] = rep;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if ((data[i] > val)){
                        data[i] = rep;
                    }
                } 
            }
        } else if (array instanceof float[] data) {
            float rep = (replacement==null) ? Float.NaN : replacement.floatValue();
            float val = threshold.floatValue();
            if (less){
                for (int i = 0; i < length; i++) {
                    if ((data[i] < val)){
                        data[i] = rep;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if ((data[i] > val)){
                        data[i] = rep;
                    }
                } 
            }
        } else if (array instanceof long[] data) {
            long rep = (replacement==null) ? 0 : replacement.longValue();
            long val = threshold.longValue();
            if (less){
                for (int i = 0; i < length; i++) {
                    if ((data[i] < val)){
                        data[i] = rep;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if ((data[i] > val)){
                        data[i] = rep;
                    }
                } 
            }
        } else if (array instanceof int[] data) {
            int rep = (replacement==null) ? 0 : replacement.intValue();
            int val = threshold.intValue();
            if (less){
                for (int i = 0; i < length; i++) {
                    if ((data[i] < val)){
                        data[i] = rep;
                    }
                }
            } else {
                for (int i = 0; i < length; i++) {
                    if ((data[i] > val)){
                        data[i] = rep;
                    }
                } 
            }
        }
    }
    
    
    public Data copy(){
        return new Data(this);
    }

    public Data copy(Class type, boolean unsigned){
        return new Data(this, type, unsigned);
    }
    
    public Data toGrayscale(){
        if (isRgb()){
            return new Data(image, true);
        }
        return this;
    }
    
}
