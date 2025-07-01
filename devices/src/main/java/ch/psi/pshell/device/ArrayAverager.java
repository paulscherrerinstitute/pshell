package ch.psi.pshell.device;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Threading;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs averaging on array register.
 */
public class ArrayAverager extends ReadonlyRegisterBase<double[]> implements ReadonlyRegisterArray<double[]> {

    ArrayAveragerConfig config;
    final ReadableArray source;
    Device innerDevice;

    @Override
    public int getSize() {
        return source.getSize();
    }

    /**
     * Entity class holding the configuration of an Averager device.
     */
    public static class ArrayAveragerConfig extends RegisterConfig {

        public int measures;
        public int interval;    //If <=0 then is based on change events
        public boolean integrate;
    }

    public ArrayAverager(String name, ReadableArray source) {
        super(name, new ArrayAveragerConfig());

        this.source = source;
        if (source instanceof DeviceBase deviceBase) {
            setParent(deviceBase);
        }
    }

    public ArrayAverager(String name, ReadableArray source, int measures) {
        this(name, source, measures, UNDEFINED);
    }

    public ArrayAverager(String name, ReadableArray source, int measures, int interval) {
        this(name, source, measures, interval, false);
    }

    public ArrayAverager(String name, ReadableArray source, int measures, int interval, boolean integrate) {
        super(name);
        config = new ArrayAveragerConfig();
        config.interval = interval;
        config.measures = measures;
        config.integrate = integrate;
        if (source instanceof ReadonlyRegister readonlyRegister) {
            config.precision = readonlyRegister.getPrecision();
        }
        this.source = source;
        if (source instanceof DeviceBase deviceBase) {
            DeviceBase parent = deviceBase;
            setParent(parent);
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    public ArrayAverager(ReadableArray source, int measures) {
        this(source, measures, UNDEFINED);
    }

    public ArrayAverager(ReadableArray source, int measures, int interval) {
        this(source, measures, interval, false);
    }

    public ArrayAverager(ReadableArray source, int measures, int interval, boolean integrate) {
        this(source.getName() + " averager", source, measures, interval, integrate);
    }

    public ArrayAverager(String name, ReadableArray source, int measures, int interval, int precision) {
        this(name, source, measures, interval);
        config.precision = precision;
    }

    public Readable getSource() {
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
            config = (ArrayAveragerConfig) getConfig();
        }
        if (config.interval < 0) {
            config.interval = UNDEFINED; //Sampling on event change
        }
        if (innerDevice instanceof Stream stream) {
            stream.initialize();   
            stream.start(true);
            stream.waitCacheChange(Stream.TIMEOUT_START_STREAMING);
        }

        if (config.interval == UNDEFINED) {
            if (!(source instanceof Device)) {
                throw new IOException("Configuration error: cannot configure read on change event if source is not a Device");
            }
            if (innerDevice != null) {
                innerDevice.setMonitored(true);
            }
            sourceListener = new DeviceListener() {
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    addSample(value);
                }
            };
            ((Device) source).addListener(sourceListener);
        }
    }

    public boolean isReadOnChangeEvent() {
        return (config.interval < 0);
    }

    DeviceListener sourceListener;
    ScheduledExecutorService monitoringTimer;

    @Override
    protected void doSetMonitored(boolean value) {
        if (value) {
            if (config.interval >= 0) {
                monitoringTimer = Threading.scheduleAtFixedRateNotRetriggerable(() -> {
                    if (isInitialized()) {
                        readSample();
                    }
                }, 0, config.interval, TimeUnit.MILLISECONDS, "Array Averager Task: " + getName());
            }
        } else {
            if (monitoringTimer != null) {
                monitoringTimer.shutdownNow();
                monitoringTimer = null;
            }
        }
    }

    void readSample() {
        if (this.isInitialized()) {
            try {
                addSample(source.read());
            } catch (Exception ex) {
            }
        }
    }

    void addSample(Object sample) {
        if (!this.isInitialized()) {
            return;
        }
        synchronized (samples) {
            try {
                int rank = Arr.getRank(sample);
                if (rank < 1) {
                    throw new IOException("Scalar source");
                } else {
                    if (rank > 1) {
                        sample = Convert.flatten(sample);
                    }
                    sample = (double[]) Convert.toDouble(sample);
                }

                samples.add((double[]) sample);
                while (samples.size() > config.measures) {
                    samples.remove(0);
                }
                double[][] values = null;

                values = samples.toArray(new double[0][0]);
                values = Arr.removeNulls(values);

                if (values.length > 0) {
                    Integer size = values[0].length;
                    for (double[] arr : samples) {
                        if (size != arr.length) {
                            throw new IOException("Invalid array size");
                        }
                    }

                }
                setCache(calculate(values));
                for (Device dev : getChildren()){
                    if (dev instanceof ArrayAveragerStatsNumber arrayAveragerStatsNumber){
                        arrayAveragerStatsNumber.read();
                    }
                }
                
            } catch (Exception ex) {
            }
        }

    }

    double[] calculate(double[][] values) {
        double[] ret = new double[values[0].length];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < values.length; j++) {
                ret[i] += values[j][i];
            }
        }
        if (!config.integrate) {
            for (int i = 0; i < ret.length; i++) {
                ret[i] /= values.length;
            }
        }
        return ret;
    }

    final ArrayList<double[]> samples = new ArrayList<>();

    @Override
    protected double[] doRead() throws IOException, InterruptedException {
        if (!isMonitored()) {
            synchronized (samples) {
                samples.clear();
            }
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

    public abstract class ArrayAveragerStatsNumber extends ReadableNumberDevice<Double> implements Averager.RegisterStats {

        boolean forceRead;

        ArrayAveragerStatsNumber(String name, String type) {
            super((name == null) ? source.getName() + " " + type : name);
            setParent(ArrayAverager.this);
            try {
                this.initialize();
            } catch (Exception ex) {
            }
        }

        public ArrayAveragerStatsNumber withForceRead(boolean forceRead) {
            this.forceRead = forceRead;
            return this;
        }

        double[] getData() throws IOException, InterruptedException {
            return forceRead ? ArrayAverager.this.read() : ArrayAverager.this.take();
        }

    }

    public abstract class ArrayAveragerStatsMatrix extends ReadableMatrixDevice<double[][]> implements Averager.RegisterStats {

        ArrayAveragerStatsMatrix(String name, String type) {
            super((name == null) ? source.getName() + " " + type : name);
            setParent(ArrayAverager.this);
            try {
                this.initialize();
            } catch (Exception ex) {
            }
        }
    }

    public ArrayAveragerStatsMatrix getSamples() {
        return getSamples(null);
    }

    public ArrayAveragerStatsMatrix getSamples(String name) {
        return new ArrayAveragerStatsMatrix(name, "samples") {
            @Override
            public double[][] read() throws IOException, InterruptedException {
                synchronized (samples) {
                    return samples.toArray(new double[0][0]);
                }
            }

            @Override
            public int getWidth() {
                return getSize();
            }

            @Override
            public int getHeight() {
                return ArrayAverager.this.config.measures;
            }
        };
    }

    public ArrayAveragerStatsNumber getVariance() {
        return getVariance(null);
    }

    public ArrayAveragerStatsNumber getVariance(String name) {
        return new ArrayAveragerStatsNumber(name, "variance") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getVariance();
                setCache (ret);
                return ret;                       
            }
        };
    }

    public ArrayAveragerStatsNumber getRms() {
        return getRms(null);
    }

    public ArrayAveragerStatsNumber getRms(String name) {
        return new ArrayAveragerStatsNumber(name, "rms") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getRms();
                setCache (ret);
                return ret;                       
            }
        };
    }
    
    public ArrayAveragerStatsNumber getMean() {
        return getMean(null);
    }

    public ArrayAveragerStatsNumber getMean(String name) {
        return new ArrayAveragerStatsNumber(name, "mean") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getMean();
                setCache (ret);
                return ret;                       
            }
        };
    }

    public ArrayAveragerStatsNumber getStdev() {
        return getStdev(null);
    }

    public ArrayAveragerStatsNumber getStdev(String name) {
        return new ArrayAveragerStatsNumber(name, "stdev") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getStdev();
                setCache (ret);
                return ret;                       
            }
        };
    }

    public ArrayAveragerStatsNumber getMin() {
        return getMin(null);
    }

    public ArrayAveragerStatsNumber getMin(String name) {
        return new ArrayAveragerStatsNumber(name, "min") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getMin();
                setCache (ret);
                return ret;                       
            }
        };
    }

    public ArrayAveragerStatsNumber getMax() {
        return getMax(null);
    }

    public ArrayAveragerStatsNumber getMax(String name) {
        return new ArrayAveragerStatsNumber(name, "max") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getMax();
                setCache (ret);
                return ret;                       
            }
        };
    }

    public ArrayAveragerStatsNumber getSum() {
        return getSum(null);
    }

    public ArrayAveragerStatsNumber getSum(String name) {
        return new ArrayAveragerStatsNumber(name, "sum") {
            @Override
            public Double read() throws IOException, InterruptedException {
                double[] data = getData();
                Double ret =  (data == null) ? null : new DescStatsDouble(getData(), getPrecision()).getSum();
                setCache (ret);
                return ret;                       
            }
        };
    }

    @Override
    protected void doClose() throws IOException {
        if (source instanceof Device device) {
            device.removeListener(sourceListener);
        }
        if (innerDevice != null) {
            try {
                innerDevice.close();
            } catch (Exception ex) {
                Logger.getLogger(ArrayAverager.class.getName()).log(Level.WARNING, null, ex);
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
