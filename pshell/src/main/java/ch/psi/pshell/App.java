package ch.psi.pshell;

import ch.psi.pshell.framework.Options;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.stripchart.StripChartServer;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.workbench.HelpContentsDialog;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The application singleton object.
 */
public class App extends ch.psi.pshell.workbench.App{    
    public static final String COMAMND_WORBENCH = "workbench (default)";
    public static final String COMAMND_CONSOLE = "console";
    public static final String COMAMND_STRIP_CHART = "strip_chart";
    public static final String COMAMND_SCREEN_PANEL = "screen_panel";
    public static final String COMAMND_ARCHIVER_VIEWER = "archiver_viewer";
    public static final String COMAMND_DATA_VIEWER = "data_viewer";
    public static final String COMAMND_PLOT_SERVER = "plot";
    public static final String COMAMND_DEVICE_PANEL = "device";
    public static final String COMAMND_HELP_PANEL = "help_panel";
    
    public static final String[] COMMAND_LINE_COMMANDS = new String[]{COMAMND_WORBENCH, COMAMND_CONSOLE, COMAMND_STRIP_CHART, 
        COMAMND_SCREEN_PANEL, COMAMND_ARCHIVER_VIEWER, COMAMND_DATA_VIEWER, COMAMND_PLOT_SERVER, COMAMND_DEVICE_PANEL, COMAMND_HELP_PANEL};
       
    private static final Logger logger = Logger.getLogger(App.class.getName());
    Object stripChartServer;
    
    private static App createInstance() {
        if (instance==null){
            instance = new App();
        }
        return (App) instance;
    }    
    
    @Override
    protected void launchApp() {    
        //Application launching is delayed.
    }
    

    protected void onStart() {                             
                
        super.onStart();    
                        
        if (Setup.isGui()) {
            if (isHelpPanel()) {
                forceEmpty();
                HelpContentsDialog dialog = new HelpContentsDialog(new javax.swing.JFrame(), false);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setIconImage(App.getIconSmall());
                dialog.setTitle("PShell Help Contents");
                dialog.setSize(800, 600);
                SwingUtils.centerComponent(null, dialog);
                dialog.setVisible(true);
            } else  if (isStripChart()) {
                forceLocal();
                if (ch.psi.pshell.stripchart.App.isAttach()) {
                    stripChartServer = ch.psi.pshell.stripchart.App.createAttached();
                    if (stripChartServer==null){
                        //Panel handled by the server process, just quit
                        System.exit(0);
                    }
                } else {
                    ch.psi.pshell.stripchart.App.create();
                }
            } else if (isDataViewer()) {                
                forceOffline();
                //DataPanel.create(getFileArg(), getSize());
                ch.psi.pshell.dataviewer.App.create();                
            } else if (isArchiverViewer()) {
                forceOffline();
                ch.psi.pshell.archiverviewer.App.create();                
            } else if (isPlotServer()){ 
                forceLocal();
                ch.psi.pshell.plotter.App.create();
            } else if (isCamServerViewer()) {
                forceDisabled();
                ch.psi.pshell.screenpanel.App.create();
            } else if (isDeviceViewer()){
                forceDisabled();
                ch.psi.pshell.screenpanel.App.create();
            } else if (isConsole()){                
                ch.psi.pshell.console.App.create();
            } else  {
                super.launchApp();
            }
            if (!hasMainFrame()){ //Execution with command
                startRestart();
            }
        } else {
            if (isStripChart() && Setup.isServerMode()) {
                stripChartServer = new StripChartServer();
                restart();
            } else {
                super.launchApp();
            }
        }
    }       
    
    void forceOffline(){
        Options.OFFLINE.set();
        forceDisabled();
    }
    
    void forceDisabled(){
        Options.DISABLED.set();
        forceEmpty();
    }
    void forceEmpty(){
        ch.psi.pshell.devices.Options.EMPTY.set();
        Options.BARE.set();
        Options.GENERIC.set();
        forceLocal();     
    }
    
    void forceLocal(){
        ch.psi.pshell.app.Options.LOCAL.set();
        Options.DISABLE_SESSIONS.set();
    }    

    static public boolean isStripChart() {
        return getCommand().equals(COMAMND_STRIP_CHART);
    }
        
    static public boolean isCamServerViewer() {
        return getCommand().equals(COMAMND_SCREEN_PANEL);
    }
        
    static public boolean isArchiverViewer() {
        return getCommand().equals(COMAMND_ARCHIVER_VIEWER);
    }
    static public boolean isDataViewer() {
        return getCommand().equals(COMAMND_DATA_VIEWER);
    }   
    
    static public boolean isDeviceViewer() {
        return getCommand().equals(COMAMND_DEVICE_PANEL);
    }    
        
    static public boolean isPlotServer() {
        return getCommand().equals(COMAMND_PLOT_SERVER);
    }
    
    static public boolean isConsole() {
        return getCommand().equals(COMAMND_CONSOLE);
    }

    static public boolean isHelpPanel() {
        return getCommand().equals(COMAMND_HELP_PANEL);
    }
        
    public static void addOptions(String[] args){                
        String command = getCommand(args);                        
        switch (command){
            case COMAMND_STRIP_CHART -> ch.psi.pshell.stripchart.Options.add();
            case COMAMND_ARCHIVER_VIEWER -> ch.psi.pshell.archiverviewer.Options.add();
            case COMAMND_PLOT_SERVER -> ch.psi.pshell.plotter.Options.add();
            case COMAMND_SCREEN_PANEL -> ch.psi.pshell.screenpanel.Options.add();
            case COMAMND_DATA_VIEWER -> ch.psi.pshell.dataviewer.Options.add();
            case COMAMND_CONSOLE -> ch.psi.pshell.console.Options.add();
            default -> ch.psi.pshell.workbench.Options.add();
        }
    }    
    
    
    static public void main(String[] args) {
        addOptions(args);
        init(args, COMMAND_LINE_COMMANDS);            
       try {            
            createInstance().start();            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } 
    }

}
