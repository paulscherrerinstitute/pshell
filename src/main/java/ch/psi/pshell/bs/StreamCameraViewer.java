package ch.psi.pshell.bs;

import ch.psi.pshell.core.Context;
import java.io.IOException;
import java.nio.file.Paths;
import ch.psi.pshell.imaging.ImageListener;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableNumber;
import ch.psi.pshell.device.ReadableRegister.ReadableRegisterArray;
import ch.psi.pshell.device.ReadableRegister.ReadableRegisterNumber;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.imaging.ColormapSource;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.imaging.Data;
import ch.psi.pshell.imaging.DimensionDouble;
import ch.psi.pshell.imaging.Histogram;
import ch.psi.pshell.imaging.ImageBuffer;
import ch.psi.pshell.imaging.Overlay;
import ch.psi.pshell.imaging.Overlays;
import ch.psi.pshell.imaging.Overlays.Text;
import ch.psi.pshell.imaging.Pen;
import ch.psi.pshell.imaging.PointDouble;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.RendererMode;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.swing.DeviceValueChart;
import ch.psi.pshell.swing.ValueSelection;
import ch.psi.pshell.swing.ValueSelection.ValueSelectionListener;
import ch.psi.utils.Arr;
import ch.psi.utils.ArrayProperties;
import ch.psi.utils.Chrono;
import ch.psi.utils.Config;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 */
public class StreamCameraViewer extends MonitoredPanel {

    final String CAMERA_DEVICE_NAME = "CurrentCamera";
    boolean useServerStats = true;
    String userOverlaysConfigFile;
    StreamCamera camera;
    String stream;
    int polling = 1000;
    Overlay marker = null;
    JDialog histogramDialog;
    boolean showFit;
    boolean showProfile;
    boolean localFit;
    Overlay[] userOv;
    Overlay[] fitOv;
    Overlay[] profileOv;
    Overlay errorOverlay;
    boolean requestCameraListUpdate;
    String instanceName;
    Overlay titleOv = null;
    int integration = 0;
    boolean persistCameraState;

    String serverUrl;
    PipelineServer server;
    String[] instances;
    String[] pipelines;
    String[] cameras;

    public class CameraState extends Config {

        public boolean valid;
        public boolean showSidePanel;
        //public boolean showMarker; Saved in the stream instance config
        public boolean showProfile;
        public boolean showFit;
        public boolean showReticle;
        public boolean showScale;
        public boolean showTitle;
        public double zoom;
        public RendererMode mode;
        public boolean colormapAutomatic;
        public double colormapMin;
        public double colormapMax;
        public Colormap colormap;
        public boolean colormapLogarithmic;

        String getFile() throws IOException {
            if (camera == null) {
                return null;
            }
            return Context.getInstance().getSetup().expandPath("{context}/screen_panel/" + getCameraName() + ".properties");
        }
    }

    void loadCameraState() {
        if (persistCameraState) {
            try {
                CameraState state = new CameraState();
                state.load(state.getFile());
                if (state.valid) {
                    buttonSidePanel.setSelected(state.showSidePanel);
                    buttonSidePanelActionPerformed(null);
                    buttonProfile.setSelected(state.showProfile);
                    buttonProfileActionPerformed(null);
                    buttonFit.setSelected(state.showFit);
                    buttonFitActionPerformed(null);
                    buttonReticle.setSelected(state.showReticle);
                    buttonReticleActionPerformed(null);
                    buttonScale.setSelected(state.showScale);
                    buttonScaleActionPerformed(null);
                    buttonTitle.setSelected(state.showTitle);
                    buttonTitleActionPerformed(null);
                    renderer.setMode(state.mode);
                    renderer.setZoom(state.zoom);
                    if (camera instanceof ColormapSource) {
                        camera.getConfig().colormap = state.colormap;
                        camera.getConfig().colormapAutomatic = state.colormapAutomatic;
                        camera.getConfig().colormapLogarithmic = state.colormapLogarithmic;
                        camera.getConfig().colormapMax = state.colormapMax;
                        camera.getConfig().colormapMin = state.colormapMin;
                        updateColormap();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    void saveCameraState() {
        if (persistCameraState) {
            try {
                CameraState state = new CameraState();
                state.valid = true;
                if (camera instanceof ColormapSource) {
                    state.colormap = camera.getConfig().colormap;
                    state.colormapAutomatic = camera.getConfig().colormapAutomatic;
                    state.colormapLogarithmic = camera.getConfig().colormapLogarithmic;
                    state.colormapMax = camera.getConfig().colormapMax;
                    state.colormapMin = camera.getConfig().colormapMin;
                }
                state.mode = renderer.getMode();
                state.zoom = renderer.getZoom();

                state.showSidePanel = buttonSidePanel.isSelected();
                state.showProfile = buttonProfile.isSelected();
                state.showFit = buttonFit.isSelected();
                state.showReticle = buttonReticle.isSelected();
                state.showScale = buttonScale.isSelected();
                state.showTitle = buttonTitle.isSelected();

                state.save(state.getFile());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    Timer timer;

    public static class ImageData {

        boolean background;
        boolean threshold;
        boolean goodRegion;
        boolean slicing;

        ImageData(Stream stream, Renderer renderer) {
            if (stream != null) {
                cache = stream.take();
                try {
                    Map<String, Object> pars = getProcessingParameters(cache);
                    background = (boolean) pars.get("image_background_enable");
                    Number th = (Number) (pars.get("image_threshold"));
                    threshold = (th != null) && (th.doubleValue() > 0);
                    goodRegion = ((Map<String, Object>) pars.get("image_good_region")) != null;
                    slicing = ((Map<String, Object>) (pars.get("image_slices"))) != null;
                } catch (Exception ex) {
                }

                String prefix = goodRegion ? "gr_" : "";
                x_fit_mean = getDouble(prefix + "x_fit_mean");
                y_fit_mean = getDouble(prefix + "y_fit_mean");
                x_fit_standard_deviation = getDouble(prefix + "x_fit_standard_deviation");
                y_fit_standard_deviation = getDouble(prefix + "y_fit_standard_deviation");
                x_fit_gauss_function = getDoubleArray(prefix + "x_fit_gauss_function");
                y_fit_gauss_function = getDoubleArray(prefix + "y_fit_gauss_function");
                x_profile = getDoubleArray("x_profile");
                y_profile = getDoubleArray("y_profile");
                x_center_of_mass = getDouble("x_center_of_mass");
                y_center_of_mass = getDouble("y_center_of_mass");
                x_rms = getDouble("x_rms");
                y_rms = getDouble("y_rms");
                if (goodRegion) {
                    double[] gX2 = new double[x_profile.length];
                    Arrays.fill(gX2, Double.NaN);
                    try {
                        double[] axis = getDoubleArray("x_axis");
                        gr_x_axis = getDoubleArray("gr_x_axis");
                        double x = gr_x_axis[0];
                        gr_size_x = x_fit_gauss_function.length;
                        //If gr axis values are not identical, calculate the index...
                        gr_pos_x = (int) ((renderer.getCalibration() != null) ? renderer.getCalibration().convertToImageX(x) : x);
                        //But prefer checking the value to avoid raounding errors
                        for (int i = 0; i < axis.length; i++) {
                            if (almostEqual(axis[i], x, 10e-6)) {
                                gr_pos_x = i;
                            }
                        }
                        System.arraycopy(x_fit_gauss_function, 0, gX2, gr_pos_x, gr_size_x);
                    } catch (Exception ex) {
                    }
                    x_fit_gauss_function = gX2;
                    double[] gY2 = new double[y_profile.length];
                    Arrays.fill(gY2, Double.NaN);
                    try {
                        double[] axis = getDoubleArray("y_axis");
                        gr_y_axis = getDoubleArray("gr_y_axis");
                        double y = gr_y_axis[0];
                        gr_size_y = y_fit_gauss_function.length;
                        //If gr axis values are not identical, calculate the index...
                        gr_pos_y = (int) ((renderer.getCalibration() != null) ? renderer.getCalibration().convertToImageY(y) : y);
                        //But prefer checking the value to avoid raounding errors
                        for (int i = 0; i < axis.length; i++) {
                            if (almostEqual(axis[i], y, 10e-6)) {
                                gr_pos_y = i;
                            }
                        }
                        System.arraycopy(y_fit_gauss_function, 0, gY2, gr_pos_y, gr_size_y);
                    } catch (Exception ex) {
                    }
                    y_fit_gauss_function = gY2;
                    if (slicing) {
                        try {
                            int slices = getDouble("slice_amount").intValue();
                            sliceCenters = new PointDouble[slices];
                            for (int i = 0; i < slices; i++) {
                                double x = getDouble("slice_" + i + "_center_x");
                                double y = getDouble("slice_" + i + "_center_y");
                                sliceCenters[i] = new PointDouble(x, y);
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }
        public Double x_fit_mean;
        public Double y_fit_mean;
        public Double x_center_of_mass;
        public Double x_rms;
        public Double x_fit_standard_deviation;
        public Double y_fit_standard_deviation;
        public Double y_center_of_mass;
        public Double y_rms;
        public double[] x_profile;
        public double[] x_fit_gauss_function;
        public double[] y_profile;
        public double[] y_fit_gauss_function;
        public double[] gr_x_axis;
        public double[] gr_y_axis;
        public int gr_size_x;
        public int gr_pos_x;
        public int gr_size_y;
        public int gr_pos_y;
        public PointDouble[] sliceCenters;
        public StreamValue cache;

        Double getDouble(String name) {
            try {
                return (Double) Convert.toDouble(cache.__getitem__(name));
            } catch (Exception ex) {
                return null;
            }
        }

        double[] getDoubleArray(String name) {
            try {
                return (double[]) Convert.toDouble(cache.__getitem__(name));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static boolean almostEqual(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    public static class Frame extends ImageData {

        Frame(Stream stream, Renderer renderer, Data data) {
            super(stream, renderer);
            this.data = data;
        }
        Data data;
    }

    final ArrayList<Frame> imageBuffer = new ArrayList();
    Frame currentFrame;
    int imageBufferLenght = 1;
    Text imagePauseOverlay;

    public StreamCameraViewer() {
        try {
            initComponents();
            setSidePanelVisible(false);
            panelPipeline.setVisible(false);
            textStream.setBackground(panelStream.getBackground());
            SwingUtils.setEnumCombo(comboColormap, Colormap.class);

            spinnerThreshold.setVisible(false);
            btFixColormapRange.setVisible(false);
            checkBackground.setEnabled(false);
            checkThreshold.setEnabled(false);
            checkRotation.setEnabled(false);
            checkGoodRegion.setEnabled(false);
            setGoodRegionOptionsVisible(false);
            setSlicingOptionsVisible(false);
            setRotationOptionsVisible(false);
            JComponent editor = spinnerSlOrientation.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().setHorizontalAlignment(JTextField.RIGHT);
            }
            renderer.setPersistenceFile(Paths.get(Sys.getTempFolder(), "ImageViewer.bin"));
            if (App.hasArgument("poll")) {
                try {
                    polling = Integer.valueOf(App.getArgumentValue("poll"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (App.hasArgument("zoom")) {
                try {
                    renderer.setDefaultZoom(Double.valueOf(App.getArgumentValue("zoom")));
                    renderer.resetZoom();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (App.hasArgument("buf")) {
                try {
                    imageBufferLenght = Integer.valueOf(App.getArgumentValue("buf"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (App.hasArgument("usr_ov")) {
                try {
                    userOverlaysConfigFile = App.getArgumentValue("usr_ov");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (App.hasArgument("local_fit")) {
                localFit = true;
            }

            if (App.hasArgument("persist")) {
                if (Context.getInstance() != null) {
                    persistCameraState = true;
                }
            }

            if (App.hasArgument("integration")) {
                try {
                    setIntegration(Integer.valueOf(App.getArgumentValue("integration")));
                    if (integration != 0) {
                        buttonFit.setSelected(false);
                        buttonProfile.setSelected(false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            renderer.setProfileNormalized(true);
            renderer.setShowProfileLimits(false);

            JMenuItem menuRendererConfig = new JMenuItem("Renderer Parameters");
            menuRendererConfig.addActionListener((ActionEvent e) -> {
                try {
                    if (camera != null) {
                        DevicePanel.showConfigEditor(getTopLevel(), camera, false, false);
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
                }
            });

            JMenuItem menuSetImageBufferSize = new JMenuItem("Set Stack Size...");
            menuSetImageBufferSize.addActionListener((ActionEvent e) -> {
                try {
                    String ret = SwingUtils.getString(getTopLevel(), "Enter size of image buffer: ", String.valueOf(imageBufferLenght));
                    if (ret != null) {
                        this.setImageBufferSize(Integer.valueOf(ret));
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
                }
            });

            JMenuItem menuSaveStack = new JMenuItem("Save Stack");
            menuSaveStack.addActionListener((ActionEvent e) -> {
                try {
                    saveStack();
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
                }
            });

            JCheckBoxMenuItem menuFrameIntegration = new JCheckBoxMenuItem("Multi-Frame", (integration != 0));
            menuFrameIntegration.addActionListener((ActionEvent e) -> {
                if (integration == 0) {
                    JPanel panel = new JPanel();
                    GridBagLayout layout = new GridBagLayout();
                    layout.columnWidths = new int[]{150, 50};   //Minimum width
                    layout.rowHeights = new int[]{30, 30};   //Minimum height
                    panel.setLayout(layout);
                    JCheckBox checkContinuous = new JCheckBox("");
                    checkContinuous.setSelected(true);
                    JTextField textFrames = new JTextField();
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = 0;
                    panel.add(new JLabel("Number of frames:"), c);
                    c.gridy = 1;
                    panel.add(new JLabel("Continuous:"), c);
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 1;
                    panel.add(checkContinuous, c);
                    c.gridy = 0;
                    panel.add(textFrames, c);
                    if (SwingUtils.showOption(getTopLevel(), "Multi-Frame Integration", panel, OptionType.OkCancel) == OptionResult.Yes) {
                        setIntegration(checkContinuous.isSelected() ? -(Integer.valueOf(textFrames.getText())) : (Integer.valueOf(textFrames.getText())));
                    }
                } else {
                    if (SwingUtils.showOption(getTopLevel(), "Multi-Frame Integration",
                            "Do you want to disable " + ((integration < 0) ? "continuous " : "") + "multi-frame integration (" + Math.abs(integration) + ")?", OptionType.YesNo) == OptionResult.Yes) {
                        setIntegration(0);
                    }
                }
            });

            for (Component cmp : SwingUtils.getComponentsByType(renderer.getPopupMenu(), JMenu.class)) {
                JMenu menu = (JMenu) cmp;
                if (menu.getText().equals("Integration")) {
                    menu.addSeparator();
                    menu.add(menuFrameIntegration);
                }
            }

            JMenuItem menuHistogram = new JMenuItem("Show Histogram");
            menuHistogram.addActionListener((ActionEvent e) -> {
                try {
                    setHistogramVisible(true);
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
                }
            });
            renderer.getPopupMenu().add(menuHistogram);
            renderer.getPopupMenu().addSeparator();
            renderer.getPopupMenu().add(menuRendererConfig);
            renderer.getPopupMenu().add(menuSetImageBufferSize);
            renderer.getPopupMenu().add(menuSaveStack);
            renderer.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    menuSetImageBufferSize.setEnabled(!renderer.isPaused());
                    menuFrameIntegration.setSelected(integration != 0);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
            renderer.getPopupMenu().setVisible(false);
            buttonScale.setSelected(renderer.getShowColormapScale());
            clearMarker();

            showFit = buttonFit.isSelected();
            showProfile = buttonProfile.isSelected();

            pauseSelection.setVisible(false);
            pauseSelection.setMinValue(1);
            pauseSelection.addListener(new ValueSelectionListener() {
                @Override
                public void onValueChanged(ValueSelection origin, double value, boolean editing) {
                    if (editing && (value >= 1) && (value <= imageBuffer.size())) {
                        updatePause();
                    }
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        updateButtons();
    }

    public String getServer() {
        return serverUrl;
    }

    public void setServer(String server) {
        this.serverUrl = server;
        panelPipeline.setVisible(serverUrl != null);
    }

    public enum SourceSelecionMode {
        Single,
        Instances,
        Pipelines,
        Cameras

    }
    JComboBox comboStream;

    SourceSelecionMode sourceSelecionMode = SourceSelecionMode.Single;

    public void setSourceSelecionMode(SourceSelecionMode mode) {
        if (sourceSelecionMode == mode) {
            return;
        }
        GroupLayout layout = (GroupLayout) panelStream.getLayout();
        if ((getServer() == null) && (mode != SourceSelecionMode.Single)) {
            throw new RuntimeException("Invalid selection mode: Pipeline Server URL is not configured.");
        }
        if ((sourceSelecionMode == SourceSelecionMode.Single) && (mode != SourceSelecionMode.Single)) {
            comboStream = new JComboBox();
            comboStream.setModel(getStreamList(mode));
            comboStream.addActionListener((e) -> {
                try {
                        if (!comboStream.isEnabled()) {
                            throw new Exception("Invalid state");
                        }
                        comboStream.setEnabled(false);
                        final String stream = (String) comboStream.getSelectedItem();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setStream(stream.trim().isEmpty() ? null : stream);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                } finally {
                                    updateButtons();
                                    comboStream.setEnabled(true);
                                }
                            }
                        }).start();
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
                }

            });
            layout.replace(textStream, comboStream);
        }
        if ((mode == SourceSelecionMode.Single) && (sourceSelecionMode != SourceSelecionMode.Single)) {
            layout.replace(comboStream, textStream);
        }
        sourceSelecionMode = mode;
    }

    public SourceSelecionMode getSourceSelecionMode() {
        return sourceSelecionMode;
    }

    DefaultComboBoxModel getStreamList(SourceSelecionMode mode) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        List<String> streams = new ArrayList<>();
        try (PipelineServer srv =  new PipelineServer(CAMERA_DEVICE_NAME, serverUrl);) {
            switch (mode) {
                case Instances:
                    streams = srv.getInstances();
                    break;
                case Pipelines:
                    streams = srv.getPipelines();
                    break;
                case Cameras:
                    streams = srv.getCameras();
                    break;
            }
            Collections.sort(streams);

            for (String stream : streams) {
                model.addElement(stream);
            }

            if (App.hasArgument("stream")) {
                String camera = App.getArgumentValue("stream");
                if (model.getIndexOf(camera) < 0) {
                    model.addElement(camera);
                }
            }
            model.addElement("");
            if (App.hasArgument("stream")) {
                 model.setSelectedItem(App.getArgumentValue("stream")); 
            } else {
                model.setSelectedItem("");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return model;
    }

    public void setToolbarVisible(boolean value) {
        topPanel.setVisible(value);
    }

    public boolean isSidePanelVisible() {
        return sidePanel.isVisible();
    }

    public void setSidePanelVisible(boolean value) {
        sidePanel.setVisible(value);
    }

    public boolean isToolbarVisible() {
        return topPanel.isVisible();
    }

    public void setIntegration(int frames) {
        try {
            if (integration != frames) {
                integration = frames;
                if (camera != null) {
                    if (Math.abs(integration) > 1) {
                        renderer.setDevice(new ImageIntegrator(camera, integration));
                    } else {
                        renderer.setDevice(camera);
                    }
                    synchronized (imageBuffer) {
                        currentFrame = null;
                        imageBuffer.clear();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onShow() {
        try {
            timer = new Timer(1000, (ActionEvent e) -> {
                try {
                    onTimer();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.FINE, null, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onHide() {
        try {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (camera != null) {
                camera.close();
                camera = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    final Object lockOverlays = new Object();

    protected void manageFit(BufferedImage bi, Data data) {
        Overlay[][] fo = null;
        if ((showFit || showProfile)) {
            try {
                fo = getFitOverlays(data, localFit);
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
        synchronized (lockOverlays) {
            fo = (fo == null) ? new Overlay[][]{null, null} : fo;
            renderer.updateOverlays(fo[0], profileOv);
            profileOv = fo[0];
            renderer.updateOverlays(fo[1], fitOv);
            fitOv = fo[1];
        }
    }

    protected void manageUserOverlays(BufferedImage bi, Data data) {

        Overlay[] fo = (bi == null)
                ? null
                : getUserOverlays(userOverlayConfig, renderer, ((camera == null) ? null : getFrame(data)));
        synchronized (lockOverlays) {
            renderer.updateOverlays(fo, userOv);
            userOv = fo;
        }
    }

    void manageTitleOverlay() {
        Overlay to = null;
        String name = getDisplayName();
        if ((buttonTitle.isSelected()) && (name != null)) {
            Font font = new Font("Arial", Font.PLAIN, 28);
            to = new Text(renderer.getPenErrorText(), name, font, new Point(-SwingUtils.getTextSize(name, renderer.getGraphics().getFontMetrics(font)).width - 14, 26));
            to.setFixed(true);
            to.setAnchor(Overlay.ANCHOR_VIEWPORT_OR_IMAGE_TOP_RIGHT);
        }

        synchronized (lockOverlays) {
            renderer.updateOverlays(to, titleOv);
            titleOv = to;
        }
    }

    Thread devicesInitTask;

    public void setStream(String stream) throws IOException, InterruptedException {
        System.out.println("Initializing: " + stream);
        textStream.setText((stream == null) ? "" : stream);
        parseUserOverlays();
        errorOverlay = null;
        lastFrame = null;
        lastPipelinePars = null;

        if (dataTableDialog != null) {
            dataTableDialog.dispose();
            dataTableDialog = null;
        }
        dataTableModel = null;

        if (camera != null) {
            saveCameraState();
            camera.close();
            camera = null;
            server = null;
        }
        updateButtons();
        instanceName = null;
        renderer.setDevice(null);
        renderer.setShowReticle(false);
        renderer.removeOverlays(fitOv);
        renderer.removeOverlays(profileOv);
        renderer.removeOverlays(userOv);
        renderer.clear();
        renderer.resetZoom();

        boolean changed = !String.valueOf(stream).equals(this.stream);
        this.stream = stream;
        if (changed || ((serverUrl == null))) {
            spinnerThreshold.setVisible(false);
            checkBackground.setEnabled(false);
            checkThreshold.setEnabled(false);
            checkRotation.setEnabled(false);
            checkGoodRegion.setEnabled(false);
            setGoodRegionOptionsVisible(false);
            setSlicingOptionsVisible(false);
            setRotationOptionsVisible(false);
        }

        synchronized (imageBuffer) {
            currentFrame = null;
            imageBuffer.clear();
        }
        if (changed) {
            if ((devicesInitTask != null) && (devicesInitTask.isAlive())) {
                devicesInitTask.interrupt();
            }
            if (renderer.isPaused()) {
                renderer.resume();
                removePauseOverlay();
                pauseSelection.setVisible(false);
                panelStream.setVisible(true);
            }
        }

        if (stream == null) {
            return;
        }

        manageTitleOverlay();

        System.out.println("Setting stream: " + stream);
        try {
            if (serverUrl != null) {
                server = new PipelineServer(CAMERA_DEVICE_NAME, serverUrl);
                instances = server.getInstances().toArray(new String[0]);
                pipelines = server.getPipelines().toArray(new String[0]);
                cameras = server.getCameras().toArray(new String[0]);
                camera = server;
            } else {
                server = null;
                camera = new StreamCamera(CAMERA_DEVICE_NAME, stream);
            }

            camera.getConfig().flipHorizontally = false;
            camera.getConfig().flipVertically = false;
            camera.getConfig().rotation = 0.0;
            camera.getConfig().roiX = 0;
            camera.getConfig().roiY = 0;
            camera.getConfig().roiWidth = -1;
            camera.getConfig().roiHeight = -1;
            camera.setMonitored(true);
            camera.initialize();
            camera.assertInitialized();

            System.out.println("Camera initialization OK");
            loadCameraState();

            if (server != null) {
                if (Arr.containsEqual(instances, stream)) {
                    instanceName = stream;
                    server.start(stream, true);
                } else if (Arr.containsEqual(pipelines, stream)) {
                    instanceName = stream + "1";
                    server.start(stream, instanceName);
                } else if (Arr.containsEqual(cameras, stream)) {
                    stream = stream + "_sp";
                    instanceName = stream + "1";
                    server.start(stream, instanceName);
                }
                updatePipelineControls();
                checkBackground.setEnabled(true);
                checkThreshold.setEnabled(true);
                checkRotation.setEnabled(true);
                checkGoodRegion.setEnabled(true);
            }

            updateButtons();
            camera.getConfig().save();
            if (Math.abs(integration) > 1) {
                renderer.setDevice(new ImageIntegrator(camera, integration));
            } else {
                renderer.setDevice(camera);
            }
            renderer.setAutoScroll(true);
            //renderer.setMarker(marker);
            clearMarker();
            imageSize = null;

            camera.addListener(new ImageListener() {
                @Override
                public void onImage(Object o, BufferedImage bi, Data data) {
                    if (bi != null) {
                        if ((imageSize == null) || imageSize.width != bi.getWidth() || imageSize.height != bi.getHeight()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if ((renderer.getMode() == RendererMode.Zoom) || (renderer.getMode() == RendererMode.Fixed)) {
                                        centralizeRenderer();
                                    }
                                    checkReticle();
                                }
                            });
                            imageSize = new Dimension(bi.getWidth(), bi.getHeight());
                        }
                        renderer.setProfileSize(Math.min(bi.getWidth(), bi.getHeight()));
                    }
                    //renderer.setCalibration(camera.getCalibration());
                    if (!renderer.isPaused()) {
                        if (data != null) {
                            synchronized (imageBuffer) {

                                currentFrame = new Frame((camera == null) ? null : camera.getStream(), renderer, data);
                                if (imageBufferLenght >= 1) {
                                    imageBuffer.add(currentFrame);
                                    if (imageBuffer.size() > imageBufferLenght) {
                                        imageBuffer.remove(0);
                                        setBufferFull(true);
                                    } else {
                                        setBufferFull(false);
                                    }
                                } else {
                                    setBufferFull(true);
                                }
                                //Update data
                                if (!renderer.isPaused()) {
                                    updateStreamData();
                                }
                            }
                        }
                        //
                        //try{
                        //    updateStreamInfo(camera.getValue());
                        //} catch(Exception ex){
                        //    System.err.println(ex.getMessage());
                        //}
                        manageFit(bi, data);
                        manageUserOverlays(bi, data);
                    }
                    //updateImageData();
                }

                @Override
                public void onError(Object o, Exception ex) {
                    //System.err.println(ex);
                }
            });

        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            renderer.clearOverlays();
            if (renderer.getDevice() == null) {
                errorOverlay = new Text(renderer.getPenErrorText(), ex.toString(), new Font("Verdana", Font.PLAIN, 12), new Point(20, 20));
                errorOverlay.setFixed(true);
                errorOverlay.setAnchor(Overlay.ANCHOR_VIEWPORT_TOP_LEFT);
                renderer.addOverlay(errorOverlay);
            }
        } finally {
            //checkReticle();
            onTimer();
        }
    }

    public static class ImageIntegrator extends ColormapSource {

        ImageIntegrator(ColormapSource camera, int num) {
            super("Image Integrator", camera.getConfig());
            boolean continuous = (num < 0);
            final int numImages = Math.abs(num);

            camera.addListener(new ImageListener() {
                final ArrayList<Data> buffer = new ArrayList();
                Data integration = null;

                @Override
                public void onImage(Object o, BufferedImage bi, Data data) {
                    try {
                        if (continuous) {
                            buffer.add(data);
                            if (buffer.size() >= numImages) {
                                for (Data d : buffer) {
                                    process(d);
                                }
                            }
                        } else {
                            buffer.add(null); //Just to count
                            process(data);
                        }
                    } catch (Exception ex) {
                        buffer.clear();
                        integration = null;
                        ImageIntegrator.this.pushData(null);
                        ex.printStackTrace();
                        return;
                    }
                    if (buffer.size() >= numImages) {
                        if (continuous) {
                            buffer.remove(0);
                        } else {
                            buffer.clear();
                        }
                        if (integration != null) {
                            //integration.div(numImages);
                            ImageIntegrator.this.pushData(integration);
                        }
                        integration = null;
                    }
                }

                void process(Data data) {
                    if (integration == null) {
                        integration = new Data(data);
                    } else {
                        integration.sum(data);
                    }
                }

                @Override
                public void onError(Object origin, Exception ex) {
                }
            });

        }
    }

    boolean bufferFull = true;

    protected void setBufferFull(boolean value) {
        if (value != bufferFull) {
            SwingUtilities.invokeLater(() -> {
                buttonPause.setBackground(value ? buttonSave.getBackground() : buttonSave.getBackground().brighter());
            });
            bufferFull = value;
        }
    }

    volatile Dimension imageSize;

    protected void checkReticle() {
        if ((renderer.getDevice() != null) && (camera != null) && (camera.getConfig().isCalibrated()) && buttonReticle.isSelected()) {
            //renderer.setCalibration(camera.getCalibration());
            renderer.configureReticle(new Dimension(800, 800), 200);
            renderer.setShowReticle(true);
        } else {
            //renderer.setCalibration(null);
            renderer.setShowReticle(false);
        }
        renderer.refresh();
    }

    protected void checkMarker(Point p) throws IOException {
        if (camera != null) {
            if (buttonMarker.isSelected()) {
                Dimension d = renderer.getImageSize();
                if (p == null) {
                    p = (d == null) ? new Point(renderer.getWidth() / 2, renderer.getHeight() / 2) : new Point(d.width / 2, d.height / 2);
                }
                Overlay ov = null;
                marker = new Overlays.Crosshairs(renderer.getPenMarker(), p, new Dimension(100, 100));
                marker.setMovable(true);
                marker.setPassive(false);
            } else {
                marker = null;
            }
            renderer.setMarker(marker);
        }
    }

    protected void checkProfile() {
        if (!updatingButtons) {
            setShowProfile(buttonProfile.isSelected());
        }
    }

    protected void checkFit() {
        if (!updatingButtons) {
            setShowFit(buttonFit.isSelected());
        }
    }

    protected void checkPause() {
        if (!updatingButtons) {
            setPaused(buttonPause.isSelected());
        }
    }

    protected void checkColormap() {
        if (!updatingButtons) {
            renderer.setShowColormapScale(buttonScale.isSelected());
        }
    }

    public Point getStreamMarkerPos() throws IOException {
        //System.out.println(server.getInstanceConfig().get("Marker"));
        Map<String, Object> pars = getProcessingParameters(camera.getValue());
        if (pars != null) {
            List markerPosition = (List) pars.get("Marker");
            if (markerPosition != null) {
                return new Point((Integer) markerPosition.get(0), (Integer) markerPosition.get(1));
            }
            return new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        return null;
    }

    public void clearMarker() {
        marker = null;
        renderer.setMarker(marker);
    }

    void setGoodRegionOptionsVisible(boolean visible) {
        spinnerGrThreshold.setVisible(visible);
        labelGrThreshold.setVisible(visible);
        spinnerGrScale.setVisible(visible);
        labelGrScale.setVisible(visible);
        panelSlicing.setVisible(visible);
    }

    void setSlicingOptionsVisible(boolean visible) {
        spinnerSlNumber.setVisible(visible);
        labelSlNumber.setVisible(visible);
        spinnerSlScale.setVisible(visible);
        labelSlScale.setVisible(visible);
        spinnerSlOrientation.setVisible(visible);
        labelSlOrientation.setVisible(visible);
    }

    void setRotationOptionsVisible(boolean visible) {
        labelAngle.setVisible(visible);
        labelOrder.setVisible(visible);
        labelMode.setVisible(visible);
        labelConstant.setVisible(visible);
        spinnerRotationAngle.setVisible(visible);
        spinnerRotationOrder.setVisible(visible);
        spinnerRotationMode.setVisible(visible);
        spinnerRotationConstant.setVisible(visible);
    }

    public boolean isCameraStopped() {
        return ((camera == null) || camera.isClosed());
    }

    boolean updatingButtons;

    protected void updateButtons() {
        updatingButtons = true;
        try {
            boolean active = !isCameraStopped();//(camera != null);        
            buttonSave.setEnabled(active);
            buttonMarker.setEnabled(active);
            buttonProfile.setEnabled(active);
            buttonFit.setEnabled(active);
            buttonReticle.setEnabled(active && camera.getConfig().isCalibrated());
            buttonStreamData.setEnabled(active && (camera != null));
            buttonPause.setEnabled(active);

            if (renderer.isPaused() != buttonPause.isSelected()) {
                buttonPause.setSelected(renderer.isPaused());
                buttonPauseActionPerformed(null);
            }
            if (renderer.getShowReticle() != buttonReticle.isSelected()) {
                //buttonReticle.setSelected(renderer.getShowReticle());
            }
            if ((renderer.getMarker() == null) && buttonMarker.isSelected()) {
                buttonMarker.setSelected(false);
            }
            buttonProfile.setSelected(showProfile);
            buttonFit.setSelected(showFit);
            buttonSave.setSelected(renderer.isSnapshotDialogVisible());

        } finally {
            updatingButtons = false;
        }
    }

    Frame lastFrame = null;
    Map lastPipelinePars = null;

    protected void onTimer() {
        updateButtons();
        buttonScale.setSelected(renderer.getShowColormapScale());
    }

    protected Pen penFit = new Pen(new Color(192, 105, 0), 0);
    protected Pen penCross = new Pen(new Color(192, 105, 0), 0);
    protected Pen penSlices = new Pen(Color.CYAN.darker(), 1);

    public Frame getCurrentFrame() {
        if ((imageBufferLenght > 1) && (renderer.isPaused())) {
            int index = ((int) pauseSelection.getValue()) - 1;
            synchronized (imageBuffer) {
                return (index < imageBuffer.size()) ? imageBuffer.get(index) : null;
            }
        }
        return currentFrame;
    }

    public Frame getFrame(Data data) {
        synchronized (imageBuffer) {
            for (Frame f : imageBuffer) {
                if (f.data == data) {
                    return f;
                }
            }
        }
        return null;
    }

    public void setImageBufferSize(int size) {
        if (renderer.isPaused()) {
            throw new RuntimeException("Cannot change buffer size whn paused");
        }
        synchronized (imageBuffer) {
            imageBufferLenght = size;
            imageBuffer.clear();
        }

    }

    protected Overlay[][] getFitOverlays(Data data, boolean localFit) {
        Overlays.Polyline hgaussian = null;
        Overlays.Polyline vgaussian = null;
        Overlays.Polyline hprofile = null;
        Overlays.Polyline vprofile = null;
        Double xMean = null, xSigma = null, xNorm = null, xCom = null, xRms = null;
        Double yMean = null, ySigma = null, yNorm = null, yCom = null, yRms = null;
        double[] pX = null, pY = null, gX = null, gY = null;
        PointDouble[] sliceCenters = null;
        if (data != null) {
            int height = data.getHeight();
            int width = data.getWidth();
            int profileSize = renderer.getProfileSize();
            ImageData id = null;
            if (!localFit) {
                try {
                    id = getFrame(data);
                    if (id == null) {
                        return null;
                    }
                    xMean = id.x_fit_mean;
                    xSigma = id.x_fit_standard_deviation;
                    yMean = id.y_fit_mean;
                    ySigma = id.y_fit_standard_deviation;
                    gX = id.x_fit_gauss_function;
                    gY = id.y_fit_gauss_function;
                    pX = id.x_profile;
                    pY = id.y_profile;
                    xCom = id.x_center_of_mass;
                    xRms = id.x_rms;
                    yCom = id.y_center_of_mass;
                    yRms = id.y_rms;
                    sliceCenters = id.sliceCenters;

                    profileSize /= 4;
                    if (pX != null) {
                        int[] xp = Arr.indexesInt(pX.length);
                        int[] xg = xp;
                        int[] yp = new int[pX.length];
                        int[] yg = new int[pX.length];

                        List<Double> l = Arrays.asList((Double[]) Convert.toWrapperArray(pX));
                        double minProfile = Collections.min(l);
                        double maxProfile = Collections.max(l);
                        double rangeProfile = maxProfile - minProfile;
                        double minGauss = minProfile;
                        double rangeGauss = rangeProfile;
                        //If not good region, range of profile and fit  are similar so save this calcultion
                        if (id.goodRegion && id.gr_size_x > 0) {
                            l = Arrays.asList((Double[]) Convert.toWrapperArray(Arrays.copyOfRange(gX, id.gr_pos_x, id.gr_pos_x + id.gr_size_x)));
                            minGauss = Collections.min(l);
                            rangeGauss = Collections.max(l) - minGauss;
                        }

                        for (int i = 0; i < xp.length; i++) {
                            if (gX != null) {
                                yg[i] = (int) (height - 1 - (((gX[i] - minGauss) / rangeGauss) * profileSize));
                            }
                            yp[i] = (int) (height - 1 - (((pX[i] - minProfile) / rangeProfile) * profileSize));
                        }

                        if (id.goodRegion && id.gr_size_x > 0) {
                            xg = Arrays.copyOfRange(xg, id.gr_pos_x, id.gr_pos_x + id.gr_size_x);
                            yg = Arrays.copyOfRange(yg, id.gr_pos_x, id.gr_pos_x + id.gr_size_x);
                        }

                        vgaussian = new Overlays.Polyline(penFit, xg, yg);
                        vprofile = new Overlays.Polyline(renderer.getPenProfile(), xp, yp);
                    }

                    if (pY != null) {
                        int[] xp = new int[pY.length];
                        int[] xg = new int[pY.length];
                        int[] yp = Arr.indexesInt(pY.length);
                        int[] yg = yp;

                        List<Double> l = Arrays.asList((Double[]) Convert.toWrapperArray(pY));
                        double minProfile = Collections.min(l);
                        double maxProfile = Collections.max(l);
                        double rangeProfile = maxProfile - minProfile;
                        double minGauss = minProfile;
                        double rangeGauss = rangeProfile;
                        //If not good region, range of profile and fit  are similar so save this calcultion
                        if (id.goodRegion && id.gr_size_y > 0) {
                            l = Arrays.asList((Double[]) Convert.toWrapperArray(Arrays.copyOfRange(gY, id.gr_pos_y, id.gr_pos_y + id.gr_size_y)));
                            minGauss = Collections.min(l);
                            rangeGauss = Collections.max(l) - minGauss;
                        }

                        for (int i = 0; i < xp.length; i++) {
                            if (gY != null) {
                                xg[i] = (int) (((gY[i] - minGauss) / rangeGauss) * profileSize);
                            }
                            xp[i] = (int) (((pY[i] - minProfile) / rangeProfile) * profileSize);
                        }

                        if (id.goodRegion && id.gr_size_y > 0) {
                            xg = Arrays.copyOfRange(xg, id.gr_pos_y, id.gr_pos_y + id.gr_size_y);
                            yg = Arrays.copyOfRange(yg, id.gr_pos_y, id.gr_pos_y + id.gr_size_y);
                        }
                        hgaussian = new Overlays.Polyline(penFit, xg, yg);
                        hprofile = new Overlays.Polyline(renderer.getPenProfile(), xp, yp);
                    }
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                    return null;
                }
            } else {
                ArrayProperties properties = data.getProperties();
                double maxPlot = properties.max;
                double minPlot = properties.min;
                double rangePlot = maxPlot - minPlot;

                if (rangePlot <= 0) {
                    return null;
                }
                if (renderer.getCalibration() != null) {
                    try {
                        double[] sum = data.integrateVertically(true);
                        double[] saux = new double[sum.length];
                        int[] p = new int[sum.length];
                        double[] x_egu = renderer.getCalibration().getAxisX(sum.length);
                        double[] comRms = getComRms(sum, x_egu);
                        xCom = comRms[0];
                        xRms = comRms[1];
                        int[] x = Arr.indexesInt(sum.length);
                        DescriptiveStatistics stats = new DescriptiveStatistics(sum);
                        double min = stats.getMin();
                        for (int i = 0; i < sum.length; i++) {
                            saux[i] = sum[i] - min;
                        }
                        if (showFit) {
                            double[] gaussian = fitGaussian(saux, x);
                            if (gaussian != null) {
                                if ((gaussian[2] < sum.length * 0.45)
                                        && (gaussian[2] > 2)
                                        && (gaussian[0] > min * 0.03)) {
                                    xNorm = gaussian[0];
                                    xMean = gaussian[1];
                                    xSigma = gaussian[2];
                                    double[] fit = getFitFunction(gaussian, x);
                                    int[] y = new int[x.length];
                                    for (int i = 0; i < x.length; i++) {
                                        y[i] = (int) (height - 1 - ((((fit[i] + min) / height - minPlot) / rangePlot) * profileSize));
                                    }
                                    vgaussian = new Overlays.Polyline(penFit, x, y);
                                }
                            }
                        }
                        if (showProfile) {
                            for (int i = 0; i < x.length; i++) {
                                p[i] = (int) (height - 1 - (((sum[i] / height - minPlot) / rangePlot) * profileSize));
                            }
                            vprofile = new Overlays.Polyline(renderer.getPenProfile(), x, p);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    try {
                        double[] sum = data.integrateHorizontally(true);
                        double[] saux = new double[sum.length];
                        int[] p = new int[sum.length];
                        double[] y_egu = renderer.getCalibration().getAxisY(sum.length);
                        double[] comRms = getComRms(sum, y_egu);
                        yCom = comRms[0];
                        yRms = comRms[1];
                        int[] x = Arr.indexesInt(sum.length);
                        DescriptiveStatistics stats = new DescriptiveStatistics(sum);
                        double min = stats.getMin();
                        for (int i = 0; i < sum.length; i++) {
                            saux[i] = sum[i] - min;
                        }

                        if (showFit) {
                            double[] gaussian = fitGaussian(saux, x);
                            if (gaussian != null) {
                                //Only aknowledge beam fully inside the image and peak over 3% of min
                                if ((gaussian[2] < sum.length * 0.45)
                                        && (gaussian[2] > 2)
                                        && (gaussian[0] > min * 0.03)) {
                                    yNorm = gaussian[0];
                                    yMean = gaussian[1];
                                    ySigma = gaussian[2];
                                    double[] fit = getFitFunction(gaussian, x);

                                    int[] y = new int[x.length];
                                    for (int i = 0; i < x.length; i++) {
                                        y[i] = (int) ((((fit[i] + min) / width - minPlot) / rangePlot) * profileSize);
                                    }
                                    hgaussian = new Overlays.Polyline(penFit, y, x);
                                }
                            }
                        }
                        if (showProfile) {
                            for (int i = 0; i < x.length; i++) {
                                p[i] = (int) (((sum[i] / width - minPlot) / rangePlot) * profileSize);
                            }
                            hprofile = new Overlays.Polyline(renderer.getPenProfile(), p, x);
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    if (xSigma != null) {
                        xSigma *= renderer.getCalibration().getScaleX();
                    }
                    if (ySigma != null) {
                        ySigma *= renderer.getCalibration().getScaleY();
                    }
                    if (xMean != null) {
                        xMean = data.getX((int) Math.round(xMean));
                    }
                    if (yMean != null) {
                        yMean = data.getY((int) Math.round(yMean));
                    }
                }
            }
            final String units = (renderer.getCalibration() != null) ? "\u00B5m" : "px";
            final String fmt = "%7.1f" + units;
            Overlays.Text textCom = null;
            Overlay[] pOv = null, fOv = null;
            Font fontInfoText = new Font(Font.MONOSPACED, 0, 14);
            Point textPosition = new Point(12, 20);
            if (showProfile) {
                if ((xCom != null) && (yCom != null)) {
                    String text = String.format("com x: m=" + fmt + " \u03C3=" + fmt + "\ncom y: m=" + fmt + " \u03C3=" + fmt, xCom, xRms, yCom, yRms);
                    textCom = new Overlays.Text(renderer.getPenProfile(), text, fontInfoText, textPosition);
                    textCom.setFixed(true);
                    textCom.setAnchor(Overlay.ANCHOR_VIEWPORT_TOP_LEFT);
                }
                pOv = new Overlay[]{hprofile, vprofile, textCom};
                textPosition = new Point(textPosition.x, textPosition.y + 34);
            }
            if (showFit) {
                Overlays.Crosshairs cross = null;
                Overlays.Text textFit = null;
                if ((xMean != null) && (yMean != null)) {
                    String text = String.format("fit x: m=" + fmt + " \u03C3=" + fmt + "\nfit y: m=" + fmt + " \u03C3=" + fmt, xMean, xSigma, yMean, ySigma);
                    textFit = new Overlays.Text(penFit, text, fontInfoText, textPosition);
                    textFit.setFixed(true);
                    textFit.setAnchor(Overlay.ANCHOR_VIEWPORT_TOP_LEFT);
                    Point center = new Point(xMean.intValue(), yMean.intValue());
                    if (renderer.getCalibration() != null) {
                        center = renderer.getCalibration().convertToImagePosition(new PointDouble(xMean, yMean));
                        xSigma /= renderer.getCalibration().getScaleX();
                        ySigma /= renderer.getCalibration().getScaleY();
                    }
                    cross = new Overlays.Crosshairs(penCross, center, new Dimension(Math.abs(2 * xSigma.intValue()), 2 * Math.abs(ySigma.intValue())));
                }
                textPosition = new Point(textPosition.x, textPosition.y + 34);
                fOv = new Overlay[]{hgaussian, vgaussian, cross, textFit};

                if ((id != null) && (id.goodRegion)) {
                    try {
                        double[] x = id.gr_x_axis;
                        double[] y = id.gr_y_axis;
                        Overlays.Rect goodRegionOv = new Overlays.Rect(new Pen(penFit.getColor(), 0, Pen.LineStyle.dotted));
                        goodRegionOv.setCalibration(renderer.getCalibration());
                        goodRegionOv.setPosition(new Point(id.gr_pos_x, id.gr_pos_y));
                        goodRegionOv.setSize(new Dimension(id.gr_x_axis.length, id.gr_y_axis.length));
                        fOv = Arr.append(fOv, goodRegionOv);

                        if (id.slicing) {
                            if (sliceCenters != null) {
                                for (PointDouble sliceCenter : sliceCenters) {
                                    Overlays.Crosshairs center = new Overlays.Crosshairs(penSlices);
                                    center.setCalibration(renderer.getCalibration());
                                    center.setAbsolutePosition(sliceCenter);
                                    center.setSize(new Dimension(10, 10));
                                    fOv = Arr.append(fOv, center);
                                }
                                if (sliceCenters.length > 1) {
                                    double[] fit = fitPolynomial(sliceCenters, 1);
                                    double angle = Math.toDegrees(Math.atan(fit[1]));
                                    Overlays.Text text = new Overlays.Text(penSlices, String.format("slice: \u03B8= %5.1fdeg", angle), fontInfoText, textPosition);
                                    text.setFixed(true);
                                    text.setAnchor(Overlay.ANCHOR_VIEWPORT_TOP_LEFT);
                                    fOv = Arr.append(fOv, text);
                                }
                            }
                        }
                    } catch (Exception ex) {
                    }
                }

            }
            return new Overlay[][]{pOv, fOv};
        }
        return null;
    }

    public static class UserOverlay {

        String name;
        Overlay obj;
        String[] channels;
    }
    ArrayList<UserOverlay> userOverlayConfig;

    protected void parseUserOverlays() {
        Properties userOverlays = new Properties();
        userOverlayConfig = new ArrayList<>();
        if (userOverlaysConfigFile != null) {
            try {
                try (FileInputStream in = new FileInputStream(userOverlaysConfigFile)) {
                    userOverlays.load(in);

                    for (String name : userOverlays.stringPropertyNames()) {
                        String val = userOverlays.getProperty(name);
                        try {
                            UserOverlay uo = new UserOverlay();
                            uo.name = name;
                            String type = val.substring(0, val.indexOf("(")).trim();
                            String pars = val.substring(val.indexOf("(") + 1, val.lastIndexOf(")")).trim();
                            String[] tokens = pars.split(",");
                            for (int i = 0; i < tokens.length; i++) {
                                tokens[i] = tokens[i].trim();
                            }
                            Color color = Color.GRAY;
                            try {
                                color = (Color) Color.class.getField(tokens[tokens.length - 1].toUpperCase()).get(null);
                            } catch (Exception ex) {
                            }
                            Pen pen = new Pen(color);
                            try {
                                String[] penTokens = tokens[tokens.length - 1].split(":");
                                color = (Color) Color.class.getField(penTokens[0].toUpperCase()).get(null);
                                int width = Integer.valueOf(penTokens[1]);
                                Pen.LineStyle style = Pen.LineStyle.valueOf(penTokens[2]);
                                pen = new Pen(color, width, style);
                            } catch (Exception ex) {
                            }
                            switch (type) {
                                case "Point":
                                    uo.obj = new Overlays.Crosshairs();
                                    uo.obj.setSize(new Dimension(Integer.valueOf(tokens[2]), Integer.valueOf(tokens[3])));
                                    break;
                                case "Line":
                                    uo.obj = new Overlays.Line();
                                    break;
                                case "Arrow":
                                    uo.obj = new Overlays.Arrow();
                                    break;
                                case "Rect":
                                    uo.obj = new Overlays.Rect();
                                    break;
                                case "Ellipse":
                                    uo.obj = new Overlays.Ellipse();
                                    break;
                                case "Polyline":
                                    uo.obj = new Overlays.Polyline();
                                    break;
                            }
                            if (type.equals("Polyline") || type.equals("Point")) {
                                uo.channels = new String[]{tokens[0], tokens[1]};
                            } else {
                                uo.channels = new String[]{tokens[0], tokens[1], tokens[2], tokens[3]};
                            }
                            uo.obj.setPen(pen);
                            userOverlayConfig.add(uo);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected static Overlay[] getUserOverlays(ArrayList<UserOverlay> userOverlayConfig, Renderer renderer, ImageData id) {
        ArrayList<Overlay> ret = new ArrayList<>();
        if (id != null) {

            for (UserOverlay uo : userOverlayConfig) {
                try {
                    Overlay ov = uo.obj;
                    //Overlay ov = (Overlay)uo.cls.newInstance();
                    ov.setCalibration(renderer.getCalibration());
                    boolean valid = false;
                    if (ov instanceof Overlays.Polyline) {
                        double[] x = (uo.channels[0].equals("null")) ? null : id.getDoubleArray(uo.channels[0]);
                        double[] y = (uo.channels[1].equals("null")) ? null : id.getDoubleArray(uo.channels[1]);
                        if ((x != null) || (y != null)) {
                            if (x == null) {
                                x = (renderer.getCalibration() == null) ? Arr.indexesDouble(y.length) : renderer.getCalibration().getAxisX(y.length);
                            }
                            if (y == null) {
                                y = (renderer.getCalibration() == null) ? Arr.indexesDouble(x.length) : renderer.getCalibration().getAxisY(x.length);
                            }
                            ((Overlays.Polyline) ov).updateAbsolute(x, y);
                            valid = true;
                        }
                    } else {
                        Double x = id.getDouble(uo.channels[0]);
                        Double y = id.getDouble(uo.channels[1]);
                        if ((x != null) && (y != null)) {
                            PointDouble position = new PointDouble(x, y);
                            ov.setAbsolutePosition(position);
                            if (!(ov instanceof Overlays.Crosshairs)) {
                                Double x2 = id.getDouble(uo.channels[2]);
                                Double y2 = id.getDouble(uo.channels[3]);
                                if ((x != null) && (y != null)) {
                                    DimensionDouble size = new DimensionDouble(x2 - position.x, y2 - position.y);
                                    ov.setAbsoluteSize(size);
                                    valid = true;
                                }
                            } else {
                                valid = true;
                            }
                        }
                    }
                    if (valid) {
                        ret.add(ov);
                    }
                } catch (Exception ex) {
                    //ex.printStackTrace();
                }
            }
        }
        return ret.toArray(new Overlay[0]);
    }

    public static double[] getComRms(double[] arr, double[] x) {
        if (arr != null) {
            double xmd = 0;
            double xmd2 = 0;
            double total = 0;
            for (int i = 0; i < arr.length; i++) {
                double v = (arr[i] * x[i]);
                xmd += v;
                xmd2 += (v * x[i]);
                total += arr[i];
            }
            if (total > 0) {
                double com = xmd / total;
                double com2 = xmd2 / total;
                double rms = Math.sqrt(Math.abs(com2 - com * com));
                return new double[]{com, rms};
            }
        }
        return new double[]{Double.NaN, Double.NaN};
    }

    public static double[] fitGaussian(double[] y, int[] x) {
        try {
            ArrayProperties pY = ArrayProperties.get(y);
            GaussianCurveFitter fitter = GaussianCurveFitter.create().withStartPoint(new double[]{(pY.max - pY.min) / 2, x[pY.maxIndex], 1.0}).withMaxIterations(1000);
            ArrayList<WeightedObservedPoint> values = new ArrayList<>();
            for (int i = 0; i < y.length; i++) {
                values.add(new WeightedObservedPoint(1.0, x[i], y[i]));
            }
            return fitter.fit(values);
        } catch (Exception ex) {
            return null;
        }

    }

    public static double[] fitPolynomial(PointDouble[] points, int order) {
        double[] y = new double[points.length];
        double[] x = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            x[i] = points[i].x;
            y[i] = points[i].y;
        }
        return fitPolynomial(y, x, order);
    }

    public static double[] fitPolynomial(double[] y, double[] x, int order) {
        try {
            ArrayProperties pY = ArrayProperties.get(y);
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(order).withMaxIterations(1000);
            ArrayList<WeightedObservedPoint> values = new ArrayList<>();
            for (int i = 0; i < y.length; i++) {
                values.add(new WeightedObservedPoint(1.0, x[i], y[i]));
            }
            return fitter.fit(values);
        } catch (Exception ex) {
            return null;
        }

    }

    public static double[] getFitFunction(double[] pars, int[] x) {
        double[] fit = new double[x.length];
        Gaussian g = new Gaussian(pars[0], pars[1], pars[2]);
        for (int i = 0; i < x.length; i++) {
            fit[i] = g.value(x[i]);
        }
        return fit;
    }

    protected void setHistogramVisible(boolean value) {
        if (value) {
            if ((histogramDialog == null) || (!histogramDialog.isShowing())) {
                Histogram histogram = new Histogram(true);
                histogram.setRenderer(renderer);
                histogramDialog = SwingUtils.showDialog(SwingUtils.getWindow(renderer), "Histogram", null, histogram);
                renderer.refresh();
            }
        } else {
            if (histogramDialog != null) {
                histogramDialog.setVisible(false);
                histogramDialog = null;
            }
        }
    }

    protected void centralizeRenderer() {
        Point center = null;
        Dimension size = renderer.getImageSize();
        double zoom = (renderer.getMode() == RendererMode.Fixed) ? 1.0 : renderer.getZoom();
        if (renderer.getCalibration() != null) {
            center = renderer.getCalibration().getCenter();
        } else if (size != null) {
            center = new Point(size.width / 2, size.height / 2);
        }
        if (center != null) {
            Point topleft = new Point(Math.max((int) (center.x - renderer.getWidth() / 2 / zoom), 0),
                    Math.max((int) (center.y - renderer.getHeight() / 2 / zoom), 0));
            renderer.setViewPosition(topleft);
        }
    }

    protected void updatePause() {
        int index = ((int) pauseSelection.getValue()) - 1;
        synchronized (imageBuffer) {
            if (index < imageBuffer.size()) {
                Data data = imageBuffer.get(index).data;
                long pid = imageBuffer.get(index).cache.getPulseId();
                BufferedImage image = camera.generateImage(data);
                renderer.setImage(renderer.getOrigin(), image, data);

                String text = "PID: " + pid;
                if (imagePauseOverlay == null) {
                    Font font = new Font("Verdana", Font.PLAIN, 12);
                    Dimension d = SwingUtils.getTextSize(text, renderer.getFontMetrics(font));
                    imagePauseOverlay = new Text(renderer.getPenErrorText(), "", font, new Point(-20 - d.width, 42));
                    imagePauseOverlay.setFixed(true);
                    imagePauseOverlay.setAnchor(Overlay.ANCHOR_VIEWPORT_OR_IMAGE_TOP_RIGHT);
                    renderer.addOverlay(imagePauseOverlay);
                }
                //imagePauseOverlay.update(Chrono.getTimeStr(data.getTimestamp(), "HH:mm:ss.SSS"));
                imagePauseOverlay.update(text);
                manageFit(image, data);
                manageUserOverlays(image, data);
            }
        }
        updateStreamData();
    }

    protected void removePauseOverlay() {
        renderer.removeOverlay(imagePauseOverlay);
        imagePauseOverlay = null;
    }

    protected String getSnapshotPath() {
        return "."; //TODO
    }

    public static Map<String, Object> getProcessingParameters(StreamValue value) throws IOException {
        return (Map) JsonSerializer.decode(value.getValue("processing_parameters").toString(), Map.class);
    }

    StandardDialog dataTableDialog;
    DefaultTableModel dataTableModel;
    JTable dataTable;

    protected void showStreamData() {
        dataTableModel = null;
        if (camera != null) {

            if ((dataTableDialog != null) && (dataTableDialog.isShowing())) {
                SwingUtils.centerComponent(getTopLevel(), dataTableDialog);
                dataTableDialog.requestFocus();
                return;
            }
            dataTableModel = new DefaultTableModel(new Object[0][2], new String[]{"Name", "Value"}) {
                public Class getColumnClass(int columnIndex) {
                    return String.class;
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false;
                }
            };
            updateStreamData();
            StreamValue val = camera.getValue();
            dataTable = new JTable(dataTableModel);
            dataTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            dataTable.setCellSelectionEnabled(true);
            dataTable.getTableHeader().setReorderingAllowed(false);
            dataTable.getTableHeader().setResizingAllowed(true);
            Window w = getTopLevel();

            if (w instanceof Dialog) {
                dataTableDialog = new StandardDialog((Dialog) w, "Image Data", false);
            } else {
                dataTableDialog = new StandardDialog((java.awt.Frame) w, "Image Data", false);
            }

            dataTableDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setViewportView(dataTable);
            scrollPane.setPreferredSize(new Dimension(300, 400));
            dataTableDialog.setContentPane(scrollPane);
            dataTableDialog.pack();
            dataTableDialog.setVisible(true);
            dataTableDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dataTableModel = null;
                }
            });
            dataTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int row = dataTable.getSelectedRow();
                        int col = dataTable.getSelectedColumn();
                        dataTable.setToolTipText(null);
                        if (row > 1) {
                            String id = String.valueOf(dataTable.getModel().getValueAt(row, 0));
                            String locator = String.valueOf(dataTable.getModel().getValueAt(0, 1));
                            String channelId = locator + " " + id;
                            dataTable.setToolTipText(channelId);
                            if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                                if (col == 0) {
                                    SwingUtils.showMessage(dataTableDialog, "Channel Identifier", "Copied to clipboard: " + channelId);
                                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                    clipboard.setContents(new StringSelection(channelId), (Clipboard clipboard1, Transferable contents) -> {
                                    });
                                } else {
                                    Object obj = getCurrentFrame().cache.getValue(id);
                                    if (id.equals("image")) {
                                    } else if (id.equals("processing_parameters")) {
                                        Map<String, Object> pars = getProcessingParameters(getCurrentFrame().cache);
                                        StringBuilder sb = new StringBuilder();
                                        for (String key : pars.keySet()) {
                                            sb.append(key).append(" = ").append(Str.toString(pars.get(key), 10)).append("\n");
                                        }
                                        SwingUtils.showMessage(dataTableDialog, "Processing Parameters", sb.toString());
                                    } else if ((obj != null) && (obj.getClass().isArray() || (obj instanceof Number))) {
                                        DeviceValueChart chart = new DeviceValueChart();
                                        Device dev = null;
                                        if (obj.getClass().isArray()) {
                                            dev = new ReadableRegisterArray(new ReadableArray() {
                                                @Override
                                                public Object read() throws IOException, InterruptedException {
                                                    return Convert.toDouble(getCurrentFrame().cache.getValue(id));
                                                }

                                                @Override
                                                public int getSize() {
                                                    return Array.getLength(getCurrentFrame().cache.getValue(id));
                                                }
                                            });
                                        } else {
                                            dev = new ReadableRegisterNumber(new ReadableNumber() {
                                                @Override
                                                public Object read() throws IOException, InterruptedException {
                                                    return Convert.toDouble(getCurrentFrame().cache.getValue(id));
                                                }
                                            });
                                        }
                                        dev.setPolling(1000);
                                        chart.setDevice(dev);
                                        JDialog dlg = SwingUtils.showDialog(dataTableDialog, getDisplayName() + " " + id, null, chart);
                                        //TODO:
                                        //PlotBase plot = chart.getPlot();
                                        //if (plot!=null){
                                        //   plot.setPlotBackgroundColor(Color.BLACK);
                                        //}
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        SwingUtils.showException(StreamCameraViewer.this, ex);
                    }
                }
            });
            SwingUtils.centerComponent(getTopLevel(), dataTableDialog);
            updateStreamData();
        }
    }

    volatile boolean updatingStreamData = false;

    protected void updateStreamData() {
        if ((dataTableDialog == null) || !dataTableDialog.isShowing() || updatingStreamData) {
            return;
        }
        updatingStreamData = true;
        SwingUtilities.invokeLater(() -> {
            updatingStreamData = false;
            if ((dataTableModel != null) && (camera != null)) {
                StreamValue value = camera.getValue();
                Frame frame = getCurrentFrame();
                int[] sel_rows = (dataTable == null) ? null : dataTable.getSelectedRows();
                int[] sel_cols = (dataTable == null) ? null : dataTable.getSelectedColumns();
                List<String> ids = (value == null) ? new ArrayList<>() : new ArrayList(value.getIdentifiers());
                if (ids.size() + 4 != dataTableModel.getRowCount()) {
                    dataTableModel.setNumRows(0);

                    try {
                        String locator = (server == null) ? ""
                                : server.getUrl() + "/" + ((value == null) ? instanceName : server.getCurrentInstance());
                        dataTableModel.addRow(new Object[]{"Locator", locator});
                    } catch (Exception ex) {
                        dataTableModel.addRow(new Object[]{"Locator", ex.getMessage()});
                    }
                    try {
                        dataTableModel.addRow(new Object[]{"Stream", stream});
                    } catch (Exception ex) {
                        dataTableModel.addRow(new Object[]{"Stream", ex.getMessage()});
                    }
                    dataTableModel.addRow(new Object[]{"PID", ""});
                    dataTableModel.addRow(new Object[]{"Timestamp", ""});
                    Collections.sort(ids);
                    for (String id : ids) {
                        dataTableModel.addRow(new Object[]{id, ""});
                    }
                }

                if ((frame != null) && (frame.cache != null)) {
                    dataTableModel.setValueAt(frame.cache.getPulseId(), 2, 1); //PID
                    dataTableModel.setValueAt(frame.cache.getTimestamp(), 3, 1); //Timestamp
                    for (int i = 4; i < dataTableModel.getRowCount(); i++) {
                        String id = String.valueOf(dataTableModel.getValueAt(i, 0));
                        //Object obj = server.getValue(id);
                        Object obj = frame.cache.getValue(id);
                        if (obj != null) {
                            if (obj.getClass().isArray()) {
                                obj = obj.getClass().getComponentType().getSimpleName() + "[" + Array.getLength(obj) + "]";
                            } else if (obj instanceof Double) {
                                obj = Convert.roundDouble((Double) obj, 1);
                            } else if (obj instanceof Float) {
                                obj = Convert.roundDouble((Float) obj, 1);
                            }
                        }
                        dataTableModel.setValueAt(String.valueOf(obj), i, 1);
                    }
                }
                if ((sel_rows != null) && (sel_rows.length > 0)) {
                    //dataTable.setRowSelectionInterval((Integer)Arr.getMin(sel_rows), (Integer)Arr.getMax(sel_rows));
                    dataTable.setRowSelectionInterval(sel_rows[0], sel_rows[sel_rows.length - 1]);
                }
                if ((sel_cols != null) && (sel_cols.length > 0)) {
                    //dataTable.setColumnSelectionInterval((Integer)Arr.getMin(sel_cols), (Integer)Arr.getMax(sel_cols));
                    dataTable.setColumnSelectionInterval(sel_cols[0], sel_cols[sel_cols.length - 1]);
                }
            }
        });
    }

    void updateZoom() {
        try {
            buttonZoomStretch.setSelected(renderer.getMode() == RendererMode.Stretch);
            buttonZoomFit.setSelected(renderer.getMode() == RendererMode.Fit);
            if (renderer.getMode() == RendererMode.Fixed) {
                buttonZoomNormal.setSelected(true);
            } else if (renderer.getMode() == RendererMode.Zoom) {
                if (renderer.getZoom() == 1) {
                    buttonZoomNormal.setSelected(true);
                } else if (renderer.getZoom() == 0.5) {
                    buttonZoom05.setSelected(true);
                } else if (renderer.getZoom() == 0.25) {
                    buttonZoom025.setSelected(true);
                } else if (renderer.getZoom() == 2.0) {
                    buttonZoom2.setSelected(true);
                } else {
                    buttonGroup1.clearSelection();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    boolean updatingColormap;

    void updateColormap() {
        updatingColormap = true;
        try {
            if ((camera != null) && (camera instanceof ColormapSource)) {
                ColormapSource.ColormapSourceConfig config = ((ColormapSource) camera).getConfig();
                comboColormap.setSelectedItem(config.colormap);
                if (config.isDefaultColormap()) {
                    buttonFullRange.setSelected(true);
                } else if (config.colormapAutomatic) {
                    buttonAutomatic.setSelected(true);
                } else {
                    buttonManual.setSelected(true);
                }
                btFixColormapRange.setVisible(buttonAutomatic.isSelected());
                spinnerMin.setEnabled(buttonManual.isSelected());
                spinnerMax.setEnabled(buttonManual.isSelected());
                if (!Double.isNaN(config.colormapMin)) {
                    spinnerMin.setValue(Math.min(Math.max((int) config.colormapMin, 0), 65535));
                }
                if (!Double.isNaN(config.colormapMax)) {
                    spinnerMax.setValue(Math.min(Math.max((int) config.colormapMax, 0), 65535));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        updatingColormap = false;
    }

    boolean updatingServerControls;

    void updatePipelineControls() {
        if (server != null) {
            updatingServerControls = true;
            if (server.isStarted()) {
                try {
                    checkBackground.setSelected(server.getBackgroundSubtraction());
                    Double threshold = (server.getThreshold());
                    checkThreshold.setSelected(threshold != null);
                    spinnerThreshold.setValue((threshold == null) ? 0 : threshold);
                    Map<String, Object> gr = (server.getGoodRegion());
                    checkGoodRegion.setSelected(gr != null);
                    if (gr != null) {
                        spinnerGrThreshold.setValue(((Number) gr.get("threshold")).doubleValue());
                        spinnerGrScale.setValue(((Number) gr.get("gfscale")).doubleValue());
                    }
                    Map rotation = server.getRotation();
                    checkRotation.setSelected(rotation != null);
                    if (rotation != null) {
                        spinnerRotationAngle.setValue(((Number) rotation.get("angle")).doubleValue());
                        spinnerRotationOrder.setValue(((Number) rotation.get("order")).intValue());
                        String mode = (String) rotation.get("mode");
                        try {
                            spinnerRotationConstant.setValue(Double.valueOf(mode));
                            spinnerRotationMode.setValue("constant");
                        } catch (Exception ex) {
                            spinnerRotationConstant.setValue(0);
                            spinnerRotationMode.setValue(mode);
                        }
                    }

                    Map<String, Object> slicing = (server.getSlicing());
                    checkSlicing.setSelected(slicing != null);
                    if (slicing != null) {
                        spinnerSlNumber.setValue(((Number) slicing.get("number_of_slices")).intValue());
                        spinnerSlScale.setValue(((Number) slicing.get("scale")).doubleValue());
                        spinnerSlOrientation.setValue((String) slicing.get("orientation"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public ImageIcon getIcon(String name) {
        ImageIcon ret = null;
        try {
            String dir = getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "resosurces/";
            if (new File(dir + name + ".png").exists()) {
                ret = new javax.swing.ImageIcon(dir + name + ".png");
            } else {
                ret = new ImageIcon(ch.psi.pshell.ui.App.class.getResource("/ch/psi/pshell/ui/" + name + ".png"));
                if (MainFrame.isDark()) {
                    try {
                        ret = new ImageIcon(ch.psi.pshell.ui.App.class.getResource("/ch/psi/pshell/ui/dark/" + name + ".png"));
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    public static String getIconName(JButton button) {
        String ret = button.getIcon().toString();
        if (ret.indexOf(".") > 0) {
            ret = ret.substring(0, ret.indexOf("."));
        }
        return ret;
    }

    public void setPaused(boolean paused) {
        removePauseOverlay();
        if (camera != null) {
            synchronized (imageBuffer) {
                if (paused) {
                    renderer.pause();
                    panelStream.setVisible(false);
                    pauseSelection.setVisible(true);
                    if (imageBuffer.size() > 0) {
                        pauseSelection.setEnabled(true);
                        pauseSelection.setMaxValue(imageBuffer.size());
                        pauseSelection.setValue(imageBuffer.size());
                        updatePause();
                    } else {
                        pauseSelection.setEnabled(false);
                        pauseSelection.setMaxValue(1);
                        pauseSelection.setValue(1);
                    }
                } else {
                    imageBuffer.clear();
                    renderer.resume();
                    //renderer.clear();
                    pauseSelection.setVisible(false);
                    panelStream.setVisible(true);
                }
            }
            updateStreamData();
        }
    }

    public void setShowProfile(boolean value) {
        showProfile = value;
        if (value) {
            renderer.setProfile(Renderer.Profile.None);
        } else {
            renderer.removeOverlays(profileOv);
            profileOv = null;
        }
        updateButtons();
    }

    public void setShowFit(boolean value) {
        showFit = value;
        if (showFit) {
            renderer.setProfile(Renderer.Profile.None);
        } else {
            renderer.removeOverlays(fitOv);
            fitOv = null;
        }
        updateButtons();
    }

    public void saveSnapshot() throws Exception {
        boolean paused = isPaused();
        try {
            if (!paused) {
                setPaused(true);
            }
            String snapshotFile = null;
            synchronized (imageBuffer) {
                Frame frame = getCurrentFrame();
                if (frame == null) {
                    throw new Exception("No current image");
                }
                ArrayList<Frame> frames = new ArrayList<>();
                frames.add(frame);
                snapshotFile = this.saveFrames(getDisplayName() + "_snapshot", frames, null);
                if (snapshotFile != null) {
                    //renderer.saveSnapshot(snapshotFile, "png", true);
                    ImageBuffer.saveImage(SwingUtils.createImage(renderer), snapshotFile + ".png", "png");
                    SwingUtils.showMessage(getTopLevel(), "Success", "Created data file:\n" + snapshotFile);
                }
            }

        } finally {
            if (!paused) {
                setPaused(false);
            }
        }

    }

    public void saveStack() throws Exception {
        String snapshotFile = null;
        synchronized (imageBuffer) {
            snapshotFile = saveFrames(getDisplayName() + "_camera_stack", imageBuffer, null);
        }
        if (snapshotFile != null) {
            SwingUtils.showMessage(getTopLevel(), "Success", "Created data file:\n" + snapshotFile);
        }
    }

    protected String saveFrames(String name, ArrayList<Frame> frames, Map<String, Object> attributes) throws Exception {
        ArrayList<StreamValue> values = new ArrayList<>();
        for (Frame frame : frames) {
            values.add(frame.cache);
        }
        return saveImages(name, values, attributes);
    }

    protected String saveImages(String name, ArrayList<StreamValue> images, Map<String, Object> attributes) throws Exception {
        name = name.replaceAll("/", "");
        int depth = images.size();
        if (depth == 0) {
            return null;
        }
        StreamValue first = images.get(0);
        String pathRoot = "/camera1/";
        String pathImage = pathRoot + "image";
        String pathPid = pathRoot + "pulse_id";
        String pathTimestampStr = pathRoot + "timestamp_str";
        Map<String, Object> processingPars = getProcessingParameters(first);
        String camera = (String) processingPars.get("camera_name");

        int width = ((Number) first.getValue("width")).intValue();
        int height = ((Number) first.getValue("height")).intValue();
        Class dataType = first.getValue("image").getClass().getComponentType();

        DataManager dm;
        if (Context.getInstance() != null) {
            Context.getInstance().setExecutionPars(name);
            dm = Context.getInstance().getDataManager();
        } else {
            String fileName = Setup.expand("{date}_{time}_") + name + ".h5";
            String path = App.getArgumentValue("data");
            if (path == null) {
                path = Sys.getUserHome();
            }
            dm = new DataManager(Paths.get(path, fileName).toFile(), "h5");
        }

        //Create tables            
        dm.createDataset(pathImage, dataType, new int[]{depth, height, width});
        dm.createDataset(pathPid, Long.class, new int[]{depth});
        dm.createDataset(pathTimestampStr, String.class, new int[]{depth});
        for (String id : first.getIdentifiers()) {
            Object val = first.getValue(id);
            if (id.equals("image")) {
            } else if (id.equals("processing_parameters")) {
                Map<String, Object> pars = getProcessingParameters(first);
                for (String key : pars.keySet()) {
                    if ((pars.get(key) != null) && (pars.get(key) instanceof Map)) {
                        for (Object k : ((Map) pars.get(key)).keySet()) {
                            Object v = ((Map) pars.get(key)).get(k);
                            dm.setAttribute(pathImage, key + " " + k, (v == null) ? "" : v);
                        }
                    } else {
                        Object value = pars.get(key);
                        if (value == null) {
                            value = "";
                        } else if (value instanceof List) {
                            Class cls = (((List) value).size() > 0) ? ((List) value).get(0).getClass() : double.class;
                            value = Convert.toPrimitiveArray(value, cls);
                            //value = Convert.toDouble(value);
                        }
                        dm.setAttribute(pathImage, key, value);
                    }
                }
            } else if (val.getClass().isArray()) {
                dm.createDataset(pathRoot + id, Double.class, new int[]{depth, Array.getLength(val)});
            } else {
                dm.createDataset(pathRoot + id, val.getClass(), new int[]{depth});
            }
        }

        //Add metadata
        dm.setAttribute(pathRoot, "Camera", camera);
        dm.setAttribute(pathRoot, "Images", depth);
        dm.setAttribute(pathRoot, "Interval", -1);
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                dm.setAttribute(pathRoot, key, String.valueOf(attributes.get(key)));
            }
        }

        //Save data
        for (int index = 0; index < depth; index++) {
            StreamValue streamValue = images.get(index);
            dm.setItem(pathImage, streamValue.getValue("image"), new long[]{index, 0, 0}, new int[]{1, height, width});
            dm.setItem(pathPid, streamValue.getPulseId(), index);
            dm.setItem(pathTimestampStr, Chrono.getTimeStr(streamValue.getTimestamp(), "YYYY-MM-dd HH:mm:ss.SSS"), index);

            for (String id : streamValue.getIdentifiers()) {
                Object val = streamValue.getValue(id);
                if (id.equals("image")) {
                } else if (id.equals("processing_parameters")) {
                } else if (val.getClass().isArray()) {
                    dm.setItem(pathRoot + id, val, index);
                } else {
                    dm.setItem(pathRoot + id, val, index);
                }
            }
        }
        dm.closeOutput();

        return dm.getRootFileName();
    }

    public String getCameraName() throws IOException {
        return (String) getProcessingParameters(camera.getValue()).get("camera_name");
    }

    public String getDisplayName() {
        try {
            return getCameraName();
        } catch (Exception ex) {
            return stream;
        }
    }

    public void grabBackground() throws Exception {
        if (camera != null) {
            System.out.println("Grabbing background for: " + getDisplayName());
            if (server != null) {
                server.captureBackground(5);
            } else {
                camera.captureBackground(5, 0);
            }
            SwingUtils.showMessage(getTopLevel(), "Success", "Success capturing background", 5000);
        }
    }

    public boolean isPaused() {
        return renderer.isPaused();
    }

    protected Window getTopLevel() {
        return (Window) this.getTopLevelAncestor();
    }

    public static void main(String[] args) throws Exception {
        App.init(args);
        SwingUtilities.invokeLater(() -> {
            try {
                StreamCameraViewer iv = new StreamCameraViewer();
                if (App.hasArgument("server")) {
                    iv.setServer(App.getArgumentValue("server"));
                    if (App.hasArgument("mode")){
                       iv.setSourceSelecionMode(SourceSelecionMode.valueOf(App.getArgumentValue("mode")));
                    }                                       
                }
                if (App.hasArgument("stream")) {
                    iv.setStream(App.getArgumentValue("stream"));
                } 
                Window window = SwingUtils.showFrame(null, "Stream Camera Viewer", new Dimension(800, 600), iv);                             
                window.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            } catch (Exception ex) {
                Logger.getLogger(StreamCameraViewer.class.getName()).log(Level.SEVERE, null, ex);
            }

        });

    }

    ////////
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        topPanel = new javax.swing.JPanel();
        toolBar = new javax.swing.JToolBar();
        buttonSidePanel = new javax.swing.JToggleButton();
        buttonStreamData = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonGrabBackground = new javax.swing.JButton();
        buttonPause = new javax.swing.JToggleButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        buttonMarker = new javax.swing.JToggleButton();
        buttonProfile = new javax.swing.JToggleButton();
        buttonFit = new javax.swing.JToggleButton();
        buttonReticle = new javax.swing.JToggleButton();
        buttonScale = new javax.swing.JToggleButton();
        buttonTitle = new javax.swing.JToggleButton();
        pauseSelection = new ch.psi.pshell.swing.ValueSelection();
        panelStream = new javax.swing.JPanel();
        textStream = new javax.swing.JTextField();
        renderer = new ch.psi.pshell.imaging.Renderer();
        sidePanel = new javax.swing.JPanel();
        panelPipeline = new javax.swing.JPanel();
        checkThreshold = new javax.swing.JCheckBox();
        spinnerThreshold = new javax.swing.JSpinner();
        checkBackground = new javax.swing.JCheckBox();
        checkGoodRegion = new javax.swing.JCheckBox();
        spinnerGrScale = new javax.swing.JSpinner();
        spinnerGrThreshold = new javax.swing.JSpinner();
        labelGrThreshold = new javax.swing.JLabel();
        labelGrScale = new javax.swing.JLabel();
        panelSlicing = new javax.swing.JPanel();
        checkSlicing = new javax.swing.JCheckBox();
        labelSlScale = new javax.swing.JLabel();
        spinnerSlScale = new javax.swing.JSpinner();
        labelSlNumber = new javax.swing.JLabel();
        spinnerSlNumber = new javax.swing.JSpinner();
        labelSlOrientation = new javax.swing.JLabel();
        spinnerSlOrientation = new javax.swing.JSpinner();
        checkRotation = new javax.swing.JCheckBox();
        spinnerRotationAngle = new javax.swing.JSpinner();
        spinnerRotationOrder = new javax.swing.JSpinner();
        labelOrder = new javax.swing.JLabel();
        labelMode = new javax.swing.JLabel();
        spinnerRotationMode = new javax.swing.JSpinner();
        labelAngle = new javax.swing.JLabel();
        labelConstant = new javax.swing.JLabel();
        spinnerRotationConstant = new javax.swing.JSpinner();
        panelZoom = new javax.swing.JPanel();
        buttonZoomFit = new javax.swing.JRadioButton();
        buttonZoomStretch = new javax.swing.JRadioButton();
        buttonZoomNormal = new javax.swing.JRadioButton();
        buttonZoom025 = new javax.swing.JRadioButton();
        buttonZoom05 = new javax.swing.JRadioButton();
        buttonZoom2 = new javax.swing.JRadioButton();
        panelColormap = new javax.swing.JPanel();
        checkHistogram = new javax.swing.JCheckBox();
        comboColormap = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        buttonFullRange = new javax.swing.JRadioButton();
        buttonManual = new javax.swing.JRadioButton();
        buttonAutomatic = new javax.swing.JRadioButton();
        labelMin = new javax.swing.JLabel();
        spinnerMin = new javax.swing.JSpinner();
        spinnerMax = new javax.swing.JSpinner();
        labelMax = new javax.swing.JLabel();
        btFixColormapRange = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(873, 600));

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        buttonSidePanel.setIcon(getIcon("List"));
        buttonSidePanel.setText(" ");
        buttonSidePanel.setToolTipText("Show Side Panel");
        buttonSidePanel.setFocusable(false);
        buttonSidePanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSidePanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSidePanelActionPerformed(evt);
            }
        });
        toolBar.add(buttonSidePanel);

        buttonStreamData.setIcon(getIcon("Details"));
        buttonStreamData.setText(" ");
        buttonStreamData.setToolTipText("Show Data Window");
        buttonStreamData.setFocusable(false);
        buttonStreamData.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonStreamData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStreamDataActionPerformed(evt);
            }
        });
        toolBar.add(buttonStreamData);

        buttonSave.setIcon(getIcon("Save"));
        buttonSave.setText(" ");
        buttonSave.setToolTipText("Show Data Window");
        buttonSave.setFocusable(false);
        buttonSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });
        toolBar.add(buttonSave);

        buttonGrabBackground.setIcon(getIcon("Background"));
        buttonGrabBackground.setText(" ");
        buttonGrabBackground.setToolTipText("Grab Background");
        buttonGrabBackground.setFocusable(false);
        buttonGrabBackground.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonGrabBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGrabBackgroundActionPerformed(evt);
            }
        });
        toolBar.add(buttonGrabBackground);

        buttonPause.setIcon(getIcon("Pause"));
        buttonPause.setText(" ");
        buttonPause.setToolTipText("Pause");
        buttonPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPauseActionPerformed(evt);
            }
        });
        toolBar.add(buttonPause);

        jSeparator6.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator6.setPreferredSize(new java.awt.Dimension(20, 0));
        jSeparator6.setRequestFocusEnabled(false);
        toolBar.add(jSeparator6);

        buttonMarker.setIcon(getIcon("Marker"));
        buttonMarker.setText(" ");
        buttonMarker.setToolTipText("Show Marker");
        buttonMarker.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonMarker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMarkerActionPerformed(evt);
            }
        });
        toolBar.add(buttonMarker);

        buttonProfile.setIcon(getIcon("Profile"
            + ""));
    buttonProfile.setSelected(true);
    buttonProfile.setText(" ");
    buttonProfile.setToolTipText("Show Image Profile");
    buttonProfile.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonProfile.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonProfileActionPerformed(evt);
        }
    });
    toolBar.add(buttonProfile);

    buttonFit.setIcon(getIcon("Fit"));
    buttonFit.setSelected(true);
    buttonFit.setText(" ");
    buttonFit.setToolTipText("Show Fit");
    buttonFit.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonFit.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonFitActionPerformed(evt);
        }
    });
    toolBar.add(buttonFit);

    buttonReticle.setIcon(getIcon("Reticule"));
    buttonReticle.setSelected(true);
    buttonReticle.setText(" ");
    buttonReticle.setToolTipText("Show Reticle");
    buttonReticle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonReticle.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonReticleActionPerformed(evt);
        }
    });
    toolBar.add(buttonReticle);

    buttonScale.setIcon(getIcon("Scale"));
    buttonScale.setSelected(true);
    buttonScale.setText(" ");
    buttonScale.setToolTipText("Show Colormap Scale");
    buttonScale.setFocusable(false);
    buttonScale.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonScale.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonScaleActionPerformed(evt);
        }
    });
    toolBar.add(buttonScale);

    buttonTitle.setIcon(getIcon("Title"));
    buttonTitle.setText(" ");
    buttonTitle.setToolTipText("Show Camera Name");
    buttonTitle.setFocusable(false);
    buttonTitle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonTitle.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonTitleActionPerformed(evt);
        }
    });
    toolBar.add(buttonTitle);

    pauseSelection.setDecimals(0);

    textStream.setHorizontalAlignment(javax.swing.JTextField.CENTER);
    textStream.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            textStreamActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout panelStreamLayout = new javax.swing.GroupLayout(panelStream);
    panelStream.setLayout(panelStreamLayout);
    panelStreamLayout.setHorizontalGroup(
        panelStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelStreamLayout.createSequentialGroup()
            .addComponent(textStream, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
            .addContainerGap())
    );
    panelStreamLayout.setVerticalGroup(
        panelStreamLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelStreamLayout.createSequentialGroup()
            .addGap(2, 2, 2)
            .addComponent(textStream, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(1, 1, 1))
    );

    javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
    topPanel.setLayout(topPanelLayout);
    topPanelLayout.setHorizontalGroup(
        topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, topPanelLayout.createSequentialGroup()
            .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(panelStream, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(pauseSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );
    topPanelLayout.setVerticalGroup(
        topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
        .addGroup(topPanelLayout.createSequentialGroup()
            .addGap(1, 1, 1)
            .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(pauseSelection, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(panelStream, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
    );

    panelPipeline.setBorder(javax.swing.BorderFactory.createTitledBorder("Pipeline"));

    checkThreshold.setText("Threshold");
    checkThreshold.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            checkThresholdActionPerformed(evt);
        }
    });

    spinnerThreshold.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, 99999.0d, 1.0d));
    spinnerThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerThresholdonChange(evt);
        }
    });

    checkBackground.setText("Subtract Background");
    checkBackground.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            checkBackgroundActionPerformed(evt);
        }
    });

    checkGoodRegion.setText("Good Region");
    checkGoodRegion.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            checkGoodRegionActionPerformed(evt);
        }
    });

    spinnerGrScale.setModel(new javax.swing.SpinnerNumberModel(3.0d, 0.01d, 99999.0d, 1.0d));
    spinnerGrScale.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerGrScalespinnerGrThresholdonChange(evt);
        }
    });

    spinnerGrThreshold.setModel(new javax.swing.SpinnerNumberModel(0.5d, 0.04d, 1.0d, 0.1d));
    spinnerGrThreshold.setPreferredSize(new java.awt.Dimension(92, 20));
    spinnerGrThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerGrThresholdonChange(evt);
        }
    });

    labelGrThreshold.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelGrThreshold.setText("Threshold:");

    labelGrScale.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelGrScale.setText("Scale:");

    checkSlicing.setText("Slicing");
    checkSlicing.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            checkSlicingActionPerformed(evt);
        }
    });

    labelSlScale.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelSlScale.setText("Scale:");

    spinnerSlScale.setModel(new javax.swing.SpinnerNumberModel(3.0d, 0.01d, 99999.0d, 1.0d));
    spinnerSlScale.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerSlScalespinnerSlicingChange(evt);
        }
    });

    labelSlNumber.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelSlNumber.setText("Slices:");

    spinnerSlNumber.setModel(new javax.swing.SpinnerNumberModel(2, 0, 1000, 1));
    spinnerSlNumber.setPreferredSize(new java.awt.Dimension(92, 20));
    spinnerSlNumber.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerSlNumberspinnerSlicingChange(evt);
        }
    });

    labelSlOrientation.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelSlOrientation.setText("Orientation:");

    spinnerSlOrientation.setModel(new javax.swing.SpinnerListModel(new String[] {"vertical", "horizontal"}));
    spinnerSlOrientation.setPreferredSize(new java.awt.Dimension(92, 20));
    spinnerSlOrientation.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerSlOrientationspinnerSlicingChange(evt);
        }
    });

    javax.swing.GroupLayout panelSlicingLayout = new javax.swing.GroupLayout(panelSlicing);
    panelSlicing.setLayout(panelSlicingLayout);
    panelSlicingLayout.setHorizontalGroup(
        panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelSlicingLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelSlicingLayout.createSequentialGroup()
                    .addComponent(checkSlicing)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSlicingLayout.createSequentialGroup()
                            .addComponent(labelSlNumber)
                            .addGap(2, 2, 2)
                            .addComponent(spinnerSlNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSlicingLayout.createSequentialGroup()
                            .addComponent(labelSlScale)
                            .addGap(2, 2, 2)
                            .addComponent(spinnerSlScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSlicingLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(labelSlOrientation)
                    .addGap(2, 2, 2)
                    .addComponent(spinnerSlOrientation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
    );

    panelSlicingLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerSlNumber, spinnerSlOrientation, spinnerSlScale});

    panelSlicingLayout.setVerticalGroup(
        panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelSlicingLayout.createSequentialGroup()
            .addGroup(panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(checkSlicing)
                .addGroup(panelSlicingLayout.createSequentialGroup()
                    .addGroup(panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spinnerSlNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelSlNumber))
                    .addGap(0, 0, 0)
                    .addGroup(panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spinnerSlScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelSlScale))))
            .addGap(0, 0, 0)
            .addGroup(panelSlicingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(spinnerSlOrientation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelSlOrientation)))
    );

    checkRotation.setText("Rotation");
    checkRotation.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            checkRotationActionPerformed(evt);
        }
    });

    spinnerRotationAngle.setModel(new javax.swing.SpinnerNumberModel(0.0d, -360.0d, 360.0d, 1.0d));
    spinnerRotationAngle.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerRotationAngleStateChanged(evt);
        }
    });

    spinnerRotationOrder.setModel(new javax.swing.SpinnerNumberModel(1, 1, 5, 1));
    spinnerRotationOrder.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerRotationOrderspinnerRotationAngleStateChanged(evt);
        }
    });

    labelOrder.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelOrder.setText("Order:");

    labelMode.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelMode.setText("Mode:");

    spinnerRotationMode.setModel(new javax.swing.SpinnerListModel(new String[] {"constant", "reflect", "nearest", "mirror", "wrap"}));
    spinnerRotationMode.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerRotationModespinnerRotationAngleStateChanged(evt);
        }
    });

    labelAngle.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelAngle.setText("Angle:");

    labelConstant.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    labelConstant.setText("Constant:");

    spinnerRotationConstant.setModel(new javax.swing.SpinnerNumberModel(0, 0, 99999, 1));
    spinnerRotationConstant.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerRotationConstantspinnerRotationAngleStateChanged(evt);
        }
    });

    javax.swing.GroupLayout panelPipelineLayout = new javax.swing.GroupLayout(panelPipeline);
    panelPipeline.setLayout(panelPipelineLayout);
    panelPipelineLayout.setHorizontalGroup(
        panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelPipelineLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPipelineLayout.createSequentialGroup()
                    .addComponent(checkGoodRegion)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(labelOrder)
                        .addComponent(labelGrScale)
                        .addComponent(labelMode)))
                .addGroup(panelPipelineLayout.createSequentialGroup()
                    .addComponent(checkThreshold)
                    .addGap(0, 0, Short.MAX_VALUE))
                .addGroup(panelPipelineLayout.createSequentialGroup()
                    .addComponent(checkRotation)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(labelAngle))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPipelineLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(labelGrThreshold, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPipelineLayout.createSequentialGroup()
                            .addComponent(labelConstant)
                            .addGap(3, 3, 3)))))
            .addGap(2, 2, 2)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(spinnerGrThreshold, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(spinnerGrScale)
                .addComponent(spinnerThreshold)
                .addComponent(spinnerRotationOrder)
                .addComponent(spinnerRotationMode)
                .addComponent(spinnerRotationAngle)
                .addComponent(spinnerRotationConstant))
            .addContainerGap())
        .addComponent(panelSlicing, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(panelPipelineLayout.createSequentialGroup()
            .addGap(6, 6, 6)
            .addComponent(checkBackground)
            .addGap(106, 106, 106))
    );

    panelPipelineLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerGrScale, spinnerGrThreshold});

    panelPipelineLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerRotationAngle, spinnerRotationConstant, spinnerRotationMode, spinnerRotationOrder});

    panelPipelineLayout.setVerticalGroup(
        panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelPipelineLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(checkBackground)
            .addGap(2, 2, 2)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(checkThreshold)
                .addComponent(spinnerThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(2, 2, 2)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(checkRotation)
                .addComponent(spinnerRotationAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelAngle))
            .addGap(0, 0, 0)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(spinnerRotationOrder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelOrder))
            .addGap(0, 0, 0)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(spinnerRotationMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelMode))
            .addGap(0, 0, 0)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(spinnerRotationConstant, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelConstant))
            .addGap(2, 2, 2)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(checkGoodRegion)
                .addComponent(spinnerGrScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelGrScale))
            .addGap(0, 0, 0)
            .addGroup(panelPipelineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(spinnerGrThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelGrThreshold))
            .addGap(2, 2, 2)
            .addComponent(panelSlicing, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );

    panelZoom.setBorder(javax.swing.BorderFactory.createTitledBorder("Zoom"));

    buttonGroup1.add(buttonZoomFit);
    buttonZoomFit.setText("Fit");
    buttonZoomFit.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonZoomFitActionPerformed(evt);
        }
    });

    buttonGroup1.add(buttonZoomStretch);
    buttonZoomStretch.setText("Stretch");
    buttonZoomStretch.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonZoomStretchActionPerformed(evt);
        }
    });

    buttonGroup1.add(buttonZoomNormal);
    buttonZoomNormal.setText("Normal");
    buttonZoomNormal.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonZoomNormalActionPerformed(evt);
        }
    });

    buttonGroup1.add(buttonZoom025);
    buttonZoom025.setText("1/4");
    buttonZoom025.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonZoom025ActionPerformed(evt);
        }
    });

    buttonGroup1.add(buttonZoom05);
    buttonZoom05.setText("1/2");
    buttonZoom05.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonZoom05ActionPerformed(evt);
        }
    });

    buttonGroup1.add(buttonZoom2);
    buttonZoom2.setText("2");
    buttonZoom2.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonZoom2ActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout panelZoomLayout = new javax.swing.GroupLayout(panelZoom);
    panelZoom.setLayout(panelZoomLayout);
    panelZoomLayout.setHorizontalGroup(
        panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelZoomLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(buttonZoomFit)
                .addComponent(buttonZoomNormal)
                .addComponent(buttonZoomStretch))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(buttonZoom025)
                .addComponent(buttonZoom05)
                .addComponent(buttonZoom2))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    panelZoomLayout.setVerticalGroup(
        panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelZoomLayout.createSequentialGroup()
            .addGap(4, 4, 4)
            .addGroup(panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(buttonZoomNormal)
                .addComponent(buttonZoom025))
            .addGap(0, 0, 0)
            .addGroup(panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(buttonZoomFit)
                .addComponent(buttonZoom05))
            .addGap(0, 0, 0)
            .addGroup(panelZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(buttonZoomStretch)
                .addComponent(buttonZoom2))
            .addContainerGap())
    );

    panelColormap.setBorder(javax.swing.BorderFactory.createTitledBorder("Colormap"));

    checkHistogram.setText("Histogram");
    checkHistogram.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            checkHistogramActionPerformed(evt);
        }
    });

    comboColormap.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            onChangeColormap(evt);
        }
    });

    jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    jLabel3.setText("Type:");

    jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    jLabel4.setText("Range:");

    buttonGroup2.add(buttonFullRange);
    buttonFullRange.setText("Full");
    buttonFullRange.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonFullRangeonChangeColormap(evt);
        }
    });

    buttonGroup2.add(buttonManual);
    buttonManual.setText("Manual");
    buttonManual.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonManualonChangeColormap(evt);
        }
    });

    buttonGroup2.add(buttonAutomatic);
    buttonAutomatic.setText("Automatic");
    buttonAutomatic.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            buttonAutomaticonChangeColormap(evt);
        }
    });

    labelMin.setText("Min:");

    spinnerMin.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
    spinnerMin.setEnabled(false);
    spinnerMin.setPreferredSize(new java.awt.Dimension(77, 20));
    spinnerMin.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerMinonChangeColormapRange(evt);
        }
    });

    spinnerMax.setModel(new javax.swing.SpinnerNumberModel(255, 0, 65535, 1));
    spinnerMax.setEnabled(false);
    spinnerMax.setPreferredSize(new java.awt.Dimension(77, 20));
    spinnerMax.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            spinnerMaxonChangeColormapRange(evt);
        }
    });

    labelMax.setText("Max:");

    btFixColormapRange.setText("Fix");
    btFixColormapRange.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btFixColormapRangeActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout panelColormapLayout = new javax.swing.GroupLayout(panelColormap);
    panelColormap.setLayout(panelColormapLayout);
    panelColormapLayout.setHorizontalGroup(
        panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(panelColormapLayout.createSequentialGroup()
            .addGap(4, 4, 4)
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel3)
                .addComponent(jLabel4))
            .addGap(4, 4, 4)
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(buttonAutomatic)
                .addComponent(buttonFullRange)
                .addComponent(buttonManual)
                .addComponent(comboColormap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelColormapLayout.createSequentialGroup()
                    .addComponent(labelMax)
                    .addGap(2, 2, 2)
                    .addComponent(spinnerMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(checkHistogram, javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelColormapLayout.createSequentialGroup()
                    .addComponent(labelMin)
                    .addGap(2, 2, 2)
                    .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btFixColormapRange, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(spinnerMin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE))))
            .addContainerGap())
    );

    panelColormapLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerMax, spinnerMin});

    panelColormapLayout.setVerticalGroup(
        panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelColormapLayout.createSequentialGroup()
            .addGap(4, 4, 4)
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(comboColormap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel3)
                .addComponent(checkHistogram))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(buttonAutomatic)
                .addComponent(jLabel4)
                .addComponent(btFixColormapRange, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(0, 0, 0)
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(labelMin)
                .addComponent(spinnerMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(buttonFullRange))
            .addGroup(panelColormapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(buttonManual)
                .addComponent(labelMax)
                .addComponent(spinnerMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
    );

    javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
    sidePanel.setLayout(sidePanelLayout);
    sidePanelLayout.setHorizontalGroup(
        sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(sidePanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addComponent(panelZoom, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelColormap, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelPipeline, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    sidePanelLayout.setVerticalGroup(
        sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(sidePanelLayout.createSequentialGroup()
            .addGap(0, 0, 0)
            .addComponent(panelPipeline, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(panelZoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(panelColormap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGap(0, 0, 0)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(sidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, 0)
                    .addComponent(renderer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(renderer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sidePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
    );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonReticleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReticleActionPerformed
        try {
            checkReticle();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonReticleActionPerformed

    private void buttonFitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFitActionPerformed
        try {
            checkFit();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonFitActionPerformed

    private void buttonProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonProfileActionPerformed
        try {
            checkProfile();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonProfileActionPerformed

    private void buttonMarkerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMarkerActionPerformed
        try {
            checkMarker(null);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonMarkerActionPerformed

    private void buttonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPauseActionPerformed
        try {
            checkPause();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonStreamDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStreamDataActionPerformed
        try {
            showStreamData();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonStreamDataActionPerformed

    private void buttonScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScaleActionPerformed
        try {
            checkColormap();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonScaleActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            saveSnapshot();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void checkThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkThresholdActionPerformed
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    spinnerThreshold.setVisible(checkThreshold.isSelected());
                    server.setThreshold(checkThreshold.isSelected() ? (Double) spinnerThreshold.getValue() : null);
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_checkThresholdActionPerformed

    private void spinnerThresholdonChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerThresholdonChange
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    server.setThreshold((Double) spinnerThreshold.getValue());
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_spinnerThresholdonChange

    private void checkBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBackgroundActionPerformed
        if (server != null) {
            if (!updatingServerControls) {
                try {
                    if (server.isStarted()) {
                        server.setBackgroundSubtraction(checkBackground.isSelected());
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
                    updatePipelineControls();
                    updatingServerControls = true;
                    checkBackground.setSelected(false);
                    updatingServerControls = false;

                }
            }
        } else {
            camera.setBackgroundEnabled(checkBackground.isSelected());
        }
    }//GEN-LAST:event_checkBackgroundActionPerformed

    private void checkGoodRegionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkGoodRegionActionPerformed
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    boolean goodRegion = checkGoodRegion.isSelected();
                    setGoodRegionOptionsVisible(goodRegion);
                    if (goodRegion) {
                        server.setGoodRegion(((Number) spinnerGrThreshold.getValue()).doubleValue(), ((Number) spinnerGrScale.getValue()).doubleValue());
                    } else {
                        server.setGoodRegion(null);
                    }
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_checkGoodRegionActionPerformed

    private void spinnerGrScalespinnerGrThresholdonChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerGrScalespinnerGrThresholdonChange
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    server.setGoodRegion((Double) spinnerGrThreshold.getValue(), (Double) spinnerGrScale.getValue());
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_spinnerGrScalespinnerGrThresholdonChange

    private void spinnerGrThresholdonChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerGrThresholdonChange
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    server.setGoodRegion((Double) spinnerGrThreshold.getValue(), (Double) spinnerGrScale.getValue());
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_spinnerGrThresholdonChange

    private void checkSlicingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkSlicingActionPerformed
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    boolean slicing = checkSlicing.isSelected();
                    setSlicingOptionsVisible(slicing);
                    if (slicing) {
                        server.setSlicing((Integer) spinnerSlNumber.getValue(), (Double) spinnerSlScale.getValue(), spinnerSlOrientation.getValue().toString());
                    } else {
                        server.setSlicing(null);
                    }
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_checkSlicingActionPerformed

    private void spinnerSlScalespinnerSlicingChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSlScalespinnerSlicingChange
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    server.setSlicing((Integer) spinnerSlNumber.getValue(), (Double) spinnerSlScale.getValue(), spinnerSlOrientation.getValue().toString());
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_spinnerSlScalespinnerSlicingChange

    private void spinnerSlNumberspinnerSlicingChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSlNumberspinnerSlicingChange
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    server.setSlicing((Integer) spinnerSlNumber.getValue(), (Double) spinnerSlScale.getValue(), spinnerSlOrientation.getValue().toString());
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_spinnerSlNumberspinnerSlicingChange

    private void spinnerSlOrientationspinnerSlicingChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSlOrientationspinnerSlicingChange
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    server.setSlicing((Integer) spinnerSlNumber.getValue(), (Double) spinnerSlScale.getValue(), spinnerSlOrientation.getValue().toString());
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_spinnerSlOrientationspinnerSlicingChange

    private void checkRotationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkRotationActionPerformed
        if (!updatingServerControls) {
            try {
                if ((server != null) && (server.isStarted())) {
                    boolean rotation = checkRotation.isSelected();
                    setRotationOptionsVisible(rotation);
                    if (rotation) {
                        spinnerRotationAngleStateChanged(null);
                    } else {
                        server.setRotation(null);
                    }
                }
            } catch (Exception ex) {
                SwingUtils.showException(this, ex);
                updatePipelineControls();
            }
        }
    }//GEN-LAST:event_checkRotationActionPerformed

    private void spinnerRotationAngleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerRotationAngleStateChanged
        try {
            String mode = String.valueOf(spinnerRotationMode.getValue());
            server.setRotation(((Number) spinnerRotationAngle.getValue()).doubleValue(),
                    ((Number) spinnerRotationOrder.getValue()).intValue(),
                    mode.equals("constant") ? String.valueOf(spinnerRotationConstant.getValue()) : mode);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            updatePipelineControls();
        }
    }//GEN-LAST:event_spinnerRotationAngleStateChanged

    private void spinnerRotationOrderspinnerRotationAngleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerRotationOrderspinnerRotationAngleStateChanged
        try {
            String mode = String.valueOf(spinnerRotationMode.getValue());
            server.setRotation(((Number) spinnerRotationAngle.getValue()).doubleValue(),
                    ((Number) spinnerRotationOrder.getValue()).intValue(),
                    mode.equals("constant") ? String.valueOf(spinnerRotationConstant.getValue()) : mode);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            updatePipelineControls();
        }
    }//GEN-LAST:event_spinnerRotationOrderspinnerRotationAngleStateChanged

    private void spinnerRotationModespinnerRotationAngleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerRotationModespinnerRotationAngleStateChanged
        try {
            String mode = String.valueOf(spinnerRotationMode.getValue());
            server.setRotation(((Number) spinnerRotationAngle.getValue()).doubleValue(),
                    ((Number) spinnerRotationOrder.getValue()).intValue(),
                    mode.equals("constant") ? String.valueOf(spinnerRotationConstant.getValue()) : mode);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            updatePipelineControls();
        }
    }//GEN-LAST:event_spinnerRotationModespinnerRotationAngleStateChanged

    private void spinnerRotationConstantspinnerRotationAngleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerRotationConstantspinnerRotationAngleStateChanged
        try {
            String mode = String.valueOf(spinnerRotationMode.getValue());
            server.setRotation(((Number) spinnerRotationAngle.getValue()).doubleValue(),
                    ((Number) spinnerRotationOrder.getValue()).intValue(),
                    mode.equals("constant") ? String.valueOf(spinnerRotationConstant.getValue()) : mode);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            updatePipelineControls();
        }
    }//GEN-LAST:event_spinnerRotationConstantspinnerRotationAngleStateChanged

    private void buttonZoomFitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomFitActionPerformed
        try {
            renderer.setMode(RendererMode.Fit);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonZoomFitActionPerformed

    private void buttonZoomStretchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomStretchActionPerformed
        try {
            renderer.setMode(RendererMode.Stretch);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonZoomStretchActionPerformed

    private void buttonZoomNormalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomNormalActionPerformed
        try {
            renderer.setMode(RendererMode.Fixed);
            centralizeRenderer();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonZoomNormalActionPerformed

    private void buttonZoom025ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoom025ActionPerformed
        renderer.setZoom(0.25);
        renderer.setMode(RendererMode.Zoom);
        centralizeRenderer();
    }//GEN-LAST:event_buttonZoom025ActionPerformed

    private void buttonZoom05ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoom05ActionPerformed
        renderer.setZoom(0.5);
        renderer.setMode(RendererMode.Zoom);
        centralizeRenderer();
    }//GEN-LAST:event_buttonZoom05ActionPerformed

    private void buttonZoom2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoom2ActionPerformed
        renderer.setZoom(2.0);
        renderer.setMode(RendererMode.Zoom);
        centralizeRenderer();
    }//GEN-LAST:event_buttonZoom2ActionPerformed

    private void checkHistogramActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkHistogramActionPerformed
        try {
            setHistogramVisible(checkHistogram.isSelected());
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_checkHistogramActionPerformed

    private void onChangeColormap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onChangeColormap
        try {
            if ((camera != null) && (camera instanceof ColormapSource) && !updatingColormap) {
                ColormapSource source = (ColormapSource) camera;
                Color colorReticule = new Color(16, 16, 16);
                Color colorMarker = new Color(128, 128, 128);
                Colormap colormap = (Colormap) comboColormap.getSelectedItem();
                source.getConfig().colormap = (colormap == null) ? Colormap.Flame : colormap;
                switch (source.getConfig().colormap) {
                    case Grayscale:
                    case Inverted:
                    case Red:
                    case Green:
                    case Blue:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                    case Flame:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                }

                renderer.setPenReticle(new Pen(colorReticule));
                renderer.setPenProfile(new Pen(colorReticule, 0));
                renderer.setPenMarker(new Pen(colorMarker, 2));
                renderer.setShowReticle(false);
                checkReticle();
                source.getConfig().colormapAutomatic = buttonAutomatic.isSelected();
                source.getConfig().colormapMin = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMin.getValue();
                source.getConfig().colormapMax = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMax.getValue();
                try {
                    source.getConfig().save();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
                }
                source.refresh();
                if (buttonPause.isSelected()) {
                    updatePause();
                }
                updateColormap();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_onChangeColormap

    private void buttonFullRangeonChangeColormap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFullRangeonChangeColormap
        try {
            if ((camera != null) && (camera instanceof ColormapSource) && !updatingColormap) {
                ColormapSource source = (ColormapSource) camera;
                Color colorReticule = new Color(16, 16, 16);
                Color colorMarker = new Color(128, 128, 128);
                Colormap colormap = (Colormap) comboColormap.getSelectedItem();
                source.getConfig().colormap = (colormap == null) ? Colormap.Flame : colormap;
                switch (source.getConfig().colormap) {
                    case Grayscale:
                    case Inverted:
                    case Red:
                    case Green:
                    case Blue:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                    case Flame:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                }

                renderer.setPenReticle(new Pen(colorReticule));
                renderer.setPenProfile(new Pen(colorReticule, 0));
                renderer.setPenMarker(new Pen(colorMarker, 2));
                renderer.setShowReticle(false);
                checkReticle();
                source.getConfig().colormapAutomatic = buttonAutomatic.isSelected();
                source.getConfig().colormapMin = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMin.getValue();
                source.getConfig().colormapMax = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMax.getValue();
                try {
                    source.getConfig().save();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
                }
                source.refresh();
                if (buttonPause.isSelected()) {
                    updatePause();
                }
                updateColormap();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonFullRangeonChangeColormap

    private void buttonManualonChangeColormap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonManualonChangeColormap
        try {
            if ((camera != null) && (camera instanceof ColormapSource) && !updatingColormap) {
                ColormapSource source = (ColormapSource) camera;
                Color colorReticule = new Color(16, 16, 16);
                Color colorMarker = new Color(128, 128, 128);
                Colormap colormap = (Colormap) comboColormap.getSelectedItem();
                source.getConfig().colormap = (colormap == null) ? Colormap.Flame : colormap;
                switch (source.getConfig().colormap) {
                    case Grayscale:
                    case Inverted:
                    case Red:
                    case Green:
                    case Blue:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                    case Flame:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                }

                renderer.setPenReticle(new Pen(colorReticule));
                renderer.setPenProfile(new Pen(colorReticule, 0));
                renderer.setPenMarker(new Pen(colorMarker, 2));
                renderer.setShowReticle(false);
                checkReticle();
                source.getConfig().colormapAutomatic = buttonAutomatic.isSelected();
                source.getConfig().colormapMin = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMin.getValue();
                source.getConfig().colormapMax = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMax.getValue();
                try {
                    source.getConfig().save();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
                }
                source.refresh();
                if (buttonPause.isSelected()) {
                    updatePause();
                }
                updateColormap();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonManualonChangeColormap

    private void buttonAutomaticonChangeColormap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAutomaticonChangeColormap
        try {
            if ((camera != null) && (camera instanceof ColormapSource) && !updatingColormap) {
                ColormapSource source = (ColormapSource) camera;
                Color colorReticule = new Color(16, 16, 16);
                Color colorMarker = new Color(128, 128, 128);
                Colormap colormap = (Colormap) comboColormap.getSelectedItem();
                source.getConfig().colormap = (colormap == null) ? Colormap.Flame : colormap;
                switch (source.getConfig().colormap) {
                    case Grayscale:
                    case Inverted:
                    case Red:
                    case Green:
                    case Blue:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                    case Flame:
                        colorReticule = new Color(0, 192, 0);
                        colorMarker = new Color(64, 255, 64);
                        break;
                }

                renderer.setPenReticle(new Pen(colorReticule));
                renderer.setPenProfile(new Pen(colorReticule, 0));
                renderer.setPenMarker(new Pen(colorMarker, 2));
                renderer.setShowReticle(false);
                checkReticle();
                source.getConfig().colormapAutomatic = buttonAutomatic.isSelected();
                source.getConfig().colormapMin = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMin.getValue();
                source.getConfig().colormapMax = buttonFullRange.isSelected() ? Double.NaN : (Integer) spinnerMax.getValue();
                try {
                    source.getConfig().save();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
                }
                source.refresh();
                if (buttonPause.isSelected()) {
                    updatePause();
                }
                updateColormap();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonAutomaticonChangeColormap

    private void spinnerMinonChangeColormapRange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerMinonChangeColormapRange
        onChangeColormap(null);
    }//GEN-LAST:event_spinnerMinonChangeColormapRange

    private void spinnerMaxonChangeColormapRange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerMaxonChangeColormapRange
        onChangeColormap(null);
    }//GEN-LAST:event_spinnerMaxonChangeColormapRange

    private void btFixColormapRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btFixColormapRangeActionPerformed
        try {
            updatingColormap = true;
            ArrayProperties properties = currentFrame.data.getProperties();
            spinnerMax.setValue(properties.max.intValue());
            spinnerMin.setValue(properties.min.intValue());
            buttonManual.setSelected(true);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        } finally {
            updatingColormap = false;
            onChangeColormap(null);
        }
    }//GEN-LAST:event_btFixColormapRangeActionPerformed

    private void buttonSidePanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSidePanelActionPerformed
        sidePanel.setVisible(buttonSidePanel.isSelected());
    }//GEN-LAST:event_buttonSidePanelActionPerformed

    private void buttonGrabBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonGrabBackgroundActionPerformed
        try {
            grabBackground();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonGrabBackgroundActionPerformed

    private void buttonTitleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTitleActionPerformed
        try {
            manageTitleOverlay();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        } finally {
        }
    }//GEN-LAST:event_buttonTitleActionPerformed

    private void textStreamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textStreamActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_textStreamActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btFixColormapRange;
    private javax.swing.JRadioButton buttonAutomatic;
    private javax.swing.JToggleButton buttonFit;
    private javax.swing.JRadioButton buttonFullRange;
    private javax.swing.JButton buttonGrabBackground;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JRadioButton buttonManual;
    private javax.swing.JToggleButton buttonMarker;
    private javax.swing.JToggleButton buttonPause;
    private javax.swing.JToggleButton buttonProfile;
    private javax.swing.JToggleButton buttonReticle;
    private javax.swing.JButton buttonSave;
    private javax.swing.JToggleButton buttonScale;
    private javax.swing.JToggleButton buttonSidePanel;
    private javax.swing.JButton buttonStreamData;
    private javax.swing.JToggleButton buttonTitle;
    private javax.swing.JRadioButton buttonZoom025;
    private javax.swing.JRadioButton buttonZoom05;
    private javax.swing.JRadioButton buttonZoom2;
    private javax.swing.JRadioButton buttonZoomFit;
    private javax.swing.JRadioButton buttonZoomNormal;
    private javax.swing.JRadioButton buttonZoomStretch;
    private javax.swing.JCheckBox checkBackground;
    private javax.swing.JCheckBox checkGoodRegion;
    private javax.swing.JCheckBox checkHistogram;
    private javax.swing.JCheckBox checkRotation;
    private javax.swing.JCheckBox checkSlicing;
    private javax.swing.JCheckBox checkThreshold;
    private javax.swing.JComboBox comboColormap;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JLabel labelAngle;
    private javax.swing.JLabel labelConstant;
    private javax.swing.JLabel labelGrScale;
    private javax.swing.JLabel labelGrThreshold;
    private javax.swing.JLabel labelMax;
    private javax.swing.JLabel labelMin;
    private javax.swing.JLabel labelMode;
    private javax.swing.JLabel labelOrder;
    private javax.swing.JLabel labelSlNumber;
    private javax.swing.JLabel labelSlOrientation;
    private javax.swing.JLabel labelSlScale;
    private javax.swing.JPanel panelColormap;
    private javax.swing.JPanel panelPipeline;
    private javax.swing.JPanel panelSlicing;
    private javax.swing.JPanel panelStream;
    private javax.swing.JPanel panelZoom;
    private ch.psi.pshell.swing.ValueSelection pauseSelection;
    protected ch.psi.pshell.imaging.Renderer renderer;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JSpinner spinnerGrScale;
    private javax.swing.JSpinner spinnerGrThreshold;
    private javax.swing.JSpinner spinnerMax;
    private javax.swing.JSpinner spinnerMin;
    private javax.swing.JSpinner spinnerRotationAngle;
    private javax.swing.JSpinner spinnerRotationConstant;
    private javax.swing.JSpinner spinnerRotationMode;
    private javax.swing.JSpinner spinnerRotationOrder;
    private javax.swing.JSpinner spinnerSlNumber;
    private javax.swing.JSpinner spinnerSlOrientation;
    private javax.swing.JSpinner spinnerSlScale;
    private javax.swing.JSpinner spinnerThreshold;
    private javax.swing.JTextField textStream;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
}
