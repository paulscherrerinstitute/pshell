package ch.psi.pshell.plotter;

import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Sys;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 *
 */
public class PlotPane extends javax.swing.JPanel {

    static final Logger logger = Logger.getLogger(PlotPanel.class.getName());
    static final Preferences preferences = new Preferences();
    boolean ctrlPressed;

    public PlotPane() {
        initComponents();
        try {
            preferences.load(Sys.getUserHome() + "/.plot_server.properties");
        } catch (Exception ex) {
        }
        applyPreferences();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED -> ctrlPressed = (ke.getKeyCode() == KeyEvent.VK_CONTROL);
                    case KeyEvent.KEY_RELEASED -> ctrlPressed = false;
                }
                return false;
            }
        });

    }

    public Preferences getPreferences() {
        return preferences;
    }

    void applyPreferences() {
        PlotBase.setPlotBackground(preferences.colorBackground);
        PlotBase.setGridColor(preferences.colorGrid);
        PlotBase.setOutlineColor(preferences.colorOutline);
    }

    public void clear() {
        for (PlotPanel panel : getPanels()) {
            if (tabPlots.indexOfComponent(panel) > 0) {
                tabPlots.remove(panel);
            }
        }
        plotPanel.clear();
    }

    PlotPanel getCurrentPanel() {
        return (PlotPanel) tabPlots.getSelectedComponent();
    }

    public List<PlotPanel> getPanels() {
        ArrayList<PlotPanel> ret = new ArrayList();
        for (int i = 0; i < tabPlots.getTabCount(); i++) {
            Component c = tabPlots.getComponentAt(i);
            if (c instanceof PlotPanel plotPanel1) {
                ret.add(plotPanel1);
            }
        }
        return ret;
    }

    public List<String> getPanelsNames() {
        ArrayList<String> ret = new ArrayList();
        for (int i = 0; i < tabPlots.getTabCount(); i++) {
            Component c = tabPlots.getComponentAt(i);
            if (c instanceof PlotPanel) {
                ret.add(tabPlots.getTitleAt(i));
            }
        }
        return ret;
    }

    public PlotPanel getPanel(int index) {
        try {
            Component c = tabPlots.getComponentAt(index);
            if (c instanceof PlotPanel plotPanel) {
                return plotPanel;
            }
        } catch (Exception ex) {
        }
        return null;
    }

    public PlotPanel getPanel(String name) {
        return getPanel(name, true);
    }

    public PlotPanel getPanel(String name, boolean create) {
        if (name == null) {
            return plotPanel;
        }
        for (int i = 0; i < tabPlots.getTabCount(); i++) {
            Component c = tabPlots.getComponentAt(i);
            if (c instanceof PlotPanel plotPanel) {
                if (name.equals(tabPlots.getTitleAt(i))) {
                    return plotPanel;
                }
            }
        }
        if (create) {
            PlotPanel plotPanel = new PlotPanel();
            plotPanel.initialize();
            plotPanel.setPlotTitle(name);
            plotPanel.clear();
            tabPlots.add(name, plotPanel);
            tabPlots.setSelectedComponent(plotPanel);

            int index = tabPlots.getTabCount() - 1;
            SwingUtils.setTabClosable(tabPlots, index);
            setTabDetachable(tabPlots, index, null);
            return plotPanel;
        }
        throw new RuntimeException("Invalid plot panel: " + name);
    }

    void setTabDetachable(final JTabbedPane tabbedPane, int index, final HashMap<String, PlotPanel> detachedMap) {
        Component component = tabbedPane.getComponentAt(index);
        ((SwingUtils.CloseButtonTabComponent) tabbedPane.getTabComponentAt(index)).getLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if ((e.getClickCount() == 2) && (tabbedPane.getSelectedComponent() == component)) {
                    int index = tabbedPane.indexOfComponent(component);
                    if (index < 0) {
                        logger.warning("Error retrieving tab index");
                        return;
                    }
                    String title = tabbedPane.getTitleAt(index);
                    tabbedPane.remove(component);
                    JDialog dlg = new JDialog((View) getTopLevelAncestor(), title, false);
                    dlg.setSize(component.getSize());
                    dlg.add(component);
                    dlg.addWindowListener(null);
                    dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    ((View) getTopLevelAncestor()).showChildWindow(dlg);
                    if (detachedMap != null) {
                        detachedMap.put(title, (PlotPanel) component);
                    }
                    dlg.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            if (detachedMap != null) {
                                detachedMap.values().removeIf(val -> val == component);
                            }
                            if (ctrlPressed) {
                                tabbedPane.add(dlg.getTitle(), component);
                                int index = tabbedPane.getTabCount() - 1;
                                SwingUtils.setTabClosable(tabbedPane, index, null);
                                setTabDetachable(tabbedPane, index, detachedMap);
                                tabbedPane.setSelectedIndex(index);
                            }
                        }
                    });
                    e.consume();
                } else {
                    tabbedPane.setSelectedComponent(component);
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabPlots = new javax.swing.JTabbedPane();
        plotPanel = new ch.psi.pshell.plotter.PlotPanel();

        tabPlots.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabPlotsStateChanged(evt);
            }
        });
        tabPlots.addTab("Plots", plotPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(tabPlots)
                    .addGap(0, 0, 0)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(tabPlots, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tabPlotsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPlotsStateChanged
        View view = ((View) getTopLevelAncestor());
        if (view != null) {
            view.updatePanel();
        }
    }//GEN-LAST:event_tabPlotsStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.plotter.PlotPanel plotPanel;
    private javax.swing.JTabbedPane tabPlots;
    // End of variables declaration//GEN-END:variables
}
