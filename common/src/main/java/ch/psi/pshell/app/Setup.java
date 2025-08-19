package ch.psi.pshell.app;

import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Entity class holding the application defaults and command-line options, folder structure 
 * and providing file name expansion.
 */
public class Setup{    
    
    //Fixed tokens
    public static transient final String TOKEN_HOME = "{home}";
    public static transient final String TOKEN_OUTPUT = "{outp}";
    public static transient final String TOKEN_SCRIPT = "{script}";
    public static transient final String TOKEN_DEVICES = "{devices}";
    public static transient final String TOKEN_DATA = "{data}";
    public static transient final String TOKEN_CONFIG = "{config}";
    public static transient final String TOKEN_CONTEXT = "{context}";
    public static transient final String TOKEN_SESSIONS = "{sessions}";
    public static transient final String TOKEN_IMAGES = "{images}";
    public static transient final String TOKEN_LOGS = "{logs}";
    public static transient final String TOKEN_PLUGINS = "{plugins}";
    public static transient final String TOKEN_EXTENSIONS = "{extensions}";
    public static transient final String TOKEN_WWW = "{www}";
    //Variable tokens
    public static transient final String TOKEN_DATE = "{date}";
    public static transient final String TOKEN_TIME = "{time}";
    public static transient final String TOKEN_YEAR = "{year}";
    public static transient final String TOKEN_MONTH = "{month}";
    public static transient final String TOKEN_DAY = "{day}";
    public static transient final String TOKEN_HOUR = "{hour}";
    public static transient final String TOKEN_MIN = "{min}";
    public static transient final String TOKEN_SEC = "{sec}";
    public static transient final String TOKEN_SYS_HOME = "{syshome}";
    public static transient final String TOKEN_SYS_USER = "{sysuser}";

    public static transient final String DEFAULT_HOME_FOLDER = "~/pshell/home";
    public static transient final String DEFAULT_LIB_FOLDER = TOKEN_SCRIPT + "/Lib";        

    static String homePath = DEFAULT_HOME_FOLDER;
    static String outputPath = DEFAULT_HOME_FOLDER;

    static String scriptsPath = TOKEN_HOME + "/script";
    static String devicesPath = TOKEN_HOME + "/devices";
    static String pluginsPath = TOKEN_HOME + "/plugins";
    static String extensionsPath = TOKEN_HOME + "/extensions";
    static String libraryPath = TOKEN_SCRIPT + "; " + DEFAULT_LIB_FOLDER;
    static String configPath = TOKEN_HOME + "/config";
    static String wwwPath = TOKEN_HOME + "/www";
    static String contextPath = TOKEN_OUTPUT + "/context";
    static String sessionsPath = TOKEN_OUTPUT + "/sessions";
    static String logPath = TOKEN_OUTPUT + "/log";
    static String dataPath = TOKEN_OUTPUT + "/data";
    static String imagesPath = TOKEN_DATA;        
    
    static{        
        init();
    }
    

    public static String getField(String name){
        HashMap<String, String> tokens = getExpansionTokens();
        if (tokens.containsKey(name)){
            return tokens.get(name);
        }
        try {
            return Setup.class.getField(name).get(null).toString();
        } catch (Exception ex) {
            return null;
        }
    }
               
    public static void redefinePath(Options option, String value) {
        option.force(value);
        init();
    }
                
    public static void init(){
        homePath = Options.HOME_PATH.getString(homePath);
        outputPath = Options.OUTPUT_PATH.getString(homePath);
        configPath = Options.CONFIG_PATH.getString(configPath);
        dataPath = Options.DATA_PATH.getString(dataPath);
        devicesPath = Options.DEVICES_PATH.getString(devicesPath); 
        scriptsPath = Options.SCRIPTS_PATH.getString(scriptsPath);       
        pluginsPath = Options.PLUGINS_PATH.getString(pluginsPath);
        extensionsPath = Options.EXTENSIONS_PATH.getString(extensionsPath); 
        contextPath = Options.CONTEXT_PATH.getString(contextPath);
        sessionsPath = Options.SESSIONS_PATH.getString(sessionsPath);
        logPath = Options.LOGS_PATH.getString(logPath);
        imagesPath = Options.IMAGES_PATH.getString(imagesPath);
        wwwPath = Options.WWW_PATH.getString(wwwPath);                 
        libraryPath = Options.SCRIPT_LIB.getString(libraryPath);
        
        initPaths();          
    }    

    static HashMap<String, String> expansionTokens;
    static HashMap<String, String> expandedPathNames;
    
    
    protected static HashMap<String, String> getExpansionTokens(){
        HashMap<String, String> ret= new HashMap<>();
        ret.put(TOKEN_HOME, homePath);
        ret.put(TOKEN_OUTPUT, outputPath);
        ret.put(TOKEN_SCRIPT, scriptsPath);
        ret.put(TOKEN_DEVICES, devicesPath);
        ret.put(TOKEN_DATA, dataPath);
        ret.put(TOKEN_CONFIG, configPath);
        ret.put(TOKEN_CONTEXT, contextPath);
        ret.put(TOKEN_SESSIONS, sessionsPath);
        ret.put(TOKEN_IMAGES, imagesPath);
        ret.put(TOKEN_LOGS, logPath);
        ret.put(TOKEN_PLUGINS, pluginsPath);
        ret.put(TOKEN_EXTENSIONS, extensionsPath);        
        ret.put(TOKEN_WWW, wwwPath);
        return ret;
    }

    static void initPaths() {
        expansionTokens = getExpansionTokens();
        expandedPathNames = new HashMap<>();
        expandedPathNames.put(homePath,  expandPath(homePath));

        boolean finished = false;
        int count = 0;
        while (!finished) {
            finished = true;
            for (String token : expansionTokens.values()) {
                String path = expandedPathNames.get(token);
                if (path == null) {
                    path = token;
                    expandedPathNames.put(token, expandPath(path));
                }

                if (checkExpansionTokens(path)) {
                    finished = false;
                    expandedPathNames.put(token, expandPath(path));
                }
            }
            if (count++ > expansionTokens.size()) {
                throw new RuntimeException("Recursive reference expanding folder names.");
            }
        }
    }
    
    public static void mkhome(){
        String expandedHomePath = expandPath(homePath);
        try {            
            (new File(expandedHomePath)).mkdirs();
        } catch (Exception ex) {
            throw new RuntimeException("Cannot create path: " + expandedHomePath);
        }        
    }
    
    
    public static void mkdirs(){
        for (String path : expandedPathNames.values()) {
            if (path != null) {
                try {
                    (new File(path)).mkdirs();
                } catch (Exception ex) {
                    throw new RuntimeException("Cannot create path: " + path);
                }
            }
        }                                
    }
    
    public static void mkdirs(String[] tokens){
        for (String token:tokens){
            String path = expandPath(token);
            try {
                (new File(path)).mkdirs();
            } catch (Exception ex) {
                throw new RuntimeException("Cannot create path: " + path);
            }            
        }
    }
  

    static int getExpansionTokenCount(String path) {
        int count = 0;
        for (String token : expansionTokens.keySet()) {
            if (path.contains(token)) {
                count++;
            }
        }
        return count;
    }

    static boolean checkExpansionTokens(String path) {
        int expansionTokes = getExpansionTokenCount(path);
        if (expansionTokes > 1) {
            throw new RuntimeException("Only one folder expansion token is allowed: " + path);
        }
        return (expansionTokes == 1);
    }

    public static String expandPath(String path) {
        return expandPath(path, -1);
    }

    public static String expandPath(String path, long timestamp) {
        if (checkExpansionTokens(path)) {
            for (String token : expansionTokens.keySet()) {
                if (path.contains(token)) {
                    String exp = expandedPathNames.get(expansionTokens.get(token));
                    if (exp != null) {
                        path = path.replace(token, exp);
                    }
                    break;
                }
            }
        }
        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis();
        }
        path = path.replace(TOKEN_DATE + "%02d", Chrono.getTimeStr(timestamp, "YYMMdd"));
        path = path.replace(TOKEN_DATE, Chrono.getTimeStr(timestamp, "YYYYMMdd"));
        path = path.replace(TOKEN_TIME, Chrono.getTimeStr(timestamp, "HHmmss"));
        path = path.replace(TOKEN_YEAR + "%02d", Chrono.getTimeStr(timestamp, "YY"));
        path = path.replace(TOKEN_YEAR, Chrono.getTimeStr(timestamp, "YYYY"));
        path = path.replace(TOKEN_MONTH, Chrono.getTimeStr(timestamp, "MM"));
        path = path.replace(TOKEN_DAY, Chrono.getTimeStr(timestamp, "dd"));
        path = path.replace(TOKEN_HOUR, Chrono.getTimeStr(timestamp, "HH"));
        path = path.replace(TOKEN_MIN, Chrono.getTimeStr(timestamp, "mm"));
        path = path.replace(TOKEN_SEC, Chrono.getTimeStr(timestamp, "ss"));

        if (path.trim().startsWith("~")) {
            path = TOKEN_SYS_HOME + Str.trimLeft(path).substring(1);
        }
        path = path.replace(TOKEN_SYS_HOME, Sys.getUserHome());
        path = path.replace(TOKEN_SYS_USER, Sys.getUserName());
        try {
            path = Paths.get(path).toString();
        } catch (Exception ex) {
        }
        return path;
    }
    
        
    public static String getHomePath() {
        return expandedPathNames.get(homePath);
    }

    public static String getOutputPath() {
        return expandedPathNames.get(outputPath);
    }

    public static String getImagesPath() {
        return expandedPathNames.get(imagesPath);
    }

    public static String getScriptsPath() {
        return expandedPathNames.get(scriptsPath);
    }
    
    public static String getDefaultScriptLibPath() {
        return expandPath(DEFAULT_LIB_FOLDER);
    }

    public static String getDataPath() {
        return expandedPathNames.get(dataPath);
    }

    public static String getSessionsPath() {
        return expandedPathNames.get(sessionsPath);
    }
    
    public static String getContextPath() {
        return expandedPathNames.get(contextPath);
    }

    public static String getLogPath() {
        return expandedPathNames.get(logPath);
    }

        public static String getDevicesPath() {
        return expandedPathNames.get(devicesPath);
    }

    public static String getPluginsPath() {
        return expandedPathNames.get(pluginsPath);
    }

    public static String getExtensionsPath() {
        return expandedPathNames.get(extensionsPath);
    }

    public static String getConfigPath() {
        return expandedPathNames.get(configPath);
    }
        
    
    public static String getWwwPath() {
        //Check if there is a shared www folder in the jar path
        String jar = getJarFile();
        if (jar != null) {
            Path path = Paths.get(IO.getFolder(jar), "www");
            if (path.toFile().exists()) {
                return path.toString();
            }
        }
        return expandedPathNames.get(wwwPath);
    }
        
    public static String getCachePath(String name){
        return getCachePath(name, true);
    }
    
    public static String getCachePath(String name, boolean isFolder){
        String contextPath = getContextPath();
        String cachePath =  null;
        if ((contextPath!=null) && new File(contextPath).isDirectory()) {
            cachePath = Paths.get(getContextPath(),name).toString();
        } else {
            cachePath = Paths.get(Sys.getUserHome(), ".pshell", name).toString();
        }
        if (isFolder){
            new File(cachePath).mkdirs();
        } else {
            new File(cachePath).getParentFile().mkdirs();
        }
        return cachePath;
    }

    public static String getCachePathPlugins(){
        return getCachePath("plugins");
    }
  

    public static String getJarFile() {
        return IO.getExecutingJar(Setup.class);
    }
    
    
    protected static String standardLibraryPath;
    
    public static String[] getLibraryPath() {
        String[] ret = libraryPath.split(";");
        for (int i = 0; i < ret.length; i++) {
            ret[i] = expandPath(ret[i].trim());
        }
        String standard = getStandardLibraryPath();
        if (!Arr.containsEqual(ret, standard)) {
            ret = Arr.append(ret, standard);
        }
        return ret;
    }

    public static String getStandardLibraryPath() {
        if (standardLibraryPath==null){
            return Paths.get(getScriptsPath(), "Lib").toString();
        } 
        return standardLibraryPath;
    }
        
    public static String getCommandHistoryFile() {
        return Paths.get(getContextPath(), "CommandHistory.dat").toString();
    }

    
    /**
     * This does not exactly mean the process was started by the IDE, but that
     * it is running in the project source folder, and there get resources from
     * the src folder and not the jar itself. In order to know if the project is
     * running from the jar, check if getJarFile()!=null.
     */
    public static boolean isRunningInIde(String project) {
        return Paths.get(getSourceAssemblyFolder(project)).toFile().isDirectory();
    }            
            
    public static boolean isRunningInIde() {
        return isRunningInIde("common");
    }
        
    public static String getSourceAssemblyFolder(String project) {
        File cur = new File(Sys.getCurDir());
        return Paths.get(cur.getParent(), project, "src", "main", "assembly").toString();
    }        
    
    public static String getSourceAssemblyFolder() {
        return getSourceAssemblyFolder("common");
    }    
    
    public static String getAppSourceAssemblyFolder() {
        File cur = new File(Sys.getCurDir());
        return Paths.get("src", "main", "assembly").toString(); 
    }    
    
    
    public static boolean isForcedHeadless() {
        return Options.HEADLESS.getBool(false);
    }
    
    public static boolean isDebug() {
       return Options.DEBUG.getBool(false);
    }          
    
    static public boolean isHidden() {
        return  Options.HIDE.getBool(false);
    }
    
    public static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }          
    
    public static String getRootLogger(){
        return "ch.psi";
    }
    
    static public Level getConsoleLogLevel(){
        Level consoleLogLevel = Level.OFF;
        try {
            consoleLogLevel = Level.parse(Options.CONSOLE_LOG.getString("OFF"));
        } catch (Exception ex) {
        }
        return consoleLogLevel;        
    }
   
  
    public static String getLookAndFeel() {
        return Options.LAF.getString(null);
    }
    
    public static String getScaleUI(){
        return Options.UI_SCALE.getString(null);
    }
    
    public static  Dimension getSize() {
        try {
            String opt =  Options.SIZE.getString(null);
            String[] tokens = opt.toLowerCase().split("x");
            return new Dimension(Integer.valueOf(tokens[0]), Integer.valueOf(tokens[1]));
        } catch (Exception ex) {
        }
        return null;
    }
        
    public static  boolean isFullScreen() {
        return  Options.FULL_SCREEN.getBool(false);
    }       
    
    public static String getTitle(){  
        return Options.TITLE.getString(null);
    }
    
    public static Quality getQuality(){
        try{
            return Quality.valueOf(Options.QUALITY.getString(null));
        } catch (Exception ex){                
        }            
        return null;
    }        
        
    static public boolean getStartArg() {
        return Options.START.getBool(false);
    }    
        
    public static List<String> getAddedLibraryPath(){
        return Options.LIBRARY_PATH.getStringList();
    }          

    public static List<String> getAddedClassPath(){
        return Options.CONTEXT_PATH.getStringList();
    }          

    public static String getFileArg(){
        List<String> args = getFileArgs();
        if ((args==null) || (args.size()==0)){
            return null;
        }
        return args.get(0);
    }

    public static List<String> getFileArgs(){
        return Options.FILE.getStringList();
    }
    
    public static String getEvalArg(){
        List<String> args = getEvalArgs();
        if ((args==null) || (args.size()==0)){
            return null;
        }
        return args.get(0);
    }    
    
    public static List<String> getEvalArgs(){
        return Options.EVAL.getStringList();
    }
    
    public static boolean isLocal() {
        return Options.LOCAL.getBool(false); 
    }    
    
    static public String getConfigArg() {
        return  Options.CONFIG.getString(null);
    }       
            
}
