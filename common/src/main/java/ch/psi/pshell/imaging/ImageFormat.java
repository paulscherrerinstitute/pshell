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
        return switch (this) {
            case Gray8, Gray16 -> true;
            default -> false;
        };
    }

    public int getDepth() {
        return switch (this) {
            case Gray8, Bayer8 -> 8;
            case Gray16, Bayer16 -> 16;
            case Rgb24, Bgr24 -> 24;
            case Rgba32, Bgra32 -> 32;
            default -> 0;
        };
    }
}
