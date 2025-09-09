package ch.psi.pshell.plugin;

import ch.psi.pshell.app.App;
import ch.psi.pshell.app.Setup;
import ch.psi.pshell.scripting.Interpreter;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Loader;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import groovy.lang.GroovyClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the loading of plugins, both dynamic (.java files) and compiled
 * (.class or .jar). Source or binary files should contain an implementation of
 * the Plugin interface.
 */
public class PluginManager implements AutoCloseable {

    static  PluginManager INSTANCE;    
    static  int runCount = -1;
    
    public static PluginManager getInstance(){
        if (PluginManager.INSTANCE == null){
            throw new RuntimeException("Plugin Manager not instantiated.");
        }        
        return INSTANCE;
    }
    
    public static boolean hasInstance(){
        return INSTANCE!=null;
    }

    public PluginManager(){        
        INSTANCE  = this;
        runCount++;
        loadExtensionsFolder();
    }
    
    static final Logger logger = Logger.getLogger(PluginManager.class.getName());
    final ArrayList<Plugin> plugins = new ArrayList<>();
    final ArrayList<Class> dynamicClasses = new ArrayList<>();

    static final String JAR_EXTENSIONS_FOLDER = "extensions";    

    class PluginProperties {

        File file;
        String name;
        boolean started;
        boolean updating;
    }
    static HashMap<String, PluginProperties> propertiesMap = new HashMap<>();

    static PluginProperties getProperties(Plugin plugin) {
        return propertiesMap.get(plugin.getClass().getName());
    }
    
    public Plugin[] getLoadedPlugins() {
        return plugins.toArray(new Plugin[0]);
    }

    public Class[] getDynamicClasses() {
        synchronized (dynamicClasses) {
            return dynamicClasses.toArray(new Class[0]);
        }
    }

    public Class[] getDynamicClasses(Class type) {
        ArrayList<Class> ret = new ArrayList<>();
        for (Class cls : getDynamicClasses()) {
            if (type.isAssignableFrom(cls)) {
                ret.add(cls);
            }
        }
        return ret.toArray(new Class[0]);
    }

    public Class getDynamicClass(String className) {
        for (Class c : getDynamicClasses()) {
            if (c.getName().equals(className)) {
                return c;
            }
        }
        return null;
    }

    public void addDynamicClass(Class cls) {
        synchronized (dynamicClasses) {
            for (Class c : getDynamicClasses()) {
                if (c.getName().equals(cls.getName())) {
                    dynamicClasses.remove(c);
                }
            }
            dynamicClasses.add(cls);
        }
        logger.log(Level.INFO, "Added dynamic class: {0}", cls.getName());
    }
    
    public static Class getClass(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className, true, Sys.getDynamicClassLoader());
        } catch (ClassNotFoundException ex) {
            if (hasInstance()) {
                Class cls = getInstance().getDynamicClass(className);
                if (cls != null) {
                    return cls;
                }
                for (Plugin p : getInstance().getLoadedPlugins()) {
                    if (p.getClass().getName().equals(className)) {
                        return p.getClass();
                    }
                }
                if (Interpreter.hasInstance()){
                    cls = Interpreter.getInstance().getClass(className);
                    if (cls != null) {
                        return cls;
                    }
                }
            }
            throw ex;
        }
    }                     
    

    public boolean loadedPluginClass(String className) {
        for (Plugin plugin : plugins) {
            if (plugin.getClass().getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    //Public interface: adding plugins before context starts
    public Plugin loadPluginClass(String className) {
        try {
            Class c = Class.forName(className, true, Sys.getDynamicClassLoader());
            return loadPluginClass(c, null);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error resolving plugin class: {0} - {1}", new Object[]{className, ex.getMessage()});
        }
        return null;
    }

    public Plugin loadPluginClass(Class cls, File file) {
        try {
            if (loadedPluginClass(cls.getName())) {
                logger.log(Level.INFO, "Plugin class already loaded: {0}", cls.getName());
                return null;
            }
            Plugin plugin = (Plugin) cls.newInstance();
            initPlugin(plugin, file);
            plugins.add(plugin);
            return plugin;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error instantiating plugin class: {0} - {1}", new Object[]{cls.getName(), ex.getMessage()});
        }
        return null;
    }
    
    File resolveFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()){
            File file2 = new File(Setup.expandPath("{plugins}/"+fileName));
            if (file2.exists()){
                return file2;
            }
        }
        return file;
    }
    
    public Plugin loadPlugin(String fileName) {
        File file = resolveFile(fileName);
        return loadPlugin(file);
    }

    public Plugin loadPlugin(File file) {
        String fileName = file.getPath();
        logger.log(Level.INFO, "Load plugin: {0}", fileName);
        try {
            for (Plugin p : plugins) {
                if ((p.getPluginFile() != null) && (p.getPluginFile().getCanonicalFile().equals(file.getCanonicalFile()))) {
                    throw new Exception("Plugin file already loaded: " + fileName);
                }
            }
            switch (IO.getExtension(file)) {
                case "jar":
                    for (Class cls : Loader.loadJar(fileName)) {
                        if (Modifier.isPublic(cls.getModifiers())) {
                            if (Plugin.class.isAssignableFrom(cls)) {
                                //Only 1 plugin per jar file
                                return loadPluginClass(cls, file);
                            }
                        }
                    }

                case "py":
                    return loadPythonPlugin(file);

                case "java":
                    return loadJavaPlugin(file);

                case "groovy":
                    return loadGroovyPlugin(file);

                default:
                    throw new IllegalArgumentException("Unsupported plugin format");
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
            //PyException don't have message set
            logger.log(Level.WARNING, "Error loading plugin: {0} - {1}", new Object[]{fileName, (ex.getMessage() == null) ? ex.toString().trim() : ex.getMessage()});
        }
        return null;
    }

    public static File[] getFolderContents(File folder, String[] formats) {
        if ((folder.exists()) && (folder.isDirectory())) {
            ArrayList<File> ret = new ArrayList<>();
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (Arr.containsEqual(formats, IO.getExtension(file))) {
                        ret.add(file);
                    }
                }
            }
            return ret.toArray(new File[0]);
        }
        return new File[0];
    }

    public static File[] getFolderJarContents(File folder) {
        return getFolderContents(folder, new String[]{"jar","class"});
    }

    public static File[] getPluginFolderContents() {
        File pluginFolder = Paths.get(Setup.getPluginsPath()).toFile();
        return getFolderContents(pluginFolder, new String[]{"jar", "py", "groovy", "java"});
    }

    public static File[] getExtensionsFolderContents() {
        return getExtensionsFolderContents(Setup.getExtensionsPath());
    }

    public static File[] getExtensionsFolderContents(String path) {
        File pluginFolder = Paths.get(path).toFile();
        return getFolderJarContents(pluginFolder);
    }

    static final ArrayList<File> extensions = new ArrayList<>();

    static public List<File> getExtensions() {
        return (List<File>) extensions.clone();
    }
        
    
    public void loadExtensionsFolder() {
        List<File> files = loadExtensionsFolder(Setup.getExtensionsPath());
        String jar = Setup.getJarFile();
        if (jar != null) {
            addToLibraryPath(new File(jar).getParentFile());
            //If file duplicated in jar ext folder, give priority to local ext folder.
            File extFolder= Paths.get(IO.getFolder(jar), JAR_EXTENSIONS_FOLDER).toFile();
            if (extFolder.exists() && extFolder.isDirectory()) {
                addToLibraryPath(extFolder);
                for (File file : getFolderJarContents(extFolder)) {
                    boolean add = true;
                    for (File f : files) {
                        if (f.getName().equals(file.getName())) {
                            add = false;
                            break;
                        }
                    }
                    if (add){
                        addToClassPath(file);
                    }
                }
            }                
        }        
    }

    public List<File> loadExtensionsFolder(String folder) {
        File f = new File(folder);
        if (f.exists() && f.isDirectory() && (f.listFiles().length>0)){
            logger.log(Level.INFO, "Loading extensions folder: {0}", folder);
            try {
                addToLibraryPath(new File(folder));
                File[] extensionFolderContents = getExtensionsFolderContents(folder);
                ArrayList<File> files = new ArrayList<>(Arrays.asList(extensionFolderContents));
                addToClassPath(files);
                return files;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return new ArrayList<>();
    }
    
    public void addToClassPath(List<File> files){
        for (File file : files) {
            addToClassPath(file);
        }
    }
    
    public static void addToClassPath(File file){
        try {
            String path =  file.getCanonicalPath();
            Sys.addToClassPath(path);                        
            if (!extensions.contains(file)) {
                extensions.add(file);
            }
            if (file.isDirectory()){
                for (File f : getFolderJarContents(file)) {
                    addToClassPath(f);
                }
            }            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }        
    }
    
    public static void addToLibraryPath(File file){
        try {
            Sys.addToLibraryPath(file.getCanonicalPath());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }    
    }

    String pluginPrepertiesFile;
    
    void reloadPluginFolder() {
        loadPluginFolder(pluginPrepertiesFile);
    }
    //Respecting the order defined in plugins properties file
    public void loadPluginFolder(String pluginPrepertiesFile) {
        logger.info("Loading plugins properties file: " + pluginPrepertiesFile);
        this.pluginPrepertiesFile = pluginPrepertiesFile;
        try (FileInputStream in = new FileInputStream(pluginPrepertiesFile)) {
            Properties properties = new Properties();
            properties.load(in);
            //File[] plugins = getPluginFolderContents();
            ArrayList<String> orderedPlugins = IO.getOrderedPropertyKeys(pluginPrepertiesFile);
            for (String pluginFile : orderedPlugins) {
                String prop = properties.getProperty(pluginFile);
                if (prop == null) {
                    logger.log(Level.WARNING, "Error parsing plugin configuration file");
                } else if (prop.trim().equals("enabled")) {
                    File plugin = Paths.get(Setup.getPluginsPath(), pluginFile).toFile();
                    if (plugin.exists()) {
                        loadPlugin(plugin.toString());
                    } else {
                        logger.log(Level.WARNING, "Plugin file not found: {0}", pluginFile);
                    }
                }
            }
        } catch (FileNotFoundException | NoSuchFileException ex) {
            logger.log(Level.FINER, null, ex);            
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    public Plugin loadPythonPlugin(File file) throws Exception {
        if (!"py".equals(System.getProperty("build.type"))) {
            throw new Exception("Python plugin can only be loaded if script type is 'py: " + file.getName());
        }
        String class_name = IO.getPrefix(file);
        org.python.core.PySystemState ss = new org.python.core.PySystemState();
        String[] libPath = Setup.getLibraryPath();
        for (String lib : libPath) {
            if ((lib != null) && (lib.length() > 0)) {
                //TODO: if doing this that exception if built type is groovy or js -> Why PyString importing here is not lazy?
                //ss.path.append( new org.python.core.PyString(lib));                
                ss.path.append((org.python.core.PyObject) Class.forName("org.python.core.PyString").getConstructor(new Class[]{String.class}).newInstance(new Object[]{lib}));
            }
        }
        org.python.util.PythonInterpreter interp = new org.python.util.PythonInterpreter(null, null);
        interp.exec("from ch.psi.pshell.plugin import Plugin");
        interp.exec("from ch.psi.pshell.framework import Panel");
        interp.execfile(file.getAbsolutePath());
        org.python.core.PyObject py_script = interp.eval(class_name + "()");
        Plugin plugin = (Plugin) py_script.__tojava__(Plugin.class);
        initPlugin(plugin, file);
        plugins.add(plugin);
        return plugin;
    }
    
    class ReadonlyFolderException extends Exception{
        ReadonlyFolderException(String file){
            super("Readonly plogin folder: " + file);
        }
    }

    Class loadJavaFile(File file) throws Exception {
        Class cls = null;
        File classFile = new File(file.getPath().replace(".java", ".class"));

        //Check if has been compiled and is more recent. In this case load .class        
        if ((classFile.exists()) && (classFile.lastModified() > file.lastModified())) {
            try {
                cls = Loader.loadClass(classFile);
            } catch (Exception ex) {
                //Let's recompile then
                logger.log(Level.INFO, "Error loading class file: {0}", ex.getMessage());
            }
        }
        if (cls == null) {
            if (!Files.isWritable(file.getParentFile().toPath())){
                throw new ReadonlyFolderException(file.toString());
            }
            // Compile source file.       
            cls = Loader.compileClass(file);
        }         
        return cls;
    }

    public Plugin loadJavaPlugin(File file) throws Exception {
        Class cls = null;
        try{
            cls = loadJavaFile(file);
        } catch (ReadonlyFolderException ex){
            String cachePath = Setup.getCachePathPlugins();
            File parent = file.getParentFile();
            if (IO.isSamePath(parent.getAbsolutePath(), cachePath)){
                throw ex;
            }
            //Try caching the file if folder is read-only
            File cached = Paths.get(cachePath, file.getName()).toFile();
            if ((!cached.exists()) || (file.lastModified() > cached.lastModified())) {
                logger.info("Plugin is in readonly folder - caching to: " + file.getName());                                
                IO.copy(file.toString(), cached.toString());
            } else {
                logger.info("Plugin is in readonly folder - loading from cache: " + file.getName());                
            }
            cls = loadJavaFile(cached);
        }
        if (Plugin.class.isAssignableFrom(cls)) {
            Plugin plugin = (Plugin) cls.newInstance();
            initPlugin(plugin, file);
            plugins.add(plugin);            
            return plugin;
        } else {
            //Make dynamic class avilable in system class loader
            File classFile = new File(file.getPath().replace(".java", ".class"));
            Loader.loadClass(classFile);
            addDynamicClass(cls);
            return null;
        }
    }

    public Plugin loadGroovyPlugin(File file) throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        Class cls = loader.parseClass(file);

        if (Plugin.class.isAssignableFrom(cls)) {
            Plugin plugin = (Plugin) cls.newInstance();
            initPlugin(plugin, file);
            plugins.add(plugin);
            return plugin;
        } else {
            addDynamicClass(cls);
            return null;
        }
    }

    public void startPlugins() {
        logger.info("Start plugins");
        for (Plugin plugin : plugins) {
            startPlugin(plugin);
        }
    }

    public void startPlugin(Plugin plugin) {
        try {
            if (!plugin.isStarted()) {
                plugin.onStart();
                getProperties(plugin).started = true;
                logger.log(Level.INFO, "Started plugin: {0}", plugin.getPluginName());
                plugin.onStateChange(App.hasInstance() ? App.getInstance().getState() : State.Ready, null);
            }
        } catch (Exception ex) {
            //PyException don't have message set
            logger.log(Level.WARNING, "Error starting plugin: {0} - {1}", new Object[]{plugin.getPluginName(), (ex.getMessage() == null) ? ex.toString().trim() : ex.getMessage()});
        }
    }

    public void stopPlugins() {
        logger.info("Stop plugins");
        for (Plugin plugin : plugins) {
            stopPlugin(plugin);
        }
    }

    public void stopPlugin(Plugin plugin) {
        try {
            if (plugin.isStarted()) {
                getProperties(plugin).started = false;
                plugin.onStop();
                logger.log(Level.INFO, "Stopped plugin: {0}", plugin.getPluginName());
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    void initPlugin(Plugin plugin, File file) {
        PluginProperties properties = new PluginProperties();
        properties.file = file;
        properties.name = (file != null) ? file.getName() : plugin.getClass().getSimpleName();
        propertiesMap.put(plugin.getClass().getName(), properties);
    }

    public void loadPlugin(Plugin plugin, String name) {
        PluginProperties properties = new PluginProperties();
        properties.file = null;
        properties.name = name;
        propertiesMap.put(plugin.getClass().getName(), properties);
    }
    
    public void restartPlugins() {
        logger.info("Restart plugins");
        stopPlugins();
        startPlugins();
    }

    public void reloadPlugins() {
        logger.info("Reload plugins");
        stopPlugins();
        plugins.clear();
        loadExtensionsFolder();
        reloadPluginFolder();
        startPlugins();
        onInitialize(runCount);
    }

    public void restartPlugin(Plugin p) {
        logger.log(Level.INFO, "Restart plugin: {0}", p.getPluginName());
        stopPlugin(p);
        startPlugin(p);
        p.onInitialize(runCount);
    }

    public void unloadPlugin(Plugin p) {
        logger.log(Level.INFO, "Unload plugin: {0}", p.getPluginName());
        stopPlugin(p);
        plugins.remove(p);
    }

    public Plugin reloadPlugin(Plugin p) {
        if (p.getPluginFile() == null) {
            logger.log(Level.WARNING, "Cannot reload comand-line plugin: {0}", p.getPluginName());
            return null;
        }
        logger.log(Level.INFO, "Reload plugin file: {0}", p.getPluginFile().getPath());
        stopPlugin(p);
        plugins.remove(p);
        Plugin ret = loadPlugin(p.getPluginFile());
        if (ret != null) {
            startPlugin(ret);
            ret.onInitialize(runCount);
        }
        return ret;
    }

    public Plugin loadInitializePlugin(String fileName) {
        File file = resolveFile(fileName);        
        return loadInitializePlugin(file);
    }

    public Plugin loadInitializePlugin(File file) {
        Plugin p = loadPlugin(file);
        initializePlugin(p);
        return p;
    }
    
    public void initializePlugin(Plugin p){
        if (p != null) {
            startPlugin(p);
            p.onInitialize(runCount);
        }        
    }

    public boolean isLoaded(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file = Paths.get(Setup.getPluginsPath(), fileName).toFile();
            }
            for (Plugin p : getLoadedPlugins()) {
                if (p.getPluginFile().getCanonicalPath().equals(file.getCanonicalPath())) {
                    return true;
                }
            }
        } catch (Exception ex) {
        }
        return false;
    }

    public void onInitialize(int runCount) {
        for (Plugin p : plugins) {
            try {
                p.onInitialize(runCount);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    public void onUpdatedDevices() {
        for (Plugin p : plugins) {
            try {
                p.onUpdatedDevices();
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    public void onStoppedDevices() {
        for (Plugin p : plugins) {
            try {
                p.onStoppedDevices();
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    public void onStateChange(State state, State former) {
        for (Plugin p : plugins) {
            try {
                p.onStateChange(state, former);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }

        }
    }

    public void onExecutedFile(String fileName, Object result) {
        for (Plugin p : plugins) {
            try {
                p.onExecutedFile(fileName, result);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }

        }
    }

    //Disposing
    @Override
    public void close() {
        stopPlugins();
    }
}
