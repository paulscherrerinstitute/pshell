package ch.psi.pshell.device;

import ch.psi.pshell.bs.Dispatcher;
import ch.psi.pshell.bs.Provider;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.UrlDevice;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import ch.psi.utils.Chrono;
import ch.psi.utils.Threading;
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
public class Averager extends ReadonlyRegisterBase<DescStatsDouble> implements ReadonlyRegisterNumber<DescStatsDouble> {

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
        if (source instanceof DeviceBase) {
            setParent(((DeviceBase) source));
        }
    }

    public Averager(String name, Readable source, int measures) {
        this(name, source, measures, -1);
    }

    public Averager(String name, Readable source, int measures, int interval) {
        super(name);
        config = new AveragerConfig();
        config.interval = interval;
        config.measures = measures;
        if (source instanceof ReadonlyRegister) {
            config.precision = ((ReadonlyRegister) source).getPrecision();
        }
        this.source = resolveSource(source);
        if (source instanceof DeviceBase) {
            DeviceBase parent = ((DeviceBase) source);
            setParent(parent);
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    public Averager(Readable source, int measures) {
        this(source, measures, -1);
    }

    public Averager(Readable source, int measures, int interval) {
        this(source.getName() + " averager", source, measures, interval);
    }

    public Averager(String name, Readable source, int measures, int interval, int precision) {
        this(name, source, measures, interval);
        config.precision = precision;
    }
    
    Readable resolveSource(Readable source){
        if (source instanceof UrlDevice){
            try{
                if (((UrlDevice)source).getProtocol().equals("bs")){
                    Provider dispatcher = Context.getInstance().getDevicePool().getByName("dispatcher", ch.psi.pshell.bs.Provider.class);
                    if (dispatcher == null) {
                        dispatcher = Dispatcher.createDefault();
                    }                                            
                    Stream stream = new Stream("Averager inner device stream", dispatcher);
                    stream.initialize();                                                     
                    ((UrlDevice)source).setParent(stream);
                    innerDevice = stream;
                }
                ((UrlDevice)source).initialize();
                Device dev = ((UrlDevice)source).getDevice();                
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
        if (source instanceof Device) {
            ((Device) source).removeListener(sourceListener);
        }
        sourceListener = null;
        super.doInitialize();
        if (config == null) {
            config = (AveragerConfig) getConfig();
        }
        if (config.interval < 0) {
            config.interval = -1; //Sampling on event change
        }
        if (innerDevice instanceof Stream){
            ((Stream)innerDevice).start(true); 
            ((Stream)innerDevice).waitCacheChange(10000);
        }
        
        if (config.interval < 0) {
            if (!(source instanceof Device)) {
                throw new IOException("Configuration error: cannot configure read on change event if source is not a Device");
            }
            if (innerDevice!=null){
                innerDevice.setMonitored(true);                
            }
            sourceListener = new DeviceAdapter() {
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    addSample(((value != null) && (value instanceof Number)) ? ((Number) value) : null);
                }
            };
            ((Device) source).addListener(sourceListener);
        }
    }

    DeviceListener sourceListener;
    ScheduledExecutorService monitoringTimer;

    protected void doSetMonitored(boolean value) {
        if (value) {
            if (config.interval >= 0) {
                monitoringTimer = Threading.scheduleAtFixedRateNotRetriggerable(new Runnable() {
                    @Override
                    public void run() {
                        readSample();
                    }
                }, 0, config.interval, TimeUnit.MILLISECONDS, "Snapshot Dialog Task");
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
                    Thread.sleep(5);
                }
            }
        }
        return take();
    }

    public ReadableArray<double[]> getSamples() {
        return new ReadableArray<double[]>() {
            @Override
            public int getSize() {
                return config.measures;
            }

            @Override
            public double[] read() throws IOException, InterruptedException {
                DescStatsDouble cache = take();
                return (cache == null) ? new double[0] : cache.samples;
            }

            @Override
            public String getName() {
                return Averager.this.getName() + " samples";
            }
        };
    }

    public Readable<Double> getVariance() {
        return new Readable<Double>() {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = take();
                return (cache == null) ? null : cache.variance;
            }

            @Override
            public String getName() {
                return Averager.this.getName() + " variance";
            }
        };
    }

    public Readable<Double> getMean() {
        return new Readable<Double>() {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = take();
                return (cache == null) ? null : cache.mean;
            }

            @Override
            public String getName() {
                return Averager.this.getName() + " mean";
            }
        };
    }

    public Readable<Double> getStdev() {
        return new Readable<Double>() {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = take();
                return (cache == null) ? null : cache.stdev;
            }

            @Override
            public String getName() {
                return Averager.this.getName() + " stdev";
            }
        };
    }

    public Readable<Double> getMin() {
        return new Readable<Double>() {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = take();
                return (cache == null) ? null : cache.min;
            }

            @Override
            public String getName() {
                return Averager.this.getName() + " min";
            }
        };
    }

    public Readable<Double> getMax() {
        return new Readable<Double>() {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = take();
                return (cache == null) ? null : cache.max;
            }

            @Override
            public String getName() {
                return Averager.this.getName() + " max";
            }
        };
    }

    protected void doClose() throws IOException {
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
