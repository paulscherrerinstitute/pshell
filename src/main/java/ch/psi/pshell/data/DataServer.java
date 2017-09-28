package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Replier;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;

/**
 * Publishes the persisted data through a ZMQ request-reply pattern.
 */
public class DataServer extends Replier {

    public DataServer(int port) {
        super(port);
    }

    public static String execute(String request) throws Exception {
        DataManager dm = Context.getInstance().getDataManager();
        StringBuffer ret = new StringBuffer();

        String path = request;
        int page = 0; //TODO
        if (path.contains("#")) {
            String[] tokens = path.split("#");
            path = tokens[0];
            page = Integer.valueOf(tokens[1]);
        }

        if ((request != "") || !dm.isOpen()) { //Empty string means current data context if open
            //Folder 
            try {
                File dir = Paths.get(dm.getDataFolder(), request).toFile();
                if ((request.length() == 0) || (dir.exists() && dir.isDirectory())) {
                    if (dm.isDataPacked() || !dm.isRoot(dir.getPath())) {
                        String folder = Paths.get(dm.getDataFolder(), request).toString();
                        ret.append("Folder: ").append(request.length() == 0 ? "/" : request).append("\n\n");
                        ret.append("Contents:").append("\n");
                        for (String file : getFolderContents(folder)) {
                            String prefix = Paths.get(folder, file).toFile().isDirectory() ? "+ " : "- ";
                            ret.append("\t").append(prefix).append(file).append("\n");
                        }
                        return ret.toString();
                    }
                }
            } catch (Exception ex) {
                ret = new StringBuffer();
            }
        }

        DataSlice ds = null;
        String root = null;
        //Full path
        if (path.contains("|") || path.contains(" /")) {
            String[] tokens = path.contains("|") ? path.split("\\|") : path.split(" /");
            root = tokens[0].trim();
            path = (tokens.length > 1) ? tokens[1].trim() : "/";
            //Root
        } else if (dm.getChildren(path, "/").length > 0) {
            root = path;
            path = "/";
            //Current data context
        } else {
            root = dm.getOutput();
            path = (path == "") ? "/" : path;
        }

        ret.append("Root: ").append(root).append("\n");
        ret.append("Path: ").append(path).append("\n\n");

        if (dm.isDataset(root, path)) {
            ds = dm.getData(root, path, page);
        }

        ret.append("Info: ").append("\n");
        Map<String, Object> info = dm.getInfo(root, path);
        for (String key : info.keySet()) {
            ret.append("\t").append(key).append(" = ").append(Str.toString(info.get(key), 10)).append("\n");
        }
        ret.append("\n");

        ret.append("Attributes: ").append("\n");
        Map<String, Object> attrs = dm.getAttributes(root, path);
        for (String key : attrs.keySet()) {
            ret.append("\t").append(key).append(" = ").append(Str.toString(attrs.get(key), 10)).append("\n");
        }
        ret.append("\n");

        if (ds != null) {
            Object data = ds.sliceData;
            if (!data.getClass().isArray()) {
                ret.append(data);
            } else {
                for (int i = 0; i < Array.getLength(data); i++) {
                    Object element = Array.get(data, i);
                    if (!element.getClass().isArray()) {
                        ret.append(element);
                    } else {
                        for (int j = 0; j < Array.getLength(element); j++) {
                            Object obj = Array.get(element, j);
                            if (obj.getClass().isArray()) {
                                obj = Convert.arrayToString(obj, " ");
                            }
                            ret.append(obj);
                            ret.append("\t");
                        }
                    }
                    ret.append("\n");
                }
            }
        } else {
            ret.append("Contents:").append("\n");
            for (String child : dm.getChildren(root, path)) {
                String prefix = dm.isGroup(root, child) ? "+ " : "- ";
                ret.append("\t").append(prefix).append(child).append("\n");
            }
        }
        return ret.toString();
    }

    public static String getContents(String request) throws Exception {
        DataManager dm = Context.getInstance().getDataManager();
        StringBuffer ret = new StringBuffer();

        String path = request;
        int page = 0; //TODO
        if (path.contains("#")) {
            String[] tokens = path.split("#");
            path = tokens[0];
            page = Integer.valueOf(tokens[1]);
        }

        //Folder 
        if ((request != "") || !dm.isOpen()) { //Empty string means current data context if open
            try {
                File dir = Paths.get(dm.getDataFolder(), request).toFile();
                if ((request.length() == 0) || (dir.exists() && dir.isDirectory())) {
                    if (dm.isDataPacked() || !dm.isRoot(dir.getPath())) {
                        String folder = Paths.get(dm.getDataFolder(), request).toString();
                        for (String file : getFolderContents(folder)) {
                            String suffix = Paths.get(folder, file).toFile().isDirectory() ? "/" : "";
                            ret.append(file).append(suffix).append("\n");
                        }
                        return ret.toString();
                    }
                }
            } catch (Exception ex) {
                ret = new StringBuffer();
            }
        }

        String root = null;
        String prefix = "";
        //Full path
        if (path.contains("|") || path.contains(" /")) {
            String[] tokens = path.contains("|") ? path.split("\\|") : path.split(" /");
            root = tokens[0].trim();
            path = (tokens.length > 1) ? tokens[1].trim() : "/";
            //Root
        } else if (dm.getChildren(path, "/").length > 0) {
            root = path;
            path = "/";
            prefix = " /";
            //Current data context
        } else {
            root = dm.getOutput();
            path = (path == "") ? "/" : path;
        }

        if (!dm.isDataset(root, path)) {
            for (String child : dm.getChildren(root, path)) {
                String suffix = dm.isGroup(root, child) ? "/" : "";
                if (child.contains("/")) {
                    child = child.substring(child.lastIndexOf("/") + 1);
                }
                ret.append(prefix).append(child).append(suffix).append("\n");
            }
        }
        return ret.toString();
    }

    static String[] getFolderContents(String folder) {
        DataManager dm = Context.getInstance().getDataManager();
        File f = new File(folder);
        ArrayList<String> ret = new ArrayList<>();
        if (f.isDirectory()) {
            File[] files = IO.listFiles(f, dm.getFileFilter());
            //Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            Arrays.sort(files, (a, b) -> a.compareTo(b));
            for (File file : files) {
                ret.add(file.getName());
            }
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String onReceived(String request) throws Exception {
        return execute(request);
    }

    public static void main(String[] args) throws Exception {
        String server = "localhost:5573";
        if (args.length > 0) {
            server = args[0];
        }
        org.zeromq.ZMQ.Context context = org.zeromq.ZMQ.context(1);
        org.zeromq.ZMQ.Socket requester = context.socket(org.zeromq.ZMQ.REQ);

        requester.connect("tcp://" + server);

        for (String tx : new String[]{
            "2016_02/20160218/20160218_153900_test1 | scan 1/",
            "2016_02/20160218/20160218_153900_test1 | scan 1/sin",
            "2016_02/20160218/20160218_153900_test1 | scan 1/arr",}) {
            requester.send(tx);
            Object rec = requester.recvStr();
            System.out.println(rec);
        }
        requester.close();
        context.term();
    }

}
