package ch.psi.pshell.data;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.utils.Convert;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 */
public class FormatFDA extends FormatText{
    public static final String ITEM_SEPARATOR = "\t";
    public static final String INFO_FIELD_DIMENSIONS= "Field Dimensions";
    public static boolean ADD_ATTRIBUTE_FILE_TIMESTAMP = true;
    
    public String getId() {
        return "fda";
    }    
     
    public static String getDefaultItemSeparator() {
        return ITEM_SEPARATOR;
    }
    
    public FormatFDA(){
        super();
        this.setHeaderVersion(1);
        this.setItemSeparator(getDefaultItemSeparator());
    }
    @Override
    protected Path getAttributePath(String root, String path) throws IOException {
        if (ADD_ATTRIBUTE_FILE_TIMESTAMP && Layout.isFlatStorage()){
            ExecutionParameters pars = Context.getExecutionPars();
            if (pars != null) {
                if (isGroup(root, path)) {
                        return Paths.get(root, path, Setup.expandPath(
                            "{date}_{time}_{name}." + ATTR_FILE, pars.getStart()));
                }
            }
        }
        return super.getAttributePath(root, path);
    }
        
    @Override
    public DataSlice getData(String root, String path, int page) throws IOException {
        if (!matches( root, path, null, this)){
            return super.getData(root, path, page);
        } 
        
        DataSlice ret = null;
        if (!isDataset(root, path)) {
            return null;
        }

        Map<String, Object> info = getInfo(root, path);
        Integer fields = (Integer) info.get(INFO_FIELDS);
        String[] typeNames = (String[]) info.get(INFO_FIELD_TYPES);
        Path filePath = getFilePath(root, path);

        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            ArrayList data = new ArrayList();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    if (!line.startsWith(COMMENT_MARKER)) {
                        String[] vals = line.split(getItemSeparator());
                        if (fields != null) {
                            Object[] record = new Object[fields];
                            for (int i = 0; i < fields; i++) {
                                if (typeNames[i].equals(double[].class.getName())) {
                                    record[i] = Convert.toPrimitiveArray(vals[i], getArraySeparator(), double.class);                                            
                                } else {
                                    record[i] = Double.valueOf(vals[i]);
                                }
                            }
                            data.add(record);
                        } 
                    } 
                }
            }
            Object array = data.toArray(new Object[0][0]);
            return  new DataSlice(root, path, (int[]) info.get(INFO_DIMENSIONS), array, page, false);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Invalid file format");
        }
    }
    
    @Override
    public Map<String, Object> getInfo(String root, String path) throws IOException {
        Map<String, Object> ret = super.getInfo(root, path);
        if (!matches( root, path, null, this)){
            return ret;
        } 
        
        Integer fields = (Integer) ret.get(INFO_FIELDS);
        if (fields!=null){
            Path filePath = getFilePath(root, path);
            String[] typeNames = new String[fields];
            int[] fieldLengths = new int[fields];
            if (filePath.toFile().isFile()) {
                try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
                    br.readLine();                        
                    String str = br.readLine();
                    try{
                        if (str.startsWith(COMMENT_MARKER)) { 
                            str = str.substring(1);
                            String[] vals = str.split(getItemSeparator());                        
                            ret.put(INFO_FIELD_DIMENSIONS, Convert.toPrimitiveArray(vals, int.class));
                        }
                    } catch (Exception ex){                        
                    }
                    
                    String line = br.readLine();
                    if (!line.isEmpty()) {
                        String[] vals = line.split(getItemSeparator());
                        if (fields != null) {
                            Object[] record = new Object[fields];
                            for (int i = 0; i < fields; i++) {
                                typeNames[i] = vals[i].contains(getArraySeparator()) ? double[].class.getName() : Double.class.getName();     
                                if (vals[i].contains(getArraySeparator())){
                                    if (vals[i].startsWith(getArraySeparator())){
                                        vals[i] = vals[i].substring(getArraySeparator().length());
                                    }
                                    fieldLengths[i] = vals[i].split(getArraySeparator()).length;
                                } else {
                                    fieldLengths[i] = 0;
                                }
                            }
                        }
                    }
                    ret.put(INFO_FIELD_TYPES, typeNames);
                    ret.put(INFO_FIELD_LENGTHS, fieldLengths);
                } catch (Exception ex){                
                }
            }
        }
        return ret;
    }
    
    
    public static boolean matches(Path filePath) {
        return FormatFDA.matches(filePath, null);
    }
    
    public static boolean matches(Path filePath, Format p) {
        String separator = (p==null) ? ITEM_SEPARATOR: ((FormatText)p).getItemSeparator();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String first = br.readLine(); 
            String second = br.readLine();
            String third = br.readLine();
            if ((first.startsWith("#")) && (second.startsWith("#")) && (!third.startsWith("#"))){
                try{
                    //FDA serialization
                    for (String token: second.substring(1).split(separator)){
                        Integer.valueOf(token.trim());
                    }
                    return true;
                } catch (Exception ex){
                    //FDA layout
                    separator = FormatText.getItemSeparator(second);
                    //Header V1
                    for (String token: second.substring(1).split(separator)){                        
                        if (!token.startsWith("[")){ //If not an array
                            Class.forName(token.trim());    //Must be a class name
                        }
                    }
                    return true;
                }
            }
        } catch (Exception ex) {            
        }
        return false;
    }
    
    public static boolean matches(String root, String path) throws IOException {
        return matches(root, path, Context.getDataManager(), null);
    }
    
    static boolean matches(String root, String path, DataManager dm, Format p) throws IOException {
        if (dm!=null){
            if (!dm.isDataset(root, path)) {
                return false;
            }        
            if (p==null){
                p = dm.getFormat();
            } 
        }       
        if (p instanceof FormatText providerText){
            Path filePath = providerText.getFilePath(root, path);
            return FormatFDA.matches(filePath, p);
        }
        return false;
    }    
    
}
