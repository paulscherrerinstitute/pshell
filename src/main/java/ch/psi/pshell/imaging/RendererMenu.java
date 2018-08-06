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
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.MatrixPlotJFree;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.ImageTransferHandler;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
    Overlay selectionOverlay;
    SelectionType selectionType;
    SelectionType integrationType;

    static String imagesFolderName;

    public static void setImageFileFolder(String folderName) {
        imagesFolderName = folderName;
    }

    public static String getImageFileFolder() {
        return imagesFolderName;
    }

    protected enum SelectionType {

        Line,
        Horizontal,
        Vertical,
        Rectangle
    }

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
            onIntegration(SelectionType.Horizontal);
        });
        JMenuItem menuIntegrationVertical = new JMenuItem("Vertical");
        menuIntegrationVertical.addActionListener((ActionEvent e) -> {
            onIntegration(SelectionType.Vertical);
        });

        menuIntegration.add(menuIntegrationHorizontal);
        menuIntegration.add(menuIntegrationVertical);

        JMenu menuSelection = new JMenu("Data Selection");
        JMenuItem menuClear = new JMenuItem("Clear");
        menuClear.addActionListener((ActionEvent e) -> {
            removeDataSelection();
            removeDataSelectionDialog();
        });
        menuSelection.add(menuClear);
        menuSelection.addSeparator();
        for (SelectionType type : SelectionType.values()) {
            JMenuItem item = new JMenuItem(type.toString());
            item.addActionListener((ActionEvent e) -> {
                onSelection(SelectionType.valueOf(e.getActionCommand()));
            });
            menuSelection.add(item);
        }
        JMenuItem menuHistogram = new JMenuItem("Histogram");
        menuHistogram.addActionListener((ActionEvent e) -> {
            Histogram histogram = new Histogram();
            histogram.setRenderer(renderer);
            JDialog dlg = SwingUtils.showDialog(SwingUtils.getWindow(renderer), "Histogram", null, histogram);
            renderer.refresh();
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
        menuColormap.add(menuLogarithmic);

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
            MatrixPlotSeries series = new MatrixPlotSeries("Image Plot", 0, size.width - 1, size.width, 0, size.height - 1, size.height);
            PlotPanel.addSurfacePlotMenu((MatrixPlotBase) plot);
            plot.setTitle(null);
            plot.addSeries(series);
            plot.setQuality(getQuality());

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

                JFileChooser chooser = new JFileChooser(imagesFolderName);
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
        setVisible(true);

        this.addPopupMenuListener(new PopupMenuListener() {

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
                    for (Component c : menuColormap.getMenuComponents()) {
                        if (c instanceof JRadioButtonMenuItem) {
                            ((JRadioButtonMenuItem) c).setSelected(source.getConfig().colormap == Colormap.valueOf(((JMenuItem) c).getText()));
                        } else if (c instanceof JCheckBoxMenuItem) {
                            ((JCheckBoxMenuItem) c).setSelected(source.getConfig().colormapLogarithmic);
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

        selectionListener = new RendererListener() {
            @Override
            public void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
                if (selectionOverlay != null) {
                    updatePlotData();
                }
            }

            @Override
            public void onError(Renderer renderer, Object origin, Exception ex) {
                if (selectionOverlay != null) {
                    cleanPlot();
                }
            }

            @Override
            public void onMoveFinished(Renderer renderer, Overlay overlay) {
                if (overlay == selectionOverlay) {
                    updatePlotTitle();
                    updatePlotData();
                }
            }

            @Override
            public void onDeleted(Renderer renderer, Overlay overlay) {
                if (overlay == selectionOverlay) {
                    removeDataSelection();
                    removeDataSelectionDialog();
                }
            }

        };

    }

    RendererListener integrationListener = new RendererListener() {
        @Override
        public void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
            updatePlotData();
        }
    };

    Pen getPenMouseSelecting() {
        return new Pen(renderer.penSelectedOverlay.getColor(), 1, Pen.LineStyle.dotted);
    }

    Pen getPenDataSelection() {
        return new Pen(renderer.penSelectedOverlay.getColor());
    }

    protected void onZoomTo() {
        final Overlays.Rect selection = new Overlays.Rect(getPenMouseSelecting());
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

    protected void onIntegration(SelectionType type) {
        removeDataSelection();
        selectionType = null;
        integrationType = type;
        addDataSelectionDialog();
        renderer.addListener(integrationListener);
        dialogPlot.setTitle(type.toString() + " Integration");
    }

    protected void removeIntegration() {
        renderer.removeListener(integrationListener);
        integrationType = null;
    }

    protected void onSelection(SelectionType type) {
        Overlay selection = null;
        selectionType = type;
        removeIntegration();
        switch (selectionType) {
            case Line:
                selection = new Overlays.Arrow(getPenMouseSelecting());
                ((Overlays.Arrow) selection).setArrowType(Overlays.Arrow.ArrowType.end);
                //selection = new Overlays.Line(PEN_MOVING_OVERLAY);
                break;
            case Horizontal:
                selection = new Overlays.Crosshairs(getPenMouseSelecting(), new Dimension(-1, 1));
                break;
            case Vertical:
                selection = new Overlays.Crosshairs(getPenMouseSelecting(), new Dimension(1, -1));
                break;
            case Rectangle:
                selection = new Overlays.Rect(getPenMouseSelecting());
                //selection.setSolid(true);
                break;
        }
        if (selection != null) {
            startSelection(selection);
        }
    }

    protected void startSelection(Overlay overlay) {
        removeDataSelection();
        renderer.addListener(new RendererListener() {
            @Override
            public void onSelectionFinished(Renderer renderer, Overlay overlay) {
                try {
                    if (overlay.getLength() > 0) {
                        Overlay dataSelection = overlay.copy();
                        dataSelection.setPen(getPenDataSelection());
                        addDataSelection(dataSelection);
                        addDataSelectionDialog();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    renderer.removeListener(this);
                }
            }

            @Override
            public void onSelectionAborted(Renderer renderer, Overlay overlay) {
                renderer.removeListener(this);
                removeDataSelectionDialog();
            }
        });
        overlay.setPassive(false);
        renderer.startSelection(overlay);
    }

    protected void addDataSelection(Overlay overlay) {
        removeDataSelection();
        selectionOverlay = overlay;
        selectionOverlay.setSelectable(true);
        selectionOverlay.setMovable(true);
        renderer.addOverlay(selectionOverlay);
        renderer.addListener(selectionListener);
    }

    protected void removeDataSelection() {
        renderer.abortSelection();
        renderer.removeListener(selectionListener);
        if (selectionOverlay != null) {
            renderer.removeOverlay(selectionOverlay);
            selectionOverlay = null;
        }
    }

    RendererListener selectionListener;

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

    public Quality getQuality() {
        return PlotPanel.getQuality();
    }

    protected void addDataSelectionDialog() {
        if (dialogPlot == null) {
            dialogPlot = new JDialog((Window) renderer.getTopLevelAncestor(), "Data Selection");
            dialogPlot.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialogPlot.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    removeDataSelection();
                    removeIntegration();
                    selectionType = null;
                }
            });
            dialogPlot.getContentPane().setLayout(new BorderLayout());
            dialogPlot.setSize(480, 320);
            SwingUtils.centerComponent(renderer.getTopLevelAncestor(), dialogPlot);
        }
        dialogPlot.getContentPane().removeAll();
        try {
            if (selectionType != null) {
                Rectangle bounds = selectionOverlay.getBounds();
                if (selectionOverlay.isFixed()) {
                    bounds = renderer.toImageCoord(bounds);
                }
                double minX = bounds.x;
                double maxX = bounds.x + bounds.width - 1;
                int nX = bounds.width;
                double minY = bounds.y;
                double maxY = bounds.y + bounds.height - 1;
                int nY = bounds.height;

                switch (selectionType) {
                    case Line:
                    case Horizontal:
                    case Vertical:
                        plot = (PlotBase) Plot.newPlot(getLinePlotImpl());
                        series = new LinePlotSeries("Selection");
                        plot.getAxis(Plot.AxisId.X).setLabel(null);
                        plot.getAxis(Plot.AxisId.Y).setLabel(null);
                        break;
                    case Rectangle:
                        plot = (PlotBase) Plot.newPlot(getMatrixPlotImpl());
                        plot.getAxis(Plot.AxisId.Y).setInverted(true);
                        //Rectangle dataRect = renderer.getData().getInverseRect(rect);                        
                        //series = new MatrixPlotSeries("Selection", minX, maxX, dataRect.width, minY, maxY, dataRect.height );
                        series = new MatrixPlotSeries("Selection", minX, maxX, nX, minY, maxY, nY);
                        PlotPanel.addSurfacePlotMenu((MatrixPlotBase) plot);
                        break;
                }
            } else if (integrationType != null) {
                plot = (PlotBase) Plot.newPlot(getLinePlotImpl());
                series = new LinePlotSeries("Integration");
                plot.getAxis(Plot.AxisId.X).setLabel(null);
                plot.getAxis(Plot.AxisId.Y).setLabel(null);
            }

            plot.setTitle(null);
            plot.addSeries(series);
            plot.setQuality(getQuality());
            dialogPlot.getContentPane().add(plot);
            dialogPlot.setVisible(true);
            updatePlotTitle();
            updatePlotData();
            renderer.painter.requestFocus();
        } catch (Exception ex) {
            SwingUtils.showException(renderer, ex);
            removeDataSelectionDialog();
        }
    }

    protected void removeDataSelectionDialog() {
        selectionType = null;
        removeIntegration();
        if (dialogPlot != null) {
            dialogPlot.setVisible(false);
        }
    }

    JDialog dialogPlot;
    PlotBase plot;
    PlotSeries series;

    protected void updatePlotData() {
        Data data = renderer.getData();
        BufferedImage img = renderer.getImage();
        if ((selectionType != null) && (selectionOverlay != null) && (series != null)) {
            double[] x = null;
            double[] y = null;

            Rectangle bounds = selectionOverlay.getBounds();
            if (selectionOverlay.isFixed()) {
                bounds = renderer.toImageCoord(bounds);
            }

            switch (selectionType) {
                case Horizontal:
                    x = data.getRowSelectionX(true);
                    ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getX(0), data.getX(img.getWidth() - 1));
                    ((LinePlotSeries) series).setData(x, data.getRowSelection(bounds.y, true));
                    break;
                case Vertical:
                    x = data.getColSelectionX(true);
                    ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getY(0), data.getY(img.getHeight() - 1));
                    ((LinePlotSeries) series).setData(x, data.getColSelection(bounds.x, true));
                    break;
                case Line:
                    ((LinePlotSeries) series).setData(data.getLineSelection(bounds.getLocation(), new Point(bounds.x + bounds.width, bounds.y + bounds.height), true));
                    break;
                case Rectangle:
                    double minBoundsX = data.getX(bounds.x);
                    double maxBoundsX = data.getX(bounds.x + bounds.width);
                    double minBoundsY = data.getY(bounds.y);
                    double maxBoundsY = data.getY(bounds.y + bounds.height);
                    ((MatrixPlotSeries) series).setRangeX(minBoundsX, maxBoundsX);
                    ((MatrixPlotSeries) series).setRangeY(minBoundsY, maxBoundsY);
                    plot.getAxis(Plot.AxisId.X).setRange(minBoundsX, maxBoundsX);
                    plot.getAxis(Plot.AxisId.Y).setRange(minBoundsY, maxBoundsY);
                    ((MatrixPlotSeries) series).setData(data.getRectSelection(bounds, true));
                    break;
            }
        } else if (integrationType != null) {
            double[] x = null;
            double[] y = null;
            switch (integrationType) {
                case Horizontal:
                    x = data.getColSelectionX(true);
                    double[] ih = (double[]) Convert.toDouble(data.integrateHorizontally(true));
                    ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getY(0), data.getY(ih.length - 1));
                    ((LinePlotSeries) series).setData(x, ih);
                    break;
                case Vertical:
                    x = data.getRowSelectionX(true);
                    double[] iv = (double[]) Convert.toDouble(data.integrateVertically(true));
                    ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getX(0), data.getX(iv.length - 1));
                    ((LinePlotSeries) series).setData(x, iv);
                    break;
            }
        }
    }

    protected void updatePlotTitle() {
        if ((selectionType != null) && (selectionOverlay != null) && (series != null)) {
            String title = null;
            Point p = selectionOverlay.getPosition();
            Point u = selectionOverlay.getUtmost();
            switch (selectionType) {
                case Horizontal:
                    title = "row: " + p.y;
                    break;
                case Vertical:
                    title = "column: " + p.x;
                    break;
                case Line:
                    title = "line: " + "[" + p.x + "," + p.y + "]" + " to " + "[" + u.x + "," + u.y + "]";
                    break;
                case Rectangle:
                    Rectangle r = selectionOverlay.getBounds();
                    title = "rectangle: " + "[" + r.x + "," + r.y + "]" + " to " + "[" + (r.x + r.width) + "," + (r.y + r.height) + "]";
                    break;
            }
            if ((dialogPlot != null) && (title != null)) {
                title = "Data Selection  (" + title + ")";
            }
            if (!title.equals(dialogPlot.getTitle())) {
                dialogPlot.setTitle(title);
            }
        }
    }

    protected void cleanPlot() {
        if (series != null) {
            series.clear();
        }
    }

}
