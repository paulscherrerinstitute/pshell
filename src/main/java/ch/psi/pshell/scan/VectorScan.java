package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import java.io.IOException;

/**
 *
 */
public class VectorScan extends DiscreteScan {

    final double[][] vector;
    boolean lineScan;

    public VectorScan(Writable[] writables, Readable[] readables, double[][] table, boolean lineScan,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, table[0], table[table.length - 1], table.length, relative, latency, passes, zigzag);
        this.vector = table;
        this.lineScan = lineScan;
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        for (int i = 0; i < vector.length; i++) {
            processPosition(vector[isCurrentPassBackwards() ? (vector.length - 1 - i) : i]);
        }
    }

    @Override
    public int getNumberOfRecords() {
        int[] steps = getNumberOfSteps();
        if (steps.length == 0) {
            return 0;
        }
        return (steps[0] + 1) * getNumberOfPasses();
    }

    @Override
    public int getDimensions() {
        if (lineScan) {
            return 1; //Writables move together;
        } else {
            return super.getDimensions();
        }
    }
}
