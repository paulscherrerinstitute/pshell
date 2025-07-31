package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class AreaScan extends DiscreteScan {

    public AreaScan(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[],
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, numberOfSteps, relative, latency, passes, zigzag);
    }

    public AreaScan(Writable[] writables, Readable[] readables, double[] start, double[] end, double stepSize[],
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, stepSize, relative, latency, passes, zigzag);
    }
    
    //This is just a hack for JEP interpreter have access to 2 prototypes
    public static class AreaScanStepSize extends AreaScan{
         public AreaScanStepSize(Writable[] writables, Readable[] readables, double[] start, double[] end, double stepSize[],
            boolean relative, int latency, int passes, boolean zigzag) {
            super(writables, readables, start, end, stepSize, relative, latency, passes, zigzag);
        }        
    }
    public static class AreaScanNumSteps extends AreaScan{
        public AreaScanNumSteps(Writable[] writables, Readable[] readables, double[] start, double[] end, int numberOfSteps[],
            boolean relative, int latency, int passes, boolean zigzag) {
            super(writables, readables, start, end, numberOfSteps, relative, latency, passes, zigzag);
        }        
    }    

    @Override
    protected void doScan() throws IOException, InterruptedException {
        double[] start = getStart();
        double[] end = getEnd();
        double[] current = getStart();
        int[] curStep = new int[start.length];
        int lastDimension = getDimensions() - 1;
        boolean[] returning = new boolean[current.length];

        boolean finished = false;
        while (!finished) {
            for (int i = 0; i <= getNumberOfSteps()[lastDimension]; i++) {
                current[lastDimension] = getWritablePosition(i, lastDimension);
                double[] pos = Arrays.copyOf(current, current.length);
                if (zigzag) {
                    for (int j = 0; j < pos.length; j++) {
                        if (returning[j]) {
                            pos[j] = end[j] - (pos[j] - start[j]);
                        }
                    }
                }
                processPosition(pos);
            }
            returning[lastDimension] = !returning[lastDimension];
            int index = lastDimension - 1;
            while (true) {
                if (index < 0) {
                    finished = true;
                    break;
                }
                if (curStep[index] < getNumberOfSteps()[index]) {
                    curStep[index]++;
                    current[index] = getWritablePosition(curStep[index], index);
                    break;
                }
                current[index] = start[index];
                curStep[index] = 0;
                returning[index] = !returning[index];
                index--;
            }
        }
    }
}
