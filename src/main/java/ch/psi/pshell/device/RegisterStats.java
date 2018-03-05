package ch.psi.pshell.device;

import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import java.io.IOException;

/**
 * Provides statistics on ReadonlyRegisterArray and ReadonlyRegisterMatric
 * devices.
 */
public class RegisterStats extends ReadonlyRegisterBase<DescStatsDouble> implements ReadonlyRegisterNumber<DescStatsDouble> {

    ReadonlyRegister source;
    int precision = -1;

    public RegisterStats(String name, ReadonlyRegisterArray source) {
        super(name);
        this.source = source;
        if (source instanceof DeviceBase) {
            DeviceBase parent = ((DeviceBase) source);
            setParent(parent);
        }
        if (source instanceof ReadonlyRegister) {
            precision = ((ReadonlyRegister) source).getPrecision();
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }
    
    public RegisterStats(ReadonlyRegisterArray source) {
        this(source.getName() + " stats", source);
    }    

    public RegisterStats(String name, ReadonlyRegisterMatrix source) {
        super(name);
        this.source = source;
        if (source instanceof DeviceBase) {
            DeviceBase parent = ((DeviceBase) source);
            setParent(parent);
        }
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    public RegisterStats(ReadonlyRegisterMatrix source) {
        this(source.getName() + " stats", source);
    } 

    @Override
    protected DescStatsDouble doRead() throws IOException, InterruptedException {
        Object data = source.read();
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
    
    public abstract class ReadableStats extends ReadableNumberDevice<Double>{
        ReadableStats(String name, String type){
            super((name == null) ? source.getName() + " " + type : name);
            setParent(RegisterStats.this);
        }
        
    }
    
    public abstract class ReadableStatsArray extends ReadableArrayDevice<double[]>{
        ReadableStatsArray(String name, String type){
            super((name == null) ? source.getName() + " " + type : name);
            setParent(RegisterStats.this);
        }        
    }      

    public ReadableStatsArray getSamples(String name) {
        return new ReadableStatsArray(name, "samples") {
            @Override
            public double[] read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getSamples();
            }

            @Override
            public int getSize() {
                if (source instanceof ReadonlyRegisterMatrix){
                    return ((ReadonlyRegisterMatrix)source).getWidth() * ((ReadonlyRegisterMatrix)source).getHeight();
                }
                return ((ReadonlyRegisterArray)source).getSize();
            }
        };
    }     

    public ReadableStats getVariance(String name) {
        return new ReadableStats(name, "variance") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getVariance();
            }
        };
    } 

    public ReadableStats getMean(String name) {
        return new ReadableStats(name, "mean") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getMean();
            }
        };
    }  

    public ReadableStats getStdev(String name) {
        return new ReadableStats(name, "stdev") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getStdev();
            }
        };
    }  

    public ReadableStats getMin(String name) {
        return new ReadableStats(name, "min") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getMin();
            }
        };
    }  

    public ReadableStats getMax(String name) {
        return new ReadableStats(name, "max") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getMax();
            }
        };
    }  
    
    public ReadableStats getSum(String name) {
        return new ReadableStats(name, "sum") {
            @Override
            public Double read() throws IOException, InterruptedException {
                DescStatsDouble cache = RegisterStats.this.read();
                return (cache == null) ? null : cache.getSum();
            }
        };
    }      

}
