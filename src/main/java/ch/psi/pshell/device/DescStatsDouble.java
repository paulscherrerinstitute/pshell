package ch.psi.pshell.device;

import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Number extension holding an array of samples and providing statistical data on it. 
 * The numeric value is the mean of the samples.
 */
public final class DescStatsDouble extends Number implements Comparable<Double> {


    private  DescriptiveStatistics stats;
    
    private double[] samples;
    private int precision; 
    
    private Double mean;
    private Double sum;
    private Double variance;
    private Double stdev;
    private Double min;
    private Double max;

    public DescStatsDouble(double[] samples, int precision) {
        setSamples(samples, precision);
    }

    public DescStatsDouble(Double[] samples, int precision) {
        setSamples(samples, precision);
    }

    public DescStatsDouble(Number[] samples, int precision) {
        samples = Arr.removeNulls(samples);
        double[] aux = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            aux[i] = samples[i].doubleValue();
        }
        setSamples(aux, precision);
    }

    void setSamples(Double[] samples, int precision) {
        samples = Arr.removeNulls(samples);
        setSamples((double[]) Convert.toPrimitiveArray(samples), precision);
    }

    void setSamples(double[] samples, int precision) {
        this.samples = samples;
        this.precision = precision;
        mean = null;
        variance = null;
        stdev = null;
        min = null;
        max = null;

        if (samples.length == 0) {
            mean = Double.NaN;
            variance = Double.NaN;
            stdev = Double.NaN;
            min = Double.NaN;
            max = Double.NaN;
        }

        stats = new DescriptiveStatistics(samples);
    }

    public double[] getSamples() {
        return samples;
    }

    public double getSum() {
        if (sum==null){
            sum = stats.getSum();
            if (precision >= 0) {
                sum = Convert.roundDouble(sum, precision);
            }      
        }
        return sum;
    }
    
    public double getMean() {
        if (mean==null){
            mean = stats.getMean();
            if (precision >= 0) {
                mean = Convert.roundDouble(mean, precision);
            }      
        }        
        return mean;
    }

    public double getVariance() {
        if (variance==null){
            variance = stats.getVariance();
            if (precision >= 0) {
                variance = Convert.roundDouble(variance, precision);
            }      
        }        
        return variance;
    }

    public double getStdev() {
        if (stdev==null){
            stdev = stats.getStandardDeviation();
            if (precision >= 0) {
                stdev = Convert.roundDouble(stdev, precision);
            }      
        }        
        return stdev;
    }

    public double getMin() {
        if (min==null){
            min = stats.getMin();
            if (precision >= 0) {
                min = Convert.roundDouble(min, precision);
            }      
        }        
        return min;
    }

    public double getMax() {
        if (max==null){
            max = stats.getMax();
            if (precision >= 0) {
                max = Convert.roundDouble(max, precision);
            }      
        }        
        return max;
    }

    @Override
    public int intValue() {
        return (int) doubleValue();
    }

    @Override
    public long longValue() {
        return (long) doubleValue();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        return getMean();
    }

    @Override
    public String toString() {
        return Double.toString(getMean());
    }

    public String print() {
        return Double.toString(getMean()) + " \u03C3=" + Double.toString(stdev);
    }

    @Override
    public int compareTo(Double value) {
        return Double.compare(value, getMean());
    }

}
