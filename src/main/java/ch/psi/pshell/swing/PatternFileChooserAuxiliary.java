package ch.psi.pshell.swing;

import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Context;
import ch.psi.utils.Config;
import ch.psi.utils.IO;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PatternFileChooserAuxiliary extends JPanel {

    final GridBagConstraints gbc = new GridBagConstraints();
    final JFileChooser chooser;
    final String tokenName;
    JCheckBox checkStdPath;
    String selectedPath;
    JTextField textFile;
    JComboBox comboFormat;
    JComboBox comboLayout;

    public PatternFileChooserAuxiliary(JFileChooser chooser, String tokenName, boolean usePatternSelected) {
        this.chooser = chooser;
        this.tokenName = tokenName;
        String root = "{data}";
        String path = (Context.getInstance() != null) ? Context.getInstance().getSetup().expandPath(root) : Sys.getUserHome();
        chooser.setCurrentDirectory(new File(path));

        this.setLayout(new GridBagLayout());
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.weightx = 1.0;
        gbc.ipady = 10;        

        if (Context.getInstance() != null) {
            selectedPath = Context.getInstance().getConfig().dataPath;
            checkStdPath = new JCheckBox("Use name pattern");
            ActionListener listener =(e) -> {
                boolean stdPath = checkStdPath.isSelected();
                if (stdPath) {
                    if (textFile == null) {
                        for (Component child : SwingUtils.getComponentsByType(chooser, JTextField.class)) {
                            textFile = (JTextField) child;
                            textFile.addKeyListener(new java.awt.event.KeyAdapter() {
                                public void keyReleased(java.awt.event.KeyEvent evt) {
                                    selectedPath = textFile.getText();
                                }
                            });
                            break;
                        }
                    }
                    if (textFile != null) {
                        textFile.setText(Context.getInstance().getConfig().dataPath);
                    }
                } else {
                    if (textFile != null) {
                        textFile.setText("");
                    }
                    chooser.setCurrentDirectory(new File(path));
                }
                for (Component child : SwingUtils.getComponentsByType(chooser, JList.class)) {
                    child.setEnabled(!stdPath);
                    ((JList)child).updateUI();   
                    child.revalidate();
                    child.repaint();                                        
                }
            };
            checkStdPath.addActionListener(listener);
            addComponent(checkStdPath);
            if (usePatternSelected) {
                checkStdPath.setSelected(true);
                listener.actionPerformed(null);
            }
        }
    }

    public void addComponent(Component component) {
        if (this.getComponentCount()==0){
            setBorder(new EmptyBorder(0, 10, 0, 10));
        }
        add(component, gbc);
    }

    public void addFormat(String[] formats) {
        try {
            JLabel labelFormat = new JLabel("Format:");
            labelFormat.setHorizontalAlignment(SwingConstants.CENTER);
            comboFormat = new JComboBox();
            if (formats==null){
                formats = Configuration.class.getField("dataProvider").getAnnotation(Config.Defaults.class).values();
            }
            comboFormat.setModel(new DefaultComboBoxModel(formats));
            addComponent(labelFormat);
            addComponent(comboFormat);

        } catch (Exception ex) {
            Logger.getLogger(PatternFileChooserAuxiliary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addLayout(String[] layouts) {
        try {
            JLabel labelLayout = new JLabel("Layout:");
            labelLayout.setHorizontalAlignment(SwingConstants.CENTER);
            if (layouts==null){
                layouts = Configuration.class.getField("dataLayout").getAnnotation(Config.Defaults.class).values();
            }
            comboLayout = new JComboBox();
            comboLayout.setModel(new DefaultComboBoxModel(layouts));
            addComponent(labelLayout);
            addComponent(comboLayout);
        } catch (Exception ex) {
            Logger.getLogger(PatternFileChooserAuxiliary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getSelectedFile() {
        String fileName = chooser.getSelectedFile().getAbsolutePath();
        if ((fileName != null) && (!fileName.isBlank())) {
            if ((checkStdPath != null) && checkStdPath.isSelected()) {
                fileName = Context.getInstance().getSetup().expandPath(selectedPath.trim().replace("{name}", tokenName));
            }

            if (IO.getExtension(fileName).isEmpty()) {
                if (chooser.getFileSelectionMode() == JFileChooser.FILES_ONLY) {
                    FileFilter filter = chooser.getFileFilter();
                    if ((filter != null) && (filter instanceof FileNameExtensionFilter)) {
                        fileName += "." + ((FileNameExtensionFilter) filter).getExtensions()[0];
                    }
                }
            }
            return fileName;
        }
        return null;
    }
    
     public String getFormat() {
         return (comboFormat==null) ? null : String.valueOf(comboFormat.getSelectedItem());
     }
     
     public String getlayout() {
         return  (comboLayout==null) ? null : String.valueOf(comboLayout.getSelectedItem());
     }
               
}
