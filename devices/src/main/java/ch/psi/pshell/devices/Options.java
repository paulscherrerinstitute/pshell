
package ch.psi.pshell.devices;

import ch.psi.pshell.app.Option;

/**
 *
 */
 public enum Options implements Option {
    SIMULATION,
    PARALLEL,
    EMPTY,
    PIPELINE_SERVER,
    CAMERA_SERVER,
    ARCHIVER,
    BACKEND,
    DISPATCHER,
    EPICS_CONFIG;

    public static void addEpics(){        
        EPICS_CONFIG.add("ec", "EPICS config file or EPICS options separated by '|'");             
    }
        
    public static void addCamServer(){        
        CAMERA_SERVER.add("cs", "Address of CamServer camera proxy", "url");        
        PIPELINE_SERVER.add("ps", "Address of CamServer pipeline proxy", "url");
    }

    public static void addArchiver(){        
        DISPATCHER.add("dp", "Address of Databuffer Dispatcher", "url");
        ARCHIVER.add("ar", "Address of the Daqbuf server (otherwise defined by DAQBUF_DEFAULT_URL)", "url");
        BACKEND.add("be", "Default backend (otherwise defined by DAQBUF_DEFAULT_BACKEND)", "name");        
    }    
    
    public static void addPool(){
        SIMULATION.add("s",  "All devices are simulated");
        EMPTY.add("k", "Empty mode: device pool is not loaded at startup");
        PARALLEL.add("u", "Parallel initialization of devices (values: true or false)");                                
    }
    
    public static void addSpecific(){
        addEpics();
        addCamServer();
        addArchiver();
        addPool();              
    }
    
    public static void add(){        
        ch.psi.pshell.app.Options.addBasic();
        addSpecific();
    }    
        
}
