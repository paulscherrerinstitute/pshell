package ch.psi.pshell.ui;

import ch.psi.utils.Sys;
import java.io.File;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interface for the workbench to import DAQ definition files as Python scripts.
 */
public interface Importer {

    public String getDescription();

    public String[] getExtensions();

    public String importFile(File file) throws Exception;

    static ArrayList<Class> dynamicProviders = new ArrayList<>();

    public static <T extends Importer> void addServiceProvider(Class<T> cls) {
        for (Class c : dynamicProviders.toArray(new Class[0])) { //toArray to avoid ConcurrentModificationEx
            if (c.getName().equals(cls.getName())) {
                dynamicProviders.remove(c);
            }
        }
        dynamicProviders.add(cls);
    }

    public static Iterable<Importer> getServiceProviders() {
        ArrayList<Importer> ret = new ArrayList<>();
        for (Class c : dynamicProviders) {
            try {
                ret.add((Importer) c.newInstance());
            } catch (Exception ex) {
                Logger.getLogger(Importer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (Importer importer : ServiceLoader.load(Importer.class, Sys.getClassLoader())) {
            ret.add(importer);
        }
        return ret;

    }
}
