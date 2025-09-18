package ch.psi.pshell.data;

import ch.psi.pshell.logging.Logging;
import ch.psi.pshell.extension.Extensions;
import ch.psi.pshell.scripting.JepUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jep.NDArray;

/**
 * Implements format class instantiation and common data handling logic.
 */
public class DataStore implements AutoCloseable {
    //final Context context;
    static final Logger logger = Logger.getLogger(DataStore.class.getName());
    //Layout layout;
    protected Format format;
    protected File outputFile;
    protected FilePermissions filePermissions = FilePermissions.Default;
    protected DirectoryStream.Filter fileFilter;
    static DataStore defaultDataStore;
    static DataStore global;
    
    public DataStore(){        
    }
    
    public void setGlobal(){
        global = this;
    }
    
    public static DataStore getGlobal(){
        if (global == null) {
            defaultDataStore = new DataStore(new FormatHDF5());
            global = defaultDataStore;
        }
        return global;
    }
    
    public static boolean isDefault(){
        return  (global==null) || (global.equals(defaultDataStore));
    }
    
    public static String[] getLayoutIds(){
         return  new String[] {"default", "table"};
    }
    
    public static String[] getFormatIds(){
         return  new String[] {"h5", "txt","csv", "tiff"};
    }
    

    public DataStore(String format) throws Exception {
        this();
        DataStore.this.setFormat(format);     
    }

    public DataStore(Format format) {
        this();
        DataStore.this.setFormat(format);     
    }

    public DataStore(File outputFile, String format) throws Exception {
        this(format);
        setOutputFile(outputFile);
    }
    
    public DataStore(String outputFile, String format) throws Exception {
        this(new File(outputFile), format);
    }

    public DataStore(File outputFile, Format format) throws Exception {
        this(format);
        setOutputFile(outputFile);
    }
    
    public DataStore(String outputFile, Format format) throws Exception {
        this(new File(outputFile), format);
    }
    
    
    public void setOutputFile(File file) throws IOException{
        if (outputFile!=null){
            doCloseOutput();
        }
        outputFile = file;
        outputFile.mkdirs();
        doOpenOutput();        
    }

    public void setFilePermissions(FilePermissions filePermissions) {
        this.filePermissions = (filePermissions == null) ? FilePermissions.Default : filePermissions;
    }
    
    final protected static Map<String, Class> formatClasses = new HashMap();
    static{
        formatClasses.put("", FormatHDF5.class);
        formatClasses.put("default", FormatHDF5.class);
        formatClasses.put("h5", FormatHDF5.class);
        formatClasses.put("hdf5", FormatHDF5.class);
        formatClasses.put("txt", FormatText.class);
        formatClasses.put("csv", FormatCSV.class);
        formatClasses.put("tiff", FormatTIFF.class);
    }
        
    public static Class getFormatClass(String name) throws ClassNotFoundException {
        Class formatClass = null;
        String providerName = (name == null) ? null : name.toLowerCase();
        if ((providerName == null) || providerName.isBlank()){
            providerName = "";
        }
        formatClass = formatClasses.get(name);
        if (formatClass==null){
            formatClass = Extensions.getClass(name);            
        }                
        return formatClass;
    }

    public void setFormat(String name) throws Exception {
        Class providerClass = getFormatClass(name);
        if ((format != null)) {
            if (format.getClass() == providerClass) {
                return;
            }
        }
        DataStore.this.setFormat((Format) providerClass.newInstance());
    }

    public void setFormat(Format format) {
        logger.log(Level.FINE, "Setting data format: {0}", format.getClass().getName());
        this.format = format;
        fileFilter = null;
    }

    public Format cloneFormat() {
        if (format != null) {
            try {
                return format.getClass().newInstance();
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public Format getFormat() {
        return format;
    }
    
    public static boolean isSameFormat(Object o1, Object o2){
        if ((o1!=null) && (o2!=null)){
            try{
                Class c1 =  (o1 instanceof String s1) ? getFormatClass(s1) : o1.getClass();
                Class c2 =  (o2 instanceof String s2) ? getFormatClass(s2) : o2.getClass();
                if ((c1==null) || (c2==null)){
                    return false;
                }
                return c1==c2;
            } catch (Exception ex){                
            }
        }
        return false;
    }
           
    public String getDataFolder() {
        return outputFile.getParent();
    }
    
    boolean open = false;
    
    protected void doOpenOutput() throws IOException {    
        if (isOpen()){
            return;
        }
        if (getFormat()!=null){            
            getFormat().openOutput(outputFile);
            open = true;
        }
    }
    
    public void openOutput() throws IOException {        
        doOpenOutput();      
    }
    
    protected void doCloseOutput() {        
        open = false;
        if (getFormat()!=null){         
            if (outputFile!=null) { 
                try {
                    getFormat().closeOutput();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                } 
            }        
        }
    }
    
    public void closeOutput() {        
        doCloseOutput();      
    }    
    
    public boolean isOpen() {
        return open;
    }    
    
    public boolean isRoot(String path) {
        return new File(path).equals(path);
    }
    

    boolean createLogs = true;

    public boolean getCreateLogs() {
        return createLogs;
    }

    public void setCreateLogs(boolean value) {
        createLogs = value;
    }
    
    public String getDataFileType() {
        return getFormat().getFileType();
    }

    public boolean isDataPacked() {
        return getFormat().isPacked();
    }
        
    public boolean getPreserveTypes() {
        return true;
    }

    public Map getStorageFeatures(Nameable device) {
        return null;
    }

    public boolean isStorageFeaturesContiguous(Map features) {
        return (features != null) && ("contiguous".equals(features.get("layout")));
    }

    public boolean isStorageFeaturesCompressed(Map features) {
        return (features != null) && (features.get("compression") != null)
                && (!"false".equalsIgnoreCase(String.valueOf(features.get("compression"))));
    }
    
    public static Map createCompressionFeatures(Object obj, boolean shuffle){
        int[] shape =  Arr.getShape(obj);
        return createCompressionFeatures(shape, shuffle);
    }

    public static Map createCompressionFeatures(int[] shape, boolean shuffle){
        int[] chunks = new int[shape.length+1];
        System.arraycopy(shape, 0, chunks, 1, shape.length);
        Map features = new HashMap();
        features.put("compression", true);
        chunks[0] = 8 * 1024;
        if (shape.length==1){
            chunks[0] = 128; //16 * 1024;
        } else if (shape.length>1){
            chunks[0] = 1; //32 * 1024;
        }        
        features.put("shuffle", shuffle);    
        features.put("chunk", chunks);    
        return features;
    }
    

    static int defaultDepthDimension = DataSlice.DEFAULT_DEPTH_DIMENSION;
    public static void setDefaultDepthDimension(int depthDimension)  {
        defaultDepthDimension = depthDimension;
    }
    
    public static int getDefaultDepthDimension()  {
        return defaultDepthDimension;
    }

    
    public int getDepthDimension()  {
        return defaultDepthDimension;
    }
    
    public DirectoryStream.Filter getFileFilter() {
         return getFileFilter(new String[0]);
    }
    
    public DirectoryStream.Filter getFileFilter(final String[] additionalExtensions) {
        return getFileFilter(additionalExtensions, false);
    }
    
    public DirectoryStream.Filter getFileFilter(final String[] additionalExtensions, boolean hidden) {
        if (fileFilter == null) {
            fileFilter = (DirectoryStream.Filter<Path>) (Path path) -> {
                File file = path.toFile();
                if (!file.isHidden() || hidden){
                    if (file.isDirectory()) {
                        if (!isDataPacked()) {
                            if (isRoot(file.getParent())) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        String ext = IO.getExtension(file);
                        if (isDataPacked()) {
                            if (getDataFileType() != null) {
                                if (ext.equals(getDataFileType())) {
                                    return true;
                                }
                            }
                        }
                        if (Arr.containsEqual(additionalExtensions, ext)){
                            return true;
                        }
                    }
                }
                return false;
            };
        }
        return fileFilter;
    }
    
    public String getRootFileName() {
       return outputFile.getPath();
    }
    
    public String getRootFileName(String root) {
        return getFormat().getRootFileName(root);
    }
    

    public void flush() {
        getFormat().flush();
    }

    public String getOutput() {
        return outputFile.getPath();
    }    

    public static class DataOutputClosed extends IllegalStateException{
        DataOutputClosed(){
            super("Data output not instantiated");
        }
    }
    public void assertOpen() {
        if (!isOpen()) {
            throw new DataOutputClosed();
        }
    }

    public Object[] getStructure(String root) throws IOException {
        root = adjustRoot(root);
        Object[] ret = getFormat().getStructure(root);

        if ((ret != null) && (ret.length > 0) && (ret[0].equals(""))) {
            ret[0] = new File(root).getName();
        }
        return ret;
    }

    public String[] getChildren(String path) throws IOException {
        path=adjustPath(path);
        DataAddress address = getAddress(path);
        if (address != null) {
            return getChildren(address.root, address.path);
        }
        return getChildren(getOutput(), path);
    }

    public String[] getChildren(String root, String path) throws IOException {
        root = adjustRoot(root);
        path=adjustPath(path);
        try {
            if (isGroup(root, path)) {
                return getFormat().getChildren(root, path);
            }
        } catch (Exception ex) {
        }
        return new String[0];
    }

    public boolean isDataset(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return isDataset(address.root, address.path);
        }
        return isDataset(getOutput(), path);
    }

    public boolean isDataset(String root, String path) throws IOException {
        root = adjustRoot(root);
        path=adjustPath(path);
        return getFormat().isDataset(root, path);
    }

    public boolean isGroup(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return isGroup(address.root, address.path);
        }
        return isGroup(getOutput(), path);
    }

    public boolean isGroup(String root, String path) throws IOException {        
        root = adjustRoot(root);
        path=adjustPath(path);
        return getFormat().isGroup(root, path);
    }
    
    public boolean isExtLink(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return isExtLink(address.root, address.path);
        }
        return isExtLink(getOutput(), path);
    }
    
    public boolean isExtLink(String root, String path) throws IOException {        
        root = adjustRoot(root);
        path=adjustPath(path);
        return getFormat().isExtLink(root, path);
    }    
    
    public boolean isSoftLink(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return isSoftLink(address.root, address.path);
        }
        return isSoftLink(getOutput(), path);
    }
    

    public boolean isSoftLink(String root, String path) throws IOException {        
        root = adjustRoot(root);
        path=adjustPath(path);
        return getFormat().isSoftLink(root, path);
    }    


    public static DataAddress getAddress(String path) {
        if (path!=null){
            String[] rootDelimitors = new String[]{"|", " /"};
            for (String delimitor : rootDelimitors) {
                if (path.contains(delimitor)) {
                    int index = path.indexOf(delimitor);
                    return new DataAddress(path.substring(0, index).trim(), path.substring(index + delimitor.length()).trim());
                }
            }
        }
        return null;
    }

    public static String getFullPath(String root, String path) {
        return root + "|" + path;
    }

    public DataSlice getData(String path) throws IOException {
        return getData(path, 0);
    }

    /**
     * Shortcut for accessing 3d data in 2d arrays.
     */
    public DataSlice getData(String path, int index) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getData(address.root, address.path, index);
        }
        return getData(getOutput(), path, index);
    }

    public DataSlice getData(String root, String path) throws IOException {        
        return getData(root, path, 0);
    }

    /**
     * Shortcut for accessing 3d data in 2d arrays.
     */
    public DataSlice getData(String root, String path, int index) throws IOException {
        root = adjustRoot(root);
        path=adjustPath(path);

        DataSlice ret = getFormat().getData(root, path, index);
        if (ret == null) {
            throw new UnsupportedOperationException(String.format("Invalid data path : %s-%s-%d", root, path, index));
        }

        if (ret.sliceData == null) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return ret;
    }

    public Object getData(String path, long[] index, int[] shape) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getData(address.root, address.path, index, shape);
        }
        return getData(getOutput(), path, index, shape);
    }

    public DataSlice getData(String root, String path, long[] index, int[] shape) throws IOException {
        root = adjustRoot(root);
        path=adjustPath(path);

        DataSlice ret = getFormat().getData(root, path, index, shape);
        if (ret == null) {
            throw new UnsupportedOperationException(String.format("Invalid data path : %s-%s-%s", root, path, Convert.arrayToString(index, ".")));

        }
        if (ret.sliceData == null) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return ret;
    }

    protected String adjustRoot(String root) {
        if (!new File(root).exists()) {
            String base = getDataFolder();
            if (!IO.isSubPath(root, base)) {
                root = Paths.get(base, root).toString();
            }
        }
        return root;
    }
    
    protected String adjustPath(String path) {
        if (path==null){
            //throw new IllegalArgumentException();
            path = "/";
        }
        if (!path.startsWith("/")){
            path = "/" + path;
        }
        return path;
    }
    
    protected void onCreateUnpackedDataset(File file){
        IO.setFilePermissions(file, filePermissions);
    }
    
    public Map<String, Object> getInfo(String path) {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getInfo(address.root, address.path);
        }
        return getInfo(getOutput(), path);
    }

    public Map<String, Object> getInfo(String root, String path) {
        root = adjustRoot(root);
        path=adjustPath(path);      

        Map<String, Object> ret;
        try {
            ret = getFormat().getInfo(root, path);
        } catch (Exception ex) {
            ret = new HashMap<>();
            ret.put("Exception", ex.getMessage());
        }
        return ret;
    }

    public Map<String, Object> getAttributes(String path) {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getAttributes(address.root, address.path);
        }
        return getAttributes(getOutput(), path);
    }

    public Map<String, Object> getAttributes(String root, String path) {
        root = adjustRoot(root);
        path=adjustPath(path);      

        Map<String, Object> ret;
        try {
            ret = getFormat().getAttributes(root, path);
        } catch (Exception ex) {
            ret = new HashMap<>();
            ret.put("Exception", ex.getMessage());
        }
        return ret;
    }

    public Object getAttribute(String path, String name) {
        Map<String, Object> attrs = getAttributes(path);
        return attrs.get(name);
    }

    public Object getAttribute(String root, String path, String name) {
        Map<String, Object> attrs = getAttributes(root, path);
        return attrs.get(name);
    }

    public boolean exists(String path) {
        path=adjustPath(path);
        DataAddress address = getAddress(path);
        if (address != null) {
            return exists(address.root, address.path);
        }
        return exists(getOutput(), path);
    }

    public boolean exists(String root, String path) {
        root = adjustRoot(root);
        path=adjustPath(path);      
        try {
            Map<String, Object> info = getFormat().getInfo(root, path);
            if ((info == null) || (String.valueOf(info.get(Format.INFO_TYPE)).equals(Format.INFO_VAL_TYPE_UNDEFINED))) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void createGroup(String path) throws IOException {
        synchronized (this) {
            openOutput();
            getFormat().createGroup(path);
            if (!getFormat().isPacked()){
                File file = getFormat().getFilePath(path).toFile();
                IO.setFilePermissions(file, filePermissions);
            }            
        }
    }

    // External link    
    public void createLink(String path, String targetRoot, String targetPath) throws IOException {
        synchronized (this) {
            openOutput();
            getFormat().createLink(adjustPath(path), targetRoot, adjustPath(targetPath));
        }
    }
    
    // Internal link    
    public void createLink(String path, String targetPath) throws IOException {
        synchronized (this) {
            openOutput();
            getFormat().createLink(adjustPath(path), adjustPath(targetPath));
        }
    }    
    
    public void setDataset(String path, Object data) throws IOException {
        setDataset(path, data, false);
    }

    public void setDataset(String path, Object data, boolean unsigned) throws IOException {
        setDataset(path, data, unsigned, null);
    }

    public void setDataset(String path, Object data, boolean unsigned, Map features) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        path=adjustPath(path);
        openOutput();

        int index = path.lastIndexOf("/");
        String group = path.substring(0, index + 1);
        String name = path.substring(index + 1);

        if (data instanceof NDArray ndarray){
            data = JepUtils.toJavaArray(ndarray);
        } 
        Class type = data.getClass().isArray() ? Arr.getComponentType(data) : data.getClass();
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }

        int[] shape = Arr.getShape(data);
        int rank = shape.length;

        logger.log(Level.FINER, "Set \"{0}\" type={1} dims={2}", new Object[]{path, type.getSimpleName(), Str.toString(shape, 10)});
        createGroup(group);              
        getFormat().setDataset(path, data, type, rank, shape, unsigned, features);
        if (!getFormat().isPacked()){
            File file = getFormat().getFilePath(path).toFile();
            onCreateUnpackedDataset(file);
        }
        flush();
    }
    
    public void createDataset(String path, Class type) throws IOException {
        createDataset(path, type, null);
    }

    public void createDataset(String path, Class type, int[] dimensions) throws IOException {
        createDataset(path, type, null, dimensions);
    }

    public void createDataset(String path, Class type, int[] dimensions, Map features) throws IOException {
        createDataset(path, type, null, dimensions, features);
    }

    public void createDataset(String path, Class type, Boolean unsigned, int[] dimensions) throws IOException {
        createDataset(path, type, unsigned, dimensions, null);
    }

    public void createDataset(String path, Class type, Boolean unsigned, int[] dimensions, Map features) throws IOException {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        path=adjustPath(path);        
        if (unsigned == null) {
            unsigned = ((type == Byte.class) || (type == Short.class)); //Default unsigned for byte and shortt
        }
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        if (dimensions == null) {
            dimensions = new int[]{0};
        }

        openOutput();
        String group = path.substring(0, path.lastIndexOf("/") + 1);
        createGroup(group);

        logger.log(Level.FINER, "Create \"{0}\" type={1} dims={2}", new Object[]{path, type.getSimpleName(), Str.toString(dimensions, 10)});
        getFormat().createDataset(path, type, dimensions, unsigned, features);
        if (!getFormat().isPacked()){
            File file = getFormat().getFilePath(path).toFile();
            onCreateUnpackedDataset(file);
        }
    }

    public void createDataset(String path, String[] names, Class[] types, int[] lengths) throws IOException {
        createDataset(path, names, types, lengths, null);
    }

    public void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        if (names == null) {
            throw new IllegalArgumentException();
        }
        path=adjustPath(path);
        openOutput();
        int fields = names.length;
        if (types == null) {
            types = new Class[fields];
        }
        if (lengths == null) {
            lengths = new int[fields];
        }

        if ((fields != types.length) || (fields != lengths.length)) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < fields; i++) {
            if (types[i] == null) {
                types[i] = String.class;
            } else if (types[i].isPrimitive()) {
                types[i] = Convert.getWrapperClass(types[i]);
            }
        }
        String group = path.substring(0, path.lastIndexOf("/") + 1);
        createGroup(group);

        logger.log(Level.FINER, "Create \"{0}\"", path);
        getFormat().createDataset(path, names, types, lengths, features);
        if (!getFormat().isPacked()){
            File file = getFormat().getFilePath(path).toFile();
            onCreateUnpackedDataset(file);
        }      
        flush();
    }
    
    //Overloaded methods are not seen by JEP so creating a proxy 
    public void createTable(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        createDataset(path, names, types, lengths, features);
    }

    public void createCompressedDataset(String path, Object element) throws IOException {
        createCompressedDataset(path, element, false);
    }

    public void createCompressedDataset(String path, Class type, int[] shape) throws IOException {
        createCompressedDataset(path, type, shape, false);
    }    
    
    public void createCompressedDataset(String path, Object element, boolean shuffle) throws IOException {
        Class type = Arr.getComponentType(element);
        int[] shape =  Arr.getShape(element);
        int[] dimensions = new int[shape.length+1];
        System.arraycopy(shape, 0, dimensions, 1, shape.length);        
        Map features = DataStore.createCompressionFeatures(element, shuffle);
        createDataset(path, type, dimensions, features);
    }

    public void createCompressedDataset(String path, Class type, int[] shape, boolean shuffle) throws IOException {
        int[] dimensions = new int[shape.length+1];
        System.arraycopy(shape, 0, dimensions, 0, shape.length);        
        Map features = DataStore.createCompressionFeatures(shape, shuffle);
        createDataset(path, type, dimensions, features);
    }

    
    public void setItem(String path, Object value, int index) throws IOException {
        path=adjustPath(path);        
        
        Class type = null;
        if (value != null) {
            type = value.getClass();
            if (type ==  NDArray.class){
                value = JepUtils.toJavaArray((NDArray)value);
                type = value.getClass();
            }            
            if (type.isPrimitive()) {
                type = Convert.getWrapperClass(type);
            }
        }       
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Append \"{0}\" = {1}", new Object[]{path, Logging.getLogForValue(value)});
        }
        assertOpen(); //Avoid NullPointerException if don't have rights to data folder
        getFormat().setItem(path, value, type, index);
    }

    /**
     * Write to multi-dimensional dataset. val must be a 1d array
     */
    public void setItem(String path, Object val, long[] index, int[] shape) throws IOException {
        path=adjustPath(path);
        
        Class type = null;
        if (val != null) {
            type = val.getClass();
            if (type ==  NDArray.class){
                val = JepUtils.toJavaArray((NDArray)val);
                type = val.getClass();
            }            
            if ((!type.isArray()) || (!type.getComponentType().isPrimitive())) {
                throw new IllegalArgumentException("Value must be a 1d array of primitive type");
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Set \"{0}:{1}\" = {2}", new Object[]{path, Convert.arrayToString(index, "."), Logging.getLogForValue(val)});
        }
        assertOpen(); //Avoid NullPointerException if don't have rights to data folder
        getFormat().setItem(path, val, type, index, shape);
    }

    public void appendItem(String path, Object val) throws IOException {
        setItem(path, val, -1);
    }

    public void setAttribute(String path, String name, Object value) throws IOException {
        setAttribute(path, name, value, false);
    }

    public void setAttribute(String path, String name, Object value, boolean unsigned) throws IOException {
        if ((name == null) || (name.isEmpty()) || (value == null)) {
            throw new IllegalArgumentException();
        }
        path=adjustPath(path);
        openOutput();
        Class type = value.getClass();
        if (type ==  NDArray.class){
            value = JepUtils.toJavaArray((NDArray)value);
            type = value.getClass();
        }          
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        logger.log(Level.FINER, "Set attribute \"{0}/{1}\" value = {2}", new Object[]{path, name, Str.toString(value, 10)});
        getFormat().setAttribute(path, name, value, type, unsigned);

        if (!getFormat().isPacked()){
            File file = getFormat().getAttributePath(path).toFile();
            IO.setFilePermissions(file, filePermissions);
        }
    }

    public boolean isDisplayablePlot(Map<String, Object> info) {
        if (info != null){
            String infoType = String.valueOf(info.get(Format.INFO_TYPE));
            boolean isDataset = infoType.equals(Format.INFO_VAL_TYPE_DATASET) || infoType.equals(Format.INFO_VAL_TYPE_SOFTLINK);
            if (isDataset && (((Integer) info.getOrDefault(Format.INFO_RANK, 0)) > 0)) {
                String dataType = (String) info.get(Format.INFO_DATA_TYPE);
                if (dataType != null) {
                    switch (dataType) {
                        case Format.INFO_VAL_DATA_TYPE_COMPOUND:
                        case Format.INFO_VAL_DATA_TYPE_FLOAT:
                        case Format.INFO_VAL_DATA_TYPE_INTEGER:
                        case Format.INFO_VAL_DATA_TYPE_BITFIELD:
                        case Format.INFO_VAL_DATA_TYPE_BOOLEAN:
                            return true;
                    }
                }
            }
        }
        return false;
    }
    
    public void close() throws IOException {
        closeOutput();
    }
}
