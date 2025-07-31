package ch.psi.pshell.utils;

import ch.psi.pshell.utils.IO.FilePermissions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persisted history of strings.
 */
public class History {

    final static Logger logger = Logger.getLogger(History.class.getName());
    //File history    
    final List<String> history = new ArrayList<>();

    final int size;
    final boolean autoPersist;
    final String fileName;
    final IO.FilePermissions permissions;

    public History(String fileName, int size, boolean autoPersist) {
        this(fileName, size, autoPersist, defaultPermissions);
    }
    
    public History(String fileName, int size, boolean autoPersist, FilePermissions permissions) {
        this.fileName = fileName;
        this.size = size;
        this.autoPersist = autoPersist;
        this.permissions =permissions;
        if (autoPersist) {
            restore();
        }
    }

    static FilePermissions defaultPermissions = FilePermissions.Default;
    
    public static void setDefaultPermissions(FilePermissions permissions){
        defaultPermissions = permissions;
    }
    
    public static FilePermissions getDefaultPermissions(){
        return defaultPermissions;
    } 
    
    public int getSize() {
        return size;
    }

    public boolean getAutoPersit() {
        return autoPersist;
    }

    public String getFileName() {
        return fileName;
    }

    public void clear() {
        synchronized (history) {
            history.clear();
            if (getAutoPersit()) {
                persist();
            }
        }
    }

    public void put(String str) {
        if ((str != null) && (!str.isEmpty())) {
            synchronized (history) {
                history.remove(str);
                history.add(str);
                if ((getSize() >= 0) && (history.size() > getSize())) {
                    history.remove(0);
                }
                if (getAutoPersit()) {
                    persist();
                }
            }
        }
    }

    public List<String> get() {
        synchronized (history) {
            ArrayList<String> ret = new ArrayList<>();
            ret.addAll(history);
            return ret;
        }
    }

    public void remove(String str) {
        if ((str != null) && (!str.isEmpty())) {
            synchronized (history) {
                history.remove(str);
                if (getAutoPersit()) {
                    persist();
                }
            }
        }
    }

    public List<String> search(String text) {
        List<String> ret = new ArrayList<>();
        if ((text != null) && (!text.isEmpty())) {
            synchronized (history) {
                for (String str : history) {
                    if (str.contains(text) || str.matches(text)) {
                        ret.add(str);
                    }
                }
            }
        }
        return ret;
    }

    public void persist() {
        try {
            byte[] buf;
            synchronized (history) {
                buf = Serializer.encode(history);
            }
            Files.write(Paths.get(getFileName()), buf);
            IO.setFilePermissions(getFileName(), permissions);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public void restore() {
        byte[] buf;
        synchronized (history) {
            history.clear();
            try {
                buf = Files.readAllBytes(Paths.get(getFileName()));
                history.addAll((List) Serializer.decode(buf));
            } catch (NoSuchFileException ex) {
                logger.log(Level.FINE, null, ex);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
            if ((getSize() >= 0) && (history.size() > getSize())) {
                history.subList(0, history.size() - getSize()).clear();
            }
        }
    }
}
