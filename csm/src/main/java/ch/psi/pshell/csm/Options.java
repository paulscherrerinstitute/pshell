
package ch.psi.pshell.csm;

import ch.psi.pshell.app.Option;

/**
 *
 */
public enum Options implements Option {
    EXPERT,
    DB_SRC_REPO,
    IB_SRC_REPO;
    
    public static void add(){        
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.devices.Options.addCamServer();      
        EXPERT.add("ex", "Expert mode");             
        DB_SRC_REPO.add("dr", "DataBuffer source repo");             
        IB_SRC_REPO.add("it", "ImageBuffer source repo");             
    }        
        
}
