package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.RendererMode;
import ch.psi.utils.Convert;
import java.awt.BorderLayout;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 */
public class MatrixPlotRenderer extends MatrixPlotBase {

    Renderer renderer;
    BufferedImage image;
    DataBufferDouble dataBuffer;
    ColorModel cmGray;

    public MatrixPlotRenderer() {
        super();
        getRenderer().getPopupMenu().setVisible(false); //Workaroung to menu menu poping up when ServiceLoader.load(MatrixPlot.class) was called
        setRequireUpdateOnAppend(false);
        getRenderer().setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        cmGray = new ComponentColorModel(csGray, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_DOUBLE);
        setLayout(new BorderLayout());
        add(getRenderer());
    }

    private Renderer getRenderer() {
        if (renderer == null) {
            renderer = new Renderer();
            renderer.setMode(RendererMode.Fixed);
        }
        return renderer;
    }

    @Override
    protected void createPopupMenu() {
        // Update colormap
        JRadioButtonMenuItem popupMenuAutoScale = new JRadioButtonMenuItem("Auto");
        popupMenuAutoScale.addActionListener((ActionEvent e) -> {
            setAutoScale();
        });

        // Set color colormap
        JRadioButtonMenuItem popupMenuManualScale = new JRadioButtonMenuItem("Manual");
        popupMenuManualScale.addActionListener((ActionEvent e) -> {
            MatrixPlotJFree.ManualScaleDialog d = new MatrixPlotJFree.ManualScaleDialog();
            d.setLocationRelativeTo(renderer);
            d.setLow(Double.isNaN(scaleMin) ? 0.0 : scaleMin);
            d.setHigh(Double.isNaN(scaleMax) ? 1.0 : scaleMax);
            d.setMatrixPlot(this);
            d.showDialog();
            if (d.getSelectedOption() == JOptionPane.OK_OPTION) {
                setScale(d.getLow(), d.getHigh());
            }
        });

        // Group colormap related menu items
        JMenu poopupMenuChooseColorMap = new JMenu("Scale");
        poopupMenuChooseColorMap.add(popupMenuAutoScale);
        poopupMenuChooseColorMap.add(popupMenuManualScale);
        addPopupMenuItem(poopupMenuChooseColorMap);

        getRenderer().getPopupMenu().addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                popupMenuManualScale.setSelected(!isAutoScale());
                popupMenuAutoScale.setSelected(isAutoScale());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        super.createPopupMenu();

    }

    @Override
    public void setScale(double scaleMin, double scaleMax) {
        super.setScale(scaleMin, scaleMax);
        update(true);
    }

    @Override
    public void setAutoScale() {
        super.setAutoScale();
        update(true);
    }

    private boolean grayScale = true;

    public void setGrayScale(boolean value) {
        if (value != grayScale) {
            grayScale = value;
        }
    }

    public boolean isGrayScale() {
        return grayScale;
    }

    ColorSpace csGray = new ColorSpace(ColorSpace.TYPE_GRAY, 1) {
        @Override
        public float[] toRGB(float[] colorvalue) {
            float val = (colorvalue[0] - (float) scaleMin) / (float) scaleRange;
            return new float[]{(val < 0.0f ? 0.0f : (val > 1.0f ? 1.0f : val))};
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

    @Override
    protected void onAppendData(MatrixPlotSeries series, int indexX, int indexY, double x, double y, double z) {
        if (dataBuffer == null) {
            int width = series.getNumberOfBinsX();
            int height = series.getNumberOfBinsY();
            dataBuffer = new DataBufferDouble(width * height);
            //sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, width, height, 1);
            SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, width, height, 1, width, new int[]{0});
            WritableRaster wr = WritableRaster.createWritableRaster(sm, dataBuffer, null);
            image = new BufferedImage(cmGray, wr, true, new Hashtable<Object, Object>());
            if (autoScale) {
                updateScale(Double.MAX_VALUE, Double.MIN_VALUE);
            }
            requestSeriesUpdate(series);
        }
        int index = indexY * series.getNumberOfBinsX() + indexX;
        dataBuffer.setElemDouble(index, z);
        if (autoScale) {
            if (z < scaleMin) {
                updateScale(z, scaleMax);
            } else if (z > scaleMax) {
                updateScale(scaleMin, z);
            }
        }
    }

    @Override
    protected void onSetData(MatrixPlotSeries series, double[][] data, double[][] xdata, double[][] ydata) {
        int width = data[0].length;
        int height = data.length;
        dataBuffer = new DataBufferDouble((double[]) Convert.flatten(data), width);
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, width, height, 1, width, new int[]{0});
        WritableRaster wr = WritableRaster.createWritableRaster(sm, dataBuffer, null);
        image = new BufferedImage(cmGray, wr, true, new Hashtable<Object, Object>());

        if (autoScale) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (double[] row : data) {
                for (double v : row) {
                    if (v < min) {
                        min = v;
                    }
                    if (v > max) {
                        max = v;
                    }
                }
            }
            updateScale(min, max);
        }
    }

    @Override
    public void addPopupMenuItem(JMenuItem item) {
        if (item == null) {
            getRenderer().getPopupMenu().addSeparator();
        } else {
            getRenderer().getPopupMenu().add(item);
        }
    }

    @Override
    protected Object onAddedSeries(MatrixPlotSeries series) {
        dataBuffer = null;
        return dataBuffer;
    }

    @Override
    protected void onRemovedSeries(MatrixPlotSeries series) {
        dataBuffer = null;
        doUpdate();
    }

    @Override
    public void updateSeries(MatrixPlotSeries series) {
        renderer.onImage(series, image, null);
    }

    @Override
    public double[][] getSeriesData(MatrixPlotSeries series) {
        if (dataBuffer == null) {
            return null;
        }
        int width = series.getNumberOfBinsX();
        int height = series.getNumberOfBinsY();

        return (double[][]) Convert.reshape(dataBuffer.getBankData()[0],height, width);
    }
}
