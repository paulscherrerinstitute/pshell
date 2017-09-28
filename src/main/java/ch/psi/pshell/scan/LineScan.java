package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import java.io.IOException;

/**
 *
 */
public class LineScan extends DiscreteScan {

    public LineScan(Writable writable, Readable[] readables, double start, double end, int numberOfSteps,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writable, readables, start, end, numberOfSteps, relative, latency, passes, zigzag);
    }

    public LineScan(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, numberOfSteps, relative, latency, passes, zigzag);
    }

    public LineScan(Writable writable, Readable[] readables, double start, double end, double stepSize,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writable, readables, start, end, stepSize, relative, latency, passes, zigzag);
    }

    public LineScan(Writable[] writables, Readable[] readables, double[] start, double[] end, double stepSize[],
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, stepSize, relative, latency, passes, zigzag);
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        for (int i = 0; i <= getNumberOfSteps()[0]; i++) {
            double[] pos = getWritablesPositions(i);
            if (isCurrentPassBackwards()) {
                for (int j = 0; j < pos.length; j++) {
                    if (relative) {
                        pos[j] = end[j] - (pos[j] - initialPosition[j] - start[j]) + initialPosition[j];
                    } else {
                        pos[j] = end[j] - (pos[j] - start[j]);
                    }
                }
            }
            processPosition(pos);
        }
    }

    @Override
    public int getDimensions() {
        return 1; //Writables move together;
    }
}
