package ch.psi.pshell.data;

import ch.psi.pshell.app.Setup;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Processor;
import ch.psi.pshell.scan.PlotScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.Range;
import ch.psi.pshell.utils.Reflection.Hidden;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.pshell.sequencer.SequencerListener;


/**
 * Manages the automatic persistence of DAQ scritps.
 */
public class DataManager extends ch.psi.pshell.data.DataStore {

    static final Logger logger = Logger.getLogger(DataManager.class.getName());
    
    Layout layout;
    protected String dataRoot;    
    protected int fileSequentialNumber = -1;
    protected int daySequentialNumber = -1;
    
    static{
        formatClasses.put("fda", FormatFDA.class);
    }
    /**
     * Constructor for online data management
     */
    public DataManager(){
        outputFile = null;
        setFormat(new FormatHDF5());
        setLayout(new LayoutDefault());        
    }    

    public DataManager(String format, String layout) throws Exception {
        this();
        setFormat(format);
        setLayout(layout);
    }
    
    public DataManager(Format format, Layout layout) {
        this();
        setFormat(format);
        setLayout(layout);
    }
    
    public DataManager(String dataRoot, String format, String layout) throws Exception {
        this(format, layout);
        this.dataRoot=dataRoot;
    }
    
    public DataManager(String dataRoot, Format format, Layout layout) {
        this(format, layout);
        this.dataRoot=dataRoot;
    }
    
    public DataManager(String format) throws Exception {
        super(format);
    }

    public DataManager(Format format) {
        super(format);
    }


    public DataManager(File outputFile, String format) throws Exception {
        super(outputFile, format);
        setLayout(new LayoutDefault());   
    }
        
    public DataManager(File outputFile, Format format) throws Exception {
        super(outputFile, format);
        setLayout(new LayoutDefault());   
    }     
    
    public DataManager(File outputFile, String format, String layout) throws Exception {
        super(outputFile, format);
        setLayout(layout);
    }
        
    public DataManager(File outputFile, Format format, Layout layout) throws Exception {
        super(outputFile, format);
        setLayout(layout);
    }       
    

    /**
     * Configures the application data manager for scan persistence
     */
    public void initialize(String format, String layout) throws Exception {
        initialize(format, layout, null);
    }
    
    public void initialize(String format, String layout, Boolean embeddedAttributes) throws Exception {
        initialized = false;        
        logger.log(Level.INFO, "Initializing {0}", getClass().getSimpleName());
        Context.setDataManager(this);
        if (Context.hasSequencer()){
            Context.getSequencer().addScanListener(scanListener);
        }
        closeOutput();
        if (embeddedAttributes != null){            
            if (embeddedAttributes != FormatText.getDefaultEmbeddedAttributes()){                
                FormatText.setDefaultEmbeddedAttributes(embeddedAttributes);
                this.format = null; //Forces re-creation
            }
        }
        setFormat(format);
        setLayout(layout);
        logger.log(Level.INFO, "Finished {0} initialization", getClass().getSimpleName());
        initialized = true;
    }

    volatile boolean initialized;

    public boolean isInitialized() {
        return initialized;
    }

    public static String[] getFormatIds(){
        return Arr.append(DataStore.getFormatIds(), "fda");
    }

    public static String[] getLayoutIds(){
         return  new String[] {"default", "table", "sf", "fda", "nx"};
    }

    public static Class getLayoutClass(String name) throws ClassNotFoundException {
        Class layoutClass;
        String layoutName = (name == null) ? null : name.toLowerCase();

        if ((layoutName == null) || (layoutName.isEmpty()) || (layoutName.equals("default"))) {
            layoutClass = LayoutDefault.class;
        } else if (layoutName.equals("table")) {
            layoutClass = LayoutTable.class;
        } else if (layoutName.equals("sf")) {
            layoutClass = LayoutSF.class;
        } else if (layoutName.equals("nx")) {
            layoutClass = LayoutNX.class;
        } else if (layoutName.equals("fda")) {
            layoutClass = LayoutFDA.class;
        } else {
            //Class name
            layoutClass = Context.getClassByName(name);
        }
        return layoutClass;
    }        

    public void setLayout(String name) throws Exception {        
        Class layoutClass = getLayoutClass(name);
        if ((getLayout() != null) && (getLayout().getClass() == layoutClass)) {
            return;
        }
        setLayout((Layout) layoutClass.newInstance());
    }

    public void setLayout(Layout layout) {
        logger.log((this==global) ? Level.INFO : Level.FINER, "Setting data layout: {0}{1}", new Object[]{layout.getClass().getName(), (this==global) ? " on global instance" : ""});
        this.layout = layout;
        this.layout.initialize();
    }

    public Layout getLayout() {
        ExecutionParameters ep = getExecutionPars();
        Layout ret = (ep==null) ? null : ep.getDataLayout();
        return (ret == null) ? layout : ret;
    }
    

    @Override
    public Format getFormat() {
        ExecutionParameters ep = getExecutionPars();
        Format ret =  (ep==null) ? null : ep.getDataFormat();
        return (ret == null) ? super.getFormat() : ret;
    }    
    
    public static boolean isSameLayout(Object o1, Object o2){
        if ((o1!=null) && (o2!=null)){
            try{
                Class c1 =  (o1 instanceof String str) ? getLayoutClass(str) : o1.getClass();
                Class c2 =  (o2 instanceof String str) ? getLayoutClass(str) : o2.getClass();
                if ((c1==null) || (c2==null)){
                    return false;
                }
                return c1==c2;
            } catch (Exception ex){                
            }
        }
        return false;
    }
    
    protected void onCreateUnpackedDataset(File file){
        super.onCreateUnpackedDataset(file);
        try {
            Context.addDetachedFileToSession(file);
        } catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.WARNING, null, ex);
        }
    }
    
    
    public ExecutionParameters getExecutionPars() {
        return Context.getExecutionPars();
    }

    public boolean getPreserveTypes() {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ?true : ep.getPreserve();
    }

    public Map getStorageFeatures(Nameable device) {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ? new HashMap() : ep.getStorageFeatures(device);
    }
    
    
    public int getScanIndex() {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ? 0 : ep.getIndex();
    }

    public int getScanIndex(Scan scan) {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ?  0 : ep.getIndex(scan);
    }
    
    
    public String getScanPath() {
        return getScanPath(getCurrentScan());
    }

    public String getScanPath(Scan scan) {
        if (scan == null) {
            if (Context.hasSequencer()){
                if (Context.getSequencer().getRunningScriptName() == null) {
                    return null;
                }
            }
            return "/";
        }
        return getLayout().getScanPath(scan);
    }    
    
    
    public Scan[] getCurrentScans() {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ?  new Scan[0] : getExecutionPars().getCurrentScans();
    }

    public Scan getCurrentScan() {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ?  null : ep.getScan();
    }

    public String getCurrentGroup() {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ?  null : getCurrentGroup(ep.getScan());
    }

    public String getCurrentGroup(Scan scan) {
        if (scan == null) {
            if (Context.hasSequencer()){            
                if (Context.getSequencer().getRunningScriptName() == null) {
                    return null;
                }
            }
            return "/";
        }
        String ret = getLayout().getCurrentGroup(scan);
        return (ret != null) ? ret : null;
    }

    final ScanListener scanListener = new ScanListener() {
        final Object lock = new Object();
        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            if (scan instanceof PlotScan) {
                return;
            }
            synchronized (lock) {
                if (getExecutionPars().isScanPersisted(scan)) {
                    try {
                        openOutput();
                        if (isOpen()) {
                            getLayout().onStart(scan);
                            
                            if (scan.getMeta()!=null){
                                getLayout().onInitMeta(scan);
                            }
                            if (scan.getSnaps()!=null){
                                getLayout().onInitSnaps(scan);
                            }
                            if (scan.getDiags()!=null){
                                getLayout().onInitDiags(scan);
                            }
                            if (scan.getMonitors()!=null){
                                getLayout().onInitMonitors(scan);                                
                            }
                            appendLog(String.format("Scan %s started in: %s", getScanIndex(scan), getScanPath(scan)));
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (getExecutionPars().isScanPersisted(scan)) {
                try {
                    getLayout().onRecord(scan, record);
                    if (scan.getDiags()!=null){
                        getLayout().onDiags(scan);
                    }
                    if (getExecutionPars().getFlush()) {
                        flush();
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
        }
        
        @Override
        public void onMonitor(Scan scan, Device dev, Object value, long timestamp) {
            if (getExecutionPars().isScanPersisted(scan)) {
                try {
                    getLayout().onMonitor(scan, dev, value, timestamp);
                } catch (Exception e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
            
        }            

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
            if (getExecutionPars().isScanPersisted(scan)) {
                try {
                    getLayout().onFinish(scan);
                    appendLog(String.format("Scan %s ended", getScanIndex(scan)));
                } catch (Exception e) {
                    //Does not log if scan didn't triggered
                    logger.log(Level.WARNING, null, e);
                }
            }
        }
    };

    @Hidden
    public ScanListener getScanListener() {
        return scanListener;
    }

    @Override
    public int getDepthDimension() {
        ExecutionParameters ep = getExecutionPars();
        if (ep!=null){
            if (Context.getState().isProcessing()) {
                Integer  depthDimension = ep.getDepthDimension();
                if (depthDimension!=null){
                    return depthDimension;
                }
            }
        }
        return super.getDepthDimension();
    }
    
    public int[] getReadableMatrixDimension(ReadableMatrix readable) {
        return switch (getDepthDimension()) {
            case 1 -> new int[]{readable.getHeight(), 0, readable.getWidth()};
            case 2 -> new int[]{readable.getHeight(), readable.getWidth(), 0};
            default -> new int[]{0, readable.getHeight(), readable.getWidth()};
        };
    }

    Class getScanDatasetType(Readable device) throws IOException {
        //Check if device has interface if enforces type (ReadableType)
        Class type = ((Readable) device).getElementType();
        if ((type == null) || (type == Object.class)) {
            //If preserve type option, indentify types by reading cache
            if (getPreserveTypes()) {
                try {
                    type = device.resolveElementType();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (type!=null){
            if (Number.class.isAssignableFrom(type) || (type == String.class) || (type == Boolean.class)) {
               return type;
            }
        }
        //Else force double
        return Double.class;
    }
    
    public String getDataFolder() {
        if (dataRoot!=null){
            return dataRoot;
        }
        return Setup.getDataPath();
    }
    
    File getDataRootPath() {
        try {
            File ret = Paths.get(getRootFileName(getExecutionPars().getPath())).toFile();
            ret.mkdirs();
            return ret;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public int getCurrentFileSequentialNumber(){
        return fileSequentialNumber;
    }

    public int getCurrentDaySequentialNumber(){
        return daySequentialNumber;
    }

    

    public void appendOutputFile(String str){
       if (getLayout().getCreateLogs()){
            if (isOpen()){
                try {
                    getLayout().appendOutput(str);                
                } catch (IOException e) {
                }
            }
       }
    }    
    

    class OutputListener implements SequencerListener{
        StringBuilder output;
        public void start(){
            if (getLayout().getCreateLogs()){
                if (Context.hasSequencer()){
                    try {
                        Context.getSequencer().addListener(this);                        
                    } catch (Exception ex){
                        Context.getSequencer().removeListener(this);
                    }
                }
            }
        }

        public void stop() {
            if (Context.hasSequencer()){
                Context.getSequencer().removeListener(this);
            }
        }
        void append(String str){
            if (isOpen()){
                appendOutputFile(str);
            } else {
                stop();
            }
        }
        @Override
        public void onShellStdout(String str){
            append(str);
        }
        @Override
        public void onShellStderr(String str){
            append(str);
        }
        @Override
        public void onShellStdin(String str){
            append(">>> " + str);
        }
    };
    final OutputListener outputListener = new OutputListener();
    
    
    public String getRootFileName() {
        if (outputFile != null){
            return outputFile.getPath();
        } else {
            return getExecutionPars().getPath();
        }
    }
        
    public void openOutput() throws IOException {
        if (getExecutionPars()==null){
            super.openOutput();
        } else {
            //Continue using open file        
            if (isOpen()) {
                return;
            }
            getExecutionPars().initializeData();
            File dataPath = getDataRootPath();
            fileSequentialNumber = Context.getFileSequentialNumber();
            daySequentialNumber = Context.getDaySequentialNumber();

            if (dataPath != null) {                
                getExecutionPars().setDataPath(dataPath, true); //User getExecutionPars() to hold the datapath because it belongs to the running thread.
                Context.incrementSequentialNumbers();
                getLayout().onOpened(getExecutionPars().getOutputFile());
                if (getExecutionPars().getSave()) {
                    appendLog("Open persistence context: " + getExecutionPars().getOutputFile());
                }            
                try{
                    File script = getExecutionPars().getScriptFile();
                    if (script != null){
                        getLayout().onRunStarted(script);
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
                if (getExecutionPars().getSaveLogs()) {
                    outputListener.start();
                }
                if (Context.isHandlingSessions()){
                    if (getExecutionPars().getSessionId() >0) {
                        getLayout().writeSessionMetadata();
                    }
                }            
            }
        }
    }
    
    public void closeOutput() {
        ExecutionParameters ep = getExecutionPars();
        if (ep!=null){
            if (ep.getSaveLogs()) {
                outputListener.stop();
            }
        }
        if (outputFile!=null) { 
                try {
                    getFormat().closeOutput();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                } 
                return;
        }
        if (isOpen()) {
            try {
                if (ep!=null){
                    try {
                        if (ep.getSave()) {
                            appendLog("Close persistence context: " + ep.getOutputFile());
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
                try {
                    getLayout().onClosed(ep.getOutputFile());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
                try {
                    getFormat().closeOutput();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            } finally {
                if (ep!=null){
                    try {
                        ep.setDataPath(null, false);
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }
        }
    }
    
    public boolean isOpen() {
        if (outputFile != null){
            return super.isOpen();
        }
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ? false : getExecutionPars().isOpen();
    }

    public void splitScanData(Scan scan) throws IOException {
        if (!Arr.contains(getCurrentScans(), scan)) {
            throw new IOException("No ongoing scan");
        }
        ExecutionParameters ep = getExecutionPars();
        if (isOpen() && (ep.isScanPersisted(scan))) {
            int index = getScanIndex(scan);
            try {
                getLayout().onFinish(scan);
            } catch (Exception e) {
                logger.log(Level.WARNING, null, e);
            }                        
            ep.addScan(scan);
            ScanRecord rec = scan.getCurrentRecord();
            if (rec!=null){
                scan.setRecordIndexOffset(rec.getIndex()+1);
            }
            getLayout().onStart(scan);
            if (scan.getSnaps()!=null){
                getLayout().onInitSnaps(scan);
            }            
            if (scan.getMonitors()!=null){
                getLayout().onInitMonitors(scan);
            }            
            appendLog(String.format("Scan %s data was splitted with new index %s in: %s", index, getScanIndex(scan), getScanPath(scan)));
        }
    }

    public String getOutput() {
        assertOpen();
        if (outputFile!=null){
            return outputFile.getPath();
        }
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ? null : getExecutionPars().getOutput();
    }

    public String getLastOutput() {
        ExecutionParameters ep = getExecutionPars();
        return (ep==null) ? null : getExecutionPars().getLastOutput();
    }

    public String relativize(String path) {
        if (IO.isSubPath(path, getDataFolder())) {
            return IO.getRelativePath(path, getDataFolder());
        }
        return path;
    }
    
    public void appendLog(String log) throws IOException {
        if (getLayout().getCreateLogs()) {
            openOutput();
            if (isOpen()) {
                getLayout().appendLog(log);
                flush();  //Logs are always immediatelly flushed
            }
        }
    }
    
    public void createDataset(String path, Readable readable, int[] dimensions, Map features) throws IOException {
        createDataset(path, getScanDatasetType(readable), readable.isElementUnsigned(), dimensions, features);
    }

    public void createDataset(String path, Scan scan, Readable readable) throws IOException {
        Map features = getStorageFeatures(readable);
        boolean contiguous = isStorageFeaturesContiguous(features);
        int samples = contiguous ? scan.getNumberOfRecords() : 0;
        if (readable instanceof Readable.ReadableMatrix readableMatrix) {
            int[] dims = getReadableMatrixDimension(readableMatrix);
            if (contiguous) {
                dims[getDepthDimension()] = scan.getNumberOfRecords();
            }
            createDataset(path, readable, dims, features);
        } else if (readable instanceof Readable.ReadableArray readableArray) {
            createDataset(path, readable, new int[]{samples, readableArray.getSize()}, features);
        } else {
            createDataset(path, readable, new int[]{samples}, features);
        }
    }
    
    static boolean plotScalars = true;
            
    public static boolean getPlotScalars(){
        return plotScalars;
    }

    public static void setPlotScalars(boolean value){
        plotScalars = value;
    }

    public static boolean isPlottable(Map<String, Object> info) {
        return DataStore.isPlottable(info, plotScalars);
    }

    public List<PlotDescriptor> getChildrenPlots(String root, String path) throws IOException {
        List<PlotDescriptor> plots = new ArrayList<>();
        for (String child : getChildren(root, path)) {
            plots.addAll(getPlots(root, child));
        }
        return plots;
    }
    
    //Retrieve plots from dataset
    public List<PlotDescriptor> getPlots(String root, String path) throws IOException {
        path=adjustPath(path);
        ArrayList<PlotDescriptor> ret = new ArrayList<>();
        Map<String, Object> info = getInfo(root, path);
        String infoType = String.valueOf(info.get(Format.INFO_TYPE));
        if (infoType.equals(Format.INFO_VAL_TYPE_DATASET) || infoType.equals(Format.INFO_VAL_TYPE_SOFTLINK)) {
            DataSlice slice = getData(root, path);
            if (info.get(Format.INFO_DATA_TYPE) == Format.INFO_VAL_DATA_TYPE_COMPOUND) {
                Object[][] sliceData = (Object[][]) slice.sliceData;
                String[] names = (String[]) info.get(Format.INFO_FIELD_NAMES);
                int fields = (names != null) ? names.length : ((sliceData.length > 0) ? sliceData[0].length : 0);
                if (names == null) {
                    names = new String[fields];
                }
                if (fields > 0) {
                    int records = slice.sliceShape[0];
                    Object[][] data = new Object[fields][records];
                    for (int i = 0; i < records; i++) {
                        for (int j = 0; j < fields; j++) {
                            data[j][i] = (sliceData[i][j]);
                        }
                    }
                    if (Arr.equals(names,new String[]{Layout.FIELD_TIMESTAMP, Layout.FIELD_VALUE})){
                        //Monitor plots values against timestamp
                        String name = IO.getPrefix(path);
                        ret.add(new PlotDescriptor(name, data[1], (double[])Convert.toDouble(data[0])));
                    } else {
                        for (int j = 0; j < fields; j++) {
                            String name = names[j];
                            ret.add(new PlotDescriptor(name, data[j]));                            
                        }
                    }
                }

            } else if ((info.get(Format.INFO_DATA_TYPE) == Format.INFO_VAL_DATA_TYPE_FLOAT)
                    || (info.get(Format.INFO_DATA_TYPE) == Format.INFO_VAL_DATA_TYPE_INTEGER)
                    || (info.get(Format.INFO_DATA_TYPE) == Format.INFO_VAL_DATA_TYPE_BOOLEAN)
                    || (info.get(Format.INFO_DATA_TYPE) == Format.INFO_VAL_DATA_TYPE_BITFIELD)) {
                Object data = slice.sliceData;
                String name = IO.getPrefix(path);

                double[] calibration = null;
                Object o = getAttribute(root, path, Layout.ATTR_CALIBRATION);
                if (o instanceof double[] d) {
                    calibration = d;
                }

                double[] errorVector = null;
                o = getAttribute(root, path, Layout.ATTR_ERROR_VECTOR);
                if (o instanceof double[] d) {
                    errorVector = d;
                }

                if (slice.dataRank <= 3) {
                    double[] x = null;
                    double[] y = null;
                    if (calibration != null) {
                        if (calibration.length == 2) {
                            ArrayCalibration c = new ArrayCalibration(calibration[0], calibration[1]);
                            if (slice.dataRank == 2) {
                                y = c.getAxisX(slice.dataShape[1]);
                            } else {
                                x = c.getAxisX(slice.dataShape[0]);
                            }
                        } else if (calibration.length == 4) {
                            MatrixCalibration c = new MatrixCalibration(calibration[0], calibration[1], calibration[2], calibration[3]);
                            switch (getDepthDimension()) {
                                case 1:
                                    x = c.getAxisX(slice.dataShape[0]);
                                    y = c.getAxisY(slice.dataShape[2]);
                                    break;
                                case 2:
                                    x = c.getAxisX(slice.dataShape[1]);
                                    y = c.getAxisY(slice.dataShape[0]);
                                    break;
                                default:
                                    x = c.getAxisX(slice.dataShape[2]);
                                    y = c.getAxisY(slice.dataShape[1]);
                            }

                        }
                    }

                    PlotDescriptor plot = null;
                    if (slice.dataRank == 3) {
                        double[] z = Arr.indexesDouble(slice.getNumberSlices(getDepthDimension()));
                        plot = new PlotDescriptor(name, root, path, data, x, y, z);
                    } else {
                        plot = new PlotDescriptor(name, root, path, data, x, y);
                    }
                    plot.unsigned = slice.unsigned;
                    plot.error = errorVector;
                    ret.add(plot);
                }
            }
        }
        return ret;
    }
   

    public List<PlotDescriptor> getScanPlots(String root, String path) throws Exception {
        try{
            path=adjustPath(path);
            String layout = (String) getAttribute(root, "/", Layout.ATTR_LAYOUT);
            if ((layout != null) && (!layout.equals(getLayout().getClass().getName()))) {
                DataManager aux = new DataManager(getFormat().getClass().getName(), layout);
                try {
                    return aux.doGetScanPlots(root, path);
                } finally {
                    aux.close();
                }
            }
        } catch (Exception ex){
            logger.log(Level.FINER, null, ex);
        }
        return doGetScanPlots(root, path);
    }

    List<PlotDescriptor> doGetScanPlots(String root, String path) throws Exception {
        PlotPreferences plotPreferences = getPlotPreferences(root, path);
        Map<String, Object> attrs = getAttributes(root, path);
        if (attrs.containsKey(Layout.ATTR_TYPE)) {
           String type = (String) attrs.get(Layout.ATTR_TYPE);
            if (type!=null){
                for (Processor processor : Processor.getServiceProviders()) {
                    if (type.equals(processor.getScanType())){
                        throw new IOException("Yielding plotting to processor: " + processor); //Let processor make the drawing
                    }
                }
            }
        }        
        List<PlotDescriptor> plots = getLayout().getScanPlots(root, path, this);
        if (plots == null) {
            plots = new ArrayList<>();
            DataManager dm = Context.hasDataManager() ? Context.getDataManager() : this;
            if (dm.isGroup(root, path)) {
                for (String child : dm.getChildren(root, path)) {
                    plots.addAll(getPlots(root, child));
                }
            } else {
                plots.addAll(getPlots(root, path));
            }
        }

        plots.removeIf(val -> (plotPreferences.enabledPlots != null) && (!plotPreferences.enabledPlots.contains(val.name)));
        if (!plotScalars){
            plots.removeIf(val -> val.rank == 0);  
        }

        double[] xdata = null;
        if (plotPreferences.domainAxis != null) {
            try {
                if (plotPreferences.domainAxis.equals(ViewPreference.DOMAIN_AXIS_INDEX)) {
                    xdata = Arr.indexesDouble(Array.getLength(plots.get(0).data));
                } else if (plotPreferences.domainAxis.equals(ViewPreference.DOMAIN_AXIS_TIME)) {
                    String timestamps = getLayout().getTimestampsDataset(path);
                    if (timestamps != null) {
                        xdata = (double[]) Convert.toDouble(getData(root, timestamps).sliceData);
                        Long start = (Long) getAttribute(root, path, Layout.ATTR_START_TIMESTAMP);
                        if (start == null) {
                            start = ((Double) xdata[0]).longValue();
                        }
                        for (int i = 0; i < xdata.length; i++) {
                            xdata[i] = (xdata[i] - start) / 1000.0;
                        }
                    }
                } else {
                    for (PlotDescriptor plot : plots) {
                        if (plotPreferences.domainAxis.equals(plot.name)) {
                            xdata = (double[]) Convert.toDouble(plot.data);
                            plots.remove(plot);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                xdata = null;
            }
        }

        for (PlotDescriptor plot : plots) {
            if (plotPreferences.domainAxis != null) {
                if ((plot.rank < 3) && (xdata != null)) {
                    plot.setLabelX(plotPreferences.domainAxis);
                    plot.x = xdata;
                }
            }
            if ((plot.rank == 2) && (plot.root != null) && (plot.path != null) && (!plot.isMultidimentional1dArray())) {
                if (getLayout().isScanDataset(plot.root, plot.path, this)) {
                    plot.transpose();
                }
            }
        }
        return plots;
    }

    public PlotPreferences getPlotPreferences(String root, String path) {
        Map<String, Object> attrs = getAttributes(root, path);
        if (attrs != null) {
            PlotPreferences ret = new PlotPreferences();
            if (attrs.containsKey(Layout.ATTR_PLOT_ENABLE)) {
                String[] enabled = (String[]) attrs.get(Layout.ATTR_PLOT_ENABLE);
                ret.enabledPlots = Arrays.asList(enabled);
            }
            if (attrs.containsKey(Layout.ATTR_PLOT_RANGE)) {
                double[] range = (double[]) attrs.get(Layout.ATTR_PLOT_RANGE);
                if (Double.isNaN(range[0])) {
                    ret.autoRange = true;
                } else {
                    ret.range = new Range(range[0], range[1]);
                }
            }
            if (attrs.containsKey(Layout.ATTR_PLOT_RANGE_Y)) {
                double[] range = (double[]) attrs.get(Layout.ATTR_PLOT_RANGE_Y);
                if (!Double.isNaN(range[0])) {
                    ret.rangeY = new Range(range[0], range[1]);
                }
            }
            if (attrs.containsKey(Layout.ATTR_PLOT_DOMAIN)) {
                ret.domainAxis = (String) attrs.get(Layout.ATTR_PLOT_DOMAIN);
            }
            if (attrs.containsKey(Layout.ATTR_PLOT_TYPES)) {
                HashMap types = new HashMap();
                for (String token : ((String) attrs.get(Layout.ATTR_PLOT_TYPES)).split(Layout.ATTR_PLOT_TYPES_SEPARATOR)) {
                    String[] aux = token.split("=");
                    types.put(aux[0], aux[1]);
                }
                ret.setPlotTypes(types);
            }
            return ret;
        }
        return null;
    }
    
    @Override
    public void close() throws IOException {
        if (Context.hasSequencer()){
            Context.getSequencer().removeScanListener(scanListener);
        }
        closeOutput();
       
    }
}
