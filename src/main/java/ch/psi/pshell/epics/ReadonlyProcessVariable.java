package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadonlyProcessVariableBase;
import ch.psi.pshell.device.ReadonlyProcessVariableConfig;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReadonlyProcessVariable implementation, constructed with channel name.
 */
public class ReadonlyProcessVariable extends ReadonlyProcessVariableBase {

    final String channelName;
    final ChannelDouble channel;

    public ReadonlyProcessVariable(String name, String channelName) {
        this(name, channelName, true);
    }
    
    public ReadonlyProcessVariable(String name, String channelName, boolean timestamped) {
        this(name, channelName, timestamped, timestamped ? Epics.getDefaultInvalidValueAction() : null);
    }
    
    public ReadonlyProcessVariable(String name, String channelName, boolean timestamped, InvalidValueAction invalidValueAction) {
        super(name, new ReadonlyProcessVariableConfig());
        this.channelName = channelName;
        channel = new ChannelDouble(name + " channel", channelName, UNDEFINED_PRECISION, timestamped, invalidValueAction);
        setChildren(new Device[]{channel});
        setTrackChildren(true);
    }

    public String getChannelName() {
        return channelName;
    }

    public ChannelDouble getChannel() {
        return channel;
    }
    
    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();

        //If units not set assumes it is first execution and uploads config from motor record
        if (getConfig().isUndefined()) {
            uploadConfig();
        }
    }

    static void uploadProcessVariableConfig(ReadonlyProcessVariableConfig cfg, String channelName, Logger logger) throws IOException, InterruptedException {
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
        try {
            cfg.description = (String) Epics.get(channelName + ".DESC", String.class);
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
    protected void onChildValueChange(Device child, Object value, Object former) {
        if ((child == channel) && (!isSimulated())){
            setCache(convertFromRead((Double) value));
        }
    }

}
