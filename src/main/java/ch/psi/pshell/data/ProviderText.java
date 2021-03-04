package ch.psi.pshell.data;

import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Provider implementation storing data in text files.
 */
public class ProviderText implements Provider {

    class OutputFile {

        OutputFile(PrintWriter out) {
            this(out, true);
        }

        OutputFile(PrintWriter out, boolean header) {
            this.out = out;
            this.header = header;
        }
        PrintWriter out;
        boolean header;
        boolean composite;
    }

    String root;
    final HashMap<String, OutputFile> openFiles = new HashMap();

    public static final String INFO_ITEM_SEPARATOR = "Item Separator";

    public static final String COMMENT_MARKER = "#";
    public static final String PAGE_MARKER = "page: ";
    public static final String TYPE_MARKER = "type ";
    public static final String DIMS_MARKER = "dims ";
    public static final String LENGTH_SEPARATOR = " * ";

    public static final String ATTR_FILE = "attrs";
    public static final String ATTR_CLASS_MARKER = " # ";
    public static final String ATTR_VALUE_MARKER = " = ";

    static String ITEM_SEPARATOR;
    static String ARRAY_SEPARATOR;
    static String LINE_SEPARATOR;

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

    boolean embeddedAtributes = true;

    public boolean getEmbeddedAtributes() {
        return embeddedAtributes;
    }

    public void setEmbeddedAtributes(boolean value) {
        embeddedAtributes = value;
    }

    boolean orderedAtributes = true;

    public boolean getOrderedAtributes() {
        return orderedAtributes;
    }

    public void setOrderedAtributes(boolean value) {
        orderedAtributes = value;
    }
    
    boolean finalSeparator = true;

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

    @Override
    public String getFileType() {
        return "txt";
    }

    @Override
    public boolean isPacked() {
        return false;
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
            for (String key : openFiles.keySet()) {
                try {
                    openFiles.get(key).out.close();
                } catch (Exception ex) {
                    Logger.getLogger(ProviderText.class.getName()).log(Level.WARNING, null, ex);
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
                    Logger.getLogger(ProviderText.class.getName()).info("Reopening log file: " + path);
                    Path filePath = getFilePath(path);                    
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toString(), true)));
                    of = new OutputFile(out);                    
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
        IO.orderByModified(files);
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

    void addClassInfo(Class type, HashMap<String, Object> ret) {
        Integer size = null;
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

    protected String getItemSeparator(String line) {
        String separator = getItemSeparator();
        try {
            String[] tokens = line.split("[A-Za-z0-9.:$\\-_]");
            String sep = tokens[tokens.length - 1];
            if (!sep.isEmpty()) {
                separator = sep;
            }
        } catch (Exception ex) {
        }
        return separator;
    }

    @Override
    public Map<String, Object> getInfo(String root, String path) throws IOException {
        HashMap<String, Object> ret = new HashMap<>();
        Path filePath = getFilePath(root, path);
        File file = filePath.toFile();
        if (!file.exists()) {
            ret.put(INFO_TYPE, INFO_VAL_TYPE_UNDEFINED);
        } else if (filePath.toFile().isFile()) {
            ret.put(INFO_TYPE, INFO_VAL_TYPE_DATASET);
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
                int index = 0;
                boolean composite = false;
                String separator = getItemSeparator();
                while ((line = br.readLine()) != null) {
                    //line = line.trim();
                    if (!line.isEmpty()) {
                        if (!line.startsWith(COMMENT_MARKER)) {
                            if (!ret.containsKey(INFO_CLASS)) {
                                //If no type record assumes Double
                                ret.put(INFO_CLASS, Double.class.getName());
                                addClassInfo(Double.class, ret);
                            }
                            if (!ret.containsKey(INFO_DIMENSIONS)) {
                                ret.put(INFO_DIMENSIONS, new int[]{0});
                                ret.put(INFO_RANK, 1);
                            }
                            if ((Integer) ret.get(INFO_RANK) != 3) {
                                break;
                            }

                        } else {
                            line = line.substring(1);
                            //In a table dataset first line is the column names                        
                            if (index == 0) {
                                if (line.startsWith(TYPE_MARKER)) {
                                    composite = false;
                                    String dataType = line.substring(TYPE_MARKER.length());
                                    ret.put(INFO_CLASS, dataType);
                                    try {
                                        addClassInfo(Class.forName(dataType), ret);
                                    } catch (Exception ex) {
                                    }
                                } else {
                                    composite = true;
                                    separator = getItemSeparator(line);
                                    ret.put(INFO_ITEM_SEPARATOR, separator);
                                    String[] tokens = line.split(separator);
                                    ret.put(INFO_FIELD_NAMES, tokens);
                                    ret.put(INFO_FIELDS, tokens.length);
                                }
                            }
                            //In a table dataset second line is the column types
                            if ((index == 1) && composite) {
                                
                                ret.put(INFO_DIMENSIONS, new int[]{0});
                                ret.put(INFO_RANK, 1);
                                ret.put(INFO_CLASS, Object.class.getName());
                                ret.put(INFO_DATA_TYPE, INFO_VAL_DATA_TYPE_COMPOUND);
                                String[] types = line.split(separator);
                                int[] lengths = new int[types.length];
                                parseFieldTypes(types, lengths);
                                ret.put(INFO_FIELD_TYPES, types);
                                ret.put(INFO_FIELD_LENGTHS, lengths);
                            } else if (line.startsWith(DIMS_MARKER)) {
                                String aux = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                                int[] dimensions = new int[0];
                                if (!aux.isEmpty()) {
                                    String[] tokens = aux.split(",");
                                    dimensions = new int[tokens.length];
                                    for (int i = 0; i < dimensions.length; i++) {
                                        dimensions[i] = Integer.valueOf(tokens[i].trim());
                                    }
                                }
                                ret.put(INFO_DIMENSIONS, dimensions);
                                ret.put(INFO_RANK, dimensions.length);
                            } else if (line.startsWith(PAGE_MARKER)) {
                                Integer page = Integer.valueOf(line.substring(PAGE_MARKER.length()));
                                if (ret.containsKey(INFO_DIMENSIONS)) {
                                    int[] dimensions = (int[]) ret.get(INFO_DIMENSIONS);
                                    dimensions[getDepthDimension()] = page + 1;
                                    ret.put(INFO_DIMENSIONS, dimensions);
                                }
                            }
                        }
                        index++;
                    }
                }
            }
        } else {
            if (!IO.isSamePath(filePath.toString(), root)) {
                ret.put(INFO_TYPE, INFO_VAL_TYPE_GROUP);
            }
        }
        return ret;
    }

    protected Path getAttibutePath(String root, String path) throws IOException {
        return isGroup(root, path)
                ? Paths.get(root, path, ATTR_FILE)
                : Paths.get(root, path + "." + ATTR_FILE);
    }

    @Override
    public Map<String, Object> getAttributes(String root, String path) throws IOException {
        HashMap<String, Object> ret = new HashMap<>();
        Path filePath = getAttibutePath(root, path);
        File file = filePath.toFile();
        //attr file
        if ((file.exists()) && (file.isFile())) {
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                while ((line = br.readLine()) != null) {
                    parseAttr(line, ret);
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
                            if ((index >= 2) && (line.startsWith(COMMENT_MARKER))) {
                                line = line.substring(1);
                                parseAttr(line, ret);
                            }
                            index++;
                        }
                    }
                }
            }
        }
        return ret;
    }

    private void parseAttr(String line, HashMap<String, Object> ret) {
        String[] tokens = line.split(ATTR_VALUE_MARKER);
        if (line.contains(ATTR_VALUE_MARKER)) {
            if (tokens.length == 2) {
                String[] val = tokens[1].split(ATTR_CLASS_MARKER);
                if (val.length == 2) {
                    Object data = val[0];
                    Class type = String.class;
                    try {
                        type = Class.forName(val[1]);
                        if (type.isPrimitive()) {
                            type = Convert.getWrapperClass(type);
                        }
                        if (type == String.class) {
                            data = val[0];
                        } else if (type == Double.class) {
                            data = Double.valueOf(val[0]);
                        } else if (type == Float.class) {
                            data = Float.valueOf(val[0]);
                        } else if (type == Long.class) {
                            data = Long.valueOf(val[0]);
                        } else if (type == Integer.class) {
                            data = Integer.valueOf(val[0]);
                        } else if (type == Short.class) {
                            data = Short.valueOf(val[0]);
                        } else if (type == Byte.class) {
                            data = Byte.valueOf(val[0]);
                        } else if (type == BigInteger.class) {
                            data = new BigInteger(val[0]);
                        } else if (type == Boolean.class) {
                            data = Boolean.valueOf(val[0]);
                        } else if (type.isArray()) {
                            Class componentType = type.getComponentType();
                            if (type == String[].class) {
                                data = val[0].split(getArraySeparator());
                            } else {
                                if (Convert.isWrapperClass(componentType)) {
                                    componentType = Convert.getPrimitiveClass(type);
                                }
                                if (componentType.isPrimitive()) {
                                    data = Convert.toPrimitiveArray(val[0], getArraySeparator(), componentType);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        data = val[0];
                    }
                    ret.put(tokens[0].trim(), data);
                }
            }
        }
    }

    Class getClass(String name) throws IOException {
        try {
            Class type = Class.forName(name);
            if (type.isPrimitive()) {
                type = Convert.getWrapperClass(type);
            }
            return type;
        } catch (ClassNotFoundException ex) {
            throw new IOException("Invalid data type: " + name);
        }
    }

    @Override
    public DataSlice getData(String root, String path, int index) throws IOException {
        DataSlice ret = null;
        if (!isDataset(root, path)) {
            return null;
        }

        Map<String, Object> info = getInfo(root, path);

        Class type = getClass((String) info.get(INFO_CLASS));
        int[] dimensions = (int[]) info.get(INFO_DIMENSIONS);
        //If not null assumes is heterogeneous table (compound type)       
        Integer fields = (Integer) info.get(INFO_FIELDS);
        boolean composite = fields != null;
        Class[] fieldTypes = composite ? getFieldTypes((String[]) info.get(INFO_FIELD_TYPES)) : null;
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
                                if ((sep != null) && (sep instanceof String) && !((String) sep).trim().isEmpty()) {
                                    separator = (String) sep;
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
                            ? Convert.toPrimitiveArray(vals[i], getArraySeparator(), compType)
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

    protected Class[] getFieldTypes(String[] typeNames) {
        Class[] fieldTypes = new Class[typeNames.length];
        for (int i = 0; i < fieldTypes.length; i++) {
            if ((typeNames == null) || (typeNames.length <= i)) {
                fieldTypes[i] = String.class;
            } else {
                try {
                    fieldTypes[i] = getClass(typeNames[i]);
                } catch (IOException ex) {
                    Logger.getLogger(ProviderText.class.getName()).log(Level.WARNING, null, ex);
                    fieldTypes[i] = Object.class;
                }
            }
        }
        return fieldTypes;
    }
    
    protected void parseFieldTypes(String[] types, int[] lengths) {      
        for (int i = 0; i < types.length; i++) {
            if (types[i].contains(LENGTH_SEPARATOR)) {
                String[] aux = types[i].split(Pattern.quote(LENGTH_SEPARATOR));
                types[i] = aux[0];
                lengths[i] = Integer.valueOf(aux[1].trim());
            }
            types[i] = types[i].trim();
        }    
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
        Paths.get(root, path).toFile().mkdirs();
    }

    final Object attributesLock = new Object();

    @Override
    public void setAttribute(String path, String name, Object value, Class type, boolean unsigned) throws IOException {
        Path filePath = getFilePath(root, path);
        if (filePath.toFile().isFile() && getEmbeddedAtributes()) {
            OutputFile of = null;
            synchronized (openFiles) {
                of = openFiles.get(path);
            }
            if (of == null) {
                throw new IOException("Output file not opened");
            }
            synchronized (of) {
                PrintWriter out = of.out;
                StringBuffer sb = new StringBuffer();
                sb.append(COMMENT_MARKER + name + ATTR_VALUE_MARKER);
                writeElement(sb, value);
                sb.append(ATTR_CLASS_MARKER + type.getName());
                sb.append(getLineSeparator());

                if (!getOrderedAtributes() || of.header) {
                    out.print(sb.toString());
                } else {
                    out.close();
                    int index = 0;
                    try (RandomAccessFile r = new RandomAccessFile(filePath.toFile(), "r")) {
                        if ((r.readLine() != null) && (r.readLine() != null)) { //Read 2 Info lines
                            index = (int) r.getFilePointer();
                        }
                    } catch (Exception ex) {
                    }
                    IO.insert(filePath.toString(), index, sb.toString().getBytes());
                    out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toString(), true)));
                    synchronized (openFiles) {
                        openFiles.put(path, new OutputFile(out, false));
                    }
                }
            }
        } else {
            synchronized (attributesLock) {
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(getAttibutePath(root, path).toString(), true)))) {
                    writer.print(name + ATTR_VALUE_MARKER);
                    writeElement(writer, value);
                    writer.print(ATTR_CLASS_MARKER + type.getName());
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
                of = new OutputFile(out);
                openFiles.put(path, of);
            }
        }
        synchronized (of) {
            of.composite = false;
            out.print(COMMENT_MARKER + TYPE_MARKER + type.getName());
            out.print(getLineSeparator());
            out.print(COMMENT_MARKER + DIMS_MARKER + Arrays.toString(dimensions));
            out.print(getLineSeparator());
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
                of = new OutputFile(out);
                openFiles.put(path, of);
            }
        }
        synchronized (of) {
            of.composite = true;
            out.print(COMMENT_MARKER);
            out.print(String.join(getItemSeparator(), names));
            if (getFinalSeparator() &&  (names.length > 0)) {
                out.append(getItemSeparator());
            }
            out.append(getLineSeparator());

            StringJoiner sj = new StringJoiner(getItemSeparator(), COMMENT_MARKER, getLineSeparator());
            for (int i = 0; i < types.length; i++) {
                String type = types[i].getName();
                if (lengths[i] > 0) {
                    type += LENGTH_SEPARATOR + String.valueOf(lengths[i]);
                }
                sj.add(type);
            }
            out.append(sj.toString());
        }
    }

    void writeElement(Appendable out, Object value) throws IOException {
        if (value == null) {
            out.append("");
        } else if (value.getClass().isArray()) {
            int rank = Arr.getRank(value);
            //
            if (rank > 1) {
                out.append("");
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
            throw new IOException("Output file not opened");
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
        }
    }
}
