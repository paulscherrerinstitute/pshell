package ch.psi.pshell.epics;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelDescriptor;
import ch.psi.jcae.ChannelException;
import ch.psi.jcae.ChannelService;
import ch.psi.jcae.DummyChannelDescriptor;
import ch.psi.jcae.impl.DefaultChannelService;
import ch.psi.jcae.impl.JcaeProperties;
import ch.psi.utils.Convert;
import java.util.Comparator;
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

    public static void create() {
        destroy();
        if (System.getProperty(PROPERTY_JCAE_CONFIG_FILE) == null) {
            System.setProperty(PROPERTY_JCAE_CONFIG_FILE, "jcae.properties");
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
        synchronized (creationLock) {
            if (factory.isDryrun()) {
                return getChannelFactory().createChannel(new DummyChannelDescriptor(descriptor.getType(), descriptor.getName(), descriptor.getMonitored(), descriptor.getSize()));
            }
            return getChannelFactory().createChannel(descriptor);
        }
    }

    public static <T> Channel<T> newChannel(String name, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException {
        DefaultChannelService factory = getChannelFactory();
        if (type == null) {
            try {
                type = factory.getDefaultType(name);
            } catch (Exception ex) {
            }
        }
        ChannelDescriptor descriptor = new ChannelDescriptor<>(type, name, false, size);
        return newChannel(descriptor);
    }

    public static <T> Channel<T> newChannel(String name, Class<T> type) throws ChannelException, InterruptedException, TimeoutException {
        return newChannel(name, type, null);
    }

    public static void closeChannel(Channel channel) {
        synchronized (creationLock) {
            try {
                channel.destroy();
            } catch (Exception ex) {
                Logger.getLogger(Epics.class.getName()).log(Level.FINER, null, ex);
            }
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

    public static <T> T waitValue(String channelName, T value, Comparator<T> comparator, Integer timeout, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        Channel<T> channel = newChannel(channelName, type, size);
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
        } finally {
            closeChannel(channel);
        }
    }

    public static <T extends Number> T waitValue(String channelName, T value, double precision, Integer timeout, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException, ExecutionException {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Number value required");
        }
        if ((type != null) && (!Number.class.isAssignableFrom(type))) {
            throw new IllegalArgumentException("Must be a number type");
        }
        Comparator<T> comparator = (T o1, T o2) -> {
            if ((o1 == null) && (o2 == null)) {
                return 0;
            }
            if ((o1 == null) || (o2 == null)) {
                return 1;
            }
            if (Math.abs(o1.doubleValue() - o2.doubleValue()) <= Math.abs(precision)) {
                return 0;
            }
            return Double.valueOf(o1.doubleValue()).compareTo(o2.doubleValue());
        };

        return waitValue(channelName, value, comparator, timeout, type, size);
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

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type) {
        return newChannelDevice(name, channelName, type, true);
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped) {
        return newChannelDevice(name, channelName, type, timestamped, -1);
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision) {
        return newChannelDevice(name, channelName, type, timestamped, -1, -1);
    }
    
    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision, int size) {
        return newChannelDevice(name, channelName, type, timestamped, -1, -1, timestamped ? getDefaultInvalidValueAction() : null); ////By default, if not timestamped, request only value data
    }

    public static EpicsRegister newChannelDevice(String name, String channelName, Class type, boolean timestamped, int precision, int size, InvalidValueAction invalidAction) {
        DefaultChannelService factory = getChannelFactory();
        if (type == null) {
            try {
                type = factory.getDefaultType(channelName);
            } catch (Exception ex) {
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
    
    
    public static InvalidValueAction getDefaultInvalidValueAction() {
        return defaultInvalidValueAction;
    }
    
    public static void setDefaultInvalidValueAction(InvalidValueAction value) {
        defaultInvalidValueAction = value;
    }
}
