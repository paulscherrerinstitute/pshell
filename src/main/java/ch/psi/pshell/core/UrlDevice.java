/*
 *  Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.bs.PipelineServer;
import ch.psi.pshell.bs.Scalar;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.ArrayAverager;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.pshell.device.ArrayRegisterStats;
import ch.psi.pshell.device.Averager.RegisterStats;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.epics.EpicsRegister;
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
public class UrlDevice extends DeviceBase implements Readable, Writable {

    final String url;
    final String protocol;
    final String name;
    final String id;
    final Map<String, String> pars;
    final List<Device> instantiatedDevices = new ArrayList<>();

    DeviceBase parent;
    Device device;

    static List<Stream> cameraStreams = new ArrayList<>();

    public UrlDevice(String url) {
        this.url = url;
        this.protocol = getUrlProtocol(url);
        this.pars = getUrlPars(url);
        this.id = getUrlId(url);
        this.name = getUrlName(url);
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
                    if (Context.getInstance().isSimulation()) {
                        source.setSimulated();
                    }
                    source.initialize();
                }
            }
        }
    }

    public static Device getSourceDevice(Device device) {
        if ((device != null) && (device instanceof RegisterStats)) {
            device = device.getParent();
        }
        if ((device != null) && ((device instanceof Averager)
                || (device instanceof ArrayAverager)
                || (device instanceof ArrayRegisterStats))) {
            device = device.getParent();
        }
        return device;
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
        if ((device != null) && (device instanceof Writable)) {
            ((Writable) device).write(value);
        }
    }

    @Override
    public Object read() throws IOException, InterruptedException {
        if ((device != null) && (device instanceof Readable)) {
            return ((Readable) device).read();
        }
        return null;
    }

    protected Device resolve() throws IOException, InterruptedException {
        Device ret = null;
        switch (protocol) {
            case "dev":
                if (Context.getInstance() != null) {
                    try {
                        ret = (Device) Context.getInstance().getDevicePool().getByName(id);
                    } catch (Exception ex) {
                    }
                }
                break;
            case "pv":
            case "ca":
                boolean timestamped = false;
                int precision = -1;
                int size = -1;
                Class type = null;
                if ("true".equalsIgnoreCase(pars.get("timestamped"))) {
                    timestamped = true;
                }
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

                ret = Epics.newChannelDevice(name, id, type, timestamped, precision, size);
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
                        if (d instanceof Scalar) {
                            if (String.valueOf(id).equals(((Scalar) d).getId())) {
                                ret = d;
                            }
                        }
                    }
                    if (ret == null) {
                        int modulo = Scalar.DEFAULT_MODULO;
                        int offset = Scalar.DEFAULT_OFFSET;
                        boolean waveform = false;
                        int sz = -1;
                        int width = -1;
                        int height = -1;
                        if ("true".equalsIgnoreCase(pars.get("waveform"))) {
                            waveform = true;
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
                        if ((width >= 0) && (height >= 0)) {
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
                String url = id;
                if (!url.startsWith("tcp://")) {
                    String instanceName = url.substring(url.lastIndexOf("/") + 1);
                    url = url.substring(0, url.lastIndexOf("/"));
                    PipelineServer server = new PipelineServer(null, url);
                    try {
                        server.initialize();
                        url = server.getStream(instanceName);
                    } finally {
                        server.close();
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
                        ch.psi.pshell.bs.Provider p = new ch.psi.pshell.bs.Provider(null, url, false, false);
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
        String setName = pars.containsKey("name") ? name : null;
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
                if (ret instanceof ReadableArray) {
                    boolean integrate = false;
                    if (pars.containsKey("integrate")) {
                        integrate = true;
                    }

                    ArrayAverager av = (setName != null) ? new ArrayAverager(name, (ReadableArray) ret, samples, interval, integrate)
                            : new ArrayAverager((ReadableArray) ret, samples, interval, integrate);
                    if (async) {
                        av.setMonitored(true);
                    }
                    if (pars.containsKey("op")) {
                        boolean forceRead = (!av.isMonitored() && !av.isReadOnChangeEvent());
                        switch (pars.get("op")) {
                            case "sum":
                                return av.getSum(setName, forceRead);
                            case "min":
                                return av.getMin(setName, forceRead);
                            case "max":
                                return av.getMax(setName, forceRead);
                            case "mean":
                                return av.getMean(setName, forceRead);
                            case "stdev":
                                return av.getStdev(setName, forceRead);
                            case "variance":
                                return av.getVariance(setName, forceRead);
                            case "samples":
                                return av.getSamples(setName);
                        }
                    }
                    return av;
                } else {
                    Averager av = (setName != null) ? new Averager(name, (Readable) ret, samples, interval)
                            : new Averager((Readable) ret, samples, interval);
                    if (async) {
                        av.setMonitored(true);
                    }
                    if (pars.containsKey("op")) {
                        switch (pars.get("op")) {
                            case "sum":
                                return av.getSum(setName, false);
                            case "min":
                                return av.getMin(setName, false);
                            case "max":
                                return av.getMax(setName, false);
                            case "mean":
                                return av.getMean(setName, false);
                            case "stdev":
                                return av.getStdev(setName, false);
                            case "variance":
                                return av.getVariance(setName, false);
                            case "samples":
                                return av.getSamples(setName, false);
                        }
                    }
                    return av;
                }
            } else {
                if ((ret instanceof ReadonlyRegisterArray) || (ret instanceof ReadonlyRegisterMatrix)) {
                    if (pars.containsKey("op")) {
                        ArrayRegisterStats rs = null;
                        if (ret instanceof ReadonlyRegisterArray) {
                            rs = (setName != null) ? new ArrayRegisterStats(name, (ReadonlyRegisterArray) ret)
                                    : new ArrayRegisterStats((ReadonlyRegisterArray) ret);
                        } else if (ret instanceof ReadonlyRegisterMatrix) {
                            rs = (setName != null) ? new ArrayRegisterStats(name, (ReadonlyRegisterMatrix) ret)
                                    : new ArrayRegisterStats((ReadonlyRegisterMatrix) ret);
                        }
                        switch (pars.get("op")) {
                            case "sum":
                                return rs.getSum(setName);
                            case "min":
                                return rs.getMin(setName);
                            case "max":
                                return rs.getMax(setName);
                            case "mean":
                                return rs.getMean(setName);
                            case "stdev":
                                return rs.getStdev(setName);
                            case "variance":
                                return rs.getVariance(setName);
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
                    if (cameraStreams.contains(id)) {
                        cameraStreams.remove(id);
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
        UrlDevice dev = new UrlDevice(url);
        Stream innerStream = null;
        if (parent == null) {
            if (getUrlProtocol(url).equals("bs")) {
                innerStream = new Stream("Url device stream");
                innerStream.initialize();
                parent = innerStream;
            }
        }
        if (parent != null) {
            dev.setParent(parent);
        }
        dev.initialize();
        if (innerStream != null) {
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
        for (int i = 0; i < url.size(); i++) {
            DeviceBase parent = null;
            if (parents.size() > 0) {
                parent = (parents.size() == 1) ? parents.get(0) : parents.get(i);
            }
            if (parent == null) {
                if (getUrlProtocol(url.get(i)).equals("bs")) {
                    if (innerStream == null) {
                        innerStream = new Stream("Url device stream");
                        innerStream.initialize();
                    }
                    parent = innerStream;
                }
            }
            Device dev = create(url.get(i), parent);
            ret.add(dev);
        }
        if (innerStream != null) {
            innerStream.start();
            innerStream.waitValueChange(Stream.TIMEOUT_START_STREAMING);
        }
        return ret;
    }

}
