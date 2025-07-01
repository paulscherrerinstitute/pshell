package ch.psi.pshell.imaging;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

/**
 *
 */
public class ImageDescriptor {

    final public ImageFormat format;
    final public int width;
    final public int height;
    final public int depth;
    final public int stride;

    final public int pixelStride;
    final public int[] bandOffset;
    final public ColorModel colorModel;
    final public ColorSpace colorSpace;
    final public Class dataBufferType;
    final public int totalSize;
    final public int numberOfPixels;

    final public boolean opaque;

    public ImageDescriptor(ImageFormat format, int width, int height) {
        this(format, width, height, -1);
    }

    public ImageDescriptor(ImageFormat format, int width, int height, int stride) {
        this(format, width, height, stride, true);
    }

    public ImageDescriptor(ImageFormat format, int width, int height, int stride, boolean opaque) {
        this.format = format;
        this.width = width;
        this.height = height;
        this.depth = format.getDepth();
        this.opaque = opaque;

        switch (format) {
            case Gray8:
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                colorModel = new ComponentColorModel(colorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                bandOffset = new int[]{0};
                pixelStride = 1;
                break;
            case Gray16:
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                colorModel = new ComponentColorModel(colorSpace, new int[]{16}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
                bandOffset = new int[]{0};
                pixelStride = 1;
                break;
            case Rgb24:
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                bandOffset = new int[]{0, 1, 2};
                pixelStride = 3;
                break;
            case Bgr24:
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                bandOffset = new int[]{2, 1, 0};
                pixelStride = 3;
                break;

            case Rgba32:
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                if (this.opaque) {
                    colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8, 0}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                    bandOffset = new int[]{0, 1, 2};
                } else {
                    colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8, 8}, true, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                    bandOffset = new int[]{0, 1, 2, 3};
                }
                pixelStride = 4;
                break;
            case Bgra32:
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                if (this.opaque) {
                    colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8, 8}, true, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                    bandOffset = new int[]{2, 1, 0, 3};
                } else {
                    colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8, 8}, true, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                    bandOffset = new int[]{2, 1, 0, 3};
                }
                pixelStride = 4;
                break;
            default:
                throw new IllegalArgumentException("Unsupported Format: " + format);
        }
        if (stride < 0) {
            this.stride = this.width * pixelStride;
        } else {
            this.stride = stride;
        }
        numberOfPixels = width * height;
        totalSize = stride * height;
        if (colorModel.getTransferType() == DataBuffer.TYPE_USHORT) {
            dataBufferType = short[].class;
        } else {
            dataBufferType = byte[].class;
        }
    }
}
