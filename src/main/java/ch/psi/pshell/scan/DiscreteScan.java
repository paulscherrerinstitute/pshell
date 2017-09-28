package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import java.util.Arrays;

/**
 *
 */
public abstract class DiscreteScan extends ScanBase {

    static int[] toArray(int val, int size) {
        int[] ret = new int[size];
        Arrays.fill(ret, val);
        return ret;
    }

    public DiscreteScan(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, toArray(numberOfSteps, Math.max(writables.length, 1)), relative, latency, passes, zigzag);
    }

    public DiscreteScan(Writable writable, Readable[] readables, double start, double end, int numberOfSteps,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[]{writable}, readables, new double[]{start}, new double[]{end}, new int[]{numberOfSteps}, relative, latency, passes, zigzag);
    }

    public DiscreteScan(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[],
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, numberOfSteps, relative, latency, passes, zigzag);
    }

    public DiscreteScan(Writable writable, Readable[] readables, double start, double end, double stepSize,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[]{writable}, readables, new double[]{start}, new double[]{end}, new double[]{stepSize}, relative, latency, passes, zigzag);
    }

    public DiscreteScan(Writable[] writables, Readable[] readables, double[] start, double[] end, double stepSize[],
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, stepSize, relative, latency, passes, zigzag);
    }

}
