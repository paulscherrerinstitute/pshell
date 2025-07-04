package ch.psi.pshell.device;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.DoubleType;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import ch.psi.pshell.devices.InlineDevice;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Threading;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs averaging on Register devices or Readable interfaces, havinf a DescStatsDouble object as
 * value, which provide mean, variance and sigma of the valuies. Configuration (measures, interval)
 * can be provided in constructor or config properties file. If interval is less than 0, sample is
 * updated on data change event. If the Averager is not monitored, read() is blocking, sampling the
 * source 'measures' times. Otherwise, read is not blocking, returning the 'measures' last samples
 * average. A permanent update timer will be created if averager is monitored and interval>=0.
 */
public class Averager extends ReadonlyRegisterBase<DescStatsDouble> implements ReadonlyRegisterNumber<DescStatsDouble>, DoubleType {

    AveragerConfig config;
    final Readable source;
    Device innerDevice;

    /**
     * Entity class holding the configuration of an Averager device.
     */
    public static class AveragerConfig extends RegisterConfig {

        public int measures;
        public int interval;    //If <=0 then is based on change events
    }

    
    
    public Averager(String name, Readable source) {
        super(name, new AveragerConfig());
        
        this.source = resolveSource(source);
        if (source instanceof DeviceBase deviceBase) {
            setParent(deviceBase);
        }
    }

    public Averager(String name, Readable source, int measures) {
        this(name, source, measures, UNDEFINED);
    }

    public Averager(String name, Readable source, int measures, int interval) {
        super(name);
        config = new AveragerConfig();
        config.interval = interval;
        config.measures = measures;
        if (source instanceof ReadonlyRegister readonlyRegister) {
            config.precision = readonlyRegister.getPrecision();
        }
        this.source = resolveSource(source);
        if (source instanceof DeviceBase deviceBase) {
            DeviceBase parent = deviceBase;
            setParent(parent);
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    public Averager(Readable source, int measures) {
        this(source, measures, UNDEFINED);
    }

    public Averager(Readable source, int measures, int interval) {
        this(source.getName() + " averager", source, measures, interval);
    }

    public Averager(String name, Readable source, int measures, int interval, int precision) {
        this(name, source, measures, interval);
        config.precision = precision;
    }
    
    public Readable getSource(){
        return source;
    }
    
    Readable resolveSource(Readable source){
        if (source instanceof InlineDevice inlineDevice){
            try{
                if (inlineDevice.getProtocol().equals("bs")){
                    Stream stream = new Stream("Averager inner device stream");
                    inlineDevice.setParent(stream);
                    innerDevice = stream;
                }
                inlineDevice.initialize();
                Device dev = inlineDevice.getDevice();                
                source = (Readable) dev;
                if (innerDevice == null){
                    innerDevice = dev;
                }         
                dev.initialize();
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
        return source;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (source instanceof Device device) {
            device.removeListener(sourceListener);
        }
        sourceListener = null;
        super.doInitialize();
        if (config == null) {
            config = (AveragerConfig) getConfig();
        }
        if (config.interval < 0) {
            config.interval = UNDEFINED; //Sampling on event change
        }
        if (innerDevice instanceof Stream stream){
            stream.initialize();                                                                 
            stream.start(true); 
            stream.waitCacheChange(Stream.TIMEOUT_START_STREAMING);
        }
        
        if (config.interval == UNDEFINED) {
            if (!(source instanceof Device)) {
                throw new IOException("Configuration error: cannot configure read on change event if source is not a Device");
            }
            if (innerDevice!=null){
                innerDevice.setMonitored(true);                
            }
            sourceListener = new DeviceListener() {
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    addSample(value instanceof Number number ? number : null);
                }
            };
            ((Device) source).addListener(sourceListener);
        }
    }
    
    public boolean isReadOnChangeEvent(){
        return (config.interval < 0);
    }

    DeviceListener sourceListener;
    ScheduledExecutorService monitoringTimer;

    @Override
    protected void doSetMonitored(boolean value) {
        if (value) {
            if (config.interval >= 0) {
                    monitoringTimer = Threading.scheduleAtFixedRateNotRetriggerable(() -> {
                    if(isInitialized()){
                        readSample();
                    }
                }, 0, config.interval, TimeUnit.MILLISECONDS, "Averager Task: " + getName());
            }
        } else {
            if (monitoringTimer != null) {
                monitoringTimer.shutdownNow();
                monitoringTimer = null;
            }
        }
    }

    void readSample() {
        Number sample = null;
        try {
            sample = ((Number) source.read());
        } catch (Exception ex) {
        }
        addSample(sample);
    }
    
    void addSample(Number sample) {
        try {
            synchronized (samples) {
                samples.add(sample);
                while (samples.size() > config.measures) {
                    samples.remove(0);
                }
            }
            Number[] values = null;
            synchronized (samples) {
                values = samples.toArray(new Number[0]);
            }
            setCache(new DescStatsDouble(values, config.precision));
            for (Device dev : getChildren()){
                if (dev instanceof RegisterStats registerStats){                  
                    registerStats.read();
                }
            }
        } catch (Exception ex) {
        }
    }

    final ArrayList<Number> samples = new ArrayList<>();

    @Override
    protected DescStatsDouble doRead() throws IOException, InterruptedException {
        if (!isMonitored()) {
            samples.clear();
            while (true) {
                Chrono chrono = new Chrono();
                if (config.interval >= 0) {
                    readSample();
                }
                if ((config.measures <= 0) || (samples.size() >= config.measures)) {
                    break;
                }
                if (config.interval > 0) {
                    chrono.waitTimeout(config.interval);
                } else if (config.interval < 0) {
                    Thread.sleep(getWaitSleep());
                }
            }
        }
        return take();
    }
   
    public interface RegisterStats extends Device{
        default Device getSource(){
            return InlineDevice.getSourceDevice(this);
        }
        @Override
        default boolean isMonitored() {
            return getParent().isMonitored();
        }        
        Object read() throws IOException, InterruptedException;
    }

    
    public abstract class AveragerStatsNumber extends ReadableNumberDevice<Double> implements RegisterStats{
        boolean forceRead;
        AveragerStatsNumber(String name, String type){
            super((name == null) ? source.getName() + " " + type : name);
            setParent(Averager.this);
            try {
                this.initialize();
            } catch (Exception ex) {
            }                   
        }
        public AveragerStatsNumber withForceRead(boolean forceRead){
            this.forceRead = forceRead;
            return this;
        }
        DescStatsDouble getData() throws IOException, InterruptedException{
            return forceRead ?  Averager.this.read() : Averager.this.take();
        }               
    }
    
    public abstract class AveragerStatsArray extends ReadableArrayDevice<double[]> implements RegisterStats{
        boolean forceRead;
        AveragerStatsArray(String name, String type){
            super((name == null) ? source.getName() + " " + type : name);
            setParent(Averager.this);
            try {
                this.initialize();
            } catch (Exception ex) {
            }            
        }    
        public AveragerStatsArray withForceRead(boolean forceRead) {
            this.forceRead = forceRead;
            return this;
        }     
        DescStatsDouble getData() throws IOException, InterruptedException{
            return forceRead ?  Averager.this.read() : Averager.this.take();
        }         
    }    

    public AveragerStatsArray getSamples(){
        return getSamples(null);
    }
    
    public AveragerStatsArray getSamples(String name) {
        return new AveragerStatsArray(name, "samples") {
            @Override
            public double[] read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                double[] ret = (data == null) ? null : data.getSamples();
                setCache (ret);
                return ret;
            }

            @Override
            public int getSize() {
                return Averager.this.config.measures;
            }
        };
    }     
    
    public AveragerStatsNumber getVariance(){
        return getVariance(null);
    }
    
    public AveragerStatsNumber getVariance(String name) {
        return new AveragerStatsNumber(name, "variance") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getVariance();
                setCache (ret);
                return ret;
            }
        };
    } 

    public AveragerStatsNumber getMean(){
        return getMean(null);
    }
    
    public AveragerStatsNumber getMean(String name) {
        return new AveragerStatsNumber(name, "mean") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getMean();
                setCache (ret);
                return ret;
            }
        };
    }  

    public AveragerStatsNumber getStdev(){
        return getStdev(null);
    }    
    
    public AveragerStatsNumber getStdev(String name) {
        return new AveragerStatsNumber(name, "stdev") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getStdev();
                setCache (ret);
                return ret;
                
            }
        };
    }  

    public AveragerStatsNumber getMin(){
        return getMin(null);
    }  
    
    public AveragerStatsNumber getMin(String name) {
        return new AveragerStatsNumber(name, "min") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getMin();
                setCache (ret);
                return ret;
            }
        };
    }  

    public AveragerStatsNumber getMax(){
        return getMax(null);
    }  
    
    public AveragerStatsNumber getMax(String name) {
        return new AveragerStatsNumber(name, "max") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getMax();
                setCache (ret);
                return ret;
            }
        };
    }  
    
    public AveragerStatsNumber getSum(){
        return getSum(null);
    }
    
    public AveragerStatsNumber getSum(String name) {
        return new AveragerStatsNumber(name, "sum") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getSum();
                setCache (ret);
                return ret;
            }
        };
    }      
    
    public AveragerStatsNumber getRms(){
        return getRms(null);
    }
    
    public AveragerStatsNumber getRms(String name) {
        return new AveragerStatsNumber(name, "rms") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                Double ret = (data == null) ? null : data.getRms();
                setCache (ret);
                return ret;
            }
        };
    }      
        
    
    //Utilities
    public static boolean isAverager(Readable readable){
        if (readable instanceof Averager){
            return true;
        }
        if ((readable instanceof CacheReadable cacheReadable) && (cacheReadable.getParent() instanceof Averager)){
             return true;
         }
        return false;
    }    

    @Override
    protected void doClose() throws IOException {
        if (source instanceof Device device) {
            device.removeListener(sourceListener);
        }        
        if (innerDevice!=null){
            try {
                innerDevice.close();
            } catch (Exception ex) {
                Logger.getLogger(Averager.class.getName()).log(Level.WARNING, null, ex);
            }
            innerDevice = null;
        }
        if (monitoringTimer != null) {
            monitoringTimer.shutdownNow();
            monitoringTimer = null;
        }
        super.doClose();
    }

}
