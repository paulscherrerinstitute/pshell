package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.IntegerType;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.utils.Convert;
import ch.psi.utils.Histogram;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Device providing calculation of a histogram of a source device.
 */
public class HistogramGenerator extends ReadonlyRegisterBase<Histogram> implements ReadonlyRegisterArray<Histogram>, IntegerType {
    
    public static int DEFAULT_NUMBER_OF_SAMPLES = 100;
    public static int DEFAULT_NUMBER_OF_BINS = 100;
    
    public static class HistogramDeviceConfig extends RegisterConfig{
        public int numberOfSamples;
        public double min = Double.NaN;
        public double max = Double.NaN;
        public int bins;
    }

    final ReadonlyRegisterBase source;
    final HistogramDeviceConfig volatileConfig;
    final List<Double> samples = new ArrayList<>();
    volatile Object currentSample;
    volatile int sampleCount;
    DeviceListener sourceListener;

    Histogram histogram;
    
    @Override
    public HistogramDeviceConfig getConfig(){
        if (volatileConfig != null) {
            return volatileConfig;
        }        
        return (HistogramDeviceConfig) super.getConfig();
    }
    
    public HistogramGenerator(String name, ReadonlyRegisterBase source) {
        super(name, new HistogramDeviceConfig());
        this.source = source;
        setParent(source);
        volatileConfig = null;
    }

    public HistogramGenerator(String name, ReadonlyRegisterBase source, int numberOfSamples, double min, double max, int bins) {
        super(name);
        this.source = source;
        setParent(source);        
        volatileConfig = new HistogramDeviceConfig();        
        volatileConfig.numberOfSamples = numberOfSamples;
        volatileConfig.min = min;
        volatileConfig.max = max;
        volatileConfig.bins = bins;
    }



    public ReadonlyRegisterBase getSource() {
        return (ReadonlyRegisterBase) getParent();
    }

    public double[] getX() {
        if (histogram==null){
            return new double[0];
        }
        return histogram.x;
    }

    public double getMin() {
        return getConfig().min;        
    }

    public Double getMax() {
        return getConfig().max;  
    }
    
    public double[] getRange(){
         double min =  getMin();
         double max =  getMax();
   
        if (Double.isNaN(min) || Double.isNaN(max)){
            return null;
        }
        return new double[]{min,max};
    }

    public int getBins() {
        int bins =  getConfig().bins;        
        return (bins <= 0) ? DEFAULT_NUMBER_OF_BINS : bins;
    }
    
    public int getNumberOfSamples() {
        int numberOfSamples =getConfig().numberOfSamples;        
        if (numberOfSamples<=0){
            if (source instanceof ReadonlyRegisterArray){
                return 1;
            }
            return DEFAULT_NUMBER_OF_SAMPLES;
        }
        return numberOfSamples;
    }    
       

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        source.removeListener(sourceListener);
        super.doInitialize();

        sourceListener = new DeviceAdapter() {
            @Override
            public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                addSample(value);
            }
        };
        source.addListener(sourceListener);
    }

    void readSample() {
        if (this.isInitialized()) {
            try {
                addSample(source.read());
            } catch (Exception ex) {
                Logger.getLogger(HistogramGenerator.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    void addSample(Object sample) {
        if (!this.isInitialized()) {
            return;
        }
        currentSample = sample;
        sampleCount++;
        synchronized (samples) {
            try {
                if (sample instanceof Number) {
                    samples.add(((Number) sample).doubleValue());
                    while (samples.size() > getNumberOfSamples()) {
                        samples.remove(0);
                    }
                } 
                if (isMonitored()){
                    calculate();
                }
            } catch (Exception ex) {
                Logger.getLogger(HistogramGenerator.class.getName()).log(Level.WARNING, null, ex);
            }
        }

    }
        
    volatile int lastSampleCount;
    void calculate(){        
        if (sampleCount==lastSampleCount){
            return;
        }
        lastSampleCount = sampleCount;
        Object data;
        if (currentSample instanceof Number) {
            data = samples;
        } else if (currentSample.getClass().isArray()) {
            data = currentSample;
        } else {
            return;
        }
        setCache( Histogram.calc((double[]) Convert.toPrimitiveArray(data, Double.class), getMin(), getMax(), getBins()));
    }

    @Override
    protected Histogram doRead() throws IOException, InterruptedException {
        calculate();
        return take();
    }

    @Override
    public int getSize() {
        if (histogram==null){
            return 0;
        }
        return histogram.bins;
    }
    
    @Override
    protected void doClose() throws IOException {
        source.removeListener(sourceListener);
        super.doClose();
    }    
}
