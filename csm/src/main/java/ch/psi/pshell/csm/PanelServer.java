
package ch.psi.pshell.csm;

import ch.psi.pshell.camserver.ProxyClient;
import java.util.Map;

/**
 *
 */
public class PanelServer extends javax.swing.JPanel {

    ProxyClient proxy;
    
    public PanelServer() {
        initComponents();
        setPipeline(false);        
    }
    
    public void setUrl(String url){
        setProxy(new ProxyClient(url));
    }
    
    public String getUrl(){
       if (proxy==null){
           return null;
       }
       return proxy.getUrl();
    }   
    
    public void setProxy(ProxyClient proxy){
        this.proxy = proxy;
        panelStatus.setProxy(proxy);
        panelConfig.setProxy(proxy);
        panelCreation.setProxy(proxy);
    }
    
    public ProxyClient getProxy(){
       return proxy;
    }      
    
    
   public boolean getPipeline(){
       return panelStatus.getPipeline();
   }

   public void setPipeline(boolean value){
       panelStatus.setPipeline(value);
       panelCreation.setPipeline(value);
   }
   
   
    public Map getInstanceCfg(String instance){
        return panelStatus.getInstanceCfg(instance);
    } 
    
    public boolean isPush(String instance){
        return panelStatus.isPush(instance);
    }    
    
    public PanelConfig getPanelConfig(){
        return panelConfig;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        panelStatus = new ch.psi.pshell.csm.PanelStatus();
        panelConfig = new ch.psi.pshell.csm.PanelConfig();
        panelCreation = new ch.psi.pshell.csm.PanelCreation();

        jTabbedPane1.addTab("Status", panelStatus);
        jTabbedPane1.addTab("Config", panelConfig);
        jTabbedPane1.addTab("Testing", panelCreation);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 643, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jTabbedPane1;
    private ch.psi.pshell.csm.PanelConfig panelConfig;
    private ch.psi.pshell.csm.PanelCreation panelCreation;
    private ch.psi.pshell.csm.PanelStatus panelStatus;
    // End of variables declaration//GEN-END:variables
}
