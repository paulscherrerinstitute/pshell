/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */

package ch.psi.pshell.scan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.epics.Epics;
import java.io.IOException;

/**
 * Dynamic resolved devices, according to URL (protocol://name).
 * Can be passed as argument to scans.
 */
public class InnerDevice implements Readable, Writable{
    String url;
    public InnerDevice(String url){
        this.url = url;
    }

    @Override
    public void write(Object value) throws IOException, InterruptedException {
    }

    @Override
    public Object read() throws IOException, InterruptedException {
        return null;
    }
    
    public Object resolve() throws IOException, InterruptedException{
        String[] tokens = url.split("://");
        String protocol = tokens[0];       
        String name = tokens[1];   
        Device dev = null;
        
        switch (protocol){
            case "pv":
            case "ca":
                dev = Epics.newChannelDevice(name, name, null);                
                break;
        }
                
        if (dev!=null){
            if (Context.getInstance().isSimulation())
                dev.setSimulated();
            dev.initialize();
            return dev;
        }
        throw new IOException("Invalid device: " + url);
    }
}

