package ch.psi.pshell.core;

import ch.psi.pshell.core.Configuration.DataTransferMode;
import ch.psi.pshell.core.Configuration.SessionHandling;
import ch.psi.pshell.data.RSync;
import ch.psi.pshell.swing.MetadataEditor;
import ch.psi.utils.Arr;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.Folder;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.OrderedProperties;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

        void onChange(int id, ChangeType type);
    }

    void triggerChanged(int id, ChangeType type) {
        for (SessionManagerListener listener : getListeners()) {
            try {
                listener.onChange(id, type);
            } catch (Exception ex) {
                Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    final static String CURRENT_SESSION = "CurrentSession";
    final static String SESSION_COUNTER = "SessionCounter";
    final static String PACKED = "Packed";

    final static String INFO_FILE = "info.json";
    final static String METADATA_FILE = "metadata.json";

    public final static String STATE_STARTED = "started";
    public final static String STATE_PAUSED = "paused";
    public final static String STATE_COMPLETED = "completed";
    public final static String STATE_RUNNING = "running";
    public final static String STATE_ARCHIVED = "archived";
    public final static String STATE_ERROR = "error";
    public final static String STATE_TRANSFERING = "transfering";
    public final static String STATE_TRANSFERRED = "transfered";

    public final static String UNNAMED_SESSION_NAME = "unnamed";
    public final static String UNDEFINED_SESSION_NAME = "unknown";
    
    public final static int UNDEFINED_SESSION_ID = 0;

    boolean firstTransfer = true;

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
        if (isStarted()) {
            if (getNumberRuns() == 0) {
                cancel();
            } else {
                stop();
            }
        }
        name = checkName(name);
        metadata = checkMetadata(metadata);
        root = checkRoot(root);
        int sessionId = getNewSession();
        setCurrentSession(sessionId);
        Map info = new HashMap<String, Object>();
        info.put("id", sessionId);
        info.put("name", name);        
        info.put("user", Sys.getUserName());
        info.put("start", getTimestamp());
        info.put("state", STATE_STARTED);
        info.put("root", root);
        info.put("format", Context.getInstance().getConfig().dataProvider);
        info.put("layout", Context.getInstance().getConfig().dataLayout);
        info.put("mode", getMode());
        
        List runs = new ArrayList();
        info.put("runs", runs);
        setInfo(info);
        setMetadata(metadata);
        triggerChanged(sessionId, ChangeType.STATE);
        Context.getInstance().getCommandManager().onSessionStarted(sessionId);
        return sessionId;
    }
    
    public SessionHandling getMode(){
        return Context.getInstance().getConfig().sessionHandling;
    }
    
    int getNewSession() throws IOException{
        int sessionId = getSessionCounter();
        sessionId++;
        createSessionPath(sessionId);
        setSessionCounter(sessionId);
        return sessionId;
    }
    
    void setSessionCounter(Integer value) throws IOException{
        Context.getInstance().setProperty(getSessionsInfoFile(), SESSION_COUNTER, value);
    }
    
    int getSessionCounter() {
        try {
            return Integer.valueOf(Context.getInstance().getProperty(getSessionsInfoFile(), SESSION_COUNTER));
        } catch (Exception ex) {
            return 0;
        }

    }
    
    void setCurrentSession(Integer value) throws IOException{
        //Context.getInstance().setVariable(CURRENT_SESSION, name);      
        Context.getInstance().setProperty(getSessionsInfoFile(), CURRENT_SESSION, value);
    }
    
    public int getCurrentSession() {
        try {
            return Integer.valueOf(Context.getInstance().getProperty(getSessionsInfoFile(), CURRENT_SESSION));
        } catch (Exception ex) {
        }
        return UNDEFINED_SESSION_ID;
    }    
   
    public void stop() throws IOException {
        if (isStarted()) {
            int sessionId = getCurrentSession();
            Context.getInstance().getCommandManager().onSessionFinished(sessionId);
            try {
                setInfo("state", STATE_COMPLETED);
                setInfo("stop", getTimestamp());
            } catch (Exception ex) {
                Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            setCurrentSession(null);
            triggerChanged(sessionId, ChangeType.STATE);
        }
    }

    public int restart(int sessionId) throws IOException {
        String state = getState(sessionId);
        if (!isSessionEditable(state)) {
            throw new IOException("Cannot restart session with state: " + state);
        }
        String user = getUser(sessionId);
        if (!user.isBlank() && !user.equals(Sys.getUserName())) {
            throw new IOException("Cannot restart session of user: " + user);
        }
        if (isStarted()) {
            if (getNumberRuns() == 0) {
                cancel();
            } else {
                stop();
            }
        }
        setCurrentSession(sessionId);
        Map<String, Object> info = getInfo();
        info.put("state", STATE_STARTED);
        info.remove("stop");
        setInfo(info);
        triggerChanged(sessionId, ChangeType.STATE);
        Context.getInstance().getCommandManager().onSessionStarted(sessionId);
        return sessionId;
    }

    public void pause() throws IOException {
        if (isStarted() && !isPaused()) {
            setState(STATE_PAUSED);
        }
    }

    public void resume() throws IOException {
        if (isPaused()) {
            setState(STATE_STARTED);
        }
    }

    //Stop current session and restore previous session id (i f no run has been executed)
    public void cancel() throws IOException {
        if (isStarted() && (getNumberRuns() == 0)) {
            int sessionId = getCurrentSession();
            int currentCounter = getSessionCounter();
            Path path = getSessionPath(getCurrentSession());
            IO.deleteRecursive(path.toFile());
            if (currentCounter == sessionId) {
                setSessionCounter(sessionId - 1);
            }
            setCurrentSession(null);
            triggerChanged(sessionId, ChangeType.STATE);
        }
    }
    
    //Stop current session leaving the database in inconsistent state
    public void abort() throws IOException {
        if (isStarted()) {
            setCurrentSession(null);
            triggerChanged(getCurrentSession(), ChangeType.STATE);
        }
    }

    public void move(int id, List<String> data, int destination) throws IOException { 
        Map<String, Object> metadata = getMetadata(id);
        Map<String, Object> info = getMetadata(id);
        List<Map<String, Object>> runs = getRuns(id, true);                
        List<Map<String, Object>> movedRuns = new ArrayList<>();
        String state = getState(id);
        if (!isSessionEditable(state)){
            throw new IOException("Invalid origin sesion state: " + state);
        }
        state = getState(destination);
        if (!isSessionEditable(state)){
            throw new IOException("Invalid destination sesion state: " + state);
        }
        String user = getUser(id);
        if (!user.isBlank() && !user.equals(Sys.getUserName())) {
            throw new IOException("Cannot move data from a different user");
        }
        user = getUser(destination);
        if (!user.isBlank() && !user.equals(Sys.getUserName())) {
            throw new IOException("Cannot move data to a different user");
        }                        
        
        List<Integer> runIndexes = getRunIndexes(id, data);        
        for (int i: runIndexes){
            if (i>=0){
                Map<String, Object> run = runs.get(i);
                if (!run.getOrDefault("state", "").equals(STATE_COMPLETED)){
                    throw new IOException("Run state must be completed");
                }
                movedRuns.add(run);
            }
        }        
        if (movedRuns.size()<=0){
            throw new IOException("No data voced from session");
        }

        for (Map<String, Object> run : movedRuns){
            addRun(destination, run);
        }
        
        info = getInfo(id);
        runs.removeAll(movedRuns);
        info.put("runs", runs);       
        setInfo(id, info);

        triggerChanged(id, ChangeType.STATE);
    }
    
      public int detach(String name, int id, List<String> data) throws IOException {
        name = checkName(name);    
        Map<String, Object> metadata = getMetadata(id);
        Map<String, Object> info = getInfo(id);
        List<Map<String, Object>> runs = getRuns(id, true);                
        List<Map<String, Object>> detachedRuns = new ArrayList<>();
        
        String user = getUser(id);
        if (!user.isBlank() && !user.equals(Sys.getUserName())) {
            throw new IOException("Cannot detach a session from a different user");
        }
        List<Integer> runIndexes = getRunIndexes(id, data);        
        for (int i: runIndexes){
            if (i>=0){
                Map<String, Object> run = runs.get(i);
                if (!run.getOrDefault("state", "").equals(STATE_COMPLETED)){
                    throw new IOException("Run state must be completed");
                }
                detachedRuns.add(run);
            }
        }
        
        if (detachedRuns.size()<=0){
            throw new IOException("No data detached from session");
        }

        int session = getNewSession();
        info.put("name",name);
        info.put("id",session);        
        info.put("start", detachedRuns.get(0).getOrDefault("start", System.currentTimeMillis()));
        info.put("stop", detachedRuns.get(detachedRuns.size()-1).getOrDefault("stop", System.currentTimeMillis()));
        info.put("state",STATE_COMPLETED);
        info.put("runs", detachedRuns);   
        info.remove("files");
        setInfo(session, info);
        setMetadata(session, metadata);
        
        info = getInfo(id);
        runs.removeAll(detachedRuns);
        info.put("runs", runs);       
        setInfo(id, info);

        triggerChanged(session, ChangeType.STATE);
        return session;
    }
  
    

    public int create(String name,  List<String> files, Map<String, Object> metadata, String root) throws IOException {        
        name = checkName(name);
        metadata = checkMetadata(metadata);
        root = checkRoot(root);
        long start=System.currentTimeMillis();
        long stop=0;
        for (String fileName: files){
            File file = new File(getFileName(fileName,false,getDefaultRoot()));
            long time = file.lastModified();
            start = Math.min(start, time);
            stop = Math.max(stop, time);            
        }                
        int sessionId = getNewSession();
        Map info = new HashMap<String, Object>();        
        info.put("id", sessionId);
        info.put("name", name);        
        info.put("user", Sys.getUserName());
        info.put("start", start);
        info.put("stop",stop);
        info.put("state", STATE_COMPLETED);
        info.put("root", root);
        info.put("format", Context.getInstance().getConfig().dataProvider);
        info.put("layout", Context.getInstance().getConfig().dataLayout);
        List runs = new ArrayList();
        info.put("runs", runs);
        setInfo(sessionId, info);
        setMetadata(sessionId, metadata);
        setAdditionalFiles(sessionId, files);
        triggerChanged(sessionId, ChangeType.STATE);
        return sessionId;
        
    }
    
    private String checkName(String name){
        if ((name == null) || name.isBlank()) {
            name = UNNAMED_SESSION_NAME;
        }
        return name;
    }
    
    private Map<String, Object> checkMetadata(Map<String, Object> metadata){
        if (metadata == null) {
            metadata = getMetadataDefault();
        }
        return metadata;
    }

    private String checkRoot(String root){
        if ((root == null) || root.isBlank()) {
            root = getDefaultRoot();
        }
        try {
            root = Paths.get(root).toRealPath().toString();
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }
        return root;
    }
    
    public boolean isPaused() throws IOException {
        try {
            if (isStarted()) {
                return getState().equals(STATE_PAUSED);
            }
        } catch (Exception ex) {
        }
        return false;
    }

    public String getCurrentName() {
        try {
            if (isStarted()) {
                String name = getName();
                if (name.isBlank()) {
                    return UNNAMED_SESSION_NAME;
                }
                return name;
            }
        } catch (Exception ex) {
        }
        return UNDEFINED_SESSION_NAME;
    }

    Path createSessionPath(int id) throws IOException {
        Path path = _getSessionPath(id);
        path.toFile().mkdirs();
        IO.setFilePermissions(path.toString(),Context.getInstance().getConfig().filePermissionsConfig);
        return path;
    }

    public Path getCurrentPath() throws IOException {
        assertStarted();
        return getSessionPath(getCurrentSession());
    }

    public Path getSessionPath(int id) throws IOException {
        Path ret = _getSessionPath(id);
        if (!ret.toFile().isDirectory()) {
            if (getCurrentSession() == id) {
                throw new IOException("Current session folder cannot be found.");
            } else {
                throw new IOException("Invalid session id: " + id);
            }
        }
        return ret;
    }
    
    public String getSessionsInfoFile() throws IOException {
        return Context.getInstance().getSetup().getSessionsConfigurationFile();
    }    

    Path _getSessionPath(int id) throws IOException {
        return Paths.get(Context.getInstance().getSetup().getUserSessionsPath(), String.valueOf(id));
    }

    public List<Integer> getIDs() {
        return getIDs(null);
    }

    public List<Integer> getIDs(String state) {
        return getIDs(state, null);
    }
    
    public List<Integer> getIDs(String state, String user) {
        List<Integer> ret = new ArrayList<>();
        try {
            File folder = new File(Context.getInstance().getSetup().getUserSessionsPath());
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    String name = file.getName();
                    try {
                        int id = Integer.valueOf(name);
                        if ((state!=null) && (!state.isBlank())){
                            if (!state.equals(getState(id))){
                                continue;
                            }
                        }                        
                        if ((user!=null) && (!user.isBlank())){
                            String idUser = getUser(id);
                            if (!user.equals(idUser)){
                                continue;
                            }
                        }                        
                        ret.add(id);
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
        return getName(getCurrentSession());
    }

    public long getStart(int id) throws IOException {
        return (Long) getInfo(id).get("start");
    }

    public long getStart() throws IOException {
        assertStarted();
        return getStart(getCurrentSession());
    }

    public long getStop(int id) throws IOException {
        return (Long) getInfo(id).get("stop");
    }

    public String getRoot(int id) throws IOException {
        return (String) getInfo(id).getOrDefault("root", getDefaultRoot());
    }
    
    public String getDefaultRoot(){
        return Context.getInstance().getSetup().getDataPath();
    }

    public String getRoot() throws IOException {
        assertStarted();
        return getRoot(getCurrentSession());
    }

    Object getTimestamp() {
        //return Chrono.getTimeStr(System.currentTimeMillis(), "YYYY-MM-dd HH:mm:ss.SSS");
        return System.currentTimeMillis();
    }

    String transferData(File from) throws Exception {
        IO.assertExists(from);
        String path = Context.getInstance().config.dataTransferPath;
        String user = Context.getInstance().config.dataTransferUser;
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
        final int sessionId = getCurrentSession();
        final boolean updateRun = saveRun;

        boolean transfer = (Context.getInstance().config.dataTransferMode != DataTransferMode.Off);
        try {
            if (isStarted()) {
                if (dataPath != null) {
                    saveRun = !isPaused();
                    if (saveRun) {
                        Map<String, Object> run = new HashMap<>();
                        run.put("start", getTimestamp());
                        run.put("data", getFileName(dataPath.getCanonicalPath(), true));
                        run.put("state", STATE_RUNNING);
                        addRun(run);
                    }
                } else {
                    if (saveRun) {
                        updateLastRun("stop", getTimestamp());
                        updateLastRun("state", STATE_COMPLETED);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void onCreateDetachedFile(File file) {
        if (getMode() == SessionHandling.Files){
            try {
                if (isStarted()) {
                    if (file.exists()){
                        addAdditionalFile(file.toString());
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
            }                        
        }
    }

    public Set<Map.Entry<Object, Object>> getMetadataDefinition() {
        OrderedProperties properties = new OrderedProperties();
        try (FileInputStream in = new FileInputStream(Context.getInstance().getSetup().getSessionMetadataDefinitionFile())) {
            properties.load(in);
        } catch (FileNotFoundException ex) {
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
                    if (val.contains(";")) {
                        val = val.substring(0, val.indexOf(";")).trim();
                    }
                    return MetadataType.valueOf(val);
                }
            }
        } catch (Exception ex) {
        }
        return MetadataType.String;
    }
    
    public boolean isMetadataDefinedKey(String key) {
        try {
            for (Map.Entry entry : getMetadataDefinition()) {
                if (entry.getKey().equals(key)) {
                    return true;
                }
            }
        } catch (Exception ex) {
        }
        return false;
    }    

    public static Object getDefaultValue(MetadataType type) {
        switch (type) {
            case List:
                return "[]";
            case Map:
                return "{}";
            default:
                return "";
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
        if (val.contains(";")) {
            return val.substring(val.indexOf(";") + 1).trim();
        }
        return "";
    }

    public Map<String, Object> getMetadataDefault() {
        Map<String, Object> ret = new HashMap();
        for (Map.Entry entry : getMetadataDefinition()) {
            ret.put(entry.getKey().toString(), getMetadataDefault(entry));
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
        setInfo(getCurrentSession(), info);
    }

    void setInfo(int id, Map<String, Object> info) throws IOException {
        String json = EncoderJson.encode(info, false);
        Path path = Paths.get(getSessionPath(id).toString(), INFO_FILE);
        Files.writeString(path, json);
        IO.setFilePermissions(path.toString(),Context.getInstance().getConfig().filePermissionsConfig);
        if (id == getCurrentSession()) {
            triggerChanged(id, ChangeType.INFO);
        }
    }

    public void setInfo(String key, Object value) throws IOException {
        assertStarted();
        setInfo(getCurrentSession(), key, value);
    }

    public void setInfo(int id, String key, Object value) throws IOException {
        Map<String, Object> info = getInfo(id);
        info.put(key, value);
        setInfo(id, info);
    }

    public Map<String, Object> getLastRun(boolean relative) throws IOException {
        assertStarted();
        List<Map<String, Object>> runs = getRuns(relative);
        if (runs.size() == 0) {
            return null;
        }
        return runs.get(runs.size() - 1);
    }
    
    public String getLastRunData(boolean relative) throws IOException {
        Map<String, Object> run  = getLastRun(relative);
        if ((run!=null) && (run.containsKey("data"))) {
            return (String)run.get("data");
        }
        return null;
    }    

    public List<Map<String, Object>> getRuns() throws IOException {
        assertStarted();
        return getRuns(getCurrentSession(), false);
    }

    public List<Map<String, Object>> getRuns(int id) throws IOException {
        return getRuns(id, false);
    }

    public List<Map<String, Object>> getRuns(boolean relative) throws IOException {
        assertStarted();
        return getRuns(getCurrentSession(), relative);
    }

    public List<Map<String, Object>> getRuns(int id, boolean relative) throws IOException {        
        return getRuns(id, relative, null);
    }

    public List<Map<String, Object>> getRuns(int id, boolean relative, String filter) throws IOException {
        Map<String, Object> info = getInfo(id);
        List<Map<String, Object>> runs = (List) info.get("runs");
        String root = getRoot(id);
        for (int i = 0; i < runs.size(); i++) {
            Map<String, Object> run = runs.get(i);
            if (run.containsKey("data")) {
                run.put("data",getFileName(Str.toString(run.get("data")), relative, root));
            }
        }
        
        if ((filter!=null) && (!filter.isBlank())){
            filter = filter.trim();        
            List<Map<String, Object>> filtered = new ArrayList<>();            
            for (int i = 0; i < runs.size(); i++) {
                Map<String, Object> run = runs.get(i);
                if (run.containsKey("data")) {
                    if (Str.toString(run.get("data")).contains(filter)){
                        filtered.add(run);
                    }                    
                }
            }
            return filtered;
        }        
        return runs;
    }
    void addRun(int id, Map<String, Object> value) throws IOException {
        Map<String, Object> info = getInfo(id);
        List runs = (List) info.get("runs");
        runs.add(value);
        setInfo(id, info);
    }
    
    void addRun(Map<String, Object> value) throws IOException {
        assertStarted();
        addRun(getCurrentSession(), value);        
    }

    public int getNumberRuns(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        List<Map<String, Object>> runs = (List) info.get("runs");
        return runs.size();
    }

    public int getNumberRuns() throws IOException {
        assertStarted();
        return getNumberRuns(getCurrentSession());
    }

    public boolean updateLastRun(String key, Object value) throws IOException {
        return updateRun(-1, key, value);
    }

    public boolean updateLastRun(int id, String key, Object value) throws IOException {
        return updateRun(id, -1, key, value);
    }

    public boolean updateRun(int runIndex, String key, Object value) throws IOException {
        assertStarted();
        return updateRun(getCurrentSession(), runIndex, key, value);
    }

    public boolean updateRun(int id, int runIndex, String key, Object value) throws IOException {
        Map<String, Object> info = getInfo(id);
        List runs = (List) info.get("runs");
        if (runIndex < 0) {
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
        return setRunEnabled(getCurrentSession(), runIndex, enabled);
    }

    public boolean setRunEnabled(int id, int runIndex, boolean enabled) throws IOException {
        return updateRun(id, runIndex, "enabled", enabled);
    }

    public boolean setRunEnabled(int id, String data, boolean enabled) throws IOException {
        int runIndex = getRunIndex(id, data);
        return updateRun(id, runIndex, "enabled", enabled);
    }        
    
    public boolean isRunEnabled(int id, String data) throws IOException {
        return isRunEnabled(id, getRunIndex(id, data));
    }        
    
    public boolean isRunEnabled(int id, int index) throws IOException {
        try{
            return (Boolean)getRuns(id).get(index).get("enabled");
        } catch (Exception ex){
            return true;
        }
    }        

    public int getRunIndex(int id, String data) throws IOException {
        data = getFileName(data, true, id).trim();
        List<Map<String, Object>> runs = getRuns(id, true);
        for (int i=0; i< runs.size(); i++){
           if (runs.get(i).containsKey("data")) {
                if (data.equals(runs.get(i).get("data"))){
                    return i;
                }
            }
        }
        throw new IOException(String.format("Cannot find data file %s in session %d", data, id)); 
    }        
    
    public List<Integer> getRunIndexes(int id, List<String> data) throws IOException {        
        List<Integer> ret = new ArrayList<>();
        for (String d : data){
            ret.add(getRunIndex(id, d));
        }       
        return ret;
    }
    
    public List<String> getAdditionalFiles() throws IOException {
        assertStarted();
        return getAdditionalFiles(getCurrentSession());
    }

    public List<String> getAdditionalFiles(int id) throws IOException {
        return getAdditionalFiles(id, false);
    }

    public List<String> getAdditionalFiles(boolean relative) throws IOException {
        assertStarted();
        return getAdditionalFiles(getCurrentSession(), relative);
    }

    public List<String> getAdditionalFiles(int id, boolean relative) throws IOException {
        Map<String, Object> info = getInfo(id);
        List<String> files = (List) info.getOrDefault("files", new ArrayList<>());

        String root = getRoot(id);
        if (relative) {
            for (int i = 0; i < files.size(); i++) {
                files.set(i, getFileName(files.get(i), relative, root));
            }
        }
        return files;
    }

    public static String getFileName(String filename, boolean relative, String root) {
        if (new File(filename).isAbsolute() == relative) {
            if (relative) {
                if (IO.isSubPath(filename, root)) {
                    return IO.getRelativePath(filename, root);
                }
            } else {
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

    public List<String> getFileList() throws IOException {
        assertStarted();
        return getFileList(getCurrentSession());
    }

    public List<String> getFileList(int id) throws IOException {
        return getFileList(id, false);
    }

    public List<String> getFileList(boolean relative) throws IOException {
        assertStarted();
        return getFileList(getCurrentSession(), relative);
    }

    public List<String> getFileList(int id, boolean relative) throws IOException {
        List<String> ret = new ArrayList();
        String root = getRoot(id);
        ret.add(Paths.get(getSessionPath(id).toString(), INFO_FILE).toString());
        ret.add(Paths.get(getSessionPath(id).toString(), METADATA_FILE).toString());
        if (getMode() != SessionHandling.Files){
            for (Map<String, Object> run : getRuns(id)) {
                if (run.containsKey("data")) {
                    if (Boolean.TRUE.equals(run.getOrDefault("enabled", true))) {
                        ret.add(getFileName(run.get("data").toString(), relative, root));
                    }
                }
            }
        }
        for (String str : getAdditionalFiles(id)) {
            ret.add(getFileName(str, relative, root));
        }
        List<String> removeDuplicates = new ArrayList<>(new LinkedHashSet<>(ret));
        return removeDuplicates; //Remove duplicates
    }

    public List<String> getFileListAtRoot(int id) throws IOException {
        List<String> ret = new ArrayList<>();
        for (String name : getFileList(id, true)) {
            if (new File(name).isAbsolute()) {
                Logger.getLogger(SessionManager.class.getName()).fine("File not on data root: " + name);
            } else {
                ret.add(name);
            }
        }
        return ret;
    }

    public void createZipFile(String file, boolean preserveDirectoryStructure) throws IOException {
        createZipFile(new File(file), preserveDirectoryStructure);
    }

    public void createZipFile(File file, boolean preserveDirectoryStructure) throws IOException {
        assertStarted();
        createZipFile(getCurrentSession(), file, preserveDirectoryStructure);
    }

    public void createZipFile(int id, String file, boolean preserveDirectoryStructure) throws IOException {
        createZipFile(id, new File(file), preserveDirectoryStructure);
    }

    public void createZipFile(int id, File file, boolean preserveDirectoryStructure) throws IOException {
        List<String> names = getFileList(id, false);
        List<File> files = new ArrayList<>();
        for (String name : names) {
            File f = new File(name);
            if (f.exists()) {
                files.add(f);
            } else {
                Logger.getLogger(SessionManager.class.getName()).warning("Invalid data file: " + f.toString());
            }
        }
        IO.createZipFile(file, files, preserveDirectoryStructure ? new File(getRoot(id)) : null);
    }

    public String getState() throws IOException {
        assertStarted();
        return getState(getCurrentSession());
    }

    public String getState(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        String state = (String) info.getOrDefault("state", STATE_COMPLETED);
        return state;
    }

    public void setState(String state) throws IOException {
        assertStarted();
        setState(getCurrentSession(), state);
    }

    public void setState(int id, String state) throws IOException {
        Map<String, Object> info = getInfo(id);
        info.put("state", state);
        setInfo(id, info);
        triggerChanged(id, ChangeType.STATE);
    }
    
    public String getUser() throws IOException {
        assertStarted();
        return getUser(getCurrentSession());
    }

    public String getUser(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        String ret = (String) info.getOrDefault("user", "");
        return ret;
    }
    

    public void setAdditionalFiles(List<String> files) throws IOException {
        assertStarted();
        setAdditionalFiles(getCurrentSession(), files);
    }

    public void setAdditionalFiles(int id, List<String> files) throws IOException {
        List<String> filesWithoutDuplicates = new ArrayList<>(new LinkedHashSet<>(files));
        Map<String, Object> info = getInfo(id);
        info.put("files", filesWithoutDuplicates);
        setInfo(id, info);
    }

    public void addAdditionalFile(String file) throws IOException {
        assertStarted();
        addAdditionalFile(getCurrentSession(), file);
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
        return getInfo(getCurrentSession());
    }

    public Map<String, Object> getInfo(int id) throws IOException {
        String json = Files.readString(Paths.get(getSessionPath(id).toString(), INFO_FILE));
        Map<String, Object> ret = (Map) EncoderJson.decode(json, Map.class);
        return ret;
    }
    

    public static String toString(Object obj) throws IOException {
        if (obj == null) {
            return "";
        }
        if ((obj instanceof Map) || (obj instanceof List)) {
            return EncoderJson.encode(obj, false);
        }
        return obj.toString();
    }

    public static Object fromString(MetadataType type, String str) throws Exception {
        str = str.trim();
        switch (type) {
            case Integer:
                return Integer.valueOf(str);
            case Double:
                return Double.valueOf(str);
            case Boolean:
                return Boolean.valueOf(str);
            case List:
                return EncoderJson.decode(str, List.class);
            case Map:
                return EncoderJson.decode(str, Map.class);
            default:
                return str;
        }
    }

    public void setMetadata(int id, Map<String, Object> metadata) throws IOException {
        for (String key : metadata.keySet().toArray(new String[0])) {
            MetadataType type = getMetadataType(key);
            Object value = metadata.get(key);
            try {
                if (value == null) {
                    value = "";
                }
                if (value instanceof String) {
                    value = fromString(type, (String) value);
                } else if (type == MetadataType.String) {
                    value = Str.toString(value);
                }
            } catch (Exception ex) {
            }
            metadata.put(key, value);
        }

        String json = EncoderJson.encode(metadata, false);
        Path path = Paths.get(getSessionPath(id).toString(), METADATA_FILE);
        Files.writeString(path, json);
        IO.setFilePermissions(path.toString(),Context.getInstance().getConfig().filePermissionsConfig);
        if (id == getCurrentSession()) {
            triggerChanged(id, ChangeType.METADATA);
        }
    }

    public void setMetadata(Map<String, Object> metadata) throws IOException {
        assertStarted();
        setMetadata(getCurrentSession(), metadata);
    }

    public void setMetadata(String key, Object value) throws IOException {
        assertStarted();
        setMetadata(getCurrentSession(), key, value);
    }

    public void setMetadata(int id, String key, Object value) throws IOException {
        Map<String, Object> info = getMetadata(id);
        if (value == null) {
            if (!info.containsKey(key)) {
                return;
            }
            info.remove(key);
        } else {
            info.put(key, value);
        }
        setMetadata(id, info);
    }

    public Map<String, Object> getMetadata() throws IOException {
        return getMetadata(false);
    }

    public Map<String, Object> getMetadata(int id) throws IOException {
        return getMetadata(id, false);
    }

    public Map<String, Object> getMetadata(boolean asString) throws IOException {
        assertStarted();
        return getMetadata(getCurrentSession(), asString);
    }

    public Map<String, Object> getMetadata(int id, boolean asString) throws IOException {
        String json = Files.readString(Paths.get(getSessionPath(id).toString(), METADATA_FILE));
        Map<String, Object> ret = (Map) EncoderJson.decode(json, Map.class);
        if (asString) {
            for (String key : ret.keySet()) {
                Object value = ret.get(key);
                value = toString(value);
                ret.put(key, value);
            }
        }
        return ret;
    }

    public Object getMetadata(int id, String field) throws IOException {       
        return getMetadata(id, field, false);
    }    
    
    public Object getMetadata(int id, String field, boolean asString) throws IOException {       
        return getMetadata(id, asString).getOrDefault(field, null);
    }    
    
    public List<ImmutablePair<String, Object>> getDisplayableMetadata() throws IOException {
        assertStarted();
        return getDisplayableMetadata(getCurrentSession());
    }

    public List<ImmutablePair<String, Object>> getDisplayableMetadata(int id) throws IOException {
        List<ImmutablePair<String, Object>> ret = new ArrayList<>();
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
        for (String key : keys) {
            Object value = metadata.getOrDefault(key, "");
            ret.add(new ImmutablePair(key, value));
        }
        return ret;
    }

    public boolean isStarted() throws IOException {
        return (getCurrentSession() > 0);
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
    
    static public boolean isSessionArchivable(String state) {
        return state.equals(STATE_COMPLETED);
    }
    
    static public boolean isSessionEditable(String state) {
      return  state.equals(STATE_COMPLETED) ||
              state.equals(STATE_STARTED) ||
              state.equals(STATE_PAUSED);           
    }
}
