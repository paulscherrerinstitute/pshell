package ch.psi.pshell.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System utilities.
 */
public class Sys {

    final static Boolean hasJython;
    static {
        boolean aux=false;
        try{
            Class.forName("org.python.core.PyObject");            
            aux=true;
        } catch (Throwable t){                        
        }
        hasJython = aux;
    }
        
            
    public enum OSFamily {

        Windows,
        Linux,
        Mac,
        Solaris,
        Unknown
    }

    public static OSFamily getOSFamily() {
        String name = java.lang.System.getProperty("os.name").toLowerCase();

        if (name.contains("win")) {
            return OSFamily.Windows;
        }
        if (name.contains("mac")) {
            return OSFamily.Mac;
        }
        if (name.contains("nix") || name.contains("nux") || name.contains("aix")) {
            return OSFamily.Linux;
        }
        if (name.contains("sunos")) {
            return OSFamily.Solaris;
        }
        return OSFamily.Unknown;
    }

    public static boolean isMac() {
        return getOSFamily() == OSFamily.Mac;
    }

    public static boolean isWindows() {
        return getOSFamily() == OSFamily.Windows;
    }

    public static boolean isLinux() {
        return getOSFamily() == OSFamily.Linux;
    }

    public enum Arch {
        X86,
        X86_64,
        ARM64,
        ARM,
        Unknown
    }    
    
    public static Arch getArch() {
        String arch = java.lang.System.getProperty("os.arch").toLowerCase();
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return Arch.ARM64;
        } else if (arch.contains("64")) {
            return Arch.X86_64;
        } else if (arch.contains("86")){
            return Arch.X86;
        } else if (arch.contains("arm")) {
            return Arch.ARM;
        }
        return Arch.Unknown;
    }
    
    public static boolean isX86() {
        return getArch() == Arch.X86;
    }

    public static boolean isARM64() {
        return getArch() == Arch.ARM64;
    }

    public static boolean isX86_64() {
        return getArch() == Arch.X86_64;
    }
    
    public static boolean isArm() {
        return getArch() == Arch.ARM;
    }

    public static double getJavaVersion() {
        try {
            return Double.parseDouble(System.getProperty("java.specification.version"));
        } catch (Exception ex) {
            return 1.7; //If cannot parse assumes 1.7
        }
    }

    public static String getVmName() {
        return String.valueOf(System.getProperty("java.vm.name"));
    }

    public static String getUserName() {
        return String.valueOf(System.getProperty("user.name"));
    }

    public static String getUserHome() {
        return String.valueOf(System.getProperty("user.home"));
    }

    public static String getCurDir(){
        return String.valueOf(System.getProperty("user.dir"));
    }
    
    public static String getCommand() {
        return String.valueOf(System.getProperty("sun.java.command"));
    }

    public static String getProcessName() {
        try {
            return String.valueOf(ManagementFactory.getRuntimeMXBean().getName());
        } catch (Exception ex) {
            return "Unknown";
        }
    }

    public static int getPid() {
        try {
            return Integer.valueOf(getProcessName().split("@")[0]);
        } catch (Exception ex) {
            return -1;
        }
    }

    public static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "Unknown";
        }
    }

    public static String getTempFolder() {
        return System.getProperty("java.io.tmpdir");
    }

    public static String getNativeLibExtension() {
        switch (getOSFamily()) {
            case Windows:
                return "dll";
            case Mac:
                return "jnilib";
            default:
                return "so";
        }
    }
    
    public static String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public static boolean is64bits() {
        return System.getProperty("os.arch").contains("64");
    }

    public static String[] getClassPath(){
        try{
           return System.getProperty("java.class.path").split(File.pathSeparator);
        } catch (Exception ex){
            return new String[0];
        }
    }    
 
    public static DynamicClassLoader newDynamicClassLoader() {
        try {
            return newDynamicClassLoader(new String[0]);
        } catch (MalformedURLException ex) {
            //Should not happen
            return null;
        }
    }
    
    public static DynamicClassLoader newDynamicClassLoader(String[] folderNames) throws MalformedURLException {
        URL[] urls = new URL[folderNames.length];
        for (int i = 0; i < folderNames.length; i++) {
            urls[i] = new File(folderNames[i]).toURI().toURL();
        }
        return new  DynamicClassLoader(urls);
    }
    
    public static class DynamicClassLoader extends URLClassLoader {
        public DynamicClassLoader(URL[] urls) {
            super(urls);
        }
        
        public DynamicClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);            
        }
        
        public void addClass(Class cls) {
            try {
                loadClass(cls.getName());
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Sys.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }
    
    
    static final DynamicClassLoader dynamicClassLoader = newDynamicClassLoader();
    public static DynamicClassLoader getDynamicClassLoader(){     
        return dynamicClassLoader;       
    }
    
    
    public static void addToClassPath(File file)  throws Exception {        
        addToClassPath(file.getPath());
    }

    public static void addToClassPath(String path) throws Exception {        
        String[] classPath = getClassPath();
        if (!Arr.containsEqual(classPath, path)){  
            File f = new File(path);
            if (f.exists()){
                URL u = f.toURI().toURL();
                 if (getDynamicClassLoader() instanceof DynamicClassLoader dynamicClassLoader){
                    dynamicClassLoader.addURL(u);
                    //Just to keep track, java.class.path is not parsed again by classloader.
                    System.setProperty("java.class.path", String.join(File.pathSeparator, Arr.append(classPath, path)));
                }
            }
        }
    }
    
    public static String[] getLibraryPath(){
        try{            
            return System.getProperty("java.library.path").split(File.pathSeparator);
        } catch (Exception ex){
            return new String[0];
        }
    }

    public static final List<ClassLoader> childClassLoaders = new ArrayList<>();
    public static void addToLibraryPath(String path) throws Exception {
        String[] libraryPath =  getLibraryPath();
        if (!Arr.containsEqual(libraryPath, path)){     
            //Only work in Java>=10 if called before usr_paths is initialized (first loadLibrary call): " + path);
            //java.library.path is not parsed again by classloader.
            System.setProperty("java.library.path", String.join(File.pathSeparator, Arr.append(libraryPath, path)));
        }
    }

    public static void loadJarLibrary(Class cls, String resourceName) throws IOException {
        try (InputStream in = cls.getResourceAsStream(resourceName)) {
            byte[] buffer = new byte[1024];
            int read = -1;
            File temp = File.createTempFile(resourceName, "");
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
            System.load(temp.getAbsolutePath());
        }
    }

    public static int getPid(Process p) {
        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                return f.getInt(p);
            } else if (p.getClass().getName().equals("java.lang.Win32Process")
                    || p.getClass().getName().equals("java.lang.ProcessImpl")) {
                Field f = p.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handle = f.getLong(p);

                Windows.Kernel32 kernel = Windows.Kernel32.INSTANCE;
                return kernel.GetProcessId((int) handle);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return -1;
    }    
    
    public static int getUid() {
        if (isWindows()) {
            return -1;
        }

        return Posix.getuid();
    }
    
    public static int setUid(int uid) {
        if (isWindows()) {
            return -1;
        }
        return Posix.setuid(uid);
    }    
    
    public static int getEuid() {
        if (isWindows()) {
            return -1;
        }

        return Posix.geteuid();
    }    
    
    public static int setEuid(int uid) {
        if (isWindows()) {
            return -1;
        }
        return Posix.seteuid(uid);
    }    
        
    public static int getGid() {
        if (isWindows()) {
            return -1;
        }

        return Posix.geteuid();
    }       

    public static int setGid(int gid) {
        if (isWindows()) {
            return -1;
        }
        return Posix.setgid(gid);
    }
    
    public static int getEgid() {
        if (isWindows()) {
            return -1;
        }

        return Posix.geteuid();
    }       

    public static int setEgid(int gid) {
        if (isWindows()) {
            return -1;
        }
        return Posix.setgid(gid);
    }    

    public static int setUmask(int umask) {
        if (isWindows()) {
            return -1;
        }
        return Posix.setumask(umask);
    }   
    
    public static class UserInfo{
        public final String name;
        public final int uid;
        public final int gid;        
        
        UserInfo (String name, int uid, int gid){
            this.name = name;
            this.uid = uid;
            this.gid = gid;
        }
        
        @Override
        public String toString(){
            return "user name=" + name+ " uid=" + uid+ " gid=" + gid;
        }
    }
    
    public static UserInfo getUserInfo(String userName) {
        if (isWindows()) {
            return null;
        }
        Posix.Passwd passwd = Posix.getpwnam(userName);    
        return new UserInfo(passwd.getName(), passwd.getUID(), passwd.getGID());
    }               
    
     public static void setEnvironmentVariable(String key, String value) throws Exception{ 
            Map<String, String> env = System.getenv(); 
            Class<?> cl = env.getClass(); 
            Field field = cl.getDeclaredField("m"); 
            field.setAccessible(true); 
            Map<String, String> writableEnv = (Map<String, String>) field.get(env); 
            writableEnv.put(key, value); 
    } 
     
    public static boolean hasJython(){
        return hasJython;
    }                                  
}
