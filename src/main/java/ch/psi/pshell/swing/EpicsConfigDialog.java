package ch.psi.pshell.swing;

import ch.psi.pshell.epics.Epics;
import ch.psi.utils.Config;
import ch.psi.utils.OrderedProperties;
import ch.psi.utils.Str;
import ch.psi.utils.swing.StandardDialog;
import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.JTextComponent;

/**
 *
 */
public class EpicsConfigDialog extends StandardDialog {

    final OrderedProperties properties;
    boolean readOnly;

    final static String keyAddressList = "ch.psi.jcae.ContextFactory.addressList";
    final static String keyServerPort = "ch.psi.jcae.ContextFactory.serverPort";
    final static String keyMaxArraySize = "ch.psi.jcae.ContextFactory.maxArrayBytes";
    final static String keyMaxSendArraySize = "ch.psi.jcae.ContextFactory.maxSendArrayBytes";
    final static String keyAutoAddressList = "ch.psi.jcae.ContextFactory.autoAddressList";
    final static String keyUseShellVariables = "ch.psi.jcae.ContextFactory.useShellVariables";
    final static String keyLocalBroadcastInterfaces = "ch.psi.jcae.ContextFactory.addLocalBroadcastInterfaces";
    final static String keyConenctionRetries = "ch.psi.jcae.ChannelFactory.retries";
    final static String keyConenctionTimeout = "ch.psi.jcae.ChannelFactory.timeout";
    final static String keyChannelRetries = "ch.psi.jcae.impl.DefaultChannelService.retries";
    final static String keyChannelTimeout = "ch.psi.jcae.impl.DefaultChannelService.timeout";


    public EpicsConfigDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        textAddressList.setFont(textMaxArraySize.getFont());
        this.properties = new OrderedProperties();
        properties.clear();
        try (FileInputStream in = new FileInputStream(Epics.getConfigFile())) {
            properties.load(in);
            textAddressList.setText(Str.removeMultipleSpaces(properties.getProperty(keyAddressList, "")).replace(" ", "\n"));
            textServerPort.setText(properties.getProperty(keyServerPort, ""));
            textMaxArraySize.setText(properties.getProperty(keyMaxArraySize, ""));
            textMaxSendArraySize.setText(properties.getProperty(keyMaxSendArraySize, ""));
            textConenctionRetries.setText(properties.getProperty(keyConenctionRetries, ""));
            textConenctionTimeout.setText(properties.getProperty(keyConenctionTimeout, ""));
            textChannelRetries.setText(properties.getProperty(keyChannelRetries, ""));
            textChannelTimeout.setText(properties.getProperty(keyChannelTimeout, ""));
            textChannelTimeout.setText(properties.getProperty(keyChannelTimeout, ""));
            checkAutoAddressList.setSelected(!properties.getProperty(keyAutoAddressList, "").equalsIgnoreCase("false"));
            checkShellVatiables.setSelected(properties.getProperty(keyUseShellVariables, "").equalsIgnoreCase("true"));
            checkAddLocalBroadcast.setSelected(properties.getProperty(keyLocalBroadcastInterfaces, "").equalsIgnoreCase("true"));
            //textWaitTimeout.setText(properties.getProperty(keyWaitTimeout, ""));
            //textWaitRetryPeriod.setText(properties.getProperty(keyWaitRetryPeriod, ""));
        } catch (Exception ex) {
            Logger.getLogger(Config.class.getName()).log(Level.INFO, null, ex);
        }
    }

    void save() throws Exception {
        properties.setProperty(keyAddressList, textAddressList.getText().replace("\n", " ").trim());
        properties.setProperty(keyServerPort, textServerPort.getText().trim());
        properties.setProperty(keyMaxArraySize, textMaxArraySize.getText().trim());
        properties.setProperty(keyMaxSendArraySize, textMaxSendArraySize.getText().trim());
        properties.setProperty(keyConenctionRetries, textConenctionRetries.getText().trim());
        properties.setProperty(keyConenctionTimeout, textConenctionTimeout.getText().trim());
        properties.setProperty(keyChannelRetries, textChannelRetries.getText().trim());
        properties.setProperty(keyChannelTimeout, textChannelTimeout.getText().trim());
        properties.setProperty(keyAutoAddressList, String.valueOf(checkAutoAddressList.isSelected()));
        properties.setProperty(keyUseShellVariables, String.valueOf(checkShellVatiables.isSelected()));
        properties.setProperty(keyLocalBroadcastInterfaces, String.valueOf(checkAddLocalBroadcast.isSelected()));
        try (FileOutputStream out = new FileOutputStream(Epics.getConfigFile())) {
            properties.store(out, null);
        }
    }

    public void setReadOnly(boolean value) {
        readOnly = value;
        for (Component c : this.getContentPane().getComponents()) {
            if (c instanceof JTextComponent) {
                ((JTextComponent) c).setEditable(!value);
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

        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textMaxArraySize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        textConenctionTimeout = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textConenctionRetries = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textChannelTimeout = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        textChannelRetries = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        textAddressList = new javax.swing.JTextArea();
        jLabel7 = new javax.swing.JLabel();
        textServerPort = new javax.swing.JTextField();
        checkShellVatiables = new javax.swing.JCheckBox();
        checkAutoAddressList = new javax.swing.JCheckBox();
        checkAddLocalBroadcast = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();
        textMaxSendArraySize = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOk.setText("Ok");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Address list:");

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Max array size:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Connection timeout:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Connection retries:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Channel timeout:");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Channel retries:");

        textAddressList.setColumns(20);
        textAddressList.setLineWrap(true);
        textAddressList.setRows(5);
        jScrollPane1.setViewportView(textAddressList);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Server port:");

        checkShellVatiables.setText("Use shell variables");

        checkAutoAddressList.setText("Auto address list");

        checkAddLocalBroadcast.setText("Add local broadcast interfaces");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Max send array size:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textMaxArraySize))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textConenctionRetries))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textChannelTimeout))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addGap(33, 33, 33)
                        .addComponent(buttonCancel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textServerPort)
                            .addComponent(textConenctionTimeout)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textChannelRetries)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkAutoAddressList)
                                    .addComponent(checkShellVatiables)
                                    .addComponent(checkAddLocalBroadcast))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textMaxSendArraySize)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6, jLabel8});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(textServerPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textMaxArraySize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(textMaxSendArraySize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textConenctionTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textConenctionRetries, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(textChannelTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(textChannelRetries, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkShellVatiables)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkAutoAddressList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkAddLocalBroadcast)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        try {
            if (!readOnly) {
                save();
            }
            accept();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JCheckBox checkAddLocalBroadcast;
    private javax.swing.JCheckBox checkAutoAddressList;
    private javax.swing.JCheckBox checkShellVatiables;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea textAddressList;
    private javax.swing.JTextField textChannelRetries;
    private javax.swing.JTextField textChannelTimeout;
    private javax.swing.JTextField textConenctionRetries;
    private javax.swing.JTextField textConenctionTimeout;
    private javax.swing.JTextField textMaxArraySize;
    private javax.swing.JTextField textMaxSendArraySize;
    private javax.swing.JTextField textServerPort;
    // End of variables declaration//GEN-END:variables
}
