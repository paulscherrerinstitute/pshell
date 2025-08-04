package ch.psi.pshell.app;

import ch.psi.pshell.app.MainFrame.LookAndFeelType;
import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.plot.PlotPanel;
import ch.psi.pshell.plugin.PluginManager;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.ObservableBase;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Sys.OSFamily;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/**
 * base class for application singleton
 */
public abstract class App extends ObservableBase<AppListener> {
    protected static App instance;
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static CommandLine commandLine;
    private static String[] args;
    private SwingPropertyChangeSupport pcs;    
    
     
    final static org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
    public App(){
        instance = this;               
    }
    
    public static MainFrame getMainFrame(){
        return MainFrame.getInstance();
    }
    
    public static boolean hasMainFrame(){
        return getMainFrame()!=null;
    }    
                
    
    public static String getHeaderMessage() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(getApplicationTitle()).append(" - ");
        sb.append(getApplicationDescription()).append("\n");
        sb.append(getApplicationCopyright()).append("\n");
        sb.append("Version " + getApplicationBuildInfo());
        return sb.toString();
    }
    
    
    static public String getCommand(String[] args){
         String[] positionalArguments = getPositionalArguments(args);
         if (positionalArguments.length>0){
             return positionalArguments[0];
         }
        String val = System.getenv(Options.COMMAND.toEnvVar());
        if ((val!=null) && (!val.isBlank())){
            return val.trim();            
        }
         
        return "";
    }

    static public String getCommand(){
        return getCommand(args);
    }

    
    static public String[] getCommandArguments(String[] args){
         String[] positionalArguments = getPositionalArguments(args);
         if (positionalArguments.length>0){
             return Arr.getSubArray(positionalArguments, 1);
         }
         return new String[0];
    }
    
    static public String[] getCommandArguments(){
        return getCommandArguments(args);
    }
    
    static public String[] getPositionalArguments(String[] args){
        String[] ret = new String[0];
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(new org.apache.commons.cli.Options(), args, true); 
            for (String arg : cmd.getArgList()){
                if (arg.startsWith("-")) {
                    break;
                }
                ret = Arr.append(ret, arg);
            }            
        } catch (ParseException e) {
            System.out.println("Error parsing command-line arguments: " + e.getMessage());
        }
        return ret;
    }
        
    static public boolean hasCommandLineArgument(String[] args, String argument) {
        return Arr.containsEqual(args, argument);
    }
    
    static public void addOption(String shortName, String longName, String desc){
        addOption(shortName, longName,  desc, "");
    }

    static public void addOption(String shortName, String longName, String desc, String argName){
        options.addOption(Option.builder(shortName)
                .longOpt(longName)
                .hasArg()   // Allows the option to have an argument
                .optionalArg(true) // Allows the option to appear with or without an argument
                .argName(argName)
                .desc(desc)
                .build());    
    }
    static public void init(String[] args) {
        init(args, null);
    }
    
    static public void init(String[] args, String[] commands) {
        init(args, commands, true);
    }
        
    static public void init(String[] args, String[] commands, boolean strict) {
        //Parse arguments to set system properties
        String jarFile = IO.getExecutingJar(App.class);
        if (jarFile != null) {
            try {
                //URL url = App.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
                //Manifest manifest = new Manifest(url.openStream());              
                JarInputStream jarStream = new JarInputStream(new FileInputStream(new File(jarFile)));
                Manifest manifest = jarStream.getManifest();
                Attributes attr = manifest.getMainAttributes();
                String buildTime = attr.getValue("Build-Time");
                if (buildTime != null) {
                    System.setProperty("build.time", buildTime);
                }
                String buildType = attr.getValue("Build-Type");
                if (buildType != null) {
                    System.setProperty("build.type", buildType);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }        
        
        System.out.println(getHeaderMessage());        
        ch.psi.pshell.app.Options.addBasic();
        App.args = args;
        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(options, args, !strict);            
            
        } catch (ParseException ex) {
            ex.printStackTrace();
            Runtime.getRuntime().exit(0);
        }                
                        
        if (commandLine.hasOption(Options.HELP.toArgument())) {            
            HelpFormatter formatter = new org.apache.commons.cli.HelpFormatter();
            formatter.setWidth(120);
            formatter.setOptionComparator(null);
            //formatter.setArgName("");
            String commandName = getApplicationId();
            StringBuilder sb = new StringBuilder();
            sb.append(commandName);
            if (commands!=null){
                sb.append(" <command> [options]\n");
                sb.append("Commands:\n");
                for (String cmd: commands){
                    sb.append(" ").append(cmd).append("\n");
                }
            } else{
                sb.append(" [options]\n");
            }
            sb.append("Options");
            String command = getCommand(args);
            if ((commands!=null) && (!command.isBlank())){
                sb.append(" (").append(command).append(")");
            }
            sb.append(":\n");
            formatter.printHelp(sb.toString(), options);            
            Runtime.getRuntime().exit(0);
        }
        
        if (commandLine.hasOption(Options.VERSION.toArgument())) {
            Runtime.getRuntime().exit(0);
        }
        
         
        //Disable eventual loggers of dependencies.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel","OFF");
        
        if (Setup.isForcedHeadless()) {
            System.setProperty("java.awt.headless", "true");
        }
        LogManager.setConsoleLoggerLevel(Setup.getConsoleLogLevel());         
        
        Quality quality = Setup.getQuality();
        if (quality!=null){
            System.setProperty(PlotPanel.PROPERTY_PLOT_QUALITY, quality.toString());
        }
        applyLookAndFeel();
    }
        
    //Arguments
    static public String[] getAditionalArguments(){
         if (commandLine==null){
             return new String[0];
         }
         return commandLine.getArgList().toArray(new String[0]);
    }
    
    static public boolean hasAditionalArgument(String argument) {
        return Arr.containsEqual(getAditionalArguments(), argument);
    }    
    
    
    static public boolean hasAditionalArgumentValue(String argument) {
        return getAditionalArgumentValue(argument)!=null;
    }    

    static public String getAditionalArgumentValue(String argument) {
        String[] args = getAditionalArguments();
        for (int i=0; i<args.length-1; i++){
            if (args[i].trim().equals(argument.trim())){
                return args[i+1];
            }
        }
        return null;
    }    

    static public String getAditionalArgument() {
        String[] args = getAditionalArguments();
        if ((args!=null) && (args.length>0)){
            return args[0].trim();
        }
        return null;
    }        

    static public boolean hasAditionalArgument() {
        String[] args = getAditionalArguments();
        return ((args!=null) && (args.length>0));
    }    
    
    static public boolean getBoolAditionalArgumentValue(String arg) {
        if (hasAditionalArgumentValue(arg)) {
            String val = getAditionalArgumentValue(arg);
            if (val!=null){
                return !(val.equalsIgnoreCase("false")) && !(val.equalsIgnoreCase("0"));
            }
            return true;
        }
        return false;
    }
        
    
    //Options
    static public String[] getCommandLineArguments() {
        return args;
    }

    static public boolean hasCommandLineArgument(String argument) {
        return Arr.containsEqual(args, argument);
    }
    
    /**
     * Returns the command line argument value for a given key, if present. If
     * not defined returns null. If defined multiple times then returns the
     * latest.
     */
    static public String getArgumentValue(String name) {
        if (commandLine!=null){
            if (commandLine.hasOption(name)) {
                var values = commandLine.getOptionValues(name);
                if ((values!=null) && (values.length>0)){
                    return values[values.length-1];
                }
            }
        }
        return null;
    }
    
    static public boolean hasArgumentValue(String name) {
        return getArgumentValue(name)!=null;
    }

    /**
     * Returns true if argument value is set and not empty
     */
    static public boolean isArgumentDefined(String name) {
        return ((getArgumentValue(name) != null) && !getArgumentValue(name).isBlank());
    }

    /**
     * Returns the command line argument values for a given key. If key is no
     * present then returns an empty list.
     */
    static public List<String> getArgumentValues(String name) {
        ArrayList<String> argumentValues = new ArrayList<>();
        if (commandLine!=null){
            if (commandLine.hasOption(name)) {
                for (String value : commandLine.getOptionValues(name)) {
                    argumentValues.add(value);
                }
            }
        }
        return argumentValues;
    }

    static public boolean hasArgument(String name) {
        if (commandLine==null){
            return false;
        }
        return commandLine.hasOption(name);
    }

    //accepts -option as -option=true
    static public boolean getBoolArgumentValue(String arg) {
        if (hasArgument(arg)) {
            String val = getArgumentValue(arg);
            if (val!=null){
                return !(val.equalsIgnoreCase("false")) && !(val.equalsIgnoreCase("0"));
            }
            return true;
        }
        return false;
    }
    
        
    public StatusBar getStatusBar() {
        JFrame mainFrame = getMainFrame();
        for (StatusBar statusBar: SwingUtils.getComponentsByType(mainFrame, StatusBar.class)){
            return statusBar;
        }
        return null;
    }
    
    @Hidden
    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return pcs;
    }
    
   
    static public File getFileArg() {
        try {
            var files = Setup.getFileArgs();
            if ((files==null) || files.isEmpty()){
                return null;
            }
            String fileName = Setup.expandPath(files.get(0));
            return new File(fileName);
        } catch (Exception ex) {
            return null;
        }
    }
    
    static public List<File> getFileArgs() {
        var files = Setup.getFileArgs();
        ArrayList<File> ret = new ArrayList<File>();
        for (String fileName : files) {
            try {
                fileName = Setup.expandPath(fileName);
                File file = new File(fileName);
                ret.add(file);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }     
        return ret;
    }    
    
    State state;
    
    public State getState() {
        return state;
    }        

    public void setState(State state) {
        this.state = state;
    }                
     
    
    static public String getEvalArg() {
        var evals = Setup.getEvalArgs();
        if ((evals!=null) && (evals.size()>0)){
            return evals.get(0);
        }
        return null;
    }    
    
    protected void applyViewOptions(){
        if (hasMainFrame()){
            Dimension size = Setup.getSize();
            if (size != null) {
                getMainFrame().setSize(size);
            }            
        }
    }
    
    
    public void persistViewState() {
        MainFrame mainFrame = getMainFrame();
        if (mainFrame!=null){
            if (mainFrame.isFullScreen()) {
                logger.fine("Do not persist state in full screen mode");
            } else {
                try {                    
                    mainFrame.saveState();
                    mainFrame.savePersistedWindowsStates();
                } catch (Exception ex) {
                    logger.log(Level.INFO, null, ex);
                }
            }                   
        }
    }

    static public File getFolderArg(String arg) {
        File defaultFolder = null;
        if (hasArgument(arg)) {
            String defaultFolderName = getArgumentValue(arg);
            if (defaultFolderName != null) {
                defaultFolderName =Setup.expandPath(defaultFolderName);
                if (defaultFolderName != null) {
                    File f = new File(defaultFolderName);
                    if (f.exists()) {
                        defaultFolder = f;
                    }
                }
            }

        }
        return defaultFolder;
    }
        
    //Resources
    static public String getResourceBundleValue(String key) {
        return getResourceBundleValue(App.class, key);
    }

    static String resourceBase = "ch/psi/pshell/ui/";
    static public String getResourceBase() {
        return resourceBase;
    }
    
    static public String getResourceBundleValue(Class cls, String key) {
        return getResourceBundleValue(cls.getSimpleName(), key);
    }
    
    static public String getResourceBundleValue(String bundle, String key) {
        return ResourceBundle.getBundle(getResourceBase() + bundle).getString(key);
    }
    

    static public URL getResourceUrl(String name) {
        return App.class.getResource("/"+ getResourceBase() + name);
    }

    static public Image getResourceImage(String name) {
        return Toolkit.getDefaultToolkit().getImage(getResourceUrl(name));
    }

    public static ImageIcon searchIcon(String name) {
        return MainFrame.searchIcon(name, App.class, "/"+ getResourceBase());
    }

    static public String getApplicationVersion() {
        return getResourceBundleValue("Common", "Application.version");
    }

    static public String getApplicationCopyright() {
        return App.getResourceBundleValue("Common", "Application.copyright");
    }
    
    static public String getVendorId() {
        return getResourceBundleValue("Common", "Application.vendorId");
    }            
    

    static public String getApplicationBuildTime() {
        return System.getProperty("build.time");
    }

    static public String getApplicationBuildType() {
        return System.getProperty("build.type");
    }

    static public String getApplicationBuildInfo() {
        String buildTime = getApplicationBuildTime();        
        String ret = getApplicationVersion();
        if (buildTime != null) {
            String buildType = getApplicationBuildType();
            buildType =(buildType==null) ?  "" : "-" + buildType; 
            ret += " (build " + buildTime + buildType + ")";
        }
        return ret;
    }

    static public String getApplicationName() {
        return getResourceBundleValue("Application.name");
    }
    
    static public String getApplicationId() {
        return getResourceBundleValue("Application.id");
    }

    static public String getApplicationTitle() {
        return getResourceBundleValue("Application.title");
    }
    
    static public String getApplicationDescription() {
        return getResourceBundleValue("Application.description");
    }
    
    static public Image getIconSmall() {
        return getResourceImage("IconSmall.png");
    }

    static public Image getIconLarge() {
        return getResourceImage("IconLarge.png");
    }

    static public App getInstance() {
        return instance;
    }
    
     static public boolean hasInstance(){
        return getInstance() !=null;
    }    

    public void start(boolean sync) throws Exception {
        try {            
            appendLibraryPath();
            appendClassPath();  
            
            if (sync) {
                onStart();
            } else {
                SwingUtilities.invokeAndWait(() -> {
                    if (!Setup.isHeadless()) {
                        pcs = new SwingPropertyChangeSupport(this, true);
                    }                    
                    onStart();
                });
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            throw ex;
        }
    }
    
    protected void onStart(){           
    }

    
    static void applyLookAndFeel() {
        MainFrame.setScaleUI(Setup.getScaleUI());
        if (Setup.isHeadless()) {
            try{
                //Try setting LAF for offscreen plotting
                MainFrame.setLookAndFeel(getLookAndFeel());
            } catch(Throwable t){                
            }
            return;
        }
        
        if (!Setup.isHeadless()) {
            if (Sys.getOSFamily() == OSFamily.Mac) {
                try {
                    SwingUtils.setMacScreenMenuBar(getApplicationTitle());
                } catch (Exception ex) {
                }
            }                    
            MainFrame.setLookAndFeel(getLookAndFeel());
        }
    }
    
    static String lafDefault;
    public static void setLookAndFeelDefault(String laf) {
        lafDefault = laf;
    }
    
    public static void setLookAndFeelDefault(LookAndFeelType type) {
        setLookAndFeelDefault(MainFrame.getLookAndFeel(type));
    }
    
    public static String getLookAndFeelDefault() {
        if (lafDefault==null){
            // Default LAF is Nimbus but prefer System on Mac
            if (Sys.getOSFamily() == OSFamily.Mac) {
                return MainFrame.getLookAndFeel(LookAndFeelType.system);
            } else {
                return MainFrame.getLookAndFeel(LookAndFeelType.nimbus);
            }
        }
        return lafDefault;
    }
    
    public static String getLookAndFeel() {
        String laf = Setup.getLookAndFeel();
        if (laf == null) {
            return getLookAndFeelDefault();
        }
        return laf;
    }    
    
    
    static void appendLibraryPath() {
        for (String path : Setup.getAddedLibraryPath()) {
            try {
                PluginManager.addToLibraryPath(new File(path));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }            
        }
    }

    static void appendClassPath() {        
        for (String path : Setup.getAddedClassPath()) {
            try {
                PluginManager.addToClassPath(new File(path));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }        
    
    
    protected void onExit() {
    }
    
    //Exit
    protected void requestExit(Object origin) {
        for (AppListener listener : getListeners()) {
            try {
                if (!listener.canExit(origin)) {
                    logger.info("Applicaiton exit denied by: " + listener.toString());
                    return;
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        try {            
            for (AppListener listener : getListeners()) {
                persistViewState();
                try {
                    listener.willExit(origin);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
            onExit();
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        } finally {
            end();
        }
    }

    class ExitTask implements Runnable {

        Object origin;

        @Override
        public void run() {
            requestExit(origin);
        }
    }

    final ExitTask exitTask = new ExitTask();

    public void exit() {
        exit(null);
    }

    public void exit(final Object origin) {
        logger.log(Level.INFO, "Aplication exit command by: {0}", String.valueOf(origin));
        exitTask.origin = origin;
        if ((getMainFrame() == null) || SwingUtilities.isEventDispatchThread()) {
            exitTask.run();
        } else {
            new Thread(exitTask).start();
        }
    }

    synchronized void end() {
        JFrame mainFrame = getMainFrame();
        if (mainFrame != null && mainFrame.isDisplayable()) {
            mainFrame.setVisible(false);
            mainFrame.dispose();
        }
        logger.info("Finished");
        Runtime.getRuntime().exit(0);
    }

    //For logging
    @Override
    public String toString() {
        return "Application";
    }

}
