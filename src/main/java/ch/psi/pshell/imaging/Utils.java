package ch.psi.pshell.imaging;

import ch.psi.utils.Convert;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Facade to Java2D imaging operations
 */
public class Utils {

    //Properties
    public static boolean isGrayscale(BufferedImage image) {
        return ((image.getType() == BufferedImage.TYPE_BYTE_GRAY)
                || (image.getType() == BufferedImage.TYPE_USHORT_GRAY));
    }

    //Instantiation of new image objects using existing data buffer on memory
    public static BufferedImage newImage(byte[] array, ImageDescriptor ip) {
        DataBuffer buffer = new DataBufferByte(array, ip.stride * ip.height);
        WritableRaster wr = Raster.createInterleavedRaster(buffer, ip.width, ip.height, ip.stride, ip.pixelStride, ip.bandOffset, null);
        BufferedImage ret = new BufferedImage(ip.colorModel, wr, false, null);

        //This is a Java Bug
        if (ip.format == ImageFormat.Bgra32) {
            ret = setImageAlpha(ret, 1.0f, true);
        }

        return ret;
    }

    public static BufferedImage newImage(short[] array, ImageDescriptor ip) {
        DataBuffer buffer = new DataBufferUShort(array, ip.stride * ip.height);
        WritableRaster wr = Raster.createInterleavedRaster(buffer, ip.width, ip.height, ip.stride, ip.pixelStride, ip.bandOffset, null);
        BufferedImage ret = new BufferedImage(ip.colorModel, wr, false, null);
        return ret;
    }

    public static BufferedImage newImage(Object array, ImageDescriptor ip) {
        if ((ip.colorModel.getTransferType() == DataBuffer.TYPE_BYTE) && (array instanceof byte[])) {
            return Utils.newImage((byte[]) array, ip);
        } else if ((ip.colorModel.getTransferType() == DataBuffer.TYPE_USHORT) && (array instanceof short[])) {
            return Utils.newImage((short[]) array, ip);
        }
        throw new IllegalArgumentException("Unsupported format");
    }

    public static BufferedImage newImage(Data data, ColorModel grayRange, Colormap colormap, boolean logarithmic) {
        return newImage(data.getDataBuffer(), data.getWidth(), data.getHeight(), grayRange, colormap, logarithmic);
    }

    public static BufferedImage newImage(DataBuffer buffer, int width, int height, ColorModel grayRange, Colormap colormap, boolean logarithmic) {
        SampleModel sm = new PixelInterleavedSampleModel(buffer.getDataType(), width, height, 1, width, new int[]{0});
        WritableRaster wr = WritableRaster.createWritableRaster(sm, buffer, null);
        BufferedImage ret = new BufferedImage(grayRange, wr, true, new Hashtable<Object, Object>());
        if ((colormap != Colormap.Grayscale) || logarithmic) {
            ret = Utils.newImage(ret, colormap, logarithmic);
        }
        return ret;
    }

    public static BufferedImage newImage(Data data, float scaleMin, float scaleMax, Colormap colormap, boolean logarithmic) {
        return newImage(data.getDataBuffer(), data.getWidth(), data.getHeight(), scaleMin, scaleMax, colormap, logarithmic, data.isUnsigned());
    }

    public static BufferedImage newImage(DataBuffer buffer, int width, int height, float scaleMin, float scaleMax, Colormap colormap, boolean logarithmic) {
        return newImage(buffer, width, height, scaleMin, scaleMax, colormap, logarithmic, Utils.isDataBufferUnsigned(buffer));
    }

    static BufferedImage newImage(DataBuffer buffer, int width, int height, float scaleMin, float scaleMax, Colormap colormap, boolean logarithmic, boolean unsigned) {
        ColorModel grayRange = getGrayRange(scaleMin, scaleMax, buffer.getDataType(), unsigned);
        return newImage(buffer, width, height, grayRange, colormap, logarithmic);
    }

    public static ColorModel getGrayRange(float scaleMin, float scaleMax, int dataType) {
        return getGrayRange(scaleMin, scaleMax, dataType, false);
    }
    
    public static ColorModel getGrayRange(float scaleMin, float scaleMax, int dataType, boolean unsigned) {
        float scaleRange = (scaleMax - scaleMin) == 0 ? Float.NaN : (scaleMax - scaleMin);

        ColorSpace csGray = new ColorSpace(ColorSpace.TYPE_GRAY, 1) {
            @Override
            public float[] toRGB(float[] colorvalue) {
                float val = (colorvalue[0] - scaleMin) / scaleRange;
                return new float[]{((Float.isNaN(val) || (val < 0.0f)) ? 0.0f : (val > 1.0f ? 1.0f : val))};
            }

            @Override
            public float[] fromRGB(float[] rgbvalue) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public float[] toCIEXYZ(float[] colorvalue) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public float[] fromCIEXYZ(float[] colorvalue) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public float getMinValue(int component) {
                return (float) scaleMin;
            }

            @Override
            public float getMaxValue(int component) {
                return (float) scaleMax;
            }
        };

        ComponentColorModel cmGray = new ComponentColorModel(csGray, false, false, ColorModel.OPAQUE, dataType) {
            public float[] getNormalizedComponents(Object pixel,
                    float[] normComponents,
                    int normOffset) {
                switch (transferType) {
                    case DataBuffer.TYPE_BYTE:
                        if (unsigned) {
                            return new float[]{(float) Convert.toUnsigned(((byte[]) pixel)[0])};
                        } else {
                            return new float[]{(float) ((byte[]) pixel)[0]};
                        }
                    case DataBuffer.TYPE_USHORT:
                        return new float[]{(float) Convert.toUnsigned(((short[]) pixel)[0])};
                    case DataBuffer.TYPE_INT:
                        if (unsigned) {
                            return new float[]{(float) Convert.toUnsigned(((int[]) pixel)[0])};
                        } else {
                            return new float[]{(float) ((int[]) pixel)[0]};
                        }
                    case DataBuffer.TYPE_SHORT:
                        if (unsigned) {
                            return new float[]{(float) Convert.toUnsigned(((short[]) pixel)[0])};
                        } else {
                            return new float[]{(float) ((short[]) pixel)[0]};
                        }
                    case DataBuffer.TYPE_FLOAT:
                        return new float[]{((float[]) pixel)[0]};
                    case DataBuffer.TYPE_DOUBLE:
                        return new float[]{(float) ((double[]) pixel)[0]};
                    default:
                        return new float[1];
                }
            }
        };
        return cmGray;
    }

    //Instantiation of new empty image objects derived from reference image
    public static BufferedImage newImage(BufferedImage ref, Integer type, int width, int height) {
        if (ref == null) {
            return null;
        }
        if (width <= 0) {
            width = ref.getWidth();
        }
        if (height <= 0) {
            height = ref.getHeight();
        }
        if (type == null) {
            type = ref.getType();
        }

        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_RGB;
        }

        ColorModel cm = ref.getColorModel();
        BufferedImage ret;
        if ((cm instanceof IndexColorModel) && ((type == BufferedImage.TYPE_BYTE_INDEXED) || (type == BufferedImage.TYPE_BYTE_BINARY))) {
            ret = new BufferedImage(width, height, type, (IndexColorModel) cm);
        } else {
            ret = new BufferedImage(width, height, type);
        }
        return ret;
    }

    //Instantiation of new empty image objects derived from streams (image files: bmp, png or jpg).
    public static BufferedImage newImage(byte[] array) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(array));
    }

    public static BufferedImage newImage(String fileName) throws IOException {
        return ImageIO.read(new BufferedInputStream(new FileInputStream(fileName)));
    }

    public static final BufferedImage newImage(BufferedImage ref, Colormap colormap) {
        return newImage(ref, colormap, false);
    }

    public static final BufferedImage newImage(BufferedImage ref, Colormap colormap, boolean logarithmic) {
        BufferedImage aux = copy(ref, BufferedImage.TYPE_INT_RGB, null);
        return execLookup(aux, logarithmic ? colormap.getLookupTableLogarithmic() : colormap.getLookupTable(), true);
    }

    // Operations and transformations 
    public static BufferedImage copy(BufferedImage image, Integer type, Rectangle bounds) {
        if (image == null) {
            return null;
        }
        BufferedImage subImage = image;
        if (bounds != null) {
            subImage = image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        BufferedImage ret = newImage(subImage, type, -1, -1);
        Graphics2D g = ret.createGraphics();
        g.drawImage(subImage, 0, 0, null);
        g.dispose();
        return ret;
    }

    public static final BufferedImage grayscale(BufferedImage image) {
        return grayscale(image, null);
    }

    public static final BufferedImage grayscale(BufferedImage image, Rectangle bounds) {
        return copy(image, BufferedImage.TYPE_BYTE_GRAY, bounds);
    }

    //Color filling
    public static void fill(BufferedImage image, Color c) {
        fill(image, c, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
    }

    public static void fill(BufferedImage image, Color c, Rectangle bounds) {
        Graphics2D g = image.createGraphics();
        g.setPaintMode();
        g.setPaint(c);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.dispose();
    }

    public static BufferedImage stretch(BufferedImage image, int width, int height) {
        if ((image == null) || (width <= 0) || (height <= 0)) {
            return null;
        }
        if ((image.getWidth() <= 0) || (image.getHeight() <= 0)) {
            return null;
        }

        BufferedImage ret = newImage(image, null, width, height);

        AffineTransform transf = new AffineTransform();
        transf.scale(((double) width) / image.getWidth(), ((double) height) / image.getHeight());

        Graphics2D g = ret.createGraphics();
        g.drawImage(image, transf, null);
        g.dispose();
        return ret;
    }

    public static BufferedImage scale(BufferedImage image, double factor) {
        if ((image == null) || (factor <= 0)) {
            return null;
        }
        if ((image.getWidth() <= 0) || (image.getHeight() <= 0)) {
            return null;
        }

        int width = (int) (image.getWidth() * factor);
        int height = (int) (image.getHeight() * factor);
        BufferedImage ret = newImage(image, null, width, height);
        AffineTransform transf = new AffineTransform();
        transf.scale(factor, factor);
        Graphics2D g = ret.createGraphics();
        g.drawImage(image, transf, null);
        g.dispose();
        return ret;
    }

    public static BufferedImage transpose(BufferedImage image) {
        if ((image == null) || (image.getWidth() <= 0) || (image.getHeight() <= 0)) {
            return null;
        }

        BufferedImage ret = newImage(image, null, image.getHeight(), image.getWidth());
        AffineTransform transf = new AffineTransform();
        transf.setToTranslation(image.getWidth(), 0);
        transf.setToScale(-1, 1);
        transf.rotate(90.0 * Math.PI / 180.0, 0, 0);

        Graphics2D g = ret.createGraphics();
        g.drawImage(image, transf, null);
        g.dispose();
        return ret;
    }

    public static BufferedImage setImageAlpha(BufferedImage image, float alpha, boolean inPlace) {
        BufferedImage ret = inPlace ? image : newImage(image, null, -1, -1);
        RasterOp rop = new RescaleOp(new float[]{1.0f, 1.0f, 1.0f, 0.0f}, new float[]{0.0f, 0.0f, 0.0f, 255.0f * alpha}, null);
        rop.filter(image.getData(), ret.getRaster());
        return ret;
    }

    public static BufferedImage rotate(BufferedImage image, double degrees) {
        return rotate(image, degrees, false);
    }

    public static BufferedImage rotate(BufferedImage image, double degrees, boolean crop) {
        double rads = degrees * Math.PI / 180.0;

        int inWidth = image.getWidth();
        int inHeight = image.getHeight();
        if ((inWidth <= 0) || (inHeight <= 0)) {
            return null;
        }

        int outWidth = (int) (Math.abs(Math.cos(rads) * inWidth) + Math.abs(Math.sin(rads) * inHeight));
        int outHeight = (int) (Math.abs(Math.sin(rads) * inWidth) + Math.abs(Math.cos(rads) * inHeight));

        BufferedImage ret = null;

        AffineTransform transf = new AffineTransform();
        if (crop) {
            ret = newImage(image, null, -1, -1);
        } else {
            ret = newImage(image, null, outWidth, outHeight);
            transf.setToTranslation((outWidth - inWidth) / 2, (outHeight - inHeight) / 2);
        }
        transf.rotate(rads, inWidth / 2, inHeight / 2);

        Graphics2D g = ret.createGraphics();
        g.drawImage(image, transf, null);
        g.dispose();
        return ret;
    }

    public static BufferedImage flip(BufferedImage image, boolean vertically, boolean horizontally) {
        if (image == null) {
            return null;
        }
        AffineTransformOp op;
        if (vertically && horizontally) {
            AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
            tx.translate(-image.getWidth(null), -image.getHeight(null));
            op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        } else if (vertically) {
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -image.getHeight(null));
            op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        } else if (horizontally) {
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-image.getWidth(null), 0);
            op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        } else {
            return image;
        }
        return op.filter(image, null);
    }

    //Operations that can be executed in place
    public static BufferedImage rescale(BufferedImage image, double scale, double offset, boolean inPlace) {
        RescaleOp oper = new RescaleOp((float) scale, (float) offset, null);
        BufferedImage ret = inPlace ? image : newImage(image, null, -1, -1);
        oper.filter(image, ret);
        return ret;
    }

    public static BufferedImage translate(BufferedImage image, int min, int max, boolean inPlace) {
        int range = max - min;
        double scale = (range > 0) ? (255.0 / range) : 1.0;
        double offset = -min;
        return rescale(image, scale, offset, inPlace);
    }

    public static BufferedImage invert(BufferedImage image, boolean inPlace) {

        if (image == null) {
            return null;
        }

        if (image.getColorModel().getPixelSize() == 16) {
            short[] lut = new short[65536];
            for (int i = 0; i < 65536; i++) {
                lut[i] = (short) (65535 - i);
            }
            return execLookup(image, lut, inPlace);
        } else {
            short[] lut = new short[256];
            for (short i = 0; i < 256; i++) {
                lut[i] = (short) (255 - i);
            }
            return execLookup(image, lut, inPlace);
        }
    }

    public static BufferedImage execLookup(BufferedImage image, LookupTable lut, boolean inPlace) {
        BufferedImage ret = inPlace ? image : newImage(image, null, -1, -1);
        LookupOp oper = new LookupOp(lut, null);
        oper.filter(image, ret);
        return ret;
    }

    public static BufferedImage execLookup(BufferedImage image, short[] lookupData, boolean inPlace) {
        LookupTable lut = new ShortLookupTable(0, lookupData);
        return execLookup(image, lut, inPlace);
    }

    public static BufferedImage execLookup(BufferedImage image, short[] red, short[] green, short[] blue, boolean inPlace) {
        LookupTable lut = new ShortLookupTable(0, new short[][]{red, green, blue});
        return execLookup(image, lut, inPlace);
    }

    //Bitwise Operations
    public static BufferedImage and(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ret.setRGB(x, y, image1.getRGB(x, y) & image2.getRGB(x, y));
            }
        }
        return ret;
    }

    public static BufferedImage or(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ret.setRGB(x, y, image1.getRGB(x, y) | image2.getRGB(x, y));
            }
        }
        return ret;
    }

    public static BufferedImage xor(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ret.setRGB(x, y, image1.getRGB(x, y) ^ image2.getRGB(x, y));
            }
        }
        return ret;
    }

    //Aritmethic operations
    public static BufferedImage sub(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ret.setRGB(x, y, Math.max(image1.getRGB(x, y) - image2.getRGB(x, y), 0));
            }
        }
        return ret;
    }

    public static BufferedImage add(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        int max = getMaxPixelValue(ret);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ret.setRGB(x, y, Math.min(image1.getRGB(x, y) + image2.getRGB(x, y), max));
            }
        }
        return ret;
    }

    public static BufferedImage mult(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        int max = getMaxPixelValue(ret);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ret.setRGB(x, y, Math.min(image1.getRGB(x, y) * image2.getRGB(x, y), max));
            }
        }
        return ret;
    }

    public static BufferedImage div(BufferedImage image1, BufferedImage image2, boolean inPlace) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage ret = inPlace ? image1 : newImage(image1, null, -1, -1);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double d = image2.getRGB(x, y);
                ret.setRGB(x, y, (d <= 0) ? 0 : (int) (image1.getRGB(x, y) / d));
            }
        }
        return ret;
    }

    //Convolution
    public static BufferedImage convolve(BufferedImage image, Kernel kernel) {
        if ((image == null) || (kernel == null)) {
            return null;
        }
        BufferedImageOp oper = new ConvolveOp(kernel);
        return oper.filter(image, null);
    }

    public static BufferedImage convolve(BufferedImage image, int width, int height, float[] data) {
        Kernel kernel = new Kernel(width, height, data);
        return convolve(image, kernel);
    }

    public static BufferedImage convolve(BufferedImage image, float[] data) {
        //Assumes square
        int size = (int) Math.sqrt(data.length);
        Kernel kernel = new Kernel(size, size, data);
        return convolve(image, kernel);
    }

    //Standard convolution filters
    public static BufferedImage blur(BufferedImage image) {
        return convolve(image, new float[]{0.1111f, 0.1111f, 0.1111f, 0.1111f, 0.1111f, 0.1111f, 0.1111f, 0.1111f, 0.1111f});
    }

    public static BufferedImage sharpen(BufferedImage image) {
        return convolve(image, new float[]{0.0f, -0.75f, 0.0f, -0.75f, 4.0f, -0.75f, 0.0f, -0.75f, 0.0f});
    }

    public static BufferedImage sharpen2(BufferedImage image) {
        return convolve(image, new float[]{-1.0f, -1.0f, -1.0f, -1.0f, 9.0f, -1.0f, -1.0f, -1.0f, -1.0f});
    }

    public static BufferedImage light(BufferedImage image) {
        return convolve(image, new float[]{0.1f, 0.1f, 0.1f, 0.1f, 1.0f, 0.1f, 0.1f, 0.1f, 0.1f});
    }

    public static BufferedImage dark(BufferedImage image) {
        return convolve(image, new float[]{0.01f, 0.01f, 0.01f, 0.01f, 0.5f, 0.01f, 0.01f, 0.01f, 0.01f});
    }

    public static BufferedImage edgeDetect(BufferedImage image) {
        return convolve(image, new float[]{0.0f, -0.75f, 0.0f, -0.75f, 3.0f, -0.75f, 0.0f, -0.75f, 0.0f});
    }

    public static BufferedImage edgeDetect2(BufferedImage image) {
        return convolve(image, new float[]{-0.5f, -0.5f, -0.5f, -0.5f, 4.0f, -0.5f, -0.5f, -0.5f, -0.5f});
    }

    public static BufferedImage sobel(BufferedImage image) {
        return convolve(image, new float[]{2.0f, 2.0f, 0.0f, 2.0f, 0.0f, -2.0f, 0.0f, -2.0f, -2.0f});
    }

    public static BufferedImage prewitt(BufferedImage image) {
        return convolve(image, new float[]{-2.0f, -1.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 2.0f});
    }

    public static BufferedImage differentialEdgeDetect(BufferedImage image) {
        return convolve(image, new float[]{-1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, -1.0f});
    }

    //Simple image operations
    public static int[] integrateVertically(BufferedImage image) {
        int[] ret = new int[image.getWidth()];
        DataBuffer buffer = image.getData().getDataBuffer();
        int stride = image.getWidth();
        for (int x = 0; x < image.getWidth(); x++) {
            int aux = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                aux += buffer.getElem(x + y * stride);
            }
            ret[x] = aux;
        }
        return ret;
    }

    public static int[] integrateHorizontally(BufferedImage image) {
        int[] ret = new int[image.getHeight()];
        DataBuffer buffer = image.getData().getDataBuffer();
        int stride = image.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            int aux = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                aux += buffer.getElem(x + y * stride);
            }
            ret[y] = aux;
        }
        return ret;
    }

    public static double[] integrateVerticallyDouble(BufferedImage image) {
        double[] ret = new double[image.getWidth()];
        DataBuffer buffer = image.getData().getDataBuffer();
        int stride = image.getWidth();
        for (int x = 0; x < image.getWidth(); x++) {
            double aux = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                aux += buffer.getElemDouble(x + y * stride);
            }
            ret[x] = aux;
        }
        return ret;
    }

    public static double[] integrateHorizontallyDouble(BufferedImage image) {
        double[] ret = new double[image.getHeight()];
        DataBuffer buffer = image.getData().getDataBuffer();
        int stride = image.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            double aux = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                aux += buffer.getElemDouble(x + y * stride);
            }
            ret[y] = aux;
        }
        return ret;
    }

    public static List<Integer> getRange(BufferedImage img) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        if (!isGrayscale(img)) {
            img = grayscale(img);
        }
        DataBuffer db = img.getData().getDataBuffer();
        for (int i = 0; i < db.getSize(); i++) {
            int v = db.getElem(i);
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        ArrayList<Integer> ret = new ArrayList<>();
        ret.add(min);
        ret.add(max);
        return ret;
    }

    public static int getMaxPixelValue(BufferedImage img) {
        switch (img.getColorModel().getPixelSize()) {
            case 16:
                return 0xFFFF;
        }
        return 255;
    }

    static String selectedImageFolder;

    public static void setSelectedImageFolder(String folderName) {
        if (folderName != null) {
            selectedImageFolder = folderName;
        }
    }

    public static String getSelectedImageFolder() {
        return selectedImageFolder;
    }

    public static Object getDataBufferArray(DataBuffer buffer) {
        switch (buffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return ((DataBufferByte) buffer).getData();
            case DataBuffer.TYPE_DOUBLE:
                return ((DataBufferDouble) buffer).getData();
            case DataBuffer.TYPE_FLOAT:
                return ((DataBufferFloat) buffer).getData();
            case DataBuffer.TYPE_INT:
                return ((DataBufferInt) buffer).getData();
            case DataBuffer.TYPE_SHORT:
                return ((DataBufferShort) buffer).getData();
            case DataBuffer.TYPE_USHORT:
                return ((DataBufferUShort) buffer).getData();
            default:
                throw new IllegalArgumentException("Invalid data buffer type");
        }
    }

    public static boolean isDataBufferUnsigned(DataBuffer buffer) {
        switch (buffer.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
                return true;
            default:
                return false;
        }

    }
}
