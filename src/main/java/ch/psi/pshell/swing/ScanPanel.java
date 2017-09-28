package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.scan.PlotScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class ScanPanel extends MonitoredPanel {

    public ScanPanel() {
        initComponents();
    }

    public void initialize() {
        onHide();
        setActive(false);
    }

    public void setActive(boolean value) {
        if (value) {
            Context.getInstance().addScanListener(scanListener);
        } else {
            Context.getInstance().removeScanListener(scanListener);
            clear();
        }
    }

    public boolean isActive() {
        return Context.getInstance().getScanListeners().contains(scanListener);
    }

    public void clear() {
        table.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{}));
    }

    ScanListener scanListener = new ScanListener() {
        Scan scan;
        DefaultTableModel model;

        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            if (scan instanceof PlotScan) {
                return;
            }

            ArrayList<String> columns = new ArrayList<>();
            columns.add("Time");
            columns.add("Index");
            for (Writable w : scan.getWritables()) {
                columns.add(Context.getInstance().getDataManager().getAlias(w));
            }
            for (Readable r : scan.getReadables()) {
                columns.add(Context.getInstance().getDataManager().getAlias(r));
            }
            model = new DefaultTableModel(new Object[][]{}, columns.toArray(new String[0])) {
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false;
                }
            };

            table.setModel(model);
            this.scan = scan;
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (scan instanceof PlotScan) {
                return;
            }

            String time = Chrono.getTimeStr(record.getTimestamp(), "HH:mm:ss.SSS");
            if (this.scan == scan) {
                ArrayList values = new ArrayList();
                values.add(time);
                values.add(String.format("%04d", record.getIndex()));
                for (Number value : record.getPositions()) {
                    values.add(value);
                }
                for (Object value : record.getValues()) {
                    if (value == null) {
                        values.add(null);
                    } else if (value.getClass().isArray()) {
                        int[] dims = Arr.getDimensions(value);
                        if (dims.length == 1) {
                            values.add(Convert.arrayToString(value, " ", 1000));
                        } else {
                            values.add(Convert.arrayToString(dims, " x "));
                        }

                    } else if (value instanceof Number) {
                        values.add(value);
                    } else {
                        values.add(null);
                    }
                }
                if (isShowing()) {
                    int rows = model.getRowCount();
                    boolean lastVisible = (rows > 0) && SwingUtils.isCellVisible(table, rows - 1, 0);
                    model.addRow(values.toArray());
                    if (lastVisible) {
                        SwingUtils.scrollToVisible(table, -1, 0);
                    }
                } else {
                    model.addRow(values.toArray());
                }
            }
        }

        @Override

        public void onScanEnded(Scan scan, Exception ex) {
        }
    };

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
