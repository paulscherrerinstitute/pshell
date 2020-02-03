package ch.psi.pshell.imaging;

import ch.psi.utils.Arr;
import ch.psi.utils.ArrayProperties;
import ch.psi.utils.Range;
import ch.psi.utils.Convert;
import ch.psi.utils.swing.SwingUtils;
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
import java.lang.reflect.Array;
import java.io.Serializable;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Redundant, not implemented with reflection for performance.
 */
public class Data implements Serializable {

    final Object array;
    final int length;
    final int width;
    final int height;
    final int depth;
    final boolean unsigned;
    final boolean rgb;
    final BufferedImage image;
    final long timestamp;

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
        timestamp = System.currentTimeMillis();
        image = null;
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

    public Data(BufferedImage image) {

        DataBuffer buf = image.getRaster().getDataBuffer();
        Object array = null;
        boolean unsigned = false;
        switch (buf.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                array = ((DataBufferByte) buf).getData();
                unsigned = true;
                break;
            case DataBuffer.TYPE_DOUBLE:
                array = ((DataBufferDouble) buf).getData();
                break;
            case DataBuffer.TYPE_FLOAT:
                array = ((DataBufferFloat) buf).getData();
                break;
            case DataBuffer.TYPE_INT:
                array = ((DataBufferInt) buf).getData();
                break;
            case DataBuffer.TYPE_SHORT:
                array = ((DataBufferShort) buf).getData();
                break;
            case DataBuffer.TYPE_USHORT:
                array = ((DataBufferUShort) buf).getData();
                unsigned = true;
                break;
        }

        this.array = array;
        this.length = Array.getLength(array);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.unsigned = unsigned;
        this.rgb = !Utils.isGrayscale(image);
        this.depth = image.getColorModel().getNumComponents();
        this.image = image;
        timestamp = System.currentTimeMillis();
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

        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
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
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
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
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
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
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
            for (int i = 0; i < length; i++) {
                val = (int) ((unsigned) ? (scale * (Convert.toUnsigned(data[i]) - range.min)) : (scale * (data[i] - range.min)));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
            for (int i = 0; i < length; i++) {
                val = (int) ((unsigned) ? (scale * (Convert.toUnsigned(data[i]) - range.min)) : (scale * (data[i] - range.min)));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            for (int i = 0; i < length; i++) {
                val = (int) (scale * (data[i] - range.min));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            for (int i = 0; i < length; i++) {
                val = (int) (scale * (data[i] - range.min));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            for (int i = 0; i < length; i++) {
                val = (int) (scale * (data[i] - range.min));
                ret[i] = (val > 255) ? (byte) 255 : ((val < 0) ? 0 : (byte) val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        } else if (array instanceof byte[]) {
            if (unsigned) {
                return getUnsigned(((byte[]) array)[index]);
            }
            return ((byte[]) array)[index];
        } else if (array instanceof double[]) {
            return ((double[]) array)[index];
        } else if (array instanceof short[]) {
            if (unsigned) {
                return getUnsigned(((short[]) array)[index]);
            }
            return ((short[]) array)[index];
        } else if (array instanceof int[]) {
            if (unsigned) {
                return getUnsigned(((int[]) array)[index]);
            }
            return ((int[]) array)[index];
        } else if (array instanceof long[]) {
            return ((long[]) array)[index];
        } else if (array instanceof float[]) {
            return ((float[]) array)[index];
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
        } else if (array instanceof byte[]) {
            (((byte[]) array)[index]) = number.byteValue();
        } else if (array instanceof double[]) {
            (((double[]) array)[index]) = number.doubleValue();
        } else if (array instanceof short[]) {
            (((short[]) array)[index]) = number.shortValue();
        } else if (array instanceof int[]) {
            (((int[]) array)[index]) = number.intValue();
        } else if (array instanceof long[]) {
            (((long[]) array)[index]) = number.longValue();
        } else if (array instanceof float[]) {
            (((float[]) array)[index]) = number.floatValue();
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
        if ((x - (long) x) < Math.pow(10.0, -precision)) {
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
        if ((y - (long) y) < Math.pow(10.0, -precision)) {
            return String.format("%d", (long) y);
        } else {
            String ret = String.format("%s", Convert.roundDouble(y, precision));
            return (ret.length() > 8) ? String.format("%." + precision + "G", y) : ret;
        }
    }

    Number getUnsigned(Number value) {
        if (value instanceof Integer) {
            value = Convert.toUnsigned((int) value);
        } else if (value instanceof Short) {
            value = Convert.toUnsigned((short) value);
        } else if (value instanceof Byte) {
            value = Convert.toUnsigned((byte) value);
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
    public Object getElement(Point p, boolean transformed) {
        return getElement(p.y, p.x, transformed);
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

    public Double getElementDbl(Point p, boolean transformed) {
        return getElementDbl(p.y, p.x, transformed);
    }

    public Double getElementDbl(int row, int col, boolean transformed) {
        Object val = getElement(row, col, transformed);
        if (val == null) {
            return Double.NaN;
        }
        if (val instanceof Color) {
            return (double) SwingUtils.getPerceivedLuminance((Color) val);
        }
        return ((Number) val).doubleValue();
    }

    public String getElementStr(Point p, boolean transformed) {
        return getElementStr(p.y, p.x, transformed);
    }

    public String getElementStr(int row, int col, boolean transformed) {
        Object e = getElement(row, col, transformed);
        if (e == null) {
            return "";
        }
        if (e instanceof Color) {
            Color c = (Color) e;
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

    public double[][] getRectSelection(Rectangle rect, boolean transformed) {
        if (rect == null) {
            rect = new Rectangle(getSize(transformed));
        }
        return getRectSelection(rect.x, rect.y, rect.width, rect.height, transformed);
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

    public double[] getLineSelection(Point p1, Point p2, boolean transformed) {
        return getLineSelection(p1.x, p1.y, p2.x, p2.y, transformed);
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

    public double[] getRowSelection(int row, boolean transformed) {
        double[] ret = new double[getSize(true).width];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getElementDbl(row, i, transformed);
        }
        return ret;
    }

    public double[] getRowSelectionX(boolean transformed) {
        double[] ret = new double[getSize(true).width];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getX(i);
        }
        return ret;
    }

    public double[] getColSelection(int col, boolean transformed) {
        double[] ret = new double[getSize(true).height];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getElementDbl(i, col, transformed);
        }
        return ret;
    }

    public double[] getColSelectionX(boolean transformed) {
        double[] ret = new double[getSize(true).height];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getY(i);
        }
        return ret;
    }

    //Transformation
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

    public BufferedImage toBufferedImage(boolean transformed) {
        BufferedImage ret = Utils.newImage((getType() == byte.class) ? array : translateToByteArray(null), new ImageDescriptor(ImageFormat.Gray8, width, height));

        if ((ret != null) && transformed && (sourceConfig != null)) {
            if (sourceConfig.transpose) {
                ret = Utils.transpose(ret);
            }
            if ((sourceConfig.flipVertically) || (sourceConfig.flipHorizontally)) {
                ret = Utils.flip(ret, sourceConfig.flipVertically, sourceConfig.flipHorizontally);
            }
            if (sourceConfig.rotation != 0) {
                ret = Utils.rotate(ret, sourceConfig.rotation, sourceConfig.rotationCrop);
            }
            if (sourceConfig.scale != 1.0) {
                ret = Utils.scale(ret, sourceConfig.scale);
            }
            if ((sourceConfig.roiX > 0) || (sourceConfig.roiY > 0) || (sourceConfig.roiWidth >= 0) || (sourceConfig.roiHeight >= 0)) {
                ret = ret.getSubimage(sourceConfig.roiX, sourceConfig.roiY, sourceConfig.roiWidth, sourceConfig.roiHeight);
            }
        }
        return ret;
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
    
    
    public double integrate(boolean transformed) {
        double ret = 0;
        double[] arr = integrateHorizontally(transformed);
        for (double d : arr) {
            ret += d;
        }
        return ret;
    }    

    void checkValidOpertator(Data op) {
        if ((length != op.length) || (getType() != op.getType()) || (unsigned != op.unsigned)) {
            throw new IllegalArgumentException();
        }
    }

    public void sub(Data op) {
        checkValidOpertator(op);
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] - opdata[i]);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] - opdata[i]);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] - opdata[i]);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] + opdata[i]);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] + opdata[i]);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] + opdata[i]);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (opdata[i]==0) ? Double.NaN : (double) (data[i] / opdata[i]);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (opdata[i]==0) ? Float.NaN :(float) (data[i] / opdata[i]);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (opdata[i]==0) ? Long.MAX_VALUE :(long) (data[i] / opdata[i]);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] * opdata[i]);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] * opdata[i]);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] * opdata[i]);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
            byte[] opdata = (byte[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
            short[] opdata = (short[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
            int[] opdata = (int[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], opdata[i]);
            }
        }
    }

    public void max(Data op) {
        checkValidOpertator(op);
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
            byte[] opdata = (byte[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
            short[] opdata = (short[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double[] opdata = (double[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float[] opdata = (float[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long[] opdata = (long[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
            int[] opdata = (int[]) (op.array);
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], opdata[i]);
            }
        }
    }

    public void sub(Number op) {
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] - val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            double val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] - val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] - val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] + val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] + val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] + val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double val = op.doubleValue();
            double[] data = (double[]) array;
            for (int i = 0; i < length; i++) {
                data[i] = (val==0) ? Double.NaN :(double) (data[i] / val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (val==0) ? Float.NaN :(float) (data[i] / val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (val==0) ? Long.MAX_VALUE : (long) (data[i] / val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = (double) (data[i] * val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = (float) (data[i] * val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = (long) (data[i] * val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.min(data[i], op.byteValue());
            }
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.min(data[i], op.shortValue());
            }
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
            int val = op.intValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.min(data[i], val);
            }
        }
    }

    public void max(Number op) {
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
            for (int i = 0; i < length; i++) {
                data[i] = (byte) Math.max(data[i], op.byteValue());
            }
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
            for (int i = 0; i < length; i++) {
                data[i] = (short) Math.max(data[i], op.shortValue());
            }
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
            double val = op.doubleValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
            float val = op.floatValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
            long val = op.longValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
                int val = op.intValue();
            for (int i = 0; i < length; i++) {
                data[i] = Math.max(data[i], val);
            }
        }
    }
    
    public void threshold(Number threshold, boolean less, Number replacement) {
        if (array instanceof byte[]) {
            byte[] data = (byte[]) array;
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
        } else if (array instanceof short[]) {
            short[] data = (short[]) array;
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
        } else if (array instanceof double[]) {
            double[] data = (double[]) array;
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
        } else if (array instanceof float[]) {
            float[] data = (float[]) array;
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
        } else if (array instanceof long[]) {
            long[] data = (long[]) array;
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
        } else if (array instanceof int[]) {
            int[] data = (int[]) array;
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

}
