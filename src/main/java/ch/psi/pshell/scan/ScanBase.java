package ch.psi.pshell.scan;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.Waveform;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.device.Motor;
import ch.psi.pshell.device.Movable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.ProcessVariable;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.device.Stoppable;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    long startTimestamp;
    long endTimestamp;

    int scanIndex = -1;
    String scanPath;
    int pass = 1;
    boolean accumulate;
    String name = "Scan";

    boolean useWritableReadback = true;
    boolean restorePosition = getRestorePositionOnRelativeScans();
    boolean abortOnReadableError = getAbortScansOnReadableError();
    boolean checkMotorPositions = getCheckScanMotorPositions();

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
        if (Context.getInstance() != null) {
            for (ScanListener listener : Context.getInstance().getScanListeners()) {
                addListener(listener);
            }
        }
    }

    String plotTitle;

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
        if (Context.getInstance() != null) {
            for (ScanListener listener : Context.getInstance().getScanListeners()) {
                addListener(listener);
            }
        }
    }

    int settleTimeout = -1;

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

    public void setPlotTitle(String plotTitle) {
        this.plotTitle = plotTitle;
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
        for (int i = 0; i < getWritables().length; i++) {
            getWritables()[i].write(position[i]);
        }
        waitSettled(position);
    }

    protected void waitSettled(double[] pos) throws IOException, InterruptedException {
        if (pos.length < getWritables().length) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < getWritables().length; i++) {
            waitSettled(getWritables()[i], pos[i], stepSize[i]);
        }
    }

    protected ReadonlyRegister getReadback(Writable writable) {
        if (writable instanceof ReadbackDevice) {
            return ((ReadbackDevice) writable).getReadback();
        } else if (writable instanceof ReadonlyRegister) {
            return (ReadonlyRegister) writable;
        }
        return null;
    }

    protected void waitSettled(Writable writable, double pos, double stepSize) throws IOException, InterruptedException {
        if (writable instanceof Movable) {
            Movable m = (Movable) writable;
            m.waitReady(settleTimeout);
            if (writable instanceof Motor) {
                if (checkMotorPositions) {
                    m.assertInPosition(pos);
                }
            } else {
                //Filtering some possible overshooting (Motors manage internally)
                m.waitInPosition(pos, 5000);
            }
        } else {
            ReadonlyRegister readback = getReadback(writable);
            if (readback != null) {
                double resolution = stepSize / 2;
                if (writable instanceof ProcessVariable) {
                    resolution = ((ProcessVariable) writable).getResolution();
                }
                readback.waitValueInRange(pos, resolution, settleTimeout);
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
                throw new IOException("Positioner type must be <Double> for relative scans: " + getDeviceName(writables[i]));
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

    @Override
    public void start() throws IOException, InterruptedException {
        startTimestamp = System.currentTimeMillis();
        if (relative) {
            readInitialPosition();
        }
        Boolean fail = false;
        try {
            assertFieldsOk();
            recordIndex = 0;
            result = newResult();
            setStreams();
            triggerStarted();
            moveToStart();
            try {
                onBeforeScan();
                try {
                    for (pass = 1; pass <= getNumberOfPasses(); pass++) {
                        doScan();
                    }
                } finally {
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
                resetStreams();
            }
        } finally {
            if (relative && restorePosition) {
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
        checkInterrupted();
        assertNotAborted();

        ScanRecord record = newRecord();
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
            onBeforeReadout(pos);

            record.invalidated = false;
            record.values = new Object[getReadables().length];
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
                    record.values[j] = getReadables()[j].read();
                    if (record.values[j] instanceof List) { //Jython Lists
                        record.values[j] = Convert.toArray((List) record.values[j]);
                        record.values[j] = Convert.toDouble(record.values[j]); //TODO: let Data manager make this transformation based on getPreserveTypes()
                    } else if (record.values[j] instanceof Map) { //JS Lists
                        record.values[j] = Convert.toArray((Map) record.values[j]);
                        record.values[j] = Convert.toDouble(record.values[j]); //TODO: let Data manager make this transformation based on getPreserveTypes()
                    }
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
            record.timestamp = (timestamp == null) ? System.currentTimeMillis() : timestamp;
            onAfterReadout(record);
            if (record.invalidated) {
                logger.warning("Resampling record " + record.index);
            }
        } while (record.invalidated);
        if (!record.canceled) {
            triggerNewRecord(record);
        }
        return record;
    }

    protected ScanRecord newRecord() {
        ScanRecord record = new ScanRecord();
        record.index = recordIndex;
        record.pass = getCurrentPass();
        record.dimensions = getDimensions();
        record.timestamp = System.currentTimeMillis();
        recordIndex++;
        return record;
    }

    protected ScanRecord newRecord(Number[] setpoints, Number[] positions, Object[] values, long timestamp) {
        ScanRecord ret = newRecord();
        ret.setpoints = setpoints;
        ret.positions = positions;
        ret.values = values;
        ret.timestamp = timestamp;
        return ret;
    }

    protected void onBeforeReadout(double[] pos) {
    }

    protected void onAfterReadout(ScanRecord record) {
    }

    abstract protected void doScan() throws IOException, InterruptedException;

    protected void triggerStarted() {
        executionThread = Thread.currentThread();
        if (Context.getInstance() != null) {
            name = Context.getInstance().getExecutionPars().toString();
        }
        aborted = false;
        for (ScanListener listener : getListeners()) {
            listener.onScanStarted(ScanBase.this, plotTitle);
            if (scanIndex < 0) {
                if (Context.getInstance() != null) {
                    scanIndex = Context.getInstance().getDataManager().getScanIndex(this);
                    String scanRoot = Context.getInstance().getExecutionPars().getPath();
                    if (IO.isSubPath(scanRoot, Context.getInstance().getSetup().getDataPath())) {
                        scanRoot = IO.getRelativePath(scanRoot, Context.getInstance().getSetup().getDataPath());
                    }
                    scanPath = scanRoot + " | " + Context.getInstance().getDataManager().getScanPath(this);
                    accumulate = Context.getInstance().getExecutionPars().getAccumulate();
                }
            }
        }
    }

    ScanRecord currentRecord;

    protected void triggerNewRecord(ScanRecord record) {
        if (accumulate) {
            result.records.add(record);
        }
        currentRecord = record;
        for (ScanListener listener : getListeners()) {
            listener.onNewRecord(ScanBase.this, record);
        }
    }

    protected void triggerEnded(Exception ex) {
        result.completed = true;
        for (ScanListener listener : getListeners()) {
            listener.onScanEnded(ScanBase.this, ex);
        }
    }

    @Override
    public int getIndex() {
        return scanIndex;
    }

    @Override
    public String getPath() {
        return scanPath;
    }

    @Override
    public boolean getAccumulate() {
        return accumulate;
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
    public String[] getWritableNames() {
        ArrayList<String> names = new ArrayList();
        for (Writable writable : getWritables()) {
            names.add(getDeviceName(writable));
        }
        return names.toArray(new String[0]);
    }

    @Override
    public String[] getReadableNames() {
        ArrayList<String> names = new ArrayList();
        for (Readable readable : getReadables()) {
            names.add(getDeviceName(readable));
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

    protected void setStreams() throws IOException, InterruptedException {
        startedStreams = new ArrayList<>();
        for (Readable r : readables) {
            if (r instanceof Stream) {
                if (((Stream) r).getState() == State.Ready) {
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

    protected void resetStreams() {
        if (startedStreams != null) {
            for (Stream stream : startedStreams) {
                try {
                    stream.stop();
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
    public void setRecordIndexOffset(int value) {
        recordIndexOffset = Math.max(value, 0);
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
    public boolean getCheckMotorPositions() {
        return checkMotorPositions;
    }

    @Override
    public void setCheckMotorPositions(boolean value) {
        checkMotorPositions = value;
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

    static boolean checkScanMotorPositions = true;

    public static boolean getCheckScanMotorPositions() {
        return checkScanMotorPositions;
    }

    public static void setCheckScanMotorPositions(boolean value) {
        checkScanMotorPositions = value;
    }
}
