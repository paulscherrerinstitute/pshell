package ch.psi.pshell.scripting;

import ch.psi.utils.IO;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 *
 */
public class Library {

    final static Logger logger = Logger.getLogger(Library.class.getName());

    ScriptEngine engine;

    Library(ScriptEngine engine) {
        this.engine = engine;
    }

    String[] path = new String[]{"."};

    String[] getPath() {
        return path;
    }

    void setPath(String[] path) {
        this.path = path;
        if (this.path != null) {
            for (int i = 0; i < path.length; i++) {
                this.path[i] = this.path[i].trim();
            }
        }
    }

    ScriptType getScriptType() {
        try {
            if (engine.getFactory().getExtensions().size() > 0) {
                return ScriptType.valueOf(engine.getFactory().getExtensions().get(0));
            }
        } catch (Exception ex) {
        }
        return null;
    }

    String getDefaultExtension() {
        ScriptType type = getScriptType();
        if (type != null) {
            return type.toString();
        }
        return engine.getFactory().getLanguageName().toLowerCase();
    }

    public String resolveFile(String name) {
        if (IO.getExtension(name).isEmpty()) {
            name = name + "." + getDefaultExtension();
        }

        //First check if an absolute path
        File file = new File(name);
        if (file.exists()) {
            return name;
        }

        //Then look in the library path
        for (String entry : getPath()) {
            file = new File(entry, name);
            if (file.exists()) {
                return file.getPath();
            }
        }
        return null;
    }

    /**
     * Executes the script. If defines a class with Java-style naming then returns the class
     */
    public Class load(String name) throws IOException, ScriptException {
        String fileName = resolveFile(name);
        if (fileName == null) {
            throw new FileNotFoundException();
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
            engine.eval(reader);

            if (getScriptType() == ScriptType.groovy) {
                ClassLoader parent = Library.class.getClassLoader();
                groovy.lang.GroovyClassLoader loader = new groovy.lang.GroovyClassLoader(parent);
                try {
                    Class cls = loader.parseClass(new File(fileName));
                    return cls;
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }

            //engine.getContext().getScopes().
        } catch (ScriptException ex) {
            logger.log(Level.INFO, null, ex);
            throw ex;
        } catch (Throwable ex) {
            logger.log(Level.INFO, null, ex);
            throw ex;
        }
        return null;
    }

    public Class loadClass(String className) throws FileNotFoundException, IOException {
        String fileName = resolveFile(className);
        if (fileName == null) {
            throw new FileNotFoundException();
        }

        ClassLoader parent = Library.class.getClassLoader();
        groovy.lang.GroovyClassLoader loader = new groovy.lang.GroovyClassLoader(parent);
        Class cls = loader.parseClass(new File(fileName));
        return cls;

    }
}
