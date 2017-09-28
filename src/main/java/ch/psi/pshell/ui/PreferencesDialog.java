package ch.psi.pshell.ui;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.SurfacePlot;
import ch.psi.pshell.plot.TimePlot;
import ch.psi.pshell.swing.DevicePoolPanel.DefaultPanel;
import ch.psi.pshell.plotter.PlotLayout;
import ch.psi.pshell.ui.Preferences.PanelLocation;
import ch.psi.pshell.ui.Preferences.ScriptPopupDialog;
import ch.psi.utils.swing.FontDialog;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/**
 * Editing dialog to Workbench displaying preferences.
 */
public class PreferencesDialog extends StandardDialog {

    final DefaultTableModel modelPanels;
    Color selectedEditorBackground;
    Color selectedEditorForeground;
    Color selectedPlotBackground;

    public PreferencesDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        setTitle("Preferences");
        initComponents();

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        try {
            for (LinePlot plot : ServiceLoader.load(LinePlot.class)) {
                model.addElement(plot.getClass().getName());
            }
        } catch (Throwable ex) {
            Logger.getLogger(PreferencesDialog.class.getName()).log(Level.WARNING, null, ex);
        }
        comboLinePlot.setModel(model);

        model = new DefaultComboBoxModel();
        try {
            for (MatrixPlot plot : ServiceLoader.load(MatrixPlot.class)) {
                model.addElement(plot.getClass().getName());
            }
        } catch (Throwable ex) {
            Logger.getLogger(PreferencesDialog.class.getName()).log(Level.WARNING, null, ex);
        }
        comboMatrixPlot.setModel(model);

        model = new DefaultComboBoxModel();
        try {
            for (SurfacePlot plot : ServiceLoader.load(SurfacePlot.class)) {
                model.addElement(plot.getClass().getName());
            }
        } catch (Throwable ex) {
            Logger.getLogger(PreferencesDialog.class.getName()).log(Level.WARNING, null, ex);
        }
        comboSurfacePlot.setModel(model);
        model = new DefaultComboBoxModel();
        try {
            for (TimePlot plot : ServiceLoader.load(TimePlot.class)) {
                model.addElement(plot.getClass().getName());
            }
        } catch (Throwable ex) {
            Logger.getLogger(PreferencesDialog.class.getName()).log(Level.WARNING, null, ex);
        }
        comboTimePlot.setModel(model);

        SwingUtils.setEnumCombo(comboQuality, Quality.class);
        SwingUtils.setEnumCombo(comboLayout, PlotLayout.class);
        SwingUtils.setEnumCombo(comboConsoleLocation, PanelLocation.class);
        SwingUtils.setEnumCombo(comboColormapPlot, Colormap.class);
        SwingUtils.setEnumCombo(comboScriptPopup, ScriptPopupDialog.class);

        modelPanels = (DefaultTableModel) tablePanels.getModel();
        updateTablePanels();

    }

    public void setReadOnly(boolean value) {
        buttonOk.setEnabled(!value);
    }

    public boolean isReadOnly() {
        return !buttonOk.isEnabled();
    }

    Preferences preferences;
    Font[] selectedFonts = new Font[4];

    String getFontDesc(Font f) {
        StringBuilder sb = new StringBuilder();
        sb.append(f.getName());
        sb.append(" ").append(f.getSize());
        if (f.isBold()) {
            sb.append(" ").append("bold");
        }
        if (f.isItalic()) {
            sb.append(" ").append("italic");
        }
        return sb.toString();
    }

    @Override
    protected void onOpened() {
        if (MainFrame.isDark()) {
            ////TODO: Repeating the model initialization as a workaround for the exception when spinner getting focus on Darcula LAF.
            spinnerTab.setModel(new javax.swing.SpinnerNumberModel(4, 0, 24, 1));
            spinnerContentWidth.setModel(new javax.swing.SpinnerNumberModel(0, 0, 50000, 100));
            spinnerMarkerSize.setModel(new javax.swing.SpinnerNumberModel(2, 1, 12, 1));
        }
    }

    public void set(Preferences preferences) throws Exception {
        this.preferences = preferences;
        spinnerTab.setValue(Integer.valueOf(preferences.tabSize));
        spinnerContentWidth.setValue(Integer.valueOf(preferences.contentWidth));
        selectedEditorBackground = preferences.editorBackground;
        panelEditorBackground.setBackground(selectedEditorBackground);
        selectedEditorForeground = preferences.editorForeground;
        checkSyntaxHighlight.setSelected(!preferences.simpleEditor);
        checkShowRowNumbers.setSelected(!preferences.hideEditorLineNumbers);
        checkEditorContextMenu.setSelected(!preferences.hideEditorContextMenu);
        panelEditorForeground.setBackground(selectedEditorForeground);
        selectedFonts = new Font[]{preferences.fontShellPanel, preferences.fontEditor, preferences.fontOutput, preferences.fontShellCommand};
        comboConsoleLocation.setSelectedItem(preferences.consoleLocation);
        textSP.setText(getFontDesc(preferences.fontShellPanel));
        textSC.setText(getFontDesc(preferences.fontShellCommand));
        textOP.setText(getFontDesc(preferences.fontOutput));
        textSE.setText(getFontDesc(preferences.fontEditor));
        ckAsyncUpdate.setSelected(preferences.asyncViewersUpdate);
        ckScanPlotDisabled.setSelected(preferences.scanPlotDisabled);
        ckScanTableDisabled.setSelected(preferences.scanTableDisabled);
        checkCachedDataPanel.setSelected(preferences.cachedDataPanel);
        comboLinePlot.setSelectedItem(preferences.linePlot);
        comboMatrixPlot.setSelectedItem(preferences.matrixPlot);
        comboSurfacePlot.setSelectedItem(preferences.surfacePlot);
        comboTimePlot.setSelectedItem(preferences.timePlot);
        comboPlotsLocation.setSelectedIndex(preferences.plotsDetached ? 1 : 0);
        comboQuality.setSelectedItem(preferences.quality);
        comboLayout.setSelectedItem(preferences.plotLayout);
        comboScriptPopup.setSelectedItem(preferences.getScriptPopupDialog());
        if ((preferences.markerSize < (Integer) ((SpinnerNumberModel) spinnerMarkerSize.getModel()).getMinimum())
                || (preferences.markerSize > (Integer) ((SpinnerNumberModel) spinnerMarkerSize.getModel()).getMaximum())) {
            spinnerMarkerSize.setValue(2);
        } else {
            spinnerMarkerSize.setValue(preferences.markerSize);
        }
        selectedPlotBackground = preferences.plotBackground;
        panelBackground.setBackground(selectedPlotBackground);
        comboColormapPlot.setSelectedItem(preferences.defaultPlotColormap);

        ckeckBackgroundRendering.setSelected(preferences.backgroundRendering);
        checkStatusBar.setSelected(preferences.showImageStatusBar);
        checkPersistRendererWindows.setSelected(preferences.persistRendererWindows);

        for (DefaultPanel defaultPanel : preferences.defaultPanels) {
            modelPanels.addRow(new Object[]{defaultPanel.deviceClassName, defaultPanel.panelClassName});
        }
        updateTablePanels();
    }

    void getFont(int index, JTextField txt) {
        FontDialog dlg = new FontDialog(SwingUtils.getFrame(this), true, selectedFonts[index]);
        dlg.setVisible(true);
        if (dlg.getResult()) {
            selectedFonts[index] = dlg.getSelectedFont();
            txt.setText(getFontDesc(selectedFonts[index]));
        }
    }

    private void updateTablePanels() {
        buttonDelete.setEnabled(modelPanels.getRowCount() > 0);
        checkSyntaxHighlightActionPerformed(null);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        textSP = new javax.swing.JTextField();
        buttonSP = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        textSC = new javax.swing.JTextField();
        buttonSC = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        textOP = new javax.swing.JTextField();
        buttonOP = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        textSE = new javax.swing.JTextField();
        buttonSE = new javax.swing.JButton();
        buttonDefaultFonts = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        spinnerTab = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        spinnerContentWidth = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        checkSyntaxHighlight = new javax.swing.JCheckBox();
        checkShowRowNumbers = new javax.swing.JCheckBox();
        checkEditorContextMenu = new javax.swing.JCheckBox();
        jPanel11 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        panelEditorBackground = new javax.swing.JPanel();
        buttonSetEditorBackground = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        panelEditorForeground = new javax.swing.JPanel();
        buttonSetEditorForeground = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        buttonDefaultEditorColors = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        comboConsoleLocation = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        comboPlotsLocation = new javax.swing.JComboBox();
        panelPlots = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        comboLinePlot = new javax.swing.JComboBox();
        comboMatrixPlot = new javax.swing.JComboBox();
        comboSurfacePlot = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        comboTimePlot = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        comboQuality = new javax.swing.JComboBox();
        jLabel11 = new javax.swing.JLabel();
        comboLayout = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        panelBackground = new javax.swing.JPanel();
        buttonSetBackground = new javax.swing.JButton();
        buttonResetBackground = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        spinnerMarkerSize = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        comboColormapPlot = new javax.swing.JComboBox();
        jPanel5 = new javax.swing.JPanel();
        ckeckBackgroundRendering = new javax.swing.JCheckBox();
        checkPersistRendererWindows = new javax.swing.JCheckBox();
        checkStatusBar = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablePanels = new javax.swing.JTable();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonDefaultPanels = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        ckAsyncUpdate = new javax.swing.JCheckBox();
        ckScanPlotDisabled = new javax.swing.JCheckBox();
        ckScanTableDisabled = new javax.swing.JCheckBox();
        checkCachedDataPanel = new javax.swing.JCheckBox();
        jLabel21 = new javax.swing.JLabel();
        comboScriptPopup = new javax.swing.JComboBox();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Console:");

        textSP.setEditable(false);

        buttonSP.setText("Set");
        buttonSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSPActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Command:");

        textSC.setEditable(false);

        buttonSC.setText("Set");
        buttonSC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSCActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Output Panel:");

        textOP.setEditable(false);

        buttonOP.setText("Set");
        buttonOP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOPActionPerformed(evt);
            }
        });

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Script Editor:");

        textSE.setEditable(false);

        buttonSE.setText("Set");
        buttonSE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSEActionPerformed(evt);
            }
        });

        buttonDefaultFonts.setText("Defaults");
        buttonDefaultFonts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultFontsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonDefaultFonts)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textSP, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textSC, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textOP, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textSE, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonSP)
                            .addComponent(buttonSC)
                            .addComponent(buttonOP)
                            .addComponent(buttonSE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel2, jLabel3, jLabel4, jLabel5});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {textOP, textSC, textSE, textSP});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonOP, buttonSC, buttonSE, buttonSP});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textSC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSC))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textOP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonOP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(textSE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSE))
                .addGap(18, 18, 18)
                .addComponent(buttonDefaultFonts)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Fonts", jPanel1);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Tab size:");

        spinnerTab.setModel(new javax.swing.SpinnerNumberModel(4, 0, 24, 1));

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Content width:");

        spinnerContentWidth.setModel(new javax.swing.SpinnerNumberModel(0, 0, 50000, 100));

        jLabel13.setText("(pixels, 0 for default)");

        checkSyntaxHighlight.setText("Syntax Highlight");
        checkSyntaxHighlight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkSyntaxHighlightActionPerformed(evt);
            }
        });

        checkShowRowNumbers.setText("Show Row Numbers");

        checkEditorContextMenu.setText("Context Menu");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkSyntaxHighlight)
                    .addComponent(checkShowRowNumbers)
                    .addComponent(checkEditorContextMenu))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addComponent(checkSyntaxHighlight)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkShowRowNumbers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkEditorContextMenu)
                .addGap(0, 0, 0))
        );

        panelEditorBackground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout panelEditorBackgroundLayout = new javax.swing.GroupLayout(panelEditorBackground);
        panelEditorBackground.setLayout(panelEditorBackgroundLayout);
        panelEditorBackgroundLayout.setHorizontalGroup(
            panelEditorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 35, Short.MAX_VALUE)
        );
        panelEditorBackgroundLayout.setVerticalGroup(
            panelEditorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 17, Short.MAX_VALUE)
        );

        buttonSetEditorBackground.setText("Set");
        buttonSetEditorBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetEditorBackgroundActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(panelEditorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonSetEditorBackground))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(panelEditorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSetEditorBackground))
                .addGap(0, 0, 0))
        );

        panelEditorForeground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout panelEditorForegroundLayout = new javax.swing.GroupLayout(panelEditorForeground);
        panelEditorForeground.setLayout(panelEditorForegroundLayout);
        panelEditorForegroundLayout.setHorizontalGroup(
            panelEditorForegroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 35, Short.MAX_VALUE)
        );
        panelEditorForegroundLayout.setVerticalGroup(
            panelEditorForegroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 17, Short.MAX_VALUE)
        );

        buttonSetEditorForeground.setText("Set");
        buttonSetEditorForeground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetEditorForegroundActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(panelEditorForeground, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonSetEditorForeground))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(panelEditorForeground, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSetEditorForeground))
                .addGap(0, 0, 0))
        );

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel16.setText("Background:");

        buttonDefaultEditorColors.setText("Defaults");
        buttonDefaultEditorColors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultEditorColorsActionPerformed(evt);
            }
        });

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Foreground:");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonDefaultEditorColors)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel16)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel17)
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(buttonDefaultEditorColors))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spinnerTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(spinnerContentWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel13))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(60, 60, 60)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel12});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerContentWidth, spinnerTab});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spinnerTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(spinnerContentWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Editor", jPanel2);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText("Console Location:");

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel20.setText("Plots Location:");

        comboPlotsLocation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Internal", "Detached" }));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboConsoleLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboPlotsLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(314, Short.MAX_VALUE))
        );

        jPanel9Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel14, jLabel20});

        jPanel9Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {comboConsoleLocation, comboPlotsLocation});

        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboConsoleLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboPlotsLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20))
                .addContainerGap(148, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Layout", jPanel9);

        panelPlots.setMinimumSize(new java.awt.Dimension(500, 230));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Line Plot:");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Matrix Plot:");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Surface Plot:");

        comboLinePlot.setEditable(true);

        comboMatrixPlot.setEditable(true);

        comboSurfacePlot.setEditable(true);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Time Plot:");

        comboTimePlot.setEditable(true);

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Quality:");

        comboQuality.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Layout:");

        comboLayout.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));

        panelBackground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelBackground.setPreferredSize(new java.awt.Dimension(44, 23));

        javax.swing.GroupLayout panelBackgroundLayout = new javax.swing.GroupLayout(panelBackground);
        panelBackground.setLayout(panelBackgroundLayout);
        panelBackgroundLayout.setHorizontalGroup(
            panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
        panelBackgroundLayout.setVerticalGroup(
            panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel4.add(panelBackground);

        buttonSetBackground.setText("Set");
        buttonSetBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetBackgroundActionPerformed(evt);
            }
        });
        jPanel4.add(buttonSetBackground);

        buttonResetBackground.setText("Default");
        buttonResetBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetBackgroundActionPerformed(evt);
            }
        });

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Background:");

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel18.setText("Marker size:");

        spinnerMarkerSize.setModel(new javax.swing.SpinnerNumberModel(2, 1, 12, 1));

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel19.setText("Colormap:");

        comboColormapPlot.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout panelPlotsLayout = new javax.swing.GroupLayout(panelPlots);
        panelPlots.setLayout(panelPlotsLayout);
        panelPlotsLayout.setHorizontalGroup(
            panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPlotsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(comboSurfacePlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboMatrixPlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboTimePlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(comboLinePlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPlotsLayout.createSequentialGroup()
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(comboQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 116, Short.MAX_VALUE)
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(panelPlotsLayout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonResetBackground))
                            .addGroup(panelPlotsLayout.createSequentialGroup()
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboColormapPlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerMarkerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel6, jLabel7, jLabel8, jLabel9});

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11});

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {comboLayout, comboQuality});

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel15, jLabel18});

        panelPlotsLayout.setVerticalGroup(
            panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPlotsLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(comboLinePlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(comboMatrixPlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(comboSurfacePlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(comboTimePlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(comboQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(buttonResetBackground))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel19)
                    .addComponent(comboColormapPlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(spinnerMarkerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {comboColormapPlot, jLabel19});

        jTabbedPane1.addTab("Plots", panelPlots);

        ckeckBackgroundRendering.setText("Background rendering");

        checkPersistRendererWindows.setText("Persist renderer windows");

        checkStatusBar.setText("Show status bar");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkStatusBar)
                    .addComponent(checkPersistRendererWindows)
                    .addComponent(ckeckBackgroundRendering))
                .addContainerGap(329, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(ckeckBackgroundRendering)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkPersistRendererWindows)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkStatusBar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Imaging", jPanel5);

        tablePanels.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Device Class", "Default Panel Class"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tablePanels.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablePanels.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tablePanels);

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonDefaultPanels.setText("Defaults");
        buttonDefaultPanels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultPanelsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(buttonDefaultPanels)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDelete)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonDefaultPanels))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Panels", jPanel6);

        ckAsyncUpdate.setText("Asynchronous update");

        ckScanPlotDisabled.setText("Scan plots disabled");

        ckScanTableDisabled.setText("Scan table disabled");

        checkCachedDataPanel.setText("Data panel cached");

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel21.setText("Script popup dialog:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ckScanPlotDisabled)
                            .addComponent(ckScanTableDisabled)
                            .addComponent(checkCachedDataPanel))
                        .addContainerGap(374, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(ckAsyncUpdate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboScriptPopup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(comboScriptPopup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel21))
                    .addComponent(ckAsyncUpdate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkCachedDataPanel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ckScanPlotDisabled)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ckScanTableDisabled)
                .addContainerGap(79, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", jPanel3);

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonOk)
                .addGap(33, 33, 33)
                .addComponent(buttonCancel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jTabbedPane1)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        try {
            if (preferences != null) {
                //First make access that may throw exceptions before starting changing preferences
                ArrayList<DefaultPanel> panels = new ArrayList<>();
                for (int i = 0; i < modelPanels.getRowCount(); i++) {
                    DefaultPanel defaultPanel = new DefaultPanel((String) modelPanels.getValueAt(i, 0), (String) modelPanels.getValueAt(i, 1));
                    defaultPanel.getDeviceClass();
                    defaultPanel.getPanelClass();
                    panels.add(defaultPanel);
                }
                Quality quality = (Quality) comboQuality.getSelectedItem();
                PlotLayout plotLayout = (PlotLayout) comboLayout.getSelectedItem();
                ScriptPopupDialog scriptPopup = (ScriptPopupDialog) comboScriptPopup.getSelectedItem();

                //Then update preferences                
                preferences.fontShellPanel = selectedFonts[0];
                preferences.fontEditor = selectedFonts[1];
                preferences.fontOutput = selectedFonts[2];
                preferences.fontShellCommand = selectedFonts[3];
                preferences.tabSize = (Integer) spinnerTab.getValue();
                preferences.contentWidth = (Integer) spinnerContentWidth.getValue();
                preferences.editorBackground = selectedEditorBackground;
                preferences.editorForeground = selectedEditorForeground;
                preferences.simpleEditor = !checkSyntaxHighlight.isSelected();
                preferences.hideEditorLineNumbers = !checkShowRowNumbers.isSelected();
                preferences.hideEditorContextMenu = !checkEditorContextMenu.isSelected();

                preferences.consoleLocation = (PanelLocation) comboConsoleLocation.getSelectedItem();
                preferences.asyncViewersUpdate = ckAsyncUpdate.isSelected();
                preferences.scanPlotDisabled = ckScanPlotDisabled.isSelected();
                preferences.scanTableDisabled = ckScanTableDisabled.isSelected();
                preferences.cachedDataPanel = checkCachedDataPanel.isSelected();
                preferences.linePlot = String.valueOf(comboLinePlot.getSelectedItem());
                preferences.matrixPlot = String.valueOf(comboMatrixPlot.getSelectedItem());
                preferences.surfacePlot = String.valueOf(comboSurfacePlot.getSelectedItem());
                preferences.timePlot = String.valueOf(comboTimePlot.getSelectedItem());
                preferences.plotsDetached = (comboPlotsLocation.getSelectedIndex() == 1);
                preferences.plotLayout = plotLayout;
                preferences.scriptPopupDialog = scriptPopup;
                preferences.quality = quality;
                preferences.markerSize = (Integer) spinnerMarkerSize.getValue();
                preferences.plotBackground = selectedPlotBackground;
                preferences.defaultPlotColormap = (Colormap) comboColormapPlot.getSelectedItem();
                preferences.backgroundRendering = ckeckBackgroundRendering.isSelected();
                preferences.showImageStatusBar = checkStatusBar.isSelected();
                preferences.persistRendererWindows = checkPersistRendererWindows.isSelected();
                preferences.defaultPanels = panels.toArray(new DefaultPanel[0]);
            }
            accept();

        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSPActionPerformed
        getFont(0, textSP);
    }//GEN-LAST:event_buttonSPActionPerformed

    private void buttonSCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSCActionPerformed
        getFont(3, textSC);
    }//GEN-LAST:event_buttonSCActionPerformed

    private void buttonOPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOPActionPerformed
        getFont(2, textOP);
    }//GEN-LAST:event_buttonOPActionPerformed

    private void buttonSEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSEActionPerformed
        getFont(1, textSE);
    }//GEN-LAST:event_buttonSEActionPerformed

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        try {
            modelPanels.insertRow(tablePanels.getSelectedRow() + 1, new Object[]{"", ""});
            updateTablePanels();
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        try {
            if (tablePanels.getSelectedRow() >= 0) {
                modelPanels.removeRow(tablePanels.getSelectedRow());
                updateTablePanels();
            }
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonDefaultPanelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultPanelsActionPerformed
        try {
            modelPanels.setRowCount(0);
            for (DefaultPanel defaultPanel : Preferences.getDefaultPanels()) {
                modelPanels.addRow(new Object[]{defaultPanel.deviceClassName, defaultPanel.panelClassName});
            }
            updateTablePanels();
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonDefaultPanelsActionPerformed

    private void buttonDefaultFontsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultFontsActionPerformed
        try {
            Font[] fonts = Preferences.getDefaultFonts();
            for (int i = 0; i < 4; i++) {
                selectedFonts[i] = fonts[i];
            }
            textSP.setText(getFontDesc(fonts[0]));
            textSE.setText(getFontDesc(fonts[1]));
            textOP.setText(getFontDesc(fonts[2]));
            textSC.setText(getFontDesc(fonts[3]));
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonDefaultFontsActionPerformed

    private void buttonSetBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetBackgroundActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", selectedPlotBackground); //preferences.backgroundColor);            
            if (c != null) {
                selectedPlotBackground = c;
                panelBackground.setBackground(selectedPlotBackground);
            }
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonSetBackgroundActionPerformed

    private void buttonSetEditorBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetEditorBackgroundActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", selectedEditorBackground);
            if (c != null) {
                selectedEditorBackground = c;
                panelEditorBackground.setBackground(selectedEditorBackground);
            }
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonSetEditorBackgroundActionPerformed

    private void buttonSetEditorForegroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetEditorForegroundActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", selectedEditorForeground);
            if (c != null) {
                selectedEditorForeground = c;
                panelEditorForeground.setBackground(selectedEditorForeground);
            }
        } catch (Exception ex) {
            SwingUtils.showException(PreferencesDialog.this, ex);
        }
    }//GEN-LAST:event_buttonSetEditorForegroundActionPerformed

    private void buttonDefaultEditorColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultEditorColorsActionPerformed
        selectedEditorBackground = null;
        panelEditorBackground.setBackground(selectedEditorBackground);
        selectedEditorForeground = null;
        panelEditorForeground.setBackground(selectedEditorForeground);
    }//GEN-LAST:event_buttonDefaultEditorColorsActionPerformed

    private void buttonResetBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetBackgroundActionPerformed
        selectedPlotBackground = null;
        panelBackground.setBackground(selectedPlotBackground);
    }//GEN-LAST:event_buttonResetBackgroundActionPerformed

    private void checkSyntaxHighlightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkSyntaxHighlightActionPerformed
        checkShowRowNumbers.setEnabled(checkSyntaxHighlight.isSelected());
    }//GEN-LAST:event_checkSyntaxHighlightActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonDefaultEditorColors;
    private javax.swing.JButton buttonDefaultFonts;
    private javax.swing.JButton buttonDefaultPanels;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonOP;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonResetBackground;
    private javax.swing.JButton buttonSC;
    private javax.swing.JButton buttonSE;
    private javax.swing.JButton buttonSP;
    private javax.swing.JButton buttonSetBackground;
    private javax.swing.JButton buttonSetEditorBackground;
    private javax.swing.JButton buttonSetEditorForeground;
    private javax.swing.JCheckBox checkCachedDataPanel;
    private javax.swing.JCheckBox checkEditorContextMenu;
    private javax.swing.JCheckBox checkPersistRendererWindows;
    private javax.swing.JCheckBox checkShowRowNumbers;
    private javax.swing.JCheckBox checkStatusBar;
    private javax.swing.JCheckBox checkSyntaxHighlight;
    private javax.swing.JCheckBox ckAsyncUpdate;
    private javax.swing.JCheckBox ckScanPlotDisabled;
    private javax.swing.JCheckBox ckScanTableDisabled;
    private javax.swing.JCheckBox ckeckBackgroundRendering;
    private javax.swing.JComboBox comboColormapPlot;
    private javax.swing.JComboBox comboConsoleLocation;
    private javax.swing.JComboBox comboLayout;
    private javax.swing.JComboBox comboLinePlot;
    private javax.swing.JComboBox comboMatrixPlot;
    private javax.swing.JComboBox comboPlotsLocation;
    private javax.swing.JComboBox comboQuality;
    private javax.swing.JComboBox comboScriptPopup;
    private javax.swing.JComboBox comboSurfacePlot;
    private javax.swing.JComboBox comboTimePlot;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel panelBackground;
    private javax.swing.JPanel panelEditorBackground;
    private javax.swing.JPanel panelEditorForeground;
    private javax.swing.JPanel panelPlots;
    private javax.swing.JSpinner spinnerContentWidth;
    private javax.swing.JSpinner spinnerMarkerSize;
    private javax.swing.JSpinner spinnerTab;
    private javax.swing.JTable tablePanels;
    private javax.swing.JTextField textOP;
    private javax.swing.JTextField textSC;
    private javax.swing.JTextField textSE;
    private javax.swing.JTextField textSP;
    // End of variables declaration//GEN-END:variables
}
