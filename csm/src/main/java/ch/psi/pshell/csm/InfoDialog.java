package ch.psi.pshell.csm;

import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Str;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 */
public class InfoDialog extends StandardDialog {

    final DefaultTreeModel model;
    String currentInstance;
    boolean changed;
    Integer initCountTx;
    Integer initCountRx;
    Long  initTime;
    
    public InfoDialog(java.awt.Window parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
        setInstance(null);
        model =(DefaultTreeModel) tree.getModel();  
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }
    
    void setInstance(String name){        
        if ((name==null) || !name.equals(currentInstance)){
            this.setTitle((name==null)?  "Instance Info"  :name);
            currentInstance = name;
            changed = true;
        }
    }
    
    void update(Map<String,Map<String, Object>> instanceInfo){
        boolean instanceSelected = (currentInstance !=null) && (instanceInfo!=null);
        DefaultMutableTreeNode root = ((DefaultMutableTreeNode)model.getRoot());        
        if (changed){
            root.removeAllChildren();
            root.setUserObject("");
            model.nodeStructureChanged(root);
            initCountTx = null;
            initCountRx = null;
            initTime = null;              
            changed = false;          
        }
        if (instanceSelected){
            root.setUserObject(currentInstance);
            DefaultMutableTreeNode statistics;
            DefaultMutableTreeNode config ;  
            DefaultMutableTreeNode stream ;  
            DefaultMutableTreeNode start ;  
            DefaultMutableTreeNode mode ;  
            if (root.getChildCount()==0){
                config = new DefaultMutableTreeNode("Config");
                statistics = new DefaultMutableTreeNode("Statistics");
                start = new DefaultMutableTreeNode();
                mode = new DefaultMutableTreeNode();
                stream = new DefaultMutableTreeNode();
                DefaultMutableTreeNode streamParent = new DefaultMutableTreeNode("Stream");
                DefaultMutableTreeNode modeParent = new DefaultMutableTreeNode("Mode");
                DefaultMutableTreeNode startParent = new DefaultMutableTreeNode("Start");
                streamParent.add(stream);
                modeParent.add(mode);
                startParent.add(start);
                root.add(streamParent);
                root.add(modeParent);
                root.add(startParent);
                root.add(statistics);
                root.add(config);
                model.nodeChanged(root);
                SwingUtilities.invokeLater(()->{SwingUtils.expandAll(tree);});                
            } else {
                stream = (DefaultMutableTreeNode) root.getChildAt(0).getChildAt(0);
                mode = (DefaultMutableTreeNode) root.getChildAt(1).getChildAt(0);
                start = (DefaultMutableTreeNode) root.getChildAt(2).getChildAt(0);
                statistics = (DefaultMutableTreeNode) root.getChildAt(3);
                config = (DefaultMutableTreeNode) root.getChildAt(4);                
            }
               
            Map instanceData = instanceInfo.getOrDefault(currentInstance, new HashMap());
            stream.setUserObject(instanceData.getOrDefault("stream_address", ""));                      
            start.setUserObject(instanceData.getOrDefault("last_start_time", ""));                      
            mode.setUserObject(PanelStatus.isPush(instanceData) ? "PUSH" : "PUB");                      
            
            Map cfg = (Map) instanceData.getOrDefault("config", new HashMap());            
            if (config.getChildCount() != cfg.size()){
                while (config.getChildCount() > cfg.size()){
                    config.remove(config.getChildCount() -1);                    
                }
                while (config.getChildCount() < cfg.size()){
                    config.add(new DefaultMutableTreeNode()); 
                }
                model.nodeStructureChanged(config);
            }
            int index = 0;
            for (Object key : cfg.keySet()){
                ((DefaultMutableTreeNode)config.getChildAt(index++)).setUserObject(Str.toString(key) + ": " + Str.toString(cfg.get(key)));                 
            }              
            
            Map stats = (Map) instanceData.getOrDefault("statistics", new HashMap());  
            Integer rx=null, tx=null;
            try{
                rx = Integer.valueOf(((String)stats.get("rx")).split(" - ")[1]);
            } catch (Exception ex){                                        
            }
            try{
                tx = Integer.valueOf(((String)stats.get("tx")).split(" - ")[1]);
            } catch (Exception ex){                                        
            }
            if ((initTime == null) && (tx!=null) && (rx!=null)){
                initCountTx = tx;
                initCountRx = rx;
                initTime = System.currentTimeMillis();
            }
            if (stats.size()>0){
                if (!stats.containsKey("frame_shape")){
                    stats.put("frame_shape", "unknown        ");
                }
                stats.put("average_rx", "undefined        ");
                stats.put("average_tx", "undefined        ");
                if (initTime!=null){
                    double span = (System.currentTimeMillis()-initTime)/1000.0;
                    if (span>0){
                        double fpsrx = (rx-initCountRx) / span;
                        double fpstx = (tx-initCountTx) / span;
                        stats.put("average_rx", String.format("%1.2f fps     ", fpsrx));
                        stats.put("average_tx", String.format("%1.2f fps     ", fpstx));
                    }
                }
            }
            if (statistics.getChildCount() != stats.size()){
                while (statistics.getChildCount() > stats.size()){
                    statistics.remove(statistics.getChildCount() -1);                    
                }
                while (statistics.getChildCount() < stats.size()){
                    statistics.add(new DefaultMutableTreeNode()); 
                }
                model.nodeStructureChanged(statistics);
            }
            index = 0;
            for (Object key : stats.keySet()){
                ((DefaultMutableTreeNode)statistics.getChildAt(index++)).setUserObject(Str.toString(key) + ": " + PanelStatus.getDisplayValue(stats.get(key)));                 
            }              
            model.nodeChanged(statistics);
            model.nodeChanged(root);
        } else {
            root.removeAllChildren();
            root.setUserObject("");
            model.nodeStructureChanged(root);
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

        jScrollPane2 = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Name");
        tree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane2.setViewportView(tree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables
}
