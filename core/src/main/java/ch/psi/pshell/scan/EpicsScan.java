package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.*;
import java.io.IOException;

/**
 * Control execution of a EPCIS scan record.
 */
public class EpicsScan extends ScanBase {

    static int[] getNumberSteps(String name) throws IOException, InterruptedException {
        ChannelInteger c = new ChannelInteger(null, name + ".NPTS");
        c.initialize();
        return new int[]{c.read()};
    }

    static double[] getStart(String name) throws IOException, InterruptedException {
        ChannelDouble c = new ChannelDouble(null, name + ".P1SP");
        c.initialize();
        return new double[]{c.read()};
    }

    static double[] getEnd(String name) throws IOException, InterruptedException {
        ChannelDouble c = new ChannelDouble(null, name + ".P1EP");
        c.initialize();
        return new double[]{c.read()};
    }

    static Writable[] getWritables(String name) throws IOException, InterruptedException {
        ChannelString c = new ChannelString(null, name + ".P1PV");
        c.initialize();
        ChannelDouble w = new ChannelDouble(null, c.read());
        w.initialize();
        return new Writable[]{w};
    }

    static Readable[] getReadables(String name) throws IOException, InterruptedException {
        ChannelString c = new ChannelString(null, name + ".D01PV");
        c.initialize();
        ChannelDouble w = new ChannelDouble(null, c.read());
        w.initialize();
        return new Readable[]{w};
    }

    final String name;

    public EpicsScan(String name) throws IOException, InterruptedException {
        super(getWritables(name), getReadables(name), getStart(name), getEnd(name), getNumberSteps(name), false, 0, 1, false);
        this.name = name;
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        try (ChannelInteger start = new ChannelInteger(null, name + ".EXSC")) {
            start.setMonitored(true);
            start.write(1);
            start.read();
            try {
                while (start.take(1000) == 1) {         //Returns within 1ms but polls evert second just in case.
                    Thread.sleep(1);
                }
            } catch (InterruptedException ex) {
                start.write(0);
                throw ex;
            }
        }
    }

}
