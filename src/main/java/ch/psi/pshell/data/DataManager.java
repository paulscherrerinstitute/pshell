package ch.psi.pshell.data;

import ch.psi.pshell.core.*;
import static ch.psi.pshell.data.Layout.FIELD_TIMESTAMP;
import static ch.psi.pshell.data.Layout.FIELD_VALUE;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.scan.PlotScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.IO.FilePermissions;
import ch.psi.utils.Str;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.utils.Range;
import java.lang.reflect.Array;
import java.util.Arrays;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.scripting.JepUtils;
import ch.psi.utils.Reflection.Hidden;
import java.util.List;
import java.util.Map;
import jep.NDArray;

/**
 * Manages the automatic persistence of DAQ scritps.
 */
public class DataManager implements AutoCloseable {

    final Context context;
    static final Logger logger = Logger.getLogger(DataManager.class.getName());
    Layout layout;
    Provider provider;
    int fileSequentialNumber = -1;
    int daySequentialNumber = -1;
    final File outputFile;
    FilePermissions filePermissions = FilePermissions.Default;

    class ProviderData {

        final HashMap<String, Integer> tableIndex = new HashMap<>();
        volatile DirectoryStream.Filter fileFilter;
    }

    final HashMap<Provider, ProviderData> providerData = new HashMap<>();

    /**
     * Constructor for online data management
     */
    public DataManager(Context context) {
        this.context = context;
        context.addScanListener(scanListener);
        outputFile = null;
    }

    /**
     * Constructor for single file dump
     */
    public DataManager(File file, String provider) throws Exception {
        this.context = null;
        outputFile = file;
        setProvider(provider);
        outputFile.mkdirs();
        getProvider().openOutput(outputFile);
    }

    /**
     * Constructor for offline data access
     */
    public DataManager(Context context, String provider, String layout) throws Exception {
        this.context = context;
        outputFile = null;
        setProvider(provider);
        setLayout(layout);
        dataRootDepth = Paths.get(IO.getRelativePath(getExecutionPars().getPath(), getDataFolder())).getNameCount();
    }

    int dataRootDepth;

    public void initialize() throws Exception {
        initialized = false;
        logger.info("Initializing " + getClass().getSimpleName());
        closeOutput();
        setCreateLogs(!context.getConfig().disableDataFileLogs);
        setEmbeddedAttributes(!context.getConfig().disableEmbeddedAttributes);
        setProvider(context.getConfig().getDataProvider());
        setLayout(context.getConfig().getDataLayout());
        dataRootDepth = Paths.get(IO.getRelativePath(getExecutionPars().getPath(), getDataFolder())).getNameCount();
        logger.info("Finished " + getClass().getSimpleName() + " initialization");
        initialized = true;
    }

    volatile boolean initialized;

    public boolean isInitialized() {
        return initialized;
    }

    public void setFilePermissions(FilePermissions filePermissions) {
        this.filePermissions = (filePermissions == null) ? FilePermissions.Default : filePermissions;
    }

    public static Class getProviderClass(String name) throws ClassNotFoundException {
        Class providerClass;
        String providerName = (name == null) ? null : name.toLowerCase();
        if ((providerName == null) || (providerName.isEmpty()) || (providerName.equals("default")) || (providerName.equals("h5"))) {
            providerClass = ProviderHDF5.class;
        } else if (providerName.equals("txt")) {
            providerClass = ProviderText.class;
        } else if (providerName.equals("csv")) {
            providerClass = ProviderCSV.class;
        } else if (providerName.equals("fda")) {
            providerClass = ProviderFDA.class;
        } else {
            //Class name
            providerClass = Context.getInstance().getClassByName(name);
        }
        return providerClass;
    }

    public void setProvider(String name) throws Exception {
        Class providerClass = getProviderClass(name);
        if ((provider != null)) {
            if (provider.getClass() == providerClass) {
                return;
            }
            synchronized (providerData) {
                providerData.remove(provider);
            }
        }
        setProvider((Provider) providerClass.newInstance());
    }

    public void setProvider(Provider provider) throws Exception {
        if (context != null){
            if (this == context.getDataManager()) {
                logger.info("Setting data provider: " + provider.getClass().getName());
            } else {
                logger.fine("Setting data provider in auxiliary data manager: " + provider.getClass().getName());
            }
        }
        this.provider = provider;
    }

    public Provider cloneProvider() {
        if (provider != null) {
            try {
                return provider.getClass().newInstance();
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public Provider getProvider() {
        Provider ret = (context !=null)  ? (getExecutionPars().getDataProvider()) : null;
        return (ret == null) ? provider : ret;
    }

    ProviderData getProviderData() {
        Provider provider = getProvider();
        synchronized (providerData) {
            ProviderData ret = providerData.get(provider);
            if (ret == null) {
                ret = new ProviderData();
                providerData.put(provider, ret);
            }
            return ret;
        }
    }

    void clearProviderData() {
        Provider provider = getProvider();
        synchronized (providerData) {
            //Keeps general provider data
            if (provider == this.provider) {
                ProviderData pd = providerData.get(provider);
                pd.tableIndex.clear();
            } else {
                providerData.remove(provider);
            }
        }
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
        } else if (layoutName.equals("fda")) {
            layoutClass = LayoutFDA.class;            
        } else {
            //Class name
            layoutClass = Context.getInstance().getClassByName(name);
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

    public void setLayout(Layout layout) throws Exception {
        if (this == context.getDataManager()) {
            logger.info("Setting data layout: " + layout.getClass().getName());
        } else {
            logger.fine("Setting data layout in auxiliary data manager: " + layout.getClass().getName());
        }
        this.layout = layout;
        this.layout.initialize();
    }

    public Layout getLayout() {
        Layout ret = getExecutionPars().getDataLayout();
        return (ret == null) ? layout : ret;
    }

    boolean createLogs = true;

    public boolean getCreateLogs() {
        return createLogs;
    }

    public void setCreateLogs(boolean value) {
        createLogs = value;
    }

    boolean embeddedAttributes = true;

    public boolean getEmbeddedAttributes() {
        return embeddedAttributes;
    }

    public void setEmbeddedAttributes(boolean value) {
        embeddedAttributes = value;
    }

    
    public String getDataFileType() {
        return getProvider().getFileType();
    }

    public boolean isDataPacked() {
        return getProvider().isPacked();
    }

    public ExecutionParameters getExecutionPars() {
        return context.getExecutionPars();
    }

    public boolean getPreserveTypes() {
        return getExecutionPars().getPreserve();
    }

    public Map getStorageFeatures(Nameable device) {
        return getExecutionPars().getStorageFeatures(device);
    }

    public boolean isStorageFeaturesContiguous(Map features) {
        return (features != null) && ("contiguous".equals(features.get("layout")));
    }

    public boolean isStorageFeaturesCompressed(Map features) {
        return (features != null) && (features.get("compression") != null)
                && (!"false".equalsIgnoreCase(String.valueOf(features.get("compression"))));
    }

    public int getDepthDimension() {
        Context context = Context.getInstance();
        if (context.getState().isProcessing()) {
            return getExecutionPars().getDepthDimension();
        }
        return context.getConfig().getDepthDim();
    }

    public int[] getReadableMatrixDimension(ReadableMatrix readable) {
        int[] dimensions;
        switch (getDepthDimension()) {
            case 1:
                dimensions = new int[]{readable.getHeight(), 0, readable.getWidth()};
                break;
            case 2:
                dimensions = new int[]{readable.getHeight(), readable.getWidth(), 0};
                break;
            default:
                dimensions = new int[]{0, readable.getHeight(), readable.getWidth()};
                break;
        }
        return dimensions;
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

    public DirectoryStream.Filter getFileFilter() {
        ProviderData pd = getProviderData();
        if (pd == null) {
            return null;
        }
        if (pd.fileFilter == null) {
            pd.fileFilter = (DirectoryStream.Filter<Path>) (Path path) -> {
                File file = path.toFile();
                if (file.isDirectory()) {
                    if (!isDataPacked()) {
                        if (isRoot(file.getParent())) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    if (isDataPacked()) {
                        if (getDataFileType() != null) {
                            String ext = IO.getExtension(file);
                            if (ext.equals(getDataFileType())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            };
        }
        return pd.fileFilter;
    }

    public int getScanIndex() {
        return getExecutionPars().getIndex();
    }

    public Scan[] getCurrentScans() {
        return getExecutionPars().getCurrentScans();
    }

    public Scan getCurrentScan() {
        return getExecutionPars().getCurrentScan();
    }

    public int getScanIndex(Scan scan) {
        return getExecutionPars().getIndex(scan);
    }

    public String getCurrentGroup() {
        return getCurrentGroup(getExecutionPars().getCurrentScan());
    }

    public String getCurrentGroup(Scan scan) {
        if (scan == null) {
            if (context.getRunningScriptName() == null) {
                return null;
            }
            return "/";
        }
        String ret = getLayout().getCurrentGroup(scan);
        return (ret != null) ? ret : null;
    }

    public String getScanPath() {
        return getScanPath(getExecutionPars().getCurrentScan());
    }

    public String getScanPath(Scan scan) {
        if (scan == null) {
            if (context.getRunningScriptName() == null) {
                return null;
            }
            return "/";
        }
        return getLayout().getScanPath(scan);
    }

    final ScanListener scanListener = new ScanListener() {

        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            if (scan instanceof PlotScan) {
                return;
            }
            synchronized (this) {
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
                    logger.log(Level.WARNING, null, e);
                }
            }
        }
    };

    @Hidden
    public ScanListener getScanListener() {
        return scanListener;
    }

    public String getDataFolder() {
        return context.getSetup().getDataPath();
    }
    

    public String getRootFileName() {
        if (outputFile != null){
            return outputFile.getPath();
        } else {
            return getExecutionPars().getPath();
        }
    }
    
    public String getRootFileName(String root) {
        return getProvider().getRootFileName(root);
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

    class OutputListener extends ContextAdapter{
        StringBuilder output;
        public void start(){
            output = new StringBuilder();
            try {
                context.addListener(this);
                createDataset(getLayout().getOutputFilePath(), String.class);
            } catch (Exception ex){
                output = null;
                context.removeListener(this);
            }
        }

        public void stop() {
            context.removeListener(this);
        }

        void append(String str){
            if (isOpen()){
                try {
                    appendItem(getLayout().getOutputFilePath(),str);
                } catch (IOException e) {
                }
            } else {
                stop();
            }
        }
        public void onShellStdout(String str){
            append(str);
        }
        public void onShellStderr(String str){
            append(str);
        }
        public void onShellStdin(String str){
            append(">>> " + str);
        }
    };
    final OutputListener outputListener = new OutputListener();


    public int getCurrentFileSequentialNumber(){
        return fileSequentialNumber;
    }

    public int getCurrentDaySequentialNumber(){
        return daySequentialNumber;
    }

    public void openOutput() throws IOException {
        //Continue using open file        
        if (isOpen()) {
            return;
        }
        fileSequentialNumber = context.getFileSequentialNumber();
        daySequentialNumber = context.getDaySequentialNumber();
        getExecutionPars().initializeData();
        File dataPath = getDataRootPath();
        if (dataPath != null) {
            getExecutionPars().setDataPath(dataPath);
            getProvider().openOutput(dataPath);
            IO.setFilePermissions(dataPath, filePermissions);
            context.incrementSequentialNumbers();
            getLayout().onOpened(getExecutionPars().getOutputFile());
            if (getExecutionPars().getSave()) {
                appendLog("Open persistence context: " + getExecutionPars().getOutputFile());
            }
            if (getExecutionPars().getSaveScripts()){
                try{
                    File script = getExecutionPars().getScriptFile();
                    if (script != null){
                        getLayout().saveScript(script.getName(), new String(Files.readAllBytes(script.toPath())));
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
            if (getExecutionPars().getSaveOutput()) {
                outputListener.start();
            }
            if (Context.getInstance().isHandlingSessions()){
                if (getExecutionPars().getSessionId() >0) {
                    getLayout().writeSessionMetadata();
                }
            }            
        }
    }

    public void flush() {
        getProvider().flush();
    }

    public void closeOutput() {
        if (getExecutionPars().getSaveOutput()) {
            outputListener.stop();
        }
        if (outputFile!=null) { 
                try {
                    getProvider().closeOutput();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                } 
                return;
        }
        if (isOpen()) {
            try {
                try {
                    if (getExecutionPars().getSave()) {
                        appendLog("Close persistence context: " + getExecutionPars().getOutputFile());
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
                try {
                    getLayout().onClosed(getExecutionPars().getOutputFile());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
                try {
                    clearProviderData();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
                try {
                    getProvider().closeOutput();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            } finally {
                getExecutionPars().setDataPath(null);
            }
        }
    }

    public void splitScanData(Scan scan) throws IOException {
        if (!Arr.contains(getCurrentScans(), scan)) {
            throw new IOException("No ongoing scan");
        }
        if (isOpen() && (getExecutionPars().isScanPersisted(scan))) {
            int index = getScanIndex(scan);
            getLayout().onFinish(scan);            
            getExecutionPars().addScan(scan);
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

    public boolean isOpen() {
        if (outputFile != null){
            return true;
        }
        return getExecutionPars().isOpen();
    }

    public String getOutput() {
        assertOpen();
        return getExecutionPars().getOutput();
    }

    public String getLastOutput() {
        return getExecutionPars().getLastOutput();
    }

    public String relativize(String path) {
        if (IO.isSubPath(path, getDataFolder())) {
            return IO.getRelativePath(path, getDataFolder());
        }
        return path;
    }

    public static class DataOutputClosed extends IllegalStateException{
        DataOutputClosed(){
            super("Data output not instantiated");
        }
    }
    public void assertOpen() {
        if (!isOpen()) {
            throw new DataOutputClosed();
        }
    }

    //File reading        
    public boolean isRoot(String path) {
        try {
            String rel = IO.getRelativePath(path, getDataFolder());
            return Paths.get(rel).getNameCount() == dataRootDepth;
        } catch (Exception ex) {
        }
        return false;
    }

    public Object[] getStructure(String root) throws IOException {
        root = adjustRoot(root);
        Object[] ret = getProvider().getStructure(root);

        if ((ret != null) && (ret.length > 0) && (ret[0].equals(""))) {
            ret[0] = new File(root).getName();
        }
        return ret;
    }

    public String[] getChildren(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getChildren(address.root, address.path);
        }
        return getChildren(getOutput(), path);
    }

    public String[] getChildren(String root, String path) throws IOException {
        root = adjustRoot(root);
        try {
            if (isGroup(root, path)) {
                return getProvider().getChildren(root, path);
            }
        } catch (Exception ex) {
        }
        return new String[0];
    }

    public boolean isDataset(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return isDataset(address.root, address.path);
        }
        return getProvider().isDataset(getOutput(), path);
    }

    public boolean isDataset(String root, String path) throws IOException {
        root = adjustRoot(root);
        return getProvider().isDataset(root, path);
    }

    public boolean isGroup(String path) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return isGroup(address.root, address.path);
        }
        return getProvider().isGroup(getOutput(), path);
    }

    public boolean isGroup(String root, String path) throws IOException {
        root = adjustRoot(root);
        return getProvider().isGroup(root, path);
    }

    public static class DataAddress {
        DataAddress(String root, String path){
            this.root=root;
            this.path=path;
        }
        final public String root;
        final public String path;
    }

    public static DataAddress getAddress(String path) {
        String[] rootDelimitors = new String[]{"|", " /"};
        for (String delimitor : rootDelimitors) {
            if (path.contains(delimitor)) {
                int index = path.indexOf(delimitor);
                return new DataAddress(path.substring(0, index).trim(), path.substring(index + delimitor.length()).trim());
            }
        }
        return null;
    }

    public static String getFullPath(String root, String path) {
        return root + "|" + path;
    }

    public DataSlice getData(String path) throws IOException {
        return getData(path, 0);
    }

    /**
     * Shortcut for accessing 3d data in 2d arrays.
     */
    public DataSlice getData(String path, int index) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getData(address.root, address.path, index);
        }
        return getData(getOutput(), path, index);
    }

    public DataSlice getData(String root, String path) throws IOException {
        return getData(root, path, 0);
    }

    /**
     * Shortcut for accessing 3d data in 2d arrays.
     */
    public DataSlice getData(String root, String path, int index) throws IOException {
        root = adjustRoot(root);

        DataSlice ret = getProvider().getData(root, path, index);
        if (ret == null) {
            throw new UnsupportedOperationException(String.format("Invalid data path : %s-%s-%d", root, path, index));
        }

        if (ret.sliceData == null) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return ret;
    }

    public Object getData(String path, long[] index, int[] shape) throws IOException {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getData(address.root, address.path, index, shape);
        }
        return getData(getOutput(), path, index, shape);
    }

    public DataSlice getData(String root, String path, long[] index, int[] shape) throws IOException {
        root = adjustRoot(root);

        DataSlice ret = getProvider().getData(root, path, index, shape);
        if (ret == null) {
            throw new UnsupportedOperationException(String.format("Invalid data path : %s-%s-%s", root, path, Convert.arrayToString(index, ".")));

        }
        if (ret.sliceData == null) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return ret;
    }

    String adjustRoot(String root) {
        if (!new File(root).exists()) {
            String base = getDataFolder();
            if (!IO.isSubPath(root, base)) {
                root = Paths.get(base, root).toString();
            }
        }
        return root;
    }

    public Map<String, Object> getInfo(String path) {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getInfo(address.root, address.path);
        }
        return getInfo(getOutput(), path);
    }

    public Map<String, Object> getInfo(String root, String path) {
        root = adjustRoot(root);

        Map<String, Object> ret;
        try {
            ret = getProvider().getInfo(root, path);
        } catch (Exception ex) {
            ret = new HashMap<>();
            ret.put("Exception", ex.getMessage());
        }
        return ret;
    }

    public Map<String, Object> getAttributes(String path) {
        DataAddress address = getAddress(path);
        if (address != null) {
            return getAttributes(address.root, address.path);
        }
        return getAttributes(getOutput(), path);
    }

    public Map<String, Object> getAttributes(String root, String path) {
        root = adjustRoot(root);

        Map<String, Object> ret;
        try {
            ret = getProvider().getAttributes(root, path);
        } catch (Exception ex) {
            ret = new HashMap<>();
            ret.put("Exception", ex.getMessage());
        }
        return ret;
    }

    public Object getAttribute(String path, String name) {
        Map<String, Object> attrs = getAttributes(path);
        return attrs.get(name);
    }

    public Object getAttribute(String root, String path, String name) {
        Map<String, Object> attrs = getAttributes(root, path);
        return attrs.get(name);
    }

    public boolean exists(String path) {
        DataAddress address = getAddress(path);
        if (address != null) {
            return exists(address.root, address.path);
        }
        return exists(getOutput(), path);
    }

    public boolean exists(String root, String path) {
        root = adjustRoot(root);
        try {
            Map<String, Object> info = getProvider().getInfo(root, path);
            if ((info == null) || (String.valueOf(info.get(Provider.INFO_TYPE)).equals(Provider.INFO_VAL_TYPE_UNDEFINED))) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void appendLog(String log) throws IOException {
        if (getCreateLogs() && getLayout().getCreateLogs() && getLayout().getLogsPath()!=null) {
            openOutput();
            if (isOpen()) {
                getLayout().appendLog(log);
                flush();  //Logs are always immediatelly flushed
            }
        }
    }

    public void createGroup(String path) throws IOException {
        synchronized (providerData) {
            openOutput();
            getProvider().createGroup(path);
            if (!getProvider().isPacked()){
                File file = getProvider().getFilePath(path).toFile();
                IO.setFilePermissions(file, filePermissions);
            }            
        }
    }

    public void setDataset(String path, Object data) throws IOException {
        setDataset(path, data, false);
    }

    public void setDataset(String path, Object data, boolean unsigned) throws IOException {
        setDataset(path, data, unsigned, null);
    }

    public void setDataset(String path, Object data, boolean unsigned, Map features) throws IOException {
        if ((data == null) || (path == null) || (!path.contains("/"))) {
            throw new IllegalArgumentException();
        }
        openOutput();

        int index = path.lastIndexOf("/");
        String group = path.substring(0, index + 1);
        String name = path.substring(index + 1);

        if (data instanceof NDArray){
            data = JepUtils.toJavaArray((NDArray)data);
        } 
        Class type = data.getClass().isArray() ? Arr.getComponentType(data) : data.getClass();
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }

        int[] shape = Arr.getShape(data);
        int rank = shape.length;

        logger.finer(String.format("Set \"%s\" type = %s dims = %s", path, type.getSimpleName(), rank, Str.toString(shape, 10)));
        createGroup(group);              
        getProvider().setDataset(path, data, type, rank, shape, unsigned, features);
        if (!getProvider().isPacked()){
            File file = getProvider().getFilePath(path).toFile();
            Context.getInstance().addDetachedFileToSession(file);
            IO.setFilePermissions(file, filePermissions);
        }
        flush();
    }

    public void createDataset(String path, Class type) throws IOException {
        createDataset(path, type, null);
    }

    public void createDataset(String path, Readable readable, int[] dimensions, Map features) throws IOException {
        createDataset(path, getScanDatasetType(readable), readable.isElementUnsigned(), dimensions, features);
    }

    public void createDataset(String path, Scan scan, Readable readable) throws IOException {
        Map features = getStorageFeatures(readable);
        boolean contiguous = isStorageFeaturesContiguous(features);
        int samples = contiguous ? scan.getNumberOfRecords() : 0;
        if (readable instanceof Readable.ReadableMatrix) {
            int[] dims = getReadableMatrixDimension((Readable.ReadableMatrix) readable);
            if (contiguous) {
                dims[getDepthDimension()] = scan.getNumberOfRecords();
            }
            createDataset(path, readable, dims, features);
        } else if (readable instanceof Readable.ReadableArray) {
            createDataset(path, readable, new int[]{samples, ((Readable.ReadableArray) readable).getSize()}, features);
        } else {
            createDataset(path, readable, new int[]{samples}, features);
        }
    }

    public void createDataset(String path, Class type, int[] dimensions) throws IOException {
        createDataset(path, type, null, dimensions);
    }

    public void createDataset(String path, Class type, int[] dimensions, Map features) throws IOException {
        createDataset(path, type, null, dimensions, features);
    }

    public void createDataset(String path, Class type, Boolean unsigned, int[] dimensions) throws IOException {
        createDataset(path, type, unsigned, dimensions, null);
    }

    public void createDataset(String path, Class type, Boolean unsigned, int[] dimensions, Map features) throws IOException {
        if ((type == null) || (path == null) || (!path.contains("/"))) {
            throw new IllegalArgumentException();
        }
        if (unsigned == null) {
            unsigned = ((type == Byte.class) || (type == Short.class)); //Default unsigned for byte and shortt
        }
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        if (dimensions == null) {
            dimensions = new int[]{0};
        }

        openOutput();
        String group = path.substring(0, path.lastIndexOf("/") + 1);
        createGroup(group);

        logger.finer(String.format("Create \"%s\" type = %s dims = %s", path, type.getSimpleName(), Str.toString(dimensions, 10)));
        getProvider().createDataset(path, type, dimensions, unsigned, features);
        if (!getProvider().isPacked()){
            File file = getProvider().getFilePath(path).toFile();
            Context.getInstance().addDetachedFileToSession(file);
            IO.setFilePermissions(file, filePermissions);
        }
        ProviderData pd = getProviderData();
    }

    public void createDataset(String path, String[] names, Class[] types, int[] lengths) throws IOException {
        createDataset(path, names, types, lengths, null);
    }

    public void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        if ((names == null) || (path == null) || (!path.contains("/"))) {
            throw new IllegalArgumentException();
        }
        openOutput();
        int fields = names.length;
        if (types == null) {
            types = new Class[fields];
        }
        if (lengths == null) {
            lengths = new int[fields];
        }

        if ((fields != types.length) || (fields != lengths.length)) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < fields; i++) {
            if (types[i] == null) {
                types[i] = String.class;
            } else if (types[i].isPrimitive()) {
                types[i] = Convert.getWrapperClass(types[i]);
            }
        }
        String group = path.substring(0, path.lastIndexOf("/") + 1);
        createGroup(group);

        logger.finer(String.format("Create \"%s\"", path));
        getProvider().createDataset(path, names, types, lengths, features);
        if (!getProvider().isPacked()){
            File file = getProvider().getFilePath(path).toFile();
            Context.getInstance().addDetachedFileToSession(file);
            IO.setFilePermissions(file, filePermissions);
        }      
        flush();
    }
    
    //Overloaded methods are not seen by JEP so creating a proxy 
    public void createTable(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        createDataset(path, names, types, lengths, features);
    }
    
    public void setItem(String path, Object value, int index) throws IOException {
        if (path == null) {
            return;
        }
        Class type = null;
        if (value != null) {
            type = value.getClass();
            if (type ==  NDArray.class){
                value = JepUtils.toJavaArray((NDArray)value);
                type = value.getClass();
            }            
            if (type.isPrimitive()) {
                type = Convert.getWrapperClass(type);
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("Append  \"%s:%d\" = %s", path, index, LogManager.getLogForValue(value)));
        }
        assertOpen(); //Avoid NullPointerException if don't have rights to data folder
        getProvider().setItem(path, value, type, index);
    }

    /**
     * Write to multi-dimensional dataset. val must be a 1d array
     */
    public void setItem(String path, Object val, long[] index, int[] shape) throws IOException {
        if (path == null) {
            return;
        }
        Class type = null;
        if (val != null) {
            type = val.getClass();
            if (type ==  NDArray.class){
                val = JepUtils.toJavaArray((NDArray)val);
                type = val.getClass();
            }            
            if ((!type.isArray()) || (!type.getComponentType().isPrimitive())) {
                throw new IllegalArgumentException("Value must be a 1d array of primitive type");
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("Append  \"%s:%s\" = %s", path, Convert.arrayToString(index, "."), LogManager.getLogForValue(val)));
        }
        assertOpen(); //Avoid NullPointerException if don't have rights to data folder
        getProvider().setItem(path, val, type, index, shape);
    }

    /**
     * Convenient but slow
     */
    public void appendItem(String path, Object val) throws IOException {
        Integer index = 0;
        ProviderData pd = getProviderData();
        synchronized (pd.tableIndex) {
            index = pd.tableIndex.get(path);
            if (index == null) {
                index = 0;
            } else {
                index++;
            }
            pd.tableIndex.put(path, index);
        }
        setItem(path, val, index);
    }

    public void setAttribute(String path, String name, Object value) throws IOException {
        setAttribute(path, name, value, false);
    }

    public void setAttribute(String path, String name, Object value, boolean unsigned) throws IOException {
        if ((name == null) || (name.isEmpty()) || (path == null) || (!path.contains("/")) || (value == null)) {
            throw new IllegalArgumentException();
        }
        openOutput();
        Class type = value.getClass();
        if (type ==  NDArray.class){
            value = JepUtils.toJavaArray((NDArray)value);
            type = value.getClass();
        }          
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        logger.finer(String.format("Set attribute \"%s/%s\" value = %s", path, name, Str.toString(value, 10)));
        getProvider().setAttribute(path, name, value, type, unsigned);

        if (!getProvider().isPacked()){
            File file = getProvider().getAttributePath(path).toFile();
            IO.setFilePermissions(file, filePermissions);
        }
    }

    /**
     * Retrieve plots from dataset
     */
    public List<PlotDescriptor> getPlots(String root, String path) throws IOException {
        ArrayList<PlotDescriptor> ret = new ArrayList<>();
        Map<String, Object> info = getInfo(root, path);
        String infoType = String.valueOf(info.get(Provider.INFO_TYPE));
        if (infoType.equals(Provider.INFO_VAL_TYPE_DATASET) || infoType.equals(Provider.INFO_VAL_TYPE_SOFTLINK)) {
            DataSlice slice = getData(root, path);
            if (info.get(Provider.INFO_DATA_TYPE) == Provider.INFO_VAL_DATA_TYPE_COMPOUND) {
                Object[][] sliceData = (Object[][]) slice.sliceData;
                String[] names = (String[]) info.get(Provider.INFO_FIELD_NAMES);
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
                    if (Arr.equals(names,new String[]{FIELD_TIMESTAMP, FIELD_VALUE})){
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

            } else if ((info.get(Provider.INFO_DATA_TYPE) == Provider.INFO_VAL_DATA_TYPE_FLOAT)
                    || (info.get(Provider.INFO_DATA_TYPE) == Provider.INFO_VAL_DATA_TYPE_INTEGER)
                    || (info.get(Provider.INFO_DATA_TYPE) == Provider.INFO_VAL_DATA_TYPE_BOOLEAN)
                    || (info.get(Provider.INFO_DATA_TYPE) == Provider.INFO_VAL_DATA_TYPE_BITFIELD)) {
                Object data = slice.sliceData;
                String name = IO.getPrefix(path);

                double[] calibration = null;
                Object o = getAttribute(root, path, Layout.ATTR_CALIBRATION);
                if ((o != null) && (o instanceof double[])) {
                    calibration = (double[]) o;
                }

                double[] errorVector = null;
                o = getAttribute(root, path, Layout.ATTR_ERROR_VECTOR);
                if ((o != null) && (o instanceof double[])) {
                    errorVector = (double[]) o;
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
                        double[] z = Arr.indexesDouble(slice.getNumberSlices());
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
    
    void checkLogFile(String logFile) throws IOException {
        if (!exists(logFile)) {
            createDataset(logFile, String.class);
        } 
        getProvider().checkLogFile(logFile);
    }

    public List<PlotDescriptor> getScanPlots(String root, String path) throws Exception {
        try{
            String layout = (String) getAttribute(root, "/", Layout.ATTR_LAYOUT);
            if ((layout != null) && (!layout.equals(getLayout().getClass().getName()))) {
                DataManager aux = new DataManager(context, getProvider().getClass().getName(), layout);
                try {
                    return aux.doGetScanPlots(root, path);
                } finally {
                    aux.close();
                }
            }
        } catch (Exception ex){
            logger.log(Level.INFO, null, ex);
        }
        return doGetScanPlots(root, path);
    }

    List<PlotDescriptor> doGetScanPlots(String root, String path) throws Exception {

        PlotPreferences plotPreferences = getPlotPreferences(root, path);
        List<PlotDescriptor> plots = getLayout().getScanPlots(root, path, this);
        if (plots == null) {
            plots = new ArrayList<>();
            DataManager dm = context.getDataManager();
            if (dm.isGroup(root, path)) {
                for (String child : dm.getChildren(root, path)) {
                    plots.addAll(getPlots(root, child));
                }
            } else {
                plots.addAll(getPlots(root, path));
            }
        }

        plots.removeIf(val -> (plotPreferences.enabledPlots != null) && (!plotPreferences.enabledPlots.contains(val.name)));
        plots.removeIf(val -> val.rank == 0);   //Don't plot scalar datasets    

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
                    plot.labelX = plotPreferences.domainAxis;
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

    public boolean isDisplayablePlot(Map<String, Object> info) {
        if (info != null){
            String infoType = String.valueOf(info.get(Provider.INFO_TYPE));
            boolean isDataset = infoType.equals(Provider.INFO_VAL_TYPE_DATASET) || infoType.equals(Provider.INFO_VAL_TYPE_SOFTLINK);
            if (isDataset && (((Integer) info.get(Provider.INFO_RANK)) > 0)) {
                String dataType = (String) info.get(Provider.INFO_DATA_TYPE);
                if (dataType != null) {
                    switch (dataType) {
                        case Provider.INFO_VAL_DATA_TYPE_COMPOUND:
                        case Provider.INFO_VAL_DATA_TYPE_FLOAT:
                        case Provider.INFO_VAL_DATA_TYPE_INTEGER:
                        case Provider.INFO_VAL_DATA_TYPE_BITFIELD:
                        case Provider.INFO_VAL_DATA_TYPE_BOOLEAN:
                            return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        closeOutput();
        synchronized (providerData) {
            providerData.clear();
        }
        if (context!=null) {            
            context.removeScanListener(scanListener);
        }
    }
}
