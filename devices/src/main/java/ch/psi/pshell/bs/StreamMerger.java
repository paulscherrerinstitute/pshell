package ch.psi.pshell.bs;

import ch.psi.bsread.message.ChannelConfig;
import ch.psi.bsread.message.Type;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.TimestampedValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StreamMerger extends Stream {

    final Stream stream1;
    final Stream stream2;

    long lastPulseId;
    StreamValue v1;
    StreamValue v2;

    Map<String, ChannelConfig> config;

    final DeviceListener listener = new DeviceListener() {
        public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
            try {
                update();
            } catch (Exception ex) {
                Logger.getLogger(StreamMerger.class.getName()).log(Level.FINEST, null, ex);
            }
        }
    };

    public StreamMerger(String name, Stream stream1, Stream stream2) {
        super(name, stream1, stream2);
        this.stream1 = stream1;
        this.stream2 = stream2;
        lastPulseId = -1;
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    @Override
    protected void doInitialize() {
        channels.clear();
        channelNames.clear();
        readables.clear();
        readables.add(this);
    }

    protected void doUpdate() {       
        boolean hasMoreSt1,hasMoreSt2;
        while (true) {
            hasMoreSt1=false;
            hasMoreSt2=false;
            if ((v1 == null) || ((v1 != null) && (v2 != null) && (v1.pulseId < v2.pulseId))) {
                if (stream1.getBufferCapacity() > 0) {
                    TimestampedValue tv = stream1.popBuffer();
                    v1 = (tv == null) ? null : (StreamValue) tv.getValue();
                    hasMoreSt1 = (v1 != null);
                } else {
                    v1 = stream1.take();
                }
            }
            if ((v2 == null) || ((v1 != null) && (v2 != null) && (v1.pulseId > v2.pulseId))) {
                if (stream2.getBufferCapacity() > 0) {
                    TimestampedValue tv = stream2.popBuffer();
                    v2 = (tv == null) ? null : (StreamValue) tv.getValue();
                    hasMoreSt2 = (v2 != null);
                } else {
                    v2 = stream2.take();
                }
            }
            if ((v1 != null) && (v2 != null) && (v1.pulseId == v2.pulseId)) {
                if (v1.pulseId > lastPulseId) {
                    List<String> names = v1.getKeys();
                    names.addAll(v2.getKeys());
                    List<StreamValue> values = v1.getValues();
                    values.addAll(v2.getValues());
                    HashMap<String, ChannelConfig> config = new HashMap<>();
                    config.putAll(v1.getConfig());
                    config.putAll(v2.getConfig());

                    StreamValue value = new StreamValue(v1.pulseId, v1.timestamp, v1.nanosOffset, names, values, config);

                    if (pidReader != null) {
                        setCache((DeviceBase) pidReader, (Object)v1.pulseId, v1.timestamp, v1.nanosOffset);
                    }
                    if (timestampReader != null) {
                        setCache((DeviceBase) timestampReader, (Object)v1.timestamp, v1.timestamp, v1.nanosOffset);
                    }                    
                    for (String channel : names) {
                        StreamChannel c = channels.get(channel);
                        ChannelConfig cfg = config.get(channel);
                        if (c==null){                            
                            int[] shape = cfg.getShape();
                            Type type = cfg.getType();
                            if ((shape == null) || (shape.length == 0)) {
                                c = new Scalar(channel, this, channel);
                            } else if ((shape.length == 1) && (shape[0] <= 1)) {
                                c = new Scalar(channel, this, channel);
                            } else if ((shape.length == 2) && (getCreateMatrix())) {
                                c = new Matrix(channel, this, channel);
                            } else {
                                c = new Waveform(channel, this, channel);
                            }
                            try {
                                c.initialize();
                            } catch (Exception ex) {
                            }                            
                            appendChild(c);                            
                        }
                        c.set(v1.pulseId, v1.timestamp, v1.nanosOffset, value.getValue(channel), cfg);
                    }
                    setCache(value, v1.timestamp, v1.nanosOffset);
                }
                v1 = null;
                v2 = null;
            } else {
                if (!hasMoreSt1 &&  !hasMoreSt2) {
                    break;
                }
            }
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        if (value) {
            stream1.addListener(listener);
            stream2.addListener(listener);
        } else {
            stream1.removeListener(listener);
            stream2.removeListener(listener);
        }
    }

    @Override
    protected void doClose() {
        doSetMonitored(false);
    }

    @Override
    public void start() {
        if (!stream1.isStarted()) {
            stream1.start();
        }
        if (!stream2.isStarted()) {
            stream2.start();
        }
        setState(State.Busy);
    }

    @Override
    public void stop() {
        setState(State.Ready);
        if (stream1.isStarted()) {
            stream1.stop();
        }
        if (stream2.isStarted()) {
            stream2.stop();
        }
    }

    @Override
    public boolean isStarted() {
        return stream1.isStarted() && stream2.isStarted();
    }
    
    public String getAddress() {
        return stream1.getAddress() +  " + " + stream2.getAddress();
    }

    public String getStreamSocket() {
        return null;
    }

    public int getSocketType() {
        return -1;
    }    
}
