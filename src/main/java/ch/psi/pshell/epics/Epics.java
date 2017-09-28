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

    public static ChannelService getChannelFactory() {
        if (factory == null) {
            throw new RuntimeException("Epics context not initialized");
        }
        return factory;
    }

    final static Object creationLock = new Object();

    public static Channel newChannel(ChannelDescriptor descriptor) throws ChannelException, InterruptedException, TimeoutException {
        synchronized (creationLock) {
            if (factory.isDryrun()) {
                return getChannelFactory().createChannel(new DummyChannelDescriptor(descriptor.getType(), descriptor.getName(), descriptor.getMonitored(), descriptor.getSize()));
            }
            return getChannelFactory().createChannel(descriptor);
        }
    }

    public static <T> Channel<T> newChannel(String name, Class<T> type, Integer size) throws ChannelException, InterruptedException, TimeoutException {
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
                    return channel.waitForValue(value, comparator, timeout);
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
        if (type == null) {
            type = Double.class;
        }
        if (type == byte[].class) {
            return new ChannelByteArray(name, channelName);
        }
        if (type == short[].class) {
            return new ChannelShortArray(name, channelName);
        }
        if (type == int[].class) {
            return new ChannelIntegerArray(name, channelName);
        }
        if (type == float[].class) {
            return new ChannelFloatArray(name, channelName);
        }
        if (type == double[].class) {
            return new ChannelDoubleArray(name, channelName);
        }

        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        if (type == Byte.class) {
            return new ChannelByte(name, channelName);
        }
        if (type == Short.class) {
            return new ChannelShort(name, channelName);
        }
        if (type == Integer.class) {
            return new ChannelInteger(name, channelName);
        }
        if (type == Float.class) {
            return new ChannelFloat(name, channelName);
        }
        if (type == Double.class) {
            return new ChannelDouble(name, channelName);
        }
        if (type == String.class) {
            return new ChannelString(name, channelName);
        }
        throw new RuntimeException("Invalid channel type");
    }
}
