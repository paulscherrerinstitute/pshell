package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.xscan.model.Configuration;
import ch.psi.pshell.xscan.model.Data;
import ch.psi.pshell.xscan.model.Variable;
import ch.psi.pshell.xscan.model.Visualization;
import java.awt.Component;
import javax.swing.JFormattedTextField;

/**
 *
 */
public class ConfigurationPanel extends EditablePanel<Configuration> {

    final Configuration configuration;

    public ConfigurationPanel() {
        this(new Configuration());
    }

    /**
     * Creates new form ConfiguratioPanel
     */
    public ConfigurationPanel(final Configuration configuration) {
        super(configuration);        
        this.configuration = configuration;
        if (configuration.getScan() == null) {
            configuration.setScan(configuration.getScan());
        }
        // Correct number of execution if number of execution <=0
        if (configuration.getNumberOfExecution() <= 0) {
            configuration.setNumberOfExecution(1);
        }
        if (configuration.getData() == null) {
            configuration.setData(new Data());
        }

        initComponents();
        jLabel1.setText("Executions:");

        setManagedFields(jButton1, new Component[0], new Component[]{collapsibleListContainerVisualization});
        setManagedFields(jButton2, new Component[0], new Component[]{collapsibleListContainerNotification, collapsibleListContainerVariables, collapsibleListContainerDescription});

        // Establish bindings
        bindEditor(jFormattedTextFieldNumber, "numberOfExecution", false, 1);
        bindEditor(jCheckBoxFailOnSensorError, "failOnSensorError");
        bindEditor(jTextFieldFileName, configuration.getData(), "fileName");
        bindEditor(jComboBoxFormat, configuration.getData(), "format");
        
        try{
            if (!Context.getInstance().getConfig().fdaSerialization){
                jComboBoxFormat.setVisible(false);
                labelFormat.setVisible(false);
            }
        } catch (Exception ex){            
        }

        collapsibleListContainerVisualization.setHeader("Visualizations");
        collapsibleListContainerVisualization.setName("Visualizations");
        collapsibleListContainerVisualization.setIcon("chart_line");

        collapsibleListContainerNotification.setHeader("Notifications");
        collapsibleListContainerNotification.setName("Notifications");

        collapsibleListContainerDescription.setHeader("Description");
        collapsibleListContainerDescription.setName("Description");

        collapsibleListContainerVariables.setHeader("Variables");
        collapsibleListContainerVariables.setName("Variables");

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jFormattedTextFieldNumber = new JFormattedTextField(FieldUtilities.getIntegerFormat());
        jLabel3 = new javax.swing.JLabel();
        jTextFieldFileName = new javax.swing.JTextField();
        labelFormat = new javax.swing.JLabel();
        jComboBoxFormat = new javax.swing.JComboBox();
        jCheckBoxFailOnSensorError = new javax.swing.JCheckBox();
        scanPanel1 = new ScanPanel(configuration.getScan());
        collapsibleListContainerVisualization = new CollapsibleListContainer<Visualization>(new VisualizationListItemProvider(configuration.getVisualization()));
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        collapsibleListContainerNotification = new CollapsibleListContainer<String>(new RecipientListItemProvider(configuration));
        jPanelOptionalNotificaton = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        collapsibleListContainerVariables = new CollapsibleListContainer<Variable>(new GlobalVariableListItemProvider(configuration.getVariable()));
        collapsibleListContainerDescription = new CollapsibleListContainer<String>(new DescriptionListItemProvider(configuration));

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setLabelFor(jFormattedTextFieldNumber);
        jLabel1.setText("Number of Execution:");
        jPanel3.add(jLabel1);

        jFormattedTextFieldNumber.setToolTipText("Number of executions");
        jFormattedTextFieldNumber.setName("Number of Execution"); // NOI18N
        jFormattedTextFieldNumber.setPreferredSize(new java.awt.Dimension(40, 28));
        jPanel3.add(jFormattedTextFieldNumber);

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel3.setLabelFor(jTextFieldFileName);
        jLabel3.setText("Type:");
        jPanel3.add(jLabel3);

        jTextFieldFileName.setPreferredSize(new java.awt.Dimension(120, 28));
        jPanel3.add(jTextFieldFileName);

        labelFormat.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/xscan/ui/Bundle"); // NOI18N
        labelFormat.setText(bundle.getString("ConfigurationPanel.labelFormat.text")); // NOI18N
        jPanel3.add(labelFormat);

        jComboBoxFormat.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "txt" }));
        jPanel3.add(jComboBoxFormat);

        jCheckBoxFailOnSensorError.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jCheckBoxFailOnSensorError.setText("Fail on Sensor Error");
        jPanel3.add(jCheckBoxFailOnSensorError);

        scanPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 153)));

        collapsibleListContainerVisualization.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 0, 153)));

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        jPanelOptionalNotificaton.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jPanelOptionalNotificaton.add(jButton2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(collapsibleListContainerDescription, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(collapsibleListContainerNotification, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(jPanelOptionalNotificaton, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(collapsibleListContainerVariables, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(scanPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(collapsibleListContainerVisualization, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapsibleListContainerDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapsibleListContainerNotification, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelOptionalNotificaton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapsibleListContainerVariables, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scanPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapsibleListContainerVisualization, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerDescription;
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerNotification;
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerVariables;
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerVisualization;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBoxFailOnSensorError;
    private javax.swing.JComboBox jComboBoxFormat;
    private javax.swing.JFormattedTextField jFormattedTextFieldNumber;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelOptionalNotificaton;
    private javax.swing.JTextField jTextFieldFileName;
    private javax.swing.JLabel labelFormat;
    private ch.psi.pshell.xscan.ui.ScanPanel scanPanel1;
    // End of variables declaration//GEN-END:variables

    /**
     * WORKAROUND Set the name of the file ()
     *
     * @param string
     */
    public void setFileName(String string) {
        jTextFieldFileName.setText(string);
    }
}
