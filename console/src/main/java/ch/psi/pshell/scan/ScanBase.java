package ch.psi.pshell.scan;

import ch.psi.pshell.bs.Matrix;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.Waveform;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.device.*;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.devices.InlineDevice;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.scripting.JepUtils;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.utils.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jep.NDArray;
import org.apache.commons.lang3.tuple.ImmutablePair;
import ch.psi.pshell.data.Format;

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

    ScanCallbacks callbacks;
    ScanResult result;
    int recordIndex = 0;
    int recordIndexOffset = 0;
    int passOffset = 0;
    long startTimestamp;
    long endTimestamp;
    boolean canPause=true;
    
    String plotTitle;
    boolean plottingActive;
    boolean hidden;
    boolean splitPasses;
    
    int scanIndex = -1;
    String scanPath;
    String tag;
    int pass = 1;
    boolean keep;
    boolean lazy;
    String name = "Scan";
    Layout dataLayout;
    Format dataFormat;
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
    
    public void setCallbacks(ScanCallbacks callbacks){
        this.callbacks=callbacks;
    }

    public ScanCallbacks getCallbacks(){
        return callbacks;
    }
    
    @Override
    public Layout getDataLayout(){
        return dataLayout;
    }
    
    @Override
    public Format getDataFormat(){
        return dataFormat;
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
    public boolean isPlottingActive(){
        return plottingActive;
    }
   
    @Override
    public  void setPlottingActive(boolean value){
        plottingActive=value;
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
        if (writable instanceof ReadbackDevice reg) {
            return reg.getReadback();
        } else if (writable instanceof ReadonlyRegister reg) {
            return reg;
        } else if (writable instanceof Readable reg) {
            return new ReadableRegister(reg);
        }
        return null;
    }

    protected void waitSettled(Writable writable, double pos, double stepSize) throws IOException, InterruptedException {
        if (writable instanceof Movable m) {
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
            if ((readback != null) && (writable instanceof Resolved resolved)) {
                double resolution = resolved.getResolution();
                readback.waitValueInRange(pos, resolution, settleTimeout);
            } else if (writable instanceof Device device) {
                device.waitStateNot(State.Busy, -1);
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
    
    
    final DeviceListener monitorListener = new DeviceListener() {
        @Override
        public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
            if (isLazy() && !triggeredStart){
                synchronized(monitorListener){
                    if (monitorBuffer!=null){
                        monitorBuffer.get(device).add(new ImmutablePair(value, timestamp));                        ;
                    } 
                    return;
                }
            }
            triggerMonitor(device, value, timestamp);
        }
    };
    
    
    Map<Device,List<ImmutablePair<Object, Long>>> monitorBuffer;
    protected void startMonitor(Device dev){
        dev.addListener(monitorListener);
        if (isLazy()){
            synchronized(monitorListener){
                if (monitorBuffer==null){
                    monitorBuffer = new HashMap<>();
                }
                monitorBuffer.put(dev, new ArrayList<>());
            }
        }
    }
    
    protected void stopMonitor(Device dev){
        dev.removeListener(monitorListener);
    }
    
    protected void startMonitors(){
        if (monitors != null){
            for (Device dev: monitors){
                try {
                    if (dev instanceof Otf otf){
                        logger.log(Level.INFO, "Starting OTF: {0}", dev.getAlias());
                        otf.start();
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
                            logger.log(Level.INFO, "Waiting OTF to complete: {0}", dev.getAlias());
                            dev.waitReady(-1);
                            logger.log(Level.INFO, "OTF completed: {0}", dev.getAlias());
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
                initialize();
                if(getInitialMove()){
                    moveToStart();
                }
                try {
                    onBeforeScan();
                    if (getCallbacks()!=null){
                        getCallbacks().onBeforeScan(this);
                    }
                    startMonitors();
                    try {
                        for (pass = 1; pass <= getNumberOfPasses(); pass++) {
                            passOffset = recordIndex;
                            onBeforePass(pass);
                            if (getCallbacks()!=null){
                                getCallbacks().onBeforePass(this, pass);
                            }
                            doScan();
                            onAfterPass(pass);
                            if (getCallbacks()!=null){
                                getCallbacks().onAfterPass(this, pass);
                            }
                            if (getSplitPasses()){
                                if(pass < getNumberOfPasses()){
                                    Context.getDataManager().splitScanData(this);
                                    setRecordIndexOffset(getRecordIndex());
                                }
                            }
                        }
                    } finally {
                        stopMonitors();
                        onAfterScan();
                        if (getCallbacks()!=null){
                            getCallbacks().onAfterScan(this);
                        }  
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
                    if (relative && getRestorePosition() && (initialPosition!=null)) {
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
        logger.log(Level.INFO, "Aborting: {0}", toString());
        aborted = true;
        doAbort();
    }
     
    protected void doAbort() throws InterruptedException {
        //Not stopping relative moves because it affects recovering position
        if (!relative || !getRestorePosition()) {
            stopAll();
        }
    }

    protected void assertNotAborted() throws ScanAbortedException {
        if (isAborted()) {
            throw new ScanAbortedException();
        }
    }

    boolean paused;

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    final public void pause() throws InterruptedException {
        if (!isPaused() && !isAborted()){
            logger.log(Level.INFO, "Pausing: {0}", toString());
            paused = true;
            doPause();
        }
    }  
    
    @Override
    final public void resume() throws InterruptedException {
        if (isPaused() && !isAborted()){
            logger.log(Level.INFO, "Resuming: {0}", toString());
            paused = false;
            doResume();
        }
    }      
    
    protected void doPause() throws InterruptedException {
    }       
    
    protected void doResume() throws InterruptedException {
    }    
    
    @Override
    public boolean canPause(){
        return canPause;
    }
    
    @Override
    public void setCanPause(boolean value){
        canPause = value;
    }
    
    protected void waitPauseDone() throws ScanAbortedException, InterruptedException{
         while (isPaused() && canPause()){
             assertNotAborted();
             Thread.sleep(10);
         }
    }        
    
    public void stopAll() {
        for (Writable w : getWritables()) {
            try {
                if (w instanceof Stoppable stoppable) {
                    stoppable.stopAsync();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        
        if (monitors != null){
            for (Device dev: monitors){
                try {
                    if (dev instanceof Otf otf){
                        logger.log(Level.INFO, "Aborting OTF: {0}", dev.getAlias());
                        otf.abort();
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
        waitPauseDone();

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
            if (getCallbacks()!=null){
                getCallbacks().onBeforeReadout(this, pos);
            }

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
                    if (val instanceof TimestampedValue timestampedValue){
                        record.deviceTimestamps[j] = timestampedValue.getTimestamp();
                        val =  timestampedValue.getValue();
                    }  else if (getReadables()[j] instanceof Timestamped timestamped){
                        //TODO: Not atomic, should use takeTimestasmped, but has performance impact and assumes that read() is behaving well (setting cache)
                        record.deviceTimestamps[j] = timestamped.getTimestamp();
                    }
                    
                    if (val instanceof List list) { //Jython Lists
                        val = Convert.toArray(list);
                        val = Convert.toDouble(val); //TODO: let Data manager make this transformation based on getPreserveTypes()
                    } else if (val instanceof Map map) { //JS Lists
                        val = Convert.toArray(map);
                        val = Convert.toDouble(val); //TODO: let Data manager make this transformation based on getPreserveTypes()
                    } else if (val instanceof jep.NDArray na){ //Numpy array
                        val = JepUtils.toJavaArray(na);
                    }                    
                    record.values[j] = val;
                } catch (InterruptedException ex) {
                    throw ex;
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                    if (abortOnReadableError) {
                        throw (ex instanceof IOException ioe) ? ioe : new IOException(ex);
                    }
                    record.values[j] = null;
                }
            }
            record.localTimestamp = System.currentTimeMillis();
            record.timestamp = timestamp;
            onAfterReadout(record);
            if (getCallbacks()!=null){
                getCallbacks().onAfterReadout(this, record);
            }
            if (record.invalidated) {
                if (!resampleOnInvalidate){
                    logger.log(Level.FINER, "Record invalidated:  {0}", record.index);
                    record.index=-1;
                    recordIndex--;
                    return record;
                }
                logger.log(Level.WARNING, "Resampling record: {0}", record.index);
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
        String val = Context.getExecutionPars().getTag();
        val = Setup.expandPath((val == null) ? Context.getScanTag() : val);
        tag = val;
    }
    
    protected void initialize() {
        startTimestamp = System.currentTimeMillis();
        removeAllListeners();
        if (isHidden()) {
            addListener(Context.getDataManager().getScanListener());
        } else{ 
            if (Context.hasInterpreter()){
                for (ScanListener listener : Context.getInterpreter().getScanListeners()) {
                    addListener(listener);
                }
            }
        }
        final ExecutionParameters execPars = Context.getExecutionPars();
        synchronized (execPars) {
            execPars.addScan(this);
            updateTag();
            name = execPars.toString();
            scanIndex = execPars.getIndex(this);
            keep = execPars.getKeep();
            lazy = execPars.getLazy();
        }

        executionThread = Thread.currentThread();
        aborted = false;
        if (!isLazy()){
            triggerStarted();
        }
    }

    boolean triggeredStart;
    protected void triggerStarted() {        
        try{
            for (ScanListener listener : getListeners()) {
                try{
                    listener.onScanStarted(ScanBase.this, plotTitle);
                } catch (Exception ex){
                    logger.log(Level.WARNING, null, ex);
                }
            }

            //Called later to let DataManager initialize storage before reading the scan path
            final ExecutionParameters execPars = Context.getExecutionPars();
            synchronized (execPars) {
                String scanRoot = execPars.getPath();
                if (IO.isSubPath(scanRoot, Setup.getDataPath())) {
                    scanRoot = IO.getRelativePath(scanRoot, Setup.getDataPath());
                }
                scanPath = scanRoot + " | " + Context.getDataManager().getScanPath(this);
                if (execPars.isScanPersisted(this)) {
                    dataLayout = (execPars.getDataLayout() != null) ? execPars.getDataLayout() : Context.getLayout();
                    dataFormat = (execPars.getDataFormat() != null) ? execPars.getDataFormat() : Context.getFormat();
                }
            }
        } finally {
            triggeredStart = true;
        }
    }

    ScanRecord currentRecord;
    
    @Override
    public ScanRecord getCurrentRecord(){
        return currentRecord;
    }
        
    protected void triggerNewRecord(ScanRecord record) {
        if (record.values!=null){
            for (int i=0; i<record.values.length; i++){
                if (record.values[i] instanceof NDArray na){
                    record.values[i] = JepUtils.toJavaArray(na);
                }
            } 
        }
        
        if (isLazy()){
            if (triggeredStart==false){
                triggerStarted();
                synchronized(monitorListener){
                    if (monitorBuffer!=null){
                        for (Device dev : monitorBuffer.keySet()){
                            for (ImmutablePair<Object, Long> pair : monitorBuffer.get(dev)){
                                triggerMonitor(dev, pair.left, pair.right);                                
                            }
                        }
                    }                
                    monitorBuffer = null;
                }
            }
        }        
        
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
        if (triggeredStart){
            for (ScanListener listener : getListeners()) {
                try {
                    listener.onMonitor(ScanBase.this, dev, value, timestamp);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }    

    protected void triggerEnded(Exception ex) {
        result.completed = true;
        if (triggeredStart){
            for (ScanListener listener : getListeners()) {
                try{
                    listener.onScanEnded(ScanBase.this, ex);
                } catch (Exception e){
                    logger.log(Level.WARNING, null, e);
                }
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
            return getDataLayout().getData(this, device, null);
        }
        return null;
    }
    
    
    @Override
    public Object readMonitor(String device){
        if (getMonitors()!=null){
            if (getDataLayout()!=null) {
                return getDataLayout().getMonitor(this, device, null);
            }
        }
        return null;
    }    
    
    @Override
    public Object readDiag(String device){
        if (getDiags()!=null){
            if (getDataLayout()!=null) {
                return getDataLayout().getDiag(this, device, null);
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
                    ret.put(snap, readSnap(getDeviceName(snap)));
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
            return getDataLayout().getSnap(this, device, null);
        }
        return null;        
    }              
    
    @Override
    public long[] readTimestamps(){
        if (getDataLayout()!=null) {
            return getDataLayout().getTimestamps(this, null);
        }
        return null;    
    }
    
    @Override
    public boolean isLazy() {
        return lazy;
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
            if (obj instanceof Number number){
                return number.intValue();
            }
            for (int i=0;  i< Array.getLength(arr); i++) {
                Nameable element = (Nameable) Array.get(arr,i);
                if (obj== element){
                    return i;
                }
                if (obj instanceof String str){
                    if (str.equals(element.getAlias()) || str.equals(element.getName())){
                        return i;
                    }
                }
                if (element instanceof Cacheable.CacheReadable cacheReadable) {
                    Cacheable parent = cacheReadable.getParent();
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
            try{
                names.add(getDeviceName(writable));
            } catch (Exception es) {
                names.add("?");
            }
        }
        return names.toArray(new String[0]);
    }

    @Override
    public String[] getReadableNames() {
        ArrayList<String> names = new ArrayList();
        for (Readable readable : getReadables()) {
            try{
                names.add(getDeviceName(readable));
            } catch (Exception es) {
                names.add("?");
            }   
        }
        return names.toArray(new String[0]);
    }
    
    @Override
    public String[] getMonitorNames() {
        ArrayList<String> names = new ArrayList();
        if (getMonitors()!=null){
            for (Device monitor : getMonitors()) {
                try{
                    names.add(getDeviceName(monitor));
                } catch (Exception es) {
                    names.add("?");
                }
            }
        }
        return names.toArray(new String[0]);
    }
    
    @Override
    public String[] getDiagNames() {
        ArrayList<String> names = new ArrayList();
        if (getDiags()!=null){
            for (Readable diag : getDiags()) {
                names.add(getDeviceName(diag));
            }                
        }
        return names.toArray(new String[0]);
    }    
    
    @Override
    public String getDeviceName(Nameable device){
        try{
            //return (device instanceof Device) ? ((Device)readable).getAlias() : device.getName();
            return device.getAlias();
        } catch (Exception es) {
            //TODO: Error in single-threaded interpreter when accessing device in different thread
            return ("?");
        }            
    }
    
    @Override
    public String[] getSnapsNames() {
        ArrayList<String> names = new ArrayList();
        if (getSnaps()!=null){
            for (Readable snap : getSnaps()) {
                names.add(getDeviceName(snap));
            }
        }
        return names.toArray(new String[0]);
    }    

    @Override
    public String[] getDeviceNames(){
        ArrayList<String> names = new ArrayList();
        for (Nameable device : getDevices()) {
            names.add(getDeviceName(device));
        }
        return names.toArray(new String[0]);
    }

    @Override
    public double[] getStart() {
        double[] ret = Arrays.copyOf(start, start.length);
        if (relative) {
            ret = relativeToAbsolute(ret);
        }
        return ret;
    }

    @Override
    public double[] getEnd() {
        double[] ret = Arrays.copyOf(end, start.length);
        if (relative) {
            ret = relativeToAbsolute(ret);
        }
        return ret;
    }
    
    public double[] relativeToAbsolute(double[] pos){
        double[] ret = Arrays.copyOf(pos, pos.length);
        if (initialPosition != null) {
            for (int j = 0; j < pos.length; j++) {
                ret[j] += initialPosition[j];
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
            if (writables[i] instanceof InlineDevice inlineDevice) {
                inlineDevice.initialize();
                innerDevices.add(inlineDevice);
                writables[i] = (Writable) inlineDevice.getDevice();
            }
        }
        Stream innerStream = null;
        for (int i = 0; i < readables.length; i++) {
            if (readables[i] instanceof InlineDevice inlineDevice) {
                if (inlineDevice.getProtocol().equals("bs")) {
                    if (innerStream == null) {
                        innerStream = new Stream("Scan devices stream");
                        innerDevices.add(innerStream);                        
                    }
                    inlineDevice.setParent(innerStream);
                }
                inlineDevice.initialize();
                innerDevices.add(inlineDevice);
                readables[i] = (Readable) inlineDevice.getDevice();
            }
        }
        if (monitors!=null){
            for (int i = 0; i < monitors.length; i++) {
                if (monitors[i] instanceof InlineDevice inlineDevice) {
                    if (inlineDevice.getProtocol().equals("bs")) {
                        if (innerStream == null) {
                            innerStream = new Stream("Scan devices stream");
                            innerDevices.add(innerStream);
                        }
                        inlineDevice.setParent(innerStream);
                    }
                    inlineDevice.initialize();
                    innerDevices.add(inlineDevice);
                    monitors[i] = (Device) inlineDevice.getDevice();
                }
            }
        }     
        
        if (innerStream != null) {
            innerStream.initialize();
            innerStream.start(true);
            innerStream.waitCacheChange(10000);
        }
        for (Readable r : readables) {
            if (r instanceof Stream stream) {
                if (stream.getState().isReady()) {
                    stream.start(true); //Start in asynchronous mode
                    startedStreams.add(stream);
                }
            } else if (r instanceof Waveform waveform) {
                try {
                    waveform.getSize();
                } catch (Exception ex) {
                    waveform.update(); //Performs a first read to initialize array length    
                }
            } else if (r instanceof Matrix matrix) {
                try {
                    matrix.getWidth();
                    matrix.getHeight();
                } catch (Exception ex) {
                    matrix.update(); //Performs a first read to initialize array length    
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
    public String getDescription() {
        return name + "-" + scanIndex;
    }
        
    @Override
    public String toString() {
        return Nameable.getShortClassName(getClass()) + " " + getDescription();
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
