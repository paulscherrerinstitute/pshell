package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 */
public class RegionScan extends DiscreteScan {

    public RegionScan(Writable writable, Readable[] readables, double[] start, double[] end, double[] stepSize, boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[]{writable}, readables, start, end, stepSize, relative, latency, passes, zigzag);
    }

    public RegionScan(Writable writable, Readable[] readables, double[] start, double[] end, int[] steps, boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[]{writable}, readables, start, end, steps, relative, latency, passes, zigzag);
    }
    
    //This is just a hack for JEP interpreter have access to 2 prototypes
    public static class RegionScanNumSteps extends RegionScan{
        public RegionScanNumSteps(Writable writable, Readable[] readables, double[] start, double[] end, int[] steps, boolean relative, int latency, int passes, boolean zigzag) {
            super(writable, readables, start, end, steps, relative, latency, passes, zigzag);
        }    
    }    
    
    public static class RegionScanStepSize extends RegionScan{
        public RegionScanStepSize(Writable writable, Readable[] readables, double[] start, double[] end, double[] stepSize, boolean relative, int latency, int passes, boolean zigzag) {
            super(writable, readables, start, end, stepSize, relative, latency, passes, zigzag);
        }    
    }  
    
    @Override
    public int getNumberOfRecords() {
        int[] steps = getNumberOfSteps();
        int records = 0;
        for (int i = 0; i < steps.length; i++) {
            records += (steps[i] + 1);
        }
        return records * getNumberOfPasses();
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        if (isCurrentPassBackwards()) {
            for (int region = getNumberOfSteps().length - 1; region >= 0; region--) {
                onBeforeRegion(region);
                if (getCallbacks()!=null){
                    getCallbacks().onBeforeRegion(this, region);
                }
                for (int step = getNumberOfSteps()[region]; step >= 0; step--) {
                    double[] pos = new double[]{getWritablePosition(step, region)};
                    processPosition(pos);
                }
            }

        } else {
            for (int region = 0; region < getNumberOfSteps().length; region++) {
                if (getCallbacks()!=null){
                    getCallbacks().onBeforeRegion(this, region);
                }
                for (int step = 0; step <= getNumberOfSteps()[region]; step++) {
                    double[] pos = new double[]{getWritablePosition(step, region)};
                    processPosition(pos);
                }
            }
        }
    }

    public double[] getRange() {
        Double[] arr = (Double[]) Convert.toWrapperArray(start);
        arr = Arr.append(arr, (Double[]) Convert.toWrapperArray(end));

        return new double[]{Collections.min(Arrays.asList(arr)), Collections.max(Arrays.asList(arr))};
    }
    
    protected void onBeforeRegion(int region) {
    }
   

    @Override
    public int getDimensions() {
        return 1; //Writables move together;
    }
}
