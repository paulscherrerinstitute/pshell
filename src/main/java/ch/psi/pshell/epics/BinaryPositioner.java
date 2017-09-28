package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Wraps EPICS bi and bo records.
 */
public class BinaryPositioner extends DiscretePositioner {

    /**
     * For constructor with no positions assume channel is bi and reads positions from ZNAM and ONAM
     */
    public BinaryPositioner(String name, String channel) {
        this(name, channel, null);
    }

    /**
     * For constructor with no positions assume channel is bi and reads positions from ZNAM and ONAM
     */
    public BinaryPositioner(String name, String channel, String readback) {
        this(name, "", "", channel, readback);
    }

    public BinaryPositioner(String name, String position1, String position2, String channel) {
        this(name, position1, position2, channel, null);
    }

    public BinaryPositioner(String name, String position1, String position2, String channelSetpoint, String channelReadback) {
        this(name, position1, position2, channelSetpoint, channelReadback, null);
    }

    public BinaryPositioner(String name, String position1, String position2, String channelSetpoint, String channelReadback, String channelStop) {
        super(name, channelSetpoint, channelReadback, channelStop, position1, position2);
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        String[] pos = getPositions();
        if ((pos == null) || ((pos.length) != 2) || (pos[0] == null) || (pos[0].isEmpty())) {
            //If undefined try to read from a binary output channel.            
            try {
                String position1 = Epics.get(setpoint.getChannelName() + ".ZNAM", String.class);
                String position2 = Epics.get(setpoint.getChannelName() + ".ONAM", String.class);
                setPositions(position1, position2);
            } catch (ChannelException | java.util.concurrent.TimeoutException | ExecutionException ex) {
                throw new DeviceException(ex);
            }
        }

        super.doInitialize();
    }
}
