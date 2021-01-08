package ch.psi.pshell.core;

import ch.psi.pshell.core.Configuration.DataTransferMode;
import ch.psi.pshell.data.RSync;
import ch.psi.utils.Chrono;
import ch.psi.utils.Folder;
import ch.psi.utils.IO;
import ch.psi.utils.Str;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {

    final static String CURRENT_SESSION = "CurrentSession";
    final static String SESSION_COUNTER = "SessionCounter";

    final static String INFO_FILE = "info.json";
    final static String METADATA_FILE = "metadata.json";
    
    boolean firstTransfer = true;

    int getCurrentCounter() {
        try {
            return Integer.valueOf(Context.getInstance().getVariable("SessionCounter"));
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
        int counter = getCurrentCounter();
        counter++;
        Context.getInstance().setVariable("SessionCounter", counter);
        Context.getInstance().setVariable("CurrentSession", (name == null) ? counter : name);
        Path path = getCurrentPath();
        path.toFile().mkdirs();
        Map info = new HashMap<String, Object>();
        info.put("name", name);
        info.put("start", getTimestamp());
        List runs = new ArrayList();
        info.put("runs", runs);
        setInfo(info);
        setMetadata(metadata);
        return counter;
    }

    public void stop() throws IOException {
        if (isStarted()) {
            addInfo("stop", getTimestamp());
            Context.getInstance().setVariable("CurrentSession", "");
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
                return Context.getInstance().getVariable("CurrentSession");
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

    Object getTimestamp() {
        return Chrono.getTimeStr(System.currentTimeMillis(), "YYYY-MM-dd HH:mm:ss.SSS");
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
                    if (transfer) {
                        updateRun("transfer", "wait");
                    }
                    addRun(run);
                    writeMetadata();
                } else {
                    updateRun("stop", getTimestamp());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }

        try {
            if ((dataPath == null) && (currentDataPath != null)) {
                if (transfer) {
                    if (isStarted()) {
                        updateRun("transfer", "started");
                    }
                    final File origin = currentDataPath;
                    new Thread(() -> {
                        try {
                            String dest = transferData(origin);
                            if (isStarted()) {
                                updateRun(runId, "data", dest);
                                updateRun(runId, "transfer", "completed");
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
                            try {
                                if (isStarted()) {
                                    updateRun(runId, "transfer", "error: " + ex.getMessage());
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

    void setInfo(Map<String, Object> metadata) throws IOException {
        assertStarted();
        setInfo(getCurrentId(), metadata);
    }

    void setInfo(int id, Map<String, Object> metadata) throws IOException {
        String json = JsonSerializer.encode(metadata);
        Files.writeString(Paths.get(getSessionPath(id).toString(), INFO_FILE), json);
    }

    void addInfo(String key, Object value) throws IOException {
        Map<String, Object> info = getInfo();
        info.put(key, value);
        setInfo(info);
    }

    public List<Map<String, Object>> getRuns() throws IOException {
        Map<String, Object> info = getInfo();
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
        String json = JsonSerializer.encode(metadata);
        Files.writeString(Paths.get(getCurrentPath().toString(), METADATA_FILE), json);
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
        String cur = Context.getInstance().getVariable("CurrentSession");
        return ((cur != null) && !cur.isBlank());
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

    public void writeMetadata() throws IOException {
        Map<String, Object> metadata = getMetadata();
        for (String key : metadata.keySet()) {
            Object value = metadata.get(key);
            if (value != null) {
                try {
                    Context.getInstance().getDataManager().setAttribute("/", key, value);
                } catch (Exception ex) {
                    Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
    }
}
