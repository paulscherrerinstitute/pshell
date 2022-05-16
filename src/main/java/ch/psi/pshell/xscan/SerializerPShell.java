package ch.psi.pshell.xscan;

import ch.psi.utils.Message;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.DataManager;
import static ch.psi.pshell.data.Layout.ATTR_FILE;
import static ch.psi.pshell.data.Layout.ATTR_LAYOUT;
import ch.psi.utils.Arr;
import ch.psi.utils.EventBusListener;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
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

    String basename;
    String dataset = null;
    private boolean newfile = true;
    private int icount = 1;
    private boolean dataInBetween = false;
    final DataManager dm;

    public SerializerPShell() {
       this(null); 
    }
    
    public SerializerPShell(String basename ) {
        dm=Context.getInstance().getDataManager();
        try {
            if (basename!=null){
                Context.getInstance().setExecutionPar("path", basename);
            }
            dm.openOutput();
            this.basename = IO.getPrefix(dm.getRootFileName());	
            dm.setAttribute("/", ATTR_FILE,this.basename+".xml");            
            System.out.print(Thread.currentThread());
        } catch (Exception ex) {
            Logger.getLogger(SerializerPShell.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onMessage(Message message) throws Exception {
        if (message instanceof DataMessage) {
            dataInBetween = true;
            DataMessage m = (DataMessage) message;
            List data = m.getData();
            if (newfile) {
                dataset = String.format("/scan% d", icount);
                dm.appendLog(String.format("Xscan execution started: %s", dataset));
                List<String> names = new ArrayList<>();
                List<Class> types = new ArrayList<>();
                List<Integer> lenghts = new ArrayList<>();
                List<Integer> dims = new ArrayList<>();

                for (Metadata c : ((DataMessage) message).getMetadata()) {
                    names.add(c.getId());
                    dims.add(c.getDimension());
                    types.add(Double.class);
                    lenghts.add(1);
                }
                for (int i = 0; i < data.size(); i++) {
                    Object o = data.get(i);
                    Class type = o.getClass();
                    if (o.getClass().isArray()) {
                        lenghts.set(i, Array.getLength(o));
                        type = Arr.getComponentType(data);
                    }
                    if ((type != Double.class) && (type != double.class)) {
                        types.set(i, type);
                    }
                }
                dm.createDataset(dataset,
                        names.toArray(new String[0]),
                        types.toArray(new Class[0]),
                        (int[]) Convert.toPrimitiveArray(lenghts.toArray(new Integer[0])));
                
                dm.setAttribute(dataset, ATTR_LAYOUT,"fda");
                dm.setAttribute(dataset, "dims", (int[]) Convert.toPrimitiveArray(dims.toArray(new Integer[0])));
                dm.setAttribute(dataset, "names", names.toArray(new String[0]));
                newfile = false;
            }
            dm.appendItem(dataset, data.toArray());
        } else if (message instanceof StreamDelimiterMessage) {
            StreamDelimiterMessage m = (StreamDelimiterMessage) message;
            logger.finer("Delimiter - number: " + m.getNumber() + " iflag: " + m.isIflag());
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
                dm.appendLog(String.format("Xscan completed"));
            } catch (Exception ex) {
                Logger.getLogger(SerializerPShell.class.getName()).log(Level.FINE, null, ex);
            }
        }
    }
}
