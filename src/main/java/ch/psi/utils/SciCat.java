package ch.psi.utils;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.core.SessionManager;
import ch.psi.pshell.core.SessionManager.MetadataType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SciCat {

    public enum DatasetType {
        raw,
        derived
    }
    
    final public static String DEFAULT_PROPERTIES_FILE = "{config}/scicat.properties";
    final public static String FILE_LISTING_FILE = "{context}/filelisting.txt";
    final public static String JSON_FILE = "{context}/metadata.json";
    
    final public static String DATASET_TYPE_RAW = "raw";
    final public static String DATASET_TYPE_DERIVED = "derived";

    //final public static String FILE_LISTING_FILE = "filelisting.txt";
    //final public static String JSON_FILE = "metadata.json";

    static String DEFAULT_PARAMETERS = "-ingest -allowexistingsource -noninteractive -autoarchive";     
    
    final Map<String, Object> metadata = new HashMap<>();
    final Map<String, Object> info = new HashMap<>();
    final List<String> files = new ArrayList<>();
    int id;

    public static final Map<String, MetadataType> metadataFields;
    
    public enum Environment{
        dev,
        test,
        prod;
        public static Environment getFromArguments(String args){
            Environment ret = prod;
            if (args!=null){
                for (String arg : args.split(" ")){
                    if (arg.equals(dev.getParameter())){
                       ret =  dev;
                    }
                    if (arg.equals(test.getParameter())){
                       ret =  test;
                    }
                }
            }
            return ret;
        }
        public String getParameter(){
            switch (this){
                case dev:
                    return "-devenv";
                case test:
                    return "-testenv";
                default:
                case prod:
                    return "";
            }
        }              
    }
    

    static {
        Map<String, MetadataType> map = new HashMap<>();
        map.put("pid", MetadataType.String);
        map.put("owner", MetadataType.String);
        map.put("ownerEmail", MetadataType.String);
        map.put("orcidOfOwner", MetadataType.String);
        map.put("contactEmail", MetadataType.String);
        map.put("datasetName", MetadataType.String);
        map.put("sourceFolder", MetadataType.String);
        map.put("size", MetadataType.Integer);
        map.put("packedSize", MetadataType.Integer);
        map.put("creationTime", MetadataType.String);
        map.put("type", MetadataType.String);
        map.put("validationStatus", MetadataType.String);
        map.put("keywords", MetadataType.List);
        map.put("description", MetadataType.String);
        map.put("classification", MetadataType.String);
        map.put("license", MetadataType.String);
        map.put("version", MetadataType.String);
        map.put("doi", MetadataType.String);
        map.put("isPublished", MetadataType.Boolean);
        map.put("ownerGroup", MetadataType.String);
        map.put("accessGroups", MetadataType.List);
        map.put("principalInvestigator", MetadataType.String);
        map.put("endTime	date", MetadataType.String);
        map.put("creationLocation", MetadataType.String);
        map.put("dataFormat", MetadataType.String);
        //These are derived metadata parameters
        //map.put("investigator", MetadataType.String);    
        //map.put("inputDatasets", MetadataType.List);
        //map.put("usedSoftware", MetadataType.String);
        //map.put("jobParameters", MetadataType.Map);
        //map.put("jobLogData", MetadataType.String);
        map.put("scientificMetadata", MetadataType.Map);
        metadataFields = Collections.unmodifiableMap(map);
    }

    public class ScicatConfig extends Config {

        public String creationLocation = "/PSI";
        public String sourceFolder = ".";
        public DatasetType type = DatasetType.raw;
        public String ownerGroup = "";
        public String principalInvestigator = "";
        public String parameters = DEFAULT_PARAMETERS;
        public Environment environment = Environment.prod;
        public String prodParameters = Environment.prod.getParameter() + " -user slssim:slssim";
        public String testParameters = Environment.test.getParameter() + " -user slssim:slssim";
        public String devParameters = Environment.dev.getParameter() + " -user slssim:slssim";    
        
        public Environment getEnvironment(){
            if (environment == null){
                environment = Environment.prod;
            }
            return environment;
        }            
        
        public String getEnvironmentParameters(){
            switch (getEnvironment()){
                 case dev:
                    return devParameters;
                case test:
                    return testParameters;
                default:
                case prod:
                    return prodParameters;
            }
        }
        public String getParameters(){
            return getEnvironmentParameters() + " " + parameters;
        }
    }

    final ScicatConfig config;
    
    public SciCat() {
        this(Context.getInstance().getSetup().expandPath(DEFAULT_PROPERTIES_FILE));
    }
            
    public SciCat(String configurationFile) {
        config = new ScicatConfig();
        try {
            config.load(configurationFile);
        } catch (IOException ex) {
            Logger.getLogger(SciCat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setInfo(int id, Map<String, Object> info) {
        this.id = id;
        this.info.clear();
        this.info.putAll(info);
    }

    public ScicatConfig getConfig() {
        return config;
    }

    public void setCreationLocation(String value) throws IOException {
        config.creationLocation = value;
        config.save();
    }

    public void setDatasetType(DatasetType value) throws IOException {
        config.type = value;
        config.save();
    }

    public void setSourceFolder(String value) throws IOException {
        config.sourceFolder = value;
        config.save();
    }

    public void setOwnerGroup(String value) throws IOException {
        config.ownerGroup = value;
        config.save();
    }

    public void setPrincipalInvestigator(String value) throws IOException {
        config.principalInvestigator = value;
        config.save();
    }

    public void setParameters(String value) throws IOException {
        config.parameters = value;
        config.save();
    }

    public void setEnvironment(Environment value) throws IOException {
        config.environment = value;
        config.save();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata.clear();
        this.metadata.putAll(metadata);
    }

    public void setFiles(List<String> files) {
        this.files.clear();
        this.files.addAll(files);
    }
    
    public String getDefaultName(){
        String name = info.getOrDefault("name", "").toString();
        return id + "_" + name;
    }
    
    public Environment getEnvironment() throws IOException {
        return config.getEnvironment();        
    }    
    

    public String getDefaultDesciption(){
        String name = info.getOrDefault("name", "").toString();
        return name;
    }    

    String datasetName;
    public Map<String, Object> getJsonMap() throws IOException {
        Map ret = new HashMap<>();
        Map<String, Object> scientificMetadata = new HashMap<>();        
        String user = Sys.getUserName();        
        String pgroup = null;   
        datasetName = null;
        boolean isEaccount=false;
        try{
             if (user.startsWith("e")){
                 Integer.valueOf(user.substring(1));
                 pgroup = "p" + user.substring(1);
                 isEaccount = true;
             }
        } catch (Exception ex){            
        }
                

        for (String key : metadata.keySet()) {
            if (metadataFields.containsKey(key)) {
                ret.put(key, metadata.get(key));
            } else {
                scientificMetadata.put(key, metadata.get(key));
            }
        }

        if (!ret.containsKey("creationLocation")) {
            if (config.creationLocation.isBlank()){
                throw new IOException("The creation location must be defined");
            }            
            ret.put("creationLocation", config.creationLocation);
        }
        if (!ret.containsKey("sourceFolder")) {
            ret.put("sourceFolder", config.sourceFolder);
        }
        if (!ret.containsKey("type")) {
            ret.put("type", config.type.toString());
        }
        if (!ret.containsKey("ownerGroup")) {
            if (!config.ownerGroup.isBlank()){
                ret.put("ownerGroup", config.ownerGroup);                    
            }else {
                if (isEaccount){
                    ret.put("ownerGroup", pgroup);
                } else {
                    throw new IOException("If not running on an e-accunt then the owner group must be defined");
                }
            }                
        }
        if (!ret.containsKey("principalInvestigator")) {            
            if (!config.principalInvestigator.isBlank()){
                ret.put("principalInvestigator", config.principalInvestigator);
            }
        }
        if (!ret.containsKey("datasetName")) {
            ret.put("datasetName", getDefaultName());
        }
        datasetName = Str.toString(ret.get("datasetName"));
        if (!ret.containsKey("description")) {
            ret.put("description", getDefaultDesciption());
        }
        if (!ret.containsKey("isPublished")) {
            ret.put("isPublished", false);
        }
        if (!ret.containsKey("dataFormat")) {
            ret.put("dataFormat", info.getOrDefault("format", ""));
        }

        if (!ret.containsKey("scientificMetadata")) {
            ret.put("scientificMetadata", scientificMetadata);
        } else {
            ((Map<String, Object>) ret.get("scientificMetadata")).putAll(scientificMetadata);
        }
        if (!ret.containsKey("endTime")) {
            Long stopTimestamp = (Long) info.getOrDefault("stop", System.currentTimeMillis());
            ret.put("endTime", getDate(stopTimestamp));

        }

        return ret;
    }

    public static String getDate(long timestamp) {
        return Chrono.getTimeStr(timestamp, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    public String getJson() throws IOException {
        return JsonSerializer.encode(getJsonMap());
    }

    public String getFileListing() throws IOException {
        if (files.size() == 0) {
            throw new IOException("Empty data file list");
        }
        return String.join("\n", files);
    }
    
    public static class IngestOutput {
        IngestOutput(boolean success, String output, String datasetId, String datasetName){
            this.success = success;
            this.output = output;
            this.datasetId = datasetId;
            this.datasetName = datasetName;
        }
        public final boolean success;
        public final String output;
        public final String datasetId;
        public final String datasetName;
    }

    public synchronized IngestOutput ingest() throws IOException, InterruptedException {
        String listing = getFileListing();
        String json = getJson();
        //Files.writeString(Paths.get(".", FILE_LISTING_FILE), listing);
        //Files.writeString(Paths.get(".", JSON_FILE), json);
        String jsonFile = Context.getInstance().getSetup().expandPath(JSON_FILE);
        String fileListingFile =   Context.getInstance().getSetup().expandPath(FILE_LISTING_FILE);
        Files.writeString(Paths.get(jsonFile), json);
        Files.writeString(Paths.get(fileListingFile), listing);

        List<String> pars = new ArrayList<>();
        pars.add("datasetIngestor");
        for (String par : config.getParameters().split(" ")) {
            if (!par.isBlank()) {
                pars.add(par.trim());
            }
        }               
        //pars.add("metadata.json");
        //pars.add("filelisting.txt");
        pars.add(jsonFile);
        pars.add(fileListingFile);        

        String[] ret = ch.psi.utils.Processing.run(pars);        
        String out = ret[0];
        String err = ret[1];
        String datasetId = out.trim();
        return new IngestOutput(!datasetId.isEmpty(), err, datasetId, datasetName);
    }
        
    public IngestOutput ingest(int sessionId, Map<String, Object> metadata) throws IOException, InterruptedException {
        SessionManager manager = Context.getInstance().getSessionManager();
        Map<String, Object> info =manager.getInfo(sessionId);           
        Map<String,String> ingested = info.containsKey("ingested") ? (Map<String,String>)info.get("ingested") : new HashMap<>();
        Environment environment = getEnvironment();
        String env = environment.toString();
        for (String ingestedEnv : ingested.keySet()){
            if (ingestedEnv.equals(env)){
                String id = Str.toString(ingested.get(env));                
                throw new IOException("This session was already ingested as dataset " + id);
            }
        }                    
        setSourceFolder(manager.getRoot(sessionId));
        setDatasetType(SciCat.DatasetType.raw);

        Map<String, Object> pars = manager.getMetadata(sessionId);
        if (metadata!=null){
             pars.putAll(metadata);
        }                
        setMetadata(pars);
        setFiles(manager.getFileListAtRoot(sessionId));        
        setInfo(sessionId, info);            
        IngestOutput result = ingest();
        if (result.success){
            ingested.put(env, result.datasetId);
            manager.setInfo(sessionId, "ingested", ingested);
            manager.setState(sessionId, SessionManager.STATE_ARCHIVED);
        }
        return result;
    }
}
