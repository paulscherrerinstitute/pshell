package ch.psi.pshell.data;

import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * Format implementation storing 2D data as TIFF file (txt otherwise).
 * Write can be speed up paralellizing seting dataset feature {"parallel":True}.
 * Default value for parallelization is False, can be changed with setParallelWriting.
 * Parallelization can break appending logic (index determination) and read back.
 * In scans, file lists can be transformed in a TIFF stack with the feature: stack=True.
 */
public class FormatTIFF extends FormatText {
    public static final String IMAGE_FILE_TYPE = "tiff";
    public static final int SINGLE_FILE = -2;    
    static String FILE_SUFFIX = "." + IMAGE_FILE_TYPE;
    static boolean PARALLEL_WRITING = false; 
    static String IMAGE_LIST_SUFFIX = "_%04d." + IMAGE_FILE_TYPE;    
    static Pattern IMAGE_LIST_PATTERN = Pattern.compile("^(.*)_(\\d+)\\.tiff$");
    
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
    
    public static void setImageListPattern(String regex) {
        IMAGE_LIST_PATTERN = Pattern.compile(regex);
    }

    public static Pattern getImageListPattern() {
        return IMAGE_LIST_PATTERN;
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
    public void closeOutput() throws IOException {
        if (root != null){
            for (String dataset: datasets.keySet()){
                try{
                    if (isStack(dataset)){
                        stack(dataset);
                    }
                }catch (Exception ex){     
                     Logger.getLogger(FormatTIFF.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        super.closeOutput();
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
            addClassInfo(Arr.getComponentType(array), false, ret);
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
            addClassInfo(Arr.getComponentType(array), false, ret);
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

    public boolean isStack(String path){
        Map features = datasets.get(path);        
        if (features != null){
            Object stack =  features.get("stack");
            if (stack != null){
                if (Str.toString(stack).equalsIgnoreCase(Boolean.TRUE.toString())){
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public void setItem(String path, Object data, Class type, int index) throws IOException {
        int[] shape = Arr.getShape(data);
        int rank = shape.length;
        
        //Composite
        if (rank==1 && type==Object[].class){
            Map<String, Object> info = getInfo(this.root, path);
            String[] names = (String[]) info.get(INFO_FIELD_NAMES);
            for (int i=0; i< shape[0]; i++){
                Object value = Array.get(data, i);
                if (Arr.getRank(value) == 2){
                    Path prefix = getFilePath(path, false);
                    String filename = prefix.toString() + "_" + names[i] + String.format(IMAGE_LIST_SUFFIX, index);
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
    
            
    public static Set<String> getImageListPrefixes(Path dir) throws IOException {        
        Set<String> prefixes = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.tiff")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                Matcher m = getImageListPattern().matcher(name);
                if (m.matches()) {
                    prefixes.add(m.group(1)); // prefix before "_<NUMERIC_INDEX>"
                }
            }
        }
        return prefixes;
    }
    
    private static class FileEntry {
        int index;
        String name;
        FileEntry(int index, String name) {
            this.index = index;
            this.name = name;
        }
    }    

    public static List<String> getFilesForPrefix(Path dir, String prefix) throws IOException {
        List<FileEntry> matches = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, prefix + "_*.tiff")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                Matcher m = getImageListPattern().matcher(name);
                if (m.matches() && m.group(1).equals(prefix)) {
                    int index = Integer.parseInt(m.group(2));
                    matches.add(new FileEntry(index, name));
                }
            }
        }
        // sort by numeric index
        matches.sort(Comparator.comparingInt(f -> f.index));
        // return just filenames
        List<String> result = new ArrayList<>();
        for (FileEntry f : matches) {
            result.add(f.name);
        }
        return result;
    }
    
    public void stack(String path) throws IOException{
        Path dir = getFilePath(path, false).getParent();
        Set<String> prefixes = getImageListPrefixes(dir);
        for (var prefix:prefixes){
            List<String> files = getFilesForPrefix(dir, prefix);
            List data = new ArrayList();
            for (var file:files){
                data.add(Tiff.load(Paths.get(dir.toString(),file ).toString()));
            }            
            if (!data.isEmpty()) {
                Logger.getLogger(FormatTIFF.class.getName()).info("Creating stack of: " + prefix);
                int depth = data.size();
                int height = Array.getLength(data.get(0));
                int width = Array.getLength(Array.get(data.get(0), 0));
                Class type = Arr.getComponentType(data.get(0));
                Object stack = Array.newInstance(type, depth, height, width);
                for (int z = 0; z < depth; z++) {
                    Array.set(stack, z, data.get(z));
                }
                String stackFile = Paths.get(dir.toString(), prefix + ".tiff").toString();
                Tiff.saveStack(stack, stackFile);
                for (var file:files){
                    Files.delete(Paths.get(dir.toString(), file ));
                }            
                Logger.getLogger(FormatTIFF.class.getName()).info("Success creating stack: " + stackFile);
            }
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
                    Matcher matcher = getImageListPattern().matcher(dataset);
                    if  (matcher.matches()){
                        String prefix = matcher.group(1);
                        if (!contents.contains(prefix)){
                            contents.add(prefix);
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
