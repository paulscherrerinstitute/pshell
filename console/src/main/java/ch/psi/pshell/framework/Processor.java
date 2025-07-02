package ch.psi.pshell.framework;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * File-based process executor - and alternative to script execution, in order
 * to provide extensions to create other syntaxes for DAQ.
 */
public interface Processor extends Executor {

    public String getType();

    public String getDescription();

    public String[] getExtensions();
    
    default public String getScanType(){return null;}

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

    @Override
    default boolean isExecuting() {
        return Context.getState().isProcessing();
    }

    public String getHomePath();

    default public String resolveFile(String fileName) throws IOException {
        String home = getHomePath();
        home = Setup.expandPath(home);        
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

    default public void open() throws IOException {
        String home = Setup.expandPath(getHomePath());
        File homeFile = new File(home);
        if (!homeFile.exists()) {
            homeFile.mkdirs();
        }
        JFileChooser chooser = new JFileChooser(home);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(getDescription(), getExtensions());
        chooser.setFileFilter(filter);

        int rVal = chooser.showOpenDialog(getPanel());
        if (rVal == JFileChooser.APPROVE_OPTION) {
            open(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void open(String fileName) throws IOException;

    default public void save() throws IOException {
        save(null);
    }

    default public void save(FilePermissions filePermissions) throws IOException {
        String fileName = getFileName();
        if (fileName == null) {
            String home = Setup.expandPath(getHomePath());
            File homeFile = new File(home);
            if (!homeFile.exists()) {
                homeFile.mkdirs();
            }
            JFileChooser chooser = new JFileChooser(home);
            FileNameExtensionFilter filter = new FileNameExtensionFilter(getDescription(), getExtensions());
            chooser.setFileFilter(filter);
            if (chooser.showSaveDialog(getPanel()) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            fileName = chooser.getSelectedFile().getAbsolutePath();
            if (IO.getExtension(chooser.getSelectedFile().getAbsolutePath()).isEmpty()){          
                if (getExtensions().length > 0) {
                    fileName += "." + getExtensions()[0];
                }
            }
        }
        saveAs(fileName);
        if (filePermissions!=null){
            IO.setFilePermissions(fileName, filePermissions);
        }
    }
    
    default public void saveAs() throws IOException {
        save(null);
    }

    default public void saveAs(FilePermissions filePermissions) throws IOException {       
        JFileChooser chooser = new JFileChooser(Setup.expandPath(getHomePath()));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(getDescription(), getExtensions());
        chooser.setFileFilter(filter);
        try {
            String fileName = getFileName();
            if (fileName != null) {
                chooser.setSelectedFile(new File(fileName));
            }
        } catch (Exception ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.WARNING, null, ex);            
        }
        int rVal = chooser.showSaveDialog(getPanel());
        if (rVal == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (IO.getExtension(chooser.getSelectedFile().getAbsolutePath()).isEmpty()) {
                fileName += "." + getExtensions()[0];
            }
            saveAs(fileName);
            if (filePermissions!=null){
                IO.setFilePermissions(fileName, filePermissions);
            }
        }
    }
    

    public void saveAs(String fileName) throws IOException;

    default public void saveAs(String fileName, FilePermissions filePermissions) throws IOException{
        saveAs(fileName);
        IO.setFilePermissions(fileName, filePermissions);
    }

    default public void clear() throws IOException{        
    }
    
    public default JPanel getPanel() {
        //Processors must be instances of JPanel
        return (JPanel) this;
    }

    default public boolean checkChangeOnClose() throws IOException {
        if (hasChanged()) {
            switch (SwingUtils.showOption(getPanel(), "Closing", "Document has changed. Do you want to save it?", SwingUtils.OptionType.YesNoCancel)) {
                case Yes -> save();
                case No -> {}
                case Cancel -> {return false;}
            }
        }
        return true;
    }

    final static ArrayList<Class> dynamicProviders = new ArrayList<>();
    

    public static <T extends Processor> void addServiceProvider(Class<T> cls) {
        synchronized(dynamicProviders){
            removeServiceProvider(cls);
            dynamicProviders.add(cls);
        }
    }
    
    public static <T extends Processor> void removeServiceProvider(Class<T> cls) {
        synchronized(dynamicProviders){
            for (Class c : dynamicProviders.toArray(new Class[0])) { //toArray to avoid ConcurrentModificationEx
                if (c.getName().equals(cls.getName())) {
                    dynamicProviders.remove(c);
                }
            }
        }
    }

    public static Iterable<Processor> getServiceProviders() {
        ArrayList<Processor> ret = new ArrayList<>();
        synchronized(dynamicProviders){
            for (Class c : dynamicProviders) {
                try {
                    ret.add((Processor) c.newInstance());
                } catch (Exception ex) {
                    Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        try {
            for (Processor processor : ServiceLoader.load(Processor.class, Sys.getClassLoader())) {
                ret.add(processor);
            }        
        } catch (Throwable ex) {
            Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;

    }

    public static boolean canProcessorsPlot(String root, String path, DataManager dm) {
        HashMap<FileNameExtensionFilter, Processor> processors = new HashMap<>();
        File rootFile = new File(root);
        for (Processor processor : Processor.getServiceProviders()) {
            if (processor.isPlottable(rootFile, path, dm)){
                return true;
            }
        }
        return false;
    }
    
    public static boolean tryProcessorsPlot(String root, String path, DataManager dm) {
        HashMap<FileNameExtensionFilter, Processor> processors = new HashMap<>();
        File rootFile = new File(root);
        for (Processor processor : Processor.getServiceProviders()) {
            if (processor.isPlottable(rootFile, path, dm)){
                try {
                    processor.plotDataFile(rootFile, path, dm);
                    return true;
                } catch (Exception e) {
                    Logger.getLogger(Processor.class.getName()).log(Level.WARNING, null, e);  
                }
            }
        }
        return false;
    }
    
    default void plotDataFile(File file) throws Exception {
        plotDataFile(file, null);
    }
    
    default void plotDataFile(File file, DataManager dm) throws Exception {
        plotDataFile(file, null, dm);
    }
    
    default void plotDataFile(File file, String path, DataManager dm) throws Exception {
        throw new Exception("Not implemented");
    }   
    
    default boolean isPlottable(File file) {
        return isPlottable (file, null);
    } 
    
    default boolean isPlottable(File file, DataManager dm) {
        return isPlottable (file, null, dm);
    } 
    
    default boolean isPlottable(File file, String path, DataManager dm) {
        return false;
    }     
    
    default boolean createFilePanel() {
        return false;
    }

    default boolean createMenuNew() {
        return false;
    }

    @Override
    default boolean canStep() {
        return false;
    }

    @Override
    default boolean canPause() {
        return false;
    }  
    
    @Override
    default boolean canSave() {
        String[] extensions = getExtensions();
        return (extensions!=null) && (extensions.length>0);
    }   
    
    default void step() {
    }

    default void pause() {
    }
    
    default void resume() {
    }    

    default void onStateChanged(State state, State former) {

    }

    default void onTaskFinished(Task task) {

    }
                
}
