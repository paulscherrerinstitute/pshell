package ch.psi.pshell.xscan;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.LayoutTable;
import ch.psi.utils.EventBus;
import ch.psi.utils.IO;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Deserializer for PShell storage
 */
public class DeserializerPShell implements Deserializer {

    private static Logger logger = Logger.getLogger(DeserializerPShell.class.getName());

    private EventBus bus;
    private List<Metadata> metadata;
    private File file;
    private String filename;
    private String path;

    private List<Integer> dindex;
    private List<Integer> iindex;
    private boolean table;
    private DataManager dm;
    int[] dimensions;
    String[] ids;

    public DeserializerPShell(EventBus b, File file, String path, DataManager dm) {
        if (path==null){
            path = "/" + IO.getPrefix(file);
            file = file.getParentFile();
        }
        this.dm=dm;
        this.bus = b;
        this.file = file;
        this.dindex = new ArrayList<Integer>();
        this.iindex = new ArrayList<Integer>();
        filename = file.toString();
        this.path = path;

        this.metadata = new ArrayList<>();
        try {
            // Read metadata
            //dm = new DataManager(Context.getInstance(), file.isDirectory() ? "txt" : "h5", Context.getInstance().getConfig().getDataLayout());
            if (!ProcessorXScan.SCAN_TYPE.toString().equalsIgnoreCase(String.valueOf(dm.getAttribute(filename, path, Layout.ATTR_TYPE)))) {
                throw new RuntimeException("Not XScan data");
            }
            dimensions = (int[]) dm.getAttribute(filename, path, "dims");
            ids = (String[]) dm.getAttribute(filename, path, "names");
            String layoutName = (String) dm.getAttribute(filename, "/", Layout.ATTR_LAYOUT);
            try{
                table = LayoutTable.class.isAssignableFrom(Class.forName(layoutName));
            } catch (Exception ex){
            }

            // Create data message metadata
            Integer d = -1;
            for (int i = 0; i < ids.length; i++) {
                Integer dimension = dimensions[i];
                metadata.add(new Metadata(ids[i], dimension));

                // Store the first index of the first component
                // in each dimension ...
                if (!d.equals(dimension)) {
                    logger.finest("Add component index: " + i);
                    dindex.add(dimension);
                    iindex.add(i);
                    d = dimension;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read file metadata and initialize data queue", e);
        }
    }

    @Override
    public void read() {
        try {
            Object[] row = null;
            List<Double> checklist = new ArrayList<Double>(dindex.size());
            for (int i = 0; i < dindex.size(); i++) {
                checklist.add(null);
            }
            Object[] data;
            if (table) {
                data = (Object[][]) dm.getData(filename, path).sliceData;
            } else {
                data = new Object[ids.length];
                row = new Object[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    data[i] = dm.getData(filename, path + "/" + ids[i]).sliceData;
                }
            }
            int records = table ? data.length : Array.getLength(data[0]);
            // Read file
            for (int i = 0; i < records; i++) {
                // Create and populate new data message
                if (table) {
                    row = (Object[]) data[i];
                } else {
                    for (int j = 0; j < ids.length; j++) {
                        row[j] = Array.get(data[j], i);
                    }
                }

                DataMessage message = new DataMessage(metadata);
                for (Object value : row) {
                    message.getData().add(value);
                }

                // Check whether to issue a end of dimension control message
                for (int t = 0; t < iindex.size(); t++) {
                    Integer k = iindex.get(t);
                    if (dindex.get(t) > 0) {
                        Double d = (Double) message.getData().get(k);
                        if (checklist.get(k) != null && !checklist.get(k).equals(d)) {
                            // If value changes issue a dimension delimiter message
                            bus.post(new StreamDelimiterMessage(dindex.get(t) - 1));
                        }
                        checklist.set(k, d);
                    }
                }

                // Put message to queue
                bus.post(message);

                // TODO Need to detect dimension boundaries
            }

            // Add delimiter for all the dimensions
            for (int i = dindex.size() - 1; i >= 0; i--) {
                bus.post(new StreamDelimiterMessage(dindex.get(i)));
            }

            // Place end of stream message
            bus.post(new EndOfStreamMessage());

        } catch (Exception e) {
            throw new RuntimeException("Data deserializer had a problem reading the specified datafile", e);
        }

    }

}
