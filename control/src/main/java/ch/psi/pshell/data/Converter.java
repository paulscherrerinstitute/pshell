package ch.psi.pshell.data;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.swing.ExtensionFileFilter;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Threading;
import java.awt.Component;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

public interface Converter {

    public String getName();

    public String getExtension();
    
    default boolean canConvert(Map<String, Object> info) throws Exception{
        return false;
    }  
    
    default boolean canConvert(File file) throws Exception{
        return false;
    }      
    
    default boolean canConvert(DataManager dataManager, File file){
        try{
            return canConvert(file);
        } catch (Exception ex) {
            return false;
        }              
    }          
    
    default boolean canConvert(DataManager dataManager, String root, String path, Map<String, Object> info){
        try{
            if (info==null){
                info = dataManager.getInfo(root, path);
            }
            return canConvert(info);
        } catch (Exception ex) {
            return false;
        }  
    }
  
    
    default void convert(DataSlice slice, Map<String, Object> info, Map<String, Object> attrs, File output) throws Exception{
        throw new Exception ("Not implemented");
    }

    default void convert(DataManager dataManager, String root, String path, File output) throws Exception {
        DataSlice slice = dataManager.getData(root, path);
        Map<String, Object> info = dataManager.getInfo(root, path);
        Map<String, Object> attrs = dataManager.getAttributes(root, path);
        convert(slice, info, attrs, output);
    }
    
    default void convert(File file, File output) throws Exception{        
        DataManager dataManager = new DataManager(file.isDirectory() ?  Context.getFormat().getId() : IO.getExtension(file),
                                                 Context.getLayout().getId());
        convert(dataManager, file.getParent(), file.getName(), output);
    }        

    default File convert(DataManager dataManager, String root, String path, Component parent) throws Exception {
        JFileChooser chooser = new JFileChooser(getDefaultOutputFolder(root, path));        
        chooser.addChoosableFileFilter(new ExtensionFileFilter(getName() + " files (*." + getExtension() + ")", new String[]{getExtension()}));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(Paths.get(chooser.getCurrentDirectory().toString(),IO.getPrefix(path)+"." + getExtension()).toFile());
        int rVal = chooser.showSaveDialog(parent);
        File ret = null;
        if (rVal == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (IO.getExtension(fileName).isEmpty()) {
                fileName += "." + getExtension();
            }
            ret = new File(fileName);
            convert(dataManager, root, path, ret);
        }
        return ret;
    }
    
    default File convert(File file, Component parent) throws Exception {
        JFileChooser chooser = new JFileChooser(file.getParent());        
        chooser.addChoosableFileFilter(new ExtensionFileFilter(getName() + " files (*." + getExtension() + ")", new String[]{getExtension()}));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(Paths.get(chooser.getCurrentDirectory().toString(),IO.getPrefix(file)+"." + getExtension()).toFile());
        int rVal = chooser.showSaveDialog(parent);
        File ret = null;
        if (rVal == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (IO.getExtension(fileName).isEmpty()) {
                fileName += "." + getExtension();
            }
            ret = new File(fileName);
            convert(file, ret);
        }
        return ret;
    }    
        
    
    default String getDefaultOutputFolder(String root, String path){
        File f = Paths.get(root, path).toFile();
        if (f.isDirectory()){
            return f.toString();
        }
        if (f.isFile()){
            return f.getParentFile().toString();
        }        
        f = new File(root);
        if (f.isDirectory()){
            return f.toString();
        }
        if (f.isFile()){
            return f.getParentFile().toString();
        }           
        return Setup.getDataPath();
    }

    default CompletableFuture startConvert(DataManager dataManager, String root, String path, File output) {
        return Threading.getFuture(() -> convert(dataManager, root, path, output));
    }

    default CompletableFuture startConvert(DataManager dataManager, String root, String path, Component parent) {
        return Threading.getFuture(() -> convert(dataManager, root, path, parent));
    }
    
    default CompletableFuture startConvert(File file, File output) {
        return Threading.getFuture(() -> convert(file, output));
    }
    
    default CompletableFuture startConvert(File file, Component parent) {
        return Threading.getFuture(() -> convert(file, parent));
    }


    static ArrayList<Class> dynamicProviders = new ArrayList<Class>() {
        /*{   //Defaut conveters
            add(MATConverter.class);
            add(MAT2DConverter.class);
            add(MAT2DZigZagConverter.class);
        }*/
    };

    public static <T extends Converter> void addServiceProvider(Class<T> cls) {
        for (Class c : dynamicProviders.toArray(new Class[0])) { //toArray to avoid ConcurrentModificationEx
            if (c.getName().equals(cls.getName())) {
                dynamicProviders.remove(c);
            }
        }
        dynamicProviders.add(cls);
    }

    public static Iterable<Converter> getServiceProviders() {
        ArrayList<Converter> ret = new ArrayList<>();
        for (Class c : dynamicProviders) {
            try {
                ret.add((Converter) c.newInstance());
            } catch (Exception ex) {
                Logger.getLogger(Converter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try{
            for (Converter importer : ServiceLoader.load(Converter.class, Sys.getClassLoader())) {
                ret.add(importer);
            }
        } catch (Throwable ex) {
            Logger.getLogger(Converter.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return ret;

    }
}
