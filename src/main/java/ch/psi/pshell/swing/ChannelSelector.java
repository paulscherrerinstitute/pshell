package ch.psi.pshell.swing;

import ch.psi.pshell.camserver.PipelineSource;
import ch.psi.utils.DataAPI;
import ch.psi.utils.DispatcherAPI;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.ui.App;
import ch.psi.utils.EpicsBootInfoAPI;
import ch.psi.utils.Arr;
import ch.psi.utils.History;
import ch.psi.utils.IocInfoAPI;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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
import ch.psi.utils.ChannelQueryAPI;
import ch.psi.utils.Daqbuf;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;

/**
 *
 */
public class ChannelSelector extends MonitoredPanel {

    //DataAPI dataApi;
    //EpicsBootInfoAPI epicsApi;
    ChannelQueryAPI channelNameSource;
    PipelineSource pipelineServer;
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
    
    boolean multipleSelection;
    
    final HashSet<String> excludes = new HashSet<>();        

    public enum Type {
        DataAPI,
        DispatcherAPI,
        Epics,
        IocInfo,
        Camera,
        Daqbuf
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
                if (!multipleSelection){
                    if (!updatingList) {
                        String str = list.getSelectedValue();
                        if ((str != null) && !str.isEmpty()) {
                            setText(str);
                        }
                        closeListDialog();
                    }
                }
            }
        });
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setFocusLostListener(list);
        setFocusLostListener(listScrollPanel);
        setFocusLostListener(text);
        listScrollPanel.setViewportView(list);
    }
    
    public void configure(Type type, String url, String backend, int limit) {
        configure(type, url, backend, limit, null);
    }
    
    public void configure(Type type, String url, String backend, int limit, String[] excludes) {
        channelNameSource = null;
        this.url = url;
        this.backend = backend;
        this.limit = limit;
        this.type = type;
        this.excludes.clear();
        if (excludes!=null){
            Collections.addAll(this.excludes, excludes);
        }
        if (type == Type.Epics) {
            channelNameSource = new EpicsBootInfoAPI(url);
        } else if (type == Type.Daqbuf) {
            channelNameSource = new Daqbuf(url, backend);
        } else if (type == Type.IocInfo) {
            channelNameSource = new IocInfoAPI(url);
        } else if (type == Type.Camera) {
            pipelineServer = new PipelineSource(null, url);
            channelNameSource = new ChannelQueryAPI() {
                @Override
                public List<String> queryChannels(String text, String backend, int limit) throws IOException {
                    List<String> ret = pipelineServer.getInstances();
                    Collections.sort(ret);
                    return ret;

                }
            };

        } else {
            channelNameSource = (type == Type.DataAPI) ? new DataAPI(url) : new DispatcherAPI(url);
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
                combo.setEnabled(isEnabled());
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
                            List entries = history.get();
                            Collections.reverse(entries);
                            combo.setModel(new DefaultComboBoxModel(entries.toArray()));
                        }
                    }
                });

                layout.replace(text, combo);
                String path = (Context.getInstance() != null) ? Context.getInstance().getSetup().expandPath("{context}") : Sys.getUserHome();
                history = new History(path + "/ChannelSelector" + getName() + ".dat", historySize, true);
                List entries = history.get();
                Collections.reverse(entries);
                combo.setModel(new DefaultComboBoxModel(entries.toArray()));
            } else {
                layout.replace(combo, text);
                combo = null;
                history = null;
            }

        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        list.setEnabled(enabled);
        text.setEnabled(enabled);
        if (combo != null) {
            combo.setEnabled(enabled);
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
                if (getWindow() == null) {
                    dialogList = new JDialog(getFrame());
                } else {
                    dialogList = new JDialog(getWindow());
                }
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
                    if (getWindow() != null) {
                        getWindow().requestFocus();
                    }
                    getEditor().requestFocus();
                }, 100);

            }
        }
    }

    private void setFocusLostListener(Component component) {
        component.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (listMode == ListMode.Popup) {
                    Object[] aux = new Object[]{getWindow(), getEditor(), list, listScrollPanel, dialogList, getEditorComponent()};
                    //System.out.println((!Arr.contains(aux, evt.getOppositeComponent())) + " - " + evt.getOppositeComponent());
                    if (!Arr.contains(aux, evt.getOppositeComponent())) {
                        closeListDialog();
                    }
                }
            }
        });
    }

    public Component getEditor() {
        return (combo != null) ? combo : text;
    }

    public JTextComponent getEditorComponent() {
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

    
    public void setMultipleSelection(boolean value) {
        multipleSelection = value;
    }

    public boolean getMultipleSelection() {
        return multipleSelection;
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
                    List<String> ret = channelNameSource.queryChannels(str, backend, limit);
                    ret.removeIf(s -> s == null);
                    for (String exclude : excludes){
                        if (exclude.startsWith("*")){
                            ret.removeIf(s -> (s.endsWith(exclude.substring(1))));
                        } else if (exclude.endsWith("*")){
                            ret.removeIf(s -> (s.startsWith(exclude.substring(0,exclude.length()-1))));
                        } else {
                            ret.removeIf(s -> (s.contains(exclude)));
                        }
                    }
                    
                    if ((limit > 0) && (ret.size() > limit)) {
                        ret = ret.subList(0, limit);
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

    public List<String> getSelection() {
        return list.getSelectedValuesList();
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
        App.init(args);
        //java.awt.EventQueue.invokeLater(() -> {
        //sf-imagebuffer, sf-databuffer, sf-archiverappliance
        ChannelSelector cs = new ChannelSelector();
        //cs.configure(Type.DataAPI, "https://data-api.psi.ch/sf", "sf-databuffer", 5000);
        //cs.configure(Type.DispatcherAPI,"https://dispatcher-api.psi.ch/sf", "sf-databuffer", 5000);
        //cs.configure(Type.Epics, "https://epics-boot-info.psi.ch", "swissfel", 5000);
        //cs.configure(Type.IocInfo, "http://iocinfo.psi.ch/api/v2", "swissfel", 5000);
        cs.configure(Type.Daqbuf, null, "sf-databuffer", 5000);
        cs.setName("Test");
        cs.setHistorySize(0);
        //s.setMultipleSelection(true);
        cs.setListMode(ListMode.Popup);
        JDialog dlg = SwingUtils.showDialog(null, "Channel Selection", new Dimension(300, cs.getPreferredSize().height + 30), cs);
        SwingUtils.centerComponent(null, dlg);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        System.out.println(cs.getSelection());
        //});
    }

    public static class ChannelSelectorCellEditor extends AbstractCellEditor
            implements TableCellEditor, TreeCellEditor {

        protected JComponent editorComponent;
        protected EditorDelegate delegate;
        protected int clickCountToStart = 2;

        public ChannelSelectorCellEditor(final ChannelSelector channelSelector) {
            editorComponent = channelSelector;
            this.clickCountToStart = 2;
            delegate = new EditorDelegate() {
                public void setValue(Object value) {
                    channelSelector.setText((value != null) ? value.toString() : "");
                }

                public Object getCellEditorValue() {
                    return channelSelector.getText();
                }
            };
            ((JTextField) channelSelector.getEditorComponent()).addActionListener(delegate);
        }

        public Component getComponent() {
            return editorComponent;
        }

        public Object getCellEditorValue() {
            return delegate.getCellEditorValue();
        }

        public boolean isCellEditable(EventObject anEvent) {
            return delegate.isCellEditable(anEvent);
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return delegate.shouldSelectCell(anEvent);
        }

        public boolean stopCellEditing() {
            return delegate.stopCellEditing();
        }

        public void cancelCellEditing() {
            delegate.cancelCellEditing();
        }

        public Component getTreeCellEditorComponent(JTree tree, Object value,
                boolean isSelected,
                boolean expanded,
                boolean leaf, int row) {
            String stringValue = tree.convertValueToText(value, isSelected,
                    expanded, leaf, row, false);

            delegate.setValue(stringValue);
            return editorComponent;
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected,
                int row, int column) {
            delegate.setValue(value);
            editorComponent.setOpaque(false);
            return editorComponent;
        }

        protected class EditorDelegate implements ActionListener, ItemListener, Serializable {

            protected Object value;

            protected EditorDelegate() {
            }

            public Object getCellEditorValue() {
                return value;
            }

            public void setValue(Object value) {
                this.value = value;
            }

            public boolean isCellEditable(EventObject anEvent) {
                if (anEvent instanceof MouseEvent) {
                    return ((MouseEvent) anEvent).getClickCount() >= clickCountToStart;
                }
                return true;
            }

            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }

            public boolean startCellEditing(EventObject anEvent) {
                return true;
            }

            public boolean stopCellEditing() {
                fireEditingStopped();
                return true;
            }

            public void cancelCellEditing() {
                fireEditingCanceled();
            }

            public void actionPerformed(ActionEvent e) {
                ChannelSelectorCellEditor.this.stopCellEditing();
            }

            public void itemStateChanged(ItemEvent e) {
                ChannelSelectorCellEditor.this.stopCellEditing();
            }
        }
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
