package ch.psi.pshell.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Dynamic loading of .class or .jar files.
 */
public class Loader {          
    static final Logger logger = Logger.getLogger(Loader.class.getName());
    
    public static Class loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        boolean sys_cl = classLoader == Sys.getDynamicClassLoader();
        logger.log(Level.FINER, "Loading class: {0}{1}", new Object[]{className, sys_cl ? " (sys class loader)" : ""});
        return classLoader.loadClass(className);
    }

    public static Class loadClass(File file) throws Exception {
        String fileName = file.getAbsolutePath();
        String extension = IO.getExtension(fileName);
        String cls = IO.getPrefix(fileName);
        String folder = IO.getFolder(fileName);

        if (!extension.toLowerCase().equals("class")) {
            throw new IllegalArgumentException(fileName);
        }
        return loadClass(Sys.newDynamicClassLoader(new String[]{folder}), cls);
    }    

    public static Class compileClass(File file) throws Exception {
        logger.log(Level.INFO, "Compiling class file: {0}", file.getPath());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();                     
        if (compiler == null) {
            throw new Exception("Java compiler is not present");
        }                  
        if (compiler.run(null, System.out, System.err, "-proc:none", file.getPath()) == 0) {
            File location = (file.getParentFile() == null) ? new File(".") : file.getParentFile();            
            File classFile = new File(file.getPath().replace(".java", ".class"));
            return Loader.loadClass(classFile);
        } else {            
               throw new Exception("Error compiling plugin: " + file);
        }       
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
        Class[] ret = new Class[classes.size()];
        ClassLoader cl = Sys.newDynamicClassLoader(new String[]{fileName});
        for (int i = 0; i < classes.size(); i++) {
            ret[i] = loadClass(cl, classes.get(i));
        }        
        return ret;
    }
}
