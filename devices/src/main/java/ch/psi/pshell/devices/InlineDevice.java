package ch.psi.pshell.devices;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamChannel;
import ch.psi.pshell.camserver.PipelineSource;
import ch.psi.pshell.device.ArrayAverager;
import ch.psi.pshell.device.ArrayRegisterStats;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.Averager.RegisterStats;
import ch.psi.pshell.device.Cacheable;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.epics.EpicsRegister;
import ch.psi.pshell.epics.InvalidValueAction;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Str;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Dynamic resolved devices, according to URL (protocol://name). Can be passed
 * as argument to scans.
 */
public class InlineDevice extends DeviceBase implements Readable, Writable {

    final String url;
    final String protocol;
    final String name;
    final String id;
    final Map<String, String> pars;
    final List<Device> instantiatedDevices = new ArrayList<>();

    DeviceBase parent;
    Device device;
    String givenName;

    static List<Stream> cameraStreams = new ArrayList<>();

    public InlineDevice(String url) {
        this(null, url);
    }

    public InlineDevice(String name, String url) {
        this.url = url;
        this.protocol = getUrlProtocol(url);
        this.pars = getUrlPars(url);
        this.id = getUrlId(url);
        this.name = (name == null) ? getUrlName(url) : name;
        givenName = (pars.containsKey("name") || (name != null)) ? this.name : null;
    }            

    public static String getUrlProtocol(String url) {
        if (!url.contains("://")) {
            throw new RuntimeException("Invalid device url: " + url);
        }
        return url.substring(0, url.indexOf("://"));
    }

    public static String getUrlPath(String url) {
        if (!url.contains("://")) {
            throw new RuntimeException("Invalid device url: " + url);
        }
        return url.substring(url.indexOf("://") + 3, url.length());
    }

    public static Map<String, String> getUrlPars(String url) {
        String path = getUrlPath(url);
        Map<String, String> pars = new HashMap<>();
        if (path.contains("?")) {
            for (String str : path.split("\\?")[1].split("&")) {
                if (str.contains("=")) {
                    pars.put(str.substring(0, str.indexOf("=")), str.substring(str.indexOf("=") + 1, str.length()));
                } else {
                    pars.put(str, null);
                }
            }
        }
        return pars;
    }

    public static String getUrlName(String url) {
        Map<String, String> pars = getUrlPars(url);
        return pars.containsKey("name") ? pars.get("name") : getUrlId(url);
    }

    static String getUrlId(String url) {
        String path = getUrlPath(url);
        if (path.contains("?")) {
            path = path.split("\\?")[0].trim();
        }
        return path;
    }

    public static String getDeviceName(String url) {
        Map<String, String> pars = getUrlPars(url);
        if (pars.containsKey("name")) {
            return pars.get("name");
        }
        if (getUrlProtocol(url).equals("cs")) {
            if (pars.get("channel") != null) {
                return pars.get("channel");
            }
        }
        return getUrlId(url);
    }

    @Override
    public void setParent(DeviceBase parent) {
        this.parent = parent;
    }

    @Override
    public DeviceBase getParent() {
        return parent;
    }

    @Override
    public void doInitialize() throws IOException, InterruptedException {
        closeDevice();
        device = resolve();
        if (device != null) {
            Device source = getSourceDevice(device);
            if (source != null) {
                if (!source.isInitialized()) {
                    if (Setup.isSimulation()) {
                        source.setSimulated();
                    }
                    source.initialize();
                }
            }
        }
    }

    public static Device getSourceDevice(Device device) {
        if (device instanceof RegisterStats) {
            device = device.getParent();
        }
        if ((device instanceof Averager)
                || (device instanceof ArrayAverager)
                || (device instanceof ArrayRegisterStats)) {
            device = device.getParent();
        }
        return device;
    }

    public static String getChannelName(Object device) {

        if (device instanceof Cacheable.CacheReadable cacheReadable) {
            Cacheable parent = cacheReadable.getParent();
            if ((parent != null) && (parent instanceof Device dev)) {
                device = dev;
            }
        }
        if (device instanceof Device dev) {
            device = getSourceDevice(dev);
            try {
                return (String) device.getClass().getMethod("getChannelName").invoke(device);
            } catch (Exception ex) {
            }
            try {
                return (String) device.getClass().getMethod("getChannelName").invoke(device);
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public Device getDevice() {
        return device;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map getPars() {
        return pars;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void write(Object value) throws IOException, InterruptedException {
        if (device instanceof Writable writable) {
            writable.write(value);
        }
    }

    @Override
    public Object read() throws IOException, InterruptedException {
        if (device instanceof Readable readable) {
            return readable.read();
        }
        return null;
    }

    protected Device resolve() throws IOException, InterruptedException {
        Device ret = null;
        switch (protocol) {
            case "dev":
                if (DevicePool.hasInstance()){
                    return DevicePool.getInstance().getByName(name, Device.class);
                }                
                break;
            case "pv":
            case "ca":
                boolean timestamped = true;
                int precision = -1;
                int size = -1;
                Class type = null;
                InvalidValueAction invalidValueAction = Epics.getDefaultInvalidValueAction();
                try {
                    size = Integer.valueOf(pars.get("size"));
                } catch (Exception ex) {
                }
                try {
                    precision = Integer.valueOf(pars.get("precision"));
                } catch (Exception ex) {
                }
                try {
                    type = Epics.getChannelType(pars.get("type"));
                } catch (Exception ex) {
                }
                if (pars.containsKey("invalid")) {
                    try {
                        if (Boolean.valueOf(pars.get("invalid"))) {
                            invalidValueAction = InvalidValueAction.None;
                        }
                    } catch (Exception ex) {
                    }
                }

                ret = Epics.newChannelDevice(name, id, type, timestamped, precision, size, invalidValueAction);
                boolean blocking = true;
                if (pars.containsKey("blocking")) {
                    try {
                        blocking = Boolean.valueOf(pars.get("blocking"));
                    } catch (Exception ex) {
                    }
                }
                ((EpicsRegister) ret).setBlockingWrite(blocking);
                break;

            case "bs":
                if (this.id.equals("PID")) {
                    ret = ((Stream) getParent()).getPidReader();
                    if (pars.containsKey("filter")) {
                        ((Stream) getParent()).setFilter(pars.get("filter"));
                    }
                } else if (this.id.equals("Timestamp")) {
                    ret = ((Stream) getParent()).getTimestampReader();
                } else {
                    //Use existing channel if already defined
                    for (Device d : ((Stream) getParent()).getChildren()) {
                        if (d instanceof StreamChannel streamChannel) {
                            if (String.valueOf(id).equals(streamChannel.getId())) {
                                ret = d;
                            }
                        }
                    }
                    if (ret == null) {
                        int modulo = StreamChannel.DEFAULT_MODULO;
                        int offset = StreamChannel.DEFAULT_OFFSET;
                        boolean waveform = false;
                        boolean matrix = false;
                        int sz = -1;
                        int width = -1;
                        int height = -1;
                        if ("true".equalsIgnoreCase(pars.get("waveform"))) {
                            waveform = true;
                        }
                        if ("true".equalsIgnoreCase(pars.get("matrix"))) {
                            matrix = true;
                        }                        
                        try {
                            sz = Integer.valueOf(pars.get("size"));
                            waveform = true;
                        } catch (Exception ex) {
                        }
                        try {
                            width = Integer.valueOf(pars.get("width"));
                        } catch (Exception ex) {
                        }
                        try {
                            height = Integer.valueOf(pars.get("height"));
                        } catch (Exception ex) {
                        }
                        try {
                            modulo = Integer.valueOf(pars.get("modulo"));
                        } catch (Exception ex) {
                        }
                        try {
                            offset = Integer.valueOf(pars.get("offset"));
                        } catch (Exception ex) {
                        }
                        if (matrix){
                            ret = ((Stream) getParent()).addMatrix(name, id, modulo, offset);
                        }
                        else if ((width >= 0) && (height >= 0)) {
                            ret = ((Stream) getParent()).addMatrix(name, id, modulo, offset, width, height);
                        } else if (waveform) {
                            if (sz >= 0) {
                                ret = ((Stream) getParent()).addWaveform(name, id, modulo, offset, sz);
                            } else {
                                ret = ((Stream) getParent()).addWaveform(name, id, modulo, offset);
                            }
                        } else {
                            ret = ((Stream) getParent()).addScalar(name, id, modulo, offset);
                        }
                    }
                }
                break;
            case "cs":
                String url = id.trim();
                //If configured as in stripchart translate it 
                if ((pars.get("channel") == null) && (url.contains(" "))) {
                    pars.put("channel",url.substring(url.lastIndexOf(" ") + 1));
                    url = Str.replaceLast(url, " ", "?channel=");
                }               
                if (!url.startsWith("tcp://")) {
                    String instanceName = null; 
                    if (url.lastIndexOf("/") >= 0 ){
                        instanceName = url.substring(url.lastIndexOf("/") + 1);
                        url = url.substring(0, url.lastIndexOf("/"));
                    } else {
                        instanceName= url;
                        url = Setup.getPipelineServer();                        
                    }
                    PipelineSource server = new PipelineSource(null, url);
                    try {
                        server.initialize();
                        url = server.getStream(instanceName);
                    } finally {
                        server.close();
                    }
                } else {
                    if (url.contains("?")){
                        url = url.substring(0, url.lastIndexOf("?"));
                    }
                }
                String channel = pars.get("channel");
                Stream s = null;
                url = url.trim();
                synchronized (cameraStreams) {
                    for (Stream cs : cameraStreams.toArray(new Stream[0])) {
                        if (cs.isInitialized()) {
                            if (cs.getAddress().equals(url)) {
                                s = cs;
                                break;
                            }
                        } else {
                            cameraStreams.remove(cs);
                        }
                    }
                    if (s == null) {
                        ch.psi.pshell.bs.Provider p = new ch.psi.pshell.bs.Provider(null, url, false);
                        s = new Stream(null, p);
                        cameraStreams.add(s);
                        instantiatedDevices.add(s);
                        p.initialize();
                        s.start();
                        s.waitCacheChange(Stream.TIMEOUT_START_STREAMING);
                        synchronized (instantiatedDevices) {
                            instantiatedDevices.add(s);
                            instantiatedDevices.add(p);
                        }
                    }
                    ret = s.getChild(channel);
                }
                break;
        }
        if (ret != null) {
            if (pars.containsKey("monitored")) {
                try {
                    ret.setMonitored(Boolean.valueOf(pars.get("monitored")));
                } catch (Exception ex) {
                }
            }
            try {
                ret.setPolling(Integer.valueOf(pars.get("polling")));
            } catch (Exception ex) {
            }
            try {
                if (Boolean.valueOf(pars.get("simulated")) == true) {
                    ret.setSimulated();
                }
            } catch (Exception ex) {
            }

            //Averaging
            Integer samples = null;
            int interval = -1;
            boolean async = false;
            if (pars.containsKey("samples")) {
                try {
                    samples = Integer.valueOf(pars.get("samples"));
                    async = samples < 0;
                    samples = Math.abs(samples);
                    if (samples < 2) {
                        samples = null;
                    }
                } catch (Exception ex) {
                }
            }
            if (pars.containsKey("interval")) {
                try {
                    interval = Integer.valueOf(pars.get("interval"));
                } catch (Exception ex) {
                }
            }
            if (samples != null) {
                if (ret instanceof ReadableArray readableArray) {
                    boolean integrate = false;
                    if (pars.containsKey("integrate")) {
                        integrate = true;
                    }

                    ArrayAverager av = (givenName != null) ? new ArrayAverager(name, readableArray, samples, interval, integrate)
                            : new ArrayAverager(readableArray, samples, interval, integrate);
                    if (async) {
                        av.setMonitored(true);
                    }
                    if (pars.containsKey("op")) {
                        boolean forceRead = (!av.isMonitored() && !av.isReadOnChangeEvent());
                        switch (pars.get("op")) {
                            case "sum":
                                return av.getSum(givenName).withForceRead(forceRead);
                            case "min":
                                return av.getMin(givenName).withForceRead(forceRead);
                            case "max":
                                return av.getMax(givenName).withForceRead(forceRead);
                            case "mean":
                                return av.getMean(givenName).withForceRead(forceRead);
                            case "stdev":
                                return av.getStdev(givenName).withForceRead(forceRead);
                            case "rms":
                                return av.getRms(givenName).withForceRead(forceRead);
                            case "variance":
                                return av.getVariance(givenName).withForceRead(forceRead);
                            case "samples":
                                return av.getSamples(givenName);
                        }
                    }
                    return av;
                } else {
                    Averager av = (givenName != null) ? new Averager(name, (Readable) ret, samples, interval)
                            : new Averager((Readable) ret, samples, interval);
                    if (async) {
                        av.setMonitored(true);
                    }
                    if (pars.containsKey("op")) {
                        switch (pars.get("op")) {
                            case "sum":
                                return av.getSum(givenName);
                            case "min":
                                return av.getMin(givenName);
                            case "max":
                                return av.getMax(givenName);
                            case "mean":
                                return av.getMean(givenName);
                            case "stdev":
                                return av.getStdev(givenName);
                            case "rms":
                                return av.getRms(givenName);
                            case "variance":
                                return av.getVariance(givenName);
                            case "samples":
                                return av.getSamples(givenName);
                        }
                    }
                    return av;
                }
            } else {
                if ((ret instanceof ReadonlyRegisterArray) || (ret instanceof ReadonlyRegisterMatrix)) {
                    if (pars.containsKey("op")) {
                        ArrayRegisterStats rs = null;
                        if (ret instanceof ReadonlyRegisterArray readonlyRegisterArray) {
                            rs = (givenName != null) ? new ArrayRegisterStats(name,readonlyRegisterArray)
                                    : new ArrayRegisterStats(readonlyRegisterArray);
                        } else if (ret instanceof ReadonlyRegisterMatrix readonlyRegisterMatrix) {
                            rs = (givenName != null) ? new ArrayRegisterStats(name, readonlyRegisterMatrix)
                                    : new ArrayRegisterStats(readonlyRegisterMatrix);
                        }
                        switch (pars.get("op")) {
                            case "sum":
                                return rs.getSum(givenName);
                            case "min":
                                return rs.getMin(givenName);
                            case "max":
                                return rs.getMax(givenName);
                            case "mean":
                                return rs.getMean(givenName);
                            case "stdev":
                                return rs.getStdev(givenName);
                            case "variance":
                                return rs.getVariance(givenName);
                        }
                    }
                }
            }
            return ret;
        }
        throw new IOException("Invalid device: " + url);
    }

    void closeDevice() {
        if (device != null) {
            try {
                if (!"dev".equals(protocol)) {
                    Device dev = device;
                    //closes all parent devices, but not the externally set
                    while ((dev != null) && (!dev.equals(parent))) {
                        dev.close();
                        if (dev == dev.getParent()) {
                            break;
                        }
                        dev = dev.getParent();
                    }
                }
                for (Device id : instantiatedDevices) {
                    id.close();
                    instantiatedDevices.remove(id);
                    if (id instanceof Stream stream){
                        if (cameraStreams.contains(stream)) {
                            cameraStreams.remove(stream);
                        }
                    }
                }
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            device = null;
        }
    }

    @Override
    public void doClose() throws IOException {
        closeDevice();
        super.doClose();
    }

    public static Device create(String url, DeviceBase parent) throws IOException, InterruptedException {
        InlineDevice dev = new InlineDevice(url);
        Stream innerStream = null;
        if (parent == null) {
            if (getUrlProtocol(url).equals("bs")) {
                innerStream = new Stream("Inline device stream");                
                parent = innerStream;
            }
        }
        if (parent != null) {
            dev.setParent(parent);
        }
        dev.initialize();
        if (innerStream != null) {
            Stream finalInnerStream = innerStream;
            dev.getDevice().addListener(new DeviceListener() {
                @Override
                public void onStateChanged(Device device, State state, State former) {
                    if (state==State.Closing){
                        finalInnerStream.close();
                    }                    
                };
            });
            innerStream.initialize();
            innerStream.start();
            innerStream.waitValueChange(Stream.TIMEOUT_START_STREAMING);
        }
        return dev.getDevice();
    }

    public static List<Device> create(List<String> url, DeviceBase parent) throws IOException, InterruptedException {
        List<DeviceBase> parents = new ArrayList<>();
        parents.add(parent);
        return create(url, parents);
    }

    public static List<Device> create(List<String> url, List<DeviceBase> parents) throws IOException, InterruptedException {
        Stream innerStream = null;
        List<Device> ret = new ArrayList<>();
        int firstStreamIndex=0;
        for (int i = 0; i < url.size(); i++) {
            DeviceBase parent = null;
            if (parents.size() > 0) {
                parent = (parents.size() == 1) ? parents.get(0) : parents.get(i);
            }
            if (parent == null) {
                if (getUrlProtocol(url.get(i)).equals("bs")) {
                    if (innerStream == null) {
                        innerStream = new Stream("Inline device stream");                        
                        firstStreamIndex = i;
                    }
                    parent = innerStream;
                }
            }
            Device dev = create(url.get(i), parent);
            ret.add(dev);
        }
        if (innerStream != null) {
            Stream finalInnerStream = innerStream;
            Device dev = ret.get(firstStreamIndex);
            dev.addListener(new DeviceListener() {
                @Override
                public void onStateChanged(Device device, State state, State former) {
                    if (state==State.Closing){
                        finalInnerStream.close();
                    }                    
                };
            });            
            innerStream.initialize();
            innerStream.start();
            innerStream.waitValueChange(Stream.TIMEOUT_START_STREAMING);
        }
        return ret;
    }

}
