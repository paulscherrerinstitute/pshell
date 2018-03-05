/*
 *  Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.bs.PipelineServer;
import ch.psi.pshell.bs.Scalar;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.device.Averager;
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

    static List<Stream> cameraStreams = new ArrayList<>();

    public UrlDevice(String url) {
        this.url = url;
        this.protocol = getUrlProtocol(url);     
        this.pars = getUrlPars(url);
        this.id = getUrlId(url);
        this.name = getUrlName(url);
    }
    
    public static String getUrlProtocol(String url){
        if (!url.contains("://")) {
            throw new RuntimeException("Invalid device url: " + url);
        }        
        return url.substring(0, url.indexOf("://"));
    }
    
    public static String getUrlPath(String url){
        if (!url.contains("://")) {
            throw new RuntimeException("Invalid device url: " + url);
        }        
        return url.substring(url.indexOf("://") + 3, url.length());
    }    
    
    public static Map<String, String> getUrlPars(String url){    
        String path = getUrlPath(url);
        Map<String, String> pars = new HashMap<>();
        if (path.contains("?")) {
            for (String str : path.split("\\?")[1].split("&")) {
                String[] tokens = str.split("=");
                if (tokens.length == 2) {
                    pars.put(tokens[0].trim(), tokens[1].trim());
                }
            }
        }
        return pars;
    }    
    
    public static String getUrlName(String url){   
        Map<String, String> pars = getUrlPars(url);         
        return pars.containsKey("name") ? pars.get("name") : getUrlId(url);
    }      
    
    static String getUrlId(String url){
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
            Device dev = device;
            if ((dev instanceof Averager.AveragerStats) || (dev instanceof Averager.AveragerStatsArray)){
                dev = dev.getParent();
            }            
            if ((dev!= null) && (dev instanceof Averager)){
                dev = dev.getParent();
            }
            if (dev!= null){   
                if (Context.getInstance().isSimulation()) {
                    dev.setSimulated();
                }
                dev.initialize();
            }
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
                Boolean hasSetName = pars.containsKey("name");
                Averager av = hasSetName ? new Averager(name, (Readable) ret, samples, interval) :
                                                         new Averager((Readable) ret, samples, interval);
                if (async) {
                    av.setMonitored(true);
                }
                if (pars.containsKey("op")) {
                    String opName = hasSetName ? name : null;
                    switch(pars.get("op")){
                        case "sum": return av.getSum(opName);      
                        case "min": return av.getMin(opName);      
                        case "max": return av.getMax(opName);      
                        case "mean": return av.getMean(opName);      
                        case "stdev": return av.getStdev(opName);   
                        case "variance": return av.getVariance(opName);  
                        case "samples": return av.getSamples(opName);  
                    }
                }
                return av;
            }
            return ret;
        }
        throw new IOException("Invalid device: " + url);
    }

    void closeDevice() {
        if (device != null) {
            try {
                device.close();
                for (Device dev : instantiatedDevices) {
                    dev.close();
                    instantiatedDevices.remove(dev);
                    if (cameraStreams.contains(dev)) {
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
    
    
    public static Device create(String url, DeviceBase parent) throws IOException, InterruptedException {
        UrlDevice dev = new UrlDevice(url);
        Stream innerStream = null;
        if (parent == null){
            if (getUrlProtocol(url).equals("bs")){
                innerStream = new Stream("Url device stream");
                innerStream.initialize();     
                parent = innerStream;
            }
        }
        if (parent != null){
            dev.setParent(parent);
        }
        dev.initialize();
        if (innerStream!=null){
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
        for (int i=0; i<url.size(); i++){
            DeviceBase parent = null;
            if (parents.size() > 0){
                parent = (parents.size()==1) ? parents.get(0) : parents.get(i);
            }
            if (parent == null){
                if (getUrlProtocol(url.get(i)).equals("bs")){
                    if (innerStream==null){
                        innerStream = new Stream("Url device stream");
                        innerStream.initialize();        
                    }
                    parent = innerStream;
                }
            }            
            Device dev = create(url.get(i), parent);
            ret.add(dev);
        }
        if (innerStream!=null){
            innerStream.start();
            innerStream.waitValueChange(Stream.TIMEOUT_START_STREAMING);
        }
        return ret;
    }      

}
