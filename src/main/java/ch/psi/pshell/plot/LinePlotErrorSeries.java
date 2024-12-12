package ch.psi.pshell.plot;

import static ch.psi.pshell.plot.LinePlot.Style.ErrorX;
import static ch.psi.pshell.plot.LinePlot.Style.ErrorXY;
import static ch.psi.pshell.plot.LinePlot.Style.ErrorY;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
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
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    //sx.add(x, low, high, y);
                    sx.add(new XIntervalDataItem(x, low, high, y), updatesEnabled);
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    //sy.add(x, y, low, high);
                    sy.add(new YIntervalDataItem(x, y, low, high), updatesEnabled);
                    break;
                //Consider low = errorx, high=errory
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    //s.add(x, x - low, x + low, y, y - high, y + high);
                    s.add(new XYIntervalDataItem(x, x, x, y, low, high), updatesEnabled);

            }
        }

    }
    
    public void appendData(double[] x, double[]  y, double[]  low, double[]  high) {
        if (getPlot() != null) {
            int length = Math.min(Math.min(Math.min(x.length, y.length), low.length), high.length);
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        sx.add(new XIntervalDataItem(x[i], low[i], high[i], y[i]), updatesEnabled);
                    }
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        sy.add(new YIntervalDataItem(x[i], y[i], low[i], high[i]), updatesEnabled);
                    }
                    break;
                //Consider low = errorx, high=errory
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        double ix = x[i];
                        s.add(new XYIntervalDataItem(ix, ix, ix, y[i], low[i], high[i]), updatesEnabled);
                    }

            }
        }
    }
    

    public void appendData(List<? extends Number> x, List<? extends Number> y, List<? extends Number> low, List<? extends Number> high) {
        if (getPlot() != null) {
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            int length = Math.min(Math.min(Math.min(x.size(), y.size()), low.size()), high.size());
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        if (x.get(i) != null && y.get(i) != null && low.get(i) != null && high.get(i) != null){
                            sx.add(new XIntervalDataItem(x.get(i).doubleValue(), low.get(i).doubleValue(), high.get(i).doubleValue(), y.get(i).doubleValue()), updatesEnabled);
                        }
                    }
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        if (x.get(i) != null && y.get(i) != null && low.get(i) != null && high.get(i) != null){
                            sy.add(new YIntervalDataItem(x.get(i).doubleValue(), y.get(i).doubleValue(), low.get(i).doubleValue(), high.get(i).doubleValue()), updatesEnabled);
                        }
                    }
                    break;
                //Consider low = errorx, high=errory
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        if (x.get(i) != null && y.get(i) != null && low.get(i) != null && high.get(i) != null){
                            double ix = x.get(i).doubleValue();
                            s.add(new XYIntervalDataItem(ix, ix, ix, y.get(i).doubleValue(), low.get(i).doubleValue(), high.get(i).doubleValue()), updatesEnabled);
                        }
                    }

            }
        }
    }

    @Override
    public void appendData(double x, double y) {
        appendData(x, y, 0.0);
    }

    public void appendData(double[] x, double[] y) {
        appendData(x, y, new double[x.length]);
    }
    
    public void appendData(List<? extends Number> x, List<? extends Number> y) {
        List<Double> error = new ArrayList<>(Collections.nCopies(x.size(), 0.0));
        appendData(x, y, error);
    }
    

    public void appendData(double x, double y, double error) {
        if (getPlot() != null) {
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    sx.add(new XIntervalDataItem(x, x - error, x + error, y), updatesEnabled);
                    break; 
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    sy.add(new YIntervalDataItem(x, y, y - error, y + error), updatesEnabled);
                    break;
                case ErrorXY:
                    appendData(x, x - error, x + error, y, y - error, y + error);
            }
        }
    }
    
    public void appendData(double[] x, double[] y, double[] error){
        if (getPlot() != null) {
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            int length = Math.min(Math.min(x.length, y.length), error.length);
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        double ix = x[i];
                        double ie = error[i];
                        sx.add(new XIntervalDataItem(ix, ix-ie, ix+ie, y[i]), updatesEnabled);
                    }
                    break; 
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        double iy = y[i];
                        double ie = error[i];
                        sy.add(new YIntervalDataItem(x[i], iy, iy - ie, iy + ie), updatesEnabled);
                    }
                    break;
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        double ix = x[i];
                        double iy = y[i];
                        double ie = error[i];
                        s.add(new XYIntervalDataItem(ix, ix - ie, ix + ie, iy, iy - ie, iy + ie), updatesEnabled);
                    }                    
            }
        }        
    }    
    
    public void appendData(List<? extends Number> x, List<? extends Number> y, List<? extends Number> error){
        if (getPlot() != null) {
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            int length = Math.min(Math.min(x.size(), y.size()), error.size());
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        if (x.get(i) != null && y.get(i) != null && error.get(i) != null){
                            double ix = x.get(i).doubleValue();
                            double ie = error.get(i).doubleValue();
                            sx.add(new XIntervalDataItem(ix, ix-ie, ix+ie, y.get(i).doubleValue()), updatesEnabled);
                        }
                    }
                    break; 
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        if (x.get(i) != null && y.get(i) != null && error.get(i) != null){
                            double iy = y.get(i).doubleValue();
                            double ie = error.get(i).doubleValue();
                            sy.add(new YIntervalDataItem(x.get(i).doubleValue(), iy, iy - ie, iy + ie), updatesEnabled);
                        }
                    }
                    break;
                case ErrorXY:
                    XYIntervalSeries s = (XYIntervalSeries) getToken();
                    for (int i = 0; i < length; i++) {
                        if (x.get(i) != null && y.get(i) != null && error.get(i) != null){
                            double ix = x.get(i).doubleValue();
                            double iy = y.get(i).doubleValue();
                            double ie = error.get(i).doubleValue();
                            s.add(new XYIntervalDataItem(ix, ix - ie, ix + ie, iy, iy - ie, iy + ie), updatesEnabled);                            
                        }
                    }                    
            }
        }        
    }
    

    public void appendData(double x, double xLow, double xHigh, double y, double yLow, double yHigh) {
        if (getPlot() != null) {            
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            if (getPlot().getStyle() == LinePlot.Style.ErrorXY) {
                XYIntervalSeries s = (XYIntervalSeries) getToken();
                s.add(new XYIntervalDataItem(x, xLow, xHigh, y, yLow, yHigh), updatesEnabled);
            }
        }
    }

    public void appendData(double[] x, double[] xLow, double[] xHigh, double[] y, double[] yLow, double[] yHigh) {
        if (getPlot() != null) {
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            int length = Math.min( Math.min(Math.min(Math.min(Math.min(x.length, y.length), xLow.length), xHigh.length), yLow.length), yHigh.length);
            if (getPlot().getStyle() == LinePlot.Style.ErrorXY) {
                XYIntervalSeries s = (XYIntervalSeries) getToken();
                for (int i = 0; i < length; i++) {
                    s.add(new XYIntervalDataItem(x[i], xLow[i], xHigh[i], y[i], yLow[i], yHigh[i]), updatesEnabled);
                }
            }
        }
        
    }
    
    public void appendData(List<? extends Number> x, List<? extends Number> xLow, List<? extends Number> xHigh, List<? extends Number> y, List<? extends Number> yLow, List<? extends Number> yHigh) {
        if (getPlot() != null) {
            boolean updatesEnabled = getPlot().isUpdatesEnabled();
            int length = Math.min( Math.min(Math.min(Math.min(Math.min(x.size(), y.size()), xLow.size()), xHigh.size()), yLow.size()), yHigh.size());
            if (getPlot().getStyle() == LinePlot.Style.ErrorXY) {
                XYIntervalSeries s = (XYIntervalSeries) getToken();
                for (int i = 0; i < length; i++) {
                    if (x.get(i) != null && xLow.get(i) != null && xHigh.get(i) != null && y.get(i) != null && yLow.get(i) != null && yHigh.get(i) != null){
                        s.add(new XYIntervalDataItem(x.get(i).doubleValue(), xLow.get(i).doubleValue(), xHigh.get(i).doubleValue(), y.get(i).doubleValue(), yLow.get(i).doubleValue(), yHigh.get(i).doubleValue()), updatesEnabled);
                    }
                }
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
            int length = Math.min(Math.min(Math.min(x.length, y.length), error.length), errorY.length);
            switch (getPlot().getStyle()) {
                case ErrorX:
                    XIntervalSeries sx = (XIntervalSeries) getToken();
                    sx.clear();
                    for (int i = 0; i < length; i++) {
                        appendData(x == null ? i : x[i], y[i], error == null ? 0.0 : error[i]);
                    }
                    break;
                case ErrorY:
                    YIntervalSeries sy = (YIntervalSeries) getToken();
                    sy.clear();
                    for (int i = 0; i < length; i++) {
                        appendData(x == null ? i : x[i], y[i], error == null ? 0.0 : error[i]);
                    }
                    break;
                case ErrorXY:
                    XYIntervalSeries sxy = (XYIntervalSeries) getToken();
                    sxy.clear();
                    for (int i = 0; i < length; i++) {
                        appendData(x == null ? i : x[i], y[i], error == null ? 0.0 : error[i], errorY == null ? 0.0 : errorY[i]);
                    }
            }
        }
    }
}
