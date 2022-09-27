package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.utils.IO.FilePermissions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Provider implementations execute the actual reading/writing of data in a hierarchical storage.
 */
public interface Provider {

    public static final String INFO_TYPE = "Type";
    public static final String INFO_CREATION = "Creation";
    public static final String INFO_DATA_TYPE = "Data Type";
    public static final String INFO_CLASS = "Class";
    public static final String INFO_DIMENSIONS = "Dimensions";
    public static final String INFO_RANK = "Rank";
    public static final String INFO_ELEMENTS = "Elements";
    public static final String INFO_ELEMENT_SIZE = "Element Size";
    public static final String INFO_LAYOUT = "Layout";
    public static final String INFO_SIZE = "Size";
    public static final String INFO_SIGNED= "Signed";
    public static final String INFO_CHUNK_SIZES = "Chunk Sizes";
    public static final String INFO_FIELDS = "Fields";
    public static final String INFO_FIELD_NAMES = "Field Names";
    public static final String INFO_FIELD_TYPES = "Field Types";
    public static final String INFO_FIELD_LENGTHS = "Field Lengths";
    public static final String INFO_LINK_ROOT = "Link Root";
    public static final String INFO_LINK_PATH = "Link Path";

    public static final String INFO_VAL_TYPE_GROUP = "GROUP";
    public static final String INFO_VAL_TYPE_DATASET = "DATASET";
    public static final String INFO_VAL_TYPE_UNDEFINED = "NONEXISTENT";
    public static final String INFO_VAL_TYPE_SOFTLINK = "SOFT_LINK";

    public static final String INFO_VAL_DATA_TYPE_STRING = "STRING";
    public static final String INFO_VAL_DATA_TYPE_FLOAT = "FLOAT";
    public static final String INFO_VAL_DATA_TYPE_INTEGER = "INTEGER";
    public static final String INFO_VAL_DATA_TYPE_COMPOUND = "COMPOUND";
    public static final String INFO_VAL_DATA_TYPE_BOOLEAN = "BOOLEAN";
    public static final String INFO_VAL_DATA_TYPE_BITFIELD = "BITFIELD";

    public static final String[] COMPOUND_DATA_INFO = new String[]{INFO_FIELDS, INFO_FIELD_NAMES, INFO_FIELD_TYPES, INFO_FIELD_LENGTHS};

    /**
     * Null for folder organization
     */
    String getFileType();

    /**
     * @return If is single-file
     */
    boolean isPacked();

    void openOutput(File root) throws IOException;

    void closeOutput() throws IOException;

    void flush();

    Object[] getStructure(String root) throws IOException;

    Map<String, Object> getInfo(String root, String path) throws IOException;

    Map<String, Object> getAttributes(String root, String path) throws IOException;

    DataSlice getData(String root, String path, int index) throws IOException;

    boolean isDataset(String root, String path) throws IOException;

    boolean isGroup(String root, String path) throws IOException;
    
    default boolean isLink(String root, String path) throws IOException{
        return false;
    }
    
    String[] getChildren(String root, String path) throws IOException;

    void createGroup(String path) throws IOException;

    void setAttribute(String path, String name, Object value, Class type, boolean unsigned) throws IOException;

    void setDataset(String path, Object data, Class type, int rank, int[] dimensions, boolean unsigned, Map features) throws IOException;

    void createDataset(String path, Class type, int[] dimensions, boolean unsigned, Map features) throws IOException;

    /**
     * A composite dataset (heterogeneous table)
     */
    void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException;

    void setItem(String path, Object data, Class type, int index) throws IOException;
    
    /**
     * Multidimensional array writing
     */
    default void setItem(String path, Object data, Class type, long[] index, int[] shape) throws IOException{
       throw new UnsupportedOperationException();
    }
    
    default DataSlice getData(String root, String path,  long[] index, int[]shape) throws IOException {        
        throw new UnsupportedOperationException();
    }

    default String getRootFileName(String root) {
        if (isPacked()) {
            String ext = "." + getFileType();
            if (!root.endsWith(ext)) {
                root += ext;
            }
        }
        return root;
    }            

    default int getDepthDimension(){
        if (!Context.isDataManagerInstantiated()){
            return 0;
        }
        return Context.getInstance().getDataManager().getDepthDimension();
    }

    default FilePermissions getFilePermissions(){
        return Context.getInstance().getDataManager().filePermissions;
    }

    default public void checkLogFile(String logFile) throws IOException{
    }    
    
    /**
    * Returns the actual file path for the relative location. 
    * The defualt implementation returns the output file (valid for packed providers as hdf5).  
    */
    default public Path getFilePath(String path) {        
        Context.assertInstantiated();
        return Paths.get(Context.getInstance().getDataManager().getRootFileName());
    }

    /**
     * Returns the actual file path for the relative location.
     * The defualt implementation returns the File path (true for packed data and for embedded attributes).
     */
    default public Path getAttributePath(String path) {
        return getFilePath(path);
    }


    
    default public boolean getEmbeddedAtributes() {
        if (!Context.isDataManagerInstantiated()){
            return true;                
        }
        return Context.getInstance().getDataManager().getEmbeddedAttributes();
    }

        
}
