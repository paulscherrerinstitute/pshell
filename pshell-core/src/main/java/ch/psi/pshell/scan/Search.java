package ch.psi.pshell.scan;

import ch.psi.pshell.device.Writable;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 */
public abstract class Search extends DiscreteScan {

    //For search, step size is the resolution of the scan
    public Search(Writable[] writables, ch.psi.pshell.device.Readable[] readables, double[] start, double[] end, double[] resolution,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, start, end, resolution, relative, latency, passes, zigzag);
        clearNeighborhood();
    }

    @Override
    protected ScanResult newResult() {
        return new SearchResult(this);
    }

    //Neighborhood list management
    private ArrayList<double[]> neighborgood;

    protected void clearNeighborhood() {
        neighborgood = new ArrayList<double[]>();
    }

    protected void addNeighbor(double[] pos) {
        for (int i = 0; i < this.getWritables().length; i++) {
            if (pos[i] < (getStart()[i] - getStepSize()[i])) {
                return;
            }
            if (pos[i] > (getEnd()[i] + getStepSize()[i])) {
                return;
            }
            pos[i] = Math.max(pos[i], getStart()[i]);
            pos[i] = Math.min(pos[i], getEnd()[i]);
        }
        for (int i = 0; i < neighborgood.size(); i++) {
            double[] item = neighborgood.get(i);
            if (Arrays.equals(item, pos)) {
                return;
            }
        }
        neighborgood.add(ArrayUtils.clone(pos));
    }

    protected ArrayList<double[]> getNeighbors() {
        return neighborgood;
    }

}
