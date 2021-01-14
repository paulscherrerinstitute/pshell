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
        List
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
        return start(name, new HashMap<>());
    }

    public int start(String name, Map<String, Object> metadata) throws IOException {
        stop();
        if ((name == null) || name.isBlank()){
            name = "";
        }
        int counter = getCurrentCounter();
        counter++;
        Context.getInstance().setVariable(SESSION_COUNTER, counter);
        Context.getInstance().setVariable(CURRENT_SESSION, name);
        Path path = getCurrentPath();
        path.toFile().mkdirs();
        Map info = new HashMap<String, Object>();
        info.put("name", name);
        info.put("start", getTimestamp());
        info.put("state", "started");
        List runs = new ArrayList();
        info.put("runs", runs);
        setInfo(info);
        setMetadata(metadata);
        triggerChanged(ChangeType.STATE);
        return counter;
    }

    public void stop() throws IOException {
        if (isStarted()) {
            addInfo("state", "completed");
            addInfo("stop", getTimestamp());
            Context.getInstance().setVariable(CURRENT_SESSION, null);
            triggerChanged(ChangeType.STATE);
        }
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
                return Context.getInstance().getVariable(CURRENT_SESSION);
            }
        } catch (Exception ex) {
        }
        return "null";
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

    public long getStart(int id) throws IOException {
        return (Long) getInfo(id).get("start");
    }

    public long getStart() throws IOException {
        assertStarted();
        return getStart(getCurrentId());
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

    void onChangeDataPath(File dataPath) {
        int runId = getCurrentId();
        boolean transfer = (Context.getInstance().config.dataTransferMode != DataTransferMode.Off);
        try {
            if (isStarted()) {
                if (dataPath != null) {
                    Map<String, Object> run = new HashMap<>();
                    run.put("start", getTimestamp());
                    run.put("data", dataPath.getCanonicalPath());
                    run.put("state", "running");
                    addRun(run);
                } else {
                    updateRun("stop", getTimestamp());
                    updateRun("state", "completed");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }

        try {
            if ((dataPath == null) && (currentDataPath != null)) {
                if (transfer) {
                    if (isStarted()) {
                        updateRun("state", "transfering");
                    }
                    final File origin = currentDataPath;
                    new Thread(() -> {
                        try {
                            String dest = transferData(origin);
                            if (isStarted()) {
                                updateRun(runId, "data", dest);
                                updateRun(runId, "state", "transferred");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
                            try {
                                if (isStarted()) {
                                    updateRun(runId, "state", "transfer error: " + ex.getMessage());
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
                    return MetadataType.valueOf(String.valueOf(entry.getValue()).trim());
                }
            }
        } catch (Exception ex) {
        }
        return MetadataType.String;
    }

    public void onMetadataDefinitionChanged() {
        try {
            if (isStarted()) {
                Set<Map.Entry<Object, Object>> metadataDefinition = getMetadataDefinition();
                Map<String, Object> metadata = getMetadata();
                Map<String, Object> newMetadata = new HashMap<>();
                for (Map.Entry<Object, Object> entry : metadataDefinition) {
                    newMetadata.put(Str.toString(entry.getKey()), metadata.getOrDefault(entry.getKey(), ""));
                }
                setMetadata(newMetadata);
            }
        } catch (Exception ex) {
            Logger.getLogger(MetadataEditor.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    void setInfo(Map<String, Object> metadata) throws IOException {
        assertStarted();
        setInfo(getCurrentId(), metadata);
    }

    void setInfo(int id, Map<String, Object> metadata) throws IOException {
        String json = JsonSerializer.encode(metadata);
        Files.writeString(Paths.get(getSessionPath(id).toString(), INFO_FILE), json);
        if (id == getCurrentId()) {
            triggerChanged(ChangeType.INFO);
        }
    }

    void addInfo(String key, Object value) throws IOException {
        Map<String, Object> info = getInfo();
        info.put(key, value);
        setInfo(info);
    }

    public List<Map<String, Object>> getRuns() throws IOException {
        assertStarted();
        return getRuns(getCurrentId());
    }

    public List<Map<String, Object>> getRuns(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        List<Map<String, Object>> runs = (List) info.get("runs");
        return runs;
    }

    void addRun(Map<String, Object> value) throws IOException {
        Map<String, Object> info = getInfo();
        List runs = (List) info.get("runs");
        runs.add(value);
        setInfo(info);
    }

    boolean updateRun(String key, Object value) throws IOException {
        assertStarted();
        return updateRun(getCurrentId(), key, value);
    }

    boolean updateRun(int id, String key, Object value) throws IOException {
        Map<String, Object> info = getInfo(id);
        List runs = (List) info.get("runs");
        if (runs.size() < 1) {
            return false;
        }
        Map<String, Object> run = (Map<String, Object>) runs.get(runs.size() - 1);
        run.put(key, value);
        setInfo(id, info);
        return true;
    }

    public List<String> getAdditionalFiles() throws IOException {
        assertStarted();
        return getAdditionalFiles(getCurrentId());
    }

    public List<String> getAdditionalFiles(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        List files = (List) info.getOrDefault("files", new ArrayList<>());
        return files;
    }
    
    public String getState(int id) throws IOException {
        Map<String, Object> info = getInfo(id);
        String state = (String) info.getOrDefault("state", "completed");
        return state;
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
                    String str = (String) value;
                    value = str.trim();
                    switch (type) {
                        case Integer:
                            value = Integer.valueOf(str);
                            break;
                        case Double:
                            value = Double.valueOf(str);
                            break;
                        case Boolean:
                            value = Boolean.valueOf(str);
                            break;
                        case List:
                            if (str.startsWith("[") && str.endsWith("]")) {
                                str = str.substring(1, str.length() - 1);
                            }
                            value = Str.trim(Str.split(str, new String[]{"|", ";", ","}));
                            break;
                    }
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

    public void addMetadata(String key, Object value) throws IOException {
        Map<String, Object> info = getMetadata();
        info.put(key, value);
        setMetadata(info);
    }

    public Map<String, Object> getMetadata() throws IOException {
        assertStarted();
        return getMetadata(getCurrentId());
    }

    public Map<String, Object> getMetadata(int id) throws IOException {
        String json = Files.readString(Paths.get(getSessionPath(id).toString(), METADATA_FILE));
        Map<String, Object> ret = (Map) JsonSerializer.decode(json, Map.class);
        return ret;
    }

    public boolean isStarted() throws IOException {
        String cur = Context.getInstance().getVariable(CURRENT_SESSION);
        return (cur != null) ;
    }

    void assertStarted() throws IOException {
        if (!isStarted()) {
            throw new IOException("Session not stated");
        }
    }

    void assertNotStarted() throws IOException {
        if (isStarted()) {
            throw new IOException("Session already started");
        }
    }

    List<File> getFiles() {
        return null;
    }
}
