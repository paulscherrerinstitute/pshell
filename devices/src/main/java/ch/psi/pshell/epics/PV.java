package ch.psi.pshell.epics;

import ch.psi.pshell.device.Register;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
public abstract class PV<T> extends RegisterBase<T> {
    T value;
    final Type type;
    Integer fixedSize = null;  
    final CAS cas;
    static final HashMap<String, PV> PVs = new HashMap<>();
    
    public enum Type{
        STRING(String.class),
        SHORT(short.class),
        FLOAT(float.class),
        ENUM(int.class),
        BYTE(byte.class),
        INT(int.class),
        DOUBLE(double.class);
        Class componentType;
        Type(Class componentType){
            this.componentType = componentType;
        }        
        public Class getComponentType(){
            return componentType;
        }        
    }
    
    protected PV(String name, String type, boolean array, Integer fixedSize) throws IOException, InterruptedException {
        this( name, Type.valueOf(type.toUpperCase()), array, fixedSize);
    }
    
    protected PV(String name, Type type, boolean array, Integer fixedSize) throws IOException, InterruptedException {
        super(name);
        if (PVs.containsKey(name)) {            
            throw new IOException("PV already created");
        }
        this.type = type;
        this.fixedSize = fixedSize;
        clear();
        initialize();
        cas = new CAS(name, this, type.toString());
        PVs.put(name, this);
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
        public Scalar(String name, Type type) throws IOException, InterruptedException {
            super(name, type, false, null);
        }
        
        public Scalar(String name, String type) throws IOException, InterruptedException {
            super(name, type, false, null);
        }

        
        @Override
        protected void clear() {
            switch(type){
                case STRING -> value = (T) "";
                case DOUBLE -> value = (T) Double.valueOf(Double.NaN);
                case FLOAT -> value = (T) Float.valueOf(Float.NaN);
                case INT,ENUM -> value = (T) Integer.valueOf(0);
                case SHORT -> value = (T) Short.valueOf((short) 0);
                case BYTE -> value = (T) Byte.valueOf((byte) 0);
            }
        }
    }

    public static class Waveform<T> extends PV<T> implements Register.RegisterArray<T> {

        public Waveform(String name, Type type) throws IOException, InterruptedException {
            super(name, type, true, null);            
        }

        public Waveform(String name, Type type, Integer fixedSize) throws IOException, InterruptedException {
            super(name, type, true, fixedSize);            
        }

        public Waveform(String name, String type) throws IOException, InterruptedException {
            super(name, type, true, null);            
        }

        public Waveform(String name, String type, Integer fixedSize) throws IOException, InterruptedException {
            super(name, type, true, fixedSize);            
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
            return switch (type) {
                case STRING -> (T) "";
                case DOUBLE -> {
                    var arr = new double[sz];
                    Arrays.fill(arr, Double.NaN);
                    yield (T) arr;
                }
                case FLOAT -> {
                    var arr = new float[sz];
                    Arrays.fill(arr, Float.NaN);
                    yield (T) arr;
                }
                case INT, ENUM -> {
                    var arr = new int[sz];
                    Arrays.fill(arr, 0);
                    yield (T) arr;
                }
                case SHORT -> {
                    var arr = new short[sz];
                    Arrays.fill(arr, (short) 0);
                    yield (T) arr;
                }
                case BYTE -> {
                    var arr = new byte[sz];
                    Arrays.fill(arr, (byte) 0);
                    yield (T) arr;
                }
            };
        }
          
        @Override
        protected void clear() {
            int sz = ((fixedSize == null) || (fixedSize <= 0)) ? 1 : fixedSize;  //Array cannot have size 0.
            value = getEmptyBuffer(sz);
        }        
    }    

    public static Scalar scalar(String name, String type) throws IOException, InterruptedException {
        return scalar(name, Type.valueOf(type.toUpperCase()));
    }

    public static Scalar scalar(String name, Type type) throws IOException, InterruptedException {
        if (PVs.containsKey(name)) {
            return (Scalar) PVs.get(name);
        }
        return new Scalar(name, type);
    }

    public static Waveform waveform(String name, String type) throws IOException, InterruptedException {
        return waveform(name, Type.valueOf(type.toUpperCase()), null);
    }

    public static Waveform waveform(String name, Type type) throws IOException, InterruptedException {
        return waveform(name, type, null);
    }

    public static Waveform waveform(String name, String type, Integer size) throws IOException, InterruptedException {
        return waveform(name, type, size);
    }
    
    public static Waveform waveform(String name, Type type, Integer size) throws IOException, InterruptedException {
        if (PVs.containsKey(name)) {
            return (Waveform) PVs.get(name);
        }
        return  new Waveform(name, type, size);
    }
    
    public static void destroy(){
        for (PV pv : PVs.values()){            
            pv.close();
        }
        PVs.clear();
    }
    
}
