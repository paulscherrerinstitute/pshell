package ch.psi.pshell.core;

import ch.psi.utils.Arr;
import ch.psi.utils.IO;
import ch.psi.utils.Loader;
import ch.psi.utils.State;
import ch.psi.utils.Sys;
import groovy.lang.GroovyClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Modifier;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Manages the loading of plugins, both dynamic (.java files) and compiled
 * (.class or .jar). Source or binary files should contain an implementation of
 * the Plugin interface.
 */
public class PluginManager implements AutoCloseable {

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

    PluginManager() {
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
        logger.info("Added dynamic class: " + cls.getName());
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
            Class c = Class.forName(className, true, Sys.getClassLoader());
            return loadPluginClass(c, null);
        } catch (Exception ex) {
            logger.warning("Error resolving plugin class: " + className + " - " + ex.getMessage());
        }
        return null;
    }

    public Plugin loadPluginClass(Class cls, File file) {
        try {
            if (loadedPluginClass(cls.getName())) {
                logger.info("Plugin class already loaded: " + cls.getName());
                return null;
            }
            Plugin plugin = (Plugin) cls.newInstance();
            initPlugin(plugin, file);
            plugins.add(plugin);
            return plugin;
        } catch (Exception ex) {
            logger.warning("Error instantiating plugin class: " + cls.getName() + " - " + ex.getMessage());
        }
        return null;
    }
    
    File resolveFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()){
            File file2 = new File(Context.getInstance().getSetup().expandPath("{plugins}/"+fileName));
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
        logger.info("Load plugin: " + fileName);
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
            logger.warning("Error loading plugin: " + fileName + " - " + ((ex.getMessage() == null) ? ex.toString().trim() : ex.getMessage()));
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
        File pluginFolder = Paths.get(Context.getInstance().getSetup().getPluginsPath()).toFile();
        return getFolderContents(pluginFolder, new String[]{"jar", "py", "groovy", "java"});
    }

    public static File[] getExtensionsFolderContents() {
        return getExtensionsFolderContents(Context.getInstance().getSetup().getExtensionsPath());
    }

    public static File[] getExtensionsFolderContents(String path) {
        File pluginFolder = Paths.get(path).toFile();
        return getFolderJarContents(pluginFolder);
    }

    static final ArrayList<File> extensions = new ArrayList<>();

    static public List<File> getExtensions() {
        return (List<File>) extensions.clone();
    }
        
    
    void loadExtensionsFolder() {
        List<File> files = loadExtensionsFolder(Context.getInstance().getSetup().getExtensionsPath());
        String jar = Context.getInstance().getSetup().getJarFile();
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

    List<File> loadExtensionsFolder(String folder) {
        File f = new File(folder);
        if (f.exists() && f.isDirectory() && (f.listFiles().length>0)){
            logger.info("Loading extensions folder: " + folder);
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

    //Respecting the order defined in plugins.properties
    void loadPluginFolder() {
        logger.info("Loading plugins folder");
        try (FileInputStream in = new FileInputStream(Context.getInstance().getSetup().getPluginsConfigurationFile())) {
            Properties properties = new Properties();
            properties.load(in);
            //File[] plugins = getPluginFolderContents();
            ArrayList<String> orderedPlugins = IO.getOrderedPropertyKeys(Context.getInstance().getSetup().getPluginsConfigurationFile());
            for (String pluginFile : orderedPlugins) {
                String prop = properties.getProperty(pluginFile);
                if (prop == null) {
                    logger.log(Level.WARNING, "Error parsing plugin configuration file");
                } else if (prop.trim().equals("enabled")) {
                    File plugin = Paths.get(Context.getInstance().getSetup().getPluginsPath(), pluginFile).toFile();
                    if (plugin.exists()) {
                        loadPlugin(plugin.toString());
                    } else {
                        logger.warning("Plugin file not found: " + pluginFile);
                    }
                }
            }
        } catch (FileNotFoundException | NoSuchFileException ex) {
            logger.log(Level.FINE, null, ex);
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    public Plugin loadPythonPlugin(File file) throws Exception {
        if (!"py".equals(System.getProperty("pshell.build.type"))) {
            throw new Exception("Python plugin can only be loaded if script type is 'py: " + file.getName());
        }
        String class_name = IO.getPrefix(file);
        org.python.core.PySystemState ss = new org.python.core.PySystemState();
        String[] libPath = Context.getInstance().getSetup().getLibraryPath();
        for (String lib : libPath) {
            if ((lib != null) && (lib.length() > 0)) {
                //TODO: if doing this that exception if built type is groovy or js -> Why PyString importing here is not lazy?
                //ss.path.append( new org.python.core.PyString(lib));                
                ss.path.append((org.python.core.PyObject) Class.forName("org.python.core.PyString").getConstructor(new Class[]{String.class}).newInstance(new Object[]{lib}));
            }
        }
        org.python.util.PythonInterpreter interp = new org.python.util.PythonInterpreter(null, null);
        interp.exec("from ch.psi.pshell.core import Plugin");
        interp.exec("from ch.psi.pshell.ui import Panel");
        interp.execfile(file.getAbsolutePath());
        org.python.core.PyObject py_script = interp.eval(class_name + "()");
        Plugin plugin = (Plugin) py_script.__tojava__(Plugin.class);
        initPlugin(plugin, file);
        plugins.add(plugin);
        return plugin;
    }

    Class loadJavaFile(File file) throws Exception {
        Class cls = null;
        File classFile = new File(file.getPath().replace(".java", ".class"));

        //Check if has been compiled and is more recent. In this case load .class        
        if ((classFile.exists()) && (classFile.lastModified() > file.lastModified())) {
            try {
                cls = Loader.loadClass(classFile.getPath());
            } catch (Exception ex) {
                //Let's recompile then
                logger.info("Error loading class file: " + ex.getMessage());
            }
        }
        if (cls == null) {
            // Compile source file.       
            logger.info("Compiling class file: " + file.getPath());
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new Exception("Java compiler is not present");
            }            
            if (compiler.run(null, System.out, System.err, file.getPath()) == 0) {
                // Load and instantiate compiled class.
                File location = (file.getParentFile() == null) ? new File(".") : file.getParentFile();
                //URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{location.toURI().toURL()});
                Sys.addToClassPath(location);
                cls = Class.forName(IO.getPrefix(file), true, Sys.getClassLoader());
            } else {
                throw new Exception("Error compiling plugin: " + file);
            }
        }
        return cls;
    }

    public Plugin loadJavaPlugin(File file) throws Exception {
        Class cls = loadJavaFile(file);
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
                logger.info("Started plugin: " + plugin.getPluginName());
                plugin.onStateChange(Context.getInstance().getState(), null);
            }
        } catch (Exception ex) {
            //PyException don't have message set
            logger.warning("Error starting plugin: " + plugin.getPluginName() + " - " + ((ex.getMessage() == null) ? ex.toString().trim() : ex.getMessage()));
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
                logger.info("Stopped plugin: " + plugin.getPluginName());
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
        loadPluginFolder();
        startPlugins();
        onInitialize(Context.getInstance().runCount);
    }

    public void restartPlugin(Plugin p) {
        logger.info("Restart plugin: " + p.getPluginName());
        stopPlugin(p);
        startPlugin(p);
        p.onInitialize(Context.getInstance().runCount);
    }

    public void unloadPlugin(Plugin p) {
        logger.info("Unload plugin: " + p.getPluginName());
        stopPlugin(p);
        plugins.remove(p);
    }

    public Plugin reloadPlugin(Plugin p) {
        if (p.getPluginFile() == null) {
            logger.warning("Cannot reload comand-line plugin: " + p.getPluginName());
            return null;
        }
        logger.info("Reload plugin file: " + p.getPluginFile().getPath());
        stopPlugin(p);
        plugins.remove(p);
        Plugin ret = loadPlugin(p.getPluginFile());
        if (ret != null) {
            startPlugin(ret);
            ret.onInitialize(Context.getInstance().runCount);
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
            p.onInitialize(Context.getInstance().runCount);
        }        
    }

    public boolean isLoaded(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file = Paths.get(Context.getInstance().getSetup().getPluginsPath(), fileName).toFile();
            }
            for (ch.psi.pshell.core.Plugin p : Context.getInstance().getPlugins()) {
                if (p.getPluginFile().getCanonicalPath().equals(file.getCanonicalPath())) {
                    return true;
                }
            }
        } catch (Exception ex) {
        }
        return false;
    }

    void onInitialize(int runCount) {
        for (Plugin p : plugins) {
            try {
                p.onInitialize(runCount);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    void onUpdatedDevices() {
        for (Plugin p : plugins) {
            try {
                p.onUpdatedDevices();
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    void onStoppedDevices() {
        for (Plugin p : plugins) {
            try {
                p.onStoppedDevices();
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    void onStateChange(State state, State former) {
        for (Plugin p : plugins) {
            try {
                p.onStateChange(state, former);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }

        }
    }

    protected void onExecutedFile(String fileName, Object result) {
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
