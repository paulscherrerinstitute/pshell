package ch.psi.pshell.plot;

import java.awt.Color;
import org.jfree.data.xy.XIntervalDataItem;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XYIntervalDataItem;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.YIntervalDataItem;
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
                    //sx.add(x, low, high, y);
                    sx.add(new XIntervalDataItem(x, low, high, y),  getPlot().isUpdatesEnabled());
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    //sy.add(x, y, low, high);
                    sy.add(new YIntervalDataItem(x, y, low, high),  getPlot().isUpdatesEnabled());
                    break;
                //Consider low = errorx, high=errory
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    //s.add(x, x - low, x + low, y, y - high, y + high);
                    s.add(new XYIntervalDataItem(x, x, x, y, y - high, y + high),  getPlot().isUpdatesEnabled());

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
                    //sx.add(x, x - error, x + error, y);
                    sx.add(new XIntervalDataItem(x, x - error, x + error, y),  getPlot().isUpdatesEnabled());
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    //sy.add(x, y, y - error, y + error);
                    sy.add(new YIntervalDataItem(x, y, y - error, y + error),  getPlot().isUpdatesEnabled());
                    break;
                case ErrorXY:
                    appendData(x, x - error, x + error, y, y - error, y + error);
            }
        }
    }

    public void appendData(double x, double xLow, double xHigh, double y, double yLow, double yHigh) {
        if (getPlot() != null) {
            if (getPlot().getStyle() == LinePlot.Style.ErrorXY) {
                XYIntervalSeries s = (XYIntervalSeries) getToken();
                //s.add(x, xLow, xHigh, y, yLow, yHigh);
                s.add(new XYIntervalDataItem(x, xLow, xHigh, y, yLow, yHigh),  getPlot().isUpdatesEnabled());
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
