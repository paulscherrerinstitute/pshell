import ch.psi.pshell.framework.Panel;
import ch.psi.pshell.utils.State;

/**
 *
 */
public class PanelPlugin extends Panel {

    public PanelPlugin() {
        initComponents();
    }

    //Overridable callbacks
    @Override
    public void onInitialize(int runCount) {

    }

    @Override
    public void onStateChange(State state, State former) {

    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }

    @Override
    protected void onTimer() {
    }

    @Override
    protected void onLoaded() {

    }

    @Override
    protected void onUnloaded() {

    }

    //Invoked by 'update()' to update components in the event thread
    @Override
    protected void doUpdate() {
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 449, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 137, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
