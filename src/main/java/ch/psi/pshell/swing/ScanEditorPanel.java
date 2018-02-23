/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 */
public class ScanEditorPanel extends MonitoredPanel implements Processor {

    boolean changed;
    boolean running;
    String fileName;

    final DefaultTableModel modelPositioners;
    final DefaultTableModel modelSensors;
    final DefaultTableModel modelParameters;
    final DefaultTableModel modelVector;

    /**
     * Creates new form ScanEditorPanel
     */
    public ScanEditorPanel() {
        initComponents();
        modelPositioners = (DefaultTableModel) tablePositioners.getModel();
        modelSensors = (DefaultTableModel) tableSensors.getModel();
        modelParameters = (DefaultTableModel) tableParameters.getModel();
        modelVector = (DefaultTableModel) tableVector.getModel();

        setupColumns();

        TableModelListener tableChangeListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                onControlChanged();
                update();
            }
        };
        modelPositioners.addTableModelListener(tableChangeListener);
        modelSensors.addTableModelListener(tableChangeListener);
        modelParameters.addTableModelListener(tableChangeListener);
        update();
    }

    void setupColumns() {
        setupTypeColumn(tablePositioners, tablePositioners.getColumnModel().getColumn(0), true);
        setupTypeColumn(tableSensors, tableSensors.getColumnModel().getColumn(0), false);
        tablePositioners.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablePositioners.getColumnModel().getColumn(1).setPreferredWidth(214);
        tablePositioners.getColumnModel().getColumn(2).setPreferredWidth(60);
        tablePositioners.getColumnModel().getColumn(3).setPreferredWidth(60);
        tablePositioners.getColumnModel().getColumn(4).setPreferredWidth(60);
        tableSensors.getColumnModel().getColumn(2).setPreferredWidth(60);
        tableSensors.getColumnModel().getColumn(2).setPreferredWidth(334);
        tableSensors.getColumnModel().getColumn(2).setPreferredWidth(60);

        /*
        TableColumn columnStep = tablePositioners.getColumnModel().getColumn(4);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        columnStep.setCellRenderer(renderer);
        JTextField tf = new JTextField();
        tf.setHorizontalAlignment(JTextField.RIGHT);
        columnStep.setCellEditor(new DefaultCellEditor(tf));
        */
    }

    void setupTypeColumn(JTable table, TableColumn colType, boolean readWrite) {
        JComboBox comboType = new JComboBox();
        table.setRowHeight(Math.max(table.getRowHeight(), comboType.getPreferredSize().height - 3));
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (Type type : Type.values()) {
            if (!type.isReadOnly() || !readWrite) {
                model.addElement(type);
            }
        }
        comboType.setModel(model);
        comboType.setEditable(true);

        DefaultCellEditor cellEditor = new DefaultCellEditor(comboType);
        cellEditor.setClickCountToStart(2);
        colType.setCellEditor(cellEditor);
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
        modelParameters.setDataVector(toObjectArray((List) state.get(3)), SwingUtils.getTableColumnNames(tableParameters));
        if (getScanCommand().equals("vscan")){
            ArrayList<String> columns = new ArrayList<>();
            for (int i=0; i<modelPositioners.getRowCount(); i++ ){
                columns.add(String.valueOf(modelPositioners.getValueAt(i, 1)));
            }
            modelVector.setDataVector(toObjectArray((List) state.get(4)), columns.toArray());
        }
        textTitle.setText((String) state.get(5));
        spinnerPasses.setValue(state.get(6));
        spinnerTime.setValue(state.get(7));
        spinnerLatency.setValue(state.get(8));
        checkRelative.setSelected((boolean) state.get(9));
        checkZigzag.setSelected((boolean) state.get(10));
        checkPersist.setSelected((boolean) state.get(11));
        checkDisplay.setSelected((boolean) state.get(12));
        
        setupColumns();
        this.fileName = fileName;
        changed = false;
        updateCommand();
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
        if (!getScanCommand().equals("vscan")){
            modelVector.setColumnCount(0);
            modelVector.setRowCount(0);
        }
                
        List state = new ArrayList();
        state.add(comboType.getSelectedItem());
        state.add(modelPositioners.getDataVector());
        state.add(modelSensors.getDataVector());
        state.add(modelParameters.getDataVector());
        state.add(modelVector.getDataVector());
        state.add(textTitle.getText());
        state.add(spinnerPasses.getValue());
        state.add(spinnerTime.getValue());
        state.add(spinnerLatency.getValue());
        state.add(checkRelative.isSelected());
        state.add(checkZigzag.isSelected());
        state.add(checkPersist.isSelected());
        state.add(checkDisplay.isSelected());
        
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
        if (textCommand.getForeground().equals(Color.RED)){
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
            SwingUtilities.invokeLater(()->{
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
            List    <String> pars = getParameters();
            StringBuilder cmd = new StringBuilder();
            cmd.append(scan).append("(");
            if (hasPositioners) {
                //    if (pos.size()==1){
                //        cmd.append(pos.get(0)).append(", ");
                //    } else {
                cmd.append("[").append(String.join(", ", pos)).append("], ");
                //    }
            }
            //if (sensors.size()==1){
            //    cmd.append(sensors.get(0)).append(", ");
            //} else {
            cmd.append("[").append(String.join(", ", sensors)).append("], ");
            //}
            if (hasRange()) {
                //if (pos.size()==1){
                //    cmd.append(getStart().get(0)).append(", ");
                //    cmd.append(getStop().get(0)).append(", ");
                //    cmd.append(getStep().get(0)).append(", ");                       
                //} else {
                cmd.append("[").append(String.join(", ", getStart())).append("], ");
                cmd.append("[").append(String.join(", ", getStop())).append("], ");
                cmd.append("[").append(String.join(", ", getStep())).append("], ");
                //}
            }
            cmd.append(String.join(", ", getParameters()));
            cmd.append(")");
            textCommand.setForeground(Color.BLACK);
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
        rows = modelParameters.getRowCount();
        cur = tableParameters.getSelectedRow();
        buttonParsDelete.setEnabled((rows > 0) && (cur >= 0) && enabled);
        buttonParsInsert.setEnabled(enabled);
        tableParameters.setEnabled(enabled);

        textTitle.setEnabled(enabled);
        spinnerPasses.setEnabled(enabled);
        spinnerTime.setEnabled(enabled && (hasTime || cmd.equals("mscan")));
        spinnerLatency.setEnabled(enabled && !cmd.equals("mscan"));
        checkRelative.setEnabled(enabled && hasPasses());
        checkZigzag.setEnabled(enabled && hasPasses());
        checkPersist.setEnabled(enabled);
        checkDisplay.setEnabled(enabled);
        
        labelPasses.setText(hasPasses() ? "Passes:" : "Records:");
        
        switch (cmd){
            case "tscan":
                labelLatency.setText("Interval (s):");
                break;
            default:
                labelLatency.setText("Latency (s):");
                break;                
        }
         
        if (cmd.equals("vscan")){
            panelVector.setVisible(true);
            if(modelVector.getColumnCount() != modelPositioners.getRowCount()){
                modelVector.setColumnCount(modelPositioners.getRowCount());
                modelVector.setRowCount(1000);
            }
            for (int i=0; i< modelPositioners.getRowCount(); i++){
                tableVector.getColumnModel().getColumn(i).setHeaderValue(modelPositioners.getValueAt(i, 1));
            }
        } else{
            panelVector.setVisible(false);
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
    
    boolean hasPasses(){
        return Arr.containsEqual(new String[]{"lscan", "ascan", "vscan", "cscan"}, getScanCommand());
    }

    boolean hasRange(){
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
                    break;
            }
            if (sensor!=null){
                Number samples = ((Number)modelSensors.getValueAt(i,2));
                if ((samples==null) || samples.intValue() <0){
                    samples = 1;
                }
                Number interval = ((Number)modelSensors.getValueAt(i,3));
                if (interval==null){
                    interval = 0.0;
                }        
                if (samples.intValue() > 1){
                    sensor = "create_averager(" + sensor+ ", " + samples.intValue() + ", " + interval.doubleValue() + ")";
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
        if (getScanCommand()=="tscan"){
            ret.add(getParName("points") + spinnerPasses.getValue());
            ret.add(getParName("interval") + spinnerLatency.getValue());
        } else if (getScanCommand()=="mscan"){
            ret.add(getParName("points") + ((((Number)spinnerPasses.getValue()).doubleValue() == 0) ? -1 : spinnerPasses.getValue()));  
            ret.add(getParName("timeout") + ((((Number)spinnerTime.getValue()).doubleValue() == 0) ? -1 : spinnerTime.getValue()));
        } else if (getScanCommand()=="vscan"){
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i=0; i<modelVector.getRowCount(); i++){
                if ((modelVector.getValueAt(i, 0) == null) || (modelVector.getValueAt(i, 0).toString().trim().isEmpty())){
                    break;
                }
                sb.append("[");
                for (int j=0; j<modelVector.getColumnCount(); j++){
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
        if (Context.getInstance().getSetup().getScriptType() == ScriptType.py){
            if (!checkPersist.isSelected()){
                ret.add(getParName("persist") + getBoolValue(false));
            }
            if (!checkDisplay.isSelected()){
                ret.add(getParName("accumulate") + getBoolValue(false));
                ret.add(getParName("plot_disabled") + getBoolValue(true));
                ret.add(getParName("table_disabled") + getBoolValue(true));
            }            
            if (modelParameters.getRowCount() > 0) {
                for (int i = 0; i < modelParameters.getRowCount(); i++) {
                    String par = modelParameters.getValueAt(i, 0).toString();
                    String value = modelParameters.getValueAt(i, 1).toString();
                    if (!par.trim().isEmpty() && !value.trim().isEmpty()) {
                        ret.add(getParName(par) + value);
                    }
                }
            }
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
        labelLatency = new javax.swing.JLabel();
        spinnerLatency = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        textTitle = new javax.swing.JTextField();
        checkRelative = new javax.swing.JCheckBox();
        labelPasses = new javax.swing.JLabel();
        spinnerPasses = new javax.swing.JSpinner();
        checkZigzag = new javax.swing.JCheckBox();
        spinnerTime = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        tableParameters = new javax.swing.JTable();
        buttonParsInsert = new javax.swing.JButton();
        buttonParsDelete = new javax.swing.JButton();
        checkPersist = new javax.swing.JCheckBox();
        checkDisplay = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
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
                "Type", "Name", "Samples", "Interval"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class
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
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSensorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSenDelete)
                    .addComponent(buttonSenInsert)
                    .addComponent(buttonSenDown)
                    .addComponent(buttonSenUp))
                .addContainerGap())
        );

        panelParameters.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));

        labelLatency.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelLatency.setText("Latency (s):");

        spinnerLatency.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, 10000.0d, 1.0d));
        spinnerLatency.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerLatencyStateChanged(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Title:");

        textTitle.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textTitleKeyReleased(evt);
            }
        });

        checkRelative.setText("Relative ");
        checkRelative.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkRelativeActionPerformed(evt);
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

        checkZigzag.setText("Zigzag");
        checkZigzag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkZigzagActionPerformed(evt);
            }
        });

        spinnerTime.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.0d, 10000.0d, 1.0d));
        spinnerTime.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTimeStateChanged(evt);
            }
        });

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Time (s):");

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Advanced"));

        tableParameters.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableParameters.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableParameters.getTableHeader().setReorderingAllowed(false);
        tableParameters.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableParametersMouseReleased(evt);
            }
        });
        tableParameters.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableParametersKeyReleased(evt);
            }
        });
        jScrollPane6.setViewportView(tableParameters);

        buttonParsInsert.setText("Insert");
        buttonParsInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonParsInsertActionPerformed(evt);
            }
        });

        buttonParsDelete.setText("Delete");
        buttonParsDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonParsDeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonParsInsert)
                        .addGap(30, 30, 30)
                        .addComponent(buttonParsDelete)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonParsDelete, buttonParsInsert});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonParsDelete)
                    .addComponent(buttonParsInsert))
                .addGap(2, 2, 2))
        );

        checkPersist.setSelected(true);
        checkPersist.setText("Save");
        checkPersist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkPersistActionPerformed(evt);
            }
        });

        checkDisplay.setSelected(true);
        checkDisplay.setText("Display");
        checkDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkDisplayActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelParametersLayout = new javax.swing.GroupLayout(panelParameters);
        panelParameters.setLayout(panelParametersLayout);
        panelParametersLayout.setHorizontalGroup(
            panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(labelPasses)
                    .addComponent(jLabel3)
                    .addComponent(labelLatency))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(spinnerPasses)
                        .addComponent(spinnerTime)
                        .addComponent(textTitle))
                    .addComponent(spinnerLatency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkZigzag)
                    .addComponent(checkRelative)
                    .addComponent(checkPersist)
                    .addComponent(checkDisplay))
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelParametersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, jLabel7, labelLatency, labelPasses});

        panelParametersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerLatency, spinnerPasses, spinnerTime});

        panelParametersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {checkDisplay, checkPersist, checkRelative, checkZigzag});

        panelParametersLayout.setVerticalGroup(
            panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelParametersLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelParametersLayout.createSequentialGroup()
                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(textTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkPersist))
                        .addGap(0, 0, 0)
                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerPasses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelPasses)
                            .addComponent(checkDisplay))
                        .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelParametersLayout.createSequentialGroup()
                                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel7)
                                    .addComponent(spinnerTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, 0)
                                .addGroup(panelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(labelLatency)
                                    .addComponent(spinnerLatency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelParametersLayout.createSequentialGroup()
                                .addComponent(checkRelative)
                                .addGap(0, 0, 0)
                                .addComponent(checkZigzag)))))
                .addGap(2, 2, 2))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonPosUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPosDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPosInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPositionersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );

        panelPositionersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonPosDown, buttonPosInsert, buttonPosUp});

        panelPositionersLayout.setVerticalGroup(
            panelPositionersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPositionersLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
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
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6))
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelParameters, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            ((TitledBorder)panelPositioners.getBorder()).setTitle(getScanCommand().equals("mscan") ? "Trigger" : "Positioners");
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
        Object[] data = new Object[]{Type.Channel, "", 1, 0};
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

    private void tableParametersMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableParametersMouseReleased
        update();
    }//GEN-LAST:event_tableParametersMouseReleased

    private void tableParametersKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableParametersKeyReleased
        update();
    }//GEN-LAST:event_tableParametersKeyReleased

    private void buttonParsDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonParsDeleteActionPerformed
        if (modelParameters.getRowCount() > 0) {
            modelParameters.removeRow(Math.max(tableParameters.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonParsDeleteActionPerformed

    private void buttonParsInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonParsInsertActionPerformed
        Object[] data = new Object[]{"", ""};
        if (tableParameters.getSelectedRow() >= 0) {
            modelParameters.insertRow(tableSensors.getSelectedRow() + 1, data);
        } else {
            modelParameters.addRow(data);
        }
        modelParameters.fireTableDataChanged();
        update();
    }//GEN-LAST:event_buttonParsInsertActionPerformed

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

    private void checkPersistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkPersistActionPerformed
        onControlChanged();
    }//GEN-LAST:event_checkPersistActionPerformed

    private void checkDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkDisplayActionPerformed
        onControlChanged();
    }//GEN-LAST:event_checkDisplayActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonParsDelete;
    private javax.swing.JButton buttonParsInsert;
    private javax.swing.JButton buttonPosDown;
    private javax.swing.JButton buttonPosInsert;
    private javax.swing.JButton buttonPosUp;
    private javax.swing.JButton buttonSenDelete;
    private javax.swing.JButton buttonSenDown;
    private javax.swing.JButton buttonSenInsert;
    private javax.swing.JButton buttonSenUp;
    private javax.swing.JCheckBox checkDisplay;
    private javax.swing.JCheckBox checkPersist;
    private javax.swing.JCheckBox checkRelative;
    private javax.swing.JCheckBox checkZigzag;
    private javax.swing.JComboBox<String> comboType;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel labelLatency;
    private javax.swing.JLabel labelPasses;
    private javax.swing.JPanel panelParameters;
    private javax.swing.JPanel panelPositioners;
    private javax.swing.JPanel panelSensors;
    private javax.swing.JPanel panelVector;
    private javax.swing.JSpinner spinnerLatency;
    private javax.swing.JSpinner spinnerPasses;
    private javax.swing.JSpinner spinnerTime;
    private javax.swing.JTable tableParameters;
    private javax.swing.JTable tablePositioners;
    private javax.swing.JTable tableSensors;
    private javax.swing.JTable tableVector;
    private javax.swing.JTextArea textCommand;
    private javax.swing.JTextField textTitle;
    // End of variables declaration//GEN-END:variables

}
