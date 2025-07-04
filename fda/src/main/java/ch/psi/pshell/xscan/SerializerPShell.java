package ch.psi.pshell.xscan;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.LayoutFDA;
import ch.psi.pshell.data.LayoutTable;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.EventBusListener;
import ch.psi.pshell.utils.Message;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serialize data received by a DataQueue
 */
public class SerializerPShell implements EventBusListener {

    private static final Logger logger = Logger.getLogger(SerializerPShell.class.getName());

    private String dataset = null;
    private boolean newfile = true;
    private int icount = 1;
    private boolean dataInBetween = false;
    private boolean table;
    private final DataManager dm;

    private List<String> names;
    private List<Class> types;
    private List<Integer> lenghts;
    private List<Integer> dims;

    public SerializerPShell() {
        this(null);
    }

    public SerializerPShell(String basename) {
        dm = Context.getDataManager();
        try {
            if (basename != null) {
                Context.getInterpreter().setExecutionPar("path", basename);
            }
            dm.openOutput();
            dm.setAttribute("/", Layout.ATTR_FILE, basename + ".xml");
            table = LayoutTable.class.isAssignableFrom(dm.getLayout().getClass());
        } catch (Exception ex) {
            Logger.getLogger(SerializerPShell.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onMessage(Message message) throws Exception {
        if (message instanceof DataMessage dataMessage) {
            dataInBetween = true;
            DataMessage m = dataMessage;
            List data = m.getData();
            if (newfile) {
                dataset = String.format("/scan %d", icount);
                dm.appendLog(String.format("XScan execution started: %s", dataset));
                names = new ArrayList<>();
                types = new ArrayList<>();
                lenghts = new ArrayList<>();
                dims = new ArrayList<>();

                for (Metadata c : dataMessage.getMetadata()) {
                    names.add(c.getId());
                    dims.add(c.getDimension());
                    types.add(Double.class);
                    lenghts.add(0);
                }
                for (int i = 0; i < data.size(); i++) {
                    Object o = data.get(i);
                    Class type = o.getClass();
                    if (type.isArray()) {
                        lenghts.set(i, Array.getLength(o));
                        type = Arr.getComponentType(o);
                        if (table) {
                            if (Convert.isWrapperClass(type)) {
                                type = Convert.getPrimitiveClass(type);
                            }
                            type = Array.newInstance(type, 0).getClass();
                        }
                    }
                    if (type == DescStatsDouble.class){
                        type=Double.class;
                    }
                    types.set(i, type);
                }
                if (table) {
                    if (dm.getLayout() instanceof LayoutFDA layoutFDA){
                        if (LayoutFDA.isFlatStorage()){
                            String filePrefix = Setup.expandPath(layoutFDA.getFilePrefix(), Context.getExecutionPars().getStart());
                            dataset = String.format("%s_%04d", filePrefix, icount-1);                          
                        }
                    }
                    dm.createDataset(dataset,
                            names.toArray(new String[0]),
                            types.toArray(new Class[0]),
                            (int[]) Convert.toPrimitiveArray(lenghts.toArray(new Integer[0])));
                } else {
                    dm.createGroup(dataset);
                    for (int i = 0; i < names.size(); i++) {
                        try {
                            if (lenghts.get(i) > 0) {
                                dm.createDataset(dataset + "/" + names.get(i), types.get(i), new int[]{0, lenghts.get(i)});
                            } else {
                                dm.createDataset(dataset + "/" + names.get(i), types.get(i));
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(SerializerPShell.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                dm.setAttribute(dataset, Layout.ATTR_TYPE, ProcessorXScan.SCAN_TYPE);
                dm.setAttribute(dataset, "dims", (int[]) Convert.toPrimitiveArray(dims.toArray(new Integer[0])));
                dm.setAttribute(dataset, "names", names.toArray(new String[0]));
                newfile = false;
            }
            if (table) {
                dm.appendItem(dataset, data.toArray());
            } else {
                for (int i = 0; i < names.size(); i++) {
                    try {
                        dm.appendItem(dataset + "/" + names.get(i), data.get(i));
                    } catch (Exception ex) {
                        Logger.getLogger(SerializerPShell.class.getName()).log(Level.FINE, null, ex);
                    }
                }
            }
        } else if (message instanceof StreamDelimiterMessage m) {
            logger.log(Level.FINER, "Delimiter - number: {0} iflag: {1}", new Object[]{m.getNumber(), m.isIflag()});
            if (m.isIflag()) {
                // Only increase iflag counter if there was data in between
                // subsequent StreamDelimiterMessages.
                if (dataInBetween) {
                    icount++;
                }
                dataInBetween = false;

                // Set flag to open new file
                newfile = true;
            }
        } else if (message instanceof EndOfStreamMessage) {
            try {
                //dm.closeOutput();
                dm.appendLog(String.format("XScan completed"));
            } catch (Exception ex) {
                Logger.getLogger(SerializerPShell.class.getName()).log(Level.FINE, null, ex);
            }
        }
    }
}
