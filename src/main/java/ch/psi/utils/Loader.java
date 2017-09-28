package ch.psi.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * Dynamic loading of .class or .jar files.
 */
public class Loader {

    public static Class loadClass(URLClassLoader classLoader, String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    public static Class loadClass(String fileName) throws IllegalArgumentException, MalformedURLException, ClassNotFoundException {
        String extension = IO.getExtension(fileName);
        String cls = IO.getPrefix(fileName);
        String folder = IO.getFolder(fileName);

        if (!extension.toLowerCase().equals("class")) {
            throw new IllegalArgumentException(fileName);
        }

        URLClassLoader classLoader = newClassLoader(new String[]{folder});
        return loadClass(classLoader, cls);
    }

    public static Class[] loadJar(String fileName) throws MalformedURLException, ClassNotFoundException, IOException {
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
        URLClassLoader classLoader = newClassLoader(new String[]{fileName});
        Class[] ret = new Class[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            ret[i] = loadClass(classLoader, classes.get(i));
        }
        return ret;
    }

    public static URLClassLoader newClassLoader(String[] folderNames) throws MalformedURLException {
        URL[] urls = new URL[folderNames.length];
        for (int i = 0; i < folderNames.length; i++) {
            urls[i] = new File(folderNames[i]).toURI().toURL();
        }
        URLClassLoader classLoader = new URLClassLoader(urls);
        return classLoader;
    }
}
