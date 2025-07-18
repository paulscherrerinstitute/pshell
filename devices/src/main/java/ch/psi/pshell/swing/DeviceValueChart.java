package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotPanel;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 *
 */
public class DeviceValueChart extends DevicePanel {

    JPanel chart;
    ScheduledExecutorService scheduler;
    final JCheckBoxMenuItem menuAsyncUpdates;
    final JCheckBoxMenuItem menuInvertScale;
    final JMenuItem menuInterval;
    PlotSeries series;

    public DeviceValueChart() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        super();
        setPreferredSize(new Dimension(480, 240));
        setLayout(new java.awt.BorderLayout());
        menuInterval = new JMenuItem("Change interval");
        menuInterval.addActionListener((ActionEvent e) -> {
            try {
                String str = getString("Enter update interval in milliseconds (0 to disable):", String.valueOf(DeviceValueChart.this.getTimerInteval()));
                if (str != null) {
                    setInterval(Integer.valueOf(str));
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
        menuAsyncUpdates = new JCheckBoxMenuItem("Asynchronous updates");
        menuAsyncUpdates.addActionListener((ActionEvent e) -> {
            try {
                setAsyncUpdates(menuAsyncUpdates.isSelected());
            } catch (Exception ex) {
                showException(ex);
            }
        });
        menuInvertScale = new JCheckBoxMenuItem("Inverted scale");
        menuInvertScale.addActionListener((ActionEvent e) -> {
            try {
                setInvertedScale(menuInvertScale.isSelected());
            } catch (Exception ex) {
                showException(ex);
            }
        });
        menuInvertScale.setSelected(true);
    }

    boolean asyncUpdates;

    public void setAsyncUpdates(boolean value) {
        if (asyncUpdates != value) {
            asyncUpdates = value;
            menuAsyncUpdates.setSelected(asyncUpdates);
        }
        if (chart instanceof  HistoryChart historyChart){
            historyChart.setAsyncUpdates(value);
        }
    }

    public boolean getAsyncUpdates() {
        return asyncUpdates;
    }

    boolean invertedScale = true;

    public void setInvertedScale(boolean value) {
        if (invertedScale != value) {
            invertedScale = value;
            menuInvertScale.setSelected(invertedScale);
            if (chart instanceof MatrixPlotBase matrixPlotBase) {
                matrixPlotBase.getAxis(Plot.AxisId.Y).setInverted(invertedScale);
            }
        }
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int value) {
        if (value <= 0) {
            stopTimer();
        } else {
            startTimer(value);
        }
    }

    public boolean getInvertedScale() {
        return invertedScale;
    }

    @Override
    public void setDevice(final Device device) {
        super.setDevice(device);
        this.removeAll();
        menuInvertScale.setVisible(false);
        if (device != null) {
            try {
                if (device instanceof ReadonlyRegisterArray) {
                    chart = (LinePlotBase) Plot.newPlot(PlotPanel.getLinePlotImpl());
                    series = new LinePlotSeries(device.getName());
                    ((LinePlotBase) chart).addSeries((LinePlotSeries) series);
                } else if (device instanceof ReadonlyRegisterMatrix readonlyRegisterMatrix) {
                    menuInvertScale.setVisible(true);
                    chart = (MatrixPlotBase) Plot.newPlot(PlotPanel.getMatrixPlotImpl());
                    ((MatrixPlotBase) chart).getAxis(Plot.AxisId.Y).setInverted(invertedScale);
                    ReadonlyRegisterMatrix reg = readonlyRegisterMatrix;
                    series = new MatrixPlotSeries(device.getName(),reg.getWidth(), reg.getHeight());
                    ((MatrixPlotBase) chart).addSeries((MatrixPlotSeries) series);
                    if (device instanceof ReadableCalibratedMatrix cm) {
                        double minBoundsX = cm.getCalibration().getValueX(0);
                        double maxBoundsX = cm.getCalibration().getValueX(reg.getWidth() - 1);
                        double minBoundsY = cm.getCalibration().getValueY(0);
                        double maxBoundsY = cm.getCalibration().getValueY(reg.getHeight() - 1);
                        ((MatrixPlotSeries) series).setRangeX(minBoundsX, maxBoundsX);
                        ((MatrixPlotSeries) series).setRangeY(minBoundsY, maxBoundsY);
                        ((MatrixPlotBase) chart).getAxis(Plot.AxisId.X).setRange(minBoundsX, maxBoundsX);
                        ((MatrixPlotBase) chart).getAxis(Plot.AxisId.Y).setRange(minBoundsY, maxBoundsY);
                    }

                } else {
                    chart = HistoryChart.create(device);    
                    ((HistoryChart) chart).setAsyncUpdates(getAsyncUpdates());
                }
                if (chart instanceof PlotBase plotBase) {
                    plotBase.setTitle(null);
                    plotBase.addPopupMenuItem(null);
                    plotBase.addPopupMenuItem(menuAsyncUpdates);
                    plotBase.addPopupMenuItem(menuInterval);
                    plotBase.addPopupMenuItem(menuInvertScale);
                    startTimer(1000, 0);
                }
                add(chart);
            } catch (Exception ex) {
                showException(ex);
            }
        }
    }
    
    public PlotBase getPlot(){
        if (chart!=null){
            if (chart instanceof PlotBase plotBase) {
                return plotBase;
            } else if (chart instanceof HistoryChart historyChart){
                return historyChart.getPlot();
            }
        }
        return null;
    }
    
    
    @Override
    protected void onDeviceValueChanged(Object value, Object former) {
        if (asyncUpdates) {
            onTimer();
        }
    }

    @Override
    protected void onTimer() {
        try {
            if (device instanceof ReadonlyRegisterArray) {
                double[] x = null;
                double[] y = (double[]) Convert.toDouble(device.take());
                if (y != null) {
                    if (device instanceof ReadableCalibratedArray ca) {
                        x = ca.getCalibration().getAxisX(y.length);
                    }
                }
                ((LinePlotSeries) series).setData(x, y);
            } else if (device instanceof ReadonlyRegisterMatrix) {
                double[][] z = (double[][]) Convert.toDouble(device.take());
                if (z != null) {
                    int[] shape = Arr.getShape(z);
                    double minBoundsX = 0;
                    double maxBoundsX = shape[1] - 1;
                    double minBoundsY = 0;
                    double maxBoundsY = shape[0] - 1;
                    if (device instanceof ReadableCalibratedMatrix cm) {
                        minBoundsX = cm.getCalibration().getValueX(0);
                        maxBoundsX = cm.getCalibration().getValueX(shape[1] - 1);
                        minBoundsY = cm.getCalibration().getValueY(0);
                        maxBoundsY = cm.getCalibration().getValueY(shape[0] - 1);
                    }
                    if (z.length != ((MatrixPlotSeries) series).getNumberOfBinsY()) {
                        ((MatrixPlotBase) chart).removeSeries(((MatrixPlotSeries) series));
                        ((MatrixPlotSeries) series).setNumberOfBinsY(shape[0]);
                        ((MatrixPlotSeries) series).setRangeY(minBoundsY, maxBoundsY);
                        ((MatrixPlotBase) chart).addSeries(((MatrixPlotSeries) series));
                        ((MatrixPlotBase) chart).getAxis(Plot.AxisId.Y).setRange(minBoundsY, maxBoundsY);
                    }
                    if (z[0].length != ((MatrixPlotSeries) series).getNumberOfBinsX()) {
                        ((MatrixPlotBase) chart).removeSeries(((MatrixPlotSeries) series));
                        ((MatrixPlotSeries) series).setNumberOfBinsX(shape[1]);
                        ((MatrixPlotSeries) series).setRangeX(minBoundsX, maxBoundsX);
                        ((MatrixPlotBase) chart).addSeries(((MatrixPlotSeries) series));
                        ((MatrixPlotBase) chart).getAxis(Plot.AxisId.X).setRange(minBoundsX, maxBoundsX);
                    }
                }
                ((MatrixPlotSeries) series).setData(z);
            }
        } catch (Exception ex) {
            getLogger().log(Level.FINER, null, ex);
        }
    }
}
