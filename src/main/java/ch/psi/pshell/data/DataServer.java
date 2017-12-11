package ch.psi.pshell.data;

import ch.psi.bsread.compression.Compression;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.bs.Encoder;
import ch.psi.pshell.core.Replier;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Publishes the persisted data through a ZMQ request-reply pattern.
 */
public class DataServer extends Replier {

    static ObjectMapper mapper = new ObjectMapper();

    public DataServer(int port) {
        super(port);
        setRequestParts(2);
    }

    public static Object execute(String address, String format) throws Exception {
        DataManager dm = Context.getInstance().getDataManager();
        if (isFolder(address)) {
            FolderContent fc = getFolderContents(Paths.get(dm.getDataFolder(), address).toString());
            if (format.equals("txt")) {
                return asTxt(fc);
            } else if (format.equals("json")) {
                return asJson(fc);
            }
        } else {
            DataContent dc = getDataContents(address);
            if (format.equals("txt")) {
                return asTxt(dc);
            } else if (format.equals("json")) {
                return asJson(dc);
            } else if (format.equals("bs")) {
                return asBsread(dc);
            } else if (format.equals("bin")) {
                return asBin(dc);
            }
        }
        throw new IllegalArgumentException();
    }

    static String asTxt(FolderContent fc) throws Exception{
        StringBuffer ret = new StringBuffer();
        ret.append("Folder: ").append(fc.name).append("\n\n");
        ret.append("Contents:").append("\n");
        for (String file : fc.contents) {
            String prefix = Paths.get(fc.name, file).toFile().isDirectory() ? "+ " : "- ";
            ret.append("\t").append(prefix).append(file).append("\n");
        }
        return ret.toString();
    }

    static String asTxt(DataContent dc) throws Exception {
        StringBuffer ret = new StringBuffer();
        ret.append("Root: ").append(dc.root).append("\n");
        ret.append("Path: ").append(dc.path).append("\n\n");

        ret.append("Info: ").append("\n");
        for (String key : dc.info.keySet()) {
            ret.append("\t").append(key).append(" = ").append(Str.toString(dc.info.get(key), 10)).append("\n");
        }
        ret.append("\n");

        ret.append("Attributes: ").append("\n");
        for (String key : dc.attributes.keySet()) {
            ret.append("\t").append(key).append(" = ").append(Str.toString(dc.attributes.get(key), 10)).append("\n");
        }
        ret.append("\n");
        
        if (dc.data != null) {
            ret.append("Data:").append("\n");
            ret.append("\t").append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dc.data.sliceData));
        } else {
            ret.append("Contents:").append("\n");
            for (String child : dc.contents) {
                String prefix = child.endsWith("/") ? "+ " : "- ";
                ret.append("\t").append(prefix).append(child).append("\n");
            }
        }
        return ret.toString();
    }

    static String asJson(FolderContent fc) throws Exception {
        return mapper.writeValueAsString(fc);
    }

    static String asJson(DataContent dc) throws Exception {
        return mapper.writeValueAsString(dc);
    }

    static List asBsread(DataContent dc) throws Exception {
        Encoder encoder = new Encoder(new ObjectMapper(),Compression.none);                
        encoder.addValue("data", dc.data.sliceData);
        return encoder.encode();
    }

    static List asBin(DataContent dc) throws Exception {
        return asBsread(dc);
    }

    public static boolean isFolder(String address) {
        DataManager dm = Context.getInstance().getDataManager();
        try {
            if ((address != "") || !dm.isOpen()) { //Empty string means current data context if open
                File dir = Paths.get(dm.getDataFolder(), address).toFile();
                if ((address.length() == 0) || (dir.exists() && dir.isDirectory())) {
                    if (dm.isDataPacked() || !dm.isRoot(dir.getPath())) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
        }
        return false;
    }

    public static String getContents(String address) throws Exception {
        StringBuffer ret = new StringBuffer();
        DataManager dm = Context.getInstance().getDataManager();
        if (isFolder(address)) {
            String folder = Paths.get(dm.getDataFolder(), address).toString();
            FolderContent fc = getFolderContents(folder);
            String prefix = address.trim().endsWith("/") ? "" : "/";
            for (String file : fc.contents) {
                String suffix = Paths.get(folder, file).toFile().isDirectory() ? "/" : "";
                ret.append(prefix).append(file).append(suffix).append("\n");
            }
            return ret.toString();
        } else {
            DataContent dc = getDataContents(address);
            if (!dc.dataset) {
                //String prefix = " /";
                for (String child : dc.contents) {
                    ///ret.append(prefix).append(child).append("\n");
                    ret.append(child).append("\n");
                }
            }
        }
        return ret.toString();
    }

    public static class DataContent {

        public String root;
        public String path;
        public int index;
        public boolean dataset;
        public DataSlice data;
        public Map<String, Object> info;
        public Map<String, Object> attributes;
        public List<String> contents;
    }

    public static DataContent getDataContents(String path) throws Exception {
        DataManager dm = Context.getInstance().getDataManager();
        DataContent ret = new DataContent();
        if (path.contains(".")) {
            String[] tokens = path.split(".");
            try{
                ret.index = Integer.valueOf(path.substring(path.lastIndexOf(".")+1));
                path = path.substring(0, path.lastIndexOf("."));
            } catch (Exception ex){
                
            }            
        }
        String prefix = "";

        //Full path
        if (path.contains("|") || path.contains(" /")) {
            String[] tokens = path.contains("|") ? path.split("\\|") : path.split(" /");
            ret.root = tokens[0].trim();
            ret.path = (tokens.length > 1) ? tokens[1].trim() : "/";
            if (!ret.path.endsWith("/")){
                prefix = "/";
            }
            //Root
        } else if (dm.getChildren(path, "/").length > 0) {
            ret.root = path;
            ret.path = "/";
            prefix = " /";
            //Current data context
        } else {
            ret.root = dm.getOutput();
            ret.path = (path == "") ? "/" : path;
        }
        ret.info = dm.getInfo(ret.root, ret.path);
        ret.attributes = dm.getAttributes(ret.root, ret.path);
        if (dm.isDataset(ret.root, ret.path)) {
            ret.data = dm.getData(ret.root, ret.path, ret.index);
            ret.dataset = true;
        } else {
            ret.dataset = false;
            ret.contents = new ArrayList<>();
            for (String child : dm.getChildren(ret.root, ret.path)) {
                if (child.contains("/")) {
                    child = child.substring(child.lastIndexOf("/") + 1);
                }
                if (dm.isGroup(ret.root, ret.path + "/" + child)) {
                    child += "/";
                }
                ret.contents.add(prefix + child);
            }
        }
        return ret;
    }

    public static class FolderContent {

        public String name;
        public List<String> contents;
    }

    public static FolderContent getFolderContents(String folder) {
        DataManager dm = Context.getInstance().getDataManager();
        FolderContent ret = new FolderContent();
        ret.name = folder.length() == 0 ? "/" : folder;
        File f = new File(folder);
        ret.contents = new ArrayList<>();
        if (f.isDirectory()) {
            File[] files = IO.listFiles(f, dm.getFileFilter());
            //Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            Arrays.sort(files, (a, b) -> a.compareTo(b));
            for (File file : files) {
                ret.contents.add(file.getName());
            }
        }
        return ret;
    }

    @Override
    protected List reply(List request) throws InterruptedException {
        DataManager dm = Context.getInstance().getDataManager();
        if ((request == null) || (request.size() != 2)) {
            return null;
        }
        List ret = new ArrayList();
        String format = (String) request.get(0);
        String address = (String) request.get(1);

        try {
            Object msg = execute(address, format);
            if (msg instanceof List) {
                ret.addAll((List) msg);
            } else {
                ret.add(msg);
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            ret.add("ERROR: " + ex.getMessage());
        }
        return ret;
    }            
}
