package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.ProviderCSV;
import ch.psi.pshell.data.ProviderText;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.plot.TimePlotJFree;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.pshell.plotter.Preferences;
import ch.psi.pshell.swing.ChannelSelector;
import ch.psi.pshell.swing.HistoryChart;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.utils.Arr;
import ch.psi.utils.Daqbuf;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreeCellEditor;

/**
 *
 */
public class DaqbufPanel extends StandardDialog {

    public static final String FILE_EXTENSION = "scd";

    Color defaultBackgroundColor = null;
    Color defaultGridColor = null;
    Font tickLabelFont = null;
    boolean pulse;

    final DefaultTableModel modelSeries;
    final DefaultTableModel modelCharts;

    final ArrayList<TimePlotBase> plots = new ArrayList<>();
    final HashMap<Device, Long> appendTimestamps = new HashMap<>();
    volatile boolean started = false;

    Color backgroundColor;
    Color gridColor;
    final Daqbuf daqbuf;
    
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
    public DaqbufPanel(Window parent, String url, boolean modal) {
        super(parent, modal);        
        initComponents();
        daqbuf = new Daqbuf(url);
        if (App.hasArgument("background_color")) {
            try {
                defaultBackgroundColor = Preferences.getColorFromString(App.getArgumentValue("background_color"));
                panelColorBackground.setBackground(defaultBackgroundColor);
                backgroundColor = defaultBackgroundColor;
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("grid_color")) {
            try {
                defaultGridColor = Preferences.getColorFromString(App.getArgumentValue("grid_color"));
                panelColorGrid.setBackground(defaultGridColor);
                gridColor = defaultGridColor;
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("tick_label_font")) {
            try {
                String[] tokens = App.getArgumentValue("tick_label_font").split(":");
                tickLabelFont = new Font(tokens[0], Font.PLAIN, Integer.valueOf(tokens[1]));
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        buttonQuery.setEnabled(false);
        textFileName.setEnabled(false);
        comboFormat.setEnabled(false);
        comboLayout.setEnabled(false);
        textFileName.setText((Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : "");
        comboFormat.setSelectedItem(getInitFormat());
        comboLayout.setSelectedItem(getInitLayout());
        setCancelledOnEscape(false);

        //pnGraphs.setLayout(new GridBagLayout());
        pnGraphs.setLayout(new BoxLayout(pnGraphs, BoxLayout.Y_AXIS));

        modelSeries = (DefaultTableModel) tableSeries.getModel();
        modelSeries.addTableModelListener(modelSeriesListener);
        modelCharts = (DefaultTableModel) tableCharts.getModel();
        modelCharts.addTableModelListener(modelChartsListener);

        initializeTable();
        clear();

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
        return panelSeries;
    }
    
    
    public void initializeTable() {
        //Fix bug of nimbus rendering Boolean in table
        ((JComponent) tableSeries.getDefaultRenderer(Boolean.class)).setOpaque(true);
        tableSeries.getColumnModel().getColumn(0).setResizable(true);

        TableColumn colEnabled = tableSeries.getColumnModel().getColumn(0);
        colEnabled.setPreferredWidth(60);

        TableColumn colName = tableSeries.getColumnModel().getColumn(1);
        JTextField textNameEditor = new JTextField();
        ChannelSelector selector = new ChannelSelector();
        selector.configure(ChannelSelector.Type.Daqbuf, null,  null, 1000);
        selector.setHistorySize(0);
        selector.setListMode(ChannelSelector.ListMode.Popup);
        
        
        colName.setPreferredWidth(320);
        colName.setCellEditor(new ChannelSelector.ChannelSelectorCellEditor(selector));    
        
        TableColumn colType = tableSeries.getColumnModel().getColumn(2);
        colType.setPreferredWidth(120);
        DefaultComboBoxModel modelType = new DefaultComboBoxModel();
        for (String backend: daqbuf.getBackends()) {
            modelType.addElement(backend);
        }        
        JComboBox comboType = new JComboBox();
        tableSeries.setRowHeight(Math.max(tableSeries.getRowHeight(), comboType.getPreferredSize().height - 3));
        comboType.setModel(modelType);
        DefaultCellEditor cellEditor = new DefaultCellEditor(comboType);
        cellEditor.setClickCountToStart(2);
        colType.setCellEditor(cellEditor);            
        

        TableColumn colPlot = tableSeries.getColumnModel().getColumn(3);
        colPlot.setPreferredWidth(60);
        JComboBox comboPlot = new JComboBox();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (int i = 1; i <= 5; i++) {
            model.addElement(i);
        }
        comboPlot.setModel(model);
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
                            color = JColorChooser.showDialog(DaqbufPanel.this, "Choose a Color - Click 'Cancel for default", color);
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
        boolean editing = true;
        int rows = modelSeries.getRowCount();
        int cur = tableSeries.getSelectedRow();
        buttonUp.setEnabled((rows > 0) && (cur > 0) && editing);
        buttonDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && editing);
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && editing);
        buttonInsert.setEnabled(editing);
        buttonQuery.setEnabled(modelSeries.getRowCount() > 0);
        textFileName.setEnabled(true);
        comboFormat.setEnabled(textFileName.isEnabled());
        comboLayout.setEnabled(textFileName.isEnabled());
    }

    boolean isSeriesTableRowEditable(int row, int column) {
        return true;
    }

    boolean isChartTableRowEditable(int row, int column) {
        return (column > 0);
    }


    public void clear() {
        Logger.getLogger(DaqbufPanel.class.getName()).info("Init");
        reset();
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
    }

    File file;
    
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
    
    String removeAlias(String str){
        str=str.trim();
        if (str.endsWith(">")) {
            int start = str.lastIndexOf('<');
            if (start>=0){
                str=str.substring(0, start);
            }
        }    
        return str;
    }
    
    String getChannelName(String str){
        str=removeAlias(str);
        if (str.contains(" ")) {
            String[] tokens = str.split(" ");
            str = tokens[0];
        }
        return str.trim();        
    }

    String[] getChannelArgs(String str){
        str=removeAlias(str);
        if (str.contains(" ")) {
            String[] tokens = str.split(" ");
            String[] args = Arr.remove(tokens, 0);
            args=Arr.removeEquals(args, "");
            return args;
        }
        return new String[0];        
    }

    String getChannelAlias(String str){
        str=str.trim();
        if (str.endsWith(">")) {
            int end = str.lastIndexOf('>');            
            int start = str.lastIndexOf('<');
            if (start>=0){
                String alias = str.substring(start + 1, end);
                if (!alias.isBlank()){
                    return alias;
                }
            }
        }
        return getChannelName(str);               
    }

    public void query() throws Exception {
        reset();

        if (modelSeries.getRowCount() == 0) {
            return;
        }
        int numPlots = 1;
        Vector vector = modelSeries.getDataVector();
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
        update();

        Vector[] rows = (Vector[]) vector.toArray(new Vector[0]);
        for (int i = 0; i < rows.length; i++) {
            Vector info = rows[i];
            if (info.get(0).equals(true)) {
                final String name = getChannelAlias(((String) info.get(1)).trim());
                final String backend = info.get(2).toString();
                final int plotIndex = ((Integer) info.get(3)) - 1;
                final int axis = (Integer) info.get(4);
                final TimePlotBase plot = plots.get(plotIndex);
                final Color color = Preferences.getColorFromString((String) info.get(5));

                TimePlotSeries graph = (color != null) ? new TimePlotSeries(name, color, axis) : new TimePlotSeries(name, axis);
                seriesIndexes.put(info, plot.getNumberOfSeries());
                timePlotSeries.add(graph);
                plot.addSeries(graph);
                
                //TODO
            }
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
        String backend = ((String) info.get(2)).trim();
        
        if (!comboFormat.getSelectedItem().equals("h5")) {
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


    final Object lock = new Object();
    
    void reset() {
        update();
        synchronized (lock) {
            lock.notifyAll();
        }
        seriesIndexes.clear();
        timePlotSeries.clear();
        initializePlots();
        appendTimestamps.clear();
    }

    @Override
    protected void onClosed() {
    }


    public static void create(String url, boolean modal) {
        java.awt.EventQueue.invokeLater(() -> {
            DaqbufPanel dialog = new DaqbufPanel(null, url, modal);
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    dialog.onClosed();
                    if (modal) {
                        System.exit(0);
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
        panelSeries = new javax.swing.JPanel();
        panelSerie = new javax.swing.JPanel();
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
        panelFile = new javax.swing.JPanel();
        buttonQuery = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        textFileName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        comboFormat = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        comboLayout = new javax.swing.JComboBox<>();
        buttonSaveData = new javax.swing.JButton();
        panelCharts = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableCharts = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isChartTableRowEditable(row, column);
            };
        };
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

        panelSeries.setName("panelSeries"); // NOI18N

        panelSerie.setBorder(javax.swing.BorderFactory.createTitledBorder("Series"));

        tableSeries.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Name", "Backend", "Plot", "Y Axis", "Color"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
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

        javax.swing.GroupLayout panelSerieLayout = new javax.swing.GroupLayout(panelSerie);
        panelSerie.setLayout(panelSerieLayout);
        panelSerieLayout.setHorizontalGroup(
            panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSerieLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(panelSerieLayout.createSequentialGroup()
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

        panelSerieLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        panelSerieLayout.setVerticalGroup(
            panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSerieLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addGroup(panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonDown)
                    .addComponent(buttonUp))
                .addContainerGap())
        );

        panelSerieLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        buttonQuery.setText("Query");
        buttonQuery.setPreferredSize(new java.awt.Dimension(89, 23));
        buttonQuery.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonQueryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFileLayout = new javax.swing.GroupLayout(panelFile);
        panelFile.setLayout(panelFileLayout);
        panelFileLayout.setHorizontalGroup(
            panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFileLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonQuery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelFileLayout.setVerticalGroup(
            panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFileLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(buttonQuery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Persistence"));

        jLabel1.setText("Format:");

        comboFormat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "h5", "txt", "csv" }));
        comboFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboFormatActionPerformed(evt);
            }
        });

        jLabel5.setText("Layout:");

        comboLayout.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "default", "table", "sf" }));
        comboLayout.setSelectedIndex(1);

        buttonSaveData.setText("Save");
        buttonSaveData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveDataActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonSaveData)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textFileName, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
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
                    .addComponent(textFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(comboFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSaveData))
                .addContainerGap())
        );

        javax.swing.GroupLayout panelSeriesLayout = new javax.swing.GroupLayout(panelSeries);
        panelSeries.setLayout(panelSeriesLayout);
        panelSeriesLayout.setHorizontalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelSerie, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelSeriesLayout.setVerticalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSeriesLayout.createSequentialGroup()
                .addComponent(panelSerie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 177, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabPane.addTab("Series", panelSeries);

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonDefaultColors)
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
                    .addComponent(buttonDefaultColors, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDefaultColors, jLabel15, jLabel17, panelColorBackground, panelColorGrid});

        javax.swing.GroupLayout panelChartsLayout = new javax.swing.GroupLayout(panelCharts);
        panelCharts.setLayout(panelChartsLayout);
        panelChartsLayout.setHorizontalGroup(
            panelChartsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelChartsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelChartsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelChartsLayout.setVerticalGroup(
            panelChartsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelChartsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(182, Short.MAX_VALUE))
        );

        tabPane.addTab("Charts", panelCharts);

        panelPlots.setName("panelPlots"); // NOI18N
        panelPlots.setLayout(new java.awt.BorderLayout());

        scrollPane.setPreferredSize(new java.awt.Dimension(356, 303));

        pnGraphs.setPreferredSize(new java.awt.Dimension(354, 301));

        javax.swing.GroupLayout pnGraphsLayout = new javax.swing.GroupLayout(pnGraphs);
        pnGraphs.setLayout(pnGraphsLayout);
        pnGraphsLayout.setHorizontalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 583, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 469, Short.MAX_VALUE)
        );

        scrollPane.setViewportView(pnGraphs);

        panelPlots.add(scrollPane, java.awt.BorderLayout.CENTER);

        tabPane.addTab("Plots", panelPlots);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabPane)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tabPane)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
        Object[] data = new Object[]{Boolean.TRUE, "", Daqbuf.getDefaultBackend(), 1, 1};
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

    private void tabPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPaneStateChanged
        try {
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_tabPaneStateChanged

    private void tableSeriesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSeriesMouseReleased
        update();
    }//GEN-LAST:event_tableSeriesMouseReleased

    private void tableSeriesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableSeriesKeyReleased
        update();
    }//GEN-LAST:event_tableSeriesKeyReleased

    private void buttonQueryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonQueryActionPerformed
        try {
            if (modelSeries.getRowCount() > 0) {
                query();
                tabPane.setSelectedComponent(panelPlots);
            }
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_buttonQueryActionPerformed

    private void panelColorBackgroundMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorBackgroundMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", backgroundColor);
        if (c != null) {
            backgroundColor = c;
            panelColorBackground.setBackground(backgroundColor);
            for (TimePlotBase plot : plots) {
                plot.setPlotBackgroundColor(backgroundColor);
            }
        }
    }//GEN-LAST:event_panelColorBackgroundMouseClicked

    private void panelColorGridMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorGridMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", gridColor);
        if (c != null) {
            gridColor = c;
            panelColorGrid.setBackground(gridColor);
            for (TimePlotBase plot : plots) {
                plot.setPlotGridColor(gridColor);
            }
        }
    }//GEN-LAST:event_panelColorGridMouseClicked

    private void buttonDefaultColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultColorsActionPerformed
        backgroundColor = defaultBackgroundColor;
        gridColor = defaultGridColor;
        panelColorBackground.setBackground(backgroundColor);
        panelColorGrid.setBackground(gridColor);
        for (TimePlotBase plot : plots) {
            plot.setPlotGridColor(PlotBase.getGridColor());
            plot.setPlotBackgroundColor(PlotBase.getPlotBackground());
        }
    }//GEN-LAST:event_buttonDefaultColorsActionPerformed

    private void buttonSaveDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveDataActionPerformed
        try {
            saveData();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveDataActionPerformed

    private void comboFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboFormatActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboFormatActionPerformed

    /**
     */
    public static void main(String args[]) {
        App.init(args);
        create(null, true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDefaultColors;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonQuery;
    private javax.swing.JButton buttonSaveData;
    private javax.swing.JButton buttonUp;
    private javax.swing.JComboBox<String> comboFormat;
    private javax.swing.JComboBox<String> comboLayout;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel panelCharts;
    private javax.swing.JPanel panelColorBackground;
    private javax.swing.JPanel panelColorGrid;
    private javax.swing.JPanel panelFile;
    private javax.swing.JPanel panelPlots;
    private javax.swing.JPanel panelSerie;
    private javax.swing.JPanel panelSeries;
    private javax.swing.JPanel pnGraphs;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTable tableCharts;
    private javax.swing.JTable tableSeries;
    private javax.swing.JTextField textFileName;
    // End of variables declaration//GEN-END:variables
}
