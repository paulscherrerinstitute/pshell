package ch.psi.pshell.framework;

/**
 * Task to restart the context.
 */
public class Restart extends Task {

    public Restart() {
        super();
        setTrackTime(false);
    }

    @Override
    protected String doInBackground() throws Exception {
        try {
            Context.restart();            
            return "Ok";
        } catch (Exception ex) {
            sendErrorApp(ex);
            throw ex;
        } finally {
        }
    }
    
}
