package ch.psi.pshell.scan;

import ch.psi.pshell.device.DummyPositioner;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import java.io.IOException;

/**
 *
 */
public class StripScan extends ManualScan {

    long initialTimestamp;
    long initialDeviceTimestamp;

    public StripScan(Readable readable) {
        super(new Writable[]{new DummyPositioner("Time")}, new Readable[]{readable});
    }

    public StripScan(String name) {
        this(new Readable() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Object read() throws IOException, InterruptedException {
                return null;
            }
        });
    }

    public enum TimePositionerType {
        DeviceTimestamp,
        LocalTimestamp,
        DeviceTimespan,
        LocalTimespan,
    }

    TimePositionerType timePositionerType = TimePositionerType.DeviceTimestamp;

    public TimePositionerType getTimePositionerType() {
        return timePositionerType;
    }

    public void setTimePositionerType(TimePositionerType value) {
        timePositionerType = value;
    }

    public void append(Number value, long timestamp, long deviceTimestamp) {
        //long time = timestamp - startTimestamp;
        if (getRecordIndexInPass() == 0) {
            initialTimestamp = timestamp;
            initialDeviceTimestamp = deviceTimestamp;
        }
        long time = 0;
        switch(timePositionerType){
            case DeviceTimestamp -> time = deviceTimestamp;
            case LocalTimestamp -> time = timestamp;
            case DeviceTimespan -> time = deviceTimestamp - initialDeviceTimestamp;
            case LocalTimespan -> time = timestamp - initialTimestamp;                  
        }
        ScanRecord record = newRecord(new Number[]{Double.NaN}, new Number[]{time}, new Object[]{value}, 0, timestamp, new Long[]{deviceTimestamp});
        triggerNewRecord(record);
    }

}
