package ch.psi.utils;

import ch.psi.pshell.core.JsonSerializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SciCat {
    public enum DatasetType{
        raw,
        derived
    }
    final public static String DATASET_TYPE_RAW =  "raw";
    final public static String DATASET_TYPE_DERIVED =  "derived";
    
    final public static String FILE_LISTING_FILE = "filelisting.txt";
    final public static String JSON_FILE = "metadata.json";
    
    final Map<String, Object> metadata = new HashMap<>();
    final List<String> files = new ArrayList<>();
    
    String name;
    long start;
    long stop;
    
    
    
    public class ScicatConfig extends Config{
        public String creationLocation = "/PSI";
        public String sourceFolder = ".";
        public DatasetType type = DatasetType.raw;
        public String ownerGroup = "";
        public String parameters = "--ingest";
    }
    
    final ScicatConfig config; 
    
    public SciCat(String configurationFile){
        config = new ScicatConfig();
        try {            
            config.load(configurationFile);
        } catch (IOException ex) {
            Logger.getLogger(SciCat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setInfo(String name, long start, long stop){
        this.name = name;
        this.start = start;
        this.stop = stop;        
    }
    
    
    public ScicatConfig getConfig(){
        return config;
    }
    
    public void setCreationLocation(String value) throws IOException{
        config.creationLocation = value;
        config.save();
    }
    
    public void setDatasetType(DatasetType value) throws IOException{
        config.type = value;
        config.save();
    }
    
    public void setSourceFolder(String value) throws IOException{
        config.sourceFolder = value;
        config.save();
    }    
    
    public void setOwnerGroup(String value) throws IOException{
        config.ownerGroup = value;
        config.save();
    }

    public void setParameters(String value) throws IOException{
        config.parameters = value;
        config.save();
    }


    
    public Map<String, Object> getMetadata(){
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata){
        this.metadata.clear();
        this.metadata.putAll(metadata);
    }

    public void setFiles(List<String> files){
        this.files.clear();
        this.files.addAll(files);
    }
    
    

    public Map<String, Object> getJsonMap(){
        Map ret = new HashMap<>();
        ret.putAll(metadata);
        ret.put("creationLocation", config.creationLocation);
        ret.put("sourceFolder", config.sourceFolder);
        ret.put("type", config.type.toString());
        ret.put("ownerGroup", config.ownerGroup);
        return ret;
    }    
    
    public String getJson() throws IOException{
        return JsonSerializer.encode(getJsonMap());
    }
    
    public String getFileListing() throws IOException { 
        if (files.size()==0){
            throw new IOException("Empty data file list");
        }
        return String.join("\n",files);
    }
    
    
    public String ingest() throws IOException, InterruptedException { 
        String listing = getFileListing();
        String json = getJson();
        Files.writeString(Paths.get(".", FILE_LISTING_FILE), listing);
        Files.writeString(Paths.get(".", JSON_FILE), json);
        
        List<String> pars= new ArrayList<>();
        pars.add("datasetIngestor");
        for (String par: config.parameters.split(" ")){
            if (!par.isBlank()){
                pars.add(par.trim());
            }
        }        
        pars.add("metadata.json");
        pars.add("filelisting.txt");
        
        ProcessBuilder pb = new ProcessBuilder(pars);
        Process p = pb.start();
        p.waitFor();      
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line).append(Sys.getLineSeparator());
        }
        String error = builder.toString();
        if (!error.isBlank()){
            throw new IOException(error);
        }
        
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        builder = new StringBuilder();
        while ( (line = reader.readLine()) != null) {
            builder.append(line).append(Sys.getLineSeparator());
        }
        String result = builder.toString(); 
        return result;        
    }

    
}
