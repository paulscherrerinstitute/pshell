package ch.psi.pshell.epics;

import ch.psi.pshell.device.Register;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.pshell.utils.Convert;
import gov.aps.jca.dbr.DBRType;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
public abstract class PV<T> extends RegisterBase<T> {
    
    final Class type;
    T value;
    Integer fixedSize = null;  
    final CAS cas;
    static final HashMap<String, PV> PVs = new HashMap<>();

    protected PV(String name, String type, boolean array, Integer fixedSize) throws IOException, InterruptedException {
        super(name);
        this.type = getType(type, array);
        this.fixedSize = fixedSize;
        clear();
        initialize();
        cas = new CAS(name, this, type);
    }
    
    @Override
    protected void doClose() throws IOException {
        try{
            cas.close();
        } catch (Exception ex){
            //Ignored
        }
        super.doClose();
    }
    

    Class getType(String type, boolean array) throws IOException {
        return getType(DBRType.forName("DBR_" + type.toUpperCase()), array);
    }

    abstract Class getType(DBRType type, boolean array) throws IOException;

    @Override
    protected void doWrite(T value) throws IOException, InterruptedException {
        if (value == null) {
            clear();
        } else {
            this.value = value;
        }
    }

    @Override
    protected T doRead() throws IOException, InterruptedException {
        return value;
    }
        

    protected void clear() {
        value = null;
    }

    public static class Scalar<T> extends PV<T> {

        protected Scalar(String name, String type) throws IOException, InterruptedException {
            super(name, type, false, null);
        }

        Class getType(DBRType type, boolean array) throws IOException {
            if (type == DBRType.STRING) {
                return String.class;
            } else if (type == DBRType.DOUBLE) {
                return double.class;
            } else if (type == DBRType.FLOAT) {
                return float.class;
            } else if ((type == DBRType.INT) || (type == DBRType.ENUM)) {
                return int.class;
            } else if (type == DBRType.SHORT) {
                return short.class;
            } else if (type == DBRType.BYTE) {
                return byte.class;
            } else {
                throw new IOException("Invalid DBRType: " + type);
            }
        }

        @Override
        protected void clear() {
            if (type == String.class) {
                value = (T) "";
            } else if (type == double.class) {
                value = (T) Double.valueOf(Double.NaN);
            } else if (type == float.class) {
                value = (T) Float.valueOf(Float.NaN);
            } else if (type == int.class) {
                value = (T) Integer.valueOf(0);
            } else if (type == short.class) {
                value = (T) Short.valueOf((short) 0);
            } else if (type == byte.class) {
                value = (T) Byte.valueOf((byte) 0);
            }
        }
    }

    public static class Waveform<T> extends PV<T> implements Register.RegisterArray<T> {

        protected Waveform(String name, String type, Integer fixedSize) throws IOException, InterruptedException {
            super(name, type, true, fixedSize);            
        }

        Class getType(DBRType type, boolean array) throws IOException {
            if (type == DBRType.STRING) {
                return String.class;
            } else if (type == DBRType.DOUBLE) {
                return double[].class;
            } else if (type == DBRType.FLOAT) {
                return float[].class;
            } else if ((type == DBRType.INT) || (type == DBRType.ENUM)) {
                return int[].class;
            } else if (type == DBRType.SHORT) {
                return short[].class;
            } else if (type == DBRType.BYTE) {
                return byte[].class;
            } else {
                throw new IOException("Invalid DBRType: " + type);
            }
        }

        @Override
        protected void doWrite(T value) throws IOException, InterruptedException {
            if (value instanceof List list){
                value = (T) Convert.toPrimitiveArray(list, type.getComponentType());
            }
            int sz = Array.getLength(value);
            if ((value == null) || (sz == 0)) {
                clear();
            } else if (fixedSize != null) {
                clear();
                System.arraycopy(value, 0, this.value, 0, Math.min(fixedSize, sz));
            } else {
                this.value = value;
            }
        }
        
        @Override
        public T take() {
            T cache = super.take();      
            if (cache instanceof List list){
                cache = (T) Convert.toPrimitiveArray(list, type.getComponentType());
            }            
            int sz = Array.getLength(cache);
            if (cache != null){
                if (fixedSize != null) {
                    T buffer = getEmptyBuffer(sz);
                    System.arraycopy(cache, 0, buffer, 0, Math.min(fixedSize, sz));
                    return buffer;
                }                
            }
            return cache;
        }
                
        

        @Override
        public int getSize() {
            return Array.getLength(value);
        }

        public void setFixedSize(Integer size) {
            this.fixedSize = size;
            clear();
        }
        
        public Integer getFixedSize() {
            return fixedSize;
        }        
      
        
        T getEmptyBuffer(int size) {
            int sz = ((fixedSize == null) || (fixedSize <= 0)) ? 1 : fixedSize;  //Array cannot have size 0.
            if (type == String.class) {
                return (T) "";
            } else if (type == double[].class) {
                var arr = new double[sz];
                Arrays.fill(arr, Double.NaN);
                return (T) arr;
            } else if (type == float[].class) {
                var arr = new float[sz];
                Arrays.fill(arr, Float.NaN);
                return  (T) arr;
            } else if (type == int[].class) {
                var arr = new int[sz];
                Arrays.fill(arr, 0);
                return  (T) arr;
            } else if (type == short[].class) {
                var arr = new short[sz];
                Arrays.fill(arr, (short) 0);
                return  (T) arr;
            } else if (type == byte[].class) {
                var arr = new byte[sz];
                Arrays.fill(arr, (byte) 0);
                return  (T) arr;
            } 
            return null;
        }
          
        @Override
        protected void clear() {
            int sz = ((fixedSize == null) || (fixedSize <= 0)) ? 1 : fixedSize;  //Array cannot have size 0.
            value = getEmptyBuffer(sz);
        }        
    }    

    public static Scalar newScalar(String name, String type) throws IOException, InterruptedException {
        if (!PVs.containsKey(name)) {
            PVs.put(name, new Scalar(name, type));
        }
        return (Scalar) PVs.get(name);
    }

    public static Waveform newWaveform(String name, String type) throws IOException, InterruptedException {
        return newWaveform(name, type, null);
    }

    public static Waveform newWaveform(String name, String type, Integer size) throws IOException, InterruptedException {
        if (!PVs.containsKey(name)) {
            PVs.put(name, new Waveform(name, type, size));
        }
        return (Waveform) PVs.get(name);
    }
    
    public static void destroy(){
        for (PV pv : PVs.values()){            
            pv.close();
        }
        PVs.clear();
    }
    
}
