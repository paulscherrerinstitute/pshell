package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.Task;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The application singleton object.
 */
public class App extends Shell{    

    private static final Logger logger = Logger.getLogger(App.class.getName());      
    
    
    static public App getInstance() {
        return (App) instance;
    }

    private static App createInstance() {
        if (instance==null){
            instance = new App();
        }
        return (App) instance;
    }   
    
    protected void launchApp() {
        if (hasStdio()) {
            Stdio c = new Stdio();
            c.start();
        }
        launchApp(View.class);
    }    
        
    public static class Restart extends Task {

        public Restart() {
            super();
            setTrackTime(false);
        }

        @Override
        protected String doInBackground() throws Exception {
            try {
                getInstance().restart();            
                return "Ok";
            } catch (Exception ex) {
                sendErrorApp(ex);
                throw ex;
            } finally {
            }
        }
    }          
        
    static public boolean hasStdio() {
        return Setup.isGui() && Options.STDIO.getBool(false);
    }
    
    static public void create() {
       try {            
            createInstance().start();            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } 
    }    
       
    static public void main(String[] args) {
        Options.add();
        init(args);            
        create();            
    }

}
