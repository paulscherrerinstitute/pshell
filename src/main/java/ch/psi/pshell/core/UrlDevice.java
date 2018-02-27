/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.bs.PipelineServer;
import ch.psi.pshell.bs.Scalar;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.Readable;
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
    
    static List<Stream> cameraStreams= new ArrayList<>();

    public UrlDevice(String url) {
        if (!url.contains("://")) {
            throw new RuntimeException("Invalid device url: " + url);
        }
        this.url = url;
        this.protocol = url.substring(0, url.indexOf("://"));
        pars = new HashMap<>();
        String id = url.substring(url.indexOf("://")+3, url.length());
        if (id.contains("?")) {
            String[] tokens = id.split("\\?");
            id = tokens[0].trim();
            for (String str : tokens[1].split("&")) {
                tokens = str.split("=");
                if (tokens.length == 2) {
                    pars.put(tokens[0].trim(), tokens[1].trim());
                }
            }
        }
        this.id = id;
        this.name = pars.containsKey("name") ? pars.get("name") : id;
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
            if (Context.getInstance().isSimulation()) {
                device.setSimulated();
            }
            device.initialize();
        }
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
                } else if (this.id.equals("Timestamp")) {
                    ret = ((Stream) getParent()).getTimestampReader();
                } else {
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
                String channel  = pars.get("channel");
                Stream s = null;
                url = url.trim();
                synchronized (cameraStreams) {
                    for (Stream cs : cameraStreams.toArray(new Stream[0])) {
                        if (cs.isInitialized()){
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
                        s.waitCacheChange(10000);
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
            return ret;
        }
        throw new IOException("Invalid device: " + url);
    }

    void closeDevice() {
        if (device != null) {
            try {
                device.close();
                for (Device dev: instantiatedDevices){
                    dev.close();
                    instantiatedDevices.remove(dev);
                    if (cameraStreams.contains(dev)){
                        cameraStreams.remove(dev);
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

}
