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
    final boolean fixedRate;

    public TimeScan(Readable[] readables, int points, int interval_ms) {
        this(readables, points, interval_ms, 1);
    }

    public TimeScan(Readable[] readables, int points, int interval_ms, boolean fixedRate) {
        this(readables, points, interval_ms, 1, fixedRate);
    }

    public TimeScan(Readable[] readables, int points, int interval_ms, int passes) {
        this(readables, points, interval_ms, passes, true);
    }

    public TimeScan(Readable[] readables, int points, int interval_ms, int passes, boolean fixedRate) {
        super(new Writable[0], readables, new double[]{0.0}, 
                (points<0) ? new double[]{100.0} : new double[]{points - 1}, 
                (points<0) ? Integer.MAX_VALUE : points - 1, 
                false, 0, passes, false);
        this.interval_ms = interval_ms;
        this.fixedRate = fixedRate;
        setResampleOnInvalidate(false);
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        int steps = getNumberOfSteps()[0];
        Chrono chrono = new Chrono();
        int timeout = interval_ms;
        for (int i = 0; (i <= steps) || (steps==Integer.MAX_VALUE); i++) {
            if (!fixedRate) {
                chrono = new Chrono();
            }
            processPosition(new double[0]);
            if (i == steps) {
                break;
            }
            if (fixedRate) {
                if (!chrono.waitTimeout(timeout)) {
                    getResult().errorCode++;
                }
                timeout += interval_ms;
            } else {
                chrono.waitTimeout(interval_ms);
            }
        }
    }
}
