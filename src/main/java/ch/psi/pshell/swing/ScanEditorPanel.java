package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.swing.StripChart.Type;
import ch.psi.pshell.ui.Processor;
import ch.psi.utils.Arr;
import ch.psi.utils.Str;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class ScanEditorPanel extends MonitoredPanel implements Processor {

    static final int COLUMN_TYPE = 0;
    static final int COLUMN_PLOT = 4;

    boolean changed;
    boolean running;
    String fileName;

    final DefaultTableModel modelPositioners;
    final DefaultTableModel modelSensors;
    final DefaultTableModel modelVector;

    enum PlotOption {
        Enabled,
        Disabled,
        Domain,
        Line
    }

    boolean updatingDomain;

    /**
     * Creates new form ScanEditorPanel
     */
    public ScanEditorPanel() {
        initComponents();
        modelPositioners = (DefaultTableModel) tablePositioners.getModel();
        modelSensors = (DefaultTableModel) tableSensors.getModel();
        modelVector = (DefaultTableModel) tableVector.getModel();
        SwingUtils.setupTableClipboardTransfer(tableVector);

        setupColumns();

        TableModelListener tableChangeListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                onControlChanged();
                update();
            }
        };

        TableModelListener sensorsTableChangeListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {

                if (e.getColumn() == COLUMN_PLOT) {
                    int row = e.getFirstRow();
                    if ((PlotOption) modelSensors.getValueAt(row, COLUMN_PLOT) == PlotOption.Domain) {
                        if (!updatingDomain) {
                            updatingDomain = true;
                            try {
                                setSensorDomain(row);
                            } finally {
                                updatingDomain = false;
                            }
                        }
                    }
                }
                tableChangeListener.tableChanged(e);
            }
        };

        modelPositioners.addTableModelListener(tableChangeListener);
        modelSensors.addTableModelListener(sensorsTableChangeListener);
        //modelParameters.addTableModelListener(tableChangeListener);
        update();
    }

    void setSensorDomain(int row) {
        try {
            if (row >= 0) {
                modelSensors.setValueAt(PlotOption.Domain, row, COLUMN_PLOT);
                comboDomain.setSelectedIndex(3 + row);
            }
            for (int i = 0; i < modelSensors.getRowCount(); i++) {
                if ((row != i) && ((PlotOption) modelSensors.getValueAt(i, COLUMN_PLOT) == PlotOption.Domain)) {
                    modelSensors.setValueAt(PlotOption.Enabled, i, COLUMN_PLOT);
                }
            }
        } catch (Exception ex) {
        }
    }

    void setupColumns() {
        SwingUtils.setEnumTableColum(tableSensors, COLUMN_TYPE, Type.class);
        SwingUtils.setEnumTableColum(tablePositioners, COLUMN_TYPE, Type.class);
        JComboBox comboType = ((JComboBox) ((DefaultCellEditor) tablePositioners.getColumnModel().getColumn(COLUMN_TYPE).getCellEditor()).getComponent());
        for (Type type : new Type[]{Type.Stream, Type.CamServer}) {
            comboType.removeItem(type);
        }
        SwingUtils.setEnumTableColum(tableSensors, COLUMN_PLOT, PlotOption.class);
        JComboBox comboPlot = ((JComboBox) ((DefaultCellEditor) tableSensors.getColumnModel().getColumn(COLUMN_PLOT).getCellEditor()).getComponent());
        ((JLabel) comboPlot.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
        ((JTextField) comboPlot.getEditor().getEditorComponent()).setHorizontalAlignment(JLabel.RIGHT);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        tableSensors.getColumnModel().getColumn(COLUMN_PLOT).setCellRenderer(renderer);

        tablePositioners.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablePositioners.getColumnModel().getColumn(1).setPreferredWidth(214);
        tablePositioners.getColumnModel().getColumn(2).setPreferredWidth(60);
        tablePositioners.getColumnModel().getColumn(3).setPreferredWidth(60);
        tablePositioners.getColumnModel().getColumn(4).setPreferredWidth(60);
        tableSensors.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableSensors.getColumnModel().getColumn(1).setPreferredWidth(214);
        tableSensors.getColumnModel().getColumn(2).setPreferredWidth(60);
        tableSensors.getColumnModel().getColumn(3).setPreferredWidth(60);
        tableSensors.getColumnModel().getColumn(4).setPreferredWidth(60);
    }

    void onControlChanged() {
        changed = true;
        updateCommand();
    }

    //Processor interface
    @Override
    public String getType() {
        return "Scan Editor";
    }

    @Override
    public String getDescription() {
        return "Scan configuration file  (*.scan)";
    }

    @Override
    public String[] getExtensions() {
        return new String[]{"scan"};
    }

    @Override
    public void abort() throws InterruptedException {
        Context.getInstance().abort();
    }

    @Override
    public String getHomePath() {
        return Context.getInstance().getSetup().getScriptPath();
    }

    Object[][] toObjectArray(List list) {
        int rows = list.size();
        int cols = (list.size() == 0) ? 0 : ((List) list.get(0)).size();
        Object[][] ret = new Object[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                ret[i][j] = ((List) list.get(i)).get(j);
            }
        }
        //Set Type type back to Type
        if (cols > 2) {
            for (int i = 0; i < rows; i++) {
                ret[i][0] = Type.valueOf((String) ret[i][0]);
            }
        }
        return ret;
    }

    @Override
    public void open(String fileName) throws IOException {
        String json = new String(Files.readAllBytes(new File(fileName).toPath()));
        List state = (List) JsonSerializer.decode(json, List.class);
        comboType.setSelectedItem(state.get(0));
        modelPositioners.setDataVector(toObjectArray((List) state.get(1)), SwingUtils.getTableColumnNames(tablePositioners));
        modelSensors.setDataVector(toObjectArray((List) state.get(2)), SwingUtils.getTableColumnNames(tableSensors));
        if (getScanCommand().equals("vscan")) {
            ArrayList<String> columns = new ArrayList<>();
            for (int i = 0; i < modelPositioners.getRowCount(); i++) {
                columns.add(String.valueOf(modelPositioners.getValueAt(i, 1)));
            }
            modelVector.setDataVector(toObjectArray((List) state.get(4)), columns.toArray());
        }
        textTitle.setText(String.valueOf(state.get(5)));
        spinnerPasses.setValue(state.get(6));
        spinnerTime.setValue(state.get(7));
        spinnerLatency.setValue(state.get(8));
        checkRelative.setSelected((Boolean) state.get(9));
        checkZigzag.setSelected((Boolean) state.get(10));
        checkSave.setSelected((Boolean) state.get(11));
        checkDisplay.setSelected((Boolean) state.get(12));
        
        textFile.setText((String) state.get(13));
        textTag.setText((String) state.get(14));
        comboLayout.setSelectedItem(String.valueOf(state.get(15)).trim());
        comboProvider.setSelectedItem(String.valueOf(state.get(16)).trim());
        Integer range = ((Number) state.get(17)).intValue();
        buttonGrouPlotRange.setSelected((range == 2) ? radioManual.getModel() : ((range == 1) ? radioAuto.getModel() : radioScan.getModel()), true);
        if (radioManual.isSelected() && (state.get(18)!=null) && (state.get(19)!=null)) {
            spinnerRangeFrom.setValue(((Number) state.get(18)).doubleValue());
            spinnerRangeTo.setValue(((Number) state.get(19)).doubleValue());
        }
        comboDomain.setSelectedItem(String.valueOf(state.get(20)).trim());
                 
        setupColumns();
        this.fileName = fileName;
        changed = false;
        updateCommand();
        update();
    }

    @Override
    public void save() throws IOException {
        if (fileName == null) {
            JFileChooser chooser = new JFileChooser(getHomePath());
            int rVal = chooser.showSaveDialog(this);
            if (rVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            fileName = chooser.getSelectedFile().getAbsolutePath();
        }
        saveAs(fileName);
    }

    @Override
    public void saveAs(String fileName) throws IOException {
        if (!getScanCommand().equals("vscan")) {
            modelVector.setColumnCount(0);
            modelVector.setRowCount(0);
        }

        List state = new ArrayList();
        state.add(comboType.getSelectedItem());
        state.add(modelPositioners.getDataVector());
        state.add(modelSensors.getDataVector());
        state.add(new Vector()); //modelParameters.gettDataVector());
        state.add(modelVector.getDataVector());
        state.add(textTitle.getText());
        state.add(spinnerPasses.getValue());
        state.add(spinnerTime.getValue());
        state.add(spinnerLatency.getValue());
        state.add(checkRelative.isSelected());
        state.add(checkZigzag.isSelected());
        state.add(checkSave.isSelected());
        state.add(checkDisplay.isSelected());
        state.add(textFile.getText().trim());
        state.add(textTag.getText().trim());
        state.add(comboLayout.getSelectedItem());
        state.add(comboProvider.getSelectedItem());
        state.add(radioManual.isSelected() ? 2 : (radioAuto.isSelected() ? 1 : 0));
        state.add(radioManual.isSelected() ? spinnerRangeFrom.getValue() : null);
        state.add(radioManual.isSelected() ? spinnerRangeTo.getValue() : null);
        state.add(comboDomain.getSelectedItem());
        JsonSerializer.encode(state, changed);
        String json = JsonSerializer.encode(state, true);
        Files.write(new File(fileName).toPath(), json.getBytes());
        this.fileName = fileName;
        changed = false;
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean createMenuNew() {
        return true;
    }

    @Override
    public void execute() throws Exception {
        spinnerPasses.commitEdit();
        spinnerTime.commitEdit();
        spinnerLatency.commitEdit();
        updateCommand();
        if (textCommand.getForeground().equals(Color.RED)) {
            throw new Exception("Error in scan definition");
        }
        String command = getCommand();
        if (command.isEmpty()) {
            throw new Exception("No scan defined");
        }
        Context.getInstance().evalLineAsync(command).handle((ok, ex) -> {
            if ((ex != null) && (!Context.getInstance().isAborted())) {
                SwingUtils.showException(this, (Exception) ex);
            }
            running = false;
            SwingUtilities.invokeLater(() -> {
                update();
            });
            return ok;
        });

        running = true;
        update();
    }

    public String getCommand() {
        return textCommand.getText();
    }

    void updateCommand() {
        try {
            boolean hasPositioners = hasPositioners();
            String scan = getScanCommand();
            List<String> pos = hasPositioners ? getPositioners() : null;
            List<String> sensors = getSensors();
            List<String> pars = getParameters();
            StringBuilder cmd = new StringBuilder();
            cmd.append(scan).append("(");
            if (hasPositioners) {
                cmd.append("[").append(String.join(", ", pos)).append("], ");
            }
            cmd.append("[").append(String.join(", ", sensors)).append("], ");
            if (hasRange()) {
                cmd.append("[").append(String.join(", ", getStart())).append("], ");
                cmd.append("[").append(String.join(", ", getStop())).append("], ");
                cmd.append("[").append(String.join(", ", getStep())).append("], ");
            }
            cmd.append(String.join(", ", getParameters()));
            cmd.append(")");
            textCommand.setForeground(Shell.STDOUT_COLOR);
            textCommand.setText(cmd.toString());
        } catch (Exception ex) {
            textCommand.setForeground(Color.RED);
            textCommand.setText(ex.getMessage());
        }
    }

    protected void update() {
        String cmd = getScanCommand();
        boolean hasPositioners = hasPositioners();
        boolean hasTime = hasTime();
        boolean enabled = (!running) && hasPositioners;
        int rows = modelPositioners.getRowCount();
        int cur = tablePositioners.getSelectedRow();
        buttonPosUp.setEnabled((rows > 0) && (cur > 0) && enabled);
        buttonPosDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && enabled);
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && enabled);
        buttonPosInsert.setEnabled(enabled);
        tablePositioners.setEnabled(enabled);

        enabled = !running;
        rows = modelSensors.getRowCount();
        cur = tableSensors.getSelectedRow();
        buttonSenUp.setEnabled((rows > 0) && (cur > 0) && enabled);
        buttonSenDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && enabled);
        buttonSenDelete.setEnabled((rows > 0) && (cur >= 0) && enabled);
        buttonSenInsert.setEnabled(enabled);
        tableSensors.setEnabled(enabled);

        enabled = !running;
        comboType.setEnabled(enabled);
        textTitle.setEnabled(enabled);
        spinnerPasses.setEnabled(enabled);
        spinnerTime.setEnabled(enabled && (hasTime || cmd.equals("mscan")));
        spinnerLatency.setEnabled(enabled && !cmd.equals("mscan"));
        checkRelative.setEnabled(enabled && hasPasses());
        checkZigzag.setEnabled(enabled && hasPasses());
        checkSave.setEnabled(enabled);
        checkDisplay.setEnabled(enabled);
        textFile.setEnabled(enabled && checkSave.isSelected());
        textTag.setEnabled(enabled && checkSave.isSelected());
        comboLayout.setEnabled(enabled && checkSave.isSelected());
        comboProvider.setEnabled(enabled && checkSave.isSelected());
        radioManual.setEnabled(enabled);
        radioAuto.setEnabled(enabled);
        radioScan.setEnabled(enabled);
        spinnerRangeFrom.setEnabled(enabled && radioManual.isSelected());
        spinnerRangeTo.setEnabled(enabled && radioManual.isSelected());
        spinnerRangeFrom.setVisible(radioManual.isSelected());
        labelRangeFrom.setVisible(spinnerRangeFrom.isVisible());
        spinnerRangeTo.setVisible(radioManual.isSelected());
        labelRangeTo.setVisible(spinnerRangeTo.isVisible());
        labelPasses.setText(hasPasses() ? "Passes:" : "Records:");
        comboDomain.setEnabled(enabled);

        switch (cmd) {
            case "tscan":
                labelLatency.setText("Interval (s):");
                break;
            default:
                labelLatency.setText("Latency (s):");
                break;
        }

        if (cmd.equals("vscan")) {
            panelVector.setVisible(true);
            if (modelVector.getColumnCount() != modelPositioners.getRowCount()) {
                modelVector.setColumnCount(modelPositioners.getRowCount());
                modelVector.setRowCount(1000);
            }
            for (int i = 0; i < modelPositioners.getRowCount(); i++) {
                tableVector.getColumnModel().getColumn(i).setHeaderValue(modelPositioners.getValueAt(i, 1));
            }
        } else {
            panelVector.setVisible(false);
        }
        if (comboDomain.getModel().getSize() != (3 + modelSensors.getRowCount())) {
            DefaultComboBoxModel model = ((DefaultComboBoxModel) comboDomain.getModel());
            for (int i = model.getSize() - 1; i > 2; i--) {
                model.removeElementAt(i);
            }
            for (int i = 0; i < modelSensors.getRowCount(); i++) {
                model.addElement("Sensor " + (i + 1));
            }
        }
        updateCommand();
    }

    String getScanCommand() {
        switch ((String) comboType.getSelectedItem()) {
            case "Linear":
                return "lscan";
            case "Multidimensional":
                return "ascan";
            case "Vector":
                return "vscan";
            case "Continuous":
                return "cscan";
            case "Time series":
                return "tscan";
            case "Change event series":
                return "mscan";
            case "Beam synchronous series ":
                return "bscan";
            default:
                throw new RuntimeException("Invalid scan type");
        }
    }

    boolean hasPositioners() {
        return !Arr.containsEqual(new String[]{"tscan"}, getScanCommand());
    }

    boolean hasTime() {
        return Arr.containsEqual(new String[]{"cscan"}, getScanCommand());
    }

    boolean hasPasses() {
        return Arr.containsEqual(new String[]{"lscan", "ascan", "vscan", "cscan"}, getScanCommand());
    }

    boolean hasRange() {
        return !Arr.containsEqual(new String[]{"tscan", "mscan", "bscan", "vscan"}, getScanCommand());
    }

    List<String> getPositioners() {
        List<String> ret = new ArrayList<>();
        if (hasPositioners()) {
            for (int i = 0; i < modelPositioners.getRowCount(); i++) {
                Type type = (Type) modelPositioners.getValueAt(i, 0);
                String name = modelPositioners.getValueAt(i, 1).toString().trim();
                switch (type) {
                    case Channel:
                        ret.add("'ca://" + name + "'");
                        break;
                    case Device:
                        ret.add(name);
                        break;
                    default:
                        throw new RuntimeException("Invalid positioner type");
                }
            }
        }
        return ret;
    }

    List<String> getStart() {
        List<String> ret = new ArrayList<>();
        if (hasPositioners()) {
            for (int i = 0; i < modelPositioners.getRowCount(); i++) {
                ret.add(modelPositioners.getValueAt(i, 2).toString());
            }
        }
        return ret;
    }

    List<String> getStop() {
        List<String> ret = new ArrayList<>();
        if (hasPositioners()) {
            for (int i = 0; i < modelPositioners.getRowCount(); i++) {
                ret.add(modelPositioners.getValueAt(i, 3).toString());
            }
        }
        return ret;
    }

    List<String> getStep() {
        List<String> ret = new ArrayList<>();
        if (hasPositioners()) {
            for (int i = 0; i < modelPositioners.getRowCount(); i++) {
                ret.add(modelPositioners.getValueAt(i, 4).toString());
            }
        }
        return ret;
    }

    List<String> getSensors() {
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < modelSensors.getRowCount(); i++) {
            Type type = (Type) modelSensors.getValueAt(i, 0);
            String name = modelSensors.getValueAt(i, 1).toString().trim();
            String sensor = null;
            switch (type) {
                case Channel:
                    sensor = "'ca://" + name + "'";
                    break;
                case Device:
                    sensor = name;
                    break;
                case Stream:
                    sensor = "'bs://" + name + "'";
                    break;
                case CamServer:
                    sensor = "'cs://" + name + "'";
                    break;
            }
            if (sensor != null) {
                Number samples = ((Number) modelSensors.getValueAt(i, 2));
                if ((samples == null) || samples.intValue() < 0) {
                    samples = 1;
                }
                Number interval = ((Number) modelSensors.getValueAt(i, 3));
                if (interval == null) {
                    interval = 0.0;
                }
                if (samples.intValue() > 1) {
                    sensor = "create_averager(" + sensor + ", " + samples.intValue() + ", " + interval.doubleValue() + ")";
                }
                ret.add(sensor);
            }
        }
        return ret;
    }

    String getBoolValue(boolean value) {
        String ret = String.valueOf(value);
        if (Context.getInstance().getSetup().getScriptType() == ScriptType.py) {
            return Str.capitalizeFirst(ret);
        }
        return ret;
    }

    String getNullValue(boolean value) {
        String ret = String.valueOf(value);
        if (Context.getInstance().getSetup().getScriptType() == ScriptType.py) {
            return "None";
        }
        return "null";
    }

    String getParName(String par) {
        if (Context.getInstance().getSetup().getScriptType() == ScriptType.js) {
            return "";
        }
        return par + "=";
    }

    List<String> getParameters() {

        boolean addSpacersCommas = (Context.getInstance().getSetup().getScriptType() == ScriptType.js);
        List<String> ret = new ArrayList<>();
        if (getScanCommand() == "tscan") {
            ret.add(getParName("points") + spinnerPasses.getValue());
            ret.add(getParName("interval") + spinnerLatency.getValue());
        } else if (getScanCommand() == "mscan") {
            ret.add(getParName("points") + ((((Number) spinnerPasses.getValue()).doubleValue() == 0) ? -1 : spinnerPasses.getValue()));
            ret.add(getParName("timeout") + ((((Number) spinnerTime.getValue()).doubleValue() == 0) ? -1 : spinnerTime.getValue()));
        } else if (getScanCommand() == "vscan") {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < modelVector.getRowCount(); i++) {
                if ((modelVector.getValueAt(i, 0) == null) || (modelVector.getValueAt(i, 0).toString().trim().isEmpty())) {
                    break;
                }
                sb.append("[");
                for (int j = 0; j < modelVector.getColumnCount(); j++) {
                    sb.append(modelVector.getValueAt(i, j)).append(", ");
                }
                sb.append("], ");
            }
            sb.append("]");
            ret.add(getParName("vector") + sb.toString());
            ret.add(getParName("line") + getBoolValue(true));
        } else {
            ret.add(getParName("latency") + spinnerLatency.getValue());
            if (hasTime()) {
                ret.add(getParName("time") + spinnerTime.getValue());
            }
            ret.add(getParName("relative") + getBoolValue(checkRelative.isSelected()));
            ret.add(getParName("passes") + spinnerPasses.getValue());
            ret.add(getParName("zigzag") + getBoolValue(checkZigzag.isSelected()));
            if (addSpacersCommas) {
                ret.add("");
                ret.add(""); //Callbaks
            }
        }
        if (!textTitle.getText().trim().isEmpty()) {
            ret.add(getParName("title") + "'" + textTitle.getText() + "'");
        }
        if (Context.getInstance().getSetup().getScriptType() == ScriptType.py) {
            if (!checkSave.isSelected()) {
                ret.add(getParName("persist") + getBoolValue(false));
            }
            if (!checkDisplay.isSelected()) {
                ret.add(getParName("accumulate") + getBoolValue(false));
                ret.add(getParName("plot_disabled") + getBoolValue(true));
                ret.add(getParName("table_disabled") + getBoolValue(true));
            }

            if (!textFile.getText().trim().isEmpty()) {
                if (textFile.getText().contains(File.separator) || textFile.getText().contains("/") || textFile.getText().contains(".")) {
                    ret.add(getParName("path") + "'" + textFile.getText() + "'");
                } else {
                    ret.add(getParName("name") + "'" + textFile.getText() + "'");
                }
            }
            if (!textTag.getText().trim().isEmpty()) {
                ret.add(getParName("tag") + "'" + textTag.getText() + "'");
            }
            if ((!comboLayout.getSelectedItem().toString().trim().isEmpty())
                    && !comboLayout.getSelectedItem().equals(comboLayout.getModel().getElementAt(0))) {
                ret.add(getParName("layout") + "'" + comboLayout.getSelectedItem() + "'");
            }
            if ((!comboProvider.getSelectedItem().toString().trim().isEmpty())
                    && !comboProvider.getSelectedItem().equals(comboProvider.getModel().getElementAt(0))) {
                ret.add(getParName("provider") + "'" + comboProvider.getSelectedItem() + "'");
            }
            if (radioAuto.isSelected()) {
                ret.add(getParName("auto_range") + getBoolValue(true));
            }
            if (radioManual.isSelected()) {
                ret.add(getParName("manual_range") + "[" + spinnerRangeFrom.getValue() + ", " + spinnerRangeTo.getValue() + "]");
            }

            List<String> sensorNames = new ArrayList<>();
            List<String> enabledPlots = new ArrayList<>();
            List<String> linePlots = new ArrayList<>();
            String domain = null;
            for (int i = 0; i < modelSensors.getRowCount(); i++) {
                String device = getSensorName(i);
                sensorNames.add(device);

                Object val = modelSensors.getValueAt(i, COLUMN_PLOT);
                PlotOption option = (val == null) ? null : PlotOption.valueOf(val.toString());
                if (option == null) {
                    option = PlotOption.Enabled;
                }
                if (option != PlotOption.Disabled) {
                    enabledPlots.add("'" + device + "'");
                    if (option == PlotOption.Domain) {
                        domain = device;
                    } else if (option == PlotOption.Line) {
                        linePlots.add(device);
                    }
                }
            }
            if (domain == null) {
                if (comboDomain.getSelectedIndex() == 1) {
                    domain = "Index";
                } else if (comboDomain.getSelectedIndex() == 2) {
                    domain = "Time";
                }
            }
            if (domain != null) {
                ret.add(getParName("domain_axis") + "'" + domain + "'");
            }
            if (linePlots.size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                for (String plot : linePlots) {
                    sb.append("'" + plot + "'" + ":1, ");
                }
                sb.append("}");
                ret.add(getParName("plot_types") + sb.toString());
            }

            if (enabledPlots.size() < sensorNames.size()) {
                ret.add(getParName("enabled_plots") + "[" + String.join(", ", enabledPlots) + "]");
            }
        }
        return ret;
    }

    String getSensorName(int row) {
        if ((row < 0) || (row > modelSensors.getRowCount() - 1)) {
            return null;
        }
        String ret = String.valueOf(modelSensors.getValueAt(row, 1));
        if (ret.contains("?")) {
            ret = ret.split("\\?")[0];
        }
        Integer samples = ((Integer) modelSensors.getValueAt(row, 2));
        if ((samples != null) && (samples > 1)) {
            ret = ret + " averager";
        }
        return ret;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBox1 = new javax.swing.JCheckBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        buttonGrouPlotRange = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textCommand = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        comboType = new javax.swing.JComboBox<>();
        panelSensors = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableSensors = new javax.swing.JTable();
        buttonSenDelete = new javax.swing.JButton();
        buttonSenUp = new javax.swing.JButton();
        buttonSenInsert = new javax.swing.JButton();
        buttonSenDown = new javax.swing.JButton();
        panelParameters = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        comboLayout = new javax.swing.JComboBox<>();
        comboProvider = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        textFile = new javax.swing.JTextField();
        textTag = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        radioScan = new javax.swing.JRadioButton();
        radioAuto = new javax.swing.JRadioButton();
        radioManual = new javax.swing.JRadioButton();
        spinnerRangeFrom = new javax.swing.JSpinner();
        spinnerRangeTo = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        comboDomain = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        labelRangeFrom = new javax.swing.JLabel();
        labelRangeTo = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        checkSave = new javax.swing.JCheckBox();
        checkDisplay = new javax.swing.JCheckBox();
        labelPasses = new javax.swing.JLabel();
        spinnerPasses = new javax.swing.JSpinner();
        spinnerLatency = new javax.swing.JSpinner();
        checkZigzag = new javax.swing.JCheckBox();
        labelLatency = new javax.swing.JLabel();
        checkRelative = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        spinnerTime = new javax.swing.JSpinner();
        textTitle = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        panelPositioners = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tablePositioners = new javax.swing.JTable();
        buttonDelete = new javax.swing.JButton();
        buttonPosUp = new javax.swing.JButton();
        buttonPosInsert = new javax.swing.JButton();
        buttonPosDown = new javax.swing.JButton();
        panelVector = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        tableVector = new javax.swing.JTable();

        jCheckBox1.setText("jCheckBox1");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(jTable1);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Shell Command"));

        textCommand.setEditable(false);
        textCommand.setColumns(20);
        textCommand.setRows(1);
        jScrollPane1.setViewportView(textCommand);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Scan Definition"));

        jLabel1.setText("Scan Type:");

        comboType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Linear", "Multidimensional", "Vector", "Continuous", "Time series", "Change event series" }));
        comboType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTypeActionPerformed(evt);
            }
        });

        panelSensors.setBorder(javax.swing.BorderFactory.createTitledBorder("Sensors"));

        tableSensors.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "Name", "Samples", "Interval", "Plot"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableSensors.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSensors.getTableHeader().setReorderingAllowed(false);
        tableSensors.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableSensorsMouseReleased(evt);
            }
        });
        tableSensors.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableSensorsKeyReleased(evt);
            }
        });
        jScrollPane4.setViewportView(tableSensors);

        buttonSenDelete.setText("Delete");
        buttonSenDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSenDeleteActionPerformed(evt);
            }
        });

        buttonSenUp.setText("Move Up");
        buttonSenUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSenUpActionPerformed(evt);
            }
        });

        buttonSenInsert.setText("Insert");
        buttonSenInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSenInsertActionPerformed(evt);
            }
        });

        buttonSenDown.setText("Move Down");
        buttonSenDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSenDownActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelSensorsLayout = new javax.swing.GroupLayout(panelSensors);
        panelSensors.setLayout(panelSensorsLayout);
        panelSensorsLayout.setHorizontalGroup(
            panelSensorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSensorsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSensorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(panelSensorsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonSenUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSenDown)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSenInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSenDelete)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelSensorsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonSenDelete, buttonSenDown, buttonSenInsert, buttonSenUp});

        panelSensorsLayout.setVerticalGroup(
            panelSensorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSensorsLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSensorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSenDelete)
                    .addComponent(buttonSenInsert)
                    .addComponent(buttonSenDown)
                    .addComponent(buttonSenUp))
                .addContainerGap())
        );

        panelParameters.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Data"));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Layout:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Provider:");

        comboLayout.setEditable(true);
        comboLayout.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Default", "Table" }));

        comboProvider.setEditable(true);
        comboProvider.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "h5", "txt" }));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("File:");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Tag:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(comboLayout, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboProvider, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(textTag)
                    .addComponent(textFile))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel4, jLabel5, jLabel6, jLabel8});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(textFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(textTag, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(comboProvider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot"));

        buttonGrouPlotRange.add(radioScan);
        radioScan.setSelected(true);
        radioScan.setText("Scan");
        radioScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioScanActionPerformed(evt);
            }
        });

        buttonGrouPlotRange.add(radioAuto);
        radioAuto.setText("Auto");
        radioAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioAutoActionPerformed(evt);
            }
        });

        buttonGrouPlotRange.add(radioManual);
        radioManual.setText("Manual");
        radioManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioManualActionPerformed(evt);
            }
        });

        spinnerRangeFrom.setModel(new javax.swing.SpinnerNumberModel(0.0d, null, null, 1.0d));

        spinnerRangeTo.setModel(new javax.swing.SpinnerNumberModel(0.0d, null, null, 1.0d));

        jLabel9.setText("Domain:");

        comboDomain.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Positioner", "Index", "Time" }));
        comboDomain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboDomainActionPerformed(evt);
            }
        });

        jLabel10.setText("Range:");

        labelRangeFrom.setText("from:");

        labelRangeTo.setText("to:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(radioAuto)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelRangeFrom))
                            .addComponent(radioScan)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(radioManual)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelRangeTo)))
                        .addGap(1, 1, 1)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spinnerRangeTo)
                            .addComponent(spinnerRangeFrom)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(65, 65, 65)
                        .addComponent(comboDomain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(11, 11, 11))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(comboDomain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioScan)
                    .addComponent(jLabel10))
                .addGap(0, 0, 0)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioAuto)
                    .addComponent(spinnerRangeFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelRangeFrom))
                .addGap(0, 0, 0)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioManual)
                    .addComponent(spinnerRangeTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelRangeTo))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("General"));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Title:");

        checkSave.setSelected(true);
        checkSave.setText("Save");
        checkSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkSaveActionPerformed(evt);
            }
        });

        checkDisplay.setSelected(true);
        checkDisplay.setText("Display");
        checkDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkDisplayActionPerformed(evt);
            }
        });

        labelPasses.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelPasses.setText("Passes:");

        spinnerPasses.setModel(new javax.swing.SpinnerNumberModel(1, 0, 1000000, 1));
        spinnerPasses.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerPassesStateChanged(evt);
            }
        });

        spinnerLatency.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, 10000.0d, 1.0d));
        spinnerLatency.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerLatencyStateChanged(evt);
            }
        });

        checkZigzag.setText("Zigzag");
        checkZigzag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkZigzagActionPerformed(evt);
            }
        });

        labelLatency.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelLatency.setText("Latency (s):");

        checkRelative.setText("Relative ");
        checkRelative.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkRelativeActionPerformed(evt);
            }
        });

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Time (s):");

        spinnerTime.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.0d, 10000.0d, 1.0d));
        spinnerTime.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTimeStateChanged(evt);
            }
        });

        textTitle.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textTitleKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(labelPasses)
                    .addComponent(jLabel3)
                    .addComponent(labelLatency))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spinnerPasses, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                    .addComponent(spinnerTime)
                    .addComponent(textTitle)
                    .addComponent(spinnerLatency))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkZigzag)
                    .addComponent(checkRelative)
                    .addComponent(checkSave)
                    .addComponent(checkDisplay))
                .addContainerGap())
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, jLabel7, labelLatency, labelPasses});

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {checkDisplay, checkRelative, checkSave, checkZigzag});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(checkSave)
                    .addComponent(textTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(0, 0, 0)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(checkDisplay)
                    .addComponent(spinnerPasses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelPasses))
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(checkRelative)
                    .addComponent(spinnerTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(checkZigzag)
                    .addComponent(spinnerLatency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelLatency))
                .addContainerGap())
        );

        javax.swing.GroupLayout panelParametersLayout = new javax.swing.GroupLayout(panelParameters);
        panelParameters.setLayout(panelParametersLayout);
        panelParametersLayout.setHorizontalGroup(
            panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelParametersLayout.setVerticalGroup(
            panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelParametersLayout.createSequentialGroup()
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 134, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelParametersLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel3, jPanel4, jPanel6});

        panelPositioners.setBorder(javax.swing.BorderFactory.createTitledBorder("Positioners"));

        tablePositioners.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "Name", "Start", "Stop", "Step"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tablePositioners.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablePositioners.getTableHeader().setReorderingAllowed(false);
        tablePositioners.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tablePositionersMouseReleased(evt);
            }
        });
        tablePositioners.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tablePositionersKeyReleased(evt);
            }
        });
        jScrollPane3.setViewportView(tablePositioners);

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonPosUp.setText("Move Up");
        buttonPosUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPosUpActionPerformed(evt);
            }
        });

        buttonPosInsert.setText("Insert");
        buttonPosInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPosInsertActionPerformed(evt);
            }
        });

        buttonPosDown.setText("Move Down");
        buttonPosDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPosDownActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelPositionersLayout = new javax.swing.GroupLayout(panelPositioners);
        panelPositioners.setLayout(panelPositionersLayout);
        panelPositionersLayout.setHorizontalGroup(
            panelPositionersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPositionersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelPositionersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPositionersLayout.createSequentialGroup()
                        .addGap(0, 61, Short.MAX_VALUE)
                        .addComponent(buttonPosUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPosDown)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPosInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDelete)
                        .addGap(0, 62, Short.MAX_VALUE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        panelPositionersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonPosDown, buttonPosInsert, buttonPosUp});

        panelPositionersLayout.setVerticalGroup(
            panelPositionersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPositionersLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPositionersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonPosInsert)
                    .addComponent(buttonPosDown)
                    .addComponent(buttonPosUp))
                .addContainerGap())
        );

        panelVector.setBorder(javax.swing.BorderFactory.createTitledBorder("Vector"));

        tableVector.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tableVector.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jScrollPane7.setViewportView(tableVector);

        javax.swing.GroupLayout panelVectorLayout = new javax.swing.GroupLayout(panelVector);
        panelVector.setLayout(panelVectorLayout);
        panelVectorLayout.setHorizontalGroup(
            panelVectorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelVectorLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                .addGap(7, 7, 7))
        );
        panelVectorLayout.setVerticalGroup(
            panelVectorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelVectorLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addGap(7, 7, 7))
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(panelPositioners, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelVector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelVector, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelPositioners, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelSensors, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelParameters, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(comboType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelSensors, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(panelParameters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void comboTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTypeActionPerformed
        try {
            onControlChanged();
            update();
            ((TitledBorder) panelPositioners.getBorder()).setTitle(getScanCommand().equals("mscan") ? "Trigger" : "Positioners");
            updateUI();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_comboTypeActionPerformed

    private void tablePositionersMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tablePositionersMouseReleased
        update();
    }//GEN-LAST:event_tablePositionersMouseReleased

    private void tablePositionersKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tablePositionersKeyReleased
        update();
    }//GEN-LAST:event_tablePositionersKeyReleased

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (modelPositioners.getRowCount() > 0) {
            modelPositioners.removeRow(Math.max(tablePositioners.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonPosUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPosUpActionPerformed
        try {
            int rows = modelPositioners.getRowCount();
            int cur = tablePositioners.getSelectedRow();
            modelPositioners.moveRow(cur, cur, cur - 1);
            tablePositioners.setRowSelectionInterval(cur - 1, cur - 1);
            update();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonPosUpActionPerformed

    private void buttonPosInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPosInsertActionPerformed
        Object[] data = new Object[]{Type.Channel, "", 0.0, 0.0, 1};
        if (tablePositioners.getSelectedRow() >= 0) {
            modelPositioners.insertRow(tablePositioners.getSelectedRow() + 1, data);
        } else {
            modelPositioners.addRow(data);
        }
        modelPositioners.fireTableDataChanged();
        update();
    }//GEN-LAST:event_buttonPosInsertActionPerformed

    private void buttonPosDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPosDownActionPerformed
        try {
            int rows = modelPositioners.getRowCount();
            int cur = tablePositioners.getSelectedRow();
            modelPositioners.moveRow(cur, cur, cur + 1);
            tablePositioners.setRowSelectionInterval(cur + 1, cur + 1);
            update();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonPosDownActionPerformed

    private void tableSensorsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSensorsMouseReleased
        update();
    }//GEN-LAST:event_tableSensorsMouseReleased

    private void tableSensorsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableSensorsKeyReleased
        update();
    }//GEN-LAST:event_tableSensorsKeyReleased

    private void buttonSenDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSenDeleteActionPerformed
        if (modelSensors.getRowCount() > 0) {
            modelSensors.removeRow(Math.max(tableSensors.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonSenDeleteActionPerformed

    private void buttonSenUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSenUpActionPerformed
        try {
            int rows = modelSensors.getRowCount();
            int cur = tableSensors.getSelectedRow();
            modelSensors.moveRow(cur, cur, cur - 1);
            tableSensors.setRowSelectionInterval(cur - 1, cur - 1);
            update();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonSenUpActionPerformed

    private void buttonSenInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSenInsertActionPerformed
        Object[] data = new Object[]{Type.Channel, "", 1, 0, PlotOption.Enabled};
        if (tableSensors.getSelectedRow() >= 0) {
            modelSensors.insertRow(tableSensors.getSelectedRow() + 1, data);
        } else {
            modelSensors.addRow(data);
        }
        modelSensors.fireTableDataChanged();
        update();
    }//GEN-LAST:event_buttonSenInsertActionPerformed

    private void buttonSenDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSenDownActionPerformed
        try {
            int rows = modelSensors.getRowCount();
            int cur = tableSensors.getSelectedRow();
            modelSensors.moveRow(cur, cur, cur + 1);
            tableSensors.setRowSelectionInterval(cur + 1, cur + 1);
            update();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonSenDownActionPerformed

    private void checkRelativeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkRelativeActionPerformed
        onControlChanged();
    }//GEN-LAST:event_checkRelativeActionPerformed

    private void checkZigzagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkZigzagActionPerformed
        onControlChanged();
    }//GEN-LAST:event_checkZigzagActionPerformed

    private void spinnerPassesStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerPassesStateChanged
        onControlChanged();
    }//GEN-LAST:event_spinnerPassesStateChanged

    private void spinnerTimeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTimeStateChanged
        onControlChanged();
    }//GEN-LAST:event_spinnerTimeStateChanged

    private void spinnerLatencyStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerLatencyStateChanged
        onControlChanged();
    }//GEN-LAST:event_spinnerLatencyStateChanged

    private void textTitleKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textTitleKeyReleased
        onControlChanged();
    }//GEN-LAST:event_textTitleKeyReleased

    private void checkSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkSaveActionPerformed
        onControlChanged();
        update();
    }//GEN-LAST:event_checkSaveActionPerformed

    private void checkDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkDisplayActionPerformed
        onControlChanged();
    }//GEN-LAST:event_checkDisplayActionPerformed

    private void radioScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioScanActionPerformed
        onControlChanged();
        update();
    }//GEN-LAST:event_radioScanActionPerformed

    private void radioAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioAutoActionPerformed
        onControlChanged();
        update();
    }//GEN-LAST:event_radioAutoActionPerformed

    private void radioManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioManualActionPerformed
        onControlChanged();
        update();
    }//GEN-LAST:event_radioManualActionPerformed

    private void comboDomainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboDomainActionPerformed
        if (!updatingDomain) {
            updatingDomain = true;
            try {
                if (comboDomain.getSelectedIndex() > 2) {
                    setSensorDomain(comboDomain.getSelectedIndex() - 3);
                } else {
                    setSensorDomain(-1);
                }
                onControlChanged();
            } finally {
                updatingDomain = false;
            }
        }
    }//GEN-LAST:event_comboDomainActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.ButtonGroup buttonGrouPlotRange;
    private javax.swing.JButton buttonPosDown;
    private javax.swing.JButton buttonPosInsert;
    private javax.swing.JButton buttonPosUp;
    private javax.swing.JButton buttonSenDelete;
    private javax.swing.JButton buttonSenDown;
    private javax.swing.JButton buttonSenInsert;
    private javax.swing.JButton buttonSenUp;
    private javax.swing.JCheckBox checkDisplay;
    private javax.swing.JCheckBox checkRelative;
    private javax.swing.JCheckBox checkSave;
    private javax.swing.JCheckBox checkZigzag;
    private javax.swing.JComboBox<String> comboDomain;
    private javax.swing.JComboBox<String> comboLayout;
    private javax.swing.JComboBox<String> comboProvider;
    private javax.swing.JComboBox<String> comboType;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel labelLatency;
    private javax.swing.JLabel labelPasses;
    private javax.swing.JLabel labelRangeFrom;
    private javax.swing.JLabel labelRangeTo;
    private javax.swing.JPanel panelParameters;
    private javax.swing.JPanel panelPositioners;
    private javax.swing.JPanel panelSensors;
    private javax.swing.JPanel panelVector;
    private javax.swing.JRadioButton radioAuto;
    private javax.swing.JRadioButton radioManual;
    private javax.swing.JRadioButton radioScan;
    private javax.swing.JSpinner spinnerLatency;
    private javax.swing.JSpinner spinnerPasses;
    private javax.swing.JSpinner spinnerRangeFrom;
    private javax.swing.JSpinner spinnerRangeTo;
    private javax.swing.JSpinner spinnerTime;
    private javax.swing.JTable tablePositioners;
    private javax.swing.JTable tableSensors;
    private javax.swing.JTable tableVector;
    private javax.swing.JTextArea textCommand;
    private javax.swing.JTextField textFile;
    private javax.swing.JTextField textTag;
    private javax.swing.JTextField textTitle;
    // End of variables declaration//GEN-END:variables

}
