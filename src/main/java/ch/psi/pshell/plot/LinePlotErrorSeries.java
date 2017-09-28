package ch.psi.pshell.plot;

import java.awt.Color;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.YIntervalSeries;

/**
 *
 */
public class LinePlotErrorSeries extends LinePlotSeries {

    public LinePlotErrorSeries(String name) {
        super(name);
    }

    public LinePlotErrorSeries(String name, Color color) {
        super(name, color);
    }

    public LinePlotErrorSeries(String name, Color color, int axisY) {
        super(name, color, axisY);
    }

    //TODO: Only on LinePlotJFree
    @Override
    public LinePlotJFree getPlot() {
        return (LinePlotJFree) super.getPlot();
    }

    public void appendData(double x, double y, double low, double high) {
        if (getPlot() != null) {
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    sx.add(x, low, high, y);
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    sy.add(x, y, low, high);
                    break;
                //Consider low = errorx, high=errory
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    s.add(x, x - low, x + low, y, y - high, y + high);

            }
        }
    }

    @Override
    public void appendData(double x, double y) {
        appendData(x, y, 0.0);
    }

    public void appendData(double x, double y, double error) {
        if (getPlot() != null) {
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    sx.add(x, x - error, x + error, y);
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    sy.add(x, y, y - error, y + error);
                    break;
                case ErrorXY:
                    appendData(x, y, error, error);
            }
        }
    }

    public void appendData(double x, double xLow, double xHigh, double y, double yLow, double yHigh) {
        if (getPlot() != null) {
            if (getPlot().getStyle() == LinePlot.Style.ErrorXY) {
                XYIntervalSeries s = (XYIntervalSeries) getToken();
                s.add(x, xLow, xHigh, y, yLow, yHigh);
            }
        }
    }

    @Override
    public void setData(double[] y) {
        setData(null, y);
    }

    public void setData(double[] x, double[] y) {
        setData(x, y, null);
    }

    public void setData(double[] x, double[] y, double[] error) {
        setData(x, y, error, null);
    }

    public void setData(double[] x, double[] y, double[] error, double[] errorY) {
        if (getPlot() != null) {
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    sx.clear();
                    for (int i = 0; i < y.length; i++) {
                        appendData(x == null ? i : x[i], y[i], error == null ? 0.0 : error[i]);
                    }
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    sy.clear();
                    for (int i = 0; i < y.length; i++) {
                        appendData(x == null ? i : x[i], y[i], error == null ? 0.0 : error[i]);
                    }
                    break;
                case ErrorXY:
                    XYIntervalSeries sxy = (XYIntervalSeries) getToken();
                    sxy.clear();
                    for (int i = 0; i < y.length; i++) {
                        appendData(x == null ? i : x[i], y[i], error == null ? 0.0 : error[i], errorY == null ? 0.0 : errorY[i]);
                    }
            }
        }
    }
}
