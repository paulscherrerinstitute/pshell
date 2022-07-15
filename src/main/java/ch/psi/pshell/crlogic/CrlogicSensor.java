package ch.psi.pshell.crlogic;

import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import java.io.IOException;

/**
 * Readables that are received, in a CrlogicScan, through the ZMQ stream.
 */
public class CrlogicSensor extends ReadonlyAsyncRegisterBase<Double> {

    final boolean isDelta;
    final String key;

    public CrlogicSensor(String key) {
        this(key, key);
    }

    public CrlogicSensor(String name, String key) {
        this(name.startsWith("% ") ? name.substring(1) : name,
                key.startsWith("%") ? key.substring(1) : key, key.startsWith("%") ? true : false);
    }

    public CrlogicSensor(String name, String key, boolean isDelta) {
        this(name, key, isDelta, -1);
    }

    public CrlogicSensor(String name, String key, boolean isDelta, int precision) {
        super(name, precision);
        this.key = key;
        this.isDelta = isDelta;
        this.setMonitored(true);
        try {
            this.initialize();
        } catch (Exception ex) {
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        lastValue = null;
    }

    public String getKey() {
        return key;
    }

    public boolean isDelta() {
        return isDelta;
    }

    private Double lastValue = null;

    void setRawValue(Double value) {
        if (isDelta) {
            setCache((lastValue == null) ? Double.NaN : (value - lastValue));
            lastValue = value;
        } else {
            setCache(value);
        }
    }
    
    public static class Timestamp extends CrlogicSensor{
        public Timestamp(){
            this("Timestamp");
        }

        public Timestamp(String name){
            super(name, "TIMESTAMP");
        }
    }
    
    public static class Scaler extends CrlogicSensor{
        public Scaler(int scalerNumber){
            this("Scaler"+scalerNumber, scalerNumber);
        }

        public Scaler(String name, int scalerNumber){
            super(name, "SCALER"+scalerNumber);
        }
        
        public Scaler(String name, int scalerNumber, boolean isDelta){
            super(name, "SCALER"+scalerNumber, isDelta);
        }
        public Scaler(String name, int scalerNumber, boolean isDelta, int precision){
            super(name, "SCALER"+scalerNumber, isDelta,precision);
        }
    }    
    
    //TODO: Implement SCR detectors
}
