package ch.psi.pshell.xscan;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.sequencer.Sequencer;
import ch.psi.pshell.utils.IO.FilePermissions;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The application singleton object.
 */
public class App extends ch.psi.pshell.framework.App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    final Sequencer sequencer;
    final DataManager dataManager;            
    
    static public App getInstance() {
        return (App) instance;
    }

    private static App createInstance() {
        if (instance==null){
            instance = new App();        
            logger.info("Created");
        }
        return (App) instance;
    }
    
    static View view;
    public static View getView(){
        return view;
    }
    
    
    
    static public void main(String[] args) {
        try {
            Options.add();
            init(args);                       
            createInstance().start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }    
    
    
    protected App(){
        Setup.mkdirs(new String[]{Setup.TOKEN_CONFIG, Setup.TOKEN_CONTEXT, Setup.TOKEN_DATA, Setup.TOKEN_LOGS, Setup.TOKEN_SCRIPT});
        dataManager = new DataManager();            
        sequencer = new Sequencer();
        sequencer.disableStartupScriptsExecution();
    }
    
    @Override
    protected void onStart() {        
        Epics.create(Setup.getDefaultEpicsConfigFile());
        super.onStart();
        try {            
            launchApp(View.class);                        
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    void initializeData() throws Exception {
        Context.setDataFilePattern(Config.getConfig().dataPath); 
        dataManager.initialize(Config.getConfig().getDataFormat(), Config.getConfig().getDataLayout());        
    }
    
    @Override
    protected void onRestart() {                
        setupLogger(Setup.TOKEN_LOGS + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME, Level.INFO, 7, FilePermissions.Default);        
        try {
            initializeData();
            sequencer.restart(); 
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
        
}
