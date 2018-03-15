package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.device.Writable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;

/**
 *
 */
public class ManualScan extends DiscreteScan {

    public ManualScan(Writable[] writables, Readable[] readables) {
        super(writables, readables, null, null, -1, false, 0, 1, false);
    }

    public ManualScan(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[], boolean relative) {
        super(writables, readables,
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
                    String name = names[i].substring(0, names[i].indexOf("["));
                    int size = Integer.valueOf(names[i].substring(names[i].indexOf("[") + 1, names[i].indexOf("]")));
                    ret[i] = new DummyReadableArray(name, size);
                    continue;

                } catch (Exception ex) {
                }
            }
            ret[i] = new DummyReadable(names[i]);
        }
        return ret;
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

    @Override
    protected void doScan() throws IOException, InterruptedException {
    }
    
    public void append(Object setpoints, Object positions, Object values) throws InterruptedException {
         append(setpoints, positions, values, null);
    }

    public void append(Object setpoints, Object positions, Object values, Long timestamp) throws InterruptedException {
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
            record.timestamp = (timestamp == null) ? record.localTimestamp : timestamp;
            triggerNewRecord(record);
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    @Override
    public void start() throws IOException, InterruptedException {
        startTimestamp = System.currentTimeMillis();
        assertFieldsOk();
        recordIndex = 0;
        result = newResult();
        openDevices();
        triggerStarted();
        moveToStart();
        onBeforeScan();
    }

    public void end() throws IOException, InterruptedException {
        try {
            onAfterScan();
            triggerEnded(null);
            closeDevices();
        } finally {
            endTimestamp = System.currentTimeMillis();
        }
    }
}
