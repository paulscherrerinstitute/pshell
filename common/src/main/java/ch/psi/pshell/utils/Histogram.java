package ch.psi.pshell.utils;

/**
 * Simple histogram calculation
 */
public class Histogram {
    
    final public double[] counts;
    final public double[] x;
    final public double min;
    final public double max;
    final public int bins;
    
    
    Histogram (double[] counts, double[] x, double min, double max, int bins){
        this.counts = counts;
        this.x = x;
        this.min = min;
        this.max = max;
        this.bins = bins;
    }
    
    @Override
    public String toString(){
        return Str.toString(this.counts, 100);
    }
    
    public static Histogram calc(double[] data, double min, double max, int bins) {
        if (data==null){
            return null;
        }
        if (Double.isNaN(min)){
            min = (Double) Arr.getMin(data);
        }
        if (Double.isNaN(max)){
            max = (Double) Arr.getMax(data);
        }
        if (bins <=0 ){
            bins = 100;
        }
        final double binSize = (max - min) / bins;
        
        final double[] y = new double[bins];
        final double[] x = new double[bins];
        for (int i=0;i<bins; i++){
            x[i] = min + binSize * i;
        }
        
        for (double d : data) {                                
            int bin = (int) ((d - min) / binSize);
            if ((bin >= 0) && (bin < bins)) {
                y[bin] += 1;
            }            
        }
        return new Histogram(y,x, min, max, bins);
    }
}
