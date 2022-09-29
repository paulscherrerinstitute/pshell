package ch.psi.pshell.ui;

import ch.psi.pshell.bs.PipelineServer;
import ch.psi.pshell.bs.Scalar;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.InlineDevice;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.data.ProviderCSV;
import ch.psi.pshell.data.ProviderText;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.epics.ChannelDouble;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.plot.TimePlotJFree;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.pshell.plotter.Preferences;
import ch.psi.pshell.scan.StripScanExecutor;
import ch.psi.pshell.swing.HistoryChart;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.ui.StripChartAlarmEditor.StripChartAlarmConfig;
import ch.psi.utils.Audio;
import ch.psi.utils.Chrono;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.IO;
import ch.psi.utils.InvokingProducer;
import ch.psi.utils.State;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Time plotting of a set of devices. Can be used in the workbench or opened
 * standalone with '-strp' option. The '-attach'option make standalone panels be
 * handled in the same process.
 *
 * StripChart plot panel can be included in Panel plugins, for example adding in
 * the plugin constructor: stripChart = new StripChart(this.getTopLevel(),
 * false, App.getStripChartFolderArg()); panel.add(stripChart.getPlotPanel());
 * try { stripChart.open(new File("test.scd"))); stripChart.start(); } catch
 * (Exception ex) { showException(ex); }
 *
 */
public class StripChart extends StandardDialog {

    public static final String FILE_EXTENSION = "scd";

    int dragInterval = 1000;
    final int disableAlarmTimer = 30 * 60 * 1000; //30 minutes
    Chrono chronoDisableAlarmSound;
    Color defaultBackgroundColor = null;
    Color defaultGridColor = null;
    Font tickLabelFont = null;
    int alarmInterval = 1000;
    File alarmFile = null;
    boolean pulse;

    enum Type {
        Channel,
        Stream,
        Device,
        CamServer;

        public boolean isReadOnly() {
            return (this == CamServer) || (this == Stream);
        }
    }

    final DefaultTableModel modelSeries;
    final DefaultTableModel modelCharts;

    final ArrayList<TimePlotBase> plots = new ArrayList<>();
    final ArrayList<DeviceTask> tasks = new ArrayList<>();
    final HashMap<Device, Long> appendTimestamps = new HashMap<>();
    private int streamDevices = 0;
    final File defaultFolder;

    Color backgroundColor;
    Color gridColor;

    class ChartElement {

        ChartElement(TimePlotBase plot, int seriesIndex, long time, double value, boolean drag) {
            this.plot = plot;
            this.seriesIndex = seriesIndex;
            this.time = time;
            this.value = value;
            this.drag = drag;
        }
        int seriesIndex;
        long time;
        double value;
        boolean drag;
        TimePlotBase plot;
    }
    final InvokingProducer<ChartElement> chartElementProducer;

    JToggleButton buttonPause = new JToggleButton();
    JToggleButton buttonSound = new JToggleButton();

    boolean persisting;
    StripScanExecutor persistenceExecutor;

    public StripChart(Window parent, boolean modal, File defaultFolder) {
        super(parent, modal);
        
        initComponents();
        chartElementProducer = new InvokingProducer<ChartElement>() {
            @Override
            protected void consume(ChartElement element) {
                if (element.drag) {
                    element.plot.drag(element.seriesIndex, element.time, element.value);
                } else {
                    element.plot.add(element.seriesIndex, element.time, element.value);
                }
            }
        };

        if (App.hasArgument("background_color")) {
            try {
                defaultBackgroundColor = Preferences.getColorFromString(App.getArgumentValue("background_color"));
                panelColorBackground.setBackground(defaultBackgroundColor);
                backgroundColor = defaultBackgroundColor;
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("grid_color")) {
            try {
                defaultGridColor = Preferences.getColorFromString(App.getArgumentValue("grid_color"));
                panelColorGrid.setBackground(defaultGridColor);
                gridColor = defaultGridColor;
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("tick_label_font")) {
            try {
                String[] tokens = App.getArgumentValue("tick_label_font").split(":");
                tickLabelFont = new Font(tokens[0], Font.PLAIN, Integer.valueOf(tokens[1]));
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        if (App.hasArgument("alarm_interval")) {
            try {
                alarmInterval = Integer.valueOf(App.getArgumentValue("alarm_interval"));
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("alarm_file")) {
            try {
                alarmFile = new File(App.getArgumentValue("alarm_file"));
                if (!alarmFile.exists()) {
                    throw new FileNotFoundException(App.getArgumentValue("alarm_file"));
                }
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        buttonStartStop.setEnabled(false);
        textFileName.setEnabled(false);
        comboFormat.setEnabled(false);
        comboLayout.setEnabled(false);
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        comboFormat.setSelectedItem(getInitFormat());
        comboLayout.setSelectedItem(getInitLayout());
        updateTitle();
        setCancelledOnEscape(false);

        //pnGraphs.setLayout(new GridBagLayout());
        pnGraphs.setLayout(new BoxLayout(pnGraphs, BoxLayout.Y_AXIS));

        modelSeries = (DefaultTableModel) tableSeries.getModel();
        modelSeries.addTableModelListener(modelSeriesListener);
        modelCharts = (DefaultTableModel) tableCharts.getModel();
        modelCharts.addTableModelListener(modelChartsListener);
        this.defaultFolder = defaultFolder;
        initializeTable();

        setLayout(new LayoutManager() {
            @Override
            public void layoutContainer(Container parent) {
                Insets insets = parent.getInsets();
                boolean dlaf = MainFrame.isDarcula();
                for (Component component : parent.getComponents()) {
                    if (component == buttonPause) {
                        Dimension ps = component.getPreferredSize();
                        int width = Math.min(ps.width, 30); //ps.width; //Math.max(ps.width, 60);
                        int height = Math.min(ps.height, 24);
                        if (!dlaf) {
                            try {
                                height = Math.min(height, pnGraphs.getLocationOnScreen().y - tabPane.getLocationOnScreen().y - 5);
                            } catch (Exception ex) {
                            }
                        }
                        component.setBounds(parent.getWidth() - insets.right - 2 - width, insets.top - (dlaf ? 2 : 0), width, height);
                    } else if (component == buttonSound) {
                        Dimension ps = component.getPreferredSize();
                        int width = Math.min(ps.width, 30); //ps.width; //Math.max(ps.width, 60);
                        int height = Math.min(ps.height, 24);
                        if (!dlaf) {
                            try {
                                height = Math.min(height, pnGraphs.getLocationOnScreen().y - tabPane.getLocationOnScreen().y - 5);
                            } catch (Exception ex) {
                            }
                        }
                        component.setBounds(parent.getWidth() - insets.right - 2 - 2 * width - 2, insets.top - (dlaf ? 2 : 0), width, height);
                    } else {
                        component.setBounds(insets.left, insets.top, parent.getWidth() - insets.left - insets.right, parent.getHeight() - insets.top - insets.bottom);
                    }
                }
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return new Dimension();
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return preferredLayoutSize(parent);
            }

            @Override
            public void addLayoutComponent(String name, Component comp) {
            }

            @Override
            public void removeLayoutComponent(Component comp) {
            }
        });

        setButtonPause(true);
        buttonPause.setFocusable(false);
        buttonPause.setHorizontalTextPosition(SwingConstants.CENTER);
        buttonPause.setVerticalTextPosition(SwingConstants.CENTER);
        buttonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boolean pause = buttonPause.isSelected();
                for (TimePlotBase plot : plots) {
                    if (pause) {
                        plot.stop();
                    } else {
                        plot.start();
                    }
                }
                setButtonPause(!pause);
            }
        });
        add(buttonPause, 0);

        buttonSound.setText("");
        buttonSound.setFocusable(false);
        buttonSound.setHorizontalTextPosition(SwingConstants.CENTER);
        buttonSound.setVerticalTextPosition(SwingConstants.CENTER);
        buttonSound.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (buttonSound.isSelected()) {
                    chronoDisableAlarmSound = new Chrono();
                } else {
                    chronoDisableAlarmSound = null;
                }
            }
        });
        add(buttonSound, 0);

        if (Context.getInstance() != null) {
            Context.getInstance().addListener(new ContextAdapter() {
                boolean hasStopped;

                @Override
                public void onContextStateChanged(State state, State former) {
                    if ((state == State.Initializing) || (state == State.Closing)) {
                        if (started) {
                            stop();
                            hasStopped = (state == State.Initializing);
                        }
                    } else if ((former == State.Initializing) && state.isActive()
                            && (modelSeries.getRowCount() > 0) && hasStopped) {
                        try {
                            start();
                        } catch (Exception ex) {
                            Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
                        }
                        hasStopped = false;
                    } else {
                        hasStopped = false;
                    }
                }
            });
        }
        onLafChange();
    }

    //Access functions
    public JTabbedPane getTabbedPane() {
        return this.tabPane;
    }

    public JPanel getPlotPanel() {
        return panelPlots;
    }

    public JPanel getConfigPanel() {
        return panelConfig;
    }

    void setButtonPause(boolean paused) {
        buttonPause.setText("");
        buttonPause.setToolTipText(paused ? "Pause" : "Resume");
        onLafChange();
    }
    
    @Override
    protected void onLafChange() {
        boolean paused = "Pause".equals(buttonPause.getToolTipText());
        buttonPause.setIcon(new ImageIcon(App.getResourceUrl((MainFrame.isDark() ? "dark/" : "") + (paused ? "Pause.png" : "Play.png"))));
        buttonSound.setIcon(new ImageIcon(App.getResourceUrl((MainFrame.isDark() ? "dark/" : "") + "Sound.png")));
    }  

    void updateTooltip(JComponent text, int row) {
        String tooltip = "";
        try {
            if (row >= 0) {
                Type type = Type.valueOf(modelSeries.getValueAt(row, 2).toString());
                switch (type) {
                    case Channel:
                        tooltip = "Format: ChannelName [Polling=-1 Precision=-1]";
                        break;
                    case Device:
                        tooltip = "Format: DeviceName";
                        break;
                    case Stream:
                        tooltip = "Format: Identifier [Modulo=" + Scalar.DEFAULT_MODULO + " Offset=" + Scalar.DEFAULT_OFFSET + " GlobalTime=true]";
                        break;
                    case CamServer:
                        tooltip = "Format: URL Identifier";
                        break;
                }
            }
        } catch (Exception ex) {
        }
        text.setToolTipText(tooltip);
    }

    JTextField textNameEditor;

    void updateTooltip() {
        SwingUtilities.invokeLater(() -> {
            updateTooltip(textNameEditor, tableSeries.getSelectedRow());
        });
    }

    public void initializeTable() {
        //Fix bug of nimbus rendering Boolean in table
        ((JComponent) tableSeries.getDefaultRenderer(Boolean.class)).setOpaque(true);
        tableSeries.getColumnModel().getColumn(0).setResizable(true);

        TableColumn colEnabled = tableSeries.getColumnModel().getColumn(0);
        colEnabled.setPreferredWidth(60);

        TableColumn colName = tableSeries.getColumnModel().getColumn(1);
        textNameEditor = new JTextField();
        colName.setPreferredWidth(360);
        colName.setCellEditor(new DefaultCellEditor(textNameEditor));
        colName.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean c, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, c, row, column);
                updateTooltip(label, row);
                return label;
            }
        });

        TableColumn colType = tableSeries.getColumnModel().getColumn(2);
        colType.setPreferredWidth(80);
        SwingUtils.setEnumTableColum(tableSeries, 2, Type.class);

        TableColumn colPlot = tableSeries.getColumnModel().getColumn(3);
        colPlot.setPreferredWidth(60);
        JComboBox comboPlot = new JComboBox();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (int i = 1; i <= 5; i++) {
            model.addElement(i);
        }
        comboPlot.setModel(model);
        DefaultCellEditor cellEditor = new DefaultCellEditor(comboPlot);
        cellEditor.setClickCountToStart(2);
        colPlot.setCellEditor(cellEditor);

        TableColumn colY = tableSeries.getColumnModel().getColumn(4);
        colY.setPreferredWidth(60);
        JComboBox comboY = new JComboBox();
        model = new DefaultComboBoxModel();
        model.addElement(1);
        model.addElement(2);
        comboY.setModel(model);
        cellEditor = new DefaultCellEditor(comboY);
        cellEditor.setClickCountToStart(2);
        colY.setCellEditor(cellEditor);

        TableColumn colColors = tableSeries.getColumnModel().getColumn(5);
        colColors.setPreferredWidth(60);

        class ColorEditor extends AbstractCellEditor implements TableCellEditor {

            private final JTextField field = new JTextField();
            private Color color;

            ColorEditor() {
                field.setBorder(null);
                field.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            color = JColorChooser.showDialog(StripChart.this, "Choose a Color - Click 'Cancel for default", color);
                            field.setBackground(color);
                            stopCellEditing();
                        }
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                color = Preferences.getColorFromString((String) value);
                field.setBackground(color);
                field.setEditable(false);
                return field;
            }

            @Override
            public Object getCellEditorValue() {
                return (color == null) ? "" : color.getRed() + "," + color.getGreen() + "," + color.getBlue();
            }

            @Override
            public boolean isCellEditable(EventObject ev) {
                if (ev instanceof MouseEvent) {
                    return ((MouseEvent) ev).getClickCount() >= 2;
                }
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject ev) {
                return false;
            }
        }
        colColors.setCellEditor(new ColorEditor());

        colColors.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                Color color = Preferences.getColorFromString((String) value);
                ((JLabel) comp).setBackground(color);
                ((JLabel) comp).setEnabled(false);
                return comp;
            }
        });

        TableColumn colAlarm = tableSeries.getColumnModel().getColumn(6);
        colAlarm.setPreferredWidth(50);
        class AlarmEditor extends AbstractCellEditor implements TableCellEditor {

            private final JCheckBox check = new JCheckBox();
            StripChartAlarmConfig config;

            AlarmEditor() {
                check.setHorizontalAlignment(SwingConstants.CENTER);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

                Type type = Type.valueOf(modelSeries.getValueAt(row, 2).toString());
                String channel = (type == Type.Channel) ? String.valueOf(modelSeries.getValueAt(row, 1)) : null;
                StripChartAlarmEditor alarmEditor = new StripChartAlarmEditor(SwingUtils.getFrame(tableSeries), true, (StripChartAlarmConfig) value, channel);
                alarmEditor.setLocationRelativeTo(tableSeries);
                alarmEditor.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                alarmEditor.setVisible(true);
                config = (alarmEditor.getResult()) ? alarmEditor.config : null;
                if (config != null) {
                    config.updateChannelLimits();
                }
                check.setSelected(value != null);
                SwingUtilities.invokeLater(() -> {
                    stopCellEditing();
                });
                return check;
            }

            @Override
            public Object getCellEditorValue() {
                return config;
            }
        }
        colAlarm.setCellEditor(new AlarmEditor());

        colAlarm.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component aux = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                JCheckBox check = new JCheckBox("", (value != null) && ((StripChartAlarmConfig) value).isEnabled());
                check.setHorizontalAlignment(SwingConstants.CENTER);
                check.setBackground(aux.getBackground());
                return check;
            }
        });

        update();

        tableCharts.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableCharts.getColumnModel().getColumn(0).setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
                return comp;
            }
        });
        final DecimalFormat formatter = new DecimalFormat("#.############");
        for (int i = 1; i <= 5; i++) {
            tableCharts.getColumnModel().getColumn(i).setPreferredWidth(90);
            tableCharts.getColumnModel().getColumn(i).setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    if (value != null) {
                        value = formatter.format((Double) value);
                    }
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            });
        }

        TableColumn colMarkers = tableCharts.getColumnModel().getColumn(6);
        colMarkers.setPreferredWidth(80);

        TableColumn colLocalTime = tableCharts.getColumnModel().getColumn(7);
        colLocalTime.setPreferredWidth(90);

        ((JComboBox) ((DefaultCellEditor) tableSeries.getColumnModel().getColumn(2).getCellEditor()).getComponent()).addActionListener((ActionEvent e) -> {
            updateTooltip();
        });
        tableSeries.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            updateTooltip();
        });

    }

    TableModelListener modelSeriesListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (started) {
                try {
                    int index = e.getFirstRow();
                    if (e.getColumn() == 5) {
                        final Color color = Preferences.getColorFromString((String) modelSeries.getValueAt(index, 5));
                        getTimePlotSeries(index).setColor(color);
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        }
    };

    TableModelListener modelChartsListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (started) {
                for (int i = 0; i < plots.size(); i++) {
                    Double y1min = (Double) modelCharts.getValueAt(i, 1);
                    Double y1max = (Double) modelCharts.getValueAt(i, 2);
                    Double y2min = (Double) modelCharts.getValueAt(i, 3);
                    Double y2max = (Double) modelCharts.getValueAt(i, 4);
                    Double duration = (Double) modelCharts.getValueAt(i, 5);
                    Boolean markers = (Boolean) modelCharts.getValueAt(i, 6);
                    TimePlotBase plot = plots.get(i);
                    plot.setMarkersVisible(Boolean.TRUE.equals(markers));
                    plot.setDurationMillis((duration == null) ? 60000 : (int) (duration * 1000));
                    if ((y1min != null) && (y1max != null)) {
                        plot.setY1AxisScale(y1min, y1max);
                    }
                    if ((y2min != null) && (y2max != null)) {
                        plot.setY2AxisScale(y2min, y2max);
                    }
                }
            }
        }
    };

    public void initializePlots() {
        pnGraphs.removeAll();
        plots.clear();
    }

    JButton saveButton;

    protected void update() {
        boolean editing = !started;
        int rows = modelSeries.getRowCount();
        int cur = tableSeries.getSelectedRow();
        buttonUp.setEnabled((rows > 0) && (cur > 0) && editing);
        buttonDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && editing);
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && editing);
        buttonInsert.setEnabled(editing);
        buttonNew.setEnabled(editing);
        buttonLoad.setEnabled(editing);
        buttonNew.setEnabled(editing);
        buttonStartStop.setEnabled((modelSeries.getRowCount() > 0) || (started));
        buttonStartStop.setText(started ? "Stop" : "Start");

        //tableSeries.setEnabled(editing);
        //tableCharts.setEnabled(editing);
        ckPersistence.setEnabled(editing && (Context.getInstance() != null));
        textFileName.setEnabled((ckPersistence.isSelected() && editing) || !ckPersistence.isSelected());
        comboFormat.setEnabled(textFileName.isEnabled());
        comboLayout.setEnabled(textFileName.isEnabled());
        textStreamFilter.setEnabled(editing);
        spinnerDragInterval.setEnabled(editing);
        spinnerUpdate.setEnabled(editing);

        boolean saveButtonVisible = started && !ckPersistence.isSelected();
        if (saveButtonVisible && (saveButton == null)) {
            saveButton = new JButton("Save");
            saveButton.setEnabled(Context.getInstance() != null);
            saveButton.addActionListener((ActionEvent e) -> {
                try {
                    saveData();
                } catch (Exception ex) {
                    showException(ex);
                }
            });
            ((GroupLayout) jPanel2.getLayout()).replace(ckPersistence, saveButton);
            saveButton.setMaximumSize(new Dimension(ckPersistence.getPreferredSize().width, comboFormat.getHeight()));
            saveButton.setMinimumSize(new Dimension(ckPersistence.getPreferredSize().width, comboFormat.getHeight()));
        } else if (!saveButtonVisible && (saveButton != null)) {
            ((GroupLayout) jPanel2.getLayout()).replace(saveButton, ckPersistence);
            saveButton = null;
        }
    }

    boolean isSeriesTableRowEditable(int row, int column) {
        boolean editing = !started;
        return ((column >= tableSeries.getColumnCount() - 2) || editing);
    }

    boolean isChartTableRowEditable(int row, int column) {
        boolean editing = !started;
        return (column > 0) && (editing || (column < 7));
    }

    String getDefaultFolder() {
        if (defaultFolder == null) {
            return Sys.getUserHome();
        }
        return defaultFolder.getAbsolutePath();
    }

    protected void save() throws IOException {
        JFileChooser chooser = new JFileChooser(getDefaultFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Strip Chart definition file", FILE_EXTENSION);
        chooser.setFileFilter(filter);
        if (file != null) {
            try {
                chooser.setSelectedFile(file);
            } catch (Exception ex) {
            }
        }
        chooser.setFileHidingEnabled(false);
        int rVal = chooser.showSaveDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (IO.getExtension(chooser.getSelectedFile().getAbsolutePath()).isEmpty()) {
                fileName += "." + FILE_EXTENSION;
            }
            save(new File(fileName));
        }
    }

    public void save(File file) throws IOException {
        Logger.getLogger(StripChart.class.getName()).info("Save: " + file.getAbsolutePath());
        this.file = file;
        updateTitle();
        ArrayList state = new ArrayList();
        state.add(modelSeries.getDataVector());
        state.add(modelCharts.getDataVector());
        if (ckPersistence.isSelected()) {
            state.add(new Object[][]{new Object[]{textFileName.getText(), String.valueOf(comboFormat.getSelectedItem()), true, comboLayout.getSelectedItem()}});
        } else {
            state.add(new Object[][]{new Object[0]});
        }
        Color c = backgroundColor;
        String background = (c == null) ? "" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        c = gridColor;
        String grid = (c == null) ? "" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        state.add(new Object[][]{new Object[]{textStreamFilter.getText().trim(), spinnerDragInterval.getValue(), spinnerUpdate.getValue()},
        new Object[]{background, grid}});

        String json = EncoderJson.encode(state, true);
        //This is to make easy to access the file in text editor.
        json = json.replace("] ] ]", "] ]  \n]").replace("[ [ [", "[ \n    [ [").replace("],", "],\n   ").replace("  [ [", "[ [");
        Files.write(file.toPath(), json.getBytes());
    }

    public void clear() throws IOException {
        Logger.getLogger(StripChart.class.getName()).info("Clear");
        stop();
        ckPersistence.setSelected(false);
        textStreamFilter.setText("");
        spinnerDragInterval.setValue(1000);
        spinnerUpdate.setValue(100);
        backgroundColor = defaultBackgroundColor;
        gridColor = defaultGridColor;
        panelColorBackground.setBackground(backgroundColor);
        panelColorGrid.setBackground(gridColor);
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        comboFormat.setSelectedItem(getInitFormat());
        comboLayout.setSelectedItem(getInitLayout());
        modelSeries.setRowCount(0);
        modelCharts.setDataVector(new Object[][]{
            {"1", null, null, null, null, null, false},
            {"2", null, null, null, null, null, false},
            {"3", null, null, null, null, null, false},
            {"4", null, null, null, null, null, false},
            {"5", null, null, null, null, null, false}
        }, SwingUtils.getTableColumnNames(tableCharts));
        file = null;
        updateTitle();
        initializeTable();
        update();
    }

    String getInitFormat() {
        if (Context.getInstance() != null) {
            if (Context.getInstance().getDataManager().getProvider() instanceof ProviderCSV) {
                return "csv";
            }
            if (Context.getInstance().getDataManager().getProvider() instanceof ProviderText) {
                return "txt";
            }
        }
        return "h5";
    }

    String getInitLayout() {
        return "table";
        /*
        if (Context.getInstance() != null) {
            switch (Context.getInstance().getDataManager().getLayout().getId()) {
                case ("LayoutTable"):
                    return "table";
                case ("LayoutSF"):
                    return "sf";
            }
        }
        return "default";
         */
    }

    File file;

    void updateTitle() {
        String title = "Strip Chart";
        if (App.isStripChartServer()) {
            int pid = Sys.getPid();
            title += " [" + pid + "]";
        }
        if ((file == null) || (!file.exists())) {
            setTitle(title);
        } else {
            setTitle(title + " - " + file.getName());
        }
    }

    protected void open() throws IOException {
        JFileChooser chooser = new JFileChooser(getDefaultFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Strip Chart definition file", FILE_EXTENSION);
        chooser.setFileFilter(filter);
        chooser.setFileHidingEnabled(true);

        int rVal = chooser.showOpenDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            open(chooser.getSelectedFile());
        }
    }

    public void open(File file) throws IOException {
        Logger.getLogger(StripChart.class.getName()).info("Open: " + file.getAbsolutePath());
        String json = new String(Files.readAllBytes(file.toPath()));
        open(json);
        this.file = file;
        updateTitle();
    }

    public void open(String json) throws IOException {
        json = json.replace("'", "\"");
        Object[][][] state = null;
        try {
            //parse a complete configuration
            state = (Object[][][]) EncoderJson.decode(json, Object[][][].class);
        } catch (Exception ex) {
            //parse list of channel names
            List<String> channels = (List<String>) EncoderJson.decode(json, List.class);
            state = new Object[1][channels.size()][];
            for (int i = 0; i < channels.size(); i++) {
                Type type = Type.Channel;
                String name = channels.get(i);
                int plot = 1;
                int axis = 1;
                String color = null;
                try {
                    //Try parsing protocol/parameters
                    InlineDevice dev = new InlineDevice(name);
                    name = dev.getId();
                    switch (dev.getProtocol()) {
                        case "bs":
                            type = Type.Stream;
                            break;
                        case "cs":
                            type = Type.CamServer;
                            break;
                        case "dev":
                            type = Type.Device;
                            break;
                        case "pv":
                        case "ca":
                        default:
                            break;
                    }
                    if (dev.getPars().keySet().contains("plot")) {
                        plot = Integer.valueOf(String.valueOf(dev.getPars().get("plot")));
                    }
                    if (dev.getPars().keySet().contains("axis")) {
                        axis = Integer.valueOf(String.valueOf(dev.getPars().get("axis")));
                    }
                    if (dev.getPars().keySet().contains("color")) {
                        color = String.valueOf(dev.getPars().get("color"));
                    }
                } catch (Exception e) {
                }
                state[0][i] = new Object[]{true, name, type, plot, axis, color, null};
            }
        }
        if (state.length > 0) {
            modelSeries.setDataVector((Object[][]) state[0], SwingUtils.getTableColumnNames(tableSeries));
        }
        if (modelSeries.getColumnCount() > 6) {
            for (int i = 0; i < modelSeries.getRowCount(); i++) {
                Object value = modelSeries.getValueAt(i, 6);
                Type type = Type.valueOf(modelSeries.getValueAt(i, 2).toString());
                String channel = (type == Type.Channel) ? String.valueOf(modelSeries.getValueAt(i, 1)) : null;
                modelSeries.setValueAt((value != null) && (value instanceof Map) ? new StripChartAlarmConfig((Map) value, channel) : null, i, 6);
            }
        }

        if (state.length > 1) {
            modelCharts.setDataVector((Object[][]) state[1], SwingUtils.getTableColumnNames(tableCharts));
        }
        textStreamFilter.setText("");
        spinnerDragInterval.setValue(1000);
        spinnerUpdate.setValue(100);
        backgroundColor = defaultBackgroundColor;
        gridColor = defaultGridColor;
        panelColorBackground.setBackground(backgroundColor);
        panelColorGrid.setBackground(gridColor);
        ckPersistence.setSelected(false);
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        comboFormat.setSelectedItem(getInitFormat());
        comboLayout.setSelectedItem(getInitLayout());
        if (state.length > 1) {

        }
        if (state.length > 2) {
            Object[] persistPars = ((Object[][]) state[2])[0];
            if (persistPars.length > 0) {
                String fileName = (String) persistPars[0];
                if ((fileName != null) && !fileName.isEmpty()) {
                    ckPersistence.setSelected(true);
                    textFileName.setText(fileName);
                }
                if (persistPars.length > 1) {
                    if ((((String) persistPars[1]).equalsIgnoreCase("txt"))) {
                        comboFormat.setSelectedIndex(1);
                    } else if ((((String) persistPars[1]).equalsIgnoreCase("csv"))) {
                        comboFormat.setSelectedIndex(2);
                    }
                }
                if (persistPars.length > 3) {
                    comboLayout.setSelectedItem((String) persistPars[3]);
                }
            }
        }
        if (state.length > 3) {
            //Loading settings
            Object[] settings = ((Object[][]) state[3])[0];
            if ((settings.length > 0) && (settings[0] != null)) {
                textStreamFilter.setText(settings[0].toString().trim());
            }
            if ((settings.length > 1) && (settings[1] != null) && (settings[1] instanceof Integer)) {
                spinnerDragInterval.setValue((Integer) settings[1]);
            }
            if ((settings.length > 2) && (settings[2] != null) && (settings[2] instanceof Integer)) {
                spinnerUpdate.setValue((Integer) settings[2]);
            }
            if (((Object[][]) state[3]).length > 1) {
                //Loading colors
                Object[] colors = ((Object[][]) state[3])[1];
                if ((colors[0] != null) && (colors[0] instanceof String) && !((String) colors[0]).isEmpty()) {
                    backgroundColor = Preferences.getColorFromString((String) colors[0]);
                    panelColorBackground.setBackground(backgroundColor);
                }
                if ((colors[1] != null) && (colors[1] instanceof String) && !((String) colors[1]).isEmpty()) {
                    gridColor = Preferences.getColorFromString((String) colors[1]);
                    panelColorGrid.setBackground(gridColor);
                }
            }
        }
        updateTitle();
        initializeTable();
        update();

    }

    final ArrayList<Device> devices = new ArrayList<>();
    final HashMap<Vector, Integer> seriesIndexes = new HashMap<>();
    final ArrayList<TimePlotSeries> timePlotSeries = new ArrayList<>();

    TimePlotSeries getTimePlotSeries(int row) {
        int index = 0;
        for (int i = 0; i < modelSeries.getRowCount(); i++) {
            if (modelSeries.getValueAt(i, 0).equals(Boolean.TRUE)) {
                if (row == i) {
                    return timePlotSeries.get(index);
                }
                index++;
            }
        }
        return null;
    }

    public void start() throws Exception {
        stop();
        Logger.getLogger(StripChart.class.getName()).info("Start");
        chartElementProducer.reset();
        chartElementProducer.setDispatchTimer((Integer) spinnerUpdate.getValue());

        if (modelSeries.getRowCount() == 0) {
            return;
        }
        streamDevices = 0;
        int numPlots = 1;
        Vector vector = modelSeries.getDataVector();
        for (Vector info : (Vector[]) vector.toArray(new Vector[0])) {
            final Type type = Type.valueOf(info.get(2).toString());
            if (info.get(0).equals(true)) {
                numPlots = Integer.max(numPlots, (Integer) info.get(3));
                if (type == Type.Stream) {
                    streamDevices++;
                }
            }
        }

        for (int i = 0; i < numPlots; i++) {
            Double y1min = (Double) modelCharts.getValueAt(i, 1);
            Double y1max = (Double) modelCharts.getValueAt(i, 2);
            Double y2min = (Double) modelCharts.getValueAt(i, 3);
            Double y2max = (Double) modelCharts.getValueAt(i, 4);
            Double duration = (Double) modelCharts.getValueAt(i, 5);
            Boolean markers = (Boolean) modelCharts.getValueAt(i, 6);
            Boolean localTime = (Boolean) modelCharts.getValueAt(i, 7);

            TimePlotBase plot = (TimePlotBase) Class.forName(HistoryChart.getTimePlotImpl()).newInstance();
            plot.setQuality(PlotPanel.getQuality());
            plot.setTimeAxisLabel(null);
            plot.setLegendVisible(true);
            plot.setMarkersVisible(Boolean.TRUE.equals(markers));
            plot.setDurationMillis((duration == null) ? 60000 : (int) (duration * 1000));
            if ((y1min != null) && (y1max != null)) {
                plot.setY1AxisScale(y1min, y1max);
            }
            if ((y2min != null) && (y2max != null)) {
                plot.setY2AxisScale(y2min, y2max);
            }
            if (numPlots > 1) {
                plot.setAxisSize(50);
            }

            if (backgroundColor != null) {
                plot.setPlotBackgroundColor(backgroundColor);
            }
            if (gridColor != null) {
                plot.setPlotGridColor(gridColor);
            }
            if ((tickLabelFont != null) && (plot instanceof TimePlotJFree)) {
                ((TimePlotJFree) plot).setLabelFont(tickLabelFont);
                ((TimePlotJFree) plot).setTickLabelFont(tickLabelFont);
            }

            plots.add(plot);
            pnGraphs.add(plot);
        }
        started = true;
        persisting = ckPersistence.isSelected();
        update();
        if (streamDevices > 0) {
            if (dispatcher == null) {
                dispatcher = ch.psi.pshell.bs.Provider.getOrCreateDefault();
                if (dispatcher != ch.psi.pshell.bs.Provider.getDefault()) {
                    synchronized (instantiatedDevices) {
                        instantiatedDevices.add(dispatcher);
                    }
                }
            }
            stream = new Stream("StripChart stream", dispatcher);
            if (!textStreamFilter.getText().trim().isEmpty()) {
                try {
                    stream.setFilter(textStreamFilter.getText().trim());
                } catch (Exception ex) {
                    showException(ex);
                }
            }
            synchronized (instantiatedDevices) {
                instantiatedDevices.add(stream);
            }
            try {
                stream.initialize();
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        if (Context.getInstance() != null) {
            if (ckPersistence.isSelected()) {
                String path = Context.getInstance().getSetup().expandPath(textFileName.getText().trim().replace("{name}", "StripChart"));
                persistenceExecutor = new StripScanExecutor();
                persistenceExecutor.start(path, getNames(), String.valueOf(comboFormat.getSelectedItem()), String.valueOf(comboLayout.getSelectedItem()), true);
            }
        }
        boolean alarming = false;
        Vector[] rows = (Vector[]) vector.toArray(new Vector[0]);
        for (int i = 0; i < rows.length; i++) {
            Vector info = rows[i];
            if (info.get(0).equals(true)) {
                final String name = ((String) info.get(1)).trim();
                final Type type = Type.valueOf(info.get(2).toString());
                final int plotIndex = ((Integer) info.get(3)) - 1;
                final int axis = (Integer) info.get(4);
                final TimePlotBase plot = plots.get(plotIndex);
                final Color color = Preferences.getColorFromString((String) info.get(5));
                final StripChartAlarmConfig alarmConfig = (StripChartAlarmConfig) info.get(6);

                TimePlotSeries graph = (color != null) ? new TimePlotSeries(name, color, axis) : new TimePlotSeries(name, axis);
                seriesIndexes.put(info, plot.getNumberOfSeries());
                timePlotSeries.add(graph);
                plot.addSeries(graph);
                DeviceTask task = new DeviceTask();
                task.row = i;
                task.info = info;
                Boolean localTime = (Boolean) modelCharts.getValueAt(plotIndex, 7);
                task.localTime = (localTime == null) ? false : localTime;
                Thread t = new Thread(task, "Strip chart: " + name);
                t.setDaemon(true);
                tasks.add(task);
                t.start();
                if ((alarmConfig != null) && (alarmConfig.isEnabled())) {
                    alarming = true;
                }
            }
        }

        if (alarming) {
            startTimer();
        }
    }

    String[] getNames() throws IOException {
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < modelSeries.getRowCount(); i++) {
            String id = getId(i);
            if (id != null) {
                names.add(id);
            }
        }
        return names.toArray(new String[0]);
    }

    String getBaseId(Vector[] rows, int index) throws IOException {
        if ((index < 0) || (index >= rows.length)) {
            throw new IOException("Invalid name index");
        }
        Vector info = rows[index];
        if (info.get(0).equals(false)) {
            return null;
        }
        String id = ((String) info.get(1)).trim();
        Type type = Type.valueOf(info.get(2).toString());
        if (type == Type.CamServer) {
            if (id.contains("//")) {
                id = id.substring(id.indexOf("//") + 2); //Remove 'http://' or 'https://'
            }
            if (id.contains("/")) {
                id = id.substring(id.lastIndexOf("/") + 1);
            }
            id = id.replace(" ", ":");
        } else {
            if (id.contains(" ")) {
                id = id.substring(0, id.indexOf(" "));
            }
        }
        if (Context.getInstance() != null) {
            if (!Context.getInstance().getDataManager().getProvider().isPacked()) { //Filenames don't support ':'
                id = id.replace(":", "_");
            }
        }
        id = id.replace("/", "_");
        return id;
    }

    String getId(int index) throws IOException {
        Vector[] rows = (Vector[]) modelSeries.getDataVector().toArray(new Vector[0]);
        String id = getBaseId(rows, index);
        if (id != null) {
            int existing = 0;
            for (int i = 0; i < index; i++) {
                String otherId = getBaseId(rows, i);
                if (otherId != null) {
                    if (otherId.equals(id)) {
                        existing++;
                    }
                }
            }
            if (existing > 0) {
                id = id + "-" + (existing + 1);
            }
        }
        return id;
    }

    Timer timer;

    public void startTimer() {
        stopTimer();
        pulse = false;
        timer = new Timer(alarmInterval, (ActionEvent e) -> {
            try {
                if (isShowing()) {
                    onAlarmTimer();
                }
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
            }
        });
        timer.setInitialDelay(alarmInterval);
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    void onAlarmTimer() {
        List<Integer> alarmingPlots = new ArrayList<>();
        boolean alarming = false;
        boolean paused = buttonPause.isSelected();
        if (!paused) {
            for (DeviceTask task : tasks) {
                if (task.isAlarming()) {
                    alarming = true;
                    alarmingPlots.add(((Integer) task.info.get(3)) - 1);
                }
            }
        }
        if (alarming) {
            pulse = !pulse;
            if ((chronoDisableAlarmSound == null) || (chronoDisableAlarmSound.getEllapsed() > disableAlarmTimer)) {
                try {
                    if (alarmFile == null) {
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        Audio.playFile(alarmFile);
                    }
                } catch (Throwable t) {
                    Logger.getLogger(StripChart.class.getName()).log(Level.FINEST, null, t);
                }
                buttonSound.setSelected(false);
                buttonSound.setToolTipText("Stop sound alarm for 30 minutes");
            } else if (chronoDisableAlarmSound != null) {
                buttonSound.setToolTipText("Alarm sound will be restored in " + new SimpleDateFormat("mm:ss").format(disableAlarmTimer - chronoDisableAlarmSound.getEllapsed()));
            }
        } else {
            pulse = false;
        }
        setAlarming(alarming);

        for (int i = 0; i < plots.size(); i++) {
            TimePlotBase p = plots.get(i);
            if (alarmingPlots.contains(i)) {
                p.setPlotOutlineColor(pulse ? Color.RED : LinePlotBase.getOutlineColor());
            } else {
                p.setPlotOutlineColor(LinePlotBase.getOutlineColor());
            }
        }
    }

    boolean alarming;

    void setAlarming(boolean value) {
        alarming = value;
        buttonSound.setVisible(alarming && (tabPane.getSelectedComponent() == panelPlots));
    }

    final ArrayList<Device> instantiatedDevices = new ArrayList<>();

    ch.psi.pshell.bs.Provider dispatcher;
    Stream stream;
    ArrayList<Stream> cameraStreams = new ArrayList<>();

    class DeviceTask implements Runnable {

        Vector info;
        String id;
        int row;
        int index;
        double currentValue = Double.NaN;
        long currentTimestamp = -1;
        final Object persistLock = new Object();
        boolean localTime;

        boolean isAlarming() {
            final StripChartAlarmConfig alarmConfig = (StripChartAlarmConfig) info.get(6);
            return (alarmConfig == null) ? false : alarmConfig.isAlarm(currentValue);
        }

        void add(Device device, Object value, Long timestamp, TimePlotBase plot, int seriesIndex, boolean dragging) {
            try {
                long now = System.currentTimeMillis();
                if (localTime) {
                    value = device.take();
                    timestamp = now;
                } else {
                    TimestampedValue tValue = device.takeTimestamped();
                    if (tValue != null) {
                        if (timestamp == null) {
                            //To Make sure  value/timestamp pair is correct                
                            value = tValue.getValue();
                            timestamp = tValue.getTimestamp();
                        } else {
                            if (timestamp <= tValue.getTimestamp()) {
                                //Won't plot dragging values lower than currentValue
                                return;
                            }
                        }
                    }
                }

                long time = (timestamp == null) ? now : timestamp;
                double doubleValue;
                boolean repeatCurrent = false;
                if (value == null) {
                    doubleValue = Double.NaN;
                } else if (value instanceof Number) {
                    doubleValue = ((Number) value).doubleValue();
                } else if (value instanceof Boolean) {
                    doubleValue = Boolean.TRUE.equals(value) ? 1.0 : 0;
                    repeatCurrent = true;
                } else if (value.getClass().isEnum()) {
                    doubleValue = (double) Arrays.asList(value.getClass().getEnumConstants()).indexOf(value);
                    repeatCurrent = true;
                } else {
                    return;
                }

                if ((time == currentTimestamp) && (currentValue == doubleValue)) {
                    return;
                }

                if (repeatCurrent) {
                    if (!Double.isNaN(currentValue)) {
                        chartElementProducer.post(new ChartElement(plot, seriesIndex, time - 1, currentValue, true)); //Don't store
                    }
                }

                //plot.add(seriesIndex, time, doubleValue);
                //Invoking event thread to prevent https://sourceforge.net/p/jfreechart/bugs/1009/ (JFreeChart is not thread safe)                
                chartElementProducer.post(new ChartElement(plot, seriesIndex, time, doubleValue, dragging));

                if (!dragging) {
                    appendTimestamps.put(device, now);
                    if (persisting) {
                        synchronized (persistLock) {
                            persistenceExecutor.append(id, doubleValue, now, time);
                            index++;
                        }
                    }
                    currentValue = doubleValue;
                    currentTimestamp = time;
                }
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
            }
        }

        int sleep_ms;

        void checkDrag(TimePlotBase plot, int seriesIndex, Device dev) {
            long now = System.currentTimeMillis();
            Long appendTimestamp = appendTimestamps.get(dev);
            Long age = (appendTimestamp == null) ? null : now - appendTimestamp;
            if ((age == null) || (age >= sleep_ms)) {
                Long devTimestamp = dev.getTimestamp();
                if (devTimestamp != null) {
                    now = devTimestamp + age;
                }
                //System.out.println(seriesIndex + " | " + value + Chrono.getTimeStr(time, "dd/MM/YY HH:mm:ss.SSS"));
                add(dev, dev.take(), now, plot, seriesIndex, true);
            }
        }

        @Override
        public void run() {
            String name = ((String) info.get(1)).trim();
            final Type type = Type.valueOf(info.get(2).toString());
            final int plotIndex = ((Integer) info.get(3)) - 1;
            final int axis = (Integer) info.get(4);
            final int seriesIndex = seriesIndexes.get(info);
            final TimePlotBase plot = plots.get(plotIndex);

            Device dev = null;
            final DeviceListener deviceListener = new DeviceAdapter() {
                //@Override
                //public void onValueChanged(Device device, Object value, Object former) {
                //    add(device, value, null, plot, seriesIndex);
                //}
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    add(device, value, null, plot, seriesIndex, false);
                }
            };
            Logger.getLogger(StripChart.class.getName()).finer("Starting device monitoring task: " + name);
            try {
                id = getId(row);
                switch (type) {
                    case Channel:
                        int polling = -1;
                        boolean timestamped = true;
                        int precision = -1;
                        if (name.contains(" ")) {
                            String[] tokens = name.split(" ");
                            name = tokens[0];
                            try {
                                polling = Integer.valueOf(tokens[1]);
                                precision = Integer.valueOf(tokens[2]);
                            } catch (Exception ex) {
                            }
                        }
                        dev = new ChannelDouble(name, name, precision, timestamped);

                        synchronized (instantiatedDevices) {
                            instantiatedDevices.add(dev);
                        }
                        if (polling <= 0) {
                            dev.setMonitored(true);
                        } else {
                            dev.setPolling(polling);
                        }
                        break;
                    case Device:
                        if (Context.getInstance() != null) {
                            if (Context.getInstance().getState() == State.Initializing) {
                                Logger.getLogger(StripChart.class.getName()).fine("Waiting finish context initialization...");
                                Context.getInstance().waitStateNot(State.Initializing, -1);
                                Logger.getLogger(StripChart.class.getName()).fine("Waiting done");
                            }
                            try {
                                dev = Context.getInstance().getDevicePool().getByName(name, Device.class);
                                if (dev == null) {
                                    dev = (Device) Context.getInstance().tryEvalLineBackground(name);
                                }
                            } catch (Exception ex) {
                                try {
                                    dev = InlineDevice.create(name, null);
                                } catch (Exception e) {
                                }
                            }
                        }
                        break;
                    case Stream:
                        int modulo = Scalar.DEFAULT_MODULO;
                        int offset = Scalar.DEFAULT_OFFSET;
                        boolean useGlobalTimestamp = true;
                        if (name.contains(" ")) {
                            String[] tokens = name.split(" ");
                            name = tokens[0];
                            try {
                                modulo = Integer.valueOf(tokens[1]);
                                offset = Integer.valueOf(tokens[2]);
                                useGlobalTimestamp = !tokens[3].equalsIgnoreCase("false");
                            } catch (Exception ex) {
                            }
                        }
                        dev = stream.addScalar(name, name, modulo, offset);
                        Logger.getLogger(StripChart.class.getName()).finer("Adding channel to stream: " + name);
                        ((Scalar) dev).setUseLocalTimestamp(!useGlobalTimestamp);
                        streamDevices--;
                        break;
                    case CamServer:
                        if (name.contains(" ")) {
                            String[] tokens = name.split(" ");
                            String url = tokens[0];
                            if (!url.startsWith("tcp://")) {
                                String instanceName = url.substring(url.lastIndexOf("/") + 1);
                                url = url.substring(0, url.lastIndexOf("/"));
                                PipelineServer server = new PipelineServer(null, url);
                                try {
                                    server.initialize();
                                    url = server.getStream(instanceName);
                                } finally {
                                    server.close();
                                }
                            }
                            name = tokens[1];
                            Stream s = null;
                            url = url.trim();
                            synchronized (cameraStreams) {
                                for (Stream cs : cameraStreams) {
                                    if (cs.getAddress().equals(url)) {
                                        s = cs;
                                        break;
                                    }
                                }
                                if (s == null) {
                                    Logger.getLogger(StripChart.class.getName()).fine("Connecting to cam server stream: " + url);
                                    ch.psi.pshell.bs.Provider p = new ch.psi.pshell.bs.Provider(null, url, false);
                                    s = new Stream(null, p);
                                    cameraStreams.add(s);
                                    p.initialize();
                                    s.start();
                                    s.waitCacheChange(10000);
                                    synchronized (instantiatedDevices) {
                                        instantiatedDevices.add(s);
                                        instantiatedDevices.add(p);
                                    }
                                }
                                dev = s.getChild(name);
                            }
                        }
                        break;
                }
                if (dev == null) {
                    return;
                }
                dev.addListener(deviceListener);
                if (!dev.isInitialized()) {
                    dev.initialize();
                }
                if ((type == Type.Stream) && (streamDevices == 0)) {
                    try {
                        Logger.getLogger(StripChart.class.getName()).fine("Starting stream");
                        stream.start(true);
                    } catch (Exception ex) {
                        Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
                    }
                } else if (type == Type.Channel) {
                    dev.request();
                }

                dragInterval = (Integer) spinnerDragInterval.getValue();
                while (started) {
                    sleep_ms = (dragInterval > 0)
                            ? dragInterval
                            : //((dev.isPolled() && !dev.isMonitored()) ? dev.getPolling() + dragInterval: dragInterval):
                            0;
                    synchronized (lock) {
                        lock.wait(sleep_ms);
                    }
                    if ((started) && (sleep_ms > 0)) {
                        checkDrag(plot, seriesIndex, dev);
                    }
                }
            } catch (InterruptedException ex) {
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
            } finally {
                Logger.getLogger(StripChart.class.getName()).finer("Exiting device monitoring task: " + name);
                if (dev != null) {
                    dev.removeListener(deviceListener);
                }
            }
        }

        void saveDataset() throws Exception {
            if (!persisting) {
                String name = ((String) info.get(1)).trim();
                final Type type = Type.valueOf(info.get(2).toString());
                final int plotIndex = ((Integer) info.get(3)) - 1;
                final int axis = (Integer) info.get(4);
                final int seriesIndex = seriesIndexes.get(info);
                final TimePlotBase plot = plots.get(plotIndex);
                id = getId(row);

                //TimestampedValue current = null;
                Double current = null;
                for (TimestampedValue<Double> item : plot.getSeriestData(seriesIndex)) {
                    Double value = item.getValue();
                    if (value == null) {
                        value = Double.NaN;
                    }
                    //Todo: Find better way to filter dragging, this don't match continuous persistence 
                    persistenceExecutor.append(id, value, item.getTimestamp(), item.getTimestamp());
                    //current = item;
                    current = value;
                }

            }
        }
    };

    final Object lock = new Object();
    volatile boolean started = false;

    void stop() {
        if (started) {
            stopTimer();
            chartElementProducer.setDispatchTimer(0);
            Logger.getLogger(StripChart.class.getName()).info("Stop");
            started = false;
            setAlarming(false);
            tasks.clear();
            setButtonPause(true);
            update();
            synchronized (lock) {
                lock.notifyAll();
            }
            synchronized (instantiatedDevices) {
                ArrayList<Device> devices = (ArrayList<Device>) instantiatedDevices.clone();
                Collections.reverse(devices);
                for (Device dev : devices) {
                    try {
                        dev.close();
                    } catch (Exception ex) {
                        Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
                    }
                }
                instantiatedDevices.clear();
            }
            synchronized (cameraStreams) {
                cameraStreams.clear();
            }
            dispatcher = null;
            if (persistenceExecutor != null) {
                persistenceExecutor.finish();
            }
            seriesIndexes.clear();
            timePlotSeries.clear();
            initializePlots();
            appendTimestamps.clear();
        }
    }

    @Override
    protected void onOpened() {
        if (MainFrame.isDarcula()) {
            //TODO: Repeating the model initialization as a workaround for the exception when spinner getting focus on Darcula LAF.
            spinnerDragInterval.setModel(new javax.swing.SpinnerNumberModel(1000, -1, 99999, 1));
            spinnerUpdate.setModel(new javax.swing.SpinnerNumberModel(0, 0, 60000, 1000));
        }
    }

    @Override
    protected void onClosed() {
        stop();
    }

    public static File resolveFile(File file, File defaultFolder) throws FileNotFoundException {
        if (Context.getInstance() != null) {
            file = new File(Context.getInstance().getSetup().expandPath(file.getPath()));
        }
        if (!file.exists()) {
            if (defaultFolder != null) {
                File aux = Paths.get(defaultFolder.getAbsolutePath(), file.getPath()).toFile();
                if (aux.exists()) {
                    file = aux;
                }
            } else {
                File aux = Paths.get(Sys.getUserHome(), file.getPath()).toFile();
                if (aux.exists()) {
                    file = aux;
                }
            }
        }
        IO.assertExistsFile(file);
        return file;
    }

    public static void create(File file, String config, File defaultFolder, boolean start, boolean modal) {
        java.awt.EventQueue.invokeLater(() -> {
            StripChart dialog = new StripChart(null, modal, defaultFolder);
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            try {
                if (file != null) {
                    File f = resolveFile(file, defaultFolder);
                    dialog.open(f);
                } else if (config != null) {
                    dialog.open(config);
                }
                if (start) {
                    dialog.buttonStartStopActionPerformed(null);
                }
            } catch (IOException ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    dialog.onClosed();
                    if (modal) {
                        System.exit(0);
                    } else if (App.isStripChart() && App.isAttach()) {
                        if (SwingUtils.getVisibleWindows().length == 1) {
                            System.out.println("Last StripChart window closes: finishing process" + Sys.getProcessName() + "\n");
                            System.exit(0);
                        }
                    }
                }
            });
            SwingUtils.centerComponent(null, dialog);
            if (dialog.getOwner()!=null){
                dialog.getOwner().setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            }
            dialog.setVisible(true);
            dialog.requestFocus();
        });
    }

    void saveData() throws IOException {
        if ((started) && (!persisting)) {
            try {
                persistenceExecutor = new StripScanExecutor();
                String path = Context.getInstance().getSetup().expandPath(textFileName.getText().trim().replace("{name}", "StripChart"));
                persistenceExecutor.start(path, getNames(), String.valueOf(comboFormat.getSelectedItem()), String.valueOf(comboLayout.getSelectedItem()), false);
                for (DeviceTask task : tasks) {
                    try {
                        task.saveDataset();
                    } catch (Exception ex) {
                        Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
                showMessage("Strip Chart", "Success saving data to: \n" + path);
            } finally {
                persistenceExecutor.finish();
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabPane = new javax.swing.JTabbedPane();
        panelConfig = new javax.swing.JPanel();
        panelSeries = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSeries = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isSeriesTableRowEditable(row, column);
            };
        };
        buttonDelete = new javax.swing.JButton();
        buttonUp = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        panelAxis = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableCharts = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isChartTableRowEditable(row, column);
            };
        };
        panelFile = new javax.swing.JPanel();
        buttonLoad = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonNew = new javax.swing.JButton();
        buttonStartStop = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        ckPersistence = new javax.swing.JCheckBox();
        textFileName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        comboFormat = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        comboLayout = new javax.swing.JComboBox<>();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        textStreamFilter = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        spinnerDragInterval = new javax.swing.JSpinner();
        jPanel1 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        panelColorBackground = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        panelColorGrid = new javax.swing.JPanel();
        buttonDefaultColors = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        spinnerUpdate = new javax.swing.JSpinner();
        panelPlots = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        pnGraphs = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        tabPane.setName("tabPane"); // NOI18N
        tabPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabPaneStateChanged(evt);
            }
        });

        panelConfig.setName("panelConfig"); // NOI18N

        panelSeries.setBorder(javax.swing.BorderFactory.createTitledBorder("Series"));

        tableSeries.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Name", "Type", "Plot", "Y Axis", "Color", "Alarm"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableSeries.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSeries.getTableHeader().setReorderingAllowed(false);
        tableSeries.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableSeriesMouseReleased(evt);
            }
        });
        tableSeries.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableSeriesKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tableSeries);

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonUp.setText("Move Up");
        buttonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDown.setText("Move Down");
        buttonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelSeriesLayout = new javax.swing.GroupLayout(panelSeries);
        panelSeries.setLayout(panelSeriesLayout);
        panelSeriesLayout.setHorizontalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSeriesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(panelSeriesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDown)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDelete)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelSeriesLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        panelSeriesLayout.setVerticalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSeriesLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addGroup(panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonDown)
                    .addComponent(buttonUp))
                .addContainerGap())
        );

        panelSeriesLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        panelAxis.setBorder(javax.swing.BorderFactory.createTitledBorder("Charts"));

        tableCharts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"1", null, null, null, null, null, null, null},
                {"2", null, null, null, null, null, null, null},
                {"3", null, null, null, null, null, null, null},
                {"4", null, null, null, null, null, null, null},
                {"5", null, null, null, null, null, null, null}
            },
            new String [] {
                "Chart", "Y1min", "Y1max", "Y2min", "Y2max", "Duration(s)", "Markers", "Local Time"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableCharts.setRowSelectionAllowed(false);
        tableCharts.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableCharts.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tableCharts);

        javax.swing.GroupLayout panelAxisLayout = new javax.swing.GroupLayout(panelAxis);
        panelAxis.setLayout(panelAxisLayout);
        panelAxisLayout.setHorizontalGroup(
            panelAxisLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAxisLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );
        panelAxisLayout.setVerticalGroup(
            panelAxisLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAxisLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        buttonLoad.setText("Open");
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        buttonSave.setText("Save");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonNew.setText("New");
        buttonNew.setPreferredSize(new java.awt.Dimension(89, 23));
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewActionPerformed(evt);
            }
        });

        buttonStartStop.setText("Start");
        buttonStartStop.setPreferredSize(new java.awt.Dimension(89, 23));
        buttonStartStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFileLayout = new javax.swing.GroupLayout(panelFile);
        panelFile.setLayout(panelFileLayout);
        panelFileLayout.setHorizontalGroup(
            panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFileLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonStartStop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonNew, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonLoad)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonSave)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelFileLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonLoad, buttonNew, buttonSave, buttonStartStop});

        panelFileLayout.setVerticalGroup(
            panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFileLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSave)
                    .addComponent(buttonLoad)
                    .addComponent(buttonNew, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonStartStop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        panelFileLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonLoad, buttonNew, buttonSave, buttonStartStop});

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Persistence"));

        ckPersistence.setText("Save to:");
        ckPersistence.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckPersistenceActionPerformed(evt);
            }
        });

        jLabel1.setText("Format:");

        comboFormat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "h5", "txt", "csv" }));

        jLabel5.setText("Layout:");

        comboLayout.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "default", "table", "sf" }));
        comboLayout.setSelectedIndex(1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ckPersistence)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textFileName, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(ckPersistence)
                    .addComponent(textFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(comboFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        jLabel2.setText("Stream filter:");

        jLabel3.setText("Drag Interval (ms):");

        spinnerDragInterval.setModel(new javax.swing.SpinnerNumberModel(1000, -1, 99999, 100));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textStreamFilter)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerDragInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textStreamFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(spinnerDragInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Graphics"));

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Background:");

        panelColorBackground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelColorBackground.setPreferredSize(new java.awt.Dimension(44, 23));
        panelColorBackground.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelColorBackgroundMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelColorBackgroundLayout = new javax.swing.GroupLayout(panelColorBackground);
        panelColorBackground.setLayout(panelColorBackgroundLayout);
        panelColorBackgroundLayout.setHorizontalGroup(
            panelColorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 44, Short.MAX_VALUE)
        );
        panelColorBackgroundLayout.setVerticalGroup(
            panelColorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Grid:");

        panelColorGrid.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelColorGrid.setPreferredSize(new java.awt.Dimension(44, 23));
        panelColorGrid.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelColorGridMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelColorGridLayout = new javax.swing.GroupLayout(panelColorGrid);
        panelColorGrid.setLayout(panelColorGridLayout);
        panelColorGridLayout.setHorizontalGroup(
            panelColorGridLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 44, Short.MAX_VALUE)
        );
        panelColorGridLayout.setVerticalGroup(
            panelColorGridLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        buttonDefaultColors.setText("Defaults");
        buttonDefaultColors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultColorsActionPerformed(evt);
            }
        });

        jLabel4.setText("Plot Update (ms):");

        spinnerUpdate.setModel(new javax.swing.SpinnerNumberModel(100, 0, 60000, 100));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelColorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelColorGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonDefaultColors)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelColorGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelColorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(spinnerUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(buttonDefaultColors, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDefaultColors, jLabel15, jLabel17, panelColorBackground, panelColorGrid});

        javax.swing.GroupLayout panelConfigLayout = new javax.swing.GroupLayout(panelConfig);
        panelConfig.setLayout(panelConfigLayout);
        panelConfigLayout.setHorizontalGroup(
            panelConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelSeries, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelAxis, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelConfigLayout.setVerticalGroup(
            panelConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelConfigLayout.createSequentialGroup()
                .addComponent(panelSeries, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelAxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabPane.addTab("Config", panelConfig);

        panelPlots.setName("panelPlots"); // NOI18N
        panelPlots.setLayout(new java.awt.BorderLayout());

        scrollPane.setPreferredSize(new java.awt.Dimension(356, 303));

        pnGraphs.setPreferredSize(new java.awt.Dimension(354, 301));

        javax.swing.GroupLayout pnGraphsLayout = new javax.swing.GroupLayout(pnGraphs);
        pnGraphs.setLayout(pnGraphsLayout);
        pnGraphsLayout.setHorizontalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 642, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 546, Short.MAX_VALUE)
        );

        scrollPane.setViewportView(pnGraphs);

        panelPlots.add(scrollPane, java.awt.BorderLayout.CENTER);

        tabPane.addTab("Plots", panelPlots);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tabPane)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
        try {
            int rows = modelSeries.getRowCount();
            int cur = tableSeries.getSelectedRow();
            modelSeries.moveRow(cur, cur, cur - 1);
            tableSeries.setRowSelectionInterval(cur - 1, cur - 1);
            update();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonUpActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        try {
            int rows = modelSeries.getRowCount();
            int cur = tableSeries.getSelectedRow();
            modelSeries.moveRow(cur, cur, cur + 1);
            tableSeries.setRowSelectionInterval(cur + 1, cur + 1);
            update();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        Object[] data = new Object[]{Boolean.TRUE, "", Type.values()[0], 1, 1};
        if (tableSeries.getSelectedRow() >= 0) {
            modelSeries.insertRow(tableSeries.getSelectedRow() + 1, data);
        } else {
            modelSeries.addRow(data);
        }
        modelSeries.fireTableDataChanged();
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (modelSeries.getRowCount() > 0) {
            modelSeries.removeRow(Math.max(tableSeries.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        try {
            open();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void tabPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPaneStateChanged
        try {
            if (tabPane.getSelectedComponent() == panelPlots) {
                if (!started) {
                    start();
                }
                buttonPause.setVisible(plots.size() > 0);
                buttonSound.setVisible(alarming);
            } else {
                //stop();
                buttonPause.setVisible(false);
                buttonSound.setVisible(false);
            }
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_tabPaneStateChanged

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
        try {
            clear();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonNewActionPerformed

    private void tableSeriesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSeriesMouseReleased
        update();
    }//GEN-LAST:event_tableSeriesMouseReleased

    private void tableSeriesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableSeriesKeyReleased
        update();
    }//GEN-LAST:event_tableSeriesKeyReleased

    private void ckPersistenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckPersistenceActionPerformed
        update();
    }//GEN-LAST:event_ckPersistenceActionPerformed

    private void buttonStartStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartStopActionPerformed
        try {
            if (!started) {
                if (modelSeries.getRowCount() > 0) {
                    start();
                    tabPane.setSelectedComponent(panelPlots);
                }
            } else {
                stop();
            }
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_buttonStartStopActionPerformed

    private void panelColorBackgroundMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorBackgroundMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", backgroundColor);
        if (c != null) {
            backgroundColor = c;
            panelColorBackground.setBackground(backgroundColor);
            if (started) {
                for (TimePlotBase plot : plots) {
                    plot.setPlotBackgroundColor(backgroundColor);
                }
            }
        }
    }//GEN-LAST:event_panelColorBackgroundMouseClicked

    private void panelColorGridMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorGridMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", gridColor);
        if (c != null) {
            gridColor = c;
            panelColorGrid.setBackground(gridColor);
            if (started) {
                for (TimePlotBase plot : plots) {
                    plot.setPlotGridColor(gridColor);
                }
            }
        }
    }//GEN-LAST:event_panelColorGridMouseClicked

    private void buttonDefaultColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultColorsActionPerformed
        backgroundColor = defaultBackgroundColor;
        gridColor = defaultGridColor;
        panelColorBackground.setBackground(backgroundColor);
        panelColorGrid.setBackground(gridColor);
        if (started) {
            for (TimePlotBase plot : plots) {
                plot.setPlotGridColor(PlotBase.getGridColor());
                plot.setPlotBackgroundColor(PlotBase.getPlotBackground());
            }
        }
    }//GEN-LAST:event_buttonDefaultColorsActionPerformed

    /**
     */
    public static void main(String args[]) {
        App.init(args);
        String prop = System.getProperty(Setup.PROPERTY_PARALLEL_INIT);
        boolean parallel = ((prop != null) && (prop.length()>0)) ? Boolean.valueOf(prop) : false;
        String path = System.getProperty(Setup.PROPERTY_HOME_PATH);
        Epics.create(Paths.get((path==null) ? "." : path, "jcae.properties").toString(), parallel);
        create(App.getFileArg(), null, null, false, true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDefaultColors;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonStartStop;
    private javax.swing.JButton buttonUp;
    private javax.swing.JCheckBox ckPersistence;
    private javax.swing.JComboBox<String> comboFormat;
    private javax.swing.JComboBox<String> comboLayout;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel panelAxis;
    private javax.swing.JPanel panelColorBackground;
    private javax.swing.JPanel panelColorGrid;
    private javax.swing.JPanel panelConfig;
    private javax.swing.JPanel panelFile;
    private javax.swing.JPanel panelPlots;
    private javax.swing.JPanel panelSeries;
    private javax.swing.JPanel pnGraphs;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JSpinner spinnerDragInterval;
    private javax.swing.JSpinner spinnerUpdate;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTable tableCharts;
    private javax.swing.JTable tableSeries;
    private javax.swing.JTextField textFileName;
    private javax.swing.JTextField textStreamFilter;
    // End of variables declaration//GEN-END:variables
}
