package ch.psi.pshell.scan;

import ch.psi.pshell.bs.StreamDevice;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.DummyPositioner;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.devices.InlineDevice;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Scan implementation that persists a given number of records received by a Stream object.
 */
public class BsScan extends LineScan {

    final int records;
    final int time_ms;
    StreamDevice stream;
    Chrono chrono;
    long initialTimestamp;
    boolean addPid;

    public BsScan(StreamDevice stream, int records, int time_ms) {
        this(stream, records, time_ms, 1);                        
    }
    
    public BsScan(StreamDevice stream, int records, int time_ms, int passes) {
        this(stream.getReadables().toArray(new Readable[0]), records, time_ms, passes, stream);
        this.addPid = true; //stream.getReadables()  contains PID
    }
    
    static Readable[] adjustReadables(String[] channels){
        Readable[] ret = new Readable[channels.length];
        for (int i=0; i<channels.length; i++){
            if (!channels[i].startsWith("bs://")){
                channels[i] = "bs://" + channels[i];
            }  
            ret[i] = new InlineDevice(channels[i]);
        }
        return ret;
    }
    
    public BsScan(String[] channelNames, int records, int time_ms) {
        this(channelNames, records, time_ms, 1);
    }
    
    public BsScan(String[] channelNames, int records, int time_ms, int passes) {
        this(adjustReadables(channelNames), records, time_ms, passes, null);
        if (getReadableNames()[0].equalsIgnoreCase("PID")){
            this.addPid = true;
        }        
    }    

    
     BsScan(Readable[] readables, int records, int time_ms, int passes, StreamDevice stream) {
        super(  (records > 0) ? new Writable[0] : new Writable[]{new DummyPositioner("Time")},
                readables,
                new double[]{0.0},
                (records > 0) ? new double[]{records - 1} : new double[]{time_ms},
                (records > 0) ? records - 1 : time_ms,
                false, 0, passes, false);           
        this.stream = stream;
        this.records = records;
        this.time_ms = time_ms;        
    }    
    
    
    public StreamDevice getStream(){
        //innrerStream
        if (stream==null){
            for (Device dev : getInnerDevices()){
                if (dev instanceof StreamDevice streamDevice){
                    stream = streamDevice;
                    break;
                }
            }
        }
        return stream;
    }

    final DeviceListener listener = new DeviceListener() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            if (!finished()) {        
                if (!isPaused()){
                    int index = getRecordIndexInPass();                 
                    onBeforeReadout(new double[]{index});
                    StreamValue val = (StreamValue) value;   
                    if (index==0) {
                        initialTimestamp = val.getTimestamp() - chrono.getEllapsed();
                    }                  
                    Number[] setpoints = (records < 0) ? new Number[]{Double.NaN} : new Number[0];
                    Number[] positions = (records < 0) ? new Number[]{val.getTimestamp()     - initialTimestamp} :new Number[0];
                    int offset = (addPid ? 1 : 0);
                    var streamValues = val.getValues();
                    Object[] values = new Object[streamValues.size() + offset];
                    Long[] deviceTimestamps = new Long[values.length];
                    if (addPid){
                        values[0] = val.getPulseId();
                    }
                    System.arraycopy(streamValues.toArray(), 0, values, offset , streamValues.size());
                    Arrays.fill(deviceTimestamps, val.getTimestamp());
                    ScanRecord record = newRecord(setpoints, positions, values, val.getPulseId(), val.getTimestamp(), deviceTimestamps);
                    onAfterReadout(record);
                    triggerNewRecord(record);
                }
            }
            synchronized (listener) {
                listener.notifyAll();
            }            
        }

        @Override
        public void onStateChanged(Device device, State state, State former) {
            synchronized (listener) {
                listener.notifyAll();
            }
        }
    };

    @Override
    protected void doScan() throws IOException, InterruptedException {
        chrono = new Chrono();
            if (finished()){
            return;
        }
        try {
            getStream().addListener(listener);
            while (!finished()) {         
                getStream().assertState(State.Busy);
                assertNotAborted();
                synchronized (listener) {
                    listener.wait();
                }
            }

        } finally {
            try {
                getStream().removeListener(listener);
            } catch (Exception ex) {
                Logger.getLogger(BsScan.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
    boolean finished(){
        if ((records <= 0) && (time_ms<=0)){
            return true;
        }
        if ((records>0) && (getRecordIndexInPass() >= records)){
            return true;
        }
        if ((chrono !=null) &&(time_ms>0) && (chrono.isTimeout(time_ms))){
            return true;
        }          
        return false;
    }
}
