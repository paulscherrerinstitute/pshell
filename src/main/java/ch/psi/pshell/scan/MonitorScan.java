package ch.psi.pshell.scan;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.DummyPositioner;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.utils.Chrono;
import ch.psi.pshell.device.Cacheable;
import ch.psi.pshell.device.DeviceAdapter;
import java.io.IOException;

/**
 *
 */
public class MonitorScan extends LineScan {    
    final int time_ms;
    final int points;
    final boolean async;
    final boolean takeInitialValue;
    final Object lock = new Object();

    Device trigger;
    Chrono chrono;
    Exception exception;

    public MonitorScan(Device trigger, Readable[] readables, int points) {
        this(trigger, readables, points, -1);
    }

    public MonitorScan(Device trigger, Readable[] readables, int points, int time_ms) {
        this(trigger, readables, points, time_ms, true, false);
    }

    public MonitorScan(Device trigger, Readable[] readables, int points, int time_ms, boolean async, boolean takeInitialValue) {
        super((points > 0) ? new Writable[0] : new Writable[]{new DummyPositioner("Time")},
                getReadables(trigger, readables, async),
                new double[]{0.0},
                (points > 0) ? new double[]{points - 1} : new double[]{time_ms},
                (points > 0) ? points - 1 : time_ms,
                false, 0, 1, false);
        this.trigger = trigger;
        this.time_ms = time_ms;
        this.points = points;
        this.takeInitialValue = takeInitialValue;
        this.async = async;
    }

    static Readable[] getReadables(Device trigger, Readable[] readables, boolean async) {
        for (int i = 0; i < readables.length; i++) {
            if (((readables[i] == trigger) || async) && (readables[i] instanceof Cacheable)) {
                readables[i] = ((Cacheable) readables[i]).getCache();
            }
        }
        return readables;
    }

    DeviceListener listener = new DeviceAdapter() {
        @Override
        public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
            if ((chrono != null) && (!isCompleted())) {
                try {
                    appendSample(timestamp);
                    if (points > 0) {
                        if (getRecordIndex() == points) {
                            onAfterScan();
                            return;
                        }
                    }
                    if (time_ms > 0) {
                        if (chrono.isTimeout(time_ms)) {
                            onAfterScan();
                        }
                    }
                } catch (Exception ex) {
                    onAfterScan();
                    exception = ex;
                }

            }
        }
    };

    @Override
    protected void onAfterScan() {
        if (async) {
            synchronized (lock) {
                trigger.removeListener(listener);
                lock.notifyAll();
                chrono = null;
            }
        }
    }

    void appendSample(Long timestamp) throws IOException, InterruptedException {
        if (timestamp == null) {
            timestamp = trigger.getTimestamp();
            if (timestamp == null) {
                timestamp = 0L;
            }
        }
        if (firstSample) {
            firstSample = false;
            initialTimestamp = timestamp - chrono.getEllapsed();
        }
        if (points > 0) {
            //Based on index
            processPosition(new double[0], timestamp);
        } else {
            //Based on time            
            //processPosition(new double[]{chrono.getEllapsed()});      
            processPosition(new double[]{timestamp - initialTimestamp}, timestamp);
        }
    }

    long initialTimestamp;
    boolean firstSample;

    @Override
    protected void doScan() throws IOException, InterruptedException {
        if (trigger instanceof InnerDevice){       
            //TODO: trigger must be equal to readables[0]: add checking
            this.trigger = (Device) readables[0];
            readables[0] = ((Cacheable) readables[0]).getCache();            
        }        
        
        firstSample = true;
        chrono = new Chrono();
        int steps = getNumberOfSteps()[0];
        if (takeInitialValue) {
            if (trigger.take() != null) {
                appendSample(null);
            }
        }
        if (async) {
            trigger.addListener(listener);
            synchronized (lock) {
                lock.wait();
            }
            if (exception != null) {
                if (exception instanceof IOException) {
                    throw (IOException) exception;
                }
                throw new IOException(exception);
            }

        } else {
            if (points > 0) {
                for (int i = 0; i <= steps; i++) {
                    if ((i > 0) || !takeInitialValue) {
                        trigger.waitCacheChange(0);
                    }
                    appendSample(null);
                    if (i == steps) {
                        break;
                    }
                    if (time_ms > 0) {
                        if (chrono.isTimeout(time_ms)) {
                            break;
                        }
                    }
                }
            } else {
                boolean start = true;
                while (!chrono.isTimeout(time_ms)) {
                    if ((!start) || !takeInitialValue) {
                        trigger.waitCacheChange(time_ms - chrono.getEllapsed());
                    }
                    start = false;
                    appendSample(null);
                }
            }
        }
    }

}
