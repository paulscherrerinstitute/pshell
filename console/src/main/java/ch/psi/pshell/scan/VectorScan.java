package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class VectorScan extends DiscreteScan {

    final double[][] vector;
    Iterator iterator;
    boolean lineScan;

    static double[] getMin(double[][] table ){
        double[] ret = new double[table[0].length];
        for (int j=0; j< table[0].length; j++){
            ret[j]=table[0][j];
        }
        for (int i=1; i< table.length; i++){
            for (int j=0; j< table[0].length; j++){
                if (table[i][j] < ret[j]){
                    ret[j] = table[i][j];
                }
            }
        }
        return ret;
    }
    
    static double[] getMax(double[][] table ){
        double[] ret = new double[table[0].length];
        for (int j=0; j< table[0].length; j++){
            ret[j]=table[0][j];
        }
        for (int i=1; i< table.length; i++){
            for (int j=0; j< table[0].length; j++){
                if (table[i][j] > ret[j]){
                    ret[j] = table[i][j];
                }
            }
        }
        return ret;
    }    
    public VectorScan(Writable[] writables, Readable[] readables, double[][] table, boolean lineScan,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(writables, readables, getMin(table), getMax(table), ((writables.length == 2) && !lineScan) ? -1: table.length, relative, latency, passes, zigzag);
        this.vector = table;
        this.iterator = null;
        this.lineScan = lineScan;
    }

    
    public VectorScan(Writable[] writables, Readable[] readables, Iterator iterator, boolean lineScan,
            boolean relative, int latency) {
        super(writables, readables, new double[writables.length], new double[writables.length], new int[]{-1}, relative, latency, 1, false);
        this.vector = null;
        this.iterator = iterator;
        this.lineScan = lineScan;
    }
    
    @Override
    protected void doScan() throws IOException, InterruptedException {
        if (vector!=null) {
            for (int i = 0; i < vector.length; i++) {    
                double[] pos = vector[isCurrentPassBackwards() ? (vector.length - 1 - i) : i];
                if (relative) {
                    pos = relativeToAbsolute(pos);
                }                
                processPosition(pos);
                        
            }
        } else {
           while (iterator.hasNext()){
                Object pos = iterator.next();
                if (pos instanceof List){
                    pos = Convert.toDouble(pos);
                } else if (pos instanceof Number number){
                    pos = new double[]{number.doubleValue()};
                }
                if (relative) {
                    pos = relativeToAbsolute((double[])pos);
                }                   
                processPosition((double[])pos);
            }
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
    
    @Override
    public boolean getInitialMove(){
        return false;
    }    
    
}
