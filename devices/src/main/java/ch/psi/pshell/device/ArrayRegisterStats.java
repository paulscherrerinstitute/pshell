package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.DoubleType;
import ch.psi.pshell.device.ReadableRegister.ReadableRegisterArray;
import ch.psi.pshell.device.ReadableRegister.ReadableRegisterMatrix;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;

/**
 * Provides statistics on ReadonlyRegisterArray and ReadonlyRegisterMatric
 * devices.
 */
public class ArrayRegisterStats extends ReadonlyRegisterBase<DescStatsDouble> implements ReadonlyRegisterNumber<DescStatsDouble>, DoubleType {

    ReadonlyRegister source;
    int precision = UNDEFINED_PRECISION;
    DeviceListener sourceListener;

    public ArrayRegisterStats(String name, ReadableArray source) {
        super(name);
        this.source = (source instanceof ReadonlyRegisterArray reg) ? reg : new ReadableRegisterArray(source);
        if (source instanceof DeviceBase deviceBase) {
            DeviceBase parent = deviceBase;
            setParent(parent);
        }
        if (source instanceof ReadonlyRegister readonlyRegister) {
            precision = readonlyRegister.getPrecision();
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }
    
    public ArrayRegisterStats(ReadableArray source) {
        this(source.getName() + " stats", source);
    }

    public ArrayRegisterStats(String name,  ReadableMatrix source) {
        super(name);
        this.source = (source instanceof ReadonlyRegisterMatrix reg) ? reg : new ReadableRegisterMatrix(source);        
        
        if (source instanceof DeviceBase deviceBase) {
            DeviceBase parent = deviceBase;
            setParent(parent);
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    public ArrayRegisterStats(ReadableMatrix source) {
        this(source.getName() + " stats", source);
    }
    
    @Override
    protected void doSetMonitored(boolean value) {
        if (value) {
            sourceListener = new DeviceListener() {
                @Override
                public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                    setCache(processData(value), timestamp);
                }
            };
            source.addListener(sourceListener);
        } else {
            source.removeListener(sourceListener);
        }
    }

    @Override
    protected DescStatsDouble doRead() throws IOException, InterruptedException {
        if (isMonitored()) {
            return take();
        }
        Object data = source.getValue();
        return processData(data);
    }

    DescStatsDouble processData(Object data) {
        int rank = Arr.getRank(data);
        if (rank < 1) {
            return null;
        }
        if (rank > 1) {
            data = Convert.flatten(data);
        }
        if (!data.getClass().getComponentType().isPrimitive()) {
            data = Convert.toPrimitiveArray(data);
        }
        return new DescStatsDouble((double[]) Convert.toDouble(data), precision);
    }

    public abstract class ReadableStatsNumber extends ReadableNumberDevice<Double> implements Averager.RegisterStats {

        ReadableStatsNumber(String name, String type) {
            super((name == null) ? source.getName() + " " + type : name);
            setParent(ArrayRegisterStats.this);
            try {
                this.initialize();
            } catch (Exception ex) {
            }
        }

        DescStatsDouble getData() throws IOException, InterruptedException {
            return ArrayRegisterStats.this.read();
        }
    }

    public ReadableStatsNumber getVariance() {
        return getVariance(null);
    }

    public ReadableStatsNumber getVariance(String name) {
        return new ReadableStatsNumber(name, "variance") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                return (data == null) ? null : data.getVariance();
            }
        };
    }

    public ReadableStatsNumber getMean() {
        return getMean(null);
    }

    public ReadableStatsNumber getMean(String name) {
        return new ReadableStatsNumber(name, "mean") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                return (data == null) ? null : data.getMean();
            }
        };
    }

    public ReadableStatsNumber getStdev() {
        return getStdev(null);
    }

    public ReadableStatsNumber getStdev(String name) {
        return new ReadableStatsNumber(name, "stdev") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                return (data == null) ? null : data.getStdev();
            }
        };
    }

    public ReadableStatsNumber getMin() {
        return getMin(null);
    }

    public ReadableStatsNumber getMin(String name) {
        return new ReadableStatsNumber(name, "min") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                return (data == null) ? null : data.getMin();
            }
        };
    }

    public ReadableStatsNumber getMax() {
        return getMax(null);
    }

    public ReadableStatsNumber getMax(String name) {
        return new ReadableStatsNumber(name, "max") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                return (data == null) ? null : data.getMax();
            }
        };
    }

    public ReadableStatsNumber getSum() {
        return getSum(null);
    }

    public ReadableStatsNumber getSum(String name) {
        return new ReadableStatsNumber(name, "sum") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble data = getData();
                return (data == null) ? null : data.getSum();
            }
        };
    }

    @Override
    protected void doClose() throws IOException {
        if (source != null) {
            source.removeListener(sourceListener);
        }
        super.doClose();
    }

}
