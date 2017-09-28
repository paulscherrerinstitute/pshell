package ch.psi.pshell.scan;

import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Readable;
import ch.psi.utils.Convert;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 */
public class HillClimbingSearch extends Search {

    final boolean maximum;
    final int noiseFilteringSteps;
    final double[] initialStepSize;
    double currentValue;
    double[] currentPosition;
    double[] currentStepSize;
    int currentWritable;

    public HillClimbingSearch(Writable[] writables, Readable<Number> readable, double[] rangeMin, double[] rangeMax,
            double[] initialStepSize, double[] resolution, int noiseFilteringSteps, boolean maximum, boolean relative, int latency) {
        super(writables, new Readable[]{readable}, rangeMin, rangeMax, resolution, relative, latency, 1, false);
        this.maximum = maximum;
        this.noiseFilteringSteps = Math.max(noiseFilteringSteps, 1);
        this.initialStepSize = initialStepSize;
        currentPosition = new double[this.writables.length];
        currentStepSize = new double[this.writables.length];
    }

    ArrayList<double[]> getNeighborhood() {
        clearNeighborhood();
        for (int i = -noiseFilteringSteps; i <= noiseFilteringSteps; i++) {
            if (i != 0) {
                double[] pos = ArrayUtils.clone(currentPosition);
                pos[currentWritable] += (currentStepSize[currentWritable] * i);
                addNeighbor(pos);
            }
        }
        return super.getNeighbors();
    }

    double evaluate(double[] position) throws IOException, InterruptedException {
        List<ScanRecord> record = result.getRecords();
        for (int i = 0; i < record.size(); i++) {
            if (Arrays.equals(position, (double[]) Convert.toPrimitiveArray(record.get(i).positions, Double.class))) {
                return (Double) record.get(i).getValues()[0] * (maximum ? 1.0 : -1.0);
            }
        }
        processPosition(position);
        double value = (Double) currentRecord.values[0] * (maximum ? 1.0 : -1.0);
        return value;
    }

    @Override
    protected void moveToStart() throws IOException, InterruptedException {
        //Start from current position
        //start/end are the search limits/
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        {
            if (!relative) { //If relative it is done already
                readInitialPosition();
            }
            for (int i = 0; i < this.writables.length; i++) {
                //Always start in current position
                currentPosition[i] = initialPosition[i];
                currentStepSize[i] = initialStepSize[i];
            }
            currentValue = evaluate(currentPosition);

            boolean completed;
            do {
                completed = true;
                for (currentWritable = writables.length - 1; currentWritable >= 0; currentWritable--) {
                    for (double[] position : getNeighborhood()) {
                        double value = evaluate(position);
                        if (value > currentValue) {
                            completed = false;
                            currentValue = value;
                            currentPosition = position;
                        }
                    }
                }
                if (completed) {
                    for (int i = 0; i < currentStepSize.length; i++) {
                        currentStepSize[i] = currentStepSize[i] / 2;
                        if (currentStepSize[i] >= getStepSize()[i]) {
                            completed = false;
                        }
                        currentStepSize[i] = Math.max(currentStepSize[i], getStepSize()[i]);
                    }
                }
            } while (!completed);
            ((SearchResult) result).optimal = currentValue;
            ((SearchResult) result).optimalPosition = currentPosition;
            applyPosition(currentPosition);
        }
    }
}
