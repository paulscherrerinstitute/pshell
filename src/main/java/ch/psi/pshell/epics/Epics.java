package ch.psi.pshell.epics;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelDescriptor;
import ch.psi.jcae.ChannelException;
import ch.psi.jcae.ChannelService;
import ch.psi.jcae.DummyChannelDescriptor;
import ch.psi.jcae.impl.DefaultChannelService;
import ch.psi.jcae.impl.JcaeProperties;
import ch.psi.jcae.impl.type.TimestampValue;
import ch.psi.utils.Convert;
import ch.psi.utils.NumberComparator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EPICS layer access.
 */
public class Epics {

    static DefaultChannelService factory;
    static Integer maxArrayBytes;
    static InvalidValueAction defaultInvalidValueAction = InvalidValueAction.Nullify;

    public static final String PROPERTY_JCAE_CONFIG_FILE = "ch.psi.jcae.config.file";
    public static boolean parallelCreation;

    public static void create() {
        create(false);
    }
    
    public static void create(String configFile) {
        create(configFile, false);
    }    
    
    public static void create(boolean parallelCreation) {
        create("jcae.properties", null, parallelCreation);
    }
            
    public static void create(String configFile, boolean parallelCreation) {        
        create(configFile, null, parallelCreation);               
    }
    
    public static void create(String configFile, String defaultProperties, boolean parallelCreation) {        
        destroy();                
        System.setProperty(PROPERTY_JCAE_CONFIG_FILE, configFile);
        File file = new File(configFile);
        if (! file.exists()){
            if (defaultProperties!=null){
                try {
                    Files.writeString(file.toPath(), defaultProperties);
                } catch (IOException ex) {
                    Logger.getLogger(Epics.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }        
        try {
            factory = new DefaultChannelService();
        } catch (Exception ex) {
            Logger.getLogger(Epics.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            maxArrayBytes = Integer.valueOf(JcaeProperties.getInstance().getMaxArrayBytes());
        } catch (Exception ex) {
        }
        Epics.parallelCreation = parallelCreation;    
    }

    public static void destroy() {
        try {
            if (factory != null) {
                factory.destroy();
                factory = null;
            }
        } catch (Exception ex) {
            Logger.getLogger(Epics.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static String getConfigFile() {
        return System.getProperty("ch.psi.jcae.config.file");
    }

    public static DefaultChannelService getChannelFactory() {
        if (factory == null) {
            throw new RuntimeException("Epics context not initialized");
        }
        return factory;
    }

    final static Object creationLock = new Object();

    public static Channel newChannel(ChannelDescriptor descriptor) throws ChannelException, InterruptedException, TimeoutException {
        ChannelService factory = getChannelFactory();
        if (parallelCreation){
            if (factory.isDryrun()) {
                return getChannelFactory().createChannel(new DummyChannelDescriptor(descriptor.getType(), descriptor.getName(), descriptor.getMonitored(), descriptor.getSize()));
            }
            return getChannelFactory().createChannel(descriptor);            
        } else {
            synchronized (creationLock) {
                if (factory.isDryrun()) {
                    return getChannelFactory().createChannel(new DummyChannelDescriptor(descriptor.getType(), descriptor.getName(), descriptor.getMonitored(), descriptor.getSize()));
                }
                return getChannelFactory().createChannel(descriptor);
            }
        }
    }

    public static <T> Channel<T> newChannel(String name, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException {
        if (type == null) {
            type = getDefaultType(name);
        }
        ChannelDescriptor descriptor = new ChannelDescriptor<>(type, name, false, size);
        return newChannel(descriptor);
    }

    public static <T> Channel<T> newChannel(String name, Class<T> type) throws ChannelException, InterruptedException, TimeoutException {
        return newChannel(name, type, null);
    }

    public static void closeChannel(Channel channel) {                
        try {
            if (parallelCreation){               
                channel.destroy();
            } else {
                synchronized (creationLock) {
                    channel.destroy();
                }
            }                
        } catch (Exception ex) {
            Logger.getLogger(Epics.class.getName()).log(Level.FINER, null, ex);
        }
}

    public static Integer getMaxArrayBytes() {
        return maxArrayBytes;
    }

    public static <T> T get(String channelName, Class<T> type) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        return get(channelName, type, null);
    }

    public static <T> T get(String channelName, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        Channel<T> channel = newChannel(channelName, type, size);
        try {
            return channel.getValue();
        } finally {
            closeChannel(channel);
        }
    }

    public static <T> Map<String, Object> getMeta(String channelName, Class<T> type) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        return getMeta(channelName, type, null);
    }

    public static <T> Map<String, Object> getMeta(String channelName, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        DefaultChannelService factory = getChannelFactory();
        if (type == null) {
            type = getDefaultType(channelName);
        }
        Channel<T> channel = newChannel(channelName, getMetadataChannelType(type), size);
        try {
            TimestampValue val = (TimestampValue) channel.getValue();
            Map<String, Object> ret = new HashMap<>();
            ret.put("value", val.getValue());
            try{
                ret.put("severity", Severity.values()[val.getSeverity()]);
            } catch (Exception ex) {
                ret.put("severity", val.getSeverity());
            }
            ret.put("timestamp", val.getTimestampPrimitive());
            ret.put("nanos", val.getNanosecondOffset());
            return ret;
        } finally {
            closeChannel(channel);
        }
    }

    public static <T> T waitValue(String channelName, T value, Integer timeout, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {       
        return waitValue(channelName, value, null, timeout, type, size);
    }
    
    public static <T> T waitValue(String channelName, T value, Comparator<T> comparator, Integer timeout, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        Channel<T> channel = newChannel(channelName, type, size);
        try{
            return waitValue(channel, value, comparator, timeout);
        } finally {
            closeChannel(channel);
        }        
    }    

    public static <T> T waitValue(Channel<T> channel, T value, Integer timeout) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        return waitValue(channel, value, null, timeout);
    }
    
    public static <T> T waitValue(Channel<T> channel, T value, Comparator<T> comparator, Integer timeout) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {        
        try {
            if (comparator != null) {
                if (timeout != null) {
                    return channel.waitForValue(value, timeout, comparator);
                } else {
                    return channel.waitForValue(value, comparator);
                }
            } else {
                if (timeout != null) {
                    return channel.waitForValue(value, timeout);
                } else {
                    return channel.waitForValue(value);
                }
            }
        } catch (TimeoutException ex) {
            //In some rare occasions the waiting monitor fails.
            //Checking if channel is in the expected value beor triggering the exception.
            //TODO: Should then not base the wait in monitor?
            try{
                T v = channel.get(true);
                if (comparator != null) {
                    if (comparator.compare(v, value)==0){
                        return v;
                    }
                } else {
                    if (v.equals(value)){
                        return v;
                    }
                }
            } catch (Exception e){                
            }
            throw ex;
        } 
    }

    public static <T extends Number> T waitValue(String channelName, T value, double precision, Integer timeout, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Number value required");
        }
        if ((type != null) && (!Number.class.isAssignableFrom(type))) {
            throw new IllegalArgumentException("Must be a number type");
        }
        return waitValue(channelName, value, new NumberComparator<T>(precision), timeout, type, size);
    }           
    
    public static <T extends Number> T waitValue(Channel<T> channel, T value, double precision, Integer timeout) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Number value required");
        }       
        return waitValue(channel, value, new NumberComparator<T>(precision), timeout);
    }    

    public static void put(String channelName, Object value) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        put(channelName, value, null);
    }

    public static void put(String channelName, Object value, Integer timeout) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        Channel channel = newChannel(channelName, value.getClass());
        try {
            if (timeout != null) {
                channel.setValueAsync(value).get(timeout, TimeUnit.MILLISECONDS);
            } else {
                channel.setValue(value);
            }
        } finally {
            closeChannel(channel);
        }
    }

    public static void putq(String channelName, Object value) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        Channel channel = newChannel(channelName, value.getClass());
        try {
            channel.setValueNoWait(value);
        } finally {
            closeChannel(channel);
        }
    }

    public static <T> Class<T> getDefaultType(String channelName) {
        DefaultChannelService factory = getChannelFactory();
        try {
            return factory.getDefaultType(channelName);
        } catch (Exception ex) {
            return null;
        }
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type) {
        return newChannelDevice(name, channelName, type, true);
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped) {
        return newChannelDevice(name, channelName, type, timestamped, EpicsRegister.UNDEFINED_PRECISION);
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision) {
        return newChannelDevice(name, channelName, type, timestamped, EpicsRegister.UNDEFINED_PRECISION, EpicsRegister.SIZE_MAX);
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision, int size) {
        return newChannelDevice(name, channelName, type, timestamped, EpicsRegister.UNDEFINED_PRECISION, size, timestamped ? getDefaultInvalidValueAction() : null); ////By default, if not timestamped, request only value data
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision, int size, InvalidValueAction invalidAction) {
        if (type == null) {
            type = getDefaultType(channelName);
            if (type == null) {
                type = Double.class;
            }
        }
        if (type == byte[].class) {
            return new ChannelByteArray(name, channelName, size, timestamped, invalidAction);
        }
        if (type == short[].class) {
            return new ChannelShortArray(name, channelName, size, timestamped, invalidAction);
        }
        if (type == int[].class) {
            return new ChannelIntegerArray(name, channelName, size, timestamped, invalidAction);
        }
        if (type == float[].class) {
            return new ChannelFloatArray(name, channelName, precision, size, timestamped, invalidAction);
        }
        if (type == double[].class) {
            return new ChannelDoubleArray(name, channelName, precision, size, timestamped, invalidAction);
        }

        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        if (type == Byte.class) {
            return new ChannelByte(name, channelName, timestamped, invalidAction);
        }
        if (type == Short.class) {
            return new ChannelShort(name, channelName, timestamped, invalidAction);
        }
        if (type == Integer.class) {
            return new ChannelInteger(name, channelName, timestamped, invalidAction);
        }
        if (type == Float.class) {
            return new ChannelFloat(name, channelName, precision, timestamped, invalidAction);
        }
        if (type == Double.class) {
            return new ChannelDouble(name, channelName, precision, timestamped, invalidAction);
        }
        if (type == String.class) {
            return new ChannelString(name, channelName, timestamped, invalidAction);
        }
        throw new RuntimeException("Invalid channel type");
    }
    
    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision, int size, InvalidValueAction invalidAction, boolean unsigned) {
        EpicsRegister ret = newChannelDevice(name, channelName, type, timestamped, precision, size, invalidAction);
        if (ret!=null){
            ret.setElementUnsigned(unsigned);
        }
        return ret;
    }

    public static Class getChannelType(String typeId) throws ClassNotFoundException {
        if (typeId == null) {
            return null;
        }
        switch (typeId) {
            case "b":
                return Byte.class;
            case "i":
                return Short.class;
            case "l":
                return Integer.class;
            case "f":
                return Float.class;
            case "d":
                return Double.class;
            case "s":
                return String.class;
            case "[b":
                return byte[].class;
            case "[i":
                return short[].class;
            case "[l":
                return int[].class;
            case "[f":
                return float[].class;
            case "[d":
                return double[].class;
            case "[s":
                return String[].class;
        }
        return Class.forName(typeId);
    }

    public static Class getChannelType(String typeId, boolean requestMetadata) throws ClassNotFoundException {
        Class ret = getChannelType(typeId);
        return requestMetadata ? getMetadataChannelType(ret) : ret;
    }

    public static Class getMetadataChannelType(Class type) {
        if (type != null) {
            if (type == Byte.class) {
                return ch.psi.jcae.impl.type.ByteTimestamp.class;
            }
            if (type == Short.class) {
                return ch.psi.jcae.impl.type.ShortTimestamp.class;
            }
            if (type == Integer.class) {
                return ch.psi.jcae.impl.type.IntegerTimestamp.class;
            }
            if (type == Float.class) {
                return ch.psi.jcae.impl.type.FloatTimestamp.class;
            }
            if (type == Double.class) {
                return ch.psi.jcae.impl.type.DoubleTimestamp.class;
            }
            if (type == String.class) {
                return ch.psi.jcae.impl.type.StringTimestamp.class;
            }
            if (type == byte[].class) {
                return ch.psi.jcae.impl.type.ByteArrayTimestamp.class;
            }
            if (type == short[].class) {
                return ch.psi.jcae.impl.type.ShortArrayTimestamp.class;
            }
            if (type == int[].class) {
                return ch.psi.jcae.impl.type.IntegerArrayTimestamp.class;
            }
            if (type == float[].class) {
                return ch.psi.jcae.impl.type.FloatArrayTimestamp.class;
            }
            if (type == double[].class) {
                return ch.psi.jcae.impl.type.DoubleArrayTimestamp.class;
            }
            if (type == String[].class) {
                return ch.psi.jcae.impl.type.StringArrayTimestamp.class;
            }
        }
        throw new RuntimeException("Invalid channel type");
    }

    public static InvalidValueAction getDefaultInvalidValueAction() {
        return defaultInvalidValueAction;
    }

    public static void setDefaultInvalidValueAction(InvalidValueAction value) {
        defaultInvalidValueAction = value;
    }
}
