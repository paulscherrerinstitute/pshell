package ch.psi.pshell.scan;

import ch.psi.pshell.bs.StreamChannel;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamDevice;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.core.InlineDevice;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.DummyPositioner;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.utils.Chrono;
import ch.psi.pshell.device.Cacheable;
import ch.psi.pshell.device.Cacheable.CacheReadable;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.utils.Arr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 */
public class MonitorScan extends LineScan {

    final int time_ms;
    final int points;
    final boolean async;
    final boolean takeInitialValue;
    final boolean timeBased;
    final Readable[] originalReadables;
    final Object lock = new Object();

    Device trigger;
    Chrono chrono;
    Exception exception;
    long initialTimestamp;

    public MonitorScan(Device trigger, Readable[] readables, int points) {
        this(trigger, readables, points, -1);
    }

    public MonitorScan(Device trigger, Readable[] readables, int points, int time_ms) {
        this(trigger, readables, points, time_ms, true, false);
    }

    public MonitorScan(Device trigger, Readable[] readables, int points, int time_ms, boolean async, boolean takeInitialValue) {
        this(trigger, readables, points, time_ms, true, false, 1);
    }

    public MonitorScan(Device[] triggers, Readable[] readables, int points, int time_ms, boolean async, boolean takeInitialValue) {
        this(triggers, readables, points, time_ms, async, takeInitialValue, 1);

    }

    public MonitorScan(Device[] triggers, Readable[] readables, int points, int time_ms, boolean async, boolean takeInitialValue, int passes) {
        this(((triggers == null) || (triggers.length == 0)) ? null : new CompositeTrigger(triggers), readables, points, time_ms, async, takeInitialValue, passes);

    }

    public MonitorScan(Device trigger, Readable[] readables, int points, int time_ms, boolean async, boolean takeInitialValue, int passes) {
        super(((points > 0)||(time_ms<0)) ? new Writable[0] : new Writable[]{new DummyPositioner("Time")},
                getReadables(trigger, readables, async),
                new double[]{0.0},
                (points > 0) ? new double[]{points - 1} : new double[]{(time_ms>=0) ? time_ms : 100.0},
                (points > 0) ? points - 1 : ((time_ms>=0) ? time_ms : Integer.MAX_VALUE),
                false, 0, passes, false);
        this.trigger = trigger;
        this.time_ms = time_ms;
        this.points = points;
        this.takeInitialValue = takeInitialValue;
        this.async = async;
        this.timeBased =  (time_ms>0) && (points<=0);
        setResampleOnInvalidate(false);
        this.originalReadables = readables == null ? null : Arr.copy(readables);
    }

    public Device getTrigger(){
        return trigger;
    }

    public StreamDevice getStream(){
        return ((trigger!=null) && (trigger instanceof StreamDevice)) ? (StreamDevice) trigger : null;
    }

    @Override
    protected void openDevices() throws IOException, InterruptedException {
        super.openDevices();
        //TODO: forcing async update because JCAE freezes when making a read from a monitor callback (on any channel)
        if (async){
            for (int i=0; i< originalReadables.length; i++){
                if (originalReadables[i] instanceof InlineDevice){
                    if (readables[i] instanceof ReadonlyRegisterBase){
                        ((ReadonlyRegisterBase)readables[i]).setAsyncUpdate(true);
                    }
                }
            }
        }
        //If no trigger defined
        if (trigger == null) {
            //Uses the stream if contains a bsread device
            for (Readable r : readables) {
                Device src = getSourceDevice(r);
                if (src != null){
                    if ((src instanceof StreamChannel) || (src instanceof Stream.PidReader) || (src instanceof Stream.TimestampReader)) {
                        trigger = ((Device) src).getParent();
                        break;
                    }
                }
            }
        }
        if (trigger == null) {
            //Uses a composite trigger of all monitored readables if not
            List<Device> triggers = new ArrayList<>();
            for (Readable r : readables) {
                Device device = getSourceDevice(r);
                if (device != null){
                    triggers.add(device);
                }
            }
            if (triggers.size() > 0) {
                trigger = new CompositeTrigger(triggers.toArray(new Device[0]));
            }
        }

        if ((trigger != null) && (trigger instanceof CompositeTrigger)) {
            trigger.initialize();
        }
    }

    static Device getSourceDevice(Readable r){
        Device dev = null;
        if (r instanceof Device){
            dev = (Device) r;
        }
        if (r instanceof CacheReadable) {
            Cacheable parent = ((CacheReadable) r).getParent();
            if ((parent!=null) && (parent instanceof Device)){
                dev = (Device) parent;
            }
        }
        if ((dev!=null) && (r instanceof Device)){
            dev = InlineDevice.getSourceDevice(dev);
        }
        return dev;
    }


    @Override
    protected void closeDevices() {
        super.closeDevices();
        if ((trigger != null) && (trigger instanceof CompositeTrigger)) {
            try {
                trigger.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    static Readable[] getReadables(Device trigger, Readable[] readables, boolean async) {
        if (readables!=null){
            for (int i = 0; i < readables.length; i++) {
                if (async || (readables[i] == trigger)) {
                    if (readables[i] instanceof Cacheable){
                        readables[i] = ((Cacheable) readables[i]).getCache();
                    }
                }
            }
        }
        return readables;
    }

    DeviceListener listener = new DeviceAdapter() {
        @Override
        public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
            if ((chrono != null) && (!isCompleted())) {
                try {
                    appendSample();
                    if (points > 0) {
                        if (getRecordIndexInPass() == points) {
                            stopTriggerListening();
                            return;
                        }
                    }
                    if (time_ms > 0) {
                        if (chrono.isTimeout(time_ms)) {
                            stopTriggerListening();
                        }
                    }

                } catch (Exception ex) {
                    stopTriggerListening();
                    exception = ex;
                }
            }
        }
    };

    protected void stopTriggerListening() {
        if (async) {
            synchronized (lock) {
                if (trigger != null) {
                    trigger.removeListener(listener);
                }
                lock.notifyAll();
                chrono = null;
            }
        }
    }

    protected void doAbort() throws InterruptedException {
        super.doAbort();
        stopTriggerListening();
    }

    ScanRecord appendSample() throws IOException, InterruptedException {
        if (!isPaused()){
            Long timestamp = trigger.getTimestamp();
            if (timestamp == null) {
                timestamp =  0L;
            }
            long id = 0;
            StreamDevice stream = getStream();
            if (stream != null){
                StreamValue val = stream.take();
                if (val!=null){
                    id = val.getPulseId();
                }
            }
            if (getRecordIndexInPass()==0) {
                initialTimestamp = timestamp - chrono.getEllapsed();
            }
            if (timeBased){
                //Based on time
                return processPosition(new double[]{ (double)(timestamp - initialTimestamp)}, timestamp, id);
            } else {
                //Based on index
                return processPosition(new double[0], timestamp, id);
            }
        }
        return null;
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        try{
            if (trigger == null) {
                throw new IOException("No trigger defined");
            }
            if (trigger instanceof InlineDevice) {
                //TODO: trigger must be equal to readables[0]: add checking
                trigger = (Device) readables[0];
                readables[0] = ((Cacheable) readables[0]).getCache();
            }

            chrono = new Chrono();
            int steps = getNumberOfSteps()[0];
            if (takeInitialValue) {
                if (trigger.take() != null) {
                    appendSample();
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
                        ScanRecord ret = null;
                        do {
                            if ((i > 0) || !takeInitialValue) {
                                trigger.waitCacheChange(0);
                            }
                            ret = appendSample();
                            if ((ret!=null) && !ret.invalidated) {
                                if (i == steps) {
                                    break;
                                }
                                if (time_ms > 0) {
                                    if (chrono.isTimeout(time_ms)) {
                                        break;
                                    }
                                }
                            }
                        } while ((ret==null) || (ret.invalidated));
                    }
                } else {
                    boolean start = true;
                    while (  (time_ms <= 0) || (!chrono.isTimeout(time_ms))) {
                        if ((!start) || !takeInitialValue) {
                            trigger.waitCacheChange(time_ms - chrono.getEllapsed());
                        }
                        start = false;
                        appendSample();
                    }
                }
            }
        } finally {
            stopTriggerListening();
        }
    }

}
