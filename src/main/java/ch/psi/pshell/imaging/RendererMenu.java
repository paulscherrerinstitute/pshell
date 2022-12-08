package ch.psi.pshell.imaging;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.ManualScaleDialog;
import ch.psi.pshell.plot.ManualScaleDialog.ScaleChangeListener;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.MatrixPlotJFree;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.utils.IO;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.ImageTransferHandler;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.WindowConstants;

/**
 *
 */
public class RendererMenu extends JPopupMenu {

    final Renderer renderer;
    JMenu menuMode;
    JMenu menuZoom;
    JMenu menuProfile;
    JCheckBoxMenuItem menuPause;
    JCheckBoxMenuItem menuStatus;

    RendererMenu(final Renderer renderer) {        
        this.renderer = renderer;
        menuPause = new JCheckBoxMenuItem("Pause");
        menuStatus = new JCheckBoxMenuItem("Status Bar");

        menuPause.addActionListener((ActionEvent e) -> {
            if (renderer.isPaused() != menuPause.isSelected()) {
                if (menuPause.isSelected()) {
                    renderer.pause();
                } else {
                    renderer.resume();
                }
            }
        });

        menuStatus.addActionListener((ActionEvent e) -> {
            if (renderer.getShowStatus() != menuStatus.isSelected()) {
                renderer.setShowStatus(menuStatus.isSelected());
            }
        });

        menuMode = new JMenu("Mode");
        for (RendererMode mode
                : RendererMode.values()) {
            JMenuItem item = new JCheckBoxMenuItem(mode.toString());
            item.addActionListener((ActionEvent e) -> {
                renderer.abortSelection();
                RendererMode mode1 = RendererMode.valueOf(e.getActionCommand());
                renderer.setMode(mode1);
            });
            menuMode.add(item);
        }

        menuZoom = new JMenu("Zoom");
        for (Double zoom
                : new Double[]{0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0}) {
            JMenuItem item = new JCheckBoxMenuItem(zoom.toString());
            item.addActionListener((ActionEvent e) -> {
                renderer.abortSelection();
                Double zoom1 = Double.valueOf(e.getActionCommand());
                renderer.setZoom(zoom1);
            });
            menuZoom.add(item);
        }

        JMenuItem menuZoomTo = new JMenuItem("Zoom To...");
        menuZoomTo.addActionListener((ActionEvent e) -> {
            renderer.abortSelection();
            onZoomTo();
        });

        JMenuItem menuResetZoom = new JMenuItem("Reset Zoom");
        menuResetZoom.addActionListener((ActionEvent e) -> {
            renderer.abortSelection();
            renderer.resetZoom();
        });

        JMenu menuIntegration = new JMenu("Integration");
        JMenuItem menuIntegrationHorizontal = new JMenuItem("Horizontal");
        menuIntegrationHorizontal.addActionListener((ActionEvent e) -> {
            renderer.addIntegration(false);
        });
        JMenuItem menuIntegrationVertical = new JMenuItem("Vertical");
        menuIntegrationVertical.addActionListener((ActionEvent e) -> {
            renderer.addIntegration(true);
        });

        menuIntegration.add(menuIntegrationHorizontal);
        menuIntegration.add(menuIntegrationVertical);

        JMenu menuSelection = new JMenu("Data Selection");
        JMenuItem menuClear = new JMenuItem("Clear");
        menuClear.addActionListener((ActionEvent e) -> {
            renderer.removeDataSelection();
        });
        menuSelection.add(menuClear);
        menuSelection.addSeparator();
        for (Renderer.SelectionType type : Renderer.SelectionType.values()) {
            JMenuItem item = new JMenuItem(type.toString());
            item.addActionListener((ActionEvent e) -> {
                renderer.startDataSelection(Renderer.SelectionType.valueOf(e.getActionCommand()));
            });
            menuSelection.add(item);
        }
        JMenuItem menuHistogram = new JMenuItem("Histogram");
        menuHistogram.addActionListener((ActionEvent e) -> {
            Histogram.create(renderer);
        });
        JMenu menuColormap = new JMenu("Colormap");
        for (Colormap c : Colormap.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(c.toString());
            item.addActionListener((ActionEvent e) -> {
                try {
                    Object origin = renderer.getOrigin();
                    if ((origin != null) && (origin instanceof ColormapSource)) {
                        ColormapSource source = (ColormapSource) origin;
                        source.getConfig().colormap = Colormap.valueOf(item.getText());
                        source.getConfig().save();
                        source.refresh();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            menuColormap.add(item);
        }
        menuColormap.addSeparator();
        JCheckBoxMenuItem menuLogarithmic = new JCheckBoxMenuItem("Logarithmic");
        menuLogarithmic.addActionListener((ActionEvent e) -> {
            try {
                Object origin = renderer.getOrigin();
                if ((origin != null) && (origin instanceof ColormapSource)) {
                    ColormapSource source = (ColormapSource) origin;
                    source.getConfig().colormapLogarithmic = menuLogarithmic.isSelected();
                    source.getConfig().save();
                    source.refresh();
                }
            } catch (IOException ex) {
                Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        JRadioButtonMenuItem menuAutoScale = new JRadioButtonMenuItem("Automatic");
        menuAutoScale.addActionListener((ActionEvent e) -> {
            try {
                Object origin = renderer.getOrigin();
                if ((origin != null) && (origin instanceof ColormapSource)) {
                    ColormapSource source = (ColormapSource) origin;
                    source.getConfig().colormapAutomatic = true;
                    source.getConfig().save();
                    source.refresh();
                }
            } catch (IOException ex) {
                Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        JRadioButtonMenuItem menuManualScale = new JRadioButtonMenuItem("Manual");
        menuManualScale.addActionListener((ActionEvent e) -> {
            try {
                Object origin = renderer.getOrigin();
                if ((origin != null) && (origin instanceof ColormapSource)) {
                    ColormapSource source = (ColormapSource) origin;
                    ManualScaleDialog d = new ManualScaleDialog();
                    d.setLocationRelativeTo(d);
                    SwingUtils.centerComponent(renderer.getTopLevelAncestor(), d);
                    Double low = source.getConfig().colormapMin;
                    Double high = source.getConfig().colormapMax;
                    Boolean auto = source.getConfig().colormapAutomatic;
                    d.setLow(low);
                    d.setHigh(high);
                    d.setScaleChangeListener(new ScaleChangeListener() {
                        @Override
                        public void setScale(double scaleMin, double scaleMax) {
                            source.getConfig().colormapAutomatic = false;
                            source.getConfig().colormapMin = d.getLow();
                            source.getConfig().colormapMax = d.getHigh();
                            source.refresh();
                        }
                    });
                    d.showDialog();
                    if (d.getSelectedOption() == JOptionPane.OK_OPTION) {
                        source.getConfig().colormapAutomatic = false;
                        source.getConfig().colormapMin = d.getLow();
                        source.getConfig().colormapMax = d.getHigh();
                        source.getConfig().save();
                        source.refresh();
                    } else {
                        source.getConfig().colormapAutomatic = auto;
                        source.getConfig().colormapMin = low;
                        source.getConfig().colormapMax = high;
                        source.refresh();
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        JCheckBoxMenuItem menuShowScale = new JCheckBoxMenuItem("Show Scale");
        
        menuShowScale.addActionListener((e)->{
            try {
                renderer.setShowColormapScale(menuShowScale.isSelected());
            } catch (Exception ex) {
                Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
            }            
        });          
        
        JMenu menuScale = new JMenu("Scale");
        menuScale.add(menuAutoScale);
        menuScale.add(menuManualScale);
        menuColormap.add(menuScale);
        menuColormap.add(menuLogarithmic);
        menuColormap.addSeparator();
        menuColormap.add(menuShowScale);      
        
        JMenuItem menuPlot = new JMenuItem("Detach Plot");
        menuPlot.addActionListener((ActionEvent e) -> {
            Data data = renderer.getData();
            MatrixPlotJFree plot = new MatrixPlotJFree();
            plot.getAxis(Plot.AxisId.Y).setInverted(true);
            double[][] matrix = data.getRectSelection(null, true);
            //Dimension size = renderer.getImageSize();
            if ((matrix == null) || (matrix.length == 0)) {
                return;
            }
            Dimension size = new Dimension(matrix[0].length, matrix.length);
            MatrixPlotSeries series = new MatrixPlotSeries("Image Plot", size);
            PlotPanel.addSurfacePlotMenu((MatrixPlotBase) plot);
            plot.setTitle(null);
            plot.addSeries(series);
            plot.setQuality(PlotPanel.getQuality());

            double minBoundsX = data.getX(0);
            double maxBoundsX = data.getX(size.width);
            double minBoundsY = data.getY(0);
            double maxBoundsY = data.getY(size.height);
            ((MatrixPlotSeries) series).setRangeX(minBoundsX, maxBoundsX);
            ((MatrixPlotSeries) series).setRangeY(minBoundsY, maxBoundsY);
            plot.getAxis(Plot.AxisId.X).setRange(minBoundsX, maxBoundsX);
            plot.getAxis(Plot.AxisId.Y).setRange(minBoundsY, maxBoundsY);
            ((MatrixPlotSeries) series).setData(matrix);

            JDialog dialog = new JDialog((Window) renderer.getTopLevelAncestor(), "Image Plot");
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.setSize(480, 320);
            SwingUtils.centerComponent(renderer.getTopLevelAncestor(), dialog);
            dialog.getContentPane().add(plot);
            dialog.setVisible(true);
            renderer.painter.requestFocus();
        });

        menuProfile = new JMenu("Profile");
        for (Renderer.Profile profile : Renderer.Profile.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(profile.toString());
            item.addActionListener((ActionEvent e) -> {
                renderer.setProfile(Renderer.Profile.valueOf(e.getActionCommand()));
            });
            menuProfile.add(item);
        }
        JCheckBoxMenuItem menuReticle = new JCheckBoxMenuItem("Reticle");
        menuReticle.addActionListener((ActionEvent e) -> {
            renderer.setShowReticle(menuReticle.isSelected());
        });

        JCheckBoxMenuItem menuMeasure = new JCheckBoxMenuItem("Measure Tool");
        menuMeasure.addActionListener((ActionEvent e) -> {
            renderer.setMeasureToolVisible(menuMeasure.isSelected());
        });

        JMenu menuMarker = new JMenu("Marker");
        JMenuItem menuClearMarker = new JMenuItem("Clear");
        menuClearMarker.addActionListener((ActionEvent e) -> {
            renderer.setMarker(null);
        });
        menuMarker.add(menuClearMarker);
        menuMarker.addSeparator();
        JMenuItem markerCross = new JMenuItem("Cross");
        JMenuItem markerCrosshairs = new JMenuItem("Crosshairs");
        ActionListener markerListener = (ActionEvent e) -> {
            Dimension d = renderer.getImageSize();
            Point p = (d == null) ? new Point(renderer.getWidth() / 2, renderer.getHeight() / 2) : new Point(d.width / 2, d.height / 2);
            Overlay ov = null;
            if (e.getActionCommand().equals("Crosshairs")) {
                ov = new Overlays.Crosshairs(renderer.getPenMarker(), p, new Dimension(-1, -1));
            } else {
                ov = new Overlays.Crosshairs(renderer.getPenMarker(), p, new Dimension(40, 40));
            }
            ov.setMovable(true);
            ov.setPassive(false);
            renderer.setMarker(ov);
        };
        markerCross.addActionListener(markerListener);
        markerCrosshairs.addActionListener(markerListener);
        menuMarker.add(markerCross);
        menuMarker.add(markerCrosshairs);

        final JMenuItem menuCopy = new JMenuItem("Copy");
        final JMenuItem menuCopyWithOverlays = new JMenuItem("Copy with Overlays");
        ActionListener al = ((ActionEvent e) -> {
            try {
                BufferedImage img = renderer.getImage(e.getActionCommand().equals(menuCopyWithOverlays.getText()));
                if (img != null) {
                    ImageTransferHandler imageSelection = new ImageTransferHandler(img);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(imageSelection, (Clipboard clipboard1, Transferable contents) -> {
                    });
                }
            } catch (Exception ex) {
                SwingUtils.showException(renderer, ex);
            }
        });
        menuCopy.addActionListener(al);
        menuCopyWithOverlays.addActionListener(al);

        JMenuItem menuSaveSnapshot = new JMenuItem("Save Snapshot...");
        menuSaveSnapshot.addActionListener((ActionEvent e) -> {
            try {

                JFileChooser chooser = new JFileChooser(Utils.getSelectedImageFolder());
                JPanel panel = new JPanel();
                JCheckBox overlays = new JCheckBox("Overlays");
                panel.add(overlays);
                chooser.setAccessory(panel);
                chooser.addChoosableFileFilter(new ExtensionFileFilter("PNG files (*.png)", new String[]{"png"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("Bitmap files (*.bmp)", new String[]{"bmp"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("GIF files (*.gif)", new String[]{"gif"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("TIFF files (*.tif)", new String[]{"tif", "tiff"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("JPEG files (*.jpg)", new String[]{"jpg", "jpeg"}));
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showSaveDialog(renderer) == JFileChooser.APPROVE_OPTION) {
                    Utils.setSelectedImageFolder(chooser.getSelectedFile().getParent());
                    String filename = chooser.getSelectedFile().getAbsolutePath();
                    String type = "png";
                    String ext = IO.getExtension(chooser.getSelectedFile());
                    for (String fe : new String[]{"bmp", "jpg", "tif", "gif"}) {
                        if ((chooser.getFileFilter().getDescription().contains(fe))
                                || (fe.equals(ext))) {
                            type = fe;
                            break;
                        }
                    }
                    if ((ext == null) || (ext.isEmpty())) {
                        filename += "." + type;
                    }
                    if (new File(filename).exists()) {
                        if (SwingUtils.showOption(renderer, "Overwrite", "File " + filename + " already exists.\nDo you want to overwrite it?", SwingUtils.OptionType.YesNo) == SwingUtils.OptionResult.No) {
                            return;
                        }
                    }
                    renderer.saveSnapshot(filename, type, overlays.isSelected());
                }
            } catch (Exception ex) {
                SwingUtils.showException(renderer, ex);
            }

        });

        JCheckBoxMenuItem menuSnapshotDialog = new JCheckBoxMenuItem("Snapshot Dialog");
        menuSnapshotDialog.addActionListener((ActionEvent e) -> {
            try {
                renderer.setSnapshotDialogVisible(menuSnapshotDialog.isSelected());
            } catch (Exception ex) {
                SwingUtils.showException(renderer, ex);
            }
        });

        add(menuPause);
        addSeparator();
        add(menuMode);
        add(menuZoom);
        add(menuZoomTo);
        add(menuResetZoom);
        addSeparator();
        add(menuHistogram);
        add(menuColormap);
        add(menuProfile);
        add(menuReticle);
        add(menuMeasure);
        add(menuMarker);
        add(menuIntegration);
        add(menuSelection);
        add(menuStatus);
        addSeparator();
        add(menuCopy);
        add(menuCopyWithOverlays);
        add(menuSaveSnapshot);
        add(menuSnapshotDialog);
        add(menuPlot);        

        addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                RendererMode mode = renderer.getMode();
                for (int i = 0; i < menuMode.getItemCount(); i++) {
                    menuMode.getItem(i).setSelected(mode.toString().equals(menuMode.getItem(i).getText()));
                }
                Double zoom = renderer.getZoom();
                for (int i = 0; i < menuZoom.getItemCount(); i++) {
                    menuZoom.getItem(i).setSelected(zoom.toString().equals(menuZoom.getItem(i).getText()));
                }
                for (Component item : menuProfile.getMenuComponents()) {
                    ((JRadioButtonMenuItem) item).setSelected(((JRadioButtonMenuItem) item).getText().equals(renderer.getProfile().toString()));
                }
                menuReticle.setVisible(renderer.getCalibration() != null);
                menuReticle.setSelected(renderer.getShowReticle());
                menuMeasure.setSelected(renderer.isMeasureToolVisible());
                menuHistogram.setEnabled(renderer.getImage() != null);
                menuSnapshotDialog.setSelected(renderer.isSnapshotDialogVisible());
                Object origin = renderer.getOrigin();
                boolean hasColormap = (renderer.getImage() != null) && (origin != null) && (origin instanceof ColormapSource);
                menuColormap.setVisible(hasColormap);
                if (hasColormap) {
                    ColormapSource source = (ColormapSource) origin;
                    boolean autoColormap = source.getConfig().colormapAutomatic;
                    menuAutoScale.setSelected(autoColormap);
                    menuManualScale.setSelected(!autoColormap);
                    for (Component c : menuColormap.getMenuComponents()) {
                        if (c instanceof JRadioButtonMenuItem) {
                            ((JRadioButtonMenuItem) c).setSelected(source.getConfig().colormap == Colormap.valueOf(((JMenuItem) c).getText()));
                        } else if (c==menuLogarithmic) {
                            ((JCheckBoxMenuItem) c).setSelected(source.getConfig().colormapLogarithmic);
                        } else if (c==menuShowScale) {
                            ((JCheckBoxMenuItem) c).setSelected(renderer.getShowColormapScale());
                        }
                    }

                }

                menuStatus.setSelected(renderer.getShowStatus());
                menuPause.setSelected(renderer.isPaused());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    protected void onZoomTo() {
        final Overlays.Rect selection = new Overlays.Rect(renderer.getPenMouseSelecting());
        renderer.addListener(new RendererListener() {
            @Override
            public void onSelectionFinished(Renderer renderer, Overlay overlay) {
                try {
                    renderer.zoomTo(overlay.isFixed() ? renderer.toImageCoord(overlay.getBounds()) : overlay.getBounds());
                } catch (Exception ex) {
                } finally {
                    renderer.removeListener(this);
                }
            }

            @Override
            public void onSelectionAborted(Renderer renderer, Overlay overlay) {
                renderer.removeListener(this);
            }
        });
        selection.setFixed(true);
        renderer.startSelection(selection);
    }

    //Data selection plots
    Class DEFAULT_PLOT_IMPL_LINE = ch.psi.pshell.plot.LinePlotJFree.class;
    Class DEFAULT_PLOT_IMPL_MATRIX = ch.psi.pshell.plot.MatrixPlotJFree.class;
    Quality DEFAULT_PLOT_QUALITY = Quality.High;

    public String getLinePlotImpl() {
        String impl = System.getProperty(PlotPanel.PROPERTY_PLOT_IMPL_LINE);
        return (impl == null) ? DEFAULT_PLOT_IMPL_LINE.getName() : impl;
    }

    public String getMatrixPlotImpl() {
        String impl = System.getProperty(PlotPanel.PROPERTY_PLOT_IMPL_MATRIX);
        return (impl == null) ? DEFAULT_PLOT_IMPL_MATRIX.getName() : impl;
    }

    public String getSurfacePlotImpl() {
        String impl = System.getProperty(PlotPanel.PROPERTY_PLOT_IMPL_SURFACE);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? null : impl;
    }

}
