package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This data layout stores each positioner and sensor as an individual dataset
 */
public class LayoutNX extends LayoutDefault implements Layout {

public static final String ATTR_CLASS = "NX_class";

public static final String NEXUS_CLASS_ROOT = "NXroot";
public static final String ATTR_CREATOR = "creator";
public static final String ATTR_CREATOR_VERSION = "creator_version";
public static final String ATTR_HDF5_VERSION= "HDF5_Version";
public static final String ATTR_FILE_NAME= "file_name";
public static final String ATTR_FILE_TIME= "file_time";
public static final String ATTR_DEFAULT = "default";

public static final String NEXUS_CLASS_ENTRY = "NXentry";
public static final String DSET_TITLE= "title";
public static final String DSET_EXP_ID = "experiment_identifier";
public static final String DSET_EXP_DESC = "experiment_description";
public static final String DSET_COLLECT_ID = "collection_identifier";
public static final String DSET_COLLECT_DESC = "collection_description";
public static final String DSET_ENTRY_ID = "entry_identifier";
public static final String DSET_ENTRY_UUID = "entry_identifier_uuid";
public static final String DSET_START_TIME = "start_time";
public static final String DSET_END_TIME = "end_time";
public static final String DSET_DURATION = "duration";

public static final String NEXUS_CLASS_DATA = "NXdata";

public static final String NEXUS_CLASS_COLLECTION = "NXcollection";
public static final String ATTR_SIGNAL = "signal";
public static final String ATTR_AUX_SIGNALS = "auxiliary_signals";
public static final String ATTR_AXIS = "axes";
public static final String ATTR_AXISNAME_INDICES = "%s_indices";
public static final String ATTR_TITLE = "title";
public static final String ATTR_X = "x";
public static final String ATTR_Y = "y";
public static final String ATTR_Z = "z";


    public static String getNxTimeStr(Long millis){
        return ((millis==null) || (millis<=0)) ? "" : Chrono.getTimeStr(millis, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }
    
    @Override
    public void initialize() {
    }

    public String getEntryPath(Scan scan) {        
        String ret = super.getScanPath(scan);
        if (!ret.startsWith("/")){
            ret = "/" + ret;
        }
        ret = ret.replaceAll(" ", "_");
        return ret;
    }

    @Override
    public String getScanPath(Scan scan) {
        return getEntryPath(scan) + "measurement/";
    }
    
    public String getPlotPath(Scan scan) {
        return getEntryPath(scan) + "plot/";
    }
    
    
    @Override
    public void onOpened(File output) throws IOException {
        super.onOpened(output);
        getDataManager().setAttribute("/", ATTR_CLASS, NEXUS_CLASS_ROOT);
        getDataManager().setAttribute("/", ATTR_CREATOR, App.getApplicationName());
        getDataManager().setAttribute("/", ATTR_CREATOR_VERSION, App.getApplicationVersion());
        getDataManager().setAttribute("/", ATTR_HDF5_VERSION, String.join(".", Convert.toStringArray(ProviderHDF5.getVersion())));
        getDataManager().setAttribute("/", ATTR_FILE_NAME, output.toString());
        getDataManager().setAttribute("/", ATTR_FILE_TIME, getNxTimeStr(IO.getCreation(output)));
        getDataManager().setAttribute("/", ATTR_FILE_TIME, getNxTimeStr(IO.getCreation(output)));   
    } 
    
    @Override
    public void onClosed(File output) throws IOException {
         setScriptVersionAttibute(); //Doing on close because file is commited asynchronously on start of scan.
    }          
    
    @Override
    public void onStart(Scan scan) throws IOException {
        super.onStart(scan);
        String entryPath = getEntryPath(scan);        
        if (getDataManager().getAttribute("/", ATTR_DEFAULT)==null){            
            getDataManager().setAttribute("/", ATTR_DEFAULT, entryPath.replaceAll("/$", ""));   //remove last / if present
        }
        getDataManager().setAttribute(entryPath, ATTR_CLASS, NEXUS_CLASS_ENTRY);
        //getDataManager().setAttribute(entryPath, ATTR_DEFAULT, getPlotPath(scan).replaceAll("/$", ""));
        getDataManager().setAttribute(entryPath, ATTR_DEFAULT, "plot");
        
        getDataManager().setDataset(entryPath + "/" + DSET_START_TIME, getNxTimeStr(scan.getStartTimestamp()));
        getDataManager().setDataset(entryPath + "/" + DSET_TITLE, scan.getTag());        
        getDataManager().setDataset(entryPath + "/" + DSET_EXP_ID, Context.getInstance().getSessionId());
        getDataManager().setDataset(entryPath + "/" + DSET_EXP_DESC, Context.getInstance().getSessionName());
        getDataManager().setDataset(entryPath + "/" + DSET_COLLECT_DESC, scan.toString());
        getDataManager().setDataset(entryPath + "/" + DSET_ENTRY_UUID, UUID.randomUUID().toString());
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        super.onRecord(scan, record);
    }

    @Override
    public void onFinish(Scan scan) throws IOException {
        super.onFinish(scan);
        String entryPath = getEntryPath(scan);                
        getDataManager().setDataset(entryPath + "/" + DSET_END_TIME, getNxTimeStr(System.currentTimeMillis()));
        getDataManager().setDataset(entryPath + "/" + DSET_DURATION, ((double) scan.getTimeElapsed())/1000.0);
        
        String dataPath = getScanPath(scan); 
        getDataManager().setAttribute(dataPath, ATTR_CLASS, NEXUS_CLASS_COLLECTION);
        
        
        String plotPath = getPlotPath(scan);
        getDataManager().createGroup(plotPath);
        
        //Copy attributes from measnuremente to to data (plot) group 
        Map<String,Object> atts = getDataManager().getAttributes(dataPath);
        for (String att:atts.keySet()){    
            if (!att.startsWith("NX")){
                getDataManager().setAttribute(plotPath, att, atts.get(att));
            }
        }
        getDataManager().setAttribute(plotPath, ATTR_CLASS, NEXUS_CLASS_DATA);
        
        for (Nameable dev : scan.getDevices()){
            getDataManager().createLink(plotPath + dev.getAlias(), getPath(scan,dev.getAlias()));
        }

        String[] readables = scan.getReadableNames();
        String[] writables = scan.getWritableNames();

        if (readables.length>0){
            getDataManager().setAttribute(plotPath, ATTR_SIGNAL, readables[0]);
            String[] aux = Arr.getSubArray(readables, 1);
            if (aux.length>0){
                 getDataManager().setAttribute(plotPath, ATTR_AUX_SIGNALS, aux); 
            }
        }
        if (writables.length>0){
            getDataManager().setAttribute(plotPath, ATTR_AXIS, writables);
            for (String writable: writables){
                Object dim = getDataManager().getAttribute(getPath(scan, writable), ATTR_WRITABLE_DIMENSION);
                Object index = getDataManager().getAttribute(getPath(scan, writable),ATTR_WRITABLE_INDEX);
                if ((dim!=null) && (((Number)dim).intValue()>0)){
                    getDataManager().setAttribute(plotPath, ATTR_AXISNAME_INDICES.formatted(writable), ((Number)dim).intValue() - 1);
                }
                
            }
        }
        getDataManager().setAttribute(plotPath, ATTR_TITLE, scan.getTag());                
    }

    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        if (NEXUS_CLASS_ENTRY.equals(getDataManager().getAttribute(root, path, ATTR_CLASS))){
            path = path + "plot/";
        }
        return super.getScanPlots(root, path, dm);
    }
}
