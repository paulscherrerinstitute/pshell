package ch.psi.pshell.core;

import ch.psi.pshell.core.Configuration.DataTransferMode;
import ch.psi.pshell.data.RSync;
import ch.psi.pshell.swing.MetadataEditor;
import ch.psi.utils.Arr;
import ch.psi.utils.Folder;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.OrderedProperties;
import ch.psi.utils.Str;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class SessionManager extends ObservableBase<SessionManager.SessionManagerListener> {

    public enum ChangeType {
        STATE,
        INFO,
        METADATA
    }

    public enum MetadataType {
        String,
        Integer,
        Double,
        Boolean,
        List,
        Map
    }

    public static interface SessionManagerListener {

        void onChange(ChangeType type);
    }

    void triggerChanged(ChangeType type) {
        for (SessionManagerListener listener : getListeners()) {
            try {
                listener.onChange(type);
            } catch (Exception ex) {
                Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    final static String CURRENT_SESSION = "CurrentSession";
    final static String SESSION_COUNTER = "SessionCounter";

    final static String INFO_FILE = "info.json";
    final static String METADATA_FILE = "metadata.json";
    
    public final static String STATE_STARTED = "started";
    public final static String STATE_PAUSED = "paused";
    public final static String STATE_COMPLETED = "completed";
    public final static String STATE_RUNNING = "running";
    public final static String STATE_TRANSFERING = "transfering";
    public final static String STATE_TRANSFERRED = "transfered";
    public final static String STATE_ERROR = "error";
    
    public final static String UNNAMED_SESSION_NAME = "unnamed";
    public final static String UNDEFINED_SESSION_NAME = "unknown";

    boolean firstTransfer = true;
    
    int getCurrentCounter() {
        try {
            return Integer.valueOf(Context.getInstance().getVariable(SESSION_COUNTER));
        } catch (Exception ex) {
            return 0;
        }

    }

    public int start() throws IOException {
        return start(null);
    }

    public int start(String name) throws IOException {
        return start(name, null);
    }
    
    public int start(String name, Map<String, Object> metadata) throws IOException {            
        return start(name, metadata, null);
    }

    public int start(String name, Map<String, Object> metadata, String root) throws IOException {
        stop();
        if ((name == null) || name.isBlank()){
            name = UNNAMED_SESSION_NAME;
        }
        if (metadata == null){
            metadata = getMetadataDefault();
        }
        if ((root == null) || root.isBlank()){
            root = Context.getInstance().getSetup().getDataPath();
        }
        try{
            root = Paths.get(root).toRealPath().toString();
        } catch (Exception ex){
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }
        int sessionId = getCurrentCounter();
        sessionId++;
        Context.getInstance().setVariable(SESSION_COUNTER, sessionId);
        Context.getInstance().setVariable(CURRENT_SESSION, name);
        Path path = getCurrentPath();
        path.toFile().mkdirs();
        Map info = new HashMap<String, Object>();
        info.put("name", name);
        info.put("start", getTimestamp());
        info.put("state", STATE_STARTED);
        info.put("root", root);
        info.put("format", Context.getInstance().getConfig().dataProvider);
        info.put("layout", Context.getInstance().getConfig().dataLayout);
        List runs = new ArrayList();
        info.put("runs", runs);
        setInfo(info);
        setMetadata(metadata);
        triggerChanged(ChangeType.STATE);
        Context.getInstance().getCommandManager().onSessionStarted(sessionId);        
        return sessionId;
    }

    public void stop() throws IOException {
        if (isStarted()) {
            int sessionId = getCurrentId();
            Context.getInstance().getCommandManager().onSessionFinished(sessionId);        
            setInfo("state", STATE_COMPLETED);
            setInfo("stop", getTimestamp());
            Context.getInstance().setVariable(CURRENT_SESSION, null);
            triggerChanged(ChangeType.STATE);            
        }
    }
    public void pause() throws IOException {
        if (isStarted() && !isPaused()) {
            setState(STATE_PAUSED);
        }
    }

    public void resume() throws IOException {
        if (isPaused()){
            setState(STATE_STARTED);
        }
    }
    
    public void cancel() throws IOException {
        if (isStarted() && (getNumberRuns()==0)) {
            int sessionId = getCurrentId();
            Path path = getSessionPath(getCurrentId());
            IO.deleteRecursive(path.toFile());
            Context.getInstance().setVariable(SESSION_COUNTER, sessionId-1);
            Context.getInstance().setVariable(CURRENT_SESSION, null);
            triggerChanged(ChangeType.STATE);            
            
        }
    }    
    
    
    public boolean isPaused() throws IOException {
        if (isStarted()) {
            return getState().equals(STATE_PAUSED);
        }
        return false;
    }    

    public int getCurrentId() {
        try {
            if (isStarted()) {
                return getCurrentCounter();
            }
        } catch (Exception ex) {
        }
        return 0;
    }

    public String getCurrentName() {
        try {
            if (isStarted()) {
                String ret = Context.getInstance().getVariable(CURRENT_SESSION);
                if (ret.isBlank()){
                    return UNNAMED_SESSION_NAME;                
                }
                return ret;
            }
        } catch (Exception ex) {
        }
        return UNDEFINED_SESSION_NAME;
    }

    public Path getCurrentPath() throws IOException {
        assertStarted();
        return getSessionPath(getCurrentId());
    }

    public Path getSessionPath(int id) throws IOException {
        return Paths.get(Context.getInstance().getSetup().getUserSessionsPath(), String.valueOf(id));
    }

    public List<Integer> getIDs() {
        List<Integer> ret = new ArrayList<>();
        try {
            File folder = new File(Context.getInstance().getSetup().getUserSessionsPath());
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    String name = file.getName();
                    try {
                        ret.add(Integer.valueOf(name));
                    } catch (Exception ex) {
                    }
                }
            }

        } catch (Exception ex) {
        }
        Collections.sort(ret);
        return ret;
    }

    public String getName(int id) {
        try {
            return (String) getInfo(id).get("name");
        } catch (Exception ex) {
            return "";
        }
    }
    public String getName() throws IOException {
        assertStarted();
        return getName(getCurrentId());
    }           

    public long getStart(int id) throws IOException {
        return (Long) getInfo(id).get("start");
    }

    public long getStart() throws IOException {
        assertStarted();
        return getStart(getCurrentId());
    }
    
    public long getStop(int id) throws IOException {
         return (Long) getInfo(id).get("stop");
    }

    public String getRoot(int id) throws IOException {
        return (String) getInfo(id).getOrDefault("root", Context.getInstance().getSetup().getDataPath());
    }

    public String getRoot() throws IOException {
        assertStarted();
        return getRoot(getCurrentId());
    }    

    Object getTimestamp() {
        //return Chrono.getTimeStr(System.currentTimeMillis(), "YYYY-MM-dd HH:mm:ss.SSS");
        return System.currentTimeMillis();
    }

    String transferData(File from) throws Exception {
        IO.assertExists(from);
        String path = Context.getInstance().config.getDataTransferPath();
        String user = Context.getInstance().config.getDataTransferUser();
        if (path.isBlank()) {
            throw new IOException("Undefined target folder");
        }
        if (user.isBlank()) {
            path = Context.getInstance().setup.expandPath(path);
            Path to = Paths.get(path, from.getName());
            //Direct transfer        
            new File(path).mkdirs();
            if (from.isDirectory()) {
                Folder folder = new Folder(from);
                folder.copy(to.toFile().getCanonicalPath(), false);
            } else {
                IO.copy(from.getCanonicalPath(), to.toFile().getCanonicalPath());
            }
            if (Context.getInstance().config.dataTransferMode == DataTransferMode.Move) {
                IO.deleteRecursive(from);
            }
            return to.toFile().getCanonicalPath();
        } else {
            //Should not expand ~
            if (path.startsWith("~")) {
                path = path.replaceFirst("~", "&");
            }
            path = Context.getInstance().setup.expandPath(path);
            if (path.startsWith("&")) {
                path = path.replaceFirst("&", "~");
            }
            Path to = Paths.get(path, from.getName());
            boolean move = (Context.getInstance().config.dataTransferMode == DataTransferMode.Move);
            String ret = RSync.sync(user, from.getCanonicalPath(), path, move);
            //Thread.sleep(5000);
            return to.toString();
        }
    }

    private File currentDataPath;
    boolean saveRun;

    void onChangeDataPath(File dataPath) {
        final int sessionId = getCurrentId();        
        final boolean updateRun = saveRun;
        
        boolean transfer = (Context.getInstance().config.dataTransferMode != DataTransferMode.Off);
        try {
            if (isStarted()) {
                if (dataPath != null) {
                    saveRun = !isPaused();
                    if (saveRun){
                        Map<String, Object> run = new HashMap<>();
                        run.put("start", getTimestamp());                        
                        run.put("data", getFileName(dataPath.getCanonicalPath(), true));
                        run.put("state", STATE_RUNNING);
                        addRun(run);
                    }
                } else {
                    if (saveRun){
                        updateLastRun("stop", getTimestamp());
                        updateLastRun("state", STATE_COMPLETED);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }

        try {
            if ((dataPath == null) && (currentDataPath != null)) {
                if (transfer) {
                    if (isStarted() && updateRun) {
                        updateLastRun("state", STATE_TRANSFERING);
                    }
                    final File origin = currentDataPath;
                    new Thread(() -> {
                        try {
                            String dest = transferData(origin);
                            if (isStarted() && updateRun) {                                
                                updateLastRun(sessionId, "data", getFileName(dest, true));
                                updateLastRun(sessionId, "state", STATE_TRANSFERRED);
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
                            try {
                                if (isStarted() && updateRun) {
                                    updateLastRun(sessionId, "state", STATE_ERROR + ": " + ex.getMessage());
                                }
                            } catch (IOException ex1) {
                                Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                        }
                    }, "Data transfer task: " + currentDataPath.getName()).start();
                }
            }
            currentDataPath = dataPath;
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }

    }

    public Set<Map.Entry<Object, Object>> getMetadataDefinition() {
        OrderedProperties properties = new OrderedProperties();
        try (FileInputStream in = new FileInputStream(Context.getInstance().getSetup().getSessionMetadataDefinitionFile())) {
            properties.load(in);
        } catch (Exception ex) {
            Logger.getLogger(MetadataEditor.class.getName()).log(Level.WARNING, null, ex);
        }
        
        return properties.entrySet();
    }

    public MetadataType getMetadataType(String key) {
        try {
            for (Map.Entry entry : getMetadataDefinition()) {
                if (entry.getKey().equals(key)) {
                    String val = String.valueOf(entry.getValue()).trim();
                    if (val.contains(";")){
                        val = val.substring(0, val.indexOf(";")).trim();
                    }
                    return MetadataType.valueOf(val);
                }
            }
        } catch (Exception ex) {
        }
        return MetadataType.String;
    }

    public static Object getDefaultValue(MetadataType type) {
        switch (type){
            case List: return "[]";
            case Map: return "{}";
            default: return "";
        }            
    }
    
    public static Object getDefaultValue(String type) {
        try {
            return getDefaultValue(MetadataType.valueOf(type));
        } catch (Exception ex) {
        }
        return "";     
    }    
    
    public Object getMetadataDefault(String key) {
        try {
            for (Map.Entry entry : getMetadataDefinition()) {
                if (entry.getKey().equals(key)) {
                    return getMetadataDefault(entry);
                }
            }
        } catch (Exception ex) {
        }
        return getDefaultValue(getMetadataType(key));
    }
    
    public Object getMetadataDefault(Map.Entry entry) {
            String val = String.valueOf(entry.getValue()).trim();
            if (val.contains(";")){
                return  val.substring(val.indexOf(";") + 1).trim();
            }         
            return "";
    }
    
    public Map<String, Object> getMetadataDefault() {
        Map<String, Object> ret = new HashMap();
        for (Map.Entry entry : getMetadataDefinition()) {
            ret.put(entry.getKey().toString(),getMetadataDefault(entry));
        }    
        return ret;
    }
    
    public void onMetadataDefinitionChanged() {
        try {
            if (isStarted()) {
                Set<Map.Entry<Object, Object>> metadataDefinition = getMetadataDefinition();
                Map<String, Object> metadata = getMetadata();
                Map<String, Object> newMetadata = new HashMap<>();
                for (Map.Entry<Object, Object> entry : metadataDefinition) {
                    String val = String.valueOf(entry.getValue()).trim();
                    Object def = getMetadataDefault(entry);
                    newMetadata.put(Str.toString(entry.getKey()), metadata.getOrDefault(entry.getKey(), def));
                }
                setMetadata(newMetadata);
            }
        } catch (Exception ex) {
            Logger.getLogger(MetadataEditor.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    void setInfo(Map<String, Object> info) throws IOException {
        assertStarted();
        setInfo(getCurrentId(), info);
    }

    void setInfo(int id, Map<String, Object> info) throws IOException {
        String json = JsonSerializer.encode(info);
        Files.writeString(Paths.get(getSessionPath(id).toString(), INFO_FILE), json);
        if (id == getCurrentId()) {
            triggerChanged(ChangeType.INFO);
        }
    }

    public void setInfo(String key, Object value) throws IOException {
        setInfo(getCurrentId(), key, value);
    }
    
    public void setInfo(int id, String key, Object value) throws IOException {
        Map<String, Object> info = getInfo(id);
        info.put(key, value);
        setInfo(id,info);
    }  

    public List<Map<String, Object>> getRuns() throws IOException {
        return getRuns(false);
    }

    public List<Map<String, Object>> getRuns(int id) throws IOException {
        return getRuns(id, false);
    }
    
    public List<Map<String, Object>> getRuns(boolean relative) throws IOException {
        return getRuns(getCurrentId(), relative);
    }

    public List<Map<String, Object>> getRuns(int id, boolean relative) throws IOException {
        Map<String, Object> info = getInfo(id);
        List<Map<String, Object>> runs = (List) info.get("runs");
        String root = getRoot(id);
        for (int i=0; i<runs.size(); i++){
            Map<String, Object> run = runs.get(i);
            if (run.containsKey("data")){
                run.put("data",getFileName(Str.toString(run.get("data")), relative, root));
            }
        }
        return runs;
    }    
    

    void addRun(Map<String, Object> value) throws IOException {
        Map<String, Object> info = getInfo();
        List runs = (List) info.get("runs");
        runs.add(value);
        setInfo(info);
    }
    
    
    public int getNumberRuns(int id) throws IOException{
        Map<String, Object> info = getInfo(id);
        List<Map<String, Object>> runs = (List) info.get("runs");
        return runs.size();
    }

    public int getNumberRuns() throws IOException{
        assertStarted();
        return getNumberRuns(getCurrentId());
    }
    public boolean updateLastRun(String key, Object value) throws IOException {
        return updateRun(-1, key, value);
    }

    public boolean updateLastRun(int id, String key, Object value) throws IOException {
        return updateRun(id, -1, key, value);
    }
    
    public boolean updateRun(int runIndex, String key, Object value) throws IOException {
        assertStarted();
        return updateRun(getCurrentId(), runIndex, key, value);
    }

    public boolean updateRun(int id, int runIndex, String key, Object value) throws IOException {
        Map<String, Object> info = getInfo(id);
        List runs = (List) info.get("runs");
        if (runIndex<0){
            runIndex = runs.size() - 1;
        }
        if (runIndex >= runs.size()) {
            return false;
        }
        Map<String, Object> run = (Map<String, Object>) runs.get(runIndex);
        run.put(key, value);
        setInfo(id, info);
        return true;
    }    
        
    public boolean setRunEnabled(int runIndex, boolean enabled) throws IOException {
        assertStarted();
        return setRunEnabled(getCurrentId(), runIndex, enabled);
    }

    public boolean setRunEnabled(int id, int runIndex, boolean enabled) throws IOException {
        return updateRun(id, runIndex, "enabled", enabled);
    }    

    public List<String> getAdditionalFiles() throws IOException {
        assertStarted();
        return getAdditionalFiles(getCurrentId());
    }

    public List<String> getAdditionalFiles(int id) throws IOException {
        return getAdditionalFiles(id, false);
    }
    
    public List<String> getAdditionalFiles(int id, boolean relative) throws IOException {
        Map<String, Object> info = getInfo(id);
        List <String> files = (List) info.getOrDefault("files", new ArrayList<>());
        
        String root = getRoot(id);
        if (relative){
            for (int i=0; i< files.size(); i++){
                files.set(i, getFileName(files.get(i), relative, root));
            }
        }
        return files;
    }
       
    public static String getFileName(String filename, boolean relative,  String root) {   
        if (new File(filename).isAbsolute() == relative){       
            if (relative){
                if (IO.isSubPath(filename,root)) {
                    return IO.getRelativePath(filename, root);
                } 
            } else{
                return Paths.get(root, filename).toString();
            }        
        }
        return filename;
    }    

    public String getFileName(String filename, boolean relative, int id) throws IOException {    
        return getFileName(filename, relative, getRoot(id));
    }        
    
    public String getFileName(String filename, boolean relative) throws IOException {    
        return getFileName(filename, relative, getRoot());
    }        
    
    public List<String> getFileList(int id) throws IOException{
        return getFileList(id, false);        
    }
    
    
    public List<String> getFileList(int id, boolean relative) throws IOException{
        List<String> ret = new ArrayList();
        String root = getRoot(id);
        ret.add(Paths.get(getSessionPath(id).toString(), INFO_FILE).toString());
        ret.add(Paths.get(getSessionPath(id).toString(), METADATA_FILE).toString());
        for (Map<String, Object> run : getRuns(id)){
            if (run.containsKey("data")){
               if (Boolean.TRUE.equals(run.getOrDefault("enabled", true))){
                   ret.add(getFileName(run.get("data").toString(), relative, root));
               }
            }   
        }
        for (String str : getAdditionalFiles(id)){
            ret.add(getFileName(str, relative, root));
        }
        return ret;
    }
    
    public List<String> getFileListAtRoot(int id) throws IOException{
        List<String> ret = new ArrayList<>();
        for (String name: getFileList(id, true)){
            if (new File(name).isAbsolute()){
                Logger.getLogger(SessionManager.class.getName()).fine("File not on data root: " + name);
            } else{
                ret.add(name);
            }
        }
        return ret;
    }
    
    public void createZipFile(int id, File file, boolean preserveDirectoryStructure) throws IOException{
        List<String> names = getFileList(id, false);
        List<File> files = new ArrayList<>();
        for (String name: names){
            File f = new File(name); 
            if (f.exists()){
                files.add(f);
            } else {
                Logger.getLogger(SessionManager.class.getName()).warning("Invalid data file: " + f.toString());
            }
        }
        IO.createZipFile(file, files, preserveDirectoryStructure ? new File(getRoot(id)): null);
    }    
    
    public String getState() throws IOException {
        assertStarted();
        return getState(getCurrentId());
    }    

    public String getState(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        String state = (String) info.getOrDefault("state", STATE_COMPLETED);
        return state;
    }    

    public void setState(String state) throws IOException {
        assertStarted();
        setState(getCurrentId(), state);
    }    
    
    public void setState(int id, String state) throws IOException {
        Map<String, Object> info = getInfo(id);
        info.put("state", state);
        setInfo(id, info);
    }    

    public void setAdditionalFiles(List<String> files) throws IOException {
        assertStarted();
        setAdditionalFiles(getCurrentId(), files);
    }

    public void setAdditionalFiles(int id, List<String> files) throws IOException {
        List<String> filesWithoutDuplicates = new ArrayList<>(new HashSet<>(files));        
        Map<String, Object> info = getInfo(id);
        info.put("files", filesWithoutDuplicates);
        setInfo(id, info);
    }

    public void addAdditionalFile(String file) throws IOException {
        assertStarted();
        addAdditionalFile(getCurrentId(), file);
    }

    void addAdditionalFile(int id, String file) throws IOException {
        file = getFileName(file, true, id);
        List<String> files = getAdditionalFiles(id);
        if (!Arr.containsEqual(files.toArray(), file)) {
            files.add(file);
        }
        setAdditionalFiles(id, files);
    }

    public Map<String, Object> getInfo() throws IOException {
        assertStarted();
        return getInfo(getCurrentId());
    }

    public Map<String, Object> getInfo(int id) throws IOException {
        String json = Files.readString(Paths.get(getSessionPath(id).toString(), INFO_FILE));
        Map<String, Object> ret = (Map) JsonSerializer.decode(json, Map.class);
        return ret;
    }
    
    public static String toString(Object obj) throws IOException{
        if (obj == null){
            return "";
        }
        if ((obj instanceof Map) || (obj instanceof List)){
            return  JsonSerializer.encode(obj);
        }    
        return obj.toString();
    }
    
    public static Object fromString(MetadataType type, String str) throws Exception{
        str = str.trim();        
        switch (type) {
            case Integer:
                return Integer.valueOf(str);
            case Double:
                return Double.valueOf(str);
            case Boolean:
                return Boolean.valueOf(str);
            case List:
                return JsonSerializer.decode(str, List.class);
            case Map:
                return JsonSerializer.decode(str, Map.class);
            default:
                return str;
        }        
    }

    public void setMetadata(Map<String, Object> metadata) throws IOException {
        assertStarted();
        Set<Map.Entry<Object, Object>> metadataDefinition = getMetadataDefinition();
        for (String key : metadata.keySet().toArray(new String[0])) {
            MetadataType type = getMetadataType(key);
            Object value = metadata.get(key);
            try {
                if (value == null) {
                    value = "";
                }
                if (value instanceof String) {
                    value = fromString (type, (String)value);
                } else if (type == MetadataType.String) {
                    value = Str.toString(value);
                }
            } catch (Exception ex) {
            }
            metadata.put(key, value);
        }

        String json = JsonSerializer.encode(metadata);
        Files.writeString(Paths.get(getCurrentPath().toString(), METADATA_FILE), json);
        triggerChanged(ChangeType.METADATA);
    }

    public void setMetadata(String key, Object value) throws IOException {
        Map<String, Object> info = getMetadata();
        info.put(key, value);
        setMetadata(info);
    }

    public Map<String, Object> getMetadata() throws IOException {
        return getMetadata(false);
    }
    
    public Map<String, Object> getMetadata(int id) throws IOException {
        return getMetadata(id, false);
    }
    
    public Map<String, Object> getMetadata(boolean asString) throws IOException {
        assertStarted();
        return getMetadata(getCurrentId(), asString);
    }
    
    public Map<String, Object> getMetadata(int id, boolean asString) throws IOException {
        String json = Files.readString(Paths.get(getSessionPath(id).toString(), METADATA_FILE));
        Map<String, Object> ret = (Map) JsonSerializer.decode(json, Map.class);
        if (asString){
            for (String key : ret.keySet()){
                Object value = ret.get(key);
                value = toString(value);
                ret.put(key, value);
            }
        }
        return ret;
    }
    
    public  List<ImmutablePair<String,Object>> getDisplayableMetadata() throws IOException{
        assertStarted();
        return getDisplayableMetadata(getCurrentId());
    }
    
    public  List<ImmutablePair<String,Object>> getDisplayableMetadata(int id) throws IOException{
        List<ImmutablePair<String,Object>> ret = new ArrayList<>();
        Map<String, Object> metadata = getMetadata(id, true);
        Set<String> keys = new HashSet<>(metadata.keySet());
        Set<Map.Entry<Object, Object>> entries = getMetadataDefinition();        
        for (Map.Entry entry : entries) {
            String key = entry.getKey().toString();
            Object def = getMetadataDefault(entry);
            Object value = metadata.getOrDefault(entry.getKey(), def);
            ret.add(new ImmutablePair(key, value));
            keys.remove(key);
        }
        //Keys not in metadata definition
        for (String key: keys){
            Object value = metadata.getOrDefault(key, "");
            ret.add(new ImmutablePair(key, value));
        }
        return ret;
    }

    public boolean isStarted() throws IOException {
        String cur = Context.getInstance().getVariable(CURRENT_SESSION);
        return (cur != null) ;
    }

    void assertStarted() throws IOException {
        if (!isStarted()) {
            throw new IOException("Session not started");
        }
    }

    void assertNotStarted() throws IOException {
        if (isStarted()) {
            throw new IOException("Session already started");
        }
    }
}
