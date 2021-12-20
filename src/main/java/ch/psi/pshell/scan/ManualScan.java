package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.device.Writable;
import ch.psi.utils.Str;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;

/**
 *
 */
public class ManualScan extends DiscreteScan {

    public ManualScan(Writable[] writables, Readable[] readables) {
        this(writables, readables,null, null, null, false);
    }

    public ManualScan(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[], boolean relative) {
        super(writables, readables,
                start == null ? new double[writables.length] : start,
                end == null ? new double[writables.length] : end,
                numberOfSteps == null ? new int[writables.length] : numberOfSteps, relative, 0, 1, false);
    }
    
    //This is just a hack for fixing the interface for JEP 
    public static class ManualScanDev extends ManualScan{
        public ManualScanDev(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[], boolean relative) {
            super(writables, readables,start, end, numberOfSteps, relative);
        }        
    } 
    public static class ManualScanStr extends ManualScan{
        public ManualScanStr(String[] writables, String[] readables, double[] start, double[] end, int numberOfSteps[], boolean relative) {
            super(writables, readables,start, end, numberOfSteps, relative);
        }        
    }        
    
    public ManualScan(String[] writables, String[] readables) {
        this(writables, readables, null, null, null, false);
    }

    public ManualScan(String[] writables, String[] readables, double[] start, double[] end, int numberOfSteps[], boolean relative) {
        super(getDummyWritables(writables), getDummyReadables(readables),
                start == null ? new double[writables.length] : start,
                end == null ? new double[writables.length] : end,
                numberOfSteps == null ? new int[writables.length] : numberOfSteps, relative, 0, 1, false);
    }    
    
    public ManualScan(String[] writables, Readable[] readables) {
        this(writables, readables, null, null, null, false);
    }

    public ManualScan(String[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[], boolean relative) {
        super(getDummyWritables(writables), readables,
                start == null ? new double[writables.length] : start,
                end == null ? new double[writables.length] : end,
                numberOfSteps == null ? new int[writables.length] : numberOfSteps, relative, 0, 1, false);
    }        

    public static class DummyWritable implements Writable {

        String name;

        DummyWritable(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void write(Object value) {
        }
    }

    public static class DummyReadable implements Readable {

        String name;

        DummyReadable(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object read() {
            return null;
        }
    }

    public static class DummyReadableArray extends DummyReadable implements ReadableArray {
        int size;
        DummyReadableArray(String name, int size) {
            super(name);
            this.size = size;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public Object read() {
            return null;
        }
    }
        
    public static class DummyReadableMatrix extends DummyReadable implements ReadableMatrix {
        int width;
        int height;
        DummyReadableMatrix(String name, int width, int height) {
            super(name);
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
        
        @Override
        public Object read() {
            return null;
        }
    }    

    public static Writable[] getDummyWritables(String[] names) {
        Writable[] ret = new Writable[names.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new DummyWritable(names[i]);
        }
        return ret;
    }

    public static Readable[] getDummyReadables(String[] names) {
        Readable[] ret = new Readable[names.length];
        for (int i = 0; i < ret.length; i++) {
            if (names[i].contains("[")) {
                try {
                    if (Str.count(names[i],"[") == 2){
                        String name = names[i].substring(0, names[i].lastIndexOf("["));
                        int height = Integer.valueOf(names[i].substring(names[i].lastIndexOf("[") + 1, names[i].lastIndexOf("]")));
                        name = names[i].substring(0, names[i].indexOf("["));
                        int width = Integer.valueOf(names[i].substring(names[i].indexOf("[") + 1, names[i].indexOf("]")));
                        ret[i] = new DummyReadableMatrix(name, width, height);                   
                    } else{
                        String name = names[i].substring(0, names[i].indexOf("["));
                        int size = Integer.valueOf(names[i].substring(names[i].indexOf("[") + 1, names[i].indexOf("]")));
                        ret[i] = new DummyReadableArray(name, size);
                    }
                    continue;

                } catch (Exception ex) {
                }
            }
            ret[i] = new DummyReadable(names[i]);
        }
        return ret;
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
    }
    
    public void append(Object setpoints, Object positions, Object values) throws InterruptedException, ScanAbortedException {
         append(setpoints, positions, values, null);
    }

    public void append(Object setpoints, Object positions, Object values, Long timestamp) throws InterruptedException, ScanAbortedException {
        checkInterrupted();
        try {
            ScanRecord record = newRecord();
            record.setpoints = new Number[getWritables().length];
            record.positions = new Number[getWritables().length];
            record.values = new Object[getReadables().length];
            record.deviceTimestamps = new Long[getReadables().length];
            for (int j = 0; j < getWritables().length; j++) {
                record.setpoints[j] = (Number) Array.get(setpoints, j);
                record.positions[j] = (Number) Array.get(positions, j);
            }
            for (int j = 0; j < getReadables().length; j++) {
                Object val = Array.get(values, j);
                if (val instanceof TimestampedValue){
                    record.deviceTimestamps[j] = ((TimestampedValue)val).getTimestamp();
                    record.values[j] = ((TimestampedValue)val).getValue();
                } else {
                    record.values[j] = val;
                }
            }
            record.localTimestamp = System.currentTimeMillis();
            record.timestamp = timestamp;
            triggerNewRecord(record);            
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
        waitPauseDone();
    }

    @Override
    public void start() throws IOException, InterruptedException {
        startTimestamp = System.currentTimeMillis();
        assertFieldsOk();
        recordIndex = 0;
        result = newResult();
        openDevices();
        triggerStarted();
        if(getInitialMove()){
            moveToStart();
        }
        onBeforeScan();
        if (getCallbacks()!=null){
            getCallbacks().onBeforeScan(this);
        }        
        startMonitors();
    }

    public void end() throws IOException, InterruptedException {
        try {
            stopMonitors();
            onAfterScan();
            if (getCallbacks()!=null){
                getCallbacks().onAfterScan(this);
            }
            triggerEnded(null);
            closeDevices();
        } finally {
            endTimestamp = System.currentTimeMillis();
        }
    }
}
