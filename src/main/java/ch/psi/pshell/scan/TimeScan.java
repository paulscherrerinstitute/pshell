package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.utils.Chrono;
import java.io.IOException;

/**
 *
 */
public class TimeScan extends LineScan {

    final int interval_ms;

    public TimeScan(Readable[] readables, int points, int interval_ms) {
        super(new Writable[0], readables, new double[]{0.0}, new double[]{points - 1}, points - 1, false, 0, 1, false);
        this.interval_ms = interval_ms;
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        int steps = getNumberOfSteps()[0];
        Chrono chrono = new Chrono();
        int timeout = interval_ms;
        for (int i = 0; i <= steps; i++) {
            processPosition(new double[0]);
            if (i == steps) {
                break;
            }
            if (!chrono.waitTimeout(timeout)) {
                getResult().errorCode++;
            }
            timeout += interval_ms;
        }
    }
}
