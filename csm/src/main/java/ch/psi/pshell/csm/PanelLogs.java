package ch.psi.pshell.csm;

import ch.psi.pshell.logging.Logging;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Str;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 */
public class PanelLogs extends MonitoredPanel {

    public static final Color INFO_COLOR = ch.psi.pshell.app.MainFrame.isDark() ? new Color(187, 187, 187) : Color.BLACK;
    public static final Color WARNING_COLOR = Color.ORANGE;
    public static final Color ERROR_COLOR = ch.psi.pshell.app.MainFrame.isDark() ? new Color(210, 21, 73) : Color.RED;
    public static final String FILE_SEPARATOR = Logging.FILE_SEPARATOR;    
    
    final DefaultTableModel model;
    final ExecutorService executorService;
    final LoggerRetrieveFunction function;
    boolean inverted;

    public void setInverted(boolean value) {
        inverted = value;
    }
    
    public interface LoggerRetrieveFunction {
        String get() throws Exception;
    }

    
    public static JDialog show(Component parent, String title, ExecutorService executorService, LoggerRetrieveFunction function){
        PanelLogs panel = new PanelLogs(executorService, function);
        JDialog dlg =  SwingUtils.showDialog(parent, title, new Dimension(800,400), panel);        
        panel.update();
        return dlg;
    }

    public PanelLogs(ExecutorService executorService, LoggerRetrieveFunction function) {
        initComponents();
        this.executorService = executorService;
        this.function = function;
        model = (DefaultTableModel) table.getModel();
        
        configureLevelColors();
        configureMouseEvents();
    }
    
    void configureLevelColors(){
        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    comp.setForeground(INFO_COLOR);
                    String cell = (String) table.getModel().getValueAt(row, 3);
                    if (Level.SEVERE.toString().equalsIgnoreCase(cell)) {
                        comp.setForeground(ERROR_COLOR);
                    } else if (Level.WARNING.toString().equalsIgnoreCase(cell)) {
                        comp.setForeground(WARNING_COLOR);
                    }
                    return comp;
                }
            });
            column.setPreferredWidth((i == (model.getColumnCount() - 1)) ? 300 : 75);
        }        
    }
    
    
    void configureMouseEvents(){
        table.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                String tooltip = null;
                try {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if ((row >= 0) && (row < table.getRowCount())) {
                        String text = String.valueOf(table.getValueAt(row, 4));
                        if (text.contains("\n")) {
                            text = text.split("\n")[0];
                        }
                        tooltip = "<html>" + Str.toHtml(text) + "</html>";
                    }
                } catch (Exception ex) {
                }

                table.setToolTipText(tooltip);
            }
        });        
    }
    
    

    int outputLength = -1;

    public int getOutputLength() {
        return outputLength;
    }

    public void setOutputLength(int value) {
        outputLength = value;
    }
    
    volatile boolean updating;
    public void update(){
        if (updating){
            return;
        }
        updating = true;
        buttonUpdate.setEnabled(!updating);
        executorService.submit(()->{
            try{
                String logs = function.get();
                load(logs);
            } catch (Exception ex){
                SwingUtils.showException(this, ex);
            } finally{
                updating = false;
                buttonUpdate.setEnabled(!updating);
            }                 
        });   
    }


    //TODO: load a stream instead
    public void load(String logs) throws Exception {
        //model.setNumRows(0);
        List<Object[]> rows = new ArrayList<>();
        int log_cols = 4;
        try (BufferedReader reader = new BufferedReader(new StringReader(logs))) {
            String line;
            Object[] row;
            while ((line = reader.readLine()) != null) {
                try{
                    // process the line.
                    String[] data = line.split(FILE_SEPARATOR);
                    //The text contains FILE_SEPARATOR
                    if (data.length > log_cols) {
                        String[] aux = new String[log_cols];
                        System.arraycopy(data, 0, aux, 0, 3);
                        aux[log_cols-1] = String.join(FILE_SEPARATOR, Arr.getSubArray(data, 3));
                        data = aux;
                    }
                    String[] time = data[0].trim().split(" ");
                    row = new Object[log_cols+1];
                    System.arraycopy(time, 0, row, 0, 2);
                    System.arraycopy(data, 1, row ,2, 3);                                        
                } catch (Exception ex){
                    row = new Object[]{"", "", "LoggerPanel", "Exception", ex.getMessage()};
                }
                //addRow(row);
                rows.add(row);
            }
            setRows(rows.toArray(new Object[0][0]));
        } finally {
            textRows.setText(Str.toString(model.getRowCount()));
        }
    }
    
    
    
    void setRows(Object[][] data) throws Exception {     
        model.setDataVector(data, SwingUtils.getTableColumnNames(table));    
        configureLevelColors();
    }
    

    void addRow(Object[] data) throws Exception {     
        if (data.length != 5) {
            throw new Exception ("Invalid row format: " + Str.toString(data));
        }
        if (inverted) {
            model.insertRow(0, data);
            if ((getOutputLength() >= 0) && (model.getRowCount() > getOutputLength())) {
                model.removeRow(model.getRowCount() - 1);
            }

        } else {
            model.addRow(data);
            if ((getOutputLength() >= 0) && (model.getRowCount() > getOutputLength())) {
                model.removeRow(0);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonClose = new javax.swing.JButton();
        buttonUpdate = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textRows = new javax.swing.JTextField();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Date", "Time", "Origin", "Level", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getTableHeader().setReorderingAllowed(false);
        scrollPane.setViewportView(table);

        buttonClose.setText("Close");
        buttonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCloseActionPerformed(evt);
            }
        });

        buttonUpdate.setText("Update");
        buttonUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpdateActionPerformed(evt);
            }
        });

        jLabel1.setText("Rows:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPane)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textRows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 397, Short.MAX_VALUE)
                        .addComponent(buttonUpdate)
                        .addGap(18, 18, 18)
                        .addComponent(buttonClose)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonClose, buttonUpdate});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonClose)
                    .addComponent(buttonUpdate)
                    .addComponent(jLabel1)
                    .addComponent(textRows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpdateActionPerformed
        update();
    }//GEN-LAST:event_buttonUpdateActionPerformed

    private void buttonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseActionPerformed
        SwingUtils.getWindow(this).setVisible(false);
    }//GEN-LAST:event_buttonCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClose;
    private javax.swing.JButton buttonUpdate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTable table;
    private javax.swing.JTextField textRows;
    // End of variables declaration//GEN-END:variables
}
