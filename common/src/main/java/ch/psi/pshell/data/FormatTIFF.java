package ch.psi.pshell.data;

import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Format implementation storing 2D data as TIFF file (txt otherwise).
 * Write can be speed up paralellizing seting dataset feature {"parallel":True}.
 * Default value for parallelization is False, can be cahnged with setParallelWriting.
 * Parallelization can break appending logic (index determination) and read back.
 */
public class FormatTIFF extends FormatText {
    public static final String IMAGE_FILE_TYPE = "tiff";
    static String IMAGE_LIST_SUFFIX = "_%04d." + IMAGE_FILE_TYPE;
    public static int SINGLE_FILE = -2;
    static String FILE_SUFFIX = "." + IMAGE_FILE_TYPE;
    static boolean PARALLEL_WRITING = false;    
    

    @Override
    public String getId() {
        return "tiff";
    }
    
    public static void setParallelWriting(boolean value) {
        PARALLEL_WRITING = value;
    }

    public static boolean getParallelWriting() {
        return PARALLEL_WRITING;
    }
    
    public static void setImageListSuffix(String value) {
        IMAGE_LIST_SUFFIX = value;
    }

    public static String getImageListSuffix() {
        return IMAGE_LIST_SUFFIX;
    }    
    
    static Map metadata;
    public static void setMetadata(Map value){
        metadata = value;
    }

    public static Map getMetadata(){
        return metadata;
    }
   
    Map<String,Map> datasets = new HashMap<String,Map>();
    @Override
    public void openOutput(File root) throws IOException {
        super.openOutput(root);
        datasets.clear();
    }
    
    @Override
    public Map<String, Object> getInfo(String root, String path) throws IOException {        
        HashMap<String, Object> ret = new HashMap<>();
        Path filePath = getFilePath(root, path, false);        
        String dataset = filePath.toFile().getName();
        File first =  new File(filePath.toString() + String.format(IMAGE_LIST_SUFFIX, 0));
        File single =  new File(filePath.toString() + FILE_SUFFIX); 
        if (first.exists()) {         
            Object array = Tiff.load(first.toString());
            
            File folder = new File(first.getParent());        
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (name.startsWith(dataset) && name.endsWith(IMAGE_FILE_TYPE)){
                        for (int i = dataset.length()+1; i < dataset.length()+5; i++) {
                            if (!Character.isDigit(name.charAt(i))) {                      
                                return false;
                            }  
                        }
                        return true;
                    }
                    return false;
                }
            };        
            int numImages = folder.listFiles(filter).length;
            int[] shape = Arr.getShape(array);
            ret.put(INFO_TYPE, INFO_VAL_TYPE_DATASET);
            ret.put(INFO_DIMENSIONS, new int[] {numImages, shape[0], shape[1]});
            ret.put(INFO_RANK, 3);
            addClassInfo(Arr.getComponentType(array), ret);
            return ret;
        } else if (single.exists()) {    
            int numImages = Tiff.getStackSize(single.toString());
            Object array = Tiff.load(single.toString());            
            int[] shape = Arr.getShape(array);            
            ret.put(INFO_TYPE, INFO_VAL_TYPE_DATASET);
            if (numImages<0){
                ret.put(INFO_DIMENSIONS, shape);
                ret.put(INFO_RANK, 2);
            } else {
                ret.put(INFO_DIMENSIONS, new int[] {numImages, shape[0], shape[1]});
                ret.put(INFO_RANK, 3);
            }            
            addClassInfo(Arr.getComponentType(array), ret);
            return ret;            
        } else {
            return super.getInfo(root, path);
        }
    }

    @Override
    public DataSlice getData(String root, String path, int index) throws IOException {
        Path filePath = getFilePath(root, path, false);        
        File first =  new File(filePath.toString() + String.format(IMAGE_LIST_SUFFIX, 0));        
        File single =  new File(filePath.toString() + FormatTIFF.FILE_SUFFIX);        
        if (first.exists()) {  
            File file =  new File(filePath.toString() + String.format(IMAGE_LIST_SUFFIX, index));  
            Object array = Tiff.load(file.toString());            
            //int[] shape = Arr.getShape(array);
            Map<String, Object>  info = getInfo(root, path);
            int[] shape = (int[]) info.get(INFO_DIMENSIONS);
            return new DataSlice(root, path,  new int[] {shape[0], shape[1], shape[2]}, array, index, false);            
        } else if (single.exists()) {        
            Object array = Tiff.load(single.toString(), index);                        
            Map<String, Object>  info = getInfo(root, path);
            int[] shape = (int[]) info.get(INFO_DIMENSIONS);
            return new DataSlice(root, path,  shape, array, index, false);            
        } else {
            return super.getData(root, path, index);
        }
    }   

    @Override
    public void createDataset(String path, Class type, int[] dimensions, boolean unsigned, Map features) throws IOException {
        datasets.put(path, features);
        if (dimensions.length <=2){
            super.createDataset(path, type, dimensions, true, features);
        } else if (dimensions.length == 3){
        } else if (dimensions.length == 4){
        } else {
            throw new UnsupportedOperationException("Only support up to 4 dimensional datasets");
        }
    }

    @Override
    public void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        datasets.put(path, features);
        for (int i=0; i< names.length; i++){
            int rank = Arr.getRank(types[i]);
            if ((rank==2) || (rank==3)){
                //Will be saved as String (filename)
                lengths[i]=0;
                types[i]=String.class;
            }
        }
        super.createDataset(path, names, types, lengths, features);
    }
    
    @Override
    public void setDataset(String path, Object data, Class type, int rank, int[] dimensions, boolean unsigned, Map features) throws IOException {
        datasets.put(path, features);            
        if ((rank == 2) || (rank==3)) {                        
            setItem(path, data, type, SINGLE_FILE);
        } else {
            super.setDataset(path, data, type, rank, dimensions, unsigned, features);
        }
    }
    
    public boolean isParallelWriting(String path){
        Map features = datasets.get(path);        
        if (features != null){
            Object parallel =  features.get("parallel");
            if (parallel != null){
                if (Str.toString(parallel).equalsIgnoreCase(Boolean.TRUE.toString())){
                    return true;
                }
            }
        }
        return PARALLEL_WRITING;
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
                    String filename = prefix.toString() + "/" + name + String.format(IMAGE_LIST_SUFFIX, index);
                    Tiff.save(value, filename, isParallelWriting(path), getMetadata());                    
                    Array.set(data, i, new File(filename).getName());
                }
            }
            super.setItem(path, data, type, index);            
        } else if (rank < 2) {
            super.setItem(path, data, type, index);                        
        } else if (rank == 2) {
            Path prefix = getFilePath(path, false);
            if (index==-1){
                //Appending
                index = nextTiffIndex(prefix);        
            }            
            String suffix = (index == SINGLE_FILE) ? FILE_SUFFIX : String.format(IMAGE_LIST_SUFFIX, index);
            String filename = prefix.toString() + suffix;
            Tiff.save(data, filename, isParallelWriting(path), getMetadata());
        } else if (rank == 3) {
            Path prefix = getFilePath(path, false);
            String filename = prefix.toString() + FILE_SUFFIX;
            Tiff.saveStack(data, filename, isParallelWriting(path), getMetadata() );
        } else {            
            throw new IllegalArgumentException("Cannot set data in CSV format with rank: " + rank);
        }
    }
    
    public static int nextTiffIndex(Path prefix) throws IOException {
        Path dir = prefix.getParent();
        String baseName = prefix.getFileName().toString();
        // regex: basename_XXXX.tiff
        Pattern pattern = Pattern.compile(Pattern.quote(baseName) + "_(\\d+)\\." + IMAGE_FILE_TYPE);
        int maxIndex = -1;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                Matcher m = pattern.matcher(f.getFileName().toString());
                if (m.matches()) {
                    int idx = Integer.parseInt(m.group(1));
                    if (idx > maxIndex) {
                        maxIndex = idx;
                    }
                }
            }
        }
        return maxIndex + 1;
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
                } else if (ext.equals(IMAGE_FILE_TYPE)) {
                    if (content.contains("_")){
                        String child = path + content.substring(0, content.lastIndexOf("_"));
                        if (!ret.contains(child)){
                            ret.add(child);
                        }
                    } else {
                        ret.add(path + IO.getPrefix(content));
                    }
                    
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
                    } else {
                        contents.add(IO.getPrefix(dataset));
                    }
                }
            }
        }
        contents.addAll(folders);
        return contents.toArray();        
    }
    

}
