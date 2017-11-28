package ch.psi.pshell.swing;

import ch.psi.pshell.bs.Dispatcher;
import ch.psi.pshell.bs.PipelineServer;
import ch.psi.pshell.bs.Scalar;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.data.ProviderHDF5;
import ch.psi.pshell.data.ProviderText;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.epics.ChannelDouble;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.pshell.plotter.Preferences;
import ch.psi.pshell.ui.App;
import ch.psi.utils.IO;
import ch.psi.utils.InvokingProducer;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 */
public class StripChart extends StandardDialog {

    public static final String FILE_EXTENSION = "scd";

    int dragInterval = 1000;

    enum Type {
        Channel,
        Stream,
        Device,
        CamServer
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

        ChartElement(TimePlotBase plot, int seriesIndex, long time, double value) {
            this.plot = plot;
            this.seriesIndex = seriesIndex;
            this.time = time;
            this.value = value;
        }
        int seriesIndex;
        long time;
        double value;
        TimePlotBase plot;
    }
    final InvokingProducer<ChartElement> chartElementProducer;

    JButton buttonPause = new JButton();

    boolean persisting;

    public StripChart(java.awt.Frame parent, boolean modal, File defaultFolder) {
        super(parent, modal);
        initComponents();
        chartElementProducer = new InvokingProducer<ChartElement>() {
            @Override
            protected void consume(ChartElement element) {
                element.plot.add(element.seriesIndex, element.time, element.value);
            }
        };

        buttonStartStop.setEnabled(false);
        textFileName.setEnabled(false);
        comboFormat.setEnabled(false);
        ckFlush.setEnabled(false);
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        ckFlush.setSelected((Context.getInstance() != null) ? Context.getInstance().getConfig().dataScanFlushRecords : false);
        updateTitle();
        setCancelledOnEscape(false);

        //pnGraphs.setLayout(new GridBagLayout());
        pnGraphs.setLayout(new BoxLayout(pnGraphs, BoxLayout.Y_AXIS));

        modelSeries = (DefaultTableModel) tableSeries.getModel();
        modelCharts = (DefaultTableModel) tableCharts.getModel();
        modelCharts.addTableModelListener(modelChartsListener);
        this.defaultFolder = defaultFolder;
        initializeTable();

        setLayout(new LayoutManager() {
            @Override
            public void layoutContainer(Container parent) {
                Insets insets = parent.getInsets();
                for (Component component : parent.getComponents()) {
                    if (component == buttonPause) {
                        Dimension ps = component.getPreferredSize();
                        int width = Math.min(ps.width, 30); //ps.width; //Math.max(ps.width, 60);
                        int height = Math.min(ps.height, 24);
                        if (!MainFrame.isDark()) {
                            try {
                                height = Math.min(height, pnGraphs.getLocationOnScreen().y - tabPane.getLocationOnScreen().y - 5);
                            } catch (Exception ex) {
                            }
                        }
                        component.setBounds(parent.getWidth() - insets.right - 2 - width, insets.top - (MainFrame.isDark() ? 2 : 0), width, height);
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
                boolean pause = buttonPause.getToolTipText().equals("Pause");
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
    }

    void setButtonPause(boolean paused) {
        buttonPause.setText("");
        buttonPause.setToolTipText(paused ? "Pause" : "Resume");
        buttonPause.setIcon(new ImageIcon(App.getResourceUrl((MainFrame.isDark() ? "dark/" : "") + (paused ? "Pause.png" : "Play.png"))));
    }

    void updateTooltip(JComponent text, int row) {
        String tooltip = "";
        try {
            if (row >= 0) {
                Type type = Type.valueOf(modelSeries.getValueAt(row, 2).toString());
                switch (type) {
                    case Channel:
                        tooltip = "Format: ChannelName [Polling=-1 Timestamped=false Precision=-1]";
                        break;
                    case Device:
                        tooltip = "Format: DeviceName";
                        break;
                    case Stream:
                        tooltip = "Format: Identifier [Modulo=10 Offset=0]";
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
        JComboBox comboType = new JComboBox();
        tableSeries.setRowHeight(Math.max(tableSeries.getRowHeight(), comboType.getPreferredSize().height - 3));
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (Type type : Type.values()) {
            model.addElement(type);
        }
        comboType.setModel(model);
        comboType.setEditable(true);

        DefaultCellEditor cellEditor = new DefaultCellEditor(comboType);
        cellEditor.setClickCountToStart(2);
        colType.setCellEditor(cellEditor);

        TableColumn colPlot = tableSeries.getColumnModel().getColumn(3);
        colPlot.setPreferredWidth(60);
        JComboBox comboPlot = new JComboBox();
        model = new DefaultComboBoxModel();
        for (int i = 1; i <= 5; i++) {
            model.addElement(i);
        }
        comboPlot.setModel(model);
        comboPlot.setEditable(true);
        cellEditor = new DefaultCellEditor(comboPlot);
        cellEditor.setClickCountToStart(2);
        colPlot.setCellEditor(cellEditor);

        TableColumn colY = tableSeries.getColumnModel().getColumn(4);
        colY.setPreferredWidth(60);
        JComboBox comboY = new JComboBox();
        model = new DefaultComboBoxModel();
        model.addElement(1);
        model.addElement(2);
        comboY.setModel(model);
        comboY.setEditable(true);
        cellEditor = new DefaultCellEditor(comboY);
        cellEditor.setClickCountToStart(2);
        colY.setCellEditor(cellEditor);

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
        for (int i = 1; i < modelCharts.getColumnCount(); i++) {
            tableCharts.getColumnModel().getColumn(i).setPreferredWidth(112);
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

        //Not needed with explicit stop button
        /*        
        TableModelListener changeListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {                
                stop();
            }
        };
        modelSeries.addTableModelListener(changeListener);
        modelCharts.addTableModelListener(changeListener);
         */
        comboType.addActionListener((ActionEvent e) -> {
            updateTooltip();
        });
        tableSeries.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            updateTooltip();
        });

    }

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
                    TimePlotBase plot = plots.get(i);
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
        //Redoing layout because it may have been changed by View's initComponents()
        //panelPlots.removeAll();
        //panelPlots.setLayout(new BorderLayout());
        //panelPlots.add(scrollPane);
        //pnGraphs.setLayout(new GridBagLayout());

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

        tableSeries.setEnabled(editing);
        //tableCharts.setEnabled(editing);
        ckPersistence.setEnabled(editing);
        textFileName.setEnabled((ckPersistence.isSelected() && editing) || !ckPersistence.isSelected());
        comboFormat.setEnabled(textFileName.isEnabled());
        ckFlush.setEnabled(textFileName.isEnabled());
        textStreamFilter.setEnabled(editing);
        spinnerDragInterval.setEnabled(editing);        

        boolean saveButtonVisible = started && !ckPersistence.isSelected();
        if (saveButtonVisible && (saveButton == null)) {
            saveButton = new JButton("Save");
            saveButton.addActionListener((ActionEvent e) -> {
                try {
                    saveData();
                } catch (Exception ex) {
                    SwingUtils.showException(this, ex);
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
            state.add(new Object[][]{new Object[]{textFileName.getText(), String.valueOf(comboFormat.getSelectedItem()), ckFlush.isSelected()}});
        } else {
            state.add(new Object[][]{new Object[0]});
        }
        Color c = backgroundColor;
        String background = (c == null) ? "" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();                        
        c = gridColor;
        String grid = (c == null) ? "" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();                        
        state.add(new Object[][]{new Object[]{textStreamFilter.getText().trim(), spinnerDragInterval.getValue()}, 
                                 new Object[]{background, grid}});

        String json = JsonSerializer.encode(state, true);
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
        backgroundColor = gridColor = null;
        panelColorBackground.setBackground(null);        
        panelColorGrid.setBackground(null);  
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        ckFlush.setSelected((Context.getInstance() != null) ? Context.getInstance().getConfig().dataScanFlushRecords : false);
        modelSeries.setRowCount(0);
        modelCharts.setDataVector(new Object[][]{
            {"1", null, null, null, null, null},
            {"2", null, null, null, null, null},
            {"3", null, null, null, null, null},
            {"4", null, null, null, null, null},
            {"5", null, null, null, null, null}
        }, SwingUtils.getTableColumnNames(tableCharts));
        file = null;
        updateTitle();
        initializeTable();
        update();
    }

    File file;

    void updateTitle() {
        if ((file == null) || (!file.exists())) {
            setTitle("Strip Chart");
        } else {
            setTitle("Strip Chart - " + file.getName());
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
        Object[][][] state = (Object[][][]) JsonSerializer.decode(json, Object[][][].class);
        if (state.length > 0) {
            modelSeries.setDataVector((Object[][]) state[0], SwingUtils.getTableColumnNames(tableSeries));
        }
        if (state.length > 1) {
            modelCharts.setDataVector((Object[][]) state[1], SwingUtils.getTableColumnNames(tableCharts));
        }
        textStreamFilter.setText("");
        spinnerDragInterval.setValue(1000);
        backgroundColor = gridColor = null;
        panelColorBackground.setBackground(null);        
        panelColorGrid.setBackground(null);  
        ckPersistence.setSelected(false);
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        ckFlush.setSelected((Context.getInstance() != null) ? Context.getInstance().getConfig().dataScanFlushRecords : false);
        comboFormat.setSelectedIndex(((Context.getInstance() != null) && (Context.getInstance().getDataManager().getProvider() instanceof ProviderText)) ? 1 : 0);
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
                if ((persistPars.length > 1) && (((String) persistPars[1]).equalsIgnoreCase("txt"))) {
                    comboFormat.setSelectedIndex(1);
                }
                if (persistPars.length > 2) {
                    ckFlush.setSelected(Boolean.TRUE.equals(persistPars[2]));
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
            if (((Object[][]) state[3]).length > 1){
                //Loading colors
                Object[] colors = ((Object[][]) state[3])[1];
                if ((colors[0] != null) && (colors[0] instanceof String) && !((String)colors[0]).isEmpty()) {                
                    backgroundColor =  Preferences.getColorFromString((String)colors[0]);
                    panelColorBackground.setBackground(backgroundColor);        
                }                                        
                if ((colors[1] != null) && (colors[1] instanceof String) && !((String)colors[1]).isEmpty()) {                
                    gridColor = Preferences.getColorFromString((String)colors[1]);
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

    public void start() throws Exception {
        stop();
        Logger.getLogger(StripChart.class.getName()).info("Start");
        chartElementProducer.reset();

        if (modelSeries.getRowCount() == 0) {
            return;
        }
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

            TimePlotBase plot = (TimePlotBase) Class.forName(HistoryChart.getTimePlotImpl()).newInstance();
            plot.setQuality(PlotPanel.getQuality());
            plot.setTimeAxisLabel(null);
            plot.setLegendVisible(true);
            plot.setMarkersVisible(false);
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
            
            if (backgroundColor != null){
                plot.setPlotBackgroundColor(backgroundColor);
            }
            if (gridColor != null){
                plot.setPlotGridColor(gridColor);
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
                    SwingUtils.showException(this, ex);
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
        if (ckPersistence.isSelected()) {
            openProvider();
        }

        Vector[] rows = (Vector[]) vector.toArray(new Vector[0]);
        for (int i = 0; i < rows.length; i++) {
            Vector info = rows[i];
            if (info.get(0).equals(true)) {
                final String name = ((String) info.get(1)).trim();
                final Type type = Type.valueOf(info.get(2).toString());
                final int plotIndex = ((Integer) info.get(3)) - 1;
                final int axis = (Integer) info.get(4);
                final TimePlotBase plot = plots.get(plotIndex);

                TimePlotSeries graph = new TimePlotSeries(name, axis);
                seriesIndexes.put(info, plot.getNumberOfSeries());
                plot.addSeries(graph);

                DeviceTask task = new DeviceTask();
                task.row = i + 1;
                task.info = info;
                Thread t = new Thread(task, "Strip chart: " + name);
                t.setDaemon(true);
                tasks.add(task);
                t.start();
            }
        }
    }

    Provider provider;

    String openProvider() throws IOException {
        closeProvider();
        if (textFileName.getText().trim().isEmpty() || (Context.getInstance() == null)) {
            return null;
        }
        String dataFileName = textFileName.getText().trim();
        provider = (comboFormat.getSelectedIndex() == 1) ? new ProviderText() : new ProviderHDF5();
        dataFileName = Context.getInstance().getSetup().expandPath(dataFileName.replace("{name}", "StripChart"));
        File root = new File(provider.getRootFileName(dataFileName));
        Logger.getLogger(StripChart.class.getName()).info("Opening output: " + root.getPath());
        provider.openOutput(root);
        return provider.getRootFileName(dataFileName);
    }

    void closeProvider() {
        if (provider != null) {
            Logger.getLogger(StripChart.class.getName()).info("Closing output");
            try {
                provider.closeOutput();
            } catch (IOException ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.SEVERE, null, ex);
            }
            provider = null;
        }
    }

    String createDataset(String name, Type type, int index) {
        String id = null;
        if (provider != null) {
            id = index + "-" + name;
            if (type == Type.CamServer) {
                //String[] tokens = name.split(" ");
                //if (tokens.length>1){
                //    id = tokens[1];
                //} 
                if (id.contains("//")) {
                    id = id.substring(id.indexOf("//") + 2); //Remove 'http://' or 'https://'
                }
            }
            if (!provider.isPacked()) { //Filenames don't support ':'
                id = id.replace(":", "_");
            }
            id = id.replace("/", "_");
            try {
                provider.createDataset(id, new String[]{"Timestamp", "Value"}, new Class[]{Long.class, Double.class}, new int[]{0, 0});
                provider.setAttribute(id, "Type", type.toString(), String.class, false);
                provider.setAttribute(id, "Name", name, String.class, false);
                provider.setAttribute(id, "Index", Integer.valueOf(index), Integer.class, false);
                provider.flush();
            } catch (IOException ex) {
                id = null;
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        return id;
    }

    void addDataset(String id, Long timestamp, Object value, int index) {
        if ((id != null) && (provider != null)) {
            try {
                provider.setItem(id, new Object[]{timestamp, value}, Object[].class, index);
                if (ckFlush.isSelected()) {
                    provider.flush();
                }
            } catch (IOException ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
            }
        }

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
        Double current;
        final Object persistLock = new Object();

        void add(Device device, Object value, Long timestamp, TimePlotBase plot, int seriesIndex) {
            try {
                TimestampedValue tValue = device.takeTimestamped();
                if (tValue != null) {
                    if (timestamp == null) {
                        //To Make sure  value/timestamp pair is correct                
                        value = tValue.getValue();
                        timestamp = tValue.getTimestamp();
                    } else {
                        if (timestamp <= tValue.getTimestamp()) {
                            //Won't plot dragging values lower than current
                            return;
                        }
                    }
                }
                if ((value == null) || (value instanceof Number)) {
                    Double d = (value == null) ? Double.NaN : ((Number) value).doubleValue();
                    long time = (timestamp == null) ? System.currentTimeMillis() : timestamp;

                    appendTimestamps.put(device, System.currentTimeMillis());

                    //plot.add(seriesIndex, time, d);
                    //Invoking event thread to prevent https://sourceforge.net/p/jfreechart/bugs/1009/ (JFreeChart is not thread safe)
                    chartElementProducer.post(new ChartElement(plot, seriesIndex, time, d));

                    if (persisting) {
                        if (d == null) {
                            d = Double.NaN;
                        }
                        synchronized (persistLock) {
                            if (!d.equals(current)) {
                                addDataset(id, time, d, index);
                                index++;
                                current = d;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
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

            if (persisting) {
                id = createDataset(name, type, row);
            }

            Device dev = null;
            final DeviceListener deviceListener = new DeviceAdapter() {
                @Override
                public void onValueChanged(Device device, Object value, Object former) {
                    add(device, value, null, plot, seriesIndex);
                }
            };
            try {
                switch (type) {
                    case Channel:
                        int polling = -1;
                        boolean timestamped = false;
                        int precision = -1;
                        if (name.contains(" ")) {
                            String[] tokens = name.split(" ");
                            name = tokens[0];
                            try {
                                polling = Integer.valueOf(tokens[1]);
                                timestamped = Boolean.valueOf(tokens[2]);
                                precision = Integer.valueOf(tokens[3]);
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
                            dev = Context.getInstance().getDevicePool().getByName(name, Device.class);
                        }
                        break;
                    case Stream:
                        int modulo = Scalar.DEFAULT_MODULO;
                        int offset = Scalar.DEFAULT_OFFSET;
                        if (name.contains(" ")) {
                            String[] tokens = name.split(" ");
                            name = tokens[0];
                            try {
                                modulo = Integer.valueOf(tokens[1]);
                                offset = Integer.valueOf(tokens[2]);
                            } catch (Exception ex) {
                            }
                        }
                        dev = stream.addScalar(name, name, modulo, offset);
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
                                    ch.psi.pshell.bs.Provider p = new ch.psi.pshell.bs.Provider(null, url, false, false);
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
                        stream.start(true);
                    } catch (Exception ex) {
                        Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
                    }
                } else if (type == Type.Channel) {
                    dev.request();
                }

                dragInterval = (Integer) spinnerDragInterval.getValue();
                while (started) {
                    int sleep_ms = (dragInterval > 0)
                            ? dragInterval
                            : //((dev.isPolled() && !dev.isMonitored()) ? dev.getPolling() + dragInterval: dragInterval):
                            0;
                    synchronized (lock) {
                        lock.wait(sleep_ms);
                    }
                    if (sleep_ms > 0) {
                        //Device time may not be the same as PC time
                        //long age = dev.getAge();
                        long now = System.currentTimeMillis();
                        Long appendTimestamp = appendTimestamps.get(dev);
                        Long age = (appendTimestamp == null) ? null : now - appendTimestamp;
                        if (((age == null) || (age >= sleep_ms))) {
                            Long devTimestamp = dev.getTimestamp();
                            Integer devAge = dev.getAge();
                            if ((devTimestamp != null) && (devAge != null)) {
                                now = devTimestamp + devAge;
                            }
                            add(dev, dev.take(), now, plot, seriesIndex);
                        }
                    }
                }
            } catch (InterruptedException ex) {
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
            } finally {
                if (dev != null) {
                    dev.removeListener(deviceListener);
                }
            }
        }

        void saveDataset() {
            if (!persisting) {
                String name = ((String) info.get(1)).trim();
                final Type type = Type.valueOf(info.get(2).toString());
                final int plotIndex = ((Integer) info.get(3)) - 1;
                final int axis = (Integer) info.get(4);
                final int seriesIndex = seriesIndexes.get(info);
                final TimePlotBase plot = plots.get(plotIndex);
                id = createDataset(name, type, row);
                int index = 0;

                Number current = null;
                for (TimestampedValue<Double> item : plot.getSeriestData(seriesIndex)) {
                    Number value = item.getValue();
                    if (value == null) {
                        value = Double.NaN;
                    }
                    if (!value.equals(current)) {
                        addDataset(id, item.getTimestamp(), value, index++);
                        current = value;
                    }
                }

            }
        }
    };

    final Object lock = new Object();
    volatile boolean started = false;

    void stop() {
        if (started) {
            Logger.getLogger(StripChart.class.getName()).info("Stop");
            started = false;
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
            closeProvider();
            seriesIndexes.clear();
            initializePlots();
            appendTimestamps.clear();
        }
    }

    @Override
    protected void onOpened() {
        if (MainFrame.isDark()) {
            //TODO: Repeating the model initialization as a workaround for the exception when spinner getting focus on Darcula LAF.
            spinnerDragInterval.setModel(new javax.swing.SpinnerNumberModel(1000, -1, 99999, 1));
        }
    }

    @Override
    protected void onClosed() {
        stop();
    }

    public static void create(File file, String config, File defaultFolder, boolean start) {
        java.awt.EventQueue.invokeLater(() -> {
            StripChart dialog = new StripChart(new javax.swing.JFrame(), true, defaultFolder);
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            try {
                if ((file != null) && (file.exists())) {
                    dialog.open(file);
                }
                if (config != null) {
                    dialog.open(config);
                }
                if (start) {
                    dialog.buttonStartStopActionPerformed(null);
                }
            } catch (IOException ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.SEVERE, null, ex);
            }
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    dialog.onClosed();
                    System.exit(0);
                }
            });
            SwingUtils.centerComponent(null, dialog);
            dialog.setVisible(true);
        });
    }

    void saveData() throws IOException {
        if ((started) && (!persisting)) {
            try {
                String file = openProvider();
                //String createDataset( String name, String type, int index){
                //void addDataset(String id, Long timestamp, Double value, int index){
                for (DeviceTask task : tasks) {
                    try {
                        task.saveDataset();
                    } catch (Exception ex) {
                        Logger.getLogger(StripChart.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
                SwingUtils.showMessage(this, "Strip Chart", "Success saving data to: \n" + file);
            } finally {
                closeProvider();
            }
        }
        //void openProvider() throws IOException{
        //void closeProvider(){
        //String createDataset( String name, String type, int index){
        //void addDataset(String id, Long timestamp, Double value, int index){

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabPane = new javax.swing.JTabbedPane();
        panelConfig = new javax.swing.JPanel();
        panelSeries = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSeries = new javax.swing.JTable();
        buttonDelete = new javax.swing.JButton();
        buttonUp = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        panelAxis = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableCharts = new javax.swing.JTable();
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
        ckFlush = new javax.swing.JCheckBox();
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
                "Enabled", "Name", "Type", "Plot", "Y Axis"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
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
                {"1", null, null, null, null, null},
                {"2", null, null, null, null, null},
                {"3", null, null, null, null, null},
                {"4", null, null, null, null, null},
                {"5", null, null, null, null, null}
            },
            new String [] {
                "Chart", "Y1min", "Y1max", "Y2min", "Y2max", "Duration(s)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
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

        comboFormat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "h5", "txt" }));

        ckFlush.setText("Flush");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ckPersistence)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textFileName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckFlush)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(ckFlush))
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        jLabel2.setText("Stream filter:");

        jLabel3.setText("Drag Interval(ms):");

        spinnerDragInterval.setModel(new javax.swing.SpinnerNumberModel(1000, -1, 99999, 1));

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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Colors"));

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
            .addGap(0, 16, Short.MAX_VALUE)
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
            .addGap(0, 16, Short.MAX_VALUE)
        );

        buttonDefaultColors.setText("Defaults");
        buttonDefaultColors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultColorsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelColorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelColorGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonDefaultColors)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelColorGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelColorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonDefaultColors, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 6, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDefaultColors, jLabel15, jLabel17});

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
            .addGap(0, 580, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 528, Short.MAX_VALUE)
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
            SwingUtils.showException(this, ex);
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
            SwingUtils.showException(this, ex);
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
            SwingUtils.showException(this, ex);
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
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void tabPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPaneStateChanged
        try {
            if (tabPane.getSelectedComponent() == panelPlots) {
                if (!started) {
                    start();
                }
                buttonPause.setVisible(plots.size() > 0);
            } else {
                //stop();
                buttonPause.setVisible(false);
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_tabPaneStateChanged

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
        try {
            clear();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
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
            SwingUtils.showException(this, ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_buttonStartStopActionPerformed

    private void panelColorBackgroundMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorBackgroundMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", backgroundColor);        
        if (c!=null){
            backgroundColor = c;
            panelColorBackground.setBackground(backgroundColor);
            if (started){
                for (TimePlotBase plot : plots){
                    plot.setPlotBackgroundColor(backgroundColor);
                }
            }            
        }
    }//GEN-LAST:event_panelColorBackgroundMouseClicked

    private void panelColorGridMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorGridMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", gridColor);        
        if (c!=null){
            gridColor = c;
            panelColorGrid.setBackground(gridColor);
            if (started){
                for (TimePlotBase plot : plots){
                    plot.setPlotGridColor(gridColor);
                }
            }            
        }
    }//GEN-LAST:event_panelColorGridMouseClicked

    private void buttonDefaultColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultColorsActionPerformed
            gridColor = backgroundColor = null;
            panelColorBackground.setBackground(null);
            panelColorGrid.setBackground(null);
            if (started){
                for (TimePlotBase plot : plots){
                    plot.setPlotGridColor(PlotBase.getGridColor());
                    plot.setPlotBackgroundColor(PlotBase.getPlotBackground());
                }
            }            
    }//GEN-LAST:event_buttonDefaultColorsActionPerformed

    /**
     */
    public static void main(String args[]) {
        MainFrame.setLookAndFeel(MainFrame.getNimbusLookAndFeel());
        String configPath = "./home/config";
        System.setProperty(Epics.PROPERTY_JCAE_CONFIG_FILE, Paths.get(configPath, "jcae.properties").toString());
        Epics.create();
        create(Paths.get(Sys.getUserHome(), "test." + FILE_EXTENSION).toFile(), null, null, false);
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
    private javax.swing.JCheckBox ckFlush;
    private javax.swing.JCheckBox ckPersistence;
    private javax.swing.JComboBox<String> comboFormat;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
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
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTable tableCharts;
    private javax.swing.JTable tableSeries;
    private javax.swing.JTextField textFileName;
    private javax.swing.JTextField textStreamFilter;
    // End of variables declaration//GEN-END:variables
}
