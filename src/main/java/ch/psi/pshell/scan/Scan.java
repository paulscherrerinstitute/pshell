package ch.psi.pshell.scan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Nameable;
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

    public Writable[] getWritables();

    public String[] getWritableNames();

    public Readable[] getReadables();

    public String[] getReadableNames();

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

    public int getSettleTimeout();

    public void setSettleTimeout(int value);

    public long getStartTimestamp();

    public long getEndTimestamp();

    public int getRecordIndex();
    
    public int getRecordIndexInPass();
    
    /**
     * Allows splitting records in multiple tables
     */
    public int getRecordIndexOffset();

    public void setRecordIndexOffset(int value);

    public boolean isCompleted();

    public int getIndex();

    public String getPath();
    
    public String getTag();
    
    /**
     * If true the scans records are kept in memory.
     */
    public boolean getKeep();
    
    public Thread getThread();
    
    public void setPlotTitle(String plotTitle);
    
    public String getPlotTitle(); 
    
    public void setHidden(boolean value);
    
    public boolean isHidden();     

    public void setUseWritableReadback(boolean value);

    public boolean getUseWritableReadback();

    public boolean getRestorePosition();

    public void setRestorePosition(boolean value);

    public boolean getAbortOnReadableError();

    public void setAbortOnReadableError(boolean value);

    public boolean getCheckPositions();

    public void setCheckPositions(boolean value);
    
    public boolean getInitialMove();

    public void setInitialMove(boolean value);    
    
    public boolean getParallelPositioning();

    public void setParallelPositioning(boolean value);     

    @Reflection.Hidden
    public default String getHeader(String separator) {
        ArrayList<String> header = new ArrayList<>();
        header.add(String.format("%-12s", "Time"));
        header.add("Index");
        for (Writable w : getWritables()) {
            header.add(getDeviceName(w));
        }
        for (ch.psi.pshell.device.Readable r : getReadables()) {
            header.add(getDeviceName(r));
        }
        return String.join(separator, header);
    }

    default String getDeviceName(Nameable dev) {
        Context c = Context.getInstance();
        return (c == null) ? dev.getName() : c.getDataManager().getAlias(dev);
    }
}
