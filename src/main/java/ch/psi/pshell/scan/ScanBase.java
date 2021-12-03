package ch.psi.pshell.scan;

import ch.psi.pshell.core.InlineDevice;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.Waveform;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ExecutionParameters;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.device.*;
import ch.psi.pshell.device.Readable;
import ch.psi.utils.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class ScanBase extends ObservableBase<ScanListener> implements Scan {

    static final Logger logger = Logger.getLogger(ScanBase.class.getName());

    final Writable[] writables;
    final Readable[] readables;
    final double[] start;
    final double[] end;
    final double[] stepSize;
    final int[] numberOfSteps;
    final boolean relative;
    final int latency;
    final int passes;
    final boolean zigzag;

    ScanResult result;
    int recordIndex = 0;
    int recordIndexOffset = 0;
    int passOffset = 0;
    long startTimestamp;
    long endTimestamp;
    
    String plotTitle;
    boolean hidden;
    boolean splitPasses;
    
    int scanIndex = -1;
    String scanPath;
    String tag;
    int pass = 1;
    boolean keep;
    String name = "Scan";
    Layout dataLayout;
    Provider dataProvider;
    Device[] monitors;
    Readable[] diags;
    Readable[] snaps;
    Map meta;

    boolean useWritableReadback = getScansUseWritableReadback();
    boolean restorePosition = getRestorePositionOnRelativeScans();
    boolean abortOnReadableError = getAbortScansOnReadableError();
    boolean checkPositions = ScanBase.getScansCheckPositions();
    boolean parallelPositioning = getScansParallelPositioning();
    boolean initialMove = getScansTriggerInitialMove();
    boolean resampleOnInvalidate = true;

    public ScanBase(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[],
            boolean relative, int latency, int passes, boolean zigzag) {
        this.writables = writables;
        this.readables = readables;
        this.start = start;
        this.end = end;
        this.relative = relative;
        this.latency = latency;
        this.passes = passes;
        this.zigzag = zigzag;

        if ((numberOfSteps.length == 1) && (writables.length > 1)) {
            int steps = numberOfSteps[0];
            numberOfSteps = new int[writables.length];
            Arrays.fill(numberOfSteps, steps);
        }
        this.numberOfSteps = numberOfSteps;

        stepSize = new double[start.length];
        for (int i = 0; i < start.length; i++) {
            if (this.numberOfSteps[i] == 0) {
                stepSize[i] = 0.0;
                end[i] = start[i];
            } else {
                stepSize[i] = (end[i] - start[i]) / this.numberOfSteps[i];
            }
        }
        assertFieldsOk();
    }

    public ScanBase(Writable[] writables, Readable[] readables, double[] start, double[] end, double stepSize[],
            boolean relative, int latency, int passes, boolean zigzag) {
        this.writables = writables;
        this.readables = readables;
        this.start = start;
        this.end = end;
        this.relative = relative;
        this.latency = latency;
        this.passes = passes;
        this.zigzag = zigzag;
        this.stepSize = stepSize;
        this.numberOfSteps = new int[start.length];
        assertFieldsOk();
        for (int i = 0; i < start.length; i++) {
            if (stepSize[i] == 0) {
                this.numberOfSteps[i] = 0;
            } else {
                //this.numberOfSteps[i] = (int) Math.ceil((end[i] - start[i]) / stepSize[i]);            
                //Adding the final point if steps are not integer generates problems in plotting
                this.numberOfSteps[i] = (int) Math.floor((end[i] - start[i]) / stepSize[i]);
            }
            this.end[i] = start[i] + stepSize[i] * this.numberOfSteps[i];
        }
        //if (Context.getInstance() != null) {
        //    for (ScanListener listener : Context.getInstance().getScanListeners()) {
        //        addListener(listener);
        //    }
        //}
    }
    
    @Override
    public Layout getDataLayout(){
        return dataLayout;
    }
    
    @Override
    public Provider getDataProvider(){
        return dataProvider;
    }    
    
    @Override
    public void setMeta(Map meta){
        this.meta = meta;
        if (isStarted()){
            try{
                getDataLayout().onMeta(this, meta);
            } catch(Exception ex){
                logger.log(Level.WARNING, null, ex);
            }
        }
    }
    
    @Override
    public Map getMeta(){
        return meta;
    }

    public void addMeta(Map<String, Object> meta) throws IOException{
        if (getDataLayout()!=null) {
            getDataLayout().onMeta(this, meta);
        }        
        this.meta.putAll(meta);
    }

    
    @Override
    public Device[] getMonitors(){        
        if (monitors==null){
            return null;
        }
        ArrayList<Device> ret = new ArrayList<>();
        
        for (Device dev : monitors){
            if (dev instanceof Otf){
                if (dev.getComponents().length>0){
                    for (Device component: dev.getComponents()){
                        ret.add(component);
                    }
                    continue;
                }
            } 
            ret.add(dev);      
        }        
        return ret.toArray(new Device[0]);
    }    

    @Override
    public void setMonitors(Device[] monitors){
        this.monitors = monitors;
    }    

    @Override
    public Readable[] getDiags(){        
        return diags;
    }    

    @Override
    public void setDiags(Readable[] diags){
        this.diags = diags;
    }    
    
    @Override
    public void setSnaps(Readable[] snaps){
        this.snaps = snaps;
    }    
    
    @Override
    public Readable[] getSnaps(){
        return snaps;
    }        

    int settleTimeout = getScansSettleTimeout();

    @Override
    public int getSettleTimeout() {
        return settleTimeout;
    }

    @Override
    public void setSettleTimeout(int value) {
        settleTimeout = value;
    }

    void assertFieldsOk() {
        if ((writables == null) || (readables == null)) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < writables.length; i++) {
            if (isPositiveDirection(i) && (stepSize[i] < 0)) {
                //throw new IllegalArgumentException();
                stepSize[i] *= -1;

            } else if (isNegativeDirection(i) && (stepSize[i] > 0)) {
                //throw new IllegalArgumentException();
                stepSize[i] *= -1;
            }
        }
    }

    @Override
    public void setPlotTitle(String plotTitle) {
        this.plotTitle = plotTitle;
    }
    
    @Override
    public String getPlotTitle(){
        return plotTitle;
    }
    
    @Override
    public void setHidden(boolean value){
        hidden = value;
    }
    
    @Override
    public boolean isHidden(){
        return hidden;
    }

    @Override
    public void setSplitPasses(boolean value){
        splitPasses = value;
    }
    
    @Override
    public boolean getSplitPasses(){
        return splitPasses;
    }

    
    protected boolean isPositiveDirection(int writableIndex) {
        return (start[writableIndex] <= end[writableIndex]);
    }

    protected boolean isNegativeDirection(int writableIndex) {
        return (start[writableIndex] > end[writableIndex]);
    }

    protected void moveToStart() throws IOException, InterruptedException {
        setPosition(getStart());
    }

    protected void moveToEnd() throws IOException, InterruptedException {
        setPosition(getEnd());
    }

    protected void setPosition(double[] position) throws IOException, InterruptedException {
        if (getParallelPositioning()){
            for (int i = 0; i < getWritables().length; i++) {
                getWritables()[i].write(position[i]);
            }
            waitSettled(position);
        } else {
             for (int i = 0; i < getWritables().length; i++) {
                getWritables()[i].write(position[i]);
                waitSettled(getWritables()[i], position[i], stepSize[i]);
            }     
        }
    }

    protected void waitSettled(double[] position) throws IOException, InterruptedException {
        if (position.length < getWritables().length) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < getWritables().length; i++) {
            waitSettled(getWritables()[i], position[i], stepSize[i]);
        }
    }

    protected ReadonlyRegister getReadback(Writable writable) {
        if (writable instanceof ReadbackDevice) {
            return ((ReadbackDevice) writable).getReadback();
        } else if (writable instanceof ReadonlyRegister) {
            return (ReadonlyRegister) writable;
        } else if (writable instanceof Readable) {
            return new ReadableRegister((Readable) writable);
        }
        return null;
    }

    protected void waitSettled(Writable writable, double pos, double stepSize) throws IOException, InterruptedException {
        if (writable instanceof Movable) {
            Movable m = (Movable) writable;
            m.waitReady(settleTimeout);
            if (writable instanceof Motor) {
                if (getCheckPositions()) {
                    m.assertInPosition(pos);
                }
            } else {
                //Filtering some possible overshooting (Motors manage internally)
                m.waitInPosition(pos, 10000);
            }
        } else {
            ReadonlyRegister readback = getReadback(writable);
            if ((readback != null) && (writable instanceof Resolved)) {
                double resolution = ((Resolved) writable).getResolution();
                readback.waitValueInRange(pos, resolution, settleTimeout);
            } else if (writable instanceof Device) {
                ((Device) writable).waitStateNot(State.Busy, -1);
            }
        }
    }

    volatile Thread executionThread;
    double[] initialPosition;

    protected void readInitialPosition() throws IOException, InterruptedException {
        initialPosition = new double[writables.length];
        for (int i = 0; i < initialPosition.length; i++) {
            try {
                initialPosition[i] = (Double) ((ch.psi.pshell.device.Readable) writables[i]).read();
            } catch (IOException | InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException("Positioner type must be <Double> for relative scans: " + writables[i].getAlias());
            }
        }
    }

    /**
     * The position of the Writables in the beginning of a relative scan
     */
    public double[] getInitialPosition() {
        return initialPosition;
    }

    protected double getWritablePosition(int step, int writableIndex) {
        double ret = getStart()[writableIndex] + (getStepSize()[writableIndex] * step);
        if (isPositiveDirection(writableIndex)) {
            ret = Math.min(ret, getEnd()[writableIndex]);
        } else {
            ret = Math.max(ret, getEnd()[writableIndex]);
        }
        return ret;
    }

    protected double[] getWritablesPositions(int step) {
        double[] pos = new double[writables.length];
        for (int i = 0; i < getWritables().length; i++) {
            pos[i] = getWritablePosition(step, i);
        }
        return pos;
    }
    
    
    DeviceListener monitorListener = new DeviceAdapter() {
        @Override
        public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
            triggerMonitor(device, value, timestamp);
        }
    };
    
    
    protected void startMonitor(Device dev){
        dev.addListener(monitorListener);
    }
    
    protected void stopMonitor(Device dev){
        dev.removeListener(monitorListener);
    }
    
    protected void startMonitors(){
        if (monitors != null){
            for (Device dev: monitors){
                try {
                    if (dev instanceof Otf){
                        logger.info("Starting OTF: " + dev.getAlias());
                        ((Otf) dev).start();
                        if (dev.getComponents().length>0){
                            for (Device component: dev.getComponents()){
                                startMonitor(component);
                            }
                            continue;
                        }
                    }                    
                    startMonitor(dev);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }        
    }
  

    protected void stopMonitors(){
        if (monitors != null){
            for (Device dev: monitors){
                try {
                    if (dev instanceof Otf){
                        if (!isAborted()){
                            logger.info("Waiting OTF to complete: " + dev.getAlias());
                            dev.waitReady(-1);
                            logger.info("OTF completed: " + dev.getAlias());
                        }
                    }
                    stopMonitor(dev);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }        
    }

    @Override
    public void start() throws IOException, InterruptedException {
        Boolean fail = false;
        initialPosition = null;
        try {
            assertFieldsOk();
            recordIndex = 0;
            result = newResult();
            try{
                openDevices();
                if (relative) {
                    readInitialPosition();
                }            
                triggerStarted();
                if(getInitialMove()){
                    moveToStart();
                }
                try {
                    onBeforeScan();
                    startMonitors();
                    try {
                        for (pass = 1; pass <= getNumberOfPasses(); pass++) {
                            passOffset = recordIndex;
                            onBeforePass(pass);
                            doScan();
                            onAfterPass(pass);
                            if (getSplitPasses()){
                                if(pass < getNumberOfPasses()){
                                    Context.getInstance().getDataManager().splitScanData(this);
                                    setRecordIndexOffset(getRecordIndex());
                                }
                            }
                        }
                    } finally {
                        stopMonitors();
                        onAfterScan();
                    }
                    triggerEnded(null);
                } catch (Exception ex) {
                    fail = true;
                    try {
                        assertNotAborted();
                    } catch (Exception e) {
                        triggerEnded(e);
                        throw e;
                    }
                    triggerEnded(ex);
                    throw ex;
                } finally {
                    if (relative && restorePosition && (initialPosition!=null)) {
                        try {
                            setPosition(initialPosition);
                        } catch (InterruptedException ex) {
                            if (!fail) {
                                throw ex;
                            }
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                    }                
                }
            } finally{
                closeDevices();
            }
        } finally {
            endTimestamp = System.currentTimeMillis();
        }
    }

    protected ScanResult newResult() {
        return new ScanResult(this);
    }

    boolean aborted;

    public boolean isAborted() {
        return aborted;
    }

    @Override
    final public void abort() throws InterruptedException {
        logger.info("Aborting: " + toString());
        aborted = true;
        doAbort();
    }

    protected void doAbort() throws InterruptedException {
        //Not stopping relative moves because it affects recovering position
        if (!relative || !restorePosition) {
            stopAll();
        }
    }

    protected void assertNotAborted() throws ScanAbortedException {
        if (isAborted()) {
            throw new ScanAbortedException();
        }
    }

    public void stopAll() {
        for (Writable w : getWritables()) {
            try {
                if (w instanceof Stoppable) {
                    ((Stoppable) w).stopAsync();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        
        if (monitors != null){
            for (Device dev: monitors){
                try {
                    if (dev instanceof Otf){
                        logger.info("Aborting OTF: " + dev.getAlias());
                        ((Otf) dev).abort();
                    }                    
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }        
        
    }

    /**
     * Called to move to the given position during the scan
     */
    protected void applyPosition(double[] position) throws IOException, InterruptedException {
        setPosition(position);
    }

    protected ScanRecord processPosition(double[] pos) throws IOException, InterruptedException {
        return processPosition(pos, null);
    }
    
    protected ScanRecord processPosition(double[] pos, Long timestamp) throws IOException, InterruptedException {
        return processPosition(pos, timestamp, 0);
    }

    protected ScanRecord processPosition(double[] pos, Long timestamp, long id) throws IOException, InterruptedException {
        checkInterrupted();
        assertNotAborted();

        ScanRecord record = newRecord();
        record.id = id;
        record.setpoints = new Number[getWritables().length];
        record.positions = new Number[getWritables().length];
        for (int i = 0; i < getWritables().length; i++) {
            record.setpoints[i] = pos[i];
        }

        applyPosition(pos);

        if (getLatency() > 0) {
            Thread.sleep(getLatency());
        }
        do {
            assertNotAborted();
            onBeforeReadout(pos);

            record.invalidated = false;
            record.values = new Object[getReadables().length];
            record.deviceTimestamps = new Long[getReadables().length];
            for (int j = 0; j < getWritables().length; j++) {
                ReadonlyRegister readback = getReadback(getWritables()[j]);
                if ((readback != null) && useWritableReadback) {
                    record.positions[j] = (Number) readback.getValue(); //For sure have been updated in waitSettled
                } else {
                    record.positions[j] = record.setpoints[j];
                }
            }

            for (int j = 0; j < getReadables().length; j++) {
                try {
                    Object val = getReadables()[j].read();
                    if (val instanceof TimestampedValue){
                        record.deviceTimestamps[j] = ((TimestampedValue)val).getTimestamp();
                        val =  ((TimestampedValue)val).getValue();
                    }  else if (getReadables()[j] instanceof Timestamped){
                        //TODO: Not atomic, should use takeTimestasmped, but has performance impact and assumes that read() is behaving well (setting cache)
                        record.deviceTimestamps[j] = ((Timestamped)getReadables()[j]).getTimestamp();
                    }
                    
                    if (val instanceof List) { //Jython Lists
                        val = Convert.toArray((List) val);
                        val = Convert.toDouble(val); //TODO: let Data manager make this transformation based on getPreserveTypes()
                    } else if (val instanceof Map) { //JS Lists
                        val = Convert.toArray((Map) val);
                        val = Convert.toDouble(val); //TODO: let Data manager make this transformation based on getPreserveTypes()
                    }
                    record.values[j] = val;
                } catch (InterruptedException ex) {
                    throw ex;
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                    if (abortOnReadableError) {
                        throw (ex instanceof IOException) ? (IOException) ex : new IOException(ex);
                    }
                    record.values[j] = null;
                }
            }
            record.localTimestamp = System.currentTimeMillis();
            record.timestamp = timestamp;
            onAfterReadout(record);
            if (record.invalidated) {
                if (!resampleOnInvalidate){
                    logger.finer("Record invalidated:  " + record.index);
                    record.index=-1;
                    recordIndex--;
                    return record;
                }
                logger.warning("Resampling record: " + record.index);
            }
        } while (record.invalidated);
        if (!record.canceled) {
            triggerNewRecord(record);
        }
        return record;
    }

    protected ScanRecord newRecord() {
        ScanRecord record = new ScanRecord();
        record.scan = this;
        record.index = recordIndex;
        record.pass = getCurrentPass();
        record.indexInPass = getRecordIndexInPass();
        record.localTimestamp = System.currentTimeMillis();
        record.timestamp =  record.localTimestamp;
        recordIndex++;
        return record;
    }
    
    protected ScanRecord newRecord(Number[] setpoints, Number[] positions, Object[] values, long id, Long timestamp, Long[] deviceTimestamps) {
        ScanRecord record = newRecord();
        record.setpoints = setpoints;
        record.positions = positions;
        record.values = values;
        record.id = id;
        record.localTimestamp = System.currentTimeMillis();
        record.timestamp = timestamp;
        record.deviceTimestamps = deviceTimestamps;
        return record;
    }
    

    protected void onBeforeReadout(double[] pos) {
    }

    protected void onAfterReadout(ScanRecord record) {
    }
    
    protected void onBeforePass(int pass) {
    }

    protected void onAfterPass(int pass) {
    }    

    abstract protected void doScan() throws IOException, InterruptedException;
    
    void updateTag(){
        String val = Context.getInstance().getExecutionPars().getTag();
        val = Context.getInstance().getSetup().expandPath((val == null) ? Context.getInstance().getScanTag() : val);
        tag = val;
    }

    protected void triggerStarted() {
        startTimestamp = System.currentTimeMillis();
        Context context = Context.getInstance();
        if (context != null) {
            removeAllListeners();
            if (isHidden()) {
                addListener(context.getDataManager().getScanListener());
            } else{ 
                for (ScanListener listener : context.getScanListeners()) {
                    addListener(listener);
                }
            }
            final ExecutionParameters execPars = context.getExecutionPars();
            synchronized (execPars) {
                execPars.addScan(this);
                updateTag();
                name = execPars.toString();
                scanIndex = execPars.getIndex(this);
                keep = execPars.getKeep();
            }
        }

        executionThread = Thread.currentThread();
        aborted = false;
        for (ScanListener listener : getListeners()) {
            try{
                listener.onScanStarted(ScanBase.this, plotTitle);
            } catch (Exception ex){
                logger.log(Level.WARNING, null, ex);
            }
        }
        //Called later to let DataManager initialize storage before reading the scan path
        if (context != null) {
            final ExecutionParameters execPars = context.getExecutionPars();
            synchronized (execPars) {
                String scanRoot = execPars.getPath();
                if (IO.isSubPath(scanRoot, context.getSetup().getDataPath())) {
                    scanRoot = IO.getRelativePath(scanRoot, context.getSetup().getDataPath());
                }
                scanPath = scanRoot + " | " + context.getDataManager().getScanPath(this);
                if (execPars.isScanPersisted(this)) {
                    dataLayout = (execPars.getDataLayout() != null) ? execPars.getDataLayout() : context.getDataManager().getLayout();
                    dataProvider = (execPars.getDataProvider() != null) ? execPars.getDataProvider() : context.getDataManager().getProvider();
                }
            }
        }
    }

    ScanRecord currentRecord;
    
    @Override
    public ScanRecord getCurrentRecord(){
        return currentRecord;
    }
        
    protected void triggerNewRecord(ScanRecord record) {
        if (keep) {
            result.records.add(record);
        }
        currentRecord = record;
        for (ScanListener listener : getListeners()) {
            try {
                listener.onNewRecord(ScanBase.this, record);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        currentRecord.sent = true;
    }
    
    protected void triggerMonitor(Device dev, Object value, long timestamp) {
        for (ScanListener listener : getListeners()) {
            try {
                listener.onMonitor(ScanBase.this, dev, value, timestamp);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }    

    protected void triggerEnded(Exception ex) {
        result.completed = true;
        for (ScanListener listener : getListeners()) {
            try{
                listener.onScanEnded(ScanBase.this, ex);
            } catch (Exception e){
                logger.log(Level.WARNING, null, e);
            }
        }
    }

    @Override
    public int getIndex() {
        return scanIndex;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public String getPath() {
        return scanPath;
    }

    @Override
    public Object readData(String device){
        if (getDataLayout()!=null) {
            return getDataLayout().getData(this, device, Context.getInstance().getDataManager());
        }
        return null;
    }
    
    
    @Override
    public Object readMonitor(String device){
        if (getMonitors()!=null){
            if (getDataLayout()!=null) {
                return getDataLayout().getMonitor(this, device, Context.getInstance().getDataManager());
            }
        }
        return null;
    }    
    
    @Override
    public Object readDiag(String device){
        if (getDiags()!=null){
            if (getDataLayout()!=null) {
                return getDataLayout().getDiag(this, device, Context.getInstance().getDataManager());
            }
        }
        return null;
    }       
    
    @Override
    public Map<Readable, Object> readSnaps(){
        Map<Readable, Object> ret = new LinkedHashMap<>();
        if (getSnaps()!=null){
            for (Readable snap: getSnaps()){
                try{
                    ret.put(snap, readSnap(getReadableName(snap)));
                } catch(Exception ex){
                    ret.put(snap, null);
                }
            }
        }
        return ret;
    }
    
    @Override
    public Object readSnap(String device){
        if (getDataLayout()!=null) {
            return getDataLayout().getSnap(this, device, Context.getInstance().getDataManager());
        }
        return null;        
    }              
    
    @Override
    public long[] readTimestamps(){
        if (getDataLayout()!=null) {
            return getDataLayout().getTimestamps(this,Context.getInstance().getDataManager());
        }
        return null;    
    }

    @Override
    public boolean getKeep() {
        return keep;
    }

    @Override
    public ScanResult getResult() {
        return result;
    }

    @Override
    public Writable[] getWritables() {
        return writables;
    }

    @Override
    public Readable[] getReadables() {
        return readables;
    }

    @Override
    public Nameable[] getDevices(){
        Nameable[] devices = new Nameable[readables.length];
        System.arraycopy(readables, 0, devices, 0, readables.length);
        if (writables!=null){
            devices = Arr.append(devices, writables);
        }
        if (monitors!=null){
            devices = Arr.append(devices, getMonitors());
        }
        if (diags!=null){
            devices = Arr.append(devices, getDiags());
        }        
        if (snaps!=null){
            devices = Arr.append(devices, getSnaps());
        }        
        return devices;
    }

    int getDeviceIndex(Object arr, Object obj) {
        if (arr!=null){
            if (obj instanceof Number){
                return ((Number) obj).intValue();
            }
            for (int i=0;  i< Array.getLength(arr); i++) {
                Nameable element = (Nameable) Array.get(arr,i);
                if (obj== element){
                    return i;
                }
                if (obj instanceof String){
                    if ((((String)obj).equals(element.getAlias())) || ((((String)obj).equals(element.getName())))){
                        return i;
                    }
                }
                if (element instanceof Cacheable.CacheReadable) {
                    Cacheable parent = ((Cacheable.CacheReadable) element).getParent();
                    if (obj== parent){
                        return i;
                    }

                }
            }
        }
        throw new RuntimeException("Invalid device: " + obj);
    }

    @Override
    public int getReadableIndex(Object obj) {
        return getDeviceIndex(getReadables(), obj);
    }

    @Override
    public int getWritableIndex(Object obj) {
        return getDeviceIndex(getWritables(), obj);
    }

    @Override
    public int getMonitorIndex(Object obj) {
        return getDeviceIndex(getMonitors(), obj);
    }
    
    @Override
    public int getSnapIndex(Object obj) {
        return getDeviceIndex(getSnaps(), obj);
    }    
    
    @Override
    public int getDiagIndex(Object obj) {
        return getDeviceIndex(getDiags(), obj);
    }    
    
    @Override
    public int getDeviceIndex(Object obj){
        return getDeviceIndex(getDevices(), obj);
    }

    @Override
    public String[] getWritableNames() {
        ArrayList<String> names = new ArrayList();
        for (Writable writable : getWritables()) {
            names.add(writable.getAlias());
        }
        return names.toArray(new String[0]);
    }

    @Override
    public String[] getReadableNames() {
        ArrayList<String> names = new ArrayList();
        for (Readable readable : getReadables()) {
            names.add(readable.getAlias());
        }
        return names.toArray(new String[0]);
    }
    
    @Override
    public String[] getMonitorNames() {
        ArrayList<String> names = new ArrayList();
        if (getMonitors()!=null){
            for (Device monitor : getMonitors()) {
                names.add(monitor.getAlias());
            }
        }
        return names.toArray(new String[0]);
    }
    
    @Override
    public String[] getDiagNames() {
        ArrayList<String> names = new ArrayList();
        if (getDiags()!=null){
            for (Readable diag : getDiags()) {
                names.add(getReadableName(diag));
            }
        }
        return names.toArray(new String[0]);
    }    
    
    @Override
    public String getReadableName(Readable snap){
        return (snap instanceof Device) ? ((Device)snap).getAlias() : snap.getName();
    }
    
    @Override
    public String[] getSnapsNames() {
        ArrayList<String> names = new ArrayList();
        if (getSnaps()!=null){
            for (Readable snap : getSnaps()) {
                names.add(getReadableName(snap));
            }
        }
        return names.toArray(new String[0]);
    }    

    @Override
    public String[] getDeviceNames(){
        ArrayList<String> names = new ArrayList();
        for (Nameable device : getDevices()) {
            names.add(device.getAlias());
        }
        return names.toArray(new String[0]);
    }

    @Override
    public double[] getStart() {
        double[] ret = Arrays.copyOf(start, start.length);
        if (relative) {
            if (initialPosition != null) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] += initialPosition[i];
                }
            }
        }
        return ret;
    }

    @Override
    public double[] getEnd() {
        double[] ret = Arrays.copyOf(end, start.length);
        if (relative) {
            if (initialPosition != null) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] += initialPosition[i];
                }
            }
        }
        return ret;
    }

    @Override
    public double[] getStepSize() {
        return Arrays.copyOf(stepSize, start.length);
    }

    @Override
    public int getLatency() {
        return latency;
    }

    @Override
    public int getNumberOfPasses() {
        return Math.max(passes, 1);
    }

    @Override
    public int getCurrentPass() {
        return pass;
    }

    @Override
    public boolean isCurrentPassBackwards() {
        return (isZigzag() && ((pass % 2) == 0));
    }

    protected void setCurrentPass(int pass) {
        this.pass = pass;
    }

    @Override
    public boolean isZigzag() {
        return zigzag;
    }

    @Override
    public int[] getNumberOfSteps() {
        return Arrays.copyOf(numberOfSteps, start.length);
    }

    @Override
    public int getNumberOfRecords() {
        int dims = getDimensions();
        if (dims == 0) {
            return 0;
        }
        int[] steps = getNumberOfSteps();
        int records = 1;
        try {
            for (int i = 0; i < dims; i++) {
                records *= steps[i] + 1;
            }
        } catch (Exception ex) {
            return 0;
        }
        return records * getNumberOfPasses();
    }

    @Override
    public boolean isArray(int readableIndex) {
        if ((readableIndex < 0) || (readableIndex > readables.length)) {
            return false;
        }
        return readables[readableIndex] instanceof Readable.ReadableArray;
    }

    @Override
    public int getDimensions() {
        return writables.length;
    }

    protected void onBeforeScan() throws IOException, InterruptedException {
    }

    protected void onAfterScan() throws IOException, InterruptedException {
    }

    ArrayList<Stream> startedStreams;
    ArrayList<Device> innerDevices;
    
    protected ArrayList<Stream> getStartedStreams(){
        return startedStreams;
    }
    protected ArrayList<Device> getInnerDevices(){
        return innerDevices;
    }   

    protected void openDevices() throws IOException, InterruptedException {
        startedStreams = new ArrayList<>();
        innerDevices = new ArrayList<>();
        for (int i = 0; i < writables.length; i++) {
            if (writables[i] instanceof InlineDevice) {
                ((InlineDevice) writables[i]).initialize();
                innerDevices.add((InlineDevice) writables[i]);
                writables[i] = (Writable) ((InlineDevice) writables[i]).getDevice();
            }
        }
        Stream innerStream = null;
        for (int i = 0; i < readables.length; i++) {
            if (readables[i] instanceof InlineDevice) {
                InlineDevice inlineDevice = (InlineDevice) readables[i]; 
                if (inlineDevice.getProtocol().equals("bs")) {
                    if (innerStream == null) {
                        innerStream = new Stream("Scan devices stream");
                        innerDevices.add(innerStream);
                        innerStream.initialize();
                    }
                    inlineDevice.setParent(innerStream);
                }
                inlineDevice.initialize();
                innerDevices.add((InlineDevice) readables[i]);
                readables[i] = (Readable) inlineDevice.getDevice();
            }
        }
        if (monitors!=null){
            for (int i = 0; i < monitors.length; i++) {
                if (monitors[i] instanceof InlineDevice) {
                    InlineDevice inlineDevice = (InlineDevice) monitors[i]; 
                    if (inlineDevice.getProtocol().equals("bs")) {
                        if (innerStream == null) {
                            innerStream = new Stream("Scan devices stream");
                            innerDevices.add(innerStream);
                            innerStream.initialize();
                        }
                        inlineDevice.setParent(innerStream);
                    }
                    inlineDevice.initialize();
                    innerDevices.add((InlineDevice) monitors[i]);
                    monitors[i] = (Device) inlineDevice.getDevice();
                }
            }
        }     
        
        if (innerStream != null) {
            innerStream.start(true);
            innerStream.waitCacheChange(10000);
        }
        for (Readable r : readables) {
            if (r instanceof Stream) {
                if (((Stream) r).getState().isReady()) {
                    ((Stream) r).start(true); //Start in asynchronous mode
                    startedStreams.add(((Stream) r));
                }
            } else if (r instanceof Waveform) {
                try {
                    ((Waveform) r).getSize();
                } catch (Exception ex) {
                    ((Waveform) r).update(); //Performs a first read to initialize array length    
                }
            }
        }
    }

    protected void closeDevices() {
        if (startedStreams != null) {
            for (Stream stream : startedStreams) {
                try {
                    stream.stop();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
        if (innerDevices != null) {
            for (Device dev : innerDevices) {
                try {
                    dev.close();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    @Override
    public long getStartTimestamp() {
        return startTimestamp;
    }

    @Override
    public long getEndTimestamp() {
        return endTimestamp;
    }

    @Override
    public int getRecordIndex() {
        return recordIndex;
    }

    @Override
    public int getRecordIndexOffset() {
        return recordIndexOffset;
    }
    
    @Override
    public int getRecordIndexInPass() {
        return recordIndex - passOffset;
    }    

    @Override
    public void setRecordIndexOffset(int value) {
        recordIndexOffset = Math.max(value, 0);
        updateTag(); //This is called to split the scan data, must update scan tag
    }

    @Override
    public boolean isStarted() {
        return (startTimestamp != 0);
    }
    
    @Override
    public boolean isRunning() {
        return isStarted() && !isCompleted();
    }

    @Override
    public boolean isCompleted() {
        return (getEndTimestamp() != 0)
                || ((result != null) && (result.completed))
                || ((executionThread != null) && (!executionThread.isAlive())); //For derived classes that re-implement start() 
    }

    @Override
    public String toString() {
        return Nameable.getShortClassName(getClass()) + " " + name + "-" + scanIndex;
    }

    public Thread getThread() {
        return executionThread;
    }

    @Override
    public void setUseWritableReadback(boolean value) {
        useWritableReadback = value;
    }

    @Override
    public boolean getUseWritableReadback() {
        return useWritableReadback;
    }

    @Override
    public boolean getRestorePosition() {
        return restorePosition;
    }

    @Override
    public void setRestorePosition(boolean value) {
        restorePosition = value;
    }

    @Override
    public boolean getAbortOnReadableError() {
        return abortOnReadableError;
    }

    @Override
    public void setAbortOnReadableError(boolean value) {
        abortOnReadableError = value;
    }

    @Override
    public boolean getCheckPositions() {
        return checkPositions;
    }

    @Override
    public void setCheckPositions(boolean value) {
        checkPositions = value;
    }
    
    @Override
    public boolean getInitialMove(){
        return initialMove;
    }

    @Override
    public void setInitialMove(boolean value){
        initialMove=value;
    }

    @Override
    public boolean getResampleOnInvalidate(){
        return resampleOnInvalidate;
    }

    @Override
    public void setResampleOnInvalidate(boolean value){
        resampleOnInvalidate=value;
    }

    @Override
    public boolean getParallelPositioning(){
        return parallelPositioning;
    }

    @Override
    public void setParallelPositioning(boolean value){
        parallelPositioning = value;
    }       

    static boolean restorePositionOnRelativeScans = true;

    public static boolean getRestorePositionOnRelativeScans() {
        return restorePositionOnRelativeScans;
    }

    public static void setRestorePositionOnRelativeScans(boolean value) {
        restorePositionOnRelativeScans = value;
    }

    static boolean abortScansOnReadableError = false;

    public static boolean getAbortScansOnReadableError() {
        return abortScansOnReadableError;
    }

    public static void setAbortScansOnReadableError(boolean value) {
        abortScansOnReadableError = value;
    }

    static boolean scansCheckPosition = true;

    public static boolean getScansCheckPositions() {
        return scansCheckPosition;
    }

    public static void getScansCheckPositions(boolean value) {
        scansCheckPosition = value;
    }
    
    static boolean scansTriggerInitialMove = true;

    public static boolean getScansTriggerInitialMove() {
        return scansTriggerInitialMove;
    }

    public static void getScansTriggerInitialMove(boolean value) {
        scansTriggerInitialMove = value;
    }   
    
    static boolean scansParallelPositioning = true;

    public static boolean getScansParallelPositioning() {
        return scansParallelPositioning;
    }

    public static void getScansParallelPositioning(boolean value) {
        scansParallelPositioning = value;
    }     
    
    static int scansSettleTimeout = -1;

    public static int getScansSettleTimeout() {
        return scansSettleTimeout;
    }

    public static void setScansSettleTimeout(int value) {
        scansSettleTimeout = value;
    }    
    
    static boolean scansUseWritableReadback = true;

    public static boolean getScansUseWritableReadback() {
        return scansUseWritableReadback;
    }

    public static void setScansUseWritableReadback(boolean value) {
        scansUseWritableReadback = value;
    }       
}
