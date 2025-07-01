package ch.psi.pshell.csm;

import ch.psi.pshell.camserver.CameraClient;
import ch.psi.pshell.camserver.PipelineClient;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Threading;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class BackgroundPanel extends MonitoredPanel {

    PipelineClient pc;
    CameraClient cc;
    Set<String> cameras = new HashSet<>();
    final DefaultTableModel model;
    final DefaultTableModel modelHistory;
    
    volatile boolean running = false;
    String camera=null;
    BufferedImage historyBackground;
    String historyBackgroundId;
    List<String> visibleNames = new ArrayList<>();

    public BackgroundPanel() {
        initComponents();
        model = (DefaultTableModel) table.getModel();
        modelHistory = (DefaultTableModel) tableHistory.getModel();
    }
    
    
    public void setUrl(String urlPipeline, String urlCamera){
        pc = new PipelineClient(urlPipeline);
        cc = new CameraClient(urlCamera);
    }
    
    public String getUrl(){
       if (pc==null){
           return null;
       }
       return pc.getUrl();
    }   
    
    public void setProxy(PipelineClient proxy){
        this.pc = proxy;

    }
    
    public PipelineClient getProxy(){
       return pc;
    }     
    
    void updateButtons(){
        if (!SwingUtilities.isEventDispatchThread()){
            SwingUtilities.invokeLater(()->{updateButtons();});
            return;
        }        
        boolean updating = (cameraUpdateThread!=null);
        buttonCapture.setEnabled(!updating && (camera!=null));
        buttonGetImage.setEnabled(!updating &&(camera!=null));
        buttonShowHist.setEnabled(!updating &&(tableHistory.getSelectedRow()>=0)&&(historyBackground!=null));
    }
    
    Thread updateCameras(){
        Thread t = new Thread(()->{
            //model.setNumRows(0);
            
            cameras = new HashSet<>();
            try {
                for (String camera: pc.getCameras()){
                    try {
                        if ((camera!=null) && (!camera.isBlank())){
                            cameras.add(camera);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);            
            }
            List<String> names = new ArrayList<>(cameras);
            Collections.sort(names);
            visibleNames = List.copyOf(names);
            if ((filterName!=null) && (!filterName.isBlank())){
                visibleNames = visibleNames
                    .stream()
                    .filter(c -> c.toLowerCase().contains(filterName))
                    .collect(Collectors.toList());                            
            }
                        
            SwingUtilities.invokeLater(()->{        
                model.setNumRows(0);
                for (String camera: visibleNames){
                    model.addRow(new Object[]{camera,});
                }
                updateButtons();        
            });
        }, "BP Update Cameras");
        t.start();
        return t;
    }    
    
    volatile Thread cameraUpdateThread;
    int selectedIndex;
            
    Thread updateCamera(boolean force) throws IOException{   
        if (cameraUpdateThread!=null){
            try {
                Threading.stop(cameraUpdateThread, true, 1000);
            } catch (InterruptedException ex) {
                return null;
            }
            cameraUpdateThread=null;
        }
        selectedIndex = table.getSelectedRow();
        String camera = (selectedIndex>=0) ? Str.toString(model.getValueAt(selectedIndex, 0)) : null;  
        if ((force) || (camera != this.camera)){
            this.camera = camera;            
            textLast.setText("");
            textGeometryBackHist.setText("");
            checkOnline.setSelected(false);
            textGeometry.setText("");            
            modelHistory.setNumRows(0);
            historyBackground=null;
            if (camera!=null) {
                cameraUpdateThread = new Thread(()->{
                    try{    
                        String lastBackgroundID = null;
                        try{
                            lastBackgroundID = pc.getLastBackground(camera);
                            textLast.setText(lastBackgroundID); 
                        } catch (Exception ex){
                             Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);   
                        }
                        
                        try{
                            List<String> bgs = pc.getBackgrounds(camera);
                            modelHistory.setNumRows(bgs.size());
                            for (int i=0; i< bgs.size(); i++){
                                String id = bgs.get(i);
                                modelHistory.setValueAt(id, i, 0);
                                if (id.equals(lastBackgroundID)){
                                    int index=i;
                                    SwingUtilities.invokeLater(()->{
                                        tableHistory.setRowSelectionInterval(index, index);
                                        SwingUtils.scrollToVisible(tableHistory, index, 0);
                                        updateHistory();
                                    });                                    
                                }
                            }
                            
                        } catch (Exception ex){
                             Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);   
                             if (lastBackgroundID!=null){
                                modelHistory.setNumRows(1);
                                modelHistory.setValueAt(lastBackgroundID, 0, 0);
                                SwingUtilities.invokeLater(()->{
                                    tableHistory.setRowSelectionInterval(0, 0);
                                    updateHistory();
                                });                                    
                             }
                        }                        
                        updateButtons();        
                        if (!camera.equals(this.camera)){
                            return;
                        }
                        updateButtons();        
                        if (!camera.equals(this.camera)){
                            return;
                        }
                        try{
                            checkOnline.setSelected(cc.isOnline(camera));
                        } catch (Exception ex){
                            Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);   
                        }   
                        if (!camera.equals(this.camera)){
                            return;
                        }
                        try{
                            Dimension g = cc.getGeometry(camera);
                            if ((g.getWidth()>=0) && ( g.getHeight()>=0)){
                                textGeometry.setText(g.width + "x" + g.height);
                            }
                        } catch (Exception ex){
                            Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);   
                        }    
                    } finally {
                        //table.setEnabled(true);
                        updateButtons();    
                        cameraUpdateThread = null;
                    }   
                }, "BP Update Camera");
                //table.setEnabled(false);
                cameraUpdateThread.start();
            }
            updateButtons();
            return cameraUpdateThread;
        }
        return null;
    }   
    
    
    void updateHistory(){
        int row = tableHistory.getSelectedRow();
        textGeometryBackHist.setText("");
        if (row>=0){
            String id = (String) modelHistory.getValueAt(row, 0);
            new Thread(()->{
                try{
                    historyBackgroundId = id;
                    historyBackground = pc.getBackgroundImage(historyBackgroundId);
                    if ((historyBackground.getWidth()>=0) && ( historyBackground.getHeight()>=0)){
                        textGeometryBackHist.setText(historyBackground.getWidth() + "x" + historyBackground.getHeight());
                    }                    
                } catch (Exception ex){
                     Logger.getLogger(BackgroundPanel.class.getName()).log(Level.WARNING, null, ex);   
                }         
                 updateButtons();    
               }).start();                
        }
    }
    
    @Override
    protected void onShow(){
        if (model.getRowCount()==0){
            updateCameras();
        }
    }
    
    String filterName;
    void setFilter(String str){        
        if (str==null){
            str="";
        }
        if (!str.equals(filterName)){
            filterName = str;
            updateCameras();
        }
    }
        
    void onFilter(){
        setFilter(textFilter.getText().trim().toLowerCase());
    }       

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        textFilter = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        buttonCapture = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        spinnerImages = new javax.swing.JSpinner();
        textLast = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableHistory = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        textGeometryBackHist = new javax.swing.JTextField();
        buttonShowHist = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        checkOnline = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        buttonGetImage = new javax.swing.JButton();
        textGeometry = new javax.swing.JTextField();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Cameras"));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Camera Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableMouseReleased(evt);
            }
        });
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(table);

        jLabel5.setText("Filter:");

        textFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textFilterKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFilter)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(textFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Background"));

        buttonCapture.setText("Capture");
        buttonCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCaptureActionPerformed(evt);
            }
        });

        jLabel2.setText("Images:");

        spinnerImages.setModel(new javax.swing.SpinnerNumberModel(5, 1, 100, 1));

        textLast.setEditable(false);

        jLabel1.setText("Last:");

        tableHistory.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Backgroung Image ID"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableHistory.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableHistory.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableHistoryMouseReleased(evt);
            }
        });
        tableHistory.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableHistoryKeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(tableHistory);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Size:");

        textGeometryBackHist.setEditable(false);
        textGeometryBackHist.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        buttonShowHist.setText("Show");
        buttonShowHist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonShowHistActionPerformed(evt);
            }
        });

        jLabel8.setText("History:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textLast))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(7, 7, 7)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(textGeometryBackHist, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buttonShowHist))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(buttonCapture)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerImages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel8});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textLast, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textGeometryBackHist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(buttonShowHist))
                .addGap(30, 30, 30)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCapture)
                    .addComponent(jLabel2)
                    .addComponent(spinnerImages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Information"));

        checkOnline.setEnabled(false);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Online:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Size:");

        buttonGetImage.setText("Get Snapshot");
        buttonGetImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGetImageActionPerformed(evt);
            }
        });

        textGeometry.setEditable(false);
        textGeometry.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(checkOnline)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(textGeometry, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 65, Short.MAX_VALUE)
                        .addComponent(buttonGetImage)))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, jLabel4});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3)
                    .addComponent(checkOnline))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textGeometry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonGetImage))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        try{
            updateCamera(false);
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_tableMouseReleased

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        try{
            updateCamera(false);
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_tableKeyReleased

    private void buttonCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCaptureActionPerformed
        try{
            buttonCapture.setEnabled(false);
            SwingUtilities.invokeLater(()->{    
              try{
                pc.captureBackground(camera, (Integer) spinnerImages.getValue());
                updateCamera(true);
              } catch (Exception ex){
                Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
                showException(ex);
              }
            });
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_buttonCaptureActionPerformed

    private void buttonGetImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonGetImageActionPerformed
        try{
            SwingUtils.showDialog(this, camera + " snapshot", new Dimension (600,400),  new ImagePanel(cc.getImage(camera)));
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_buttonGetImageActionPerformed

    private void buttonShowHistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShowHistActionPerformed
        try{
            SwingUtils.showDialog(this, historyBackgroundId, new Dimension (600,400),  new ImagePanel(historyBackground));            
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_buttonShowHistActionPerformed

    private void tableHistoryMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableHistoryMouseReleased
        try{
            updateHistory();
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_tableHistoryMouseReleased

    private void tableHistoryKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableHistoryKeyReleased
        try{
            updateHistory();
        } catch (Exception ex){
            Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
            showException(ex);
        }
    }//GEN-LAST:event_tableHistoryKeyReleased

    private void textFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFilterKeyReleased
        try{
            onFilter();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textFilterKeyReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCapture;
    private javax.swing.JButton buttonGetImage;
    private javax.swing.JButton buttonShowHist;
    private javax.swing.JCheckBox checkOnline;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner spinnerImages;
    private javax.swing.JTable table;
    private javax.swing.JTable tableHistory;
    private javax.swing.JTextField textFilter;
    private javax.swing.JTextField textGeometry;
    private javax.swing.JTextField textGeometryBackHist;
    private javax.swing.JTextField textLast;
    // End of variables declaration//GEN-END:variables
}
