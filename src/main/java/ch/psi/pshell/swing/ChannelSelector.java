package ch.psi.pshell.swing;

import ch.psi.pshell.bs.PipelineServer;
import ch.psi.utils.DataAPI;
import ch.psi.utils.DispatcherAPI;
import ch.psi.pshell.core.Context;
import ch.psi.utils.EpicsBootInfoAPI;
import ch.psi.utils.Arr;
import ch.psi.utils.History;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 *
 */
public class ChannelSelector extends javax.swing.JPanel {

    DataAPI dataApi;
    EpicsBootInfoAPI epicsApi;
    PipelineServer pipelineServer;
    final AtomicBoolean updating;
    int historySize;
    String backend;
    volatile String nextRequest;
    volatile boolean updatingList;
    JComboBox combo;
    History history;
    String url;
    int limit;

    JList<String> list;
    JScrollPane listScrollPanel;
    Type type;

    public enum Type {
        DataAPI,
        DispatcherAPI,
        Epics,
        Camera
    }

    public ChannelSelector() {
        initComponents();

        updating = new AtomicBoolean(false);
        listScrollPanel = new javax.swing.JScrollPane();
        list = new javax.swing.JList<>();

        text.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textKeyReleased(evt);
            }
        });

        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                if (!updatingList) {
                    String str = list.getSelectedValue();
                    if ((str != null) && !str.isEmpty()) {
                        setText(str);
                    }
                    closeListDialog();
                }
            }
        });
        setFocusLostListener(list);
        setFocusLostListener(listScrollPanel);
        setFocusLostListener(text);
        listScrollPanel.setViewportView(list);
    }

    public void configure(Type type, String url, String backend, int limit) {
        dataApi = null;
        epicsApi = null;
        this.url = url;
        this.backend = backend;
        this.limit = limit;
        this.type = type;
        if (type == Type.Epics) {
            epicsApi = new EpicsBootInfoAPI(url);
        } else if (type == Type.Camera) {
            pipelineServer = new PipelineServer(null, url);
        } else {
            dataApi = (type == Type.DataAPI) ? new DataAPI(url) : new DispatcherAPI(url);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getBackend() {
        return backend;
    }

    public void setHistorySize(int historySize) {
        if (historySize < 0) {
            historySize = 0;
        }
        this.historySize = historySize;

        boolean hasHistory = historySize > 0;
        boolean hadHistory = combo != null;

        if (hasHistory != hadHistory) {
            GroupLayout layout = (GroupLayout) getLayout();
            if (hasHistory) {
                combo = new JComboBox();
                combo.setEditable(true);
                getEditorComponent().addKeyListener(new java.awt.event.KeyAdapter() {
                    public void keyReleased(java.awt.event.KeyEvent evt) {
                        update();
                    }
                });
                getEditorComponent().addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        //update();
                    }
                });
                setFocusLostListener(getEditorComponent());
                setFocusLostListener(combo);
                combo.addActionListener(new ActionListener() {
                    String selection = "";

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Object sel = combo.getSelectedItem();
                        sel = (sel == null) ? "" : sel;
                        if (!sel.equals(selection)) {
                            selection = sel.toString();
                            history.put(selection);
                            combo.setModel(new DefaultComboBoxModel(Lists.reverse(history.get()).toArray()));
                        }
                    }
                });

                layout.replace(text, combo);
                String path = (Context.getInstance() != null) ? Context.getInstance().getSetup().expandPath("{context}") : Sys.getUserHome();
                history = new History(path + "/ChannelSelector" + getName() + ".dat", historySize, true);
                combo.setModel(new DefaultComboBoxModel(Lists.reverse(history.get()).toArray()));
            } else {
                layout.replace(combo, text);
                combo = null;
                history = null;
            }

        }
    }

    void closeListDialog() {
        if (listMode == ListMode.Popup) {
            if ((dialogList != null) && dialogList.isShowing()) {
                dialogList.setVisible(false);
                dialogList = null;
            }
        }
    }

    void openListDialog() {
        if (listMode == ListMode.Popup) {
            if ((dialogList == null) || (!dialogList.isShowing())) {
                Component editor = getEditor();
                dialogList = new JDialog(SwingUtils.getWindow(this));
                dialogList.getContentPane().setLayout(new BorderLayout());
                dialogList.getContentPane().add(listScrollPanel, BorderLayout.CENTER);
                dialogList.setSize(new Dimension(editor.getWidth(), 200));
                dialogList.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                dialogList.setTitle(backend);
                dialogList.setLocation(editor.getLocationOnScreen().x, editor.getLocationOnScreen().y + editor.getHeight());
                dialogList.setFocusable(false);
                listScrollPanel.setFocusable(false);
                list.setFocusable(false);
                dialogList.setVisible(true);
                setFocusLostListener(dialogList);
                //dialogList.addWindowListener(new WindowAdapter() {
                //    @Override
                //    public void windowOpened(WindowEvent e) {
                //        getEditorComponent().requestFocus();
                //    }
                //});
               //this.update();
                //dialogList.setFocusable(false);
                SwingUtils.invokeDelayed(() -> {
                    SwingUtils.getWindow(this).requestFocus();
                    getEditor().requestFocus();
                }, 100);
                
                
            }
        }
    }

    private void setFocusLostListener(Component component) {
        component.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (listMode == ListMode.Popup) {
                    Object[] aux =  new Object[]{SwingUtils.getWindow(ChannelSelector.this), getEditor(), list, listScrollPanel, dialogList, getEditorComponent()};
                    //System.out.println((!Arr.contains(aux, evt.getOppositeComponent())) + " - " + evt.getOppositeComponent());
                    if (!Arr.contains(aux, evt.getOppositeComponent())) {
                        closeListDialog();
                    }
                }
            }
        });
    }

    Component getEditor() {
        return (combo != null) ? combo : text;
    }

    JTextComponent getEditorComponent() {
        return (combo != null) ? (JTextComponent) combo.getEditor().getEditorComponent() : text;
    }

    public int getHistorySize() {
        return historySize;
    }

    ListMode listMode = ListMode.Popup;

    public enum ListMode {
        Visible,
        Disabled,
        Popup;
    }

    //final JPanel dummy = new JPanel();
    public void setListMode(ListMode value) {
        if (listMode != value) {
            listMode = value;
            GroupLayout layout = new GroupLayout(this);
            setLayout(layout);
            if (listMode == ListMode.Visible) {
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent((combo == null) ? text : combo, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                                .addComponent(listScrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent((combo == null) ? text : combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(2, 2, 2)
                                        .addComponent(listScrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE))
                );
            } else {
                setLayout(new BorderLayout());
                add((combo == null) ? text : combo);
            }

        }
    }

    public ListMode getListMode() {
        return listMode;
    }

    void setData(List<String> data) throws InterruptedException, InvocationTargetException {
        //SwingUtilities.invokeAndWait(() -> {
        setContents(data);
        //});
    }

    void request(String str) throws InterruptedException {
        try {
            if (str.length() < 3) {
                setData(null);
            } else {
                try {
                    List<String> ret = null;
                    if (type == Type.Epics) {
                        ret = epicsApi.queryNames(str, backend, "substring", limit);
                    } else if (type == Type.Camera) {
                        ret = pipelineServer.getInstances();
                        Collections.sort(ret);
                    } else {
                        ret = dataApi.queryNames(str, backend, DataAPI.Ordering.desc, Boolean.FALSE);
                    }
                    if (limit > 0) {
                        if (ret.size() > limit) {
                            ret = ret.subList(0, limit);
                        }
                    }
                    setData(ret);
                } catch (Exception ex) {
                    setData(null);
                }
            }
        } catch (Exception ex) {
        } finally {
            updating.compareAndSet(true, false);
            if (nextRequest != null) {
                String aux = nextRequest;
                nextRequest = null;
                update(aux);
            }
        }
    }

    void update(String str) {
        if (updating.compareAndSet(false, true)) {
            Thread t = new Thread(() -> {
                try {
                    request(str);
                } catch (InterruptedException ex) {
                }
            }, "Chanel selector updater");
            t.setDaemon(true);
            t.start();
        } else {
            nextRequest = str;
        }
    }

    void update() {
        update(getText().trim());
    }

    public void setText(String str) {
        if ((str != null) && !str.isEmpty()) {
            if (combo != null) {
                combo.setSelectedItem(str);
            } else {
                text.setText(str);
            }
        }
    }

    public String getText() {
        return getEditorComponent().getText();
    }

    JDialog dialogList;

    void setContents(List<String> data) {
        if (listMode == ListMode.Disabled) {
            return;
        }
        updatingList = true;
        try {
            if (listMode == ListMode.Popup) {
                if ((data == null) /*|| (data.size() < 3)*/) {
                    closeListDialog();
                } else {
                    openListDialog();
                }
            }

            list.setModel(new javax.swing.AbstractListModel<String>() {
                @Override
                public int getSize() {
                    return (data == null) ? 0 : data.size();
                }

                @Override
                public String getElementAt(int i) {
                    return (data == null) ? "" : data.get(i);
                }
            });
        } finally {
            updatingList = false;
        }

    }

    public static void main(String[] args) {
        //java.awt.EventQueue.invokeLater(() -> {
        //sf-imagebuffer, sf-databuffer, sf-archiverappliance
        ChannelSelector cs = new ChannelSelector();
        cs.configure(Type.DataAPI, "https://data-api.psi.ch/sf", "sf-databuffer", 5000);
        //cs.configure(Type.DispatcherAPI,"https://dispatcher-api.psi.ch/sf", "sf-databuffer", 5000);
        //cs.configure(Type.Epics, "http://epics-boot-info.psi.ch", "swissfel", 5000);
        cs.setName("Test");
        cs.setHistorySize(10);
        cs.setListMode(ListMode.Popup);
        JDialog dlg = SwingUtils.showDialog(null, "Channel Selection", new Dimension(300, cs.getPreferredSize().height + 30), cs);
        SwingUtils.centerComponent(null, dlg);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        //});
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        text = new javax.swing.JTextField();

        text.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                textMouseClicked(evt);
            }
        });
        text.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(text, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(text)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void textKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textKeyReleased
        update();
    }//GEN-LAST:event_textKeyReleased

    private void textMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textMouseClicked
        //update();
    }//GEN-LAST:event_textMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField text;
    // End of variables declaration//GEN-END:variables
}
