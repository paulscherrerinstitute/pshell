package ch.psi.pshell.ui;

import ch.psi.pshell.core.CommandSource;
import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Configuration.LogLevel;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.DevicePool;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.core.LogManager;
import ch.psi.pshell.swing.HelpContentsDialog;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.Shell;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.Config;
import ch.psi.utils.State;
import ch.psi.utils.Sys;
import ch.psi.utils.Sys.OSFamily;
import ch.psi.pshell.core.PlotListener;
import ch.psi.pshell.core.PluginManager;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.core.UserInterface;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plotter.Client;
import ch.psi.pshell.plotter.Plotter;
import ch.psi.pshell.plotter.PlotterBinder;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.swing.ScanEditorPanel;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.Str;
import ch.psi.utils.swing.ConfigDialog;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MainFrame.LookAndFeelType;
import static ch.psi.utils.swing.MainFrame.isDark;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import javax.swing.WindowConstants;
import javax.swing.event.SwingPropertyChangeSupport;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

/**
 * The application singleton object.
 */
public class App extends ObservableBase<AppListener> {

    private static App instance;
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static String[] arguments;
    private SwingPropertyChangeSupport pcs;

    static public void main(String[] args) {
        try {
            System.out.println(getHeaderMessage());
            init(args);
            createInstance().start();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    static public void init(String[] args) {
        arguments = args;
        if (isForcedHeadless()) {
            System.setProperty("java.awt.headless", "true");
        }

        Level consoleLogLevel = Level.WARNING;
        try {
            consoleLogLevel = Level.parse(getArgumentValue("clog"));
        } catch (Exception ex) {
        }
        LogManager.setConsoleLoggerLevel(consoleLogLevel);

        appendLibraryPath();
        appendClassPath();
        applyLookAndFeel();

        //Parse arguments to set system properties
        if (Setup.getJarFile() != null) {
            try {
                //URL url = App.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
                //Manifest manifest = new Manifest(url.openStream());              
                JarInputStream jarStream = new JarInputStream(new FileInputStream(new File(Setup.getJarFile())));
                Manifest manifest = jarStream.getManifest();
                Attributes attr = manifest.getMainAttributes();
                String buildTime = attr.getValue("Build-Time");
                if (buildTime != null) {
                    System.setProperty("pshell.build.time", buildTime);
                }
                String buildType = attr.getValue("Build-Type");
                if (buildType != null) {
                    System.setProperty("pshell.build.type", buildType);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        PshellProperties pshellProperties = new PshellProperties();
        File propertiesFile = new File("pshell.properties");
        if (propertiesFile.exists()) {
            try {
                pshellProperties.load(propertiesFile.getPath());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        if (isArgumentDefined("setp")) {
            System.setProperty(Context.PROPERTY_SETUP_FILE, getArgumentValue("setp"));
        } else if (Config.isStringDefined(pshellProperties.setp)) {
            System.setProperty(Context.PROPERTY_SETUP_FILE, pshellProperties.setp);
        }

        if (isArgumentDefined("home")) {
            System.setProperty(Setup.PROPERTY_HOME_PATH, getArgumentValue("home"));
        } else if (Config.isStringDefined(pshellProperties.home)) {
            System.setProperty(Setup.PROPERTY_HOME_PATH, pshellProperties.home);
        }

        if (isArgumentDefined("conf")) {
            System.setProperty(Setup.PROPERTY_CONFIG_FILE, getArgumentValue("conf"));
        } else if (Config.isStringDefined(pshellProperties.conf)) {
            System.setProperty(Setup.PROPERTY_CONFIG_FILE, pshellProperties.conf);
        }

        if (isArgumentDefined("plug")) {
            System.setProperty(Setup.PROPERTY_PLUGINS_FILE, getArgumentValue("plug"));
        } else if (Config.isStringDefined(pshellProperties.plug)) {
            System.setProperty(Setup.PROPERTY_PLUGINS_FILE, pshellProperties.plug);
        }

        if (isArgumentDefined("task")) {
            System.setProperty(Setup.PROPERTY_TASKS_FILE, getArgumentValue("task"));
        } else if (Config.isStringDefined(pshellProperties.task)) {
            System.setProperty(Setup.PROPERTY_TASKS_FILE, pshellProperties.task);
        }

        if (isArgumentDefined("sets")) {
            System.setProperty(Setup.PROPERTY_SETTINGS_FILE, getArgumentValue("sets"));
        } else if (Config.isStringDefined(pshellProperties.sets)) {
            System.setProperty(Setup.PROPERTY_SETTINGS_FILE, pshellProperties.sets);
        }

        if (isArgumentDefined("pini")) {
            System.setProperty(Setup.PROPERTY_PARALLEL_INIT, getArgumentValue("pini"));
        } else if (Config.isStringDefined(pshellProperties.pini)) {
            System.setProperty(Setup.PROPERTY_PARALLEL_INIT, pshellProperties.pini);
        }

        if (isArgumentDefined("outp")) {
            System.setProperty(Setup.PROPERTY_OUTPUT_PATH, getArgumentValue("outp"));
        } else if (Config.isStringDefined(pshellProperties.outp)) {
            System.setProperty(Setup.PROPERTY_OUTPUT_PATH, pshellProperties.outp);
        }

        if (isArgumentDefined("data")) {
            System.setProperty(Setup.PROPERTY_DATA_PATH, getArgumentValue("data"));
        } else if (Config.isStringDefined(pshellProperties.data)) {
            System.setProperty(Setup.PROPERTY_DATA_PATH, pshellProperties.data);
        }

        if (isArgumentDefined("scpt")) {
            System.setProperty(Setup.PROPERTY_SCRIPT_PATH, getArgumentValue("scpt"));
        } else if (Config.isStringDefined(pshellProperties.scpt)) {
            System.setProperty(Setup.PROPERTY_SCRIPT_PATH, pshellProperties.scpt);
        }

        if (isArgumentDefined("pool")) {
            System.setProperty(Setup.PROPERTY_DEVICES_FILE, getArgumentValue("pool"));
        } else if (pshellProperties.pool != null) {
            System.setProperty(Setup.PROPERTY_DEVICES_FILE, pshellProperties.pool.toString());
        }

        if (isArgumentDefined("type")) {
            System.setProperty(Setup.PROPERTY_SCRIPT_TYPE, getArgumentValue("type"));
        }

        if (isArgumentDefined("dfmt")) {
            System.setProperty(Configuration.PROPERTY_DATA_PROVIDER, getArgumentValue("dfmt"));
        }

        if (isArgumentDefined("dlay")) {
            System.setProperty(Configuration.PROPERTY_DATA_LAYOUT, getArgumentValue("dlay"));
        }

        if (hasArgument("nbcf")) {
            System.setProperty(Configuration.PROPERTY_NO_BYTECODE_FILES, String.valueOf(getBoolArgumentValue("nbcf")));
        }

        if (isArgumentDefined("user")) {
            System.setProperty(Context.PROPERTY_USER, getArgumentValue("user"));
        } else if (Config.isStringDefined(pshellProperties.user)) {
            System.setProperty(Context.PROPERTY_USER, pshellProperties.user);
        }
        if (isArgumentDefined("clog")) {
            System.setProperty(Configuration.PROPERTY_CONSOLE_LOG, getArgumentValue("clog"));
        } else if (pshellProperties.consoleLog != null) {
            System.setProperty(Configuration.PROPERTY_CONSOLE_LOG, pshellProperties.consoleLog.toString());
        }

        //Only used if View is not instantiated
        if (isArgumentDefined("quality")) {
            System.setProperty(PlotPanel.PROPERTY_PLOT_QUALITY, Plot.Quality.valueOf(getArgumentValue("quality")).toString());
        }

        if (isArgumentDefined("scrp")) {
            System.setProperty(Setup.PROPERTY_EXT_SCRIPT_PATH, String.join(";", getArgumentValues("scrp")));
        }

        if (isLocalMode()) {
            System.setProperty(Context.PROPERTY_LOCAL_MODE, "true");
        }

        if (isBareMode()) {
            System.setProperty(Context.PROPERTY_BARE_MODE, "true");
        }

        if (isEmptyMode()) {
            System.setProperty(Context.PROPERTY_EMPTY_MODE, "true");
        }

        if (isGenericMode()) {
            System.setProperty(Context.PROPERTY_GENERIC_MODE, "true");
        }
        
        if (isDisabled()) {
            System.setProperty(Context.PROPERTY_DISABLED, "true");
        }

        if (isServerMode()) {
            System.setProperty(Context.PROPERTY_SERVER_MODE, "true");
        }

        if (isSimulation()) {
            System.setProperty(Context.PROPERTY_SIMULATION, "true");
        }

        if (isScanPlottingDisabled()) {
            setScanPlottingActive(false);
        }

        if (isScanPrintingDisabled()) {
            setScanPrintingActive(false);
        }

        Boolean enableExtract = isEnableExtract();
        if (enableExtract!=null) {
            System.setProperty(Context.PROPERTY_ENABLE_EXTRACT, String.valueOf(enableExtract));
        }

        Boolean enableVersioning = isEnableVersioning();
        if (enableVersioning!=null) {
            System.setProperty(Context.PROPERTY_ENABLE_VERSIONING, String.valueOf(enableVersioning));
        }
        
        List<File> packages = getPackageArgs();
        if (packages.size()>0){
            System.setProperty(Context.PROPERTY_PACKAGES, String.join(";", Convert.toStringArray(packages)));
        }
        
        if (isVolatile()){
            System.setProperty(DevicePool.PROPERTY_EPICS_DEFAULT_CONFIG, defaultJcaeProperties);
        }        
        if (isArgumentDefined("jcae")) {            
            String jcae = getArgumentValue("jcae");
            String fileName = Setup.expand(jcae);
            if (new File (fileName).isFile()){
                System.setProperty(DevicePool.PROPERTY_EPICS_CONFIG_FILE, fileName);
            } else if (isVolatile() && jcae.contains("=")){
                System.setProperty(DevicePool.PROPERTY_EPICS_DEFAULT_CONFIG, jcae.replaceAll("\\|", "\n"));
            }           
        }                         
        //Raise exception if wrong formatting;
        getTaskArgs();

        System.setProperty(Context.PROPERTY_FILE_LOCK, isFileLock() ? "true" : "false");        
        System.setProperty(Context.PROPERTY_SESSIONS_ENABLED, isHandlingSessions() ? "true" : "false" );

        if (isVolatile()) {
            try {
                Path tempDir = Files.createTempDirectory("pshell_home");
                IO.deleteFolderOnExit(tempDir.toFile());
                System.setProperty(Setup.PROPERTY_HOME_PATH, tempDir.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }
    }

    final static String defaultJcaeProperties=  "ch.psi.jcae.ContextFactory.autoAddressList=true\n" +
                                                "ch.psi.jcae.ContextFactory.useShellVariables=true\n" +
                                                "ch.psi.jcae.ContextFactory.maxArrayBytes=20000000\n" +
                                                "ch.psi.jcae.ContextFactory.maxSendArrayBytes=100000";   
    

    public State getState() {
        return context.getState();
    }

    private Context context;

    public Context getContext() {
        return context;
    }

    private View view;

    public View getMainFrame() {
        return view;
    }

    static String getHeaderMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourceBundleValue("Application.description")).append("\n");
        sb.append(App.getResourceBundleValue("Application.copyright"));
        return sb.toString();
    }

    static String getHelpMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Arguments:");
        sb.append("\n\t-h\tPrint this help message");
        sb.append("\n\t-c\tStart command line interface");
        sb.append("\n\t-x\tStart GUI with plots only");
        sb.append("\n\t-v\tStart in server mode");
        sb.append("\n\t-w\tStart the GUI shell console window only");
        sb.append("\n\t-l\tExecution in local mode: no servers, no lock, no versioning, no context persistence");
        sb.append("\n\t-d\tDetached mode: no Workbench, Panel plugins are instantiated in a private frames");
        sb.append("\n\t-k\tPersist state of  detached frames");
        sb.append("\n\t-i\tNo file locks");
        sb.append("\n\t-b\tBare mode: no plugin is loaded");
        sb.append("\n\t-e\tEmpty mode: device pool is not loaded");
        sb.append("\n\t-g\tLocal initialization script is not executed in startup");
        sb.append("\n\t-j\tDisable session management");
        sb.append("\n\t-u\tHide graphical user interface at startup");
        sb.append("\n\t-r\tRedirect standard output to Output window");
        sb.append("\n\t-o\tStart in offline mode: data access only");
        sb.append("\n\t-s\tAll devices are simulated");
        sb.append("\n\t-n\tInterpreter is not started");
        sb.append("\n\t-q\tQuiet mode");
        sb.append("\n\t-a\tAuto close after executing file");
        sb.append("\n\t-y\tHeadless mode");
        sb.append("\n\t-z\tHome folder is volatile (created in tmp folder)");        
        sb.append("\n\t-home=<path>\tSet the home folder (default is ./home)");
        sb.append("\n\t-outp=<path>\tSet the output folder (default is {home})");
        sb.append("\n\t-data=<path>\tSet the data folder (default is {home}/data)");
        sb.append("\n\t-scpt=<path>\tSet the script folder (default is {home}/script)");
        sb.append("\n\t-setp=<path>\tOverride the setup file(default is {config}/setup.properties)");
        sb.append("\n\t-conf=<path>\tOverride the config file(default is {config}/config.properties)");
        sb.append("\n\t-pool=<path>\tOverride the device pool configuration file");
        sb.append("\n\t-plug=<path>\tOverride the plugin definition file (default is {config}/plugins.properties)");
        sb.append("\n\t-task=<path>\tOverride the task definition file (default is {config}/tasks.properties)");
        sb.append("\n\t-sets=<path>\tOverride the settings file (default is {config}/settings.properties)");
        sb.append("\n\t-pini=<value>\tOverride config flag for parallel initialization (values: true or false)");
        sb.append("\n\t-clog=<level>\tSet the console logging level");
        sb.append("\n\t-user=<name>\tSet the startup user");
        sb.append("\n\t-type=<ext>\tSet the script type, overriding the setup");
        sb.append("\n\t-dfmt=<format>\tSet the data format, overriding the configuration: h5, txt, csv or fda");
        sb.append("\n\t-dlay=<layout>\tSet the data layout, overriding the configuration: default, table, sf or fda");
        sb.append("\n\t-dspt        \tDisable scan plots");
        sb.append("\n\t-dspr        \tDisable printing scans to console");
        sb.append("\n\t-sbar        \tAppend status bar to detached windows");
        sb.append("\n\t-dplt        \tCreate plots for detached windows");
        sb.append("\n\t-strp        \tShow strip chart window (can be used together with -f)");
        sb.append("\n\t-dtpn        \tShow data panel window only (can be used together with -f)");
        sb.append("\n\t-help        \tStart the GUI help window");
        sb.append("\n\t-full        \tStart in full screen mode");
        sb.append("\n\t-dual        \tStart GUI and command line interface (not allowed if running in the background)");
        sb.append("\n\t-extr=<value>\tForce (true) or disable (false) extraction of startup and utility scrips");
        sb.append("\n\t-vers=<value>\tForce versioning enabled (true) or disabled (false)");
        sb.append("\n\t-nbcf=<value>\tForce disabling (true) or enabling (false) the use of bytecode files");
        sb.append("\n\t-strh=<path> \tStrip chart default configuration folder");
        sb.append("\n\t-libp=<path> \tAdd to library path");
        sb.append("\n\t-clsp=<path> \tAdd to class path");
        sb.append("\n\t-scrp=<path> \tAdd to script path");
        sb.append("\n\t-jcae=<file> \tForce EPICS config file (or, in volatile mode, EPICS options separated by '|')");
        sb.append("\n\t-laf=<name>  \tLook and feel: system, metal, nimbus, darcula, flat, or dark");
        sb.append("\n\t-size=WxH    \tSet application window size if GUI state not persisted");
        sb.append("\n\t-args=<...>  \tProvide arguments to interpreter");
        sb.append("\n\t-f=<...>     \tFile to run (together with -c option) or open in file in editor");
        sb.append("\n\t-t=<...>     \tStart a task using the format script,delay,interval");
        sb.append("\n\t-p=<...>     \tLoad a plugin");
        sb.append("\n\t-m=<...>     \tLoad a package");

        if (isStripChart()) {
            sb.append("\n\nStripChart arguments:");
            sb.append("\n\t-f=<..>\tOpen a StripChart configuration file (.scd)");
            sb.append("\n\t-config=<..>\tJSON configuration string (as in .scd file) or list of channel names");
            sb.append("\n\t-start\tStart the data displaying immediately");
            sb.append("\n\t-v\tCreate a StripChart server");
            sb.append("\n\t-attach\tShared mode: try connecting to existing server, or create one if not available");
            sb.append("\n\t-background_color=<..>\tSet default plot background color");
            sb.append("\n\t-grid_color=<..>\tSet default plot grid color");
            sb.append("\n\t-tick_label_font=name:size\tSet font for time plot tick labels");
            sb.append("\n\t-alarm_interval=<..>\tSet the alarm timer interval (default 1000ms)");
            sb.append("\n\t-alarm_file=<..>\tSet alarm sound file (default use system beep)");
        }
        sb.append("\n");
        return sb.toString();
    }

    static public boolean isLocalMode() {
        return getBoolArgumentValue("l") || isPlotOnly() || isHelpOnly() || isDataPanel() || isStripChart() || isOffline() || isVolatile();
    }

    static public boolean isBareMode() {
        return getBoolArgumentValue("b") || isVolatile();
    }

    static public boolean isEmptyMode() {
        return getBoolArgumentValue("e");
    }

    static public boolean isGenericMode() {
        return getBoolArgumentValue("g");
    }

    static public boolean isHandlingSessions() {
        boolean disableSessions =getBoolArgumentValue("j")  || isPlotOnly() || isHelpOnly() || isDataPanel() || isStripChart() || isVolatile() || isDetached();
        return !disableSessions;
    }    
   
    static public boolean isFileLock() {
        return !getBoolArgumentValue("i");
    }

    static public boolean isCli() {
        return getBoolArgumentValue("c");
    }

    static public boolean isDebug() {
        return getBoolArgumentValue("debug");
    }

    static public boolean isServerMode() {
        return getBoolArgumentValue("v");
    }

    static public boolean isOffscreenPlotting() {
        return isServerMode() || isHeadless();
    }

    static public boolean isGui() {
        return !isCli() && !isServerMode() && !isHeadless();
    }

    static public boolean isDual() {
        return isGui() && getBoolArgumentValue("dual");
    }

    static public boolean isAttach() {       
        return isGui() && getBoolArgumentValue("attach");
    }

    static public boolean isConsole() {
        return isGui() && getBoolArgumentValue("w");
    }

    static public boolean isDetachedPersisted() {
        return getBoolArgumentValue("k");
    }

    static public boolean isDetached() {
        return isGui() && getBoolArgumentValue("d");
    }

    static public boolean isDetachedPanelsPersisted() {
        return isDetached() && isDetachedPersisted() && !isFullScreen();
    }

    static public boolean isDetachedAppendStatusBar() {
        return isDetached() && getBoolArgumentValue("sbar");
    }

    static public boolean isFullScreen() {
        return isGui() && getBoolArgumentValue("full");
    }

    static public boolean isQuiet() {
        return getBoolArgumentValue("q");
    }

    static public boolean isVolatile() {
        return getBoolArgumentValue("z");
    }

    static public String getDetachedPanel() {
        return getArgumentValue("d");
    }

    //If running from jar and no console, redirect stdout and stderr to Output window
    //Console null and no jar means debbuging in IDE
    static public boolean isOutputRedirected() {
        return (((System.console() == null) && (Context.getInstance().getSetup().getJarFile() != null))
                || (getBoolArgumentValue("r")));
    }

    static public boolean isOffline() {
        return getBoolArgumentValue("o") || isDataPanel();
    }

    static public boolean isSimulation() {
        return getBoolArgumentValue("s");
    }

    static public boolean isDetachedPlots() {
        return App.getBoolArgumentValue("dplt");
    }

    static public boolean isPlotOnly() {
        return App.getBoolArgumentValue("x");
    }

    static public boolean isHelpOnly() {
        return (getBoolArgumentValue("help") && !isHeadless());
    }

    static public boolean isAutoClose() {
        return App.getBoolArgumentValue("a");
    }

    static public boolean isStripChart() {
        return getBoolArgumentValue("strp");
    }

    static public boolean isStripChartServer() {
        return isStripChart() && ((isAttach() || (isServerMode())));
    }

    static public boolean isDataPanel() {
        return getBoolArgumentValue("dtpn");
    }

    static public boolean isScanPlottingDisabled() {
        return getBoolArgumentValue("dspt");
    }

    static public boolean isScanPrintingDisabled() {
        return getBoolArgumentValue("dspr");
    }

    static public Boolean isEnableExtract() {   
        if (isDisabled()){
            return false;
        }
        if (hasArgument("extr")){
            return getBoolArgumentValue("extr");
        }
        if (isVolatile()){
            return true;
        }        
        return null; //Default
    }

    static public Boolean isEnableVersioning(){
        if (hasArgument("vers")){
            return getBoolArgumentValue("vers");
        }
        return null; //Default
    }
   
    static public Dimension getSize() {
        try {
            String opt = getArgumentValue("size");
            String[]tokens= opt.toLowerCase().split("x");
            return new Dimension(Integer.valueOf(tokens[0]), Integer.valueOf(tokens[1]));
        } catch (Exception ex){
        }
        return null;
    }

    static public File getFileArg() {
        try {
            if (!hasArgument("f")) {
                return null;
            }
            String fileName = getArgumentValue("f");
            if (Context.getInstance() != null) {
                fileName = Context.getInstance().getSetup().expandPath(fileName);
            }
            return new File(fileName);
        } catch (Exception ex) {
            return null;
        }
    }

    static public List<File> getFileArgs() {
        ArrayList<File> ret = new ArrayList<File>();
        for (String fileName : getArgumentValues("f")) {
            try {
                File file = new File(fileName);
                if (Context.getInstance() != null) {
                    fileName = Context.getInstance().getSetup().expandPath(fileName);
                    file = new File(fileName);
                    if (!file.exists()) {
                        File f = Context.getInstance().getScriptFile(fileName);
                        file = (f != null) ? f : file;
                    }
                }
                ret.add(file);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        return ret;
    }
    
    static public List<File> getPackageArgs() {
        ArrayList<File> ret = new ArrayList<File>();
        for (String path : getArgumentValues("m")) {
            try {
                if (Context.getInstance() != null) {
                    path = Context.getInstance().getSetup().expandPath(path);                    
                }
                File file = new File(path);
                if (file.exists()  && file.isDirectory()){
                    ret.add(file);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        return ret;
    }
    
    static public List<ch.psi.pshell.core.Task> getTaskArgs() {
        List<ch.psi.pshell.core.Task> ret = new ArrayList<>();
        //Check formatting
        for (String str : getArgumentValues("t")){
            ret.add(ch.psi.pshell.core.Task.fromString(str));
        }
        return ret;
    }
    

    static public String getPlotServer() {
        return App.getArgumentValue("psrv");
    }

    static public Map<String, Object> getInterpreterArgs() {
        try {
            if (!hasArgument("args")) {
                return null;
            }
            HashMap<String, Object> ret = new HashMap<>();
            for (String token : getArgumentValue("args").split(",")) {
                if (token.contains(":")) {
                    String name = token.substring(0, token.indexOf(":")).trim();
                    String value = token.substring(token.indexOf(":") + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        ret.put(name, value.substring(1, value.length() - 1));
                    } else if (value.equalsIgnoreCase("false")) {
                        ret.put(name, Boolean.FALSE);
                    } else if (value.equalsIgnoreCase("true")) {
                        ret.put(name, Boolean.TRUE);
                    } else {
                        try {
                            ret.put(name, Integer.valueOf(value));
                        } catch (Exception ex) {
                            try {
                                ret.put(name, Double.valueOf(value));
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (!ret.containsKey(name)) {
                        System.err.println("Invalid argument value: " + value);
                    }
                }
            }
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static File getStripChartFolderArg() {
        File defaultFolder = null;
        if (hasArgument("strh")) {
            String defaultFolderName = getArgumentValue("strh");
            if (defaultFolderName != null) {
                if (Context.getInstance() != null) {
                    defaultFolderName = Context.getInstance().getSetup().expandPath(defaultFolderName);
                }
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

    /**
     * If disabled the interpreter is not instantiated
     */
    static public boolean isDisabled() {
        return getBoolArgumentValue("n");
    }

    static public boolean isHelpMessage() {
        return hasArgument("h");
    }

    static public boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    static public boolean isForcedHeadless() {
        return getBoolArgumentValue("y") || isHelpMessage() ;
    }

    //Resources
    static public String getResourceBundleValue(String key) {
        return getResourceBundleValue(App.class, key);
    }

    static public String getResourceBundleValue(Class cls, String key) {
        return ResourceBundle.getBundle("ch/psi/pshell/ui/" + cls.getSimpleName()).getString(key);
    }

    static public URL getResourceUrl(String name) {
        return App.class.getResource("/ch/psi/pshell/ui/" + name);
    }
    
    static public Image getResourceImage(String name) {
        return Toolkit.getDefaultToolkit().getImage(getResourceUrl(name));
    }
    
    public static ImageIcon searchIcon(String name) {
        return MainFrame.searchIcon(name, App.class, "/ch/psi/pshell/ui/") ;     
    }
    
    static public String getApplicationName() {
        return getResourceBundleValue("Application.name");
    }

    static public String getApplicationVersion() {
        return getResourceBundleValue("Application.version");
    }

    static public String getApplicationBuildTime() {
        return System.getProperty("pshell.build.time");
    }

    static public String getApplicationBuildType() {
        return System.getProperty("pshell.build.type");
    }

    static public String getApplicationBuildInfo() {
        String buildTime = getApplicationBuildTime();
        String buildType = getApplicationBuildType();
        String ret = getResourceBundleValue("Application.version");
        if (buildTime != null) {
            ret += " (build " + buildTime + "-" + String.valueOf(buildType) + ")";
        }
        return ret;
    }

    static public String getApplicationId() {
        return getResourceBundleValue("Application.id");
    }

    static public String getApplicationTitle() {
        return getResourceBundleValue("Application.title");
    }

    static public String getVendorId() {
        return getResourceBundleValue("Application.vendorId");
    }

    static public Image getIconSmall() {
        return getResourceImage("IconSmall.png");
    }

    static public Image getIconLarge() {
        return getResourceImage("IconLarge.png");
    }

    //Possibility to fix defaults for home folder & other command line options in properties file, if not in arguments        
    static public class PshellProperties extends Config {

        public String home;
        public String outp;
        public String scpt;
        public String data;
        public String user;
        public String setp;
        public String conf;
        public String plug;
        public String task;
        public String pool;
        public String sets;
        public String pini;
        public LogLevel consoleLog;
    }

    Object stripChartServer;

    protected void startup() {
        System.out.println("Version " + getApplicationBuildInfo());

        if (isHelpMessage()) {
            System.out.println(getHelpMessage());
            return;
        }

        if (isHelpOnly()) {
            HelpContentsDialog dialog = new HelpContentsDialog(new javax.swing.JFrame(), true);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });
            dialog.setIconImage(getIconSmall());
            dialog.setTitle("PShell Help Contents");
            dialog.setSize(800, 600);
            SwingUtils.centerComponent(null, dialog);
            dialog.setVisible(true);
            return;
        }

        if (isLocalMode()) {
            setContextPersisted(false);
        }

        Context.createInstance();
        logger.log(Level.INFO, "Version: " + getApplicationBuildInfo());

        context = Context.getInstance();
        context.addListener(new ContextAdapter() {
            @Override
            public void onContextStateChanged(State state, State former) {
                try {
                    if ((former == State.Initializing) && (state == State.Ready)){
                        for (ch.psi.pshell.core.Task task: getTaskArgs()){
                            context.startTask(task);
                        }
                    }
                    for (AppListener listener : getListeners()) {
                        try {
                            listener.onStateChanged(state, former);
                        } catch (Exception ex) {
                        }
                    }
                    if (pcs != null) {
                        pcs.firePropertyChange("appstate", former, state);
                    }
                    checkNext(state);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }

            @Override
            public void onPreferenceChange(ViewPreference preference, Object value) {
                switch (preference) {
                    case STATUS:
                        if (value == null) {
                            value = context.getState().toString();
                        }
                        pcs.firePropertyChange("message", "", String.valueOf(value));
                        break;
                }
            }

            @Override
            public void onShellStdout(String str) {
                if (isDetached()) {
                    if (!isOutputRedirected()) {
                        System.out.println(str);
                    }
                }
            }

            @Override
            public void onShellStderr(String str) {
                if (isDetached()) {
                    if (!isOutputRedirected()) {
                        System.err.println(str);
                    }
                }
            }

        });

        if (!isHeadless()) {
            context.setLocalUserInterface(new UserInterface() {

                @Override
                public String getString(String message, String defaultValue) {
                    return SwingUtils.getString(getMainFrame(), message, defaultValue);
                }

                @Override
                public String getString(String message, String defaultValue, String[] alternatives) {

                    return SwingUtils.getString(getMainFrame(), message, alternatives, defaultValue);
                }

                @Override
                public String getPassword(String message, String title) {
                    return SwingUtils.getPassword(getMainFrame(), title, message);
                }

                @Override
                public String getOption(String message, String type) {
                    SwingUtils.OptionResult ret = SwingUtils.showOption(getMainFrame(), null, message, SwingUtils.OptionType.valueOf(type));
                    if (ret == SwingUtils.OptionResult.Closed) {
                        ret = SwingUtils.OptionResult.Cancel;
                    }
                    return ret.toString();
                }

                @Override
                public void showMessage(String message, String title, boolean blocking) {
                    if (blocking) {
                        SwingUtils.showMessageBlocking(getMainFrame(), title, message);
                    } else {
                        SwingUtils.showMessage(getMainFrame(), title, message);
                    }
                }

                @Override
                public Object showPanel(GenericDevice dev) {
                    return getDevicePanelManager().showPanel(dev);
                }

                @Override
                public Object showPanel(Config config) throws InterruptedException {
                    return ConfigDialog.showConfigEditor(getMainFrame(), config, false, false);
                }

                @Override
                public int waitKey(int timeout) throws InterruptedException {
                    return Shell.waitKey(timeout);
                }
            });

        }

        loadCommandLinePlugins();
        context.redirectScriptStdio();
        if (getPlotServer() != null) {
            startPlotServerConnection(getPlotServer(), 5000);
        }
        if (isGui()) {
            pcs = new SwingPropertyChangeSupport(this, true);
            if (isConsole()) {
                startStandaloneShell();
                logger.log(Level.INFO, "Create shell");
            } else if (isStripChart()) {
                if (isAttach()) {
                    try {
                        String ret = StripChartServer.create(getFileArg(), getArgumentValue("config"), getBoolArgumentValue("start"));
                        System.out.println("Panel handled by server: " + ret);
                        System.exit(0);
                    } catch (Exception ex) {
                        if ((ex.getCause() != null) && (ex.getCause() instanceof ConnectException)) {
                            System.out.println("Server not found");
                            StripChart.create(getFileArg(), getArgumentValue("config"), getStripChartFolderArg(), getBoolArgumentValue("start"), false);
                            stripChartServer = new StripChartServer();
                        } else {
                            ex.printStackTrace();
                            System.exit(0);
                        }
                    }
                } else {
                    StripChart.create(getFileArg(), getArgumentValue("config"), getStripChartFolderArg(), getBoolArgumentValue("start"), true);
                }
            } else if (isDataPanel()) {
                DataPanel.createPanel(getFileArg());
            } else {
                if (isDual()) {
                    Console c = new Console();
                    c.start();
                }
                if (isDetached()) {
                    logger.log(Level.INFO, "Create panels");
                    if (isDetachedPlots()) {
                        setConsolePlotEnvironment(null);
                        setupConsoleScanPlotting();
                    }
                } else {
                    logger.log(Level.INFO, "Create workbench");
                    registerProcessors();
                    view = new View();
                    if (isFullScreen()) {
                        SwingUtils.setFullScreen(view, true);
                    } else if (isContextPersisted()) {
                        view.restoreState();
                    } else {
                        if (isPlotOnly() && isDetachedPersisted()) {
                            try {
                                MainFrame.restore(view, getPlotWindowStatePath());
                            } catch (Exception ex) {
                                Logger.getLogger(DataPanel.class.getName()).log(Level.INFO, null, ex);
                            }
                        } else {
                            Dimension size = getSize();
                            if (size!=null){
                                view.setSize(size);
                            }
                            SwingUtils.centerComponent(null, view);
                        }
                    }
                    view.setVisible(!getBoolArgumentValue("u"));
                    outputPanel = (OutputPanel) SwingUtils.getComponentByName(getMainFrame(), "outputPanel");
                }
            }
            //setupStatusBar(getStatusBar());

            if (!isOffline()) {
                //Not running in GUI thread
                new Thread(() -> {
                    context.start();
                    if (isPlotOnly() || isConsole()) {
                        File file = getFileArg();
                        if (file != null) {
                            runFile(file, false);
                            if (isAutoClose()) {
                                exit(this);
                            }
                        }
                    }
                }, "Context startup").start();
            } else {
                context.disable();
            }
        } else {
            if (isStripChart() && isServerMode()) {
                stripChartServer = new StripChartServer();
                context.start();
            } else {
                context.start();
                File file = getFileArg();
                if (file != null) {
                    runFile(file, !isServerMode());
                    exit(this);
                } else {
                    setConsolePlotEnvironment(null);
                    if (isCli()) {
                        logger.log(Level.INFO, "Start console");
                        try {
                            console = new ch.psi.pshell.core.Console();
                            setScanPrintingActive(!isServerMode() && !isScanPrintingDisabled());
                            setupConsoleScanPlotting();
                            console.run(System.in, System.out, !isServerMode());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                    } else if (isServerMode()) {
                        logger.log(Level.INFO, "Start server");
                    }
                    if (isHeadless()) {
                        logger.log(Level.WARNING, "Headless mode");
                    }
                    if (isOffscreenPlotting()) {
                        setupConsoleScanPlotting();
                    }
                }
            }
        }
    }

    void registerProcessors() {
        Processor.addServiceProvider(ScanEditorPanel.class);
        Processor.addServiceProvider(QueueProcessor.class);
    }

    Path getPlotWindowStatePath() {
        return Paths.get(context.getSetup().getContextPath(), "PlotWindow_WindowState.xml");
    }

    static volatile boolean scanPlottingActive = true;

    public static boolean isScanPlottingActive() {
        return scanPlottingActive;
    }

    public static void setScanPlottingActive(boolean value) {
        scanPlottingActive = value;
    }

    DevicePanelManager devicePanelManager;

    public DevicePanelManager getDevicePanelManager() {
        if (devicePanelManager == null) {
            devicePanelManager = new DevicePanelManager(view);
        }
        return devicePanelManager;
    }

    HashMap<String, PlotPanel> plotPanels = new HashMap<>();

    public PlotPanel getPlotPanel(String title, Window parent) {
        return getPlotPanel(title, parent, true);
    }

    PlotPanel getPlotPanel(String title, Window parent, boolean create) {
        title = checkPlotsTitle(title);
        PlotPanel plotPanel = plotPanels.get(title);
        if ((plotPanel != null) && (!isOffscreenPlotting() && !plotPanel.isDisplayable())) {
            plotPanels.remove(title);
            plotPanel = null;
        }
        if ((plotPanel == null) && create) {
            plotPanel = new PlotPanel();
            plotPanels.put(title, plotPanel);
            if (!isOffscreenPlotting()) {
                JFrame frame = SwingUtils.showFrame(parent, title, new Dimension(600, 400), plotPanel);
                frame.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            }
            plotPanel.initialize();
            plotPanel.setPlotTitle(title);
        }
        return plotPanel;
    }

    List<String> getPlotPanels() {
        return new ArrayList(plotPanels.keySet());
    }

    void removePlotPanel(String title) {
        title = checkPlotsTitle(title);
        if (!title.equals(checkPlotsTitle(null))) {
            plotPanels.remove(title);
        }
    }

    void setConsolePlotEnvironment(JFrame parent) {
        Context.getInstance().setPlotListener(new PlotListener() {
            @Override
            public List<Plot> plot(String title, PlotDescriptor[] plots) throws Exception {

                ArrayList<Plot> ret = new ArrayList<>();
                PlotPanel plotPanel = getPlotPanel(title, parent, true);
                plotPanel.clear();
                if ((plots != null) && (plots.length > 0)) {
                    for (PlotDescriptor plot : plots) {
                        try {
                            if (plot != null) {
                                ret.add(plotPanel.addPlot(plot));
                            } else {
                                ret.add(null);
                            }
                        } catch (Exception ex) {
                            if (plot == null) {
                            } else {
                                System.err.println("Error creating plot: " + String.valueOf((plot != null) ? plot.name : null));
                            }
                        }
                    }
                }
                return ret;
            }

            @Override
            public List<Plot> getPlots(String title) {
                title = checkPlotsTitle(title);
                PlotPanel plotPanel = plotPanels.get(title);
                if (plotPanel != null) {
                    return plotPanel.getPlots();
                }
                return new ArrayList<Plot>();
            }
        });

    }

    void setupConsoleScanPlotting() {
        try {
            PlotPanel scanPlot = new PlotPanel();
            plotPanels.put(checkPlotsTitle(null), scanPlot);
            scanPlot.initialize();
            scanPlot.setPlotTitle(checkPlotsTitle(null));

            context.addScanListener(new ScanListener() {
                final HashMap<Scan, String> plotTitles = new HashMap<>();

                @Override
                public void onScanStarted(Scan scan, String title) {
                    if (scanPlottingActive) {
                        title = checkPlotsTitle(title);
                        synchronized (plotTitles) {
                            plotTitles.put(scan, title);
                            PlotPanel plottingPanel = getPlotPanel(title, null, true);
                            plottingPanel.setPreferences(context.getPlotPreferences());
                            plottingPanel.triggerScanStarted(scan, title);
                        }
                    }
                }

                @Override
                public void onNewRecord(Scan scan, ScanRecord record) {
                    if (scanPlottingActive) {
                        PlotPanel plottingPanel = null;
                        synchronized (plotTitles) {
                            plottingPanel = getPlotPanel(plotTitles.get(scan), null, false);
                        }
                        if (plottingPanel != null) {
                            plottingPanel.triggerOnNewRecord(scan, record);
                        }
                    }
                }

                @Override
                public void onScanEnded(Scan scan, Exception ex) {
                    if (scanPlottingActive) {
                        PlotPanel plottingPanel = null;
                        synchronized (plotTitles) {
                            plottingPanel = getPlotPanel(plotTitles.get(scan), null, false);
                            plotTitles.remove(scan);
                        }
                        if (plottingPanel != null) {
                            plottingPanel.triggerScanEnded(scan, ex);
                        }
                    }
                }

            });
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }

    }

    String checkPlotsTitle(String title) {
        if ((title == null) || (title.isEmpty())) {
            title = "Plots";
        }
        return title;
    }

    ch.psi.pshell.core.Console console;
    Shell shell;

    static volatile boolean scanPrintingActive;

    public static boolean isScanPrintingActive() {
        return scanPrintingActive;
    }

    public static void setScanPrintingActive(boolean value) {
        scanPrintingActive = value;
        ch.psi.pshell.core.Console.setDefaultPrintScan(value);
        Shell.setDefaultPrintScan(value);
        if (getConsole() != null) {
            getConsole().setPrintScan(value);
        }
        if (getShell()!=null){
            getShell().setPrintScan(value);
        }
    }

    public static Shell getShell(){
        if (getInstance()!=null) {
            if (getInstance().shell != null) {
                return getInstance().shell;
            }
            if (getInstance().view != null) {
                return getInstance().view.getShell();
            }
        }
        return null;
    }

    public static  ch.psi.pshell.core.Console getConsole(){
        if (getInstance()!=null) {
            return (getInstance().console);
        }
        return null;
    }

    @Hidden
    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return pcs;
    }

    void runFile(File file, boolean printScan) {
        logger.log(Level.INFO, "Run file: " + file.getPath());
        registerProcessors();
        try {
            console = new ch.psi.pshell.core.Console();
            console.attachInterpreterOutput();
            setScanPrintingActive(printScan && !isScanPrintingDisabled());
            Object ret = evalFile(file, getInterpreterArgs(), true);

            if (ret != null) {
                System.out.println(ret);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.WARNING, null, ex);
        }
    }

    Processor runningProcessor;

    public Object evalFile(File file, Map<String, Object> args) throws Exception {
        return evalFile(file, args, false);
    }

    public Object evalFile(File file, Map<String, Object> args, boolean topLevel) throws Exception {
        context.clearAborted();
        Processor processor = getProcessor(file);
        if (processor != null) {
            if (view != null) {
                view.setCurrentProcessor(processor, topLevel);
            }
            runningProcessor = processor;
            try {
                processor.execute(processor.resolveFile(file.getPath()), args);
                Thread.sleep(100); //Give som time if processor does not change app state immediatelly;
                return processor.waitComplete(-1);
            } finally {
                runningProcessor = null;
            }
        }
        return context.evalFile(file.getPath(), args);
    }

    public Object evalStatement(String statement) throws Exception {
        context.clearAborted();
        return context.evalLine(statement);
    }

    public void abortEval(File file) throws InterruptedException {
        if (runningProcessor != null) {
            runningProcessor.abort();
        }
        Context.getInstance().abort();
    }

    Processor getProcessor(File file) throws Exception {
        if (!IO.getExtension(file).isEmpty()) {
            for (Processor processor : Processor.getServiceProviders()) {
                if (Arr.containsEqual(processor.getExtensions(), IO.getExtension(file))) {
                    return processor;
                }
            }
        }
        return null;
    }

    public static class ExecutionStage {

        final public File file;
        final public Map<String, Object> args;
        final public String statement;

        ExecutionStage(File file, Map<String, Object> args) {
            this.file = file;
            this.args = args;
            this.statement = null;
        }

        ExecutionStage(String statement) {
            this.file = null;
            this.args = null;
            this.statement = statement;
        }

        public String getArgsStr() {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (Object key : args.keySet()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    Object value = args.get(key);
                    String text;
                    try {
                        text = JsonSerializer.encode(value);
                    } catch (Exception ex) {
                        text = String.valueOf(value);
                    }
                    sb.append(key).append(":").append(text);
                }
            }
            return sb.toString();
        }
        
        public String toString() {
            if (file!=null){
                String args = getArgsStr();
                return file.toString() + ( args.isEmpty() ? " " :  "(" + args + ")") ;
            } else {
                return statement;
            }
        }
    }

    final List<ExecutionStage> executionQueue = new ArrayList<>();

    public void evalFileNext(File file) throws State.StateException {
        evalFileNext(file, null);
    }

    public void evalFileNext(File file, Map<String, Object> args) throws State.StateException {
        Context.getInstance().getState().assertProcessing();
        ExecutionStage stage = new ExecutionStage(file, args);        
        synchronized (executionQueue) {
            logger.info("Next stage: " + stage.toString());
            executionQueue.add(stage);
        }
        view.onExecQueueChanged(getExecutionQueue());
        getStatusBar().setTransitoryStatusMessage("Run next: " + file.toString(), 5000);
    }

    public void evalStatementNext(String statement) throws State.StateException {
        Context.getInstance().getState().assertProcessing();
        ExecutionStage stage = new ExecutionStage(statement);        
        synchronized (executionQueue) {
            logger.info("Next stage: " + stage.toString());
            executionQueue.add(stage);
        }
        view.onExecQueueChanged(getExecutionQueue());
        getStatusBar().setTransitoryStatusMessage("Run next: " + statement, 5000);
    }

    public ExecutionStage[] getExecutionQueue() {
        synchronized (executionQueue) {
            return executionQueue.toArray(new ExecutionStage[0]);
        }
    }

    public void cancelExecutionStage(int index) {        
        synchronized (executionQueue) {            
            if ((index >= 0) & (index < executionQueue.size())) {
                logger.info("Cancel execution stage index: " + index + " - " + executionQueue.get(index).toString());
                executionQueue.remove(index);
            }
        }
        view.onExecQueueChanged(getExecutionQueue());
    }

    public void cancelExecutionQueue() {        
        synchronized (executionQueue) {
            logger.info("Cancel execution queue");
            executionQueue.clear();
        }
        view.onExecQueueChanged(getExecutionQueue());
    }

    public boolean hasNextStage() {
        synchronized (executionQueue) {
            return executionQueue.size() > 0;
        }
    }

    ExecutionStage popNextStage() {
        synchronized (executionQueue) {
            if (executionQueue.size() > 0) {
                return executionQueue.remove(0);
            }
            return null;
        }
    }

    void checkNext(State state) {
        if (state == State.Ready) {
            ExecutionStage nextStage = popNextStage();
            if (nextStage != null) {
                //if (getContext().isSuccess()){                            
                SwingUtilities.invokeLater(() -> {
                    try {
                        view.onExecQueueChanged(getExecutionQueue());
                        startTask(new Task.ExecutorTask(nextStage));
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    };
                });
                //}                
            }
        }
    }

    //Acceps multiple -p options or plugin names can be separetate by ','
    void loadCommandLinePlugins() {
        if (hasArgument("p")) {
            for (String arg : getArgumentValues("p")) {
                for (String pluginName : arg.split(",")) {
                    if (new File(pluginName).exists()) {
                        context.getPluginManager().loadPlugin(pluginName);
                    } else if (Paths.get(context.getSetup().getPluginsPath(), pluginName).toFile().exists()) {
                        context.getPluginManager().loadPlugin(Paths.get(context.getSetup().getPluginsPath(), pluginName).toString());
                    } else {
                        //Try class name
                        context.getPluginManager().loadPluginClass(pluginName);
                    }
                }
            }
        }
    }

    StatusBar statusBar;

    StatusBar getStatusBar() {
        if (view != null) {
            return view.getStatusBar();
        }
        return statusBar;
    }

    @Hidden
    public SwingPropertyChangeSupport getSwingPropertyChangeSupport() {
        return pcs;
    }

    void startStandaloneShell() {
        statusBar = new StatusBar();
        shell = new Shell();
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(shell, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        setScanPrintingActive(!isScanPrintingDisabled());
        shell.initialize();
        JFrame frame = new JFrame();
        frame.setIconImage(getIconSmall());
        frame.setTitle("PShell Console");
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        SwingUtils.centerComponent(null, frame);
        frame.setVisible(true);
        shell.requestFocus();
        setConsolePlotEnvironment(frame);
        setupConsoleScanPlotting();
    }

    PlotterBinder pmb;

    void startPlotServerConnection(String url, int timeout) {
        Client pc = new Client(url, timeout);
        Plotter pm = pc.getProxy();
        pmb = new PlotterBinder(context, pm);
    }

    //Helper methods to tasks
    OutputPanel outputPanel;

    public void sendOutput(String str) {
        if ((outputPanel != null) && (outputPanel.isDisplayable())) {
            outputPanel.putOutput(str);
        }
    }

    public void sendTaskInit(String task) {
        sendOutput(OutputPanel.getTaskInitMessage(task));
    }

    public void sendTaskFinish(String task) {
        sendOutput(OutputPanel.getTaskFinishMessage(task));
    }

    public void sendError(String str) {
        if ((outputPanel != null) && (outputPanel.isDisplayable())) {
            outputPanel.putError(str);
        }
    }

    Task currentTask;

    public void startTask(Task task) throws State.StateException {
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                if ("state".equals(propertyName)) {
                    StateValue state = (StateValue) (e.getNewValue());
                    switch (state) {
                        case STARTED:
                            break;
                        case DONE:
                            if (view != null) {
                                view.onTaskFinished(task);
                            }
                            task.removePropertyChangeListener(this);
                            currentTask = null;
                            break;
                    }
                }
            }
        });
        if (task instanceof Task.Restart) {
            getState().assertNot(State.Initializing);
            abort();
        } else {
            getState().assertReady();
        }
        currentTask = task;
        task.execute();
    }

    public Task getRunningTask() {
        return currentTask;
    }

    public void abort() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    //Arguments
    static public String[] getArguments() {
        return arguments;
    }

    /**
     * Returns the command line argument value for a given key, if present. If
     * not defined returns null. If defined multiple times then returns the
     * latest.
     */
    static public String getArgumentValue(String name) {
        List<String> values = getArgumentValues(name);
        int entries = values.size();
        if (entries <= 0) {
            return null;
        }
        return values.get(entries - 1);
    }

    /**
     * Returns true if argument value is set and not empty
     */
    static public boolean isArgumentDefined(String name) {
        return ((getArgumentValue(name) != null) && (getArgumentValue(name).length() > 0));
    }

    /**
     * Returns the command line argument values for a given key. If key is no
     * present then returns an empty list.
     */
    static public List<String> getArgumentValues(String name) {
        ArrayList<String> argumentValues = new ArrayList<>();
        for (String arg : arguments) {
            if (arg.startsWith("-")) {
                arg = arg.substring(1);
            }
            int indexEq = arg.indexOf("=");
            if (indexEq>0){
                if (arg.substring(0, indexEq).trim().equals(name)) {
                    argumentValues.add(arg.substring(indexEq + 1, arg.length()).trim());
                }
            }
        }
        return argumentValues;
    }

    static public boolean hasArgument(String name) {
        if (arguments != null) {
            for (String arg : arguments) {
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                }
                if (arg.equals(name)) {
                    return true;
                }
                int indexEq = arg.indexOf("=");
                if (indexEq>0){
                    if (arg.substring(0, indexEq).trim().equals(name)) {
                        return true;
                    }                    
                }
            }
        }
        return false;
    }
    
    //accepts -option as -option=true
    static public boolean getBoolArgumentValue(String arg){
        if (hasArgument(arg)){
            String val = Str.toString(getArgumentValue(arg));
            return !(val.equalsIgnoreCase("false")) && !(val.equalsIgnoreCase("0"));
        }
        return false;
    } 
    

    boolean contextPersisted = true;

    public boolean isContextPersisted() {
        return contextPersisted;
    }

    protected void setContextPersisted(boolean value) {
        contextPersisted = value;
    }

    public void persistGuiState() {
        if (isContextPersisted()) {
            if (isFullScreen()) {
                logger.fine("Do not persist state in full screen mode");
            } else {
                try {
                    if (view != null) {
                        view.saveState();
                        view.savePersistedWindowsStates();
                    }
                } catch (Exception ex) {
                    logger.log(Level.INFO, null, ex);
                }
            }
        } else {
            if (App.isPlotOnly()) {
                if (App.isDetachedPersisted()) {
                    try {
                        MainFrame.save(view, getPlotWindowStatePath());
                    } catch (Exception ex) {
                        Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
            }
        }

    }

    static public App getInstance() {
        return instance;
    }

    private static App createInstance() {
        instance = new App();
        logger.info("Created");
        return instance;
    }

    void start() throws Exception {
        try {
            if (isCli()) {
                startup();
            } else {
                SwingUtilities.invokeAndWait(() -> {
                    startup();
                });
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            throw ex;
        }

    }

    static void applyLookAndFeel() {
        if (isHeadless()) {
            return;
        }
        if (Sys.getOSFamily() == OSFamily.Mac) {
            try {
                SwingUtils.setMacScreenMenuBar(getApplicationTitle());
            } catch (Exception ex) {
            }
        }
        //Look and feel: default is system
        String laf = getArgumentValue("laf");
        if (laf == null) {
            LookAndFeelType type;
            //Deprecated arguments
            if (hasArgument("mlaf")) {
                type = LookAndFeelType.metal;
            } else if (hasArgument("slaf")) {
                type = LookAndFeelType.system;
            } else if (hasArgument("nlaf")) {
                type = LookAndFeelType.nimbus;
            } else if (hasArgument("blaf")) {
                type = LookAndFeelType.dark;
            } else if (hasArgument("dlaf")) {
                type = LookAndFeelType.darcula;
            } else {
                // Default is system laf (or Metal, if no system installed).
                // However prefer Nimbus on Windows & Linux
                type = LookAndFeelType.nimbus;
                if (Sys.getOSFamily() == OSFamily.Mac) {
                    type = LookAndFeelType.system;
                }
            }
            laf = MainFrame.getLookAndFeel(type);
        }
        MainFrame.setLookAndFeel(laf);
        if (isDark()) {
            UIManager.put("FileView.directoryIcon", new ImageIcon(getResourceImage("FolderClosed.png")));
            UIManager.put("FileChooser.homeFolderIcon", new ImageIcon(getResourceImage("Home.png")));
            UIManager.put("FileView.computerIcon", new ImageIcon(getResourceImage("Computer.png")));
            UIManager.put("FileView.floppyDriveIcon", new ImageIcon(getResourceImage("Floppy.png")));
            UIManager.put("FileView.hardDriveIcon", new ImageIcon(getResourceImage("HardDrive.png")));
            UIManager.put("FileChooser.upFolderIcon", new ImageIcon(getResourceImage("FolderUp.png")));
            UIManager.put("FileChooser.newFolderIcon", new ImageIcon(getResourceImage("FolderNew.png")));
            UIManager.put("FileView.fileIcon", new ImageIcon(getResourceImage("File.png")));
            UIManager.put("FileChooser.listViewIcon", new ImageIcon(getResourceImage("List.png")));
            UIManager.put("FileChooser.detailsViewIcon", new ImageIcon(getResourceImage("Details.png")));
            UIManager.put("Tree.openIcon", new ImageIcon(getResourceImage("FolderOpen.png")));
            UIManager.put("Tree.closedIcon", new ImageIcon(getResourceImage("FolderClosed.png")));
            UIManager.put("Tree.leafIcon", new ImageIcon(getResourceImage("File.png")));
        }        
    }

    static void appendLibraryPath() {
        if (isArgumentDefined("libp")) {
            for (String path : getArgumentValues("libp")) {
                PluginManager.addToLibraryPath(new File(path));
            }
        }
    }

    static void appendClassPath() {
        if (hasArgument("cp")) {
            for (String path : getArgumentValue("cp").split(";")) {
                try {
                    Sys.addToClassPath(new File(path).getCanonicalPath());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
        if (isArgumentDefined("clsp")) {
            for (String path : getArgumentValues("clsp")) {
                try {
                    PluginManager.addToClassPath(new File(path));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
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
            persistGuiState();
            if (getContext() != null) {
                getContext().close();
            }
            for (AppListener listener : getListeners()) {
                try {
                    listener.willExit(origin);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
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
        logger.info("Aplication exit command by: " + String.valueOf(origin));
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
