package ch.psi.pshell.framework;

import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Miniconda;
import ch.psi.pshell.utils.Sys;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Entity class holding the application defaults and command-line options, folder structure 
 * and providing file name expansion.
 */
public class Setup extends ch.psi.pshell.devices.Setup {    
    public static transient final String DEFAULT_LOCAL_STARTUP_FILE_PREFIX  = "local";
    
    public static transient final String TOKEN_FILE_SEQUENTIAL_NUMBER = "{seq}";
    public static transient final String TOKEN_DAY_SEQUENTIAL_NUMBER = "{dseq}";
    public static transient final String TOKEN_EXEC_NAME = "{name}";
    public static transient final String TOKEN_EXEC_TYPE = "{type}";
    public static transient final String TOKEN_EXEC_COUNT = "{count}";
    public static transient final String TOKEN_EXEC_INDEX = "{index}";
    public static transient final String TOKEN_SESSION_ID = "{session_id}";
    public static transient final String TOKEN_SESSION_NAME = "{session_name}";
    public static transient final String TOKEN_USER = "{user}";    

    
    static String queuesPath = TOKEN_SCRIPT;       
    static String consolePath = TOKEN_LOGS+ "/console";  
    
    static String configFile = TOKEN_CONFIG + "/config.properties";
    static String configFilePlugins = TOKEN_CONFIG + "/plugins.properties";
    static String configFileDevices = TOKEN_CONFIG + "/devices.properties";
    static String configFileTasks = TOKEN_CONFIG + "/tasks.properties";
    static String configFileSettings = TOKEN_CONFIG + "/settings.properties";
    static String configFileVariables = TOKEN_CONFIG + "/variables.properties";
    static String configFileSessions = TOKEN_CONFIG + "/sessions.properties";
    
        
    static {               
        if (isRunningInIde()) {
            standardLibraryPath = Paths.get(getSourceAssemblyFolder(),  "script", "Lib").toString();
        }            
        additionalInit();
    }
    
    public static void init(){
        ch.psi.pshell.devices.Setup.init();
        additionalInit();
    }    
       
    
    public static void additionalInit(){
        configFile = ch.psi.pshell.app.Options.CONFIG.getPropertyFile(configFile);
        configFileDevices = Options.DEVICE_POOL.getPropertyFile(configFileDevices);
        configFilePlugins = Options.PLUGIN_CONFIG.getPropertyFile(configFilePlugins);
        configFileTasks = Options.TASK_CONFIG.getPropertyFile(configFileTasks);
        configFileSettings = Options.SETTINGS.getPropertyFile(configFileSettings);
        queuesPath = Options.QUEUES_PATH.getString(queuesPath);                
    }
    
    
    
    public static String expandPath(String path) {
        return expandPath(path, -1);
    }    

    public static String expandPath(String path, long timestamp) {
        if (path==null){
            return null;
        }
        if (path.contains(TOKEN_USER)) {
            path = path.replace(TOKEN_USER, Context.getUserName());
        }        
        ExecutionParameters executionContext = Context.getExecutionPars();
        if (executionContext != null) {
            String execName = executionContext.getName();
            String execType = executionContext.getType();
            String name = (execName == null) ? "unknown" : execName;
            String type = (execType == null) ? "" : execType;
            path = path.replace(TOKEN_EXEC_NAME, name);
            path = path.replace(TOKEN_EXEC_TYPE, type);
            while (path.contains(TOKEN_EXEC_COUNT)) {
                int count = executionContext.getCount();
                int i = path.indexOf(TOKEN_EXEC_COUNT) + TOKEN_EXEC_COUNT.length();
                if ((i < path.length()) && path.substring(i, i + 1).equals("%")) {
                    String format = path.substring(i).split(" ")[0];
                    path = path.replaceFirst(Pattern.quote(TOKEN_EXEC_COUNT) + format, String.format(format, count));
                } else {
                    path = path.replaceFirst(Pattern.quote(TOKEN_EXEC_COUNT), String.format("%04d", count));
                }
            }
            while (path.contains(TOKEN_EXEC_INDEX)) {
                int index = executionContext.getIndex();
                int i = path.indexOf(TOKEN_EXEC_INDEX) + TOKEN_EXEC_INDEX.length();
                if ((i < path.length()) && path.substring(i, i + 1).equals("%")) {
                    String format = path.substring(i).split(" ")[0];
                    path = path.replaceFirst(Pattern.quote(TOKEN_EXEC_INDEX) + format, String.format(format, index));
                } else {
                    path = path.replaceFirst(Pattern.quote(TOKEN_EXEC_INDEX), String.format("%04d", index));
                }
            }
            while (path.contains(TOKEN_FILE_SEQUENTIAL_NUMBER)) {
                int index = executionContext.getSeq();
                int i = path.indexOf(TOKEN_FILE_SEQUENTIAL_NUMBER) + TOKEN_FILE_SEQUENTIAL_NUMBER.length();
                if ((i < path.length()) && path.substring(i, i + 1).equals("%")) {
                    String format = path.substring(i).split(" ")[0];
                    path = path.replaceFirst(Pattern.quote(TOKEN_FILE_SEQUENTIAL_NUMBER) + format, String.format(format, index));
                } else {
                    path = path.replaceFirst(Pattern.quote(TOKEN_FILE_SEQUENTIAL_NUMBER), String.format("%04d", index));
                }
            }
            while (path.contains(TOKEN_DAY_SEQUENTIAL_NUMBER)) {
                int index = executionContext.getDaySeq();
                int i = path.indexOf(TOKEN_DAY_SEQUENTIAL_NUMBER) + TOKEN_DAY_SEQUENTIAL_NUMBER.length();
                if ((i < path.length()) && path.substring(i, i + 1).equals("%")) {
                    String format = path.substring(i).split(" ")[0];
                    path = path.replaceFirst(Pattern.quote(TOKEN_DAY_SEQUENTIAL_NUMBER) + format, String.format(format, index));
                } else {
                    path = path.replaceFirst(Pattern.quote(TOKEN_DAY_SEQUENTIAL_NUMBER), String.format("%04d", index));
                }
            }
            //Cannot be used in defining other tokens, only for data folder as depends on the running context
            while (path.contains(TOKEN_SESSION_ID)) {
                int index = Context.getSessionId();
                int i = path.indexOf(TOKEN_SESSION_ID) + TOKEN_SESSION_ID.length();
                if ((i < path.length()) && path.substring(i, i + 1).equals("%")) {
                    String format = path.substring(i).split(" ")[0];
                    path = path.replaceFirst(Pattern.quote(TOKEN_SESSION_ID) + format, String.format(format, index));
                } else {
                    path = path.replaceFirst(Pattern.quote(TOKEN_SESSION_ID), String.format("%04d", index));
                }
            }
            if (path.contains(TOKEN_SESSION_NAME)) {
                path = path.replace(TOKEN_SESSION_NAME, Context.getSessionName());
            }            
        }
        return ch.psi.pshell.app.Setup.expandPath(path, timestamp);
    }            
    
    public static String[] getLibraryPath() {
        String[] ret = ch.psi.pshell.devices.Setup.getLibraryPath();
        //If default path exists and not in path, included it - so Lib can be shared
        if (!isRunningInIde()) {
            String standard = getStandardLibraryPath();
            String file = "startup." + getScriptType().toString();
            if (!Paths.get(standard, file).toFile().exists()) {
                String defaultLib = Paths.get(getHomePath(), "script", "Lib").toString();
                if (!Arr.containsEqual(ret, defaultLib) && (new File(defaultLib).exists())) {
                    if (Paths.get(getHomePath(), "script", "Lib", file).toFile().exists()) {
                        ret = Arr.append(ret, defaultLib);
                    }
                }
            }
        }
        return ret;
    }
        
    
    
    public static String getQueuePath() {
        return expandPath(queuesPath);
    }                
    
    public static String getConsolePath() {
        return expandPath(consolePath);
    }
    
    
    public static String getWwwPath() {        
        if (isRunningInIde()) {
            return Paths.get(getSourceAssemblyFolder(), "www").toString();
        }
        return ch.psi.pshell.devices.Setup.getWwwPath();
    }

    public static String getWwwIndexFile() {
        return Paths.get(getWwwPath(), "index.html").toString();
    }
        
    
    public static String getConfigFile() {
        return expandPath(configFile);
    }

    public static String getPluginsConfigurationFile() {
        return expandPath(configFilePlugins);
    }
    
    public static String getSessionsConfigurationFile() {
        return expandPath(configFileSessions);        
    }
        
    public static String getSessionMetadataDefinitionFile() {
        return expandPath("{config}/session_metadata.properties");
    }

    public static String getDevicePoolFile() {
        return expandPath(configFileDevices);
    }

    public static String getTasksFile() {
        return expandPath(configFileTasks);
    }

    public static String getSettingsFile() {
        return expandPath(configFileSettings);
    }

    public static String getVariablesFile() {
        return expandPath(configFileVariables);
    }                  

    
    public static String getDefaultStartupScript() {
        String file = getScriptType().getDefaultStartupFile();
        return Paths.get(getStandardLibraryPath(), file).toString();
    }

    public static String getStartupScript() {
        String file = getScriptType().getDefaultStartupFile();
        Path ret = Paths.get(getStandardLibraryPath(), file);
        if (ret.toFile().exists()) {
            return ret.toString();
        }
        for (String path : getLibraryPath()) {
            ret = Paths.get(path, file);
            if (ret.toFile().exists()) {
                return ret.toString();
            }
        }
        return null;
    }
    
    
    public static ScriptType getScriptType() {
        String type =  Options.SCRIPT_TYPE.getString(ScriptType.getDefault().toString());
        return ScriptType.valueOf(type);
    }    
    
    public static boolean redefinedStartupScript() {
        return Options.LOCAL_STARTUP.hasValue();
    }
    
    public static String getLocalStartupScript() {
        String ret = Options.LOCAL_STARTUP.getString(DEFAULT_LOCAL_STARTUP_FILE_PREFIX);
        if (!ret.endsWith(getScriptType().getExtension())){
            ret = ret + "." + getScriptType().getExtension();
        }
        return ret;
    }
    
    public static String getPythonHome() {
         String ph = System.getenv("PYTHONHOME");
         String ret =  Options.PYTHON_HOME.getString(((ph==null) || ph.isBlank()) ? null : ph);
         return (ret==null) ? null : expandPath(ret.trim());
    }        
    
    public static boolean isPythonInstalled(){
        try{
            Path path = Paths.get(getPythonHome());
            if (path.toFile().isDirectory()){
                String version = Miniconda.getVersion(path);
                return (version == null) ? false : true;                
            }
        } catch (Exception ex){            
        }
        return false;
    }      
    
    public static String getDataFormat() {
         return Options.DATA_FORMAT.getString(null);
    }        
    
    public static String getDataLayout() {
         return Options.DATA_LAYOUT.getString(null);
    }        
    
    
    public static String getUser() {
         return Options.USER.getString(null);
    }        
    
    public static boolean getNoBytecodes() {
         return Options.NO_BYTECODE.getBool(false);
    }        
    
    static public boolean isVolatile() {
        return Options.VOLATILE.getBool(false);
    }

    static public boolean isServerMode() {
        return Options.SERVER.getBool(false);
    }

    static public boolean isOffline() {
        return Options.OFFLINE.getBool(false);
    }
    
    static public boolean isDisabled() {
        return Options.DISABLED.getBool(false) || isOffline();
    }

    static public boolean isGeneric() {
        return Options.GENERIC.getBool(false);
    }

    static public Boolean getForceVersioning() {
        return Options.VERSIONING.getBool(null);
    }
    
    static public boolean isBare() {
        return Options.BARE.getBool(false) || isVolatile();
    }

    static public boolean isLocal() {
        return ch.psi.pshell.app.Setup.isLocal() || isPlotOnly() || isOffline() || isVolatile(); 
    }
    
    static public boolean isHandlingSessions() {
        boolean disableSessions = Options.DISABLE_SESSIONS.getBool(false) || isPlotOnly() || isVolatile() || isDetached();
        return !disableSessions;
    }
    
    static public boolean isCli() {
         return Options.CLI.getBool(false);
    }

    static public boolean isConsole() {
         return isCli() || isServerMode() || isHeadless();
    }

    static public boolean isOffscreenPlotting() {
        return isServerMode() || isHeadless();
    }

    static public boolean isGui() {
        return !isCli() && !isServerMode() && !isHeadless();
    }
    
    static public boolean isFileLock() {
        return Options.NO_LOCK.getBool(true);
    }

    static public boolean isShell() {
        return isGui() && Options.SHELL.getBool(false);
    }

    static public boolean isDetachedPersisted() {
        return Options.PERSIST.getBool(false);
    }

    static public boolean isDetached() {
        return isGui() && Options.DETACHED.getBool(false);
    }

    static public boolean isDetachedPanelsPersisted() {
        return isDetached() && isDetachedPersisted() && !isFullScreen();
    }

    static public boolean isDetachedAppendStatusBar() {
        return isDetached() && Options.STATUSBAR.getBool(false);
    }

    static public boolean isDetachedAppendToolBar() {
        return isDetached() && Options.TOOLBAR.getBool(false);
    }
    
    static public boolean isQuiet() {
        return Options.QUIET.getBool(false);
    }

    static public List<String> getDetachedPanels() { 
        List<String> ret =  Options.DETACHED.getStringList();
        if (ret != null) {
            if ((ret.size()==0) || ((ret.size()==1) && (ret.get(0).isBlank()))){
                return null;
            }
        }
        return ret;
    }
    
    //If running from jar and no console, redirect stdout and stderr to Output window
    //Console null and no jar means debbuging in IDE
    static public boolean isOutputRedirected() {
        return (((System.console() == null) && (!isRunningInIde())) ||
               Options.REDIRECT.getBool(false));
    }

    static public boolean isDetachedPlots() {
        return Options.DETACHED_PLOTS.getBool(false);
    }

    static public boolean isPlotOnly() {
        return Options.PLOT_ONLY.getBool(false);
    }

    static public boolean isAutoClose() {
        return Options.AUTO_CLOSE.getBool(false);
    }
            
    public static List<String> getHiddenComponents(){
        return Options.HIDE_COMPONENT.getStringList();
    }

    public static List<String> getPackageArgs(){
        return Options.PACKAGE.getStringList();
    }
    
    public static List<String> getPluginArgs(){
        return Options.PLUGIN.getStringList();
    }

    public static List<String> getTaskArgs(){
        return Options.TASK.getStringList();
    }        
      
    public static List<String> getInterpreterArgs(){
        return Options.INTERP_ARGS.getStringList();
    }          
    
    static public boolean isScanPlottingDisabled() {
        return Options.DISABLE_PLOT.getBool(false);
    }

    static public boolean isScanPrintingDisabled() {
        return Options.DISABLE_PRINT.getBool(false);
    }

    static public Boolean isEnableExtract() {
        if (isDisabled()) {
            return false;
        }
        Boolean ret = Options.EXTRACT.getBool(null);
        if (ret!=null){
            return ret;
        }
        if (isVolatile()) {
            return true;
        }
        return null; //Default
    }    
    
    public static String getDefaultEpicsConfigFile(){
        return isVolatile() ? null : Epics.getDefaultConfigFile();
    }
    
    /**
     * Entity class holding a help element type and content.
     */
    public static class HelpContent {

        public HelpContentType type;
        public String content;
    }

    public enum HelpContentType {

        txt, html, md, py, js, groovy;
    }

    public static HelpContent getHelpContent(String path) {
        HelpContent ret = null;
        String content = null;
        HelpContentType type = null;
        for (HelpContentType ct : HelpContentType.values()) {
            String file = path + "." + ct.toString();
            if (isRunningInIde()) {
                try {
                    content = new String(Files.readAllBytes(Paths.get(getHelpPath(), file.split("/"))));
                    type = ct;
                    break;
                } catch (Exception ex) {
                }
            } else {
                File jarFile = null;
                try {
                    jarFile = new File(getJarFile());
                    content = new String(IO.extractZipFileContent(jarFile, getHelpPath() + file));
                    type = ct;
                    break;
                } catch (Exception ex) {
                }
            }
        }
        if (content != null) {
            ret = new HelpContent();
            ret.content = content;
            ret.type = type;
        }
        return ret;
    }

    public static String[] getHelpItems(String path) {
        ArrayList<String> ret = new ArrayList<>();
        if (isRunningInIde()) {
            File[] files = Paths.get(getHelpPath(), path.split("/")).toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    ret.add(IO.getPrefix(file));
                }
            }
        } else {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(getJarFile());
                for (String file : IO.getJarChildren(jarFile, getHelpPath() + path)) {
                    ret.add(IO.getPrefix(file));
                }
            } catch (Exception ex) {
            }
        }
        String[] sorted = ret.toArray(new String[0]);
        Arrays.sort(sorted);
        return sorted;
    }
    
    public static boolean isRunningInIde() {
        return isRunningInIde("core");
    }

    public static String getSourceAssemblyFolder() {
        return getSourceAssemblyFolder("core");
    }        

    
    public static BufferedImage getAssemblyImage(String path) throws IOException {
        if (isRunningInIde()) {
            String filename = Paths.get(getAppSourceAssemblyFolder(), path.split("/")).toString();
            return ImageIO.read(new BufferedInputStream(new FileInputStream(filename)));
        } else {
            File jarFile = new File(getJarFile());
            byte[] content = IO.extractZipFileContent(jarFile,  path);
            return ImageIO.read(new ByteArrayInputStream(content));
        }
    }
    
    public static String getHelpPath() {
        if (isRunningInIde()) {
            return Paths.get(getAppSourceAssemblyFolder(), "help").toString();
        } else {
            return "help/";
        }
    }
    
    
    public static  String getPlotServer() {
        String ret = Options.PLOT_SERVER.getString(null);
        if ((ret!=null) && ret.isBlank()){
            return null;
        }
        return ret;
    }        
    
    public static String getJarFile() {
        return IO.getExecutingJar(Setup.class);
    }
    
}
