package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.SettlingCondition;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class ChannelSettlingCondition extends SettlingCondition{
    
    public ChannelSettlingCondition(String channelName, Object value) {
        this(channelName, value, null);
    }

    public ChannelSettlingCondition(String channelName, Object value, Integer timeout) {
        this(channelName, value, timeout, null);
    }

    public ChannelSettlingCondition(String channelName, Object value, Integer timeout, String type) {
        this(channelName, value, timeout, type, null);
    }

    public ChannelSettlingCondition(String channelName, Object value, Integer timeout, String type, Integer size) {
        this(channelName, value, timeout, type, size, null);
    }

    public ChannelSettlingCondition(String channelName, Object value, Integer timeout, String type, Integer size, Comparator comparator) {
        this.channelName = channelName;
        this.value = value;
        this.timeout = timeout;
        this.type = type;
        this.size = size;
        this.comparator = comparator;
    }
    final public String channelName;
    final public Object value;
    final public Integer timeout;
    final public String type;
    final public Integer size;
    final public Comparator comparator;

    @Override
    protected void doWait() throws IOException, InterruptedException {
        try {
            Epics.waitValue(channelName, value, comparator, timeout, Epics.getChannelType(type), size);
        } catch (java.util.concurrent.TimeoutException | ChannelException | ExecutionException | ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }    

}
