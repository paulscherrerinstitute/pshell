package ch.psi.pshell.imaging;

import ch.psi.pshell.imaging.ColormapSource.ColormapSourceConfig;
import ch.psi.utils.ArrayProperties;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotRenderer;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.RangeSelectionPlot;
import ch.psi.pshell.plot.RangeSelectionPlot.RangeSelectionPlotListener;
import ch.psi.utils.Convert;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;

/**
 *
 */
public class Histogram extends MonitoredPanel implements RendererListener, ImageListener {

    //double[] data = new double[]{1.0,1.0,1.0,1.0,1.0};
    LinePlotSeries series = new LinePlotSeries("Histogram");
    Object origin;
    final boolean autoSave;

    public Histogram() {
        this(false);
    }

    public Histogram(boolean autoSave) {
        initComponents();
        this.autoSave = autoSave;
        rangeSelectionPlot.getAxis(Plot.AxisId.X).setLabel(null);
        rangeSelectionPlot.getAxis(Plot.AxisId.Y).setLabel(null);
        rangeSelectionPlot.setManualSelectionEnabled(false);
        rangeSelectionPlot.setMultipleSelection(false);
        rangeSelectionPlot.addSeries(series);
        rangeSelectionPlot.setListener(new RangeSelectionPlotListener() {
            @Override
            public void onRangeAdded(RangeSelectionPlot.RangeSelection range) {
                if (origin != null) {
                    if (origin instanceof ColormapSource){
                        ColormapSource source = (ColormapSource) origin;
                        saveOriginalConfig(source.getConfig());
                        source.getConfig().colormapAutomatic = false;
                        source.getConfig().colormapMin = range.getMin();
                        source.getConfig().colormapMax = range.getMax();
                        source.refresh();
                        if (autoSave) {
                            try {
                                source.getConfig().save();
                            } catch (IOException ex) {
                                Logger.getLogger(Histogram.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        //So paused renderer is updated
                        if (renderer != null) {
                            renderer.setImage(source, source.getOutput(), source.getData());
                        }
                    } else if (origin instanceof MatrixPlotRenderer){
                        MatrixPlotRenderer plot = (MatrixPlotRenderer)origin;
                        plot.setScale(range.getMin(), range.getMax());
                        if (renderer != null) {
                            renderer.setImage(renderer, plot.getImage(), plot.getData());
                        }
                    }                    
                    rangeSelectionPlot.selectMarker(range);
                }

            }

            @Override
            public void onRangeChanged(RangeSelectionPlot.RangeSelection range) {
            }

            @Override
            public void onRangeRemoved(RangeSelectionPlot.RangeSelection range) {
                if (origin != null) {
                    if (origin instanceof ColormapSource){
                        ColormapSource source = (ColormapSource) origin;
                        saveOriginalConfig(source.getConfig());
                        source.getConfig().colormapAutomatic = false;
                        source.getConfig().colormapMin = Double.NaN;
                        source.getConfig().colormapMax = Double.NaN;
                        source.refresh();
                        if (autoSave) {
                            try {
                                source.getConfig().save();
                            } catch (IOException ex) {
                                Logger.getLogger(Histogram.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } else if (origin instanceof MatrixPlotRenderer){
                        MatrixPlotRenderer renderer = (MatrixPlotRenderer)origin;
                        renderer.setAutoScale();
                    }
                }
            }

            @Override
            public void onRangeSelected(RangeSelectionPlot.RangeSelection range) {
            }

            @Override
            public void onDataChanged() {
            }
        });
    }

    public long[] calculate(BufferedImage image) {
        if (image == null) {
            return null;
        }
        ImagePlus ip = new ImagePlus("Histogram", image);
        ip.deleteRoi();
        long[] histogram = calculate(ip, 256, 0.0, 256.0);
        return histogram;
    }

    public long[] calculate(Data data) {
        if (data == null) {
            return null;
        }
        ImageProcessor proc = null;
        Object array = data.getArray();
        int lenght = data.width * data.height;
        if (data.getType() == byte.class) {
            if (data.getLength() > lenght) {
                array = Arrays.copyOf((byte[]) array, lenght);
            }
            proc = new ByteProcessor(data.width, data.height, (byte[]) array);
        } else if (data.getType() == short.class) {
            if (data.getLength() > lenght) {
                array = Arrays.copyOf((short[]) array, lenght);
            }
            proc = new ShortProcessor(data.width, data.height, (short[]) array, null);
        } else {
            if (data.getType() == float.class) {
                if (data.getLength() > lenght) {
                    array = Arrays.copyOf((float[]) array, lenght);
                }
                proc = new FloatProcessor(data.width, data.height, (float[]) array);
            } else {
                array = (double[]) Convert.toDouble(array);
                if (data.getLength() > lenght) {
                    array = Arrays.copyOf((double[]) array, lenght);
                }
                proc = new FloatProcessor(data.width, data.height, (double[]) array);
            }
        }
        ArrayProperties props = data.getProperties();
        int hist_bins = 256;
        double hist_min = props.min;
        double hist_max = props.max;
        ImagePlus ip = new ImagePlus("Histogram", proc);
        long[] histogram = calculate(ip, hist_bins, hist_min, hist_max);
        return histogram;

    }

    public long[] calculate(ImagePlus ip, int hist_bins, double hist_min, double hist_max) {
        if (ip == null) {
            return null;
        }
        ImageStatistics is = ip.getStatistics(0, hist_bins, hist_min, hist_max);
        long[] histogram = is.getHistogram();
        double[] y = (double[]) Convert.toDouble(histogram);
        double[] x = new double[y.length];
        double stepSize = (hist_max - hist_min) / hist_bins;
        for (int i = 0; i < y.length; i++) {
            x[i] = hist_min + i * stepSize;
        }
        series.setData(x, y);
        return histogram;

    }

    public void clear() {
        series.clear();
        origin = null;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rangeSelectionPlot = new ch.psi.pshell.plot.RangeSelectionPlot();

        rangeSelectionPlot.setTitle("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(rangeSelectionPlot, javax.swing.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(rangeSelectionPlot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.plot.RangeSelectionPlot rangeSelectionPlot;
    // End of variables declaration//GEN-END:variables

    @Override
    public void onImage(Object origin, BufferedImage image, Data data) {
        this.origin = origin;
        boolean enabled = ((origin != null) && ((origin instanceof ColormapSource) || (origin instanceof MatrixPlotRenderer)));
        rangeSelectionPlot.setManualSelectionEnabled(enabled);
        if ((data != null) && (data.getSourceImage() == null)) {
            calculate(data);
        } else if (image != null) {
            calculate(image);
        } else {
            clear();
        }
    }

    @Override
    public void onError(Object origin, Exception ex) {
        clear();
    }

    @Override
    public void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
        onImage(origin, image, data);
    }

    @Override
    public void onError(Renderer renderer, Object origin, Exception ex) {
        onError(origin, ex);
    }

    @Override
    protected void onShow() {
        if (renderer != null) {
            renderer.addListener(this);
            onImage(renderer, renderer.currentOrigin, renderer.currentImage, renderer.currentData);
        }
        if (source != null) {
            source.addListener(this);
            onImage(null, source.getOutput(), source.getData());
        }
    }

    @Override
    protected void onHide() {
        if (renderer != null) {
            renderer.removeListener(this);
        }
        if (source != null) {
            source.removeListener(this);
        }
        if ((origin != null) && (origin instanceof ColormapSource)) {
            ColormapSource source = (ColormapSource) origin;
            if (originalConfig != null) {
                if (SwingUtils.showOption(getTopLevelAncestor().isShowing() ? getTopLevelAncestor() : getTopLevelAncestor().getParent(), "Close",
                        "Save new colormap range?", OptionType.YesNo) == OptionResult.Yes) {
                    try {
                        source.getConfig().save();
                    } catch (IOException ex) {
                        Logger.getLogger(Histogram.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    source.getConfig().colormapAutomatic = (Boolean) originalConfig.get("colormapAutomatic");
                    source.getConfig().colormapMin = (Double) originalConfig.get("colormapMin");
                    source.getConfig().colormapMax = (Double) originalConfig.get("colormapMax");
                    source.refresh();
                }
            }
        }
    }

    HashMap originalConfig;

    void saveOriginalConfig(ColormapSourceConfig config) {
        if (!autoSave) {
            if (originalConfig == null) {
                originalConfig = new HashMap();
                try {
                    originalConfig.put("colormapAutomatic", config.colormapAutomatic);
                    originalConfig.put("colormapMin", config.colormapMin);
                    originalConfig.put("colormapMax", config.colormapMax);
                } catch (Exception ex) {
                }
            }
        }
    }

    Renderer renderer;

    public void setRenderer(Renderer renderer) {
        if (isVisible()) {
            onHide();
        }
        this.source = null;
        this.renderer = renderer;
        if (isVisible()) {
            onShow();
        }                
        originalConfig = null;
    }

    Source source;

    public void setSource(Source source) {
        if (isVisible()) {
            onHide();
        }
        this.renderer = null;
        this.source = source;
        if (isVisible()) {
            onShow();
        }
        originalConfig = null;
    }
    
    public static Histogram create(Renderer renderer){
        return create(renderer, false);
    }
            
    public static Histogram create(Renderer renderer, boolean autoSave){
        Histogram histogram = new Histogram(autoSave);
        histogram.setRenderer(renderer);
        JDialog dlg = SwingUtils.showDialog(SwingUtils.getWindow(renderer), "Histogram", null, histogram);
        renderer.refresh();

        renderer.addHierarchyListener((e)->{
            if (!renderer.isShowing()){
                dlg.setVisible(false);
                dlg.dispose();
            }
        });        
        return histogram;
    }
}
