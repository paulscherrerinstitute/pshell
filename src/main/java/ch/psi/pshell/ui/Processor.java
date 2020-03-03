package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.DataManager;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.State;
import ch.psi.utils.swing.SwingUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * File-based process executor - and alternative to script execution, in order
 * to provide extensions to create other syntaxes for DAQ.
 */
public interface Processor {

    public String getType();

    public String getDescription();

    public String[] getExtensions();

    public void execute() throws Exception;

    public void abort() throws InterruptedException;
    
    /**
     * Support to command line starting execution of processor. If
     * implementation expects arguments from command line, this must be
     * overridden.
     */
    default public void execute(String file, Map<String, Object> vars) throws Exception {
        open(file);
        execute();
    }
    
    default Object waitComplete(int timeout) throws Exception {
        Chrono chrono = new Chrono();
        try {
            chrono.waitCondition(new Condition() {
                @Override
                public boolean evaluate() throws InterruptedException {
                    return completed();
                }
            }, timeout);
        } catch (TimeoutException ex) {
        }        
        Object result = getResult();
        if (result instanceof Exception){
            throw (Exception)result;
        }
        return result;
    }        
    
    default Object getResult(){
        return null;
    }    
    
    default boolean completed() {
        return !Context.getInstance().getState().isProcessing();
    }            

    public String getHomePath();

    default public String resolveFile(String fileName) throws IOException {
        String home = getHomePath();
        if ((home != null) && !home.isEmpty()) {
            try {
                Path p = Paths.get(home, fileName);
                if (p.toFile().exists()) {
                    return p.toString();
                }
            } catch (Exception ex) {
            }
        }
        File f = new File(fileName);
        if (f.exists()) {
            return f.getPath();
        }
        throw new FileNotFoundException("File not found: " + fileName);
    }

    public void open(String fileName) throws IOException;

    default public void save() throws IOException {
        String fileName = getFileName();
        if (fileName == null) {
            JFileChooser chooser = new JFileChooser(getHomePath());
            FileNameExtensionFilter filter = new FileNameExtensionFilter(getDescription(), getExtensions());
            chooser.setFileFilter(filter);
            if (chooser.showSaveDialog(getPanel()) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            fileName = chooser.getSelectedFile().getAbsolutePath();
        }
        saveAs(fileName);
    }

    public void saveAs(String fileName) throws IOException;

    public default JPanel getPanel() {
        //Processors must be instances of JPanel
        return (JPanel) this;
    }

    public boolean hasChanged();

    default public boolean checkChangeOnClose() throws IOException {
        if (hasChanged()) {
            switch (SwingUtils.showOption(getPanel(), "Closing", "Document has changed. Do you want to save it?", SwingUtils.OptionType.YesNoCancel)) {
                case Yes:
                    save();
                    break;
                case No:
                    break;
                case Cancel:
                    return false;
            }
        }
        return true;
    }

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

    public static boolean checkProcessorsPlotting(String root, String path, DataManager dm) {
        HashMap<FileNameExtensionFilter, Processor> processors = new HashMap<>();
        for (Processor processor : Processor.getServiceProviders()) {
            try {
                processor.plotDataFile(Paths.get(root, path + "." + dm.getProvider().getFileType()).toFile());
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    default void plotDataFile(File file) throws Exception {
        throw new Exception("Not implemented");
    }

    default boolean createFilePanel() {
        return false;
    }

    default boolean createMenuNew() {
        return false;
    }
    
    default boolean canStep() {
        return false;
    }    
    
    default boolean canPause() {
        return false;
    }     

    default void step(){
    }    
    

    default void pause(){
    }    
    
    default void onStateChanged(State state, State former){
        
    }
    
    default void onTaskFinished(Task task) {
        
    }
    
    
}
