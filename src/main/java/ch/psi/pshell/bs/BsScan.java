package ch.psi.pshell.bs;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import java.io.IOException;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.scan.ContinuousScan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.State;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Scan implementation that persists a given number of records received by a Stream object.
 */
public class BsScan extends ContinuousScan {

    final Stream stream;
    final int records;
    StreamValue firstVal;

    public BsScan(Stream stream, int records) {
        this(stream, records, 1);
    }
    
    public BsScan(Stream stream, int records, int passes) {
        super(stream.getReadables().toArray(new Readable[0]), 0.0, records - 1, records - 1, false, 0, passes, false);
        this.stream = stream;
        this.records = records;
    }

    final DeviceAdapter listener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            if (getRecordIndexInPass() < records) {
                onBeforeReadout(new double[]{getRecordIndexInPass()});
                StreamValue val = (StreamValue) value;
                Number[] setpoints = new Number[0];
                Number[] positions = new Number[0];
                Object[] values = new Object[val.values.size() + 1];
                values[0] = val.pulseId;
                System.arraycopy(val.values.toArray(), 0, values, 1, val.values.size());
                ScanRecord record = newRecord(setpoints, positions, values, val.getTimestamp());
                onAfterReadout(record);
                triggerNewRecord(record);
                synchronized (listener) {
                    listener.notifyAll();
                }
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
        int count;
        if (records <= 0) {
            return;
        }
        try {
            stream.addListener(listener);
            synchronized (listener) {
                while (getRecordIndexInPass() < records) {
                    stream.assertState(State.Busy);
                    assertNotAborted();
                    listener.wait();
                }
            }

        } finally {
            try {
                stream.removeListener(listener);
            } catch (Exception ex) {
                Logger.getLogger(BsScan.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
}
