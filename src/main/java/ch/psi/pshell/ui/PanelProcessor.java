package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.swing.Executor;
import ch.psi.utils.IO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class PanelProcessor extends Panel implements Processor{

    public PanelProcessor() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void onStart() {
        super.onStart();
        Processor.addServiceProvider(this.getClass());
        if (getView()!=null){
            getView().addProcessorComponents(this);
        }
    }

    @Override
    public void onStop() {
        Processor.removeServiceProvider(this.getClass());
        if (getView()!=null){
            getView().removeProcessorComponents(this);
        }     
        super.onStop();
    }
    
    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public void abort() throws InterruptedException {
        super.abort();
    }

    @Override
    public String getHomePath() {
        return Context.getInstance().getSetup().getScriptPath(); 
    }

    @Override
    public void open(String fileName) throws IOException {
    }

    @Override
    public void saveAs(String fileName) throws IOException {
    }

    @Override
    public String[] getExtensions() {
        return new String[0];
    }
    
    @Override
    public String getDescription() {
        return "";
    }    

    
    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }      
    
    static QueueProcessor detachedQueueProcessor;
    
    @Override
    protected void onUnloaded() {
        super.onUnloaded();
        if (this.isDetached()) {
            if (detachedQueueProcessor != null) {
                getContext().getPluginManager().unloadPlugin(detachedQueueProcessor);
                getApp().exit(this);
            }
        }
    }
    
    protected QueueProcessor getQueueProcessor() throws Exception{
        QueueProcessor tq;
        if (this.isDetached()) {
            if ((detachedQueueProcessor == null) || !detachedQueueProcessor.isLoaded()) {
                detachedQueueProcessor = new QueueProcessor();
                Context.getInstance().getPluginManager().loadPlugin(detachedQueueProcessor, "Queue");
                Context.getInstance().getPluginManager().startPlugin(detachedQueueProcessor);
            }
            tq = detachedQueueProcessor;
            tq.requestFocus();
        } else {
            List<QueueProcessor> queues = getView().getQueues();
            if (queues.isEmpty()) {
                tq = getView().openProcessor(QueueProcessor.class, null);
            } else {
                tq = queues.get(0);
            }
            getView().getDocumentsTab().setSelectedComponent(tq);
        }
        return tq;
    }
    
    public void queue() throws Exception{
        if (getFileName()==null){
            throw new Exception("File not saved");
        }
        getQueueProcessor().addNewFile(getFileName());
    }
    
    
    public void runNext() throws Exception{
        runNext(this);
    }    
    
    public static void runNext(Executor executor) throws Exception{
        String filename = executor.getFileName();
        if (filename==null){
            throw new Exception("File not saved");
        }           
        
        File file = new File(filename);        
        if (Context.getInstance().getState().isProcessing()) {
            App.getInstance().evalFileNext(file);
        } else {
            App.getInstance().evalFile(file);
        }
    }    

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
