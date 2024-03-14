package ch.psi.pshell.data;

import ch.psi.utils.Arr;
import ch.psi.utils.IO;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider implementation storing 2D data as TIFF file (txt otherwise)
 */
public class ProviderTIFF extends ProviderText {
    public static final String IMAGE_FILE_TYPE = "tiff";
    static String IMAGE_FILE_SUFIX = "_%04d." + IMAGE_FILE_TYPE;
    static boolean PARALLEL_WRITING = true;
    

    public static void setParallelWriting(boolean value) {
        PARALLEL_WRITING = value;
    }

    public static boolean getParallelWriting() {
        return PARALLEL_WRITING;
    }
    
    public static void setImageFileSuffix(String value) {
        IMAGE_FILE_SUFIX = value;
    }

    public static String getImageFileSuffix() {
        return IMAGE_FILE_SUFIX;
    }    
    
    public ProviderTIFF() {
        super.setEmbeddedAtributes(false);
    }
    
    static Map metadata;
    public static void setMetadata(Map value){
        metadata = value;
    }

    public static Map getMetadata(){
        return metadata;
    }
    
            
    @Override
    public void setEmbeddedAtributes(boolean value) {
        if (value) {
            throw new IllegalArgumentException("Cannot set embedded attributes in TIFF format");
        }
    }
    
    @Override
    public Map<String, Object> getInfo(String root, String path) throws IOException {        
        HashMap<String, Object> ret = new HashMap<>();
        Path filePath = getFilePath(root, path, false);        
        String dataset = filePath.toFile().getName();
        File file =  new File(filePath.toString() + String.format(IMAGE_FILE_SUFIX, 0));
        if (file.exists()) {         
            Object array = Tiff.load(file.toString());
            
            File folder = new File(file.getParent());        
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(dataset) && name.endsWith(IMAGE_FILE_TYPE);
                }
            };        
            int numImages = folder.listFiles(filter).length;
            int[] shape = Arr.getShape(array);
            ret.put(INFO_TYPE, INFO_VAL_TYPE_DATASET);
            ret.put(INFO_DIMENSIONS, new int[] {numImages, shape[0], shape[1]});
            ret.put(INFO_RANK, 3);
            addClassInfo(Arr.getComponentType(array), ret);
            return ret;
        } else {
            return super.getInfo(root, path);
        }
    }

    @Override
    public DataSlice getData(String root, String path, int index) throws IOException {
        HashMap<String, Object> ret = new HashMap<>();
        Path filePath = getFilePath(root, path, false);        
        File file =  new File(filePath.toString() + String.format(IMAGE_FILE_SUFIX, index));
        if (file.exists()) {         
            Object array = Tiff.load(file.toString());            
            int[] shape = Arr.getShape(array);
            return new DataSlice(root, path,  new int[] {3, shape[0], shape[1]}, array, index, false);            
        } else {
            return super.getData(root, path, index);
        }
    }

    @Override
    public void createDataset(String path, Class type, int[] dimensions, boolean unsigned, Map features) throws IOException {
        if (dimensions.length <=2){
            super.createDataset(path, type, dimensions, true, features);
        } else if (dimensions.length == 3){
        } else {
            throw new UnsupportedOperationException("Only support up to 3 dimensional datasets");
        }
    }

    @Override
    public void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        for (int i=0; i< names.length; i++){
            if (Arr.getRank(types[i])==2){
                //Will be saved as String (filename)
                lengths[i]=0;
                types[i]=String.class;
            }
        }
        super.createDataset(path, names, types, lengths, features);
    }

    @Override
    public void setItem(String path, Object data, Class type, int index) throws IOException {
        int[] shape = Arr.getShape(data);
        int rank = shape.length;
        
        //Composite
        if (rank==1 && type==Object[].class){
            for (int i=0; i< shape[0]; i++){
                Object value = Array.get(data, i);
                if (Arr.getRank(value) == 2){
                    Map<String, Object> info = getInfo(this.root, path);
                    String[] names = (String[]) info.get(INFO_FIELD_NAMES);
                    String name = names[i];
                    Path prefix = getFilePath(path, false).getParent();
                    String filename = prefix.toString() + "/" + name + String.format(IMAGE_FILE_SUFIX, index);
                    Tiff.save(value, filename, PARALLEL_WRITING, getMetadata());                    
                    Array.set(data, i, new File(filename).getName());
                }
            }
            super.setItem(path, data, type, index);            
        } else if (rank < 2) {
            super.setItem(path, data, type, index);                        
        } else if (rank == 2) {
            Path prefix = getFilePath(path, false);
            String filename = prefix.toString() + String.format(IMAGE_FILE_SUFIX, index);
            Tiff.save(data, filename, PARALLEL_WRITING, getMetadata());
        } else {            
            throw new IllegalArgumentException("Cannot set data in CSV format with rank: " + rank);
        }
    }
    
    
    @Override
    public String[] getChildren(String root, String path) throws IOException {
        ArrayList<String> ret = new ArrayList<>(Arrays.asList(super.getChildren(root, path)));                
        
        Path filePath = getFilePath(root, path);
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (filePath.toFile().isDirectory()) {
            for (String content : filePath.toFile().list()) {                                
                String ext = IO.getExtension(content);
                if (Paths.get(filePath.toString(), content).toFile().isDirectory()){                
                } else  if (ext.equals(IMAGE_FILE_TYPE) && content.contains("_")) {
                    ret.add(path + content.substring(0, content.lastIndexOf("_")));
                    break;
                }
            }
        }
        return ret.toArray(new String[0]);
    }    
    
    
    @Override
    public Object[] getStructure(String root) throws IOException {
        String format = getFileType();
        List contents = new ArrayList();
        List folders = new ArrayList();
        File file = Paths.get(root).toFile();
        contents.add(file.getName());
        File[] files = IO.listFiles(file);
        IO.orderByModified(files);
        for (File content : files) {
            if (content.isDirectory()) {
                folders.add(getStructure(content.getPath()));
            } else {
                String ext = IO.getExtension(content);
                if (ext.equals(format)) {
                    contents.add(IO.getPrefix(content));
                } else  if (ext.equals(IMAGE_FILE_TYPE)){
                     String dataset = content.getName();
                     if (dataset.contains("_")){
                         dataset = dataset.substring(0, dataset.lastIndexOf("_"));
                         if (!contents.contains(dataset)){
                             contents.add(dataset);
                         }                        
                     }
                }
            }
        }
        contents.addAll(folders);
        return contents.toArray();        
    }
    

}
