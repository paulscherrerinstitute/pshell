package ch.psi.pshell.bs;

import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.Readable;
import java.io.IOException;
import ch.psi.bsread.Receiver;
import ch.psi.bsread.message.Message;
import ch.psi.bsread.ReceiverConfig;
import ch.psi.bsread.message.ValueImpl;
import ch.psi.pshell.device.Device;
import ch.psi.bsread.converter.MatlabByteConverter;
import ch.psi.pshell.device.Cacheable;
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.utils.Arr;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.Threading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * A device implementing a beam synchronous string, having, for each identifier,
 * a corresponding Scalar or Waveform child.
 */
public class Stream extends DeviceBase implements Readable<StreamValue>, Cacheable<StreamValue> {

    public static final int TIMEOUT_START_STREAMING = 10000;

    Thread thread;
    final Map<String, Scalar> channels;
    final List<String> channelNames;
    final List<Readable> readables;
    static MatlabByteConverter converter;
    volatile boolean reading;
    volatile AtomicBoolean started = new AtomicBoolean(false);
    volatile AtomicBoolean closing = new AtomicBoolean(false);
    Receiver receiver;
    Boolean fixedChildren;
    final Boolean privateProvider;

    @Override
    public StreamConfig getConfig() {
        return (StreamConfig) super.getConfig();
    }

    public String getAddress() {
        return ((Provider) getParent()).getAddress();
    }

    public int getSocketType() {
        return ((Provider) getParent()).getSocketType();
    }

    public Stream(String name, boolean persisted) {
        this(name, null, persisted);
    }

    /**
     * If provider is null then uses default provider.
     */
    public Stream(String name, Provider provider, boolean persisted) {
        super(name, persisted ? new StreamConfig() : null);
        if (converter == null) {
            converter = new MatlabByteConverter();
        }
        if (provider == null) {
            provider = Provider.getOrCreateDefault();
            privateProvider = (provider != Provider.getDefault());
        } else {
            privateProvider = false;
        }

        setParent(provider);
        channels = new HashMap<>();
        channelNames = new ArrayList<>();
        readables = new ArrayList<>();
        setMonitored(true);
        if (persisted) {
            setFilter(getConfig().filter);
            ArrayList<ScalarConfig> cfg = getConfig().getChannels();
            for (ScalarConfig cc : cfg) {
                if (cc != null) {
                    if (cc.id.startsWith("[")) {
                        addChild(new Waveform(cc.id.substring(1), this, cc.id.substring(1), cc.precision, cc.offset));
                    } else {
                        addChild(new Scalar(cc.id, this, cc.id, cc.precision, cc.offset));
                    }
                }
            }
        }
    }

    public Stream(String name) {
        this(name, (Provider) null);
    }

    public Stream(String name, Provider provider) {
        this(name, provider, false);
    }

    public Stream(String name, String filter) {
        this(name, null, filter);
    }

    public Stream(String name, Provider provider, String filter) {
        this(name, provider);
        setFilter(filter);
    }

    public Stream(String name, Scalar... channels) {
        this(name, null, channels);
    }

    public Stream(String name, Provider provider, Scalar... channels) {
        this(name, provider, false);
        setChildren(channels);
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        stop();
        super.doInitialize();
        channels.clear();
        channelNames.clear();
        readables.clear();
        readables.add(this);
        for (Device dev : getChildren()) {
            if (dev instanceof Scalar) {
                appendChild((Scalar) dev);
            }
        }
        //fixed by first initialize
        if (fixedChildren == null) {
            fixedChildren = Arr.containsClass(getChildren(), Scalar.class);
        }
    }

    void appendChild(Scalar c) {
        channels.put(c.getId(), c);
        channelNames.add(c.getId());
        readables.add(c);
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (getState() == State.Busy) {
            read();
        } else if (getState() == State.Ready) {
            start(false);
            try {
                read();
            } finally {
                stop();
            }
        }
        super.doUpdate();
    }

    boolean debug;

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean value) {
        debug = value;
    }

    public Scalar addScalar(String name, String id, int modulo, int offset) throws IOException, InterruptedException {
        assertStateNot(State.Busy);
        Scalar scalar = new Scalar(name, this, id, modulo, offset);
        addChild(scalar);
        if (isInitialized()) {
            doInitialize();
        }
        return scalar;
    }

    public Waveform addWaveform(String name, String id, int modulo, int offset) throws IOException, InterruptedException {
        return addWaveform(name, id, modulo, offset, -1);
    }

    public Waveform addWaveform(String name, String id, int modulo, int offset, int size) throws IOException, InterruptedException {
        assertStateNot(State.Busy);
        Waveform waveform = new Waveform(name, this, id, modulo, offset, size);
        addChild(waveform);
        if (isInitialized()) {
            doInitialize();
        }
        return waveform;
    }

    public Matrix addMatrix(String name, String id, int modulo, int offset, int width, int height) throws IOException, InterruptedException {
        assertStateNot(State.Busy);
        Matrix matrix = new Matrix(name, this, id, modulo, offset, width, height);
        addChild(matrix);
        if (isInitialized()) {
            doInitialize();
        }
        return matrix;
    }

    void receiverTask() {
        try {
            getLogger().finer("Entering reveiver task");

            int minModulo = 100;
            for (String channel : channels.keySet()) {
                minModulo = Math.min(minModulo, channels.get(channel).getModulo());
            }

            for (FilterCondition filterCondition : filterConditions) {
                if (!Arr.containsEqual(channels.keySet().toArray(), filterCondition.id)) {
                    getLogger().info("Adding filter condition id to stream: " + filterCondition.id);
                    channels.put(filterCondition.id, new Scalar(filterCondition.id, this, filterCondition.id, minModulo, 0));
                }
            }

            ((Provider) getParent()).createStream(this);
            onStart();
            ReceiverConfig config = ((Provider) getParent()).getReceiverConfig(this);
            ArrayList<ch.psi.bsread.configuration.Channel> channelsConfig = new ArrayList<>();
            for (String channel : channels.keySet()) {
                Scalar c = channels.get(channel);
                ch.psi.bsread.configuration.Channel channelConfig = new ch.psi.bsread.configuration.Channel(
                        c.getId(), c.getModulo(), c.getOffset());
                channelsConfig.add(channelConfig);
            }
            config.setRequestedChannels(channelsConfig);
            getLogger().fine("Connecting to: " + config.getAddress() + " (" + config.getSocketType() + ")");
            receiver = new Receiver(config);
            receiver.connect();

            while (!Thread.currentThread().isInterrupted() && started.get()) {
                Message msg = receiver.receive();
                if (msg == null) {
                    started.set(false);
                } else {
                    if (isMonitored() || reading) {
                        Map<String, ValueImpl> data = msg.getValues();
                        if (data != null) {
                            if (checkFilter(data)) {
                                reading = false;
                                long pulseId = msg.getMainHeader().getPulseId();
                                long timestamp = msg.getMainHeader().getGlobalTimestamp().getAsMillis();
                                long nanosOffset = msg.getMainHeader().getGlobalTimestamp().getNs() % 1000000L;
                                onMessage(pulseId, timestamp, nanosOffset, data);
                            }
                        }
                    }
                }
            }
            if (started.get() == false) {
                getLogger().finer("Receiver was closed");
            } else {
                getLogger().finer("Receiver thread was interrupted");
            }
            onStop(null);
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
            onStop(ex);
        } finally {
            reading = false;
            closeReceiver();
            closeStream();
            getLogger().fine("Quitting receiver task");
            setState(State.Ready);
        }
    }

    public void start() throws IOException, InterruptedException {
        start(null);
    }

    public void start(Boolean async) throws IOException, InterruptedException {
        if (async != null) {
            setMonitored(async);
        }
        if (started.compareAndSet(false, true)) {
            thread = new Thread(() -> {
                receiverTask();
            });
            thread.setName("Stream receiver: " + getName());
            thread.setDaemon(true);
            setState(State.Busy);
            thread.start();
        }
    }

    public void stop() throws IOException {
        started.set(false);
        closeReceiver();
        if (thread != null) {
            try {
                long start = System.currentTimeMillis();
                while (thread.isAlive()) {
                    if (System.currentTimeMillis() - start > 1000) {
                        getLogger().log(Level.WARNING, "Receiver did't quit: interrupting thread");
                        //TODO: Killing thread because it blocks  if no message is received
                        Threading.stop(thread, true, 2000);
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                //TODO: Filtering InterruptedException. But stop() should not throw InterruptedException;
                getLogger().log(Level.WARNING, null, ex);
            }
            thread = null;
        }
    }

    void closeReceiver() {
        if (closing.compareAndSet(false, true)) {
            try {
                if (receiver != null) {
                    getLogger().log(Level.FINE, "Closing receiver");
                    try {
                        receiver.close();
                    } catch (Exception ex) {
                        getLogger().log(Level.WARNING, null, ex);
                    }
                    receiver = null;
                }
            } finally {
                closing.compareAndSet(true, false);
            }
        }
    }

    void closeStream() {
        try {
            ((Provider) getParent()).closeStream(this);
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
        }
    }

    protected void onStart() {
    }

    protected void onStop(Exception ex) {
    }

    @Override
    public StreamValue read() throws IOException, InterruptedException {
        assertState(State.Busy);
        Object cache = take();
        reading = true;
        while (true) {
            try {
                waitValueNot(cache, 100);
                break;
            } catch (DeviceTimeoutException ex) {
                assertState(State.Busy);
            }
        }
        return take();
    }

    @Override
    protected boolean hasChanged(Object value, Object former) {
        return value != former;
    }

    String filter;
    ArrayList<FilterCondition> filterConditions = new ArrayList<>();

    @Override
    public StreamValue take() {
        return (StreamValue) super.take();
    }

    @Override
    public StreamValue request() {
        return (StreamValue) super.request();
    }

    enum FilterOp {
        equal,
        notEqual,
        less,
        greater,
        greaterOrEqual,
        lessOrEqual
    }

    class FilterCondition {

        String id;
        FilterOp op;
        Object value;

        FilterCondition(String str) throws InvalidValueException {
            try {
                String aux = null;
                if (str.contains("==")) {
                    aux = "==";
                    op = FilterOp.equal;
                } else if (str.contains("!=")) {
                    aux = "!=";
                    op = FilterOp.notEqual;
                } else if (str.contains(">=")) {
                    aux = ">=";
                    op = FilterOp.greaterOrEqual;
                } else if (str.contains("<=")) {
                    aux = "<=";
                    op = FilterOp.lessOrEqual;
                } else if (str.contains(">")) {
                    aux = ">";
                    op = FilterOp.greater;
                } else if (str.contains("<")) {
                    aux = "<";
                    op = FilterOp.less;
                }
                String[] tokens = str.split(aux);
                id = tokens[0].trim();
                aux = tokens[1].trim();
                if ((aux.startsWith("\"") && aux.endsWith("\"")) || (aux.startsWith("'") && aux.endsWith("'"))) {
                    value = aux.substring(1, aux.length() - 1);
                } else if (aux.equalsIgnoreCase("false")) {
                    value = Boolean.FALSE;
                } else if (aux.equalsIgnoreCase("true")) {
                    value = Boolean.TRUE;
                } else {
                    value = Double.valueOf(aux);
                }
            } catch (Exception ex) {
                throw new InvalidValueException(str);
            }
        }

        boolean check(Comparable c) {
            if (c instanceof Number) {
                c = (Double) (((Number) c).doubleValue());
            }
            switch (op) {
                case equal:
                    return c.compareTo(value) == 0;
                case notEqual:
                    return c.compareTo(value) != 0;
                case greater:
                    return c.compareTo(value) > 0;
                case less:
                    return c.compareTo(value) < 0;
                case greaterOrEqual:
                    return c.compareTo(value) >= 0;
                case lessOrEqual:
                    return c.compareTo(value) <= 0;
            }
            return false;
        }
    }

    public final void setFilter(String filter) throws InvalidValueException {
        this.filter = null;
        filterConditions.clear();
        if (filter != null) {
            try {
                for (String token : filter.split(" AND ")) {
                    filterConditions.add(new FilterCondition(token));
                }
            } catch (InvalidValueException ex) {
                filterConditions.clear();
                throw ex;
            }
            this.filter = filter;
        }
    }

    public String getFilter() {
        return filter;
    }

    public boolean checkFilter(Map<String, ValueImpl> data) {
        if (filter != null) {
            try {
                for (FilterCondition filterCondition : filterConditions) {
                    ValueImpl v = data.get(filterCondition.id);
                    Comparable val = (Comparable) v.getValue();
                    if (!filterCondition.check(val)) {
                        return false;
                    }
                }
            } catch (Exception ex) {
                return false;
            }
        }
        return true;
    }

    protected void onMessage(long pulse_id, long timestamp, long nanosOffset, Map<String, ValueImpl> data) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        for (String channel : data.keySet()) {
            Scalar c = channels.get(channel);
            ValueImpl v = data.get(channel);
            Object val = v.getValue();
            try {
                if ((!fixedChildren) && (c == null) && (val != null)) {
                    c = (val.getClass().isArray()) ? new Waveform(channel, this, channel) : new Scalar(channel, this, channel);
                    appendChild(c);
                    try {
                        c.initialize();
                    } catch (Exception ex) {
                    }
                }

                if (c != null) {
                    c.set(pulse_id, timestamp, nanosOffset, val);
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
                if (c != null) {
                    c.set(pulse_id, timestamp, nanosOffset, null);
                }
            }
            if (debug) {
                System.out.println(channel + ": " + Str.toString(val, 100));
            }
            names.add(channel);
            values.add(val);
        }

        if (pidReader != null) {
            setCache((DeviceBase) pidReader, (Object) pulse_id, timestamp);
        }
        if (timestampReader != null) {
            setCache((DeviceBase) timestampReader, (Object) timestamp, timestamp);
        }

        if (fixedChildren) {
            //If ids are declared, value list is fixed and ordered 
            setCache(new StreamValue(pulse_id, timestamp, nanosOffset, channelNames, Arrays.asList(getChildrenValues())), timestamp, nanosOffset);
        } else {
            //Else caches everything received
            setCache(new StreamValue(pulse_id, timestamp, nanosOffset, names, values), timestamp, nanosOffset);
        }
    }

    public List<Readable> getReadables() {
        return readables;
    }

    StreamValue getCurrentValue() {
        StreamValue cache = take();
        if (cache == null) {
            throw new RuntimeException("No stream data");
        }
        return cache;
    }

    public List<String> getIdentifiers() {
        return getCurrentValue().getIdentifiers();
    }

    public List getValues() {

        return getCurrentValue().getValues();
    }

    public Object getValue(String id) {
        return getCurrentValue().getValue(id);
    }

    public Object getValue(int index) {
        return getCurrentValue().getValue(index);
    }

    public static List readChannels(List<String> names) throws IOException, InterruptedException {
        Stream stream = new Stream(null);
        try {
            for (String name : names) {
                stream.addScalar(name, name, 1, 0);
            }
            stream.initialize();
            stream.start();
            stream.waitValueNot(null, 5000);
            return stream.getValues();
        } finally {
            stream.close();
        }
    }

    @Override
    protected void doClose() throws IOException {
        stop();
        channels.clear();
        if (privateProvider) {
            try {
                getParent().close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
        super.doClose();
    }

    public class PidReader extends ReadonlyAsyncRegisterBase<Long> {

        PidReader() {
            super("PID");
            setParent(Stream.this);
        }
    };

    PidReader pidReader;

    public PidReader getPidReader() {
        if (pidReader == null) {
            pidReader = new PidReader();
        }
        return pidReader;
    }

    public class TimestampReader extends ReadonlyAsyncRegisterBase<Long> {

        TimestampReader() {
            super("Timestamp");
            setParent(Stream.this);
        }
    };

    TimestampReader timestampReader;

    public TimestampReader getTimestampReader() {
        if (timestampReader == null) {
            timestampReader = new TimestampReader();
        }
        return timestampReader;
    }
}
