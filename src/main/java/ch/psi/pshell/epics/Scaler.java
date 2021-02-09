package ch.psi.pshell.epics;

import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public class Scaler extends ReadonlyRegisterBase<int[]> implements ReadonlyRegisterArray<int[]> {

    final String channelName;
    final ArrayList<ScalerChannel> channels;
    final ChannelInteger control;

    public Scaler(String name, String channel) {
        super(name);
        this.channelName = channel;
        control = new ChannelInteger(getName() + " control", channel + ".CNT", false);
        channels = new ArrayList<>();
        setTrackChildren(true);
        addChild(control);
        control.addListener(new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                if (value != null) {
                    if (getState().isInitialized()) {
                        setState(value.equals(1) ? State.Busy : State.Ready);
                    }
                }
            }
        });
    }

    public String getChannelName() {
        return channelName;
    }

    /**
     * T = S1 / FREQ
     */
    public double getElapsedTime() throws InterruptedException, IOException {
        try {
            return Epics.get(Scaler.this.channelName + ".T", Double.class);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * TP = PR1 / FREQ
     */
    public void setPresetTime(double value) throws InterruptedException, IOException {
        try {
            Epics.putq(Scaler.this.channelName + ".TP", Double.valueOf(value));
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public double getPresetTime() throws InterruptedException, IOException {
        try {
            return Epics.get(Scaler.this.channelName + ".TP", Double.class);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public double getFrequency() throws InterruptedException, IOException {
        try {
            return Epics.get(Scaler.this.channelName + ".FREQ", Double.class);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public int getDelay() throws InterruptedException, IOException {
        try {
            return Epics.get(Scaler.this.channelName + "DLY", Integer.class);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void setDelay(int delay) throws InterruptedException, IOException {
        try {
            Epics.putq(Scaler.this.channelName + "DLY", delay);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void start() throws IOException, InterruptedException {
        control.write(1);
    }

    public void stop() throws IOException, InterruptedException {
        control.write(0);
    }

    public void setOneShot() throws InterruptedException, IOException {
        try {
            Epics.putq(Scaler.this.channelName + ".CONT", 0);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void setAutoCount() throws InterruptedException, IOException {
        try {
            Epics.putq(Scaler.this.channelName + ".CONT", 1);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        channels.clear();
        try {
            int numChannels = 0;
            if (isSimulated()) {
                numChannels = 16;
            } else {
                numChannels = Epics.get(channelName + ".NCH", Integer.class);
            }
            for (int i = 1; i <= numChannels; i++) {
                channels.add(new ScalerChannel(i));
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        super.doInitialize();    //After  adding children so they are initialized...
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        startSimulationTimer();
    }

    @Override
    protected void onSimulationTimer() throws IOException, InterruptedException {
        if (!isReady()) {
            for (ScalerChannel channel : getChannels()) {
                int count = channel.take() == null ? 0 : channel.take();
                channel.setCache(count + 1, null, 0L);
            }
        }
    }

    public ScalerChannel[] getChannels() {
        return channels.toArray(new ScalerChannel[0]);
    }

    @Override
    protected int[] doRead() throws IOException, InterruptedException {
        int[] ret = new int[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            ret[i] = channels.get(i).read();
        }
        return ret;
    }

    void updateCache() {
        int[] cache = new int[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            ScalerChannel channel = channels.get(i);
            cache[i] = ((channel == null) || (channel.take() == null)) ? UNDEFINED : channel.take();
        }
        setCache(cache);
    }

    @Override
    public int getSize() {
        return channels.size();
    }

    String getUserName(int channel) throws InterruptedException {
        String ret = "";
        if (!isSimulated()) {
            try {
                ret = Epics.get(Scaler.this.channelName + ".NM" + channel, String.class);
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
            }
        }
        if (ret.trim().isEmpty()) {
            ret = Scaler.this.getName() + " channel " + channel;
        }
        return ret;
    }

    public enum CounterDirection {

        Up,
        Dn
    }

    public class ScalerChannel extends ChannelInteger {

        final int counterNumber;

        ScalerChannel(int couner_no) throws InterruptedException {
            super(getUserName(couner_no), Scaler.this.channelName + ".S" + couner_no);
            this.setAccessType(AccessType.Read);
            this.counterNumber = couner_no;
            this.setParent(Scaler.this);
            if (Scaler.this.isSimulated()) {
                setSimulated();
            }
        }

        @Override
        protected void onValueChange(Object value, Object former) {
            updateCache();
        }

        public CounterDirection getDirection() throws InterruptedException, IOException {
            try {
                return CounterDirection.valueOf(Epics.get(Scaler.this.channelName + ".D" + counterNumber, String.class));
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        public void setDirection(CounterDirection direction) throws InterruptedException, IOException {
            try {
                Epics.putq(Scaler.this.channelName + ".D" + counterNumber, direction.toString());
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        public boolean isPreset() throws InterruptedException, IOException {
            try {
                return Epics.get(Scaler.this.channelName + ".G" + counterNumber, String.class).equals("Y");
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        public void setPreset(boolean value) throws InterruptedException, IOException {
            try {
                Epics.putq(Scaler.this.channelName + ".G" + counterNumber, value ? "Y" : "N");
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        public int getPresetValue() throws InterruptedException, IOException {
            try {
                return Epics.get(Scaler.this.channelName + ".PR" + counterNumber, Integer.class);
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        public void setPresetValue(int value) throws InterruptedException, IOException {
            try {
                Epics.putq(Scaler.this.channelName + ".PR" + counterNumber, value);
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        //Exposing to outter class
        @Override
        protected void setCache(Object cache, Long timestamp, Long nanosOffset) {
            super.setCache(cache, timestamp, nanosOffset);
        }

    }
}
