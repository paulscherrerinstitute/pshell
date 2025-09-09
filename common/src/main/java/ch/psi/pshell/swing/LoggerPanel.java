package ch.psi.pshell.swing;

import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.logging.Logging;
import ch.psi.pshell.utils.Str;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 */
public class LoggerPanel extends MonitoredPanel {

    Color colorInfo ;
    Color colorWarning;
    Color colorError;
    
    final Object insertLock = new Object();
    
    final DefaultTableModel model;
    
    @Override
    protected final void onLafChange() {  
        colorInfo = MainFrame.isDark() ? new Color(187, 187, 187) : Color.BLACK;
        colorWarning = Color.ORANGE;
        colorError = MainFrame.isDark() ? new Color(210, 21, 73) : Color.RED;                
    }    

    boolean inverted;

    public void setInverted(boolean value) {
        inverted = value;
    }

    public LoggerPanel() {
        initComponents();
        onLafChange();
        model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    comp.setForeground(colorInfo);
                    String cell = (String) table.getModel().getValueAt(row, 3);
                    if (Level.SEVERE.toString().equalsIgnoreCase(cell)) {
                        comp.setForeground(colorError);
                    } else if (Level.WARNING.toString().equalsIgnoreCase(cell)) {
                        comp.setForeground(colorWarning);
                    }
                    return comp;
                }
            });
            column.setPreferredWidth((i == (model.getColumnCount() - 1)) ? 300 : 75);
        }
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

    public void start() {
        Logging.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                try{
                    synchronized(insertLock){
                        addRow(Logging.parseLogRecord(record));
                    }
                    if (!inverted) {                    
                        if (!scrollPane.getVerticalScrollBar().getValueIsAdjusting()) {
                            SwingUtils.scrollToVisible(table, -1, 0);
                        }
                    }
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
    }

    //TODO: load a stream instead
    public void load(Path path) throws FileNotFoundException, IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // process the line.
                String[] data = line.split(Logging.FILE_SEPARATOR);
                //The text contains FILE_SEPARATOR
                if (data.length > 5) {
                    String[] aux = new String[5];
                    System.arraycopy(data, 0, aux, 0, 4);
                    aux[4] = "";
                    for (int i = 4; i < data.length; i++) {
                        aux[4] += data[i];
                        if (i < (data.length - 1)) {
                            aux[4] += Logging.FILE_SEPARATOR;
                        }
                    }
                    data = aux;
                }
                addRow(data);
            }
        }
    }

    public void load(List<String[]> data) {
        for (String[] item : data) {
            addRow(item);
        }

    }

    void addRow(Object[] data) {        
        if (data.length == 5) {
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
        scrollPane.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
