package ch.psi.pshell.scan;

import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Readable;
import ch.psi.utils.Reflection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 */
public interface Scan {

    public void start() throws IOException, InterruptedException;

    public ScanResult getResult();
    
    public Layout getDataLayout();
    
    public Provider getDataProvider();
    
    public void setCallbacks(ScanCallbacks callbacks);

    public ScanCallbacks getCallbacks();
    
    public Writable[] getWritables();

    public String[] getWritableNames();

    public Readable[] getReadables();

    public String[] getReadableNames();

    public Nameable[] getDevices();

    public String[] getDeviceNames();

    public int getReadableIndex(Object obj);

    public int getWritableIndex(Object obj);

    public int getMonitorIndex(Object obj);
    
    public int getSnapIndex(Object obj);

    public int getDiagIndex(Object obj);

    public int getDeviceIndex(Object obj);

    public double[] getStart();

    public double[] getEnd();

    public double[] getStepSize();

    public int[] getNumberOfSteps();

    public int getNumberOfRecords();

    public int getNumberOfPasses();

    public boolean isZigzag();

    public int getLatency();

    public int getCurrentPass();

    public boolean isCurrentPassBackwards();

    public int getDimensions();

    public boolean isArray(int readableIndex);

    public void abort() throws InterruptedException;
    
    public boolean isAborted();

    public boolean canPause();
    
    public void setCanPause(boolean value);
    
    public void pause() throws InterruptedException;
    
    public void resume() throws InterruptedException;
    
    public boolean isPaused();
   
    public int getSettleTimeout();

    public void setSettleTimeout(int value);

    public long getStartTimestamp();

    public long getEndTimestamp();
    
    public default long getTimeElapsed() {
        long start = getStartTimestamp();
        if (start<=0){
            return 0;
        }
        long end = getEndTimestamp();
        if (end <=0){
            return System.currentTimeMillis();
        }
        return Math.max(0, end - start);
    }

    public int getRecordIndex();
    
    public int getRecordIndexInPass();
    
    public ScanRecord getCurrentRecord();
        
    /**
     * Allows splitting records in multiple tables
     */
    public int getRecordIndexOffset();

    public void setRecordIndexOffset(int value);
    
    public boolean isStarted();

    public boolean isRunning();    

    public boolean isCompleted();

    public int getIndex();

    public String getPath();

    public Object readData(String device);
    
    public Object readMonitor(String device);
    
    public Object readDiag(String device);
    
    public Map<Readable, Object> readSnaps();
    
    public Object readSnap(String device);
       
    public long[] readTimestamps();    
    
    public String getTag();
    
    public String getDescription();
    
    /**
     * If true the scans records are kept in memory.
     */
    public boolean getKeep();
    
    /**
     * If true the start scan event waits the reception of first record
     */
    public boolean isLazy();        
    
    public Thread getThread();
    
    public void setPlotTitle(String plotTitle);
    
    public String getPlotTitle(); 
    
    public void setHidden(boolean value);
    
    public boolean isHidden();     
    
    public void setSplitPasses(boolean value);
    
    public boolean getSplitPasses();            

    public void setUseWritableReadback(boolean value);

    public boolean getUseWritableReadback();

    public boolean getRestorePosition();

    public void setRestorePosition(boolean value);

    public boolean getAbortOnReadableError();

    public void setAbortOnReadableError(boolean value);

    public boolean getCheckPositions();

    public void setCheckPositions(boolean value);
    
    public void setMeta(Map meta);
    
    public Map getMeta();
    
    public Device[] getMonitors();
        
    public String[] getMonitorNames();    

    public void setMonitors(Device[] monitors);

    public Readable[] getDiags();
        
    public String[] getDiagNames();    

    public void setDiags(Readable[] diags);
        
    public Readable[] getSnaps();
    
    public String[] getSnapsNames(); 
    
    public String getDeviceName(Nameable device);
    
    public void setSnaps(Readable[] snaps);

    public boolean getInitialMove();

    public void setInitialMove(boolean value);

    public boolean getResampleOnInvalidate();

    public void setResampleOnInvalidate(boolean value);

    public boolean getParallelPositioning();

    public void setParallelPositioning(boolean value);     

    @Reflection.Hidden
    public default String getHeader(String separator) {
        ArrayList<String> header = new ArrayList<>();
        header.add(String.format("%-12s", "Time"));
        header.add("Index");
        for (Writable w : getWritables()) {
            header.add(w.getAlias());
        }
        for (ch.psi.pshell.device.Readable r : getReadables()) {
            header.add(r.getAlias());
        }
        return String.join(separator, header);
    }
}
