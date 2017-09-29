package ch.psi.pshell.epics;

import java.io.IOException;
import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.PositionerBase;
import ch.psi.pshell.device.PositionerConfig;
import ch.psi.pshell.device.ReadonlyRegister;

/**
 * Positioner implementation, constructed with setpoint and readback channel names. Optionally a
 * readback device may be supplied.
 *
 * If using rotation positioner, and a modulo readback is desired, it can be implemented in specific
 * classes and passed as pamarameter to Positioner constructor:
 *
 * public class RotationReadback extends ChannelDouble { public RotationReadback(String name, String
 * channelName) { super(name, channelName); setAccessType(AccessType.Read); }
 *
 * @Override protected Double convertFromRead(Double value) { return Convert.toDegrees0To360(value);
 * } }
 */
public class Positioner extends PositionerBase {

    final String channelName;
    final ChannelDouble channel;
    final ReadonlyRegister<Double> readbackChannel;

    public Positioner(String name, String channelName, String readbackChannelName) {
        this(name, channelName, readbackChannelName, null);
    }

    public Positioner(String name, String channelName, String readbackChannelName, ReadonlyRegister<Double> readbackChannel) {
        super(name, new PositionerConfig());
        this.channelName = channelName;
        channel = new ChannelDouble(name + " channel", channelName, getConfig().precision);
        this.readbackChannel = (readbackChannel != null) ? readbackChannel : new ReadbackChannel(name + " readback channel", readbackChannelName);
        setChildren(new Device[]{channel, this.readbackChannel});
        setTrackChildren(true);
        setReadback(this.readbackChannel);
    }

    public String getChannelName() {
        return channelName;
    }

    public ChannelDouble getSetpoint() {
        return channel;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();

        //If units not set assumes it is first execution and uploads config from motor record
        if (!getConfig().hasDefinedUnit()) {
            uploadConfig();
        }
    }

    public void uploadConfig() throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        ProcessVariable.uploadProcessVariableConfig(getConfig(), channelName, getLogger());
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        return channel.read();
    }

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        channel.write(value);
    }

    @Override
    protected void onChildValueChange(Device child, Object value, Object former) {
        if ((child == channel) && (!isSimulated())) {
            setCache(value);
        }
    }

    class ReadbackChannel extends ChannelDouble {

        ReadbackChannel(String name, String channelName) {
            super(name, channelName, Positioner.this.getPrecision());
            setParent(Positioner.this);
            setAccessType(AccessType.Read);
        }

        @Override
        protected Double convertFromRead(Double value) {
            return Positioner.this.convertFromRead(value);
        }
    }
}
