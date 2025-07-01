package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 */
public class BinarySearch extends Search {

    final boolean maximum;
    final Strategy strategy;

    double currentValue;
    double[] currentPosition;
    double[] currentStepSize;

    public enum Strategy {
        Normal,
        Boundary,
        FullNeighborhood
    }

    public BinarySearch(Writable[] writables, Readable<Number> readable, double[] start, double[] end, double[] resolution,
            boolean maximum, Strategy strategy, boolean relative, int latency) {
        super(writables, new Readable[]{readable}, start, end, resolution, relative, latency, 1, false);
        this.maximum = maximum;
        this.strategy = strategy;
        currentPosition = new double[this.writables.length];
        currentStepSize = new double[this.writables.length];
    }

    ArrayList<double[]> getNeighborhood() {
        clearNeighborhood();
        if (strategy == Strategy.FullNeighborhood) {
            double[] pos = ArrayUtils.clone(currentPosition);
            double[][] values = new double[writables.length][];
            int[] indexes = new int[writables.length];
            for (int i = 0; i < writables.length; i++) {
                values[i] = new double[]{pos[i] - currentStepSize[i], pos[i], pos[i] + currentStepSize[i]};
                indexes[i] = 0;
            }
            for (int i = 0; i < (Math.pow(3, writables.length)); i++) {
                double[] v = new double[writables.length];
                for (int j = 0; j < writables.length; j++) {
                    v[j] = values[j][indexes[j]];
                }
                for (int j = 0; j < writables.length; j++) {
                    indexes[j]++;
                    if (indexes[j] < 3) {
                        break;
                    }
                    indexes[j] = 0;
                }
                if (!Arrays.equals(pos, v)) {
                    addNeighbor(v);
                }
            }
        } else {
            for (int i = 0; i < writables.length; i++) {
                double[] pos = ArrayUtils.clone(currentPosition);
                pos[i] -= currentStepSize[i];
                addNeighbor(pos);

                pos = ArrayUtils.clone(currentPosition);
                pos[i] += currentStepSize[i];
                addNeighbor(pos);
            }
        }
        return super.getNeighbors();
    }

    double evaluate(double[] position) throws IOException, InterruptedException {
        if (strategy == Strategy.Normal) {
            List<ScanRecord> record = result.getRecords();
            for (int i = 0; i < record.size(); i++) {
                if (Arrays.equals(position, (double[]) Convert.toPrimitiveArray(record.get(i).positions, Double.class))) {
                    return (Double) record.get(i).getReadables()[0] * (maximum ? 1.0 : -1.0);
                }
            }
        }
        processPosition(position);
        double value = (Double) currentRecord.values[0] * (maximum ? 1.0 : -1.0);
        return value;
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        {
            for (int i = 0; i < this.writables.length; i++) {
                currentPosition[i] = (getStart()[i] + getEnd()[i]) / 2; //Adjust relative
                if (strategy == Strategy.Boundary) {
                    currentStepSize[i] = Math.abs(end[i] - start[i]) / 2;
                } else {
                    currentStepSize[i] = Math.abs(end[i] - start[i]) / 4;
                }
            }
            currentValue = evaluate(currentPosition);
            boolean completed;
            do {
                double former = currentValue;
                for (double[] position : getNeighborhood()) {
                    double value = evaluate(position);
                    if (value > currentValue) {
                        currentValue = value;
                        currentPosition = position;
                    }
                }
                completed = true;
                for (int i = 0; i < writables.length; i++) {
                    if (currentStepSize[i] > stepSize[i]) {
                        completed = false;
                    }
                    if ((strategy == Strategy.Normal) && (former != currentValue)) {
                        completed = false;
                    } else {
                        currentStepSize[i] = currentStepSize[i] / 2;
                    }
                    if (currentStepSize[i] < stepSize[i]) {
                        currentStepSize[i] = stepSize[i];
                    }
                }
            } while (!completed);
            ((SearchResult) result).optimal = currentValue;
            ((SearchResult) result).optimalPosition = currentPosition;
            applyPosition(currentPosition);
        }
    }
}
