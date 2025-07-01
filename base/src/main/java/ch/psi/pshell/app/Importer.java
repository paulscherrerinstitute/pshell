package ch.psi.pshell.app;

import ch.psi.pshell.utils.Sys;
import java.io.File;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
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
        try{
            for (Importer importer : ServiceLoader.load(Importer.class, Sys.getClassLoader())) {
                ret.add(importer);
            }
        } catch (Throwable ex) {
            Logger.getLogger(Importer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;

    }
}
