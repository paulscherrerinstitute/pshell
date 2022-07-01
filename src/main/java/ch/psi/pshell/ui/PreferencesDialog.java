package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.SurfacePlot;
import ch.psi.pshell.plot.TimePlot;
import ch.psi.pshell.swing.DevicePoolPanel.DefaultPanel;
import ch.psi.pshell.plotter.PlotLayout;
import ch.psi.pshell.ui.Preferences.FontSpec;
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
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Editing dialog to Workbench displaying preferences.
 */
public class PreferencesDialog extends StandardDialog {

    final DefaultTableModel modelPanels;    
    final DefaultTableModel modelProcessingScripts;
    Integer selectedEditorBackground;
    Integer selectedEditorForeground;
    Integer selectedPlotBackground;
    Integer selectedPlotGrid;
    Integer selectedPlotOutline;

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
        SwingUtils.setEnumCombo(comboDataPanelLocation, PanelLocation.class);
        SwingUtils.setEnumCombo(comboColormapPlot, Colormap.class);
        SwingUtils.setEnumCombo(comboScriptPopup, ScriptPopupDialog.class);

        modelPanels = (DefaultTableModel) tablePanels.getModel();        
        modelProcessingScripts = (DefaultTableModel) tableProcessingScripts.getModel();
        updateTablePanels();
        updateListProcFiles();

    }

    public void setReadOnly(boolean value) {
        buttonOk.setEnabled(!value);
    }

    public boolean isReadOnly() {
        return !buttonOk.isEnabled();
    }

    Preferences preferences;
    FontSpec[] selectedFonts = new FontSpec[7];

    
    String getFontDesc(FontSpec fs) {
        Font f = fs.toFont();
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
        if (MainFrame.isDarcula()) {
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
        panelEditorBackground.setBackground((selectedEditorBackground==null) ? null : new Color(selectedEditorBackground));
        selectedEditorForeground = preferences.editorForeground;
        checkSyntaxHighlight.setSelected(!preferences.simpleEditor);
        checkShowRowNumbers.setSelected(!preferences.hideEditorLineNumbers);
        checkEditorContextMenu.setSelected(!preferences.hideEditorContextMenu);
        panelEditorForeground.setBackground((selectedEditorForeground==null) ? null : new Color(selectedEditorForeground));
        selectedFonts = new FontSpec[]{ preferences.fontShellPanel, 
                                    preferences.fontEditor, 
                                    preferences.fontOutput, 
                                    preferences.fontShellCommand,
                                    preferences.fontPlotLabel,
                                    preferences.fontPlotTick,
                                    preferences.fontPlotTitle,
                                    preferences.fontTerminal
                                  };
        comboConsoleLocation.setSelectedItem(preferences.consoleLocation);
        comboDataPanelLocation.setSelectedItem(preferences.dataPanelLocation);
        radioDataDocTab.setSelected(preferences.openDataFilesInDocTab);
        radioDataEmbedded.setSelected(!preferences.openDataFilesInDocTab);
        textSP.setText(getFontDesc(preferences.fontShellPanel));
        textSC.setText(getFontDesc(preferences.fontShellCommand));
        textOP.setText(getFontDesc(preferences.fontOutput));
        textSE.setText(getFontDesc(preferences.fontEditor));
        textPTit.setText(getFontDesc(preferences.fontPlotTitle));
        textPL.setText(getFontDesc(preferences.fontPlotLabel));
        textPT.setText(getFontDesc(preferences.fontPlotTick));
        textT.setText(getFontDesc(preferences.fontTerminal));
        ckAsyncUpdate.setSelected(preferences.asyncViewersUpdate);
        ckScanPlotEnabled.setSelected(!preferences.scanPlotDisabled);
        ckScanTableEnabled.setSelected(!preferences.scanTableDisabled);
        checkCachedDataPanel.setSelected(preferences.cachedDataPanel);
        textDataExtensions.setText((preferences.dataExtensions==null) ? "" : preferences.dataExtensions);
        checkShowEmergencyStop.setSelected(preferences.showEmergencyStop);
        checkShowHomingButtons.setSelected(preferences.showHomingButtons);
        checkShowJogButtons.setSelected(preferences.showJogButtons); 
        checkOutputPanel.setSelected(!preferences.hideOutputPanel);
        checkScanPanel.setSelected(!preferences.hideScanPanel);        
        checkXScanBrowser.setSelected(preferences.showXScanFileBrowser);     
        checkQueueBrowser.setSelected(preferences.showQueueBrowser);     
        comboLinePlot.setSelectedItem(preferences.linePlot);
        comboMatrixPlot.setSelectedItem(preferences.matrixPlot);
        comboSurfacePlot.setSelectedItem(preferences.surfacePlot);
        comboTimePlot.setSelectedItem(preferences.timePlot);
        comboPlotsLocation.setSelectedIndex(preferences.plotsDetached ? 1 : 0);
        comboQuality.setSelectedItem(preferences.quality);
        comboLayout.setSelectedItem(preferences.plotLayout);
        comboScriptPopup.setSelectedItem(preferences.getScriptPopupDlg());
        if ((preferences.markerSize < (Integer) ((SpinnerNumberModel) spinnerMarkerSize.getModel()).getMinimum())
                || (preferences.markerSize > (Integer) ((SpinnerNumberModel) spinnerMarkerSize.getModel()).getMaximum())) {
            spinnerMarkerSize.setValue(2);
        } else {
            spinnerMarkerSize.setValue(preferences.markerSize);
        }
        checkOffscreenBuffers.setSelected(!preferences.disableOffscreenBuffer);
        selectedPlotBackground = preferences.plotBackground;
        selectedPlotGrid = preferences.gridColor;
        selectedPlotOutline = preferences.outlineColor;
        panelBackground.setBackground((selectedPlotBackground==null) ? null : new Color(selectedPlotBackground));
        panelGrid.setBackground((selectedPlotGrid==null) ? null : new Color(selectedPlotGrid));
        panelOutline.setBackground((selectedPlotOutline==null) ? null : new Color(selectedPlotOutline));
        comboColormapPlot.setSelectedItem(preferences.defaultPlotColormap);

        ckeckBackgroundRendering.setSelected(preferences.backgroundRendering);
        checkStatusBar.setSelected(preferences.showImageStatusBar);
        checkPersistRendererWindows.setSelected(preferences.persistRendererWindows);

        for (DefaultPanel defaultPanel : preferences.defaultPanels) {
            modelPanels.addRow(new Object[]{defaultPanel.deviceClassName, defaultPanel.panelClassName});
        }
        
        for (String plottingScript: preferences.processingScripts) {
            String[] tokens = plottingScript.split("\\|");        
            String file = tokens[0].trim();
            String category = ((tokens.length==1) || (tokens[1].isBlank()))? "" : tokens[1].trim();
            modelProcessingScripts.addRow(new Object[]{file, category});
        }
                
        updateTablePanels();
        updateListProcFiles();
    }

    void getFont(int index, JTextField txt) {
        FontDialog dlg = new FontDialog(getFrame(), true, selectedFonts[index].toFont());
        dlg.setVisible(true);
        if (dlg.getResult()) {
            selectedFonts[index] = FontSpec.fromFont(dlg.getSelectedFont());
            txt.setText(getFontDesc(selectedFonts[index]));
        }
    }

    private void updateTablePanels() {
        buttonDelete.setEnabled(modelPanels.getRowCount() > 0);
        checkSyntaxHighlightActionPerformed(null);
    }
    
    private void updateListProcFiles() {        
        buttonRemoveProcScript.setEnabled(modelProcessingScripts.getRowCount() > 0);
    }    

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        panelFonts = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel17 = new javax.swing.JPanel();
        buttonSC = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        buttonDefaultPT = new javax.swing.JButton();
        buttonDefaultSE = new javax.swing.JButton();
        textSE = new javax.swing.JTextField();
        buttonPT = new javax.swing.JButton();
        buttonDefaultSP = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        textPTit = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        buttonDefaultOP = new javax.swing.JButton();
        buttonPL = new javax.swing.JButton();
        textPT = new javax.swing.JTextField();
        buttonSE = new javax.swing.JButton();
        buttonSP = new javax.swing.JButton();
        jLabel22 = new javax.swing.JLabel();
        buttonDefaultPL = new javax.swing.JButton();
        buttonDefaultPTit = new javax.swing.JButton();
        buttonPTit = new javax.swing.JButton();
        textPL = new javax.swing.JTextField();
        textSP = new javax.swing.JTextField();
        buttonOP = new javax.swing.JButton();
        textSC = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textOP = new javax.swing.JTextField();
        buttonDefaultSC = new javax.swing.JButton();
        jLabel31 = new javax.swing.JLabel();
        textT = new javax.swing.JTextField();
        buttonT = new javax.swing.JButton();
        buttonDefaultT = new javax.swing.JButton();
        panelColors = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        panelGrid = new javax.swing.JPanel();
        buttonSetGrid = new javax.swing.JButton();
        buttonResetGrid = new javax.swing.JButton();
        jLabel28 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        panelOutline = new javax.swing.JPanel();
        buttonOutline = new javax.swing.JButton();
        buttonResetOutline = new javax.swing.JButton();
        jLabel29 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        panelBackground = new javax.swing.JPanel();
        buttonSetBackground = new javax.swing.JButton();
        buttonResetBackground = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        panelEditorBackground = new javax.swing.JPanel();
        buttonSetEditorBackground = new javax.swing.JButton();
        buttonResetTextBg = new javax.swing.JButton();
        jPanel20 = new javax.swing.JPanel();
        panelEditorForeground = new javax.swing.JPanel();
        buttonSetEditorForeground = new javax.swing.JButton();
        buttonResetTextFg = new javax.swing.JButton();
        panelEditor = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        spinnerTab = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        spinnerContentWidth = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jLabel26 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        checkSyntaxHighlight = new javax.swing.JCheckBox();
        checkShowRowNumbers = new javax.swing.JCheckBox();
        checkEditorContextMenu = new javax.swing.JCheckBox();
        panelData = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        buttonInsertProcScript = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableProcessingScripts = new javax.swing.JTable();
        buttonRemoveProcScript = new javax.swing.JButton();
        jLabel30 = new javax.swing.JLabel();
        textDataExtensions = new javax.swing.JTextField();
        checkCachedDataPanel = new javax.swing.JCheckBox();
        jLabel33 = new javax.swing.JLabel();
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
        jLabel18 = new javax.swing.JLabel();
        spinnerMarkerSize = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        comboColormapPlot = new javax.swing.JComboBox();
        checkOffscreenBuffers = new javax.swing.JCheckBox();
        jLabel35 = new javax.swing.JLabel();
        panelLayout = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        checkShowJogButtons = new javax.swing.JCheckBox();
        checkShowEmergencyStop = new javax.swing.JCheckBox();
        checkShowHomingButtons = new javax.swing.JCheckBox();
        jPanel14 = new javax.swing.JPanel();
        checkXScanBrowser = new javax.swing.JCheckBox();
        checkQueueBrowser = new javax.swing.JCheckBox();
        checkOutputPanel = new javax.swing.JCheckBox();
        checkScanPanel = new javax.swing.JCheckBox();
        jPanel15 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        comboConsoleLocation = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        comboDataPanelLocation = new javax.swing.JComboBox();
        comboPlotsLocation = new javax.swing.JComboBox();
        jPanel6 = new javax.swing.JPanel();
        radioDataEmbedded = new javax.swing.JRadioButton();
        radioDataDocTab = new javax.swing.JRadioButton();
        panelPanels = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablePanels = new javax.swing.JTable();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonDefaultPanels = new javax.swing.JButton();
        panelGeneral = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        ckScanTableEnabled = new javax.swing.JCheckBox();
        ckScanPlotEnabled = new javax.swing.JCheckBox();
        ckAsyncUpdate = new javax.swing.JCheckBox();
        comboScriptPopup = new javax.swing.JComboBox();
        jLabel21 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        ckeckBackgroundRendering = new javax.swing.JCheckBox();
        checkStatusBar = new javax.swing.JCheckBox();
        checkPersistRendererWindows = new javax.swing.JCheckBox();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel25.setText("Background:");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(540, 270));

        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        buttonSC.setText("Set");
        buttonSC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSCActionPerformed(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Console:");

        buttonDefaultPT.setText("Default");
        buttonDefaultPT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultPTActionPerformed(evt);
            }
        });

        buttonDefaultSE.setText("Default");
        buttonDefaultSE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultSEActionPerformed(evt);
            }
        });

        textSE.setEditable(false);

        buttonPT.setText("Set");
        buttonPT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPTActionPerformed(evt);
            }
        });

        buttonDefaultSP.setText("Default");
        buttonDefaultSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultSPActionPerformed(evt);
            }
        });

        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel23.setText("Plot Label:");

        textPTit.setEditable(false);

        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel24.setText("Plot Tick:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Output Panel:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Command:");

        buttonDefaultOP.setText("Default");
        buttonDefaultOP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultOPActionPerformed(evt);
            }
        });

        buttonPL.setText("Set");
        buttonPL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPLActionPerformed(evt);
            }
        });

        textPT.setEditable(false);

        buttonSE.setText("Set");
        buttonSE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSEActionPerformed(evt);
            }
        });

        buttonSP.setText("Set");
        buttonSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSPActionPerformed(evt);
            }
        });

        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel22.setText("Plot Title:");

        buttonDefaultPL.setText("Default");
        buttonDefaultPL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultPLActionPerformed(evt);
            }
        });

        buttonDefaultPTit.setText("Default");
        buttonDefaultPTit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultPTitActionPerformed(evt);
            }
        });

        buttonPTit.setText("Set");
        buttonPTit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPTitActionPerformed(evt);
            }
        });

        textPL.setEditable(false);

        textSP.setEditable(false);

        buttonOP.setText("Set");
        buttonOP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOPActionPerformed(evt);
            }
        });

        textSC.setEditable(false);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Script Editor:");

        textOP.setEditable(false);

        buttonDefaultSC.setText("Default");
        buttonDefaultSC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultSCActionPerformed(evt);
            }
        });

        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel31.setText("Terminal");

        textT.setEditable(false);

        buttonT.setText("Set");
        buttonT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTActionPerformed(evt);
            }
        });

        buttonDefaultT.setText("Default");
        buttonDefaultT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultTActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel22)
                    .addComponent(jLabel23)
                    .addComponent(jLabel24)
                    .addComponent(jLabel31))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textSP, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textSC, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textOP, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textSE, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textPTit, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textPL, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textPT, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textT, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(buttonSP)
                    .addComponent(buttonSE)
                    .addComponent(buttonSC)
                    .addComponent(buttonOP)
                    .addComponent(buttonPTit)
                    .addComponent(buttonPL)
                    .addComponent(buttonPT)
                    .addComponent(buttonT))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonDefaultSP)
                    .addComponent(buttonDefaultSC)
                    .addComponent(buttonDefaultOP)
                    .addComponent(buttonDefaultSE)
                    .addComponent(buttonDefaultPTit)
                    .addComponent(buttonDefaultPL)
                    .addComponent(buttonDefaultPT)
                    .addComponent(buttonDefaultT))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel17Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel2, jLabel22, jLabel23, jLabel24, jLabel3, jLabel31, jLabel4, jLabel5});

        jPanel17Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {textOP, textPL, textPT, textPTit, textSC, textSE, textSP, textT});

        jPanel17Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonOP, buttonPL, buttonPT, buttonPTit, buttonSC, buttonSE, buttonSP, buttonT});

        jPanel17Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDefaultOP, buttonDefaultPL, buttonDefaultPT, buttonDefaultPTit, buttonDefaultSC, buttonDefaultSE, buttonDefaultSP, buttonDefaultT});

        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSP)
                    .addComponent(buttonDefaultSP))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textSC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSC)
                    .addComponent(buttonDefaultSC))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textOP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonOP)
                    .addComponent(buttonDefaultOP))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(textSE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSE)
                    .addComponent(buttonDefaultSE))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(textPTit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonPTit)
                    .addComponent(buttonDefaultPTit))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(textPL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonPL)
                    .addComponent(buttonDefaultPL))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(textPT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonPT)
                    .addComponent(buttonDefaultPT))
                .addGap(1, 1, 1)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(textT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonT)
                    .addComponent(buttonDefaultT))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane3.setViewportView(jPanel17);

        javax.swing.GroupLayout panelFontsLayout = new javax.swing.GroupLayout(panelFonts);
        panelFonts.setLayout(panelFontsLayout);
        panelFontsLayout.setHorizontalGroup(
            panelFontsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        panelFontsLayout.setVerticalGroup(
            panelFontsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        jTabbedPane1.addTab("Fonts", panelFonts);

        jPanel18.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));

        panelGrid.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelGrid.setPreferredSize(new java.awt.Dimension(44, 23));

        javax.swing.GroupLayout panelGridLayout = new javax.swing.GroupLayout(panelGrid);
        panelGrid.setLayout(panelGridLayout);
        panelGridLayout.setHorizontalGroup(
            panelGridLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelGridLayout.setVerticalGroup(
            panelGridLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel18.add(panelGrid);

        buttonSetGrid.setText("Set");
        buttonSetGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetGridActionPerformed(evt);
            }
        });
        jPanel18.add(buttonSetGrid);

        buttonResetGrid.setText("Default");
        buttonResetGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetGridActionPerformed(evt);
            }
        });
        jPanel18.add(buttonResetGrid);

        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel28.setText("Plot Outline:");

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Plot Background:");

        jPanel13.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));

        panelOutline.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelOutline.setPreferredSize(new java.awt.Dimension(44, 23));

        javax.swing.GroupLayout panelOutlineLayout = new javax.swing.GroupLayout(panelOutline);
        panelOutline.setLayout(panelOutlineLayout);
        panelOutlineLayout.setHorizontalGroup(
            panelOutlineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelOutlineLayout.setVerticalGroup(
            panelOutlineLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel13.add(panelOutline);

        buttonOutline.setText("Set");
        buttonOutline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOutlineActionPerformed(evt);
            }
        });
        jPanel13.add(buttonOutline);

        buttonResetOutline.setText("Default");
        buttonResetOutline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetOutlineActionPerformed(evt);
            }
        });
        jPanel13.add(buttonResetOutline);

        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel29.setText("Plot Grid:");

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));

        panelBackground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelBackground.setPreferredSize(new java.awt.Dimension(44, 23));

        javax.swing.GroupLayout panelBackgroundLayout = new javax.swing.GroupLayout(panelBackground);
        panelBackground.setLayout(panelBackgroundLayout);
        panelBackgroundLayout.setHorizontalGroup(
            panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
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
        jPanel4.add(buttonResetBackground);

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel16.setText("Text Background:");

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Text Foreground:");

        jPanel19.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));

        panelEditorBackground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelEditorBackground.setPreferredSize(new java.awt.Dimension(44, 23));

        javax.swing.GroupLayout panelEditorBackgroundLayout = new javax.swing.GroupLayout(panelEditorBackground);
        panelEditorBackground.setLayout(panelEditorBackgroundLayout);
        panelEditorBackgroundLayout.setHorizontalGroup(
            panelEditorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelEditorBackgroundLayout.setVerticalGroup(
            panelEditorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel19.add(panelEditorBackground);

        buttonSetEditorBackground.setText("Set");
        buttonSetEditorBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetEditorBackgroundActionPerformed(evt);
            }
        });
        jPanel19.add(buttonSetEditorBackground);

        buttonResetTextBg.setText("Default");
        buttonResetTextBg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetTextBgActionPerformed(evt);
            }
        });
        jPanel19.add(buttonResetTextBg);

        jPanel20.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));

        panelEditorForeground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelEditorForeground.setPreferredSize(new java.awt.Dimension(44, 23));

        javax.swing.GroupLayout panelEditorForegroundLayout = new javax.swing.GroupLayout(panelEditorForeground);
        panelEditorForeground.setLayout(panelEditorForegroundLayout);
        panelEditorForegroundLayout.setHorizontalGroup(
            panelEditorForegroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelEditorForegroundLayout.setVerticalGroup(
            panelEditorForegroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel20.add(panelEditorForeground);

        buttonSetEditorForeground.setText("Set");
        buttonSetEditorForeground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetEditorForegroundActionPerformed(evt);
            }
        });
        jPanel20.add(buttonSetEditorForeground);

        buttonResetTextFg.setText("Default");
        buttonResetTextFg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetTextFgActionPerformed(evt);
            }
        });
        jPanel20.add(buttonResetTextFg);

        javax.swing.GroupLayout panelColorsLayout = new javax.swing.GroupLayout(panelColors);
        panelColors.setLayout(panelColorsLayout);
        panelColorsLayout.setHorizontalGroup(
            panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelColorsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel28, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel29, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelColorsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel15, jLabel16, jLabel17, jLabel28, jLabel29});

        panelColorsLayout.setVerticalGroup(
            panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelColorsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16)
                    .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17)
                    .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addGap(0, 0, 0)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28))
                .addGap(0, 0, 0)
                .addGroup(panelColorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel29))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Colors", panelColors);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Tab size:");

        spinnerTab.setModel(new javax.swing.SpinnerNumberModel(4, 0, 24, 1));

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Content width:");

        spinnerContentWidth.setModel(new javax.swing.SpinnerNumberModel(0, 0, 50000, 100));

        jLabel13.setText("(pixels, 0 for default)");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel26.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel26.setText("Syntax Highlight:");

        jLabel32.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel32.setText("Show Row Numbers:");

        jLabel34.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel34.setText("Context Menu:");

        checkSyntaxHighlight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkSyntaxHighlightActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelEditorLayout = new javax.swing.GroupLayout(panelEditor);
        panelEditor.setLayout(panelEditorLayout);
        panelEditorLayout.setHorizontalGroup(
            panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelEditorLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelEditorLayout.createSequentialGroup()
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel34)
                            .addComponent(jLabel32)
                            .addComponent(jLabel26)
                            .addComponent(jLabel12)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkEditorContextMenu)
                            .addComponent(checkShowRowNumbers)
                            .addComponent(checkSyntaxHighlight)
                            .addComponent(spinnerContentWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spinnerTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jLabel13))
                    .addGroup(panelEditorLayout.createSequentialGroup()
                        .addGap(251, 251, 251)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelEditorLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel12, jLabel26, jLabel32, jLabel34});

        panelEditorLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerContentWidth, spinnerTab});

        panelEditorLayout.setVerticalGroup(
            panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelEditorLayout.createSequentialGroup()
                .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelEditorLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(spinnerTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(2, 2, 2)
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(spinnerContentWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addGap(18, 18, 18)
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel26)
                            .addComponent(checkSyntaxHighlight))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel32)
                            .addComponent(checkShowRowNumbers))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelEditorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel34)
                            .addComponent(checkEditorContextMenu)))
                    .addGroup(panelEditorLayout.createSequentialGroup()
                        .addGap(105, 105, 105)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Editor", panelEditor);

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder("Processing Scripts"));

        buttonInsertProcScript.setText("Insert");
        buttonInsertProcScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertProcScriptActionPerformed(evt);
            }
        });

        tableProcessingScripts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File Name", "Category"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableProcessingScripts.setColumnSelectionAllowed(true);
        tableProcessingScripts.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableProcessingScripts.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tableProcessingScripts);
        tableProcessingScripts.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        buttonRemoveProcScript.setText("Delete");
        buttonRemoveProcScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveProcScriptActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonInsertProcScript)
                .addGap(18, 18, 18)
                .addComponent(buttonRemoveProcScript)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonRemoveProcScript)
                    .addComponent(buttonInsertProcScript)))
        );

        jLabel30.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel30.setText("Additional visible file extensions:");

        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel33.setText("Data panel cached:");

        javax.swing.GroupLayout panelDataLayout = new javax.swing.GroupLayout(panelData);
        panelData.setLayout(panelDataLayout);
        panelDataLayout.setHorizontalGroup(
            panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelDataLayout.createSequentialGroup()
                        .addGap(46, 46, 46)
                        .addGroup(panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel33, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel30))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkCachedDataPanel)
                            .addComponent(textDataExtensions))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        panelDataLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel30, jLabel33});

        panelDataLayout.setVerticalGroup(
            panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDataLayout.createSequentialGroup()
                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel33)
                    .addComponent(checkCachedDataPanel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel30)
                    .addComponent(textDataExtensions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Data", panelData);

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

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel18.setText("Marker Size:");

        spinnerMarkerSize.setModel(new javax.swing.SpinnerNumberModel(2, 1, 12, 1));

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel19.setText("Colormap:");

        comboColormapPlot.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel35.setText("Offscreen Buffers:");

        javax.swing.GroupLayout panelPlotsLayout = new javax.swing.GroupLayout(panelPlots);
        panelPlots.setLayout(panelPlotsLayout);
        panelPlotsLayout.setHorizontalGroup(
            panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPlotsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPlotsLayout.createSequentialGroup()
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comboMatrixPlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboLinePlot, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboTimePlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboSurfacePlot, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(panelPlotsLayout.createSequentialGroup()
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19)
                            .addComponent(jLabel11)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(comboQuality, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboLayout, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboColormapPlot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel35, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spinnerMarkerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkOffscreenBuffers))))
                .addContainerGap())
        );

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel6, jLabel7, jLabel8});

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11, jLabel18, jLabel19});

        panelPlotsLayout.setVerticalGroup(
            panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPlotsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(comboLinePlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(comboMatrixPlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(comboSurfacePlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(comboTimePlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPlotsLayout.createSequentialGroup()
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(comboQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addGap(2, 2, 2)
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11))
                        .addGap(2, 2, 2))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPlotsLayout.createSequentialGroup()
                        .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerMarkerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel18))
                        .addGap(18, 18, 18)))
                .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPlotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(comboColormapPlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel19))
                    .addComponent(checkOffscreenBuffers)
                    .addComponent(jLabel35))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelPlotsLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {comboColormapPlot, jLabel19});

        jTabbedPane1.addTab("Plots", panelPlots);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Device Control Visibility"));

        checkShowJogButtons.setText("Jog buttons");

        checkShowEmergencyStop.setText("Emergency stop button");

        checkShowHomingButtons.setText("Homing buttons");
        checkShowHomingButtons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkShowHomingButtonsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkShowHomingButtons)
                    .addComponent(checkShowJogButtons)
                    .addComponent(checkShowEmergencyStop))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {checkShowHomingButtons, checkShowJogButtons});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(checkShowHomingButtons)
                .addGap(0, 0, 0)
                .addComponent(checkShowJogButtons)
                .addGap(0, 0, 0)
                .addComponent(checkShowEmergencyStop)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder("Panel Visibility"));

        checkXScanBrowser.setText("XScan File Browser");

        checkQueueBrowser.setText("Queue File Browser");

        checkOutputPanel.setText("Output Panel");

        checkScanPanel.setText("Scan Panel");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkXScanBrowser)
                    .addComponent(checkQueueBrowser)
                    .addComponent(checkOutputPanel)
                    .addComponent(checkScanPanel))
                .addContainerGap(38, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel14Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(checkOutputPanel)
                .addGap(0, 0, 0)
                .addComponent(checkScanPanel)
                .addGap(0, 0, 0)
                .addComponent(checkXScanBrowser)
                .addGap(0, 0, 0)
                .addComponent(checkQueueBrowser)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder("Panel Location"));

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText("Console:");

        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel27.setText("Data:");

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel20.setText("Plots:");

        comboDataPanelLocation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboDataPanelLocationActionPerformed(evt);
            }
        });

        comboPlotsLocation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Internal", "Detached" }));

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel20))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comboConsoleLocation, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(comboPlotsLocation, 0, 101, Short.MAX_VALUE)))
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboDataPanelLocation, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jPanel15Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel14, jLabel20, jLabel27});

        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboConsoleLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboPlotsLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboDataPanelLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Data File Browsing"));

        buttonGroup1.add(radioDataEmbedded);
        radioDataEmbedded.setText("Embedded in Data Panel");

        buttonGroup1.add(radioDataDocTab);
        radioDataDocTab.setText("Open in Documents Tab");
        radioDataDocTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioDataDocTabActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(radioDataDocTab)
                    .addComponent(radioDataEmbedded))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(radioDataEmbedded)
                .addGap(0, 0, 0)
                .addComponent(radioDataDocTab)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelLayoutLayout = new javax.swing.GroupLayout(panelLayout);
        panelLayout.setLayout(panelLayoutLayout);
        panelLayoutLayout.setHorizontalGroup(
            panelLayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayoutLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelLayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelLayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelLayoutLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jPanel14, jPanel15, jPanel3, jPanel6});

        panelLayoutLayout.setVerticalGroup(
            panelLayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayoutLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelLayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Layout", panelLayout);

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

        javax.swing.GroupLayout panelPanelsLayout = new javax.swing.GroupLayout(panelPanels);
        panelPanels.setLayout(panelPanelsLayout);
        panelPanelsLayout.setHorizontalGroup(
            panelPanelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPanelsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelPanelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(panelPanelsLayout.createSequentialGroup()
                        .addComponent(buttonDefaultPanels)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDelete)))
                .addContainerGap())
        );
        panelPanelsLayout.setVerticalGroup(
            panelPanelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPanelsLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPanelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonDefaultPanels))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Panels", panelPanels);

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("Feedback"));

        ckScanTableEnabled.setText("Scan table enabled");

        ckScanPlotEnabled.setText("Scan plots enabled ");

        ckAsyncUpdate.setText("Async device widgets updates");

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel21.setText("Script popup dialog:");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboScriptPopup, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ckScanPlotEnabled)
                    .addComponent(ckScanTableEnabled)
                    .addComponent(ckAsyncUpdate))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(comboScriptPopup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ckScanPlotEnabled)
                .addGap(0, 0, 0)
                .addComponent(ckScanTableEnabled)
                .addGap(0, 0, 0)
                .addComponent(ckAsyncUpdate)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Imaging"));

        ckeckBackgroundRendering.setText("Background rendering");

        checkStatusBar.setText("Show status bar");

        checkPersistRendererWindows.setText("Persist renderer windows");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkStatusBar)
                    .addComponent(checkPersistRendererWindows)
                    .addComponent(ckeckBackgroundRendering))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ckeckBackgroundRendering)
                .addGap(0, 0, 0)
                .addComponent(checkPersistRendererWindows)
                .addGap(0, 0, 0)
                .addComponent(checkStatusBar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelGeneralLayout = new javax.swing.GroupLayout(panelGeneral);
        panelGeneral.setLayout(panelGeneralLayout);
        panelGeneralLayout.setHorizontalGroup(
            panelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelGeneralLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );
        panelGeneralLayout.setVerticalGroup(
            panelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("General", panelGeneral);

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
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                
                ArrayList<String> plottingScript = new ArrayList<>();
                for (int i = 0; i < modelProcessingScripts.getRowCount(); i++) {
                    String file = (String) modelProcessingScripts.getValueAt(i, 0);
                    String category = (String) modelProcessingScripts.getValueAt(i, 1);
                    plottingScript.add(file + "|" + category);
                }
                
                Quality quality = (Quality) comboQuality.getSelectedItem();
                PlotLayout plotLayout = (PlotLayout) comboLayout.getSelectedItem();
                ScriptPopupDialog scriptPopup = (ScriptPopupDialog) comboScriptPopup.getSelectedItem();

                //Then update preferences                
                preferences.fontShellPanel = selectedFonts[0];
                preferences.fontEditor = selectedFonts[1];
                preferences.fontOutput = selectedFonts[2];
                preferences.fontShellCommand = selectedFonts[3];
                preferences.fontPlotLabel = selectedFonts[4];
                preferences.fontPlotTick = selectedFonts[5];
                preferences.fontPlotTitle = selectedFonts[6];
                preferences.fontTerminal = selectedFonts[7];
                preferences.tabSize = (Integer) spinnerTab.getValue();
                preferences.contentWidth = (Integer) spinnerContentWidth.getValue();
                preferences.editorBackground = selectedEditorBackground;
                preferences.editorForeground = selectedEditorForeground;
                preferences.simpleEditor = !checkSyntaxHighlight.isSelected();
                preferences.hideEditorLineNumbers = !checkShowRowNumbers.isSelected();
                preferences.hideEditorContextMenu = !checkEditorContextMenu.isSelected();

                preferences.consoleLocation = (PanelLocation) comboConsoleLocation.getSelectedItem();
                preferences.dataPanelLocation = (PanelLocation) comboDataPanelLocation.getSelectedItem();
                preferences.openDataFilesInDocTab = radioDataDocTab.isSelected();
                preferences.asyncViewersUpdate = ckAsyncUpdate.isSelected();
                preferences.scanPlotDisabled = !ckScanPlotEnabled.isSelected();
                preferences.scanTableDisabled = !ckScanTableEnabled.isSelected();
                preferences.cachedDataPanel = checkCachedDataPanel.isSelected();
                preferences.dataExtensions = textDataExtensions.getText();
                preferences.showEmergencyStop = checkShowEmergencyStop.isSelected();
                preferences.showHomingButtons = checkShowHomingButtons.isSelected();
                preferences.showJogButtons = checkShowJogButtons.isSelected();
                preferences.hideOutputPanel = !checkOutputPanel.isSelected();
                preferences.hideScanPanel = !checkScanPanel.isSelected();
                preferences.showXScanFileBrowser = checkXScanBrowser.isSelected();
                preferences.showQueueBrowser = checkQueueBrowser.isSelected();
                preferences.linePlot = String.valueOf(comboLinePlot.getSelectedItem());
                preferences.matrixPlot = String.valueOf(comboMatrixPlot.getSelectedItem());
                preferences.surfacePlot = String.valueOf(comboSurfacePlot.getSelectedItem());
                preferences.timePlot = String.valueOf(comboTimePlot.getSelectedItem());
                preferences.plotsDetached = (comboPlotsLocation.getSelectedIndex() == 1);
                preferences.plotLayout = plotLayout;
                preferences.scriptPopupDialog = scriptPopup;
                preferences.quality = quality;
                preferences.markerSize = (Integer) spinnerMarkerSize.getValue();
                preferences.disableOffscreenBuffer = !checkOffscreenBuffers.isSelected();
                preferences.plotBackground = selectedPlotBackground;
                preferences.gridColor = selectedPlotGrid;
                preferences.outlineColor = selectedPlotOutline;
                preferences.editorBackground = selectedEditorBackground;
                preferences.editorForeground = selectedEditorForeground;
                preferences.defaultPlotColormap = (Colormap) comboColormapPlot.getSelectedItem();
                preferences.backgroundRendering = ckeckBackgroundRendering.isSelected();
                preferences.showImageStatusBar = checkStatusBar.isSelected();
                preferences.persistRendererWindows = checkPersistRendererWindows.isSelected();
                preferences.defaultPanels = panels.toArray(new DefaultPanel[0]);
                preferences.processingScripts = plottingScript.toArray(new String[0]);
            }
            accept();

        } catch (Exception ex) {
            showException(ex);
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
            showException(ex);
        }
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        try {
            if (tablePanels.getSelectedRow() >= 0) {
                modelPanels.removeRow(tablePanels.getSelectedRow());
                updateTablePanels();
            }
        } catch (Exception ex) {
            showException(ex);
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
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultPanelsActionPerformed

    private void buttonDefaultSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultSPActionPerformed
        try {
            selectedFonts[0] = Preferences.getDefaultFonts()[0];
            textSP.setText(getFontDesc(selectedFonts[0]));         
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultSPActionPerformed

    private void buttonSetBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetBackgroundActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", (selectedPlotBackground==null) ? null : new Color(selectedPlotBackground));            
            if (c != null) {
                selectedPlotBackground = c.getRGB();
                panelBackground.setBackground(c);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSetBackgroundActionPerformed

    private void buttonSetEditorBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetEditorBackgroundActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", (selectedEditorBackground==null) ? null : new Color(selectedEditorBackground));
            if (c != null) {
                selectedEditorBackground =  c.getRGB();
                panelEditorBackground.setBackground(c);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSetEditorBackgroundActionPerformed

    private void buttonSetEditorForegroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetEditorForegroundActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", (selectedEditorForeground==null) ? null : new Color(selectedEditorForeground));
            if (c != null) {
                selectedEditorForeground = c.getRGB();
                panelEditorForeground.setBackground(c);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSetEditorForegroundActionPerformed

    private void buttonResetBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetBackgroundActionPerformed
        selectedPlotBackground = null;
        panelBackground.setBackground(null);
    }//GEN-LAST:event_buttonResetBackgroundActionPerformed

    private void checkSyntaxHighlightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkSyntaxHighlightActionPerformed
        checkShowRowNumbers.setEnabled(checkSyntaxHighlight.isSelected());
    }//GEN-LAST:event_checkSyntaxHighlightActionPerformed

    private void buttonPTitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPTitActionPerformed
        getFont(6, textPTit);
    }//GEN-LAST:event_buttonPTitActionPerformed

    private void buttonPLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPLActionPerformed
        getFont(4, textPL);
    }//GEN-LAST:event_buttonPLActionPerformed

    private void buttonPTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPTActionPerformed
        getFont(5, textPT);
    }//GEN-LAST:event_buttonPTActionPerformed

    private void buttonInsertProcScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertProcScriptActionPerformed
        try {
            Context context =Context.getInstance();
            JFileChooser chooser = new JFileChooser(context.getSetup().getScriptPath());
            chooser.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Script files (*." + context.getScriptType().getExtension() + ")", 
                    new String[]{String.valueOf(context.getScriptType().getExtension())});
            chooser.setFileFilter(filter);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                String file = context.getStandardScriptName(chooser.getSelectedFile().getPath());
                for (int i =0; i< modelProcessingScripts.getRowCount(); i++){
                    if (file.equals(modelProcessingScripts.getValueAt(i, 0))){
                        return;
                    }
                }
                modelProcessingScripts.insertRow(tableProcessingScripts.getSelectedRow() + 1, new Object[]{file,""});
                updateListProcFiles();
                
            }            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonInsertProcScriptActionPerformed

    private void buttonRemoveProcScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveProcScriptActionPerformed
        try {
            if (tableProcessingScripts.getSelectedRow() >= 0) {
                modelProcessingScripts.removeRow(tableProcessingScripts.getSelectedRow());
                updateListProcFiles();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonRemoveProcScriptActionPerformed

    private void buttonDefaultSCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultSCActionPerformed
        try {
            selectedFonts[3] = Preferences.getDefaultFonts()[3];
            textSC.setText(getFontDesc(selectedFonts[3]));
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultSCActionPerformed

    private void buttonDefaultOPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultOPActionPerformed
        try {
            selectedFonts[2] = Preferences.getDefaultFonts()[2];
            textOP.setText(getFontDesc(selectedFonts[2]));            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultOPActionPerformed

    private void buttonDefaultSEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultSEActionPerformed
        try {
            selectedFonts[1] = Preferences.getDefaultFonts()[1];
            textSE.setText(getFontDesc(selectedFonts[1]));            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultSEActionPerformed

    private void buttonDefaultPTitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultPTitActionPerformed
        try {
            selectedFonts[6] = Preferences.getDefaultFonts()[6];
            textPTit.setText(getFontDesc(selectedFonts[6]));            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultPTitActionPerformed

    private void buttonDefaultPLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultPLActionPerformed
        try {
            selectedFonts[4] = Preferences.getDefaultFonts()[4];
            textPL.setText(getFontDesc(selectedFonts[4]));            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultPLActionPerformed

    private void buttonDefaultPTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultPTActionPerformed
        try {
            selectedFonts[5] = Preferences.getDefaultFonts()[5];
            textPT.setText(getFontDesc(selectedFonts[5]));            
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonDefaultPTActionPerformed

    private void buttonTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTActionPerformed
        getFont(7, textT);
    }//GEN-LAST:event_buttonTActionPerformed

    private void buttonDefaultTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultTActionPerformed
        try {
            selectedFonts[7] = Preferences.getDefaultFonts()[7];
            textT.setText(getFontDesc(selectedFonts[7]));            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDefaultTActionPerformed

    private void buttonOutlineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOutlineActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", (selectedPlotOutline==null) ? null : new Color(selectedPlotOutline));            
            if (c != null) {
                selectedPlotOutline = c.getRGB();
                panelOutline.setBackground(c);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonOutlineActionPerformed

    private void buttonResetOutlineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetOutlineActionPerformed
        selectedPlotOutline = null;
        panelOutline.setBackground(null);
    }//GEN-LAST:event_buttonResetOutlineActionPerformed

    private void buttonSetGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetGridActionPerformed
        try {
            Color c = JColorChooser.showDialog(null, "Choose a Color", (selectedPlotGrid==null) ? null : new Color(selectedPlotGrid));            
            if (c != null) {
                selectedPlotGrid = c.getRGB();
                panelGrid.setBackground(c);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSetGridActionPerformed

    private void buttonResetGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetGridActionPerformed
        selectedPlotGrid = null;
        panelGrid.setBackground(null);
    }//GEN-LAST:event_buttonResetGridActionPerformed

    private void buttonResetTextBgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetTextBgActionPerformed
        selectedEditorBackground = null;
        panelEditorBackground.setBackground(null);
    }//GEN-LAST:event_buttonResetTextBgActionPerformed

    private void buttonResetTextFgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetTextFgActionPerformed
        selectedEditorForeground = null;
        panelEditorForeground.setBackground(null);
    }//GEN-LAST:event_buttonResetTextFgActionPerformed

    private void radioDataDocTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioDataDocTabActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radioDataDocTabActionPerformed

    private void checkShowHomingButtonsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkShowHomingButtonsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_checkShowHomingButtonsActionPerformed

    private void comboDataPanelLocationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboDataPanelLocationActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboDataPanelLocationActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonDefaultOP;
    private javax.swing.JButton buttonDefaultPL;
    private javax.swing.JButton buttonDefaultPT;
    private javax.swing.JButton buttonDefaultPTit;
    private javax.swing.JButton buttonDefaultPanels;
    private javax.swing.JButton buttonDefaultSC;
    private javax.swing.JButton buttonDefaultSE;
    private javax.swing.JButton buttonDefaultSP;
    private javax.swing.JButton buttonDefaultT;
    private javax.swing.JButton buttonDelete;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonInsertProcScript;
    private javax.swing.JButton buttonOP;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonOutline;
    private javax.swing.JButton buttonPL;
    private javax.swing.JButton buttonPT;
    private javax.swing.JButton buttonPTit;
    private javax.swing.JButton buttonRemoveProcScript;
    private javax.swing.JButton buttonResetBackground;
    private javax.swing.JButton buttonResetGrid;
    private javax.swing.JButton buttonResetOutline;
    private javax.swing.JButton buttonResetTextBg;
    private javax.swing.JButton buttonResetTextFg;
    private javax.swing.JButton buttonSC;
    private javax.swing.JButton buttonSE;
    private javax.swing.JButton buttonSP;
    private javax.swing.JButton buttonSetBackground;
    private javax.swing.JButton buttonSetEditorBackground;
    private javax.swing.JButton buttonSetEditorForeground;
    private javax.swing.JButton buttonSetGrid;
    private javax.swing.JButton buttonT;
    private javax.swing.JCheckBox checkCachedDataPanel;
    private javax.swing.JCheckBox checkEditorContextMenu;
    private javax.swing.JCheckBox checkOffscreenBuffers;
    private javax.swing.JCheckBox checkOutputPanel;
    private javax.swing.JCheckBox checkPersistRendererWindows;
    private javax.swing.JCheckBox checkQueueBrowser;
    private javax.swing.JCheckBox checkScanPanel;
    private javax.swing.JCheckBox checkShowEmergencyStop;
    private javax.swing.JCheckBox checkShowHomingButtons;
    private javax.swing.JCheckBox checkShowJogButtons;
    private javax.swing.JCheckBox checkShowRowNumbers;
    private javax.swing.JCheckBox checkStatusBar;
    private javax.swing.JCheckBox checkSyntaxHighlight;
    private javax.swing.JCheckBox checkXScanBrowser;
    private javax.swing.JCheckBox ckAsyncUpdate;
    private javax.swing.JCheckBox ckScanPlotEnabled;
    private javax.swing.JCheckBox ckScanTableEnabled;
    private javax.swing.JCheckBox ckeckBackgroundRendering;
    private javax.swing.JComboBox comboColormapPlot;
    private javax.swing.JComboBox comboConsoleLocation;
    private javax.swing.JComboBox comboDataPanelLocation;
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
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel panelBackground;
    private javax.swing.JPanel panelColors;
    private javax.swing.JPanel panelData;
    private javax.swing.JPanel panelEditor;
    private javax.swing.JPanel panelEditorBackground;
    private javax.swing.JPanel panelEditorForeground;
    private javax.swing.JPanel panelFonts;
    private javax.swing.JPanel panelGeneral;
    private javax.swing.JPanel panelGrid;
    private javax.swing.JPanel panelLayout;
    private javax.swing.JPanel panelOutline;
    private javax.swing.JPanel panelPanels;
    private javax.swing.JPanel panelPlots;
    private javax.swing.JRadioButton radioDataDocTab;
    private javax.swing.JRadioButton radioDataEmbedded;
    private javax.swing.JSpinner spinnerContentWidth;
    private javax.swing.JSpinner spinnerMarkerSize;
    private javax.swing.JSpinner spinnerTab;
    private javax.swing.JTable tablePanels;
    private javax.swing.JTable tableProcessingScripts;
    private javax.swing.JTextField textDataExtensions;
    private javax.swing.JTextField textOP;
    private javax.swing.JTextField textPL;
    private javax.swing.JTextField textPT;
    private javax.swing.JTextField textPTit;
    private javax.swing.JTextField textSC;
    private javax.swing.JTextField textSE;
    private javax.swing.JTextField textSP;
    private javax.swing.JTextField textT;
    // End of variables declaration//GEN-END:variables
}
