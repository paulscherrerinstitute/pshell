package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import java.io.IOException;

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
    }

    @Override
    public void onStop() {
        super.onStop();
        Processor.removeServiceProvider(this.getClass());
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
