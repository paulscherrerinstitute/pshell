package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ProcessVariableBase;
import ch.psi.pshell.device.ProcessVariableConfig;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProcessVariable implementation, constructed with channel name.
 */
public class ProcessVariable extends ProcessVariableBase {

    final String channelName;
    final ChannelDouble channel;

    public ProcessVariable(String name, String channelName) {
        super(name, new ProcessVariableConfig());
        this.channelName = channelName;
        channel = new ChannelDouble(name + " channel", channelName);
        setChildren(new Device[]{channel});
        setTrackChildren(true);
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

    static void uploadProcessVariableConfig(ProcessVariableConfig cfg, String channelName, Logger logger) throws IOException, InterruptedException {
        try {
            cfg.minValue = (Double) Epics.get(channelName + ".DRVL", Double.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            logger.log(Level.WARNING, null, ex);
        }
        try {
            cfg.maxValue = (Double) Epics.get(channelName + ".DRVH", Double.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            logger.log(Level.WARNING, null, ex);
        }
        try {
            cfg.precision = (Integer) Epics.get(channelName + ".PREC", Integer.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            logger.log(Level.WARNING, null, ex);
        }
        try {
            cfg.unit = (String) Epics.get(channelName + ".EGU", String.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            logger.log(Level.WARNING, null, ex);
        }
        cfg.save();
    }

    public void uploadConfig() throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        uploadProcessVariableConfig(getConfig(), channelName, getLogger());
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
        setCache(convertFromRead((Double) value));
    }

}
