
package ch.psi.pshell.xscan;

import ch.psi.pshell.app.Option;

/**
 *
 */
 public enum Options implements Option {
    XSCAN_PATH;
    
    public static void addSpecific(){              
        XSCAN_PATH.add("xscp", "Set the xscan file folder (default is {script})", "path");
    }
        
    public static void add(){              
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.devices.Options.addEpics();
        ch.psi.pshell.devices.Options.addPool();                
        //General options
        ch.psi.pshell.app.Options.FILE.add("f", "File to run or open in file in editor", "path");    
        ch.psi.pshell.framework.Options.PLOT_ONLY.add("x", "Start GUI with plots only");
        ch.psi.pshell.framework.Options.AUTO_CLOSE.add("a", "Auto close after executing file");        
        ch.psi.pshell.framework.Options.VOLATILE.add("z", "Home folder is volatile (created in tmp folder)");              
        ch.psi.pshell.app.Options.addPath();
        addSpecific();
        ch.psi.pshell.framework.Options.DATA_FORMAT.add("dfmt", "Set the data format, overriding the configuration: h5, txt, csv or fda");        
        ch.psi.pshell.framework.Options.DATA_LAYOUT.add("dlay", "Set the data layout, overriding the configuration: default, table, sf or fda");                        
    }    
        
}
