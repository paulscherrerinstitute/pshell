
package ch.psi.pshell.workbench;

import ch.psi.pshell.app.Option;


/**
 *
 */
 public enum Options implements Option {
    PREFERENCES,
    STDIO;
    
    public static void addShell(){     
        ch.psi.pshell.framework.Options.add();
        ch.psi.pshell.app.Options.CONFIG.add("cfg", "Set config file (default is " + ch.psi.pshell.app.Options.CONFIG.toEnvVar() + " or else {config}/config.properties)", "path");
    }    
    
    public static void add(){           
        addShell();
        ch.psi.pshell.xscan.Options.addSpecific();
        STDIO.add("dual", "Start GUI command line interface (not allowed if running in the background)");                        
        PREFERENCES.add("pref", "Override the view preferences file", "path");                
    }    
}
