package ch.psi.pshell.swing;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Readable;
import ch.psi.utils.Config;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JTable;

/**
 *
 */
public class DevicePoolParametersEditor extends StandardDialog {

    public DevicePoolParametersEditor(java.awt.Frame parent, boolean modal, Class type, String value, HashMap<String, Class> referencedDevices) {
        super(parent, modal);
        initComponents();
        init(parent, type, value, referencedDevices);
    }

    public DevicePoolParametersEditor(java.awt.Dialog parent, boolean modal, Class type, String value, HashMap<String, Class> referencedDevices) {
        super(parent, modal);
        initComponents();
        init(parent, type, value, referencedDevices);
    }

    ArrayList<Constructor> constructors = new ArrayList<>();
    HashMap<String, Class> referencedDevices;

    void init(Window parent, Class type, String value, HashMap<String, Class> referencedDevices) {
        textPars.setText(value);
        textClass.setText(type.getName());
        this.referencedDevices = referencedDevices;

        for (Constructor c : type.getConstructors()) {
            if (Modifier.isPublic(c.getModifiers())) {
                Parameter[] ps = c.getParameters();
                if ((ps.length > 0) && (ps[0].getType() == String.class)) { //First parameter is name
                    constructors.add(c);
                }
            }
        }

        DefaultListModel model = new DefaultListModel();
        for (Constructor c : constructors) {
            ArrayList<String> pars = new ArrayList<>();
            Parameter[] ps = c.getParameters();
            for (int i = 1; i < ps.length; i++) {
                Parameter p = ps[i];
                Class parType = p.getType();
                String name = p.getName();
                pars.add(name + ":" + parType.getSimpleName());
            }
            model.addElement((pars.size() == 0) ? "\t" : String.join("  ", pars));
        }

        textConstructors.setModel(model);
        textConstructors.setSelectionBackground(new JTable().getSelectionBackground());
        textConstructors.setSelectionForeground(new JTable().getSelectionForeground());
        textConstructors.setBackground(CodeEditor.TEXT_DISABLED_BACKGROUND_COLOR);
        textClass.setBackground(CodeEditor.TEXT_DISABLED_BACKGROUND_COLOR);
        SwingUtils.centerComponent(parent, this);
        update();
    }

    public String getValue() {
        return textPars.getText().trim();
    }

    private final AtomicBoolean updating = new AtomicBoolean(false);

    void update() {
        if (updating.compareAndSet(false, true)) {
            try {
                textConstructors.clearSelection();
                String[] pars = getValue().isEmpty() ? new String[0] : getValue().split(" ");

                for (int i = 0; i < constructors.size(); i++) {
                    Constructor c = constructors.get(i);
                    boolean match = false;
                    try {
                        if (c.getParameterCount() == (pars.length + 1)) {
                            match = true;
                            for (int j = 0; j < pars.length; j++) {
                                Class parType = c.getParameterTypes()[j + 1];
                                if ((GenericDevice.class.isAssignableFrom(parType))
                                        || (Writable.class.isAssignableFrom(parType))
                                        || (Readable.class.isAssignableFrom(parType))) {
                                    Class cls = referencedDevices.get(pars[j]);
                                    if ((cls == null) || !parType.isAssignableFrom(cls)) {
                                        match = false;
                                    }
                                } else if (parType.isArray()) {
                                    match = false;
                                } else {
                                    if (Config.fromString(parType, pars[j]) == null) {
                                        match = false;
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(DevicePoolParametersEditor.class.getName()).log(Level.WARNING, null, ex);
                    }
                    if (match) {
                        textConstructors.addSelectionInterval(i, i);
                    }
                }
            } finally {
                updating.set(false);
            }
            buttonOk.setEnabled(textConstructors.getSelectedIndices().length > 0);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textClass = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textConstructors = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        textPars = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Device Parameters Editor");

        buttonOk.setText("Ok");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Class:");

        textClass.setEditable(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Constructors:");

        textConstructors.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                textConstructorsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(textConstructors);

        jLabel3.setText("Parameters:");

        textPars.setToolTipText("");
        textPars.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textParsKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 191, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addGap(33, 33, 33)
                        .addComponent(buttonCancel)
                        .addGap(0, 191, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textClass))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1)
                            .addComponent(textPars))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textPars, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        try {
            accept();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void textConstructorsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_textConstructorsValueChanged
        update();
    }//GEN-LAST:event_textConstructorsValueChanged

    private void textParsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textParsKeyReleased
        update();
    }//GEN-LAST:event_textParsKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField textClass;
    private javax.swing.JList textConstructors;
    private javax.swing.JTextField textPars;
    // End of variables declaration//GEN-END:variables
}
