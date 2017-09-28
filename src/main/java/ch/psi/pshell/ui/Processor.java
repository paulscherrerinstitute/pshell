package ch.psi.pshell.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 * File-based process executor - and alternative to script execution, in order to provide extensions
 * to create other syntaxes for DAQ.
 */
public interface Processor {

    public String getType();

    public String getDescription();

    public String[] getExtensions();

    public void execute() throws Exception;

    public void abort();

    public void execute(String file, Map<String, Object> vars) throws Exception;

    public String getHomePath();

    public void open(String fileName) throws IOException;

    public void save() throws IOException;

    public void saveAs(String fileName) throws IOException;

    public default JPanel getPanel() {
        //Processors must be instances of JPanel
        return (JPanel) this;
    }

    public boolean hasChanged();

    public boolean checkChangeOnClose() throws IOException;

    public String getFileName();

    static ArrayList<Class> dynamicProviders = new ArrayList<>();

    public static <T extends Processor> void addServiceProvider(Class<T> cls) {
        for (Class c : dynamicProviders.toArray(new Class[0])) { //toArray to avoid ConcurrentModificationEx
            if (c.getName().equals(cls.getName())) {
                dynamicProviders.remove(c);
            }
        }
        dynamicProviders.add(cls);
    }

    public static Iterable<Processor> getServiceProviders() {
        ArrayList<Processor> ret = new ArrayList<>();
        for (Class c : dynamicProviders) {
            try {
                ret.add((Processor) c.newInstance());
            } catch (Exception ex) {
                Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (Processor processor : ServiceLoader.load(Processor.class)) {
            ret.add(processor);
        }
        return ret;

    }

    default void plotDataFile(File file) throws Exception {
        throw new Exception("Not implemented");
    }

}
