package ch.psi.pshell.epics;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DiscretePositionerBase;
import ch.psi.pshell.device.RegisterConfig;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Reflection.Hidden;
import java.io.IOException;

/**
 * DiscretePositioner having each position selected by an individual channel (integer) and a single
 * readback (string).
 */
public class Manipulator extends DiscretePositionerBase {

    final ChannelString readback;
    String setpoint;

    public static class ManipulatorConfig extends RegisterConfig {

        public String[] positions;
        public String[] position_pvs;
        public String readback_pv;
        public String stop_pv;
    }

    @Override
    public ManipulatorConfig getConfig() {
        return (ManipulatorConfig) super.getConfig();
    }

    public Manipulator(String name) {
        super(name, new ManipulatorConfig());
        setPositions(getConfig().positions);
        readback = new ChannelString(name + " readback", getConfig().readback_pv) {
            @Hidden
            @Override
            public Number takeAsNumber() {
                int index = getIndex(take());
                return (index >= 0) ? index : null;
            }

        };
        setChildren(new Device[]{readback});
        this.setReadback(readback);
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        if (!Config.isStringDefined(getConfig().stop_pv)) {
            throw new StopNotConfiguredException();
        }
        try (ChannelInteger channel = new ChannelInteger(getName() + " stop", getConfig().stop_pv + ".PROC", false)) {
            channel.initialize();
            channel.write(1);
        }
    }

    @Override
    protected String doRead() throws IOException, InterruptedException {
        return take();
    }

    @Override
    protected void doWrite(String value) throws IOException, InterruptedException {
        for (int i = 0; i < getConfig().positions.length; i++) {
            if (value.equals(getConfig().positions[i].trim())) {
                if (i < getConfig().position_pvs.length) {
                    try (ChannelInteger channel = new ChannelInteger(getName() + " " + getConfig().positions[i], getConfig().position_pvs[i].trim() + ".PROC", false)) {
                        channel.initialize();
                        channel.write(1);
                        setpoint = value;
                    }
                }
                return;
            }
        }

    }

}
