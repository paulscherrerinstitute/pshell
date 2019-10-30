package ch.psi.pshell.data;

import ch.psi.utils.Arr;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Provider implementation storing data in CSV files.
 */
public class ProviderCSV extends ProviderText {
    
    static String ITEM_SEPARATOR;

    public static void setDefaultItemSeparator(String str) {
        ITEM_SEPARATOR = str;
    }

    public static String getDefaultItemSeparator() {
        if (ITEM_SEPARATOR == null) {
            ITEM_SEPARATOR = "; ";
        }
        return ITEM_SEPARATOR;
    }
    
    public ProviderCSV() {
        super.setEmbeddedAtributes(false);
        setItemSeparator(getDefaultItemSeparator());
        setFinalSeparator(false); //By default don't use final separator
    }
    
    @Override
    public String getFileType() {
        return "csv";
    }    

    @Override
    public void setEmbeddedAtributes(boolean value) {
        if (value) {
            throw new IllegalArgumentException("Cannot set embedded attributes in CSV format");
        }
    }

    @Override
    public Map<String, Object> getInfo(String root, String path) throws IOException {
        HashMap<String, Object> ret = new HashMap<>();
        Path filePath = getFilePath(root, path);
        File file = filePath.toFile();
        if (!file.exists() || !filePath.toFile().isFile()) {
            return super.getInfo(root, path);
        }
        ret.put(INFO_TYPE, INFO_VAL_TYPE_DATASET);
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    String separator = getItemSeparator(line);
                    ret.put(INFO_ITEM_SEPARATOR, separator);
                    String[] tokens = line.split(separator);
                    ret.put(INFO_FIELD_NAMES, tokens);
                    ret.put(INFO_FIELDS, tokens.length);
                    ret.put(INFO_DIMENSIONS, new int[]{0});
                    ret.put(INFO_RANK, 1);
                    ret.put(INFO_CLASS, Object.class.getName());
                    ret.put(INFO_DATA_TYPE, INFO_VAL_DATA_TYPE_COMPOUND);
                }
                break;
            }
        }

        return ret;
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
        String separator = getItemSeparator();
         Object sep =  info.get(INFO_ITEM_SEPARATOR);
         if ((sep!= null) && (sep instanceof String ) && !((String)sep).trim().isEmpty()){
             separator = (String)sep;
         }
        
        
        String[] typeNames = getAttributes(root, path).get(INFO_FIELD_TYPES).toString().split(separator);
        int[] lengths = new int[typeNames.length];
        parseFieldTypes(typeNames, lengths);       
        Class[] fieldTypes = getFieldTypes(typeNames);
        Path filePath = getFilePath(root, path);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            int rank = dimensions.length;
            ArrayList data = new ArrayList();
            String line;
            int lineIndex = 0;
            while ((line = br.readLine()) != null) {
                if (lineIndex == 0){
                    //Column name
                } else if (!line.isEmpty()) {
                    Object[] record = getRecord(line, separator, fieldTypes);
                    data.add(record);  
                }
                lineIndex++;
            }
            ret = new DataSlice(root, path, dimensions, data.toArray(new Object[0][0]), index, false);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Invalid file format");
        }
        return ret;
    }

    @Override
    public void createDataset(String path, Class type, int[] dimensions, boolean unsigned, Map features) throws IOException {
        if (dimensions.length == 1){
            createDataset(path, new String[]{"Value"}, new Class[]{type}, dimensions, features);
            return;
        }
        throw new UnsupportedOperationException("CSV format only support one dimensional datasets");
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
            out.print(String.join(getItemSeparator(), names));
            if (getFinalSeparator() &&  (names.length > 0)) {
                out.append(getItemSeparator());
            }
            out.append(getLineSeparator());
        }
        StringJoiner sj = new StringJoiner(getItemSeparator(), "", getLineSeparator());
        for (int i = 0; i < types.length; i++) {
            String type = types[i].getName();
            if (lengths[i] > 0) {
                type += LENGTH_SEPARATOR + String.valueOf(lengths[i]);
            }
            sj.add(type);
        }      
        setAttribute(path, INFO_FIELD_TYPES, sj.toString().trim(), String.class, false);
    }

    @Override
    public void setItem(String path, Object data, Class type, int index) throws IOException {
        int[] shape = Arr.getShape(data);
        int rank = shape.length;
        if (rank > 1) {
            throw new IllegalArgumentException("Cannot set data in CSV format with rank: " + rank);
        }
        super.setItem(path, data, type, index);
    }

}
