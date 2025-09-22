package ch.psi.pshell.data;

import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Type;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format implementation storing data in text files.
 */
public class FormatText implements Format {
            
    @Override
    public String getId() {
        return "txt";
    }
    
    class OutputFile {

        OutputFile(Path path, PrintWriter out) {
            this(path, out, true);
        }

        OutputFile(Path path, PrintWriter out, boolean header) {
            this.out = out;
            this.header = header;
            this.records = 0;
            this.path = path;
        }
        Path path;
        PrintWriter out;
        boolean header;
        boolean composite;
        int records;
        int[] dimensions;
        boolean indeterminateSize;
    }

    String root;
    final HashMap<String, OutputFile> openFiles = new HashMap();

    public static final String INFO_ITEM_SEPARATOR = "Item Separator";

    public static final String COMMENT_MARKER = "#";
    public static final String PAGE_MARKER = "page: ";

    public static final String ATTR_FILE = "attrs";
    public static final String ATTR_CLASS_MARKER = " # ";
    public static final String ATTR_VALUE_MARKER = " = ";
    
    //V1 header
    public static final String TYPE_MARKER = "type ";
    public static final String DIMS_MARKER = "dims ";
    public static final String LENGTH_SEPARATOR = " * ";
    
    //V2 header
    public static final String ARRAY_MARKER = "array";
    public static final String COMPOSITE_MARKER = "table";
    public static final String INDETERMINATE_SIZE_PLACEHOLDER = "         "; //Max 1G records
    
    static String ITEM_SEPARATOR;
    static String ARRAY_SEPARATOR;
    static String LINE_SEPARATOR;


    static boolean EMBEDDED_ATTRIBUTES = false;

    public static void setDefaultEmbeddedAttributes(boolean value) {
        EMBEDDED_ATTRIBUTES = value;
    }

    public static boolean getDefaultEmbeddedAttributes() {
        return EMBEDDED_ATTRIBUTES;
    }   
    
    static int HEADER_VERSION = 2;

    public static void setDefaultHeaderVersion(int version) {
        HEADER_VERSION = version;
    }

    public static int getDefautHeaderVersion() {
        return HEADER_VERSION;
    }        

    static String NULL_VALUE = " ";

    public static void setNullValue(String str) {
        NULL_VALUE = str;
    }

    public static String getNullValue() {
        return NULL_VALUE;
    }
    
    static boolean IDENTIFY_SEPARATOR = true;

    public static void setDefaultItemSeparator(String str) {
        ITEM_SEPARATOR = str;
    }

    public static String getDefaultItemSeparator() {
        if (ITEM_SEPARATOR == null) {
            ITEM_SEPARATOR = "; ";
        }
        return ITEM_SEPARATOR;
    }

    public static void setDefaultArraySeparator(String str) {
        ARRAY_SEPARATOR = str;
    }

    public static String getDefaultArraySeparator() {
        if (ARRAY_SEPARATOR == null) {
            ARRAY_SEPARATOR = " ";
        }
        return ARRAY_SEPARATOR;
    }

    public static void setDefaultLineSeparator(String str) {
        LINE_SEPARATOR = str;
    }

    public static String getDefaultLineSeparator() {
        if (LINE_SEPARATOR == null) {
            LINE_SEPARATOR = "\n";
        }
        return LINE_SEPARATOR;
    }

    public static void setIdentifySeparator(boolean str) {
        IDENTIFY_SEPARATOR = str;
    }

    public static boolean getIdentifySeparator() {
        return IDENTIFY_SEPARATOR;
    }

    String itemSeparator = getDefaultItemSeparator();
    String arraySeparator = getDefaultArraySeparator();
    String lineSeparator = getDefaultLineSeparator();
    
    
    boolean embeddedAtributes = EMBEDDED_ATTRIBUTES;

    public void setEmbeddedAttributes(boolean value) {
        embeddedAtributes = value;
    }

    public boolean getEmbeddedAttributes() {
        return embeddedAtributes;
    }   
    
    boolean orderedAtributes = true;

    public boolean getOrderedAtributes() {
        return orderedAtributes;
    }

    public void setOrderedAtributes(boolean value) {
        orderedAtributes = value;
    }
    
    boolean finalSeparator = false;

    public boolean getFinalSeparator() {
        return finalSeparator;
    }

    public void setFinalSeparator(boolean value) {
        finalSeparator = value;
    }    

    public void setItemSeparator(String str) {
        itemSeparator = str;
    }

    public String getItemSeparator() {
        return itemSeparator;
    }

    public void setArraySeparator(String str) {
        arraySeparator = str;
    }

    public String getArraySeparator() {
        return arraySeparator;
    }

    public void setLineSeparator(String str) {
        lineSeparator = str;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }
    
    static int headerVersion = HEADER_VERSION;

    public void setHeaderVersion(int version) {
        headerVersion = version;
    }

    public int getHeaderVersion() {
        return headerVersion;
    }         
    
    public int getHeaderSize(){
        if (headerVersion==1){
            return 2;
        }
        return 1;
    }

    @Override
    public String getFileType() {
        return "txt";
    }

    @Override
    public boolean isPacked() {
        return false;
    }
    
    String compositePrefix;
    protected String getCompositePrefix(){
        if (compositePrefix==null){
           compositePrefix = createAttrStr(COMPOSITE_MARKER, null, new int[]{0}, null) + INDETERMINATE_SIZE_PLACEHOLDER;
        }
        return compositePrefix;
    }

    protected String getFileExtension(String path) {
        String ext = getFileType();
        if ((ext != null) && (ext.length() > 0)) {
            ext = "." + ext;
        } else {
            ext = "";
        }
        return ext;
    }
    
    @Override
    public Path getFilePath(String path) {
        return getFilePath(this.root, path);
    }

    @Override
    public Path getAttributePath (String path)  {
        try {
            if (!getEmbeddedAttributes() || !(new File(path).isFile())) {
                return getAttributePath(root, path);
            }
        } catch (IOException ex){
        }
        return Format.super.getAttributePath(path);
    }


    public Path getFilePath(String path, boolean addExtension) {
        return getFilePath(this.root, path, addExtension);
    }

    public Path getFilePath(String root, String path) {
        return getFilePath(root, path, true);
    }

    public Path getFilePath(String root, String path, boolean addExtension) {
        //Filenames don't support ':'
        path = path.replace(":", "_");

        Path ret = Paths.get(root, path);
        if (addExtension){
            if (!ret.toFile().isDirectory()) {
                String extension = IO.getExtension(path);
                if ((extension == null) || extension.isEmpty()) {
                    path += getFileExtension(path);
                }
                ret = Paths.get(root, path);
            }
        }
        return ret;
    }   

    @Override
    public void openOutput(File root) throws IOException {
        closeOutput();
        if (root.isFile()) {
            throw new IllegalArgumentException(root.getPath());
        }
        root.mkdirs();
        try {
            this.root = root.getCanonicalPath();
        } catch (IOException ex) {
            throw new IllegalArgumentException(root.getPath());
        }
    }

    @Override
    public void closeOutput() throws IOException {
        synchronized (openFiles) {
            for (OutputFile of : openFiles.values()) {
                try {
                    of.out.close();
                } catch (Exception ex) {
                    Logger.getLogger(FormatText.class.getName()).log(Level.WARNING, null, ex);
                }
                if (getHeaderVersion()!=1.0){
                    if(of.indeterminateSize && (of.records>0) && (of.dimensions!=null) && (of.dimensions.length>0)){
                        //Update dimensions
                        try (RandomAccessFile raf = new RandomAccessFile(of.path.toFile(), "rw")) {
                            String firstLine = raf.readLine();  // read and discard to know length
                            long offset = firstLine.indexOf("[0"); 
                            if (offset >= 0) {
                                of.dimensions[0] = of.records;
                                raf.seek(offset);  
                                raf.write(Str.toString(of.dimensions).getBytes(StandardCharsets.UTF_8));  
                            }
                        }                        
                    }
                }
            }
            openFiles.clear();
        }
        root = null;
    }

   
    @Override
    public void checkLogFile(String path) throws IOException {
        if (!openFiles.containsKey(path)) {
            OutputFile of;
            synchronized (openFiles) {
                of = openFiles.get(path);
                if (of == null) {                    
                    Logger.getLogger(FormatText.class.getName()).info("Reopening log file: " + path);
                    Path filePath = getFilePath(path);    
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toString(), true)));
                    of = new OutputFile(filePath, out);                    
                    openFiles.put(path, of);
                }
            }
        }
    }  
     
    @Override
    public void flush() {
        synchronized (openFiles) {
            for (String key : openFiles.keySet()) {
                openFiles.get(key).out.flush();
            }
        }
    }

    @Override
    public Object[] getStructure(String root) throws IOException {
        String format = getFileType();
        List contents = new ArrayList();
        List folders = new ArrayList();
        File file = Paths.get(root).toFile();
        contents.add(file.getName());
        File[] files = IO.listFiles(file);
        IO.orderByModifiedAndName(files);
        for (File content : files) {
            if (content.isDirectory()) {
                folders.add(getStructure(content.getPath()));
            } else {
                String ext = IO.getExtension(content);
                if (ext.equals(format)) {
                    contents.add(IO.getPrefix(content));
                }
            }
        }
        contents.addAll(folders);
        return contents.toArray();
    }

    @Override
    public String[] getChildren(String root, String path) throws IOException {
        String format = getFileType();
        ArrayList<String> ret = new ArrayList<>();
        Path filePath = getFilePath(root, path);
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (filePath.toFile().isDirectory()) {
            for (String content : filePath.toFile().list()) {
                String ext = IO.getExtension(content);
                if (ext.equals(format) || Paths.get(filePath.toString(), content).toFile().isDirectory()) {
                    if (ext.equals(format)) {
                        content = content.substring(0, content.length() - format.length() - 1);
                    }
                    //ret.add(IO.getRelativePath(content, root));
                    //ret.add(content);
                    ret.add(path + content);
                }
            }
        }
        return ret.toArray(new String[0]);
    }

    void addClassInfo(String typeName, Map<String, Object> ret)  {
        boolean unsigned = false;
        Class cls;
        try{
            Type type = Type.fromKey(typeName);            
            cls = type.toClass();
            unsigned = type.isUnsigned();                    
        } catch (Exception ex){
            try {
                cls = Class.forName(typeName);
            } catch (ClassNotFoundException ex1) {
                Logger.getLogger(FormatText.class.getName()).warning("Invalid dataset type, setting to Double: " + typeName);
                cls = Double.class;
            }
        }       
        addClassInfo(cls, unsigned, ret);
    }
    
    
    void addClassInfo(Class type, boolean unsigned, Map<String, Object> ret)  {
        Integer size = null;        
        
        ret.put(INFO_CLASS, type.getName());
        
        String dataType = null;
        if (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        if (Number.class.isAssignableFrom(type)) {
            if ((type == Double.class) || (type == Long.class)) {
                size = 8;
            } else if ((type == Float.class) || (type == Integer.class)) {
                size = 4;
            } else if (type == Short.class) {
                size = 2;
            } else if (type == Byte.class) {
                size = 1;
            } else if (type == BigInteger.class) {
                size = -1;
            }

            if ((type == Double.class) || (type == Float.class)) {
                dataType = INFO_VAL_DATA_TYPE_FLOAT;
            } else {
                dataType = INFO_VAL_DATA_TYPE_INTEGER;    
                ret.put(INFO_SIGNED, !unsigned);
            }
        } else if (type == String.class) {
            dataType = INFO_VAL_DATA_TYPE_STRING;
        }
        if (dataType != null) {
            ret.put(INFO_DATA_TYPE, dataType);
        }
        if (size != null) {
            ret.put(INFO_ELEMENT_SIZE, size);
        }
    }
    
    public  String getItemSeparator(String line) {
        return getSeparator(line, getItemSeparator(), getArraySeparator());
    }  
    
    public static  String getSeparator(String line, String defaultFallback, String forbidden) {
        String separator = defaultFallback;         

        // Match sequences of non-alphanumeric, non-bracket, non-dot, non-colon chars
        // Basically "separators"
        Pattern p = Pattern.compile("[^A-Za-z0-9.:$_\\[\\]-]+");
        Matcher m = p.matcher(line);

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        
        while (m.find()) {
            String sep = m.group();
            if (forbidden != null && sep.equals(forbidden)) {
                continue;
            }            
            counts.merge(sep, 1, Integer::sum);
        }

        if (!counts.isEmpty()) {
            Comparator<Entry<String,Integer>> cmp = (e1, e2) -> {
                // 1) by descending count
                int c = Integer.compare(e2.getValue(), e1.getValue());
                if (c != 0) return c;

                String s1 = e1.getKey();
                String s2 = e2.getKey();
                
                // 2) prefer defaultFallback if one of them matches it
                boolean d1 = s1.equals(defaultFallback);
                boolean d2 = s2.equals(defaultFallback);
                if (d1 != d2) return d1 ? -1 : 1;

                // 3) prefer separators containing non-whitespace characters
                boolean nonWS1 = s1.chars().anyMatch(ch -> !Character.isWhitespace(ch));
                boolean nonWS2 = s2.chars().anyMatch(ch -> !Character.isWhitespace(ch));
                if (nonWS1 != nonWS2) return nonWS1 ? -1 : 1;

                // 4) prefer longer separator
                int lenComp = Integer.compare(s2.length(), s1.length());
                if (lenComp != 0) return lenComp;

                // 6) stable fallback
                return s1.compareTo(s2);
            };
            
            separator = counts.entrySet().stream()
                       .sorted(cmp)
                       .findFirst()
                       .get()
                       .getKey();            
        }

        return separator;
    }

    @Override
    public Map<String, Object> getInfo(String root, String path) throws IOException {
        synchronized (openFiles) {
            OutputFile openFile = openFiles.get(path);
            if (openFile!=null){
                openFile.out.flush();
            }
        }        
        
        HashMap<String, Object> ret = new HashMap<>();
        List<String> header = new ArrayList<>();
        boolean parsingHeader = true;
        Path filePath = getFilePath(root, path);
        File file = filePath.toFile();
        if (!file.exists()) {
            ret.put(INFO_TYPE, INFO_VAL_TYPE_UNDEFINED);
        } else if (filePath.toFile().isFile()) {
            ret.put(INFO_TYPE, INFO_VAL_TYPE_DATASET);
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
                int index = 0;
                while ((line = br.readLine()) != null) {
                    //line = line.trim();
                    if (!line.isEmpty()) {
                        if (!line.startsWith(COMMENT_MARKER)) {
                            if (parsingHeader == true){
                                parsingHeader = false;
                                processFileHeader(header, ret);                                
                                if ((Integer) ret.get(INFO_RANK) != 3) {
                                    break;
                                }
                                int[] dims = (int[]) ret.get(INFO_DIMENSIONS);
                                if ((dims==null)  || (dims.length==0) || (dims[0]>0)){
                                    break;
                                }
                                //Header Version1: Parse number of images
                            }

                        } else {                            
                            line = line.substring(1);
                            if (parsingHeader == true){                                
                                if ((index==0) || !line.contains(ATTR_VALUE_MARKER)){
                                    header.add(line);
                                }
                            } else {
                                //Header Version1: Parse number of images
                                if (line.startsWith(PAGE_MARKER)) {
                                    Integer page = Integer.valueOf(line.substring(PAGE_MARKER.length()));
                                    if (ret.containsKey(INFO_DIMENSIONS)) {
                                        int[] dimensions = (int[]) ret.get(INFO_DIMENSIONS);
                                        dimensions[0] = page + 1;
                                        ret.put(INFO_DIMENSIONS, dimensions);
                                    }
                                }                                
                            }
                        }
                        index++;
                    }
                }
                if (header.size()>0){
                    if (parsingHeader == true){
                        processFileHeader(header, ret);
                    }                    
                    if (!ret.containsKey(INFO_CLASS)) {
                        //If no type record assumes Double                                
                        addClassInfo(Double.class, false, ret);
                    }
                    if (!ret.containsKey(INFO_DIMENSIONS)) {
                        ret.put(INFO_DIMENSIONS, new int[]{0});
                        ret.put(INFO_RANK, 1);
                    }                    
                    int[] dimensions = (int[]) ret.get(INFO_DIMENSIONS);
                    long elements = 0;
                    if (dimensions.length>0){
                        elements = 1;
                        for (int l : dimensions){
                            elements *= l;
                        }
                    }
                    ret.put(INFO_ELEMENTS, elements);
                }                
            }
        } else {
            if (!IO.isSamePath(filePath.toString(), root)) {
                ret.put(INFO_TYPE, INFO_VAL_TYPE_GROUP);
            }
        }
        return ret;
    }
    
    protected void addSimpleDatasetInfo(String dataType, int[] dimensions, Map<String, Object> info){
        addClassInfo(dataType, info);
        if (dimensions!=null){
            info.put(INFO_DIMENSIONS, dimensions);
            info.put(INFO_RANK, dimensions.length);        
        }
    }
    
    protected void addCompositeDatasetInfo(String[] names, String[] types, int[] lenghts, int records, String separator, Map<String, Object> info){
            info.put(INFO_FIELD_NAMES, names);
            info.put(INFO_FIELDS, names.length);        
            info.put(INFO_DIMENSIONS, new int[]{records});
            info.put(INFO_RANK, 1);
            info.put(INFO_CLASS, Object.class.getName());
            info.put(INFO_DATA_TYPE, INFO_VAL_DATA_TYPE_COMPOUND);
            info.put(INFO_FIELD_TYPES, types);
            info.put(INFO_FIELD_LENGTHS, lenghts);        
            info.put(INFO_ITEM_SEPARATOR, separator);
    }


    protected Path getAttributePath(String root, String path) throws IOException {
        String attrFileName = ATTR_FILE;
        return isGroup(root, path)
                ? Paths.get(root, path, attrFileName)
                : Paths.get(root, path + "." + attrFileName);
    }

    @Override
    public Map<String, Object> getAttributes(String root, String path) throws IOException {
        Map<String, Object> ret = new HashMap<>();
        Path filePath = getAttributePath(root, path);
        File file = filePath.toFile();
        //attr file
        if ((file.exists()) && (file.isFile())) {            
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                while ((line = br.readLine()) != null) {
                    parseAttribute(line, ret, true); //Last more recent
                }
            }
        } else {
            //embedded
            filePath = getFilePath(root, path);
            file = filePath.toFile();
            if (file.isFile()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    int index = 0;
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.isEmpty()) {
                            if (getOrderedAtributes()) {
                                //If header finished then break
                                if ((!line.isEmpty()) && (!line.startsWith(COMMENT_MARKER))) {
                                    break;
                                }
                            }
                            if ((index >= getHeaderSize()) && (line.startsWith(COMMENT_MARKER))) {
                                line = line.substring(1);
                                parseAttribute(line, ret, false); //First more recent
                            }
                            index++;
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    protected Object getAttrVal(Class type, String attr){
            if (type.isPrimitive()) {
                type = Convert.getWrapperClass(type);
            }
            if (type == String.class) {
                return attr;
            } else if (type == Double.class) {
                return  Double.valueOf(attr);
            } else if (type == Float.class) {
                return  Float.valueOf(attr);
            } else if (type == Long.class) {
                return  Long.valueOf(attr);
            } else if (type == Integer.class) {
                return  Integer.valueOf(attr);
            } else if (type == Short.class) {
                return  Short.valueOf(attr);
            } else if (type == Byte.class) {
                return  Byte.valueOf(attr);
            } else if (type == BigInteger.class) {
                return  new BigInteger(attr);
            } else if (type == Boolean.class) {
                return  Boolean.valueOf(attr);
            } else if (type.isArray()) {
                Class componentType = type.getComponentType();
                String arrSeparator = getArraySeparator();
                if (type == String[].class) {
                    return  attr.split(arrSeparator);
                } else {
                    if (Convert.isWrapperClass(componentType)) {
                        componentType = Convert.getPrimitiveClass(type);
                    }
                    if (componentType.isPrimitive()) {
                        return  Convert.toPrimitiveArray(attr, arrSeparator, componentType);
                    }
                }
            }
            return attr;
    }    
    
    
    @Override
    public DataSlice getData(String root, String path, int index) throws IOException {
        DataSlice ret = null;
        if (!isDataset(root, path)) {
            return null;
        }

        Map<String, Object> info = getInfo(root, path);

        Class type = getTypeClass((String) info.get(INFO_CLASS));
        int[] dimensions = (int[]) info.get(INFO_DIMENSIONS);
        //If not null assumes is heterogeneous table (compound type)       
        Integer fields = (Integer) info.get(INFO_FIELDS);
        boolean composite = fields != null;
        Class[] fieldTypes = composite ? getFieldTypes((String[]) info.get(INFO_FIELD_TYPES), (int[]) info.get(INFO_FIELD_LENGTHS)) : null;
        Path filePath = getFilePath(root, path);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            int rank = dimensions.length;
            int[] pageDim = dimensions;
            boolean started = true;
            if (rank == 3) {
                pageDim = new int[]{dimensions[1], dimensions[2]};
                started = false;
            }
            ArrayList data = new ArrayList();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    if (!line.startsWith(COMMENT_MARKER)) {
                        if (started) {
                            if (composite) {
                                String separator = getItemSeparator();
                                Object sep = info.get(INFO_ITEM_SEPARATOR);
                                if ((sep instanceof String s) && !s.isBlank()) {
                                    separator = s;
                                }                     
                                Object[] record = getRecord(line, separator, fieldTypes);
                                data.add(record);
                            } else {
                                switch (rank) {
                                    case 0:
                                        Object array = (Number.class.isAssignableFrom(type)) ? Convert.stringToNumber(line, type) : line;
                                        return new DataSlice(root, path, dimensions, array, 0, false);
                                    case 1:
                                        data.add(Number.class.isAssignableFrom(type) ? Convert.stringToNumber(line, type) : line);
                                        break;
                                    default:
                                        String[] vals = line.split(getArraySeparator());
                                        Class arrayType = Convert.getPrimitiveClass(type);
                                        data.add((arrayType != null)
                                                ? Convert.toPrimitiveArray(vals, arrayType)
                                                : vals);
                                        break;

                                }
                            }
                        }
                        //}
                    } else {
                        if (rank == 3) {
                            if (!started) {
                                if (line.startsWith(COMMENT_MARKER + PAGE_MARKER + index)) {
                                    started = true;
                                }
                            } else if (line.startsWith(COMMENT_MARKER + PAGE_MARKER)) {
                                break;
                            }
                        }
                    }
                }
            }
            Object array = null;
            if (composite) {
                array = data.toArray(new Object[0][0]);
            } else if ((rank == 1) && (Number.class.isAssignableFrom(type))) {
                array = Convert.toPrimitiveArray(data.toArray(), type);
            } else if ((rank == 2) && (Number.class.isAssignableFrom(type))) {
                array = data.toArray((Object[]) Array.newInstance(Convert.getPrimitiveClass(type), 0, 0));
            } else {
                array = data.toArray();
            }
            ret = new DataSlice(root, path, dimensions, array, index, false);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Invalid file format");
        }
        return ret;
    }
    
    protected Object[] getRecord(String line, String separator, Class[] fieldTypes) {
        String[] vals = line.split(separator);
        Object[] record = new Object[vals.length];
        for (int i = 0; i < vals.length; i++) {
            Class fieldType = fieldTypes[i];
            if (fieldType.isArray()) {
                Class compType = fieldType.getComponentType();
                //Do not support rank>1 in tables
                if (compType.isArray() || vals[i].isEmpty()) {
                    record[i] = null;
                } else {
                    record[i] = compType.isPrimitive()
                            ? Convert.toPrimitiveArray(vals[i],  getArraySeparator(), compType)
                            : vals[i];
                }
            } else {
                if (vals[i].isEmpty()) {
                    if (fieldType == Double.class) {
                        record[i] = Double.NaN;
                    } else if (fieldType == Float.class) {
                        record[i] = Float.NaN;
                    } else if (Number.class.isAssignableFrom(fieldType)) {
                        record[i] = Convert.stringToNumber("0", fieldType);
                    } else {
                        record[i] = vals[i];
                    }
                } else {
                    record[i] = Number.class.isAssignableFrom(fieldType)
                            ? Convert.stringToNumber(vals[i], fieldType)
                            : vals[i];
                }
            }
        }
        return record;
    }

    protected Class[] getFieldTypes(String[] typeNames, int[] lenghts) {
        Class[] fieldTypes = new Class[typeNames.length];
        for (int i = 0; i < fieldTypes.length; i++) {
            if (typeNames == null){
                fieldTypes[i] = String.class;
            } else {
                if ((lenghts==null) || (lenghts[i] <=0)){
                    fieldTypes[i] = getTypeClass(typeNames[i]);
                } else {
                    fieldTypes[i] = getTypeArrayClass(typeNames[i]);
                }

            }
        }
        return fieldTypes;
    }            

    @Override
    public boolean isDataset(String root, String path) throws IOException {
        return getFilePath(root, path).toFile().isFile();
        }
                
    @Override
    public boolean isGroup(String root, String path) throws IOException {
        return Paths.get(root, path).toFile().isDirectory();
    }

    @Override
    public void createGroup(String path) throws IOException {
        Path filePath = getFilePath(root, path, false);
        filePath.toFile().mkdirs();
    }

    final Object attributesLock = new Object();

    @Override
    public void setAttribute(String path, String name, Object value, Class type, boolean unsigned) throws IOException {
        Path filePath = getFilePath(root, path);
        if (filePath.toFile().isFile() && getEmbeddedAttributes()) {
            OutputFile of = null;
            synchronized (openFiles) {
                of = openFiles.get(path);
            }
            if (of == null) {
                throw new IOException("Output file not opened: " + filePath);
            }
            synchronized (of) {
                PrintWriter out = of.out;
                StringBuffer sb = new StringBuffer();
                sb.append(COMMENT_MARKER);
                writeAttribute(name, value, type, unsigned, sb);
                sb.append(getLineSeparator());

                if (!getOrderedAtributes() || of.header) {
                    out.print(sb.toString());
                } else {
                    out.close();
                    int index = 0;
                    try (RandomAccessFile r = new RandomAccessFile(filePath.toFile(), "r")) {
                        for (int i=0; i< getHeaderSize(); i++){
                            if (r.readLine() == null){
                                break;
                            }
                            if (i==getHeaderSize()-1){
                                index = (int) r.getFilePointer();
                            }
                        }
                    } catch (Exception ex) {
                    }
                    IO.insert(filePath.toString(), index, sb.toString().getBytes());
                    out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toString(), true)));
                    synchronized (openFiles) {
                        openFiles.put(path, new OutputFile(filePath, out, false));
                    }
                }
            }
        } else {
            synchronized (attributesLock) {
                Path attrPath = getAttributePath(root, path);
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(attrPath.toString(), true)))) {
                    writeAttribute(name, value, type, unsigned, writer);
                    writer.print(getLineSeparator());
                }
            }
        }
    }

    @Override
    public void setDataset(String path, Object data, Class type, int rank, int[] dimensions, boolean unsigned, Map features) throws IOException {
        createDataset(path, type, dimensions, unsigned, features);
        if (rank == 0) {
            setItem(path, data, type, 0);
        } else {
            int size = Array.getLength(data);
            for (int i = 0; i < size; i++) {
                setItem(path, Array.get(data, i), type, i);
            }
        }
    }

    @Override
    public void createDataset(String path, Class type, int[] dimensions, boolean unsigned, Map features) throws IOException {
        Path filePath = getFilePath(path);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toString(), true)));
        OutputFile of;
        synchronized (openFiles) {
            of = openFiles.get(path);
            if (of == null) {
                of = new OutputFile(filePath, out);
                openFiles.put(path, of);
            }
        }
        synchronized (of) {
            of.composite = false;
            of.dimensions = dimensions;
            of.indeterminateSize = ((dimensions!=null) && (dimensions[0]==0));
            writeSingleDatasetHeader(out, type, dimensions, unsigned, of.indeterminateSize);
        }
    }
    
    @Override
    public void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        Path filePath = getFilePath(path);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toString(), true)));
        OutputFile of;
        synchronized (openFiles) {
            of = openFiles.get(path);
            if (of == null) {
                of = new OutputFile(filePath, out);
                openFiles.put(path, of);
            }
        }
        synchronized (of) {
            of.composite = true;
            of.dimensions = new int[]{0};
            of.indeterminateSize = true;            
            writeCompositeDatasetHeader(out, names, types, lengths);
        }
    }
    
    public String getTypeName( Class type, boolean unsigned){        
        try{
            String ret = Type.toKey(type, unsigned);
            return ret;
            
        } catch (Exception ex){
            return type.getName();
        }        
    }

    public Class getTypeClass(String name){        
        Class type = null;
        try{
            type = Type.toClass(name);
        } catch (Exception ex){
            try {
                type = Class.forName(name);
            } catch (ClassNotFoundException ex1) {
                Logger.getLogger(FormatText.class.getName()).warning("Invalid data type name, setting to Double: " + name);
            }
        }        
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }        
        return type;
    }

    public Class getTypeArrayClass(String name){        
        Class type = null;
        try{
            type = Type.fromKey(name).toPrimitiveArrayClass();
        } catch (Exception ex){
            try {
                type = Class.forName(name);
            } catch (ClassNotFoundException ex1) {
                Logger.getLogger(FormatText.class.getName()).warning("Invalid data type name, setting to Double: " + name);
            }
        }        
        return type;
    }
    
    protected String createAttrStr(String name, String type, int[] dimensions, Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (type!=null){
            sb.append(":");
            if (dimensions!=null){              
                type = Type.toComponentTypeKey(type);
            }
            sb.append(type);
        }
        if (dimensions!=null){              
            sb.append(Str.toString(dimensions));
        }        
        if (value!=null){
            sb.append("=");
            try{
                writeElement(sb, value);
            } catch (IOException ex){
                sb.append(String.valueOf(value));
            }

        }
        return sb.toString();
    }
    
    
    protected record Attr(String name, String type, int[] dimensions, String value) {}
   
    protected static Attr parseAttrStr(String s) {
        if (s == null) throw new IllegalArgumentException("Input is null");
        String line = Str.trimLeft(s); //Preserve trailing spaces in value
        if (line.isBlank()) throw new IllegalArgumentException("Input is empty");

        // split off value if present (first '=')
        int eq = line.indexOf('=');
        String left = (eq >= 0) ? line.substring(0, eq).trim() : line.trim();
        String value = (eq >= 0) ? line.substring(eq + 1) : null;

        String name;
        String type = null;
        int[] dims = null;

        // check for colon (type separator)
        int colon = left.indexOf(':');
        if (colon >= 0) {
            name = left.substring(0, colon).trim();
            String rest = left.substring(colon + 1).trim();

            // does rest contain dimensions?
            int brOpen = rest.indexOf('[');
            if (brOpen >= 0) {
                type = rest.substring(0, brOpen).trim();
                dims = parseDims(rest, brOpen, s);
            } else {
                type = rest.isEmpty() ? null : rest;
            }
        } else {
            // no type marker, check for dims directly
            int brOpen = left.indexOf('[');
            if (brOpen >= 0) {
                name = left.substring(0, brOpen).trim();
                dims = parseDims(left, brOpen, s);
            } else {
                name = left.trim();
            }
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Empty name in: " + s);
        }

        return new Attr(name, type, dims, value);
    }
    
    private static int[] parseDims(String str, int brOpen, String original) {
        int brClose = str.indexOf(']', brOpen);
        if (brClose < 0) {
            throw new IllegalArgumentException("Missing closing ']' in: " + original);
        }
        String inner = str.substring(brOpen + 1, brClose).trim();
        if (inner.isEmpty()) {
            return new int[]{0}; // treat [] as [0]
        }
        String[] parts = inner.split(",");
        int[] dims = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                dims[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid integer in dimensions: '" + parts[i] + "' in: " + original, ex);
            }
        }
        return dims;
    }    

    protected void writeSingleDatasetHeader(PrintWriter out,  Class type, int[] dimensions, boolean unsigned, boolean indeterminateSize) throws IOException{
        if (getHeaderVersion()==1.0){
            out.print(COMMENT_MARKER + TYPE_MARKER + type.getName());        
            out.print(getLineSeparator());
            out.print(COMMENT_MARKER + DIMS_MARKER + Arrays.toString(dimensions));            
        } else {
            out.print(COMMENT_MARKER + createAttrStr(ARRAY_MARKER, getTypeName(type, unsigned), dimensions, null));        
            if (indeterminateSize){
                out.print(INDETERMINATE_SIZE_PLACEHOLDER);
            }
        }
        out.print(getLineSeparator());        
    }
    
    protected void writeCompositeDatasetHeader(PrintWriter out,  String[] names, Class[] types, int[] lengths) throws IOException{        
        if (getHeaderVersion()==1.0){
            out.print(COMMENT_MARKER);
            out.print(String.join(getItemSeparator(), names));
            if (getFinalSeparator() &&  (names.length > 0)) {
                out.append(getItemSeparator());
            }
            out.append(getLineSeparator());
            StringJoiner sj = new StringJoiner(getItemSeparator(), COMMENT_MARKER, getLineSeparator());
            for (int i = 0; i < types.length; i++) {
                String type =  types[i].getName();
                if (lengths[i] > 0) {
                    type += LENGTH_SEPARATOR + String.valueOf(lengths[i]);
                }
                sj.add(type);
            }             
            out.append(sj.toString());
        } else {
            out.print(COMMENT_MARKER + getCompositePrefix());
            var header = new ArrayList<String>();
            for (int i = 0; i < names.length; i++) {
                int[] dims = lengths[i]>0 ? new int[]{lengths[i]} : null;
                header.add(createAttrStr(names[i], getTypeName(types[i], false), dims, null));
            }
            out.print(String.join(getItemSeparator(), header));
            if (getFinalSeparator() &&  (names.length > 0)) {
                out.append(getItemSeparator());
            }            
            out.print(getLineSeparator());
        }        
    }
    
    protected void writeAttribute(String name, Object value, Class type, boolean unsigned, Appendable buffer) throws IOException{
        if (getHeaderVersion()==1.0){
            buffer.append(name + ATTR_VALUE_MARKER);
            writeElement(buffer, value);
            buffer.append(ATTR_CLASS_MARKER + getTypeName(type, unsigned));                        
        } else {
            int[] dimensions = ((value != null) && (value.getClass().isArray())) ? new int[]{Array.getLength(value)} : null;
            buffer.append(createAttrStr(name, getTypeName(type, unsigned), dimensions, value));            
        }
    }
        
    private void parseAttribute(String line, Map<String, Object> ret, boolean overwrite) {        
        if (line.contains(ATTR_VALUE_MARKER)) { 
            //Header Version ==1.0
            String[] tokens = line.split(ATTR_VALUE_MARKER);
            if (tokens.length == 2) {
                String[] val = tokens[1].split(ATTR_CLASS_MARKER);
                if (val.length == 2) {
                    Object data = val[0];
                    Class type = String.class;
                    try {
                        type = getTypeClass(val[1]);
                        data = getAttrVal(type, val[0]);
                    } catch (Exception ex) {
                    }         
                    String name = tokens[0].trim();
                    if (overwrite || ! ret.containsKey(name)){
                        ret.put(name, data);
                    }
                }
            }
        } else {
            Attr attr = parseAttrStr(line);
            Class type = (attr.dimensions != null) ? getTypeArrayClass(attr.type) : getTypeClass(attr.type);
            if (overwrite || ! ret.containsKey(attr.name)){
                ret.put(attr.name, getAttrVal(type, attr.value));            
            }
        }
    }
    
    
    protected void processFileHeader(List<String> header, Map<String, Object> info){        
        if (header.size()==0){
            return;
        }                
        boolean v2 = ((header.size()==1) || 
                     header.get(0).startsWith(ARRAY_MARKER) || 
                     header.get(0).startsWith(COMPOSITE_MARKER));        
        if (v2){       
            if (header.get(0).startsWith(ARRAY_MARKER)){ //Simple
                String array = header.get(0).trim();     
                Attr attr = parseAttrStr(array.trim());
                addSimpleDatasetInfo(attr.type, attr.dimensions, info);
            } else if (header.get(0).startsWith(COMPOSITE_MARKER)){ //Composite   
                String line = header.get(0).substring(getCompositePrefix().length());
                if (line.isBlank()){
                    //Empty table
                    addCompositeDatasetInfo(new String[0], new String[0], new int[0], 0, getItemSeparator(), info); 
                } else {
                    String separator = getItemSeparator(line);
                    String[] columns = line.split(separator);                     
                    String[] names = new String[columns.length];
                    String[] types =  new String[columns.length];
                    int[] lengths = new int[columns.length];
                    for (int i=0; i<columns.length; i++){
                        Attr attr = parseAttrStr(columns[i]);
                        names[i] = attr.name;
                        types[i] = attr.type;
                        lengths[i] = attr.dimensions==null ? 0 : attr.dimensions[0];
                    }                
                    String compositePrefix = header.get(0).substring(0, getCompositePrefix().length());
                    Attr attr = parseAttrStr(compositePrefix.trim());
                    addCompositeDatasetInfo(names, types, lengths, attr.dimensions==null ? 0 : attr.dimensions[0], separator, info);                
                }
            }         
        } else {                       
            if (header.get(0).startsWith(TYPE_MARKER)){ //Simple
                String dataType = header.get(0).substring(TYPE_MARKER.length());     
                int[] dimensions = new int[0];
                try{
                    String line = header.get(1);
                    String aux = line.substring(line.indexOf("[") + 1, line.indexOf("]"));                
                    if (!aux.isEmpty()) {
                        String[] tokens = aux.split(",");
                        dimensions = new int[tokens.length];
                        for (int i = 0; i < dimensions.length; i++) {
                            dimensions[i] = Integer.valueOf(tokens[i].trim());
                        }
                    }                
                } catch (Exception ex){
                     //XScan dims attribute
                }
                addSimpleDatasetInfo(dataType, dimensions, info);

            } else { //Composite                           
                String separator = getItemSeparator(header.get(0));
                String[] names = header.get(0).split(separator);
                String[] types = header.get(1).split(separator);
                int[] lengths = new int[types.length];
                for (int i = 0; i < types.length; i++) {
                    if (types[i].contains(LENGTH_SEPARATOR)) {
                        String[] aux = types[i].split(Pattern.quote(LENGTH_SEPARATOR));
                        types[i] = aux[0];
                        lengths[i] = Integer.valueOf(aux[1].trim());
                    }
                    types[i] = types[i].trim();
                }                    
                addCompositeDatasetInfo(names, types, lengths, 0, separator, info);
            }            
        }
    }    

    void writeElement(Appendable out, Object value) throws IOException {
        if (value == null) {
            out.append(NULL_VALUE);
        } else if (value.getClass().isArray()) {
            int rank = Arr.getRank(value);
            //
            if (rank > 1) {
                out.append(NULL_VALUE);
            } else {
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    out.append(String.valueOf(Array.get(value, i)));
                    if (getFinalSeparator() ||  (i<length-1)) {
                        out.append(getArraySeparator());
                    }                                
                }
            }
        } else {
            out.append(String.valueOf(value));
        }
    }

    @Override
    /**
     * Only support appending
     */
    public void setItem(String path, Object data, Class type, int index) throws IOException {
        OutputFile of = null;
        synchronized (openFiles) {
            of = openFiles.get(path);
        }
        if (of == null) {
            throw new IOException("Output file not opened: " + path);
        }
        synchronized (of) {
            PrintWriter out = of.out;
            of.header = false;
            String separator = of.composite ? getItemSeparator() : getArraySeparator();
            int[] shape = Arr.getShape(data);
            int rank = shape.length;
            if (rank == 0) {
                writeElement(out, data);
            } else if (rank == 1) {
                for (int i = 0; i < shape[0]; i++) {
                    writeElement(out, Array.get(data, i));
                    if (getFinalSeparator() ||  (i<shape[0]-1)) {
                        out.print(separator);
                    }
                }
            } else if (rank == 2) {
                out.print(COMMENT_MARKER + PAGE_MARKER + index);
                out.print(getLineSeparator());
                for (int i = 0; i < shape[0]; i++) {
                    Object item = Array.get(data, i);
                    for (int j = 0; j < shape[1]; j++) {
                        writeElement(out, Array.get(item, j));
                        if (getFinalSeparator() ||  (j<shape[1]-1)) {
                            out.print(separator);
                        }                        
                    }
                    out.print(getLineSeparator());
                }
            }
            out.print(getLineSeparator());
            of.records++;
        }
    }    
}
