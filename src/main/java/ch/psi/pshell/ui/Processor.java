package ch.psi.pshell.ui;

import ch.psi.pshell.swing.Executor;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.DataManager;
import ch.psi.utils.IO;
import ch.psi.utils.IO.FilePermissions;
import ch.psi.utils.Sys;
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
        return Context.getInstance().getState().isProcessing();
    }

    public String getHomePath();

    default public String resolveFile(String fileName) throws IOException {
        String home = getHomePath();
        home = Context.getInstance().getSetup().expandPath(home);        
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
        String home = Context.getInstance().getSetup().expandPath(getHomePath());
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
            String home = Context.getInstance().getSetup().expandPath(getHomePath());
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
        JFileChooser chooser = new JFileChooser(Context.getInstance().getSetup().expandPath(getHomePath()));
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

    static ArrayList<Class> dynamicProviders = new ArrayList<>();

    public static <T extends Processor> void addServiceProvider(Class<T> cls) {
        removeServiceProvider(cls);
        dynamicProviders.add(cls);
    }
    
    public static <T extends Processor> void removeServiceProvider(Class<T> cls) {
        for (Class c : dynamicProviders.toArray(new Class[0])) { //toArray to avoid ConcurrentModificationEx
            if (c.getName().equals(cls.getName())) {
                dynamicProviders.remove(c);
            }
        }
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
        for (Processor processor : ServiceLoader.load(Processor.class, Sys.getClassLoader())) {
            ret.add(processor);
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
