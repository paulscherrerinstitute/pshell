package ch.psi.pshell.xscan;

import ch.psi.pshell.app.AboutDialog;
import ch.psi.pshell.crlogic.CrlogicConfig;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Executor;
import ch.psi.pshell.framework.MainFrame;
import ch.psi.pshell.framework.Processor;
import ch.psi.pshell.framework.QueueProcessor;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.StatusBar;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.EpicsConfigDialog;
import ch.psi.pshell.swing.ExtensionFileFilter;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class View extends MainFrame {
    static final Logger logger = Logger.getLogger(View.class.getName());
    static final PlotPreferences preferences = new PlotPreferences();
    
    public JTabbedPane getDocumentsTab() {return tabDoc;}
    public JTabbedPane getPlotsTab() {return tabPlots;}
    public JTabbedPane getStatusTab() {return tabStatus;}
    public JTabbedPane getLeftTab() {return tabLeft;}
    public StatusBar getStatusBar() {return statusBar;}
    public JToolBar getToolBar() {return toolBar;}
    public DataPanel getDataPanel() {return dataPanel;}    
    public OutputPanel getOutputPanel() {return outputPanel;}
    public JMenu getMenuFileNew() {return menuFileNew;}
    public JMenuBar getMenu() {return menuBar;}
    public ch.psi.pshell.swing.PlotPanel getScanPlot() {return scanPlot;}    
        
    public View() {
        super();
        Context.setState(State.Initializing);
        initComponents();
        setIcon(getClass().getResource("/ch/psi/pshell/ui/Icon.ico"));

        setTitle(Optional.ofNullable(Setup.getTitle()).orElse("FDA"));
        
        try {
            preferences.load(Setup.expandPath("{config}/xscan_plots.properties"));
        } catch (Exception ex) {
        }
        
        applyPreferences();
        
        if (Setup.isPlotOnly()) {
            splitterVertical.setVisible(false);
            splitterHorizontal.setDividerSize(0);
            menuBar.setVisible(false);
            Component[] visible = new Component[]{buttonAbort, buttonPause, buttonRun, buttonAbout, separatorInfo};
            for (Component c : toolBar.getComponents()) {
                if (!Arr.contains(visible, c)) {
                    toolBar.remove(c);
                }
            }
        }        
        

        loggerPanel.setOutputLength(1000);
        //loggerPanel.setInverted(true);
        loggerPanel.start();

        boolean persist  = !Setup.isLocal();
        //After ps is instantiated-> the context file depends on the port & host.
        if (persist){
             restoreState();
        } else {
            if (Setup.getSize()!=null){
                SwingUtilities.invokeLater(()->{
                    View.this.setSize(Setup.getSize());
                    SwingUtils.centerComponent(null, View.this);                    
                });
            }
        }
        


        Epics.create();
        
        Processor.addServiceProvider(ProcessorXScan.class);
        ProcessorXScan processor = new ProcessorXScan();
        scriptsPanel.initialize(processor.getHomePath(), processor.getExtensions());
        scriptsPanel.setListener((File file) -> {
            try {
                openProcessor(processor.getClass(), file.getAbsolutePath());
            } catch (Exception ex) {
                showException(ex);
            }
        });
        
        updateViewState();
    }
    
    @Override
    protected void onOpen() {
        super.onOpen();
        var visibleFiles = new String[]{"*.log", "*.txt", "*.xml", "*.png", "*.tif", "*.tiff", "*.mat", "*.h5"};        
        dataPanel.initialize(visibleFiles);
        
        if (Context.getApp().isViewPersisted()) {
            restoreOpenedFiles();
        }            
    }
    
    @Override
    public void updateViewState() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                updateViewState();
            });
            return;
        }
        Processor runningProcessor = getRunningProcessor();
        State state = Context.getState();
        boolean showingProcessor= getSelectedProcessor()!=null;
        boolean ready = (state == State.Ready);
        boolean busy = (state == State.Busy);
        boolean paused = (state == State.Paused);
        buttonRun.setEnabled((ready && (showingProcessor || Setup.isPlotOnly())) || paused);
        buttonPause.setEnabled(!paused && (runningProcessor != null) && (runningProcessor.canPause()));
        //buttonPause.setEnabled(interp.canPause() || ((runningProcessor != null) && (runningProcessor.canPause())));
        buttonAbort.setEnabled(busy || paused);
        
        menuSave.setEnabled(showingProcessor);
        buttonSave.setEnabled(menuSave.isEnabled());
        menuSaveAs.setEnabled(menuSave.isEnabled());
        
    }
    
    void applyPreferences(){
        PlotBase.setPlotBackground(preferences.colorBackground);
        PlotBase.setGridColor(preferences.colorGrid);
        PlotBase.setOutlineColor(preferences.colorOutline);   
        statusBar.setShowDataFileName(true);
    }
    
    
 
    /**
     * Called when window is being closed
     */
    @Override
    protected void onClosing() {
        saveState();
        setVisible(false);
        dispose();
        System.exit(0);
    }

    @Override
    protected void onTimer() {
        super.onTimer();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
    }


    static String getHelpMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Arguments:");
        sb.append("\n\t-h\tPrint this help message");
        sb.append("\n\t-l\tLocal mode: No GUI state persistence");
        sb.append("\n\t-size=WxH    \tSet application window size if GUI state not persisted");
        sb.append("\n\t-port=<int>  \tSet the plot server port (default: 7777)");
        sb.append("\n\t-title=<str> \tSet the main window title");
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toolBar = new javax.swing.JToolBar();
        buttonNew = new javax.swing.JButton();
        buttonOpen = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        buttonRun = new javax.swing.JButton();
        buttonPause = new javax.swing.JButton();
        buttonAbort = new javax.swing.JButton();
        separatorInfo = new javax.swing.JToolBar.Separator();
        buttonAbout = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        statusBar = new ch.psi.pshell.framework.StatusBar();
        mainPanel = new javax.swing.JPanel();
        splitterHorizontal = new javax.swing.JSplitPane();
        tabPlots = new javax.swing.JTabbedPane();
        scanPlot = new ch.psi.pshell.swing.PlotPanel();
        splitterVertical = new javax.swing.JSplitPane();
        tabStatus = new javax.swing.JTabbedPane();
        scriptsPanel = new ch.psi.pshell.swing.ScriptsPanel();
        outputPanel = new ch.psi.pshell.swing.OutputPanel();
        loggerPanel = new ch.psi.pshell.swing.LoggerPanel();
        splitterDoc = new javax.swing.JSplitPane();
        tabLeft = new javax.swing.JTabbedPane();
        dataPanel = new ch.psi.pshell.swing.DataPanel();
        tabDoc = new javax.swing.JTabbedPane();
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuFileNew = new javax.swing.JMenu();
        menuNewXscan = new javax.swing.JMenuItem();
        menuOpen = new javax.swing.JMenuItem();
        menuSave = new javax.swing.JMenuItem();
        menuSaveAs = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        menuConfig = new javax.swing.JMenuItem();
        menuEpics = new javax.swing.JMenuItem();
        menuCrlogic = new javax.swing.JMenuItem();
        menuAddToQueue = new javax.swing.JMenu();
        menuOpenRecent = new javax.swing.JMenu();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem menuExit = new javax.swing.JMenuItem();
        menuView = new javax.swing.JMenu();
        menuFullScreen = new javax.swing.JCheckBoxMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        menuCloseAllPlots = new javax.swing.JMenuItem();
        jSeparator20 = new javax.swing.JPopupMenu.Separator();
        menuPreferences = new javax.swing.JMenuItem();
        javax.swing.JMenu menuHelp = new javax.swing.JMenu();
        menuSetup = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem menuAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setName("XScan"); // NOI18N

        buttonNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/New.png"))); // NOI18N
        buttonNew.setToolTipText("New");
        buttonNew.setFocusable(false);
        buttonNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewActionPerformed(evt);
            }
        });
        toolBar.add(buttonNew);

        buttonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Open.png"))); // NOI18N
        buttonOpen.setToolTipText("Open");
        buttonOpen.setFocusable(false);
        buttonOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOpenActionPerformed(evt);
            }
        });
        toolBar.add(buttonOpen);

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Save.png"))); // NOI18N
        buttonSave.setToolTipText("Save");
        buttonSave.setFocusable(false);
        buttonSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });
        toolBar.add(buttonSave);

        jSeparator5.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator5.setPreferredSize(new java.awt.Dimension(20, 0));
        jSeparator5.setRequestFocusEnabled(false);
        toolBar.add(jSeparator5);

        buttonRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Play.png"))); // NOI18N
        buttonRun.setToolTipText("Run");
        buttonRun.setFocusable(false);
        buttonRun.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRun.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });
        toolBar.add(buttonRun);

        buttonPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Pause.png"))); // NOI18N
        buttonPause.setToolTipText("Pause\n");
        buttonPause.setFocusable(false);
        buttonPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonPause.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPauseActionPerformed(evt);
            }
        });
        toolBar.add(buttonPause);

        buttonAbort.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Stop.png"))); // NOI18N
        buttonAbort.setToolTipText("Abort");
        buttonAbort.setFocusable(false);
        buttonAbort.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAbort.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAbortActionPerformed(evt);
            }
        });
        toolBar.add(buttonAbort);

        separatorInfo.setMaximumSize(new java.awt.Dimension(20, 32767));
        separatorInfo.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(separatorInfo);

        buttonAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Info.png"))); // NOI18N
        buttonAbout.setToolTipText("About");
        buttonAbout.setFocusable(false);
        buttonAbout.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAbout.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        toolBar.add(buttonAbout);
        toolBar.add(filler1);
        toolBar.add(filler2);

        splitterHorizontal.setDividerLocation(650);
        splitterHorizontal.setResizeWeight(1.0);
        splitterHorizontal.setToolTipText("");

        javax.swing.GroupLayout scanPlotLayout = new javax.swing.GroupLayout(scanPlot);
        scanPlot.setLayout(scanPlotLayout);
        scanPlotLayout.setHorizontalGroup(
            scanPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 345, Short.MAX_VALUE)
        );
        scanPlotLayout.setVerticalGroup(
            scanPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 486, Short.MAX_VALUE)
        );

        tabPlots.addTab("Plots", scanPlot);

        splitterHorizontal.setRightComponent(tabPlots);

        splitterVertical.setDividerLocation(350);
        splitterVertical.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitterVertical.setResizeWeight(1.0);

        tabStatus.addTab("Scripts", scriptsPanel);
        tabStatus.addTab("Output", outputPanel);
        tabStatus.addTab("Logs", loggerPanel);

        splitterVertical.setRightComponent(tabStatus);

        splitterDoc.setDividerLocation(250);

        dataPanel.setEmbedded(false);
        tabLeft.addTab("Data", dataPanel);

        splitterDoc.setLeftComponent(tabLeft);

        tabDoc.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabDocStateChanged(evt);
            }
        });
        splitterDoc.setRightComponent(tabDoc);

        splitterVertical.setLeftComponent(splitterDoc);

        splitterHorizontal.setLeftComponent(splitterVertical);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitterHorizontal, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(splitterHorizontal, javax.swing.GroupLayout.DEFAULT_SIZE, 521, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/xscan/View"); // NOI18N
        menuFile.setText(bundle.getString("View.menuFile.text")); // NOI18N
        menuFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuFileStateChanged(evt);
            }
        });

        menuFileNew.setText("New");
        menuFileNew.setName("menuFileNew"); // NOI18N

        menuNewXscan.setText("Xscan");
        menuNewXscan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuNewXscanActionPerformed(evt);
            }
        });
        menuFileNew.add(menuNewXscan);

        menuFile.add(menuFileNew);

        menuOpen.setText("Open...");
        menuOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenActionPerformed(evt);
            }
        });
        menuFile.add(menuOpen);

        menuSave.setText("Save");
        menuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveActionPerformed(evt);
            }
        });
        menuFile.add(menuSave);

        menuSaveAs.setText("Save As...");
        menuSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveAsActionPerformed(evt);
            }
        });
        menuFile.add(menuSaveAs);
        menuFile.add(jSeparator3);

        menuConfig.setText(bundle.getString("View.menuConfig.text")); // NOI18N
        menuConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuConfigActionPerformed(evt);
            }
        });
        menuFile.add(menuConfig);

        menuEpics.setText(bundle.getString("View.menuEpics.text")); // NOI18N
        menuEpics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuEpicsActionPerformed(evt);
            }
        });
        menuFile.add(menuEpics);

        menuCrlogic.setText(bundle.getString("View.menuCrlogic.text")); // NOI18N
        menuCrlogic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCrlogicActionPerformed(evt);
            }
        });
        menuFile.add(menuCrlogic);

        menuAddToQueue.setText(bundle.getString("View.menuAddToQueue.text")); // NOI18N
        menuFile.add(menuAddToQueue);

        menuOpenRecent.setText(bundle.getString("View.menuOpenRecent.text")); // NOI18N
        menuFile.add(menuOpenRecent);
        menuFile.add(jSeparator2);

        menuExit.setText(bundle.getString("View.menuExit.text")); // NOI18N
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        menuBar.add(menuFile);

        menuView.setText(bundle.getString("View.menuView.text")); // NOI18N
        menuView.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuViewStateChanged(evt);
            }
        });

        menuFullScreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFullScreen.setSelected(true);
        menuFullScreen.setText(bundle.getString("View.menuFullScreen.text")); // NOI18N
        menuFullScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFullScreenActionPerformed(evt);
            }
        });
        menuView.add(menuFullScreen);
        menuView.add(jSeparator13);

        menuCloseAllPlots.setText(bundle.getString("View.menuCloseAllPlots.text")); // NOI18N
        menuCloseAllPlots.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCloseAllPlotsActionPerformed(evt);
            }
        });
        menuView.add(menuCloseAllPlots);
        menuView.add(jSeparator20);

        menuPreferences.setText(bundle.getString("View.menuPreferences.text")); // NOI18N
        menuPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPreferencesActionPerformed(evt);
            }
        });
        menuView.add(menuPreferences);

        menuBar.add(menuView);

        menuHelp.setText(bundle.getString("View.menuHelp.text")); // NOI18N

        menuSetup.setText("Setup");
        menuSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSetupActionPerformed(evt);
            }
        });
        menuHelp.add(menuSetup);
        menuHelp.add(jSeparator1);

        menuAbout.setText(bundle.getString("View.menuAbout.text")); // NOI18N
        menuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        menuHelp.add(menuAbout);

        menuBar.add(menuHelp);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(statusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        onClosing();
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuFullScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFullScreenActionPerformed
        try {
            setFullScreen(menuFullScreen.isSelected());
        } catch (Exception ex) {
            showException(ex);
        } catch (Error e) {
            logger.severe(e.toString());
        }
    }//GEN-LAST:event_menuFullScreenActionPerformed

    private void menuCloseAllPlotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCloseAllPlotsActionPerformed
        try {
            for (int index = tabPlots.getTabCount()-1; index>0; index--) {
                tabPlots.remove(index);
            }
            scanPlot.clear();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCloseAllPlotsActionPerformed

    private void menuPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPreferencesActionPerformed
        try {
            ConfigDialog dlg = new ConfigDialog(this, true);
            dlg.setTitle("Preferences");
            dlg.setConfig(preferences);
            dlg.setSize(600, 480);
            showChildWindow(dlg);
            if (dlg.getResult()) {
                preferences.save();
                applyPreferences();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPreferencesActionPerformed

    private void menuViewStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuViewStateChanged
        if (menuView.isSelected()) {
            try {
                menuFullScreen.setSelected(isFullScreen());
            } catch (Exception ex) {
            }
        }
    }//GEN-LAST:event_menuViewStateChanged

    private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
        AboutDialog aboutDialog = new AboutDialog(this, true);
        aboutDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        showChildWindow(aboutDialog);
    }//GEN-LAST:event_menuAboutActionPerformed

    String text;
    String marker;
    private void menuSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetupActionPerformed
        try {
            String[][] entries = new String[][]{
                {"Process", Sys.getProcessName()},
                {"User", Sys.getUserName()},
                {"Version", App.getApplicationBuildInfo()},
                {"Java", System.getProperty("java.vm.name") + " (" + System.getProperty("java.version") + ")"},
                {"Arguments", String.join(" ", App.getCommandLineArguments())},
                {"Current folder", new File(".").getCanonicalPath()},
                {"Preferences", Config.config.getFileName()},
                };

            JTable table = new JTable();
            table.setModel(new DefaultTableModel(
                    entries,
                    new String[]{
                        "Parameter", "Value"
                    }
            ) {
                public Class
                        getColumnClass(int columnIndex) {
                    return String.class;
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return (columnIndex == 1);
                }
            });
            //table.setPreferredSize(new Dimension(400,200));
            table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            table.getColumnModel().getColumn(0).setPreferredWidth(150);
            table.getColumnModel().getColumn(1).setPreferredWidth(450);
            JTextField textField = new JTextField();
            textField.setEditable(false);
            DefaultCellEditor editor = new DefaultCellEditor(textField);
            table.getColumnModel().getColumn(1).setCellEditor(editor);
            StandardDialog dlg = new StandardDialog(this, "Setup", true);
            dlg.setContentPane(table);
            dlg.pack();
            dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            //dlg.setResizable(false);
            showChildWindow(dlg);

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSetupActionPerformed

    private void menuConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuConfigActionPerformed
        try {
            ConfigDialog dlg = new ConfigDialog(this, true);
            dlg.setTitle("Configuration");
            dlg.setConfig(Config.getConfig());
            dlg.setSize(600, 480);
            showChildWindow(dlg);
            if (dlg.getResult()) {
                Config.getConfig().save();
                App.getInstance().initializeData();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuConfigActionPerformed

    private void menuEpicsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEpicsActionPerformed
        try {
            EpicsConfigDialog dlg = new EpicsConfigDialog(this, true, Context.getConfigFilePermissions());
            dlg.setTitle("EPICS Properties");
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuEpicsActionPerformed

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
            try {
                openProcessor(ProcessorXScan.class, null);
            } catch (Exception ex) {
                showException(ex);
            }
    }//GEN-LAST:event_buttonNewActionPerformed

    private void buttonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOpenActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(ProcessorXScan.getScriptPath());
            FileFilter xmlFilter = new ExtensionFileFilter("XScan files (*.xml)", new String[]{"xml"});
            FileFilter queFilter = new ExtensionFileFilter("Execution queue (*." + QueueProcessor.EXTENSION + ")", new String[]{QueueProcessor.EXTENSION});
            chooser.addChoosableFileFilter(xmlFilter);            
            chooser.addChoosableFileFilter(queFilter);
            chooser.setAcceptAllFileFilterUsed(false);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {                
                if (chooser.getFileFilter()==queFilter){
                    openProcessor(QueueProcessor.class, chooser.getSelectedFile().toString());
                } else {
                    openProcessor(ProcessorXScan.class, chooser.getSelectedFile().toString());
                }
                
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonOpenActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }   
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        try {
            run();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonRunActionPerformed

    private void buttonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPauseActionPerformed
        try {
            pause();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAbortActionPerformed
         try {
            abort();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonAbortActionPerformed

    private void menuNewXscanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuNewXscanActionPerformed
         buttonNewActionPerformed(null);
    }//GEN-LAST:event_menuNewXscanActionPerformed

    private void menuFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuFileStateChanged
        if (menuFile.isSelected()) {
            try {
                menuOpenRecent.removeAll();
                ActionListener listener = (ActionEvent e) -> {
                    String file = e.getActionCommand();
                    try {
                        openScriptOrProcessor(file);
                    } catch (Exception ex) {
                        showException(ex);                        
                    }
                };
                for (String file : getFileHistory().get()) {
                    JMenuItem item = new JMenuItem(file);
                    item.addActionListener(listener);
                    menuOpenRecent.add(item, 0);
                }

                Executor executor = getSelectedExecutor();
                Processor runningProcessor = getRunningProcessor();
                QueueProcessor queueProcessor = getSelectedQueueProcessor();

                List<QueueProcessor> queues = getQueues();
                if (queueProcessor != null) {
                    queues.remove(queueProcessor);
                }
                menuAddToQueue.setVisible(isShowingExecutor());
                menuAddToQueue.removeAll();
                if (executor != null) {
                    String _filename = null;
                    try{
                        _filename = executor.getFileName();
                    } catch (Exception ex){                        
                    }                    
                    if (executor instanceof Processor processor){
                        String home = processor.getHomePath();
                        if ((home!=null) && (!home.isBlank()) && (_filename!=null)){
                            if (IO.isSubPath(_filename, home)) {
                                //If in script folder then use only relative
                                _filename =  IO.getRelativePath(_filename, home);
                            }
                        }
                    }

                    String filename = _filename;
                    Map<String, Object> args = null;
                    menuAddToQueue.setEnabled(filename != null);
                    if (filename != null) {
                        if (queues.size() == 0) {
                            JMenuItem item = new JMenuItem("New");
                            item.addActionListener((e) -> {
                                try {
                                    QueueProcessor tq = openProcessor(QueueProcessor.class, null);
                                    tq.addNewFile(filename, args);
                                } catch (Exception ex) {
                                    showException(ex);
                                }
                            });
                            menuAddToQueue.add(item);

                        } else {
                            for (int i = 0; i < queues.size(); i++) {
                                if (queues.get(i) != executor) {
                                    String queueFilename = queues.get(i).getFileName();
                                    if (queueFilename == null) {
                                        queueFilename = "Unknown";
                                    }
                                    JMenuItem item = new JMenuItem(IO.getPrefix(queueFilename));
                                    item.setEnabled(queues.get(i) != runningProcessor);
                                    int index = i;
                                    item.addActionListener((e) -> {
                                        try {
                                            queues.get(index).addNewFile(filename, args);
                                            tabDoc.setSelectedComponent(queues.get(index));
                                        } catch (Exception ex) {
                                            showException(ex);
                                        }
                                    });
                                    menuAddToQueue.add(item);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                showException(ex);
            }

        }
    }//GEN-LAST:event_menuFileStateChanged

    private void menuSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveAsActionPerformed
        try {
            saveAs();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSaveAsActionPerformed

    private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSaveActionPerformed

    private void menuOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenActionPerformed
        buttonOpenActionPerformed(null);
    }//GEN-LAST:event_menuOpenActionPerformed

    private void tabDocStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabDocStateChanged
        updateViewState();
    }//GEN-LAST:event_tabDocStateChanged

    private void menuCrlogicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCrlogicActionPerformed
        try {
            ConfigDialog dlg = new ConfigDialog(this, true);
            dlg.setTitle("Crlogic");
            dlg.setConfig(CrlogicConfig.getConfig());
            dlg.setSize(600, 480);
            showChildWindow(dlg);
            if (dlg.getResult()) {
               CrlogicConfig.getConfig().save();
                App.getInstance().initializeData();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCrlogicActionPerformed
    
    public static void create(Dimension size){
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               View view = new View();
               if (size!=null){
                   view.setSize(size);
               }
               view.setVisible(true);
            }
        });        
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAbort;
    private javax.swing.JButton buttonAbout;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonOpen;
    private javax.swing.JButton buttonPause;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonSave;
    private ch.psi.pshell.swing.DataPanel dataPanel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator20;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator5;
    private ch.psi.pshell.swing.LoggerPanel loggerPanel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenu menuAddToQueue;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem menuCloseAllPlots;
    private javax.swing.JMenuItem menuConfig;
    private javax.swing.JMenuItem menuCrlogic;
    private javax.swing.JMenuItem menuEpics;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenu menuFileNew;
    private javax.swing.JCheckBoxMenuItem menuFullScreen;
    private javax.swing.JMenuItem menuNewXscan;
    private javax.swing.JMenuItem menuOpen;
    private javax.swing.JMenu menuOpenRecent;
    private javax.swing.JMenuItem menuPreferences;
    private javax.swing.JMenuItem menuSave;
    private javax.swing.JMenuItem menuSaveAs;
    private javax.swing.JMenuItem menuSetup;
    private javax.swing.JMenu menuView;
    private ch.psi.pshell.swing.OutputPanel outputPanel;
    private ch.psi.pshell.swing.PlotPanel scanPlot;
    private ch.psi.pshell.swing.ScriptsPanel scriptsPanel;
    private javax.swing.JToolBar.Separator separatorInfo;
    private javax.swing.JSplitPane splitterDoc;
    private javax.swing.JSplitPane splitterHorizontal;
    private javax.swing.JSplitPane splitterVertical;
    private ch.psi.pshell.framework.StatusBar statusBar;
    private javax.swing.JTabbedPane tabDoc;
    private javax.swing.JTabbedPane tabLeft;
    private javax.swing.JTabbedPane tabPlots;
    private javax.swing.JTabbedPane tabStatus;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables
}
