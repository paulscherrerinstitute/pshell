package ch.psi.pshell.imaging;

/**
 *
 */
public enum ImageFormat {

    Gray8,
    Gray16,
    Bayer8,
    Bayer16,
    Rgb24,
    Rgba32,
    Bgr24,
    Bgra32,
    Unknown;

    public boolean isGrayscale() {
        switch (this) {
            case Gray8:
            case Gray16:
                return true;
            default:
                return false;
        }
    }

    public int getDepth() {
        switch (this) {
            case Gray8:
            case Bayer8:
                return 8;
            case Gray16:
            case Bayer16:
                return 16;
            case Rgb24:
            case Bgr24:
                return 24;
            case Rgba32:
            case Bgra32:
                return 32;
            default:
                return 0;
        }
    }
}
