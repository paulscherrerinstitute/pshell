package ch.psi.pshell.core;

import ch.psi.utils.Chrono;

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

    int getCurrentCounter(){
        try {
            return Integer.valueOf(Context.getInstance().getVariable("SessionCounter"));
        } catch (Exception ex){
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
        Context.getInstance().setVariable("CurrentSession", (name==null) ? counter : name);
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
        if (isStarted()){
            addInfo("stop", getTimestamp());
            Context.getInstance().setVariable("CurrentSession", "");
        }
    }

    public int getCurrentId() {
        try{
            if (isStarted()){
                return getCurrentCounter();
            }
        } catch(Exception ex) {
        }
        return 0;
    }

    public String getCurrentName(){
        try{
            if (isStarted()){
                return Context.getInstance().getVariable("CurrentSession");
            }
        } catch(Exception ex) {
        }
        return "null";
    }

    public Path getCurrentPath() throws IOException {
        assertStarted();
        return getSessionPath(getCurrentId());
    }

    public Path getSessionPath(int id) throws IOException {
        return Paths.get(Context.getInstance().getSetup().getUserSessionsPath(),String.valueOf(id));
    }

    void setInfo(Map<String, Object> metadata) throws IOException {
        assertStarted();
        String json = JsonSerializer.encode(metadata);
        Files.writeString(Paths.get(getCurrentPath().toString(), INFO_FILE), json);
    }

    void addInfo(String key, Object value) throws IOException {
        Map<String, Object> info = getInfo();
        info.put(key, value);
        setInfo(info);
    }

    Object getTimestamp(){
        return Chrono.getTimeStr(System.currentTimeMillis(), "YYYY-MM-dd HH:mm:ss.SSS");
    }

    void onChangeDataPath(File dataPath) {
        try {
            if (isStarted()){
                if (dataPath!=null) {
                    Map<String, Object> run = new HashMap<>();
                    run.put("start", getTimestamp());
                    run.put("data", dataPath.getCanonicalPath());
                    addRun(run);
                } else {
                    updateRun("stop", getTimestamp());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SessionManager.class.getName()).log(Level.WARNING, null, ex);
        }
    }


    public List<Map<String, Object>> getRuns() throws IOException {
        Map<String, Object> info = getInfo();
        List<Map<String, Object>> runs=  (List) info.get("runs");
        return runs;
    }

    void addRun(Map<String, Object> value) throws IOException {
        Map<String, Object> info = getInfo();
        List runs = (List) info.get("runs");
        runs.add(value);
        setInfo(info);
    }

    boolean updateRun(String key, Object value) throws IOException {
        Map<String, Object> info = getInfo();
        List runs = (List) info.get("runs");
        if (runs.size()<1) {
            return false;
        }
        Map<String, Object> run = (Map<String, Object>) runs.get(runs.size()-1);
        run.put(key,value);
        setInfo(info);
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
        return ((cur!=null) && !cur.isBlank());
    }

    void assertStarted() throws IOException{
        if (!isStarted()){
            throw new IOException("Session not stated");
        }
    }

    void assertNotStarted() throws IOException{
        if (isStarted()){
            throw new IOException("Session already started");
        }
    }

    List<File> getFiles(){
        return null;
    }

}
