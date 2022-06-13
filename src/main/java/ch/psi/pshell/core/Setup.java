package ch.psi.pshell.core;

import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.security.User;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Config;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
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
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Entity class holding the application folder structure and providing file name
 * expansion.
 */
public class Setup extends Config {

    public static transient final String PROPERTY_HOME_PATH = "ch.psi.pshell.home.path";
    public static transient final String PROPERTY_OUTPUT_PATH = "ch.psi.pshell.output.path";
    public static transient final String PROPERTY_DATA_PATH = "ch.psi.pshell.data.path";
    public static transient final String PROPERTY_SCRIPT_PATH = "ch.psi.pshell.script.path";
    public static transient final String PROPERTY_EXT_SCRIPT_PATH = "ch.psi.pshell.ext_script.path";
    public static transient final String PROPERTY_DEVICES_PATH = "ch.psi.pshell.devices.path";
    public static transient final String PROPERTY_PLUGINS_PATH = "ch.psi.pshell.plugins.path";
    public static transient final String PROPERTY_EXTENSIONS_PATH  = "ch.psi.pshell.exensions.path";
    public static transient final String PROPERTY_CONTEXT_PATH  = "ch.psi.pshell.context.path";
    public static transient final String PROPERTY_SESSIONS_PATH  = "ch.psi.pshell.sessions.path";
    public static transient final String PROPERTY_LOGS_PATH  = "ch.psi.pshell.logs.path";
    public static transient final String PROPERTY_IMAGE_PATH  = "ch.psi.pshell.image.path";
    public static transient final String PROPERTY_CONFIG_FILE = "ch.psi.pshell.config.file";
    public static transient final String PROPERTY_DEVICES_FILE = "ch.psi.pshell.devices.file";
    public static transient final String PROPERTY_PLUGINS_FILE = "ch.psi.pshell.plugins.file";
    public static transient final String PROPERTY_TASKS_FILE = "ch.psi.pshell.tasks.file";
    public static transient final String PROPERTY_SETTINGS_FILE = "ch.psi.pshell.settings.file";
    public static transient final String PROPERTY_SCRIPT_TYPE = "ch.psi.pshell.type";
    public static transient final String PROPERTY_LOCAL_STARTUP_SCRIPT = "ch.psi.pshell.local.startup";
    public static transient final String PROPERTY_PARALLEL_INIT = "ch.psi.pshell.parallel.init";

    //Fixed tokens
    public static transient final String TOKEN_HOME = "{home}";
    public static transient final String TOKEN_HOMEDATA = "{outp}";
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
    public static transient final String TOKEN_USER = "{user}";
    public static transient final String TOKEN_DATE = "{date}";
    public static transient final String TOKEN_TIME = "{time}";
    public static transient final String TOKEN_YEAR = "{year}";
    public static transient final String TOKEN_MONTH = "{month}";
    public static transient final String TOKEN_DAY = "{day}";
    public static transient final String TOKEN_HOUR = "{hour}";
    public static transient final String TOKEN_MIN = "{min}";
    public static transient final String TOKEN_SEC = "{sec}";
    public static transient final String TOKEN_EXEC_NAME = "{name}";
    public static transient final String TOKEN_EXEC_TYPE = "{type}";
    public static transient final String TOKEN_EXEC_COUNT = "{count}";
    public static transient final String TOKEN_EXEC_INDEX = "{index}";
    public static transient final String TOKEN_FILE_SEQUENTIAL_NUMBER = "{seq}";
    public static transient final String TOKEN_DAY_SEQUENTIAL_NUMBER = "{dseq}";
    public static transient final String TOKEN_SYS_HOME = "{syshome}";
    public static transient final String TOKEN_SYS_USER = "{sysuser}";
    public static transient final String TOKEN_SESSION_ID = "{session_id}";
    public static transient final String TOKEN_SESSION_NAME = "{session_name}";

    public static transient final String DEFAULT_HOME_FOLDER = "./home";
    public static transient final String DEFAULT_LIB_FOLDER = TOKEN_SCRIPT + "/Lib";    
    public static transient final String DEFAULT_LOCAL_STARTUP_FILE_PREFIX  = "local";

    String homePath;
    String outputPath;

    public String scriptPath = TOKEN_HOME + "/script";
    public String devicesPath = TOKEN_HOME + "/devices";
    public String pluginsPath = TOKEN_HOME + "/plugins";
    public String extensionsPath = TOKEN_HOME + "/extensions";
    public String libraryPath = TOKEN_SCRIPT + "; " + DEFAULT_LIB_FOLDER;
    public String configPath = TOKEN_HOME + "/config";
    public String wwwPath = TOKEN_HOME + "/www";
    public String contextPath = TOKEN_HOMEDATA + "/context";
    public String sessionsPath = TOKEN_HOMEDATA + "/sessions";
    public String logPath = TOKEN_HOMEDATA + "/log";
    public String dataPath = TOKEN_HOMEDATA + "/data";
    public String imagesPath = TOKEN_HOMEDATA + "/images";

    public String configFile = TOKEN_CONFIG + "/config.properties";
    public String configFilePlugins = TOKEN_CONFIG + "/plugins.properties";
    public String configFileDevices = TOKEN_CONFIG + "/devices.properties";
    public String configFileTasks = TOKEN_CONFIG + "/tasks.properties";
    public String configFileSettings = TOKEN_CONFIG + "/settings.properties";
    public String configFileVariables = TOKEN_CONFIG + "/variables.properties";
    public String configFileSessions = TOKEN_CONFIG + "/sessions.properties";
    
    public String userSessionsPath = TOKEN_SESSIONS + "/user";
    public String consoleSessionsPath = TOKEN_SESSIONS + "/console";    

    public String xscanPath = TOKEN_SCRIPT;   
    public String queuePath = TOKEN_SCRIPT;   
    
    

    String originalScriptPath;
    String originalDataPath;

    public ScriptType scriptType = ScriptType.getDefault();

    ScriptType type;

    void setScriptType(ScriptType type) {
        //Don't write to persisted parameter, but to a lolatile variable
        this.type = type;
    }

    public ScriptType getScriptType() {
        //If type has been overriden
        if (type != null) {
            return type;
        }
        return scriptType;
    }

    @Override
    public void load(String fileName) throws IOException {
        if (System.getProperty(PROPERTY_HOME_PATH) == null) {
            System.setProperty(PROPERTY_HOME_PATH, DEFAULT_HOME_FOLDER);
        }
        homePath = System.getProperty(PROPERTY_HOME_PATH);

        if (System.getProperty(PROPERTY_OUTPUT_PATH) == null) {
            System.setProperty(PROPERTY_OUTPUT_PATH, homePath);
        }
        outputPath = System.getProperty(PROPERTY_OUTPUT_PATH);

        //Config folder can be redirected, but setup is always located in home/config
        if (fileName == null) {
            fileName = Paths.get(homePath, "config", "setup.properties").toString();
        } else {
            fileName = fileName.replace(TOKEN_CONFIG, Paths.get(homePath, "config").toString());
            if (IO.getExtension(fileName).isEmpty()) {
                fileName += ".properties";
            }
        }
        if (fileName.trim().startsWith("~")) {
            fileName = TOKEN_SYS_HOME + Str.trimLeft(fileName).substring(1);
        }
        fileName = fileName.replace(TOKEN_SYS_HOME, Sys.getUserHome());
        fileName = fileName.replace(TOKEN_SYS_USER, Sys.getUserName());
        super.load(fileName);

        //Backward compatibility
        if (Str.toString(configFileSettings).equals((Str.toString(null)))) {
            configFileSettings = TOKEN_CONFIG + "/settings.properties";
            save();
        }
        if (Str.toString(configFileVariables).equals((Str.toString(null)))) {
            configFileVariables = TOKEN_CONFIG + "/variables.properties";
            save();
        }
        if (Str.toString(configFileSessions).equals((Str.toString(null)))) {
            configFileSessions = TOKEN_CONFIG + "/sessions.properties";
            save();
        }
        if (Str.toString(consoleSessionsPath).equals((Str.toString(null)))) {
            consoleSessionsPath = TOKEN_SESSIONS + "/console";
            save();
        }
        if (Str.toString(userSessionsPath).equals((Str.toString(null)))) {
            userSessionsPath = TOKEN_SESSIONS + "/user";
            save();
        }
        if (Str.toString(xscanPath).equals((Str.toString(null)))) {
            xscanPath = TOKEN_SCRIPT;
            save();
        }        
        if (Str.toString(queuePath).equals((Str.toString(null)))) {
            queuePath = TOKEN_SCRIPT;
            save();
        }        
        
 
        if (System.getProperty(PROPERTY_DATA_PATH) != null) {
            dataPath = System.getProperty(PROPERTY_DATA_PATH);
        }

        if (System.getProperty(PROPERTY_SCRIPT_PATH) != null) {
            scriptPath = System.getProperty(PROPERTY_SCRIPT_PATH);
        }
        
        if (System.getProperty(PROPERTY_DEVICES_PATH) != null) {
            devicesPath = System.getProperty(PROPERTY_DEVICES_PATH);
        }  
        
        if (System.getProperty(PROPERTY_PLUGINS_PATH) != null) {
            pluginsPath = System.getProperty(PROPERTY_PLUGINS_PATH);
        }   
        
        if (System.getProperty(PROPERTY_IMAGE_PATH) != null) {
            imagesPath = System.getProperty(PROPERTY_IMAGE_PATH);
        }   

        if (System.getProperty(PROPERTY_EXTENSIONS_PATH) != null) {
            extensionsPath = System.getProperty(PROPERTY_EXTENSIONS_PATH);
        }   

        if (System.getProperty(PROPERTY_CONTEXT_PATH) != null) {
            contextPath = System.getProperty(PROPERTY_CONTEXT_PATH);
        }   

        if (System.getProperty(PROPERTY_SESSIONS_PATH) != null) {
            sessionsPath = System.getProperty(PROPERTY_SESSIONS_PATH);
        }   

        if (System.getProperty(PROPERTY_LOGS_PATH) != null) {
            logPath = System.getProperty(PROPERTY_LOGS_PATH);
        }           
        
        initPaths();

        if (System.getProperty(PROPERTY_CONFIG_FILE) != null) {
            configFile = System.getProperty(PROPERTY_CONFIG_FILE);
            if (IO.getExtension(configFile).isEmpty()) {
                configFile += ".properties";
            }
        }
        if (System.getProperty(PROPERTY_DEVICES_FILE) != null) {
            configFileDevices = System.getProperty(PROPERTY_DEVICES_FILE);
            if (IO.getExtension(configFileDevices).isEmpty()) {
                configFileDevices += ".properties";
            }
        }

        if (System.getProperty(PROPERTY_PLUGINS_FILE) != null) {
            configFilePlugins = System.getProperty(PROPERTY_PLUGINS_FILE);
            if (IO.getExtension(configFilePlugins).isEmpty()) {
                configFilePlugins += ".properties";
            }
        }

        if (System.getProperty(PROPERTY_TASKS_FILE) != null) {
            configFileTasks = System.getProperty(PROPERTY_TASKS_FILE);
            if (IO.getExtension(configFileTasks).isEmpty()) {
                configFileTasks += ".properties";
            }
        }

        if (System.getProperty(PROPERTY_SETTINGS_FILE) != null) {
            configFileSettings = System.getProperty(PROPERTY_SETTINGS_FILE);
            if (IO.getExtension(configFileSettings).isEmpty()) {
                configFileSettings += ".properties";
            }
        }

        if (System.getProperty(PROPERTY_SCRIPT_TYPE) != null) {
            setScriptType(ScriptType.valueOf(System.getProperty(PROPERTY_SCRIPT_TYPE)));
        }
        originalScriptPath = scriptPath;
        originalDataPath = dataPath;
    }

    String user = User.DEFAULT_USER_NAME;

    boolean setUser(User user) {
        if (!user.name.equals(this.user)) {
            this.user = user.name;
            initPaths();
            return true;
        }
        return false;
    }

    HashMap<String, String> expansionTokens;
    HashMap<String, String> expandedPathNames;

    void initPaths() {
        expansionTokens = new HashMap<>();
        expansionTokens.put(TOKEN_HOME, homePath);
        expansionTokens.put(TOKEN_HOMEDATA, outputPath);
        expansionTokens.put(TOKEN_SCRIPT, scriptPath);
        expansionTokens.put(TOKEN_DEVICES, devicesPath);
        expansionTokens.put(TOKEN_DATA, dataPath);
        expansionTokens.put(TOKEN_CONFIG, configPath);
        expansionTokens.put(TOKEN_CONTEXT, contextPath);
        expansionTokens.put(TOKEN_SESSIONS, sessionsPath);
        expansionTokens.put(TOKEN_IMAGES, imagesPath);
        expansionTokens.put(TOKEN_LOGS, logPath);
        expansionTokens.put(TOKEN_PLUGINS, pluginsPath);
        expansionTokens.put(TOKEN_EXTENSIONS, extensionsPath);
        expansionTokens.put(TOKEN_WWW, wwwPath);

        expandedPathNames = new HashMap<>();
        String expandedHomePath = expandPath(homePath);
        expandedPathNames.put(homePath, expandedHomePath);
        try {
            (new File(expandedHomePath)).mkdirs();
        } catch (Exception ex) {
            throw new RuntimeException("Cannot create path: " + expandedHomePath);
        }

        boolean finished = false;
        int count = 0;
        while (!finished) {
            finished = true;
            for (String token : expansionTokens.values()) {
                String path = expandedPathNames.get(token);
                if (path == null) {
                    path = token;
                    expandedPathNames.put(token, expandStaticTokens(path));
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

    int getExpansionTokenCount(String path) {
        int count = 0;
        for (String token : expansionTokens.keySet()) {
            if (path.contains(token)) {
                count++;
            }
        }
        return count;
    }

    boolean checkExpansionTokens(String path) {
        int expansionTokes = getExpansionTokenCount(path);
        if (expansionTokes > 1) {
            throw new RuntimeException("Only one folder expansion token is allowed: " + path);
        }
        return (expansionTokes == 1);
    }

    public String expandPath(String path) {
        return expandPath(path, -1);
    }

    public String expandPath(String path, long timestamp) {
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
        return expandStaticTokens(path, timestamp);
    }

    String expandStaticTokens(String path) {
        return expandStaticTokens(path, -1);
    }

    String expandStaticTokens(String path, long timestamp) {
        return expand(path, timestamp, user);
    }

    public static String expand(String path) {
        return expand(path, -1, null);
    }

    public static String expand(String path, long timestamp, String user) {
        if (user != null) {
            path = path.replace(TOKEN_USER, user);
        }
        Context ctx = Context.getInstance();
        ExecutionParameters executionContext = (ctx == null) ? null : ctx.getExecutionPars();
        if (executionContext != null) {
            String execName = executionContext.getName();
            String execType = executionContext.getType();
            String name = (execName == null) ? "unknown" : execName;
            String type = (execType == null) ? "" : execType;
            path = path.replace(TOKEN_EXEC_NAME, name);
            path = path.replace("{exec}", name); //TODO: Remove, this is for backward compatibility
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
                int index = ctx.getSessionId();
                int i = path.indexOf(TOKEN_SESSION_ID) + TOKEN_SESSION_ID.length();
                if ((i < path.length()) && path.substring(i, i + 1).equals("%")) {
                    String format = path.substring(i).split(" ")[0];
                    path = path.replaceFirst(Pattern.quote(TOKEN_SESSION_ID) + format, String.format(format, index));
                } else {
                    path = path.replaceFirst(Pattern.quote(TOKEN_SESSION_ID), String.format("%04d", index));
                }
            }
            if (path.contains(TOKEN_SESSION_NAME)) {
                path = path.replace(TOKEN_SESSION_NAME, ctx.getSessionName());
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

    boolean redefineDataPath(String path) {
        if (path == null) {
            path = originalDataPath;
        }
        if (!path.equals(dataPath)) {
            dataPath = path;
            initPaths();
            return true;
        }
        return false;
    }

    boolean redefineScriptPath(String path) {
        if (path == null) {
            path = originalScriptPath;
        }
        if (!path.equals(scriptPath)) {
            scriptPath = path;
            initPaths();
            return true;
        }
        return false;
    }

    public String getHomePath() {
        return expandedPathNames.get(homePath);
    }

    public String getOutputPath() {
        return expandedPathNames.get(outputPath);
    }

    public String getImagesPath() {
        return expandedPathNames.get(imagesPath);
    }

    public String getScriptPath() {
        return expandedPathNames.get(scriptPath);
    }
    
    public String getDefaultScriptLibPath() {
        return expandPath(DEFAULT_LIB_FOLDER);
    }

    public String getDataPath() {
        return expandedPathNames.get(dataPath);
    }

    public String getSessionsPath() {
        return expandedPathNames.get(sessionsPath);
    }

    public String getConsoleSessionsPath() {
        return expandPath(consoleSessionsPath);        
    }

    public String getUserSessionsPath() {
        return expandPath(userSessionsPath);        
    }
    
    public String getContextPath() {
        return expandedPathNames.get(contextPath);
    }

    public String getLogPath() {
        return expandedPathNames.get(logPath);
    }

    public String getDevicesPath() {
        return expandedPathNames.get(devicesPath);
    }

    public String getPluginsPath() {
        return expandedPathNames.get(pluginsPath);
    }

    public String getExtensionsPath() {
        return expandedPathNames.get(extensionsPath);
    }

    public String getConfigPath() {
        return expandedPathNames.get(configPath);
    }

    public String getWwwPath() {
        if (isRunningInIde()) {
            return Paths.get(getSourceAssemblyFolder(), "www").toString();
        }
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

    public String getWwwIndexFile() {
        return Paths.get(getWwwPath(), "index.html").toString();
    }

    public String getHelpPath() {
        if (isRunningInIde()) {
            return Paths.get(getSourceAssemblyFolder(), "help").toString();
        } else {
            return "help/";
        }
    }

    public String getAssemblyPath() {
        if (isRunningInIde()) {
            return getSourceAssemblyFolder();
        } else {
            return "";
        }
    }

    public static String getJarFile() {
        return IO.getExecutingJar(Context.class);
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

    public HelpContent getHelpContent(String path) {
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

    public String[] getHelpItems(String path) {
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

    public BufferedImage getAssemblyImage(String path) throws IOException {
        if (isRunningInIde()) {
            String filename = Paths.get(getAssemblyPath(), path.split("/")).toString();
            return ImageIO.read(new BufferedInputStream(new FileInputStream(filename)));
        } else {
            File jarFile = new File(getJarFile());
            byte[] content = IO.extractZipFileContent(jarFile, getAssemblyPath() + path);
            return ImageIO.read(new ByteArrayInputStream(content));
        }
    }

    public String[] getLibraryPath() {
        String[] ret = libraryPath.split(";");
        for (int i = 0; i < ret.length; i++) {
            ret[i] = expandPath(ret[i].trim());
        }
        String standard = getStandardLibraryPath();
        if (!Arr.containsEqual(ret, standard)) {
            ret = Arr.append(ret, standard);
        }

        if (System.getProperty(PROPERTY_EXT_SCRIPT_PATH) != null) {
            String[] ext = System.getProperty(PROPERTY_EXT_SCRIPT_PATH).split(";");
            for (int i = 0; i < ext.length; i++) {
                ret = Arr.append(ret, expandPath(ext[i].trim()));
            }
        }

        //If default path exists and not in path, included it - so Lib can be shared
        if (!isRunningInIde()) {
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

        if (!scriptPath.equals(originalScriptPath)) {
            Logger.getLogger(Setup.class.getName()).info("Adding original script folder to library path");
            ret = Arr.append(ret, expandPath(originalScriptPath));
        }
        return ret;
    }

    public String getStandardLibraryPath() {
        if (isRunningInIde()) {
            return Paths.get(getSourceAssemblyFolder(), "script", "Lib").toString();
        }
        return Paths.get(getScriptPath(), "Lib").toString();
    }

    public String getDefaultStartupScript() {
        String file = getScriptType().getDefaultStartupFile();
        return Paths.get(getStandardLibraryPath(), file).toString();
    }

    public String getStartupScript() {
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

    public String getLocalStartupScript() {
        String ret = DEFAULT_LOCAL_STARTUP_FILE_PREFIX;        
        if (System.getProperty(PROPERTY_LOCAL_STARTUP_SCRIPT) != null) {
            ret = System.getProperty(PROPERTY_LOCAL_STARTUP_SCRIPT);
        }                        
        if (!ret.endsWith(getScriptType().getExtension())){
            ret = ret + "." + getScriptType().getExtension();
        }
        return ret;
    }
        
    public String getCommandHistoryFile() {
        return Paths.get(getContextPath(), "CommandHistory.dat").toString();
    }

    public String getConfigFile() {
        return expandPath(configFile);
    }

    public String getPluginsConfigurationFile() {
        return expandPath(configFilePlugins);
    }
    
    public String getSessionsConfigurationFile() {
        return expandPath(configFileSessions);        
    }
        

    public String getSessionMetadataDefinitionFile() {
        return expandPath("{config}/session_metadata.properties");
    }

    public String getDevicePoolFile() {
        return expandPath(configFileDevices);
    }

    public String getTasksFile() {
        return expandPath(configFileTasks);
    }

    public String getSettingsFile() {
        return expandPath(configFileSettings);
    }

    public String getVariablesFile() {
        return expandPath(configFileVariables);
    }

    public String getXScanPath() {
        return expandPath(xscanPath);
    }
    
    public String getQueuePath() {
        return expandPath(queuePath);
    }    
    
    public static String getSourceAssemblyFolder() {
        return Paths.get("src", "main", "assembly").toString();
    }

    /**
     * This does not exactly mean the process was started by the IDE, but that
     * it is running in the project source folder, and there get resources from
     * the src folder and not the jar itself. In order to know if the project is
     * running from the jar, check if getJarFile()!=null.
     */
    public static boolean isRunningInIde() {
        return Paths.get(getSourceAssemblyFolder()).toFile().isDirectory();
    }
}
