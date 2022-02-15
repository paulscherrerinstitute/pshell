package ch.psi.utils;

import java.util.ArrayList;

/**
 * Dynamic loading of .class or .jar files.
 */
public class Loader {          
    
    public static Class loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    public static Class loadClass(String fileName) throws Exception {
        String extension = IO.getExtension(fileName);
        String cls = IO.getPrefix(fileName);
        String folder = IO.getFolder(fileName);

        if (!extension.toLowerCase().equals("class")) {
            throw new IllegalArgumentException(fileName);
        }
        Sys.addToClassPath(folder);
        return loadClass(Sys.getClassLoader(), cls);
    }

    public static Class[] loadJar(String fileName) throws Exception {
        String extension = IO.getExtension(fileName);

        if (!extension.toLowerCase().equals("jar")) {
            throw new IllegalArgumentException(fileName);
        }

        ArrayList<String> classes = new ArrayList<>();
        String[] files = IO.getJarContents(fileName);
        for (String file : files) {
            extension = IO.getExtension(file);
            if (extension.equals("class")) {
                file = file.substring(0, file.length() - extension.length() - 1).replace("/", ".");
                classes.add(file);
            }
        }
        Sys.addToClassPath(fileName);
        Class[] ret = new Class[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            ret[i] = loadClass(Sys.getClassLoader(), classes.get(i));
        }
        return ret;
    }
}
