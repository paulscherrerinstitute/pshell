
import ch.psi.pshell.ui.ScriptProcessor;
import ch.psi.pshell.ui.Task;
import ch.psi.utils.State;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class ScriptPanel extends ScriptProcessor {
    
    //TODO: set script name
    public static final String SCRIPT_NAME = "ScriptPanel";
    
    public ScriptPanel() {
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
    public void onTaskFinished(Task task) {
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
    
    
    @Override
    protected void onStartingExecution(Map<String, Object> args) throws Exception{        
    }
    
    @Override
    protected void onFinishedExecution(Map<String, Object> args, Object ret, Throwable t) throws Exception{   
    }
        

    //Invoked by 'update()' to update components in the event thread
    @Override
    protected void doUpdate() {
    }
    
     
    @Override
    public String getScript(){
        return SCRIPT_NAME;
    }
    
    @Override
    public Map<String, Object> getArgs(){
        //TODO: add script arguments
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 303, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 176, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
