package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DiscretePositionerBase;
import ch.psi.pshell.device.RegisterConfig;
import ch.psi.utils.Config;
import ch.psi.utils.Reflection.Hidden;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Wraps EPICS mbbi and mbbo records.
 */
public class DiscretePositioner extends DiscretePositionerBase {

    final ChannelString setpoint;
    final ChannelString readback;
    final ChannelInteger stop;

    public static class DiscretePositionerConfig extends RegisterConfig {

        public String[] positions;
        public String setpoint_pv;
        public String readback_pv;
        public String stop_pv;
    }

    @Override
    public DiscretePositionerConfig getConfig() {
        return (DiscretePositionerConfig) super.getConfig();
    }

    public ChannelString getSetpoint() {
        return setpoint;
    }

    public DiscretePositioner(String name) {
        super(name, new DiscretePositionerConfig());
        setPositions(getConfig().positions);
        setpoint = new ChannelString(name + " setpoint", getConfig().setpoint_pv, false);
        readback = Config.isStringDefined(getConfig().readback_pv)
                ? newReadbackChannel(getConfig().readback_pv)
                : setpoint;
        stop = Config.isStringDefined(getConfig().stop_pv)
                ? new ChannelInteger(name + "stop", getConfig().stop_pv, false)
                : null;
        setChildren(new Device[]{setpoint, readback, stop});
        setReadback(readback);
    }

    /**
     * For no-config constructor assume channel is mbbi and reads positions from ZRST, ONST ...
     */
    public DiscretePositioner(String name, String channelSetpoint) {
        this(name, channelSetpoint, null);
    }

    /**
     * For no-config constructor assume channel is mbbi and reads positions from ZRST, ONST ...
     */
    public DiscretePositioner(String name, String channelSetpoint, String channelReadback) {
        this(name, channelSetpoint, channelReadback, null, new String[0]);
    }

    protected DiscretePositioner(String name, String channelSetpoint, String channelReadback, String channelStop, String... positions) {
        super(name, positions);
        setpoint = new ChannelString(name + " setpoint", channelSetpoint, false);
        readback = (channelReadback != null)
                ? newReadbackChannel(channelReadback)
                : setpoint;
        stop = (channelStop != null)
                ? new ChannelInteger(name + "stop", getConfig().stop_pv, false)
                : null;
        setChildren(new Device[]{setpoint, readback, stop});
        setReadback(readback);
    }

    protected ChannelString newReadbackChannel(String channelName) {
        return new ChannelString(getName() + " readback", channelName) {
            @Hidden
            @Override
            public Number takeAsNumber() {
                int index = getIndex(take());
                return (index >= 0) ? index : null;
            }
        };
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        String[] pos = getPositions();
        if ((pos == null) || ((pos.length) == 0) || (pos[0] == null) || (pos[0].isEmpty())) {
            //If undefined try to read from a mbbi record.            
            try {
                ArrayList<String> positions = new ArrayList<>();
                for (String str : new String[]{"ZRST", "ONST", "TWST", "THST", "FRST", "FVST", "SXST", "SVST", "EIST", "NIST", "TEST", "ELST", "TVST", "TTST", "FTST", "FFST"}) {
                    String p = Epics.get(setpoint.getChannelName() + "." + str, String.class);
                    if (p.trim().isEmpty()) {
                        break;
                    }
                    positions.add(p);
                }
                setPositions(positions.toArray(new String[0]));
            } catch (ChannelException | java.util.concurrent.TimeoutException | ExecutionException ex) {
            }
        }
        super.doInitialize();
    }

    @Override
    protected String getSetpointValue() {
        return setpoint.take();
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        if (stop == null) {
            throw new StopNotConfiguredException();
        }
        stop.write(1);
    }

    @Override
    protected String doRead() throws IOException, InterruptedException {
        return setpoint.read();
    }

    @Override
    protected void doWrite(String value) throws IOException, InterruptedException {
        String[] positions = getPositions();
        for (int i = 0; i < positions.length; i++) {
            if (value.equals(positions[i].trim())) {
                setpoint.write(positions[i]);
                return;
            }
        }
    }

}
