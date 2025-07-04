
package ch.psi.pshell.dataviewer;

import ch.psi.pshell.app.Option;

/**
 *
 */
public enum Options implements Option{
    DUMMY;
    
    public static void add(){        
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.app.Options.FILE.add("f", "Open a data file of folder", "path");
        ch.psi.pshell.framework.Options.DATA_FORMAT.add("dfmt", "Set the data format, overriding the configuration: h5, txt, csv or fda");        
        ch.psi.pshell.framework.Options.DATA_LAYOUT.add("dlay", "Set the data layout, overriding the configuration: default, table, sf or fda");                
        
    }        
        
}
