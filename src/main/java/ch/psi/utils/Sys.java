package ch.psi.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * System utilities.
 */
public class Sys {

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

    public static String getCommand() {
        return String.valueOf(System.getProperty("sun.java.command"));
    }

    public static String getProcessName() throws IOException {
        return String.valueOf(ManagementFactory.getRuntimeMXBean().getName());
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
    
    public static boolean is64bits() {
        return System.getProperty("os.arch").contains("64");
    }    

    public static void addToClassPath(String s) throws Exception {
        File f = new File(s);
        URL u = f.toURL();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[]{u});
    }

    public static void addToLibraryPath(String pathToAdd) throws Exception {
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);
        final String[] paths = (String[]) usrPathsField.get(null);
        for (String path : paths) {
            if (path.equals(pathToAdd)) {
                return;
            }
        }
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = pathToAdd;
        usrPathsField.set(null, newPaths);
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
        } catch (Throwable e) {
            System.err.println(e);
        }
        return -1;
    }    
}
