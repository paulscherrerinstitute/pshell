package ch.psi.pshell.epics;

import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.ControlledVariableBase;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ProcessVariableConfig;
import ch.psi.pshell.device.ReadonlyRegister;
import java.io.IOException;

/**
 * ControlledVariable implementation, constructed with setpoint and readback channel names.
 * Optionally a readback device may be supplied.
 */
public class ControlledVariable extends ControlledVariableBase {

    final String channelName;
    final ChannelDouble channel;
    final ReadonlyRegister<Double> readbackChannel;

    public ControlledVariable(String name, String channelName, String readbackChannelName) {
        this(name, channelName, readbackChannelName, null);
    }

    public ControlledVariable(String name, String channelName, String readbackChannelName, ReadonlyRegister<Double> readbackChannel) {
        super(name, new ProcessVariableConfig());
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
        if (child == channel) {
            setCache(value);
        }
    }

    class ReadbackChannel extends ChannelDouble {

        ReadbackChannel(String name, String channelName) {
            super(name, channelName, ControlledVariable.this.getPrecision());
            setParent(ControlledVariable.this);
            setAccessType(AccessType.Read);
        }

        @Override
        protected Double convertFromRead(Double value) {
            return ControlledVariable.this.convertFromRead(value);
        }
    }

}
