/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.scan;

import ch.psi.pshell.bs.Scalar;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.Epics;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic resolved devices, according to URL (protocol://name). Can be passed as argument to scans.
 */
public class InnerDevice extends DeviceBase implements Readable, Writable {

    final String url;
    final String protocol;
    final String name;
    final String id;
    final Map<String, String> pars;
    Device parent;

    public InnerDevice(String url) {
        if (!url.contains("://")) {            
            throw  new RuntimeException("Invalid device url: " + url);
        }
        this.url = url;
        String[] tokens = url.split("://");
        this.protocol = tokens[0];
        pars = new HashMap<>();
        String id = tokens[1];        
        if (id.contains("?")) {            
            tokens = id.split("\\?");
            id = tokens[0].trim();
            for (String str : tokens[1].split("&")) {
                tokens = str.split("=");
                if (tokens.length == 2) {
                    pars.put(tokens[0].trim(), tokens[1].trim());
                }
            }
        }
        this.id =id;
        this.name = pars.containsKey("name") ? pars.get("name") : id;
    }

    void setParent(Device parent) {
        this.parent = parent;
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
    }

    @Override
    public Object read() throws IOException, InterruptedException {
        return null;
    }

    public Device resolve() throws IOException, InterruptedException {
        Device dev = null;

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
                    type = Class.forName(pars.get("type"));
                } catch (Exception ex) {
                }

                dev = Epics.newChannelDevice(name, id, type, timestamped, precision, size);
                break;

            case "bs":
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
                if ((width >=0) && (height >=0)){
                    dev = ((Stream) parent).addMatrix(name, id, modulo, offset, width, height);
                }
                else if (waveform){
                    if (sz>=0){
                        dev = ((Stream) parent).addWaveform(name, id, modulo, offset, sz);
                    } else {
                        dev = ((Stream) parent).addWaveform(name, id, modulo, offset);
                   }
                } else{
                    dev = ((Stream) parent).addScalar(name, id, modulo, offset);
                }                 
                break;
        }

        if (dev != null) {
            if (Context.getInstance().isSimulation()) {
                dev.setSimulated();
            }
            dev.initialize();
            return dev;
        }
        throw new IOException("Invalid device: " + url);
    }
}
