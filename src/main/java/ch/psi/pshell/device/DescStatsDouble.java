package ch.psi.pshell.device;

import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Number extension holding an array of samples and providing statistical data on it. The numeric
 * value is the mean of the samples.
 */
public final class DescStatsDouble extends Number implements Comparable<Double> {

    double[] samples;
    double mean;
    double variance;
    double stdev;
    double min;
    double max;
    int precision;

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
        mean = Double.NaN;
        variance = Double.NaN;
        stdev = Double.NaN;
        min = Double.NaN;
        max = Double.NaN;

        if (samples.length == 0) {
            return;
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(samples);
        mean = stats.getMean();
        variance = stats.getVariance();
        stdev = stats.getStandardDeviation();
        min = stats.getMin();
        max = stats.getMax();
        if (precision >= 0) {
            mean = Convert.roundDouble(mean, precision);
            variance = Convert.roundDouble(variance, precision);
            stdev = Convert.roundDouble(stdev, precision);
            min = Convert.roundDouble(min, precision);
            max = Convert.roundDouble(max, precision);
        }
    }

    public double[] getSamples() {
        return samples;
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return variance;
    }

    public double getStdev() {
        return stdev;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
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
        return mean;
    }

    @Override
    public String toString() {
        return Double.toString(mean);
    }

    public String print() {
        return Double.toString(mean) + " \u03C3=" + Double.toString(stdev);
    }

    @Override
    public int compareTo(Double value) {
        return Double.compare(value, mean);
    }

}
