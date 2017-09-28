package ch.psi.pshell.plot;

/**
 *
 */
public interface LinePlot extends Plot<LinePlotSeries> {

    //public void setShowMarkers(boolean quality);
    //public boolean getShowMarkers();    
    public enum Style {
        Normal,
        Step,
        Spline,
        ErrorX,
        ErrorY,
        ErrorXY;

        public boolean isError() {
            return (this == ErrorX) || (this == ErrorY) || (this == ErrorXY);
        }
    }

    default public Style getStyle() {
        return Style.Normal;
    }

    default public void setStyle(Style style) {
    }

}
