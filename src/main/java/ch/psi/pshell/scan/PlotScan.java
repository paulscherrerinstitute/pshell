package ch.psi.pshell.scan;

import ch.psi.pshell.device.Writable;
import ch.psi.utils.Arr;
import java.io.IOException;

/**
 * This is just a workaround: to be replaced by a specific plot functionality for scripts
 */
public class PlotScan extends DiscreteScan {

    static class ArrayReader implements ch.psi.pshell.device.Readable {

        int size;
        int counter;
        Object array;

        ArrayReader(Object array) {
            size = java.lang.reflect.Array.getLength(array);
            this.array = array;
            counter = 0;
        }

        public String getName() {
            return null;
        }

        @Override
        public Object read() throws IOException, InterruptedException {
            if (counter < size) {
                return java.lang.reflect.Array.get(array, counter++);
            }
            return null;
        }
    }

    static class ArrayofArraysReader implements ch.psi.pshell.device.Readable.ReadableArray {

        int[] dimensions;
        int counter;
        Object array;

        ArrayofArraysReader(Object array) {
            this.array = array;
            dimensions = Arr.getDimensions(array);
            counter = 0;
        }

        public String getName() {
            return "";
        }

        @Override
        public Object read() throws IOException, InterruptedException {
            if (counter < dimensions[0]) {
                return java.lang.reflect.Array.get(array, counter++);
            }
            return null;
        }

        @Override
        public int getSize() {
            return dimensions[1];
        }
    }

    Object data;
    int rank;
    int size;
    int[] dimensions;

    public PlotScan(Object data) {
        super(new Writable[0],
                Arr.getRank(data) == 2
                ? new ch.psi.pshell.device.Readable[]{new ArrayofArraysReader(data)}
                : new ch.psi.pshell.device.Readable[]{new ArrayReader(data)},
                new double[]{0},
                new double[]{java.lang.reflect.Array.getLength(data)},
                java.lang.reflect.Array.getLength(data) - 0, false, 0, 1, false);
        this.size = java.lang.reflect.Array.getLength(data);
        this.data = data;
        rank = Arr.getRank(data);
        dimensions = Arr.getDimensions(data);
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        int steps = Math.min(size, getNumberOfSteps()[0]);
        for (int i = 0; i < steps; i++) {
            ScanRecord record = new ScanRecord();
            record.dimensions = rank;
            record.setpoints = new Number[1];
            record.positions = new Number[1];
            record.values = new Object[1];
            record.index = i;
            record.pass = 1;
            record.setpoints[0] = i;
            record.positions[0] = i;
            record.values[0] = java.lang.reflect.Array.get(data, i);
            for (ScanListener listener : getListeners()) {
                listener.onNewRecord(this, record);
            }
        }
    }

    @Override
    public int getDimensions() {
        return 1;
    }
}
